#! /bin/sh

# causes the docker image to exit without error
# on no match
echo $STRIPE_ENDPOINT_SECRET | grep -qv ^whsec_ && exit 0

stripe listen \
    --forward-to prime:8080/stripe/event \
    --events customer.subscription.updated,invoice.created,invoice.payment_succeeded,invoice.upcoming
