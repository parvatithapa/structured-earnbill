package jbilling

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.pricing.RatingUnitWS
import com.sapienter.jbilling.server.pricing.db.RatingUnitDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.plugin.springsecurity.annotation.Secured

/**
 * RatingUnitController
 *
 * @author Panche Isajeski
 * @since 27-Aug-2013
 */

@Secured(["isAuthenticated()", "MENU_99"])
class RatingUnitController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    def breadcrumbService
    def webServicesSession
    def viewUtils
    SecurityValidator securityValidator


    def index (){
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def list (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
        getList(params)
    }

    def getList(params) {

        def ratingUnit = RatingUnitDTO.get(params.int('id'))

        securityValidator.validateCompany(ratingUnit?.company?.id, Validator.Type.VIEW)

        if (params.id?.isInteger() && !ratingUnit) {
            flash.error = 'ratingUnit.not.found'
            flash.args = [ params.id as String ]
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def ratingUnits= getRatingUnitsForEntity()

        if (params.applyFilter || params.partial) {
            render template: 'units', model: [ ratingUnits: ratingUnits, selected: ratingUnit ]
        } else {
            if (chainModel) {
                render view: 'list', model:[selected: ratingUnit, ratingUnits: ratingUnits, showEditTemplate: params.showEditTemplate] + chainModel
            } else {
                render view: 'list', model:[ratingUnits: ratingUnits, selected: ratingUnit, showEditTemplate: params.showEditTemplate]
            }
        }
    }

    def show (){

        def ratingUnit = RatingUnitDTO.get(params.int('id'))

        securityValidator.validateCompany(ratingUnit?.company?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, ratingUnit.id, ratingUnit.name)

        render template: 'show', model: [ selected: ratingUnit ]
    }

    def edit (){
        def ratingUnit =  params.id ? webServicesSession.getRatingUnit(params.int("id")) : new RatingUnitWS()

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? ratingUnit?.name : null

        if(params.id) {
            securityValidator.validateCompany(ratingUnit.entityId, Validator.Type.EDIT)
        }

        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def ratingUnits = RatingUnitDTO.list()

        render template: 'edit', model: [ ratingUnit: ratingUnit, ratingUnits: ratingUnits ]
    }

    def listEdit (){

        def ratingUnit =  params.id ? webServicesSession.getRatingUnit(params.int("id")) : new RatingUnitWS()

        if (params.id?.isInteger() && !ratingUnit) {
            redirect action: 'list', params: params
            return
        }
        if(ratingUnit) {
            securityValidator.validateCompany(ratingUnit.entityId, Validator.Type.EDIT)
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? ratingUnit?.name : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def ratingUnits= getRatingUnitsForEntity()

        render view: 'listEdit', model: [ratingUnit: ratingUnit, ratingUnits: ratingUnits]
    }

    @RequiresValidFormToken
    def save (){

        RatingUnitWS ws = new RatingUnitWS()
        bindData(ws, params)

        if(ws.id) {
            def ratingUnit =  webServicesSession.getRatingUnit(ws.id)
            securityValidator.validateCompany(ratingUnit.entityId, Validator.Type.EDIT)
        }

        log.debug ws

        try {
			if (params.isNew=="true")
            {
                webServicesSession.createRatingUnit(ws);
                flash.message= 'config.rating.unit.created'
            }
            else{
                webServicesSession.updateRatingUnit(ws);
                flash.message= 'config.rating.unit.updated'
            }
        } catch (SessionInternalError e){
            viewUtils.resolveException(flash, session.locale, e);
            chain action: 'list', model: [ratingUnitWS:ws], params: [showEditTemplate: true]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.rating.unit.saving.error'
        }
        redirect (action: 'list')

    }

    def delete (){
        log.debug 'delete called on ' + params.id
        if (params.id) {
            def ratingUnit= RatingUnitDTO.get(params.int('id'))
            if (ratingUnit) {
                securityValidator.validateCompany(ratingUnit.company.id, Validator.Type.EDIT)

                try {
                    boolean retVal= webServicesSession.deleteRatingUnit(params.id?.toInteger());
                    if (retVal) {
                        flash.message= 'config.rating.unit.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'config.rating.unit.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.rating.unit.delete.error'
                }
            }
        }

        // render the rating units
        params.id = null
        getList(params)
    }

    def getRatingUnitsForEntity () {
        return RatingUnitDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(session['company_id']))
            order("id", "desc")
        }
    }
}
