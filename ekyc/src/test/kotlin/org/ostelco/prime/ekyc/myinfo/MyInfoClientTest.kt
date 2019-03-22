package org.ostelco.prime.ekyc.myinfo

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
import org.ostelco.prime.ekyc.Config
import org.ostelco.prime.ekyc.ConfigRegistry
import org.ostelco.prime.ekyc.Registry
import org.ostelco.prime.ekyc.TestConfig
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

class MyInfoClientTest {

    @Before
    fun setupUnitTest() {
        // Using certs from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
        // client private key
        val base64Encoded = String(File("src/test/resources/stg-demoapp-client-privatekey-2018.pem").readBytes())
                .replace("\n","")
                .removePrefix("-----BEGIN PRIVATE KEY-----")
                .removeSuffix("-----END PRIVATE KEY-----")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Encoded))
        val clientPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

        ConfigRegistry.config = Config(
                myInfoApiUri = "http://localhost:8080",
                myInfoServerPublicKey = Base64.getEncoder().encodeToString(myInfoServerKeyPair.public.encoded),
                myInfoClientPrivateKey = Base64.getEncoder().encodeToString(clientPrivateKey.encoded))
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

        // client private key
        val base64Encoded = String(File("src/test/resources/stg-demoapp-client-privatekey-2018.pem").readBytes())
                .replace("\n","")
                .removePrefix("-----BEGIN PRIVATE KEY-----")
                .removeSuffix("-----END PRIVATE KEY-----")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Encoded))
        val clientPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

        ConfigRegistry.config = Config(
                myInfoServerPublicKey = Base64.getEncoder().encodeToString(certificate.publicKey.encoded),
                myInfoClientPrivateKey = Base64.getEncoder().encodeToString(clientPrivateKey.encoded))
        Registry.myInfoClient = HttpClientBuilder.create().build()
    }

    @Test
    fun `test myInfo client`() {
        println(MyInfoClientSingleton.getPersonData(authorisationCode = "activation-code"))
    }

    @Test
    fun `decode person data`() {
        val data = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAiLCJraWQiOiJlbmNyeXB0S2V5In0.nIc2yf-RKcnhKU4bJd-1XaSITQjurVt1ZDpGDeqiCH_XaV5mIBu2KLJRYKmo_ey6-XGL8pcm6nXynK0Y1CeWCUn3B8Pcn8Z-doJ0cIjXI8V_Dy5TyoyWCZB7tRgtMhpn93bCgsANx9lCRI6-TEYdu6U-sDki5VL6LgBJd8UeFCG9PHtKA9k3bGGQJSVRWPwq83iGe8XUhUAnZNarRgBMZe8IQbf78-YgeZw-bCzs_K4X1EOBW0ISJjj1yGts7N2uK2aQBLSw40UpWZdj_y2qwBtR5RKBMVXEH0zQBF7X9k_1diA047vFxQSgPS9M0Z46McoFmMku86SFxDvMZzE_Uw.6chXPc1zkWXeOvCg.pDKm8kbXcuatHt5iixt_FsuC87utIkG_Xec29WMv0vq9Y4JOflAwtBLcjuOIHV9ayPI70LVS7nGJMjHOcMe2jAH7airvMXB5Rg2rrGwRas_SIew_ZUWM_8-fLdlRL_KMaW2NTrsMGqKLdLhgfjd3PNTL5y9lWqCImaywEUzIH_2sFv0LrzZVH-f0q7lp9f4WjFCvc_8PGmuuMZhdxICVDvF-Ya1bivS7q_oEV6reStVuYhfIhqfuLNpBE_xHBrREd15jshxd3qIXHWbp9a5eBNO4CJ92Aqfyci5d7LBVdkf4vgVz6RVNfYaxOH1vljhMQq8oK9lv2jyZzwJzdod_YULKyCmwLJDjaXtTU4xjgksQkkQ9nkxUHh7LTijlgl0JJisatp_X4aiNfNqbbtwYCq7tZJewTy693F8s6wdEU-a0hJqMJWr3gkJ0OVwPAkiqI1lm9-T0ZHIJyGLBxOJJKLLAoOTbbW3GLt1QAr1wJKrSTTsIEBkx9UWcZmlyo0_3_yX_q0CJBlsXfN8YcX5ayzcPvSq-3Vaz-D5kkM1khbmSgBeIP4s9Vjq6Eqg_AHvPlb3qd-_Aq4N1lJ8oMPwyNMOPj8ZmDa8h8BpvuEUrBc9nw_yyTyBX21q0jAxqA5_SDYKfk35XRtqRo3c0_I_rp0nmYiVtdnsEVJuYowsCbV-UX6r-MmfojKA1vQaO-lcOUAW_BtVWRuRhkN6k6qmmEMJpxW2tbxTKN0dws_cMEaHZas0tZsBcsiFjFmLBGuexdxvpyTDiNdgiuNpcc1GCRPzRRcPtyCyRlDQLTEv4pFzmJ4Y1Gp85f7VeqebxzOkz7qudxF82eXHAO4vP-5d3pEn9YeQrGY0nU6NZrLRM2ghYzCuK9Dh-ifTwHaHnybQLY4n3n9BaKfv3q4Y3QExJIYk41zCnJP25VMH6aFFlW4XEWACttcHKOE9qCpQpWKnvsLIyXr9L4IFU9ePAamOnvD0Ce0SwA3jUXNNbfTO8otpL_JNLAJ8HQSvR7nYGt1PPJlRJp3b0ffAB_hQKZ5UL1mijTrG5f0eSiTo40ooPT-sqt_ZD0803khG4-KQqOw7J7fV7GX3QOOT-uvMgfnO-VpPAWihI16jH2Ak-1lJpXAB5rHskhpl2AL-L4Tjz24zBiWirzo81H57g3kkGWdgsyCdLeus-jKIR0-0Dnukgm5JvKFFqMg7MLPWB9iBWzNb0mPGkn5tA6yx1y_kCatKUvAS6sZSs5lQ6-ti-sxreOnwUoHiKt3AmZ2DyJgVccUef1dPFJdJF8bTfp1cASd6NwbdxROxsrPJPccU-a03UXGZJEGwHn9P81vH6G9wOd9N9a7XyjdH3Zu6pJBYo_MTzxfRzfBodEg9ECCtYPkkWWvPBetLgrzYS_htOvpwhB_-ZNpPtj_AihxRhaMHAiwR6-S1oBL6LWb4g6W8-vDIVO6b6mtrsoFO1mRv4y779JcKT66Tbv0WkPD4OU9VvInLr6NuxRSE2fihDrXv6QyPb5YypyccD60MXftJurllC8pCO61uHWvup4qQSaBtnGNxkpa3PuVwWMH5yzz_M4ls1kP0FzBpttJ6M49oBAHvAaGCg-WPacLf0A6yETOL2LRgTMD3UXL42LzLNf6VcgqD8f9EQIuzEz90K3L2HoNrYofx7K3j8uXMVXqqXUvAhKHD60g6z9d7S9qkgoZv_RNH2ykskBn5wAYiSgbB_vVfQu_3T-gdfut-z6U44_6D5zVFoxtvVGXZOros5qSHJC--LnmEXaHDcBPe2ZmjmOhoxoq_daiHOQjyD5c4dvJHLo4L-1bBsv54C60qDFGHPFetBqDuZ_C0UfBgOrMqPeYw9yWIWGHg3nUnZW3bPR8uuS_1bo1_cja8Iu5vIpvaoS9YG7hAai_T7bACi5vqmKzvbr3A19x2mhOcsVM7OWaY1ggDzFo7nrnyA0fZUSHQb80v3alTMZX4qYoOCHXVmVvzrpFgFuyJXGPUjev2dlJhkKJ8fzmPqLlqVFbA0toQmyLdrHDIb6njZSQGh207iBLVUH-DFktzpTEEEfUpIZ8cbitI8xCGmWetR6TGOaY8qs2_u5YQwzbS8eGZyy-2k0U7R8TOXv24U2athsCFDZf8eUfgV_82uE2cqdHSRV27y3SzbUaGVhTq2b4oQ1wzeRkW3mqhGh4vbepF3CT4d8b6Dgeoa9_NpLk3i9t3Osg0qx9VL5wq2N_avzWjW36URfe2h6ZDhEFCP17yFNZG4B7Pd5QQhAQy8eEFt8VaEGJFDwXKgz_UOgJt2X_IWWZMBOgjWI9ahaWDNyCjhmGC7s_8crq1lqBu7s8JzZW6SpcQVA7oFeyoBuSqbM4S1lf_aYBzSv8_1E9_1lfXQR_T3pGgAITP5xQr1-1--RgKLIRINGyssob3FeB_aO13-GS4R0svK9yvW7jHEEjRZgeZvmtPcSE_q-b0qX8xWbaR6FJMK0puyJxOdrBWtW8pglGRGdWY2k6KrFES-lRdtcjAubijqiQ1OmrWzxJbnK0gkpNj7oFlAFR6GMjvBHWRjTD8ykTv7FA_B_SOYiDTLlV76ghyXZchBtMdWL5l2ehil4dY7cvB10BJH7CyTmYTl7UNEjgwbLRvmVewKYkGrrOeFF9jZHKwPWkqDjB5e3kfM6Wp4KMgKKngXpwhB_4wO1M4S3uybUJqdQ0b7hznX_7mexbMF0dke-O0yynqqsghyvlqLboxZsfAYo6uU5Nvnt12SRJF5OmiWd2oY7RT-i-9rN-rL01tujuY6SCL2ytDo86vsu_eCg1ktV6uhUHdSNGiBHhHOl58tenFnkLGwbf_p2WeOFbYk0DGGru4QwUibY2cjVvzPHQU9po9BVf8aNd23ODLsPRJ9_AKeXjxU5G2v0mDho_7DfiPXukkHKu5qvIfGmDzKiYLbzTmx3VtPUWHDAVWHGsPjsFgprX1vGuFwDlRU2DNKJJUPBoxfcA1qvEwghifV3Nk5HGRePqk9V8Ebcd7nBHVc0g2xxccRtZmqtxsKPaGHCcMD350nkRalV_9qvHaPP3Z68TH_qrmG_3jnbG3v8eKEd65AUXvzbCVvg-FxYgKm__TCCPY0bsmjK2Lcyix9bjXQX7U3T4yWjKTeEtqkWlijadX_fEMO0xpcYLm8t-ektUMPRGz9d0ewSSMcE9IXnD8K_3BqFAQ1ZHOvxFKkDGdL3DYkK962F-resarButgCSPSFjzeC_YyVDLuTrWW7d40eDqAMrg5s-m3c_T_Fw0eqfRodot-lYokIVIOCSTjlDciTavr6NsRn7humEJA2YWP2qenxOvHRtpS_S6frXuIhixh1-XpyOKNs5kI4F2XFoLGHOKsb3S5K6mEpPpkpf9_dXWixURQ2oE5GN125p8jCkeCZYmVEX40wUHoY5A1Ucr5qjm_mYW1TLY3DU34wHVbvD90AgnXRxqYBc2NglU_KkMnC93NdRkSaYIb1qn4zu4VZ5oJHhiPC83ws7z9i-7AlrfjyyIWb6DhRH_PlcA7sEsaMyIZnpaFZl_ZNXyoDDEOu1zPYsmAsK-3KzUqGpD5NrMfAH8TYYDxM-hC14Y97HKJlnB5JrXM2rwnQpofsc3zXz5WQ2Sy7H2SGDWmuGPcyrg5T3ebf1a9AGAsCtZTomkxkMniJlpzOG5N5hyTIpYPF8pCJHzZ3F7XoewINnbd1FpyQwI4EU1TkVeJC9LX5AaCPGBtTLmcVj8IrbBlsAqy7Qm-LXK9uUuL2T20IuTo1zksgrSFbL6oLAUI1SsX6lss3XZsrZJwSqskXdArNoaZ_bWdzCJrTjeZ7JVXQbHCBbkT1WtsY27GakUOFTS82RwY4ut2lED6Jf7PRY1QfoXqqkNqZCLstpFLiVbdvW0J8JNbO6b9uMvd4U2b7DuSd_P36ZRQ1ynyrofishjDC0UpFlSUe5s8uN9-zUa8aWnhuzzBEi8eexthb8xY9KUy6WCJOwOUODKrF1novYLkHf572cgMr2FAVSfzTilTt06BSqAxqyXsBz8FpB5tOpERX_fP_t4ZPghzmF6WXelMtFYdU-UqhkHtSqM3e4JCK6OfgO-1St_7s3XxLh_VoiufL9GY2OHp1UGer_2JgcW-059RMLZ50FT3KU8lTtig43McyNxLBf9Ge7-depsomNj17ZcrepFMAMvEfOPhp3r0JKWaJRLCJdFqZZrSm1vHx07iSrdY4yrLArJYDho7i1utU01XahIz4qAeHr3q4JmQGLc_bdZYhN7e9LQ.kZHcvmdVfJzJnZJuQzsasA"
        println(MyInfoClientSingleton.decodeJweCompact(data))
    }

    companion object {

        private var clientPublicKey: PublicKey

        init {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate= certificateFactory.generateCertificate(FileInputStream("src/test/resources/stg-demoapp-client-publiccert-2018.pem"))
            clientPublicKey = certificate.publicKey
        }

        val myInfoServerKeyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
                .apply { this.initialize(2048) }
                .genKeyPair()

        @JvmStatic
        val SUPPORT: DropwizardTestSupport<MyInfoEmulatorConfig> = DropwizardTestSupport<MyInfoEmulatorConfig>(
                MyInfoEmulatorApp::class.java,
                ResourceHelpers.resourceFilePath("myinfo-emulator-config.yaml"),
                ConfigOverride.config("myInfoApiClientId", TestConfig.myInfoApiClientId),
                ConfigOverride.config("myInfoApiClientSecret", TestConfig.myInfoApiClientSecret),
                ConfigOverride.config("myInfoRedirectUri", TestConfig.myInfoRedirectUri),
                ConfigOverride.config("myInfoServerPublicKey", Base64.getEncoder().encodeToString(myInfoServerKeyPair.public.encoded)),
                ConfigOverride.config("myInfoServerPrivateKey", Base64.getEncoder().encodeToString(myInfoServerKeyPair.private.encoded)),
                ConfigOverride.config("myInfoClientPublicKey", Base64.getEncoder().encodeToString(clientPublicKey.encoded))
        )

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
        val base64Encoded = String(File("src/test/resources/stg-demoapp-client-privatekey-2018.pem").readBytes())
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