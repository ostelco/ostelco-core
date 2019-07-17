# Stripe Monitoring and payment transactions check

## Monitoring

The service/cron-job script:

```
./prime/infra/stripe-monitor-task.yaml
```

can be used for periodically verifying whether [Stripe](https://stripe.com) is
reachable and that the configuration is correct.

As part of the monitoring check, the list of "subscribed to" events with Stripe
is also checked. This list can be updated using the script:

```
./payment-processor/script/update-webhook.sh
```

On changes in the "subscribed to" list of events, the corresponding list of
events in `stripe-monitor-task.yaml` must also be updated. For the list of
events that can be subscribed to, see Stripes
[types of events](https://stripe.com/docs/api/events/types) documentation.


## Payment transactions check

Periodic check of whether payment transactions stored with Stripe matches
purchase records stored in the backed can be done using the service/cron-job
script:

```
./prime/infra/payment-transaction-check-task.yaml
```

## Status cron jobs

The status/output of the last few completed cron jobs can be checked as follows:

```
$ kubectl -n dev get pods
NAME                                 READY   STATUS      RESTARTS   AGE
monitor-stripe-1563347400-8c47t      1/1     Running     0          3s
prime-6785b464d8-bgv9n               7/7     Running     0          13m
prime-6785b464d8-qxtrp               7/7     Running     0          14m
prime-6785b464d8-r9j2h               7/7     Running     0          13m
scaninfo-shredder-1563343200-bxd2g   0/1     Completed   0          70m
scaninfo-shredder-1563345000-6grdl   0/1     Completed   0          40m
scaninfo-shredder-1563346800-kxsn5   0/1     Completed   0          10m
$ kubectl -n dev logs monitor-stripe-1563347400-8c47t
Checking Stripe
Wed Jul 17 07:10:04 UTC 2019
Hello:
200
API version:
{"match":true}
200
Webhook enabled:
{"enabled":true}
200
Event subscription:
{"match":true}
200
Failed events last 2100 seconds:
{"fetchedEvents":0}
200
Checks completed
```

## Stripe health check

For Stripe API availability the endpoint `https://api.stripe.com/healthcheck`
can be used. Example:

```
$ curl -sS --stderr - -w "%{http_code}\n" https://api.stripe.com/healthcheck
frontend--03cc61e711c8b85c3.northwest-5.apiori.com at your service! What can I do for you today? (Up)
200
```

Check of this endpoint is currently not included as part of the
[monitoring](#monitoring) task, but can f.ex. be added as a
[Prometheus](https://prometheus.io/) job or similar.
