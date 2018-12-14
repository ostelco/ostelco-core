package org.ostelco.simcards.inventory
import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.AfterClass
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class SimInventoryUnitTests {

    companion object {

        private val dao = mock(SimInventoryDAO::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SimInventoryResource(dao))
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {
        }
    }

    private val fakeIccid1 = "01234567891234567890"
    private val fakeIccid2 = "01234567891234567891"
    private val fakeIccid3 = "01234567891234567892"
    private val fakeImsi1 = "12345678912345"
    private val fakeImsi2 = "12345678912346"
    private val fakeMsisdn1 = "474747474747"
    private val fakeMsisdn2 = "464646464646"
    private val fakeHlr = "Loltel"

    private fun fakeEntryWithoutMsisdn() : SimEntry {
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

    private fun fakeEntryWithoutMsisdnAndSmdpplus() : SimEntry {
        return SimEntry(
                id = 1L,
                hlrId = fakeHlr,
                smdpplus = null,
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

    private fun fakeEntryWithMsisdn() : SimEntry {
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


    private var fakeSimEntryWithoutMsisdn = fakeEntryWithoutMsisdn()

    private var fakeSimEntryWithMsisdn = fakeEntryWithMsisdn()

    private var fakeEnrtryWithoutMsisdnAndSmdpplus = fakeEntryWithoutMsisdnAndSmdpplus()


    @Before
    fun setUp() {

        reset(dao)

        this.fakeSimEntryWithoutMsisdn = fakeEntryWithoutMsisdn()
        this.fakeSimEntryWithMsisdn = fakeEntryWithMsisdn()
        this.fakeEnrtryWithoutMsisdnAndSmdpplus = fakeEntryWithoutMsisdnAndSmdpplus()


        val mockHlrAdapter = HlrAdapter(1L, fakeHlr)
        val mockSmdpplusAdapter = SmdpPlusAdapter(1L, "Loltel")

        val idemiaProfileVendor = SimProfileVendor(id = 0L, name = "Idemia")

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

        org.mockito.Mockito.`when`(dao.getProfilevendorByName("Idemia"))
                .thenReturn(idemiaProfileVendor)

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

        org.mockito.Mockito.`when`(dao.getBatchInfo(0))
                .thenReturn(SimImportBatch(id = 0L, status = "SUCCESS", size = 4L, hlr = "Loltel", profileVendor = "Idemia", importer = "Testroutine", endedAt = 999L))


        org.mockito.Mockito.`when`(dao.findSimVendorForHlrPermissions(0L, 1L))
                .thenReturn(listOf(0L))


        val sampleCsvIinput =
                """
        ICCID, IMSI, PIN1, PIN2, PUK1, PUK2
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
       123123, 123123, 1233,1233,1233,1233
    """.trimIndent()

        val response = RULE.target("/ostelco/sim-inventory/Loltel/import-batch")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
    }
}
