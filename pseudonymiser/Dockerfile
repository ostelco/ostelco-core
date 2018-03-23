FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update && apt-get install -y --no-install-recommends \
		socat \
	&& rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/wait_for_dsemulator.sh /wait_for_dsemulator.sh

COPY config /config

COPY build/libs/pseudonymiser-uber.jar /pseudonymiser.jar

EXPOSE 8080
EXPOSE 8081

CMD ["/start.sh"]