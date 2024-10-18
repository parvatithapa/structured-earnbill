/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.mediation.MediationConfigurationBL
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS
import com.sapienter.jbilling.server.mediation.RatingSchemeAssociationWS
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration
import com.sapienter.jbilling.server.pricing.RatingSchemeBL
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.user.EntityBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import grails.plugin.springsecurity.annotation.Secured

@Secured(["isAuthenticated()", "MENU_99"])
class MediationRatingSchemeConfigController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    def breadcrumbService
    def webServicesSession
    def viewUtils

    def index (){
        redirect action: list, params: params
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
        def ratingScheme = webServicesSession.getRatingScheme(params.int('id'))

        if (params.id?.isInteger() && !ratingScheme) {
            flash.error = 'ratingScheme.not.found'
            flash.args = [ params.id as String ]
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def ratingSchemes = webServicesSession.getRatingSchemesPagedForEntity(params.max as Integer,
                                                                              params.offset as Integer)
        def size = webServicesSession.countRatingSchemesPagedForEntity();

        if (params.applyFilter || params.partial) {
            render template: 'schemes', model: [ ratingSchemes: ratingSchemes, selected: ratingScheme, size: size]
        } else {
            if(chainModel){
                render view: 'list', model:[selected: ratingScheme, ratingSchemes: ratingSchemes, size: size] + chainModel
            } else {
                render view: 'list', model:[selected: ratingScheme, ratingSchemes: ratingSchemes, size: size]
            }
        }
    }

    def show (){
        def mediationRatingScheme = webServicesSession.getRatingScheme(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, mediationRatingScheme.id, mediationRatingScheme.name)

        render template: 'show', model: [ selected: mediationRatingScheme]
    }

    def edit (){
        def mediationRatingScheme =  params.id ? webServicesSession.getRatingScheme(params.int("id")) : new MediationRatingSchemeWS()
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? mediationRatingScheme?.name : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        render template: 'edit', model: [ ratingScheme: mediationRatingScheme, mediations: getGlobalMediations()]
    }

    def save (){
        MediationRatingSchemeWS ws = new MediationRatingSchemeWS()
        bindData(ws, params)

        bindAssociations(ws)

        log.debug ws
        try {
            webServicesSession.createRatingScheme(ws);
            if (params.isNew=="true") {
                flash.message= 'config.rating.scheme.created'
            } else {
                flash.message= 'config.rating.scheme.updated'
            }
        } catch (SessionInternalError e){
            viewUtils.resolveException(flash, session.locale, e);
            chain action: 'list', model: [mediationRatingSchemeWS:ws], params: [showEditTemplate: true]
            return
        }
        redirect (action: 'list')
    }

    private void bindAssociations(MediationRatingSchemeWS ws) {
        def associations = params.get("associations")
        List<RatingSchemeAssociationWS> associationList = new ArrayList<>()
        if (associations && !ws.isGlobal()) {
            def mediations = associations.getList("mediation.id")
            def companies = associations.getList("company.id")
            def ratingSchemes = associations.getList("ratingScheme")
            def ids = associations.getList("id")
            for (int i = 0; i < mediations.size(); i++) {
                RatingSchemeAssociationWS newAssociation = new RatingSchemeAssociationWS();
                CompanyWS company = new CompanyWS()
                company.setId(Integer.parseInt(companies[i]))
                MediationConfigurationWS mediation = new MediationConfigurationWS()
                mediation.setId(Integer.parseInt(mediations[i]))
                newAssociation.setCompany(company)
                newAssociation.setMediation(mediation)
                if (!"".equals(ratingSchemes[i])) {
                    newAssociation.setRatingScheme(Integer.parseInt(ratingSchemes[i]))
                }
                if (!"".equals(ids[i])) {
                    newAssociation.setId(Integer.parseInt(ids[i]))
                }
                associationList.add(newAssociation)
            }
        }
        ws.setAssociations(associationList)
    }

    def delete (){
        log.debug 'delete called on ' + params.id
        if (params.id) {
            def ratingScheme = webServicesSession.getRatingScheme(params.int("id"))
            if (ratingScheme) {
                try {
                    boolean retVal= webServicesSession.deleteRatingScheme(params.id?.toInteger());
                    if (retVal) {
                        flash.message= 'config.rating.scheme.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'config.rating.scheme.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e);
                }
            }
        }
        params.id = null
        getList(params)
    }

    def listEdit (){

        def ratingScheme =  params.id ? webServicesSession.getRatingScheme(params.int("id")) : new MediationRatingSchemeWS()

        if (params.id?.isInteger() && !ratingScheme) {
            redirect action: 'list', params: params
            return
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? ratingScheme?.name : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def ratingSchemes = webServicesSession.getRatingSchemesForEntity()

        render view: 'listEdit', model: [ ratingScheme: ratingScheme, ratingSchemes: ratingSchemes, mediations: getGlobalMediations()]
    }

    def test(){
		def resultQuantity = null
		def callDuration = params.int('callDuration')
        def ratingSchemeId = params.int('ratingSchemeId')

		if (callDuration != null) {
			resultQuantity = webServicesSession.getQuantity((Integer) ratingSchemeId, (Integer) callDuration)
		}
        render template: 'test', model: [callDuration: callDuration, resultQuantity: resultQuantity]
    }

    private def getGlobalMediations() {
        def parentCompany = CompanyDTO.get(session['company_id'])
        List<MediationConfiguration> globalMediations =  MediationConfigurationBL.getAllGlobalByCompany(parentCompany?.id);
        return globalMediations;
    }

    public def retrieveAvailableCompanies(){
        def exclude = params.get('toExcludeCompanies[]')
        def mediation = params.int('mediation')
        def ratingSchemeId = params.int('ratingScheme')
        def excludeList = new ArrayList<>()
        if(exclude) {
            excludeList = Arrays.asList(exclude)
        }
        def parentCompany = CompanyDTO.get(session['company_id'])
        List<CompanyDTO> childs = CompanyDTO.findAllByParent(parentCompany)
        childs.add(parentCompany)


        List<CompanyDTO> notExcludedCompanies = new ArrayList<>()
        //Exclude those companies which are already in the association UI list for the current mediation
        for(CompanyDTO entity: childs) {
            if(!excludeList.contains(entity.getId().toString())) {
                notExcludedCompanies.add(entity)
            }
        }

        //Exclude those companies which already have associated a rating scheme for another mediation
        List<CompanyDTO> allowedCompanies = new ArrayList<>()
        def associatedCompaniesForMediation = RatingSchemeBL.findAssociatedCompaniesForMediation(mediation, ratingSchemeId)
        for(CompanyDTO entity: notExcludedCompanies) {
            if(!associatedCompaniesForMediation.contains(entity.getId())) {
                allowedCompanies.add(entity)
            }
        }

        if(allowedCompanies.empty) {
            render "are associated"
        } else {
            CompanyDTO.metaClass.toString = {return delegate.id + " : "+ delegate.description }
            render g.select(
                    from: allowedCompanies,
                    id: 'companies',
                    name: 'companies',
                    optionKey: 'id',
                    noSelection: ['':'-Select Company'])
        }
    }

    public def addRatingSchemeAssociation() {
        def ratingSchemeId = params.int('ratingScheme')
        def mediationId = params.int('mediation')
        def companyId = params.int('company')

        RatingSchemeAssociationWS ws = new RatingSchemeAssociationWS();
        ws.setCompany(EntityBL.getCompanyWS(CompanyDTO.get(companyId)))
        ws.setMediation(MediationConfigurationBL.getWS(MediationConfiguration.get(mediationId)))
        ws.setRatingScheme(ratingSchemeId)

        render template: 'associations', model: [associations: ws]
    }

}
