package com.sapienter.jbilling.server.filter;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.value.*;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by marcolin on 17/08/16.
 * Base DAS used to filter Entities in jBilling, provide a standard approach to use Criteria queries to retrieve
 * paged results. Should be implemented for jBilling's Entities to provide pagination API for that Entity, default queries
 * are created for Filter types, to implement custom behaviours (custom aliases, custom restrictions by query) some methods
 * can be overridden
 */
public abstract class AbstractFilterDAS<T> extends AbstractDAS<T> {

    protected List<Filter> filters;

    protected void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    protected Filter getFilter(String fieldString) {
        return filters.stream().filter(f -> f.getFieldString() != null &&
                f.getFieldString().equals(fieldString)).findFirst().get();
    }

    private Criteria initCriteria(int page, int size, String sort, String order, List<Filter> filters) {
        setFilters(filters);
        return createCriteriaWithResult(page, size, sort, order, filters);
    }

    public PagedResultList<T> findByFilters(int page, int size, String sort, String order, List<Filter> filters) {
        return new PagedResultList<>(initCriteria(page, size, sort, order, filters).list(),
                (Long) createCriteriaForTotalCount(filters).uniqueResult());
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findIdByFilters(int page, int size, String sort, String order, Integer entityId, List<Filter> filters) {
        filters = addCompanyFilter(filters, entityId);
        Criteria criteria = initCriteria(page, size, sort, order, filters);
        criteria.setProjection(Projections.id());
        return criteria.list();
    }

    private List<Filter> addCompanyFilter(List<Filter> filters, Integer entityId) {
        filters = new ArrayList<>(filters);
        filters.add(Filter.integer("company.id", FilterConstraint.EQ, entityId));
        return filters;
    }

    private Criteria createCriteriaWithResult(int page, int size, String sort, String order, List<Filter> filters) {
        Criteria criteria = createCriteriaUsingFilters(sort, order, filters);
        criteria.setFirstResult(page);
        criteria.setMaxResults(size);
        return criteria;
    }

    private Criteria createCriteriaForTotalCount(List<Filter> filters) {
        Criteria criteria = createCriteriaUsingFilters(null, null, filters);
        criteria.setProjection(Projections.projectionList()
                .add(Projections.rowCount()));
        return criteria;
    }

    private Criteria createCriteriaUsingFilters(String sort, String order, List<Filter> filters) {
        Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(getClassToFilter());
        criteria = addAliasesTo(criteria);
        List<Criterion> criterions = criterionsFromFilters(filters);
        criterions.addAll(getEntityDefaultCriterions());
        criterions = criterions.stream().filter(Objects::nonNull).collect(Collectors.toList());

        criteria.add(Restrictions.and(criterions.toArray(new Criterion[0])));
        if (sort != null) {
            criteria.addOrder(order.equals("asc") ? Order.asc(sort) : Order.desc(sort));
        }
        return criteria;
    }

    private List<Criterion> criterionsFromFilters(List<Filter> filters) {
        return filters.stream().map(this::criterionForFilter).collect(Collectors.toList());
    }

    /**
     * Can be overridden to add aliases to the query that will be done, returning always the super.addAliasesTo
     * in the overridden method will already handle the aliases for metafields if the Entity has them
     * @param criteria
     * @return
     */
    protected Criteria addAliasesTo(Criteria criteria) {
        if (thereIsFilter("contact.fields")) {
            criteria = criteria.createAlias("metaFields", "fieldValue");
            criteria = criteria.createAlias("fieldValue.field", "type");
            criteria = criteria.setFetchMode("type", FetchMode.JOIN);
        }
        return  criteria;
    }

    protected boolean thereIsFilter(String fieldString) {
        return filters.stream().anyMatch(f -> isFilterForField(f, fieldString));
    }

    private Criterion criterionForFilter(Filter filter) {
        if (filter.getConstraint() != null) {
            switch (filter.getConstraint()) {
                case EQ: return eqRestriction(filter);
                case STATUS: return eqRestriction(filter);
                case LIKE: return likeRestriction(filter);
                case IN: return inRestriction(filter);
                case DATE_BETWEEN: return dateBetweenRestriction(filter);
                case META_FIELD: return metaFieldRestriction(filter);
                case ORDER_DATE: return orderDateRestriction(filter);
                case OR: return Restrictions.or(filter.getFilters().stream()
                        .map(this::criterionForFilter).collect(Collectors.toList()).toArray(new Criterion[0]));
            }

        }
        return null;
    }

    private Criterion metaFieldRestriction(Filter filter) {
        Integer metaFieldTypeId = filter.getMetaFieldTypeId();
        MetaField metaFieldByTypeId = getMetaFieldByTypeId(metaFieldTypeId);
        if (metaFieldByTypeId != null) {
            return Restrictions.and(
                    Restrictions.eq("type.id", metaFieldTypeId),
                    Property.forName("fieldValue.id").in(metaFieldFilterCriteria(metaFieldByTypeId, (String) filter.getValue()))
            );
        }
        return null;
    }

    private Criterion orderDateRestriction(Filter filter) {
        String value = filter.getValue().toString();
        if (value.equalsIgnoreCase("Current")) {
            return Restrictions.le("activeSince", Calendar.getInstance().getTime());
        }else if(value.equalsIgnoreCase("Future")){
            return Restrictions.gt("activeSince", Calendar.getInstance().getTime());
        }
        return null;
    }

    /**
     * Should be ovverridden and must return the MetaField for the Entity and the typeId in input
     * @param typeId
     * @return
     */
    protected abstract MetaField getMetaFieldByTypeId(Integer typeId);

    protected boolean isFilterForField(Filter filter, String fieldString) {
        return filter.getFieldString() != null &&
                filter.getFieldString().equals(fieldString);
    }

    protected Integer getCompanyIdForFiltering() {
        return (Integer) getFilter("company.id").getValue();
    }

    /**
     * Overriding this method is possible to create custom Restriction for an entity and BETWEEN_DATE filters
     * @param filter
     * @return
     */
    protected Criterion dateBetweenRestriction(Filter filter) {
        if (filter.getStartDate() == null) {
            return Restrictions.lt(filter.getFieldString(), filter.getEndDate());
        } else if (filter.getEndDate() == null) {
            return Restrictions.ge(filter.getFieldString(), filter.getStartDate());
        } else {
            return Restrictions.between(filter.getFieldString(), filter.getStartDate(), filter.getEndDate());
        }
    }

    /**
     * Overriding this method is possible to create custom Restriction for an entity and IN filters
     * @param filter
     * @return
     */
    private Criterion inRestriction(Filter filter) {
        return Restrictions.in(filter.getFieldString(), (List) filter.getValue());
    }

    /**
     * Overriding this method is possible to create custom Restriction for an entity and LIKE filters
     * @param filter
     * @return
     */
    protected Criterion likeRestriction(Filter filter) {
        return Restrictions.like(filter.getFieldString(), filter.getValue().toString(), MatchMode.ANYWHERE);
    }

    /**
     * Overriding this method is possible to create custom Restriction for an entity and EQ filters
     * @param filter
     * @return
     */
    protected Criterion eqRestriction(Filter filter) {
        return Restrictions.eq(filter.getFieldString(), filter.getValue());
    }

    /**
     * Return the entity class filtered by the concrete class
     * @return
     */
    protected abstract Class getClassToFilter();

    public List<Criterion> getEntityDefaultCriterions() {
        return Arrays.asList();
    }

    /**
     *This method is adding to the criteria according to the datatype passed
     *
     * @param type type the type of Metafield
     * @param metaFieldValue metaFieldValue value in the filter field from screen
     * @return updated the criteria
     */
    protected static DetachedCriteria metaFieldFilterCriteria(MetaField type, String metaFieldValue) {
        DetachedCriteria subCriteria = null;
        switch (type.getDataType()) {
            case DATE:
                subCriteria = DetachedCriteria.forClass(DateMetaFieldValue.class, "dateValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.eq("dateValue.value", convertDate(metaFieldValue)));

                break;
            case JSON_OBJECT:
                subCriteria = DetachedCriteria.forClass(JsonMetaFieldValue.class, "jsonValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.like("jsonValue.value", metaFieldValue + "%").ignoreCase());

                break;
            case ENUMERATION:
            case STRING:
            case TEXT_AREA:
            case SCRIPT:
            case STATIC_TEXT:
            case LIST:
                subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.like("stringValue.value", metaFieldValue + "%").ignoreCase());

                break;
            case DECIMAL:
                subCriteria = DetachedCriteria.forClass(DecimalMetaFieldValue.class, "decimalValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.eq("decimalValue.value", new BigDecimal(metaFieldValue)));
                break;
            case INTEGER:
                subCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.eq("integerValue.value", Integer.parseInt(metaFieldValue)));

                break;
            case BOOLEAN:
                subCriteria = DetachedCriteria.forClass(BooleanMetaFieldValue.class, "booleanValue")
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.eq("booleanValue.value", Boolean.parseBoolean(metaFieldValue)));
                break;
        }
        return subCriteria;
    }

    private static Date convertDate(String input) {
        try {
            final String inputFormat = "MM/dd/yyyy";
            final String outputFormat = "yyyy-MM-dd HH:mm:ss";
            String output = new SimpleDateFormat(outputFormat).format(new SimpleDateFormat(inputFormat).parse(input));
            return new SimpleDateFormat(outputFormat).parse(output);
        } catch (ParseException e) {
            throw new SessionInternalError(e);
        }
    }
}
