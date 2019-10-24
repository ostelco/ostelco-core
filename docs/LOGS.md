# Prime logs in GCP

 * The logs are added to GCP stackdriver via console logs in json layout.
 
## To view logs

### Advanced filter
 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=GCP_PROJECT_ID)
 * Open hidden-menu from right of Search bar and select `Convert to advanced filter`

```properties
resource.type="k8s_container"
resource.labels.namespace_name="dev"
resource.labels.container_name="prime"
```

### Basic filter

 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=GCP_PROJECT_ID)
 * GKE container > private-cluster > All namespace_id
 * You can expand a single log and filter to log prime-only logs.

# OCS Gateway logs in GCP

Same steps as above. Use the filter below:

```properties
resource.type="gce_instance"
logName="projects/GCP_PROJECT_ID/logs/ocsgw"
```