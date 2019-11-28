#! /usr/bin/env bash

# Updates list of 'subscribed to' events with Stripe.
#
# Note:
#  - Remember to update monitoring cronjob 'stripe-monitor-task.yaml'
#    on changes.

get_webhook_id() {
    curl -sS -u $STRIPE_API_KEY: https://api.stripe.com/v1/webhook_endpoints?limit=1 | \
        jq .data[].id |\
        tr -d '"'
}

update_event_list() {
    local id=$1
    curl -sS -u $STRIPE_API_KEY: https://api.stripe.com/v1/webhook_endpoints/$id \
         -d enabled_events[]="balance.available" \
         -d enabled_events[]="charge.failed" \
         -d enabled_events[]="charge.refunded" \
         -d enabled_events[]="charge.succeeded" \
         -d enabled_events[]="charge.dispute.created" \
         -d enabled_events[]="customer.created" \
         -d enabled_events[]="customer.deleted" \
         -d enabled_events[]="customer.subscription.created" \
         -d enabled_events[]="customer.subscription.deleted" \
         -d enabled_events[]="customer.subscription.updated" \
         -d enabled_events[]="invoice.created" \
         -d enabled_events[]="invoice.deleted" \
         -d enabled_events[]="invoice.finalized" \
         -d enabled_events[]="invoice.payment_action_required" \
         -d enabled_events[]="invoice.payment_failed" \
         -d enabled_events[]="invoice.payment_succeeded" \
         -d enabled_events[]="invoice.sent" \
         -d enabled_events[]="invoice.upcoming" \
         -d enabled_events[]="invoice.updated" \
         -d enabled_events[]="invoice.voided" \
         -d enabled_events[]="payment_intent.created" \
         -d enabled_events[]="payment_intent.payment_failed" \
         -d enabled_events[]="payment_intent.succeeded" \
         -d enabled_events[]="payout.failed" \
         -d enabled_events[]="payout.paid"
}

test -z "$STRIPE_API_KEY" && echo "STRIPE_API_KEY must be set" && exit 1

id=$(get_webhook_id)

if [ -z "$id" ]
then
    echo No Stripe webhook endpoint found
else
    echo Updating Stripe webhook $id
    update_event_list $id
fi
