
import arrow.core.Either
import arrow.core.right
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.KycType
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.AllowedRegionsService
import org.ostelco.prime.storage.graph.OnKycApprovedAction
import org.ostelco.prime.storage.graph.PrimeTransaction

object : OnKycApprovedAction {

    override fun apply(
            customer: Customer,
            regionCode: String,
            kycType: KycType,
            kycExpiryDate: String?,
            kycIdType: String?,
            allowedRegionsService: AllowedRegionsService,
            transaction: PrimeTransaction): Either<StoreError, Unit> = Unit.right()
}