FROM openjdk:11.0.1-jdk

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config/config.yaml /config/

COPY build/libs/prime-uber.jar /prime.jar

EXPOSE 7687
EXPOSE 8080
EXPOSE 8081
EXPOSE 8082
EXPOSE 8083

CMD ["/start.sh"]