FROM openjdk:11

MAINTAINER CSI "csi@telenordigital.com"

COPY script/start.sh /start.sh

COPY build/libs/ext-auth-provider-uber.jar /ext-auth-provider.jar

EXPOSE 8080

CMD ["/start.sh"]