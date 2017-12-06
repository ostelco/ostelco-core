# Prime - Micro Backend for Support System



## Deploy to production

### Package
 
    gradle clean pack

With unit testing:
    
    gradle clean test pack
    
* This creates zip file `build/deploy/prime.zip`

### Run

* Upload and unzip `prime.zip` file.


    scp build/deploy/prime.zip loltel@10.6.101.1:prime/
    ssh -A loltel@10.6.101.1
    scp prime/prime.zip ubuntu@192.168.0.123:.
    ssh ubuntu@192.168.0.123
    unzip prime.zip -d prime

* Run in docker


    cd prime
    sudo docker-compose up -d --build
    sudo docker logs -f prime



## Testing

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
  * Create test subscriber
 
  
     firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "noOfBytesLeft": 0}' database:push /authorative-user-storage

 * Top up test subscriber
 
 
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "sku": "DataTopup3GB", "paymentToken": "xxxx"}' database:push /client-requests

 * Start docker-compose for prime
 * Start docker-compose for ocs-gw 


    docker-compose up --build



## Interfaces

* Subscriber (End-user and/or CRM)
* OCS (Online Charging System) Gateway (towards Packet gateway (PGw))



## Use cases

### Subscriber API
* Perform Data top up
* Get available Data balance

### OCS API
* Fetch Data bucket


    PGw requests Data buckets (of approx 100 MB) for a subscriber.
    This request is made before its current bucket is about to be exhausted.
    This request has very high throughput and low latency requirement.
    Low latency is required because if the PGw does not receives 
    a response in time before the current bucket is expired, it disconnects/throttles
    data of the subscriber.

* Return Unused Data


    When subscriber disables its data connection, unused data from the prior requested 
    buckets is returned back. 

* Activate after Data top up


    After the last bucket for a subscriber is given using `Fetch Data bucket`, OCS
    denies more data, for which the PGw disconnects data for the subscriber.
    PGw does not asks for data bucket again for that subscriber until 
    subscriber disables/enables the data connection or until OCS notifies PGw
    toactivate that subscriber again.



## API

### OCS API

* Fetch Data bucket


    fetchDataBucket (String msisdn, long bytes, int ocsgwRequestId) => (String msisdn, long bytes, int ocsgwRequestId)

From PGW to OCS

* Return Unused Data


    returnUnusedData (String msisdn, int bytes) => (String msisdn)

From PGW to OCS

* Activate


    activate (String msisdn)

From OCS to PGW

## Event flow

| Message Type              | Producer   | Handler  | Next Handler                      |
| ---                       | ---        | ---      | ---                               |
| FETCH_DATA_BUCKET         | OcsService | OcsState | OcsService, _(maybe) Subscriber_  |
| RETURN_UNUSED_DATA_BUCKET | OcsService | OcsState | OcsService, Subscriber            |
| TOPUP_DATA_BUNDLE_BALANCE | Subscriber | OcsState | OcsService (activate), Subscriber |
| GET_DATA_BUNDLE_BALANCE   | Subscriber | OcsState | Subscriber                        |

     
