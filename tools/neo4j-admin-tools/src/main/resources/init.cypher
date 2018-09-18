// Create product
CREATE (:Product {`id`:                        '1GB_249NOK',
                  `presentation/isDefault`:    'true',
                  `presentation/offerLabel`:   'Default Offer',
                  `presentation/priceLabel`:   '249 NOK',
                  `presentation/productLabel`: '+1GB',
                  `price/amount`:              '24900',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '1_000_000_000',
                  `sku`:                       '1GB_249NOK'});

CREATE (:Product {`id`:                        '2GB_299NOK',
                  `presentation/offerLabel`:   'Monday Special',
                  `presentation/priceLabel`:   '299 NOK',
                  `presentation/productLabel`: '+2GB',
                  `price/amount`:              '29900',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '2_000_000_000',
                  `sku`:                       '2GB_299NOK'});

CREATE (:Product {`id`:                        '3GB_349NOK',
                  `presentation/offerLabel`:   'Monday Special',
                  `presentation/priceLabel`:   '349 NOK',
                  `presentation/productLabel`: '+3GB',
                  `price/amount`:              '34900',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '3_000_000_000',
                  `sku`:                       '3GB_349NOK'});

CREATE (:Product {`id`:                        '5GB_399NOK',
                  `presentation/offerLabel`:   'Weekend Special',
                  `presentation/priceLabel`:   '399 NOK',
                  `presentation/productLabel`: '+5GB',
                  `price/amount`:              '39900',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '5_000_000_000',
                  `sku`:                       '5GB_399NOK'});

CREATE (:Product {`id`:                        '100MB_FREE_ON_JOINING',
                  `presentation/priceLabel`:   'Free',
                  `presentation/productLabel`: '100MB Welcome Pack',
                  `price/amount`:              '0',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '100_000_000',
                  `sku`:                       '100MB_FREE_ON_JOINING'});

CREATE (:Product {`id`:                        '1GB_FREE_ON_REFERRED',
                  `presentation/priceLabel`:   'Free',
                  `presentation/productLabel`: '1GB Referral Pack',
                  `price/amount`:              '0',
                  `price/currency`:            'NOK',
                  `properties/noOfBytes`:      '1_000_000_000',
                  `sku`:                       '1GB_FREE_ON_REFERRED'});

CREATE (:Segment {`id`: 'all'});

CREATE (:Offer {`id`: 'default_offer'});

MATCH (n:Offer {id: 'default_offer'})
WITH n
MATCH (m:Product {id: '1GB_249NOK'})
CREATE (n)-[:OFFER_HAS_PRODUCT]->(m);

MATCH (n:Offer {id: 'default_offer'})
WITH n
MATCH (m:Product {id: '2GB_299NOK'})
CREATE (n)-[:OFFER_HAS_PRODUCT]->(m);

MATCH (n:Offer {id: 'default_offer'})
WITH n
MATCH (m:Product {id: '3GB_349NOK'})
CREATE (n)-[:OFFER_HAS_PRODUCT]->(m);

MATCH (n:Offer {id: 'default_offer'})
WITH n
MATCH (m:Product {id: '5GB_399NOK'})
CREATE (n)-[:OFFER_HAS_PRODUCT]->(m);

MATCH (n:Offer {id: 'default_offer'})
WITH n
MATCH (m:Segment {id: 'all'})
CREATE (n)-[:OFFERED_TO_SEGMENT]->(m);