package com.sapienter.jbilling.server.invoiceTemplate.report;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;

/**
 * @author elmot
 */
public class InvoiceLineEnvelope {

    @Field(description = "Product Code")
    private String productCode;

    @Field(description = "Category Name")
    private String productCategoryName;

    @Field(description = "Category Title")
    private String productCategoryTitle;

    @Field(description = "Plan ID")
    private String planId;

    @Field(description = "Plan Title")
    private String planTitle;

    @Field(description = "Product Description")
    private final String description;

    @Field(description = "Quantity")
    private final BigDecimal quantity;

    @Field(description = "Price")
    private final BigDecimal price;

    @Field(description = "Total")
    private final BigDecimal total;

    @Field(description = "Asset Type")
    private final String assetType;

    @Field(description = "Asset IDs")
    private final String assetId;

    @Field(description = "Asset Detail")
    private final String assetDetail;

    @Field(description = "Active Since")
    private Date activeSince;

    @Field(description = "Active Until")
    private Date activeUntil;

    @Field(description = "Tax Rate")
    private BigDecimal taxRate;

    @Field(description = "Tax Amount")
    private BigDecimal taxAmount;

    @Field(description = "Gross Amount")
    private BigDecimal grossAmount;

    public InvoiceLineEnvelope(InvoiceLineDTO invoiceLine, Iterable<AssetEnvelope> assets, String defaultAssetIdLabel) {
        description = invoiceLine.getDescription();
        ItemDTO item = invoiceLine.getItem();
        if (item != null) {
            productCode = item.getInternalNumber();
            PlanDTO plan = findPlan(item);
            if (plan == null) {
                productCategoryName = findCategory(item);
                productCategoryTitle = productCategoryName;
            } else {
                planId = plan.getId().toString();
                planTitle = item.getDescription();
                productCategoryTitle = "Plans";
            }
        }
        if(invoiceLine.getOrder() != null){
            activeSince = invoiceLine.getOrder().getActiveSince();
            activeUntil = invoiceLine.getOrder().getActiveUntil();
        }
        quantity = invoiceLine.getQuantity();
        price = invoiceLine.getPrice();
        total = invoiceLine.getAmount();

        StringBuilder assetIdBuilder = new StringBuilder();

        Map<ItemTypeDTO, Collection<AssetEnvelope>> assetsByType = new HashMap<>();

        for (Iterator<AssetEnvelope> i = assets.iterator(); i.hasNext(); ) {
            AssetEnvelope asset = i.next();
            String assetIdentifier = asset.getIdentifier();
            assetIdBuilder.append(assetIdentifier);
            if (i.hasNext()) {
                assetIdBuilder.append("\n\r");
            }
            ItemTypeDTO assetItemType = asset.getItemType();
            if (!assetsByType.containsKey(assetItemType)) {
                assetsByType.put(assetItemType, new TreeSet<>(Comparator.comparing(AssetEnvelope::getIdentifier)));
            }
            assetsByType.get(assetItemType).add(asset);
        }

        StringBuilder assetTypeBuilder = new StringBuilder();
        StringBuilder assetDetailBuilder = new StringBuilder();

        for (Iterator<Entry<ItemTypeDTO, Collection<AssetEnvelope>>> iterator = assetsByType.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<ItemTypeDTO, Collection<AssetEnvelope>> entry = iterator.next();
            String assetType = entry.getKey().getDescription();
            String assetIdLabel = entry.getKey().getAssetIdentifierLabel();
            if (assetIdLabel == null || assetIdLabel.isEmpty()) {
                assetIdLabel = defaultAssetIdLabel;
            }
            Collection<AssetEnvelope> assetEnvelopeCollection = entry.getValue();
            assetDetailBuilder.append(assetType).append(" - ").append(assetIdLabel).append(assetEnvelopeCollection.size() > 1 ? "s" : "").append(": ");
            for (Iterator<AssetEnvelope> i = assetEnvelopeCollection.iterator(); i.hasNext(); ) {
                assetDetailBuilder.append(i.next().getIdentifier());
                if (i.hasNext()) {
                    assetDetailBuilder.append(", ");
                }
            }
            assetTypeBuilder.append(assetType);
            if (iterator.hasNext()) {
                assetTypeBuilder.append("\n\r");
                assetDetailBuilder.append("\n\r");
            }
        }

        assetId = assetIdBuilder.toString();
        assetType = assetTypeBuilder.toString();
        assetDetail = assetDetailBuilder.toString();
        taxRate = invoiceLine.getTaxRate();
        taxAmount = invoiceLine.getTaxAmount();
        grossAmount = invoiceLine.getGrossAmount();
    }

    public boolean isSameOrBigger(BigDecimal minimalTotal) {
        return minimalTotal == null || total == null || total.subtract(minimalTotal).signum() >= 0;
    }

    private String strVal(Number bigDecimal) {
        return bigDecimal == null ? null : String.valueOf(bigDecimal);
    }

    private String findCategory(ItemDTO item) {
        Set<ItemTypeDTO> itemTypes = item.getItemTypes();
        for (ItemTypeDTO itemType : itemTypes) {
            if (!itemType.isInternal()) {
                return itemType.getDescription();
            }
        }
        return null;
    }

    private PlanDTO findPlan(ItemDTO item) {
        Set<PlanDTO> plans = item.getPlans();
        if (plans != null && !plans.isEmpty()) {
            return plans.iterator().next();
        }
        return null;
    }


    public String getProductCode() {
        return productCode;
    }

    public String getProductCategoryName() {
        return productCategoryName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getProductCategoryTitle() {
        return productCategoryTitle;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanTitle() {
        return planTitle;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAssetDetail() {
        return assetDetail;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    public Date getActiveUntil() {
        return activeUntil;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

}
