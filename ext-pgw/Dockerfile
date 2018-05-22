FROM openjdk:8u171

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat \
 && rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/wait_for_ocsgw.sh /wait_for_ocsgw.sh
COPY src/main/resources/ /

COPY build/libs/ext-pgw-uber.jar /ext-pgw.jar

CMD ["/start.sh"]
