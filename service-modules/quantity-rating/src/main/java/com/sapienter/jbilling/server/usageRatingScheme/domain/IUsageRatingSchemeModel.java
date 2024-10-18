package com.sapienter.jbilling.server.usageRatingScheme.domain;

import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;

import java.util.List;
import java.util.Map;


public interface IUsageRatingSchemeModel {

    String getRatingSchemeCode();

    Map<String, String> getFixedAttributes();

    IUsageRatingScheme getRatingScheme();

    List<Map<String, String>> getDynamicAttributes();
}
