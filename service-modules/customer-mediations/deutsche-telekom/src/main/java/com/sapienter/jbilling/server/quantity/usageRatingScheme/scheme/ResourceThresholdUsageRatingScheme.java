package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme;

import static com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition.Type;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageConfiguration;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.UsageRatingSchemeAdapter;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeDefinition;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;


public class ResourceThresholdUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ATTR_THRESHOLD  = "Threshold";
    private static final String ATTR_VALUE      = "Value";

    private final List<IAttributeDefinition> fixedAttributes;


    public ResourceThresholdUsageRatingScheme() {
        this.fixedAttributes = new ArrayList<IAttributeDefinition>() {{
            add(AttributeDefinition.builder()
                    .name(ATTR_THRESHOLD)
                    .type(Type.INTEGER)
                    .required(true).build());

            add(AttributeDefinition.builder()
                    .name(ATTR_VALUE)
                    .type(Type.DECIMAL)
                    .required(true).build());
        }};
    }

    @SuppressWarnings("Duplicates")
    @Override
    public BigDecimal compute(IUsageRatingSchemeModel ratingScheme, BigDecimal quantity,
                              IUsageRecord usage, List<PricingField> fields) {

        if (quantity == null) {
            throw new QuantityRatingException("Quantity value is NULL");
        }

        if (BigDecimal.ZERO.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Non-positive quantity");
        }

        logger.info("Original quantity: {}", quantity);

        Integer thresholdIntVal = getIntValue(ratingScheme.getFixedAttributes(),
                ATTR_THRESHOLD, isFixedAttrReq(ATTR_THRESHOLD));

        BigDecimal threshold = new BigDecimal(thresholdIntVal);

        BigDecimal existingQuantity = getExistingQuantity(usage);

        if (existingQuantity.compareTo(threshold) >= 0) {
            logger.info("Existing quantity {} already >= threshold {}, returning resolved quantity ZERO",
                    existingQuantity, threshold);

            return BigDecimal.ZERO;
        }

        BigDecimal totalQuantity = getTotalQuantity(existingQuantity, quantity);

        if (totalQuantity.compareTo(threshold) >= 0) {
            logger.info("Total quantity {} >= threshold {} now, returning 'units' value",
                    totalQuantity, threshold);

            return getDecimalValue(ratingScheme.getFixedAttributes(),
                    ATTR_VALUE, isFixedAttrReq(ATTR_VALUE));
        }

        logger.info("Total quantity {} < threshold {}, returning resolved quantity ZERO",
                totalQuantity, threshold);

        return BigDecimal.ZERO;
    }

    @Override
    public void validate(IUsageRatingSchemeModel ratingSchemeModel) {

        Integer threshold = getIntValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_THRESHOLD, isFixedAttrReq(ATTR_THRESHOLD));

        BigDecimal units = getDecimalValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_VALUE, isFixedAttrReq(ATTR_VALUE));

        if (threshold <= 0) {
            throw new QuantityRatingException("Validation failed",
                    new String[]{ new StringBuilder().append("Field value '")
                      .append( ATTR_THRESHOLD)
                      .append( "' cannot be negative")
                      .toString() });
        }

        if (BigDecimal.ZERO.compareTo(units) > 0) {
            throw new QuantityRatingException("Validation failed",
                    new String[]{ new StringBuilder().append("Field value '")
                      .append( ATTR_VALUE)
                      .append( "' cannot be negative")
                      .toString() });
        }
    }

    @Override
    public IUsageConfiguration getUsageConfiguration() {

        final IUsageConfiguration defaultConfig = super.getUsageConfiguration();

        return new IUsageConfiguration() {

            IUsageConfiguration parent = defaultConfig;

            @Override
            public boolean requiresUsage() {
                return true;
            }

            @Override
            public Optional<IUsageRecord> getUsage(IUsageRatingSchemeModel model, IUsageQueryRecord query) {
                return getDefaultUsageRecordService().getItemResourceUsage(
                        query.getItemId(),
                        query.getUserId(),
                        query.getEntityId(),
                        query.getResourceId(),
                        query.getStartDate(),
                        query.getEndDate(),
                        () -> query.getKey(),
                        query.getMediationProcessId());
            }

            @Override
            public boolean hasResetCycle() {
                return parent.hasResetCycle();
            }

            @Override
            public Date getCycleStartDate(IUsageRatingSchemeModel model, Date eventDate) {
                return parent.getCycleStartDate(model, eventDate);
            }

            @Override
            public Date getCycleEndDate(IUsageRatingSchemeModel model, Date startDate) {
                return parent.getCycleEndDate(model, startDate);
            }
        };
    }

    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return this.fixedAttributes;
    }
}
