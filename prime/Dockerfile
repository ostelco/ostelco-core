FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

COPY script/start.sh /start.sh

COPY config/config.yaml /config/

COPY build/libs/prime-uber.jar /prime.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

CMD ["/start.sh"]