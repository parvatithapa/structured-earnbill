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

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.SystemProperties
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.validation.EmailValidationRuleModel
import com.sapienter.jbilling.server.process.AgeingWS
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.user.ContactBL
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDTO
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationBL
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDAS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.CurrencyWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.LogoType;
import com.sapienter.jbilling.server.util.PreferenceTypeWS
import com.sapienter.jbilling.server.util.PreferenceWS
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import java.util.regex.Pattern

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.server.order.OrderChangeStatusWS
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.order.ApplyToOrder
import com.sapienter.jbilling.server.util.db.LanguageDTO
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.joda.time.format.DateTimeFormat
import org.springframework.transaction.annotation.Transactional

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;

import java.lang.reflect.Array;

/**
 * ConfigurationController
 *
 * @author Brian Cowdery
 * @since 03-Jan-2011
 */
@Secured(["MENU_99"])
class ConfigController {

    static final viewColumnsToFields =
            ['preferenceId': 'id']
	static scope = "prototype"

    def breadcrumbService
    def companyService
    IWebServicesSessionBean webServicesSession
    def viewUtils
    def userSession
	def displayCancelledTab = false
	def disableCancelledTab = true

    private final String LOGO_BASE_PATH = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "logos/"
    private final String INVOICE_DISPLAY_PREFIX = "entity-"
    private final String NAVIGATION_BAR_PREFIX = "nav-entity-"
    private final String FAVICON_PREFIX = "fav-entity-"
    private final String NOT_FOUND_IMAGE = ServletContextHolder.servletContext.getRealPath("/images/imageNotFound.png")

    /*
        Show/edit all preferences
     */

    def index () {
        def preferenceTypes = PreferenceTypeDTO.list()

        // show preference if given id
        def preferenceId = params.int('id')
        def selected = preferenceId ? preferenceTypes.find { it.id == preferenceId } : null

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        render view: 'index', model: [preferenceTypes: preferenceTypes.sort { it?.id }, selected: selected]
    }

