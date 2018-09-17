# Pseudonym Server

SQL for joining dataconsumption and pseudonyms table

    SELECT
      hc.bytes, ps.msisdnid, hc.timestamp
    FROM
      [pantel-2decb:data_consumption.hourly_consumption] as hc
    JOIN
      [pantel-2decb:exported_pseudonyms.3ebcdc4a7ecc4cd385e82087e49b7b7b] as ps
    ON  ps.msisdn = hc.msisdn

Login to eu.gcr.io for pushing images

    docker login -u oauth2accesstoken -p "$(gcloud auth print-access-token)" https://eu.gcr.io

