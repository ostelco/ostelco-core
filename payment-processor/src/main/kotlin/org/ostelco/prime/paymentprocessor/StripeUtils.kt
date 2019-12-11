package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.left
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.InvalidRequestException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.core.ApiConnectionError
import org.ostelco.prime.paymentprocessor.core.AuthenticationError
import org.ostelco.prime.paymentprocessor.core.CardError
import org.ostelco.prime.paymentprocessor.core.GenericError
import org.ostelco.prime.paymentprocessor.core.InvalidRequestError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentVendorError
import org.ostelco.prime.paymentprocessor.core.RateLimitError

object StripeUtils {

    private val logger by getLogger()

    /**
     * Convenience function for handling Stripe I/O errors.
     */
    fun <RETURN> either(description: String, action: () -> RETURN): Either<PaymentError, RETURN> =
            try {
                Either.right(action())
            } catch (e: CardException) {
                /* Card got declined. */
                logger.warn("Payment card error : $description, Stripe codes: ${e.code}, ${e.declineCode}, ${e.statusCode}")
                CardError(description = description,
                        message = e.message,
                        code = e.code,
                        declineCode = e.declineCode).left()
            } catch (e: RateLimitException) {
                /* Too many requests made to the Stripe API at the same time. */
                logger.error("Payment rate limiting error : $description, Stripe codes: ${e.code}, ${e.statusCode}")
                RateLimitError(description = description,
                        message = e.message,
                        code = e.code).left()
            } catch (e: InvalidRequestException) {
                /* Invalid parameters were supplied to Stripe's API. */
                logger.error("Payment invalid request : $description, Stripe codes: ${e.code}, ${e.statusCode}")
                InvalidRequestError(description = description,
                        message = e.message,
                        code = e.code).left()
            } catch (e: AuthenticationException) {
                /* Authentication with Stripe's API failed.
                   (Maybe you changed API keys recently?) */
                logger.error("Payment authentication error : $description, Stripe codes: ${e.code}, ${e.statusCode}",
                        e)
                AuthenticationError(description = description,
                        message = e.message,
                        code = e.code).left()
            } catch (e: ApiConnectionException) {
                /* Network communication with Stripe failed. */
                logger.error("Payment API connection error : $description",
                        e)
                ApiConnectionError(description = description,
                        message = e.message).left()
            } catch (e: StripeException) {
                /* Unknown Stripe error. */
                logger.error("Payment unknown Stripe error : $description",
                        e)
                PaymentVendorError(description = description,
                        message = e.message).left()
            } catch (e: Exception) {
                /* Something else happened, completely unrelated to
                   Stripe. */
                logger.error("Payment unknown error : $description",
                        e)
                GenericError(description = description,
                        message = e.message).left()
            }
}
