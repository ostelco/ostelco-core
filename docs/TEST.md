## Testing

### Setup

 * Configure firebase project - `pantel-2decb`
 
 * Save `pantel-prod.json` in all folders where this file is added in `.gitignore`.  You can find these directories by
   executing the command:

```bash
grep -i pantel $(find . -name '.gitignore') | awk -F: '{print $1}' | sort | uniq | sed 's/.gitignore//g'
```     
 
 * Create test subscriber with default balance by importing `docs/pantel-2decb_test.json` at `/test` path in Firebase.
 
 * Create self-signed certificate for nginx with domain as `ocs.ostelco.org` and place them at following location:
   * In `esp`, keep `nginx.key` and `nginx.cert`.
   * In `ocsgw/config`, keep `nginx.cert`.

```bash
cd certs/ocs.ostelco.org
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=ocs.ostelco.org'
cp nginx.crt ../../ocsgw/config/ocs.crt
```

```bash
cd certs/metrics.dev.endpoints.pantel-2decb.cloud.goog
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=metrics.dev.endpoints.pantel-2decb.cloud.goog'
cp nginx.crt ../../ocsgw/config/metrics.crt
```
   
### Test ext-pgw -- ocsgw -- prime --firebase

```bash
gradle clean build  
docker-compose up --build --abort-on-container-exit
```    

 * Integration tests for Prime

```bash
gradle prime:integration
``` 
 
 * Test pubsub -- pseudonymiser(--datastore) -- pubsub

```bash
docker-compose up --build -f docker-compose.yaml -f docker-compose.pseu.yaml --abort-on-container-exit
``` 

## Configuring emulators

 * Testing with Google Cloud PubSub Emulator
 
    * Install `gcloud` cli tool & basic components.
    * Install `pubsub beta` emulator using `gcloud`.
    * Init emulator and set ENV variable.
    * Start emulator.
    
```bash
gcloud components list
gcloud components install pubsub-emulator
gcloud beta emulators pubsub env-init
gcloud beta emulators pubsub start
```


