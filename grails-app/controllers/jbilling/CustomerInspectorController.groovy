/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 * 
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

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.customerInspector.domain.CustomerInformation
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.PlanItemWS
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.PlanItemDTO
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.payment.blacklist.BlacklistBL
import com.sapienter.jbilling.server.payment.db.PaymentDTO
import com.sapienter.jbilling.server.pricing.PriceModelBL
import com.sapienter.jbilling.server.pricing.PriceModelWS
import com.sapienter.jbilling.client.pricing.util.PlanHelper
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.spa.SpaImportBL
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.user.CustomerPriceBL
import com.sapienter.jbilling.server.user.EntityBL
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO
import com.sapienter.jbilling.server.user.db.CustomerPriceDTO
import com.sapienter.jbilling.server.user.db.CustomerPricePK
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.Util

import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional

import javax.xml.XMLConstants
import javax.xml.bind.JAXB
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

@Secured(["CUSTOMER_13"])
class CustomerInspectorController {

    static pagination = [max: 10, offset: 0]
	static scope = "prototype"
	IWebServicesSessionBean webServicesSession
    def viewUtils
    def breadcrumbService
    def productService
    SecurityValidator securityValidator

    def static final String XSD_PATH = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + File.separator + "customerInspector" + File.separator + "customer_inspector_schema.xsd"
    def static final String DEFAULT_TEMPLATE_FILE = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + File.separator + "customerInspector" + File.separator + "default_customer_inspector_template.xml"

	def index () {
        flash.invalidToken = flash.invalidToken
		redirect action: 'inspect', params: params
	}

	def inspect () {

        //initialize pagination parameters
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.filterBy = params?.filterBy ?: ""

		def user = params.id ? UserDTO.get(params.int('id')) : null
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.VIEW)

        if (!user) {
            flash.error = 'no.user.found'
            flash.args = [ params.id as String ]
            return // todo: show generic error page
        }
        def customerNotes=CustomerNoteDTO.createCriteria().list(max: params.max,offset: params.offset){
            and{
                eq('customer.id',UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                order("creationTime","desc")
            }
        }
        def revenue =  webServicesSession.getTotalRevenueByUser(user.id)
        def subscriptions = webServicesSession.getUserSubscriptions(user.id)

        // last invoice
        def invoiceIds = webServicesSession.getLastInvoices(user.id, 1)
        def invoice = invoiceIds ? InvoiceDTO.get(invoiceIds.first()) : null

        // last payment
        def paymentIds = webServicesSession.getLastPayments(user.id, 1)
        def payment = paymentIds ? PaymentDTO.get(paymentIds.first()) : null

        // blacklist matches
        def blacklistMatches = BlacklistBL.getBlacklistMatches(user.id)

        // used to find the next invoice date
        def cycle = new OrderDAS().findEarliestActiveOrder(user.id)

        // all customer prices and products
        def company = CompanyDTO.get(session['company_id'])
        securityValidator.validateCompany(company?.id, Validator.Type.VIEW)

        def itemTypes = productService.getItemTypes(user.company.id, null)

		def customerUsagePools = webServicesSession.getCustomerUsagePools(user?.customer?.id)?.sort { it?.id }

        def accountType = user?.customer?.accountType
        //use customer's company to fetch products
		def products = productService.getFilteredProductsForCustomer(company, null, params, null, false, true, user.company)
        def prices = new CustomerPriceBL(user.id).getCustomerPrices()
        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }

        def priceExpiryMap= mapPriceWithExpiryDate(user.id, prices)

		List<MetaFieldValue> values =  new ArrayList<MetaFieldValue>()
		values.addAll(user?.customer?.metaFields)
		new UserBL().getCustomerEffectiveAitMetaFieldValues(values, user?.customer?.getAitTimelineMetaFieldsMap(), user.getCompany()?.id)
		
		// find all the subscription accounts and orders
		def subscriptionAccounts = new ArrayList<Integer>()
		def internalSubscriptions = new ArrayList<OrderWS>()
		
