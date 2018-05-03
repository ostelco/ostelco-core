# Prime logs in GCP

 * The logs are added to GCP stackdriver directly & to console in json layout, both.
 
## To view logs

### Direct link 
 * [Direct link to prime stackdriver logs](https://console.cloud.google.com/logs/viewer?project=pantel-2decb&resource=container%2Fcluster_name%2Fprivate-cluster&minLogLevel=0&expandAll=false&dateRangeStart=2018-05-03T07%3A15%3A33.842Z&dateRangeEnd=2018-05-03T08%3A15%3A33.842Z&interval=PT1H&scrollTimestamp=2018-05-03T07%3A50%3A54.811000000Z&advancedFilter=resource.type%3D%22container%22%0Aresource.labels.cluster_name%3D%22private-cluster%22%0AlogName%3D%22projects%2Fpantel-2decb%2Flogs%2Fjava.log%22)
 * [Direct link to prime json console logs](https://console.cloud.google.com/logs/viewer?project=pantel-2decb&minLogLevel=0&expandAll=false&resource=container%2Fcluster_name%2Fprivate-cluster%2Fnamespace_id%2Fdefault&scrollTimestamp=2018-05-03T08%3A32%3A15.000000000Z&dateRangeStart=2018-05-03T07%3A47%3A25.921Z&dateRangeEnd=2018-05-03T08%3A47%3A25.921Z&interval=PT1H&advancedFilter=resource.type%3D%22container%22%0Aresource.labels.cluster_name%3D%22private-cluster%22%0Aresource.labels.namespace_id%3D%22default%22%0AlogName%3D%22projects%2Fpantel-2decb%2Flogs%2Fprime%22)

### Advanced filter
 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=pantel-2decb)
 * Open hidden-menu from right of Search bar and select `Convert to advanced filter`

**Prime json console logs**

    resource.type="container"
    resource.labels.cluster_name="private-cluster"
    resource.labels.namespace_id="default"
    logName="projects/pantel-2decb/logs/prime"

**Prime stackdriver logs**

    resource.type="container"
    resource.labels.cluster_name="private-cluster"
    logName="projects/pantel-2decb/logs/java.log"

### Basic filter

 * Goto [this link](https://console.cloud.google.com/logs/viewer?project=pantel-2decb)
 * GKE container > private-cluster > All namespace_id
 * You can expand a single log and filter to log prime-only logs.


