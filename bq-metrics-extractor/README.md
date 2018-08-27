BigQuery metrics extractor
=======


This module is a standalone, command-line launched dropwizard application
that will:

* Talk to google [BigQuery](https://cloud.google.com/bigquery/) and extract metrics.
* Talk to [Prometheus](https://prometheus.io) [Pushgateway](https://github.com/prometheus/pushgateway) and push those metrics there.  Prometheus servers can then scrape those metrics at their leisure.

The component will be built as a docker component, and will then be periodically run as a command line application, as a [Kubernetes batch job]
(https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/) (or perhaps some other meaningful deployment architecture that we will dream up eventually).


XXX NOTE: This code was initiated using yeoman, and while functional that seems to be
have been a mistake.  It will have to be refactored into something much
leaner asap, and certainly before merging to develop.