jDiameter HA module
===================

This is an implementation of the jDiameter ISessionDatasource and ITimerFacility to be used for HA setup od the jDiameter
stack. To enable this you need to enable the extension in the jDiameter configuration file.

```
</Configuration>

   ...

  <Extensions>
    <SessionDatasource value="org.ostelco.diameter.ha.sessiondatasource.RedisReplicatedSessionDatasource" />
    <TimerFacility value="org.ostelco.diameter.ha.timer.ReplicatedTimerFacilityImpl" />
  </Extensions>
  
</Configuration>
```

RedisReplicatedSessionDatasource
================================

This SessionDatasource is using Redis for storage of session information. 
The Redis backend is set in the environment variables :  

* REDIS_HOSTNAME : for the host. ( Default : localhost )  
* REDIS_PORT : for port. ( Default : 6379 )


It uses Redis for ICCASessionData, all other session data will use the LocalSessionData without remote storage.
This is because our OCS only use the Diameter Credit-Control-Application (CCA)

ReplicatedTimerFacilityImpl
===========================

For the Diameter Credit-Control-Application server there is mainly one timer that is set for a session. This is the Tcc timer.

The use of the Tcc Timer is defined in rfc4006 :

```
The supervision session timer Tcc is used in the credit-control server to supervise the credit-control session.

...
   
Session supervision timer Tcc expired - Release reserved units

...
the session supervision timer Tcc MAY be set to two times the value of
the Validity-Time.  Since credit-control
update requests are also produced at the expiry of granted service
units and/or for mid-session service events, the omission of
Validity-Time does not mean that intermediate interrogation for the
purpose of credit-control is not performed.

```

jDiameter requires us to be able to store this timer and in the original implementation this was shared in the cluster.
This replication of the timer is not implemented here, as we are just running the server there is no message to send to the
client when it times out. We should make sure to release reserved units though.


Testing
=======

There is a jUnit test for this module in [OCSgw](../ocsgw/src/test/java/org/ostelco/ocsgw/OcsHATest.java).  
This test requires Redis to be running.  
To run Redis locally for this test you can use the Redis docker container:

```
  docker run -d --name redis-test -p 6379:6379  -v <path>/ostelco-core/diameter-ha/config/redis.conf:/redis.conf redis redis-server /redis.conf
```
To browse Redis one can use the redis-browser

```
  docker run --rm -ti -p 5001:5001 --link redis-test:redis-test marian/rebrow
```