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

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;
import java.util.List;

/**
 * Created by Andres Canevaro on 28/07/15.
 */
public class MediationRatingSchemeDAS extends AbstractDAS<MediationRatingSchemeDTO> {

    // QUERIES
    private static final String findAllByEntitySQL =
            "SELECT b " +
                    "  FROM MediationRatingSchemeDTO b " +
                    " WHERE b.entity.id = :entity " +
                    " ORDER BY id";

    private static final String countAllByEntitySQL = "SELECT COUNT(b) " +
                                                      "FROM MediationRatingSchemeDTO b " +
                                                      "WHERE b.entity.id = :entity";

    private static final String findRatingSchemeForMediation =
            "SELECT r.ratingIncrement " +
                    " FROM  RatingSchemeAssociation r" +
                    " WHERE r.mediation = :mediationCfgId" +
                    " AND r.entity = :entity";

    private static final String findRatingSchemeByName =
            "SELECT b " +
                    " FROM MediationRatingSchemeDTO b " +
                    " WHERE b.name = :name ";

    private static final String findAssociatedCompaniesForMediation =
            "SELECT rea.entity " +
                    " FROM  RatingSchemeAssociation rea" +
                    " WHERE rea.mediation = :mediation " +
                    " AND rea.ratingIncrement != :ratingIncrement" +
                    " ORDER BY entity";

    private static final String findGlobalRatingScheme =
            "SELECT b.id " +
                    " FROM MediationRatingSchemeDTO b " +
                    " WHERE b.entity.id = :rootCompany " +
                    " AND b.global = true ";

    private static final String findCompany =
            "SELECT c " +
                    " FROM CompanyDTO c " +
                    " WHERE c.id = :entityId ";

    public List<MediationRatingSchemeDTO> findAllByEntity(Integer entityId, Integer max, Integer offset) {
        Query query = getSession().createQuery(findAllByEntitySQL);
        query.setParameter("entity", entityId);
        if (max != null) {
            query.setMaxResults(max);
        }

        if (offset != null) {
            query.setFirstResult(offset);
        }

        return query.list();
    }

    public Long countAllByEntity(Integer entityId) {
        Query query = getSession().createQuery(countAllByEntitySQL);
        query.setParameter("entity", entityId);

        return (Long) query.uniqueResult();
    }

    public Integer getRatingSchemeIdForMediation(Integer mediationCfgId, Integer entity) {
        Query query = getSession().createQuery(findRatingSchemeForMediation);
        query.setParameter("mediationCfgId", mediationCfgId);
        query.setParameter("entity", entity);
        return (Integer) query.uniqueResult();
    }

    public boolean isValidName(String name, Integer recordId) {
        StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(findRatingSchemeByName);
		if (recordId != null) {
			queryBuilder.append("AND b.id != :currentRecordId");
		}

		Query query = getSession().createQuery(queryBuilder.toString());
		query.setParameter("name", name);

		if (recordId != null) {
			query.setParameter("currentRecordId", recordId);
		}

        return query.uniqueResult() == null ? true : false;
    }

    public List<Integer> findAssociatedCompaniesForMediation(Integer mediation, Integer excludeRatingScheme) {
        Query query = getSession().createQuery(findAssociatedCompaniesForMediation);
        query.setParameter("mediation", mediation);
        query.setParameter("ratingIncrement", excludeRatingScheme);
        return query.list();
    }

    public Integer findGlobalRatingScheme(Integer entity, Integer recordId) {
        StringBuilder queryBuilder = new StringBuilder();
        Integer rootEntityId = getRootEntityId(entity);

		queryBuilder.append(findGlobalRatingScheme);
		if (recordId != null) {
			queryBuilder.append("AND b.id != :currentRecordId");
		}

		Query query = getSession().createQuery(queryBuilder.toString());
		query.setParameter("rootCompany", rootEntityId);

		if (recordId != null) {
			query.setParameter("currentRecordId", recordId);
		}

        return (Integer) query.uniqueResult();
    }

    private Integer getRootEntityId(Integer entityId) {
        Query query = getSession().createQuery(findCompany);
        query.setParameter("entityId", entityId);
        CompanyDTO company = (CompanyDTO) query.uniqueResult();
        if(company == null) {
            return null;
        }
        if(company.getParent() == null) {
            return entityId;
        } else {
            return getRootEntityId(company.getParent().getId());
        }
    }

}