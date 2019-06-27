# Check JKS file contents

```bash
keytool -list -v -keystore dev-idemia-client-crt.jks
```

# JKS status

| Filename                                 | Status             | Format | Key alias  |
| ---                                      | ---                | ---    | ---        |
| es2plus-prime-csr_prime-prod-may-2019_v1 | Cannot recover key | JKS    | impcert    |
| prod-idemia-client-crt.jks               | handshake_failure  | PKCS12 | impcert    |
| es2plus-prime-csr_prime-prod-may-2019    | handshake_failure  | PKCS12 | impcert    |
| dev-idemia-client-crt.jks                | Success - 200 OK   | PKCS12 | loltel-key |


## Logs of working handshake

```text
DEBUG [2019-06-27 13:22:42,722] com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager: Connection request: [route: {s}->https://mconnect-es2-005.staging.oberthur.net:1034][total kept alive: 0; route allocated: 0 of 1024; total allocated: 0 of 1024]
DEBUG [2019-06-27 13:22:42,732] com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager: Connection leased: [id: 0][route: {s}->https://mconnect-es2-005.staging.oberthur.net:1034][total kept alive: 0; route allocated: 1 of 1024; total allocated: 1 of 1024]
DEBUG [2019-06-27 13:22:42,734] org.apache.http.impl.execchain.MainClientExec: Opening connection {s}->https://mconnect-es2-005.staging.oberthur.net:1034
DEBUG [2019-06-27 13:22:42,739] org.apache.http.impl.conn.DefaultHttpClientConnectionOperator: Connecting to mconnect-es2-005.staging.oberthur.net/213.39.85.22:1034
DEBUG [2019-06-27 13:22:42,739] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Connecting socket to mconnect-es2-005.staging.oberthur.net/213.39.85.22:1034 with timeout 500
DEBUG [2019-06-27 13:22:42,810] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Enabled protocols: [TLSv1.2, TLSv1.1, TLSv1]
DEBUG [2019-06-27 13:22:42,810] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Enabled cipher suites:[TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256, TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256, TLS_RSA_WITH_AES_256_GCM_SHA384, TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256, TLS_DHE_DSS_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_DSS_WITH_AES_128_GCM_SHA256, TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384, TLS_RSA_WITH_AES_256_CBC_SHA256, TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384, TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384, TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, TLS_DHE_DSS_WITH_AES_256_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA, TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA, TLS_ECDH_RSA_WITH_AES_256_CBC_SHA, TLS_DHE_RSA_WITH_AES_256_CBC_SHA, TLS_DHE_DSS_WITH_AES_256_CBC_SHA, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA256, TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256, TLS_DHE_RSA_WITH_AES_128_CBC_SHA256, TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_128_CBC_SHA, TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDH_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_DSS_WITH_AES_128_CBC_SHA, TLS_EMPTY_RENEGOTIATION_INFO_SCSV]
DEBUG [2019-06-27 13:22:42,810] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Starting handshake


DEBUG [2019-06-27 13:22:42,891] jdk.event.security: X509Certificate: Alg:SHA256withRSA, Serial:a2934cc0d89d0cfa, Subject:CN=mconnect-es2-005.staging.oberthur.net, O=Oberthur Technologies, L=Colombes Cedex, C=FR, Issuer:CN=OT Cloud Server CA, O=Oberthur Technologies, L=Colombes Cedex, C=FR, Key type:RSA, Length:2048, Cert Id:1130661864, Valid from:02/03/17, 3:32 PM, Valid until:28/02/27, 3:32 PM
DEBUG [2019-06-27 13:22:43,003] jdk.event.security:  TLSHandshake: mconnect-es2-005.staging.oberthur.net:1034, TLSv1.2, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, 1130661864
DEBUG [2019-06-27 13:22:43,003] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Secure session established
DEBUG [2019-06-27 13:22:43,003] org.apache.http.conn.ssl.SSLConnectionSocketFactory:  negotiated protocol: TLSv1.2
DEBUG [2019-06-27 13:22:43,003] org.apache.http.conn.ssl.SSLConnectionSocketFactory:  negotiated cipher suite: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
DEBUG [2019-06-27 13:22:43,003] org.apache.http.conn.ssl.SSLConnectionSocketFactory:  peer principal: CN=mconnect-es2-005.staging.oberthur.net, O=Oberthur Technologies, L=Colombes Cedex, C=FR
DEBUG [2019-06-27 13:22:43,004] org.apache.http.conn.ssl.SSLConnectionSocketFactory:  issuer principal: CN=OT Cloud Server CA, O=Oberthur Technologies, L=Colombes Cedex, C=FR
DEBUG [2019-06-27 13:22:43,006] org.apache.http.impl.conn.DefaultHttpClientConnectionOperator: Connection established 10.6.1.169:56205<->213.39.85.22:1034
DEBUG [2019-06-27 13:22:43,006] org.apache.http.impl.conn.DefaultManagedHttpClientConnection: http-outgoing-0: set socket timeout to 10000
DEBUG [2019-06-27 13:22:43,006] org.apache.http.impl.execchain.MainClientExec: Executing request POST /gsma/rsp2/es2plus/getProfileStatus HTTP/1.1
DEBUG [2019-06-27 13:22:43,006] org.apache.http.impl.execchain.MainClientExec: Target auth state: UNCHALLENGED
DEBUG [2019-06-27 13:22:43,007] org.apache.http.impl.execchain.MainClientExec: Proxy auth state: UNCHALLENGED
DEBUG [2019-06-27 13:22:43,013] javax.management.mbeanserver: ObjectName = metrics:name=org.apache.http.client.HttpClient.SIM inventory.post-requests
DEBUG [2019-06-27 13:22:43,013] javax.management.mbeanserver: name = metrics:name=org.apache.http.client.HttpClient.SIM inventory.post-requests
DEBUG [2019-06-27 13:22:43,013] javax.management.mbeanserver: Send create notification of object metrics:name=org.apache.http.client.HttpClient.SIM inventory.post-requests
DEBUG [2019-06-27 13:22:43,013] javax.management.mbeanserver: JMX.mbean.registered metrics:name=org.apache.http.client.HttpClient.SIM inventory.post-requests
```

