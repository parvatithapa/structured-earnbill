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
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.order.db.OrderStatusBL
import com.sapienter.jbilling.server.order.OrderStatusWS
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

/**
 * OrderStatusController 
 *
 * @author Maruthi
 * @since 17-Jul-2013
 */


@Secured(["isAuthenticated()", "MENU_100"])
class OrderStatusController {

    static pagination = [max: 10, offset: 0]

    static final viewColumnsToFields = ['orderStatusId': 'id',
                                          'description': 'description']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def orderStatusList
    ViewUtils viewUtils
    SecurityValidator securityValidator


    def index (){
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def list (){
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.server.util.Constants.PREFERENCE_USE_JQGRID);
        def selected = params.id ? OrderStatusDTO.get(params.int("id")) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (!usingJQGrid){
            orderStatusList = OrderStatusDTO.list().findAll { it?.entityId == session['company_id'] }

            render view: 'list', model: [selected: selected, orderStatusList: orderStatusList, orderStatusWS: chainModel ? chainModel?.orderStatusWS : null]
        }
    }

    def findOrderStatuses (){
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        orderStatusList = OrderStatusDTO.list().findAll { it?.entityId == session['company_id'] }

        try {
            def jsonData = getAsJsonData(orderStatusList, params)

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
        def totalRecords =  jsonCells ? jsonCells.size() : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show (){
        def orderStatus = webServicesSession.findOrderStatusById(params.int('id'))
        securityValidator.validateCompany(orderStatus?.entity.id, Validator.Type.VIEW)
        render template: 'show', model: [selected: orderStatus]
    }


    def edit (){
        def orderStatus = params.id ? webServicesSession.findOrderStatusById(params.int('id')) : null
        if(orderStatus){
            securityValidator.validateCompany(orderStatus?.entity.id, Validator.Type.EDIT)
        }

        render template: 'edit', model: [orderStatusWS: orderStatus ?: new OrderStatusWS()]
    }

    def listEdit (){

        def orderStatus = params.id ? OrderPeriodDTO.get(params.int('id')) : null
        securityValidator.validateCompany(orderStatus?.entity?.id, Validator.Type.EDIT)
        if (params.id?.isInteger() && !orderStatus) {
            redirect action: 'list', params: params
        }
        render view: 'listEdit', model: [orderStatus: orderStatus]
    }

    @RequiresValidFormToken
    def save (){

        OrderStatusWS orderStatusWS = new OrderStatusWS();
        bindData(orderStatusWS, params)
        orderStatusWS.setEntity(webServicesSession.getCompany());
        if (params.description) {
            InternationalDescriptionWS descr =
                    new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
            orderStatusWS.descriptions.add descr
        }
        try {
            def id = webServicesSession.createUpdateOrderStatus(orderStatusWS);
            if (params?.isNew?.equals('true')) {
                flash.message = 'order.status.created'
                flash.args = [id]
            } else {
                flash.message = 'order.status.updated'
				flash.args = [id]
            }
            redirect action: 'list', params: [id: id]
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            chain action: "list", model: [orderStatusWS: orderStatusWS]
            return
        }
    }

    def delete (){
        log.debug 'delete order status called on ' + params.id
        if (params.id) {
            OrderStatusDTO orderStatus = OrderStatusDTO.findByIdAndEntity(params.int('id'), CompanyDTO.get(session['company_id'] as Integer))
            securityValidator.validateCompany(orderStatus?.entity?.id, Validator.Type.EDIT)

            try {
                def orderStatusWS = OrderStatusBL.getOrderStatusWS(orderStatus)
                webServicesSession.deleteOrderStatus(orderStatusWS);
            } catch (Exception e) {
                viewUtils.resolveException(flash, session.locale, e);
                redirect action: 'list'
                return
            }
            flash.message = 'order.status.deleted'

        }
        redirect action: 'list'
    }

}
