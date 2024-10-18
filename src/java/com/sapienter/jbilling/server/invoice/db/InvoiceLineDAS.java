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

package com.sapienter.jbilling.server.invoice.db;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author abimael
 * 
 */
public class InvoiceLineDAS extends AbstractDAS<InvoiceLineDTO> {

    public InvoiceLineDTO create(String description, BigDecimal amount,
            BigDecimal quantity, BigDecimal price, Integer typeId, ItemDTO itemId,
            Integer sourceUserId, Integer isPercentage, String callIdentifier, 
            Long callCounter, String assetIdentifier, Integer usagePlanId,
            BigDecimal grossAmount, BigDecimal taxRate, BigDecimal taxAmount) {

        InvoiceLineDTO newEntity = new InvoiceLineDTO();
        newEntity.setDescription(description);
        newEntity.setAmount(amount);
        newEntity.setQuantity(quantity);
        newEntity.setPrice(price);
        newEntity.setInvoiceLineType(new InvoiceLineTypeDAS().find(typeId));
        newEntity.setItem(itemId);
        newEntity.setSourceUserId(sourceUserId);
        newEntity.setIsPercentage(isPercentage);
        newEntity.setDeleted(0);
        newEntity.setCallIdentifier(callIdentifier);
        newEntity.setCallCounter(callCounter);
        newEntity.setAssetIdentifier(assetIdentifier);
        newEntity.setUsagePlanId(usagePlanId);
        newEntity.setTaxRate(taxRate);
        newEntity.setTaxAmount(taxAmount);
        newEntity.setGrossAmount(grossAmount);
        return save(newEntity);
    }

    public InvoiceLineDTO create(String description, BigDecimal amount,
            BigDecimal quantity, BigDecimal price, Integer typeId, ItemDTO itemId,
            Integer sourceUserId, Integer isPercentage, String callIdentifier,
            Long callCounter, String assetIdentifier, Integer usagePlanId) {
        return create(description, amount, quantity, price, typeId, itemId, sourceUserId, isPercentage,
                callIdentifier, callCounter, assetIdentifier, usagePlanId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public List<Integer> findIdsByEntity(Integer entityId) {
        if (entityId == null) return null;
        Criteria criteria = getSession().createCriteria(InvoiceLineDTO.class)
                .setLockMode(LockMode.NONE)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("invoiceLineType", "lineType")
                .createAlias("invoice", "invoice")
                .createAlias("order", "order", JoinType.NONE)
                .createAlias("invoice.baseUser", "u")
                .createAlias("u.customer", "customer")
                .createAlias("u.company", "company")
                .add(Restrictions.or(Restrictions.eq("lineType.id", Constants.INVOICE_LINE_TYPE_TAX),
                        Restrictions.eq("lineType.id", Constants.INVOICE_LINE_TYPE_ITEM_RECURRING),
                        Restrictions.isNotNull("order"),
                        Restrictions.eq("lineType.id", Constants.INVOICE_LINE_TYPE_ITEM_ONETIME)))
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("u.deleted", 0))
                .add(Restrictions.eq("invoice.deleted", 0))
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .setComment("findIdsByEntity " + entityId);
        ScrollableResults scrollableResults = criteria.scroll();
        List<Integer> invoiceLineIds = new ArrayList<Integer>();
        if (scrollableResults != null) {
            try {
                while (scrollableResults.next()) {
                    invoiceLineIds.add(scrollableResults.getInteger(0));
                }
            } finally {
                scrollableResults.close();
            }
        }
        Collections.sort(invoiceLineIds);
        return invoiceLineIds;
    }
    
    /**
     * Returns list of Invoice Lines by Invoice Line Type 
     * Ex. Recurring, One Time, Penalty and Adjustment.
     *
     * @param invoiceId invoice Id
     * @return List<InvoiceLineDTO>
     */
    public List<InvoiceLineDTO> getInvoiceLinesByType(Integer invoiceId, Integer typeId) {
        Criteria criteria = getSession().createCriteria(InvoiceLineDTO.class);
        criteria.createAlias("invoiceLineType", "type");
        criteria.add(Restrictions.eq("type.id", typeId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.createAlias("invoice", "i");
        criteria.add(Restrictions.eq("i.id", invoiceId));
        criteria.addOrder(Order.desc("amount"));
        return criteria.list();
    }
}
