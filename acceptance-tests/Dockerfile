FROM openjdk:8u171

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat \
 && rm -rf /var/lib/apt/lists/*

COPY src/main/resources/ /
COPY script/wait.sh /wait.sh
COPY build/libs/acceptance-tests-uber.jar /acceptance-tests.jar

CMD ["/wait.sh"]