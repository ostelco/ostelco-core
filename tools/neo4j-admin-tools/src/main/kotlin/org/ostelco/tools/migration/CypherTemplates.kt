package org.ostelco.tools.migration

import org.ostelco.prime.model.Customer


fun createSubscriber(subscriber: Customer) = """
CREATE(node:Subscriber {id:         '${subscriber.email}',
                        `email`: '${subscriber.email}',
                        `name`: '${subscriber.name}',
                        `address`: '${subscriber.address}',
                        `postCode`: '${subscriber.postCode}',
                        `city`:     '${subscriber.city}',
                        `country`: '${subscriber.country}'});
"""

fun createSubscription(msisdn: String) = """
CREATE (to:Subscription {id: '$msisdn'});
"""

fun addSubscriptionToSubscriber(email: String, msisdn: String) = """
MATCH (from:Subscriber {id: '$email'}), (to:Subscription {id: '$msisdn'})
CREATE (from)-[:HAS_SUBSCRIPTION]->(to);
"""

fun setBalance(msisdn: String, balance: Long) = """
MATCH (node:Subscription {id: '$msisdn'})
SET node.msisdn = '$msisdn'
SET node.balance = '$balance';
"""

fun addSubscriberToSegment(email: String) = """
MATCH (to:Subscriber)
  WHERE to.id IN ['$email']
WITH to
MATCH (from:Segment {id: 'all'})
CREATE (from)-[:segmentToSubscriber]->(to);
"""