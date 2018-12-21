FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

ADD example.keystore example.keystore
ADD src/test/resources/fixtures/sample-sim-batch-for-sm-dp+.csv sample-sim-batch-for-sm-dp+.csv
ADD config.yml config.yml
ADD build/libs/sm-dp-plus-uber.jar app.jar

EXPOSE 8080
EXPOSE 8081

CMD ["java", "-jar","/app.jar", "server", "config.yml"]
