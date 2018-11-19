import junit.framework.TestCase.assertEquals
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.*
import org.ostelco.jsonValidation.JsonSchemaInputOutputValidationInterceptor
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.mockito.Mockito.*

class ES2PlusResourceTest {

    companion object {

        private val dao = mock(SimInventoryDAO::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(EsimInventoryResource(dao))
                .addProvider(JsonSchemaInputOutputValidationInterceptor("resources"))
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {
        }
    }

    val fakeIccid1 = "01234567891234567890"
    val fakeIccid2 = "01234567891234567891"
    val fakeIccid3 = "01234567891234567892"
    val fakeImsi1 = "12345678912345"
    val fakeImsi2 = "12345678912346"
    val fakeMsisdn1 = "474747474747"
    val fakeMsisdn2 = "464646464646"
    val fakeHlr = "Loltel"


    fun fakeEntryWithoutMsisdn() : SimEntry {
        return SimEntry(
                id = 1L,
                hlrId = "Loltel",
                batch = 99L,
                iccid = fakeIccid1,
                imsi = fakeImsi1,
                smdpplus = "Loltel",
                eid = "bb",
                hlrActivation = false,
                smdpPlusActivation = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss")
    }

    fun fakeEntryWithoutMsisdnAndSmdpplus() : SimEntry {
        return SimEntry(
                id = 1L,
                hlrId = fakeHlr,
                smdpplus = "Loltel",
                batch = 99L,
                iccid = fakeIccid1,
                imsi = fakeImsi1,
                eid = "bb",
                hlrActivation = false,
                smdpPlusActivation = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss")
    }

    fun fakeEntryWithMsisdn() : SimEntry{
        return SimEntry(
                id = 1L,
                hlrId = "Loltel",
                batch = 99L,
                iccid = fakeIccid2,
                imsi = fakeImsi2,
                msisdn = fakeMsisdn1,
                eid = "bb",
                hlrActivation = false,
                smdpPlusActivation = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss")
    }


    var fakeSimEntryWithoutMsisdn = fakeEntryWithoutMsisdn()

    var fakeSimEntryWithMsisdn = fakeEntryWithMsisdn()

    var fakeEnrtryWithoutMsisdnAndSmdpplus = fakeEntryWithoutMsisdnAndSmdpplus()


    @Before
    fun setUp() {

        this.fakeSimEntryWithoutMsisdn = fakeEntryWithoutMsisdn()
        this.fakeSimEntryWithMsisdn = fakeEntryWithMsisdn()
        this.fakeEnrtryWithoutMsisdnAndSmdpplus = fakeEntryWithoutMsisdnAndSmdpplus()


        val mockHlrAdapter = HlrAdapter(1L, fakeHlr)
        val mockSmdpplusAdapter = SmdpPlusAdapter(1L, "Loltel")

        reset(dao)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid3))
                .thenReturn(fakeEnrtryWithoutMsisdnAndSmdpplus)

        org.mockito.Mockito.`when`(dao.getSimProfileById(fakeSimEntryWithoutMsisdn.id!!))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid2))
                .thenReturn(null)


        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi2))
                .thenReturn(null)

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn1))
                .thenReturn(fakeSimEntryWithMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn2))
                .thenReturn(null)

        org.mockito.Mockito.`when`(dao.findNextFreeSimForMsisdn(fakeHlr))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getHlrAdapterByName(fakeHlr))
                .thenReturn(mockHlrAdapter)

        org.mockito.Mockito.`when`(dao.getSmdpPlusAdapterByName("Loltel"))
                .thenReturn(mockSmdpplusAdapter)


    }

    @Test
    fun testFindByIccidPositiveResult() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        verify(dao).getSimProfileByIccid(fakeIccid1)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithoutMsisdn, simEntry)
    }

    @Test
    fun testFindByIccidNegativeResult() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid2")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(404, response.status)
        verify(dao).getSimProfileByIccid(fakeIccid2)
    }

    @Test
    fun testFindByImsiPositiveResult() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/imsi/$fakeImsi1")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithoutMsisdn, simEntry)
        verify(dao).getSimProfileByImsi(fakeImsi1)
    }

    @Test
    fun testFindByImsiNegativeResult() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/imsi/$fakeImsi2")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(404, response.status)
        verify(dao).getSimProfileByImsi(fakeImsi2)
    }

    @Test
    fun testFindByMsisdnPositiveResult() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/msisdn/$fakeMsisdn1")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithMsisdn, simEntry)
        verify(dao).getSimProfileByMsisdn(fakeMsisdn1)
    }

    @Test
    fun testFindByMsisdnNegativeResult() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/msisdn/$fakeMsisdn2")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(404, response.status)
        verify(dao).getSimProfileByMsisdn(fakeMsisdn2)
    }

    @Test
    fun testAllocateNextFree() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/msisdn/$fakeMsisdn1/allocate-next-free")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post (or put?)x'

        assertEquals(200, response.status)
        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithMsisdn, simEntry)
    }

    @Test
    fun testActivateAll() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid1/activate/all")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    @Test
    fun testActivateHlr() {

        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid1/activate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(200, response.status)
        val simEntry = response.readEntity(SimEntry::class.java)

        // XXX Bunch of verifications missing
        verify(dao).getSimProfileById(fakeSimEntryWithoutMsisdn.id!!)
    }

    @Test
    fun testActivateEsimSuccessfully() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid1/activate/esim")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        // XXX Missing a bunch of verifications
    }

    @Test
    fun testActivateEsimFailinglyOnSimWithoutSmdpPlus() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid3/activate/esim")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(400, response.status)
    }

    @Test
    fun testDeactivate() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid1/deactivate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post

        // XXX Check what return value to expect when updating, don't think it's 200
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    @Test
    fun testImport() {
        val sampleCsvIinput =
                """
        ICCID, IMSI, PIN1, PIN2, PUK1, PUK2
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
       123123, 123123, 1233,1233,1233,1233
    """.trimIndent()

        val response = RULE.target("/ostelco/sim-inventory/Loltel/import-batch/sim-profile-vendor/Idemia")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))

        // XXX Should be 201, but we'll accept a 200 for now.
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
    }
}
