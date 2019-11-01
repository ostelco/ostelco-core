# docker-seagull

A Dockerized version of [Seagull](http://gull.sourceforge.net/ "Seagull") - a multi-protocol traffic generator.

Based on https://github.com/codeghar/Seagull

#### Build image

````
docker build -t seagull .
````


####To test with this Image with docker-compose

* Use the docker-compose file for seagull to start Prime and OCS-gw

````docker-compose -f docker-compose.seagull.yaml up --build````

* Run Docker image

```
docker run --rm -it --network="ostelco-core_net" --ip="172.16.238.2" -v <path to dockerfile>/seagull/:/config -h ocs seagull
```

Testing can then be done with the command:

```seagull -conf /config/config/conf.client.xml -dico /config/config/base_cc.xml -scen /config/scenario/ccr-cca.client.multiple-cc-units.init.xml -log /config/logs/log.log -llevel N```

Tuning of seagull is done in the configuration file
``` /config/conf.client.xml ```


####To test without docker-compose

**Start Seagull**

Check your local IP.

Update /config/conf.client with your local IP

```
docker run --rm -it --net=host -v ./seagull/:/config -h ocs seagull
```

**Start OCS-gw**

Update IPAddress for your LocalPeer in /src/resources/server-jdiameter-config.xml with your local IP

Start OCS-gw

**Run test**

In Seagull:

```
cd /config/logs

seagull -conf /config/config/conf.client.xml -dico /config/config/base_cc.xml -scen /config/scenario/ccr-cca.client.multiple-cc-units.init.xml -log /config/logs/log.log -llevel N
```
