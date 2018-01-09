FROM openjdk:8u141

MAINTAINER CSI "csi@telenordigital.com"

COPY files/start.sh /start.sh
RUN chmod +x /start.sh

COPY config /config

COPY build/libs/prime-uber.jar /prime.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

CMD ["/start.sh"]
