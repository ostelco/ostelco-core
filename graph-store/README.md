# Using neo4j as Graph datasource


`cypher` is SQL like query language, based on ASCII art, to interact with graph database.

Programmatically, there are different alternatives to interact with neo4j.
They are listed below.

## JDBI

    Directly use dropwizard-jdbi.
    
 * This will involve having JDO interfaces, which will have methods, having annotations with cypher queries directly in 
their annotations.
 * Can handle complex cypher queries, which also means no compile-time checks.
 * Duplication of code for normal CRUD operation for most of the entities.
 * Ref:
   * https://www.dropwizard.io/1.3.2/docs/manual/jdbi3.html
   * https://neo4j.com/docs/developer-manual/3.3/cypher/

##  jCypher

    Java DSL for cypher.

 * Java Fluent DSL equivalent for cypher.
 * Ref: https://github.com/Wolfgang-Schuetzelhofer/jcypher/wiki

## OGM

    Object-Graph-Mapping, which is similar to ORM for relational database.

 * Has annotations like `NodeEntity` & `RelationEntity` for classes, `Relationship` for member collections.
 * Ref: https://neo4j.com/docs/ogm-manual/current/tutorial/

## Traversal framework Java API
 * Ref: https://neo4j.com/docs/java-reference/3.3/tutorial-traversal/

## Java Cypher API using Bolt protocol fro Embedded Neo4j
 * Ref: https://neo4j.com/docs/java-reference/3.3/tutorials-java-embedded/#tutorials-java-embedded-bolt
