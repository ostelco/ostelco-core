# docker-seagull

A Dockerized version of [Seagull](http://gull.sourceforge.net/ "Seagull") - a multi-protocol traffic generator.

Based on https://github.com/codeghar/Seagull

####To test with this Image

* Build Docker image

````docker build -t seagull .````

* Check your Local IP.

```
    ifconfig
```
  
  Update /config/conf.client with your Local IP

* Run Docker image

```
docker run --rm -it --net=host -v <path to dockerfile>/seagull/:/config -h ocs seagull
```

Inside the Seagull container the working directory is `/opt/seagull`.

and then use the `diameter-env/run` scripts for testing.

For example:

```/config/logs# seagull -conf /config/config/conf.client.xml -dico /config/config/base_cc.xml -scen /config/scenario/ccr-cca.client.multiple-cc-units.init-term.xml -log /config/logs/log.log -llevel A```

