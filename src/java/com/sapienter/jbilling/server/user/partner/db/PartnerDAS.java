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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

import java.util.List;

public class PartnerDAS extends AbstractDAS<PartnerDTO> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PartnerDAS.class));

    private static final String FIND_CHILD_LIST_SQL =
            "SELECT u.id " +
                    "FROM UserDTO u " +
                    "WHERE u.deleted=0 and u.partner.parent.id = :parentID";
    private static final String FIND_PARTNERS_BY_COMPANY =
            "SELECT u.partner " +
                    "FROM UserDTO u " +
                    "WHERE u.deleted=0 " +
                    "and u.company.id = :companyID";

    private static final String FIND_USER_NAME_BY_PARTNER_ID =
            "SELECT u.userName " +
            "FROM PartnerDTO p " +
            "INNER JOIN p.baseUser u where u.deleted=0 and p.id=:partnerID";

    public List<Integer> findChildList(Integer parentID) {
        Query query = getSession().createQuery(FIND_CHILD_LIST_SQL);
        query.setParameter("parentID", parentID);
        return query.list();
    }

    public List<PartnerDTO> findPartnersByCompany(Integer entityID) {
        Query query = getSession().createQuery(FIND_PARTNERS_BY_COMPANY)
                .setParameter("companyID", entityID);
        return query.list();
    }

    public PartnerDTO findForBrokerId(String brokerId, Integer entityId) {
        List<PartnerDTO> partners = (List<PartnerDTO>)getHibernateTemplate().findByNamedQueryAndNamedParam("PartnerDTO.findForBroker",
                new String[] {"brokerId", "entityId"},
                new Object[] {brokerId, entityId});
        if(partners.isEmpty()) {
            return null;
        }
        return partners.get(0);
    }

    /**
     * This method used for find the partner name.
     *
     * @param partnerId used for find the partner name.
     * @return String
     */
    public String findPartnerNameById(Integer partnerId) {
        if (partnerId == null) return null;
        Query query = getSession().createQuery(FIND_USER_NAME_BY_PARTNER_ID);
        query.setParameter("partnerID", partnerId);
        List<String> list = query.list();
        if (list != null && list.size() > 0) return list.get(0);
        return null;
    }

    public Integer getPartnerIdByAppDirectUUID(String uuid, Integer entityId){
        String query = "select id from partner where deleted = 0 and id = (select partner_id from partner_meta_field_map pmf, meta_field_value mfv, meta_field_name mfn where pmf.meta_field_value_id = mfv.id and mfv.meta_field_name_id = mfn.id and mfn.name = :metaFieldName and mfv.string_value = :metaFieldValue and mfn.entity_id = :entityId)";
        SQLQuery sqlQuery= getSession().createSQLQuery(query);
        sqlQuery.setParameter("metaFieldName", Constants.SSO_IDP_APPDIRECT_UUID_AGENT);
        sqlQuery.setParameter("metaFieldValue", uuid);
        sqlQuery.setParameter("entityId", entityId);
        return (Integer) sqlQuery.uniqueResult();
    }
}
