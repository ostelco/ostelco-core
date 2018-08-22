# Instructions to install neo4j on GCP

For enterprise version of Neo4j, a `casual` cluster can be created in GCP using `App Launcher`.
Neo4j is licensed as A-GPL.
Since `ostelco-core` is an open source project, we can use Enterprise version of Neo4j.
This setup is not intended for production since that requires serious Operation and Maintenance of Neo4j.

### Reference
 * https://neo4j.com/developer/guide-cloud-deployment/
 * https://medium.com/google-cloud/running-neo4j-with-hosted-kubernetes-in-google-cloud-b479e87b74c0

### Steps

 * Dev cluster
Deploy Neo4j

```bash
kubectl apply -f prime/infra/dev/neo4j.yaml
```

 * Private (prod) cluster
Deploy Neo4j

```bash
kubectl apply -f prime/infra/prod/neo4j.yaml
```