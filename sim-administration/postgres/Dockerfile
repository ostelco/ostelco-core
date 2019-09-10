FROM  postgres:11-alpine

ENV POSTGRES_USER postgres_user
ENV POSTGRES_PASSWORD postgres_password
ENV POSTGRES_DB sim-inventory

COPY init.sql /docker-entrypoint-initdb.d/


