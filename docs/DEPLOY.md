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


    cd ostelco-core
    sudo docker-compose up -d --build
    
    sudo docker-compose logs -f
    
    sudo docker logs -f prime
    sudo docker logs -f ocsgw
    sudo docker logs -f auth-server