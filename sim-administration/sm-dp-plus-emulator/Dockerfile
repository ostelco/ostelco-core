FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

RUN mkdir config
COPY config/sample-sim-batch-for-sm-dp+.csv /config
COPY config/acceptance-test-config.yaml /config/config.yaml
COPY config/sk_keys.jks /config

COPY build/libs/sm-dp-plus-emulator-uber.jar /sm-dp-plus-emulator.jar

EXPOSE 8080

CMD ["/start.sh"]