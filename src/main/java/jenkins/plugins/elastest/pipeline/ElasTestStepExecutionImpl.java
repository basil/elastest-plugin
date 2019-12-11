/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.elastest.pipeline;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import jenkins.plugins.elastest.ConsoleLogFilterImpl;
import jenkins.plugins.elastest.EIMManager;
import jenkins.plugins.elastest.ElasTestService;
import jenkins.plugins.elastest.ElasTestWriter;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.docker.DockerCommandExecutor;
import jenkins.plugins.elastest.docker.DockerService;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Execution for {@link ElasTestStep}.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestStepExecutionImpl extends AbstractStepExecutionImpl {

    private static final Logger LOG = LoggerFactory.getLogger(ElasTestStepExecutionImpl.class);
    private static final long serialVersionUID = 1L;
    private static final String ETM_CONTAINER_NAME = "elastest_etm_1";

    private static final String EIM_API_KEY = "ET_EIM_API";
    private static final String EIM_PACKETLOSS_KEY = "ET_EIM_CONTROLLABILLITY_PACKETLOSS";
    private static final String EIM_CPUBURST_KEY = "ET_EIM_CONTROLLABILLITY_CPUBURST";

    private ElasTestService elasTestService;
    private DockerService dockerService;
    private ElasTestWriter writer;
    private DockerCommandExecutor dockerCommandExecutor;

    @Inject
    transient ElasTestStep elasTestStep;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean start() throws Exception {
        System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");
        
        elasTestService = ElasTestService.getInstance();
        StepContext context = getContext();
        Run<?, ?> build = context.get(Run.class);
        ElasTestBuild elasTestBuild = null;

        LOG.info("[elastest-plugin]: Working on build {}", build.getFullDisplayName());
        try {
            // Init Build Context
            elasTestBuild = new ElasTestBuild();
            elasTestBuild.setWorkspace(context.get(FilePath.class));
            // Associate the Jenkins' Job to an ElasTest Job
            elasTestService.asociateToElasTestTJob(build, elasTestStep, elasTestBuild);
        } catch (Exception e) {
            LOG.error("[elastest-plugin]: Error trying to bind the build with a TJob.");
            e.printStackTrace();
            throw e;
        }

        try {

            // Add the ElasTest menu item to the left menu
            ElasTestItemMenuAction.addActionToMenu(build);
            // Wait until the ElasTest Job is ready
            while (!elasTestBuild.getExternalJob().isReady()) {
                elasTestBuild.setExternalJob(elasTestService
                        .isReadyTJobForExternalExecution(elasTestBuild.getExternalJob()));
                try {
                    if (!elasTestBuild.getExternalJob().isReady()) {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException | IllegalArgumentException ie) {
                    LOG.warn("[elastest-plugin]: {}", ie.getMessage());
                }
            }
            writer = new ElasTestWriter(build, null,
                    elasTestService.getExternalJobByBuildFullName(build.getFullDisplayName()));
            elasTestBuild.setWriter(writer);
            // Set environment variables
            addEnvVars(build);
            // If monitoring is true, start monitoring
            if (elasTestStep.isMonitoring()) {
                dockerService = DockerService
                        .getDockerService(DockerService.DOCKER_HOST_BY_DEFAULT);
                dockerCommandExecutor = new DockerCommandExecutor(null, dockerService);
                startMonitoringContainers(elasTestStep.envVars, elasTestBuild,
                        context.get(FilePath.class).getChannel());
            }

        } catch (Exception e) {
            LOG.error("[elastest-plugin]: Error preparing Job execution.");
            e.printStackTrace();
            throw e;
        }

        ExpanderImpl expanderImpl = new ExpanderImpl();
        expanderImpl.setOverrides(elasTestStep.envVars);
        expanderImpl.expand(getContext().get(EnvVars.class));
        context.newBodyInvoker().withContext(createConsoleLogFilter(context, build))
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                        expanderImpl))
                .withCallback(BodyExecutionCallback.wrap(getContext())).start();

        manageEIMIfNecessary(build, elasTestStep);

        return false;
    }

    private static final class ExpanderImpl extends EnvironmentExpander {
        private static final long serialVersionUID = 1;
        private Map<String, String> overrides = new HashMap<>();

        @Override
        public void expand(EnvVars env) throws IOException, InterruptedException {
            env.overrideAll(overrides);
        }

        public void setOverrides(Map<String, String> overrides) {
            this.overrides = overrides;
        }
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context, Run<?, ?> build)
            throws IOException, InterruptedException {
        LOG.debug("[elastest-plugin]: Creatin console log filter.");
        ConsoleLogFilterImpl logFilterImpl = new ConsoleLogFilterImpl(build, writer);
        return logFilterImpl;
    }

    private void addEnvVars(Run<?, ?> build) {
        ExternalJob externalJob = elasTestService
                .getExternalJobByBuildFullName(build.getFullDisplayName());
        elasTestStep.envVars.putAll(externalJob.getEnvVars() != null ? externalJob.getEnvVars()
                : new HashMap<String, String>());
    }

    private void startMonitoringContainers(EnvVars envVars, ElasTestBuild elasTestBuild,
            VirtualChannel channel) throws IOException, RuntimeException, InterruptedException {
        LOG.info("[elastest-plugin]: Start container monitoring");
        String fileBeatImage = "elastest/etm-filebeat:latest";
        String dockBeatImage = "elastest/etm-dockbeat:latest";

        String logstashHost = "LOGSTASHHOST="
                + (!envVars.get("ET_MON_LSBEATS_HOST").trim().equals("localhost")
                        ? envVars.get("ET_MON_LSBEATS_HOST")
                        : dockerService.getGatewayFromContainer(ETM_CONTAINER_NAME));
        String logstashPort = "LOGSTASHPORT=" + envVars.get("ET_MON_INTERNAL_LSBEATS_PORT");
        String etMonLsbeatsHost = "ET_MON_LSBEATS_HOST=" + envVars.get("ET_MON_LSBEATS_HOST");
        String etMonLsbeatsPort = "ET_MON_LSBEATS_PORT=" + envVars.get("ET_MON_LSBEATS_PORT");
        String etMonContainersName = "ET_MON_CONTAINERS_NAME=" + "^("
                + envVars.get("ET_SUT_CONTAINER_NAME") + ")(_)?(\\d*)(.*)?";

        if (isRemoteElasTest(channel)) {
            dockerCommandExecutor.setCommand("docker", "run", "-d", "--name",
                    "fileBeat_" + envVars.get("ET_SUT_CONTAINER_NAME"), "-e", etMonLsbeatsHost,
                    "-e", etMonLsbeatsPort, "-e", etMonContainersName, "-v",
                    "/var/run/docker.sock:/var/run/docker.sock", "-v",
                    "/var/lib/docker/containers:/var/lib/docker/containers", fileBeatImage);
            LOG.info("[elastest-jenkins]: Built command to execute {}",
                    Arrays.toString(dockerCommandExecutor.getCommand()));
            elasTestBuild.getContainers()
                    .add(processDockeCommandOutput(channel.call(dockerCommandExecutor)));
        }

        dockerCommandExecutor.setCommand("docker", "run", "-d", "--name",
                "dockBeat_" + envVars.get("ET_SUT_CONTAINER_NAME"), "-e", logstashHost, "-e",
                logstashPort, "-v", "/var/run/docker.sock:/var/run/docker.sock", "-v",
                "/var/lib/docker/containers:/var/lib/docker/containers", dockBeatImage);

        LOG.info("[elastest-jenkins]: Built command to execute {}",
                Arrays.toString(dockerCommandExecutor.getCommand()));
        elasTestBuild.getContainers()
                .add(processDockeCommandOutput(channel.call(dockerCommandExecutor)));
    }

    private String processDockeCommandOutput(String output) {
        if (output.contains(":")) {
            output = output.substring(output.lastIndexOf(":latest") + 7);
        }
        return output;
    }

    private boolean isRemoteElasTest(VirtualChannel channel)
            throws IOException, RuntimeException, InterruptedException {
        LOG.info("[elastest-plugin]: Checking if ElasTest is running locally.");
        boolean result = true;
        String etContainername = "elastest_etm_1";
        String errorMessage = "No such object: " + etContainername;
        dockerCommandExecutor.setCommand("docker", "inspect", "--format=\\\"{{.Name}}\\\"",
                etContainername);
        result = channel.call(dockerCommandExecutor).contains(errorMessage);
        LOG.debug("[elastest-plugin]: Result of the inspect command: {}", result);
        return result;
    }

    private void manageEIMIfNecessary(Run<?, ?> build, ElasTestStep elasTestStep) {
        try {
            EnvVars buildVars = build.getEnvironment();
            EnvVars etBuildVars = elasTestStep.envVars;
            LOG.info("buildVarsbuildVarsbuildVarsbuildVarsbuildVars {}",buildVars);
            LOG.info("etBuildVarsetBuildVarsetBuildVarsetBuildVars {}",etBuildVars);

            String EIM_AGENTID_KEY = "ET_EIM_SUT_AGENT_ID";
            if (etBuildVars.containsKey(EIM_API_KEY) && etBuildVars.containsKey(EIM_AGENTID_KEY)
                    && (buildVars.containsKey(EIM_PACKETLOSS_KEY)
                            || buildVars.containsKey(EIM_CPUBURST_KEY))) {
                String eimApiUrl = etBuildVars.get(EIM_API_KEY);
                EIMManager eimManager = new EIMManager(eimApiUrl);

                String agentId = etBuildVars.get(EIM_AGENTID_KEY);

                // Packetloss
                if (buildVars.containsKey(EIM_PACKETLOSS_KEY)) {
                    String packetLossValue = buildVars.get(EIM_PACKETLOSS_KEY);
                    LOG.info("Sending packet loss {} to agent {} through EIM at {}",
                            packetLossValue, agentId, eimApiUrl);
                    eimManager.sendPacketLoss(agentId, packetLossValue);
                }

                // Cpu burst
                if (buildVars.containsKey(EIM_CPUBURST_KEY)) {
                    String cpuBurstValue = buildVars.get(EIM_CPUBURST_KEY);
                    LOG.info("Sending cpu burst {} to agent {} through EIM at {}", cpuBurstValue,
                            eimApiUrl);
                    eimManager.sendCpuBurst(agentId, cpuBurstValue);
                }

            }
        } catch (Exception e) {
            LOG.warn("[elastest-plugin] EIM manage: {}", e.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }

}
