## Testing

### Setup

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
 * Save `pantel-prod.json` in all folders where this file is added in `.gitignore`.
 
 * Create test subscriber with default balance by importing `docs/pantel-2decb_test.json` 
   at `/test` path in Firebase.
  

### Test ext-pgw -- ocsgw -- prime --firebase

    
    gradle clean build  
    docker-compose up --build

**Note:** `ext-pgw` is planned to to transformed from unit-test module to simulator server.
During this transition phase, skip using `--abort-on-container-exit` with above `docker-compose` command.

    gradle prime:integration
 
 * Test pubsub -- pseudonymiser(--datastore) -- pubsub
 
    docker-compose up --build -f docker-compose.yaml -f docker-compose.pseu.yaml --abort-on-container-exit

## Configuring emulators

 * Testing with Google Cloud PubSub Emulator
 
    * Install `gcloud` cli tool & basic components.
    * Install `pubsub beta` emulator using `gcloud`.
    * Init emulator and set ENV variable.
    * Start emulator.
    
    
    gcloud components list
    gcloud components install pubsub-emulator
    gcloud beta emulators pubsub env-init
    gcloud beta emulators pubsub start


