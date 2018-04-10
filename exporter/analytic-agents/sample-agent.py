#!/usr/bin/env python

import logging
import os
import cloudstorage as gcs
import webapp2

from google.appengine.api import app_identity



resultTemplate = """
producing-agent:
  name: Simple agent
  version: 1.0
offer:
  history:
    createdAt: "2018-02-22T12:41:49.871Z"
    updatedAt: "2018-02-22T12:41:49.871Z"
    visibleFrom: "2018-02-22T12:41:49.871Z"
    expiresOn: "2018-02-22T12:41:49.871Z"
  presentation:
    badgeLabel: "mbop"
    description: "Best offer you will get today"
    shortDescription: "Best offer!"
    label: "3 GB"
    name: "3 GB"
    priceLabel: "49 NOK"
    hidden: false
    image: https://www.ft-associates.com/wp-content/uploads/2015/08/Best-Offer.jpg
  financial:
    # This is the price in millis, perhaps the name should
    # reflect that?
    price: 4900
    currencyLabel: "NOK"
    repurchability:1
    taxRate: 10	
    product:
      SKU: 2
      noOfBytes: 3000000000

  segment:
    type: agent-specific-segment
    members:
       decryptionKey: none
       members:
        - 4790300157
        - 4790300144
        - 4333333333
"""

def get(self):
  bucket_name = os.environ.get('BUCKET_NAME',
                               app_identity.get_default_gcs_bucket_name())

  self.response.headers['Content-Type'] = 'text/plain'
  
                      + os.environ['CURRENT_VERSION_ID'] + '\n')
  self.response.write('Using bucket name: ' + bucket_name + '\n\n')

print (resultTemplate)

get("foo")



