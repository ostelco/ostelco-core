# Prometheus

## Setup

Anything that wants to be scraped by prometheus requires at least one annotation, see the [promotheus section](#prometheus)

## Access

### Grafana

Through external ip:

```bash
kubectl get services --namespace=monitoring | grep grafana | awk '{print $4}'
```

### Prometheus

Through port forwarding:

```bash
kubectl port-forward --namespace=monitoring $(kubectl get pods --namespace=monitoring | grep prometheus-core | awk '{print $1}') 9090
```

## Discovery

Prometheus is configured to do service discovery

## Scrape

Prometheus is configured to scrape metrics over https, thus the `ConfigMap` for `prometheus-core` requires path to ca_file from kubernetes secrets

## [Pushgateway](https://github.com/prometheus/pushgateway)

Used jobs that might not live long enough to be scraped by prometheus.

Example usage:
```bash
# Push a metric to pushgateway:8080 (specified in the service declaration for pushgateway)
kubectl run curl-it --image=radial/busyboxplus:curl -i --tty --rm
echo "some_metric 4.71" | curl -v  --data-binary @- http://pushgateway:8080/metrics/job/some_job
```
 
## [Monitoring.yaml](dev/monitoring.yaml)

Is completely based on manifests-all.yaml from this (github repo)[https://github.com/giantswarm/kubernetes-prometheus]

### Namespace
Defines monitoring namespace

### [Alert Manager](https://prometheus.io/docs/alerting/alertmanager/)

__`TODO: Add email config and / or slack api url for alerts to work`__

Defines alerts that can be sent by email or to slack.

Contains two config maps, one defining the alert template and another to configure the alertmanager itself.
There is also a kubernetes Deployment and Service configuration.

### [Grafana](https://grafana.com/)

__`TODO: Discuss how grafana should be exposed. With LoadBalancer and / or through existing ingress using auth0 to authenticate users before they can access the dashboard.`__
__`TODO: Figure out a better way / automatic way to backup dashboards and automatically import them on redeploy`__

Contains a Deployment and Service configuration and a ConfigMap with predefined dashboards.

Grafana is exposed using a LoadBalancer. 

### [](#prometheus)[Prometheus](https://prometheus.io/)

__`TODO: scraping happens over https thus requires a ca_file, figure out if this is automatically handled or if we need to add a ca file to kubernetes secrets`__

Contains a Deployment configuration and a ConfigMap. The ConfigMap defines how and what prometheus scrapes etc.

There are more configurations at the end of the file, containing ConfigMap for prometheus rules, ClusterRoleBinding, ClusterRole, ServiceAccount and finally the prometheus Service itself.

The following roles are being scraped: 
nodes, endpoints, services, pods

Given that they set the required annotations which tells prometheus that they should be scaped, see below:

`add the annotations to the pods, that means the config with type Deployment, StatefulSet, DaemonSet`
```yaml
metadata:
  annotations:
    prometheus.io/scrape: 'true' # REQUIRED: has to be set for prometheus to scrape
    prometheus.io/port: '9102' # OPTIONAL: defaults to '9102' 
    prometheus.io/path: '' # OPTIONAL: defaults to '/metrics'
    prometheus.io/scheme: '' # OPTIONAL: http or https defaults to 'https'
```

### [Kube State Metrics](https://github.com/kubernetes/kube-state-metrics)

See the above link for documentation.

### Extra Prometheus Metrics

Some DaemonSets that define different prometheus metrics, not sure if this is a general config or if its connected to any of the other configurations.
 

 


