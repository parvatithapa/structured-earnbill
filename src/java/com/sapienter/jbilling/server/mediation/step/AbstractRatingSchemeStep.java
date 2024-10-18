/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.step;

import com.sapienter.jbilling.server.pricing.RatingSchemeBL;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.math.BigDecimal;

/**
 * Created by Andres Canevaro on 13/08/15.
 */
public abstract class AbstractRatingSchemeStep<T> extends AbstractMediationStep<MediationStepResult> {

    public Integer mediation;
    public Integer company;
    private JdbcTemplate jdbcTemplate = null;

    @Override
    public boolean executeStep(MediationStepContext context) {
        mediation = context.getRecord().getMediationCfgId();
        MediationStepResult result = (MediationStepResult) context.getResult();
        company = getUserCompany(result.getUserId());
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    public BigDecimal resolveQuantityByRatingScheme(Integer quantity) {
        Integer ratingSchemeId = RatingSchemeBL.getRatingSchemeIdForMediation(mediation, company);
        return RatingSchemeBL.getQuantity(ratingSchemeId, quantity);
    }
    
    protected Integer getUserCompany(Integer userId) {
        Integer company = null;
        String sql = "select entity_id " +
                "from base_user " +
                "where id = ?";
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql,userId);
        if (rs.next()) {
            company = rs.getInt("entity_id");
        }
        return company;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}