FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config /config

COPY build/libs/auth-server-uber.jar /auth-server.jar

EXPOSE 8080
EXPOSE 8081

CMD ["/start.sh"]