FROM openjdk:8u151-jre-alpine3.7

MAINTAINER CSI "csi@telenordigital.com"

RUN apk update && apk add unzip

ADD build/distributions/ocsgw-1.0-SNAPSHOT.zip /ocsgw-1.0-SNAPSHOT.zip

RUN unzip ocsgw-1.0-SNAPSHOT.zip

CMD ["ocsgw-1.0-SNAPSHOT/bin/ocsgw"]