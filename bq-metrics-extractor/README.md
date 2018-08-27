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
[Kubernetes cron job](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/) 
(or perhaps some other meaningful deployment architecture that we will dream up eventually).


XXX NOTE: This code was initiated using yeoman, and while functional that seems to be
have been a mistake.  It will have to be refactored into something much
leaner asap, and certainly before merging to develop.
