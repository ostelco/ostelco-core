FROM ubuntu:17.10

MAINTAINER CSI "csi@telenordigital.com"

RUN apt-get update && apt-get install -y --no-install-recommends \
		curl=7.55.1-1ubuntu2.4 \
		uuid-runtime=2.30.1-0ubuntu4.1 \
	&& rm -rf /var/lib/apt/lists/*

COPY script/idle.sh /idle.sh
COPY script/export_data.sh /export_data.sh

RUN chmod +x /idle.sh
RUN chmod +x /export_data.sh

CMD ["/idle.sh"]