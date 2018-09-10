# Neo4j Admin Tools

## Online Backup

This is keep the terminal running until `ctrl+C`
```bash
kubectl config use-context $(kubectl config get-contexts --output name | grep dev-cluster)
kubectl get pods
kubectl port-forward neo4j-core-0 6362:6362 7474:7474 7687:7687
```

```bash
docker-compose -f docker-compose.backup.yaml up
```

Then stop port-forwarding terminal using `ctrl+C`.

### Restore to local neo4j

```bash
docker-compose -f docker-compose.restore.yaml up
```

### Export from local Neo4j to cypher script.

 * Run neo4j locally using restored data.
```bash
docker-compose -f docker-compose.neo4j.yaml up
```
 * Run tool with `neo4jExporterToCypherFile()`. The cypher file will be created at `src/main/resources/backup.cypher`.

## Online dump to cypher script.

This is keep the terminal running until `ctrl+C`

```bash
kubectl config use-context $(kubectl config get-contexts --output name | grep dev-cluster)
kubectl get pods
kubectl port-forward neo4j-core-0 7474:7474 7687:7687
```

Run tool with `neo4jExporterToCypherFile()`. The cypher file will be created at `src/main/resources/backup.cypher`.

Then stop port-forwarding terminal using `ctrl+C`.