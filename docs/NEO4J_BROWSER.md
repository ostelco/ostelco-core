# Neo4j Browser

 * Clone git repo
```bash
git clone https://github.com/neo4j/neo4j-browser.git
cd neo4j-browser
```

 * Install yarn (if not already)
```bash
npm install -g yarn
```

 * Change port from `8080` to `7474` in `webpack.config.js`

 * Make sure you are using python v2
```bash
python --version
``` 

 * Install dependencies.
```bash
yarn
```

 * Start browser server
```bash
yarn start
```

 * Open Neo4j browser at [http://localhost:7474](http://localhost:7474) in web browser.
 * Run `server connect` to connect to embedded Neo4j in prime, exposing `bolt` protocol over `7686`.
 * Connect to Neo4j as:
```text
   Host: 0.0.0.0:7687
   Username: <blank>
   Password: <blank> 
```

### Known issue

 * Neo4j browser hangs with `Connecting...` message.<br>
   This is observed for embedded Neo4j in prime.<br>
   But, it works when embedded Neo4j is run directly by `main` function in `EmbeddedNeo4jModule.kt`.  