		UserDAS userDas = new UserDAS()
		for(def child : user?.customer?.children) {
			if(userDas.isSubscriptionAccount(child.baseUser.id)) {
				subscriptionAccounts.add(child.baseUser.id)
				
				internalSubscriptions.addAll(webServicesSession.getUserSubscriptions(child.baseUser.id))
			}
		}

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))

        String xml = session['configurationFile'] ?: CompanyDTO.get(session['company_id']).getCustomerInformationDesign()?:new File(DEFAULT_TEMPLATE_FILE).text

        [
                configurationPreview : params.configurationPreview,
                customerInformation: createCustomerInformation(xml),
                user: user,
                blacklistMatches: blacklistMatches,
                invoice: invoice,
                payment: payment,
                subscriptions: subscriptions,
                customerUsagePools: customerUsagePools,
                prices: prices,
                priceExpiryMap: priceExpiryMap,
                company: company,
				typeId: params.typeId,
                itemTypes: itemTypes,
                products: products,
                currencies: retrieveCurrencies(false),
                cycle: cycle,
                revenue: revenue,
                accountInformationTypes: infoTypes,
                customerNotes:customerNotes,
                customerNotesTotal:customerNotes?.totalCount,
                metaFields : values,
				subscriptionAccounts : subscriptionAccounts,
				internalSubscriptions : internalSubscriptions,
                isCurrentCompanyOwning : user.company?.id?.equals(session['company_id']) ? true : false,
                displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
                retrieveItems: retrieveItemsMethod(),
                isDistributel: SpaImportBL.isDistributel(session['company_id'])
        ]
    }

    def uploadConfiguration() {
        def configurationFile = request.getFile('configurationFile').inputStream.text
        if(null!=configurationFile && configurationFile instanceof String && !configurationFile.trim().isEmpty()) {
            if(createCustomerInformation(configurationFile) != null){
                session['configurationFile'] = configurationFile
                params.configurationPreview = true
            }
        }
        else {
            flash.error = 'no.template.selected'
        }
        redirect action: 'inspect', params: params
    }

    @Transactional(readOnly = false)
    def approveConfiguration() {
        EntityBL entityBl = new EntityBL(session['company_id'])
        CompanyWS company = entityBl.getCompanyWS(entityBl.getEntity())
        company.customerInformationDesign = session['configurationFile']

        webServicesSession.updateCompany(company)

        session['configurationFile'] = null
        params.previewConfiguration = false

        redirect action: 'inspect', params: params
    }

    def discardConfiguration() {
        session['configurationFile'] = null
        params.previewConfiguration = false

        redirect action: 'inspect', params: params
    }

    private CustomerInformation createCustomerInformation(String xml) {
        if(null!=xml) {
            def ci
            if(validateXMLSchema(xml)) {
                try {
                    ci = JAXB.unmarshal(new StringReader(xml), CustomerInformation.class)
                } catch(Exception e) {
                    flash.error = 'xml.error.found'
                    return null
                }
            }
            return !xml.isEmpty() ? ci : null
        }
        return null;
    }

    private boolean validateXMLSchema(String xml){
        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(XSD_PATH));
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'XML template validation error: ' + e.getMessage()
            return false;
        }
        return true;
    }

    def subNotes (){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def user = params.id ? UserDTO.get(params.int('id')) : null
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.VIEW)
        def customerNotes=CustomerNoteDTO.createCriteria().list(max: params.max,offset: params.offset){
            and{
                eq('customer.id',UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                order("creationTime","desc")
            }
        }
        render template: 'customerNotes', model: [customerNotes: customerNotes, customerNotesTotal: customerNotes?.totalCount, user:user]
    }

    // Customer specific pricing
    def filterProducts () {
        def company = CompanyDTO.get(session['company_id'])
        def user = params.int('userId') ? UserDTO.get(params.int('userId')) : null
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.VIEW)
        def itemTypes = productService.getItemTypes(user?.company?.id, null)

        //initialize pagination parameters
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.filterBy = params?.filterBy ?: ""

        def accountType = user?.customer?.accountType
        def products =  productService.getFilteredProductsForCustomer(company, null, params, accountType, false, true, user.company)

        render template: 'products', model: [typeId: params.typeId, itemTypes: itemTypes, products: products ]
    }

    def productPrices () {
        def itemId = params.int('id')
        def product = ItemDTO.get(itemId)
        //securityValidator.validateUserAndCompany(new ItemBL().getWS(product), Validator.Type.VIEW)
        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global, true)
        def prices = new CustomerPriceBL(params.int('userId')).getCustomerPrices(itemId)

        log.debug("prices for customer ${params.userId} and item ${itemId}, ${prices.size()}")

        def priceExpiryMap= mapPriceWithExpiryDate(params.int('userId'), prices)

        render template: 'prices', model: [ prices: prices, product: product, userId: params.userId, priceExpiryMap: priceExpiryMap ]
    }

    private Map mapPriceWithExpiryDate(int userId, List<PlanItemDTO> prices) {
        def priceExpiryMap= [:]
        if ( userId && prices ) {
            for (PlanItemDTO price: prices) {
                def customerPricePK= new CustomerPricePK()
                customerPricePK.setBaseUser(UserDTO.findById(userId))
                customerPricePK.setPlanItem(price)
                CustomerPriceDTO customerPrice= CustomerPriceDTO.findById(customerPricePK)
                priceExpiryMap.put(price.id, customerPrice.getPriceExpiryDate())
            }
        }
        priceExpiryMap
    }

    def allProductPrices () {
        UserDTO user = params.userId ? UserDTO.get(params.int('userId')) : null
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.VIEW)
        def prices = new CustomerPriceBL(params.int('userId')).getCustomerPrices()
        def priceExpiryMap= mapPriceWithExpiryDate(params.int('userId'), prices)
        render template: 'prices', model: [ prices: prices, userId: params.userId, priceExpiryMap: priceExpiryMap ]
    }

    /**
     * Get the price to be edited and show the 'editCustomerPrice.gsp' view. If no ID is given
     * this screen will allow creation of a new customer-specific price.
     */
    def editCustomerPrice () {
        def userId = params.int('userId')
        def priceId = params.int('id')

        def product = webServicesSession.getItem(params.int('itemId'), userId, null)
        def userWS = webServicesSession.getUserWS(userId)
        PlanItemWS price
        def priceExpiryDate

        if (priceId) {
            price = getCustomerPrice(userId, priceId)
            def customerPricePK= new CustomerPricePK()

            UserDTO user = UserDTO.findById(userId)
            securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.EDIT)
            customerPricePK.setBaseUser(user)

            PlanItemDTO planItem = PlanItemDTO.findById(priceId)
            securityValidator.validateCompany(planItem?.item?.entity?.id, Validator.Type.EDIT)
            customerPricePK.setPlanItem(planItem)

            CustomerPriceDTO customerPrice= CustomerPriceDTO.findById(customerPricePK)
            priceExpiryDate= customerPrice.getPriceExpiryDate()
        } else {
            // copy default product price model as a starting point
            def priceModel = PriceModelBL.getWsPriceForDate(product.defaultPrices, TimezoneHelper.currentDateForTimezone(session['company_timezone']));
            priceModel?.id = null;

            price = new PlanItemWS()
            price.addModel(CommonConstants.EPOCH_DATE, priceModel);
        }

        [ price: price, product: product, user: userWS, currencies: retrieveCurrencies(true), hideSaveOption:true, priceExpiryDate:priceExpiryDate ]
    }

    def PlanItemWS getCustomerPrice(userId, priceId) {
        return webServicesSession.getCustomerPrices(userId).find{ it.id == priceId }
    }

    def updateStrategy () {
        def price = params."price.id" ? getCustomerPrice(params.int('userId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        render template: '/priceModel/model', model: [ model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies(true) ]
    }

    def addChainModel () {
        def price = params."price.id" ? getCustomerPrice(params.int('userId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        // add new price model to end of chain
        def model = priceModel
        while (model.next) {
            model = model.next
        }
        model.next = new PriceModelWS();

        render template: '/priceModel/model', model: [ model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies(true) ]
    }

    def removeChainModel () {
        def price = params."price.id" ? getCustomerPrice(params.int('userId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

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

        render template: '/priceModel/model', model: [ model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies(true) ]
    }

    def addAttribute () {
        def price = params."price.id" ? getCustomerPrice(params.int('userId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        def modelIndex = params.int('modelIndex')
        def attribute = message(code: 'plan.new.attribute.key', args: [ params.attributeIndex ])

        // find the model in the chain, and add a new attribute
        def model = priceModel
        for (int i = 0; model != null; i++) {
            if (i == modelIndex) {
                model.attributes.put(attribute, '')
            }
            model = model.next
        }

        render template: '/priceModel/model', model: [ model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies(true) ]
    }

    def removeAttribute () {
        def price = params."price.id" ? getCustomerPrice(params.int('userId'), params.int('price.id')) : null
        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

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

        render template: '/priceModel/model', model: [ model: priceModel, startDate: startDate, models: price?.models, currencies: retrieveCurrencies(true) ]
    }

    /**
     * Validate and save a customer-specific price.
     */
    @RequiresValidFormToken
    def saveCustomerPrice () {
        def user = webServicesSession.getUserWS(params.int('userId'))
        def price = new PlanItemWS()
        bindData(price, params, 'price')

        securityValidator.validateUserAndCompany(user, Validator.Type.EDIT)

        def priceModel = PlanHelper.bindPriceModel(params)
        try {
            String errorString = "PriceModelWS,startDate,validation.error.pricemodel.startdate,${message(code: 'date.format').toString()}"
            DateTime startDate = Util.getParsedDateOrThrowError(message(code: 'date.format').toString(), params.startDate.toString(),errorString)
            price.models.put(startDate.toDate(), priceModel)

            Date priceExpiryDt= null
            String expiryErrorString = ",,validation.error.accountTypePrice.priceExpiryDate,${message(code: 'date.format').toString()}"
            if ( params.priceExpiryDate ) {
                DateTime expiryDate = Util.getParsedDateOrThrowError(message(code: 'date.format').toString(), params.priceExpiryDate.toString(), expiryErrorString)
                log.debug("For expiryDate ${expiryDate.withTimeAtStartOfDay().toDate()}")
                priceExpiryDt= expiryDate.withTimeAtStartOfDay().toDate()
            }

            if (!price.id || price.id == 0) {
                log.debug("creating customer ${user.userId} specific price ${price}")

                PriceModelBL.validateWsAttributes(priceModel)
                price = webServicesSession.createCustomerPrice(user.userId, price, priceExpiryDt);

                flash.message = 'created.customer.price'
                flash.args = [ price.itemId as String ]
            } else {
                log.debug("updating customer ${user.userId} specific price ${price.id}")

                webServicesSession.updateCustomerPrice(user.userId, price, priceExpiryDt);

                flash.message = 'updated.customer.price'
                flash.args = [ price.itemId as String ]
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            def itemId = params.int('itemId')?:params.int('price.itemId')
            def product = webServicesSession.getItem(itemId, user.userId, null)
            render view: 'editCustomerPrice', model: [ price: price, product: product, user: user, currencies: retrieveCurrencies(true), priceExpiryDate: params.priceExpiryDate ?: null ]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'customer.price.save.error'
            def product = webServicesSession.getItem(params.int('price.itemId'), user.userId, null)
            render view: 'editCustomerPrice', model: [ price: price, product: product, user: user, currencies: retrieveCurrencies(true), priceExpiryDate: params.priceExpiryDate ?: null ]
            return
        }

        chain controller: 'customerInspector', action: 'inspect', params: [ id: user.userId ]
    }

    /**
     * Deletes a customer-specific price.
     */
    def deleteCustomerPrice () {
        def userId = params.int('userId')
        def planItemId = params.int('id')

        def user = webServicesSession.getUserWS(params.int('userId'))
        securityValidator.validateUserAndCompany(user, Validator.Type.EDIT)

        try {
            log.debug("deleting customer ${userId} price ${planItemId}")

            webServicesSession.deleteCustomerPrice(userId, planItemId)

            flash.message = 'deleted.customer.price'
            flash.args = [ params.itemId as String ]

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        // render remaining prices for the priced product
        redirect action: 'productPrices', params: [id: params.itemId, userId: userId ]
    }

    def retrieveCurrencies(def inUse) {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return inUse ? currencies.findAll { it.inUse } : currencies;
    }

    def redirectAction(String urlRedirect){
        redirect url:urlRedirect
    }

    /**
     * Retrieves items according to orderLine
     * */
    def retrieveItemsMethod () {

        def user = params.id ? UserDTO.get(params.int('id')) : null

        OrderWS[] orders= webServicesSession.getUserSubscriptions(user.id)

        Map<Integer, ItemDTOEx> items = new HashMap<Integer, ItemDTOEx>()

        for(OrderWS orderWS : orders){
            for(OrderLineWS line : orderWS.getOrderLines()){
                if(null != line.getItemId()){
                    ItemDTOEx itemDTOex = webServicesSession.getItem(line.getItemId(), orderWS.getUserId(), line.getPriceAsDecimal().toString())
                    items.put(line.getId(), itemDTOex)
                }
            }
        }
        return items
    }
}
