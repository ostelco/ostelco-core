## Testing

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
 * Create test subscriber
  
  
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "noOfBytesLeft": 0}' database:push /authorative-user-storage

 * Top up test subscriber
 
 
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "sku": "DataTopup3GB", "paymentToken": "xxxx"}' database:push /client-requests

 * Start docker-compose

    
    gradle clean build  
    docker-compose up --build --abort-on-container-exit

 * Testing with Google Cloud PubSub Emulator
 
    * Install `gcloud` cli tool & basic components.
    * Install `pubsub beta` emulator using `gcloud`.
    * Init emulator and set ENV variable.
    * Start emulator.
    
    
    gcloud components list
    gcloud components install pubsub-emulator
    gcloud beta emulators pubsub env-init
    gcloud beta emulators pubsub start


