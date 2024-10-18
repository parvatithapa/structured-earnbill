package jbilling

import com.sapienter.jbilling.client.util.SchemeHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

class ApiUserDetailController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    def breadcrumbService
    def webServicesSession
    def viewUtils


    def index() {
        redirect action: list, params: params
    }

    def list() {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        getList(params)
    }

    def getList(params) {
        def apiUserDetails = webServicesSession.getAllApiUserDetails(params.max as Integer,params.offset as Integer)
        def size = webServicesSession.countApiUserDetails()

        render view: 'list', model:[apiUserDetails: apiUserDetails, size: size];
    }

    def save() {
        ApiUserDetailWS ws = new ApiUserDetailWS()
        bindApiUserDetails(ws,params)
        log.debug ws

        try {
            String accessCode = webServicesSession.createApiUserDetail(ws);
            flash.message = 'api.user.details.created'
        } catch(SessionInternalError e) {
            flash.error = 'api.user.details.invaild'
        }
        redirect (action: 'list')
    }

    def bindApiUserDetails(ApiUserDetailWS ws,GrailsParameterMap params){

        if(params?.userName)
            ws.setUserName(params.userName)
        if(params?.password)
            ws.setPassword(params.password)
    }

    def edit() {

        def apiUserDetails = webServicesSession.getAllApiUserDetails(params.max as Integer,params.offset as Integer)
        def size = webServicesSession.countApiUserDetails()

        def model = [apiUserDetails:apiUserDetails,size:size]

        render template: 'edit', model: model;
    }

}
