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
import com.sapienter.jbilling.client.pricing.util.PlanHelper
import com.sapienter.jbilling.client.util.BindHelper
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.PlanItemBundleWS
import com.sapienter.jbilling.server.item.PlanItemWS
import com.sapienter.jbilling.server.item.PlanWS
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.pricing.PriceModelBL
import com.sapienter.jbilling.server.pricing.PriceModelWS
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.Util
import com.sapienter.jbilling.server.util.db.CurrencyDTO
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.collections.CollectionUtils
import org.hibernate.criterion.CriteriaSpecification
import org.joda.time.format.DateTimeFormat
/**
 * Plan builder controller
 *
 * @author Brian Cowdery
 * @since 01-Feb-2011
 */
@Secured(["hasAnyRole('PLAN_60', 'PLAN_61')"])
class PlanBuilderController {

    static pagination = [max: 10, offset: 0]
	static scope = "prototype"

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def breadcrumbService
    def productService
	def companyService
    SecurityValidator securityValidator

    def index () {
        redirect action: 'edit'
    }

    /**
     * Sorts a list of PlanItemWS objects by precedence and itemId.
     *
     * @param planItems plan items
     * @return sorted list of plan items
     */
    def sortPlanItems(planItems) {
        // precedence in ascending order, item id in descending
        return planItems.sort { a, b->
            (b.precedence <=> a.precedence) ?: (a.itemId <=> b.itemId)
        }
    }

    /**
     * Returns a sorted list of all plan item pricing dates.
     *
     * @param planItems plan items
     * @return sorted list of pricing dates
     */
    def collectPricingDates(planItems) {
        def dates = new TreeSet<Date>()

        planItems.each{ item->
            item.models.keySet().each{ date->
                dates << date
            }
        }

        return dates;
    }

    def getUsagePools() {
        log.debug("getting usage pools")
		return webServicesSession.getAllUsagePools()        
    }

