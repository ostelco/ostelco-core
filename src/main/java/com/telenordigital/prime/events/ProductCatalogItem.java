package com.telenordigital.prime.events;

/**
 * Bean used to use read items from the product catalog.
 */
public final class ProductCatalogItem {

    private String badgeLabel;
    private String currencyLabel;
    private boolean isVisible;
    private String label;
    private int price;

    private String priceLabel;

    private int amount;
    private String sku;
    private long noOfBytes;

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(final String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    public String getCurrencyLabel() {
        return currencyLabel;
    }

    public void setCurrencyLabel(final String currencyLabel) {
        this.currencyLabel = currencyLabel;
    }

    public boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(final boolean visible) {
        isVisible = visible;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(final int price) {
        this.price = price;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public void setPriceLabel(final String priceLabel) {
        this.priceLabel = priceLabel;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = sku;
    }

    public long getNoOfBytes() {
        return noOfBytes;
    }

    public void setNoOfBytes(final long noOfBytes) {
        this.noOfBytes = noOfBytes;
    }

    @Override
    public String toString() {
        return "ProductCatalogItem{"
                + "badgeLabel='" + badgeLabel + '\''
                + ", currencyLabel='" + currencyLabel + '\''
                + ", isVisible=" + isVisible
                + ", label='" + label + '\''
                + ", price=" + price
                + ", priceLabel='" + priceLabel + '\''
                + ", amount=" + amount
                + ", sku='" + sku + '\''
                + ", noOfBytes=" + noOfBytes
                + '}';
    }
}
