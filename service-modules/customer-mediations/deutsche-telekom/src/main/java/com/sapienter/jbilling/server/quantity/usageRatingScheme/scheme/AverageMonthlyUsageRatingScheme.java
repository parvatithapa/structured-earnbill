package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme;


import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.UsageRatingSchemeAdapter;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeDefinition;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeIterable;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeUtils;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;
import com.sapienter.jbilling.server.util.QuantityRatingProperties;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants.CDR_DATE_FORMAT;
import static com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition.InputType.SELECT;
import static com.sapienter.jbilling.server.util.QuantityRatingConstants.PRECISION_PROPERTY;


/**
 *   - Handles conversion of data usage from/to {@link DataUnit} units.
 *   - Expects duration in the "quantity" parameter in "hours", so suitable
 *      rating unit must be applied to convert in hours
 *   - Returns Total consumption of data per month as per output @{@link DataUnit}
 *     units = (data * duration-in-hours)/hours-per-month
 */
public class AverageMonthlyUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(CDR_DATE_FORMAT);

    private static final String ATTR_DATA_INPUT_UNITS    = "I/p Data Units";
    private static final String ATTR_DATA_OUTPUT_UNITS   = "O/p Data Units";

    private static final Integer PRECISION;

    static {
        String precisionStr = QuantityRatingProperties.get(PRECISION_PROPERTY, "6");
        PRECISION = Integer.parseInt(precisionStr);
    }

    public enum DataUnit {
        Byte    (BigDecimal.ONE),
        KB      (Byte.factor.multiply(new BigDecimal(1024))),
        MB      (KB.factor.multiply(new BigDecimal(1024))),
        GB      (MB.factor.multiply(new BigDecimal(1024))),
        TB      (GB.factor.multiply(new BigDecimal(1024))),
        PB      (TB.factor.multiply(new BigDecimal(1024)));

        DataUnit(BigDecimal factor) {
            this.factor = factor;
        }

        private BigDecimal factor;

        public BigDecimal getFactor() {
            return factor;
        }

        public String getKey() {
            return name();
        }
    }

    private final List<IAttributeDefinition> fixedAttributes;


    public AverageMonthlyUsageRatingScheme() {
        this.fixedAttributes = new ArrayList<IAttributeDefinition>() {{
            add(AttributeDefinition.builder()
                    .name(ATTR_DATA_INPUT_UNITS)
                    .inputType(SELECT)
                    .required(true)
                    .iterable(dataUnitsIterable()).build());

            add(AttributeDefinition.builder()
                    .name(ATTR_DATA_OUTPUT_UNITS)
                    .inputType(SELECT)
                    .required(true)
                    .iterable(dataUnitsIterable()).build());
        }};
    }

    @Override
    public BigDecimal compute(IUsageRatingSchemeModel ratingScheme, BigDecimal quantity,
                              IUsageRecord usage, List<PricingField> fields) {

        if (quantity == null) {
            throw new QuantityRatingException("Quantity value is NULL");
        }

        PricingField extendParamsField = Optional
                .ofNullable(PricingField.find(fields, "ExtendParams"))
                .orElseThrow(() ->
                    new QuantityRatingException("Fatal: Mandatory pricing field 'ExtendParams' is NULL")
                );


        if (BigDecimal.ZERO.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Non-positive quantity");
        }

        BigDecimal duration = quantity;

        DataUnit from = DataUnit.valueOf(getStrValue(ratingScheme
                .getFixedAttributes(), ATTR_DATA_INPUT_UNITS,
                isFixedAttrReq(ATTR_DATA_INPUT_UNITS)));

        DataUnit to = DataUnit.valueOf(getStrValue(ratingScheme
                .getFixedAttributes(), ATTR_DATA_OUTPUT_UNITS,
                isFixedAttrReq(ATTR_DATA_INPUT_UNITS)));

        BigDecimal consumption = resolveExtendParamsValue(extendParamsField);
        BigDecimal scaledConsumption = scale(consumption, from, to);

        logger.info("duration: {}, consumption: {}, scaled consumption: {}",
                duration, consumption, scaledConsumption);

        PricingField endTimeField = Optional
          .ofNullable(PricingField.find(fields, "EndTime"))
          .orElseThrow(() ->
            new QuantityRatingException("Fatal: Mandatory pricing field 'EndTime' is NULL")
          );

        BigDecimal hoursPerMonth = hoursPerMonth(getEndTime(endTimeField.getStrValue()));
        BigDecimal result = duration
                .multiply(scaledConsumption)
                .divide(hoursPerMonth, PRECISION, RoundingMode.UP);

        logger.info("Resolved Quantity: {}", result);
        return result;
    }

    private BigDecimal hoursPerMonth(Long timeInMillis) {

        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return new BigDecimal(daysInMonth * 24);
    }

    private BigDecimal resolveExtendParamsValue(final PricingField pField) {

        String paramValue = Optional.ofNullable(pField.getStrValue())
                .orElseThrow(() ->
                        new QuantityRatingException(new StringBuilder("Illegal argument - Pricing field ")
                          .append(pField.getName())
                          .append(" is NULL or empty")
                          .toString())
                );

        paramValue = paramValue.trim();
        return AttributeUtils.parseDecimal(paramValue);
    }

    private BigDecimal scale(BigDecimal value, DataUnit from, DataUnit to) {
        BigDecimal factor;

        if (from.factor.compareTo(to.factor) > 0) {
            factor = from.factor.divide(to.factor);
            logger.info("from/to: {}, scaling up {} * {}", factor, value, factor);

            return value.multiply(factor);

        } else if (from.factor.compareTo(to.factor) < 0) {
            factor = to.factor.divide(from.factor);
            logger.info("to/from: {}, scaling down {} / {}", factor, value, factor);

            return value.divide(factor, PRECISION, RoundingMode.UP);
        }
        logger.info("from == to, returning original value {}", value);
        return value;
    }

    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return this.fixedAttributes;
    }

    private AttributeIterable<DataUnit> dataUnitsIterable() {

        Iterable<DataUnit> itr = EnumSet.allOf(DataUnit.class);
        return new AttributeIterable<>(itr, "key", "key");
    }

    private Long getEndTime(String strValue) {
        if (StringUtils.isBlank(strValue)) {
            throw new QuantityRatingException(String.format(
                    "Illegal argument - Pricing field 'EndTime' is NULL or empty"));
        }

        return DATE_TIME_FORMATTER.parseDateTime(strValue.trim()).toDate().getTime();
    }
}
