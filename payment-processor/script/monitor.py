#! /usr/bin/env python3

# Monitors Stripe configuration + checks for mismatch between purchase records
# stored to neo4j and payment transactions stored in Stripe.
#
# Intended to be run as a cron job.

import argparse
import http.client
import json
import logging
import os
import sys
import time

# the '**' notation for dicts was introduced in 3.5
# the 'f' notation for string interpolation was added in 3.6
MIN_PYTHON = (3, 6)

if sys.version_info < MIN_PYTHON:
    sys.exit("Python %s.%s or later is required.\n" % MIN_PYTHON)

# time ranges in seconds
ONE_DAY = 86400
TWO_HOURS = 7200

# config
parser = argparse.ArgumentParser(add_help=False)
parser.add_argument("-h", "--host", type=str, default=os.getenv('PRIME_HOST', "localhost"),
                    help="host name")
parser.add_argument("-p", "--port", type=int, default=os.getenv('PRIME_PORT', 8080),
                    help="http port")
parser.add_argument("-i", "--interval", type=int, default=os.getenv('INTERVAL', -TWO_HOURS),
                    help="interval in seconds to check")
parser.add_argument("-d", "--dataset", type=str, default=os.getenv('STRIPE_EVENTS_DATASET', None),
                    help="file with Stripe events to expect")
parser.add_argument("-l", "--loglevel", type=str, default=os.getenv('LOG_LEVEL', "warn"),
                    help="log level (debug, error, warn, info)")
parser.add_argument('--help', action='help',
                    help='show this help message and exit')
args = parser.parse_args()

# def. log level is WARN
logging.basicConfig(level=getattr(logging, args.loglevel.upper()))

conn = http.client.HTTPConnection(args.host, args.port, timeout=10)


def get(path):
    resp = call_http("GET", path)
    logging.info(resp)
    return resp


def post(path, data):
    resp = call_http("POST", path, json.dumps(data), {'Content-type': 'application/json'})
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
        logging.error(f"HTTP {args[0]} {args[1]} error: {e}")
        return { 'status': 500, 'error': "{}".format(e) }


# seconds since epoch, optionally with offset
def epoch(offset=0):
    return round(time.time()) + offset


# optionally load list, in json format, with Stripe events from file
def load_events_from_file(fn, defvalue=None):
    if fn is not None:
        with open(fn) as f:
            return json.load(f)
    return json.loads(defvalue)


# expected list of subscribed to Stripe events (def. is "expect all events")
excpected_stripe_events = load_events_from_file(args.dataset, defvalue="""[ "*" ]""")


def main():
    # check API version local vs. at Stripe
    get("/stripe/monitor/apiversion")

    # check if Stripe events/webhook is enabled
    get("/stripe/monitor/webhook/enabled")

    # fetch all subscribed to events
    get("/stripe/monitor/webhook/events")

    # match for correct subscribed to events
    post("/stripe/monitor/webhook/events", excpected_stripe_events)

    # fetch failed events and spool to pubsub
    get(f"/stripe/monitor/events/failed?start={epoch(args.interval)}")

    # check transactions last 2h0
    get(f"/payment/check?start={epoch(args.interval)}")


if __name__ == "__main__":
    main()
