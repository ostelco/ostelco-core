FROM azul/zulu-openjdk:13

LABEL maintainer="dev@redotter.sg"

#
# Copy the files we need
#

COPY script/start.sh /start.sh
COPY config/config.yaml /config/config.yaml
COPY build/libs/scaninfo-shredder-uber.jar /scaninfo-shredder.jar

#
# Load, then dump the standard java classes into the
# image being built, to speed up java load time
# using Class Data Sharing. The "quit" command will
# simply quit the program after it's dumped the list of
# classes that should be cached.
#

RUN ["java", "-Dfile.encoding=UTF-8", "-Xshare:on", "-Xshare:dump", "-jar", "/scaninfo-shredder.jar", "quit", "config/config.yaml"]


#
# Finally the actual entry point
#

CMD ["/start.sh"]
