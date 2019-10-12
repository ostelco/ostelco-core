package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.AfterClass
import org.junit.Before
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import org.ostelco.simcards.admin.SwtHssConfig
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.profilevendors.ProfileVendorAdapterDatum
import java.io.InputStream
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

@Ignore("Fails to Mock config classes which are Kotlin Data classes")
class SimInventoryUnitTests {

    companion object {
        // TODO why mock data/pojo classes?
        private val config = mock(SimAdministrationConfiguration::class.java)
        private val hssConfig = mock(SwtHssConfig::class.java)
        private val profileVendorConfig = mock(ProfileVendorConfig::class.java)

        private val dao = mock(SimInventoryDAO::class.java)
        private val hssEntry = mock(HssEntry::class.java)
        private val profileVendorAdapterDatum = mock(ProfileVendorAdapterDatum::class.java)
        private val httpClient = mock(CloseableHttpClient::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SimInventoryResource(SimInventoryApi(httpClient, config, dao)))
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

    private val matchingId = UUID.randomUUID().toString()
    private val es9plusEndpoint = "http://localhost:8080/farFarAway"

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
            iccid = fakeIccid1,
            matchingId = matchingId,
            code = "LPA:1$$es9plusEndpoint$$matchingId")

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
        reset(profileVendorAdapterDatum)

        /* HssConfig */
        org.mockito.Mockito.`when`(hssConfig.name)
                .thenReturn(fakeHlr)
        org.mockito.Mockito.`when`(hssConfig.endpoint)
                .thenReturn("http://localhost:8080/nowhere")

        /* ProfileVendorConfig */
        org.mockito.Mockito.`when`(profileVendorConfig.name)
                .thenReturn(fakeProfileVendor)
        org.mockito.Mockito.`when`(profileVendorConfig.getEndpoint())
                .thenReturn("http://localhost:8080")
        org.mockito.Mockito.`when`(profileVendorConfig.es9plusEndpoint)
                .thenReturn(es9plusEndpoint)

        /* Top level config. */
        org.mockito.Mockito.`when`(config.hssVendors)
                .thenReturn(listOf(hssConfig))
        org.mockito.Mockito.`when`(config.profileVendors)
                .thenReturn(listOf(profileVendorConfig))
        org.mockito.Mockito.`when`(config.getProfileForPhoneType(fakePhoneType))
                .thenReturn(fakeProfile)

