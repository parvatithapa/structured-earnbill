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

import com.sapienter.jbilling.csrf.RequiresValidFormToken

import java.util.List;
import com.sapienter.jbilling.server.security.Validator

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.converters.JSON

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import grails.plugin.springsecurity.annotation.Secured

/**
 * OrderPeriodController 
 *
 * @author Vikas Bodani
 * @since 09-Mar-2011
 */


@Secured(["isAuthenticated()", "MENU_99"])
class OrderPeriodController {

	static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
	static scope = "prototype"
	
    static final viewColumnsToFields =
            ['periodId': 'id',
             'value': 'value']

	def breadcrumbService
	IWebServicesSessionBean webServicesSession
	def viewUtils
    SecurityValidator securityValidator


    def index () {
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def list () {

		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
		
        def period = OrderPeriodDTO.get(params.int('id'))
        securityValidator.validateCompany(period?.company?.id, Validator.Type.VIEW)

        if (params.id?.isInteger() && !period) {
            flash.error = 'orderPeriod.not.found'
            flash.args = [ params.id as String ]
        }
		
		breadcrumbService.addBreadcrumb(controllerName, actionName, period?.getDescription(session['language_id'] as Integer), period?.id)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'periodsTemplate', model: [selected: period]
            } else {
                if(chainModel){
                    render view: 'list', model:[selected: period]+chainModel
                } else {
                    render view: 'list', model: [selected: period]
                }
            }
            return
        }

        def periods= getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'periodsTemplate', model: [ periods: periods, selected: period ]
        } else {
            if(chainModel){
                render view: 'list', model:[selected: period, periods: periods]+chainModel
            } else {
                render view: 'list', model: [periods: periods, selected: period]
            }
        }
	}
	
	def getList(params) {
		
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return OrderPeriodDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(session['company_id']))
            if (params.periodId){
                def searchParam = params.periodId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and a.psudo_column = 'description'
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_ORDER_PERIOD, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
	}


    def findPeriods () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def periods = getList(params)

        try {
            def jsonData = getAsJsonData(periods, params)

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


    def show () {
        def period = OrderPeriodDTO.get(params.int('id'))
        securityValidator.validateCompany(period?.company?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, period.id, period.getDescription(session['language_id']))

        render template: 'show', model: [ selected: period ]
    }

    def edit () {

        def period = params.id ? OrderPeriodDTO.get(params.int('id')) : null
        securityValidator.validateCompany(period?.company?.id, Validator.Type.VIEW)

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? period?.getDescription(session['language_id']) : null
        
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def periodUnits = PeriodUnitDTO.list()
        
        render template: 'edit', model: [ period: period, periodUnits: periodUnits ]
    }
    
    def listEdit () {
        
        def period = params.id ? OrderPeriodDTO.get(params.int('id')) : null
        securityValidator.validateCompany(period?.company?.id, Validator.Type.VIEW)
        
        if (params.id?.isInteger() && !period) {
			redirect action: 'list', params: params
			return
        }
        
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		def periods = getList(params)
		
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? period?.getDescription(session['language_id']) : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)
        
        def periodUnits = PeriodUnitDTO.list()
        
        render view: 'listEdit', model: [periods: periods, period: period, periodUnits: periodUnits]
    }

    @RequiresValidFormToken
	def save () {
        
        OrderPeriodWS ws= new OrderPeriodWS()
        bindData(ws, params)
		log.debug ws
		if(params.description){
			InternationalDescriptionWS descr=
				new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
	        log.debug descr
			ws.descriptions.add descr
		}
		ws.setEntityId(session['company_id'].toInteger())
        if (validOrderPeriod(ws)) {
            log.debug ws
            if(ws.id > 0) {
                def oldPeriod = OrderPeriodDTO.get(params.int('id'))
                securityValidator.validateCompany(oldPeriod?.company?.id, Validator.Type.EDIT)
            }

            try {
                boolean retVal= webServicesSession.updateOrCreateOrderPeriod(ws);
                if (params.isNew=="true")
                {
                    flash.message= 'config.periods.created'
                } else{
                    flash.message= 'config.periods.updated'
                }
            } catch (SessionInternalError e){
                viewUtils.resolveException(flash, session.locale, e);
                chain action: 'list', model:[period:ws, periodUnits: PeriodUnitDTO.list()]
                return
            } catch (Exception e) {
                log.error e.getMessage()
                flash.error = 'config.periods.saving.error'
            }
        }
		redirect (action: 'list')
	}

    private boolean validOrderPeriod(OrderPeriodWS orderPeriod) {
        def periods = webServicesSession.getOrderPeriods()
        for (OrderPeriodWS period: periods) {
            if (period.getEntityId().equals(session['company_id']) && period.getId()!= orderPeriod?.id &&
                    period.getDescription(session['language_id']).content.equals(orderPeriod.getDescription(session['language_id'])?.content)) {
                flash.error ='bean.OrderPeriodWS.validate.duplicate'
                flash.args = [ orderPeriod.getDescription(session['language_id']).content ]
                return false
            }
        }
        return true
    }

	def delete () {
		log.debug 'delete called on ' + params.id
        if (params.id) {
            def period= OrderPeriodDTO.get(params.int('id'))
            securityValidator.validateCompany(period?.company?.id, Validator.Type.EDIT)
            if (period) {
                try {
                    boolean retVal= webServicesSession.deleteOrderPeriod(params.id?.toInteger());
                    if (retVal) { 
                        flash.message= 'config.periods.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'config.periods.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.periods.delete.error'
                }
            }
        }

        // render the period list
        params.applyFilter = true
        params.id = null
        redirect (action: 'list')
	}

}
