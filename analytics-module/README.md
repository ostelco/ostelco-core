# analytics-module

## Events
Analytics events are defined in [Events.kt](src/main/kotlin/org/ostelco/prime/analytics/events/Events.kt).

<span style="color:red">Warning:</span> when modifying event data classes, make sure that you update
the schema for the BigQuery table, otherwise events with the new schema will get discarded into the
dead-letter table.

JSON schemas for the events can be found in [bigquery/](bigquery).

## Data flows
As illustrated here:

![Data Flows](misc/dataflows.svg?raw=true&sanitize=true)

Commands for generating the BigQuery tables from the JSON schemas are to be found in [this file](bigquery/create-bq-tables.sh).
Corresponding DataFlow jobs are created using commands in [here](bigquery/run-dataflow-jobs.sh).

## Known limitations & Potential improvements
### Lineage
For now:
- event classes,
- schemas for the JSON objects that derive from those events,
- Cloud Pub/Sub topic to which those events are sent,
- Cloud Dataflow jobs consuming from the aforementioned topics, and
- BigQuery schemas & table hierarchy that Dataflow writes events to

are not linked together programmatically, and modifying the event class without updating the
BigQuery schema will cause breaking changes.

### Cost
Using one topic and one standard Dataflow template per event type is costly, since each Dataflow
job will spin up its own set of cloud instances and scale up independently.

It could be desirable to use one single Pubsub topic to which all events are sent, and then a single
Dataflow job that consumes from that topic, and writes events to the correct BigQuery table according
to their type. Thus, the computing power would be distributed across all event types and this would
be a far more efficient use of resources. However, implementing this Dataflow job is not trivial and
would take up some time. In addition, we would have to monitor and maintain that code ourselves,
which we don't have to do with the current pipelines created with the standard template.
