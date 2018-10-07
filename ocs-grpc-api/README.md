# OCS API

**From P-GW to OCS**

This is a translation from the DIAMETER Credit-Control-Request [RFC 4006](https://tools.ietf.org/html/rfc4006#page-9) to gRPC. Not all elements in the Credit-Control-Request is translated. Only the one we are currently using.

* CreditControlRequest
```
=> CreditControlRequestInfo(
        CreditControlRequestType, 
        String requestId, 
        String msisdn, 
        String imsi, 
        MultipleServiceCreditControl[], 
        ServiceInfo serviceInformation)

<= CreditControlAnswerInfo(
        String requestId, 
        String msisdn, 
        MultipleServiceCreditControl[] mscc)
```
                                
**From OCS to P-GW**

* Activate

```
<= activate (String msisdn)
```

