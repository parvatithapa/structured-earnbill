package com.sapienter.jbilling.server.dt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

/**
 * Created by Wajeeha Ahmed on 11/27/17.
 */
public class ProductFileItem implements Serializable {

    private String productCode;
    private String description;
    private String[] categories;

    private Date priceEffectiveDate;
    private Date activeSince;
    private Date activeUntil;
    private Integer accountTypeId;
    private String customerIdentifier;
    private PriceModelWS priceModelWS;
    private String company;
    private boolean chained;
    private Integer currencyId;
    private SortedMap<Date, PriceModel> priceModelTimeLine = new TreeMap<>();
    private List<Price> prices = new ArrayList<>();
    private boolean allowDecimalQuantity;
    private String priceModelCompany;
    private Date priceExpiryDate;
    private List<InternationalDescriptionWS> descriptions = new ArrayList<>();
    private SortedMap<Date, RatingConfigurationWS> ratingConfigurationTimeLine = new TreeMap<>();
    private MetaFieldValueWS[] metaFields;

    private boolean isPriceModelForEpochDateEnable = false;


    public ProductFileItem(){

    }

    public ProductFileItem(String productCode, String description, String[] categories,
                           Date priceEffectiveDate){
        this.productCode=productCode;
        this.description=description;
        this.categories=categories;
        this.priceEffectiveDate=priceEffectiveDate;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public Date getPriceEffectiveDate() {
        return priceEffectiveDate;
    }

    public void setPriceEffectiveDate(Date priceEffectiveDate) {
        this.priceEffectiveDate = priceEffectiveDate;
    }

    public Date getActiveSince() {
        return this.activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    public Date getActiveUntil() { return this.activeUntil; }

    public void setActiveUntil(Date activeUntil) { this.activeUntil = activeUntil; }

    public Integer getAccountTypeId() { return this.accountTypeId; }

    public void setAccountTypeId(Integer accountTypeId) { this.accountTypeId = accountTypeId; }

    public String getCustomerIdentifier() { return this.customerIdentifier; }

    public String getCompany() { return company;}

    public void setCompany(String company) {
        this.company = company;
    }

    public void setCustomerIdentifier(String customerIdentifier) { this.customerIdentifier = customerIdentifier; }

    public PriceModelWS getPriceModelWS() {
        return priceModelWS;
    }

    public void setPriceModelWS(PriceModelWS priceModelWS) {
        this.priceModelWS = priceModelWS;
    }

    public boolean isChained() {
        return chained;
    }

    public void setChained(boolean chained) {
        this.chained = chained;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public SortedMap<Date, RatingConfigurationWS> getRatingConfigurationTimeLine() {
        return ratingConfigurationTimeLine;
    }

    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProductFileItem{");
        sb.append("productCode='").append(productCode).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", categories=").append(Arrays.toString(categories));
        sb.append(", priceEffectiveDate=").append(priceEffectiveDate);
        sb.append(", activeSince=").append(activeSince);
        sb.append(", activeUntil=").append(activeUntil);
        sb.append(", accountTypeId=").append(accountTypeId);
        sb.append(", customerIdentifier='").append(customerIdentifier).append('\'');
        sb.append(", company='").append(company).append('\'');
        sb.append(", chained=").append(chained);
        sb.append(", currencyId=").append(currencyId);
        sb.append(", allowDecimalQuantity=").append(allowDecimalQuantity);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductFileItem productFileItem = (ProductFileItem) o;

        if (!productCode.equals(productFileItem.productCode)) return false;
        if (!description.equals(productFileItem.description)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(categories, productFileItem.categories)) return false;
        if (!priceEffectiveDate.equals(productFileItem.priceEffectiveDate)) return false;
        if (activeSince!=null && !activeSince.equals(productFileItem.activeSince)) return false;
        if (activeUntil!=null && !activeUntil.equals(productFileItem.activeUntil)) return false;
        if (accountTypeId!=null && accountTypeId.compareTo(productFileItem.accountTypeId)!=0) return false;
        if (!customerIdentifier.equals(productFileItem.customerIdentifier)) return false;
        if (!company.equals(productFileItem.company)) return false;
        if (!chained == productFileItem.chained) return false;
        return true;

    }

    @Override
    public int hashCode() {
        int result = productCode.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Arrays.hashCode(categories);
        result = 31 * result + priceEffectiveDate.hashCode();
        result = 31 * result + activeSince.hashCode();
        result = 31 * result + activeUntil.hashCode();
        result = 31 * result + accountTypeId.hashCode();
        result = 31 * result + customerIdentifier.hashCode();
        result = 31 * result + company.hashCode();
        return result;
    }

    public SortedMap<Date, PriceModel> getPriceModelTimeLine() {
        return priceModelTimeLine;
    }

    public boolean getAllowDecimalQuantity() {
        return allowDecimalQuantity;
    }

    public void setAllowDecimalQuantity(boolean allowDecimalQuantity) {
        this.allowDecimalQuantity = allowDecimalQuantity;
    }

    public String getPriceModelCompany() {
        return priceModelCompany;
    }

    public void setPriceModelCompany(String priceModelCompany) {
        this.priceModelCompany = priceModelCompany;
    }

    public Date getPriceExpiryDate() {
        return priceExpiryDate;
    }

    public void setPriceExpiryDate(Date priceExpiryDate) {
        this.priceExpiryDate = priceExpiryDate;
    }

    public List<Price> getPrices() {
        return prices;
    }

    public void setPrices(List<Price> prices) {
        this.prices = prices;
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public boolean IsPriceModelForEpochDateEnable() {
        return isPriceModelForEpochDateEnable;
    }

    public void EnablePriceModelForEpochDate(boolean priceModelForEpochDateEnable) {
        this.isPriceModelForEpochDateEnable = priceModelForEpochDateEnable;
    }
}
