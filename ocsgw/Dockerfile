FROM openjdk:8u151-jre-alpine3.7

RUN apk update && apk add unzip

RUN ls -la

ADD build/distributions/ocsgw-1.0-SNAPSHOT.zip /ocsgw-1.0-SNAPSHOT.zip

RUN ls -la

RUN unzip ocsgw-1.0-SNAPSHOT.zip

CMD ["ocsgw-1.0-SNAPSHOT/bin/ocsgw"]