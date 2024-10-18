package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme;

import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.UsageRatingSchemeAdapter;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeDefinition;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition.Type;


public class ConstantUsageRatingScheme extends UsageRatingSchemeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ATTR_VALUE = "Value(Constant)";

    private final List<IAttributeDefinition> fixedAttributes;

    public ConstantUsageRatingScheme() {
        this.fixedAttributes = new ArrayList<IAttributeDefinition>() {{
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
        
        logger.info("Original quantity: {}", quantity);

        BigDecimal result = getDecimalValue(ratingScheme.getFixedAttributes(),
                ATTR_VALUE, isFixedAttrReq(ATTR_VALUE));
        
        logger.info("Resolved Quantity: {}", result);

        return result;
    }

    @Override
    public void validate(IUsageRatingSchemeModel ratingSchemeModel) {

        BigDecimal value = getDecimalValue(ratingSchemeModel.getFixedAttributes(),
                ATTR_VALUE, isFixedAttrReq(ATTR_VALUE));

        if (BigDecimal.ZERO.compareTo(value) > 0) {
            throw new QuantityRatingException("Validation failed",
                    new String[] { new StringBuilder().append("Field value '")
                            .append(ATTR_VALUE)
                            .append( "' cannot be negative")
                            .toString() 
            });
        }
    }

    @Override
    public List<IAttributeDefinition> getFixedAttributes() {
        return this.fixedAttributes;
    }
}
