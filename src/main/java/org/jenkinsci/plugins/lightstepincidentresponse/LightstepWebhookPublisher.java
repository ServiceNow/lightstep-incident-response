/*
 * Copyright ServiceNow, Inc. 2022. All rights reserved.
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 */
package org.jenkinsci.plugins.lightstepincidentresponse;
import hudson.tasks.Notifier;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.model.AbstractProject;
import hudson.Extension;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

public class LightstepWebhookPublisher extends Notifier{

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
		return true;
	}

	@Override
	public LighstepWebhookPublisherDescriptor getDescriptor() {
		return (LighstepWebhookPublisherDescriptor) super.getDescriptor();
	}

	@Extension
	public static class LighstepWebhookPublisherDescriptor extends BuildStepDescriptor<Publisher> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Send alerts to Lightstep Incident Response";
		}

	}
}
