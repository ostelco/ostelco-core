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
 * Ref: https://www.ndi-api.gov.sg/assets/lib/trusted-data/myinfo/specs/myinfo-kyc-v2.1.1.yaml.html#section/Environments
 *
 *  https://{ENV_DOMAIN_NAME}/{VERSION}/{RESOURCE}
 *
 * ENV_DOMAIN_NAME:
 *  - Sandbox/Dev: https://myinfosgstg.api.gov.sg/dev/
 *  - Staging: https://myinfosgstg.api.gov.sg/test/
 *  - Production: https://myinfosg.api.gov.sg/
 *
 * VERSION: `/v2`
 */
// Using certs from https://github.com/jamesleegovtech/myinfo-demo-app/tree/master/ssl
private val templateTestConfig = String(File("src/test/resources/stg-demoapp-client-privatekey-2018.pem").readBytes())
        .replace("\n","")
        .removePrefix("-----BEGIN PRIVATE KEY-----")
        .removeSuffix("-----END PRIVATE KEY-----")
        .let { base64Encoded -> PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Encoded)) }
        .let { keySpec -> KeyFactory.getInstance("RSA").generatePrivate(keySpec) }
        .let { clientPrivateKey: PrivateKey ->
            Config(
                    myInfoApiUri = "https://test.api.myinfo.gov.sg/com/v3",
                    myInfoApiClientId = "STG2-MYINFO-SELF-TEST",
                    myInfoApiClientSecret = "44d953c796cccebcec9bdc826852857ab412fbe2",
                    myInfoRedirectUri = "http://localhost:3001/callback",
                    myInfoPersonDataAttributes = "name,sex,race,nationality,dob,email,mobileno,regadd,housingtype,hdbtype,marital,edulevel,ownerprivate,cpfcontributions,cpfbalances",
                    myInfoServerPublicKey = "",
                    myInfoClientPrivateKey = Base64.getEncoder().encodeToString(clientPrivateKey.encoded))
        }

class MyInfoClientTest {

    private val logger by getLogger()

