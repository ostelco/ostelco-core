#! /usr/bin/env bash

# Sets up ngrok and enables webhook at Stripe.

get_webhook_id() {
    curl -sS -u $STRIPE_API_KEY: https://api.stripe.com/v1/webhook_endpoints \
         -G | \
        grep '\"id\":' | tr -d \, | tr -d \" | awk '{print $2}' | \
        head -1
}

wait_for_ngrok_to_start() {
    local count=0
    local found=no
    while [ $count -lt 10 -a $found = "no" ]
    do
        sleep 1
        curl -sS http://localhost:4040/api/tunnels | \
            grep tunnels > /dev/null 2>&1
        test $? -eq 0 && found=yes
        count=$((count  + 1))
    done
    test $found = "no" && echo "Ngrok failed to create tunnels within 10 sec."
}

get_ngrok_url() {
    curl -sS http://localhost:4040/api/tunnels/command_line | \
        tr ',' '\012' | \
        grep public_url | tr '\"' ' ' | awk '{print $3}'
}

update_webhook() {
    local id=$1
    local url=$2
    curl -sS -u $STRIPE_API_KEY: https://api.stripe.com/v1/webhook_endpoints/$id \
         -d url=$url \
         -d enabled_events[]="customer.subscription.updated" \
         -d enabled_events[]="invoice.created" \
         -d enabled_events[]="invoice.payment_succeeded" \
         -d enabled_events[]="invoice.upcoming" \
         -d disabled=true \
         > /tmp/webhook-update.log 2>&1
}

test -z "$STRIPE_API_KEY" && \
    echo "STRIPE_API_KEY environment variable must be set" && \
    exit 1
test -z "$NGROK_AUTH_TOKEN" && \
    echo "NGROK_AUTH_TOKEN environment variable must be set" && \
    exit 0

# start ngrok
ngrok http -bind-tls=true prime:8080 > /dev/null 2>&1 &
ngrok_pid=$!

# ngrok might use a second or two to get running
wait_for_ngrok_to_start

id=$(get_webhook_id)
url=$(get_ngrok_url)/stripe/event

echo Updating Stripe webhook id $id with new url $url

update_webhook $id $url

echo Update done.

# block on ngrok
wait $ngrok_pid
