FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY build/libs/ext-auth-provider-uber.jar /ext-auth-provider.jar

EXPOSE 8080

CMD ["/start.sh"]