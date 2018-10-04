CREATE TABLE IF NOT EXISTS
`pantel-2decb.data_consumption.hourly_consumption`
(
  msisdn STRING NOT NULL,
  bytes INT64 NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  apn STRING NOT NULL,
  mccMnc STRING NOT NULL
)
PARTITION BY DATE(timestamp);


CREATE TABLE IF NOT EXISTS
`pantel-2decb.data_consumption.raw_consumption`
(
  msisdn STRING NOT NULL,
  bucketBytes INT64 NOT NULL,
  bundleBytes INT64 NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  apn STRING NOT NULL,
  mccMnc STRING NOT NULL
)
PARTITION BY DATE(timestamp);

CREATE TABLE IF NOT EXISTS
`pantel-2decb.data_consumption_dev.hourly_consumption`
(
  msisdn STRING NOT NULL,
  bytes INT64 NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  apn STRING NOT NULL,
  mccMnc STRING NOT NULL
)
PARTITION BY DATE(timestamp);


CREATE TABLE IF NOT EXISTS
`pantel-2decb.data_consumption_dev.raw_consumption`
(
  msisdn STRING NOT NULL,
  bucketBytes INT64 NOT NULL,
  bundleBytes INT64 NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  apn STRING NOT NULL,
  mccMnc STRING NOT NULL
)
PARTITION BY DATE(timestamp);