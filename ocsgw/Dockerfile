FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/start_dev.sh /start_dev.sh
COPY script/wait_including_esps.sh /wait_including_esps.sh
COPY script/wait.sh /wait.sh

COPY config/dictionary.xml /config/dictionary.xml
COPY config/logback.xml /config/logback.xml
COPY config/logback.dev.xml /config/logback.dev.xml
COPY config/logback.console.xml /config/logback.console.xml

COPY build/libs/ocsgw-uber.jar /ocsgw.jar

CMD ["/start.sh"]