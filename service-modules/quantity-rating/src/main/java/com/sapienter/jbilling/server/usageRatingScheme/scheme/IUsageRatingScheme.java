package com.sapienter.jbilling.server.usageRatingScheme.scheme;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition;

import java.math.BigDecimal;
import java.util.List;


public interface IUsageRatingScheme {

    IUsageConfiguration getUsageConfiguration();

    List<IAttributeDefinition> getFixedAttributes();

    List<IAttributeDefinition> getDynamicAttributes();

    boolean usesDynamicAttributes();

    String getDynamicAttributeName();

    void validate(IUsageRatingSchemeModel ratingSchemeModel);

    BigDecimal compute(IUsageRatingSchemeModel ratingScheme,
                       BigDecimal quantity,
                       IUsageRecord usage,
                       List<PricingField> fields);
}
