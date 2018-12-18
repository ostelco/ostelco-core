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

The backend is using Redis for storage of session information. 
The Redis backend is set in the environment variables :  

* REDIS_HOST : for the host. ( Default : localhost )  
* REDIS_PORT : for port. ( Default : 6379 )

Testing
=======

There is a jUnit test for this module in [OCSgw](../ocsgw/src/test/java/org/ostelco/ocsgw/OcsHATest.java).  
This test requires Redis to be running.  
To run Redis locally for this test you can use the Redis docker container:

```
  docker run -d --name redis-test -p 6379:6379  -v <path>/ostelco-core/diameter-ha/config/redis.conf:/redis.conf redis redis-server /redis.conf
```