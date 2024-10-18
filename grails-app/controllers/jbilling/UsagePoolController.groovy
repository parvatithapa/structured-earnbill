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
import com.sapienter.jbilling.server.notification.NotificationBL
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.notification.NotificationMediumType
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO
import com.sapienter.jbilling.server.usagePool.UsagePoolWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.client.usagePool.UsagePoolHelper;
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum
import com.sapienter.jbilling.server.usagePool.db.ComsumptionActionDTO

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.CriteriaSpecification

/**
 * UsagePoolController 
 *
 * @author Amol Gadre
 * @since 12-Nov-2013
 */


@Secured(["isAuthenticated()", "MENU_99"])
class UsagePoolController {

	static pagination = [ max: 10, offset: 0 ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['poolId': 'id']

	def breadcrumbService
	IWebServicesSessionBean webServicesSession
	def viewUtils
	def companyService
    SecurityValidator securityValidator


    def index (){
        flash.invalidToken = flash.invalidToken
       redirect action: 'list', params: params
    }

    def getList(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return UsagePoolDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('entity', new CompanyDTO(session['company_id']))
			order('id', 'desc')
            if (params.poolId){
                def searchParam = params.poolId
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
                                            and a.psudo_column = 'name'
                                            and lower(a.content) like ?
                                        )
                                    """,[Constants.TABLE_USAGE_POOL,languageId,searchParam]
                    )
                }
            }
        }
    }

    def list (){
        def selected = params.id ? UsagePoolDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, selected?.getDescription(session['language_id'], 'name'))

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'usagePools.not.found'
            flash.args = [params.id]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'usagePoolsTemplate', model: [selected: selected ]
            }else {
                [selected: selected]
            }
            return
        }

        def usagePools = getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'usagePoolsTemplate', model: [ usagePools: usagePools, selected: selected ]
        } else {
           [ usagePools: usagePools, selected: selected ]
        }
    }

    def findPools (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def pools = getList(params)

        try {
            def jsonData = getAsJsonData(pools, params)

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
        def usagePool = UsagePoolDTO.get(params.int('id'))
        securityValidator.validateCompany(usagePool?.entity?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, usagePool.id, usagePool.getDescription(session['language_id'], 'name'))

        render template: 'show', model: [ selected: usagePool ]
    }

    def retrieveChildCompanies() {
        CompanyDTO companyDTO = CompanyDTO.get(session['company_id'])
		return CompanyDTO.findAllByParent(companyDTO)
	}

    def retrieveCompanies() {
		def companies = retrieveChildCompanies()
		companies.add(CompanyDTO.get(session['company_id']))

		return companies
	}

	def retrieveCompaniesIds() {
		def ids = new ArrayList<Integer>();
		for(CompanyDTO dto : retrieveCompanies()){
			ids.add(dto.getId())
		}
		return ids
	}

    def categories (){
        def categories = getProductCategories(true, null)
        render template: 'categories', model: [ categories: categories ]
    }

    def getProductCategories(paged, excludeCategoryId) {
        if (paged) {
            params.max = params?.max?.toInteger() ?: pagination.max
            params.offset = params?.offset?.toInteger() ?: pagination.offset
        }

        List result = ItemTypeDTO.createCriteria().list(
            max: paged ? params.max : null,
            offset: paged ? params.offset : null
        ) {
		    and {
                eq('internal', false)
				or {
					createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
                    'in'('entities.id', retrieveCompaniesIds())
                    //list all global items types which belongs to the root company of current company's hierarchy
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
                }
            }
            order('description', 'asc')
        }

		return result.unique()
    }

    def products (){
        def products = getProducts()
        render template: 'products', model: [ products: products ]
    }

    def getProducts() {

        List result = ItemDTO.createCriteria().list() {

            and {
            	or {
					createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
                    'in'('entities.id', retrieveCompaniesIds())
                    //list all global items which belongs to the root company of current company's hierarchy
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
				}
            }
            and {
            	eq('deleted', 0)
            	isEmpty('plans')
            }
            order('id', 'desc')
        }

		return result.unique()
    }

	def retrievConsumptionActions() {
		return ComsumptionActionDTO.executeQuery("select con.actionName from ComsumptionActionDTO con")
	}

    def edit (){

    	def usagePool

        try {
            usagePool = params.id ? webServicesSession.getUsagePoolWS(params.int('id')) : null
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'usagePool.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'usagePool', action: 'list'
            return
        }

        if(usagePool) {
            securityValidator.validateCompany(usagePool.entityId, Validator.Type.EDIT)
        }

        def categories = getProductCategories(false, null)
        def products = getProducts()
        def cyclePeriods = new String[3]
        cyclePeriods[0] = Constants.USAGE_POOL_CYCLE_PERIOD_DAYS
        cyclePeriods[1] = Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS
		cyclePeriods[2] = Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS

		def resetValues = UsagePoolResetValueEnum.getResetValues()
		def consuptionActions = retrievConsumptionActions()

        [ usagePool: usagePool, categories: categories, products: products, cyclePeriods: cyclePeriods, resetValues: resetValues,
                consuptionActions: consuptionActions, notifications: NotificationMessageTypeDTO.findAll()]
    }

    @RequiresValidFormToken
	def save (){

        UsagePoolWS ws= new UsagePoolWS()
        bindData(ws, params, 'usagePool')
		UsagePoolHelper.bindUsagePool(ws, params)
        def categories = getProductCategories(false, null)
        def products = getProducts()
        def cyclePeriods = new String[3]
        cyclePeriods[0] = Constants.USAGE_POOL_CYCLE_PERIOD_DAYS
        cyclePeriods[1] = Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS
		cyclePeriods[2] = Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS

		def resetValues = UsagePoolResetValueEnum.getResetValues()
		def consuptionActions = retrievConsumptionActions()

        log.debug "params: " + params
		log.debug "ws: " + ws
		if(params.names){
			for (String name : params.names) {
				InternationalDescriptionWS nameWs =
					new InternationalDescriptionWS(session['language_id'] as Integer, name)
		        log.debug "name: " + nameWs
				ws.names.add nameWs
			}
		}
		ws.setEntityId(session['company_id'].toInteger())
        log.debug "ws: " + ws

		try {
			if (params.isNew=="true")
            {
             	Integer retVal = webServicesSession.createUsagePool(ws);
                flash.message= 'config.usagePool.created'
                flash.args = [ retVal ]
                params.id = retVal
            } else{
                def oldUsagePool = UsagePoolDTO.get(ws.id)
                securityValidator.validateCompany(oldUsagePool?.entity?.id, Validator.Type.EDIT)

            	 webServicesSession.updateUsagePool(ws)
                flash.message= 'config.usagePool.updated'
                flash.args = [ ws.id ]
                params.id = ws.id
            }
		} catch (SessionInternalError e){
			viewUtils.resolveException(flash, session.locale, e);
            render view: 'edit', model: [ usagePool: ws, categories: categories, products: products, cyclePeriods: cyclePeriods, resetValues: resetValues, consuptionActions: consuptionActions,
                    mediumTypes: NotificationMediumType.values(), notifications: NotificationMessageTypeDTO.findAll()]
            return
		} catch (Exception e) {
			log.error e.getMessage()
			flash.error = 'config.usagePool.saving.error'
		}
		redirect (action: 'list',  params: [ id: params.id ])
	}

	def delete (){
		log.debug 'delete called on ' + params.id
        if (params.id) {
            def usagePool = UsagePoolDTO.get(params.int('id'))
            securityValidator.validateCompany(usagePool?.entity?.id, Validator.Type.EDIT)

            if (usagePool) {
                try {
                    boolean retVal= webServicesSession.deleteUsagePool(params.id?.toInteger());
                    if (retVal) {
                        flash.message= 'usagePool.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'usagePool.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'usagePool.delete.error'
                }
            }
        }

        // render the partial user list
        params.id = null
		redirect (action: 'list')

	}

	def addAttribute (){
		UsagePoolWS ws= new UsagePoolWS()
		bindData(ws, params, 'usagePool')
		def usagePool = UsagePoolHelper.bindUsagePool(ws, params)
		int newIndex = params.int('attributeIndex')
		def attribute = message(code: 'usagePool.new.attribute.key', args: [newIndex])
		while (usagePool.attributes.containsKey(attribute)) {
			newIndex++
			attribute = message(code: 'usagePool.new.attribute.key', args: [newIndex])
		}
		usagePool.attributes.put(attribute, '')

		def consuptionActions = retrievConsumptionActions()
		render template: '/usagePool/consumption', model: [usagePool: usagePool, consuptionActions:consuptionActions]
	}

	def removeAttribute (){
		UsagePoolWS ws= new UsagePoolWS()
		bindData(ws, params, 'usagePool')
		def usagePool = UsagePoolHelper.bindUsagePool(ws, params)
		def attributeIndex = params.int('attributeIndex')
        // find the model in the chain, remove the attribute
        def name = params["usagePool.${0}.attribute.${attributeIndex}.name"]
        usagePool.attributes.remove(name)

		def consuptionActions = retrievConsumptionActions()
		render template: '/usagePool/consumption', model: [usagePool: usagePool, consuptionActions:consuptionActions]
	}

    def retrieveNotificationMediumType (){
        Integer messageTypeId = params.notificationId.toInteger()
        render template: 'mediumTypeDropDown',
                model : [mediumTypes: findMediumTypesForNotificationMessageTypeDtoId(messageTypeId, session['language_id'], webServicesSession.getCallerCompanyId()),
                        actionIndex: params.actionIndex]
    }

    def static findMediumTypesForNotificationMessageTypeDtoId(Integer messageTypeId, languageId, entityId) {
        NotificationMessageTypeDTO typeDto = NotificationMessageTypeDTO.findById(messageTypeId)
        NotificationMessageDTO dto = null
        for (NotificationMessageDTO messageDTO : typeDto.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId()?.equals(entityId)
                    && messageDTO.getLanguage().getId().equals(languageId)) {
                dto = messageDTO;
                break;
            }
        }
        return dto == null ? NotificationMediumType.values() : dto.mediumTypes;
    }

    def addAction (){
        UsagePoolWS ws= new UsagePoolWS()
        bindData(ws, params, 'usagePool')
        def usagePool = UsagePoolHelper.bindUsagePool(ws, params)
        def consuptionActions = retrievConsumptionActions()
        render template: '/usagePool/consumption', model: [usagePool: usagePool, consuptionActions:consuptionActions,
                mediumTypes: NotificationMediumType.values(), notifications: NotificationMessageTypeDTO.findAll()]
    }

    def removeAction (){
        UsagePoolWS ws= new UsagePoolWS()
        bindData(ws, params, 'usagePool')
        def usagePool = UsagePoolHelper.bindUsagePool(ws, params)
        def actionIndex = params.int('actionIndex')
        // find the model in the chain, remove the attribute
        def actionMap = params.usagePool.consumptionActions["" + actionIndex];
        if (actionMap.mediumType == null || actionMap.mediumType == "") {
            actionMap.remove("mediumType")
        }

        def actionToRemoveFromParams = new UsagePoolConsumptionActionWS(actionMap);
        def actionToRemove;
        usagePool.consumptionActions.each({ action ->
            if (action?.percentage == actionToRemoveFromParams?.percentage &&
                    action?.mediumType == actionToRemoveFromParams?.mediumType &&
                    action?.notificationId == actionToRemoveFromParams?.notificationId &&
                    action?.productId == actionToRemoveFromParams?.productId) {
                actionToRemove = action;
            }
        })
        usagePool.consumptionActions.remove(actionToRemove);

        def consuptionActions = retrievConsumptionActions()
        render template: '/usagePool/consumption', model: [usagePool: usagePool, consuptionActions:consuptionActions,
                mediumTypes: NotificationMediumType.values(), notifications: NotificationMessageTypeDTO.findAll()]
    }
}
