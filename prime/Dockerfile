FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

COPY profiler_java_agent /opt/cprof

COPY script/start.sh /start.sh

COPY config/customer.graphqls /config/
COPY config/config.yaml /config/

COPY build/libs/prime-uber.jar /prime.jar

EXPOSE 7687
EXPOSE 8080
EXPOSE 8081
EXPOSE 8082
EXPOSE 8083

CMD ["/start.sh"]