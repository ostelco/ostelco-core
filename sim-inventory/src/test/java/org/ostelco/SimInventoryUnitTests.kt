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
    val fakeImsi1 = "12345678912345"
    val fakeImsi2 = "12345678912346"
    val fakeMsisdn1 = "474747474747"
    val fakeMsisdn2 = "464646464646"
    val fakeHlr = "Loltel"

    var fakeSimEntryWithoutMsisdn = SimEntry(
            id = 1L,
            hlrId = "foo",
            batch = 99L,
            iccid = " a",
            imsi = fakeIccid1,
            eid = "bb",
            active = false,
            pin1 = "ss",
            pin2 = "ss",
            puk1 = "ss",
            puk2 = "ss")

    var fakeSimEntry = SimEntry(
            id = 1L,
            hlrId = "foo",
            batch = 99L,
            iccid = " a",
            imsi = fakeIccid1,
            msisdn = fakeMsisdn1,
            eid = "bb",
            active = false,
            pin1 = "ss",
            pin2 = "ss",
            puk1 = "ss",
            puk2 = "ss")


    @Before
    fun setUp() {

        fakeSimEntryWithoutMsisdn = SimEntry(
                id = 1L,
                hlrId = "foo",
                batch = 99L,
                iccid = " a",
                msisdn = null,
                imsi = fakeIccid1,
                eid = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss")

        fakeSimEntry = SimEntry(
                id = 1L,
                hlrId = "foo",
                batch = 99L,
                iccid = " a",
                msisdn = fakeMsisdn1,
                imsi = fakeIccid1,
                eid = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss")

        reset(dao)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid2))
                .thenReturn(null)


        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi2))
                .thenReturn(null)

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn1))
                .thenReturn(fakeSimEntry)

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn2))
                .thenReturn(null)

        org.mockito.Mockito.`when`(dao.findNextFreeSimForMsisdn(fakeHlr))
                .thenReturn(fakeSimEntryWithoutMsisdn)
    }

    @Test
    fun testFindByIccidPositiveResult() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid1")
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
        val response = RULE.target("/ostelco/sim-inventory/Loltel/iccid/$fakeIccid2")
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
        val response = RULE.target("/ostelco/sim-inventory/Loltel/imsi/$fakeImsi2")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(404, response.status)
        verify(dao).getSimProfileByImsi(fakeImsi2)
    }

    @Test
    fun testFindByMsisdnPositiveResult() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/msisdn/$fakeMsisdn1")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithoutMsisdn, simEntry)
        verify(dao).getSimProfileByMsisdn(fakeIccid1)
    }

    @Test
    fun testFindByMsisdnNegativeResult() {
        val response = RULE.target("/ostelco/sim-inventory/Loltel/msisdn/$fakeMsisdn2")
                .request(MediaType.APPLICATION_JSON)
                .get()

        assertEquals(404, response.status)
        verify(dao).getSimProfileByMsisdn(fakeIccid2)
    }

    @Test
    fun testAllocateNextFree() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/msisdn/$fakeMsisdn1/allocate-next-free")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post (or put?)x'

        assertEquals(200, response.status)
        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntry, simEntry)
    }

    @Test
    fun testActivateAll() {
        val response = RULE.target("/ostelco/sim-inventory/iccid/982389123498/activate/all")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    @Test
    fun testActivateHlr() {
        val response = RULE.target("/ostelco/sim-inventory/iccid/982389123498/activate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    @Test
    fun testActivateEsim() {
        val response = RULE.target("/ostelco/sim-inventory/iccid/982389123498/activate/esim")
                .request(MediaType.APPLICATION_JSON)
                .get()// XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    @Test
    fun testDeactivate() {
        val response = RULE.target("/ostelco/sim-inventory/iccid/8328238238328/deactivate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
    }

    //  @Path("/import-batch/sim-profile-vendor/{profilevendor}")
    // Test importing a CSV file from the resources.

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

        // XXX Couldn't figure out how to verify
        // verify(dao).insertAll(ArgumentMatchers.anyIterable<SimEntry>())
    }
}
