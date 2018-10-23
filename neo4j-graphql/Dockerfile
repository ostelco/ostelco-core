FROM neo4j:3.4.7

MAINTAINER "eng@redotter.sg"

RUN wget -P /var/lib/neo4j/plugins https://github.com/neo4j-graphql/neo4j-graphql/releases/download/3.4.0.1/neo4j-graphql-3.4.0.1.jar

RUN echo 'dbms.unmanaged_extension_classes=org.neo4j.graphql=/graphql' | tee -a /var/lib/neo4j/conf/neo4j.conf

CMD ["neo4j"]