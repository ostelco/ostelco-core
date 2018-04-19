FROM ubuntu:17.10

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update && apt-get install -y --no-install-recommends \
		curl=7.55.1-1ubuntu2.4 \
		uuid-runtime=2.30.1-0ubuntu4.1 \
		lsb-release=9.20160110ubuntu5 \
		ca-certificates=20170717 \
    && echo "deb http://packages.cloud.google.com/apt cloud-sdk-artful main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - \
    && apt-get update && apt-get install -y --no-install-recommends \
        google-cloud-sdk=198.0.0-0 \
	&& rm -rf /var/lib/apt/lists/*

COPY script/idle.sh /idle.sh
COPY script/export_data.sh /export_data.sh

RUN chmod +x /idle.sh
RUN chmod +x /export_data.sh

CMD ["/idle.sh"]