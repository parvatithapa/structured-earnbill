package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.io.Serializable;
import java.util.List;

/**
 * Class extending AbstractDAS which include generics methods to save,find and perform other queries
 */
public class AccountTypeDAS extends AbstractDAS<AccountTypeDTO> {

    public AccountTypeDTO find(Serializable id, Serializable companyId) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.add(Restrictions.eq("id", id));
        crit.add(Restrictions.eq("company.id", companyId));
        return (AccountTypeDTO) crit.uniqueResult();
    }

    public List<AccountTypeDTO> findAll(Serializable companyId) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.add(Restrictions.eq("company.id", companyId));
        return crit.list();
    }

    public Long countAllByInvoiceTemplate(Integer templateId){
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.setProjection(Projections.rowCount());
        crit.add(Restrictions.eq("invoiceTemplate.id", templateId));
        return (Long)crit.uniqueResult();
    }

    public PaymentMethodTypeDTO findPaymentMethodByMethodName(String methodName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("methodName", methodName).ignoreCase())
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (PaymentMethodTypeDTO) criteria.uniqueResult();
    }

    public AccountTypeDTO findAccountTypeByName(Integer companyId, String name) {
        JbillingTableDAS jbTableDAS = Context.getBean(Context.Name.JBILLING_TABLE_DAS);

        final String QUERY = "SELECT accountType " +
                "FROM InternationalDescriptionDTO description, AccountTypeDTO accountType " +
                "WHERE accountType.company.id = :companyId " +
                "AND description.id.tableId = :tableId " +
                "AND description.id.foreignId = accountType.id " +
                "AND description.id.languageId = 1 " +
                "AND description.content = :name";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("companyId", companyId);
        query.setParameter("tableId", jbTableDAS.findByName("account_type").getId());
        query.setParameter("name", name);

        return (AccountTypeDTO) query.uniqueResult();
    }

    public Long countCustomerByAccountType(Integer accountTypeId){
        Criteria criteria = getSession().createCriteria(CustomerDTO.class)
                            .createAlias("baseUser", "user")
                            .add(Restrictions.eq("user.deleted", 0))
                            .createAlias("accountType", "accountType")
                            .add(Restrictions.eq("accountType.id", accountTypeId))
                            .setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult();
    }
}