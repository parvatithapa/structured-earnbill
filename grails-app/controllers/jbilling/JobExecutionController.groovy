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

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDAS
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.joda.time.format.DateTimeFormat

@Secured(["isAuthenticated()", "MENU_100"])
class JobExecutionController {

    static pagination = [max: 10, offset: 0, sort: "id", order: "desc"]

    static final viewColumnsToFields = ['orderStatusId': 'id',
                                          'description': 'description']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def jobExecutionList
    ViewUtils viewUtils
    SecurityValidator securityValidator


    def index (){
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def list (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.server.util.Constants.PREFERENCE_USE_JQGRID);
        def selected = params.id ? JobExecutionHeaderDTO.get(params.int("id")) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (!usingJQGrid){
            jobExecutionList = findJobExecutions(params)
        }
        def jobTypes=new JobExecutionHeaderDAS().findDistinctJobTypes();
        jobTypes.add("");
        if(params.partial) {
            render template: 'jobExecutionList', model: [jobExecutionList: jobExecutionList,jobTypes:jobTypes]
        } else {
            render view: 'list', model: [selected: selected, jobExecutionList: jobExecutionList,jobTypes:jobTypes]
        }
    }

    private findJobExecutions(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return JobExecutionHeaderDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('entityId', session['company_id'])

            if(params.id) {
                eq('id', params.int('id'))
            }
            if(params.startDate) {
                ge('startDate', DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate())
            }
            if(params.startBeforeDate) {
                le('startDate', DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startBeforeDate).toDate()+1)
            }
            if(params.endBefore) {
                le('endDate', DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.endBefore).toDate())
            }
            if(params.jobType) {
                eq('jobType', params.jobType)
            }
            if(params.status) {
                try {
                    eq('status', JobExecutionHeaderDTO.Status.valueOf(params.status) )
                } catch (IllegalArgumentException e) {} //throws when status does not exist
            }

            // apply sorting
            SortableCriteria.buildSortNoAlias(params, delegate)
        }
    }

    def findJobExecutionsAsJson (){
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS
        params.endBefore = params.endDate
        params.endDate = ''

        jobExecutionList = findJobExecutions(params)

        try {
            def jsonData = getAsJsonData(jobExecutionList, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show (){
        def selected = JobExecutionHeaderDTO.get(params.int("id"))
        securityValidator.validateCompany(selected?.entityId, Validator.Type.VIEW)
        render template: 'show', model: [selected: selected]
    }

}
