FROM  postgres:11-alpine

ENV POSTGRES_USER postgres_user
ENV POSTGRES_PASSWORD postgres_password
ENV POSTGRES_DB sim-inventory

# Note, as files are applied in alphabetic order care should be
# made to make sure that the 'init.sql' gets applied first.
COPY init.sql /docker-entrypoint-initdb.d/
COPY setup-for-acceptance-test.sql /docker-entrypoint-initdb.d/
