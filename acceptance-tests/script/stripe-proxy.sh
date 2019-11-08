#! /bin/sh

# Only start the Stripe CLI tool in proxy forward mode
# if the STRIPE_ENDPOINT_SECRET environmentvariable is
# set to what looks like an actual secret.
#
# If not set, block until stopped by SIGTERM or similar.

echo $STRIPE_ENDPOINT_SECRET | grep -q ^whsec_

if [ $? -eq 0 ]
then
    echo Starting Stripe CLI in proxy forward mode...
    stripe listen \
        --forward-to prime:8080/stripe/event \
        --events customer.subscription.updated,invoice.created,invoice.payment_succeeded,invoice.upcoming
else
    echo Not starting Stripe CLI.
    sleep 2147483647   # block for 2^31-1 seconds
fi
