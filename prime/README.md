# Prime - Micro Backend for Support System



## Interfaces

* Subscriber (End-user and/or CRM)
* OCS (Online Charging System) Gateway (towards Packet gateway (P-GW))



## Use cases

### Subscriber API
* Perform Data top up
* Get available Data balance

### OCS API
* CreditControlRequest


    P-GW requests Data buckets (of approx 100 MB) for a subscriber.
    This request is made before its current bucket is about to be exhausted.
    This request has very high throughput and low latency requirement.
    Low latency is required because if the P-GW does not receives 
    a response in time before the current bucket is expired, it disconnects/throttles
    data of the subscriber.

* Activate after Data top up


    After the last bucket for a subscriber is given using `Fetch Data bucket`, OCS
    denies more data, for which the PGw disconnects data for the subscriber.
    PGw does not asks for data bucket again for that subscriber until 
    subscriber disables/enables the data connection or until OCS notifies PGw
    toactivate that subscriber again.

## Event flow

| Message Type              | Producer   | Handler  | Next Handler                      |
| ---                       | ---        | ---      | ---                               |
| CREDIT_CONTROL_REQUEST    | OcsService | OcsState | OcsService, _(maybe) Subscriber_  |
| RELEASE_RESERVED_BUCKET   | OcsService | OcsState | OcsService, Subscriber            |
| TOPUP_DATA_BUNDLE_BALANCE | Subscriber | OcsState | OcsService (activate), Subscriber |
| GET_DATA_BUNDLE_BALANCE   | Subscriber | OcsState | Subscriber                        |

