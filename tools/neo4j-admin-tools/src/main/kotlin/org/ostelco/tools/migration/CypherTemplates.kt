package org.ostelco.tools.migration

import org.ostelco.prime.model.Customer


fun createSubscriber(customer: Customer) = """
CREATE(node:Subscriber {id: '${customer.email}',
                        `name`: '${customer.name}'
                        `email`: '${customer.email}',
                        `analyticsId`: '${customer.analyticsId}'});
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