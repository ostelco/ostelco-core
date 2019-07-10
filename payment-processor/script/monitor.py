#! /usr/bin/env python3

import argparse
import http.client
import json
import logging
import os
import sys

# the '**' notation for dicts was introduced in 3.5
MIN_PYTHON = (3, 5)

if sys.version_info < MIN_PYTHON:
    sys.exit("Python %s.%s or later is required.\n" % MIN_PYTHON)

# config
parser = argparse.ArgumentParser()
parser.add_argument("--log", type=str, default=os.getenv('LOG_LEVEL', "warn"),
                    help="log level (debug, error, warn, info)")
parser.add_argument("--host", type=str, default=os.getenv('PRIME_HOST', "localhost"),
                    help="host name")
parser.add_argument("--port", type=int, default=os.getenv('PRIME_PORT', 8080),
                    help="http port")
args = parser.parse_args()

# def. log level is WARN
logging.basicConfig(level=getattr(logging, args.log.upper()))

# expected list of subscribed to Stripe events
excpected_stripe_events = """\
    [ "customer.deleted",
      "customer.created",
      "charge.succeeded",
      "charge.refunded" ]
"""

conn = http.client.HTTPConnection(args.host, args.port, timeout=10)


def get(path):
    resp = call_http("GET", path)
    logging.info(resp)
    return resp


def post(path, data):
    resp = call_http("POST", path, data, {'Content-type': 'application/json'})
    logging.info(resp)
    return resp


def call_http(*args):
    try:
        conn.request(*args)
        resp = conn.getresponse()
        return { **{
            'status': resp.status
        }, **(json.loads(resp.read().decode()) if resp.status == 200 else { 'error': resp.reason }) }
    except Exception as e:
        return { 'status': 500, 'error': "{}".format(e) }


# check API version local vs. at Stripe
get("/stripe/monitor/apiversion")

# check if Stripe events/webhook is enabled
get("/stripe/monitor/webhook/enabled")

# fetch all subscribed to events
get("/stripe/monitor/webhook/events")

# match for correct subscribed to events
post("/stripe/monitor/webhook/events", excpected_stripe_events)

# fetch failed events and spool to pubsub
get("/stripe/monitor/events/failed")

# check transactions last 2h
get("/payment/check")
