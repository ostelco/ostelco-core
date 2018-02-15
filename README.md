# ostelco-core

[![Build Status](https://travis-ci.org/ostelco/ostelco-core.svg?branch=master)](https://travis-ci.org/ostelco/ostelco-core) [![codebeat badge](https://codebeat.co/badges/e4c26ba7-75d6-48d2-a3d0-f72988998642)](https://codebeat.co/projects/github-com-ostelco-ostelco-core-master)  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/e7b2ae0440104a5e8ae6fa5e919147dc)](https://www.codacy.com/app/la3lma/ostelco-core?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ostelco/ostelco-core&amp;utm_campaign=Badge_Grade)[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/e7b2ae0440104a5e8ae6fa5e919147dc)](https://www.codacy.com/app/la3lma/ostelco-core?utm_source=github.com&utm_medium=referral&utm_content=ostelco/ostelco-core&utm_campaign=Badge_Coverage)


Core protocols and services

## Deploy to production

### Package
 
    gradle clean pack

With unit testing:
    
    gradle clean test pack
    
* This creates zip file `build/deploy/ostelco-core.zip`

### Run

* Upload and unzip `ostelco-core.zip` file.


    scp build/deploy/ostelco-core.zip loltel@10.6.101.1:ostelco-core/  
    ssh -A loltel@10.6.101.1  
    scp ostelco-core/ostelco-core.zip ubuntu@192.168.0.123:.  
    ssh ubuntu@192.168.0.123  
    unzip ostelco-core.zip -d ostelco-core  

* Run in docker


    cd prime  
    sudo docker-compose -f docker-compose.yaml -f docker-prod.yaml up -d --build  
    sudo docker logs -f prime  
    sudo docker logs -f ocsgw  

## Testing

 * Configure firebase project - `pantel-tests` or `pantel-2decb`
 
  * Create test subscriber
  
  
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "noOfBytesLeft": 0}' database:push /authorative-user-storage

 * Top up test subscriber
 
 
    firebase --project pantel-2decb  --data '{"msisdn": "+4747900184", "sku": "DataTopup3GB", "paymentToken": "xxxx"}' database:push /client-requests

 * Start docker-compose

    
    gradle build  
    docker-compose up --build  
