
This is a sample of how an analytic agent could be made
===

The objective of an anlytic agent is to take input from the exporter
module, do something with it, and then import into the admin-API's
"importer" interface with the result that new offers are made
available to whomever the agent decides it should be made available
to.

The code in this directory is intended as a proof of concept that
it is indeed possible to export customer data, use it for something,
and use it to produce segments and offers.

The functions being tested are the pseudo-anonymization happening in
the exporter and importers, and also the general "ergonomics" of the
agent.  If it's not something we believe can be used by a competent
person of an external organization after a two-minute setup time,
then it's not good enugh.
