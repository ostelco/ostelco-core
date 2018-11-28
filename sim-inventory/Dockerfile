FROM openjdk:11-jre
ADD ./target/esim-1.0-SNAPSHOT.jar app.jar
ADD config.yml config.yml
EXPOSE 80 81
ENTRYPOINT ["java", "-jar","/app.jar", "server", "config.yml"]
