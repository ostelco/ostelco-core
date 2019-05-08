# Glossary

 * This documents lists the terminologies used for this project.
 * The objective of doing so, is to define a jargon to assist internal communication.
 * The document may not be just limited to definition, but would also try to define user-story to compliment and to give context to the explanation.
 * The entries are not sorted alphabetically, but instead in ordered by dependency or by composition.

### Hypothesis
 * **Hypothesis** is an outcome of performing data analytics, or (unlikely but possible) product of imagination.

### Experiment

 * **Experiment** is an exercise to evaluate a **hypothesis**.
 * **Experiment** is performed by defining and exercising one or more **Offers**.

### Offer
 * **Offer** aims to target a **Segment** of customers with the **Product(s)**.
 * Thus **Offer** is composition of **Segment** and **Product(s)**. 

### Segment
 * **Segment** of customers is a _improper_ subset of all customers. 
 * **Segment** is defined as a condition which will be true for those the set of customers.
 * Outcome of applying a **Segment** filter is a list of customers. 
 * So, (unlikely but possible) **segment** may directly be a list of customers without an explicit _filter condition_.
 * **Customers** may join/leave segment dynamically.
 * Schedule/frequency of **Segment** _membership_ is upto technical feasibility.
 * If the **Segment** _membership_ is updated during its associated with an _Active_ **Offer**, then the **Product(s)** 
   offering to customers belonging to that segment should correspondingly updated.
 * Segment can exist without Offer.

### Product
 * Internally, **Product** is a complex & mutable concept, but is immutable once defined. 
 * When exposed externally, most of the aspects of **Product** will be hidden.
 * Externally, **Product** has only 3 primary attributes:
   * SKU - Unique ID representing an immutable product.
   * Price - <Amount,Currency>
   * Product class
 * Keeping just these attributes allows to keep concept of **Product** very generic.
 * Mutations in this concept of **Product** will then not impact the API definition.
 * But externally, additional attributes of **Product** might be needed, most probably for presentation.
 * These non-primary attributes of **Product** are considered _metadata_, and schema for these attributes is 
   managed via **Product Class** (defined further down below).  

### Product Class
 * Similar **Products** belong to a **Product class**.
 * This is no hierarchy in **Product classes** (as of now).  
 * **Product class** acts as a template to define **Products** with varying parameters for the _template_.
 * **Product Class** has _Properties_.
 * Some or all of these _Properties_ might be parameters for the _template_.
 * Examples:
    * Product Class - `SIMPLE_DATA` has properties as `bytes`.
    * Product Class - `LIMITED_VALIDITY_DATA` has properties as `bytes`, `duration`. Here, unused data is lost after `end_date`, which is calculated based on `purchase_date` and `duration`.
    * Product Class - `FREE_NETFLIX_FOR_PERIOD` has properties as `start_date`, `end_date`. Here, `Netflix` is part of SKU and is not an explicitly defined property.

### Resource
 * Resource is the _commodity_ which is consumed by customer.
 * It is a simple _enum_ having values such as:
   * Data
   * Voice
   * SMS
   * MMS
 * Each resource has (one or more) unit of measurements, such as
 
| **Resource** | **Unit**      | **Alternate Unit** |
| ---          | ---           | ---                |
| Data         | Bytes         | Time Duration      |
| Amount       | Currency      | -                  |
| Voice        | Time Duration | Bytes              |
| SMS          | Unit          | Size               |
| MMS          | Unit          | Size               |

 * Even for Data Only operator, _Amount_ resource might be needed to account for Roaming Data consumption.

### Resource Bundle

 * **Resource Bundle** is like an account where the **Resource** is _credited_ or _debited_.
 * All of the **Resource Bundle** might not be exposed to customer.
 * This account need not be _positive balanced_, 
   meaning customer consumption leading to _negative_ balance may be permitted.
 * In case of Prepaid, the account is first _credited_ by using _Topup_ operation, 
   and then _debited_ from due to consumption.
 * Based on _Resource Consumption Context_, **Resource** may be consumed from _other_ type of **Resource Bundle**.
 * E.g. consumption of _Roaming data_ may be debited from _Amount_ **Resource Bundle** at a defined conversion `Rate`.

### Topup operation
 * A _Topup_ operation is a purchase of (one or more) _topup_ product(s), which in turn results in _Credit_ to (one or more) 
   **Resource Bundles**.
 * It is subtype of _Purchase_ operation, which include purchase of other products, such as `SIM card` or `Topup Voucher`.
   Thus, in case of `Topup voucher`, actual **Topup operation** happens when Voucher is _claimed_ and not when it is _purchased_.

### Region
 * Region can be:
   * Country
   * Part of a country
   * Set of multiple countries
 
