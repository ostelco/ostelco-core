FROM openjdk:8u171

MAINTAINER CSI "csi@telenordigital.com"

COPY script/start.sh /start.sh
COPY config/config.yaml /config/config.yaml

COPY build/libs/bq-metrics-extractor-uber.jar /bq-metrics-extractor.jar

CMD ["/start.sh"]