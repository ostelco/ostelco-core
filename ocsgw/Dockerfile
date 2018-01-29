FROM openjdk:8u151-jre-alpine3.7

MAINTAINER CSI "csi@telenordigital.com"

RUN apk update && apk add unzip && apk add netcat-openbsd

COPY build/distributions/ocsgw-1.0.0-SNAPSHOT.zip /ocsgw-1.0.0-SNAPSHOT.zip

COPY script/wait_for_prime.sh /wait_for_prime.sh

RUN unzip ocsgw-1.0.0-SNAPSHOT.zip

CMD ["ocsgw-1.0.0-SNAPSHOT/bin/ocsgw"]