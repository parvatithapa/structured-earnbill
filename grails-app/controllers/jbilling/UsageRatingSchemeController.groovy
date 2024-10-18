package jbilling

import com.sapienter.jbilling.client.util.SchemeHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.util.StringUtils


@Secured(["isAuthenticated()", "MENU_99"])
class UsageRatingSchemeController {

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
        def ratingScheme
        try {
            ratingScheme = webServicesSession.getUsageRatingScheme(params.int('id'))

        } catch(SessionInternalError sie) {
            ratingScheme = null
        }

        if (params.id?.isInteger() && !ratingScheme) {
            flash.error = 'ratingScheme.not.found'
            flash.args = [ params.id as String ]
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def ratingSchemes = webServicesSession.getAllUsageRatingSchemes(params.max as Integer,params.offset as Integer)
        def size = webServicesSession.countUsageRatingSchemes()

        if (params.applyFilter || params.partial) {
            render template: 'schemes', model: [ ratingSchemes: ratingSchemes, selected: ratingScheme, size: size]
        } else {
            if (chainModel?.containsKey("usageRatingSchemeWS")) {
                def ratingSchemeType=null

                ratingScheme = chainModel.get("usageRatingSchemeWS")

                def ratingSchemeTypeName = ratingScheme.ratingSchemeType
                if (!StringUtils.isEmpty(ratingSchemeTypeName)) {
                    ratingSchemeType = webServicesSession.findUsageRatingSchemeInstanceByName(ratingSchemeTypeName)
                }
                def ratingSchemeStrategies = getRatingSchemeStrategies()

                render view: 'list', model: [ ratingSchemes         : ratingSchemes,
                                              ratingScheme          : ratingScheme,
                                              ratingSchemeType      : ratingSchemeType,
                                              ratingSchemeStrategies: ratingSchemeStrategies, size: size] + chainModel

            } else {
                render view: 'list', model:[selected: ratingScheme, ratingSchemes: ratingSchemes, size: size]
            }
        }
    }

    def show() {

        def usageRatingScheme=webServicesSession.getUsageRatingScheme(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, usageRatingScheme.id, usageRatingScheme.ratingSchemeCode)

        render template: 'show', model: [ selected: usageRatingScheme ]
    }

    def edit() {

        def usageRatingScheme=params.id ? webServicesSession.getUsageRatingScheme(params.int("id")) : null
        //def crumbName = params.id ? 'update' : 'create'
        //def crumbDescription = params.id ? usageRatingScheme?.ratingSchemeCode : null
        //breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def model = usageRatingScheme ?
                [ ratingScheme: usageRatingScheme,
                  ratingSchemeType: webServicesSession.findUsageRatingSchemeInstanceByName(usageRatingScheme.getRatingSchemeType()),
                  ratingSchemeStrategies: getRatingSchemeStrategies() ] :
                [ ratingScheme: usageRatingScheme,
                  ratingSchemeStrategies: getRatingSchemeStrategies() ]

        render template: 'edit', model: model
    }



    def updateStrategy() {
        def selectedTemplate = params.templateName

        IUsageRatingScheme ratingSchemeType = webServicesSession
                .findUsageRatingSchemeInstanceByName(selectedTemplate)

        render template: 'attributes',
                model: [ ratingSchemeType: ratingSchemeType ]
    }

    def addDynamicAttributeRow() {
        UsageRatingSchemeWS ws = new UsageRatingSchemeWS()
        bindData(ws, params)
        SchemeHelper.bindRatingScheme(ws,params)

       /* render template: 'attributes',
                model: [ratingScheme:ws]
       */
        render template: 'edit',
                model: [ ratingScheme: ws,
                         ratingSchemeType: webServicesSession.findUsageRatingSchemeInstanceByName(ws.getRatingSchemeType()),
                         ratingSchemeStrategies: getRatingSchemeStrategies() ]
    }

    def getRatingSchemeStrategies() {
        return webServicesSession.findAllRatingSchemeTypeValues()
    }

    def save() {
        UsageRatingSchemeWS ws = new UsageRatingSchemeWS()
        bindData(ws, params)
        SchemeHelper.bindRatingScheme(ws,params)

        log.debug ws
         try {
             webServicesSession.createUsageRatingScheme(ws)
             if (params.isNew == "true") {
                 flash.message = 'config.rating.scheme.created'
             } else {
                 flash.message = 'config.rating.scheme.updated'
             }
         } catch(SessionInternalError e) {
             viewUtils.resolveException(flash, session.locale, e)
             chain action: 'list',
                   model: [ usageRatingSchemeWS: ws, showEditTemplate: true ]
             return
         }
        redirect (action: 'list')
    }

    def delete() {
        log.debug 'delete called on ' + params.id
        if (params.id) {
            def ratingScheme = webServicesSession.getUsageRatingScheme(params.int("id"))
            if (ratingScheme) {
                try {
                    boolean retVal= webServicesSession.deleteUsageRatingScheme(params.id?.toInteger())
                    if (retVal) {
                        flash.message= 'config.rating.scheme.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'config.rating.scheme.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e)
                }
            }
        }
        params.id = null
        getList(params)
    }
}


