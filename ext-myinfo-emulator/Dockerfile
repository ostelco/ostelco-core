FROM azul/zulu-openjdk:12.0.2

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config/config.yaml /config/config.yaml

COPY build/libs/ext-myinfo-emulator-uber.jar /ext-myinfo-emulator.jar

EXPOSE 8080

CMD ["/start.sh"]