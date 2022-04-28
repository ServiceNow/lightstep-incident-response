Copyright ServiceNow, Inc. 2022. All rights reserved.
This package is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
# Lightstep Incident Response Plugin for Jenkins

The Lightstep Incident Response plugin for Jenkins when activated will trigger alerts to [Lightstep Incident Response](https://lightstep.com/incident-response) in response to build status of a Jenkins FreeStyle job.

## Features
- Trigger alerts in LIR on build statuses failure, unstable and aborted.
- Associate priority to alerts on build statuses.
- Automatically resolve alerts when build status is success.

# Prerequisites
  1. Jenkins 2.319.1+ instance.
  2. Lightstep Incident Response account. Please click [here](https://lightstep.com/incident-response/signup) to get a free trial account.

# Installation
Download Lightstep Incident Response Plugin from Jenkins Update Center. Restart jenkins to see the changes.

# Configuration
Select 'Send alerts to Lightstep Incident Response' from Post-Build Actions of your FreeStyle Job.
You need to copy the Webhook URL from LIR application in Webhook URL field.
Select the build statuses you want to get alerts in LIR. Select the Priority for the alert, corresponding to the build status.
If you want to let open alerts in LIR to get Closed when build status is Success, select 'Resolve on Back-To-Normal'.

![LIR](https://github.com/ServiceNow/lightstep-incident-response/blob/main/post-build%20action.png)

# Sample Payload

```
{
  "severity": "low",
  "buildNum": 9999999998,
  "buildName": "#9999999998",
  "buildTime": "Tue Mar 08 15:14:07 IST 2022",
  "buildId": "2022-03-08_15-14-07",
  "buildStatusURL": "blue.png",
  "buildUrl": "****/job/[Alerting]:JenkinsSampleAlert/9999999998/",
  "jobName": "[Alerting]: Jenkins Sample Alert",
  "buildStatusSummary": "hudson.model.Run$Summary@d4c513f",
  "buildDuration": "34 ms",
  "status": "failure"
}
```


## License

MIT


## Configuration Documentation from Lightstep Incident Response

[Documentation](https://lightstep.com/incident-response/docs/integrations-jenkins)



