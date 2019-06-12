import org.ostelco.prime.storage.graph.HssNameLookupService

object : HssNameLookupService {
    override fun getHssName(regionCode: String): String = "Loltel"
}
