@1;95;0cimport arrow.core.Either
import arrow.core.extensions.fx
import org.ostelco.prime.dsl.WriteTransaction
import org.ostelco.prime.dsl.withSku
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.applyProduct
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.createPurchaseRecord
import org.ostelco.prime.storage.graph.OnNewCustomerAction
import org.ostelco.prime.storage.graph.PrimeTransaction
import java.time.Instant
import java.util.*

object : OnNewCustomerAction {
    override fun apply(identity: Identity,
                       customer: Customer,
                       transaction: PrimeTransaction): Either<StoreError, Unit> {

        val welcomePackProductSku = "1GB_FREE_ON_JOINING"

        return Either.fx {
            WriteTransaction(transaction).apply {
                val (product) = get(Product withSku welcomePackProductSku)
                createPurchaseRecord(
                        customer.id,
                        PurchaseRecord(
                                id = UUID.randomUUID().toString(),
                                product = product,
                                timestamp = Instant.now().toEpochMilli()
                        )
                ).bind()
                applyProduct(
                        customerId = customer.id,
                        product = product
                ).bind()
            }
            Unit
        }
    }
}