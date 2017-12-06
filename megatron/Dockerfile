FROM openjdk:8u141

MAINTAINER CSI "csi@telenordigital.com"

ADD files/start.sh /start.sh
RUN chmod +x /start.sh

ADD config /config

ADD build/libs/prime-uber.jar /prime.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

CMD ["/start.sh"]
