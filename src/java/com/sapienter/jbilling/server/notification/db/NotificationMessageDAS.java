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
package com.sapienter.jbilling.server.notification.db;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapienter.jbilling.common.CollectionUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

/**
 * 
 * @author abimael
 * 
 */
public class NotificationMessageDAS extends AbstractDAS<NotificationMessageDTO> {


    public NotificationMessageDTO findIt(Integer typeId, Integer entityId,
            Integer languageId) {

        /*
         * query="SELECT OBJECT(a) FROM notification_message a WHERE a.type.id =
         * ?1 AND a.entityId = ?2 AND a.languageId = ?3"
         * result-type-mapping="Local"
         */
        Criteria criteria = getSession().createCriteria(
                NotificationMessageDTO.class);
        criteria.createAlias("entity", "e").add(
                Restrictions.eq("e.id", entityId.intValue()));
        criteria.createAlias("notificationMessageType", "nmt").add(
                Restrictions.eq("nmt.id", typeId.intValue()));
        criteria.createAlias("language", "l").add(
                Restrictions.eq("l.id", languageId.intValue()));

        return (NotificationMessageDTO) criteria.uniqueResult();
    }

    public List<NotificationMessageDTO> findByCompanyId(Integer entityId) {

        Criteria criteria = getSession().createCriteria(
                NotificationMessageDTO.class);
        criteria.createAlias("entity", "e").add(
                Restrictions.eq("e.id", entityId.intValue()));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }

    public NotificationMessageDTO create(Integer typeId, Integer entityId,
            Integer languageId, Boolean useFlag) {

        // search company
        CompanyDTO company = new CompanyDAS().find(entityId);
        // search language
        LanguageDTO language = new LanguageDAS().find(languageId);

        NotificationMessageTypeDTO notif = new NotificationMessageTypeDAS().find(typeId);

        short flag = useFlag ? new Short("1") : new Short("0");
        NotificationMessageDTO nm = new NotificationMessageDTO();
        nm.setEntity(company);
        nm.setNotificationMessageType(notif);
        nm.setLanguage(language);
        nm.setUseFlag(flag);
        return save(nm);

    }

    public NotificationMessageDTO create(Integer typeId, Integer entityId,
            Integer languageId, Boolean useFlag,
            Set<NotificationMessageSectionDTO> notifs) {

        // search company
        CompanyDTO company = new CompanyDAS().find(entityId);
        // search language
        LanguageDTO language = new LanguageDAS().find(languageId);

        NotificationMessageTypeDTO notif = new NotificationMessageTypeDAS().find(typeId);

        short flag = useFlag ? new Short("1") : new Short("0");
        NotificationMessageDTO nm = new NotificationMessageDTO();
        nm.setEntity(company);
        nm.setNotificationMessageType(notif);
        nm.setLanguage(language);
        nm.setUseFlag(flag);
        nm.setNotificationMessageSections(notifs);
        return save(nm);

    }

    public String getNotificationAlertMessage(String tableName, String planNo,String trigger) {
        String sql = "SELECT * FROM " + tableName + " WHERE jb_planno=:planno";
        Query query = getSession().createSQLQuery(sql);
        query.setParameter("planno", planNo);
        query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        Map<String, Object> aliasToValueMap = (Map<String, Object>) query.uniqueResult();

        if (MapUtils.isEmpty(aliasToValueMap))
            return null;
        String percentage = "";
        for (Map.Entry<String, Object> map : aliasToValueMap.entrySet()) {
            if (map.getValue().equals(trigger)) {
                percentage = map.getKey();
            }
        }
        percentage = percentage.replaceAll("[^0-9]", "");
        return (String) aliasToValueMap.get("sms_message" + percentage);
    }
}
