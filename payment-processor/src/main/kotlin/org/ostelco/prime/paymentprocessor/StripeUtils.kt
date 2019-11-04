package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.InvalidRequestException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentFailedError

object StripeUtils {

    private val logger by getLogger()

    /**
     * Convenience function for handling Stripe I/O errors.
     */
    fun <RETURN> either(errorDescription: String, action: () -> RETURN): Either<PaymentError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: CardException) {
            /* Something is wrong with the payment source/card.
               402 indicates that the card is out of funds or expired etc. */
            logger.warn("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}")
            Either.left(if (e.statusCode == 402)
                PaymentFailedError(errorDescription, e.message)
            else
                ForbiddenError(errorDescription, e.message))
        } catch (e: RateLimitException) {
            // Too many requests made to the API too quickly
            logger.warn("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}")
            Either.left(BadGatewayError(errorDescription, e.message))
        } catch (e: InvalidRequestException) {
            // Invalid parameters were supplied to Stripe's API
            logger.warn("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}")
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: AuthenticationException) {
            // Authentication with Stripe's API failed
            // (maybe you changed API keys recently)
            logger.warn("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: ApiConnectionException) {
            // Network communication with Stripe failed
            logger.warn("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: StripeException) {
            // Unknown Stripe error
            logger.error("Payment error: ${errorDescription}, Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: Exception) {
            // Something else happened, could be completely unrelated to Stripe
            logger.error(errorDescription, e)
            Either.left(BadGatewayError(errorDescription))
        }
    }
}
