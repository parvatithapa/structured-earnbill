/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.user.CompanyWS;

/**
 * Created by Andres Canevaro on 10/08/15.
 */
public class RatingSchemeAssociationWS {

    Integer id;
    Integer ratingScheme;
    MediationConfigurationWS mediation;
    CompanyWS company;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MediationConfigurationWS getMediation() {
        return mediation;
    }

    public void setMediation(MediationConfigurationWS mediation) {
        this.mediation = mediation;
    }

    public CompanyWS getCompany() {
        return company;
    }

    public void setCompany(CompanyWS company) {
        this.company = company;
    }

    public Integer getRatingScheme() {
        return ratingScheme;
    }

    public void setRatingScheme(Integer ratingScheme) {
        this.ratingScheme = ratingScheme;
    }

}
