# SIM Manager DB schema

This directory contains the DB schema for the SIM manager PostgreSQL DB, and
Docker build file for acceptance tests involving the SIM manager DB.

## DB schema

 * [Main schema definition](./init.sql)
 * [Schema file for updating existing DBs with timestamps](./add-timestamps.sql)
 * [Schema file for integration tests](../simmanager/src/integration-test/resources/init.sql)

## Acceptance tests and the Docker image

Referenced from the main [Docker compose](../../docker-compose.yaml) file and will be built
automatically as part of running the acceptance tests.

## Integration test

The [DB schema](../simmanager/src/integration-test/resources/init.sql) used in SIM manager
integration tests must be updated as needed on changes to the main [DB schema](./init.sql).
