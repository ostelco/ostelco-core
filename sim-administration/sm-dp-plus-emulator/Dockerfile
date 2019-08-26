FROM azul/zulu-openjdk:12.0.2

LABEL maintainer="dev@redotter.sg"

COPY script/start.sh /start.sh

COPY config /config

# This is bad form, but it's just to make it possible to run
# the emulator both in docker and from IDE using the same config.
RUN mkdir -p src/test/resources/fixtures
COPY src/test/resources/fixtures/sample-sim-batch-for-sm-dp+.csv  src/test/resources/fixtures/sample-sim-batch-for-sm-dp+.csv


COPY build/libs/sm-dp-plus-emulator-uber.jar /sm-dp-plus-emulator.jar

EXPOSE 8080

CMD ["/start.sh"]