    def editFlow = {//do not change this to method. Webflow will not be able to find it.

        /**
         * Initializes the plan builder, putting necessary data into the flow and conversation
         * contexts so that it can be referenced later.
         */
        initialize {
            action {
                if (!params.id && !SpringSecurityUtils.ifAllGranted("PLAN_60")) {
                    // not allowed to create
                    redirect controller: 'login', action: 'denied'
                    return
                }

                if (params.id && !SpringSecurityUtils.ifAllGranted("PLAN_61")) {
                    // not allowed to edit
                    redirect controller: 'login', action: 'denied'
                    return
                }

                def plan
                def product
				def availableFields = new ArrayList<MetaField>()

                try {
                    plan = params.id ? webServicesSession.getPlanWS(params.int('id')) : new PlanWS()
                    product = plan?.itemId ? webServicesSession.getItem(plan.itemId, session['user_id'], null) : new ItemDTOEx()
					if ( CollectionUtils.isNotEmpty(product?.entities) ) {
						product.entityId=  product.entities.get(0)
					}
                } catch (SessionInternalError e) {
                    log.error("Could not fetch WS object", e)
                    redirect controller: 'plan', action: 'list', params: params
                    return
                }

                availableFields = getMetaFieldForPlan(product, true)
				def company = CompanyDTO.get( product.entityId ?: session['company_id'])

				def itemTypes = productService.getItemTypes(session['company_id'], null)
                def internalPlansType = productService.getInternalPlansType()

				def currencies = new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
				def usagePools = getUsagePools()

                def orderPeriods = company.orderPeriods.collect { new OrderPeriodDTO(it.id) }
                def itemOrderPeriods = orderPeriods.clone()
                itemOrderPeriods << new OrderPeriodDTO(Constants.ORDER_PERIOD_ONCE) << new OrderPeriodDTO(Constants.ORDER_PERIOD_ALL_ORDERS)
                orderPeriods.sort { it.id }
                itemOrderPeriods.sort {it.id}

                // subscription product defaults for new plans
                if (!product.id || product.id == 0) {
                    product.hasDecimals = 0
                    product.types = [internalPlansType.id]
                    product.entityId = company.id

                    def priceModel = new PriceModelWS()
                    priceModel.type = PriceModelStrategy.FLAT
                    priceModel.rate = BigDecimal.ZERO
                    priceModel.currencyId = (currencies.find { it.id == session['currency_id']} ?: company.currency).id

                    product.defaultPrices.put(CommonConstants.EPOCH_DATE, priceModel)
                }

                // subscription product uses a FLAT price model
                // don't use the legacy compatibility pricing fields
                product.price = null

                log.debug("plan subscription product ${product}")

                // defaults for new plans
                if (!plan.id || plan.id == 0) {
                    plan.periodId = orderPeriods.first().id
                }

                log.debug("plan ${plan}")

                // pricing timeline
                def pricingDates = collectPricingDates(plan.planItems)
                def startDate
                if (!product.id || product.id == 0) {
                    startDate = CommonConstants.EPOCH_DATE
                } else {
                    if (pricingDates) {
                        startDate = pricingDates.asList().last()
                    } else {
                        startDate = CommonConstants.EPOCH_DATE
                    }
                }

                // add breadcrumb
                def crumbName = params.id ? 'update' : 'create'
                def crumbDescription = params.id ? product.number : null
                breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

                // model scope for this flow
                flow.company = company
                flow.itemTypes = itemTypes
                flow.currencies = CompanyDTO.get(session['company_id'] as Integer)?.currencies?.sort { it.description}
				flow.companies = companyService.getEntityAndChildEntities()
				flow.allCompanies = companyService.getEntityAndChildEntities()
                flow.orderPeriods = orderPeriods
                flow.itemOrderPeriods = itemOrderPeriods
                flow.usagePools = usagePools
                flow.availableFields = availableFields

                //initialize pagination parameters
                params.max = params?.max?.toInteger() ?: pagination.max
                params.offset = params?.offset?.toInteger() ?: pagination.offset
				
                // conversation scope
                conversation.pricingDates = pricingDates
                conversation.startDate = startDate
                conversation.plan = plan
                conversation.product = product
                conversation.products = productService.getFilteredProducts(company, product, params, null, false, true)
                conversation.products?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.maxProductsShown = params.max
                conversation.pooledItems = productService.getFilteredProducts(company, product, params, null, false, true)
                conversation.pooledItems?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.availableFields = availableFields
				//Added to address issue #7432 wherein 'Selection Category' dropdown on product tab appears empty - BEGIN
				conversation.productCategories = getProductCategories(null)
				//Added to address issue #7432 wherein 'Selection Category' dropdown on product tab appears empty - END

                if (flow.allCompanies.size()==1){
                    product.entities=flow.allCompanies.id
                }
			}
            on("success").to("build")
        }

        /**
         * Renders the plan details tab panel.
         */
        showDetails {
            action {
                conversation.availableFields = getMetaFieldForPlan(conversation.product, false)
                conversation.product.getDescriptions().each{
                    it.deleted = false
                }
                params.template = 'details'
                if(conversation.errorMessages){
                    params.errorMessages = conversation.errorMessages
                    conversation.errorMessages = null
                }
            }
            on("success").to("build")
        }

        /**
         * Renders the product list tab panel, filtering the product list by the given criteria.
         */
        showProducts {
            action {
                params.max = params?.max?.toInteger() ?: pagination.max
                params.offset = params?.offset?.toInteger() ?: pagination.offset

                // filter using the first item type by default
                if ((params.typeId == null || params.typeId == '') && flow.itemTypes)
                    params.typeId = ''

                if (null == params['filterBy'])
                    params['filterBy'] = ""

                params.template = 'products'
				
                conversation.products = productService.getFilteredProducts(flow.company, conversation.product, params, null, false, true)
                conversation.products?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.maxProductsShown = params.max
            }
            on("success").to("build")
        }

        /**
         * Renders the plan list tab panel, filtering the product list by the given criteria.
         */
        showPlans {
            action {
                params.max = params?.max?.toInteger() ?: pagination.max;
                params.offset = params?.offset?.toInteger() ?: pagination.offset

                if (null == params['filterBy'])
                    params['filterBy'] = ""

                params.template = 'plans'
                conversation.plans = productService.getFilteredPlans(null, params, true)
                conversation.plans?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.maxPlansShown = params.max
            }
            on("success").to("build")
        }

        /**
         * Renders the pricing timeline top panel, allowing navigation and creation of pricing dates.
         */
        showTimeline {
            action {
                params.template = 'timeline'
            }
            on("success").to("build")
        }

        addDate {
            action {
            
            	try {
            	
	            	Util.getParsedDateOrThrowError(message(code: 'date.format'), params.startDate, 'PlanWS,date,invalid.date.format') 
	            
	                def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

                    if(collectPricingDates(conversation.plan.planItems).any {it == startDate}){
                        throw new SessionInternalError("Prices with date already present", 'PlanWS,date,price.model.add.date.duplicate');
                    }

                    log.debug("adding plan items pricing date ${params.startDate}")
	
	                // find the closet price model to the new date and copy it
	                // to create a new price for the given start date
	                for (PlanItemWS item : conversation.plan.planItems) {
	                    def itemPriceModel = PriceModelBL.getWsPriceForDate(item.getModels(), startDate)
						log.debug 'found itemPriceModel ********************** ' + itemPriceModel
	                    def priceModel = new PriceModelWS(itemPriceModel);
						
						log.debug 'converted PriceModel ********************** ' + priceModel
						
	                    priceModel.id = null
	
	                    item.addModel(startDate, priceModel);
	                }
	
	                // update pricing dates
	                conversation.pricingDates = collectPricingDates(conversation.plan.planItems)
	                conversation.startDate = startDate
	
	                log.debug("adding subscription product pricing date ${params.startDate}")
	
	                // copy the closest model to the new date
	                def defaultPriceModel = PriceModelBL.getWsPriceForDate(conversation.product.defaultPrices, startDate)
	                def priceModel = new PriceModelWS(defaultPriceModel)
	                priceModel.id = null
                    priceModel.rate = "0"
	
					log.debug 'default PriceModel ********************** ' + priceModel
					
	                conversation.product.defaultPrices.put(startDate, priceModel)
	
	
	                params.template = 'review'
                
                } catch (SessionInternalError e) {
                	params.template = 'review'
                    viewUtils.resolveException(flash, session.locale, e)
                    if(flash.errorMessages){
                        conversation.errorMessages = flash.errorMessages
                        flash.errorMessages = null
                    }
                }
            }
            on("success").to("build")
        }

        editDate {
            action {
                def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()
                conversation.startDate = startDate

                log.debug("editing pricing date ${params.startDate}")

                params.template = 'review'
            }
            on("success").to("build")
        }

        removeDate {
            action {
                def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()
                log.debug("Removing pricing date from plan items ${startDate}")

                // remove plan items for the startDate
                for (PlanItemWS item : conversation.plan.planItems) {
                    item.removeModel(startDate);
                }

                log.debug("Remove subscription product pricing date ${startDate}")
                conversation.product.defaultPrices.remove(startDate)

                // refresh pricing dates
                conversation.pricingDates = collectPricingDates(conversation.plan.planItems)
                conversation.startDate = conversation.product.defaultPrices.lastKey();

                // (pai) this is not very good solution to delete the timeline on click
                // preffered way on application level should be on save changes
                /*
                def plan = conversation.plan
                def product = conversation.product

                if (plan?.id && product?.id) {

                    product.number = product?.number?.trim()
                    product.description = product?.description?.trim()

                    log.debug("Async saving changes to plan subscription item ${product.id}")
                    webServicesSession.updateItem(product)

                    log.debug("Async saving changes to plan ${plan.id}")
                    webServicesSession.updatePlan(plan)
                }*/

                params.template = 'review'

            }
            on("success").to("build")
        }

        /**
         * Add a new price for the given product id, and render the review panel.
         */
        addPrice {
            action {
                // product being added
                def productId = params.int('id')
                def product = conversation.products.find{ it.id == productId }
				
                //Line type product validation when creating a plan
                if(product.getPrice(conversation.startDate, session['company_id'])?.type == PriceModelStrategy.LINE_PERCENTAGE){
                    params.template = 'review'
                    flow.errorMessages= [g.message(code:"validation.error.plan.invalid.pct.product")]
                    return invalidProduct()
                }


                def productPrice = product.getPrice(conversation.startDate, session['company_id'])
                // build a new plan item, using the default item price model
                // as the new objects starting values
				def priceModel = null
				if (productPrice) {
				        priceModel= PriceModelBL.getWS(productPrice)
				} else {
				        priceModel = new PriceModelWS()
				        priceModel.type = PriceModelStrategy.FLAT
				        priceModel.rate = BigDecimal.ZERO
				        priceModel.currencyId = ( CurrencyDTO.get(session['currency_id'])?.getId() ?: CompanyDTO.get(session['company_id']).getCurrencyId() )
				}
				
                priceModel.id = null

                // empty bundle
                def bundle = new PlanItemBundleWS()

                // add price to the plan for new product without duplicating product				
				def duplicate = conversation.plan.planItems.find{ it.itemId == productId}
				def index = conversation.plan.planItems.findIndexOf {it.itemId == productId}
				
				if(!duplicate){
					conversation.plan.planItems << new PlanItemWS(productId, priceModel, bundle)
					params.newLineIndex = conversation.plan.planItems.size() - 1
				} else {
					params.newLineIndex = index
				}
                
                conversation.priceModelData = productService.getFilteredProducts(flow.company, product, params, null, false, false)
                conversation.priceModelData?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.productCategories = getProductCategories(null)
                params.template = 'review'
            }
            on("success").to("build")

            on("invalidProduct").to("build")

        }

        /**
         * Updates a price and renders the review panel.
         */
        updatePrice {
            action {
                def index = params.int('index')
                def planItem = conversation.plan.planItems[index]

                log.debug("updating price for date ${conversation.startDate}")

                if (!planItem.bundle) planItem.bundle = new PlanItemBundleWS()

                bindData(planItem, params, 'price')
                bindData(planItem.bundle, params, 'bundle')

				//TODO pass default values to binder methods when not-null values not acceptable
				if(!planItem?.bundle?.quantity) {
					planItem?.bundle?.quantity= "0"
				}

                def priceModel = PlanHelper.bindPriceModel(params)
                planItem.models.put(conversation.startDate, priceModel)

                log.debug("updated price: ${priceModel}")
                log.debug("price from timeline map ${planItem.models.get(conversation.startDate)}")

                try {
                    // validate attributes of updated price

                    PriceModelBL.validateWsAttributes(planItem.models.values())

                    // re-order plan items by precedence, unless a validation exception is thrown
                    conversation.plan.planItems = sortPlanItems(conversation.plan.planItems)
                    conversation.priceModelData = productService.getFilteredProducts(flow.company, null, params, null, false, false)
                    conversation.priceModelData?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                    conversation.productCategories = getProductCategories(null)

                    log.debug("Updated conversation plan ${conversation.plan}")
                    log.debug("Updated conversation item ${conversation.plan.planItems[index]}")

                } catch (SessionInternalError e) {
                    e.printStackTrace()
                    viewUtils.resolveException(flash, session.locale, e)
                    params.newLineIndex = index
                }

                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Removes a item price from the plan and renders the review panel.
         */
        removePrice {
            action {
                conversation.plan.planItems.remove(params.int('index'))
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Add a new plan price for the given product id, and render the review panel.
         */
        addPlanPrice {
            action {
                // product being added
                def planId = params.int('id')
                if (planId == conversation.plan.itemId) {
                    flash.errorMessages = [ message(code: 'validation.error.self.nested.plan') ]
                    params.template = 'review'
                    return invalidPlan()
                }
                def plan = conversation.plans.find{ it.id == planId }

                def planPrice = plan.getPrice(conversation.startDate)
                // build a new plan item, using the default item price model
                // as the new objects starting values
                def priceModel = planPrice ? PriceModelBL.getWS(planPrice) : new PriceModelWS()
                priceModel.id = null

                // empty bundle
                def bundle = new PlanItemBundleWS()

                // add price to the plan
                conversation.plan.planItems << new PlanItemWS(planId, priceModel, bundle)

                params.newLineIndex = conversation.plan.planItems.size() - 1
                params.template = 'review'
            }
            on("success").to("build")
            on("invalidPlan").to("build")
        }

        /**
         * Removes a item price from the plan and renders the review panel.
         */
        removePlanPrice {
            action {
                conversation.plan.planItems.remove(params.int('index'))
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Updates a strategy of a model in a pricing chain.
         */
        updateStrategy {
            action {
                def index = params.int('index')
                def planItem = conversation.plan.planItems[index]

                bindData(planItem, params, 'price')
                bindData(planItem.bundle, params['bundle'])
                planItem.models.put(conversation.startDate, PlanHelper.bindPriceModel(params))

                params.newLineIndex = index
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Adds an additional price model to the chain.
         */
        addChainModel {
            action {
                def index = params.int('index')
                def rootModel = PlanHelper.bindPriceModel(params)

                // add new price model to end of chain
                def model = rootModel
                while (model.next) {
                    model = model.next
                }
                model.next = new PriceModelWS();

                // add updated model to the plan item
                def planItem = conversation.plan.planItems[index]
                planItem.models.put(conversation.startDate, rootModel)

                params.newLineIndex = index
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Removes a price model from the chain.
         */
        removeChainModel {
            action {
                def index = params.int('index')
                def modelIndex = params.int('modelIndex')
                def rootModel = PlanHelper.bindPriceModel(params)

                // remove price model from the chain
                def model = rootModel
                for (int i = 1; model != null; i++) {
                    if (i == modelIndex) {
                        model.next = model.next?.next
                        break
                    }
                    model = model.next
                }

                // add updated model to the plan item
                def planItem = conversation.plan.planItems[index]
                planItem.models.put(conversation.startDate, rootModel)

                params.newLineIndex = index
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Adds a new attribute field to the plan price model, and renders the review panel.
         * The rendered review panel will have the edited line open for further modification.
         */
        addAttribute {
            action {
                def index = params.int('index')
                def rootModel = PlanHelper.bindPriceModel(params)
                def modelIndex = params.int('modelIndex')

                // find the model in the chain, and add a new attribute
                def model = rootModel
                for (int i = 0; model != null; i++) {
                    if (i == modelIndex) {
                        def attributeIndex = params.int('attributeIndex')
                        model.attributes.eachWithIndex {key, value, ind ->
                            if(ind == attributeIndex - 1){
                                if(key){
                                    ++attributeIndex
                                }
                            }
                        }
                        def attribute = message(code: 'plan.new.attribute.key', args: [ attributeIndex ])
                        while (model.attributes.containsKey(attribute)) {
                            newIndex++
                            attribute = message(code: 'plan.new.attribute.key', args: [newIndex])
                        }
                        model.attributes.put(attribute, '')
                    }
                    model = model.next
                }

                // add updated model to the plan item
                def planItem = conversation.plan.planItems[index]
                planItem.models.put(conversation.startDate, rootModel)

                params.newLineIndex = index
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Removes the given attribute name from a plan price model, and renders the review panel.
         * The rendered review panel will have the edited line open for further modification.
         */
        removeAttribute {
            action {
                def index = params.int('index')
                def rootModel = PlanHelper.bindPriceModel(params)

                def modelIndex = params.int('modelIndex')
                def attributeIndex = params.int('attributeIndex')

                // find the model in the chain, remove the attribute
                def model = rootModel
                for (int i = 0; model != null; i++) {
                    if (i == modelIndex) {
                        def name = params["model.${modelIndex}.attribute.${attributeIndex}.name"]
                        model.attributes.remove(name)
                    }
                    model = model.next
                }

                // add updated model to the plan item
                def planItem = conversation.plan.planItems[index]
                planItem.models.put(conversation.startDate, rootModel)

                params.newLineIndex = index
                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Updates the plan description and renders the review panel.
         */
        updatePlan {
            action {
                log.debug("updating plan details")

                Util.getParsedDateOrThrowError(message(code: 'date.format'), params.startDate, 'PlanWS,date,invalid.date.format')
                Util.getParsedDateOrThrowError(message(code: 'date.format'), params.originalStartDate, 'PlanWS,date,invalid.date.format')

                Date newStartDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)
                Date originalStartDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.originalStartDate)

				def isRoot = new CompanyDAS().isRoot(session['company_id'])

                bindData(conversation.plan, params, 'plan')
                BindHelper.bindPropertyPresentToInteger(params, conversation.plan, ["editable"], 'plan.')
                //Firstly remove all categories from product and then set new .
                conversation.product.setTypes([] as Integer[])
				int contentSize = params.product.findAll({k, v -> k.indexOf("content") > 0}).size()
                for (int i = 0; i < contentSize; i++ ) {
                    if (!conversation.product?.descriptions[i])
                        conversation.product.descriptions.add(new com.sapienter.jbilling.server.util.InternationalDescriptionWS());
                }
                bindData(conversation.product, params, 'product')
                bindMetaFields(conversation.plan, params, isRoot, conversation.product)


				//conversation.availableFields = conversation.plan.metaFields
				
                // update default price for the current working start date
                def startDate = conversation.startDate
                def defaultPriceModel = PriceModelBL.getWsPriceForDate(conversation.product.defaultPrices, startDate)
                bindData(defaultPriceModel, params, 'price')

                log.debug("updating subscription product pricing for date ${startDate} = ${defaultPriceModel}")
                conversation.product.defaultPrices.put(startDate, defaultPriceModel)

                // sort prices by precedence
                conversation.plan.planItems = sortPlanItems(conversation.plan.planItems)

                // if the startDate has changed
                if(!originalStartDate.equals(newStartDate)){
                    // update the priceModels for changed date
                    for (PlanItemWS item : conversation.plan.planItems) {
                        PriceModelWS itemPriceModel = PriceModelBL.getWsPriceForDate(item.getModels(), originalStartDate)
                        item.removeModel(originalStartDate)
                        item.addModel(newStartDate, itemPriceModel);
                    }
                    // update pricing dates
                    conversation.pricingDates = collectPricingDates(conversation.plan.planItems)
                    conversation.startDate = newStartDate
                    // update defaultPrices for the new date
                    PriceModelWS defPriceModel = PriceModelBL.getWsPriceForDate(conversation.product.defaultPrices, originalStartDate)
                    conversation.product.defaultPrices.remove(originalStartDate)
                    conversation.product.defaultPrices.put(newStartDate, defPriceModel)
                }

                params.template = 'review'
            }
            on("success").to("build")
        }

		/**
		 * Show review tab
		 */
		showReview {
			action {
				params.template = 'review'
			}
			on("success").to("build")
		}
		
		/**
		 * Renders the metafields tab panel
		 */
		showMetaFields {
			action {
				params.template = 'metafields'
			}
			on("success").to("build")
		}
		
		/**
		 * Adds a new meta field to plan's subscription product
		 */
		addNewMetaField {
			action {

				def metaFieldId = params.int('id')

				def metaField = metaFieldId ? webServicesSession.getMetaField(metaFieldId) :
					new MetaFieldWS();

				metaField.primary = false

				if (metaField?.id || metaField.id != 0) {
					// set metafield defaults
					metaField.id = 0
				} else {
					metaField.entityType = EntityType.ORDER_LINE
					metaField.entityId = session['company_id'].toInteger()
				}

				// add metafield to product
				def product = conversation.product
				def metaFields = product.orderLineMetaFields as List
				if(metaFields == null) {
					metaFields = new MetaFieldWS[0] as List;
				}
				
				metaFields.add(metaField)
				product.orderLineMetaFields = metaFields.toArray()

				conversation.product = product

				params.newLineIndex = metaFields.size() - 1
				params.template = 'metafields'
			}
			on("success").to("build")
		}

		/**
		 * Updates an metafield  and renders the metafields panel
		 */
		updateOrderMetaField {
			action {

				flash.errorMessages = null
				flash.error = null
				try {
					def product = conversation.product
	
					// get existing metafield
					def index = params.int('index')
					def metaField = product.orderLineMetaFields[index]
					
					if(!bindMetaFieldData(metaField, params, index)) {
						//#7731 - field name restriction is not working
						if(metaField.name.size() > 100) {
							metaField.name = ""
							params["metaField" + index + ".name"] = ""
							throw new SessionInternalError("Meta field name too long ", [
								"MetaFieldWS,name,metafield.validation.name.too.long,"
							] as String[])
						}
						error()
					}
					
					// add metafield to the ait
					product.orderLineMetaFields[index] = metaField
					
					// sort metafields by displayOrder
					product.orderLineMetaFields = product.orderLineMetaFields.sort { it.displayOrder }
					conversation.product = product
				} catch (SessionInternalError e) {
					viewUtils.resolveException(flash, session.locale, e)
                }
				
				params.template = 'metafields'
			}
			on("success").to("build")
		}

		/**
		 * Remove a metafield from the information type  and renders the AIT metafields panel
		 */
		removeOrderMetaField {
			action {

				def product = conversation.product

				def index = params.int('index')
				def metaFields = product.orderLineMetaFields as List

				def metaField = metaFields.get(index)
				metaFields.remove(index)

				product.orderLineMetaFields = metaFields.toArray()

				conversation.product = product

				params.template = 'metafields'
			}
			on("success").to("build")
		}

        /**
         * Shows the plan builder. This is the "waiting" state that branches out to the rest
         * of the flow. All AJAX actions and other states that build on the order should
         * return here when complete.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            // list
            on("details").to("showDetails")
            on("products").to("showProducts")
            on("plans").to("showPlans")
            on("timeline").to("showTimeline")

			on("review").to("showReview")
			on("metaFields").to("showMetaFields")
			on("addMetaField").to("addNewMetaField")
			on("updateMetaField").to("updateOrderMetaField")
			on("removeMetaField").to("removeOrderMetaField")
			
            // pricing
            on("addDate").to("addDate")
            on("editDate").to("editDate")
            on("removeDate").to("removeDate")
            on("addPrice").to("addPrice")
            on("updatePrice").to("updatePrice")
            on("removePrice").to("removePrice")
            on("addPlanPrice").to("addPlanPrice")
            on("removePlanPrice").to("removePlanPrice")

            // pricing model
            on("updateStrategy").to("updateStrategy")
            on("addChainModel").to("addChainModel")
            on("removeChainModel").to("removeChainModel")
            on("addAttribute").to("addAttribute")
            on("removeAttribute").to("removeAttribute")

            // plan
            on("update").to("updatePlan")
            on("save").to("savePlan")
            on("cancel").to("finish")
        }

        /**
         * Saves the plan and exits the builder flow.
         */
        savePlan {
            action {
                
				try {

                    def plan = conversation.plan
                    def product = conversation.product
					List aList = setMetaFieldsInConversation(product)
					conversation.availableFields = aList.get(0)
					Integer entityId = aList.get(1)
                    if(product.isGlobal()) {
                        //Empty entities
                        product.entities = new ArrayList<Integer>(0);
                    } else {
                        //Validate for entities
                        if(product.getEntities() == null || product.getEntities().size() == 0) {
                            String [] errors = ["PlanWS,company,validation.error.no.company.selected"]
                            throw new SessionInternalError("validation.error.no.company.selected", errors)
                        }
                    }

					product.entities.unique()
					
                    //to avoid spaces or tabs
					bindData(product, params, 'product')
					

                    if (!plan.id || plan.id == 0) {
                        if (SpringSecurityUtils.ifAllGranted("PLAN_60")) {

                            validateBundledQuantity(plan)
							validateItemsOnPlan(plan, product)
							        
							log.debug("creating plan ${plan}")
                            plan.id = webServicesSession.createPlan(plan, product)

                            // set success message in session, contents of the flash scope doesn't survive
                            // the redirect to the order list when the web-flow finishes
                            session.message = 'plan.created'
                            session.args = [ plan.id ]

                        } else {
                            redirect controller: 'login', action: 'denied'
                            return
                        }

                    } else {
                        if (SpringSecurityUtils.ifAllGranted("PLAN_61")) {
                            
                            validateBundledQuantity(plan)
							validateItemsOnPlan(plan, product)
							
                            log.debug("saving changes to plan ${plan.id}")
                            webServicesSession.updatePlan(plan, product)

                            session.message = 'plan.updated'
                            session.args = [ plan.id ]

                        } else {
                            redirect controller: 'login', action: 'denied'
                            return
                        }
                    }

                } catch (SessionInternalError e) {
                    if(!viewUtils.resolveException(flow, session.locale, e)) {
						flow.errorMessages = [flow.error]
					}
                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        finish {
            redirect controller: 'plan', action: 'list', id: conversation.plan?.id
        }
    }

    private List getMetaFieldForPlan(def product, boolean init) {
        def availableFields = new ArrayList<MetaField>()
        if (!init || product?.id) {
            List companyIds = null
            if (product.isGlobal()) companyIds = retrieveCompaniesIds()
            else if (product.entities) companyIds = product.entities
            if (companyIds) {
                for (Integer entityId : companyIds) {
                    availableFields.addAll(retrieveAvailableMetaFields(entityId))
                }
            }
        } else {
            availableFields = retrieveAvailableMetaFields(session['company_id'])
        }
        return availableFields
    }
    
    private void validateBundledQuantity(PlanWS plan) throws SessionInternalError {
    	
    	boolean bundledQuantityMaxError = false;
    	String planItemDescription = "";
    	
    	for (PlanItemWS planItem : plan.getPlanItems()) {
    	
    		if (planItem != null && 
    			planItem.getBundle() != null && 
    			planItem.getBundle().getQuantity() != null) {
    			
    			planItemDescription = ItemDTO.get(planItem.itemId)?.description ?: "";
    				
    			if ((planItem.getBundle().getQuantity().contains(Constants.DECIMAL_POINT) && 
    				 planItem.getBundle().getQuantity().length() > 23) 
    					|| 
    				(!planItem.getBundle().getQuantity().contains(Constants.DECIMAL_POINT) && 
    				 planItem.getBundle().getQuantity().length() > 12)) {
	    			
    				bundledQuantityMaxError = true;
	    			break;
    			}
    			
    			try {
	    			if (new BigDecimal(planItem.getBundle().getQuantity()) >= new BigDecimal(1000000000000)) {
		    			bundledQuantityMaxError = true;
		    			break;
	    			}
    			} catch (Exception e) {
    				String []errors = new String[1];
		    		errors[0] = "PlanWS.planItem,bundledQuantity,validation.error.plan.planItem.bundledQuantity.not.numeric," + planItemDescription;
		    		throw new SessionInternalError("Bundled Quantity should be numeric.", errors);
    			}
    		}
    	}
    	
    	if (bundledQuantityMaxError) {
    		String []errors = new String[1];
    		errors[0] = "PlanWS.planItem,bundledQuantity,validation.error.plan.planItem.bundledQuantity.max.exceeded," + planItemDescription;
    		throw new SessionInternalError("Max Bundled Quantity can be upto 10^12.", errors);
    	}
    	
    }
	
	private void validateItemsOnPlan(PlanWS plan,ItemDTOEx planProduct) {
		def isValid = true
		def description

        if(plan.getPlanItems().isEmpty()) {String []errors = new String[1];
            errors[0] = "validation.error.plan.no.items,";
            throw new SessionInternalError("Plan can not be created without products.", errors);
        }

		for(PlanItemWS planItem : plan.getPlanItems()) {
            List<Integer> companies  = new LinkedList<>();
			ItemDTO product = ItemDTO.get(planItem.itemId)
			description = product?.description

            for(CompanyDTO company: product.entities){
                companies.add(company.getId())
            }
				if(planProduct.global) {
					if(!product.global) {
						isValid = false
					}
                } else {
                    if(!(product.global )) {
                        // Check whether the companies for which we are creating plan has planItems ownership
                        if(!companies.containsAll(planProduct.entities)) {
                            isValid = false
                        }
                    }
                }
				if(!isValid) {
					String []errors = new String[1];
					errors[0] = "PlanWS.planItem,planItemsScope,validation.error.plan.planItem.invalid.scope," + description;
					throw new SessionInternalError("Selected products must be in Plan's scope.", errors);
				}
		}
		
	}

    def retrieveMetaFields (){
        List entities = params['entities'].tokenize(",")

        def availableFields = new ArrayList<MetaField>();
        for(String entityId : entities) {
            availableFields.addAll(MetaFieldBL.getAvailableFieldsList(Integer.parseInt(entityId), EntityType.PLAN))
        }

        render template : '/metaFields/editMetaFields',
                model : [availableFields: availableFields, fieldValues: null]
    }
	
	def retrieveAllMetaFields (){
		def availableFields = new ArrayList<MetaField>();
		for(Integer entityId : retrieveCompaniesIds()) {
			availableFields.addAll(retrieveAvailableMetaFields(entityId))
		}
		
		render template : '/metaFields/editMetaFields',
				model : [availableFields: availableFields, fieldValues: null]
	}

    def getAvailableMetaFields (){
		render template : '/metaFields/editMetaFields',
				model : [availableFields: retrieveAvailableMetaFields(session['company_id']), fieldValues: null]
	}
    
    def retrieveAvailableMetaFields(entityId) {
		List<MetaField> metaFields = MetaFieldBL.getAvailableFieldsList(entityId, EntityType.PLAN);
        //get all dependencies. If we only load in the view it causes a connection leak
        for(MetaField metaField : metaFields) {
            metaField.getDependentMetaFields().size()
        }
        return metaFields
	}
	
	def bindMetaFields(plan, params, isRoot, product) {
		def fieldsArray
		MetaFieldValueWS[] metaFields = null
		List<MetaFieldValueWS> values = new ArrayList<MetaFieldValueWS>()
		if(isRoot) {
			for(Integer entityId : retrieveCompaniesIds()) {
				fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(entityId), params)
				metaFields = fieldsArray
				values.addAll(fieldsArray)
				plan.metaFieldsMap.put(entityId, metaFields)
			}
		} else {
            // this is for handling global plan metafield bind from child company.
            if (product.entityId != null && product?.entityId != session['company_id']){
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(product?.entityId), params)
                metaFields = fieldsArray
                values.addAll(fieldsArray)
                plan.metaFieldsMap.put(product.entityId, metaFields)
            }else{
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(session["company_id"]), params)
                metaFields = fieldsArray
                values.addAll(fieldsArray)
                plan.metaFieldsMap.put(session["company_id"], metaFields)
            }

		}
		plan.metaFields = values
	}

	/**
	 * retrieve meta fields consistent with front end selection
	 * 
	 * @param product
	 * @return
	 */
	def setMetaFieldsInConversation(product) {
		def availableFields = new ArrayList<MetaField>();
		Integer entityId= product.entityId?: session['company_id'] as Integer
		//select meta fields
		availableFields = retrieveAvailableMetaFields(entityId)
		
		List aList = new ArrayList<>()
		aList.add(0, availableFields)
		aList.add(1, entityId)
		return aList
	}
	
	/**
	 * Added to address issue #7432 wherein 'Selection Category' dropdown on product tab appears empty.
	 * @param excludeCategoryId
	 * @return
	 */
	def getProductCategories(excludeCategoryId) {
		def isRoot = new CompanyDAS().isRoot(session['company_id'])
	   
		List result = ItemTypeDTO.createCriteria().list {
			and {
				eq('internal', false)
				createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
				or {					
					'in'('entities.id', retrieveCompaniesIds())
					//list all gloal entities as well
					and {
						eq('global', true)
						eq('entity.id', companyService.getRootCompanyId())
					}
				}
				if(null != excludeCategoryId){
					notEqual('id', excludeCategoryId)
				}
			}
			order('id', 'desc')
		}
		
		return result.unique()
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
	
	def retrieveCompaniesIds() {
		def hierachyEntityIds = companyService.getEntityAndChildEntities()*.id
		hierachyEntityIds
	}
	
}
