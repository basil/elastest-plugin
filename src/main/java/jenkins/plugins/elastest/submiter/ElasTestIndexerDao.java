/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
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

package jenkins.plugins.elastest.submiter;

import java.io.IOException;
import java.util.List;

import jenkins.plugins.elastest.json.ExternalJob;
import net.sf.json.JSONObject;

/**
 * Interface describing data access objects for Logstash indexers.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public interface ElasTestIndexerDao {
    static enum IndexerType {
        LOGSTASH ("logstash");
        
        private final String name;       

        private IndexerType(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {             
            return name.equals(otherName);
        }

        public String toString() {
           return this.name;
        }
    }    

    String getDescription();

    IndexerType getIndexerType();

    /**
     * Sends the log data to the Logstash indexer.
     *
     * @param data
     *            The serialized data, not null
     * @throws java.io.IOException
     *             The data is not written to the server
     */
    void push(String data) throws IOException;

    /**
     * Builds a JSON payload compatible with the Logstash schema.
     *
     * @param buildData
     *            Metadata about the current build, not null
     * @param jenkinsUrl
     *            The host name of the Jenkins instance, not null
     * @param logLines
     *            The log data to transmit, not null
     * @return The formatted JSON object, never null
     */
    JSONObject buildPayload(BuildData buildData, String jenkinsUrl,
            List<String> logLines, ExternalJob externalJjob);

    /**
     * Bulds a String playload compatible con the Logstash input.
     * 
     * @param logLines
     * @param externalJob
     * @return
     */
    String buildPayload(List<String> logLines, ExternalJob externalJob);
}