package com.sapienter.jbilling.server.usageratingscheme.scheme;


import static com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition.Type;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

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


public class TieredUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ATTR_FROM   = "From";
    private static final String ATTR_VALUE  = "Value";
    private static final String ATTR_TIERS  = "Tiers";
    private static final String VALIDATION_FAILED="Validation failed";
    private final List<IAttributeDefinition> dynamicAttributes;


    public TieredUsageRatingScheme() {
        this.dynamicAttributes = new ArrayList<IAttributeDefinition>() {{
            add(AttributeDefinition.builder()
                    .name(ATTR_FROM)
                    .type(Type.INTEGER)
                    .required(true).build());

            add(AttributeDefinition.builder()
                    .name(ATTR_VALUE)
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

        SortedMap<BigDecimal, BigDecimal> tiers =
                getTiers(ratingScheme.getDynamicAttributes());

        BigDecimal existingQuantity = getExistingQuantity(usage);

        BigDecimal totalQuantity = getTotalQuantity(existingQuantity, quantity);

        BigDecimal result = compute(tiers, existingQuantity, totalQuantity);

        logger.info("Resolved quantity {}", result);

        return result;
    }

    private BigDecimal compute(SortedMap<BigDecimal, BigDecimal> tiers, BigDecimal existingQuantity,
                               BigDecimal totalQuantity) {

        BigDecimal existingVal = BigDecimal.ZERO;
        BigDecimal currentVal = BigDecimal.ZERO;

        for (Map.Entry<BigDecimal, BigDecimal> currentTier : tiers.entrySet()) {

            logger.info("Current Tier {}", currentTier.getKey());

            if (existingQuantity.compareTo(currentTier.getKey()) >= 0) {
                logger.info("Existing quantity {} >= currentTier, onto next tier", existingQuantity);
                existingVal = currentVal = currentTier.getValue();
                continue;
            }

            if (totalQuantity.compareTo(currentTier.getKey()) < 0) {
                logger.info("Total quantity {} < currentTier, we're done!", totalQuantity);
                break;
            }

            currentVal = currentTier.getValue();
            logger.info("Total quantity {} >= currentTier, updated current val to {}",
                    totalQuantity, currentVal);
        }

        BigDecimal increment = currentVal.subtract(existingVal);
        logger.info("{} is the resolved quantity", increment);

        return increment;
    }

    private SortedMap<BigDecimal, BigDecimal> getTiers(List<Map<String, String>> dynamicAttributes) {

        SortedMap<BigDecimal, BigDecimal> tiers = new TreeMap<>();

        for (Map<String, String> line : dynamicAttributes) {
            BigDecimal from = new BigDecimal(getIntValue(line,
                    ATTR_FROM, isDynamicAttrReq(ATTR_FROM)));

            if (BigDecimal.ZERO.equals(from)) {
                logger.debug("Adjusting startValue {}", from);
                from = BigDecimal.ONE;
            }

            tiers.put(from, getDecimalValue(line, ATTR_VALUE, isDynamicAttrReq(ATTR_VALUE)));
        }

        if (tiers.isEmpty()) {
            throw new QuantityRatingException("Fatal - No tiers defined");
        }

        logger.info("Tiers(From, Units) :: {}", tiers);
        return tiers;
    }

    @Override
    public List<IAttributeDefinition> getDynamicAttributes() {
        return this.dynamicAttributes;
    }

    @Override
    public boolean usesDynamicAttributes() {
        return true;
    }

    @Override
    public String getDynamicAttributeName() {
        return ATTR_TIERS;
    }

    @Override
    public void validate(IUsageRatingSchemeModel ratingSchemeModel) {

        List<Map<String, String>> dynamicAttrs = ratingSchemeModel.getDynamicAttributes();
        LinkedHashMap<Integer, BigDecimal> tiers = new LinkedHashMap<>();

        for (Map<String, String> attrs : dynamicAttrs) {

            Integer key = getIntValue(attrs, ATTR_FROM, isDynamicAttrReq(ATTR_FROM));
            BigDecimal val = getDecimalValue(attrs, ATTR_VALUE, isDynamicAttrReq(ATTR_VALUE));

            BigDecimal old = tiers.putIfAbsent(key, val);

            if (old != null) {
                throw new QuantityRatingException(VALIDATION_FAILED,
                        new String[] { new StringBuilder().append("Duplicate value ")
                          .append(key)
                          .append(" for field '")
                          .append( ATTR_FROM )
                          .append("' ").toString()
                        });
            }
        }

        Integer prevTierFrom = -1;
        BigDecimal prevTierUnits = BigDecimal.ZERO;

        for (Map.Entry<Integer, BigDecimal> tier: tiers.entrySet()) {
            if (tier.getKey() < 0) {
                throw new QuantityRatingException(VALIDATION_FAILED,
                        new String[]{ new StringBuilder().append("Field value '")
                          .append(ATTR_FROM)
                          .append("' cannot be negative")
                          .toString()
                        });
            }

            if (tier.getKey() <= prevTierFrom) {
                throw new QuantityRatingException(VALIDATION_FAILED,
                        new String[]{ new StringBuilder().append("Field '")
                          .append( ATTR_FROM)
                          .append("' with value ")
                          .append(tier.getKey())
                          .append(" cannot be less than previous value ")
                          .append(prevTierFrom).toString()
                        });
            }

            if (BigDecimal.ZERO.compareTo(tier.getValue()) > 0) {
                throw new QuantityRatingException(VALIDATION_FAILED,
                        new String[]{ new StringBuilder().append("Field value '")
                          .append(ATTR_VALUE)
                          .append("' cannot be negative")
                          .toString()
                        });
            }

            if (tier.getValue().compareTo(prevTierUnits) < 0) {
                throw new QuantityRatingException(VALIDATION_FAILED,
                        new String[]{ new StringBuilder().append("Field '")
                          .append(ATTR_VALUE)
                          .append("' with value ")
                          .append(tier.getValue())
                          .append(" cannot be less than previous value ")
                          .append(prevTierUnits).toString()
                        });
            }

            prevTierFrom = tier.getKey();
            prevTierUnits = tier.getValue();
        }
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
