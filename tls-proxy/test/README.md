The Docker container will use netcat to listen for TCP connections.
Start it with 
``` 
docker build -t ostelco/tcptest .
docker run --rm -p 3869:3868 ostelco/tcptest
```

Then I can test a tcp connection with
```
nc 0.0.0.0 3869
```

Plan is then to use a socat client to setup a tls connection through a NGINX reverse proxy and see the traffic forwarded
to the tcptest container.