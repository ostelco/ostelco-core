## Testing

### Setup

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
 * Save `pantel-prod.json` in all folders where this file is added in `.gitignore`.
 
 * Create test subscriber
  
  
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "noOfBytesLeft": 0}' database:push /authorative-user-storage

 * Top up test subscriber
 
 
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "sku": "DataTopup3GB", "paymentToken": "xxxx"}' database:push /client-requests

### Start docker-compose

 * Test ext-pgw -- ocsgw -- prime --firebase
    
    gradle clean build  
    docker-compose up --build --abort-on-container-exit
 
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


