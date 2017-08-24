/*
 * The MIT License
 *
 * Copyright 2017 ElasTest
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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author frdiaz
 *
 */
public class ExternalJob implements Serializable {
	private static final long serialVersionUID = 1;
	
	@JsonProperty("jobName")
	private String jobName;

	@JsonProperty("executionUrl")
	private String executionUrl;
	
	@JsonProperty("analyzerUrl")
	private String logAnalyzerUrl;
	
	@JsonProperty("tJobExecId")
	private Long tJobExecId;	
	
	@JsonProperty("logstashPort")
	private String logstashPort;
	
	@JsonProperty("servicesIp")	
	private String servicesIp;
		
	public ExternalJob(){}
	
	public ExternalJob(String jobName){
		this.jobName = jobName;
	}
	
	public ExternalJob(String jobName, String executionUrl, String logAnalyzerUrl,  Long tJobExecId,
			String logstashPort, String servicesIp){
		this.jobName = jobName;
		this.executionUrl = executionUrl;
		this.logAnalyzerUrl = logAnalyzerUrl;
		this.tJobExecId = tJobExecId;
		this.logstashPort = logstashPort;
		this.servicesIp = servicesIp;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public String getExecutionUrl() {
		return executionUrl;
	}
	public void setExecutionUrl(String executionUrl) {
		this.executionUrl = executionUrl;
	}
	
	public String getLogAnalyzerUrl() {
		return logAnalyzerUrl;
	}

	public void setLogAnalyzerUrl(String logAnalyzerUrl) {
		this.logAnalyzerUrl = logAnalyzerUrl;
	}

	public Long gettJobExecId() {
		return tJobExecId;
	}

	public void settJobExecId(Long tJobExecId) {
		this.tJobExecId = tJobExecId;
	}		
	
		
	public String getLogstashPort() {
		return logstashPort;
	}

	public void setLogstashPort(String logstashPort) {
		this.logstashPort = logstashPort;
	}
	
	public String getServicesIp() {
		return servicesIp;
	}

	public void setServicesIp(String servicesIp) {
		this.servicesIp = servicesIp;
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExternalJob externalJob = (ExternalJob) o;
		return Objects.equals(this.jobName, externalJob.jobName) && Objects.equals(this.executionUrl, externalJob.executionUrl)
				&& Objects.equals(this.logAnalyzerUrl, externalJob.logAnalyzerUrl)
				&& Objects.equals(this.tJobExecId, externalJob.tJobExecId) 
				&& Objects.equals(this.logstashPort, externalJob.logstashPort)
				&& Objects.equals(this.servicesIp, externalJob.servicesIp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobName, executionUrl, tJobExecId);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class DeployConfig {\n");
		sb.append("    jobName: ").append(toIndentedString(jobName)).append("\n");
		sb.append("    executionUrl: ").append(toIndentedString(executionUrl)).append("\n");
		sb.append("    logAnalyzerUrl: ").append(toIndentedString(logAnalyzerUrl)).append("\n");
		sb.append("    tJobExecId: ").append(toIndentedString(tJobExecId)).append("\n");
		sb.append("    logstashPort: ").append(toIndentedString(logstashPort)).append("\n");
		sb.append("    servicesIp: ").append(toIndentedString(servicesIp)).append("\n");
		sb.append("}");
		
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
	
	public String toJSON(){
		ObjectMapper mapper = new ObjectMapper();

		//Object to JSON in String
		String jsonInString;
		try {
			jsonInString = mapper.writeValueAsString(this);
			return jsonInString;

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	
}
