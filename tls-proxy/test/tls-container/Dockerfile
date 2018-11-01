FROM alpine:latest
MAINTAINER CSI csi@telenordigital.com

RUN apk update && apk add socat

RUN apk add openssl

RUN apk add --update tcpdump

# Copy start script
COPY script/start.sh /start.sh
RUN chmod +x /start.sh

# Copy certificates
COPY certs/nginx.crt /etc/cert/ssl.crt
COPY certs/nginx.key /etc/cert/ssl.key

# Create pem file
RUN cat /etc/cert/ssl.key /etc/cert/ssl.crt >/etc/cert/ssl.pem
RUN chmod 600 /etc/cert/ssl.key /etc/cert/ssl.pem

ENTRYPOINT ["sh","/start.sh"]