FROM azul/zulu-openjdk:11.0.1-11.2

LABEL maintainer="dev@redotter.sg"

RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd && rm -rf /var/lib/apt/lists/*

# install gfuse to be able to fetch storage bucket for configuration
RUN apt-get update && apt-get install ca-certificates -y --no-install-recommends
RUN apt-get update && apt-get install curl -y --no-install-recommends
RUN echo "deb http://packages.cloud.google.com/apt gcsfuse-bionic main" | tee /etc/apt/sources.list.d/gcsfuse.list; \
  curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - && \
  apt-get update ; apt-get install -y apt-utils kmod && apt-get install -y gcsfuse

COPY script/start.sh /start.sh
COPY script/wait.sh /wait.sh

COPY build/libs/ocsgw-uber.jar /ocsgw.jar

CMD ["/start.sh"]