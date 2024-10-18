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

package jbilling

import com.sapienter.jbilling.server.adennet.AdennetHelperService
import com.sapienter.jbilling.server.util.Constants

import java.beans.Transient;

import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.MatchMode
import java.time.LocalDateTime;
/**
 * Filter

 * @author Brian Cowdery
 * @since  30-11-2010
 */
class Filter implements Serializable {

    static transients = [ "value", "name", "restrictions" ]

    static mapping = {
        id generator: 'org.hibernate.id.enhanced.TableGenerator',
           params: [
           table_name: 'jbilling_seqs',
           segment_column_name: 'name',
           value_column_name: 'next_id',
           segment_value: 'filter'
           ]
        filterSet column: 'filter_set_id'
    }

    static constraints = {
        booleanValue(nullable:true)
        stringValue(blank:true, nullable:true)
        integerValue(nullable:true)
        decimalValue(nullable:true)
        decimalHighValue(nullable:true)
        startDateValue(nullable:true)
        endDateValue(nullable:true)
        fieldKeyData(nullable:true)
    }

    static belongsTo = [filterSet: FilterSet]

    FilterType type
    FilterConstraint constraintType
    String field
    String template
    Boolean visible
    Object objectValue
    Boolean booleanValue
    String stringValue
    List listValue
    Integer integerValue
    BigDecimal decimalValue
    BigDecimal decimalHighValue
    Date startDateValue
    Date endDateValue
    String fieldKeyData
    FilterSet filterSet
    List filters

    def Filter() {
    }

    def Filter(Filter filter) {
        this.type = filter.type
        this.constraintType = filter.constraintType
        this.field = filter.field
        this.template = filter.template
        this.visible = filter.visible
        this.booleanValue = filter.booleanValue
        this.stringValue = filter.stringValue
        this.integerValue = filter.integerValue
        this.startDateValue = filter.startDateValue
        this.endDateValue = filter.endDateValue
        this.fieldKeyData = filter.fieldKeyData
        this.filterSet = filter.filterSet
        this.filters = filter.filters
        this.objectValue = filter.objectValue
    }

    void setVisible(Boolean visible) {
        this.visible = visible
    }

    Boolean isVisible() {
        return getValue() != null ? false : visible;
    }

    @Transient
    Object getValue() {
        if (booleanValue != null)
            return booleanValue

        if (stringValue != null)
            return stringValue

        if (integerValue != null)
            return integerValue

        if (decimalValue != null)
            return decimalValue

        if (decimalHighValue != null)
            return decimalHighValue

        if (startDateValue != null)
            return startDateValue

        if (endDateValue != null)
            return endDateValue

        if (listValue != null)
            return listValue

        if (objectValue != null)
            return objectValue

        return null
    }

    @Transient
    public String getName() {
        // Use fieldKeyData in name so that can have unique name for two filters for AIT or meta field filter
        def fieldKeyDataAsName = fieldKeyData?"_${fieldKeyData}":""
        return "${type}-${constraintType}_${field.replaceAll('\\.','_').capitalize()}${fieldKeyDataAsName}"
    }

    def void clear() {
        booleanValue = null
        stringValue = null
        integerValue = null
        decimalValue = null
        decimalHighValue = null
        startDateValue = null
        endDateValue = null
        filterSet = null
        listValue = null
    }

    @Override
    def boolean equals(o) {
        if (this.is(o)) return true;
        if (getClass() != o.class) return false;

        Filter filter = (Filter) o;

        if (constraintType != filter.constraintType) return false;
        if (field != filter.field) return false;
        if (template != filter.template) return false;
        if (type != filter.type) return false;
        return true;
    }

    @Override
    def int hashCode() {
        int result;
        result = type.hashCode();
        result = 31 * result + constraintType.hashCode();
        result = 31 * result + field.hashCode();
        result = 31 * result + template.hashCode();
        if(fieldKeyData) result = 31 * result + fieldKeyData?.hashCode();
        return result;
    }

    @Override
    def String toString ( ) {
        return "Filter{id=${id}, type=${type}, constrainttype=${constraintType}, field=${field}, value=${value}}"
    }

    @Transient
    public Criterion getRestrictions() {
        if (getValue() == null) {
            return null;
        }

        switch (constraintType) {
            case FilterConstraint.EQ:
                return Restrictions.eq(field, getValue())
                break

            case FilterConstraint.LIKE:
                return (Restrictions.ilike(field, stringValue, MatchMode.ANYWHERE))
                break

            case FilterConstraint.DATE_BETWEEN:

                if (startDateValue != null && endDateValue != null) {
                    return Restrictions.between(field, new AdennetHelperService().convertToAdenTimezone(startDateValue, Constants.DEFAULT_TIMEZONE), new AdennetHelperService().convertToAdenTimezone(endDateValue, Constants.DEFAULT_TIMEZONE));

                } else if (startDateValue != null) {
                    return Restrictions.ge(field, startDateValue);

                } else if (endDateValue != null) {
                    return Restrictions.le(field, endDateValue);
                }
                break

            case FilterConstraint.NUMBER_BETWEEN:
                if (decimalValue != null && decimalHighValue != null) {
                    if(type == FilterType.MEDIATIONPROCESS && field.equals("totalRecords")){
                        return Restrictions.between(field, decimalValue.toLong(), decimalHighValue.toLong())
                    }
                    return Restrictions.between(field, decimalValue, decimalHighValue)

                } else if (decimalValue != null) {
                    if(type == FilterType.MEDIATIONPROCESS && field.equals("totalRecords")){
                        return Restrictions.ge(field, decimalValue.toLong())
                    }
                    return Restrictions.ge(field, decimalValue)

                } else if (decimalHighValue != null) {
                    if(type == FilterType.MEDIATIONPROCESS && field.equals("totalRecords")){
                        return Restrictions.le(field, decimalHighValue.toLong())
                    }
                    return Restrictions.le(field, decimalHighValue)
                }
                break

            case FilterConstraint.SIZE_BETWEEN:
                if (decimalValue != null && decimalHighValue != null) {
                    return Restrictions.and(
                                    Restrictions.sizeGe(field, decimalValue.intValue()),
                                    Restrictions.sizeLe(field, decimalHighValue.intValue())
                            )

                } else if (decimalValue != null) {
                    return Restrictions.sizeGe(field, decimalValue.intValue())

                } else if (decimalHighValue != null) {
                    return Restrictions.sizeLe(field, decimalHighValue.intValue())
                }
                break

            case FilterConstraint.IS_EMPTY:
                if (booleanValue) {
                    return Restrictions.isEmpty(field)
                }
                break

            case FilterConstraint.IS_NOT_EMPTY:
                if (booleanValue) {
                    return Restrictions.isNotEmpty(field)
                }
                break
                case FilterConstraint.IS_NULL:
                if (booleanValue) {
                    return Restrictions.isNull(field)
                }
                break

            case FilterConstraint.IS_NOT_NULL:
                if (booleanValue) {
                    return Restrictions.isNotNull(field)
                }
                break
            case FilterConstraint.IN:
                if(listValue){
                    return Restrictions.in(field, listValue)
                }
                break
        }

        return null;
    }
}
