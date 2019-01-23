package org.ostelco.simcards.inventory

import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.*
import org.mockito.Mockito.*
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import org.ostelco.simcards.admin.ProfileVendorConfig
import java.io.ByteArrayInputStream
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class SimInventoryUnitTests {

    companion object {
        private val config = mock(SimAdministrationConfiguration::class.java)
        private val hlrConfig = mock(HlrConfig::class.java)
        private val profileVendorConfig = mock(ProfileVendorConfig::class.java)
        private val dao = mock(SimInventoryDAO::class.java)
        private val hlrAdapter = mock(HlrAdapter::class.java)
        private val profileVendorAdapter = mock(ProfileVendorAdapter::class.java)
        private val client = mock(Client::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SimInventoryResource(client, config, dao))
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
    private val fakeEid = "01010101010101010101010101010101"

    private val fakeProfileVendor = "Idemia"
    private val fakeHlr = "Loltel"

    private val fakeSimEntryWithoutMsisdn = SimEntry(
            id = 1L,
            profileVendorId = 1L,
            hlrId = 1L,
            msisdn = "",
            eid = "",
            hlrState = HlrState.NOT_ACTIVATED,
            smdpPlusState = SmDpPlusState.NOT_ACTIVATED,
            batch = 99L,
            imsi = fakeImsi1,
            iccid = fakeIccid1,
            pin1 = "0000",
            pin2 = "0000",
            puk1 = "0000",
            puk2 = "0000")

    private val fakeSimEntryWithMsisdn = fakeSimEntryWithoutMsisdn.copy(
            msisdn = fakeMsisdn1,
            eid = fakeEid,
            hlrState = HlrState.ACTIVATED,
            smdpPlusState = SmDpPlusState.ACTIVATED
    )


    @Before
    fun setUp() {
        reset(dao)
        reset(hlrAdapter)
        reset(profileVendorAdapter)

        /* HlrConfig */
        org.mockito.Mockito.`when`(hlrConfig.name)
                .thenReturn(fakeHlr)
        org.mockito.Mockito.`when`(hlrConfig.url)
                .thenReturn("http://localhost:8080/nowhere")

        /* ProfileVendorConfig */
        org.mockito.Mockito.`when`(profileVendorConfig.name)
                .thenReturn(fakeProfileVendor)
        org.mockito.Mockito.`when`(profileVendorConfig.url)
                .thenReturn("http://localhost:8080/somewhere")

        /* Top level config. */
        org.mockito.Mockito.`when`(config.hlrVendors)
                .thenReturn(listOf(hlrConfig))
        org.mockito.Mockito.`when`(config.profileVendors)
                .thenReturn(listOf(profileVendorConfig))

        /* HLR adapter. */
        org.mockito.Mockito.`when`(hlrAdapter.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(hlrAdapter.name)
                .thenReturn(fakeHlr)
        org.mockito.Mockito.`when`(hlrAdapter.activate(client, hlrConfig, dao, fakeSimEntryWithoutMsisdn))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hlrState = HlrState.ACTIVATED))
        org.mockito.Mockito.`when`(hlrAdapter.deactivate(client, hlrConfig, dao, fakeSimEntryWithoutMsisdn))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hlrState = HlrState.NOT_ACTIVATED))

        /* Profile vendor adapter. */
        org.mockito.Mockito.`when`(profileVendorAdapter.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(profileVendorAdapter.name)
                .thenReturn(fakeProfileVendor)
        org.mockito.Mockito.`when`(profileVendorAdapter.activate(client, profileVendorConfig, dao, fakeEid, fakeSimEntryWithoutMsisdn))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        smdpPlusState = SmDpPlusState.ACTIVATED,
                        eid = fakeEid))

        /* DAO. */
        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid1))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileById(fakeSimEntryWithoutMsisdn.id!!))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getSimProfileById(fakeSimEntryWithMsisdn.id!!))
                .thenReturn(fakeSimEntryWithMsisdn)

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

        org.mockito.Mockito.`when`(dao.findNextFreeSimProfileForHlr(1L))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.allocateNextFreeSimProfileForMsisdn(1L, fakeMsisdn1))
                .thenReturn(fakeSimEntryWithMsisdn)

        org.mockito.Mockito.`when`(dao.getHlrAdapterByName(fakeHlr))
                .thenReturn(hlrAdapter)

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterByName(fakeProfileVendor))
                .thenReturn(profileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterById(1L))
                .thenReturn(profileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getHlrAdapterByName(fakeHlr))
                .thenReturn(hlrAdapter)

        org.mockito.Mockito.`when`(dao.getHlrAdapterById(1L))
                .thenReturn(hlrAdapter)

        org.mockito.Mockito.`when`(dao.setHlrState(fakeSimEntryWithoutMsisdn.id!!, HlrState.ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hlrState = HlrState.ACTIVATED))

        org.mockito.Mockito.`when`(dao.setHlrState(fakeSimEntryWithoutMsisdn.id!!, HlrState.NOT_ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hlrState = HlrState.NOT_ACTIVATED))

        org.mockito.Mockito.`when`(dao.setSmDpPlusState(fakeSimEntryWithoutMsisdn.id!!, SmDpPlusState.ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        smdpPlusState = SmDpPlusState.ACTIVATED))
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
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/msisdn/$fakeMsisdn1/next-free")
                .request(MediaType.APPLICATION_JSON)
                .get() // XXX Post (or put?)x'
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
        assertEquals(fakeSimEntryWithMsisdn, simEntry)
    }

    @Test
    fun testActivateAll() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/esim/$fakeEid/all")
                .queryParam("iccid", fakeIccid1)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null))
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
    }

    @Test
    fun testActivateEsim() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/esim/${fakeEid}")
                .queryParam("iccid", fakeIccid1)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null))
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)

        verify(dao).getHlrAdapterByName(fakeHlr)
        verify(dao).getSimProfileByIccid(fakeSimEntryWithoutMsisdn.iccid)
        verify(dao).getProfileVendorAdapterById(fakeSimEntryWithoutMsisdn.profileVendorId)

        verify(profileVendorAdapter).activate(client, profileVendorConfig, dao, fakeEid, fakeSimEntryWithoutMsisdn)
        // XXX Missing a bunch of verifications
    }

    @Test
    fun testActivateHlr() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null))
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)

        verify(dao).getSimProfileByIccid(fakeSimEntryWithoutMsisdn.iccid)
        verify(dao).getHlrAdapterById(fakeSimEntryWithoutMsisdn.hlrId)

        verify(hlrAdapter).activate(client, hlrConfig, dao, fakeSimEntryWithoutMsisdn)
        // XXX Bunch of verifications missing
    }

    @Test
    fun testDeactivateHlr() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/iccid/$fakeIccid1")
                .request(MediaType.APPLICATION_JSON)
                .delete()
        // XXX Check what return value to expect when updating, don't think it's 200
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)
    }

    @Test
    @Ignore
    fun testImport() {
        org.mockito.Mockito.`when`(dao.findSimVendorForHlrPermissions(1L, 1L))
                .thenReturn(listOf(0L))
        org.mockito.Mockito.`when`(dao.simVendorIsPermittedForHlr(1L, 1L))
                .thenReturn(true)

        val sampleCsvIinput = """
            ICCID, IMSI, PIN1, PIN2, PUK1, PUK2
            123123, 123123, 1233, 1233, 1233, 1233
            123123, 123123, 1233, 1233, 1233, 1233
            123123, 123123, 1233, 1233, 1233, 1233
            123123, 123123, 1233, 1233, 1233, 1233
            """.trimIndent()
        val data = ByteArrayInputStream(sampleCsvIinput.toByteArray(Charsets.UTF_8))

        // XXX For some reason this mock fails to match...
        org.mockito.Mockito.`when`(dao.importSims("importer", 1L, 1L, data))
                .thenReturn(SimImportBatch(
                        id = 0L,
                        status = "SUCCESS",
                        size = 4L,
                        hlrId = 1L,
                        profileVendorId = 1L,
                        importer = "Testroutine",
                        endedAt = 999L))

        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/import-batch/profilevendor/$fakeProfileVendor")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
        assertNotNull(simEntry)
    }
}
