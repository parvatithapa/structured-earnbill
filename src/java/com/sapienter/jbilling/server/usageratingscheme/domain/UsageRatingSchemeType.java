package com.sapienter.jbilling.server.usageratingscheme.domain;

import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;


public class UsageRatingSchemeType {

    private final String name;
    private final IUsageRatingScheme usageRatingScheme;

    public UsageRatingSchemeType(String name, IUsageRatingScheme usageRatingScheme) {
        this.name = name;
        this.usageRatingScheme = usageRatingScheme;
    }

    public String getName() {
        return name;
    }

    public IUsageRatingScheme getUsageRatingScheme() {
        return usageRatingScheme;
    }

    @Override
    public String toString() {

        return new StringBuilder().append("UsageRatingSchemeType { ")
          .append("name='").append(name)
          .append('\'')
          .append(", usageRatingScheme=")
          .append(usageRatingScheme)
          .append(" }")
          .toString();
    }
}
