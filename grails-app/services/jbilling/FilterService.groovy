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

import com.sapienter.jbilling.client.filters.FilterFactory
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.DateMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.DecimalMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.JsonMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import org.hibernate.criterion.MatchMode

import java.text.ParseException
import java.text.SimpleDateFormat

import javax.servlet.http.HttpSession

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.springframework.web.context.request.RequestContextHolder

/**
 * FilterService
 *
 * @author Brian Cowdery
 * @since  30-11-2010
 */
class FilterService implements Serializable {

    private static final String SESSION_CURRENT_FILTER_TYPE = "current_filter_type";
    private static final String SESSION_CUSTOM_FILTER_ID = "custom_filter_id";

    /**
     * Fetches the filters for the given type and sets the filter values from the UI
     * input fields if the "applyFilter" request parameter is present.
     *
     * Filters are available in the session (and can also be set elsewhere in the session)
     * using the key FilterType.name() + "_FILTERS".
     *
     * @param type filter type
     * @param params request parameters from controller action
     * @return filters for the given filter type
     */
    def Object getFilters(FilterType type, GrailsParameterMap params) {
        def session = getSession()
        def key = getSessionKey(type)

        /*
            Fetch filters for the given filter type. If the filters are already
            in the session, use the existing filters instead of fetching new ones.
         */
        def filters = session[key] && !params?.restoreFilters ? session[key] : FilterFactory.getFilters(type)
        // update filters with values from request parameters
        if (params?.boolean("applyFilter")) {
            filters.each { it.clear() }
            params.filters.each{ filterName, filterParams ->
                if (filterParams instanceof Map) {
                    def filter = filters.find{ it.name == filterName }
                    bindData(filter, filterParams, null);
                    if(params["${filter?.name}.fieldKeyData"]) filter.fieldKeyData = params["${filter?.name}.fieldKeyData"]
                }
            }
        }

        // store filters in session for next request
        session[SESSION_CURRENT_FILTER_TYPE] = type;
        if (params?.restoreFilters) {
            session[SESSION_CUSTOM_FILTER_ID] = null
        }
        session[key] = filters
        return filters
    }

    /**
     * Adds a filter to the session list for the given type. If the filter already exists
     * the current filter will be replaced.
     *
     * Note that this is not persisted across sessions unless the user saves the filter set.
     *
     * @param type type of filter list
     * @param filter filter with value to set
     * @param reset reset other filters by clearing their value, optional; defaults to true
     */
    def void setFilter(FilterType type, Filter filter, boolean reset = true) {
        def filters = getFilters(type, null)

        if (filters) {
            def index = filters.indexOf(filter)
            if (index >= 0) {
                filters.putAt(index, filter)
            } else {
                filters.add(filter)
            }

            // clear all other filters values
            if (reset) {
                filters.each {
                    if (!it.equals(filter)) {
                        it.clear()
                    }
                }
            }

            session[getSessionKey(type)] = filters
            session[SESSION_CUSTOM_FILTER_ID] = null
        }
    }

    /**
     * Returns the current filters based on the last set used. For example, if you had previously
     * fetched customer filters, this method would return the customer filters.
     *
     * @return current filter list
     */
    def Object getCurrentFilters() {
        def type = (FilterType) session[SESSION_CURRENT_FILTER_TYPE]
        return type ? getFilters(type, null) : null
    }

    def FilterType getCurrentFilterType() {
        return (FilterType) session[SESSION_CURRENT_FILTER_TYPE]
    }

    /**
     * Loads the filters for the given FilterSet id, updating the filter list
     * in the session for current usage.
     *
     * @param filterSetId filter set id
     * @return filter list
     */
    def Object loadFilters(Integer filterSetId) {
        def filterset = FilterSet.get(filterSetId)
        def type = (FilterType) session[SESSION_CURRENT_FILTER_TYPE]

        if (filterset.filters.find{ it.type != FilterType.ALL && it.type != type }) {
            session.error = 'filters.cannot.load.message'
            return getCurrentFilters()
        }

        // always make the loaded filters visible
        filterset.filters.each { filter->
            if (filter.value) {
                filter.visible = true
            }
        }

        List filterList = filterset.filters.asList()
        session[getSessionKey(type)] = filterList
        session[SESSION_CUSTOM_FILTER_ID] = filterSetId

        return filterList
    }

    /**
     * Changes the visibility of a filter so that it appears in the filter pane.
     *
     * @param name filter name to show
     * @return updated filter list
     */
    def Object showFilter(String name) {
        def filters = getCurrentFilters()
        filters?.each{
            if (it.name == name)
                it.visible = true
        }

        def type = (FilterType) session[SESSION_CURRENT_FILTER_TYPE]
        session[getSessionKey(type)] = filters
        return filters
    }

    /**
     * Changes the visibility of the filter so that it is removed from the filter pane. This
     * method also clears the filter's set value so that it's effect on the entity criteria
     * will be removed.
     *
     * @param name filter name to remove
     * @return updated filter list
     */
    def Object removeFilter(String name) {
        def filters = getCurrentFilters()
        filters?.each{
            if (it.name == name) {
                it.visible = false
                it.clear()
            }
        }

        def type = (FilterType) session[SESSION_CURRENT_FILTER_TYPE]
        session[getSessionKey(type)] = filters
        return filters
    }

