FROM azul/zulu-openjdk:12.0.1

LABEL maintainer="dev@redotter.sg"

RUN apt-get update \
 && apt-get install -y --no-install-recommends netcat \
 && rm -rf /var/lib/apt/lists/*

COPY config/ /secret/
COPY src/main/resources/ /
COPY script/wait.sh /wait.sh
COPY build/libs/acceptance-tests-uber.jar /acceptance-tests.jar

CMD ["/wait.sh"]