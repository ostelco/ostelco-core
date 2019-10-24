// Start
MATCH (n) DETACH DELETE n;
MATCH (n) RETURN n;

// Segment-Offer-Product

CREATE (:Segment {id:"Segment"});

CREATE (:Offer {id:"Offer"});

MATCH (o:Offer), (s:Segment)
CREATE (o)-[:OFFERED_TO_SEGMENT]->(s);

CREATE (:Product {id:"Product"});

MATCH (o:Offer {id:"Offer"}), (p:Product {id:"Product"})
CREATE (o)-[:OFFER_HAS_PRODUCT]->(p);

// Region
CREATE (:Region {id:"Region"});



// Customer is created.

CREATE (:Identity {id:"Identity"});

CREATE (:Customer {id:"Customer"});

MATCH (i:Identity {id:"Identity"}), (c:Customer {id:"Customer"})
CREATE (i)-[:IDENTIFIES]->(c);

// Customer might be referred.

MATCH (c1:Customer {id:"Customer"}), (c2:Customer {id:"Customer"})
CREATE (c2)-[:REFERRED]->(c2);

// Customer gets assigned a Bundle.

CREATE (:Bundle {id:"Bundle"});

MATCH (c:Customer {id:"Customer"}), (b:Bundle {id:"Bundle"})
CREATE (c)-[:HAS_BUNDLE]->(b);


// Customer gets a welcome pack.

CREATE (:PurchaseRecord {id:"PurchaseRecord"});

MATCH (c:Customer {id:"Customer"}), (p:PurchaseRecord {id:"PurchaseRecord"})
CREATE (c)<-[:FOR_PURCHASE_BY]-(p);

MATCH (pr:PurchaseRecord {id:"PurchaseRecord"}), (p:Product {id:"Product"})
CREATE (pr)-[:FOR_PURCHASE_OF]->(p);

// Customer performs eKYC

CREATE (:ScanInformation {id:"ScanInformation"});

MATCH (c:Customer {id:"Customer"}), (s:ScanInformation {id:"ScanInformation"})
CREATE (c)-[:EKYC_SCAN]->(s);


// Customer is approved for a Region
MATCH (c:Customer {id:"Customer"}), (r:Region {id:"Region"})
CREATE (c)-[:BELONG_TO_REGION]->(r);

// Customer gets a SimProfile for a Region
CREATE (:SimProfile {id:"SimProfile"});

MATCH (c:Customer {id:"Customer"}), (s:SimProfile {id:"SimProfile"})
CREATE (c)-[:HAS_SIM_PROFILE]->(s);

MATCH (s:SimProfile {id:"SimProfile"}), (r:Region {id:"Region"})
CREATE (s)-[:SIM_PROFILE_FOR_REGION]->(r);

// SimProfile has a Subscription.
CREATE (:Subscription {id:"Subscription"});

MATCH (sn:Subscription {id:"Subscription"}), (sp:SimProfile {id:"SimProfile"})
CREATE (sn)-[:SUBSCRIPTION_UNDER_SIM_PROFILE]->(sp);

MATCH (c:Customer {id:"Customer"}), (s:Subscription {id:"Subscription"})
CREATE (c)-[:HAS_SUBSCRIPTION]->(s);


MATCH (s:Subscription {id:"Subscription"}), (b:Bundle {id:"Bundle"})
CREATE (s)-[:LINKED_TO_BUNDLE]->(b);


// Plan

CREATE (:Plan {id:"Plan"});

MATCH (c:Customer {id:"Customer"}), (p:Plan {id:"Plan"})
CREATE (c)-[:SUBSCRIBES_TO_PLAN]->(p);

// Customer is put in a segment.
MATCH (c:Customer {id:"Customer"}), (s:Segment {id:"Segment"})
CREATE (c)-[:BELONG_TO_SEGMENT]->(s);









