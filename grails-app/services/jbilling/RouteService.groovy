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

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pricing.RouteBeanFactory
import com.sapienter.jbilling.server.pricing.db.RouteDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.search.BasicFilter
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint
import com.sapienter.jbilling.server.util.search.SearchCriteria
import com.sapienter.jbilling.server.util.search.SearchResult

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

class RouteService implements Serializable {

    static transactional = true

    def dataSource
    IWebServicesSessionBean webServicesSession

    /**
     * Filter for the routes from a specific route table. The search will be case insensitive
     * and match the start of the string.
     *
     * @param tableId - route table id
     * @param filters - map of key value pairs.
     * @param params - paging and sorting arguments
     * @return
     */
    SearchResult<String> getFilteredRecords(Integer tableId, Map filters, GrailsParameterMap params) {
        final String DOUBLE_QUOTES = "\"";
        def sortIndex = params.sidx ?: 'id'
        def sortOrder  = params.sord ? params.sord.toUpperCase() : 'ASC'
        def maxRows = Integer.valueOf(params.rows)
        def currentPage = params.page ? Integer.valueOf(params.page) : 1

        def rowOffset = currentPage == 1 ? 0 : (currentPage - 1) * maxRows

        //build the criteria object
        SearchCriteria criteria = new SearchCriteria()
        criteria.offset = rowOffset
        criteria.max = maxRows
        criteria.sort = sortIndex
        criteria.direction = sortOrder
        criteria.total = params.int('total') ?: -1

        def criteriaFilters = []

        //build the search filters
        filters.each {
            if(it.value) {
                def constraint
                //change set into list
                if(it.value instanceof Set) {
                    it.value = it.value.collect {e-> e}
                }

                if(it.value instanceof List) {
                    constraint = FilterConstraint.IN
                } else if(it.key == 'id') {
                    constraint = FilterConstraint.EQ
                    it.value = it.value.isInteger() ? it.value.toInteger() : 0
                } else {
                    constraint = FilterConstraint.ILIKE
                }
                if(it.value instanceof List) {
                    if(it.key == 'in.id') {
                        it.value = it.value.collect {e-> e.isInteger() ? e.toInteger() : 0}
                    }
                }

                //IN criteria used in nested search starts with 'in.'
                def key = it.key.contains('.') ? it.key.substring(it.key.indexOf('.')+1) : it.key

                /*
                 * The double quotes are necessary, because if the name of the column has a '-'
                 * without this, postgresql throws a exception
                 */
                criteriaFilters << new BasicFilter(DOUBLE_QUOTES + key.toString().toLowerCase() + DOUBLE_QUOTES, constraint, it.value)
            }
        }

        criteria.filters = criteriaFilters as BasicFilter[]
        if (params.controller=="route"){
            return webServicesSession.searchDataTable(tableId, criteria)
        }else{
            return webServicesSession.searchRouteRateCard(tableId, criteria)
        }
    }


    SearchResult<String> getFilteredRecords(Integer tableId, Map filters) {
        //build the criteria object
        def sortIndex = 'id'
        def sortOrder  = 'ASC'
        //TODO need to fix the max count logic
        def maxRows =1000
        def currentPage =  1

        def rowOffset = currentPage == 1 ? 0 : (currentPage - 1) * maxRows

        SearchCriteria criteria = new SearchCriteria()
        criteria.offset = rowOffset
        criteria.max = maxRows
        criteria.sort = sortIndex
        criteria.direction = sortOrder
        criteria.total = -1
        def criteriaFilters = []

        //build the search filters
        filters.each {
            if(it.value) {
                def constraint

                if(it.value instanceof Map){
                    Map values = (Map)it.value
                    constraint = FilterConstraint.valueOf(values.constraint as String)
                    criteriaFilters << new BasicFilter(values.key as String, constraint, values.value)
                    return ;
                }

                //change set into list
                if(it.value instanceof Set) {
                    it.value = it.value.collect {e-> e}
                }

                if(it.value instanceof List) {
                    constraint = FilterConstraint.IN
                } else if(it.key == 'id') {
                    constraint = FilterConstraint.EQ
                    it.value = it.value.isInteger() ? it.value.toInteger() : 0
                } else {
                    constraint = FilterConstraint.ILIKE
                }
                if(it.value instanceof List) {
                    if(it.key == 'in.id') {
                        it.value = it.value.collect {e-> e.isInteger() ? e.toInteger() : 0}
                    }
                }

                //IN criteria used in nested search starts with 'in.'
                def key = it.key.contains('.') ? it.key.substring(it.key.indexOf('.')+1) : it.key

                criteriaFilters << new BasicFilter(key, constraint, it.value)
            }
        }

        criteria.filters = criteriaFilters as BasicFilter[]

        return webServicesSession.searchDataTable(tableId, criteria)

    }

    static <T> T fetchData(SearchResult<String> result, RouteDTO routeDTO, String columnName, Class<T> type) {

        if (result.getRows().size() == 0) {
            throw new SessionInternalError("No record found in \""+routeDTO.getName()+"\" data table");
        }

        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();
        Integer searchNameIdx = columnNames.indexOf(columnName);

        if (searchNameIdx.equals(-1)) {
            throw new SessionInternalError("Date table \""+routeDTO.getName()+"\" does not contain column : " + columnName);
        }

        Object value = result.getRows().get(0).get(searchNameIdx);
        if(value!=null && value.isEmpty()){
            throw new SessionInternalError("The \""+columnName+"\" column value is empty in a \""+routeDTO.getName()+"\" table.");
        }

        if (Boolean.class == type) return Boolean.parseBoolean(value);

        try{
            if (Integer.class == type) return Integer.parseInt(value);
            if (Long.class == type) return Long.parseLong(value);
            if (BigDecimal.class == type) return new BigDecimal(value);
        }catch (NumberFormatException e){
            throw new SessionInternalError("Data is not valid in data table : \""+routeDTO.getName()+"\". \n Not able to parse "+routeDTO.getName()+"."+columnName+ " value ["+value+"] into type \""+type.getSimpleName()+"\"")
        }

        return value;
    }
}
