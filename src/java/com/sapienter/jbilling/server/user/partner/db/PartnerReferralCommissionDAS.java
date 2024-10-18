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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class PartnerReferralCommissionDAS extends AbstractDAS<PartnerReferralCommissionDTO> {

    public List<PartnerReferralCommissionDTO> findAllByReferralAndNotReferrer(Integer referralId, Integer referrerId){
        Criteria criteria = getSession().createCriteria(PartnerReferralCommissionDTO.class)
                .add(Restrictions.eq("referral.id", referralId))
                .add(Restrictions.ne("referrer.id", referrerId));

        return criteria.list();
    }

    public List<PartnerReferralCommissionDTO> findAllForCompany(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PartnerReferralCommissionDTO.class)
                .createAlias("referrer","_ref")
                .createAlias("_ref.baseUser","_user")
                .add(Restrictions.eq("_user.company.id", entityId));

        return criteria.list();
    }
}
