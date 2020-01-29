FROM golang:alpine as builder

##
##  THIS DOCKERFILE describes a container that provides gcloud and
##  terraform, and is used by the circleci server to mutate cluster
##  state.  The image is stored in eu.gcr.io/pi-ostelco-prod/terraform-gcloud:11.7
##  where the tag is the terraform version (currently 11.7).
##


ENV PATH $PATH:/usr/local/gcloud/google-cloud-sdk/bin:/go/src/github.com/hashicorp/terraform

RUN apk --no-cache add \
        curl \
        python \
        py-crcmod \
        bash \
        libc6-compat \
        openssh \
        openssh-client \
        git \
    && curl https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.tar.gz > /tmp/google-cloud-sdk.tar.gz \
    && mkdir -p /usr/local/gcloud \
    && tar -C /usr/local/gcloud -xvf /tmp/google-cloud-sdk.tar.gz \
    && /usr/local/gcloud/google-cloud-sdk/install.sh \
    && gcloud --version


# apk (the package manager for Alpine)
# does not have the latest version of terraform so therefore we
# build terraform from source.

ENV TERRAFORM_VERSION=0.11.13

ENV TF_DEV=true
ENV TF_RELEASE=true

WORKDIR /home
RUN curl -o /home/tf.zip https://releases.hashicorp.com/terraform/0.11.13/terraform_0.11.13_linux_amd64.zip && \
    unzip /home/tf.zip 
    
     
# WORKDIR $GOPATH/src/github.com/hashicorp/terraform
# RUN git clone https://github.com/hashicorp/terraform.git ./ && \
#     git checkout v${TERRAFORM_VERSION} && \
#     /bin/bash scripts/build.sh


FROM alpine

ENV PATH $PATH:/usr/local/gcloud/google-cloud-sdk/bin:/usr/local/terraform/bin
RUN apk --no-cache add python ca-certificates git

COPY --from=builder /home/terraform /usr/bin/terraform
COPY --from=builder /usr/local/gcloud /usr/local/gcloud


# Copy docker-entrypoint.sh to inject sensitive info into the credentials file from environment variables
COPY docker-entrypoint.sh /

ENTRYPOINT ["/docker-entrypoint.sh"]