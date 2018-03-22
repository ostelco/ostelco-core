FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

COPY script/start.sh /start.sh

COPY config /config

COPY build/libs/pseudonymiser-uber.jar /pseudonymiser.jar

EXPOSE 8080
EXPOSE 8081

CMD ["/start.sh"]