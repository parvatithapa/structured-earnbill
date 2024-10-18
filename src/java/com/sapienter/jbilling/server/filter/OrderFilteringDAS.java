package com.sapienter.jbilling.server.filter;

import com.sapienter.jbilling.server.item.TariffPlan;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by marcolin on 17/08/16.
 */
public class OrderFilteringDAS extends AbstractFilterDAS<OrderDTO> {

    OrderFilteringDAS() {}

    @Override
    protected Class getClassToFilter() {
        return OrderDTO.class;
    }

    @Override
    protected Criteria addAliasesTo(Criteria criteria) {
        criteria = criteria.createAlias("baseUserByUserId", "u", JoinType.LEFT_OUTER_JOIN);
        criteria = criteria.createAlias("u.company", "company", JoinType.LEFT_OUTER_JOIN);

        if (thereIsFilter("userCodes.userCode.identifier")) {
            criteria.createAlias("userCodeLinks", "userCodes");
            criteria.createAlias("userCodes.userCode", "userCode");
        }
        if (thereIsFilter("tariff")) {
            criteria.createAlias("lines", "lines");
            criteria.createAlias("lines.item", "item");
        }
        if (thereIsFilter("createdBy.id")) {
            criteria.createAlias("baseUserByCreatedBy", "createdBy");
        }
        if(thereIsFilter("orderProcesses.id")){
            criteria.createAlias("orderProcesses", "orderProcesses");
        }
        return super.addAliasesTo(criteria);
    }

    @Override
    protected MetaField getMetaFieldByTypeId(Integer typeId) {
        List<MetaField> availableFieldsList = MetaFieldBL.getAvailableFieldsList(getCompanyIdForFiltering(), Arrays.asList(EntityType.ORDER).toArray(new EntityType[0]));
        return availableFieldsList.stream().filter(mf -> mf.getId() == typeId).findFirst().get();
    }

    @Override
    protected Criterion eqRestriction(Filter filter) {
        if (isFilterForField(filter, "company.id")) { return null; }
        if (isFilterForField(filter, "orderStatus") || isFilterForField(filter, "orderPeriod")) {
            return Restrictions.eq(filter.getFieldString() + ".id", filter.getValue());
        }
        if (isFilterForField(filter, "changeStatus")) {
            return Restrictions.sqlRestriction(String.format(" exists (select oc.order_id FROM order_change as oc WHERE oc.user_assigned_status_id=%s AND oc.order_id={alias}.id)", filter.getValue()));
        }
        if (isFilterForField(filter, "userCodes.userCode.identifier")) {
            return Restrictions.eq("userCode.identifier", filter.getValue());
        }
        return super.eqRestriction(filter);
    }

    @Override
    protected Criterion likeRestriction(Filter filter) {
        if (isFilterForField(filter, "tariff")) {
            boolean tariff;
            tariff = filter.getValue() != null && filter.getValue().equals(TariffPlan.TARIFF.name());
            DetachedCriteria dc = DetachedCriteria.forClass(PlanDTO.class, "plan");
            dc.add(Restrictions.eq("plan.tariff", tariff));
            dc.createAlias("plan.item", "item");
            dc.setProjection(Projections.property("item.id"));
            return Property.forName("item.id").in(dc);
        }
        return super.likeRestriction(filter);
    }

    @Override
    protected Criterion dateBetweenRestriction(Filter filter) {
        if (isFilterForField(filter, "nextBillableDay")) {
            return Restrictions.or(
                    super.dateBetweenRestriction(filter),
                    Restrictions.and(Restrictions.isNull("nextBillableDay"),
                            super.dateBetweenRestriction(createBetweenFilterFromForField(filter, "activeSince"))),
                    Restrictions.and(Restrictions.isNull("nextBillableDay"), Restrictions.isNull("activeSince"),
                            super.dateBetweenRestriction(createBetweenFilterFromForField(filter, "createDate")))
            );
        }
        return super.dateBetweenRestriction(filter);
    }

    private Filter createBetweenFilterFromForField(Filter filter, String field) {
        return Filter.betweenDates(field, filter.getStartDate(), filter.getEndDate());
    }

    public List<Criterion> getEntityDefaultCriterions() {
        CompanyDAS companyDAS = new CompanyDAS();
        List<Integer> companiesInHierarchy = companyDAS.findAllCurrentAndChildEntities(getCompanyIdForFiltering());
        return Arrays.asList(
                Restrictions.eq("deleted", 0),
                Restrictions.in("u.company", companiesInHierarchy.stream()
                        .map(companyId -> companyDAS.find(companyId)).collect(Collectors.toList())));
    }

}
