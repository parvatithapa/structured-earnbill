package com.sapienter.jbilling.server.filter;

/**
 * FilterConstraint
 
 * @author Brian Cowdery
 * @since  01-12-2010
 */
public enum FilterConstraint {
    EQ, LIKE, DATE_BETWEEN, NUMBER_BETWEEN, SIZE_BETWEEN, IS_EMPTY, IS_NOT_EMPTY, IS_NULL, IS_NOT_NULL, STATUS, GREATER_THAN, IN, LESS_THAN, NOT_LIKE, META_FIELD, OR, ORDER_DATE
}