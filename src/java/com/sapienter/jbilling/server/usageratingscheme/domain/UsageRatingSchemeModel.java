package com.sapienter.jbilling.server.usageratingscheme.domain;

import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;


public class UsageRatingSchemeModel implements IUsageRatingSchemeModel {

    private final String ratingSchemeCode;
    private final Map<String, String> fixedAttributes;
    private final IUsageRatingScheme ratingScheme;
    private final List<Map<String, String>> dynamicAttributes;


    public UsageRatingSchemeModel(String ratingSchemeCode, IUsageRatingScheme ratingScheme,
                                  Map<String, String> fixedAttributes,
                                  List<Map<String, String>> dynamicAttributes) {

        this.ratingSchemeCode = ratingSchemeCode;
        this.fixedAttributes = fixedAttributes;
        this.ratingScheme = ratingScheme;
        this.dynamicAttributes = dynamicAttributes;
    }

    @Override
    public String getRatingSchemeCode() {
        return this.ratingSchemeCode;
    }

    @Override
    public Map<String, String> getFixedAttributes() {
        return this.fixedAttributes;
    }

    @Override
    public List<Map<String, String>> getDynamicAttributes() {
        return this.dynamicAttributes;
    }

    @Override
    public IUsageRatingScheme getRatingScheme() {
        return ratingScheme;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("UsageRatingSchemeModel { ")
                .append("ratingSchemeCode='")
                .append(ratingSchemeCode)
                .append('\'')
                .append(", ratingScheme=")
                .append(ratingScheme)
                .append(", fixedAttributes=")
                .append(fixedAttributes)
                .append(", dynamicAttributes=")
                .append(dynamicAttributes)
                .append(" }").toString();
    }
}
