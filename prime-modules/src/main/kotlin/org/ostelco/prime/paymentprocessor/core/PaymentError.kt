package org.ostelco.prime.paymentprocessor.core

import org.ostelco.prime.apierror.InternalError

/**
 * For specific Stripe error messages a few 'code' fields are included
 * which can provide more details about the cause for the error.
 *
 *   - code : Mainly intended for programmatically handling of
 *            errors but can be useful for giving more context.
 *            https://stripe.com/docs/error-codes
 *   - decline code : For card errors resulting from a card
 *            issuer decline. Not always set.
 *            https://stripe.com/docs/declines/codes
 *   - status code : HTTP status code.
 *
 * The 'codes' fields are included in the error reporting when they are
 * present.
 * @param description Error description
 * @param message Error description as provided upstream (Stripe)
 * @param code A short string indicating the type of error from upstream
 * @param declineCode A short string indication the reason for a card
 *                    error from the card issuer
 * @param internalError Internal error chaining
 */
sealed class PaymentError(val description: String,
                          val message: String? = null,
                          val code: String? = null,
                          val declineCode: String? = null,
                          val internalError: InternalError?) : InternalError()

class ChargeError(description: String,
                  message: String? = null,
                  internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class InvoiceError(description: String,
                   message: String? = null,
                   internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class NotFoundError(description: String,
                    message: String? = null,
                    code: String? = null,
                    internalError: InternalError? = null) : PaymentError(description, message, code, null, internalError)

class PaymentConfigurationError(description: String,
                                message: String? = null,
                                internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class PlanAlredyPurchasedError(description: String,
                               message: String? = null,
                               internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class StorePurchaseError(description: String,
                         message: String? = null,
                         internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class SourceError(description: String,
                  message: String? = null,
                  internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class SubscriptionError(description: String,
                        message: String? = null,
                        internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class UpdatePurchaseError(description: String,
                          message: String? = null,
                          internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class CardError(description: String,
                message: String? = null,
                code: String? = null,
                declineCode: String? = null,
                internalError: InternalError? = null) : PaymentError(description, message, code, declineCode, internalError)

class RateLimitError(description: String,
                     message: String? = null,
                     code: String? = null,
                     internalError: InternalError? = null) : PaymentError(description, message, code, null, internalError)

class InvalidRequestError(description: String,
                     message: String? = null,
                     code: String? = null,
                     internalError: InternalError? = null) : PaymentError(description, message, code, null, internalError)

class AuthenticationError(description: String,
                          message: String? = null,
                          code: String? = null,
                          internalError: InternalError? = null) : PaymentError(description, message, code, null, internalError)

class ApiConnectionError(description: String,
                   message: String? = null,
                   internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class PaymentVendorError(description: String,
                         message: String? = null,
                         internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)

class GenericError(description: String,
                      message: String? = null,
                      internalError: InternalError? = null) : PaymentError(description, message, null, null, internalError)