    @Before
    fun setupUnitTest() {
        ConfigRegistry.config = templateTestConfig.copy(
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

        ConfigRegistry.config = templateTestConfig.copy(
                myInfoServerPublicKey = Base64.getEncoder().encodeToString(certificate.publicKey.encoded))

        Registry.myInfoClient = HttpClientBuilder.create().build()
    }

    @Test
    fun `test myInfo client`() {
        val durationInMilliSec = measureTimeMillis {
            logger.info("MyInfo PersonData: {}",
                    MyInfoClientSingleton.getPersonData(authorisationCode = "authorisation-code"))
        }
        logger.info("Time taken to fetch personData: {} sec", durationInMilliSec / 1000)
    }

    @Test
    fun `decode person data`() {
        val jwe = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00iLCJraWQiOiJzdGctbXlpbmZvIn0.mse7r-KuYK76L4dxrEDJOR1yvYeWik0oTuRItraXO1tY_kh3g8J5YJDEZ_EUtUvTkRwmHhjnBmsiL6SqKJMCp2cEfXw1Tun_5vQFbR8q_RaigUioemX4ZOCmXNPbekvSy45en0pNXAkFTVkC4OVf8W_Zu5B8Vd3-j9icx1rkTsfoC8J_G9TMpc-bJu0CAct0Oy1CHauK86pnK1UajGzdgH5Lh-aR2tdO4WyVVZqD-l0ROC2myXru2Dz9FxY0NMQCocIV7pXogXBZuoIt8T2PkBc8jZ0oDLtv13oOlI8ul0Vokj6Ly8c51cxa1RFkPZJpZk8faYkb5K7VVaXLTzPC_g.E_ZjnTggp0Uk1TqO.rL0tBEC8L9Ctyhi3reDTdwq4vLneMrMkxaXeI5cEFRRuiN4k1ZogoiXnDomGc1feZ0Gylog1_kceQqJ7xCj-bYMu4NWec9EDbLCb4JmoPfYhW0lYVzLMTHJDAXC6iI5vfVZB3qPiaJwv3slncet8IBU6JgIV9U04Y1qdg13wNsjX_prN7g-8yPTB6ZmvSeNCYOTWgIqJ12sdBpB9zgprjHoKWifh2vTgJfiYGdCf84xMMGbzY9Z6fb-ZFJR437QnFWNwX6tpVRp4Xj5wlsMmj6pHQd2243ADWkAaa07EMF8L9JEVavWBIgwNz8oXmMc0Oke3hTTxAjRt6Lkcdof5mXb1FyXNJFvZDAkn0BAzGkh2gQOT3VLui-lLt46dRLY18LdaeKqGVNQ9-nojqDBtJk4ZNFF0D1vqT5mYxHcF2WFepumj3rfFa6YE1Pw2uw_C_Z4TlDAr6bNOdNaj0jYgRz1gW5WKWrcDkEy5fOR38cdclDCjbfYtng6Fhr6fSBevpwDjFCWFM_695XraIwAGRfe-malS6PmwCxRZgMOFoo9vIY16HMwadPe1lKaLBEJDyLAXm6xvEJ9UuzPiCwNdCMzDQ1r5bvTL-WZlZVCOTIfLYrcczYkXJl58pHd9kBjTdZQU9YUJeFMihIZa0yXSNVXv9yrlE9wW5irNc6oG6LMdDiAM6JGGVwpSHVopnyROxOzJjAQpSutIVpyZy64C5xWNvdxfXKH-eW2McnFR0RCTvmSfEzHkvEcNcV7rX0hNmcZqBRkil8KlfLF84Rbcs6RgZjsfVNSxZW5n-krWuPmEuyjRHZgXQJrYm98RCmAFlGAZ0A7sDQYWdMDvRh5mGU7dlyOtd9aanx5prym8ZCKCD0s5h1HhBJA2xtgdVzRGchBGUYfaY8DerGgkmtDZeQeej8gVW-BHmKhl3LBEG47oLoAXsROG-RpMMfCaTpLN4pE8qM5BpS7nAqPHxBmE4tEv846rqvIaZVTLy-Yu5nJLsoOyAhyyBmLWgdZDTzFcK6CqSJZGrmFZ7amMzHFpf6qJKuc4lAje1Ng5OAjcO9VSUnxxeY4l3hRkgfQNGp2VhfE8PABPCWR84MiIb2UO-lY5Um8qvrH4p4nZXaNYZgoYtixDcRsrMq4qSjQzF8SDmBOEoIPfdqQZCDCnl_qF_Yy6lJPdpPw9FvWKrFbfHAl93FuVWLzTTzL5X5KVCsd4W85k9eCLeas-t6VDqfh1dNtt2xxWqu3kfJUeNFiWeucugiAVdnaSXo5TPqC6S1dWVrSLm4aDLab6ztiliLgqgf6QZZZI0O8trPrnFJfR5t4JTmInJaS1QjbvA4c1pBp0XzFe24lf-Ip8mFsXz2JLwq3ICMjP0QqESTQtfAYzrVTRTtjQuGVm1Pb8-KiliQJoGHEIo5jfnDFKLrTk4qQ2CrRt9Agd4A9yDIQfuaVilEFBAugGBFxoS9h_yVX5_z9emDaQJjoGDSnMJ6jXMHCvSv1VmPR-JSkBradkSxGaSX9KztBz2OHY13gdM-qy0Cuc_gjWvk8XamwM96I7c4YIQWqVGXVxPcunOMxgIMNXR6BoH20mPxTZ7zTTYOEO9nhqg1OTvIq7bp1pjwKZ0JKQ2hYGbslYR87vJyfgrNKCsq0O89r_4tTbs2K35hBn4q_kJYrP8A_kk_Pujt42AFE9OmNuRk9RuhvLW6bJMZ-yfIVOtU0fqsMfoOgQ2uGG5EtOL4nw5z845nkbJF3V8S1pcmvc2MsmypBZSrvbZiecVgACcAonJ8nHz-SbNOe13-sGul9_hwoQsT4DWiXE2F6EcZtgnQsH-OsCcBMR_MalZiYUaqhYLUTfOKP0ybsi3bbad36BcPVLmrAarC6t88mMcVdOQmEnWJfOKdww7Qt6T1DtxKWpWj4jUv82GZ6Wo3Cujw4aASSG-JwwfC55mERz5Ot9zn4OTl9p9NEOq_QQNQ3EqgB-SNnxLaEdOrvkTlxBCw4yobROqzqPd-CAE-XCoE1sgGTbhMWtyGk2z33S_Pw09TGW29dQHH59BB1XM5qAwSj_-TvFWgHpnNgC4XXoKEdS7WXDp4I1PnLUMORrF1n-jxFBQnwcwGpKLDf3onTjmtCqFyr2slVONmcxDRzANY4cTD7JqBLHOHpdjStXRhI1yQ8OdYC3Q7zn20fJpwcpodTEMkvjsLg9sAR37qmSeFICuJOXS-hFI929xNDI4eWE8fFKzForam1ppgWdP7Hv8CUB5ENQQsvnzVfKdZSp6P6KXYFrh2LXsn12ua15KQDfcLthKBVR5FEJFoopDkn6sVKj5aAUgUd95rTp8yHKSRkvym_WCUGzEJMcUlyUMoTYgS3JeDVaansClZzF3GTfTP0RofOQvfn5g4O5sTU2tk8kD0opJTAZuGpRJFe_nZKDdzs8SPhZPsfaZM3gYir_-HhFmJ_Cf42tH4vtmT79vXOMLNMPWTIWjZv4SYfiRd__q6M9kFj-IaotoU8sK1S4Apec1kqWJhuPtKWTK3lrIAJXnIvwoqaFY0hiI4i8Wpk7hodyxxCvnmLjvA2Em8SOoihBrJZlFP2sXZmZZ55iytwy0dH3qz_5uU2z_hfOVE3Y5WVgg0VLC5H6ljqLFQHFQWmy_Ujr7km8dnGphD2R32kSKYIRuo3nWRGKWbMQRVGp_48CM8feLVip0faWdyiA9eDWZiixqohXC_sLwdKWqkN7DwE9jnr-rA4CVs4yW9JK1gM6PayV-E4CvVlkt5Q3qH14KaDWuHaZIndDD6tmNAYZKttsqDfv6RDqVXnzb3wV6n0lbDYUsu2arlw-X1d7mhO_RXHVyRfltnzGxqPLDGfLo0s13paOSlln4Lz5kzBcvg8FFg9uJ0CXbhYNm8ypWF4ZU3xLnRK_pfXZUZJHHGMAUxAyymUBSqaCBQTXNkHc_VGTngHYDuTBeBsfGi0nkh-bbG356zsJDF3pGzwEpeUg78uHEUmrVx1bVAyoW8cG6sr3JlJYiyiJIbFqj7BIzu6p2cL1WCbaF4X_6t6uidIQaGVsIYqi8J6mO_lpRBxw6ABo7Gd2sX7s1kWIajsbyPXb96ZovBMu8V0u_a_tZNokeHkW3xk7GJUHtgbgNnVtkMdc2st4D7Il3aPeIiR1mOPG75G3hdDWEPKleGYbqIsk1CQl9GO6uk7oxQX-me4AzZgIvRa7tEeY9bxD2ScbIkme9b92soCzi9-vPoo5Q_gZ66quQl_hLdxm8V2jECTCPRLtnGApDRBSTPM-h1-bfY_mu51OObbeysXniB7ltRVmpXqgaKs5n1J9SBnn5kqQk-8ZAQFOl49aoVo3VtPaN8mkpNLBjl9HSZyp1IlSCYwTV-qz7hgHWSSdNkg4xxCNpSa8YEKEA2MOcVNOhjfiSUyw-MkRd78xtLPkrHyW0eu9ja3ASIxX7zA4rT3eeJa730w5JVjLj2yhampysiYMSS_1gXXhp_ZbNSBkZOP-eU2b4y3tBz7SmOd-nRcoM14fVIUZUWZtsmQ8MvNPuFdqE0VtbSfwUnzsLNOfes-jpUno9h8_1OztuwPEnT9f6fAz5daBsPl8-nhK6vXfUhIV8ajvlssW4geLzbIbatZOsvUgRjb6g55I0HO5OnlrhCvrmgmPNKhHie6un44vEX1cg-bXYWBsGtlKmp1J977jYYLX5sb1AiZ6fLF8M9Qb7eTT7E31ftpVek_2x48qcZ9sXqPKtFl-XlJftsEEDqhfoDD6re6JV-6gasyP_K7a0_C1qI9zIPf0PsEKGZ6lpkqIv2towVlC5UQtZNd2yLTH3jMvK35_b3bVvcpf3gpH7_FTWoJfH4jm-0Otyg2TZ58YHadySPlGKR_V-qX1OBbHwEq-9vqBPe2e_Ui5qSluA90zfqa-cC6zq5gH1fDOm1QjxEh4kLE8fFzDpr4fV5VawYBpVwhnYfJnFZRg-T1AgHx3kFnVDfXVdWdktMcXVS_NPtyjhmTKI79jI9EYGU3p1sipkqykBzvOP4eJAbXmbZODfNGWWcFZgfAoZ09Ofs3WmjG7P1-c_NBaRRxc6XoRHKnJRr03YoWTxXKBqL8wGXmuLj852UdeAypSYJgOwUZdPNmIv7GfXk9ydOUjOrzktdpGDN-rI80Mh6EnF2UKZfK6ZyjgwKAqLX2RBIcb1Ar1HCm-FEZzpmfYn_ekCYA5Wk_ax1cvu9DYGK6RHEMg0fq8m32hUP1VYgtzLY2h9dJGExYIvcpWCdFS6AdZJpeYscWQBrsWRV3JwoKUPoMTZczGzzdNyZafmIeiy6K1rYosP-6rRjcDHh5-cWgHjZFKBu8iln2wnZhLTpyfEHcLapPNXB9EJ0DC-6DzzUaxy8FSG9aGqOEiMO6z1L_l8xJhnrQnxeZh5U4rWjEw1vTf9NA8qAkYXTa3jD5MGlngm2weEdakgRFapttNIqWJXtIRJboyPAl3-vkE9XQ0vB8G0ZxdQo8DwaU3b1jNCkK3Jt23MmV5_ojccZtg8k6tEIgf8GWxFDaw4YcflUnuXkCM-pB87o19rYTMreNaIPCV0Z4yh4P9GA8v60-OBNB52CxBBf4dgfW4GUehFl3AJyjjQRJHXZoWWNww_87hjAG3Jz75EAadzQFybcRxY6aDylawTA65zQKERIrDbPj-YktuGIiV-b-487BjIofsJEDkYFV12B3kJEx3rFHNZ-Xkeh6vQGpYFX5UOfQktVJd_tPjphzEkBVX_TKmP4eVis3vNAmBC6rGQjxCkKjWHH4fBZqawaD-qbqdCQpZnInQrKqIu5KD_i8aABZ3nLni_y6WWifCJLo5d70X3XI_BgLAliegUDDdJZEaNQpPPZNPC85IPqRjwMVtyb4vYX_Dsw8-50uzQVv_hV-2dNP7pgitDPBkusQuNeS8p0uX1-kuRFqTP77aYmUGNY77SOtcKwvzzS6iao6VkCYLJbuEp_24kZjz7gn1oRJ2eVkpJFPAhhaJWV93PGJamrgkINkcWfSCdX-Qpu7vCZ2ZLiag4FZU57NEo-O4V0jD521FV6lcohkKYBLleaHq85A3f2SURUDr-4_cSrqwpI8mG3yw2FcXSjXdZYrL1AC4hlIvK0ORSr0d4CzrAh_90ttiKHaERG0qqHbTkHGWQyfl1pg1pkFFq3gfIa5GMfPgKc_IuMaTI24L9Bj05ZwRZ_r2AVYDBAVg7SGalmdjbkVpGqQwVbp8Q9jy0LPpV808-i0rAQVVJih5Q3b19ubBGrhHoGL4-HDuOqiWbZewDd_I8NDie_8HgYsjVB0fyWPcUucqwu6xydf0IuL73QeT7XK1HyP0_pzIDAPPWo2orSmpmXHWib8lCAyvN27em22cueVenxJhEqFClSux_Qwjc9VnI2L-VUPo7nTzLLJebY6E9Aimko5_laT3EXC9Qq5g2DNTKkzhcQq77w08EfAFWT6bqYheAWYVGQVA0I4OS-QHerbBFJK_OcFzBpAsd5xKCVV8QMF2N7nsys28nsL_hppHt6YZ9QA_-e2-HqdV00CmEVFursfe1mSdQNOpLyozXaQ-mQD0u-ewGpcn4k2JEKBxRIllEchvk7xlklh7HCH4oy0sbCZTz80DWmDwOQ6Y-kPg7gAoIZbFXW0mACGe7QbqrxoQfQKdanA9sXa2cwDSqJZbXzrir34q8oJlM7171SSO4bH1EVbq43XaA-Vr-F0Wut9Ktk7PO5MkwIUEjrS9N5-FdaUOaO8hwCdonyJa7uYVfBj5bnlU2g-U9qZ-w5xhaXXfsmMGzII4wpYSnBgOTbF9ak44a_MgkMRviUVMem-LA1npSKdjVlS5guowWAiR7ANz8m1_dIwo1DfSbrpm4AldcDeE4ZEfTylwN_h_cISVk4OD6et4SU1HsSjMwFXG4G-oENvaGyeZWTvKMEbrW3H4FvR2Lkusl66CJ5_QbG71Xvu0EUYIM7KuNzh8s9eefHvMnk3Vl2jVcfxIEWB2M73N_2e7Gpm2rbH-a4gA1t-vinLfMwZFmgxzhIcy2KKFg1vifnynRae1y4xsdpkPtNdhnEXXmgYwJrszaInwgB0Vmau4hTHt_DN2lCawoPbIjyqNypRV2dMDenyYga-08iw6F46tLDsUcrDP7vv5jkdYFBaxI_9c_ac8Dbo0j3GZi161IVoZ1H74DVMQcpgrLS6WfsYGGMeBAnXtUt8c-rgggMLmpKGQu90mneXVcISQszNpj8x6ZOq5z7F9su6ydTK-9Ore4zI-uS9iw9OSsYbAt43k_jQW_rwdLhDSAzYVTGMWB4G9c8WrzUWddJLtZd5JAOR9fnfXhBz4NtWIF9-FnNwlwfT_fvAruJU3wOXblkrbMT_KvBvNbcGuBp0eVaREeO_rRDVmJyL62tdLwF2MJVpiASpJncPnrh2VN0SAHtGUPgVj7Snrlgv8xTJRueHAojYT2DSRgnuKa6mkfP6eldV7PhccR6BLWNgOU4H8Q82djniFU9sJWqhEikMrKK5z3mow92T5B9i-NzPZBKMk1SXLhvfKwLoZaip8o2jcBp9XtgE5MJtcnrJsxVSq7TD7y4fJ6L7jVD7p1uG8hYr81TA1aUMLGUqC7D2XRoj8JNVU__Tda2uEVjcgzLqttmZSB9FWoYbrBk_ptK7tsnQoOOJNC0L6DhARn3xXfjYalwUGIiwxIgR9aeE9p8N_Zed7Sc7qTbLw59IyHq0xowbpSUXLT_fSMO5piNiGV7-Y1yHsYsjJBbHEf2J52kDVrY_uXYvk3gfT1WOGLB_GVgl0uhGwkgr04pLx8ljyAbY7OV5zDIcQB0G5oA956YGOKayaOzl8LTI42Un4V2LG06Z6mFRQLvSR7xynRRuKr20nSrpjuNCe7zKlhUJBNNpXISa0EmgQEgQ3mc6frhPwaNcu3fWKZkIXMeyUeWSNQIStcyM9F30NzYsgpz5gW_KzW2hUetKfuvOeeIqCZONZKGfX7e1JaSi4jKicOr50Ld2I8rjUSPg3lGtVVn9ExDWC5a-icVswz60_6uHS85PaU-nXbQau0I0LU1DGDFLuJNORNfV-LHS45BK16LzrjZ9QR-pk1B4gOYztsN5Q28xfPtJjqwPiAI2ORi-ZnLy-9bhyyQaHsqFxHrxeLNWEfHmH9taTS1oCUcFJkYSey4CezWDdZLm7tb3uFPMcabfrE_7qz4WHlI9OmeNqEAEgIK6XhnspW7gAomV91kpq-KuRvwOaejOtm9pnn0EPyqpdyPhJhd6k6XOuIzE1FxLYv8a6ervpXdzfz3vsAtU1itqU2kvTELNqJfRo596BxQXbHosajX6tVfa47VhGPczwEF4UH3im9Q_GH3dh56zPiEO9___b7SXqJdXHmPVvxGJEhUKs_vC-VcRPt_l7GfvvpLJNRnyla2Cpr64-vyHOxb8Kkg_oBsm7PQVPUal4M_gtwP5R9DLWCO0rkYgfZzFHXJbhzrT99A.U2VJS3nXvSGUelqqdNP3zg"
        val jws = MyInfoClientSingleton.decodeJweCompact(jwe)
        logger.info("MyInfo PersonData JWS: {}", jws)
        logger.info("MyInfo PersonData: {}", MyInfoClientSingleton.getPersonDataFromJwsClaims(jws))
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