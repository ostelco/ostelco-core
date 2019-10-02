import arrow.core.Either
import arrow.core.right
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.AllowedRegionsService
import org.ostelco.prime.storage.graph.PrimeTransaction

object : AllowedRegionsService {
    override fun get(identity: Identity, customer: Customer, transaction: PrimeTransaction): Either<StoreError, Collection<String>> {
        return if (customer.contactEmail.toLowerCase().endsWith("@bar.com"))
            listOf("no", "sg", "us", "my").right()
        else
            listOf("sg", "us", "my").right()
    }
}