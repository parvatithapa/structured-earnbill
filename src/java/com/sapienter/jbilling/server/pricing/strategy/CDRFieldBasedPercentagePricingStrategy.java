package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

/**
 * This pricing strategy is meant to be used by products that are rated i.e.
 * used by the mediation process. In the telco industry sometimes the MVNOs
 * do not define well known price for product but they define a price of a product
 * to be X percent more than what the MNO is charging. The MNO provide the price
 * of the charge through the CDRs data. This means that the original (MNO) price
 * is defined somewhere in the pricing fields. This pricing strategy locates the
 * the original (MNO) price in the pricing fields and if needed it will calculate
 * extra percentage on top of that price. The percentage can be positive or negative.
 *
 * This pricing strategy defines two attributes:
 *
 * field_name: the name of the pricing field where the original (MNO) price is located;
 * percentage: the percentage that is to be added on top of the original price. It can be
 *             left blank which means that no percentage should be added to the price. It
 *             can also be positive or negative.
 *
 * @author Vladimir Carevski
 * @since 17-DEC-2013
 *
 */
public class CDRFieldBasedPercentagePricingStrategy extends AbstractPricingStrategy {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CDRFieldBasedPercentagePricingStrategy.class));
    private static final String FIELD_NAME = "rate_pricing_field_name";
    private static final String PERCENTAGE = "apply_percentage";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public CDRFieldBasedPercentagePricingStrategy(){
        setAttributeDefinitions(
            new AttributeDefinition(FIELD_NAME, STRING, true),
            new AttributeDefinition(PERCENTAGE, DECIMAL, true)
        );

        setChainPositions(
            ChainPosition.START
        );

        setRequiresUsage(false);
        setVariableUsagePricing(false);
    }

    public void applyTo(
            OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
            PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        String fieldName = getFieldName(planPrice);
        if(null != fieldName){
            BigDecimal fieldPrice = getPriceFromFields(fieldName, fields);

            if(null != fieldPrice){

                //if percentage is defined add percentage amount on the original value
                BigDecimal percentage = getPercentage(planPrice);
                if(null != percentage){
                    fieldPrice = fieldPrice.add(fieldPrice.multiply(percentage).divide(HUNDRED, 20, RoundingMode.HALF_UP));
                }

                result.setPrice(fieldPrice);
            } else {
                LOG.debug("Price is not defined in the designate pricing field. Field name: %s", fieldName);
            }

        } else {
            LOG.debug("Can not determine pricing field name for pricing model");
        }
    }

    public String getFieldName(PriceModelDTO planPrice) {
        String fieldName = planPrice.getAttributes().get(FIELD_NAME);
        if(null != fieldName && fieldName.trim().isEmpty()) return null;
        return fieldName;
    }

    /**
     * Returns the percentage defined as attribute on this pricing model.
     * It can be null.
     */
    public BigDecimal getPercentage(PriceModelDTO planPrice) {
        String percentage = planPrice.getAttributes().get(PERCENTAGE);
        if (null == percentage) return null;
        if (null != percentage && percentage.trim().isEmpty()) return null;

        try {
            return new BigDecimal(percentage);
        } catch (NumberFormatException nfe) {
            String message = String.format("Percentage field on is not correctly formatted. Value: %s", percentage);
            LOG.info(message);
            throw new SessionInternalError(message, nfe);
        }
    }

    /**
     * Returns the price defined in a field with a given name. If the field the
     * field is empty this method will return null and if the value in the field is
     * not parsable this method will throw exception.
     *
     * @param fieldName - the name of the pricing field
     * @param fields    - list of all available pricing field
     * @return BigDecimal the price defined in the pricing field, null if the
     *         pricing field does not exists or it is empty
     */
    public BigDecimal getPriceFromFields(String fieldName, List<PricingField> fields) {
        PricingField priceField = PricingField.find(fields, fieldName);
        if (null == priceField) return null;
        if (null != priceField.getStrValue() && priceField.getStrValue().trim().isEmpty()) return null;

        try {
            return new BigDecimal(priceField.getStrValue());
        } catch (NumberFormatException nfe) {
            String message = String.format("Price from pricing field is not correctly formatted. Field Name: %s, Value: %s", fieldName, priceField.getStrValue());
            LOG.info(message);
            throw new SessionInternalError(message, nfe);
        }
    }

}
