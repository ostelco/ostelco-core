FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

ADD config.yml config.yml
ADD build/libs/simmanager-uber.jar app.jar
EXPOSE 8080
EXPOSE 8081

CMD ["java", "-jar","/app.jar", "server", "config.yml"]
