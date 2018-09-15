## Testing

### Setup

 * Configure firebase project - `pantel-2decb`
 
 * Save `pantel-prod.json` in all folders where this file is added in `.gitignore`.  You can find these directories by
   executing the command:

```bash
grep -i pantel $(find . -name '.gitignore') | awk -F: '{print $1}' | sort | uniq | sed 's/.gitignore//g'
```     
 
 * Create self-signed certificate for nginx with domain as `ocs.dev.ostelco.org` and place them at following location:
   * In `certs/ocs.dev.ostelco.org`, keep `nginx.key` and `nginx.cert`.
   * In `ocsgw/config`, keep `ocs.cert`.

```bash
cd certs/ocs.dev.ostelco.org
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=ocs.dev.ostelco.org'
cp nginx.crt ../../ocsgw/config/ocs.crt
```
 * Create self-signed certificate for nginx with domain as `metrics.dev.ostelco.org` and place them at following location:
    * In `certs/metrics.dev.ostelco.org`, keep `nginx.key` and `nginx.cert`.
    * In `ocsgw/config`, keep `metrics.cert`.

```bash
cd certs/metrics.dev.ostelco.org
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=metrics.dev.ostelco.org'
cp nginx.crt ../../ocsgw/config/metrics.crt
```

 * Set Stripe API key as env variable - `STRIPE_API_KEY`

   
### Test acceptance-tests

```bash
gradle clean build  
docker-compose up --build --abort-on-container-exit
```    
