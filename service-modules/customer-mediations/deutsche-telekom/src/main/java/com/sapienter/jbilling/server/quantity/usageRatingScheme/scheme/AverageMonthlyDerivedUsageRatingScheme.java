package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme;

import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.UsageRatingSchemeAdapter;
import com.sapienter.jbilling.server.util.QuantityRatingProperties;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants.CDR_DATE_FORMAT;
import static com.sapienter.jbilling.server.util.QuantityRatingConstants.PRECISION_PROPERTY;


/**
 *   - Computes duration in seconds from the Pricing Fields BeginTime and EndTime
 *   - Expects data usage in the "quantity" parameter, a suitable rating unit may
 *      be applied so that it matches expected output units
 *   - Returns Total consumption of data per month in the same units as input data
 *     units = (data * duration-in-secs)/secs-per-month
 */
public class AverageMonthlyDerivedUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(CDR_DATE_FORMAT);

    private static final Integer PRECISION;

    static {
        String precisionStr = QuantityRatingProperties.get(PRECISION_PROPERTY, "6");
        PRECISION = Integer.parseInt(precisionStr);
    }

    public AverageMonthlyDerivedUsageRatingScheme() { }

    @Override
    public BigDecimal compute(IUsageRatingSchemeModel ratingScheme, BigDecimal quantity,
                              IUsageRecord usage, List<PricingField> fields) {

        if (quantity == null) {
            throw new QuantityRatingException("Quantity value is NULL");
        }

        PricingField beginTimeField = getPricingField(fields, "BeginTime");
        PricingField endTimeField   = getPricingField(fields, "EndTime");

        if (BigDecimal.ZERO.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Non-positive quantity");
        }

        Long endTime = getTime(endTimeField), beginTime = getTime(beginTimeField);
        Long diff = (endTime - beginTime) / 1000;
        if (diff < 0) {
            throw new QuantityRatingException(new StringBuilder().append("Illegal arguments - EndTime: ")
                    .append(endTimeField.getStrValue())
                    .append(" is BEFORE BeginTime: " )
                    .append(beginTimeField.getStrValue())
                    .toString());
        }

        BigDecimal consumption = quantity;
        BigDecimal duration = BigDecimal.valueOf(diff);

        logger.info("duration: {}, consumption: {}", duration, consumption);

        BigDecimal secondsPerMonth = secondsPerMonth(endTime);

        BigDecimal result = duration
                .multiply(consumption)
                .divide(secondsPerMonth, PRECISION, RoundingMode.UP);

        logger.info("Resolved Quantity: {}", result);
        return result;
    }

    private BigDecimal secondsPerMonth(Long timeInMillis) {

        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return new BigDecimal(daysInMonth * 24 * 3600);
    }

    private PricingField getPricingField(List<PricingField> fields, final String name) {
        return Optional
                .ofNullable(PricingField.find(fields, name))
                .orElseThrow(() ->
                    new QuantityRatingException(String.format(
                        "Fatal: Mandatory pricing field '%s' is NULL", name))
                );
    }

    private Long getTime(PricingField pricingField) {
        String strValue = pricingField.getStrValue();

        if (StringUtils.isBlank(strValue)) {
            throw new QuantityRatingException(String.format(
                "Illegal argument - Pricing field %s is NULL or empty", pricingField.getName()));
        }

        return DATE_TIME_FORMATTER.parseDateTime(strValue.trim()).toDate().getTime();
    }
}
