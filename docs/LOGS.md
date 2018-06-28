# Prime logs in GCP

 * The logs are added to GCP stackdriver via console logs in json layout.
 
## To view logs

### Direct link 
 * [Direct link to prime logs](https://console.cloud.google.com/logs/viewer?project=pantel-2decb&minLogLevel=0&expandAll=false&resource=container%2Fcluster_name%2Fprivate-cluster%2Fnamespace_id%2Fdefault&scrollTimestamp=2018-05-09T11%3A54%3A03.000000000Z&dateRangeStart=2018-05-09T10%3A55%3A37.736Z&dateRangeEnd=2018-05-09T11%3A55%3A37.736Z&interval=PT1H&customFacets&limitCustomFacetWidth=true&advancedFilter=resource.type%3D%22container%22%0Aresource.labels.cluster_name%3D%22private-cluster%22%0Aresource.labels.namespace_id%3D%22default%22%0AlogName%3D%22projects%2Fpantel-2decb%2Flogs%2Fprime%22)

### Advanced filter
 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=pantel-2decb)
 * Open hidden-menu from right of Search bar and select `Convert to advanced filter`

```properties
resource.type="container"
resource.labels.cluster_name="private-cluster"
logName="projects/pantel-2decb/logs/prime"
```

### Basic filter

 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=pantel-2decb)
 * GKE container > private-cluster > All namespace_id
 * You can expand a single log and filter to log prime-only logs.

# OCSGW logs in GCP

Same steps as above. Use the filter below:

```properties
resource.type="global"
logName="projects/pantel-2decb/logs/ocsgw"
```