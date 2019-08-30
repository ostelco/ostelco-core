FROM golang:latest 
RUN mkdir /app
COPY "upload-sim-batch.go" "/app"
WORKDIR "/app"
RUN go build -o upload-sim-batch . 

# We start by sending in all of the profiles as pre-provisioned,
# when we've proven that we can handle that, we'll move on to 
# greater things.


## A lot of trhis can, and should, be moved  into docker compose,
##  ... see https://stackoverflow.com/questions/30063907/using-docker-compose-how-to-execute-multiple-commands/30064175#30064175

CMD ["/app/upload-sim-batch",      "-first-iccid", "890100000000000000", "-last-iccid", "890100000000000099"] #,  "-first-imsi", "310150000000000",   "-last-imsi", "310150000000099",  "-first-msisdn", "4790900000",  "-last-msisdn", "4790900099 ,  "-profile-type", "IPHONE_PROFILE_2", "-upload-hostname", "prime", "-upload-portnumber", "8080" ]




#        "-last-iccid", "8901000000000000993",  \
#        "-first-imsi", "3101500000000000",  \
#        "-last-imsi", "3101500000000099",  \
#        "-first-msisdn", "4790900700",  \
#        "-last-msisdn", "4790900000",  \
#        "-profile-type", "IPHONE_PROFILE_2",  \
#        "-profile-vendor", "Bar",  \
#        "-upload-hostname", "prime",  \
#        "-upload-portnubmer", "8080 ]

