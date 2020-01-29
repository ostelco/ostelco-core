import arrow.core.Either
import arrow.core.extensions.fx
import org.ostelco.prime.auditlog.AuditLog
import org.ostelco.prime.dsl.WriteTransaction
import org.ostelco.prime.dsl.withId
import org.ostelco.prime.model.Customer
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.OnRegionApprovedAction
import org.ostelco.prime.storage.graph.PrimeTransaction
import org.ostelco.prime.storage.graph.model.Segment

object : OnRegionApprovedAction {

    override fun apply(
            customer: Customer,
            regionCode: String,
            transaction: PrimeTransaction
    ): Either<StoreError, Unit> {
        val segmentId = when (regionCode.toLowerCase()) {
            "no" -> "country-${regionCode.toLowerCase()}"
            else -> "country-sg"
        }
        return Either.fx {
            WriteTransaction(transaction).apply {
                fact { (Customer withId customer.id) belongsToSegment (Segment withId segmentId) }.bind()
                AuditLog.info(customer.id, "Added customer to segment - $segmentId")
            }
            Unit
        }
    }
}