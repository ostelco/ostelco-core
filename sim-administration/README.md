#  SIM management

This is an esim management module that facilitates provisioning of
esim profiles from an SMDP+, via an operators HSS into customers'
phones.

It handles logistics of ordering and provisioning batches from the sim
vendor, including keeping track of production statuses in the various
HSSes where the profiles must also be referenced before they can be
used.

At present all profiles are assumed to be active in the HSS, but a
reasonable upgrade would be to also activate them either after the customer
has downloaded the profile, or shortly before.



