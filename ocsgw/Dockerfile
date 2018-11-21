FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY script/start.sh /start.sh
COPY script/wait.sh /wait.sh

COPY config/dictionary.xml /config/dictionary.xml

COPY build/libs/ocsgw-uber.jar /ocsgw.jar

ENTRYPOINT ["/start.sh"]