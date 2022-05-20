/*
 * Copyright ServiceNow, Inc. 2022. All rights reserved.
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 */
package org.jenkinsci.plugins.lightstepincidentresponse;
import hudson.model.Result;
import hudson.tasks.Notifier;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.model.AbstractProject;
import hudson.Extension;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.lang.*;
import jenkins.model.Jenkins;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

public class LightstepWebhookPublisher extends Notifier {

	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
	private static final Logger log = Logger.getLogger(LightstepWebhookPublisher.class.getName());

	public String webHookUrl;
	public Boolean onFailure;
	public Boolean onUnstable;
	public Boolean onAborted;
	public Boolean onResolve;
	public String failureSeverity;
	public String unstableSeverity;
	public String abortedSeverity;
	public Boolean selectedStatus;

	@DataBoundConstructor
	public LightstepWebhookPublisher(String webHookUrl, Boolean onSuccess, Boolean onFailure, Boolean onUnstable, Boolean onAborted, Boolean onResolve, String successSeverity, String failureSeverity, String unstableSeverity, String abortedSeverity) {
		super();
		this.webHookUrl = webHookUrl;
		this.onFailure = onFailure;
		this.onUnstable = onUnstable;
		this.onAborted = onAborted;
		this.onResolve = onResolve;
		this.failureSeverity = failureSeverity;
		this.unstableSeverity = unstableSeverity;
		this.abortedSeverity = abortedSeverity;
		this.selectedStatus = onFailure || onUnstable || onAborted;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		Result result = build.getResult();
		String res = "";

		if (result != null) {
			res = result.toString();
		} else {
			log.severe("No build result.");
		}

		String webHookUrl = this.webHookUrl;
		if (webHookUrl.isEmpty()) {
			log.severe("No webhook URL provided.");
		}
		String buildUrl = "";
		if (Jenkins.get().getRootUrl() == null) {
			buildUrl = build.getUrl();
		} else {
			buildUrl = Jenkins.get().getRootUrl() + build.getUrl();
		}
		String jobName = build.getProject().getDisplayName();
		String buildName = build.getDisplayName();
		Integer buildNum = build.number;
		String buildId = build.getId().toString();
		String buildStatusURL = build.getBuildStatusUrl().toString();
		String buildStatusSummary = build.getBuildStatusSummary().toString();
		String buildDuration = build.getDurationString();
		String buildTime = build.getTime().toString();

		JSONObject event = new JSONObject();
		event.put("buildName", buildName);
		event.put("jobName", jobName);
		event.put("buildUrl", buildUrl);
		event.put("buildNum", buildNum);
		event.put("buildId", buildId);
		event.put("buildStatusURL", buildStatusURL);
		event.put("buildStatusSummary", buildStatusSummary);
		event.put("buildDuration", buildDuration);
		event.put("buildTime", buildTime);
		event.put("source_url", buildUrl);

		if (this.selectedStatus) {
			String severity = "";
			String status = "";
			switch (res) {
				case "SUCCESS":
					if (this.onResolve) {
						severity = "clear";
						status = "resolved";
					}
					break;
				case "FAILURE":
					if (this.onFailure) {
						severity = this.failureSeverity;
						status = "failure";
					}
					break;
				case "UNSTABLE":
					if (this.onUnstable) {
						severity = this.unstableSeverity;
						status = "unstable";
					}
					break;
				case "ABORTED":
					if (this.onAborted) {
						severity = this.abortedSeverity;
						status = "aborted";
					}
					break;
				default:
					log.info("Build result did not match. Default case executed.");
			}
			if (!severity.isEmpty() && !status.isEmpty()) {
				event.put("status", status);
				event.put("severity", severity);
				log.info("Build Payload " + event);
				httpPost(webHookUrl, event);
			}
		}
		return true;
	}

	private void httpPost(String url, Object object) {
		try {
			String jsonString = JSONValue.toJSONString(object);
			RequestBody body = RequestBody.create(jsonString, JSON_MEDIA_TYPE);
			Request request = new Request.Builder().url(url).post(body).build();
			OkHttpClient client = new OkHttpClient();
			Response response = client.newCall(request).execute();
			ResponseBody responseBody = response.body();
			String resp = "";
			if (  responseBody != null ) {
				resp = responseBody.string();
			} else {
				log.info("No response from webhook");
			}

			try {
				if (response.code() == 200) {
					log.info("Webhook invocation successful " + responseBody);
				} else {
					log.severe("Webhook invocation failed " + responseBody);
				}
			} catch (Exception e) {
				log.severe("Exception occurred " + url + e);
			} finally {
				if (response != null) {
					response.close();
				}
			}
		} catch (Exception e) {
			log.severe("Exception " + url + e);
		}
	}

	@Override
	public LightstepWebhookPublisherDescriptor getDescriptor() {
		return (LightstepWebhookPublisherDescriptor) super.getDescriptor();
	}

	@Extension
	public static class LightstepWebhookPublisherDescriptor extends BuildStepDescriptor<Publisher> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	
		@Override
		public String getDisplayName() {
			return "Send alerts to Lightstep Incident Response";
		}

		public ListBoxModel doFillFailureSeverityItems() {
			ListBoxModel failureSevModel = addSeverityValues();
			return failureSevModel;
		}

		public ListBoxModel doFillUnstableSeverityItems() {
			ListBoxModel unstableSevModel = addSeverityValues();
			return unstableSevModel;
		}

		public ListBoxModel doFillAbortedSeverityItems() {
			ListBoxModel abortedSevModel = addSeverityValues();
			return abortedSevModel;
		}

		public LinkedHashMap<String, String> setSeverityValues () {
			LinkedHashMap<String, String> severity = new LinkedHashMap();
			severity.put("P1-Critical","critical");
			severity.put("P2-High", "high");
			severity.put("P3-Moderate", "moderate");
			severity.put("P4-Low", "low");
			severity.put("P5-Informational", "informational");
			return severity;
		}

		public ListBoxModel addSeverityValues() {
			ListBoxModel model = new ListBoxModel();
			LinkedHashMap<String, String> sevHashMap = setSeverityValues();
			for (Map.Entry<String, String> mapElement :
					sevHashMap.entrySet()) {
				String sevKey = mapElement.getKey();
				String sevValue = mapElement.getValue();
				model.add(sevKey, sevValue);
			}
			return model;
		}
	}
}
