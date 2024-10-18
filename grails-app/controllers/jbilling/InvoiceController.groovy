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

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO
import com.sapienter.jbilling.server.invoice.InvoiceBL
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.spa.SpaImportBL
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.InvoiceExportableWrapper
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.CsvFileGeneratorUtil
import com.sapienter.jbilling.server.util.csv.DynamicExport
import com.sapienter.jbilling.server.util.csv.Exporter

/**
 * InvoiceController
 *
 * @author Vikas Bodani
 * @since
 */
@Secured(["MENU_91"])
class InvoiceController {
    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields = [ 'userName': 'baseUser.userName',
        'company': 'company.description',
        'invoiceId': 'id',
        'dueDate': 'dueDate',
        'status': 'invoiceStatus',
        'amount': 'total',
        'balance': 'balance']

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def subAccountService
    SecurityValidator securityValidator
    def availableCredits = null
    def companyService


    def auditBL
    def spaImportBL= new SpaImportBL()

    def index () {
        list()
    }

    def list () {
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoiceIds = parameterIds
        availableCredits = null
        def displayer = UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'] as Integer)

        def csvExportFlag = getPreference(Constants.PREFERENCE_BACKGROUND_CSV_EXPORT)

        def selected = null
        if(params.id) {
            def paramsClone = params.clone()
            paramsClone.clear()

            def invoiceTmp = getInvoices([], paramsClone, [params.int('id')])
            if(invoiceTmp.size() > 0) {
                selected = invoiceTmp[0]
            }
        }

        if (selected) {
            securityValidator.validateUserAndCompany(InvoiceBL.getWS(selected), Validator.Type.VIEW)
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'] as Integer, Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'invoicesTemplate',
                model: [       currencies: retrieveCurrencies(),
                    filters: filters,
                    contactFieldTypes: contactFieldTypes,
                    displayer: displayer]
            }else {
                render  view: 'list',
                model: [       currencies: retrieveCurrencies(),
                    filters: filters,
                    contactFieldTypes: contactFieldTypes,
                    selected: selected,
                    displayer: displayer]
            }
            return
        }