## Logs of failing handshake
```text
DEBUG [2019-06-27 18:46:34,785] com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager: Connection request: [route: {s}->https://mconnect-es2-005.oberthur.net:1032][total kept alive: 0; route allocated: 0 of 1024; total allocated: 0 of 1024]
DEBUG [2019-06-27 18:46:34,795] com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager: Connection leased: [id: 0][route: {s}->https://mconnect-es2-005.oberthur.net:1032][total kept alive: 0; route allocated: 1 of 1024; total allocated: 1 of 1024]
DEBUG [2019-06-27 18:46:34,796] org.apache.http.impl.execchain.MainClientExec: Opening connection {s}->https://mconnect-es2-005.oberthur.net:1032
DEBUG [2019-06-27 18:46:34,805] org.apache.http.impl.conn.DefaultHttpClientConnectionOperator: Connecting to mconnect-es2-005.oberthur.net/213.39.85.22:1032
DEBUG [2019-06-27 18:46:34,805] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Connecting socket to mconnect-es2-005.oberthur.net/213.39.85.22:1032 with timeout 500
DEBUG [2019-06-27 18:46:34,933] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Enabled protocols: [TLSv1.2, TLSv1.1, TLSv1]
DEBUG [2019-06-27 18:46:34,933] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Enabled cipher suites:[TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TLS_RSA_WITH_AES_256_GCM_SHA384, TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_DSS_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_DSS_WITH_AES_128_GCM_SHA256, TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384, TLS_RSA_WITH_AES_256_CBC_SHA256, TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384, TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384, TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, TLS_DHE_DSS_WITH_AES_256_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA, TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA, TLS_ECDH_RSA_WITH_AES_256_CBC_SHA, TLS_DHE_RSA_WITH_AES_256_CBC_SHA, TLS_DHE_DSS_WITH_AES_256_CBC_SHA, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA256, TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256, TLS_DHE_RSA_WITH_AES_128_CBC_SHA256, TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_128_CBC_SHA, TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDH_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_DSS_WITH_AES_128_CBC_SHA, TLS_EMPTY_RENEGOTIATION_INFO_SCSV]
DEBUG [2019-06-27 18:46:34,933] org.apache.http.conn.ssl.SSLConnectionSocketFactory: Starting handshake


DEBUG [2019-06-27 18:46:35,084] jdk.event.security: X509Certificate: Alg:SHA256withRSA, Serial:a2934cc0d89d0cfc, Subject:CN=mconnect-es2-005.oberthur.net, O=Oberthur Technologies, L=Colombes Cedex, C=FR, Issuer:CN=OT Cloud Server CA, O=Oberthur Technologies, L=Colombes Cedex, C=FR, Key type:RSA, Length:2048, Cert Id:1751233116, Valid from:02/03/17, 3:32 PM, Valid until:28/02/27, 3:32 PM
DEBUG [2019-06-27 18:46:35,155] org.apache.http.impl.conn.DefaultManagedHttpClientConnection: http-outgoing-0: Shutdown connection
DEBUG [2019-06-27 18:46:35,155] org.apache.http.impl.execchain.MainClientExec: Connection discarded
DEBUG [2019-06-27 18:46:35,155] com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager: Connection released: [id: 0][route: {s}->https://mconnect-es2-005.oberthur.net:1032][total kept alive: 0; route allocated: 0 of 1024; total allocated: 0 of 1024]
javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
	at java.base/sun.security.ssl.Alert.createSSLException(Alert.java:131)
	at java.base/sun.security.ssl.Alert.createSSLException(Alert.java:117)
	at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:308)
	at java.base/sun.security.ssl.Alert$AlertConsumer.consume(Alert.java:285)
	at java.base/sun.security.ssl.TransportContext.dispatch(TransportContext.java:181)
	at java.base/sun.security.ssl.SSLTransport.decode(SSLTransport.java:164)
	at java.base/sun.security.ssl.SSLSocketImpl.decode(SSLSocketImpl.java:1152)
	at java.base/sun.security.ssl.SSLSocketImpl.readHandshakeRecord(SSLSocketImpl.java:1063)
	at java.base/sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:402)
	at org.apache.http.conn.ssl.SSLConnectionSocketFactory.createLayeredSocket(SSLConnectionSocketFactory.java:436)
	at org.apache.http.conn.ssl.SSLConnectionSocketFactory.connectSocket(SSLConnectionSocketFactory.java:384)
	at org.apache.http.impl.conn.DefaultHttpClientConnectionOperator.connect(DefaultHttpClientConnectionOperator.java:142)
	at org.apache.http.impl.conn.PoolingHttpClientConnectionManager.connect(PoolingHttpClientConnectionManager.java:374)
	at org.apache.http.impl.execchain.MainClientExec.establishRoute(MainClientExec.java:393)
	at org.apache.http.impl.execchain.MainClientExec.execute(MainClientExec.java:236)
	at org.apache.http.impl.execchain.ProtocolExec.execute(ProtocolExec.java:186)
	at org.apache.http.impl.execchain.RetryExec.execute(RetryExec.java:89)
	at org.apache.http.impl.execchain.RedirectExec.execute(RedirectExec.java:110)
	at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:185)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:83)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:108)
	at org.ostelco.tools.prime.admin.modules.DwEnvModule.init(DwEnvModule.kt:37)
	at org.ostelco.prime.PrimeApplication.run(PrimeApplication.kt:23)
	at org.ostelco.prime.PrimeApplication.run(PrimeApplication.kt:12)
	at io.dropwizard.cli.EnvironmentCommand.run(EnvironmentCommand.java:43)
	at io.dropwizard.cli.ConfiguredCommand.run(ConfiguredCommand.java:87)
	at io.dropwizard.cli.Cli.run(Cli.java:78)
	at io.dropwizard.Application.run(Application.java:93)
	at org.ostelco.tools.prime.admin.MainKt.main(Main.kt:18)
	at org.ostelco.tools.prime.admin.MainKt.main(Main.kt)

```