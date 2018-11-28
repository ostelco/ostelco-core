FROM openjdk:11-jre
ADD ./target/esim-1.0-SNAPSHOT.jar app.jar
ADD comnfig.yml config.yml
EXPOSE 8010
ENTRYPOINT ["java", "-jar","/app.jar", "server", "config.yml"]
