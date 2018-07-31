# Playbook for working with Neo4j

## Connecting to database.

This is a temporary solution till we have a proper means of setup:

### Set neo4j -> localhost entry in your `/etc/hosts`
 * On your developer machine, to `/etc/hosts` file, add `neo4j` entry pointing to `localhost`.

Your `/etc/hosts` should have this line.
```text
127.0.0.1	localhost  neo4j
```

### Set proper cluster in `kubectl` config

 * Set your `kubectl config` to point to correct kubernetes cluster.

Check your current cluster.
```bash
kubectl config get-contexts
```

If name of the cluster, where neo4j is deployed, is `private-cluster`, then change `kubectl config`. 
```bash
kubectl config set-context $(kubectl config get-contexts --output name | grep private-cluster) 
```

### Port forward from neo4j pods

```bash
kubectl get pods
```

Choose one of the neo4j pod from the list.

```bash
kubectl port-forward neo4j-core-2 7474:7474 7687:7687
``` 

Here, `neo4j browser` web-app is exposed over port `7474`.
The client-side/in-browser web-app then tries to connect to neo4j database over `bolt protocol`, exposed over port `7687`.

### Login

In the browser, use connection URL as: `bolt://neo4j:7687`.
User name and password will be ignored.

The database will expects connections only for hostname `neo4j`, and hence the setup in `/etc/hosts`.

### Fetch entire graph

Once logged in , use this cypher query to fetch entire graph.

```cypher
MATCH (n) RETURN n;
```

### Write Access

The current setup for Neo4j is a 3 node `casual cluster`.
In this setup, there is only one instance which does `read + write` whereas other 2 instances are `read only`.
In the Neo4j browser web-app, you may check this using command `:sysinfo`.
The cluster members with role as `Leader` will have `read + write` access, and those with the role `Follower` will
have `read only` access.

## Neo4j Tools

`neo4j` has following tools: 
 * cypher-shell
 * neo4j-admin
 * neo4j-import
 * neo4j-shell

They can be installed individually on developer machine, or they can accessed directly from inside a docker container. 

```bash
docker run --rm neo4j ls -l /var/lib/neo4j/bin/
```

### Performing backup

```bash
docker run --rm -v $(pwd):/var/workspace/ neo4j \
  /var/lib/neo4j/bin/neo4j-admin backup --from=neo4j --backup-dir=/var/workspace/ --name=graph.db --pagecache=4G
```

Reference: https://neo4j.com/docs/operations-manual/current/backup/perform-backup/