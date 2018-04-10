FROM openjdk:8u151

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update && apt-get install -y --no-install-recommends \
		socat=1.7.3.1-2+deb9u1 \
	&& rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/wait_for_emulators.sh /wait_for_emulators.sh

COPY config /config

COPY build/libs/exporter-uber.jar /exporter.jar

EXPOSE 8080
EXPOSE 8081

CMD ["/start.sh"]