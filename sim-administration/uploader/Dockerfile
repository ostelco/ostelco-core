FROM golang:latest 
RUN mkdir /app
COPY "upload-sim-batch.go" "/app"
WORKDIR "/app"
RUN go build -o upload-sim-batch . 

# We start by sending in all of the profiles as pre-provisioned,
# when we've proven that we can handle that, we'll move on to 
# greater things.

## XXX A convoluted way of making a script to fill the
##     prime with sim data  prior to acceptance testing.


CMD ["/app/upload-sim-batch",      "-first-iccid", "890100000000000000", "-last-iccid", "890100000000000099",  "-first-imsi", "310150000000000",   "-last-imsi", "310150000000099"   ,  "-first-msisdn", "4790900700",  "-last-msisdn", "4790900799" ,  "-profile-type", "IPHONE_PROFILE_2", "-upload-hostname", "prime", "-upload-portnumber", "8080"  ]



