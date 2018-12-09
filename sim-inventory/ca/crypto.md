

# What this is

This is, or rather will be, a description on how to set up a local
certificate authority and use that to create self-signed certificates
to be used by dropwizard and jersey-client when using two-way TLS as
required by the ES2+ standard.

THe assumption is that we will create certificates frequently, possibly
every time we run an acceptance test.   It is also assumed that 
both openssl and the java keytool are installed.

It is not assumed that any trusted certificated are located anywhere,
everything is based on self-signing.

The certificate authority setup is rudimentary, but with some modifications
sufficient to to provide generate certificates for production use (run in 
a secure environment, verify that the toolchains are uncontaminated
etc.)


# Overall flow

  TBD:   Insert  plantuml sequence diagram describing key signing process.

# Inspiration

Notes a out how to generate csrs and keystores to use for a dropwisard app.
Based on information in 

   * https://support.globalsign.com/customer/en/portal/articles/2121490-java-keytool---create-keystore
   * https://docs.oracle.com/cd/E19798-01/821-1751/ghlgj/index.html
   * https://developer.okta.com/blog/2015/12/02/tls-client-authentication-for-services
   * https://gist.github.com/Soarez/9688998
   * https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html

# Generating keystpore and truststpre

   keytool -genkey -alias mydomain -keyalg RSA -keystore KeyStore.jks -keysize 2048
   keytool -genkey -alias mydomain -keyalg RSA -keystore TrustStore.jks -keysize 2048

... for test use only.   Both have "secret" as their password.

# Generating a CSR


    keytool -certreq -alias mydomain -keystore KeyStore.jks -file mydomain.csr
    Enter keystore password:  

yields the result:

    -----BEGIN NEW CERTIFICATE REQUEST-----
    MIIC2DCCAcACAQAwYzELMAkGA1UEBhMCcmExDzANBgNVBAgTBnJhbmRvbTEQMA4G
    A1UEBxMHcmFuZHNvbTEPMA0GA1UEChMGcmFuZG9tMQ8wDQYDVQQLEwZyYW5kb20x
    DzANBgNVBAMTBnJhbmRvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
    AKqgIom6BkcFFcmi5svgKSQV3RvExds2Af/3g39x2e7JWEYFH4ceqxVkREuP0NKS
    3gVTenq2FPZii5CwCvZtcznk4WuBFNZUI00MoA9T0S9Cow0wvvaGSp+hm4HEj1Eu
    s+XQ/VPiFSIvru++qxTnYMp/K3zNr0UpS5UIMhPiW8UixB43dDXGLqqT9q3m9zBe
    qrJUYmVLZFd2v7q104SsBLsqZvSNcI5BFxcKCg1YjC4tz9sDEUCydtEq6zLDbJ4N
    hS1wH5EpC2mpXoJnJ6fsBL1FUXb+yJ6GjIZucgsxruiyYCC7qC3mFpZx/hFGxI1P
    a278ddNaQMBMDseW0TPyVkUCAwEAAaAwMC4GCSqGSIb3DQEJDjEhMB8wHQYDVR0O
    BBYEFOG+Q+Mn6rmK8s1zYYqFIDmturDpMA0GCSqGSIb3DQEBCwUAA4IBAQAE17OH
    zbCjehvtMFO3tEs6cfhBHpj5I735ahG2leIuTBHCdNuOepjvxm3Lw2lGmEuYhBZs
    ZwHGdXl1mJaMdI44WAZMF3/WZ/NoCj+GOR4LiyEbZCf/d3Gz0lnJNY+qngvf5pgq
    R93wDSHYYBHKG9dwwHj4EZd5MaF4kCuH1AfRGaYFSIZtNQ7QFlrccjO/yBBM55AH
    g5XF3VpDUOYU6vTFwYA9MaN12Ijfa8PAuWP6kbWmexUet/c6WJitTIDoriqx/Lic
    ib4auHzUhozi6Tc9v2X23Ybqdw+AHN0tG5Bz162kUzp5m30DDa10dRvjuIWzv2+j
    w3NivI42ZcAbfviR
    -----END NEW CERTIFICATE REQUEST-----




