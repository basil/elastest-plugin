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
package jenkins.plugins.elastest.submitters;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import jenkins.plugins.elastest.json.ExternalJob;

/**
 * 
 * @author Francisco R Díaz
 * @since 0.0.1
 */
abstract class AbstractElasTestSubmitter implements ElasTestSubmitter {
    protected final String host;
    protected final int port;
    protected final String key;
    protected final String username;
    protected final String password;

    AbstractElasTestSubmitter(String host, int port, String key,
            String username, String password) {
        this.host = host;
        this.port = port;
        this.key = key;
        this.username = username;
        this.password = password;

        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("host name is required");
        }
    }

    @Override
    public String buildPayload(List<String> logLines, ExternalJob externalJob) {
        String payload = logLines.get(0);
        payload = "{" + "\"component\":\"test\"" + ",\"exec\":\""
                + externalJob.gettJobExecId() + "\""
                + ",\"stream\":\"default_log\"" + ",\"message\":\"" + payload
                + "\"" + "}";

        return payload;
    }

    @Override
    public String getDescription() {
        return this.host + ":" + this.port;
    }
}
