package org.ostelco.prime.ekyc.myinfo.v3

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.ext.myinfo.MyInfoEmulatorApp
import org.ostelco.ext.myinfo.MyInfoEmulatorConfig
import org.ostelco.prime.ekyc.ConfigRegistry
import org.ostelco.prime.ekyc.MyInfoV3Config
import org.ostelco.prime.ekyc.Registry
import org.ostelco.prime.getLogger
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Ref: https://www.ndi-api.gov.sg/assets/lib/trusted-data/myinfo/specs/myinfo-kyc-v3.0.2.html#section/Environments
 *
 *  https://{ENV_DOMAIN_NAME}/{VERSION}/{RESOURCE}
 *
 * ENV_DOMAIN_NAME:
 *  - Sandbox/Dev: https://sandbox.api.myinfo.gov.sg/com/v3
 *  - Staging: https://test.api.myinfo.gov.sg/com/v3
 *  - Production: https://api.myinfo.gov.sg/com/v3
 *
 * VERSION: `/v2`
 */
// Using certs from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
private val templateTestConfig = File("src/test/resources/stg-demoapp-client-privatekey-2018.pem")
        .readText()
        .replace("\n","")
        .removePrefix("-----BEGIN PRIVATE KEY-----")
        .removeSuffix("-----END PRIVATE KEY-----")
        .let { base64Encoded -> PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Encoded)) }
        .let { keySpec -> KeyFactory.getInstance("RSA").generatePrivate(keySpec) }
        .let { clientPrivateKey: PrivateKey ->
            MyInfoV3Config(
                    myInfoApiUri = "https://test.api.myinfo.gov.sg/com/v3",
                    myInfoApiClientId = "STG2-MYINFO-SELF-TEST",
                    myInfoApiClientSecret = "44d953c796cccebcec9bdc826852857ab412fbe2",
                    myInfoRedirectUri = "http://localhost:3001/callback",
                    myInfoPersonDataAttributes = "name,sex,race,nationality,dob,email,mobileno,regadd,housingtype,hdbtype,marital,edulevel,ownerprivate,cpfcontributions,cpfbalances",
                    myInfoServerPublicKey = "",
                    myInfoClientPrivateKey = Base64.getEncoder().encodeToString(clientPrivateKey.encoded)
            )
        }

class MyInfoClientTest {

    private val logger by getLogger()

    @Before
    fun setupUnitTest() {
        ConfigRegistry.myInfoV3 = templateTestConfig.copy(
                myInfoApiUri = "http://localhost:8080",
                myInfoServerPublicKey = Base64.getEncoder().encodeToString(myInfoServerKeyPair.public.encoded))

        Registry.myInfoClient = HttpClientBuilder.create().build()
    }

    /**
     * This test to send request to real staging server of MyInfo API.
     *
     * Some setup is needed to run this test.
     *
     * 1. Checkout the forked repo: https://github.com/ostelco/myinfo-demo-app
     * 2. npm install
     * 3. ./start.sh
     * 4. Open web browser with Developer console open.
     * 5. Goto http://localhost:3001
     * 6. Click button "RETRIEVE INFO"
     * 7. Login to SingPass using username: S9812381D and password: MyInfo2o15
     * 8. Click on "Accept" button.
     * 9. Copy authorisationCode from developer console of web browser.
     * 10. Use this value in `test myInfo client` test.
     * 11. Set @Before annotation on 'setupRealStaging()' instead of 'setupUnitTest()'.
     *
     */
    // @Before
    fun setupRealStaging() {
        // Using certs from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
        // server public key
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate= certificateFactory.generateCertificate(FileInputStream("src/test/resources/stg-auth-signing-public.pem"))

        ConfigRegistry.myInfoV3 = templateTestConfig.copy(
                myInfoPersonDataAttributes = "name,sex,race,nationality,dob,email,mobileno,regadd,housingtype,hdbtype,marital,edulevel,assessableincome,ownerprivate,assessyear,cpfcontributions,cpfbalances",
                myInfoServerPublicKey = Base64.getEncoder().encodeToString(certificate.publicKey.encoded))

        Registry.myInfoClient = HttpClientBuilder.create().build()
    }

