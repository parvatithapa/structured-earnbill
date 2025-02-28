/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * AbstractPricingStrategy
 *
 * @author Brian Cowdery
 * @since 07/02/11
 */
public abstract class AbstractPricingStrategy implements PricingStrategy {

	public static final Logger LOG = Logger.getLogger(AbstractPricingStrategy.class);
	
    private List<AttributeDefinition> attributeDefinitions = Collections.emptyList();
    private List<ChainPosition> chainPositions = Collections.emptyList();
    private boolean requiresUsage = false;
    private boolean variableUsagePricing = false;
    private boolean usesDynamicAttributes = false;

    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(AttributeDefinition ...attributeDefinitions) {
        this.attributeDefinitions = Collections.unmodifiableList(Arrays.asList(attributeDefinitions));
    }

    public List<ChainPosition> getChainPositions() {
        return chainPositions;
    }

    public void setChainPositions(ChainPosition ...chainPositions) {
        this.chainPositions = Collections.unmodifiableList(Arrays.asList(chainPositions));
    }

    public boolean requiresUsage() {
        return requiresUsage;
    }

    public void setRequiresUsage(boolean requiresUsage) {
        this.requiresUsage = requiresUsage;
    }

    public void setUsesDynamicAttributes(boolean usesDynamicAttributes) {
        this.usesDynamicAttributes = usesDynamicAttributes;
    }

    @Override
    public boolean usesDynamicAttributes() {
        return usesDynamicAttributes;
    }

    public static BigDecimal getTotalQuantity(OrderDTO pricingOrder, Usage usage, BigDecimal quantity, boolean singlePurchase) {
        if (singlePurchase || pricingOrder == null) {
            return usage.getQuantity().add(quantity);
        }
        return usage.getQuantity();
    }

    public static BigDecimal getExistingQuantity(OrderDTO pricingOrder, Usage usage, BigDecimal quantity, boolean singlePurchase) {
        if (singlePurchase || pricingOrder == null) {
            return usage.getQuantity();
        }
        return usage.getQuantity().subtract(usage.getCurrentQuantity());
    }

    public static BigDecimal getCurrentQuantity(OrderDTO pricingOrder, Usage usage, BigDecimal quantity, boolean singlePurchase) {
        if (singlePurchase || pricingOrder == null) {
            return quantity;
        }
        return usage.getCurrentQuantity();
    }

    public static BigDecimal getPercentageRate(OrderDTO pricingOrder, Usage usage, BigDecimal currentUsageQuantity,
                                               BigDecimal quantity, boolean singlePurchase) {

        if (singlePurchase || pricingOrder == null) {
            return quantity;
        }

        return currentUsageQuantity.compareTo(BigDecimal.ONE) > 0 ? currentUsageQuantity:quantity;
    }


	public boolean isVariableUsagePricing() {
		return variableUsagePricing;
	}

	/**setVariableUsagePricing true if pricing strategy calculate price on the 
	 * Basis of Usage or Quantity. or if price varies on the basis of Usage
	 * @param variableUsagePricing
	 */
	public void setVariableUsagePricing(boolean variableUsagePricing) {
		this.variableUsagePricing = variableUsagePricing;
	}

    /**
     * Looks for a pricing field by name.
     *
     * @param fields fields passed to the pricing engine
     * @param fieldName field name to search for
     * @return pricing field if found, null if not
     */
    public static PricingField find(List<PricingField> fields, String fieldName) {
        if (fields != null && fieldName != null) {
            for (PricingField field : fields) {
                if (field.getName().equals(fieldName))
                    return field;
            }
        }
        return null;
    }
    
}