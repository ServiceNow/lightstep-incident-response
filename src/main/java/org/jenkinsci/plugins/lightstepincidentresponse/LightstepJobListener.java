/*
 * Copyright ServiceNow, Inc. 2022. All rights reserved.
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 */
package org.jenkinsci.plugins.lightstepincidentresponse;
import com.sun.tools.javac.main.Main;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;
import hudson.model.TaskListener;
import hudson.model.Result;
import hudson.model.BuildListener;
import javax.annotation.Nonnull;
import java.util.logging.Logger;
import java.lang.*;
import jenkins.model.Jenkins;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Extension
public class LightstepJobListener extends RunListener<AbstractBuild> {

	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

	private static final Logger log = Logger.getLogger(LightstepJobListener.class.getName());

	public LightstepJobListener() {
		super(AbstractBuild.class);
	}

	@Override
	public void onCompleted(AbstractBuild build, @Nonnull TaskListener listener) {

		LightstepWebhookPublisher publisher = getLightstepWebhookPublisher(build);
		if (publisher == null) {
			log.severe("No post build action for Lightstep Incident Response.");
			return;
		}
		Result result = build.getResult();
		String res = result.toString();

		if (result == null) {
			log.severe("No build result.");
			return;
		}

		String webHookUrl = publisher.webHookUrl;
		if (webHookUrl.isEmpty()) {
			log.severe("No webhook URL provided.");
		}
		String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
		String jobName = build.getProject().getDisplayName();
		String buildName = build.getDisplayName();
		Integer buildNum =  build.number;
		String buildId = build.getId().toString();
		String buildStatusURL = build.getBuildStatusUrl().toString();
		String buildStatusSummary = build.getBuildStatusSummary().toString();
		String buildDuration = build.getDurationString();
		String buildTime = build.getTime().toString();

		JSONObject event = new JSONObject();
		event.put("buildName" , buildName);
		event.put("jobName" , jobName);
		event.put("buildUrl" , buildUrl);
		event.put("buildNum" , buildNum);
		event.put("buildId" , buildId);
		event.put("buildStatusURL" , buildStatusURL);
		event.put("buildStatusSummary" , buildStatusSummary);
		event.put("buildDuration" , buildDuration);
		event.put("buildTime" , buildTime);
		event.put("source_url" , buildUrl);

		if ( publisher.selectedStatus ) {
			String severity = " ";
			String status = " ";
			switch (res) {
				case "SUCCESS":
					if (publisher.onResolve) {
						severity = "clear";
						status = "resolved";
					}
					break;
				case "FAILURE":
					if (publisher.onFailure) {
						severity = publisher.failureSeverity;
						status = "failure";
					}
					break;
				case "UNSTABLE":
					if (publisher.onUnstable) {
						severity = publisher.unstableSeverity;
						status = "unstable";
					}
					break;
				case "ABORTED":
					if (publisher.onAborted) {
						severity = publisher.abortedSeverity;
						status = "aborted";
					}
					break;
			}
			if(!severity.isEmpty() && !status.isEmpty()) {
				event.put("status", status);
				event.put("severity", severity);
				log.info("Build Payload " + event);
				httpPost(webHookUrl, event);
			}
		}
	}

		private LightstepWebhookPublisher getLightstepWebhookPublisher(AbstractBuild build){
			for (Object publisher : build.getProject().getPublishersList().toMap().values()) {
				if (publisher instanceof LightstepWebhookPublisher) {
					return (LightstepWebhookPublisher) publisher;
				}
			}
			log.severe("No post build action for Lightstep Incident Response ");
			return null;
		}
		private void httpPost(String url, Object object) {
		try {
			String jsonString = JSONValue.toJSONString(object);
			RequestBody body = RequestBody.create(jsonString, JSON_MEDIA_TYPE);
			Request request = new Request.Builder().url(url).post(body).build();
			OkHttpClient client = new OkHttpClient();
			Response response = client.newCall(request).execute();
			String responseBody = response.body().string();

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
}

