package com.sapienter.jbilling.server.filter;

import com.sapienter.jbilling.server.filter.FilterConstraint;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;

/**
 * Created by marcolin on 27/10/15.
 */
public class Filter implements Serializable {
    private FilterType type;
    private Expression<?> field;
    private Enum<?> enumField;
    private String fieldString;
    private FilterConstraint constraint;

    private Boolean booleanValue;
    private String stringValue;
    private Integer metaFieldTypeId;
    private Integer integerValue;
    private List listValue;
    private BigDecimal decimalValue;
    private BigDecimal decimalHighValue;
    private UUID uuid;
    private Date startDate;
    private Date endDate;
    private String fieldKeyData;
    private Object objectValue;

    @PersistenceContext
    private EntityManager entityManager;
    private List<Filter> filters;

    public Filter() {}

    public Filter(Expression<?> field, FilterConstraint constraint, Boolean booleanValue, String stringValue, Integer integerValue, BigDecimal decimalValue, BigDecimal decimalHighValue, Date startDate, Date endDateValue, String fieldKeyData) {
        this.field = field;
        this.constraint = constraint;
        this.booleanValue = booleanValue;
        this.stringValue = stringValue;
        this.integerValue = integerValue;
        this.decimalValue = decimalValue;
        this.decimalHighValue = decimalHighValue;
        this.startDate = startDate;
        this.endDate = endDateValue;
        this.fieldKeyData = fieldKeyData;
    }

    public static Filter expression(Expression<?> field, FilterConstraint constraint) {
        Filter filter = new Filter();
        filter.field = field;
        filter.constraint = constraint;
        return filter;
    }

    public static Filter string(String fieldString, FilterConstraint constraint, String stringValue) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.stringValue = stringValue;
        return filter;
    }

    public static Filter integer(String fieldString, FilterConstraint constraint, Integer integerValue) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.integerValue = integerValue;
        return filter;
    }

    public static Filter uuid(String fieldString, FilterConstraint constraint, UUID uuid) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.uuid = uuid;
        return filter;
    }

    public static Filter object(String fieldString, FilterConstraint constraint, Object objectValue) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.objectValue = objectValue;
        return filter;
    }
    
    public Filter(String fieldString, FilterConstraint constraint, Enum<?> enumField) {
        this.fieldString = fieldString;
        this.constraint = constraint;
        this.enumField = enumField;
    }

    public static Filter list(String fieldString, FilterConstraint constraint, List listValue) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.listValue = listValue;
        return filter;
    }


    public static Filter conjDisj(String fieldString, FilterConstraint constraint, List<Filter> filters) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.filters = filters;
        return filter;
    }

    public static Filter metaField(String fieldString, FilterConstraint constraint, String stringValue, Integer metaFieldTypeId) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.stringValue = stringValue;
        filter.metaFieldTypeId = metaFieldTypeId;
        return filter;
    }

    public static Filter betweenDates(String fieldString, Date startDate, Date endDate) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = FilterConstraint.DATE_BETWEEN;
        filter.startDate = startDate;
        filter.endDate = endDate;
        return filter;
    }

    public static Filter enumFilter(String fieldString, FilterConstraint constraint, Enum<?> enumField) {
        Filter filter = new Filter();
        filter.fieldString = fieldString;
        filter.constraint = constraint;
        filter.enumField = enumField;
        return filter;
    }
    
    public Object getValue() {
        if (booleanValue != null)
            return booleanValue;

        if (stringValue != null)
            return stringValue;

        if (integerValue != null)
            return integerValue;

        if (decimalValue != null)
            return decimalValue;

        if (decimalHighValue != null)
            return decimalHighValue;

        if (startDate != null)
            return startDate;

        if (endDate != null)
            return endDate;

        if (uuid != null)
            return uuid;

        if (enumField != null)
			return enumField;

        if (listValue != null)
            return listValue;

        if (objectValue != null)
            return objectValue;
        
        return null;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public Integer getMetaFieldTypeId() {
        return metaFieldTypeId;
    }

    public void clear() {
        booleanValue = null;
        stringValue = null;
        integerValue = null;
        decimalValue = null;
        decimalHighValue = null;
        startDate = null;
        listValue = null;
        endDate = null;
        fieldKeyData = null;
        enumField = null;
        metaFieldTypeId = null;
    }

    public Predicate getRestrictions() {
        if (getValue() == null) {
            return null;
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        switch (constraint) {
            case EQ:
                return criteriaBuilder.equal(field, getValue());
        }

        return null;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "field='" + field + '\'' +
                ", type=" + type +
                ", constraint=" + constraint +
                ", value=" + getValue() +
                '}';
    }

    public String getFieldString() {
        return fieldString;
    }

    public FilterConstraint getConstraint() {
        return constraint;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public List getListValue() {
        return listValue;
    }
}