    def findPreferences (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def prefs = getPreferences(params)

        try {
            def jsonData = getPreferencesJsonData(prefs, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Preferences to JSon
     */
    private def Object getPreferencesJsonData(prefs, GrailsParameterMap params) {
        def jsonCells = prefs
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    private def getPreferences(GrailsParameterMap params) {
        def languageId = session['language_id']
        PreferenceTypeDTO.createCriteria().list(max: params.max, offset: params.offset){
            if (params.preferenceId){
                def searchParam = params.preferenceId
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
                                             (select b.id from jbilling_table b where b.name =
                                              ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_PREFERENCE_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def show () {
        def selected = PreferenceTypeDTO.get(params.int('id'))

        render template: 'show', model: [selected: selected]
    }

    @RequiresValidFormToken
    def save () {
        def type = new PreferenceTypeWS()
        bindData(type, params, 'type')

        def preference = new PreferenceWS()
        bindData(preference, params, 'preference')
        preference.preferenceType = type

        try {
            webServicesSession.updatePreference(preference)

            flash.message = 'preference.updated'
            flash.args = [type.id as String]

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        chain action: 'index', params: [id: type.id]
    }

    /*
       Ageing configuration
    */


    def aging () {
		def languageId = session['language_id'] as Integer
        log.debug "Language ID = ${languageId}"
        AgeingWS[] array = webServicesSession.getAgeingConfigurationWithCollectionType(languageId, CollectionType.REGULAR)
		AgeingWS[] cancelledArray = webServicesSession.getAgeingConfigurationWithCollectionType(languageId, CollectionType.CANCELLATION_INVOICE)
		PluggableTaskWS[] pluggableTasks = webServicesSession.getPluginsWS(session['company_id'] as Integer, "com.sapienter.jbilling.server.process.task.CancellationInvoiceAgeingTask")
		if(ArrayUtils.isNotEmpty(pluggableTasks)){
			disableCancelledTab = false
		}
		if (actionName == 'aging') {
            breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        }

        render view: 'aging', model: [ageingSteps: array, cancelledAgeingSteps : cancelledArray, displayCancelledTab : displayCancelledTab, disableCancelledTab : disableCancelledTab ]
    }

    @RequiresValidFormToken
    def saveAging () {

        def cnt = params.recCnt.toInteger()
        log.debug "Records Count: ${cnt}"
		
		def collectionType = CollectionType.valueOf(params.collectionType)
		log.debug "saveAging-CollectionType: ${collectionType}"

        AgeingWS[] array = new AgeingWS[cnt]
        for (int i = 0; i < cnt; i++) {
            log.debug "${params['obj[' + i + '].statusId']}"
            AgeingWS ws = new AgeingWS()
            bindData(ws, params["obj[" + i + "]"])
			ws.setCollectionType(collectionType)
            array[i] = ws
        }

        for (AgeingWS dto : array) {
            log.debug "Printing: ${dto.toString()}"
        }
        try {
            webServicesSession.saveAgeingConfigurationWithCollectionType(array, session['language_id'] as Integer,collectionType)
            flash.message = 'config.ageing.updated'
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.error.saving.ageing'
        }
		if(CollectionType.CANCELLATION_INVOICE.equals(collectionType)) {
			displayCancelledTab = true
		}

        aging()
    }

    def addAgeingStep () {
        def cnt = params.recCnt.toInteger()
		def collectionType = CollectionType.valueOf(params.collectionType)
		AgeingWS[] array = new AgeingWS[cnt+1]
		def stepIndex = params.int('stepIndex')
		addAgeingStepHelper(array, stepIndex, cnt, collectionType);
		render template: '/config/aging/steps', model: [ageingSteps: array]
    }
	
	def addCancelAgeingStep () {
		def cnt = params.recCnt.toInteger()
		def collectionType = CollectionType.valueOf(params.collectionType)
		AgeingWS[] array = new AgeingWS[cnt+1]
		def stepIndex = params.int('cancelStepIndex')
		addAgeingStepHelper(array, stepIndex, cnt, collectionType);
		render template: '/config/aging/cancelledUserSteps', model: [cancelledAgeingSteps: array]
	}
	
	private void addAgeingStepHelper(AgeingWS[] array, int stepIndex, int cnt, CollectionType collectionType){
		for (int i = 0; i < cnt; i++) {
			log.debug "${params['obj[' + i + '].statusId']}"
			AgeingWS ws = new AgeingWS()
			bindData(ws, params["obj[" + i + "]"])
			ws.setCollectionType(collectionType)
			array[i] = ws
		}

		AgeingWS ws = new AgeingWS()
		bindData(ws, params["obj[" + stepIndex + "]"])
		ws.setCollectionType(collectionType)
		array[stepIndex] = ws
	}

    def removeAgeingStep () {
        def cnt = params.recCnt.toInteger()
		def collectionType = CollectionType.valueOf(params.collectionType)
        def stepIndex = params.int('stepIndex')

        AgeingWS[] array = new AgeingWS[cnt-1]
        int j = 0;
        for (int i = 0; i < cnt; i++) {
            if (i == stepIndex) { continue ;}
            log.debug "${params['obj[' + i + '].statusId']}"
            AgeingWS ws = new AgeingWS()
            bindData(ws, params["obj[" + i + "]"])
			ws.setCollectionType(collectionType)
            // indexes are different
            array[j] = ws
            j++;
        }
		render template: '/config/aging/steps', model: [ageingSteps: array]
    }
	
	def removeCancelAgeingStep () {
		def cnt = params.recCnt.toInteger()
		def collectionType =  CollectionType.valueOf(params.collectionType)
		def stepIndex = params.int('cancelStepIndex')
		
		AgeingWS[] array = new AgeingWS[cnt-1]
		int j = 0;
		for (int i = 0; i < cnt; i++) {
			if (i == stepIndex) { continue ;}
			log.debug "${params['obj[' + i + '].statusId']}"
			AgeingWS ws = new AgeingWS()
			bindData(ws, params["obj[" + i + "]"])
			ws.setCollectionType(collectionType)
			// indexes are different
			array[j] = ws
			j++;
		}
		render template: '/config/aging/cancelledUserSteps', model: [cancelledAgeingSteps: array]
	}

    @RequiresValidFormToken
    def runCollectionsForDate () {
        def collectionsRunDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.collectionsRunDate)
        try {
            if (webServicesSession.isAgeingProcessRunning()) {
                flash.error = 'config.collections.already.running'
            } else {
                flash.message = 'config.collections.run.triggered'
                flash.args    = [params.collectionsRunDate]

                webServicesSession.triggerCollectionsAsync(collectionsRunDate)
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.collections.error.run'
            flash.args = [params.collectionsRunDate]
        }
        chain action: 'aging'
    }

    /*
        Company configuration
     */

    def company () {
        CompanyWS company = webServicesSession.getCompany()
        def companyDto = new CompanyDAS().find(session['company_id'].intValue())
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        render view: 'company', model: [
                company: company,
                companyInformationTypes: companyDto?.companyInformationTypes?.sort { it.displayOrder },
                availableFields: retrieveAvailableMetaFields(),
                availableTimezones: TimezoneHelper.availableTimezones
        ]
    }

    /*
        IDP Authentication
     */
    def authentication () {
        CompanyWS company = webServicesSession.getCompany()
        def companyDto = CompanyDTO.get(company.getId())
        //search for AITs for the selected account type
        def cits = CompanyInformationTypeDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset,
                sort:   params.sort,
                order:  params.order
        ) {
            eq("company.id", companyDto?.id.toInteger())
        }


        render view: '/authentication/listCIT', model: [
                company: company,
                companyDto: companyDto,
                cits: cits,
                availableFields: retrieveAvailableMetaFields(),
                availableTimezones: TimezoneHelper.availableTimezones
        ]
    }

    @RequiresValidFormToken
    def saveCompany () {
        CompanyWS company = webServicesSession.getCompany()
        def companyDto = new CompanyDAS().find(session['company_id'].intValue())
        try {

            // Contact Type 1 is always Company Contact
            ContactWS contact = new ContactWS()
            bindData(company, params, ['id'])
            company.description = company.description
            bindData(contact, params, ['id'])
            company.setContact(contact)
            List<MetaFieldValueWS> allMetafields = new LinkedList<MetaFieldValueWS>();
            allMetafields.addAll(MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryMetaFields(session['company_id'].intValue()), params))
            allMetafields.addAll(MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryCompanyInfoMetaFields(session['company_id'].intValue()), params))
            company.metaFields = allMetafields
            if(params.uiColor) company.setUiColor(Integer.parseInt(params.uiColor, 16))
            webServicesSession.updateCompany(company)
            session.setAttribute("company_timezone",params.timezone)
            flash.message = 'config.company.save.success'

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch(NumberFormatException nfe){
            flash.error='invalid.value.color.error'
        }catch (Exception e) {
            flash.error = 'config.company.save.error'
        }

        render view: 'company', model: [
                company: company,
                companyInformationTypes: companyDto?.companyInformationTypes?.sort { it.displayOrder },
                availableFields: retrieveAvailableMetaFields(),
                availableTimezones: TimezoneHelper.availableTimezones
        ]
    }

    private List<MetaField> retrieveAvailableCategoryMetaFields(entityId) {
        return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.COMPANY)
    }

    private List<MetaField> retrieveAvailableCategoryCompanyInfoMetaFields(entityId) {
        return MetaFieldBL.getAllAvailableFieldsList(entityId, EntityType.COMPANY_INFO)
    }

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session['company_id'], EntityType.COMPANY);
    }

    @Secured('CONFIGURATION_1911')
    def companies() {
        CompanyWS[] companies = webServicesSession.getCompanies()

        render(view: 'companies', model: [comapnies: companies])
    }

    /**
     * Partner Commission Configuration
     */
    def partnerCommission (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        CompanyDTO entity = new CompanyDAS().find(session['company_id'].intValue())
        CommissionProcessConfigurationDTO configurationDTO = new CommissionProcessConfigurationDAS().findByEntity(entity)
        CommissionProcessConfigurationWS configurationWS

        if (configurationDTO) {
            configurationWS = CommissionProcessConfigurationBL.getCommissionProcessConfigurationWS(configurationDTO)
        }

        render view: 'partnerCommission', model: [configuration: configurationWS]
    }

    /**
     * saves the commission process configuration.
     */
    @RequiresValidFormToken
    def saveCommissionConfig (){
        try {
            CompanyDTO entity = new CompanyDAS().find(session['company_id'].intValue())
            CommissionProcessConfigurationWS configuration = new CommissionProcessConfigurationWS()

            bindData(configuration, params)
            configuration.setEntityId(entity.id)
            webServicesSession.createUpdateCommissionProcessConfiguration(configuration)
            flash.message = g.message(code: 'config.partner.save.success')


        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            flash.error = g.message(code: 'config.partner.save.error')
        }

        chain action: 'partnerCommission'
    }

    /**
     *  Triggers the commission process
     */
    def triggerCommissionProcess (){
        try {
            if (!webServicesSession.isPartnerCommissionRunning()) {
                webServicesSession.calculatePartnerCommissions()
                flash.message = 'prompt.partner.run.success'
            } else {
                flash.error = 'prompt.partner.already.running'
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            flash.error = g.message(code: 'config.partner.run.error')
        }

        chain action: 'partnerCommission'
    }

    /*
        Invoice display configuration
     */

    def invoice () {
        def number = webServicesSession.getPreference(Constants.PREFERENCE_INVOICE_NUMBER)
        def prefix = webServicesSession.getPreference(Constants.PREFERENCE_INVOICE_PREFIX)

        render view: 'invoice', model: [number: number, prefix: prefix, logoPath: invoiceDisplayLogoPath]
    }

    def uiEntityLogo () {
        render view: 'uiEntityLogo'
    }

    @Secured(["isAuthenticated()"])
    def entityLogo () {
        if(params.favicon){
            faviconLogo()
        } else {
            navigationBarLogo()
        }
    }

    def navigationBarLogo() {
        response.outputStream << new File(LogoType.NAVIGATION.getLogo(session['company_id'])).getBytes()
    }

    def faviconLogo() {
        response.outputStream << new File(LogoType.FAVICON.getLogo(session['company_id'])).getBytes()
    }
    
    def invoiceDisplayLogo() {
        File file = new File(LogoType.INVOICE.getLogo(session['company_id']))
        if (file.exists()) {
            response.outputStream << file.getBytes()
        } else {
            response.outputStream << new File(NOT_FOUND_IMAGE).getBytes()
        }
    }

    def notFoundLogo() {
        response.outputStream << new File(NOT_FOUND_IMAGE).getBytes()
    }

    def defaultNavBarLogo () {
        response.outputStream << new File(LogoType.NAVIGATION.getLogo(null)).getBytes()
    }

    def defaultFaviconLogo () {
        response.outputStream << new File(LogoType.FAVICON.getLogo(null)).getBytes()
    }

    def defaultInvoiceLogo () {
        response.outputStream << new File(NOT_FOUND_IMAGE).getBytes()
    }

    @RequiresValidFormToken
    def saveInvoice () {
        def number = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_INVOICE_NUMBER), value: params.number)
        def prefix = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_INVOICE_PREFIX), value: params.prefix)

        try {
            webServicesSession.updatePreferences((PreferenceWS[]) [number, prefix])
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'invoice', model: [number: number, prefix: prefix, logoPath: invoiceDisplayLogoPath]
            return
        }

        // save uploaded file
        def logo = request.getFile('logo');
        if (!logo.empty) {
            if (!LogoType.getExtensions().contains(logo.getContentType())) {
                flash.error = LogoType.INVOICE.getFormatErrorMessage()
                flash.args = [LogoType.getExtensionsAsString()]
            } else if(LogoType.exceedLimitSize(logo.getSize())){
                flash.error = LogoType.INVOICE.getSizeErrorMessage()
            } else {
                removeLogos(LogoType.INVOICE.getFileName(session['company_id']))
                logo.transferTo(new File(generateLogoPath(LOGO_BASE_PATH, INVOICE_DISPLAY_PREFIX, getImageExtension(logo))))
                flash.message = LogoType.INVOICE.getSuccessfullyMessage()
            }
        } else if(params.boolean('removeLogo')) {
            removeLogos(LogoType.INVOICE.getFileName(session['company_id']))
            flash.message = LogoType.INVOICE.getSuccessfullyMessage()
        }

        chain action: 'invoice'
    }

    @RequiresValidFormToken
    def saveEntityLogo () {
        // save uploaded file
        flash.infoMessages = []
        flash.errorMessages = []

        if (params.boolean('logoByDefault')){
            String fileName = LogoType.NAVIGATION.getFileName(session['company_id'])
            if (fileName) {
                removeLogos(fileName)
                flash.infoMessages.add(message(code: LogoType.NAVIGATION.getSuccessfullyMessage()))
            }
        } else {
            saveUILogo(NAVIGATION_BAR_PREFIX, request.getFile('logo'), LogoType.NAVIGATION)
        }

        if (params.boolean('faviconByDefault')) {
            String fileName = LogoType.FAVICON.getFileName(session['company_id'])
            if (StringUtils.isNotEmpty(fileName)) {
                removeLogos(fileName)
                flash.infoMessages.add(message(code: LogoType.FAVICON.getSuccessfullyMessage()))
            }
        } else {
            saveUILogo(FAVICON_PREFIX, request.getFile('favicon'), LogoType.FAVICON)
        }

        chain action: 'uiEntityLogo'
    }

    private void saveUILogo(prefix, logo, LogoType logoType) {
        if (!logo.empty) {
            if (!LogoType.getExtensions().contains(logo.getContentType())) {
                flash.errorMessages.add(message(code: logoType.getFormatErrorMessage(), args: [LogoType.getExtensionsAsString()]))
            } else if (LogoType.exceedLimitSize(logo.getSize())) {
                flash.errorMessages.add(message(code: logoType.getSizeErrorMessage()))
            } else {
                removeLogos(logoType.getFileName(session['company_id']))
                logo.transferTo(new File(generateLogoPath(LOGO_BASE_PATH, prefix, getImageExtension(logo))))
                flash.infoMessages.add(message(code: logoType.getSuccessfullyMessage()))
            }
        }
    }

    def String getInvoiceDisplayLogoPath() {
        def entityLogos = LogoType.INVOICE.getLogo(session['company_id'])
        return !entityLogos.empty ?: NOT_FOUND_IMAGE
    }

    private String generateLogoPath(String path, String prefix, String extension) {
        if (prefix.equals(INVOICE_DISPLAY_PREFIX)) {
            removeLogos(LogoType.INVOICE.getFileName(session['company_id']))
        }

        return "${path}${prefix}${session['company_id']}.${extension}";
    }

    private void removeLogos(String logoPath) {
        File file = new File(logoPath)
        if (file.exists()) {
            file.delete()
        }
    }

    private String getImageExtension(def logo) {
        return logo.getContentType().split("/").last()
    }
    
    /*
       Currencies
    */
    def currency () {
        def startDate = params.startDate ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate() : getLastTimePointDate()
        return generateCurrenciesFormModel(com.sapienter.jbilling.common.Util.truncateDate(startDate))
    }

    @RequiresValidFormToken
    def saveCurrencies () {
        def defaultCurrencyId = params.int('defaultCurrencyId')
        def startDate = params.startDate ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate() : getLastTimePointDate()
        def allowSave = true;

        // build a list of currencies
        def currencies = []
        List<String> messages = new ArrayList<>();

        params.currencies.each { k, v ->
            if (v instanceof Map) {
                def currency = new CurrencyWS()
                bindData(currency, removeBlankParams(v), ['_inUse'])
                currency.defaultCurrency = (currency.id == defaultCurrencyId)
                currency.fromDate = startDate
                if (!currency.rate && currency.inUse) {
                    allowSave = false
                    messages.add(currency.code)
                }

                currencies << currency
            }
        }

        // update all currencies if all currencies has exchange rate
        if (allowSave){
            try {
                webServicesSession.updateCurrencies((CurrencyWS[]) currencies)
                flash.message = 'currencies.updated'
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }else{
            flash.errorDetails = messages
            flash.error = 'currencies.exchange.rate.not.specified'
        }

        chain action: 'currency'
    }

    def deleteCurrency () {
        log.debug 'delete currency called on ' + params.id
        try {
            boolean retVal = webServicesSession.deleteCurrency(params.int('id'));

            if (retVal) {
                flash.message = 'currency.deleted'
                flash.args = [params.code]
                log.debug("Deleted currency ${params.code}.")
            } else {
                flash.error = 'currency.delete.failure'
                flash.args = [params.code]
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'currency.delete.error'
            flash.args = [params.code]
        }

        chain action: 'currency'
    }

    def addDatePoint () {
        def startDate = com.sapienter.jbilling.common.Util.truncateDate(TimezoneHelper.currentDateForTimezone(session['company_timezone']))
        def mdl = generateCurrenciesFormModel(startDate)
        mdl.timePoints.add(startDate)

        render template: 'currency/form', model: mdl
    }

    def editDatePoint () {
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        render template: 'currency/form', model: generateCurrenciesFormModel(startDate)
    }

    @Transactional
    def removeDatePoint () {
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()
        new CurrencyBL().removeExchangeRatesForDate(session['company_id'], startDate)

        render template: 'currency/form', model: generateCurrenciesFormModel(getLastTimePointDate())
    }

    def generateCurrenciesFormModel (date) {
        def currency = new CurrencyBL()
        def entityCurrency = currency.getEntityCurrency(session['company_id'])
        def currencies = currency.getCurrenciesToDate(session['language_id'], session['company_id'], date)
        def timePoints = currency.getUsedTimePoints(session['company_id'])

        return [entityCurrency: entityCurrency, currencies: currencies, startDate: date, timePoints: timePoints]
    }

    def getLastTimePointDate () {
        def timePoints = new CurrencyBL().getUsedTimePoints(session['company_id'])
        def lastDate = CommonConstants.EPOCH_DATE;
        if (timePoints.size() > 0) {
            lastDate = timePoints.get(timePoints.size() - 1)
        }
        return lastDate
    }

    def editCurrency () {
        // only shows edit template to create new currencies.
        // currencies can be edited from the main currency config form
        render template: 'currency/edit', model: [currency: null]
    }

    @RequiresValidFormToken
    def saveCurrency () {
        def currency = new CurrencyWS()
        bindData(currency, removeBlankParams(params))
		

        try {
			def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
			
			if(currencies.find{ it.code.equalsIgnoreCase(currency.code) }){
				throw new SessionInternalError("The currency already exist with this code: " + currency.getCode(),
					["CurrencyWS,code,validation.error.currency.already.exists," + currency.getCode()] as String[]);
	
			}

            webServicesSession.createCurrency(currency)

            flash.message = 'currency.created'
            flash.args = [currency.code]
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            chain action: 'currency', model: [currency: currency]
            return
        }

        chain action: 'currency'
    }

    // remove blank strings '' from binding parameters so that
    // we bind null for empty values
    def Map removeBlankParams(params) {
        def filtered = params.findAll { k, v ->
            if (!k.startsWith('_') && v instanceof String) {
                return v.trim().length()
            } else {
                return true
            }
        }
        return filtered
    }

    /*
       Email settings
    */

    def email () {
        def selfDeliver = webServicesSession.getPreference(Constants.PREFERENCE_PAPER_SELF_DELIVERY)
        def customerNotes = webServicesSession.getPreference(Constants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES)
        def daysForNotification1 = webServicesSession.getPreference(Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S1)
        def daysForNotification2 = webServicesSession.getPreference(Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S2)
        def daysForNotification3 = webServicesSession.getPreference(Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S3)
        def useInvoiceReminders = webServicesSession.getPreference(Constants.PREFERENCE_USE_INVOICE_REMINDERS)
        def firstReminder = webServicesSession.getPreference(Constants.PREFERENCE_FIRST_REMINDER)
        def nextReminder = webServicesSession.getPreference(Constants.PREFERENCE_NEXT_REMINDER)
        def invoiceBundleEmail = webServicesSession.getPreference(Constants.PREFERENCE_EMAIL_INVOICE_BUNDLE)

        [
                selfDeliver: selfDeliver,
                customerNotes: customerNotes,
                daysForNotification1: daysForNotification1,
                daysForNotification2: daysForNotification2,
                daysForNotification3: daysForNotification3,
                useInvoiceReminders: useInvoiceReminders,
                firstReminder: firstReminder,
                nextReminder: nextReminder,
                invoiceBundleEmail: invoiceBundleEmail
        ]
    }


    @RequiresValidFormToken
    def saveEmail () {
        def emailString = params.invoiceBundleEmail
        def error
        if(emailString) {
            if( emailString.contains(EmailValidationRuleModel.COMMA) && emailString.contains(EmailValidationRuleModel.SEMI_COLON) ) {
                error = "email.config.bundle.error.seperator"
            } else {
                List<String> emailList = ContactBL.getEmailList(params.invoiceBundleEmail)
                Pattern pattern = EmailValidationRuleModel.getMatchingPattern()
                for(String email : emailList) {
                    if(!(email ==~ pattern)) {
                        error = "email.config.bundle.error.format"
                        break
                    }
                }
            }
        }
        def selfDeliver = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_PAPER_SELF_DELIVERY), value: params.selfDeliver ? '1' : '0')
        def customerNotes = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES), value: params.customerNotes ? '1' : '0')
        def daysForNotification1 = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S1), value: params.daysForNotification1, intValue: params.daysForNotification1)
        def daysForNotification2 = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S2), value: params.daysForNotification2, intValue: params.daysForNotification2)
        def daysForNotification3 = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S3), value: params.daysForNotification3,intValue: params.daysForNotification3)
        def useInvoiceReminders = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_USE_INVOICE_REMINDERS), value: params.useInvoiceReminders ? '1' : '0')
        def firstReminder = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_FIRST_REMINDER), value: params.firstReminder, intValue: params.firstReminder?:null)
        def nextReminder = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_NEXT_REMINDER), value: params.nextReminder, intValue: params.nextReminder?:null)
        def invoiceBundleEmail = new PreferenceWS(preferenceType: new PreferenceTypeWS(id: Constants.PREFERENCE_EMAIL_INVOICE_BUNDLE), value: params.invoiceBundleEmail)

        try {
            if(error) {
                String[] errors = [ error ]
                throw new SessionInternalError("Error saving email preferences", errors)
            }
            webServicesSession.updatePreferences((PreferenceWS[]) [selfDeliver, customerNotes, daysForNotification1, daysForNotification2, daysForNotification3, useInvoiceReminders, firstReminder, nextReminder, invoiceBundleEmail])

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e, getErrorLabel(params))
            render view: 'email', model: [
                    selfDeliver: selfDeliver,
                    customerNotes: customerNotes,
                    daysForNotification1: daysForNotification1,
                    daysForNotification2: daysForNotification2,
                    daysForNotification3: daysForNotification3,
                    useInvoiceReminders: useInvoiceReminders,
                    firstReminder: firstReminder,
                    nextReminder: nextReminder,
                    invoiceBundleEmail: invoiceBundleEmail
            ]
            return
        }

        flash.message = 'preferences.updated'
        chain action: 'email'
    }

    /*
       Order Change Statuses configuration
     */
    def orderChangeStatuses () {
        log.debug "config.orderChangeStatuses ${session['language_id']}"
        def statuses = webServicesSession.getOrderChangeStatusesForCompany() as List
        statuses.removeAll {it.id == Constants.ORDER_CHANGE_STATUS_PENDING || it.id == Constants.ORDER_CHANGE_STATUS_APPLY_ERROR};
        statuses.sort{it.order}
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        render view: 'orderChangeStatuses', model: [statuses: statuses, languages: LanguageDTO.list()]
    }

    @RequiresValidFormToken
    def saveOrderChangeStatuses () {

        def cnt = params.recCnt.toInteger()
        log.debug "Records Count: ${cnt}"

        def languages = LanguageDTO.list()

        OrderChangeStatusWS[] array = new OrderChangeStatusWS[cnt]
        for (int i = 0; i < cnt; i++) {
            array[i] = bindOrderChangeStatusData(params, i, languages)
        }
        // delete empty descriptions for correct validation in webService
        for (OrderChangeStatusWS status : array) {
            Iterator<InternationalDescriptionWS> descriptionIterator = status.getDescriptions().iterator();
            while (descriptionIterator.hasNext()) {
                InternationalDescriptionWS description = descriptionIterator.next();
                if (description.getContent() == null || description.getContent().trim().equals("")) {
                    descriptionIterator.remove();
                }
            }
        }

        def count = 0;
        for (OrderChangeStatusWS ws : array) {
            if (ApplyToOrder.YES.equals(ws.applyToOrder)) {
                count++;
            }
        }

        try {
            if (count != 1) {
                String [] errors = ["OrderChangeStatusWS,applyToOrder,orderChangeStatusWS.validation.error.select.apply.to.order"]
                throw new SessionInternalError("One status should be selected as 'APPLY' status.", errors);
            }
            webServicesSession.saveOrderChangeStatuses(array);
            flash.message = 'config.orderChangeStatuses.updated'
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.error.saving.orderChangeStatuses'
        }
        chain action: 'orderChangeStatuses'
    }

    def addOrderChangeStatus () {

        def cnt = params.recCnt.toInteger()

        def languages = LanguageDTO.list()
        OrderChangeStatusWS[] array = new OrderChangeStatusWS[cnt+1]
        for (int i = 0; i <= cnt; i++) {
            array[i] = bindOrderChangeStatusData(params, i, languages)
        }

        render template: '/config/orderChangeStatuses/statuses', model: [statuses: array, languages: languages]
    }

    def removeOrderChangeStatus () {

        def cnt = params.recCnt.toInteger()
        def stepIndex = params.int('stepIndex')

        def languages = LanguageDTO.list()

        List<OrderChangeStatusWS> array = [] as List
        int j = 0;
        for (int i = 0; i < cnt; i++) {
            def ws = bindOrderChangeStatusData(params, i, languages);
            if (i == stepIndex) {
                def deletedId = params["obj[" + i + "].id"]
                if (!deletedId) {
                    continue
                } else {
                    ws.deleted = 1
                }
            }
            array << ws;
        }
        render template: '/config/orderChangeStatuses/statuses', model: [statuses: array, languages: languages]
    }

    @Secured('CONFIGURATION_1912')
    def showTools() {
    }

    @RequiresValidFormToken
    @Secured('CONFIGURATION_1912')
    def toolsUploadFile() {
        // save uploaded file
        def file = request.getFile('f')
        def folder = params.folder
        def location = params.file_location

        if(!folder) {
            flash.error = 'config.tools.file.upload.error.no.folder'
        } else if(file.empty) {
            flash.error = 'config.tools.file.upload.error.no.file'
        } else {
            File targetFolder
            if(location == 'relative') {
                targetFolder = new File((SystemProperties.getJBillingHome() ? (SystemProperties.getJBillingHome() + File.separator) : '') + folder)
            } else {
                targetFolder = new File(folder)
            }
            targetFolder.mkdirs()
            file.transferTo(new File(targetFolder.absolutePath+File.separator+file.originalFilename))
            flash.message = 'config.tools.file.upload.success'
        }
        render view: 'showTools'
    }

    private def OrderChangeStatusWS bindOrderChangeStatusData(params, index, languages) {
        OrderChangeStatusWS ws = new OrderChangeStatusWS()
        bindData(ws, params["obj[" + index + "]"])
        ws.setApplyToOrder(params.boolean("obj["+index + "].applyToOrder") ? ApplyToOrder.YES : ApplyToOrder.NO)
        for (LanguageDTO lang : languages) {
            def content = params["obj[" + index + "].description_" + lang.id]
            if (content) {
                InternationalDescriptionWS description = new InternationalDescriptionWS(lang.id, content)
                ws.addDescription(description)
            }
        }

        return ws
    }

    private List<String> getErrorLabel(Map <String,String> params){
        List <String> totalTextFields =["nextReminder","daysForNotification2","daysForNotification1","daysForNotification3","firstReminder"]
        List <String> emptyFields=[]
        for(String label:totalTextFields){
            if(params[label].toString().isEmpty() || params[label].equals("null") || params[label].toString().matches(/^\s*$/) ||params[label].toString().length()>12){
                emptyFields.add(label)
            }
        }
        return emptyFields
    }
}
