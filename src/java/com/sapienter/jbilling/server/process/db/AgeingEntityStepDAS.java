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
package com.sapienter.jbilling.server.process.db;

import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;

import org.hibernate.Criteria;

import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class AgeingEntityStepDAS extends AbstractDAS<AgeingEntityStepDTO> {

    public void create(Integer entityId, String description,
                       Integer languageId, int days,
                       int sendNotification, int retryPayment, int suspend, CollectionType collectionType, int stopActivationOnPayment) {

        UserStatusDTO userStatus = new UserStatusDTO();
        userStatus.setCanLogin(1);
        userStatus = new UserStatusDAS().save(userStatus);
        userStatus.setDescription(description, languageId);

        AgeingEntityStepDTO ageing = new AgeingEntityStepDTO();
        ageing.setCompany(new CompanyDAS().find(entityId));
        ageing.setUserStatus(userStatus);

        ageing.setDays(days);
        ageing.setSendNotification(sendNotification);
        ageing.setRetryPayment(retryPayment);
        ageing.setSuspend(suspend);
        ageing.setDescription(description, languageId);
        ageing.setCollectionType(collectionType);
        ageing.setStopActivationOnPayment(stopActivationOnPayment);

        save(ageing);
    }

    @SuppressWarnings("unchecked")
    public List<AgeingEntityStepDTO> findAgeingStepsForEntity(Integer entityId, CollectionType collectionType) {
        Criteria criteria = getSession().createCriteria(AgeingEntityStepDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("collectionType", collectionType))
                .addOrder(Order.asc("days"));
        return criteria.list();
    }

    public List<UserStatusDTO> findUserStatusDTOForEntity(Integer entityId, CollectionType collectionType) {
        Criteria criteria = getSession().createCriteria(AgeingEntityStepDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("collectionType", collectionType))
                .addOrder(Order.asc("days"))
                .setProjection(Projections.property("userStatus"));
        return criteria.list();
    }

    public boolean isAgeingStepInUse(Integer ageingStepId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .createAlias("userStatus", "status", CriteriaSpecification.INNER_JOIN)
                .createAlias("status.ageingEntityStep", "ageingStep", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.eq("ageingStep.id", ageingStepId))
                .setProjection(Projections.count("id"));
        return (Long) criteria.list().get(0) > 0;
    }

}
