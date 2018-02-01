FROM openjdk:8u151-jre-alpine3.7

MAINTAINER CSI "csi@telenordigital.com"

RUN apk update && apk add unzip && apk add netcat-openbsd

COPY script/start.sh /start.sh
COPY script/wait_for_prime.sh /wait_for_prime.sh

COPY build/libs/ocsgw-uber.jar /ocsgw.jar

CMD ["/start.sh"]