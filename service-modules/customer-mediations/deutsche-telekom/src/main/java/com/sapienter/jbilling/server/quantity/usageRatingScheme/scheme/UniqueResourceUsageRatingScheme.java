package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme;

import static com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition.InputType.SELECT;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageConfiguration;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.UsageRatingSchemeAdapter;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeDefinition;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeIterable;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;
import com.sapienter.jbilling.server.usageRatingScheme.util.ResetCycle;


public class UniqueResourceUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ATTR_RESET = "Reset Cycle";

    private final List<IAttributeDefinition> fixedAttributes;

    public UniqueResourceUsageRatingScheme() {
        this.fixedAttributes = new ArrayList<IAttributeDefinition>() {{
            add(AttributeDefinition.builder()
                    .name(ATTR_RESET)
                    .inputType(SELECT)
                    .required(true)
                    .iterable(resetCycleIterable()).build());
        }};
    }

    @Override
    public BigDecimal compute(IUsageRatingSchemeModel ratingScheme, BigDecimal quantity,
                              IUsageRecord usage, List<PricingField> fields) {

        if (quantity == null) {
            throw new QuantityRatingException("Quantity value is NULL");
        }

        if (BigDecimal.ZERO.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Non-positive quantitys");
        }

        BigDecimal existingQuantity = getExistingQuantity(usage);

        logger.info("Resource already accounted for ? {}",
                existingQuantity.compareTo(BigDecimal.ZERO) > 0);

        return existingQuantity.compareTo(BigDecimal.ZERO) > 0 ?
                BigDecimal.ZERO : BigDecimal.ONE;
    }

    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return this.fixedAttributes;
    }

    @Override
    public IUsageConfiguration getUsageConfiguration() {

        return new IUsageConfiguration() {

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
                return true;
            }

            @Override
            public Date getCycleStartDate(IUsageRatingSchemeModel model, Date eventDate) {
                String resetCycleStr = getStrValue(model.getFixedAttributes(),
                        ATTR_RESET, isFixedAttrReq(ATTR_RESET));

                ResetCycle resetCycle = ResetCycle.valueOf(resetCycleStr);

                Calendar cal = GregorianCalendar.getInstance();
                cal.setTime(eventDate);

                if (resetCycle != ResetCycle.DAY) {
                    cal.set(resetCycle.getDayOfCycle(), 1);
                }
                return Util.truncateDate(cal.getTime());
            }

            @Override
            public Date getCycleEndDate(IUsageRatingSchemeModel model, Date startDate) {
                String resetCycleStr = getStrValue(model.getFixedAttributes(),
                        ATTR_RESET, isFixedAttrReq(ATTR_RESET));

                ResetCycle resetCycle = ResetCycle.valueOf(resetCycleStr);
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(startDate);
                calendar.add(resetCycle.map(), 1);

                return calendar.getTime();
            }
        };
    }

    private AttributeIterable<ResetCycle> resetCycleIterable() {
        Iterable<ResetCycle> itr = EnumSet.allOf(ResetCycle.class);
        return new AttributeIterable<>(itr, "key", "key");
    }
}
