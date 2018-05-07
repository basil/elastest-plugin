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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Allows to set a new decorate logger that sends the logs of a 
 * build to ElasTest.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 *
 */
public class ConsoleLogFilterImpl extends ConsoleLogFilter
        implements Serializable {
    private static final long serialVersionUID = 1L;
    private final transient Run<?, ?> build;
    private ElasTestService elasTestService;

    public ConsoleLogFilterImpl(Run<?, ?> build,
            ElasTestService elasTestService) {
        this.build = build;
        this.elasTestService = elasTestService;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public OutputStream decorateLogger(Run _ignore, OutputStream logger)
            throws IOException, InterruptedException {

        ElasTestWriter elasTestWriter = getElasTestWriter(build, logger,
                elasTestService.getExternalJobByBuildFullName(build.getFullDisplayName()));
        ElasTestOutputStream los = new ElasTestOutputStream(logger,
                elasTestWriter);

        return los;
    }

    // Method to encapsulate calls for unit-testing
    ElasTestWriter getElasTestWriter(Run<?, ?> build, OutputStream errorStream,
            ExternalJob externalJob) {
        return new ElasTestWriter(build, errorStream, null, externalJob);
    }
}