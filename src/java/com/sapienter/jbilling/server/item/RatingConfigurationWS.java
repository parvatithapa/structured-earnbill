package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class RatingConfigurationWS implements Serializable {

    private UsageRatingSchemeWS usageRatingScheme;
    private RatingUnitWS ratingUnit;

    private int id;

    private List<InternationalDescriptionWS> pricingUnit =new ArrayList<>();


    public RatingConfigurationWS(){}

    public RatingConfigurationWS(RatingUnitWS ratingUnit,UsageRatingSchemeWS usageRatingScheme){
        this.ratingUnit=ratingUnit;
        this.usageRatingScheme=usageRatingScheme;
    }
}
