CREATE TABLE purchases.raw_purchases
(
    id STRING NOT NULL,
    subscriberId STRING NOT NULL,
    timestamp INT64 NOT NULL,
    status STRING NOT NULL,
    product STRUCT<
     sku STRING NOT NULL,
     price STRUCT<
       amount INT64 NOT NULL,
       currency STRING NOT NULL
     > NOT NULL,
     properties ARRAY< STRUCT<
       key STRING NOT NULL,
       value STRING NOT NULL
     > >,
     presentation ARRAY< STRUCT<
       key STRING NOT NULL,
       value STRING NOT NULL
     > >
    > NOT NULL,
    refund STRUCT<
      id STRING NOT NULL,
      reason STRING NOT NULL,
      timestamp INT64 NOT NULL
    >
)
PARTITION BY DATE(_PARTITIONTIME)

CREATE TABLE purchases_dev.raw_purchases
(
    id STRING NOT NULL,
    subscriberId STRING NOT NULL,
    timestamp INT64 NOT NULL,
    status STRING NOT NULL,
    product STRUCT<
     sku STRING NOT NULL,
     price STRUCT<
       amount INT64 NOT NULL,
       currency STRING NOT NULL
     > NOT NULL,
     properties ARRAY< STRUCT<
       key STRING NOT NULL,
       value STRING NOT NULL
     > >,
     presentation ARRAY< STRUCT<
       key STRING NOT NULL,
       value STRING NOT NULL
     > >
    > NOT NULL,
    refund STRUCT<
      id STRING NOT NULL,
      reason STRING NOT NULL,
      timestamp INT64 NOT NULL
    >
)
PARTITION BY DATE(_PARTITIONTIME)