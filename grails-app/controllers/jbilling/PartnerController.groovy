/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.notification.NotificationBL
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.partner.PartnerBL
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType
import com.sapienter.jbilling.server.user.partner.PartnerCommissionValueWS
import com.sapienter.jbilling.server.user.partner.validator.PartnerCommissionsValidator
import com.sapienter.jbilling.server.user.partner.db.CommissionDAS
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionDAS
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.partner.PartnerWS
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.SpringSecurityUtils
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.EntityType


@Secured(["MENU_901"])
class PartnerController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    static final viewColumnsToFields =
            ['userid': 'id',
             'username': 'baseUser.userName',
             'company': 'company.description',
             'status': 'baseUser.userStatus.id']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService
    SecurityValidator securityValidator


    def index () {
        list()
    }

    def getList(filters, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // #7043 - Agents && Commissions - A logged in Partner should see only his children and himself.
        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
        def partnerIds = []
        if (loggedInUser.getPartner() != null) {
            partnerIds << loggedInUser.getPartner().getUser().getId()
            if (loggedInUser.getPartner().getChildren()) {
                partnerIds += loggedInUser.getPartner().getChildren().user.id
            }
        }

        def statuses = UserStatusDTO.findAll()

        return PartnerDTO.createCriteria().list(
            max:    params.max,
            offset: params.offset
        ) {
            and {
                createAlias('baseUser', 'baseUser')
                createAlias("baseUser.contact", "contact")
                createAlias("baseUser.company","company")

                filters.each { filter ->
                    if (filter.value || filter.field == 'deleted') {
                        if (filter.constraintType == FilterConstraint.STATUS) {
                            eq("baseUser.userStatus", statuses.find { it.id == filter.integerValue })
                        }else if(filter.field == 'userName'){
                            eq("baseUser.userName", filter.value)
                        }else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }
                eq('baseUser.company', retrieveCompany())
				
                if(partnerIds) {
                    'in'('baseUser.id', partnerIds)
                }
                if(params.userid) {
                    eq('id', params.int('userid'))
                }
                if(params.company) {
                    addToCriteria(Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.username) {
                    or{
                        eq("baseUser.userName", params.username)
                        addToCriteria(Restrictions.ilike("contact.firstName", params.username, MatchMode.ANYWHERE))
                        addToCriteria(Restrictions.ilike("contact.lastName", params.username, MatchMode.ANYWHERE))
                    }
                }
            }

            // apply sorting
            SortableCriteria.buildSortNoAlias(params, delegate)
        }
    }

    def list () {
        def filters = filterService.getFilters(FilterType.PARTNER, params)

        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        if (selected && SpringSecurityUtils.ifNotGranted("AGENT_104")) {
            selected = null
        }
        if (selected) {
            securityCheck(selected)
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, null)

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && !selected && SpringSecurityUtils.ifAnyGranted("AGENT_104")) {
            flash.error = 'partner.not.found'
            flash.args = [params.id]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.client.util.Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'partnersTemplate', model: [loggedInUser: loggedInUser, filters:filters]
            }else {
                render view: 'list', model: [loggedInUser: loggedInUser, filters:filters]
            }
            return
        }

        def partners = getList(filters, params)
        def contact = selected ? ContactDTO.findByUserId(selected?.baseUser?.id) : null

        if (selected?.baseUser?.id) {
            def userBl = new UserBL(selected.baseUser.id)
            selected.baseUser.accountLocked = userBl.isAccountLocked()
            selected.baseUser.accountExpired = userBl.validateAccountExpired(selected.baseUser.accountDisabledDate)
        }

        if (params.applyFilter || params.partial) {
            render template: 'partnersTemplate', model: [ partners: partners, selected: selected, contact: contact, filters:filters ]
            return 
        } 
        render view: 'list', model: [ partners: partners, selected: selected, contact: contact, filters:filters, loggedInUser: loggedInUser]
    }

    def getListWithSelected(selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getList([idFilter], params)
    }

    def findAgents (){
        //This will enable the filters to work properly
        if (params.username){
            params.firstName = params.username
            params.lastName = params.username
        }

        def filters = filterService.getFilters(FilterType.PARTNER, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0

        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        def partners = selected ? getListWithSelected(selected) : getList(filters, params)

        try {
            render getAgentsJsonData(partners, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    private def Object getAgentsJsonData(partners, GrailsParameterMap params) {
        def jsonCells = partners
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(partners.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: partners.totalCount, total: numberOfPages]

        jsonData
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    def csv (){
        def filters = filterService.getFilters(FilterType.PARTNER, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        def partners = selected ? getListWithSelected(selected) : getList(filters, params)

        renderCsvForPartners(partners)
    }

    def renderCsvForPartners(partners) {
        if (partners.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "partners.csv")
            Exporter<PartnerDTO> exporter = CsvExporter.createExporter(PartnerDTO.class);
            render text: exporter.export(partners), contentType: "text/csv"
        }
    }

    def downloadPdf () {
        Integer runId = params.int('id')

        try {
            def commissionRun = new CommissionProcessRunDAS().find(runId)
            Map<String, Object> reportParams = new HashMap<>()
            reportParams.put("run_date", commissionRun.runDate)
            reportParams.put("end_date", commissionRun.periodEnd)
            reportParams.put("run_id", runId)
            byte[] pdfBytes = NotificationBL.generateDesignReport("commission_run", reportParams)
            DownloadHelper.sendFile(response, "commission-run-${runId}.pdf", "application/pdf", pdfBytes)

        }catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            redirect action: 'showCommissions', params: [id: runId]
        } catch (Exception e) {
            log.error("Exception generating report.", e)
            flash.error = 'commissions.prompt.failure.downloadPdf'
            redirect action: 'showCommissions', params: [id: runId]
        }
    }

    @Secured(["AGENT_104"])
    def show () {
        def partner = PartnerDTO.get(params.int('id'))
        securityCheck(partner)
        def contact = partner ? ContactDTO.findByUserId(partner?.baseUser.id) : null

        //Check if account is locked so that it can be shown on UI appropriately
        def user = partner?.baseUser.id ? webServicesSession.getUserWS(partner?.baseUser.id) : new UserWS()
        if(null != user?.id){
            partner.baseUser.accountLocked = user.isAccountLocked
            partner.baseUser.accountExpired = user.accountExpired
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, partner.id, UserHelper.getDisplayName(partner.baseUser, contact))

        render template: 'show', model: [ selected: partner, contact: contact ]
    }

    /**
     * Shows the commissions runs.
     */
    def showCommissionRuns (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def partner = PartnerDTO.get(params.int('id'))
        if (partner) {
            securityCheck(partner)
        } else {
            securityCheck(getList(null, params)?.first())
        }
        def commissionRuns = getCommissionRuns(partner)

        [ commissionRuns: commissionRuns ]
    }

    /**
     * Returns a list of runCommissions that can be paginated
     */
    private def getCommissionRuns (PartnerDTO partner) {
        params.max = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return CommissionProcessRunDTO.createCriteria()
                .list(max:params.max, offset:params.offset) {
            eq("entity", this.retrieveCompany())
            if(partner) {
                createAlias('commissions', 'commissions')
                and{
                    eq('commissions.partner', partner)
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Shows the commissions for a single commission run.
     */
    def showCommissions (){
        Integer processRunId = params.int('id')

        def commissionRun = new CommissionProcessRunDAS().find(processRunId)

        securityValidator.validateCompany(commissionRun?.entity?.id, Validator.Type.VIEW)

        def commissions = getCommissions(commissionRun)

        [ commissionRun : commissionRun, commissions: commissions]
    }

    /**
    * Returns a list of commissions that can be paginated
    * @param commissionProcessRun
    */
    private def getCommissions(CommissionProcessRunDTO commissionProcessRun) {
        params.max = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        Integer agentId = params?.int('agentId')
        return CommissionDTO.createCriteria().list(
            max:    params.max,
            offset: params.offset
            ) {
            createAlias("commissionProcessRun", "_commissionProcessRun")
            createAlias("_commissionProcessRun.entity", "_entity")
                and{
                    eq('commissionProcessRun', commissionProcessRun)
                    eq('_entity.id', session['company_id'])
                    if (agentId) {
                        createAlias("partner", "_partner")
                        and {
                            eq('_partner.id', agentId)
                        }
                    }
                }
                // apply sorting
                SortableCriteria.sort(params, delegate)
            }
    }

    /**
     * Shows the detailed commissions for a partner.
     */
    def showCommissionDetail (){
        Integer commissionId = params.int('commissionId')

        def commission = new CommissionDAS().find(commissionId)

        securityCheck(commission?.partner)

        def invoiceCommissions = new PartnerCommissionDAS().findByCommission(commission)

        [ commission : commission, invoiceCommissions: invoiceCommissions]
    }

    /**
     * Returns the list of commissions as a csv file
     */
    def commissionCsv (){
        Integer processRunId = params.int('id')

        def commissionRun = new CommissionProcessRunDAS().find(processRunId)

        securityValidator.validateCompany(commissionRun?.entity?.id, Validator.Type.VIEW)

        def commissions = new CommissionDAS().findAllByProcessRun(commissionRun, session['company_id'])

        if (commissions.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'showCommissions'
        } else {
            DownloadHelper.setResponseHeader(response, "commissions.csv")
            Exporter<CommissionDTO> exporter = CsvExporter.createExporter(CommissionDTO.class);
            render text: exporter.export(commissions), contentType: "text/csv"
        }
    }

    @Secured(["hasAnyRole('AGENT_101', 'AGENT_102')"])
    def edit () {

        def user
        def partner
        def contacts
        def parentId
        def companyInfoTypes
        def ssoActive
        def defaultIdp
        try {
            partner= params.id ? webServicesSession.getPartner(params.int('id')) : new PartnerWS()

            if (params.id) {
                PartnerDTO partnerDTO = new PartnerBL(params.int('id'))?.getDTO()
                securityCheck(partnerDTO)
                if(SpringSecurityUtils.ifNotGranted('AGENT_102')) {
                    def message = """Unauthorized agent edit by caller (id ${session['user_id']})""";
                    log.warn(message)
                    throw new SecurityException(message);
                }
            } else {
                if(SpringSecurityUtils.ifNotGranted('AGENT_101')) {
                    def message = """Unauthorized agent create by caller (id ${session['user_id']})""";
                    log.warn(message)
                    throw new SecurityException(message);
                }
            }

            user= (params.id &&  partner) ? webServicesSession.getUserWS(partner?.userId) : new UserWS()
            if(user.id>0){
                MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
                for (int i = 0; i < metaFieldValueWS.length; i++) {
                    if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_IDP_ID_AGENT)) {
                        defaultIdp = metaFieldValueWS[i].getIntegerValue()
                        break;
                    }
                }
            }
            contacts = params.id ? webServicesSession.getUserContactsWS(user.userId) : null

            breadcrumbService.addBreadcrumb(controllerName, 'edit', null, partner.id,
            	UserHelper.getDisplayName(user, contacts && contacts.length > 0 ? contacts[0] : null))

            // If a parentId comes in the params is because the Add Sub-Agent button was clicked.
            parentId = params.parentId ?: null
            companyInfoTypes = retrieveCompany().getCompanyInformationTypes()
            ssoActive = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_SSO) as int
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
            redirect action: 'list', params:params
            return
        }

        [
                partner: partner,
                user: user,
                contacts: contacts?.flatten(),
                company: retrieveCompany(),
                currencies: retrieveCurrencies(),
                availableFields: retrieveAvailableMetaFields(),
                parentId: parentId,
                loggedInUser: UserDTO.get(springSecurityService.principal.id),
                companyInfoTypes: companyInfoTypes,
                ssoActive: ssoActive,
                defaultIdp:defaultIdp
        ]
    }

    /**
     * Validate and Save the Partner User
     */
    @Secured(["hasAnyRole('AGENT_101', 'AGENT_102')"])
    @RequiresValidFormToken
    def save () {
        def partner = new PartnerWS()
        def user = new UserWS()
        def company
        def companyInfoTypes
        def ssoActive

        // Bind partner's data
        bindData(partner, params)

        if(partner.id) {
            if(SpringSecurityUtils.ifNotGranted('AGENT_102')) {
                def message = """Unauthorized agent edit by caller '${loggedInUser.getUserName()}' (id ${loggedInUser.getId()})""";
                log.warn(message)
                throw new SecurityException(message);
            }
        } else {
            if(SpringSecurityUtils.ifNotGranted('AGENT_101')) {
                def message = """Unauthorized agent create by caller '${loggedInUser.getUserName()}' (id ${loggedInUser.getId()})""";
                log.warn(message)
                throw new SecurityException(message);
            }
        }
        try {
            SessionInternalError error = null
            def commissionValues = []
            if(PartnerCommissionType.CUSTOMER.name().equals(partner.getCommissionType())) {
                BigDecimal value = null
                try {
                    value = new BigDecimal(params.commissionFee)
                } catch(Exception e) {
                    error = new SessionInternalError(e, ['partner.error.commission.value.rate.not.decimal'].toArray(new String[0]))
                    value = BigDecimal.ZERO
                }
                commissionValues += new PartnerCommissionValueWS(0, value)
            } else if(partner.getCommissionType()) {
                    params.partner?.commission?.value?.each { it ->
                    if(it.key.isNumber()) {
                        //ignore empty rows
                        if(!it.value['days'] && !it.value['rate']) {
                            return;
                        }
                        Integer days = null
                        try {
                            days = it.value['days'] as Integer
                        } catch(Exception e) {
                            if(!error) {
                                error = new SessionInternalError(e, ['partner.error.commission.value.days.not.integer'].toArray(new String[0]))
                            }
                            days = 0
                        }
                        BigDecimal value = null
                        try {
                            value = new BigDecimal(it.value['rate'])
                        } catch(Exception e) {
                            if(!error) {
                                error = new SessionInternalError(e, ['partner.error.commission.value.rate.not.decimal'].toArray(new String[0]))
                            }
                            value = BigDecimal.ZERO
                        }
                        commissionValues += new PartnerCommissionValueWS(days, value)
                    }
                }
            }
            partner.commissionValues = commissionValues
            if(error) throw error;
        } catch (SessionInternalError e) {
            flash.error = g.message(code: e.errorMessages[0])
        }

        RoleDTO partnerRole = RoleDTO.findByRoleTypeIdAndCompany(Constants.TYPE_PARTNER, CompanyDTO.findById(session['company_id']))
        user.setMainRoleId(partnerRole.id)

        // Retrieve useful information
        UserHelper.bindUser(user, params)

        def availableMetaFields = retrieveAvailableMetaFields()
        UserHelper.bindMetaFields(user, availableMetaFields, params)

        log.debug("bound fields: ${user.getMetaFields()}")

        def contacts = []
        UserHelper.bindContacts(user, contacts, retrieveCompany(), params)

        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null
        
        UserHelper.bindPassword(user, oldUser, params, flash)

        //Bind commission exceptions.
        UserHelper.bindPartnerCommissionExceptions(partner, params, g.message(code: 'datepicker.format').toString())
        UserHelper.bindPartnerReferralCommissions(partner, params, g.message(code: 'datepicker.format').toString())

        // Validate the Partner Commission data.
        PartnerCommissionsValidator commissionsValidator = new PartnerCommissionsValidator()
        String validationResult = commissionsValidator.validate(partner)

        if (validationResult) {
            flash.error = validationResult
        }

        if( partner.getId()!=null && (partner.getId()==partner.getParentId()) ) {
            flash.error = g.message(code: 'partner.error.parentOfItsOwn')
        }
        else {
            try {
                PartnerWS parent = (params.parentId && params.parentId.isNumber()) ? webServicesSession.getPartner(params.int("parentId")) : null
                if (parent && parent.parentId) {
                    if (partner.getId()==parent.getId()) {
                        flash.error = g.message(code: 'partner.error.parentOfItsOwn')
                    }
                    else {
                        flash.error = g.message(code: 'partner.error.parentIsChild')
                    }
                }
            } catch(Exception e) {
                flash.error = g.message(code: 'partner.error.parentDoesNotExist')
            }
        }

        MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
        boolean ssoEnabled = false;
        for (int i = 0; i < metaFieldValueWS.length; i++) {
            if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_ENABLED_AGENT)) {
                ssoEnabled = metaFieldValueWS[i].getBooleanValue()
                break;
            }
        }
        if(ssoEnabled){
            user.setCreateCredentials(false);
        }
        if(!params['idpConfigurationIds']?.toString()?.trim()?.isEmpty() && ssoEnabled) {
            for (int i = 0; i < metaFieldValueWS.length; i++) {
                if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_IDP_ID_AGENT)) {
                    int metaValue = params['idpConfigurationIds'] as Integer
                    metaFieldValueWS[i].setIntegerValue(metaValue)
                    break;
                }
            }
            user.setMetaFields(metaFieldValueWS)
        }
        company = retrieveCompany()
        companyInfoTypes = company.getCompanyInformationTypes();
        ssoActive = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_SSO) as int
        if ((params['idpConfigurationIds']?.toString()?.trim()?.isEmpty() || companyInfoTypes.size() == 0) && ssoEnabled) {
            log.error("No default Idp is configured for company")
            flash.error = message(code: 'default.idp.error')
        }

        if (flash.error) {
            render view: 'edit', model: [
                    partner: partner,
                    user: user,
                    contacts: contacts,
                    company: retrieveCompany(),
                    currencies: retrieveCurrencies(),
                    availableFields: availableMetaFields,
                    companyInfoTypes: companyInfoTypes,
                    ssoActive: ssoActive
            ]
            return
        }

        try {
            // save or update
            if (!oldUser) {
                if (SpringSecurityUtils.ifAllGranted("AGENT_101")) {
                    log.debug("creating partner ${user}")

                    partner.id = webServicesSession.createPartner(user, partner)

                    flash.message = 'partner.created'
                    flash.args = [partner.id]

                } else {
                    render view: '/login/denied'
                    return
                }

            } else {
                if (SpringSecurityUtils.ifAllGranted("AGENT_102")) {
                    log.debug("saving changes to partner ${user.userId} & ${user.customerId}")

                    partner.setUserId(user.getUserId())

                    PartnerDTO partnerDTO = new PartnerBL(partner?.id)?.getDTO()
                    securityCheck(partnerDTO, user.getUserId())

                    webServicesSession.updatePartner(user, partner)

                    flash.message = 'partner.updated'
                    flash.args = [partner.id]

                } else {
                    render view: '/login/denied'
                    return
                }
            }

            // save secondary contacts
            if (user.userId) {
                contacts.each {
                    webServicesSession.updateUserContact(user.userId, it);
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [ partner: partner, user: user, contacts: contacts, company: retrieveCompany(), currencies: retrieveCurrencies(), availableFields: availableMetaFields, companyInfoTypes: companyInfoTypes, ssoActive: ssoActive ]
            return
        }

        chain action: 'list', params: [ id: partner.id ]
    }

    @Secured(["AGENT_103"])
    def delete () {

        if (params.id) {
            PartnerDTO partnerDTO = new PartnerBL(params.int('id'))?.getDTO()
            securityCheck(partnerDTO)
        }

        try {
            if (params.id) {
                webServicesSession.deletePartner(params.int('id'))
                log.debug("Deleted partner ${params.id}.")
            }
            flash.message = 'partner.deleted'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            log.error("Could not delete agent", e)
            viewUtils.resolveException(flash, session.locale, e)
        }
        // render the partial user list
        params.applyFilter = true
        params.id = null
        params.partial = true
        redirect action: 'list'
    }

    def userCodeList () {
        render(view: 'userCodeListView', model: modelAndView.model)
    }

    def retrieveCurrencies() {
        def currencies = webServicesSession.getCurrencies()
        return currencies.findAll { it.inUse }
    }
    
    def retrieveCompany() {
        CompanyDTO.get(session['company_id'])
    }

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.AGENT);
    }

    private void securityCheck(PartnerDTO partner){
        securityCheck(partner, null)
    }

    /**
     * Checks that the partner belongs to the  same company as the current user.
     * If the current user is a partner, check that the ids match or if the id is from one of his children.
     */
    private void securityCheck(PartnerDTO partner, Integer userId){
        boolean throwException = false

        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

        if(!loggedInUser?.company?.equals(partner?.baseUser?.company)){
            throwException = true
        }

        def partnerIds = []
        if (loggedInUser.getPartner() != null) {
            partnerIds << loggedInUser.getPartner().getUser().getId()
            if (loggedInUser.getPartner().getChildren()) {
                partnerIds += loggedInUser.getPartner().getChildren().user.id
            }
        }

        if(partnerIds && partner?.baseUser?.id && !partnerIds.contains(partner?.baseUser?.id)){
            throwException = true
        }

        if(userId && partner.baseUser.userId != userId) {
            throwException = true
        }

        if(throwException){
            def message = """Unauthorized access by caller '${loggedInUser.getUserName()}' (id ${loggedInUser.getId()})""";
            log.warn(message)
            throw new SecurityException(message);
        }
    }

}
