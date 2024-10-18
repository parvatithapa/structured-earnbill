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
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.Constants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldHelper
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.payment.PaymentBL
import com.sapienter.jbilling.server.payment.PaymentInformationBL
import com.sapienter.jbilling.server.payment.PaymentInformationWS
import com.sapienter.jbilling.server.payment.PaymentMethodTypeBL
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS
import com.sapienter.jbilling.server.payment.PaymentTransferWS
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.payment.db.PaymentDAS
import com.sapienter.jbilling.server.payment.db.PaymentDTO
import com.sapienter.jbilling.server.payment.db.PaymentExportableWrapper
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.CsvFileGeneratorUtil
import com.sapienter.jbilling.server.util.csv.DynamicExport
import com.sapienter.jbilling.server.util.csv.Exporter

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.hibernate.criterion.Projections
import org.hibernate.Criteria

/**
 * PaymentController
 *
 * @author Brian Cowdery
 * @since 04/01/11
 */
@Secured(["MENU_93"])
class PaymentController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
    static versions = [ max: 25 ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['userName': 'u.userName',
             'company': 'company.description',
             'paymentId': 'id',
             'date': 'paymentDate',
             'paymentOrRefund': 'isRefund',
             'amount':'amount',
             'method':'paymentMethod',
             'result':'paymentResult']

    IWebServicesSessionBean webServicesSession
    def webServicesValidationAdvice
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def subAccountService
    def auditBL
    SecurityValidator securityValidator


    def index () {
        list()
    }

    def getList(filters, params, boolean projection) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

		def user_id = session['user_id'] as Integer

		def partnerDto = PartnerDTO.findByBaseUser (UserDTO.get(user_id))
		log.debug "### partner:" + partnerDto

		def customersForUser = new ArrayList()
		if( partnerDto ){
			customersForUser = 	CustomerDTO.createCriteria().list(){
                createAlias("partners", "partners")
                eq('partners.id', partnerDto.id)
			}
		}
        log.debug "### customersForUser: " + customersForUser

        return PaymentDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            if(projection) {
                projections {
                    property('id')
                }
            }
            createAlias('baseUser', 'u')
            createAlias('u.company','company')

            // create alias only if applying invoice filters to prevent duplicate results
            if (filters.find{ it.field.startsWith('i.') && it.value })
                createAlias('invoicesMap', 'i', Criteria.LEFT_JOIN)
            def paymentMetaFieldFilters = []
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                    	if (filter.field == 'contact.fields') {
                            paymentMetaFieldFilters.add(filter)
                        } else if(filter.field == 'u.company.description') {
                            addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
						} else if(filter.field == 'paymentMethod.credit.id') {
                            addToCriteria(Restrictions.in("paymentMethod.id", filter.stringValue.split(',').collect {Integer.parseInt(it)}));
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }

                if(params.company) {
                    addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                }
                if(params.paymentId) {
                    eq('id', params.int('paymentId'))
                }
                if (params.userName) {
                    addToCriteria(Restrictions.ilike('u.userName', params.userName, MatchMode.ANYWHERE))
                }

				//payments of parent + childs
				'in'('u.company', retrieveCompanies())
                eq('deleted', 0)

                if (SpringSecurityUtils.ifNotGranted("PAYMENT_36")) {
                    if (SpringSecurityUtils.ifAnyGranted("PAYMENT_37")) {
                        // restrict query to sub-account user-ids
                        'in'('u.id',subAccountService.getSubAccountUserIds())
                    } else {
                        // limit list to only this customer
                        if(customersForUser.isEmpty()){
                            eq('u.id', user_id)
                        }else {
                            or {
                                eq('u.id', user_id)
                                'in'('u.id', customersForUser.baseUser.userId)
                            }
                        }
                    }
                }
            }
            if (paymentMetaFieldFilters.size() > 0) {
                paymentMetaFieldFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(PaymentDTO.class, "payment")
                            .setProjection(Projections.property('payment.id'))
                            .createAlias("payment.metaFields", "metaFieldValue")
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
     * Gets a list of payments and renders the the list page. If the "applyFilters" parameter is given,
     * the partial "_payments.gsp" template will be rendered instead of the complete payments list page.
     */
    def list () {

		def filters = filterService.getFilters(FilterType.PAYMENT, params)
		def csvExportFlag = getPreference(Constants.PREFERENCE_BACKGROUND_CSV_EXPORT)
        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.client.util.Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'paymentsTemplate', model: [filters: filters, contactFieldTypes: contactFieldTypes]
            }else {
                render view: 'list', model: [filters: filters, contactFieldTypes: contactFieldTypes]
            }
            return
        }

        def selected = null
        if(params.id) {
            def paramsClone = params.clone()
            paramsClone.clear()
            paramsClone['paymentId'] = params.int('id')
            def pmtTmp = getList([], paramsClone, false)
            if(pmtTmp.size() > 0) {
                selected = pmtTmp[0]
            }
        }

        if(SpringSecurityUtils.ifNotGranted("PAYMENT_34")) {
            selected = null
        }
        if (selected) {
            securityValidator.validateUserAndCompany(webServicesSession.getPayment(selected.id), Validator.Type.VIEW)
		}

		breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        def payments
        try{
            payments = getList(filters, params, false)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.id?.isInteger() && selected == null){
            flash.error = message(code: 'flash.payment.not.found')
        }

        if (params.applyFilter || params.partial) {
            render template: 'paymentsTemplate', model: [ payments: payments, selected: selected, filters: filters, contactFieldTypes: contactFieldTypes, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),csvExportFlag:csvExportFlag ]
        } else {
            render view: 'list', model: [ payments: payments, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),csvExportFlag:csvExportFlag ]
        }
    }

    def findPayments () {
        def filters = filterService.getFilters(FilterType.PAYMENT, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def selected = params.id ? PaymentDTO.get(params.int("id")) : null

        if(selected) {
            securityValidator.validateUserAndCompany(PaymentBL.getWS(new PaymentBL(selected).getDTOEx(session['language_id'].toInteger())), Validator.Type.VIEW)
        }
        try {
            def payments = selected ? getListWithSelected(selected) : getList(filters, params, false)
            render getPaymentsJsonData(payments, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

    /**
     * Converts Payments to JSon
     */
    private def Object getPaymentsJsonData(payments, GrailsParameterMap params) {
        def jsonCells = payments
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

    def getListWithSelected(selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        try {
            getList([idFilter], params, false)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
    }

    /**
     * Applies the set filters to the payment list, and exports it as a CSV for download.
     */
    @Secured(["PAYMENT_35"])
    def csv () {
        def filters = filterService.getFilters(FilterType.PAYMENT, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

		def paymentExportables
        try {
            def filteredPaymentIds = getList(filters, params, true)
			if(getPreference(Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0){
	            paymentExportables = getPaymentsForCsv(filteredPaymentIds,DynamicExport.YES)
				if (paymentExportables.size() > CsvExporter.GENERATE_CSV_LIMIT) {
					flash.error = message(code: 'error.export.exceeds.maximum')
					flash.args = [ CsvExporter.GENERATE_CSV_LIMIT ]
					render "failure"
				} else {
					CsvFileGeneratorUtil.generateCSV(com.sapienter.jbilling.client.util.Constants.PAYMENT_CSV, paymentExportables, session['company_id'],session['user_id']);
					render "success"
				}
			} else {
				paymentExportables = getPaymentsForCsv(filteredPaymentIds, DynamicExport.NO)
				renderPaymentCsvFor(paymentExportables)
			}

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

	def renderPaymentCsvFor(paymentExportables) {
		if (paymentExportables.size() > CsvExporter.GENERATE_CSV_LIMIT) {
			flash.error = message(code: 'error.export.exceeds.maximum')
			flash.args = [ CsvExporter.MAX_RESULTS ]
			redirect action: 'list', id: params.id

		} else {
			DownloadHelper.setResponseHeader(response, "payments.csv")
			Exporter<PaymentExportableWrapper> exporter = CsvExporter.createExporter(PaymentExportableWrapper.class);
			render text: exporter.export(paymentExportables), contentType: "text/csv"
		}
	}

	List<PaymentExportableWrapper> getPaymentsForCsv(def payments,  def dynamicExport) {
		List<PaymentExportableWrapper> paymentExportables = new ArrayList<>()
		for (Integer paymentId : payments) {
			paymentExportables.add(new PaymentExportableWrapper(paymentId, dynamicExport))
		}
		return paymentExportables
	}

    /**
     * Downloads the payment details for the given paymentID
     */
    @Secured(["hasAnyRole('PAYMENT_34', 'PAYMENT_36')"])
    def downloadPayment () {
        // get paymentId and userId from params
        Integer paymentId = params.int('paymentId')
        Integer userId = params.int('userId')
        try {
            // limit user to to only this user's payment if he can't view all
            if ( SpringSecurityUtils.ifNotGranted("PAYMENT_36")) {
                log.debug 'Is Customer, and PAYMENT_36 Role not granted.'
                if ( !userId.equals(session['user_id'])) {
                    log.error "Unauthorized access: User ${session['user_id']} trying to view User ${userId} 's Payment"
                    throw new SessionInternalError("Unauthorized.");
                }
            }

            PaymentDTO paymentDto= PaymentDTO.get(paymentId)
            securityValidator.validateUserAndCompany(webServicesSession.getPayment(paymentDto.id), Validator.Type.VIEW)
            UserDTO user = UserDTO.get(userId)

            // retrieve the payment kept at specified location
            String baseDir= com.sapienter.jbilling.common.Util.getSysProp("base_dir")
            String separator= System.getProperty("file.separator")
            String fileName= "Payment-${paymentDto.getId()}"
            String pdfLocation = "${baseDir}notifications${separator}${user.getUserName()}${separator}"

            log.debug "Pdf Location: $pdfLocation"

            new File(pdfLocation).mkdir()
            File file = File.createTempFile(fileName, "pdf", new File(pdfLocation))

            byte[] pdfBytes = file.getBytes()
            DownloadHelper.sendFile(response, fileName, "application/pdf", pdfBytes)

        } catch (FileNotFoundException fnfe) {
            log.error("File Not Found Exception "+fnfe)
            flash.error = "payment.prompt.failure.downloadPdf.fileNotFound"
            redirect(action: 'list', params: [id: paymentId])
        } catch (Exception e) {
            log.error("Some Exception occured "+e)
            flash.error = "payment.prompt.failure.downloadPdf"
            redirect(action: 'list', params: [id: paymentId])
        }

    }

    /**
     * Show details of the selected payment.
     */
    @Secured(["PAYMENT_34"])
    def show () {
        def payment = PaymentDTO.get(params.int('id'))
        securityValidator.validateUserAndCompany(webServicesSession.getPayment(payment.id), Validator.Type.VIEW)

        if (!payment) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
        recentItemService.addRecentItem(params.int('id'), RecentItemType.PAYMENT)
        breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, params.int('id'))

        render template: 'show', model: [ selected: payment, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']) ]
    }

    /**
     * Convenience shortcut, this action shows all payments for the given user id.
     */
    def user () {
        def filter =  new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'u.id', template: 'id', visible: true, integerValue: params.id)
        filterService.setFilter(FilterType.PAYMENT, filter)

        redirect (action: "list")
    }

    /**
     * Delete the given payment id
     */
    @Secured(["PAYMENT_32", "PAYMENT_1907"])
    def delete () {
        if (params.id) {
            securityValidator.validateUserAndCompany(webServicesSession.getPayment(params.int('id')), Validator.Type.EDIT)
            try {
                webServicesSession.deletePayment(params.int('id'))
                log.debug("Deleted payment ${params.id}.")
                flash.message = 'payment.deleted'
                flash.args = [params.id]
                params.applyFilter = true
            } catch (SessionInternalError e) {
                viewUtils.resolveExceptionMessage(flash, session.local, e)
                params.applyFilter = false
                params.partial = true
            }
        }

        // render the partial payments list
		redirect action: 'list'
    }

    /**
     * Shows the payment link screen for the given payment ID showing a list of un-paid invoices
     * that the payment can be applied to.
     */
    @Secured(["PAYMENT_33"])
    def link () {
        def payment = webServicesSession.getPayment(params.int('id'))
        def user = webServicesSession.getUserWS(payment?.userId ?: params.int('userId'))
        def invoices = getUnpaidInvoices(user.userId)

        securityValidator.validateUserAndCompany(payment, Validator.Type.EDIT)
        if(payment.userId != user.id) {
            securityValidator.validateUserAndCompany(user, Validator.Type.EDIT)
        }

		// collects on those payment instruments that are allowed on front end
		List<PaymentInformationWS> paymentInstruments
        def instrument
        try {
            instrument = webServicesSession.getUserPaymentInstrument(user.userId, session['company_id'] as Integer)
        } catch (SessionInternalError e) {
            paymentDataNotFoundErrorRedirect(e, 'validation.payment.data.not.found', [user.getId()])
        }
        // verify if certain method of payment is allowed and add corresponding
		if(instrument) {
			paymentInstruments = instrument?.getUserPaymentInstruments()
		}

		// collects only those payment method types that are allowed on front end
		def paymentMethodTypes = AccountTypeDTO.get(user.accountTypeId).paymentMethodTypes

        render view: 'link', model: [ payment: payment, user: user, invoices: invoices, currencies: retrieveCurrencies(), invoiceId: params.invoiceId, availableFields: retrieveAvailableMetaFields(), paymentMethods: paymentMethodTypes, paymentInstruments : paymentInstruments, accountTypeId : user.accountTypeId ]
    }

    /**
     * Applies a given payment ID to the given invoice ID.
     */
    @Secured(["PAYMENT_33"])
    @RequiresValidFormToken
    def applyPayment () {
        def payment = webServicesSession.getPayment(params.int('id'))
        securityValidator.validateUserAndCompany(payment, Validator.Type.EDIT)

        if (payment && params.invoiceId) {
            try {
                log.debug("appling payment ${payment} to invoice ${params.invoiceId}")
                webServicesSession.createPaymentLink(params.int('invoiceId'), payment.id)

                flash.message = 'payment.link.success'
                flash.args = [ payment.id, params.invoiceId ]

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.local, e)
                link()
                return
            }

        } else if (!payment) {
            flash.warn = 'payment.not.exists'
            flash.args = [payment.id, params.invoiceId]
        } else {
            flash.warn = 'invoice.not.selected'
            flash.args = [payment.id, params.invoiceId]
        }


        // show the list page
        def filters = filterService.getFilters(FilterType.PAYMENT, params)
        def payments;
        try {
            payments = getList(filters, params, false)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        render view: 'list', model: [ payments: payments, filters: filters ]
    }

    /**
     * Un-links the given payment ID from the given invoice ID and re-renders
     * the "show payment" view panel.
     */
    @Secured(["PAYMENT_33"])
    def unlink () {
        def payment = webServicesSession.getPayment(params.int('id'))
        securityValidator.validateUserAndCompany(payment, Validator.Type.EDIT)
        try {
            webServicesSession.removePaymentLink(params.int('invoiceId'), params.int('id'))
            flash.message = "payment.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.payment"
        }

        redirect action: 'list', params: [id: params.id]
    }

    /**
     * Redirects to the user list and sets a flash message.
     */
    @Secured(["PAYMENT_30"])
    def create () {
        flash.info = 'payment.select.customer'
        redirect controller: 'customer', action: 'list'
    }

    /**
     * Gets the payment to be edited and shows the "edit.gsp" view. This edit action cannot be used
     * to create a new payment, as creation requires a wizard style flow where the user is selected first.
     */
    @Secured(["hasAnyRole('PAYMENT_30', 'PAYMENT_31')"])
    def edit () {
        def payment
        def user

        if (params.id) {
            securityValidator.validateUserAndCompany(webServicesSession.getPayment(params.int('id')), Validator.Type.EDIT)
        }

        try {
            payment = params.id ? webServicesSession.getPayment(params.int('id')) : new PaymentWS()

            if (payment?.deleted==1) {
            	paymentNotFoundErrorRedirect(params.id)
            	return
            }

            if (params.id) {
                PaymentBL paymentBL = new PaymentBL(params.int("id"))
                if (paymentBL.ifRefunded()) {
                    flash.error = 'validation.error.update.refunded.payment'
                    flash.args = [params.id]
                    redirect controller: 'payment', action: 'list'
                    return
                }
            }

            user = webServicesSession.getUserWS(payment?.userId ?: params.int('userId'))

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
			paymentNotFoundErrorRedirect(params.id)
            return
        }

        securityValidator.validateUserAndCompany(user, Validator.Type.EDIT, true)

        def invoices = getUnpaidInvoices(user.userId)

		// collects on those payment instruments that are allowed on front end
		List<PaymentInformationWS> paymentInstruments
        def instrument
        def paymentMethodTypes
        try {
            if (payment.id == 0){
                instrument = webServicesSession.getUserPaymentInstrument(user.userId, session['company_id'] as Integer)
                // collects only those payment method types that are allowed on front end
                paymentMethodTypes = AccountTypeDTO.get(user.accountTypeId).paymentMethodTypes
            }else{
                paymentInstruments = payment?.paymentInstruments
                paymentMethodTypes = new ArrayList<PaymentMethodTypeDTO>()
                paymentMethodTypes.add(PaymentMethodTypeDTO.get(payment?.paymentInstruments?.paymentMethodTypeId))
            }
        } catch (SessionInternalError e) {
            paymentDataNotFoundErrorRedirect(e, 'validation.payment.data.not.found', [user.getId()])
        }
        // verify if certain method of payment is allowed and add corresponding
		if(instrument) {
			paymentInstruments = instrument?.getUserPaymentInstruments()
		}


		//set payment amount equal of user total owned
		def payOwned=false
		if(params.payOwned){
			def owned=UserBL.getBalance(user.userId)
			payment.setAmount(owned)
			invoices=[]
			params.invoiceId=null
			payOwned=true
		}

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))


        //send all the payments of the current user as well which are not normal payments with a balance greater than zero
        List<PaymentDTO> refundablePayments = new PaymentDAS().getRefundablePayments(user.getUserId())
        log.debug "invoices are ${invoices}"
        log.debug "payments are ${refundablePayments}"

		def isRealtimePayment = isRealtimePayment(paymentInstruments, paymentMethodTypes)
		def preferenceValue = webServicesSession.getPreference(Constants.PREFERENCE_SUBMIT_TO_PAYMENT_GATEWAY).getValue()
        def submitToPaymentGatewayPreference = !StringUtils.isEmpty(preferenceValue) ? Integer.valueOf(preferenceValue) : 0
        def changeSubmitToPaymentGatewayPermission = !SpringSecurityUtils.ifAllGranted("PAYMENT_1916")

		[ payment: payment, user: user, invoices: invoices, currencies: retrieveCurrencies(),
            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
			paymentMethods: paymentMethodTypes, invoiceId: params.int('invoiceId'),
			refundablePayments: refundablePayments, refundPaymentId: params.int('payment?.paymentId'),
			availableFields: retrieveAvailableMetaFields(), payOwned:payOwned, paymentInstruments : paymentInstruments, accountTypeId : user.accountTypeId , isRealtimePayment : isRealtimePayment, submitToPaymentGatewayPreference : submitToPaymentGatewayPreference, changeSubmitToPaymentGatewayPermission : changeSubmitToPaymentGatewayPermission]
    }

    private void paymentNotFoundErrorRedirect(paymentId) {
    	flash.error = 'payment.not.found'
		flash.args = [ paymentId as String ]
		redirect controller: 'payment', action: 'list'
    }

    private void paymentDataNotFoundErrorRedirect(exception, errorString, args) {
        Exception ex = viewUtils.getRootCause(exception)
        if (ex) {
            viewUtils.resolveException(flash, session.local, ex)
        } else if (errorString && args) {
            flash.error = errorString
            flash.args = args
        }
        redirect controller: 'payment', action: 'list'
        return
    }

    def getUnpaidInvoices(Integer userId) {
        def invoiceIds = webServicesSession.getUnpaidInvoices(userId);

        List<InvoiceWS> invoices = new ArrayList<InvoiceWS>(invoiceIds.size());
        for (Integer id : invoiceIds)
            invoices.add(webServicesSession.getInvoiceWS(id))
        return invoices;
    }

    /**
     * Shows a summary of the created/edited payment to be confirmed before saving.
     */
    @Secured(["hasAnyRole('PAYMENT_30', 'PAYMENT_31')"])
    @RequiresValidFormToken
    def confirm () {
        def payment = new PaymentWS()
		bindPayment(payment, params)

        if(payment.id) {
            securityValidator.validateUserAndCompany(webServicesSession.getPayment(payment.id), Validator.Type.EDIT)
        }
        session['user_payment']= payment

        if(StringUtils.isNotBlank(params.paymentId)){
            payment.paymentId = Integer.valueOf(params.paymentId)
        }
		// bind payment instruments
		def instrumentUser = new UserWS()
		def accountTypeId = params.int("accountTypeId")

        // make sure the user still exists before
        def user
		def userId = payment?.userId ?: params.int('userId')
        try {
            user = webServicesSession.getUserWS(userId)
            instrumentUser.setEntityId(user.getEntityId()) //where does the user belong
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'customer.not.found'
            flash.args = [ params.id ]

            redirect controller: 'payment', action: 'list'
            return
        }

		def submitToPaymentGateway = params.submitToPaymentGateway ? true : false
        if (new Boolean(params.isRealtimePayment) && new Boolean(params.changeSubmitToPaymentGatewayPermission)) {
            submitToPaymentGateway = params.submitToPaymentGatewayPreference == "1"
        }
		Integer listSize = instrumentUser.getPaymentInstruments().size()
		List<PaymentInformationWS> allPayments = new ArrayList<PaymentInformationWS>(listSize)
		List<PaymentInformationWS> toProcess = new ArrayList<PaymentInformationWS>(listSize)

		// puts payment informations that will be used to process payment in payment object depending upon processing order
		// puts all the payment instruments in a different array to preserve in case an error occurs
		instrumentUser.setId(userId)

        def invoices = getUnpaidInvoices(user.userId)
		List<PaymentDTO> refundablePayments = new PaymentDAS().getRefundablePayments(user.getUserId())
        // validate before showing the confirmation page
        try {
            UserHelper.bindPaymentInformations(instrumentUser ,params.int("modelIndex"), params)
			// also set payment methods for newly entered payment instruments
			categorizePayments(allPayments, toProcess, payment, instrumentUser)
            webServicesValidationAdvice.validateObject(payment)

            if(payment.amountAsDecimal == BigDecimal.ZERO) {
                String [] errors = ["PaymentWS,amount,validation.error.payment.amount.cannot.be.zero"]
                    throw new SessionInternalError("Payment Amount Cannot Be Zero",
                        errors);
            }

			if(payment.getPaymentInstruments().size() < 1) {
				String [] errors = ["PaymentWS,paymentMethodId,validation.error.apply.without.method"]
					throw new SessionInternalError("At least one payment method must be entered",
						errors);
			}

			if(payment.isRefund) {
				if(null==payment.getPaymentId()) {
					String [] errors = [
						"PaymentWS,paymentId,validation.error.payment.linked.refund"
					]
					throw new SessionInternalError("Cannot apply a Refund without a linked Payment ID",errors);
				}
				if(!PaymentBL.validateRefund(payment)){
					String [] errors = [
						"PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount"
					]
					throw new SessionInternalError("Either refund payment was not linked to any payment or the refund amount is in-correct",
						errors);
				}
			}
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.local, e)

			//There no invoices needed for a total owned payment
			if(params.payOwned){
				invoices=null
			}

			def paymentMethodTypes = AccountTypeDTO.get(accountTypeId).paymentMethodTypes
			def isRealtimePayment = isRealtimePayment(instrumentUser.getPaymentInstruments(), paymentMethodTypes)

            render view: 'edit', model: [           payment : payment,
                                                       user : user,
                                                    displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
                                                   invoices : invoices,
                                         refundablePayments : refundablePayments,
                                                 currencies : retrieveCurrencies(),
                                             paymentMethods : paymentMethodTypes,
                                                  invoiceId : params.int('invoiceId'),
				                            availableFields : retrieveAvailableMetaFields(),
                                          isRealtimePayment : isRealtimePayment,
                                                   payOwned : params.payOwned,
				                         paymentInstruments : allPayments,
                                              accountTypeId : accountTypeId,
                                     submitToPaymentGateway : submitToPaymentGateway,
                     changeSubmitToPaymentGatewayPermission : params.changeSubmitToPaymentGatewayPermission,
                           submitToPaymentGatewayPreference : params.submitToPaymentGatewayPreference ]
            return
        }

        // validation passed, render the confirmation page
        [        payment : payment,
                    user : user,
                 displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
                invoices : invoices,
               isObscure : true,
              currencies : retrieveCurrencies(),
                    submitToPaymentGateway: submitToPaymentGateway,
               invoiceId : params.invoiceId,
         availableFields : retrieveAvailableMetaFields(),
          paymentMethods : AccountTypeDTO.get(accountTypeId).getPaymentMethodTypes(),
      paymentInstruments : toProcess]
    }

    /**
     * Validate and save payment.
     */
    @Secured(["hasAnyRole('PAYMENT_30', 'PAYMENT_31')"])
    @RequiresValidFormToken
    def save () {

        /* Reuse the same payment that was bound earlier during confirm */
        def payment = session['user_payment'];
        //new PaymentWS()
        //bindPayment(payment, params)

        def invoiceId = params.int('invoiceId')

        // save or update
        try {
            if (!payment.id || payment.id == 0) {
                if (SpringSecurityUtils.ifAllGranted("PAYMENT_30")) {
                    def submitToPaymentGateway = params.boolean('submitToPaymentGateway') && payment.methodId != Constants.PAYMENT_METHOD_CHEQUE

                    log.debug("creating payment ${payment} for invoice ${invoiceId}")

                    if (submitToPaymentGateway) {
                        log.debug("processing payment in real time")

                        def authorization = webServicesSession.processPayment(payment, invoiceId)
                        payment.id = authorization.paymentId

                        if (authorization.result) {
                            flash.message = 'payment.successful'
                            flash.args = [ payment.id ]

                        } else {						
							//Strong customer Authentication (SCA)
							def securePaymentWS = authorization.getSecurePaymentWS()
							
							if(securePaymentWS != null){
								if(securePaymentWS.isActionRequired()){
									flash.error = 'payment.failed.auth.required'
								} else if (!securePaymentWS.isSucceeded()){
									flash.error = 'payment.failed.sca'
									flash.args = [authorization.getSecurePaymentWS().getStatus()]
								}
							}else {
								def autorizationMessage = authorization.responseMessage;
								PaymentDTO paymentDTO = PaymentDTO.get(payment.getId())
								if (paymentDTO.resultId== CommonConstants.RESULT_BILLING_INFORMATION_NOT_FOUND){
									autorizationMessage = "Payer Billing Information Not found."
								}else if (autorizationMessage == null ) {
									autorizationMessage = "Payment processor unavailable"
								}
								flash.error = 'payment.failed'
								flash.args = [ payment.id, autorizationMessage ]								
							}
                        }

                    } else {
                        log.debug("entering payment")
                        payment.id = webServicesSession.applyPayment(payment, invoiceId)

                        if (payment.id) {
                            flash.message = 'payment.successful'
                            flash.args = [ payment.id ]

                        } else {
                            flash.error = 'payment.entered.failed'
                            flash.args = [ payment.id ]
                        }
                    }

                } else {
                    render view: '/login/denied'
                    return
                }

            } else {
                if (SpringSecurityUtils.ifAllGranted("PAYMENT_31")) {
                    log.debug("saving changes to payment ${payment.id}")
                    webServicesSession.updatePayment(payment)



                    if (invoiceId) {
                        log.debug("appling payment ${payment} to invoice ${invoiceId}")
                        webServicesSession.createPaymentLink(invoiceId, payment.id)
                    }


                    flash.message = 'payment.updated'
                    flash.args = [ payment.id ]

                } else {
                    render view: '/login/denied'
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.local, e)

            def user = webServicesSession.getUserWS(payment.userId)
            def invoices = getUnpaidInvoices(user.userId)
            List<PaymentDTO> refundablePayments = new PaymentDAS().getRefundablePayments(user.getUserId())

			// collects only those payment method types that are allowed on front end
			def paymentMethodTypes = AccountTypeDTO.get(user.accountTypeId).paymentMethodTypes
			def isRealtimePayment = isRealtimePayment(user.getPaymentInstruments(), paymentMethodTypes)

            render view: 'edit', model: [ payment: payment, user: user, invoices: invoices, currencies: retrieveCurrencies(),
											paymentMethods: paymentMethodTypes, invoiceId: params.int('invoiceId'), availableFields: retrieveAvailableMetaFields(),
											refundablePayments: refundablePayments, refundPaymentId: params.int('payment?.paymentId'),
											paymentInstruments : payment?.getPaymentInstruments(), accountTypeId : user.accountTypeId,
											submitToPaymentGateway : params.submitToPaymentGateway ? true : false, isRealtimePayment: isRealtimePayment ]

			return

        } finally {
            session.removeAttribute("user_payment")

            for (PaymentInformationWS paymentInformationWS: payment.getPaymentInstruments()){
                paymentInformationWS.close();
            }
        }

        chain action: 'list', params: [ id: payment.id ]
    }

    /**
     * Notify about this payment.
     */
    def emailNotify () {

        def pymId= params.id.toInteger()
        try {
            def result= webServicesSession.notifyPaymentByEmail(pymId)
            if (result) {
                flash.info = 'payment.notification.sent'
                flash.args = [ pymId ]
            } else {
                flash.error = 'payment.notification.sent.fail'
                flash.args = [ pymId ]
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.local, e)
        }
        chain action: 'list', params: [ id: pymId]
    }

    def bindPayment(payment, params) {
        if(params.isRefund == 'on' || params.isRefund == '1') {
            params.payment.isRefund = 1
        } else {
            if (params.payment.isRefund == 'on' || params.payment.isRefund == '1'){
                params.payment.isRefund = 1
            }else{
                params.payment.isRefund = 0
            }
        }
        bindData(payment, params, 'payment')

        bindMetaFields(payment, params)

        return payment
    }

    def retrieveCurrencies() {
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

	def retrieveCompanies(){
		def parentCompany = CompanyDTO.get(session['company_id'])
		def childs = CompanyDTO.findAllByParent(parentCompany)
		childs.add(parentCompany)
		return childs;
	}

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session['company_id'], EntityType.PAYMENT);
    }

    def bindMetaFields(paymentWS, params) {
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(), params);
        paymentWS.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

    @Secured(["PAYMENT_34"])
    def history (){
        def payment = PaymentDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(webServicesSession.getPayment(payment.id), Validator.Type.VIEW)

        def currentPayment = auditBL.getColumnValues(payment)
        def paymentVersions = auditBL.get(PaymentDTO.class, payment.getAuditKey(payment.id), versions.max)

        def records = [
                [ name: 'payment', id: payment.id, current: currentPayment, versions: paymentVersions ],
        ]

        render view: '/audit/history', model: [ records: records, historyid: payment.id ]
    }

    def restore (){
        switch (params.record) {
            case "payment":
                def payment = PaymentDTO.get(params.int('id'));

                securityValidator.validateUserAndCompany(webServicesSession.getPayment(payment.id), Validator.Type.EDIT)

                auditBL.restore(payment, payment.id, params.long('timestamp'))

                break;
        }

        chain action: 'history', params: [ id: params.historyid ]
    }

    private void validatePaymentInstrumentMetaFields(PaymentInformationDTO instrument, Integer entityId) {
    	instrument.updatePaymentMethodMetaFieldsWithValidation(entityId, instrument)
    }

	def cardAllowed(paymentMethods) {
		return paymentMethods.find {
			it.id == Constants.PAYMENT_METHOD_VISA ||
			it.id == Constants.PAYMENT_METHOD_VISA_ELECTRON ||
			it.id == Constants.PAYMENT_METHOD_MASTERCARD ||
			it.id == Constants.PAYMENT_METHOD_AMEX ||
			it.id == Constants.PAYMENT_METHOD_DISCOVER ||
			it.id == Constants.PAYMENT_METHOD_DINERS ||
			it.id == Constants.PAYMENT_METHOD_INSTAL_PAYMENT ||
			it.id == Constants.PAYMENT_METHOD_JCB ||
			it.id == Constants.PAYMENT_METHOD_LASER ||
			it.id == Constants.PAYMENT_METHOD_MAESTRO ||
			it.id == Constants.PAYMENT_METHOD_GATEWAY_KEY
		}
	}

	def achAllowed(paymentMethods) {
		return paymentMethods.find { it.id == Constants.PAYMENT_METHOD_ACH }
	}

	def chequeAllowed(paymentMethods) {
		return paymentMethods.find { it.id == Constants.PAYMENT_METHOD_CHEQUE }
	}

	def getAllowedPaymentMethodTypes(accountTypeId, isCardAllowed, isAchAllowed, isChequeAllowed) {
		// collects only those payment method types that are allowed on front end
		def paymentMethodTypes = new ArrayList<PaymentMethodTypeWS>(0)
		def paymentMethodWS = null;
		def typeBL = new PaymentMethodTypeBL();
		for(PaymentMethodTypeDTO dto : AccountTypeDTO.get(accountTypeId).paymentMethodTypes){
			typeBL.setPaymentMethodType(dto);
			paymentMethodWS = typeBL.getWS();
			if(dto.getPaymentMethodTemplate().getTemplateName().equalsIgnoreCase(CommonConstants.PAYMENT_CARD.toString())) {
				if(isCardAllowed) {
					removePaymentLimitMetaField(dto, paymentMethodWS);
					paymentMethodTypes.add(paymentMethodWS)
				}
			} else if(dto.getPaymentMethodTemplate().getTemplateName().equalsIgnoreCase(CommonConstants.ACH.toString())) {
				if(isAchAllowed) {
					removePaymentLimitMetaField(dto, paymentMethodWS);
					paymentMethodTypes.add(paymentMethodWS)
				}
			} else if (dto.getPaymentMethodTemplate().getTemplateName().equalsIgnoreCase(CommonConstants.CHEQUE.toString())) {
				if(isChequeAllowed) {
					paymentMethodTypes.add(paymentMethodWS)
				}
			}
		}

		return paymentMethodTypes;
	}

	def filterAllowedPaymentInstruments(paymentInformations, isCardAllowed, isAchAllowed, isChequeAllowed) {
		List<PaymentInformationWS> paymentInstruments = new ArrayList<PaymentInformationWS>(0)
		PaymentInformationBL piBl = new PaymentInformationBL();
		PaymentInformationDTO converted = null;
		for(PaymentInformationWS paymentInformation : paymentInformations) {
			converted = new PaymentInformationDTO(paymentInformation, session['company_id'])

			if(piBl.isCreditCard(converted)) {
				if(isCardAllowed) {
					removePaymentLimitMetaField(converted, paymentInformation)
					paymentInstruments.add(paymentInformation)
				}
			} else if(piBl.isACH(converted)) {
				if(isAchAllowed) {
					removePaymentLimitMetaField(converted, paymentInformation)
					paymentInstruments.add(paymentInformation)
				}
			} else if (piBl.isCheque(converted)) {
				if(isChequeAllowed) {
					paymentInstruments.add(paymentInformation)
				}
			}
		}

		return paymentInstruments
	}

	private void removePaymentLimitMetaField(PaymentMethodTypeDTO paymentInformation, PaymentMethodTypeWS ws){
			def metaId;
			for(MetaField field: paymentInformation.getMetaFields()){
				if(field.getFieldUsage().contains(MetaFieldType.AUTO_PAYMENT_LIMIT)){
					metaId = field.getId();
					break;
				}
			}
			if(metaId!=null){
				List<MetaFieldWS> temp = new ArrayList<MetaFieldWS>();
				for(MetaFieldWS field: ws.getMetaFields()){
					if(!metaId.equals(field.getId())){
						temp.add(field);
					}
				}
				MetaFieldWS[] array = temp.toArray();
				ws.setMetaFields(array);
			}
		}

	private void removePaymentLimitMetaField(PaymentInformationDTO paymentInformation, PaymentInformationWS ws){
		def metaId;
		for(MetaFieldValue value: paymentInformation.getMetaFields()){
			if(MetaFieldHelper.isValueOfType(value, MetaFieldType.AUTO_PAYMENT_LIMIT)){
				metaId = value.getField().getName();
				break;
			}
		}
		if(metaId!=null){
			List<MetaFieldValueWS> temp = new ArrayList<MetaFieldValueWS>();
			for(MetaFieldValueWS value: ws.getMetaFields()){
				if(!metaId.equals(value.getFieldName())){
					temp.add(value);
				}
			}
			MetaFieldValueWS[] array = temp.toArray();
			ws.setMetaFields(array);
		}
	}

    private void categorizePayments(
            List<PaymentInformationWS> allPayments, List<PaymentInformationWS> toProcess,
            PaymentWS payment, UserWS instrumentUser) {

        PaymentMethodDAS pmDas = new PaymentMethodDAS()
		PaymentInformationBL piBl = new PaymentInformationBL()
		PaymentInformationDTO instrument = null;
		for(PaymentInformationWS instrumentWS : instrumentUser.getPaymentInstruments()) {

			allPayments.add(instrumentWS)
			if(instrumentWS.getProcessingOrder() != null && instrumentWS.getProcessingOrder() != 0) {
				toProcess.add(instrumentWS)
				instrument = new PaymentInformationDTO(instrumentWS, session['company_id'])
				boolean ispaymentInfoAlreadyPresent = new PaymentInformationDAS().exists(instrumentUser?.userId, instrument?.paymentMethodType?.id);
				if (!ispaymentInfoAlreadyPresent) {
					instrument.setPaymentMethod(null);
					instrumentWS.setPaymentMethodId(piBl.getPaymentMethodForPaymentMethodType(instrument));
				}
				// set payment method if there is none defined
				if(instrumentWS.getPaymentMethodId() == null || instrumentWS.getPaymentMethodId() == 0) {
					instrumentWS.setPaymentMethodId(piBl.getPaymentMethodForPaymentMethodType(instrument))
				}
				validatePaymentInstrumentMetaFields(instrument, instrumentUser.entityId)
				payment.getPaymentInstruments().add(instrumentWS)
			}
		}

		// sort payment instruments with respect to processing order
		Collections.sort(payment.getPaymentInstruments(), PaymentInformationWS.ProcessingOrderComparator)
	}

    def addPaymentInstrument (){
        def user = new UserWS()
        UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)

        def accountType = AccountTypeDTO.get(params.int("accountTypeId"))

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.EDIT)

        // show only recurring payment methods
        def paymentMethods = accountType?.paymentMethodTypes
        // add a new payment instrument
        PaymentInformationWS paymentInstrument = new PaymentInformationWS()
        paymentInstrument.setPaymentMethodTypeId(paymentMethods?.iterator().next().id)

        user.paymentInstruments.add(paymentInstrument)

        render template: '/payment/paymentMethods', model: [paymentMethods : paymentMethods , paymentInstruments : user.paymentInstruments , accountTypeId : accountType?.id]
    }

    def refreshPaymentInstrument (){
        int currentIndex = params.int("currentIndex")

        def user = new UserWS()
        UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)

        def accountType = AccountTypeDTO.get(params.int("accountTypeId"))

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        def paymentMethods = accountType?.paymentMethodTypes

        def isRealtimePayment = isRealtimePayment(user.paymentInstruments, paymentMethods)
        render template: '/payment/paymentMethods', model: [paymentMethods                        : paymentMethods,
                                                            paymentInstruments                    : user.paymentInstruments,
                                                            accountTypeId                         : accountType?.id,
                                                            isRealtimePayment                     : isRealtimePayment,
                                                           changeSubmitToPaymentGatewayPermission : params.changeSubmitToPaymentGatewayPermission,
                                                            submitToPaymentGatewayPreference      : params.submitToPaymentGatewayPreference]
    }

    def removePaymentInstrument (){
        def currentIndex = params.int("currentIndex")

        def user = new UserWS()
        UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)

        def accountType = AccountTypeDTO.get(params.int("accountTypeId"))

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.EDIT)

        def paymentMethods = accountType?.paymentMethodTypes

        PaymentInformationWS removed = user.paymentInstruments.remove(currentIndex)
        log.debug("user instrument is: " + user.paymentInstruments)

        removed.close()
        def isRealtimePayment = isRealtimePayment(user.paymentInstruments, paymentMethods)
        render template: '/payment/paymentMethods', model: [paymentMethods  : paymentMethods ,
                                                        paymentInstruments  : user.paymentInstruments ,
                                                        isRealtimePayment   : isRealtimePayment,
                                                        accountTypeId       : accountType?.id]
    }

    def isRealtimePayment(paymentInstruments, paymentMethodTypes) {
        def isRealtimePayment = false

        if (paymentInstruments) {
            paymentInstruments.each { instrument ->
                def paymentMethodTemplateName = PaymentMethodTypeDTO.get(instrument.paymentMethodTypeId).paymentMethodTemplate.templateName
                if (paymentMethodTemplateName == CommonConstants.PAYMENT_CARD ||
					paymentMethodTemplateName == CommonConstants.ACH) {
                    isRealtimePayment = true
                }
            }
        } else {
            if (paymentMethodTypes) {
                isRealtimePayment = paymentMethodTypes.first()?.paymentMethodTemplate?.templateName == CommonConstants.PAYMENT_CARD
			}
        }

        return isRealtimePayment
    }

    @Secured(["hasAnyRole('PAYMENT_30', 'PAYMENT_31')"])
    def transfer () {
        def payment
        def user

        try {
            payment = params.id ? webServicesSession.getPayment(params.int('id')) : new PaymentWS()

            if (payment?.deleted==1) {
                paymentNotFoundErrorRedirect(params.id)
                return
            }

            if (params.id) {
                PaymentBL paymentBL = new PaymentBL(params.int("id"))
                if (paymentBL.ifRefunded()) {
                    flash.error = 'validation.error.update.refunded.payment'
                    flash.args = [params.id]
                    redirect controller: 'payment', action: 'list'
                    return
                }
            }

            user = webServicesSession.getUserWS(payment?.userId ?: params.int('userId'))

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
            paymentNotFoundErrorRedirect(params.id)
            return
        }

        securityValidator.validateUserAndCompany(payment, Validator.Type.EDIT)

        [ payment: payment, user: user, currencies: retrieveCurrencies() ]
    }

    def bindPaymentTransfer(paymentTransfer, params) {
        bindData(paymentTransfer, params)
        paymentTransfer.fromUserId = Integer.valueOf(params?.payment?.userId)
        paymentTransfer.paymentId = Integer.valueOf(params?.payment?.id)
        paymentTransfer.createdBy = session['company_id']
        paymentTransfer.paymentTransferNotes = params?.paymentTransferNotes
    }

    @Secured(["hasAnyRole('PAYMENT_30', 'PAYMENT_31')"])
    def confirmTransfer () {

        PaymentTransferWS paymentTransfer = new PaymentTransferWS()
        bindPaymentTransfer(paymentTransfer, params)
        try {
            webServicesSession.transferPayment(paymentTransfer)

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            def payment = params?.payment?.id? webServicesSession.getPayment(params?.payment?.id?.toInteger()) : new PaymentWS()
            def user = webServicesSession.getUserWS(payment?.userId ?: params.int('userId'))
            render view: 'transfer', model: [payment: payment, user: user, currencies: retrieveCurrencies()]
            return
        }

        flash.message = 'prompt.payment.transfer.successfully'
        flash.args = [paymentTransfer.toUserId]
        chain action: 'list', params: [ id: paymentTransfer.paymentId]

    }

	def getPreference(Integer preferenceTypeId){
		def preferenceValue = webServicesSession.getPreference(preferenceTypeId).getValue()
		return !StringUtils.isEmpty(preferenceValue) ? Integer.valueOf(preferenceValue) : new Integer(0)
	}
}