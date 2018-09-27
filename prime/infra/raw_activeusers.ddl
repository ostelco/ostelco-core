# Table for dev cluster
CREATE TABLE ocs_gateway_dev.raw_activeusers
 (
   timestamp TIMESTAMP NOT NULL,
   users ARRAY< STRUCT<
     msisdn STRING NOT NULL,
     apn STRING NOT NULL,
     mccMnc STRING NOT NULL
   > >
)
PARTITION BY DATE(_PARTITIONTIME)

# Table for production cluster
CREATE TABLE ocs_gateway.raw_activeusers
 (
   timestamp TIMESTAMP NOT NULL,
   users ARRAY< STRUCT<
     msisdn STRING NOT NULL,
     apn STRING NOT NULL,
     mccMnc STRING NOT NULL
   > >
)
PARTITION BY DATE(_PARTITIONTIME)