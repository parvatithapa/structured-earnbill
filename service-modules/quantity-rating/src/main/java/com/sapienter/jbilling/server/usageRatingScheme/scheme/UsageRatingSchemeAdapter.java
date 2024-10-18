package com.sapienter.jbilling.server.usageRatingScheme.scheme;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.quantity.usage.service.IUsageRecordService;
import com.sapienter.jbilling.server.quantity.usage.service.UsageRecordServiceFactory;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeUtils;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;
import com.sapienter.jbilling.server.util.Context;


public abstract class UsageRatingSchemeAdapter implements IUsageRatingScheme {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GET_VALUE_LOG="Attr key {}, value {}";
    private static final String UNSUPPORTED_MESSAGE="Unsupported for rating scheme";


    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return Collections.emptyList();
    }

    @Override
    public List<IAttributeDefinition> getDynamicAttributes() {
        return Collections.emptyList();
    }

    @Override
    public boolean usesDynamicAttributes() {
        return false;
    }

    @Override
    public String getDynamicAttributeName() {
        throw new QuantityRatingException(UNSUPPORTED_MESSAGE, new UnsupportedOperationException());
    }

    @Override
    public void validate(IUsageRatingSchemeModel ratingSchemeModel) {
        // pass through
    }

    @Override
    public IUsageConfiguration getUsageConfiguration() {
        return new IUsageConfiguration() {
            @Override
            public boolean requiresUsage() {
                return false;
            }

            @Override
            public Optional<IUsageRecord> getUsage(IUsageRatingSchemeModel model, IUsageQueryRecord query) {
                throw new QuantityRatingException(UNSUPPORTED_MESSAGE, new UnsupportedOperationException());
            }

            @Override
            public boolean hasResetCycle() {
                return false;
            }

            @Override
            public Date getCycleStartDate(IUsageRatingSchemeModel model, Date eventDate) {
                throw new QuantityRatingException(UNSUPPORTED_MESSAGE, new UnsupportedOperationException());
            }

            @Override
            public Date getCycleEndDate(IUsageRatingSchemeModel model, Date startDate) {
                throw new QuantityRatingException(UNSUPPORTED_MESSAGE, new UnsupportedOperationException());
            }
        };
    }


    //  Useful methods accessible to inheriting classes

    protected BigDecimal getExistingQuantity(IUsageRecord usage) {
        BigDecimal existingQty = Optional.ofNullable(usage.getQuantity())
                .orElse(BigDecimal.ZERO);

        logger.info("Existing quantity value {}", existingQty);
        return existingQty;
    }

    protected BigDecimal getTotalQuantity(BigDecimal existingQuantity, BigDecimal quantity) {
        BigDecimal totalQty = existingQuantity.add(quantity);

        logger.info("Total quantity value {}", totalQty);
        return totalQty;
    }


    protected Integer getIntValue(Map<String, String> attributes, String key, boolean required) {

        String attrVal = getAttributeValue(attributes, key, required);

        Integer val = Optional
                .ofNullable(AttributeUtils.parseInteger(attrVal))
                .orElse(Integer.MIN_VALUE);

        logger.info(GET_VALUE_LOG, key, val);
        return val;
    }

    protected String getStrValue(Map<String, String> attributes, String key, boolean required) {

        String attrVal = getAttributeValue(attributes, key, required);

        logger.info(GET_VALUE_LOG, key, attrVal);
        return attrVal;
    }

    protected BigDecimal getDecimalValue(Map<String, String> attributes, String key, boolean required) {

        String attrVal = getAttributeValue(attributes, key, required);

        BigDecimal val = Optional
                .ofNullable(AttributeUtils.parseDecimal(attrVal))
                .orElse(BigDecimal.valueOf(Double.MIN_VALUE));

        logger.info(GET_VALUE_LOG, key, val);
        return val;
    }

    protected IUsageRecordService getDefaultUsageRecordService() {
        UsageRecordServiceFactory factory = Context.getBean(UsageRecordServiceFactory.BEAN_NAME);
        return factory.getService(IUsageRecordService.DEFAULT_SERVICE_BEAN_NAME);
    }

    private String getAttributeValue(Map<String, String> attributes, final String key, boolean required) {

        Optional<String> opt = Optional
                .ofNullable(attributes.get(key))
                .filter(s -> !s.isEmpty());

        String attrValue;
        if (required) {
            attrValue = opt.orElseThrow(() ->
                    new QuantityRatingException(String.format(
                            "Attribute %s missing in the rating scheme configuration", key))
            );
        } else {
            attrValue = opt.orElse(StringUtils.EMPTY);
        }

        logger.debug("Attr Value Str {}", attrValue);
        return attrValue;
    }

    protected boolean isFixedAttrReq(String attrName) {
        return getFixedAttributes().stream()
                .filter(attr -> attrName.equalsIgnoreCase(attr.getName()))
                .findFirst()
                .orElseThrow(() ->
                    new QuantityRatingException(String.format("Attribute not found", attrName))
                )
                .isRequired();
    }

    protected boolean isDynamicAttrReq(String attrName) {
        return getDynamicAttributes().stream()
                .filter(attr -> attrName.equalsIgnoreCase(attr.getName()))
                .findFirst()
                .orElseThrow(() ->
                        new QuantityRatingException(String.format("Attribute not found", attrName))
                )
                .isRequired();
    }
}
