FROM azul/zulu-openjdk:8u212

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config /config

COPY build/libs/dataflow-pipelines-uber.jar /dataflow-pipelines.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

CMD ["/start.sh"]