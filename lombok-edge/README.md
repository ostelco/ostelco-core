
# Lombok woes

I had some issues with Lombok on Intellij. It stopped working.  The trick to use was to
install a new edge-release of lombok (see below), and to reinstall the lombok
plugin for intellij, and then of course reboot the IDE.

I'm including the jar and this description just in case I don't want to spend two hours
once more to repeat the procedure.  When the stock version of  lombok works well with
intellij this director should be removed.

To install the downloaded jar into maven's artifact storage:

    mvn install:install-file -Dfile=lombok-edge.jar -DgroupId=org.projectlombok -DartifactId=lombok -Dversion=1.16.19 -Dpackaging=jar
