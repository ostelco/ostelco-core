FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat \
 && rm -rf /var/lib/apt/lists/*

COPY bin/start.sh /start.sh
RUN chmod +x /start.sh

COPY src/main/resources/ /

COPY build/libs/ext-pgw-uber.jar /ext-pgw.jar

COPY script/wait_for_ocsgw.sh /wait_for_ocsgw.sh

CMD ["/start.sh"]
