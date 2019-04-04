FROM gcr.io/distroless/java:11

# FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

COPY config/customer.graphqls /config/
COPY config/config.yaml /config/

COPY build/libs/prime-uber.jar /prime.jar

EXPOSE 7687
EXPOSE 8080
EXPOSE 8081
EXPOSE 8082
EXPOSE 8083

# Uncomment line below if you want to use openjdk
# ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar"]

CMD ["/prime.jar", "server", "/config/config.yaml"]