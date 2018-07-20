# Instructions to install neo4j on GCP

For enterprise version of Neo4j, a `casual` cluster can be created in GCP using `App Launcher`.
Only prerequisite for this is to have Enterprise license for Neo4j.
This is already well documented and hence will not covered here.
Instead this document focuses on deploying the `Community` edition of Neo4j.
This setup is not intended for production since that requires serious Operation and Maintenance of Neo4j.

### Reference
 * https://neo4j.com/developer/guide-cloud-deployment/
 * https://medium.com/google-cloud/running-neo4j-with-hosted-kubernetes-in-google-cloud-b479e87b74c0

### Steps

 * Dev cluster
Deploy Neo4j

```bash
kubectl apply -f infra/dev/neo4j.yaml
```