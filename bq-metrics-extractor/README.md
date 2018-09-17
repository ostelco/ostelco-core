BigQuery metrics extractor
=======


This module is a standalone, command-line launched dropwizard application
that will:

* Talk to google [BigQuery](https://cloud.google.com/bigquery/) and
  extract metrics using [Googles BigQuery java library](https://cloud.google.com/bigquery/docs/reference/libraries)
* Talk to [Prometheus](https://prometheus.io)
  [Pushgateway](https://github.com/prometheus/pushgateway) and push
  those metrics there.  Prometheus servers can then scrape those
  metrics at their leisure.  We will use the
  [Prometheus java client](https://github.com/prometheus/client_java)
  to talk to the pushgateway.

The component will be built as a docker component, and will then be periodically
run as a command line application, as a
[Kubernetes cron job](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/).


The component is packaged as an individual docker artefact (details below),
and deployed as a cronjob (also described below). 

To run the program from the command line, which is useful when debugging and
necessary to know when constructing a Docker file, do this:

         java -jar /bq-metrics-extractor.jar query --pushgateway pushgateway:8080 config/config.yaml
  
the pushgateway:8080 is the hostname  (dns resolvable) and portnumber of the Prometheus Push Gateway.
  
The config.yaml file contains specifications of queries and how they map to metrics:
  
        bqmetrics:
        - type: summary
          name: active_users
          help: Number of active users
          resultColumn: count
          sql: >
          SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
          WHERE event_name = "first_open"
          LIMIT 1000
  
The idea being that to add queries of a type that is already know by the extractor program,
only an addition to the bqmetrics list.   
Use [standardSQL syntax  (not legacy)](https://cloud.google.com/bigquery/sql-reference/) for queries.

If not running in a google kubernetes cluster (e.g. in docker compose, or from the command line),
it's necessary to set the environment variable GOOGLE_APPLICATION_CREDENTIALS to point to 
a credentials file that will provide access for the BigQuery library.



How to build and deploy the cronjob manually
===

##First get credentials (upgrade gcloud for good measure):

    gcloud components update
    gcloud container clusters get-credentials dev-cluster --zone europe-west1-b --project pantel-2decb

##Build the artefact:

    gradle build
    docker build .

##Authorize tag and push to docker registry in google cloud:

    gcloud auth configure-docker
    docker tag foobarbaz eu.gcr.io/pantel-2decb/bq-metrics-extractor
    docker push eu.gcr.io/pantel-2decb/bq-metrics-extractor

... where foobarbaz is the id of the container built by docker build.

## Then start the cronjob in kubernetes
    kubectl apply -f cronjob/extractor.yaml
    kubectl describe cronjob bq-metrics-extractor

## To talk to the prometheus in the monitoring namespace & watch the users metrics evolve
    kubectl port-forward --namespace=monitoring $(kubectl get pods --namespace=monitoring | grep prometheus-core | awk '{print $1}') 9090
    watch 'curl -s localhost:9090/metrics | grep users'


TODO
===

* Rewrite the SQL so that it will pick up only today/yesterday's data,
  use a template language, either premade or ad-hoc.
  As of now, the sql in config is static.
  We need to make it as a template.
  Table name to be changed from events_* to events_${yyyyMMdd}

  Here, the suffix is yesterday’s date in yyyyMMdd format. (edited)
  There are some libraries which we can use, which enable f
  reemarker expressions in dropwizard config file 
  (this comment is @vihangpatil 's I just nicked it from 
  slack where he made the comment)


* Add more metrics.  
* Make an acceptance tests that runs a roundtrip test ub
  in docker compose, based on something like this: curl http://localhost:9091/metrics | grep -i active
* Push the first metric to production, use Kubernetes crontab
  to ensure periodic execution.
* Make it testable to send send metrics to pushgateway.
* Extend to more metrics.
* remove the TODO list and declare victory :-)