        def invoices;
        try{
            if (selected) {
                invoices = getInvoicesWithSelected(invoiceIds, selected)
                if(selected.balance > BigDecimal.ZERO){
                    availableCredits = CreditNoteDTO.createCriteria()
                            .list() {
                                eq('deleted', 0)
                                createAlias('creationInvoice', 'creationInvoice')
                                eq('creationInvoice.baseUser.id',selected?.baseUser.id)
                                gt('balance', BigDecimal.ZERO)
                                ne('creationInvoice.id', selected.id)
                            }
                }
            } else {
                invoices = getInvoices(filters, params, invoiceIds)
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        if (params.id && params.int("id") && !invoices) {
            flash.error = "flash.invoice.not.found"
        }

        if (params.applyFilter || params.partial) {
            render template: 'invoicesTemplate',
            model: [         invoices: invoices,
                filters: filters,
                selected: selected,
                currencies: retrieveCurrencies(),
                contactFieldTypes: contactFieldTypes,
                availableCredits: availableCredits,
                displayer: displayer,
                csvExportFlag: csvExportFlag]
        } else {
            Set<InvoiceDTO> lines = null
            if(selected){
                InvoiceBL invoiceBl = new InvoiceBL(selected);
                InvoiceDTO invoiceDto = invoiceBl.getInvoiceDTOWithHeaderLines();
                lines = invoiceDto.getInvoiceLines();
            }

            render  view: 'list',
            model: [        invoices: invoices,
                filters: filters,
                selected: selected,
                currencies: retrieveCurrencies(),
                lines: lines,
                availableCredits: availableCredits,
                displayer: displayer,
                csvExportFlag: csvExportFlag]
        }
    }

    def findInvoices (){
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoiceIds = parameterIds

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def selected = params.id ? InvoiceDTO.get(params.int('id')) : null
        try{
            def invoices;
            if (selected) {
                securityValidator.validateUserAndCompany(InvoiceBL.getWS(selected), Validator.Type.VIEW)
                invoices = getInvoicesWithSelected(invoiceIds, selected)
            } else {
                invoices = getInvoices(filters, params, invoiceIds)
            }

            render getInvoicesJsonData(invoices, params) as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

    /**
     * Converts Invoices to JSon
     */
    private def Object getInvoicesJsonData(invoices, GrailsParameterMap params) {
        def jsonCells = invoices
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

    def getInvoicesWithSelected(invoiceIds, selected) {
        def filter = new Filter(          type: FilterType.ALL,
        constraintType: FilterConstraint.EQ,
        field: 'id',
        template: 'id',
        visible: true,
        integerValue: selected.id)

        filterService.setFilter(FilterType.INVOICE, filter)
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        getInvoices(filters, params, invoiceIds)
    }



    def getInvoices(filters, params, ids) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // hide review invoices by default
        def reviewFilter = filters.find { it.field == 'isReview' }
        if (reviewFilter && reviewFilter.value == null) {
            reviewFilter.integerValue = Integer.valueOf(0)
        }

        // get list
        def company_id = session['company_id']
        return InvoiceDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
                ) {
                    createAlias('baseUser','u')
                    createAlias('u.customer','customer')
                    createAlias('u.company','company')
                    def invoiceMetaFieldFilters = []
                    and {
                        filters.each { filter ->
                            if (filter.value != null) {
                                //handle invoiceStatus
                                if (filter.field == 'invoiceStatus') {
                                    def statuses = new InvoiceStatusDAS().findAll()
                                    eq("invoiceStatus", statuses.find { it.primaryKey?.equals(filter.integerValue) })
                                } else if (filter.field == 'contact.fields') {
                                    invoiceMetaFieldFilters.add(filter)
                                } else if(filter.field == 'u.company.description') {
                                    addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                                } else if (filter.field == 'mediationProcess.id') {
                                    sqlRestriction(""" exists (select mom.order_id FROM mediation_order_map as mom
                                                INNER JOIN purchase_order as po on mom.order_id=po.id
                                                INNER JOIN order_process as op on op.order_id = po.id
                                                WHERE mom.mediation_process_id=? AND op.invoice_id={alias}.id) """,[filter.integerValue])
                                }else if(filter.field == 'planInternalNumber' && filter.stringValue){
                                    //filter invoice by subscribtion plan. If company is parent then find all invoice of the hierarchy else
                                    // if company is child then find only that company invoices

                                    List<Integer> customers=[0]
                                    CompanyDTO companyDTO=CompanyDTO.findById(session['company_id'])
                                    List<CompanyDTO> companyDTOList=companyDTO.parent?[companyDTO]:companyService.getHierarchyEntities(session['company_id'])

                                    List<ItemDTO> items=ItemDTO.findAllByInternalNumberIlikeAndEntityInList("${filter.stringValue}%", companyDTOList)
                                    if(items){
                                        List<PlanDTO> plans=PlanDTO.findAllByItemInList(items);
                                        if(plans){
                                            plans.each{PlanDTO plan->
                                                customers.addAll(Arrays.asList(webServicesSession.getSubscribedCustomers(plan.id)))
                                            }
                                        }
                                    }
                                    'in'('customer.id', customers)
                                }else {
                                    addToCriteria(filter.getRestrictions());
                                }
                            }
                        }

                        //invoices of parent + child companies
                        'in'('u.company', retrieveCompanies())
                        eq('deleted', 0)

                        if (SpringSecurityUtils.ifNotGranted("INVOICE_74")) {

                            UserDTO loggedInUser = UserDTO.get(session['user_id'] as Integer)

                            if (loggedInUser.getPartner() != null) {
                                // #7043 - Agents && Commissions - A logged in Partner should only see its customers and the ones of his children.
                                // A child Partner should only see its customers.
                                def partnerIds = []
                                if (loggedInUser.getPartner() != null) {
                                    partnerIds << loggedInUser.getPartner().getId()
                                    if (loggedInUser.getPartner().getChildren()) {
                                        partnerIds += loggedInUser.getPartner().getChildren().id
                                    }
                                }
                                createAlias("customer.partners", "partner")
                                'in'('partner.id', partnerIds)
                            } else if (SpringSecurityUtils.ifAnyGranted("INVOICE_75")) {
                                // restrict query to sub-account user-ids
                                'in'('u.id', subAccountService.subAccountUserIds)
                            } else {
                                // limit list to only this customer
                                eq('u.id', session['user_id'])
                            }
                        }

                        if (ids) {
                            'in'('id', ids.toArray(new Integer[ids.size()]))
                        }

                        if(params.company) {
                            addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                        }
                        if (params.userName) {
                            addToCriteria(Restrictions.ilike('u.userName', params.userName, MatchMode.ANYWHERE))
                        }
                        if(params.invoiceId) {
                            or {
                                eq('publicNumber', params.get('invoiceId'))
                                if (params.invoiceId.isInteger()) {
                                    eq('id', params.int('invoiceId'))
                                }
                            }
                        }
                    }
                    if (invoiceMetaFieldFilters.size() > 0) {
                        invoiceMetaFieldFilters.each { filter ->
                            def detach = DetachedCriteria.forClass(InvoiceDTO.class, "invoice")
                                    .setProjection(Projections.property('invoice.id'))
                                    .createAlias("invoice.metaFields", "metaFieldValue")
                                    .createAlias("metaFieldValue.field", "metaField")
                                    .setFetchMode("metaField", FetchMode.JOIN)
                            String typeId = filter.fieldKeyData
                            String ccfValue = filter.stringValue
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (typeId && ccfValue) {
                                def type1 = findMetaFieldType(typeId.toInteger())
                                try {
                                    def subCriteria = FilterService.metaFieldFilterCriteria(type1, ccfValue);
                                    if (type1 && subCriteria) {
                                        detach.add(Restrictions.eq("metaField.name", type1.name))
                                        //Adding the sub criteria according to the datatype passed
                                        detach.add(Property.forName("metaFieldValue.id").in(subCriteria))
                                        addToCriteria(Property.forName("id").in(detach))
                                    }
                                } catch (SessionInternalError e) {
                                    log.error("Invalid value in the custom field " + e.getMessage().toString())
                                    throw new SessionInternalError(e.getMessage());
                                }
                            }
                        }
                    }

                    // apply sorting
                    SortableCriteria.buildSortNoAlias(params, delegate)
                }
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    @Secured(["INVOICE_73"])
    def csv () {
        def filters = filterService.getFilters(FilterType.INVOICE, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        def invoiceExportables
        def filteredInvoiceIds = getFilteredInvoiceIds(filters, params, parameterIds)
        try {
            if(getPreference(Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0) {
                invoiceExportables  = getInvoicesForCsv(filteredInvoiceIds, DynamicExport.YES)
                if (invoiceExportables.size() > CsvExporter.GENERATE_CSV_LIMIT) {
                    flash.error = message(code: 'error.export.exceeds.maximum')
                    flash.args = [ CsvExporter.GENERATE_CSV_LIMIT ]
                    render "failure"
                } else {
                    CsvFileGeneratorUtil.generateCSV(com.sapienter.jbilling.client.util.Constants.INVOICE_CSV, invoiceExportables, session['company_id'],session['user_id']);
                    render "success"
                }
            } else {
                invoiceExportables = getInvoicesForCsv(filteredInvoiceIds, DynamicExport.NO)
                renderInvoiceCsvFor(invoiceExportables)
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
    }

    List<InvoiceExportableWrapper> getInvoicesForCsv(def invoiceIds,  def dynamicExport) {
        List<InvoiceExportableWrapper> invoiceExportables = new ArrayList<>()
        for(Integer invoiceId: invoiceIds) {
            invoiceExportables.add(new InvoiceExportableWrapper(invoiceId, dynamicExport))
        }
        return invoiceExportables
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    @Secured(["INVOICE_73"])
    def csvByProcess (){
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)

        def invoices
        try {
            invoices = getInvoices(filters, params, null)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
        renderCsvFor(invoices)
    }

    def renderCsvFor(invoices) {
        if (invoices.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "invoices.csv")
            Exporter<InvoiceDTO> exporter = CsvExporter.createExporter(InvoiceDTO.class);
            render text: exporter.export(invoices), contentType: "text/csv"
        }
    }

    def renderInvoiceCsvFor(invoiceExportables) {
        if (invoiceExportables.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "invoices.csv")
            Exporter<InvoiceExportableWrapper> exporter = CsvExporter.createExporter(InvoiceExportableWrapper.class);
            render text: exporter.export(invoiceExportables), contentType: "text/csv"
        }
    }

    def batchPdf (){
        def filters = filterService.getFilters(FilterType.INVOICE, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = PaperInvoiceBatchBL.MAX_RESULTS

        def invoices;
        try {
            invoices = getInvoices(filters, params, null)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        if (invoices.totalCount > PaperInvoiceBatchBL.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [PaperInvoiceBatchBL.MAX_RESULTS]
            redirect action: 'list', id: params.id
        } else {
            try {
                PaperInvoiceBatchBL paperInvoiceBatchBL = new PaperInvoiceBatchBL();
                String fileName = paperInvoiceBatchBL.generateBatchPdf((List<InvoiceDTO>) invoices, (Integer) session['company_id'])
                String realPath = Util.getSysProp("base_dir") + "invoices" + File.separator;
                File file = new File(realPath+fileName)
                FileInputStream fileInputStream = new FileInputStream(file)
                byte[] pdfBytes = IOUtils.toByteArray(fileInputStream)
                DownloadHelper.sendFile(response, fileName, "application/pdf", pdfBytes)
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                redirect action: 'list', id: params.id
            } catch (Exception e) {
                log.error("Exception fetching PDF invoice data.", e)
                flash.error = 'invoice.prompt.failure.downloadPdf'
                redirect action: 'list', id: params.id
            }
        }
    }

    /**
     * Convenience shortcut, this action shows all invoices for the given user id.
     */
    def user () {
        def filter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'baseUser.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, filter)

        redirect action: 'list'
    }

    @Secured(["INVOICE_72"])
    def show () {
        def invoice = InvoiceDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.VIEW)

        availableCredits = null

        if (!invoice) {
            log.debug("Redirecting to list")
            redirect(action: 'list')
            return
        }
        recentItemService.addRecentItem(invoice.id, RecentItemType.INVOICE)
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, invoice.id, invoice.number)

        InvoiceBL invoiceBl = new InvoiceBL((InvoiceDTO)invoice);
        InvoiceDTO invoiceDto = invoiceBl.getInvoiceDTOWithHeaderLines();
        Set<InvoiceLineDTO> lines = invoiceDto.getInvoiceLines();

        if(invoice?.balance > BigDecimal.ZERO){
            availableCredits = CreditNoteDTO.createCriteria().list() {
                eq('deleted', 0)
                createAlias('creationInvoice', 'creationInvoice')
                eq('creationInvoice.baseUser.id',invoice.baseUser.id)
                gt('balance', BigDecimal.ZERO)
                ne('creationInvoice.id', invoice.id)
            }
        }

        render template: params.template ?: 'show',
        model: [          selected: invoice,
            currencies: retrieveCurrencies(),
            lines: lines,
            availableCredits: availableCredits,
            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'] as Integer),
            distributelMCFFile: spaImportBL.getMCFFileName(invoice.getId()) ]
    }


    def snapshot () {
        def invoiceId = params.int('id')
        if (invoiceId) {
            InvoiceWS invoice = webServicesSession.getInvoiceWS(invoiceId)
            render template: 'snapshot', model: [ invoice: invoice, currencies: retrieveCurrencies(), availableMetaFields: retrieveAvailableMetaFields() ]
        }
    }

    @Secured(["INVOICE_70"])
    def delete () {
        int invoiceId = params.int('id')

        if (invoiceId) {
            def invoice = InvoiceDTO.get(invoiceId)
            securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.EDIT)

            try {
                webServicesSession.deleteInvoice(invoiceId)
                flash.message = 'invoice.delete.success'
                flash.args = [invoiceId]

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                log.error("Exception deleting invoice.", e)
                flash.error = 'error.invoice.delete'
                flash.args = [params.id]
                redirect action: 'list', params: [id: invoiceId]
                return
            }
        }

        redirect action: 'list'
    }

    @Secured(["INVOICE_71"])
    def email () {
        if (params.id) {
            Integer invoiceId = params.int('id')
            def invoice = InvoiceDTO.get(invoiceId)
            securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.EDIT)

            try {
                def sent = webServicesSession.notifyInvoiceByEmail(params.int('id'))

                if (sent) {
                    flash.message = 'invoice.prompt.success.email.invoice'
                    flash.args = [params.id]
                } else {
                    flash.error = 'invoice.prompt.failure.email.invoice'
                    flash.args = [params.id]
                }

            } catch (SessionInternalError sie) {
                log.error("Exception occurred sending invoice email", sie)
                viewUtils.resolveException(flash, session.locale, sie)
            }
        }

        redirect action: 'list', params: [id: params.id]
    }

    def downloadPdf () {
        Integer invoiceId = params.int('id')

        securityValidator.validateUserAndCompany(InvoiceBL.getWS(InvoiceDTO.get(invoiceId)), Validator.Type.VIEW)

        try {
            byte[] pdfBytes = webServicesSession.getPaperInvoicePDF(invoiceId)
            def invoice = webServicesSession.getInvoiceWS(invoiceId)
            DownloadHelper.sendFile(response, "invoice-${invoice?.number}.pdf", "application/pdf", pdfBytes)

        }catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            redirect action: 'list', params: [id: invoiceId]
        } catch (Exception e) {
            log.error("Exception fetching PDF invoice data.", e)
            flash.error = 'invoice.prompt.failure.downloadPdf'
            redirect action: 'list', params: [id: params.id]
        }
    }

    def downloadDistributelMCFFile() {
        Integer invoiceId = params.int('id')
        securityValidator.validateUserAndCompany(InvoiceBL.getWS(InvoiceDTO.get(invoiceId)), Validator.Type.VIEW)

        try {
            byte[] mediationFileBytes = spaImportBL.getMCFFile(invoiceId)
            DownloadHelper.sendFile(response, spaImportBL.getMCFFileName(invoiceId), "application/pdf", mediationFileBytes)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            redirect action: 'list', params: [id: invoiceId]
        } catch (Exception e) {
            log.error("Exception fetching PDF MCF data.", e)
            flash.error = 'invoice.prompt.failure.downloadPdf'
            redirect action: 'list', params: [id: params.id]
        }
    }


    @Secured(["PAYMENT_33"])
    def unlink () {
        Integer invoiceId = params.int('id')
        def invoice = InvoiceDTO.get(invoiceId)
        securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.EDIT)

        try {
            webServicesSession.removePaymentLink(invoiceId, params.int('paymentId'))
            flash.message = "payment.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.payment"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def findByProcess () {
        if (!params.id) {
            flash.error = 'error.invoice.byprocess.missing.id'
            chain action: 'list'
            return
        }

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoices

        try {
            invoices = getInvoices(filters, params, null)
            render getInvoicesJsonData(invoices, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
    }

    def byProcess (){
        if (!params.id) {
            flash.error = 'error.invoice.byprocess.missing.id'
            chain action: 'list'
            return
        }

        def csvExportFlag = getPreference(Constants.PREFERENCE_BACKGROUND_CSV_EXPORT)
        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoices;
        try {
            invoices = getInvoices(filters, params, null)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        render view: 'list', model: [invoices: invoices, filters: filters, currencies: retrieveCurrencies(),
            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),csvExportFlag:csvExportFlag]
    }

    def byMediation () {
        def mediationFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.IN, field: 'mediationProcess.id',
        template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, mediationFilter)

        redirect action: 'list'
    }

    def retrieveCurrencies() {
        //in this controller we need only currencies objects with inUse=true without checking rates on date
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

    def retrieveCompanies() {
        def parentCompany = CompanyDTO.get(session['company_id'])
        def childs = CompanyDTO.findAllByParent(parentCompany)
        childs.add(parentCompany)
        return childs;
    }

    def getAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }

    def getParameterIds() {

        // Grails bug when using lists with <g:remoteLink>
        // http://jira.grails.org/browse/GRAILS-8330
        // TODO (pai) remove workaround

        def parameterIds = new ArrayList<Integer>()
        def idParamList = params.list('ids')
        idParamList.each { idParam ->
            if (idParam?.isInteger()) {
                parameterIds.add(idParam.toInteger())
            }
        }
        if (parameterIds.isEmpty()) {
            String ids = params.ids
            if (ids) {
                ids = ids.replace('[', "").replace(']', "")
                String [] numbers = ids.split(", ")
                numbers.each { paramId ->
                    if (paramId?.isInteger()) {
                        parameterIds.add(paramId.toInteger());
                    }
                }
            }
        }

        return parameterIds;
    }

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

    @Secured(["INVOICE_72"])
    def history (){
        def invoice = InvoiceDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.VIEW)

        def currentInvoice = auditBL.getColumnValues(invoice)
        def invoiceVersions = auditBL.get(InvoiceDTO.class, invoice.getAuditKey(invoice.id), versions.max)
        def lines = auditBL.find(InvoiceLineDTO.class, getInvoiceLineSearchPrefix(invoice))

        def records = [
            [ name: 'invoice', id: invoice.id, current: currentInvoice, versions:  invoiceVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: invoice.id, lines: lines, linecontroller: 'invoice', lineaction: 'linehistory' ]
    }

    def getInvoiceLineSearchPrefix(invoice) {
        return "${invoice.baseUser.company.id}-usr-${invoice.baseUser.id}-inv-${invoice.id}-"
    }

    @Secured(["INVOICE_72"])
    def linehistory (){
        def line = InvoiceLineDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(InvoiceBL.getWS(line?.invoice), Validator.Type.VIEW)

        def currentLine = auditBL.getColumnValues(line)
        def lineVersions = auditBL.get(InvoiceLineDTO.class, line.getAuditKey(line.id), versions.max)

        def records = [
            [ name: 'line', id: line.id, current: currentLine, versions: lineVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: line.invoice.id ]
    }

    def unlinkCreditNote () {
        try {
            webServicesSession.removeCreditNoteLink(params.int('id'), params.int('creditNoteId'))
            flash.message = "creditNote.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.creditNote"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def linkCreditNote () {
        try {
            webServicesSession.applyCreditNoteToInvoice(params.int('linkCreditNoteId'),params.int('id'))
            flash.message = "creditNote.link.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception linking invoice.", e)
            flash.error = "error.invoice.link.creditNote"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def getPreference(Integer preferenceTypeId){
        def preferenceValue = webServicesSession.getPreference(preferenceTypeId).getValue()
        return !StringUtils.isEmpty(preferenceValue) ? Integer.valueOf(preferenceValue) : new Integer(0)
    }


    def getFilteredInvoiceIds(filters, params, ids) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // hide review invoices by default
        def reviewFilter = filters.find { it.field == 'isReview' }
        if (reviewFilter && reviewFilter.value == null) {
            reviewFilter.integerValue = Integer.valueOf(0)
        }

        // get list
        def company_id = session['company_id']
        return InvoiceDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
                ) {
                    property('id')
                    createAlias('baseUser','u')
                    createAlias('u.customer','customer')
                    createAlias('u.company','company')
                    def invoiceMetaFieldFilters = []
                    and {
                        filters.each { filter ->
                            if (filter.value != null) {
                                //handle invoiceStatus
                                if (filter.field == 'invoiceStatus') {
                                    def statuses = new InvoiceStatusDAS().findAll()
                                    eq("invoiceStatus", statuses.find { it.primaryKey?.equals(filter.integerValue) })
                                } else if (filter.field == 'contact.fields') {
                                    invoiceMetaFieldFilters.add(filter)
                                } else if(filter.field == 'u.company.description') {
                                    addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                                } else if (filter.field == 'mediationProcess.id') {
                                    sqlRestriction(""" exists (SELECT mom.order_id
												FROM mediation_order_map AS mom
                                                INNER JOIN purchase_order AS po ON mom.order_id=po.id
                                                INNER JOIN order_process AS op ON op.order_id = po.id
                                                WHERE mom.mediation_process_id=?
												AND op.invoice_id={alias}.id) """,[filter.integerValue])
                                }else if(filter.field == 'planInternalNumber' && filter.stringValue){
                                    //filter invoice by subscribtion plan. If company is parent then find all invoice of the hierarchy else
                                    // if company is child then find only that company invoices

                                    List<Integer> customers=[0]
                                    CompanyDTO companyDTO=CompanyDTO.findById(session['company_id'])
                                    List<CompanyDTO> companyDTOList=companyDTO.parent?[companyDTO]:companyService.getHierarchyEntities(session['company_id'])

                                    List<ItemDTO> items=ItemDTO.findAllByInternalNumberIlikeAndEntityInList("${filter.stringValue}%", companyDTOList)
                                    if(items){
                                        List<PlanDTO> plans=PlanDTO.findAllByItemInList(items);
                                        if(plans){
                                            plans.each{PlanDTO plan->
                                                customers.addAll(Arrays.asList(webServicesSession.getSubscribedCustomers(plan.id)))
                                            }
                                        }
                                    }
                                    'in'('customer.id', customers)
                                }else {
                                    addToCriteria(filter.getRestrictions());
                                }
                            }
                        }

                        //invoices of parent + child companies
                        'in'('u.company', retrieveCompanies())
                        eq('deleted', 0)

                        if (SpringSecurityUtils.ifNotGranted("INVOICE_74")) {
                            if (SpringSecurityUtils.ifAnyGranted("INVOICE_75")) {
                                // restrict query to sub-account user-ids
                                'in'('u.id', subAccountService.subAccountUserIds)
                            } else {
                                // limit list to only this customer
                                eq('u.id', session['user_id'])
                            }
                        }

                        if (ids) {
                            'in'('id', ids.toArray(new Integer[ids.size()]))
                        }

                        if(params.company) {
                            addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                        }
                        if (params.userName) {
                            addToCriteria(Restrictions.ilike('u.userName', params.userName, MatchMode.ANYWHERE))
                        }
                        if(params.invoiceId) {
                            or {
                                eq('publicNumber', params.get('invoiceId'))
                                if (params.invoiceId.isInteger()) {
                                    eq('id', params.int('invoiceId'))
                                }
                            }
                        }
                    }
                    if (invoiceMetaFieldFilters.size() > 0) {
                        invoiceMetaFieldFilters.each { filter ->
                            def detach = DetachedCriteria.forClass(InvoiceDTO.class, "invoice")
                                    .setProjection(Projections.property('invoice.id'))
                                    .createAlias("invoice.metaFields", "metaFieldValue")
                                    .createAlias("metaFieldValue.field", "metaField")
                                    .setFetchMode("metaField", FetchMode.JOIN)
                            String typeId = filter.fieldKeyData
                            String ccfValue = filter.stringValue
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (typeId && ccfValue) {
                                def type1 = findMetaFieldType(typeId.toInteger())
                                try {
                                    def subCriteria = FilterService.metaFieldFilterCriteria(type1, ccfValue);
                                    if (type1 && subCriteria) {
                                        detach.add(Restrictions.eq("metaField.name", type1.name))
                                        //Adding the sub criteria according to the datatype passed
                                        detach.add(Property.forName("metaFieldValue.id").in(subCriteria))
                                        addToCriteria(Property.forName("id").in(detach))
                                    }
                                } catch (SessionInternalError e) {
                                    log.error("Invalid value in the custom field " + e.getMessage().toString())
                                    throw new SessionInternalError(e.getMessage());
                                }
                            }
                        }
                    }

                    // apply sorting
                    SortableCriteria.buildSortNoAlias(params, delegate)
                }
    }
}
