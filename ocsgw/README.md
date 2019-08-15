
About the project
=================

This project makes it possible to connect the Gy interface from a GGSN/P-GW to this OCS gateway.
The gateway will parse the Diameter traffic and pass it through to the OCS component.
Currently it supports a Local, PubSub, gRPC or Proxy datasource as the connection from the gateway to the OCS component.

The **LocalDatasource** will accept all Credit-Control-Requests and send a Credit-Control-Answer that grant
any service units requested.

The **GrpcDatasource** will translate the Credit-Control-Request to protobuf and forward this to the OCS server.

The ProxyDatasource is a combination of the Local and any of the other DataSource that will forward all traffic to OCS using the
gRPC datasource but also use the Local datasource to get low latency.

The project is built on RestComm jDiameter Stack.

For diameter HA setup please see : [diameter-ha](../diameter-ha/README.md)

Build
===============
```
./gradlew build
```

Run
===============
```
./gradlew run
```

Test
===============
```
./gradlew test
```

Deploy to GCP
===============
Please see the script for usage
```
./ocsgw/infra/script/deploy-ocsgw.sh
```


Docker
===============

**Build**
```
docker build -t ocsgw .
```

**Run**
```
docker run --rm --name ocsgw -p 3868:3868 ocsgw
```


Testing
=====================

Seagull can be used for load testing. Please see : [seagull](../seagull/README.md)
 
**Build Seagull docker image:**

 in ./testsuite/seagull/docker
 
```
docker build -t seagull .
```

**Start Seagull**

Check your local IP.

Update /config/conf.client with your local IP`
```
docker run --rm -it --net=host -v ./seagull/:/config -h ocs seagull
```

**Start OCSgw**

Update IPAddress for your LocalPeer in /src/resources/server-jdiameter-config.xml with your local IP

Start OCSgw

**Run test**

In Seagull:
```
cd /config/logs

seagull -conf /config/config/conf.client.xml -dico /config/config/base_cc.xml -scen /config/scenario/ccr-cca.client.multiple-cc-units.init.xml -log /config/logs/log.log -llevel N
``
