import arrow.core.Either
import arrow.core.right
import org.ostelco.prime.model.Customer
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.AllowedRegionsService
import org.ostelco.prime.storage.graph.PrimeTransaction

object : AllowedRegionsService {

    override fun get(customer: Customer, transaction: PrimeTransaction): Either<StoreError, Collection<String>> {

        val allowedRegions = setOf(
                "sg",
                "my",
                "no".takeIf {
                    isEmailAllowed(
                            customerEmail = customer.contactEmail.toLowerCase(),
                            allowedEmails = setOf(
  				    "zaphood.beebelbrox@heart-of-gold.intergalactic.universal" // <-- Obviously just an example
                            ),
                            allowedEmailSuffixes = setOf(
                                    "@oya.sg",
                                    "@oya.world",
                                    "@redotter.sg",
                                    "@redotter.world"
                            )
                    )
                }
        ).filterNotNull()

        return allowedRegions.right()
    }

    private fun isEmailAllowed(
            customerEmail: String,
            allowedEmails: Set<String>,
            allowedEmailSuffixes: Set<String>
    ): Boolean = allowedEmailSuffixes.any { customerEmail.endsWith(it) } || allowedEmails.contains(customerEmail)

}
