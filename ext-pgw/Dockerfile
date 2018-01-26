FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

COPY bin/start.sh /start.sh
RUN chmod +x /start.sh

COPY build/libs/ext-pgw-uber.jar /ext-pgw.jar

CMD ["/start.sh"]
