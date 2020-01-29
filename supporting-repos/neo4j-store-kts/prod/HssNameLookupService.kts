import org.ostelco.prime.storage.graph.HssNameLookupService

object : HssNameLookupService {
    override fun getHssName(regionCode: String): String {
        return when (regionCode.toLowerCase()) {
            "sg" -> "M1"
            "my" -> "Digi"
            "no" -> "Loltel"
            else -> "TEST"
        }
    }
}