import arrow.core.Either
import arrow.core.right
import org.ostelco.prime.model.Customer
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.AllowedRegionsService
import org.ostelco.prime.storage.graph.PrimeTransaction

object : AllowedRegionsService {
    override fun get(customer: Customer, transaction: PrimeTransaction): Either<StoreError, Collection<String>> {
        val allowedEmailDomains = listOf("@bar.com", "@redotter.sg", "@test.com")
        val matchedDomains = allowedEmailDomains.filter { customer.contactEmail.toLowerCase().endsWith(it) }
        return if (matchedDomains.size > 0)
            listOf("no", "sg", "us", "my").right()
        else
            listOf("us", "my").right()
    }
}