package org.ostelco.tools.migration

import org.neo4j.driver.v1.Transaction

fun importFromNeo4j(txn: Transaction, handleCypher: (String) -> Unit) {

    val sb = StringBuilder()

    run {
        val stmtResult = txn.run("MATCH (n) RETURN n;")
        stmtResult.forEach { record ->
            val node = record["n"].asNode()
            val labels = node.labels().joinToString(separator = "", prefix = ":")

            val props = node.asMap().toSortedMap().map { entry ->
                "`${entry.key}`: '${entry.value}'"
            }.joinToString(separator = ",\n")

            sb.append("CREATE ($labels {$props});\n\n")
        }
    }

    run {
        val stmtResult = txn.run("MATCH (n)-[r]->(m) RETURN n,r,m;")
        stmtResult.forEach { record ->
            val fromNode = record["n"].asNode()
            val relation = record["r"].asRelationship()
            val toNode = record["m"].asNode()

            val type = relation.type()

            var props = relation.asMap().toSortedMap().map { entry ->
                "`${entry.key}`: '${entry.value}'"
            }.joinToString(separator = ",\n")

            props = if (props.isNotBlank()) {
                " {$props}"
            } else {
                props
            }

            sb.append(
"""
MATCH (n:${fromNode.labels().first()} {id: '${fromNode.asMap()["id"]}'})
  WITH n
MATCH (m:${toNode.labels().first()} {id: '${toNode.asMap()["id"]}'})
CREATE (n)-[:$type$props]->(m);
""")
        }
    }

    handleCypher(sb.toString())
}