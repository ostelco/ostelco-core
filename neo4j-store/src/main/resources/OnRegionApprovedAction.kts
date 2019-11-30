import arrow.core.Either
import arrow.core.fix
import arrow.core.getOrElse
import arrow.effects.IO
import arrow.core.extensions.either.monad.monad
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
        return IO {
            Either.monad<StoreError>().binding {
                WriteTransaction(transaction).apply {
                    val segmentId = get(Segment withId "plan-country-${regionCode.toLowerCase()}")
                            .getOrElse {
                                get(Segment withId "country-${regionCode.toLowerCase()}").bind()
                            }
                            .id
                    fact { (Customer withId customer.id) belongsToSegment (Segment withId segmentId) }.bind()
                    AuditLog.info(customer.id, "Added customer to segment - $segmentId")
                }
                Unit
            }.fix()
        }.unsafeRunSync()
    }
}