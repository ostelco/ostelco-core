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

TODO
===

* Set up skeleton kotlin code. [Done]
* Move to standard gradle setup [Done]
* Reduce the gradle stuff to something simple (with Vihang).[done]
* Run something from the command line ("hello world") [done]
* Set up a pushgateway running in a test environment using
  * Prometheus: https://github.com/evnsio/prom-stack. [done]
          docker run -p 9090:9090 -v $(pwd)/tmp/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus
  * Pushgateway: https://hub.docker.com/r/prom/pushgateway/
          docker pull prom/pushgateway
          docker run -d -p 9091:9091 prom/pushgateway

* Send something from the program to a pushgateway running somewhere [Done]
* Make the skeleton code read something (anything) from BigQuery, using config
  that is production-like.  [Done]
* Make a sensible metric encodedin SQL, read it from BQ and push it to pushgateway. [Done]
* Test that this works all the way in docker compose [Done]
* MOdify to accept reasonable parameters for location of pushgateway [Done].
* Build a docker image. [DONE]
* Run the docker image as a kubernetes cronjob [done]
* Make an acceptance tests that runs a roundtrip test ub
  in docker compose, based on something like this: curl http://localhost:9091/metrics | grep -i active
* Push the first metric to production, use Kubernetes crontab
  to ensure periodic execution.
* Make command to run metric every N seconds.
* Make it testable to send send metrics to pushgateway.
* Extend to more metrics.
