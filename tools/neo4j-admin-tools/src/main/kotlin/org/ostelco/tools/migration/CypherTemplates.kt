package org.ostelco.tools.migration

import org.ostelco.prime.model.Customer


fun createSubscriber(customer: Customer) = """
CREATE(node:Subscriber {id: '${customer.id}',
                        `nickname`: '${customer.nickname}'
                        `contactEmail`: '${customer.contactEmail}',
                        `analyticsId`: '${customer.analyticsId}',
                        `referralId`: '${customer.referralId}'});
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

fun addSubscriberToSegment(id: String) = """
MATCH (to:Subscriber)
  WHERE to.id IN ['$id']
WITH to
MATCH (from:Segment {id: 'all'})
CREATE (from)-[:segmentToSubscriber]->(to);
"""