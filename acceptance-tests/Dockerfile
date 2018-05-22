FROM openjdk:8u171

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat \
 && rm -rf /var/lib/apt/lists/*

COPY script/wait_for_prime.sh /wait_for_prime.sh

COPY build/libs/acceptance-tests-uber.jar /acceptance-tests.jar

CMD ["/wait_for_prime.sh"]