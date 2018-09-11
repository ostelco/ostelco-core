package org.ostelco.tools.migration

import org.neo4j.driver.v1.AccessMode

fun main(args: Array<String>) {
    cypherFileToNeo4jImporter()
}

fun cypherFileAndFirebaseToNeo4jMigration() {
    initFirebase()

    Neo4jClient.init()

    Neo4jClient.driver.session(AccessMode.WRITE).use {

        val txn = it.beginTransaction()

        println("Import from file to Neo4j")

        importFromCypherFile("src/main/resources/init.cypher") {
            query -> txn.run(query)
        }

        println("Exporting from firebase and import it to Neo4j")
        importFromFirebase {
            createQuery -> txn.run(createQuery)
        }

        println("Done")
        txn.success()
    }
    Neo4jClient.stop()
}

fun cypherFileToNeo4jImporter() {

    Neo4jClient.init()

    Neo4jClient.driver.session(AccessMode.WRITE).use {

        val txn = it.beginTransaction()

        println("Import from file to Neo4j")

        importFromCypherFile("src/main/resources/init.cypher") {
            query -> txn.run(query)
        }

        println("Done")
        txn.success()
    }
    Neo4jClient.stop()
}