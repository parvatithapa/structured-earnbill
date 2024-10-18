package jbilling

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.company.CompanyInformationTypeWS
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroupDAS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.lang.StringUtils
import org.hibernate.criterion.CriteriaSpecification

@Secured(["isAuthenticated()", "MENU_99"])
class AuthenticationController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields =
            ['typeId': 'id']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def viewUtils

    def companyService
    SecurityValidator securityValidator

    def index () {
        listCIT()
    }

    def showCIT() {

        CompanyInformationTypeDTO cit = CompanyInformationTypeDTO.get(params.int('id'))
        securityValidator.validateCompany(cit?.company?.id, Validator.Type.VIEW)

        CompanyDTO company = CompanyDTO.get(params.int('companyId').toInteger())
        securityValidator.validateCompany(company?.id, Validator.Type.VIEW)

        if (!cit) {
            log.debug "redirecting to list"
            redirect(action: 'listCIT')
            return
        }

        if (params.template) {
            // render requested template
            render template: params.template, model: [ selected: cit, company: company]

        } else {

            //search for CITs for the selected company
            def cits = CompanyInformationTypeDTO.createCriteria().list(
                    max:    params.max,
                    offset: params.offset,
                    sort:   params.sort,
                    order:  params.order
            ) {
                eq("company.id", company?.id.toInteger())
            }

            render view: 'listCIT', model: [ selected: cit, cits: cits, company: company ]
        }
    }

    def deleteCIT() {

        def companyInformationTypeId = params.int('id')
        log.debug 'CIT delete called on ' + companyInformationTypeId

        CompanyInformationTypeWS citWS = webServicesSession.getCompanyInformationType(companyInformationTypeId)
        securityValidator.validateCompany(citWS?.entityId, Validator.Type.EDIT)

        try {
            webServicesSession.deleteCompanyInformationType(params.id?.toInteger());
            flash.message = 'config.company.information.type.delete.success'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            log.error e.getMessage()
            flash.error = 'config.company.information.type.delete.error.being.used'
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.company.information.type.delete.error'
        }

        params.id = null
        listCIT()

    }

    def listCIT() {

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def company = CompanyDTO.get(session['company_id'])
        securityValidator.validateCompany(company?.id, Validator.Type.VIEW)

        def cit = CompanyInformationTypeDTO.get(params.int('id'))
        securityValidator.validateCompany(cit?.company?.id, Validator.Type.VIEW)

        def cits = webServicesSession.getInformationTypesForCompany(company?.id)

        if (params?.applyFilter || params?.partial) {
            render template: 'companyInformationTypes', model: [ cits: cits, company: company, selected: cit]
        } else {
            render view: 'listCIT', model: [ cits: cits, company: company, selected: cit ]
        }
    }

    def editCITFlow ={

        initialize {
            action {

                CompanyInformationTypeWS cit = params.id ? webServicesSession.getCompanyInformationType(params.int('id')) :
                    new CompanyInformationTypeWS();
                securityValidator.validateCompany(cit.entityId, Validator.Type.EDIT)

                if (!cit) {
                    log.error("Could not fetch WS object")
                    citNotFoundErrorRedirect(params.id, params.companyId)
                    return
                }

                def company = CompanyDTO.get(session['company_id'])
                def currencies =  new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(),
                        session['company_id'].toInteger(),true)


                // set sensible defaults for new cit
                if (!cit.id || cit.id == 0) {
                    cit.companyId = company?.id
                    cit.entityType = EntityType.COMPANY_INFO
                    cit.entityId = session['company_id'].toInteger()
                    cit.metaFields  = []
                }


                if (params.clone == "true") {
                    cit.setId(0);
                    if (cit.getMetaFields() != null) {
                        for (MetaFieldWS mf : cit.getMetaFields()) {
                            mf.setId(0);
                            mf.setPrimary(false);
                        }
                    }
                }

                // available metafields and metafield groups
                def metaFields=retrieveMetaFieldsForCompany()
                def metaFieldGroups = retrieveMetaFieldGroupsForCompany()

				conversation.compInfoType = removeUsedCit(cit)

                // model scope for this flow
                flow.company = company
                flow.company = company
                flow.currencies = currencies
				
                // conversation scope
                conversation.cit = cit
                conversation.metaFieldGroups = metaFieldGroups
                conversation.metaFields = metaFields

                List<CompanyInformationTypeDTO> infoTypes = company?.companyInformationTypes?.sort { it.displayOrder }
                List<MetaFieldWS> metaFieldWSes=[]
                com.sapienter.jbilling.server.metafields.MetaFieldBL metaFieldBL=new MetaFieldBL();
                infoTypes.each{
                    it.metaFields.each {MetaField metaField->
                        metaFieldWSes.add(metaFieldBL.getWS(metaField))
                    }
                }

                conversation.unSavedMetafields = metaFieldWSes
                if(cit.metaFields != null) {
                    cit.metaFields.each {
                        it.setFakeId(it.id)
                    }
                }
                conversation.nextId = 0
                conversation.removedMetaFields = []
                params.dependencyCheckBox = true
            }
            on("success").to("build")
        }

        /**
         * Renders the cit details tab panel.
         */
        showDetails {
            action {
                params.template = 'detailsCIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafields tab panel, containing all the metafields that can be imported
         */
        showMetaFields {
            action {
                params.template = 'metafieldsCIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafield groups tab panel, containing all the metafield groups that can be used as a template
         * for creation of the company information type
         */
        showMetaFieldGroups {
            action {
                params.template = 'metafieldGroupsCIT'
            }
            on("success").to("build")
        }

        /**
         *  Imports the selected metafield groups for using as a template for company information type creation
         *  Is available only for creating new information types
         */
        importFromMetaFieldGroup {
            action {

                def metaFieldGroupId = params.int('id')
                def metaFieldGroup = webServicesSession.getMetaFieldGroup(metaFieldGroupId)

                if (!metaFieldGroup) {
                    params.template = 'reviewCIT'
                    error()
                } else {

                    def cit = conversation.cit

                    cit.name = metaFieldGroup.getDescription()
                    cit.displayOrder = metaFieldGroup.displayOrder
                    cit.metaFields = []

                    def metaFields = cit.metaFields as List
                    metaFields.addAll(metaFieldGroup.metaFields as List)
                    metaFields.each {
                        it.setId(0)
                        it.setPrimary(false);
                    }

                    cit.metaFields = metaFields.toArray()

                    conversation.cit = cit

                    params.newLineIndex = metaFields.size() - 1
                    params.template = 'reviewCIT'
                }
            }
            on("success").to("build")
            on("error").to("build")
        }


        /**
         * Adds a metafield to the company information type
         */
        addCITMetaField {
            action {

                def metaFieldId = params.int('id')

                def metaField = metaFieldId ? webServicesSession.getMetaField(metaFieldId) :
                    new MetaFieldWS();

                metaField.primary = false

                if (metaField?.id || metaField.id != 0) {
                    // set metafield defaults
                    metaField.id = 0
                } else {
                    metaField.entityType = EntityType.COMPANY_INFO
                    metaField.entityId = session['company_id'].toInteger()
                }

                metaField.fakeId = conversation.nextId - 1
                metaField.id = metaField.fakeId
                conversation.nextId--

                // add metafield to cit
                def cit = conversation.cit
                def metaFields = cit.metaFields as List
                metaFields.add(metaField)
                cit.metaFields = metaFields.toArray()

                conversation.cit = cit
				
				conversation.compInfoType = removeUsedCit(cit)
				
                params.newLineIndex = metaFields.size() - 1
                params.dependencyCheckBox = true
                params.template = 'reviewCIT'
            }
            on("success").to("build")
        }

        /**
         * Updates an metafield  and renders the CIT metafields panel
         */
        updateCITMetaField {
            action {

                flash.errorMessages = null
                flash.error = null
                def cit = conversation.cit
                //dependency CheckBox visible at the time of update cit meta field
                params.dependencyCheckBox = true
                // get existing metafield
                def index = params.int('index')
                def metaField = cit.metaFields[index]
                if (!bindMetaFieldData(metaField, params, index)) {
                    error()
                }
				
				if(null!= metaField.fieldUsage && metaField.fieldUsage.equals(MetaFieldType.COUNTRY_CODE)) {
					flash.errorMessages = [ message(code: 'countryCode.warning.message') ];
				}
				
                if(!params.get("dependency-checkbox")){
                    metaField.dependentMetaFields=null;
                }
                if(!params.get("help-checkbox")){
                    metaField.helpContentURL=null;
                    metaField.helpDescription=null;
                }
                // add metafield to the cit
                cit.metaFields[index] = metaField

                // sort metafields by displayOrder
                cit.metaFields = cit.metaFields.sort { it.displayOrder }
                conversation.cit = cit
                def unSavedMetafields = conversation.unSavedMetafields.findAll{!cit.metaFields.id.contains(it.id)}+cit.metaFields.findAll { StringUtils.trimToNull(it.name) != null } ?: []
                conversation.unSavedMetafields = unSavedMetafields

				conversation.compInfoType = removeUsedCit(cit)

                params.template = 'reviewCIT'
            }
            on("success").to("build")
        }

        /**
         * Remove a metafield from the information type  and renders the CIT metafields panel
         */
        removeCITMetaField {
            action {

                //dependency CheckBox visible at the time of remove cit meta field
                params.dependencyCheckBox = true
                def cit = conversation.cit

                def index = params.int('index')
                def metaFields = cit.metaFields as List

                def metaField = metaFields.get(index)
                 metaFields.remove(index)
                conversation.removedMetaFields << metaField

                cit.metaFields = metaFields.toArray()

                conversation.cit = cit

                params.template = 'reviewCIT'
            }
            on("success").to("build")
        }

        /**
         * Updates company information type attributes
         */
        updateCIT {
            action {

                //dependency CheckBox visible at the time of update cit
                params.dependencyCheckBox = true
                def cit = conversation.cit
                bindData(cit, params)

                cit.metaFields = cit.metaFields.sort { it.displayOrder }
                conversation.cit = cit

                params.template = 'reviewCIT'
            }
            on("success").to("build")
        }

        /**
         * Shows the company information type metafield builder.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            on("details").to("showDetails")
            on("metaFields").to("showMetaFields")
            on("metaFieldGroups").to("showMetaFieldGroups")
            on("addMetaField").to("addCITMetaField")
            on("importMetaFieldGroup").to("importFromMetaFieldGroup")
            on("updateMetaField").to("updateCITMetaField")
            on("removeMetaField").to("removeCITMetaField")
            on("update").to("updateCIT")

            on("save").to("saveCIT")

            on("cancel").to("finish")
        }

        /**
         * Saves the company information type and exits the builder flow.
         */
        saveCIT {
            action {
                try {

                    //dependency CheckBox visible at the time of save cit meta field
                    params.dependencyCheckBox = true
                    def cit = conversation.cit
                    Set<MetaField> metaFields = cit.metaFields
                    Set<String> mfNames = metaFields*.name
                    if (metaFields.size() != mfNames.size()) {
                        throw new SessionInternalError("MetaField", ["CompanyInformationTypeDTO,metafield,metaField.name.exists"] as String[])
                    }

                    if(!cit.name){
                        cit.descriptions.add(new InternationalDescriptionWS())
                    }

                    if (!cit.id || cit.id == 0) {
                        cit.id = webServicesSession.createCompanyInformationType(cit)
                        session.message = 'company.information.type.created'
                        session.args = [ cit.id ]

                    } else {
                        webServicesSession.updateCompanyInformationType(cit)

                        session.message = 'company.information.type.updated'
                        session.args = [ cit.id ]
                    }

                } catch (SessionInternalError e) {
                    Set<MetaFieldWS> metaFieldWSes = new HashSet<>(Arrays.asList(conversation.cit?.metaFields))
                    metaFieldWSes.addAll(conversation.removedMetaFields)
                    conversation.cit?.metaFields = metaFieldWSes.toArray(new MetaFieldWS[metaFieldWSes.size()])

                    viewUtils.resolveException(flow, session.locale, e)
                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        finish {
            redirect controller: 'authentication', action: 'listCIT',
                    id: conversation.cit?.id, params: [companyId: conversation.cit?.companyId]
        }
    }

    private void citNotFoundErrorRedirect(citId, companyId) {
        session.error = 'cit.not.found'
        session.args = [ citId as String ]
        redirect controller: 'authentication', action: 'listCIT',
                params: [companyId: companyId]
    }

	private boolean bindMetaFieldData(MetaFieldWS metaField, params, index){
        try{
            MetaFieldBindHelper.bindMetaFieldName(metaField, params, false, index.toString())
        } catch (Exception e){
            log.debug("Error at binding meta field  : "+e)
            return false;
        }

        return true

	}

    def retrieveCompany() {
        CompanyDTO.get(session['company_id'])
    }

    def retrieveCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }
	
	private def retrieveMetaFieldsForCompany(){
        def types = new EntityType[1];
        types[0] = EntityType.COMPANY_INFO
        new MetaFieldDAS().getAvailableFields(session['company_id'], types, null)
    }

	private def retrieveMetaFieldGroupsForCompany(){
        return new MetaFieldGroupDAS().getAvailableFieldGroups(session['company_id'], EntityType.COMPANY_INFO)
   }
	
	private def removeUsedCit(cit) {
		def compInfoType = MetaFieldType.values()
		cit?.metaFields.each {
		   compInfoType -= it.fieldUsage
	   }
		return compInfoType
	}	
}
