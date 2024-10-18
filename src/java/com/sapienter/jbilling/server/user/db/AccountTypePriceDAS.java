package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;

import java.util.Date;
import java.util.List;

/**
 * @author Panche Isajeski
 * @since 05/14/2013
 */
public class AccountTypePriceDAS extends AbstractDAS<AccountTypePriceDTO> {

    private static final FormatLogger LOG = new FormatLogger(AccountTypePriceDAS.class);

    private static final String PLAN_ITEM_FIND_HQL =
            "select price "
                    + " from AccountTypePriceDTO price "
                    + " where price.id.accountType.id = :account_type_id"
                    + " and price.id.planItem.id = :plan_item_id"
                    + " order by price.id.planItem.precedence, price.createDatetime desc";

    private static final String ACCOUNT_TYPE_ITEM_PRICE_FIND_HQL =
            "select price.id.planItem "
                    + " from AccountTypePriceDTO price "
                    + " where price.id.planItem.item.id = :item_id "
                    + " and price.id.accountType.id = :account_type_id "
                    + " order by price.id.planItem.precedence, price.createDatetime desc";

    private static final String ACCOUNT_TYPE_ITEM_FIND_PRICES_HQL =
            "select price.id.planItem "
                    + " from AccountTypePriceDTO price "
                    + " where price.id.planItem.item.id = :item_id "
                    + " and price.id.accountType.id = :account_type_id ";

    private static final String PRICE_ATTRIBUTE_ORDER_HQL =
            " order by price.id.planItem.precedence, price.createDatetime desc";

    private static final String ACCOUNT_TYPE_ALL_PRICE_LIST_HQL =
            "select price.id.planItem"
                    + " from AccountTypePriceDTO price "
                    + " where price.id.accountType.id = :account_type_id"
                    + " order by price.id.planItem.precedence, price.createDatetime desc";

    private static final String PLAN_ITEM_DELETE_HQL =
            "delete AccountTypePriceDTO "
                    + " where id.planItem.id = :plan_item_id "
                    + " and id.accountType.id = :account_type_id";

    //expiry date inclusive
    private static final String PRICING_DATE_CONDITION =
            " and ( price.priceExpiryDate is null or price.priceExpiryDate >= :expiry_date ) ";


    public AccountTypePriceDTO find(Integer accountTypeId, Integer planItemId) {
        Query query = getSession().createQuery(PLAN_ITEM_FIND_HQL);
        query.setParameter("account_type_id", accountTypeId);
        query.setParameter("plan_item_id", planItemId);

        return (AccountTypePriceDTO) query.uniqueResult();
    }

    public PlanItemDTO findPriceByItem(Integer accountTypeId, Integer itemId) {
        Query query = getSession().createQuery(ACCOUNT_TYPE_ITEM_PRICE_FIND_HQL);

        query.setParameter("account_type_id", accountTypeId);
        query.setParameter("item_id", itemId);
        query.setMaxResults(1);
        return (PlanItemDTO) query.uniqueResult();
    }

    public List<PlanItemDTO> findAllAccountTypePrices(Integer accountTypeId) {
        Query query = getSession().createQuery(ACCOUNT_TYPE_ALL_PRICE_LIST_HQL);
        query.setParameter("account_type_id", accountTypeId);

        return query.list();
    }

    public List<PlanItemDTO> findAllAccountTypePricesByItem(Integer accountTypeId, Integer itemId) {
        Query query = getSession().createQuery(ACCOUNT_TYPE_ITEM_PRICE_FIND_HQL);

        query.setParameter("account_type_id", accountTypeId);
        query.setParameter("item_id", itemId);
        return query.list();
    }

    public List<PlanItemDTO> findAllAccountTypePrices(Integer accountTypeId, Integer itemId, Date pricingDate) {

        StringBuffer hql = new StringBuffer();
        hql.append(ACCOUNT_TYPE_ITEM_FIND_PRICES_HQL);

        if ( null != pricingDate ) {
            hql.append(PRICING_DATE_CONDITION);
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        LOG.debug("Constructed HQL query with attributes: %s", hql.toString());

        Query query = getSession().createQuery(hql.toString());

        query.setParameter("account_type_id", accountTypeId);
        query.setParameter("item_id", itemId);

        if ( null != pricingDate ) {
            query.setParameter("expiry_date", pricingDate, StandardBasicTypes.DATE);
        }

        return query.list();
    }

    /**
     * Deletes the account type price for the given plan item id (plan item price).
     *
     * @param accountTypeId     account type id
     * @param planItemId plan item price id
     * @return number of rows deleted
     */
    public int deletePrice(Integer accountTypeId, Integer planItemId) {
        Query query = getSession().createQuery(PLAN_ITEM_DELETE_HQL);
        query.setParameter("account_type_id", accountTypeId);
        query.setParameter("plan_item_id", planItemId);

        return query.executeUpdate();
    }
}
