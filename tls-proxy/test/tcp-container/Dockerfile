FROM alpine:latest
RUN apk add --update netcat-openbsd && rm -rf /var/cache/apk/*

#RUN apk add --update tcpdump

EXPOSE 3868

# Copy start script
COPY script/start.sh /start.sh
RUN chmod +x /start.sh

ENTRYPOINT ["sh","/start.sh"]