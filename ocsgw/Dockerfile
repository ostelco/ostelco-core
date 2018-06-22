FROM openjdk:8u171

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/wait_for_esp_and_prime.sh /wait_for_esp_and_prime.sh
COPY config /config

COPY build/libs/ocsgw-uber.jar /ocsgw.jar

CMD ["/start.sh"]