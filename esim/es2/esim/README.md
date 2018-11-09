# es2plus

How to start the es2plus application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/esim-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`

TODO
---

1.  Make all the rudimentary tests work, and also develop the json schemas for both requests and responses
1.  Make some recactoring in the data structure. As it is today it is simply a mess.   
2.  See if it's possible to find an example of a valid SM-DP2 interaction somewhere