    @Test
    fun `test myInfo client`() {
        val durationInMilliSec = measureTimeMillis {
            val myInfoData = MyInfoClientSingleton.getPersonData(authorisationCode = "authorisation-code")
            logger.info("MyInfo - UIN/FIN: {}", myInfoData?.uinFin)
            logger.info("MyInfo - PersonData: {}", myInfoData?.personData)
        }
        logger.info("Time taken to fetch personData: {} sec", durationInMilliSec / 1000)
    }

    companion object {

        val myInfoServerKeyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
                .apply { this.initialize(2048) }
                .genKeyPair()

        @JvmStatic
        val SUPPORT: DropwizardTestSupport<MyInfoEmulatorConfig> = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(FileInputStream("src/test/resources/stg-demoapp-client-publiccert-2018.pem"))
                .let { certificate ->
                    DropwizardTestSupport<MyInfoEmulatorConfig>(
                            MyInfoEmulatorApp::class.java,
                            ResourceHelpers.resourceFilePath("myinfo-emulator-config.yaml"),
                            ConfigOverride.config("myInfoApiClientId", templateTestConfig.myInfoApiClientId),
                            ConfigOverride.config("myInfoApiClientSecret", templateTestConfig.myInfoApiClientSecret),
                            ConfigOverride.config("myInfoRedirectUri", templateTestConfig.myInfoRedirectUri),
                            ConfigOverride.config("myInfoServerPublicKey", Base64.getEncoder().encodeToString(myInfoServerKeyPair.public.encoded)),
                            ConfigOverride.config("myInfoServerPrivateKey", Base64.getEncoder().encodeToString(myInfoServerKeyPair.private.encoded)),
                            ConfigOverride.config("myInfoClientPublicKey", Base64.getEncoder().encodeToString(certificate.publicKey.encoded)))
                }

        @JvmStatic
        @BeforeClass
        fun beforeClass() = SUPPORT.before()

        @JvmStatic
        @AfterClass
        fun afterClass() = SUPPORT.after()
    }
}

class RSAKeyTest {

    @Test
    fun `test encode and decode`() {
        val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
                .apply { this.initialize(2048) }
                .genKeyPair()

        val encodedPublicKey = keyPair.public.encoded

        val base64PublicKey = Base64.getEncoder().encodeToString(encodedPublicKey)
        val decodedPublicKey = Base64.getDecoder().decode(base64PublicKey)

        assertArrayEquals(encodedPublicKey, decodedPublicKey)

        assertEquals(keyPair.public, KeyFactory
                .getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64
                        .getDecoder()
                        .decode(base64PublicKey))))

        val encodedPrivateKey = keyPair.private.encoded
        val base64PrivateKey = Base64.getEncoder().encodeToString(encodedPrivateKey)
        val decodedPrivateKey = Base64.getDecoder().decode(base64PrivateKey)

        assertArrayEquals(encodedPrivateKey, decodedPrivateKey)

        assertEquals(keyPair.private, KeyFactory
                .getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(Base64
                        .getDecoder()
                        .decode(base64PrivateKey))))
    }

    @Test
    fun `test loading MyInfo Staging Key`() {

        // Using public cert from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate= certificateFactory.generateCertificate(FileInputStream("src/test/resources/stg-auth-signing-public.pem"))
        certificate.publicKey
    }

    @Test
    fun `test loading MyInfo Staging client private key`() {

        // Using cert from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
        val base64Encoded = File("src/test/resources/stg-demoapp-client-privatekey-2018.pem")
                .readText()
                .replace("\n","")
                .removePrefix("-----BEGIN PRIVATE KEY-----")
                .removeSuffix("-----END PRIVATE KEY-----")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Encoded))
        val clientPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    @Test
    fun `test MyInfo Staging client public key`() {

        // Using public cert from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate= certificateFactory.generateCertificate(FileInputStream("src/test/resources/stg-demoapp-client-publiccert-2018.pem"))
        certificate.publicKey
    }
}