        /* HLR profilevendors. */
        org.mockito.Mockito.`when`(hssEntry.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(hssEntry.name)
                .thenReturn(fakeHlr)


        /* Profile vendor profilevendors. */
        org.mockito.Mockito.`when`(profileVendorAdapterDatum.id)
                .thenReturn(1L)
        org.mockito.Mockito.`when`(profileVendorAdapterDatum.name)
                .thenReturn(fakeProfileVendor)

        /* DAO. */
        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid1))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

        org.mockito.Mockito.`when`(dao.getSimProfileById(fakeSimEntryWithoutMsisdn.id!!))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

        org.mockito.Mockito.`when`(dao.getSimProfileById(fakeSimEntryWithMsisdn.id!!))
                .thenReturn(fakeSimEntryWithMsisdn.right())

        org.mockito.Mockito.`when`(dao.getSimProfileByIccid(fakeIccid2))
                .thenReturn(NotFoundError("").left())

        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi1))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

        org.mockito.Mockito.`when`(dao.getSimProfileByImsi(fakeImsi2))
                .thenReturn(NotFoundError("").left())

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn1))
                .thenReturn(fakeSimEntryWithMsisdn.right())

        org.mockito.Mockito.`when`(dao.getSimProfileByMsisdn(fakeMsisdn2))
                .thenReturn(NotFoundError("").left())

        org.mockito.Mockito.`when`(dao.findNextNonProvisionedSimProfileForHss(1L, fakeProfile))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

        org.mockito.Mockito.`when`(dao.findNextReadyToUseSimProfileForHss(1L, fakeProfile))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

        org.mockito.Mockito.`when`(dao.getHssEntryByName(fakeHlr))
                .thenReturn(Either.Right(hssEntry))

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterDatumByName(fakeProfileVendor))
                .thenReturn(profileVendorAdapterDatum.right())

        org.mockito.Mockito.`when`(dao.getProfileVendorAdapterDatumById(1L))
                .thenReturn(profileVendorAdapterDatum.right())

        org.mockito.Mockito.`when`(dao.getHssEntryByName(fakeHlr))
                .thenReturn(Either.Right(hssEntry))

        org.mockito.Mockito.`when`(dao.getHssEntryById(1L))
                .thenReturn(Either.Right(hssEntry))

        org.mockito.Mockito.`when`(dao.setHssState(fakeSimEntryWithoutMsisdn.id!!, HssState.ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hssState = HssState.ACTIVATED).right())

        org.mockito.Mockito.`when`(dao.setHssState(fakeSimEntryWithoutMsisdn.id!!, HssState.NOT_ACTIVATED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        hssState = HssState.NOT_ACTIVATED).right())

        org.mockito.Mockito.`when`(dao.setSmDpPlusState(fakeSimEntryWithoutMsisdn.id!!, SmDpPlusState.RELEASED))
                .thenReturn(fakeSimEntryWithoutMsisdn.copy(
                        smdpPlusState = SmDpPlusState.RELEASED).right())

        org.mockito.Mockito.`when`(dao.setProvisionState(1L, ProvisionState.PROVISIONED))
                .thenReturn(fakeSimEntryWithoutMsisdn.right())

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
        verify(dao).getProfileVendorAdapterDatumById(fakeSimEntryWithoutMsisdn.profileVendorId)
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
        verify(dao).findNextReadyToUseSimProfileForHss(hssEntry.id, fakeProfile)
        verify(dao).setProvisionState(simEntry.id!!, ProvisionState.PROVISIONED)
    }

    private fun importSimBatch(hssState: HssState? = null): SimImportBatch {
        val sampleCsvIinput = """
                ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE
                123123, 123123, 4790000001, 1233, 1233, 1233, 1233, PROFILE_1
                123123, 123123, 4790000002, 1233, 1233, 1233, 1233, PROFILE_1
                123123, 123123, 4790000003, 1233, 1233, 1233, 1233, PROFILE_1
                123123, 123123, 4790000004, 1233, 1233, 1233, 1233, PROFILE_1
                """.trimIndent()

        val urlPath = "/ostelco/sim-inventory/$fakeHlr/import-batch/profilevendor/${fakeProfileVendor}"

        var target = RULE.target(urlPath)

        if (hssState != null) {
            target = target.queryParam("initialHssState", hssState)
        }

        val response = target
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))
        assertEquals(200, response.status)

        return response.readEntity(SimImportBatch::class.java)
    }

    private fun setUpMocksForMockedOutImportSims(initialHssState:HssState = HssState.NOT_ACTIVATED) {
        Mockito.`when`(dao.findSimVendorForHssPermissions(1L, 1L))
                .thenReturn(listOf(0L).right())
        Mockito.`when`(dao.simVendorIsPermittedForHlr(1L, 1L))
                .thenReturn(true.right())
        Mockito.`when`(dao.importSims(KotlinMockitoHelpers.eq("importer"), KotlinMockitoHelpers.eq(1L),
                KotlinMockitoHelpers.eq(1L), KotlinMockitoHelpers.any(InputStream::class.java),
                KotlinMockitoHelpers.eq(initialHssState)))
                .thenReturn(SimImportBatch(
                        id = 0L,
                        status = "SUCCESS",
                        size = 4L,
                        hssId = 1L,
                        profileVendorId = 1L,
                        importer = "Testroutine",
                        endedAt = 999L).right())
    }

    @Test
    fun testMockedOutImportSims() {
        setUpMocksForMockedOutImportSims()
        importSimBatch()
    }

    @Test
    fun testMockedOutImportSimsWithNonActicatedHssStatusSet() {
        setUpMocksForMockedOutImportSims()
        importSimBatch(HssState.NOT_ACTIVATED)
    }

    @Test
    fun testMockedOutImportSimsWithActivatedHssStatusSet() {
        setUpMocksForMockedOutImportSims(HssState.ACTIVATED)
        importSimBatch(HssState.ACTIVATED)
    }
}
