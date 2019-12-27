package org.ostelco.tools.migration

import org.neo4j.driver.v1.AccessMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun main() {
    neo4jExporterToCypherFile("prod")
    // cypherFileToNeo4jImporter()
}

fun neo4jExporterToCypherFile(env: String) {

    Neo4jClient.init()

    Neo4jClient.driver.session(AccessMode.READ).use { session ->

        val txn = session.beginTransaction()

        println("Import from Neo4j to file")

        importFromNeo4j(txn) { str ->
            Files.write(Paths.get("src/main/resources/backup.$env.${now()}.cypher"), str.toByteArray())
        }

        println("Done")
        txn.success()
    }

    Neo4jClient.stop()
}

fun cypherFileToNeo4jImporter() {

    Neo4jClient.init()

    Neo4jClient.driver.session(AccessMode.WRITE).use { session ->

        val txn = session.beginTransaction()

        println("Import from file to Neo4j")

        importFromCypherFile("src/main/resources/backup.cypher") { query ->
            txn.run(query)
        }

        println("Done")
        txn.success()
    }

    Neo4jClient.stop()
}

fun now() = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd-HHmm'UTC'"))