## Testing

### Setup

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
 * Save `pantel-prod.json` in all folders where this file is added in `.gitignore`.  You can find these directories by
   executing the command:
     
       grep -i pantel $(find . -name '.gitignore') | awk -F: '{print $1}' | sort | uniq | sed 's/.gitignore//g'
 
 * Create test subscriber
  
  
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "noOfBytesLeft": 0}' database:set /authorative-user-balance/4747900184

 * Top up test subscriber
 

    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "sku": "DataTopup3GB", "paymentToken": "xxxx"}' database:push /client-requests

### Test ext-pgw -- ocsgw -- prime --firebase
    
    gradle clean build  
    docker-compose up --build --abort-on-container-exit
    
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


