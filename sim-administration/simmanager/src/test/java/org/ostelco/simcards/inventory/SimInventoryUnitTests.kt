package org.ostelco.simcards.inventory

import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.*
import org.mockito.Mockito.*
import org.ostelco.simcards.adapter.HssEntry
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import java.io.ByteArrayInputStream
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class SimInventoryUnitTests {

    companion object {
        private val config = mock(SimAdministrationConfiguration::class.java)
        private val hssConfig = mock(HssConfig::class.java)
        private val profileVendorConfig = mock(ProfileVendorConfig::class.java)
        private val dao = mock(SimInventoryDAO::class.java)
        private val hssEntry = mock(HssEntry::class.java)
        private val profileVendorAdapter = mock(ProfileVendorAdapter::class.java)
        private val httpClient = mock(CloseableHttpClient::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SimInventoryResource(httpClient, config, dao))
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

    private val fakeProfileVendor = "Foo"
    private val fakeHlr = "Bar"
    private val fakePhoneType = "_"
    private val fakeProfile = "PROFILE_1"

    private val fakeSimEntryWithoutMsisdn = SimEntry(
            id = 1L,
            profileVendorId = 1L,
            hssId = 1L,
            msisdn = "",
            eid = "",
            profile = fakeProfile,
            hssState = HssState.NOT_ACTIVATED,
            smdpPlusState = SmDpPlusState.AVAILABLE,
            batch = 99L,
            imsi = fakeImsi1,
            iccid = fakeIccid1)

    private val fakeSimEntryWithMsisdn = fakeSimEntryWithoutMsisdn.copy(
            msisdn = fakeMsisdn1,
            eid = fakeEid,
            hssState = HssState.ACTIVATED,
            smdpPlusState = SmDpPlusState.RELEASED
    )


    @Before
    fun setUp() {
        reset(dao)
        reset(hssEntry)
        reset(profileVendorAdapter)

        /* HssConfig */
        org.mockito.Mockito.`when`(hssConfig.name)
                .thenReturn(fakeHlr)
        org.mockito.Mockito.`when`(hssConfig.endpoint)
                .thenReturn("http://localhost:8080/nowhere")

        /* ProfileVendorConfig */
        org.mockito.Mockito.`when`(profileVendorConfig.name)
                .thenReturn(fakeProfileVendor)
        org.mockito.Mockito.`when`(profileVendorConfig.es2plusEndpoint)
                .thenReturn("http://localhost:8080/somewhere")

        /* Top level config. */
        org.mockito.Mockito.`when`(config.hssVendors)
                .thenReturn(listOf(hssConfig))
        org.mockito.Mockito.`when`(config.profileVendors)
                .thenReturn(listOf(profileVendorConfig))
        org.mockito.Mockito.`when`(config.getProfileForPhoneType(fakePhoneType))
                .thenReturn(fakeProfile)

        /* HLR adapter. */
        org.mockito.Mockito.`when`(hssEntry.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(hssEntry.name)
                .thenReturn(fakeHlr)


        /* Profile vendor adapter. */
        org.mockito.Mockito.`when`(profileVendorAdapter.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(profileVendorAdapter.name)
                .thenReturn(fakeProfileVendor)
        org.mockito.Mockito.`when`(profileVendorAdapter.activate(httpClient, profileVendorConfig, dao, null, fakeSimEntryWithoutMsisdn))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        smdpPlusState = SmDpPlusState.RELEASED))

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

        org.mockito.Mockito.`when`(dao.findNextNonProvisionedSimProfileForHlr(1L, fakeProfile))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.findNextReadyToUseSimProfileForHlr(1L, fakeProfile))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(dao.getHssEntryByName(fakeHlr))
                .thenReturn(hssEntry)

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterByName(fakeProfileVendor))
                .thenReturn(profileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterById(1L))
                .thenReturn(profileVendorAdapter)

        org.mockito.Mockito.`when`(dao.getHssEntryByName(fakeHlr))
                .thenReturn(hssEntry)

        org.mockito.Mockito.`when`(dao.getHssEntryById(1L))
                .thenReturn(hssEntry)

        org.mockito.Mockito.`when`(dao.setHlrState(fakeSimEntryWithoutMsisdn.id!!, HssState.ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hssState = HssState.ACTIVATED))

        org.mockito.Mockito.`when`(dao.setHlrState(fakeSimEntryWithoutMsisdn.id!!, HssState.NOT_ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hssState = HssState.NOT_ACTIVATED))

        org.mockito.Mockito.`when`(dao.setSmDpPlusState(fakeSimEntryWithoutMsisdn.id!!, SmDpPlusState.RELEASED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        smdpPlusState = SmDpPlusState.RELEASED))

        org.mockito.Mockito.`when`(dao.setProvisionState(1L, ProvisionState.PROVISIONED))
                .thenReturn(fakeSimEntryWithoutMsisdn)

        org.mockito.Mockito.`when`(config.getProfileForPhoneType(fakePhoneType))
                .thenReturn(fakeProfile)
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
    fun testActivateEsim() {
        val response = RULE.target("/ostelco/sim-inventory/$fakeHlr/esim")
                .request(MediaType.APPLICATION_JSON)
                .get()
        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertNotNull(simEntry)

        verify(dao).getHssEntryByName(fakeHlr)
        verify(config).getProfileForPhoneType(fakePhoneType)
        verify(dao).findNextReadyToUseSimProfileForHlr(hssEntry.id, fakeProfile)
        verify(dao).setProvisionState(simEntry.id!!, ProvisionState.PROVISIONED)
    }


    @Test
    @Ignore
    fun testImport() {
        org.mockito.Mockito.`when`(dao.findSimVendorForHlrPermissions(1L, 1L))
                .thenReturn(listOf(0L))
        org.mockito.Mockito.`when`(dao.simVendorIsPermittedForHlr(1L, 1L))
                .thenReturn(true)

        val sampleCsvIinput = """
            ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE
            123123, 123123, 4790000001, 1233, 1233, 1233, 1233, PROFILE_1
            123123, 123123, 4790000002, 1233, 1233, 1233, 1233, PROFILE_1
            123123, 123123, 4790000003, 1233, 1233, 1233, 1233, PROFILE_1
            123123, 123123, 4790000004, 1233, 1233, 1233, 1233, PROFILE_1
            """.trimIndent()
        val data = ByteArrayInputStream(sampleCsvIinput.toByteArray(Charsets.UTF_8))

        // XXX For some reason this mock fails to match...
        org.mockito.Mockito.`when`(dao.importSims("importer", 1L, 1L, data))
                .thenReturn(SimImportBatch(
                        id = 0L,
                        status = "SUCCESS",
                        size = 4L,
                        hssId = 1L,
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
