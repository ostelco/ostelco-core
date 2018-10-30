## SSL-Proxy for OCS-gw

This will create a NGINX reverse proxy for using TLS on the OCS-gw


### Setup

Create self sign certificates for NGINX and copy them to the correct folder

```bash
cd certs/ocsgw.dev.ostelco.org
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=ocsgw.dev.ostelco.org'
cp nginx.* ../../ssl-proxy/certs/
```

#### Test

docker-compose up --build
