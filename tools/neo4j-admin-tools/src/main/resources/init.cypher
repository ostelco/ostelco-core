// Create product
CREATE(node:Product {id:                          '1GB_249NOK',
                     `sku`:                       '1GB_249NOK',
                     `price/amount`:              '24900',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '1_000_000_000',
                     `presentation/isDefault`:    'true',
                     `presentation/offerLabel`:   'Default Offer',
                     `presentation/priceLabel`:   '249 NOK',
                     `presentation/productLabel`: '+1GB'});

CREATE(node:Product {id:                          '2GB_299NOK',
                     `sku`:                       '2GB_299NOK',
                     `price/amount`:              '29900',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '2_000_000_000',
                     `presentation/offerLabel`:   'Monday Special',
                     `presentation/priceLabel`:   '299 NOK',
                     `presentation/productLabel`: '+2GB'});

CREATE(node:Product {id:                          '3GB_349NOK',
                     `sku`:                       '3GB_349NOK',
                     `price/amount`:              '34900',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '3_000_000_000',
                     `presentation/offerLabel`:   'Monday Special',
                     `presentation/priceLabel`:   '349 NOK',
                     `presentation/productLabel`: '+3GB'});

CREATE(node:Product {id:                          '5GB_399NOK',
                     `sku`:                       '5GB_399NOK',
                     `price/amount`:              '39900',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '5_000_000_000',
                     `presentation/offerLabel`:   'Weekend Special',
                     `presentation/priceLabel`:   '399 NOK',
                     `presentation/productLabel`: '+5GB'});

CREATE(node:Product {id:                          '100MB_FREE_ON_JOINING',
                     `sku`:                       '100MB_FREE_ON_JOINING',
                     `price/amount`:              '0',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '100_000_000',
                     `presentation/priceLabel`:   'Free',
                     `presentation/productLabel`: '100MB Welcome Pack'});

CREATE(node:Product {id:                          '1GB_FREE_ON_REFERRED',
                     `sku`:                       '1GB_FREE_ON_REFERRED',
                     `price/amount`:              '0',
                     `price/currency`:            'NOK',
                     `properties/noOfBytes`:      '1_000_000_000',
                     `presentation/priceLabel`:   'Free',
                     `presentation/productLabel`: '1GB Referral Pack'});


// Create Segment
CREATE (node:Segment {id: 'all'});

// Create Offer
CREATE (node:Offer {id: 'default_offer'});

// Add Segment to Offer
MATCH (to:Segment)
  WHERE to.id IN ['all']
WITH to
MATCH (from:Offer {id: 'default_offer'})
CREATE (from)-[:offerHasSegment]->(to);

// Add Product to Offer
MATCH (to:Product)
  WHERE to.id IN ['1GB_249NOK', '2GB_299NOK', '3GB_349NOK', '5GB_399NOK']
WITH to
MATCH (from:Offer {id: 'default_offer'})
CREATE (from)-[:offerHasProduct]->(to);