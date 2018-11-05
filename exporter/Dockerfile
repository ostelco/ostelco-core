FROM ubuntu:18.04

LABEL maintainer="dev@redotter.sg"

RUN apt-get update && apt-get install -y --no-install-recommends \
        apt-utils \
		curl \
		uuid-runtime \
		lsb-release \
		ca-certificates \
		gnupg2 \
    && echo "deb http://packages.cloud.google.com/apt cloud-sdk-artful main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - \
    && apt-get update && apt-get install -y --no-install-recommends \
        google-cloud-sdk \
	&& rm -rf /var/lib/apt/lists/*

COPY script/idle.sh /idle.sh
COPY script/export_data.sh /export_data.sh
COPY script/delete_export_data.sh /delete_export_data.sh
COPY script/map_subscribers.sh /map_subscribers.sh
COPY script/subscriber-schema.json /subscriber-schema.json

RUN chmod +x /idle.sh
RUN chmod +x /export_data.sh
RUN chmod +x /delete_export_data.sh
RUN chmod +x /map_subscribers.sh

CMD ["/idle.sh"]