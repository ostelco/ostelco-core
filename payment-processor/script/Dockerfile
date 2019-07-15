FROM alpine:3.7.4-alpine3.10

LABEL maintainer="dev@redotter.sg"

COPY script/monitor.py /monitor.py
RUN chmod +x /monitor.py
COPY script/subscribed-to-events.json /subscribed-to-events.json
