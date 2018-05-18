package org.ostelco.prime.model

/**
 * Bean used to use read items from the product catalog.
 */
class ProductCatalogItem() {
    var amount: Int = 0
    var badgeLabel: String? = null
    var categories: ArrayList<Long>? = null
    var created_at: String? = null
    var currencyLabel: String? = null
    var description: String? = null
    var expires_on: String? = null
    var external_url: String? = null
    var hidden: Boolean? = null
    var id: Int? = null
    var image: String? = null
    private var visible: Boolean = false
    var label: String? = null
    var name: String? = null
    var noOfBytes: Long? = null
    var notes: String? = null
    var price: Int = 0
    var priceLabel: String? = null
    var repurchability: Int? = null
    var short_description: String? = null
    var sku: String? = null
    var tax_rate: Int? = null
    var updated_at: String? = null
    var value: String? = null
    var visible_from: String? = null

    fun getIsVisible() = visible

    fun setIsVisible(isVisible:Boolean) {
        this.visible = isVisible
    }
}
