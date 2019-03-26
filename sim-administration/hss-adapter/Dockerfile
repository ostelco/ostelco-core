FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config/customer.graphqls /config/
COPY config/config.yaml /config/

COPY build/libs/hss-adapter-uber.jar /hss-adapter.jar

EXPOSE 9000
EXPOSE 8080
EXPOSE 8081


CMD ["/start.sh"]