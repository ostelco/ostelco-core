FROM ubuntu:15.04

LABEL maintainer="dev@redotter.sg"

RUN sed -i.bak -r 's/(archive|security).ubuntu.com/old-releases.ubuntu.com/g' /etc/apt/sources.list

RUN apt-get update \
 && apt-get install -y --no-install-recommends build-essential curl git libglib2.0-dev ksh bison flex vim tmux net-tools ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir -p root/opt/src
WORKDIR /root/opt/src
RUN git clone https://github.com/codeghar/Seagull.git ~/opt/src/seagull
WORKDIR /root/opt/src/seagull
RUN git branch build master
RUN git checkout build

WORKDIR /root/opt/src/seagull/seagull/trunk/src
RUN curl --create-dirs -o ~/opt/src/seagull/seagull/trunk/src/external-lib-src/sctplib-1.0.15.tar.gz http://www.sctp.de/download/sctplib-1.0.15.tar.gz &&\
  curl --create-dirs -o ~/opt/src/seagull/seagull/trunk/src/external-lib-src/socketapi-2.2.8.tar.gz http://www.sctp.de/download/socketapi-2.2.8.tar.gz
RUN curl --create-dirs -o ~/opt/src/seagull/seagull/trunk/src/external-lib-src/openssl-1.0.2e.tar.gz https://www.openssl.org/source/openssl-1.0.2e.tar.gz &&\
  ksh build-ext-lib.ksh
RUN ksh build.ksh -target clean &&\
  ksh build.ksh -target all
RUN cp /root/opt/src/seagull/seagull/trunk/src/bin/* /usr/local/bin
ENV LD_LIBRARY_PATH /usr/local/bin
RUN mkdir -p /opt/seagull &&\
  cp -r ~/opt/src/seagull/seagull/trunk/src/exe-env/* /opt/seagull
RUN [ "/bin/bash", "-c", "mkdir -p /opt/seagull/{diameter-env,h248-env,http-env,msrp-env,octcap-env,radius-env,sip-env,synchro-env,xcap-env}/logs" ]

WORKDIR /config/logs

