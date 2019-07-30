FROM azul/zulu-openjdk:12.0.2

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config /config

COPY build/libs/sm-dp-plus-emulator-uber.jar /sm-dp-plus-emulator.jar

EXPOSE 8080

CMD ["/start.sh"]