    def Object clearCurrentFilter(){
        def type = (FilterType) session[SESSION_CURRENT_FILTER_TYPE]
        def filters = getCurrentFilters()

        filters?.each {
            it?.clear()
        }

        session[getSessionKey(type)] = filters
        return filters
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    /**
     * Returns the session attribute key for the given set of filters.
     *
     * @param type filter type
     * @return session attribute key
     */
    def String getSessionKey(FilterType type) {
        return "${type.name()}_FILTERS"
    }

    private static def bindData(Object model, modelParams, String prefix) {
        if (model != null) {
            def args = [ model, modelParams, [exclude:[], include:[]]]
            if (prefix) args << prefix

            new BindDynamicMethod().invoke(model, 'bind', (Object[]) args)
        }
    }


    public MetaFieldType getMetaFieldTypeForFilter(String filterField){
        MetaFieldType type
        switch(filterField){
            case 'contact.firstName':
                type = MetaFieldType.FIRST_NAME
                break;
            case 'contact.lastName':
                type = MetaFieldType.LAST_NAME
                break;
            case 'contact.organizationName':
                type = MetaFieldType.ORGANIZATION
                break;
            case 'contact.postalCode':
                type = MetaFieldType.POSTAL_CODE
                break;
            case 'contact.phoneNumber':
                type = MetaFieldType.PHONE_NUMBER
                break;
            case 'contact.email':
                type = MetaFieldType.EMAIL
                break;
        }
        return type
    }


    /**
     *This method is adding to the criteria according to the datatype passed
     *
     * @param name builder represents the delegate of criteria
     * @param name type the type of Metafield
     * @param name ccfValue value in the filter field from screen
     * @return updated the criteria
     */
    static DetachedCriteria metaFieldFilterCriteria(type, String ccfValue) {
        def subCriteria = null
        try {
            switch (type.getDataType()) {
                case DataType.DATE:
                    subCriteria = DetachedCriteria.forClass(DateMetaFieldValue.class, "dateValue")
                            .setProjection(Projections.property('id'))
                            .add(Restrictions.eq('dateValue.value', convertDate(ccfValue)))

                    break;
                case DataType.JSON_OBJECT:
                    subCriteria = DetachedCriteria.forClass(JsonMetaFieldValue.class, "jsonValue")
                            .setProjection(Projections.property('id'))
                            .add(Restrictions.like('jsonValue.value', ccfValue + '%').ignoreCase())

                    break;
                case DataType.ENUMERATION:
                case DataType.STRING:
                case DataType.TEXT_AREA:
                case DataType.SCRIPT:
                case DataType.STATIC_TEXT:
                case DataType.LIST:
                    if(type.getName() == "First Name" ){
                        subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                .setProjection(Projections.property('id'))
                                .add(Restrictions.ilike('stringValue.value', ccfValue , MatchMode.ANYWHERE))
                    } else {
                        subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                .setProjection(Projections.property('id'))
                                .add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())
                    }

                    break;
                case DataType.DECIMAL:
                    subCriteria = DetachedCriteria.forClass(DecimalMetaFieldValue.class, "decimalValue")
                            .setProjection(Projections.property('id'))
                            .add(Restrictions.eq('decimalValue.value', ccfValue.toBigDecimal()))
                    break;
                case DataType.INTEGER:
                    subCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                            .setProjection(Projections.property('id'))
                            .add(Restrictions.eq('integerValue.value', ccfValue.toInteger()))

                    break;
                case DataType.BOOLEAN:
                    subCriteria = DetachedCriteria.forClass(BooleanMetaFieldValue.class, "booleanValue")
                            .setProjection(Projections.property('id'))
                            .add(Restrictions.eq('booleanValue.value', ccfValue.toBoolean()))
                    break;
            }
        } catch (NumberFormatException exception) {
            throw new SessionInternalError("Invalid value \"" + ccfValue.toString() + "\" passed in Custom Field of data type " + type.getDataType().toString());
        } catch (ParseException parseException) {
            throw new SessionInternalError("Invalid value \"" + ccfValue.toString() + "\" passed in Custom Field of data type " + type.getDataType().toString());
        }
        return subCriteria
    }

    /**
     * Mapping the filter date to the date format stored in database
     *
     * @param input date as string
     * @return string to Date
     *
     */
    static def convertDate(String input) {
        final String inputFormat = "MM/dd/yyyy";
        final String outputFormat = "yyyy-MM-dd HH:mm:ss";
        String output = new SimpleDateFormat(outputFormat).format(new SimpleDateFormat(inputFormat).parse(input));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(outputFormat)
        Date date = simpleDateFormat.parse(output)
        return date
    }

    def boolean isCustomFilter(){
        return session[SESSION_CUSTOM_FILTER_ID] ? true : false
    }
}
