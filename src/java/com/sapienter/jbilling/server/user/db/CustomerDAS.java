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

package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.*;

import java.util.Date;
import java.util.List;

public class CustomerDAS extends AbstractDAS<CustomerDTO> {
    
    private static final String UPDATE_DISTRIBUTEL_CUSTOMERS_INVOICE_TEMPLATE_QUERY = "UPDATE customer cu  " +
                                                                                         "SET invoice_template_id= :invoiceTemplateId " +
                                                                                        "FROM base_user bu " +
                                                                                       "WHERE bu.entity_id=:entityId " +
                                                                                         "AND bu.language_id=:languageId " +
                                                                                         "AND cu.user_id = bu.id";
    public CustomerDTO create() {
        CustomerDTO newCustomer = new CustomerDTO();
        newCustomer.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDAS()
                .find(Constants.D_METHOD_EMAIL));
        newCustomer.setExcludeAging(0);
        return save(newCustomer);
    }

    public Integer getCustomerId(Integer userId){
        Criteria criteria = getSession().createCriteria(CustomerDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.setProjection(Projections.id());
        return (Integer) criteria.uniqueResult();
    }

    public Integer getCustomerAccountTypeId(Integer customerId) {
    	final String findCustomerAccountTypeId = "select account_type_id from customer where id = :customerId";
    	SQLQuery query = getSession().createSQLQuery(findCustomerAccountTypeId);
    	query.setParameter("customerId", customerId);
    	return (Integer) query.uniqueResult();
    }
    public List<Integer> getCustomerAccountInfoTypeIds(Integer customerId){
        DetachedCriteria atCriteria = DetachedCriteria.forClass(CustomerDTO.class);
        atCriteria.add(Restrictions.idEq(customerId));
        atCriteria.setProjection(Projections.property("accountType.id"));
        atCriteria.addOrder(Order.asc("id"));

        Criteria criteria = getSession().createCriteria(AccountInformationTypeDTO.class);
        criteria.setProjection(Projections.id());
        criteria.add(Subqueries.propertyEq("accountType.id", atCriteria));

        return criteria.list();
    }
    
    public Long countAllByInvoiceTemplate(Integer templateId){
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.setProjection(Projections.rowCount());
        crit.add(Restrictions.eq("invoiceTemplate.id", templateId));
        return (Long)crit.uniqueResult();
    }

    public String getPrimaryAccountNumberByUserAndEntityId(Integer userId, Integer entityId){
    	String query = "select distinct(mfv.string_value) from customer c, base_user bu, customer_meta_field_map cmfm, meta_field_value mfv, meta_field_name mfn where c.user_id = bu.id and cmfm.customer_id = c.id and cmfm.meta_field_value_id= mfv.id and mfn.name = 'primaryAccountNumber' and mfv.meta_field_name_id = mfn.id and bu.id =:userId and bu.entity_id=:entityId ";
    	SQLQuery sqlQuery= getSession().createSQLQuery(query);
    	sqlQuery.setParameter("userId", userId);
    	sqlQuery.setParameter("entityId", entityId);
    	return (String) sqlQuery.uniqueResult();
    }

    public ScrollableResults findAllByCompanyId(Integer companyId){
        Criteria criteria = getSession().createCriteria(CustomerDTO.class, "customer");
        criteria.createAlias("customer.baseUser", "user");
        criteria.add(Restrictions.eq("user.company.id", companyId));
        criteria.add(Restrictions.eq("user.deleted", 0));
        return criteria.scroll(ScrollMode.FORWARD_ONLY);
    }

    public Integer getCustomerIdByPrimaryAsset(String assetIdentifier){
        String query = "select id from customer where id = (select customer_id from customer_meta_field_map cmf, meta_field_value mfv, meta_field_name mfn where cmf.meta_field_value_id = mfv.id and mfv.meta_field_name_id = mfn.id and mfn.name = :customerAccountNumber and mfv.string_value = :assetIdentifier)";
        SQLQuery sqlQuery= getSession().createSQLQuery(query);
        sqlQuery.setParameter("customerAccountNumber", FileConstants.UTILITY_CUST_ACCT_NR);
        sqlQuery.setParameter("assetIdentifier", assetIdentifier);
        return (Integer) sqlQuery.uniqueResult();
    }

    public Integer getCustomerIdByAppDirectUUID(String uuid, Integer entityId){
        String query = "select id from customer where id = (select customer_id from customer_meta_field_map cmf, meta_field_value mfv, meta_field_name mfn where cmf.meta_field_value_id = mfv.id and mfv.meta_field_name_id = mfn.id and mfn.name = :metaFieldName and mfv.string_value = :metaFieldValue and mfn.entity_id = :entityId)";
        SQLQuery sqlQuery= getSession().createSQLQuery(query);
        sqlQuery.setParameter("metaFieldName", Constants.SSO_IDP_APPDIRECT_UUID_CUSTOMER);
        sqlQuery.setParameter("metaFieldValue", uuid);
        sqlQuery.setParameter("entityId", entityId);
        return (Integer) sqlQuery.uniqueResult();
    }

    public Integer updateDistributelCustomersInvoiceTemplate(Integer entityId, Integer invoiceTemplateId, Integer languageId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(UPDATE_DISTRIBUTEL_CUSTOMERS_INVOICE_TEMPLATE_QUERY);
        sqlQuery.setParameter("invoiceTemplateId", invoiceTemplateId);
        sqlQuery.setParameter("entityId", entityId);
        sqlQuery.setParameter("languageId", languageId);
        return (Integer) sqlQuery.executeUpdate();
    }

    /**
     * Gets a list of customers whose CAIT is filtered by effective date and group ids
     * @param effectiveDate effective date
     * @param groupIds group ids
     * @return Customer List
     */
    public List<CustomerDTO> getCustomerByCAITEffectiveDateAndGroupIds(Date effectiveDate, List<Integer> groupIds) {
        Criteria criteria = getSession().createCriteria(CustomerDTO.class, "customer");
        criteria.createAlias("customerAccountInfoTypeMetaFields", "customerAccountInfoType")
                .createAlias("customerAccountInfoType.accountInfoType", "accountInfoType")
                .add(Restrictions.eq("customerAccountInfoType.effectiveDate", effectiveDate))
                .add(Restrictions.in("accountInfoType.id", groupIds))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        
        return criteria.list();
    }
}
