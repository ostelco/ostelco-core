#! /usr/bin/env python3

import http.client
import json

conn = http.client.HTTPConnection("localhost", 9090, timeout=10)

def get(path):
    try:
        conn.request("GET", path)
        resp = conn.getresponse()
        return { **{
            'status': resp.status
        }, **(json.loads(resp.read().decode()) if resp.status == 200 else { 'error': resp.reason }) }
    except Exception as e:
        return  { 'status': 500, 'error': "{}".format(e) }

print(get("/stripe/monitor/apiversion"))
print(get("/stripe/monitor/webhook/enabled"))
print(get("/stripe/monitor/webhook/events"))
print(get("/stripe/monitor/events/failed"))
