FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update \
 && apt-get install -y netcat

COPY bin/start.sh /start.sh
RUN chmod +x /start.sh

COPY build/libs/ext-pgw-uber.jar /ext-pgw.jar

COPY script/wait_for_ocsgw.sh /wait_for_ocsgw.sh

CMD ["/start.sh"]
