# Glossary

 * This documents outlines the basic Stripe concepts used by the payment processor component.
 * The objective of doing so, is to define a jargon to assist internal communication.
 * The document may not be just limited to definition, but would also try to define user-story to compliment and to give context to the explanation.
 * The entries are not sorted alphabetically, but instead in ordered by dependency or by composition.

### Charge
 * The client application captures the card information as a token or a source using the stripe client APIs.
 * This information is passed to our server for actual payment. The server then creates a **Charge** using the token/source information. The strip server APIs are used to perform this operation.

