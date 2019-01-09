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
    private val fakeImsi1 = "12345678912345"
    private val fakeImsi2 = "12345678912346"
    private val fakeMsisdn1 = "474747474747"
    private val fakeMsisdn2 = "464646464646"

    private val fakeProfileVendor = "Idemia"
    private val fakeHlr = "Loltel"

    private val fakeSimEntryWithoutMsisdn = SimEntry(
            id = 1L,
            profileVendorId = 1L,
            hlrId = 1L,
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
    private val fakeSimEntryWithMsisdn = SimEntry(
            id = 1L,
            profileVendorId = 1L,
            hlrId = 1L,
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

    @Before
    fun setUp() {
        reset(dao)

        val mockHlrAdapter = HlrAdapter(
                id = 1L,
                name = fakeHlr)
        val mockProfileVendorAdapter = ProfileVendorAdapter(
                id = 1L,
                name = fakeProfileVendor)

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

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

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterByName(fakeProfileVendor))
                .thenReturn(mockProfileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterById(1L))
                .thenReturn(mockProfileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getHlrAdapterByName(fakeHlr))
                .thenReturn(mockHlrAdapter)

        org.mockito.Mockito.`when`(dao.getHlrAdapterById(1L))
                .thenReturn(mockHlrAdapter)
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
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/imsi/$fakeImsi1")
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
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1/activate/all")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
    }

    @Test
    fun testActivateHlr() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1/activate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)

        verify(dao).getSimProfileById(fakeSimEntryWithoutMsisdn.id!!)
        // XXX Bunch of verifications missing
    }

    @Test
    fun testActivateEsimSuccessfully() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1/activate/esim")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        // XXX Missing a bunch of verifications
    }

    @Test
    fun testDeactivate() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1/deactivate/hlr")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post
        // XXX Check what return value to expect when updating, don't think it's 200
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
    }

    @Test
    fun testImport() {
        org.mockito.Mockito.`when`(dao.getBatchInfo(0))
                .thenReturn(SimImportBatch(
                        id = 0L,
                        status = "SUCCESS",
                        size = 4L,
                        hlrId = 1L,
                        profileVendorId = 1L,
                        importer = "Testroutine",
                        endedAt = 999L))
        org.mockito.Mockito.`when`(dao.findSimVendorForHlrPermissions(1L, 1L))
                .thenReturn(listOf(0L))

        val sampleCsvIinput = """
            ICCID, IMSI, PIN1, PIN2, PUK1, PUK2
            123123, 123123, 1233,1233,1233,1233
            123123, 123123, 1233,1233,1233,1233
            123123, 123123, 1233,1233,1233,1233
            123123, 123123, 1233,1233,1233,1233
            """.trimIndent()
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/import-batch/profilevendor/$fakeProfileVendor")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
        assertNotNull(simEntry)
    }
}
