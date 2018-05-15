# Admin API

 * Admin API is for back-office admin management.
 * Each of the _Resources_ are listed below shall have Create-Read-Update-Delete operations.

### Product Class

    id: <product class id>
    properties:
      <multi-level template>

### Product

##### Product for Client API

    sku: <product sku>
    class: <product class id>
    price:
      amount: <amount>
      currency: <currency code>
    properties:
      <multi-level object>

##### Additional attributes of Product for Admin API

    visibility:
      flag: <boolean>
      start_date: <datetime>
      end_date: <datetime>

### Segment

    id: <segment id>
    description: <optional human understandable text>
    export_id: <export id used to de-anonymize subscriber ids>
    type: <EXTERNAL or CONDITIONAL>
    subscribers:
      <list of subscribers - for type=EXTERNAL only>
    condition: <boolean expression - for type=CONDITIONAL>

### Offer

    id: <offer id>
    segment_id: <segment id>
    products:
      <list of products>
    validity (visibility):
      start_date: <start date of an offer>
      end_date: <end date of an offer>
  