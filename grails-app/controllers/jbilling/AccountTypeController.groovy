package jbilling

import com.sapienter.jbilling.client.pricing.util.PlanHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.account.AccountTypeBL
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.PlanItemWS
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroupDAS
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.pricing.PriceModelBL
import com.sapienter.jbilling.server.pricing.PriceModelWS
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.AccountInformationTypeWS
import com.sapienter.jbilling.server.user.AccountTypeWS
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.item.db.PlanItemDTO
import com.sapienter.jbilling.server.user.db.AccountTypePriceDTO
import com.sapienter.jbilling.server.user.db.AccountTypePricePK
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.Util

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.CriteriaSpecification
import org.joda.time.DateTime

@Secured(["isAuthenticated()", "MENU_99"])
class AccountTypeController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields =
            ['typeId': 'id']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def viewUtils

    def productService
    def companyService
    SecurityValidator securityValidator

    def index() {
        list()
    }

    def list() {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        getList(params)
    }

    def findAccountTypes() {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def accountTypes = getAccountTypesForEntity(params)

        try {
            def jsonData = getAccountTypesJsonData(accountTypes, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts AccountTypes to JSon
     */
    private def Object getAccountTypesJsonData(accounts, GrailsParameterMap params) {
        def jsonCells = accounts
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows) : 1
        def totalRecords = jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def getList(params) {

        def accountType = AccountTypeDTO.get(params.int('id'))

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        if (params.id?.isInteger() && !accountType) {
            flash.error = 'orderPeriod.not.found'
            flash.args = [params.id as String]
        }
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def accountTypes = getAccountTypesForEntity(params)
        if (params.applyFilter || params.partial) {
            render template: 'accountTypes', model: [accountTypes: accountTypes, selected: accountType]
        } else {
            if (chainModel) {
                def cp = chainModel
                render view: 'list', model: [selected: accountType, accountTypes: accountTypes] + chainModel
            } else
                render view: 'list', model: [accountTypes: accountTypes, selected: accountType]
        }
    }

    private def getAccountTypesForEntity(GrailsParameterMap params) {
        params.max    = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort   = params?.sort ?: pagination.sort
        params.order  = params?.order ?: pagination.order
        params.sort   = params?.sort ?: pagination.sort
        params.order  = params?.order ?: pagination.order

        def languageId = session['language_id']

        return AccountTypeDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(session['company_id']))
            if (params.typeId) {
                def searchParam = params.typeId
                if (searchParam.isInteger()) {
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
                                              ?)
                                            and a.language_id = ?
                                            and a.psudo_column = 'description'
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_ACCOUNT_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def invalid() {
        render view: 'edit', model: [
                accountType: params.accountTypeWS,
                company    : params.company
        ]
    }

    def edit() {
        def accountType
        if (params.id) {
            def oldAccountType = AccountTypeDTO.get(params.int('id'))
            securityValidator.validateCompany(oldAccountType?.company?.id, Validator.Type.EDIT)

            accountType = webServicesSession.getAccountType(params.int('id'))
            if (!accountType) {
                return response.sendError(Constants.ERROR_CODE_404)
            }
        } else {
            accountType = new AccountTypeWS()
        }
        def periodUnits = PeriodUnitDTO.list()

        def orderPeriods = OrderPeriodDTO.createCriteria().list() {
            eq('company', CompanyDTO.get(session['company_id']))
        }
        log.debug "Order Period is: " + orderPeriods
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? accountType?.getDescription(session['language_id']) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription?.content)

        def selectedPaymentMethodTypeIds
        def globalPaymentMethodIds
        globalPaymentMethodIds = PaymentMethodTypeDTO.findAllByAllAccountTypeAndEntity(true, CompanyDTO.get(session['company_id']))*.id
        selectedPaymentMethodTypeIds = accountType.paymentMethodTypeIds?.toList()
        render view: 'edit', model: [
                clone                       : params.clone,
                accountType                 : accountType,
                company                     : retrieveCompany(),
                periodUnits                 : periodUnits,
                orderPeriods                : orderPeriods,
                currencies                  : retrieveCurrencies(),
                selectedPaymentMethodTypeIds: selectedPaymentMethodTypeIds,
                globalPaymentMethodIds      : globalPaymentMethodIds,
                paymentMethodTypes          : PaymentMethodTypeDTO.findAllByEntity(CompanyDTO.get(session['company_id'])),
                invoiceTemplate             : getInvoiceTemplates()
        ]
    }

    @RequiresValidFormToken
    def save() {
        AccountTypeWS ws = new AccountTypeWS();
        bindData(ws, params)
        log.debug ws
        if (params.description) {
            InternationalDescriptionWS descr =
                    new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
            log.debug descr
            ws.descriptions.add descr
        }
        ws.setEntityId(session['company_id'].toInteger())
        if (params.mainSubscription) {
            def mainSubscription = new MainSubscriptionWS()
            bindData(mainSubscription, params, 'mainSubscription')
            ws.setMainSubscription(mainSubscription)
            log.debug("Main Subscrption ${mainSubscription}")
        }

        def periodUnits = PeriodUnitDTO.list()
        def orderPeriods = OrderPeriodDTO.createCriteria().list() {
            eq('company', CompanyDTO.get(session['company_id']))
        }
        def globalPaymentMethodIds = PaymentMethodTypeDTO.findAllByAllAccountTypeAndEntity(true, CompanyDTO.get(session['company_id']))*.id
        if (globalPaymentMethodIds) {
            ws.paymentMethodTypeIds = ws.paymentMethodTypeIds.plus(globalPaymentMethodIds);
        }

        if (ws.id) {
            def oldAccountType = AccountTypeDTO.get(ws.id)
            securityValidator.validateCompany(oldAccountType?.company?.id, Validator.Type.EDIT)
        }

        try {
            if (params.clone == "true") {
                ws.setId(null);
                webServicesSession.createAccountType(ws);
                flash.message = 'config.account.type.created'
            } else if (!params.clone && ws.id) {
                webServicesSession.updateAccountType(ws);
                flash.message = 'config.account.type.updated'
            } else {
                webServicesSession.createAccountType(ws);
                flash.message = 'config.account.type.created'
            }
            if (!ws.paymentMethodTypeIds && globalPaymentMethodIds.size == 0) {
                flash.info = g.message(code: 'accountTypeWS.info.for.customer', default: 'Any customers created with this Account Type will not have a Payment Method.')
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            def selectedPaymentMethodTypeIds
            selectedPaymentMethodTypeIds = ws.paymentMethodTypeIds?.toList()
            render view: 'edit', model: [
                    clone                       : params.clone,
                    accountType                 : ws,
                    company                     : retrieveCompany(),
                    periodUnits                 : periodUnits,
                    orderPeriods                : orderPeriods,
                    selectedPaymentMethodTypeIds: selectedPaymentMethodTypeIds,
                    globalPaymentMethodIds      : globalPaymentMethodIds,
                    currencies                  : retrieveCurrencies(),
                    paymentMethodTypes          : PaymentMethodTypeDTO.findAllByEntity(CompanyDTO.get(session['company_id'])),
                    selectedPaymentMethodTypeIds: ws?.paymentMethodTypeIds?.toList()
            ]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.account.type.saving.error'
        }
        redirect(action: 'list')
    }

    def show() {
        def accountType = AccountTypeDTO.get(params.int('id'))

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, accountType.id, accountType.getDescription(session['language_id']))

        render template: 'accountType', model: [selected: accountType]
    }

    def delete() {
        log.debug 'delete called on ' + params.id
        if (params.id) {
            def accountType = AccountTypeDTO.get(params.int('id'))

            securityValidator.validateCompany(accountType?.company?.id, Validator.Type.EDIT)

            if (accountType) {
                try {
                    boolean retVal = webServicesSession.deleteAccountType(params.id?.toInteger());
                    if (retVal) {
                        flash.message = 'config.account.type.delete.success'
                        flash.args = [params.id]
                    } else {
                        flash.info = 'config.account.type.delete.failure'
                    }
                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.account.type.delete.error'
                }
            }
        }
        params.applyFilter = false
        params.id = null
        getList(params)
    }

    def listPrices() {

        if (params.id) {
            def accountType = params.id ? AccountTypeDTO.get(params.int('id')) : null
            if (!accountType) {
                return response.sendError(Constants.ERROR_CODE_404)
            }

            if (accountType) {
                securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)
            }

            // all account type prices and products
            def company = CompanyDTO.get(session['company_id'])
            def itemTypes = getProductCategories()

            //initialize pagination parameters
            params.max = params?.max?.toInteger() ?: pagination.max
            params.offset = params?.offset?.toInteger() ?: pagination.offset
            params.filterBy = params?.filterBy ?: ""

            def products = productService.getFilteredProducts(company, params, accountType, true, true)
            def prices = new AccountTypePriceBL(accountType?.id).getAccountTypePrices()
            def product = params.('itemId') ? ItemDTO.get(params.int('itemId')) : null
            def priceExpiryMap = mapPriceWithExpiryDate(accountType.id, prices)

            def crumbDescription = params.id ? accountType?.getDescription(session['language_id']) : null
            breadcrumbService.addBreadcrumb(controllerName, 'listPrices', 'list', params.int('id'), crumbDescription)

            render view: 'accountTypePrices', model: [accountType   : accountType,
                                                      prices        : prices,
                                                      product       : product,
                                                      products      : products,
                                                      company       : company,
                                                      itemTypes     : itemTypes,
                                                      currencies    : retrieveCurrencies(),
                                                      priceExpiryMap: priceExpiryMap]
        } else {

            flash.error = 'config.account.type.prices.error'
            params.applyFilter = true
            params.id = null
            getList(params)
        }
    }

    // Account Type pricing
    def filterProducts() {
        def company = CompanyDTO.get(session['company_id'])
        def itemTypes = getProductCategories()
        def accountTypeId = params.typeId ? params.int('typeId') : params.int('accountTypeId')
        def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) : null

        if (accountType) {
            securityValidator.validateUserAndCompany(AccountTypeBL.getWS(accountType), Validator.Type.VIEW)
        }

        //initialize pagination parameters
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.filterBy = params?.filterBy ?: ""

        def products = productService.getFilteredProducts(company, params, accountType, true, true)

        render template: 'products', model: [itemTypes: itemTypes, products: products]
    }

    def productPrices() {
        def itemId = params.int('id')
        def prices = new AccountTypePriceBL(params.int('accountTypeId')).getAccountTypePrices(itemId)
        def product = ItemDTO.get(itemId)

        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global)

        def accountTypeId = params.int('accountTypeId')

        log.debug("*************** Account Type Id ${params.int('accountTypeId')} and prices ${prices}")

        def priceExpiryMap = mapPriceWithExpiryDate(accountTypeId, prices)

        log.debug("*************** expiry dates priceExpiryMap ${priceExpiryMap}")

        log.debug("prices for account Type ${params.accountTypeId} and item ${itemId}, ${prices.size()}")

        render template: 'prices', model: [prices: prices, product: product, accountTypeId: params.accountTypeId, priceExpiryMap: priceExpiryMap]
    }

    private Map mapPriceWithExpiryDate(int accountTypeId, List<PlanItemDTO> prices) {
        def priceExpiryMap = [:]
        if (accountTypeId && prices) {
            for (PlanItemDTO price : prices) {
                def accountTypePricePk = new AccountTypePricePK()
                accountTypePricePk.setAccountType(AccountTypeDTO.findById(accountTypeId))
                accountTypePricePk.setPlanItem(price)
                AccountTypePriceDTO accTypPrice = AccountTypePriceDTO.findById(accountTypePricePk)
                priceExpiryMap.put(price.id, accTypPrice.getPriceExpiryDate())
            }
        }
        priceExpiryMap
    }

    def allProductPrices() {
        def accountTypeId = params.int('accountTypeId')
        def accountType = AccountTypeDTO.get(accountTypeId)

        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        def prices = new AccountTypePriceBL(accountType).getAccountTypePrices()

        def priceExpiryMap = mapPriceWithExpiryDate(accountTypeId, prices)

        render template: 'prices', model: [prices: prices, accountTypeId: accountTypeId, priceExpiryMap: priceExpiryMap]
    }

    /**
     * Get the price to be edited and show the 'editAccountTypePrice.gsp' view. If no ID is given
     * this screen will allow creation of a new account type-specific price.
     */
    def editAccountTypePrice() {

        def product
        try {
            product = webServicesSession.getItem(params.int('itemId'), null, null)
        } catch (SessionInternalError sie) {
            return response.sendError(Constants.ERROR_CODE_404)
        }
        def accountTypeId = params.int('accountTypeId')
        def accountType = webServicesSession.getAccountType(accountTypeId)
        if (!accountType) {
            return response.sendError(Constants.ERROR_CODE_404)
        }
        securityValidator.validateCompany(accountType?.entityId, Validator.Type.VIEW)

        PlanItemWS price
        def priceExpiryDate

        if (params.id) {
            def priceId = params.int('id')
            if (!priceId) {
                return response.sendError(Constants.ERROR_CODE_404)
            }
            price = getAccountPrice(accountTypeId, priceId)
            def accountTypePricePk = new AccountTypePricePK()
            accountTypePricePk.setAccountType(AccountTypeDTO.get(accountTypeId))
            accountTypePricePk.setPlanItem(PlanItemDTO.findById(priceId))
            AccountTypePriceDTO accTypPrice = AccountTypePriceDTO.findById(accountTypePricePk)
            priceExpiryDate = accTypPrice.getPriceExpiryDate()
        } else {
            // copy default product price model as a starting point
            def priceModel = product.defaultPrice
            priceModel?.id = null;

            price = new PlanItemWS()
            price.addModel(CommonConstants.EPOCH_DATE, priceModel);
        }

        [price: price, product: product, accountType: accountType, currencies: retrieveCurrencies(), hideSaveOption: true, priceExpiryDate: priceExpiryDate]
    }

    def PlanItemWS getAccountPrice(accountTypeId, priceId) {
        def prices
        try {
            prices = webServicesSession.getAccountTypePrices(accountTypeId)
            return prices.find { it.id == priceId }
        } catch (SessionInternalError sie) {
            return response.sendError(Constants.ERROR_CODE_404)
        }
    }

    def updateStrategy() {
        if (params."price.id") {
            def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
            securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)
        }

        def price = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)

        render template: '/priceModel/model', model: [model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies()]
    }

    def addChainModel() {
        if (params."price.id") {
            def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
            securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)
        }

        def price = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)

        // add new price model to end of chain
        def model = priceModel
        while (model.next) {
            model = model.next
        }
        model.next = new PriceModelWS();

        render template: '/priceModel/model', model: [model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies()]
    }

    def removeChainModel() {
        if (params."price.id") {
            def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
            securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)
        }

        def price = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)

        def modelIndex = params.int('modelIndex')

        // remove price model from the chain
        def model = priceModel
        for (int i = 1; model != null; i++) {
            if (i == modelIndex) {
                model.next = model.next?.next
                break
            }
            model = model.next
        }

        render template: '/priceModel/model', model: [model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies()]
    }

    def addAttribute() {
        if (params."price.id") {
            def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
            securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)
        }

        def price = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)

        def modelIndex = params.int('modelIndex')
        def attribute = message(code: 'plan.new.attribute.key', args: [params.attributeIndex])

        // find the model in the chain, and add a new attribute
        def model = priceModel
        for (int i = 0; model != null; i++) {
            if (i == modelIndex) {
                model.attributes.put(attribute, '')
            }
            model = model.next
        }

        render template: '/priceModel/model', model: [model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies()]
    }

    def removeAttribute() {
        if (params."price.id") {
            def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
            securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)
        }

        def price = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)

        def modelIndex = params.int('modelIndex')
        def attributeIndex = params.int('attributeIndex')

        // find the model in the chain, remove the attribute
        def model = priceModel
        for (int i = 0; model != null; i++) {
            if (i == modelIndex) {
                def name = params["model.${modelIndex}.attribute.${attributeIndex}.name"]
                model.attributes.remove(name)
            }
            model = model.next
        }

        render template: '/priceModel/model', model: [model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies()]
    }

    /**
     * Validate and save a account type-specific price.
     */
    @RequiresValidFormToken
    def saveAccountTypePrice() {
        def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
        securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)

        def oldPrice = params."price.id" ? getAccountPrice(params.int('accountTypeId'), params.int('price.id')) : null
        def price = new PlanItemWS()
        bindData(price, params, 'price')

        def priceModel = PlanHelper.bindPriceModel(params)
        try {
            String errorString = "PriceModelWS,startDate,validation.error.pricemodel.startdate,${message(code: 'date.format').toString()}"
            DateTime startDate = Util.getParsedDateOrThrowError(message(code: 'date.format').toString(), params.startDate.toString(), errorString)
            price.models.put(startDate.toDate(), priceModel)

            Date priceExpiryDt = null
            String expiryErrorString = ",,validation.error.accountTypePrice.priceExpiryDate,${message(code: 'date.format').toString()}"
            if (params.priceExpiryDate) {
                DateTime expiryDate = Util.getParsedDateOrThrowError(message(code: 'date.format').toString(), params.priceExpiryDate.toString(), expiryErrorString)
                log.debug("For expiryDate ${expiryDate.withTimeAtStartOfDay().toDate()}")
                priceExpiryDt = expiryDate.withTimeAtStartOfDay().toDate()
            }

            if (!price.id || price.id == 0) {
                log.debug("creating account type ${accountType.id} specific price ${price}")

                PriceModelBL.validateWsAttributes(priceModel)
                price = webServicesSession.createAccountTypePrice(accountType.id, price, priceExpiryDt);

                flash.message = 'created.account.type.price'
                flash.args = [price.itemId as String]
            } else {
                log.debug("updating account type ${accountType.id} specific price ${price.id}")

                webServicesSession.updateAccountTypePrice(accountType.id, price, priceExpiryDt);

                flash.message = 'updated.account.type.price'
                flash.args = [price.itemId as String]
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            def itemId = params.int('itemId') ?: params.int('price.itemId')
            def product = webServicesSession.getItem(itemId, null, null)
            render view: 'editAccountTypePrice', model: [price: price, product: product, accountType: accountType, currencies: retrieveCurrencies(), priceExpiryDate: params.priceExpiryDate]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'account.type.price.save.error'
            def product = webServicesSession.getItem(params.int('price.itemId'), null, null)
            render view: 'editAccountTypePrice', model: [price: price, product: product, accountType: accountType, currencies: retrieveCurrencies(), priceExpiryDate: params.priceExpiryDate]
            return
        }

        chain controller: 'accountType', action: 'listPrices', params: [id: accountType.id, itemId: price.itemId]
    }

    /**
     * Deletes a account type-specific price.
     */
    def deleteAccountTypePrice() {
        def accountType = webServicesSession.getAccountType(params.int('accountTypeId'))
        securityValidator.validateCompany(accountType?.entityId, Validator.Type.EDIT)

        def accountTypeId = params.int('accountTypeId')
        def planItemId = params.int('id')
        def itemId = params.int('itemId')

        try {
            log.debug("deleting account type ${accountTypeId} price ${planItemId}")

            webServicesSession.deleteAccountTypePrice(accountTypeId, planItemId)

            flash.message = 'deleted.account.type.price'
            flash.args = [params.itemId as String]

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        // render remaining prices for the priced product
        def prices = new AccountTypePriceBL(params.int('accountTypeId')).getAccountTypePrices(itemId)
        def priceExpiryMap = mapPriceWithExpiryDate(accountTypeId, prices)
        def product = ItemDTO.get(itemId)

        render template: 'prices', model: [prices: prices, product: product, accountTypeId: params.accountTypeId, priceExpiryMap: priceExpiryMap]
    }

    def showAIT() {

        AccountInformationTypeDTO ait = AccountInformationTypeDTO.get(params.int('id'))
        securityValidator.validateCompany(ait?.accountType?.company?.id, Validator.Type.VIEW)

        AccountTypeDTO accountType = AccountTypeDTO.get(params.int('accountTypeId').toInteger())
        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        if (!ait) {
            log.debug "redirecting to list"
            redirect(action: 'listAIT')
            return
        }

        if (params.template) {
            // render requested template
            render template: params.template, model: [selected: ait, accountType: accountType]

        } else {

            //search for AITs for the selected account type
            def aits = AccountInformationTypeDTO.createCriteria().list(
                    max   : params.max,
                    offset: params.offset,
                    sort  : params.sort,
                    order : params.order
            ) {
                eq("accountType.id", accountType?.id.toInteger())
            }

            render view: 'listAIT', model: [selected: ait, aits: aits, accountType: accountType]
        }
    }

    def deleteAIT() {

        def accountInformationTypeId = params.int('id')
        log.debug 'AIT delete called on ' + accountInformationTypeId

        AccountInformationTypeWS aitWS = webServicesSession.getAccountInformationType(accountInformationTypeId)
        securityValidator.validateCompany(aitWS?.entityId, Validator.Type.EDIT)

        try {
            webServicesSession.deleteAccountInformationType(params.id?.toInteger());
            flash.message = 'config.account.information.type.delete.success'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.account.information.type.delete.error'
        }

        params.id = null
        listAIT()

    }

    def listAIT() {

        params.max    = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort   = params?.sort ?: pagination.sort
        params.order  = params?.order ?: pagination.order

        def accountType = AccountTypeDTO.get(params.int('accountTypeId'))
        securityValidator.validateCompany(accountType?.company?.id, Validator.Type.VIEW)

        def ait = AccountInformationTypeDTO.get(params.int('id'))
        securityValidator.validateCompany(ait?.accountType?.company?.id, Validator.Type.VIEW)

        def aits = webServicesSession.getInformationTypesForAccountType(params.accountTypeId.toInteger())

        if (params?.applyFilter || params?.partial) {
            render template: 'accountInformationTypes', model: [aits: aits, accountType: accountType, selected: ait]
        } else {
            render view: 'listAIT', model: [aits: aits, accountType: accountType, selected: ait]
        }
    }

    def editAITFlow = {

        initialize {
            action {

                AccountInformationTypeWS ait = params.id ? webServicesSession.getAccountInformationType(params.int('id')) :
                        new AccountInformationTypeWS();
                securityValidator.validateCompany(ait.entityId, Validator.Type.EDIT)

                if (!ait) {
                    log.error("Could not fetch WS object")
                    aitNotFoundErrorRedirect(params.id, params.accountTypeId)
                    return
                }

                def accountType = AccountTypeDTO.get(params.int('accountTypeId'))
                securityValidator.validateCompany(accountType?.company?.id, Validator.Type.EDIT)
                def company = CompanyDTO.get(session['company_id'])
                def currencies = new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(),
                        session['company_id'].toInteger(), true)

                // set sensible defaults for new ait
                if (!ait.id || ait.id == 0) {
                    ait.accountTypeId = accountType?.id
                    ait.entityType = EntityType.ACCOUNT_TYPE
                    ait.entityId = session['company_id'].toInteger()
                    ait.metaFields = []
                }


                if (params.clone == "true") {
                    ait.setId(0);
                    if (ait.getMetaFields() != null) {
                        for (MetaFieldWS mf : ait.getMetaFields()) {
                            mf.setId(0);
                            mf.setPrimary(false);
                        }
                    }
                }

                // available metafields and metafield groups
                def metaFields = retrieveMetaFieldsForAccountType()
                def metaFieldGroups = retrieveMetaFieldGroupsForAccountType()

                conversation.acctInfoType = removeUsedAit(ait)

                // model scope for this flow
                flow.accountType = accountType
                flow.company = company
                flow.currencies = currencies

                // conversation scope
                conversation.ait = ait
                conversation.metaFieldGroups = metaFieldGroups
                conversation.metaFields = metaFields

                List<AccountInformationTypeDTO> infoTypes = accountType?.informationTypes?.sort { it.displayOrder }
                List<MetaFieldWS> metaFieldWSes = []
                com.sapienter.jbilling.server.metafields.MetaFieldBL metaFieldBL = new MetaFieldBL();
                infoTypes.each {
                    it.metaFields.each { MetaField metaField ->
                        metaFieldWSes.add(metaFieldBL.getWS(metaField))
                    }
                }

                conversation.unSavedMetafields = metaFieldWSes
                if (ait.metaFields != null) {
                    ait.metaFields.each {
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
         * Renders the ait details tab panel.
         */
        showDetails {
            action {
                params.template = 'detailsAIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafields tab panel, containing all the metafields that can be imported
         */
        showMetaFields {
            action {
                params.template = 'metafieldsAIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafield groups tab panel, containing all the metafield groups that can be used as a template
         * for creation of the account information type
         */
        showMetaFieldGroups {
            action {
                params.template = 'metafieldGroupsAIT'
            }
            on("success").to("build")
        }

        /**
         *  Imports the selected metafield groups for using as a template for account information type creation
         *  Is available only for creating new information types
         */
        importFromMetaFieldGroup {
            action {

                def metaFieldGroupId = params.int('id')
                def metaFieldGroup = webServicesSession.getMetaFieldGroup(metaFieldGroupId)

                if (!metaFieldGroup) {
                    params.template = 'reviewAIT'
                    error()
                } else {

                    def ait = conversation.ait

                    ait.name = metaFieldGroup.getDescription()
                    ait.displayOrder = metaFieldGroup.displayOrder
                    ait.metaFields = []

                    def metaFields = ait.metaFields as List
                    metaFields.addAll(metaFieldGroup.metaFields as List)
                    metaFields.each {
                        it.setId(0)
                        it.setPrimary(false);
                    }

                    ait.metaFields = metaFields.toArray()

                    conversation.ait = ait

                    params.newLineIndex = metaFields.size() - 1
                    params.template = 'reviewAIT'
                }
            }
            on("success").to("build")
            on("error").to("build")
        }

        /**
         * Adds a metafield to the account information type
         */
        addAITMetaField {
            action {

                def metaFieldId = params.int('id')

                def metaField = metaFieldId ? webServicesSession.getMetaField(metaFieldId) :
                        new MetaFieldWS();

                metaField.primary = false

                if (metaField?.id || metaField.id != 0) {
                    // set metafield defaults
                    metaField.id = 0
                } else {
                    metaField.entityType = EntityType.ACCOUNT_TYPE
                    metaField.entityId = session['company_id'].toInteger()
                }

                metaField.fakeId = conversation.nextId - 1
                metaField.id = metaField.fakeId
                conversation.nextId--

                // add metafield to ait
                def ait = conversation.ait
                def metaFields = ait.metaFields as List
                metaFields.add(metaField)
                ait.metaFields = metaFields.toArray()

                conversation.ait = ait

                conversation.acctInfoType = removeUsedAit(ait)

                params.newLineIndex = metaFields.size() - 1
                params.dependencyCheckBox = true
                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Updates an metafield  and renders the AIT metafields panel
         */
        updateAITMetaField {
            action {

                flash.errorMessages = null
                flash.error = null
                def ait = conversation.ait
                //dependency CheckBox visible at the time of update ait meta field
                params.dependencyCheckBox = true
                // get existing metafield
                def index = params.int('index')
                def metaField = ait.metaFields[index]
                if (!bindMetaFieldData(metaField, params, index)) {
                    error()
                }

                if (null != metaField.fieldUsage && metaField.fieldUsage.equals(MetaFieldType.COUNTRY_CODE)) {
                    flash.errorMessages = [message(code: 'countryCode.warning.message')];
                }

                if (!params.get("dependency-checkbox")) {
                    metaField.dependentMetaFields = null;
                }
                if (!params.get("help-checkbox")) {
                    metaField.helpContentURL = null;
                    metaField.helpDescription = null;
                }
                // add metafield to the ait
                ait.metaFields[index] = metaField

                // sort metafields by displayOrder
                ait.metaFields = ait.metaFields.sort { it.displayOrder }
                conversation.ait = ait
                def unSavedMetafields = conversation.unSavedMetafields.findAll {
                    !ait.metaFields.id.contains(it.id)
                } + ait.metaFields.findAll { StringUtils.trimToNull(it.name) != null } ?: []
                conversation.unSavedMetafields = unSavedMetafields

                conversation.acctInfoType = removeUsedAit(ait)

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Remove a metafield from the information type  and renders the AIT metafields panel
         */
        removeAITMetaField {
            action {

                //dependency CheckBox visible at the time of remove ait meta field
                params.dependencyCheckBox = true
                def ait = conversation.ait

                def index = params.int('index')
                def metaFields = ait.metaFields as List

                def metaField = metaFields.get(index)
                metaFields.remove(index)
                conversation.removedMetaFields << metaField

                ait.metaFields = metaFields.toArray()

                conversation.ait = ait

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Updates account information type attributes
         */
        updateAIT {
            action {

                //dependency CheckBox visible at the time of update ait
                params.dependencyCheckBox = true
                def ait = conversation.ait
                bindData(ait, params)

                ait.metaFields = ait.metaFields.sort { it.displayOrder }
                conversation.ait = ait

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Shows the account information type metafield builder.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            on("details").to("showDetails")
            on("metaFields").to("showMetaFields")
            on("metaFieldGroups").to("showMetaFieldGroups")
            on("addMetaField").to("addAITMetaField")
            on("importMetaFieldGroup").to("importFromMetaFieldGroup")
            on("updateMetaField").to("updateAITMetaField")
            on("removeMetaField").to("removeAITMetaField")
            on("update").to("updateAIT")

            on("save").to("saveAIT")

            on("cancel").to("finish")
        }

        /**
         * Saves the account information type and exits the builder flow.
         */
        saveAIT {
            action {
                try {

                    //dependency CheckBox visible at the time of save ait meta field
                    params.dependencyCheckBox = true
                    def ait = conversation.ait
                    Set<MetaField> metaFields = ait.metaFields
                    Set<String> mfNames = metaFields*.name
                    if (metaFields.size() != mfNames.size()) {
                        throw new SessionInternalError("MetaField", ["AccountInformationTypeDTO,metafield,metaField.name.exists"] as String[])
                    }

                    if (!ait.name) {
                        ait.descriptions.add(new InternationalDescriptionWS())
                    }

                    if (!ait.id || ait.id == 0) {
                        ait.id = webServicesSession.createAccountInformationType(ait)
                        session.message = 'account.information.type.created'
                        session.args = [ait.id]

                    } else {
                        webServicesSession.updateAccountInformationType(ait)

                        session.message = 'account.information.type.updated'
                        session.args = [ait.id]
                    }

                } catch (SessionInternalError e) {
                    Set<MetaFieldWS> metaFieldWSes = new HashSet<>(Arrays.asList(conversation.ait?.metaFields))
                    metaFieldWSes.addAll(conversation.removedMetaFields)
                    conversation.ait?.metaFields = metaFieldWSes.toArray(new MetaFieldWS[metaFieldWSes.size()])

                    viewUtils.resolveException(flow, session.locale, e)
                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        finish {
            redirect controller: 'accountType', action: 'listAIT',
                    id: conversation.ait?.id, params: [accountTypeId: conversation.ait?.accountTypeId]
        }
    }

    private void aitNotFoundErrorRedirect(aitId, accountTypeId) {
        session.error = 'ait.not.found'
        session.args = [aitId as String]
        redirect controller: 'accountType', action: 'listAIT',
                params: [accountTypeId: accountTypeId]
    }

    private boolean bindMetaFieldData(MetaFieldWS metaField, params, index) {
        try {
            MetaFieldBindHelper.bindMetaFieldName(metaField, params, false, index.toString())
        } catch (Exception e) {
            log.debug("Error at binding meta field  : " + e)
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

    private def retrieveMetaFieldsForAccountType() {
        def types = new EntityType[1];
        types[0] = EntityType.ACCOUNT_TYPE
        new MetaFieldDAS().getAvailableFields(session['company_id'], types, null)
    }

    private def retrieveMetaFieldGroupsForAccountType() {
        return new MetaFieldGroupDAS().getAvailableFieldGroups(session['company_id'], EntityType.ACCOUNT_TYPE)
    }

    private def removeUsedAit(ait) {
        def acctInfoType = MetaFieldType.values()
        ait?.metaFields.each {
            acctInfoType -= it.fieldUsage
        }
        return acctInfoType
    }

    def getInvoiceTemplates() {
        def invoiceTemplates = InvoiceTemplateDTO.createCriteria().list()
                {
                    eq('entity', new CompanyDTO(session['company_id'] as Integer))
                    order('name', 'asc')
                }
        return invoiceTemplates
    }

    def getProductCategories() {
        List result = ItemTypeDTO.createCriteria().list {
            and {
                eq('internal', false)
                createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
                or {
                    'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
                    //list all gloal entities as well
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
                }
            }
            order('id', 'desc')
        }

        return result.unique()
    }
}
