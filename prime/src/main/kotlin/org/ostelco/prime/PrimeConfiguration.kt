package org.ostelco.prime

import io.dropwizard.Configuration
import org.ostelco.prime.module.PrimeModule

data class PrimeConfiguration(val modules: List<PrimeModule>) : Configuration()
