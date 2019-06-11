package org.ostelco.prime.admin.api

import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Product

data class CreatePlanRequest(
        val plan: Plan,
        val stripeProductName: String,
        val planProduct: Product)