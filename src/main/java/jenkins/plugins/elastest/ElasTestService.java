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
package jenkins.plugins.elastest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import hudson.model.Run;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.json.TestSupportServices;
import jenkins.plugins.elastest.pipeline.ElasTestStep;

/**
 * Service to communicate with ElasTest and store the ifo related to each TJob execution by build.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestService implements Serializable {
    private static final long serialVersionUID = 1;
    private static final Logger log = LoggerFactory
            .getLogger(ElasTestService.class);
    private static ElasTestService instance;

    private HashMap<String, ExternalJob> externalJobs;
    private Map<String, String> tSServicesCatalog;
    private String elasTestApiUrl;
    private String elasTestUrl;
    private String credentialsB64;
    private transient Client client;

    public ElasTestService() {
        this.externalJobs = new HashMap<>();
        elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
        elasTestApiUrl = elasTestUrl + "/api/external/tjob";        
        client = Client.create();
        String name = ElasTestInstallation.getLogstashDescriptor().username;
        String password = ElasTestInstallation.getLogstashDescriptor().password;
        if ((name != null && !name.equals("")) &&
                (password != null && !password.equals(""))) {
            String authString = name + ":" + password;
            credentialsB64 = new Base64().encodeAsString(authString.getBytes());
        }
        
        tSServicesCatalog = loadTSSCatalog();
    }

    private Map<String, String> loadTSSCatalog() {
        Map<String, String> tSSCatalog = new HashMap<>();
        tSSCatalog.put("EUS", "29216b91-497c-43b7-a5c4-6613f13fa0e9");
        tSSCatalog.put("EBS", "a1920b13-7d11-4ebc-a732-f86a108ea49c");
        tSSCatalog.put("EMS", "bab3ae67-8c1d-46ec-a940-94183a443825");
        tSSCatalog.put("ESS", "af7947d9-258b-4dd1-b1ca-17450db25ef7");
        tSSCatalog.put("EDS", "fe5e0531-b470-441f-9c69-721c2b4875f2");

        return tSSCatalog;
    }

    public ExternalJob asociateToElasTestTJob(Run<?, ?> build, ElasTestBuildWrapper elasTestBuilder)
            throws Exception {
        log.info("Associate a Job to a TJob {}", build.getParent().getDisplayName());
        ExternalJob externalJob = new ExternalJob(
                build.getParent().getDisplayName());
        if (elasTestBuilder.isEus()) {
            List<String> tss = new ArrayList<>();
            tss.add("EUS");
            externalJob.setTSServices(prepareTSSToSendET(tss));
        }
        externalJob = asociateToElasTestTJob(build, externalJob);
        return externalJob;
    }

    public ExternalJob asociateToElasTestTJob(Run<?, ?> build,
            ElasTestStep elasTestStep) throws Exception {
        ExternalJob externalJob = new ExternalJob(
                build.getParent().getDisplayName());
        externalJob.setTSServices(prepareTSSToSendET(elasTestStep.getTss()));
        externalJob = asociateToElasTestTJob(build, externalJob);
        
        return externalJob;
    }

    public ExternalJob asociateToElasTestTJob(Run<?, ?> build,
            ExternalJob externalJob) throws Exception {
        externalJob.settJobExecId(0L);
        externalJob = createTJobOnElasTest(externalJob);
        externalJob.setExecutionUrl(externalJob.getExecutionUrl());
        externalJob.setLogAnalyzerUrl(externalJob.getLogAnalyzerUrl());
        externalJobs.put(build.getId(), externalJob);

        return externalJob;
    }

    private ExternalJob createTJobOnElasTest(ExternalJob externalJob)
            throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestApiUrl);

        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("application/json")
                            .header("Authorization", "Basic " + credentialsB64)
                            .post(ClientResponse.class, externalJob.toJSON())
                    : webResource.type("application/json")
                            .post(ClientResponse.class, externalJob.toJSON());
            externalJob = objetMapper.readValue(
                    response.getEntity(String.class), ExternalJob.class);
        } catch (Exception e) {
            log.error("Error in the creation of a TJob in ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }
    
    public ExternalJob isReadyTJobForExternalExecution(ExternalJob externalJob)
            throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestApiUrl)
                .path(externalJob.gettJobExecId().toString());

        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("application/json")
                            .header("Authorization", "Basic " + credentialsB64)
                            .get(ClientResponse.class)
                    : webResource.type("application/json")
                            .get(ClientResponse.class);
            externalJob = objetMapper.readValue(
                    response.getEntity(String.class), ExternalJob.class);
        } catch (Exception e) {
            log.error("Error in the creation of a TJob in ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public void sendJobInformationToElasTest(ExternalJob externalJob) {
        log.info("Finalization message.");
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestApiUrl);
        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("application/json")
                            .header("Authorization", "Basic " + credentialsB64)
                            .put(ClientResponse.class, externalJob.toJSON())
                    : webResource.type("application/json")
                            .put(ClientResponse.class, externalJob.toJSON());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public ExternalJob getExternalJobByBuildId(String id) {
        return externalJobs.get(id);
    }

    public HashMap<String, ExternalJob> getExternalJobs() {
        return externalJobs;
    }

    public void removeExternalJobs(String buildId) {
        externalJobs.remove(buildId);
    }

    public static synchronized ElasTestService getInstance() {
        if (instance == null) {
            instance = new ElasTestService();
        } else {
            instance.updateInstance();
        }        

        return instance;
    }

    private List<TestSupportServices> prepareTSSToSendET(
            List<String> tSServices) {
        List<TestSupportServices> eTTSServices = new ArrayList<>();
        for (String tSSName : tSServices) {
            if (tSServicesCatalog.containsKey(tSSName)) {
                TestSupportServices newTSService = new TestSupportServices(
                        tSServicesCatalog.get(tSSName), tSSName, true);
                eTTSServices.add(newTSService);
            }
        }
        return eTTSServices;
    }
    
    private void updateInstance() {
        log.info("Updating ElasTest service instance");
        instance.elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
        instance.elasTestApiUrl = elasTestUrl + "/api/external/tjob";
        instance.client = Client.create();
        String name = ElasTestInstallation.getLogstashDescriptor().username;
        String password = ElasTestInstallation.getLogstashDescriptor().password;
        if ((name != null && !name.equals("")) &&
                (password != null && !password.equals(""))) {
            String authString = name + ":" + password;
            instance.credentialsB64 = new Base64().encodeAsString(authString.getBytes());
            log.info("Now access to ElasTest is with username and password.");
        } else {
            instance.credentialsB64 = null;
            log.info("Now access to ElasTest is without username and password.");
        }
                
        instance.tSServicesCatalog = loadTSSCatalog();
    }
}
