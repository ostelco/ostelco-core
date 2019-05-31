# Recover SIM Profiles which were Provisioned but never downloaded.

### Get all SIM Profiles which are detached from Subscriber.

```cypher
MATCH (r:Region)--(sp:SimProfile) OPTIONAL MATCH (sp)--(c:Customer) WHERE c is null RETURN sp.iccId;
```

Export as CSV.

### Get all SIM profiles which are provisioned by never downloaded.

Set values in query below.

```postgresql
SELECT iccid
FROM sim_entries
WHERE
      hlrstate = 'ACTIVATED'
      AND smdpplusstate = 'RELEASED'
      AND provisionstate = 'PROVISIONED'
      AND iccid in ('', '');
```

### Reset Provision state from 'PROVISIONED' TO 'AVAILABLE'

Set same values in query below.

```postgresql
UPDATE sim_entries
SET provisionstate = 'AVAILABLE'
WHERE
      hlrstate = 'ACTIVATED'
      AND smdpplusstate = 'RELEASED'
      AND provisionstate = 'PROVISIONED'
      AND iccid in ('', '');
```