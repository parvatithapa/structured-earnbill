package com.sapienter.jbilling.server.usageratingscheme.scheme;


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


public class TieredLinearUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ATTR_START      = "Start";
    private static final String ATTR_SIZE       = "Size";
    private static final String ATTR_INCREMENT  = "Increment";
    private static final String FIELD_VALUE="Field value";
    private static final String VALIDATION_FAILED="Validation failed";

    private final List<IAttributeDefinition> fixedAttributes;


    public TieredLinearUsageRatingScheme() {
        this.fixedAttributes = new ArrayList<IAttributeDefinition>() {{
            add(AttributeDefinition.builder()
                    .name(ATTR_START)
                    .type(Type.INTEGER)
                    .required(false).build());

            add(AttributeDefinition.builder()
                    .name(ATTR_SIZE)
                    .type(Type.INTEGER)
                    .required(true).build());

            add(AttributeDefinition.builder()
                    .name(ATTR_INCREMENT)
                    .type(Type.DECIMAL)
                    .required(true).build());
        }};
    }


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

        BigDecimal existingQuantity = getExistingQuantity(usage);

        BigDecimal totalQuantity = getTotalQuantity(existingQuantity, quantity);

        Integer size = getIntValue(ratingScheme.getFixedAttributes(),
                ATTR_SIZE, isFixedAttrReq(ATTR_SIZE));

        Integer start = getIntValue(ratingScheme.getFixedAttributes(),
                ATTR_START, isFixedAttrReq(ATTR_START));

        if (start == Integer.MIN_VALUE) {
            start = size;
        }

        BigDecimal increment = getDecimalValue(ratingScheme.getFixedAttributes(),
                ATTR_INCREMENT, isFixedAttrReq(ATTR_INCREMENT));

        BigDecimal result = compute(start, size, increment, existingQuantity, totalQuantity);

        logger.info("Resolved quantity {}", result);

        return result;
    }


    private BigDecimal compute(Integer startIntVal, Integer sizeIntVal, BigDecimal increment,
                               BigDecimal existingQuantity, BigDecimal totalQuantity) {

        BigDecimal start  = new BigDecimal(startIntVal);
        BigDecimal size   = new BigDecimal(sizeIntVal);
        BigDecimal result = BigDecimal.ZERO;

        boolean escapeZeroTier = BigDecimal.ZERO.compareTo(start) == 0 &&
                BigDecimal.ZERO.compareTo(existingQuantity) == 0 &&
                totalQuantity.compareTo(BigDecimal.ZERO) > 0;

        for (BigDecimal currentTier = start; totalQuantity.compareTo(currentTier) >= 0;
             currentTier = currentTier.add(size)) {

            logger.info("Current Tier {}", currentTier);

            if (escapeZeroTier) {
                escapeZeroTier = false;

            } else if (existingQuantity.compareTo(currentTier) >= 0) {
                logger.info("Existing quantity {} >= currentTier, move over to next tier", existingQuantity);
                continue;
            }

            logger.info("Total quantity {} >= currentTier, adding {} to resolved quantity", totalQuantity, increment);
            result = result.add(increment);
        }

        return result;
    }

    @Override
    public void validate(IUsageRatingSchemeModel ratingSchemeModel) {

        Integer start = getIntValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_START, isFixedAttrReq(ATTR_START));

        Integer size = getIntValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_SIZE, isFixedAttrReq(ATTR_SIZE));

        BigDecimal increment = getDecimalValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_INCREMENT, isFixedAttrReq(ATTR_INCREMENT));

        if (start != Integer.MIN_VALUE && start < 0) {
            throw new QuantityRatingException(VALIDATION_FAILED,
                    new String[] { new StringBuilder().append(FIELD_VALUE)
                        .append("'")
                        .append(ATTR_START)
                        .append("' cannot be negative")
                        .toString()
                    });
        }

        if (size <= 0) {
            throw new QuantityRatingException(VALIDATION_FAILED,
                    new String[] { new StringBuilder().append(FIELD_VALUE)
                        .append("'")
                        .append(ATTR_SIZE)
                        .append("' cannot be negative or zero")
                        .toString()
                    });
        }

        if (BigDecimal.ZERO.compareTo(increment) > 0) {
            throw new QuantityRatingException(VALIDATION_FAILED,
                    new String[] { new StringBuilder().append(FIELD_VALUE)
                        .append("'")
                        .append(ATTR_INCREMENT)
                        .append("' cannot be negative")
                        .toString()
                    });
        }
    }

    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return this.fixedAttributes;
    }


    @Override
    public IUsageConfiguration getUsageConfiguration() {

        final IUsageConfiguration defaultConfig = super.getUsageConfiguration();

        //noinspection Duplicates
        return new IUsageConfiguration() {

            IUsageConfiguration parent = defaultConfig;

            @Override
            public boolean requiresUsage() {
                return true;
            }

            @Override
            public Optional<IUsageRecord> getUsage(IUsageRatingSchemeModel model, IUsageQueryRecord query) {
                return getDefaultUsageRecordService().getItemUsage(
                        query.getItemId(),
                        query.getUserId(),
                        query.getEntityId(),
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
}
