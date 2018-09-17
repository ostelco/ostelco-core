FROM azul/zulu-openjdk:8u181-8.31.0.1

MAINTAINER CSI "csi@telenordigital.com"

COPY script/start.sh /start.sh

COPY config /config

COPY build/libs/dataflow-pipelines-uber.jar /dataflow-pipelines.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

CMD ["/start.sh"]