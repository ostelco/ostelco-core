

# What this is

Notes a out how to generate csrs and keystores to use for a dropwisard app.
Based on information in https://support.globalsign.com/customer/en/portal/articles/2121490-java-keytool---create-keystore


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

