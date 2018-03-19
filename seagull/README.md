# docker-seagull

A Dockerized version of [Seagull](http://gull.sourceforge.net/ "Seagull") - a multi-protocol traffic generator.

Based on https://github.com/codeghar/Seagull

The working directory is `/opt/seagull`.


Start it with `docker run --rm -it -p --net=host -v ./seagull/:/config -h ocs <seagull-img>` and then use the `diameter-env/run` scripts for testing.

For example:

`/config/logs# seagull -conf /config/config/conf.client.xml -dico /config/config/base_cc.xml -scen /config/scenario/ccr-cca.client.multiple-cc-units.init-term.xml -log /config/logs/log.log -llevel A`

