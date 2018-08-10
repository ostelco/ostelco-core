# Stripe payment
* This documents outlines the basic `Stripe` concepts used by the payment processor component.

### Customer
 *  Stripe provides a customer object that allows us to keep track of
     1. Executed payments.
     2. Cards and payment sources.
 * A customer can choose to save cards, set a default card for payments etc.
 * A saved card is required for recurring payments.

### Payments
 Payment using `Srtripe` is a two-step process involving both client and server.
 1. Securly collecting payment information. The client will securly collect the card information using one of `Stripes's` client APIs. Depending on the API sued, it will produce a card token or a payment source.
 2. Create a **Charge** to complete the payment. The client will pass the payment information (in the form of a token or source-id) to our payment server. The server creates a charge using this to complete the payment.

### Recurring Payments
 Recurring Payment in `Stripe` is done using subscriptions.
 1. Define a service product
 2. Create a pricing plan that sets how much should be billed and at what interval.
 3. Create a customer in Stripe.
 4. Subscribe the customer to the plan.

