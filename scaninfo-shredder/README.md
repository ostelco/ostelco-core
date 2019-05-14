Scan Information Shredder
=======

This module is a standalone, command-line launched dropwizard application
that will delete the Scan information stored in JUMIO data center.

The component is as a docker component, and will then be periodically
run as a command line application, as a
[Kubernetes cron job](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/).

The component is packaged as an individual docker artefact (details below),
and deployed as a cronjob (also described below). 

To run the program from the command line, which is useful when debugging and
necessary to know when constructing a Docker file, do this:

         java -jar /scaninfo-shredder.jar shred config/config.yaml
  
Up on a successful eKYC scan, all the data gathered by JUMIO is encrypted and dumped to
a bucket in cloud storage by prime.  This information is not immediately deleted from
JUMIO's databases. This cron job will wait for 2 weeks and then delete them from JUMIO.
This delete can be avoided by removing the datastore entry for that individual scan.

How to build and deploy the cronjob manually
===

##Build and deploy the artifact:

Build and deploy to dev cluster

    scaninfo-shredder/cronjob/deploy-dev-direct.sh

Build and deploy to prod cluster

    scaninfo-shredder/cronjob/deploy-direct.sh

## Display the cronjob status in kubernetes

    kubectl describe cronjob scaninfo-shredder
