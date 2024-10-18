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

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO
import com.sapienter.jbilling.server.invoice.InvoiceTemplateVersionDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.invoiceTemplate.domain.DocDesign
import com.sapienter.jbilling.server.invoiceTemplate.report.*
import com.sapienter.jbilling.server.invoiceTemplate.ui.JsonFactory
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.SecurityValidator

import static com.sapienter.jbilling.common.Util.getSysProp
import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup.*

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j

import net.sf.jasperreports.engine.design.JRValidationException

import org.apache.commons.httpclient.HttpStatus
import org.hibernate.ObjectNotFoundException

/**
 * InvoiceTemplateController
 *
 * @author Elijah Motorny
 * @since
 */
@Slf4j
class InvoiceTemplateController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    def breadcrumbService
    def webServicesSession
    def dataSource
    def viewUtils
    SecurityValidator securityValidator


    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def index () {
        redirect action: list, params: params
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def list () {
        def templates = getInvoiceTemplates()
        def selected = params.id ? InvoiceTemplateDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, selected?.name)

        // if id is present and object not found, give an error details to the user along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'invoiceTemplate.not.found'
            flash.args = [params.id]
        }

        render view: 'list', model: [invoiceTemplates: templates, selectedInvoiceTemplate: selected]
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def listVersions() {
        InvoiceTemplateDTO selected = params.id ? InvoiceTemplateDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)

        List<InvoiceTemplateVersionDTO> versionDTOs = []
        Integer versionsTotalCount = 0

        if(selected){
            (versionDTOs, versionsTotalCount) = getVersionList(selected)
            breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected.id, selected.name)
        }

        render template: 'listTemplateVersions', model: [invoiceTemplateVersions: versionDTOs,
                                                         showDeleteButton: params.partial ? false : true,
                                                         selectedTemplate:selected, versionsTotalCount: versionsTotalCount]
    }

    def getInvoiceTemplates() {
        def invoiceTemplates = InvoiceTemplateDTO.createCriteria().list()
                {
                    eq('entity', new CompanyDTO(session['company_id']))
                    order('id', 'desc')
                }
        return invoiceTemplates
    }

    def uploadJson () {
        DocDesign newDesign = new DocDesign()
        def map = [:]
        try {
            // Check if the title was filled in. Otherwise show an error.
            if (params.name) {
                // Check for duplicates.
                if (InvoiceTemplateBL.isDuplicateInvoiceTemplate(params.name,session['company_id'])){
                    map.error = message(code: "invoiceTemplate.error.name.duplicate")
                    render(status: HttpStatus.SC_OK, text: map as JSON)
                    return
                }

                newDesign.setName(params.name.toString())
            } else {
                map.error = message(code: "invoiceTemplate.error.name.empty")
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }

            // Check if the JSON content was filled in. Otherwise show an error.
            if (params.json) {
                String name = newDesign.getName()
                newDesign = JsonFactory.getGson().fromJson(params.json.toString(), DocDesign.class)
                newDesign.setName(name)
            } else {
                map.error = message(code: "invoiceTemplate.error.emptyJson")
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
        } catch (Exception ex) {
            // The provided JSON is invalid so show an error.
            map.error = message(code: "invoiceTemplate.error.invalidJson")
            render(status: HttpStatus.SC_OK, text: map as JSON)
            return
        }

        InvoiceTemplateDTO newTemplate = new InvoiceTemplateDTO()

        newTemplate = InvoiceTemplateBL.createNewTemplateWithVersion(newTemplate,
                newDesign.name,
                JsonFactory.getGson().toJson(newDesign),
                (Integer) session['company_id'],
                Integer.valueOf(session.getAttribute('user_id').toString()))

        map.id = newTemplate.id
        map.message = message(code: "invoiceTemplate.created", args: [newTemplate.name])

        render(status: HttpStatus.SC_OK, text: map as JSON)
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def show () {
        def selected = params.id ? InvoiceTemplateDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected.id, selected.name)

        render template: 'show', model: [selected: selected]
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def showVersion () {
        InvoiceTemplateVersionDTO selectedTemplateVersion = params.id ? InvoiceTemplateVersionDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selectedTemplateVersion?.invoiceTemplate?.entity?.id, Validator.Type.VIEW)

        if(!selectedTemplateVersion){
            flash.error = "invoiceTemplateVersion.find.error.message"
            flash.args = [params.id ?:'']
            return redirect(action: 'list')
        }

        breadcrumbService.addBreadcrumb(controllerName, 'showVersion', null, selectedTemplateVersion.id, selectedTemplateVersion.versionNumber)
        if(params.template && params.template.equals('showTemplateVersion')){
            return render (template: 'showTemplateVersion', model: [selected: selectedTemplateVersion])
        }
        List<InvoiceTemplateVersionDTO> versionDTOs = []
        Integer versionsTotalCount = 0

        (versionDTOs, versionsTotalCount) = getVersionList(selectedTemplateVersion.invoiceTemplate)

        render(view: 'list', model: [selectedTemplateVersion:selectedTemplateVersion, invoiceTemplateVersions: versionDTOs, showDeleteButton: true,
                                     versionsTotalCount: versionsTotalCount, selectedTemplate:selectedTemplateVersion.invoiceTemplate ])
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1804')"])
    def delete () {
        int invoiceTemplateId = params.int('id')

        if (invoiceTemplateId) {
            InvoiceTemplateVersionDTO invoiceTemplateVersion = InvoiceTemplateVersionDTO.get(invoiceTemplateId)
            securityValidator.validateCompany(invoiceTemplateVersion?.invoiceTemplate?.entity?.id, Validator.Type.EDIT)
            try{
                if(invoiceTemplateVersion && InvoiceTemplateVersionBL.validateVersionForDelete(invoiceTemplateVersion)){
                    invoiceTemplateVersion.invoiceTemplate.invoiceTemplateVersions.remove(invoiceTemplateVersion)
                    invoiceTemplateVersion.delete(flush: true);
                }
            }catch(SessionInternalError e){
                viewUtils.resolveException(flash, session.locale, e)
                return redirect(action: 'list', id: invoiceTemplateVersion.invoiceTemplate.id)
            }
            flash.message = "invoiceTemplate.version.deleted"
            flash.args = [invoiceTemplateId]
        }

        redirect action: 'list'
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1804')"])
    def deleteTemplate () {
        int invoiceTemplateId = params.int('id')

        if (invoiceTemplateId) {
            def invoiceTemplate = InvoiceTemplateDTO.get(invoiceTemplateId)
            securityValidator.validateCompany(invoiceTemplate?.entity?.id, Validator.Type.EDIT)
            try{
                if (invoiceTemplate && InvoiceTemplateBL.validateTemplateForDelete(invoiceTemplate))
                    invoiceTemplate.delete(flush: true);
            }catch(SessionInternalError e){
                viewUtils.resolveException(flash, session.locale, e)
                return redirect(action: 'list', id: invoiceTemplateId)
            }
            flash.message = "invoiceTemplate.deleted"
            flash.args = [invoiceTemplateId]
        }

        redirect action: 'list'
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def json () {
        InvoiceTemplateVersionDTO selected = params.id ? InvoiceTemplateVersionDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.invoiceTemplate?.entity?.id, Validator.Type.VIEW)
        render(text: selected.templateJson, contentType: "text/json", encoding: "UTF-8")
    }

    def create() {
        render template: 'create', model: [invoiceTemplate: new InvoiceTemplateDTO()] + (params.srcId ? [srcId: params.srcId] : [:])
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1802')"])
    def save () {
        DocDesign newDesign = new DocDesign()

        // If we are cloning we get the existing template and use its data.
        if (params.srcId) {
            // Clone an existing design.
            InvoiceTemplateVersionDTO oldVersion = InvoiceTemplateVersionDTO.get(params.srcId)
            securityValidator.validateCompany(oldVersion?.invoiceTemplate?.entity?.id, Validator.Type.EDIT)
            def oldDesign = oldVersion.invoiceTemplate
            oldDesign.setTemplateJson(oldVersion.getId())
            newDesign = JsonFactory.getGson().fromJson((oldDesign as InvoiceTemplateDTO).templateJson, DocDesign.class);
        } else {
            String designFile = getSysProp("base_dir") + "/invoiceTemplates/default_invoice_template.json";
            File defaultTemplate = new File(designFile);
            def reader = new BufferedReader(new InputStreamReader(new FileInputStream(defaultTemplate)));
            newDesign = JsonFactory.getGson().fromJson(reader, DocDesign.class);
        }

        def newTemplate = new InvoiceTemplateDTO(params)

        // If a name has been set we use it or we use the default one to check for duplicates
        if (newTemplate.name && InvoiceTemplateDTO.findByNameAndEntity(newTemplate.name, new CompanyDTO(session['company_id'])) != null) {
            flash.error = "invoiceTemplate.error.name.duplicate"
            render(view: 'list', model: [invoiceTemplates: getInvoiceTemplates(), invoiceTemplate: newTemplate, srcId: params.srcId])
            return
        } else if (!newTemplate.name) {
            while (InvoiceTemplateDTO.findByName(newDesign.name) != null) {
                def duplicates = InvoiceTemplateDTO.findAllByNameLike("${newDesign.name}%")
                newDesign.name = "${newDesign.name} ${duplicates.size}"
            }
        } else {
            if (newTemplate.name.length() > 50) {
                flash.error = "invoiceTemplate.error.name.exceedLength"
                render(view: 'list', model: [invoiceTemplates: getInvoiceTemplates(), invoiceTemplate: newTemplate, srcId: params.srcId])
                return
            }
            newDesign.name = newTemplate.name
        }

        newTemplate = InvoiceTemplateBL.createNewTemplateWithVersion(newTemplate, newDesign.name,
                JsonFactory.getGson().toJson(newDesign),
                (Integer) session['company_id'],
                Integer.valueOf(session.getAttribute('user_id').toString()))

        flash.message = "invoiceTemplate.created"
        flash.args = [newTemplate.name]

        render(view: 'list', model: [invoiceTemplates: getInvoiceTemplates(), selected: newTemplate])
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1801')"])
    def edit () {
        InvoiceTemplateVersionDTO selected = params.id ? InvoiceTemplateVersionDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.invoiceTemplate?.entity?.id, Validator.Type.EDIT)
        if(!selected){
            flash.error = "invoiceTemplateVersion.find.error.message"
            flash.args = [params.id ?:'']
            return redirect(action: 'list')
        }
        render view: 'edit', model: [selected: selected, isNew:params.boolean('isNew')]
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1802')"])
    def saveTemplate () {
        InvoiceTemplateVersionDTO selectedTemplateVersion = params.id ? InvoiceTemplateVersionDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selectedTemplateVersion?.invoiceTemplate?.entity?.id, Validator.Type.EDIT)
        InvoiceTemplateDTO selectedTemplate = selectedTemplateVersion ? selectedTemplateVersion.invoiceTemplate : null
        InvoiceTemplateVersionDTO newVersion
        Map respJson = [:]

        if (selectedTemplateVersion != null) {
            selectedTemplate.invoiceId = params.invoice_id == null || params.invoice_id == "" || !params.invoice_id.isInteger() ?
                    null : params.invoice_id as int
            selectedTemplateVersion.tagName = params.tagName ?: selectedTemplateVersion.tagName
            Boolean useForInvoice = params.useForInvoice ? params.boolean('useForInvoice') : Boolean.FALSE
            // if the flag has changed
            if((params.get('_useForInvoice') != null) && !useForInvoice.equals(selectedTemplateVersion.useForInvoice)){
                InvoiceTemplateVersionBL.updateUseForInvoice(selectedTemplateVersion, useForInvoice)
            }

            if((params.get('_includeCarriedInvoiceLines') != null)) {
                selectedTemplateVersion.includeCarriedInvoiceLines = params.includeCarriedInvoiceLines ? params.boolean('includeCarriedInvoiceLines') : false
            }

            if (params.json != null) {
                def gson = JsonFactory.getGson()
                def docDesign = gson.fromJson(params.json, DocDesign.class)
                // Check if the name's length is valid.
                if (docDesign.name.length() > 50) {
                    render(status: '409', text: message(code: 'invoiceTemplate.error.name.exceedLength'), contentType: 'text/plain', encoding: 'UTF-8')
                    return
                }

                def duplicate = InvoiceTemplateDTO.findByName(docDesign.name);
                // Check if the chosen name is already in use.
                if (duplicate != null && duplicate.id != selectedTemplate.id) {
                    render(status: '409', text: "Error. Template '${docDesign.name}' already exists. Use another name", contentType: 'text/plain', encoding: 'UTF-8')
                    return
                }
                selectedTemplate.name = docDesign.name
                selectedTemplate.setEntity(new CompanyDTO(session['company_id']))

                // if the jsonTemplate has changed, create new version
                if( (params.tempAction && params.tempAction.equals(Constants.SAVE_VERSION))
                        ||
                        (!selectedTemplateVersion.templateJson.equals(gson.toJson(docDesign)) && !params.boolean('isNew'))){
                    InvoiceTemplateVersionBL versionBL = new InvoiceTemplateVersionBL(selectedTemplateVersion)
                    newVersion = versionBL.createNewVersion()
                    newVersion.setUserId(Integer.valueOf(session.getAttribute('user_id').toString()))
                    newVersion.templateJson = gson.toJson(docDesign)
                    newVersion.invoiceTemplate = selectedTemplateVersion.invoiceTemplate
                    selectedTemplateVersion.invoiceTemplate.getInvoiceTemplateVersions().add(newVersion)
                }else{
                    selectedTemplateVersion.templateJson = gson.toJson(docDesign)
                }
            }

            selectedTemplate.save(flush:true)
            if(newVersion){
                respJson['message'] = (params.tempAction && params.tempAction.equals(Constants.SAVE_VERSION)) ? "SAVED" : 'NEW'
                respJson['version'] = newVersion
                return render(respJson as JSON)
            }
        }

        respJson['message'] = "OK"
        respJson['version'] = selectedTemplateVersion
        render(respJson as JSON)
    }

    @Secured(["hasAnyRole('INVOICE_TEMPLATES_1803')"])
    def report () {
        boolean asImage = "img".equals(params['format'])
        try {
            InvoiceTemplateVersionDTO invoiceTemplateVersion = params.id ? InvoiceTemplateVersionDTO.get(params.int('id')) : null
            securityValidator.validateCompany(invoiceTemplateVersion?.invoiceTemplate?.entity?.id, Validator.Type.VIEW)

            InvoiceTemplateDTO invoiceTemplate = invoiceTemplateVersion.invoiceTemplate
            invoiceTemplate.setTemplateJson(invoiceTemplateVersion.id)
            Integer invoiceId = invoiceTemplate.invoiceId ?: params.int('invoiceId')

            InvoiceDTO invoice = InvoiceDTO.get(invoiceId)

            InvoiceTemplateBL invoiceTemplateBL = InvoiceTemplateBL.createInvoiceTemplateBL(invoiceTemplate, invoice)

            response.addHeader("Cache-control", "private, max-age=0, no-cache")
            if (asImage) {
                response.contentType = "image/png"

                def pageIndex = params.pagenum ? ((params.pagenum as int) - 1) : 0
                if (pageIndex >= invoiceTemplateBL.pageNumber) {
                    InvoiceTemplateBL.generateErrorReport("No more pages", "Total pages: " + invoiceTemplateBL.pageNumber, asImage, response.outputStream)
                    return
                } else {
                    invoiceTemplateBL.exportToPng(pageIndex, response.outputStream)
                }
            } else {
                response.contentType = "application/pdf"
                response.addHeader("Content-disposition", "attachment; filename=invoice.pdf")

                invoiceTemplateBL.exportToPdf(response.outputStream)
            }
        }
        catch (ObjectNotFoundException e) {
            log.error "Exception: ", e
            String entityName = e.entityName == InvoiceDTO.class.name ? "Invoice" : e.entityName
            InvoiceTemplateBL.generateErrorReport("Not found", entityName + ": " + e.identifier,
                    asImage, response.outputStream)
        }
        catch (JRValidationException e) {
            log.error "Exception: ", e
            InvoiceTemplateBL.generateErrorReport("Report Design is not valid", e.message,
                    asImage, response.outputStream)
        }
        catch (Exception e) {
            log.error "Exception: ", e
            InvoiceTemplateBL.generateErrorReport(e.class.simpleName, e.message,
                    asImage, response.outputStream)
        }
    }

    def fields () {
        def j = { Collection<FieldSetup>[] src ->
            final List<FieldSetup> l = new ArrayList<FieldSetup>();
            src.each({ Collection<FieldSetup> c ->
                l.addAll(c)
            })
            return l
        }
        def w = { Collection<FieldSetup> c ->
            Set<FieldSetup> s = new TreeSet<FieldSetup>(new Comparator<FieldSetup>() {
                @Override
                public int compare(FieldSetup o1, FieldSetup o2) {
                    return o1.description.compareTo(o2.description);
                }
            })
            s.addAll(c)
            return s
        }
        render JsonFactory.getGson().toJson([
                'eventFields'  : w(j(COMMON_FIELDS, CDR_EVENTS_FIELDS)),
                'invoiceFields': w(j(COMMON_FIELDS, INVOICE_LINES_FIELDS))
        ])
    }

    // copy from product controller
    def retrieveCompanies = {
        def companies = retrieveChildCompanies()
        companies.add(CompanyDTO.get(session['company_id']))

        return companies
    }

    // copy from product controller
    def retrieveChildCompanies = {
        return CompanyDTO.findAllByParent(CompanyDTO.get(session['company_id']))
    }

    private getVersionList(InvoiceTemplateDTO selected){
        List<InvoiceTemplateVersionDTO> versionDTOs = []
        Integer versionsTotalCount = 0

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params.sort ?: 'versionNumber'

        if(!params.sort.equals('versionNumber')){
            versionDTOs = InvoiceTemplateVersionDTO.createCriteria().list([
                    max: params.max,
                    offset: params.offset
            ]){
                eq('invoiceTemplate', selected )
                order(params.sort, params.order)
            }
            versionsTotalCount = versionDTOs.totalCount
        }else{
            versionDTOs.addAll(selected.invoiceTemplateVersions)
            if(params.order && params.order.toString().equalsIgnoreCase('asc')){
                versionDTOs.sort(new InvoiceTemplateVersionBL.CompareInvoiceTemplateVersionByVerNumberAsc())
            }else{
                versionDTOs.sort(new InvoiceTemplateVersionBL.CompareInvoiceTemplateVersionByVerNumberDesc())
            }
            versionsTotalCount = versionDTOs.size()
            versionDTOs = versionDTOs.subList(params.offset,Math.min(params.offset+params.max,versionDTOs.size()))
        }
        return [versionDTOs, versionsTotalCount]
    }
}
