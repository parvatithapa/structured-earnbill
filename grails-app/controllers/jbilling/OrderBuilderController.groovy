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
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.adennet.ws.FeeWS
import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS
import com.sapienter.jbilling.server.discount.DiscountLineWS
import com.sapienter.jbilling.server.discount.DiscountableItemWS
import com.sapienter.jbilling.server.item.AssetBL
import com.sapienter.jbilling.server.item.AssetStatusDTOEx
import com.sapienter.jbilling.server.item.ItemBL
import com.sapienter.jbilling.server.item.ItemDependencyType
import com.sapienter.jbilling.server.item.db.AssetReservationDTO
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.ItemTypeBL
import com.sapienter.jbilling.server.item.PlanBL
import com.sapienter.jbilling.server.item.PlanItemBL
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.item.db.PlanItemDTO
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.order.ApplyToOrder
import com.sapienter.jbilling.server.order.CancellationFeeType
import com.sapienter.jbilling.server.order.OrderBL
import com.sapienter.jbilling.server.order.OrderChangeBL
import com.sapienter.jbilling.server.order.OrderHelper
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS
import com.sapienter.jbilling.server.order.OrderChangeTypeWS
import com.sapienter.jbilling.server.order.OrderChangeWS
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.order.OrderStatusFlag
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.SwapMethod
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator
import com.sapienter.jbilling.server.pricing.PriceModelBL
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.process.db.ProratingType
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserCodeDAS
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.Util

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.httpclient.HttpState
import org.apache.http.HttpStatus

import java.math.RoundingMode

import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import java.time.OffsetDateTime

import static com.sapienter.jbilling.server.adennet.AdennetConstants.SOURCE_POS
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.SIM_PRICE_ID

/**
 * OrderController
 *
 * @author Brian Cowdery
 * @since 20-Jan-2011
 */
@Secured(["hasAnyRole('ORDER_20', 'ORDER_21', 'ORDER_200')"])
class OrderBuilderController {

    static pagination = [max: 10, offset: 0, sort: 'applicationDate', order: 'desc']
	static scope = "prototype"

    IWebServicesSessionBean webServicesSession
    def viewUtils

    def breadcrumbService
    def productService
    def messageSource
    SecurityValidator securityValidator
    def adennetHelperService

    def index () {
        redirect action: 'edit'
    }

    def editFlow = {//do not change this to method. Webflow will not be able to find it.
        /**
         * Initializes the order builder, putting necessary data into the flow and conversation
         * contexts so that it can be referenced later.
         */
        // subscriber number required to call UMS
        List<Integer> assetList = new ArrayList<>()

        initialize {
            action {
                if (!params.id && (!SpringSecurityUtils.ifAllGranted("ORDER_20") && (params.int('userId') == session['user_id'] && !SpringSecurityUtils.ifAllGranted("ORDER_200"))) ) {
                    // not allowed to create
                    redirect controller: 'login', action: 'denied'
                    return
                }

                if (params.id && !SpringSecurityUtils.ifAllGranted("ORDER_21")) {
                    // not allowed to edit
                    redirect controller: 'login', action: 'denied'
                    return
                }

                def order = params.id ? webServicesSession.getOrder(params.int('id')) : new OrderWS()

                if(order.id) {
                    securityValidator.validateUserAndCompany(order, Validator.Type.EDIT, true)
                }

                Comparator lineComp = {a,b->((a.parentLine == null && b.parentLine == null) ? (a.hashCode() <=> b.hashCode()) :
                    ((a.parentLine && b.parentLine) ? (a.parentLine.hashCode() == b.parentLine.hashCode() ? (a.description <=> b.description) : a.parentLine.hashCode() <=> b.parentLine.hashCode()) :
                        (a.parentLine == null ? (a.hashCode() == b.parentLine.hashCode() ?  -1 : a.hashCode() <=> b.parentLine.hashCode()) :
                            (a.parentLine.hashCode() == b.hashCode() ? 1 : a.parentLine.hashCode() <=> b.hashCode()) )))}

                order.orderLines = order.orderLines.sort(lineComp)

                if (!order) {
                    log.error("Could not fetch WS object")
                    orderNotFoundErrorRedirect(params.id)
                    return
                }

                if (order?.deleted==1) {
                	log.error("order is deleted, redirect to list")
                    orderNotFoundErrorRedirect(params.id)
                    return
                }

				if (order?.orderLines) {
					def lineTypeIds = order.orderLines.collect {it?.typeId}.unique()
					log.debug lineTypeIds.size()
					if (lineTypeIds.get(0) as Integer == Constants.ORDER_LINE_TYPE_DISCOUNT) {
						cannotEditDiscountOrder()
						return
					}
				}
				
                def user = UserDTO.get(order?.userId ?: params.int('userId'))
                def accountType = user?.customer?.accountType
                def contact = ContactDTO.findByUserId(order?.userId ?: user?.id)

                def company = CompanyDTO.get(session['company_id'])
                def currencies =  new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)

				def usedCategories = new ArrayList<Integer>();
				
                // set sensible defaults for new orders
                if (!order.id || order.id == 0) {
                	order.userId        = user.id
                    order.currencyId    = (currencies.find { it.id == user.currency.id} ?: company.currency).id
                    order.period        = Constants.ORDER_PERIOD_ONCE
                    order.billingTypeId = Constants.ORDER_BILLING_POST_PAID
                    order.activeSince   = TimezoneHelper.currentDateForTimezone(session['company_timezone'])
                    order.orderLines    = []
                    order.id            = -1
					order.dueDateUnitId = Constants.PERIOD_UNIT_DAY
                }
				def customerBillingCycleUnit = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId();
				def customerBillingCycleValue = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getValue();
				order.customerBillingCycleUnit = customerBillingCycleUnit
				order.customerBillingCycleValue = customerBillingCycleValue
				disableIsProrate(order, user)

                // add breadcrumb for order editing
                if (params.id) {
                    breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))
                }

                // map between itemId and List of asset ids for the original order
                //only for ItemTypes where the default status is available
                OrderBL bl = new OrderBL()
				def orderItemAssetsMap = [:]
                order.orderLines.each {
                    if(it.assetIds) {
                        def itemId = it.itemId
                        ItemTypeDTO itemTypeDTO = new ItemTypeBL().findItemTypeWithAssetManagementForItem(itemId)
                        if(itemTypeDTO.findDefaultAssetStatus().isAvailable == 1) {
                            def assets = orderItemAssetsMap[itemId]
                            if(!assets) {
                                assets = []
                                orderItemAssetsMap[itemId] = assets
                            }
                            it.assetIds.each {
                                assets << it
                            }
                        }
                    }

					bl.isCompatible(order.userId, ItemDTO.get(it.itemId), order.activeSince, order.activeUntil, usedCategories, it)
                }

                // available order periods, statuses and order types
                log.debug("Company Id*****: "+company?.id)
                def itemTypes = productService.getItemTypes(user.company.id, null)

                def orderStatuses = OrderStatusDTO.list().findAll { 
					it.getOrderStatusFlag() != OrderStatusFlag.SUSPENDED_AGEING && it?.entityId==company?.id }
				
                def orderPeriods = company.orderPeriods.collect { new OrderPeriodDTO(it.id) } << new OrderPeriodDTO(Constants.ORDER_PERIOD_ONCE)
                orderPeriods.sort { it.id }
                def periodUnits = PeriodUnitDTO.list()
				def cancellationFeeTypes = CancellationFeeType
                def orderBillingTypes = [
                        new OrderBillingTypeDTO(Constants.ORDER_BILLING_PRE_PAID),
                        new OrderBillingTypeDTO(Constants.ORDER_BILLING_POST_PAID)
                ]

                def rootOrder = OrderHelper.findRootOrderIfPossible(order)
                removeDeletedOrdersFromHierarchy(rootOrder)

                // model scope for this flow
                flow.company = company
                flow.itemTypes = itemTypes
                flow.orderStatuses = orderStatuses
                flow.orderPeriods = orderPeriods
                flow.periodUnits = periodUnits
                flow.orderBillingTypes = orderBillingTypes
                flow.user = user
                flow.contact = contact
				flow.cancellationFeeTypes = cancellationFeeTypes

                //initialize pagination parameters
                params.max = params?.max?.toInteger() ?: pagination.max
                params.offset = params?.offset?.toInteger() ?: pagination.offset

                // conversation scope
                conversation.order = order
                conversation.nextFakeId = -2; // id generator for hierarchy elements
                conversation.products = productService.getFilteredProducts(company, params, accountType, false, true)
                conversation.products?.each{it.getEntities().size(); it.getDefaultPrices().size(); }
                conversation.maxProductsShown = params.max
                conversation.plans = productService.getFilteredPlans(company, params, true)
                conversation.plans?.each{it.getEntities().size(); it.getDefaultPrices().size(); PlanItemBL.collectPlanItemsIncludingNested(it.plans.asList().get(0));}
                conversation.maxPlansShown = params.max
                conversation.discountableItems = []
                conversation.availableFields = getAvailableMetaFields()
                conversation.pricingDate = order.activeSince ?: TimezoneHelper.convertToTimezone(order.createDate, session['company_timezone']) ?: TimezoneHelper.currentDateForTimezone(session['company_timezone'])
                conversation.selectedAssets = []
                //assets for the same Item which belongs to other lines on the order
                conversation.assetsForItemOnOrder = []
                //available metafields for the asset for filtering
                conversation.assetMetaFields = []
                // map between itemId and List of asset ids for the original order
                conversation.orderItemAssetsMap = orderItemAssetsMap
                //assets which were on the saved order, but have been removed in the UI
                //they must also appear under the list of available assets
                conversation.assetsToInclude = []
                conversation.itemTypeDefaultStatusAvailable;
                conversation.maxChangesShown = params.max
                conversation.changesFilterBy = ''
                conversation.changesFilterStatusId = null
                conversation.allChanges = collectOrderChanges(rootOrder, conversation.products, conversation.plans)
                /*
                    map for store orderLines' and orderChanges' dependencies in format
                        key : line|change_<id>
                        value = {type: mandatory|optional, productId: <item id of line or change>, met: <true|false (is dependency satisfied or not)>}
                */
                conversation.productDependencies = recalculateProductDependencies(rootOrder, conversation.allChanges, conversation.products, conversation.plans)
                conversation.orderChanges = []
                conversation.currentOrderChangeId = []
                conversation.orderChangeStatuses = webServicesSession.getOrderChangeStatusesForCompany() as List
                conversation.orderChangeTypes = webServicesSession.getOrderChangeTypesForCompany() as List
                conversation.defaultOrderChangeType = conversation.orderChangeTypes.find {it.defaultType}
                conversation.orderChangeUserStatuses = conversation.orderChangeStatuses.findAll { it.id != Constants.ORDER_CHANGE_STATUS_APPLY_ERROR && it.id != Constants.ORDER_CHANGE_STATUS_PENDING }
                conversation.applyOrderChangeStatus = conversation.orderChangeStatuses.find {it.applyToOrder == ApplyToOrder.YES }
                conversation.persistedOrderOrderLinesMap = [:]
                conversation.persistedOrderOrderLinesMap.put(rootOrder.id, rootOrder.orderLines)
                OrderHelper.findAllChildren(rootOrder).each {
                    conversation.persistedOrderOrderLinesMap.put(it.id, it.orderLines)
                }

                // #7043 - Agents && Commissions - Get the ids of the Partner and its children.
                UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
                def partnerIds = []
                if (loggedInUser.getPartner() != null) {
                    partnerIds << loggedInUser.partner.user.id
                    if (loggedInUser.partner.children) {
                        partnerIds += loggedInUser.partner.children.user.id
                    }
                }

                //get all the logged in user's user codes. If the user is a Partner then we have to show the User Codes for the Partner and Sub-Partners if it corresponds.
                def userCodes = []
                if (partnerIds) {
                    userCodes = new UserCodeDAS().findActiveForPartner(partnerIds).collect { it.identifier }
                } else {
                    userCodes = new UserCodeDAS().findActiveForUser(session['user_id'] as int).collect { it.identifier }
                }

                conversation.userCodes = userCodes
				conversation.usedCategories = usedCategories
				conversation.hasSubscriptionProduct = new UserDAS().hasSubscriptionProduct(user.id)
				
				//#7853 - If no order statuses are configured thro' the configuration menu an exception is shown on the 'create order' UI.
				//Following exception handling added to take care of the issue.
				//Also, once an exception occurs in the initialize block, the session is getting cleared even after handling the exception thus
				//impacting the initialization code that comes after catch block. So call to getDefaultOrderStatusId was moved here at the end of block.
				//Idea is to just swallow the exception here and show user a proper message later while validating the order.   
				if (order.id == -1) {
					try {
						def orderStatus = webServicesSession.findOrderStatusById ( webServicesSession.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, session['company_id'].toInteger()) )
						order.orderStatusWS   = orderStatus
					} catch (Exception e) {
						log.error('No order status found for the order')
					}
				}
				conversation.billingConfiguration = webServicesSession.getBillingProcessConfiguration()
            }
            on("success").to("build")
        }

        /**
         * Renders the order details tab panel.
         */
        showDetails {
            action {
            	def orderStatuses = OrderStatusDTO.list().findAll{it?.entityId==flow?.company?.id}
            	def idVsDescription = ""
            	for(OrderStatusDTO statusDTO: orderStatuses){
	            	idVsDescription = idVsDescription+statusDTO.orderStatusFlag+":"+statusDTO.id.toString()+",";
            	}
                disableIsProrate(conversation.order, flow.user)
            	params.template = 'details'
                params.idVsDescription = idVsDescription
            }
            on("success").to("build")
        }

        /**
         * New Discounts : Renders the Discount details tab panel.
         */
        showDiscounts {
            action {
				params.template = 'discounts'
				conversation.discountableItems = []
				
				// add order line level discountable items to dropdown
				conversation.order?.orderLines.each {
					if(!it.itemId) {
                        return
                    }
					def today = TimezoneHelper.currentDateForTimezone(session['company_timezone'])
					def product = ItemDTO.get(it.itemId)
					
					conversation.discountableItems << new DiscountableItemWS(it.itemId, 
															"item", 
															it.description + 
																" - Qty=" + new Double(it.quantity).toInteger() +
																(product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])?.type != PriceModelStrategy.LINE_PERCENTAGE ?
																	" - Rate=" + 
																	Util.formatMoney(new BigDecimal(it.price?:0), 
																	conversation.order?.userId, 
																	conversation.order?.currencyId, false) :
																	" - Percentage=" + Util.formatPercentage(new BigDecimal(it.price?:0), conversation.order.userId)
																), 
															new BigDecimal(it.amount?:0).setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND).toString())
					
					
					// add plan item bundle level items to dropdown .. if any
					if (product?.plans) {
						product.plans.each { 
							if (it.planItems) {
								it.planItems.each { 
									if (it.bundle?.quantity && conversation.order?.orderLines) {
										conversation.discountableItems << new DiscountableItemWS( it.id, 
												"planItem", 
												(it.plan.description?:it.plan.item.description) + " - " + 
													it.item.description + " - Qty=" + it.bundle?.quantity?.toInteger() + 
													(it.item?.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['companyId'])?.type != PriceModelStrategy.LINE_PERCENTAGE ?
														" - Rate=" + Util.formatMoney( it.getPrice(today)?.rate?:BigDecimal.ZERO, conversation.order.userId, it.getPrice(today)?.currency?.id?:1, false) :
														" - Percentage=" + Util.formatPercentage(new BigDecimal(it.item.percentage), conversation.order.userId) 
												), null
										)
									}
								}
							}
						}
					}
				}
				
				// add order level as discountable item to dropdown
				conversation.discountableItems << new DiscountableItemWS(null, "order", messageSource.getMessage('discount.level.order', null, session.locale))
				
            }
            on("success").to("build")
        }
        
        /**
	     * New Discounts : Adds or removes the discount line from discounts tab. 
	     */
	    addRemoveDiscountLine {
	    	action {
	    	
				log.debug "to do ${params.discountLineWhatToDo}"
				
	    		if (params.discountLineWhatToDo == "addDiscountLine") {
	    			// Adds a new discount line to the order and render the discounts tab.
			        
			        def discountLineIndex = params.int('discountLineIndex')
					log.debug "discountLineIndex: ${discountLineIndex}"
			        SortedMap<String, String> discountLineParams = new TreeMap<String, String>()
			        
			        params.each {
			        	if (it.key.startsWith("discountableItem.${discountLineIndex}") || it.key.startsWith("discount.${discountLineIndex}")) {
							if ( !('' == it.value)) {
								discountLineParams.put(new String(it.key), new String(it.value))
								log.debug( "** Discount Line Key=${it.key}, Value=${it.value}")
							}
			        	}
			        }
			        
			        // add discount line to order
			        def order = conversation.order
			        def dlines = order?.discountLines ? new ArrayList<DiscountLineWS>(order.discountLines as List) : new ArrayList<DiscountLineWS>()
					
					def dline = new DiscountLineWS()
					
					String[] lineLevelDetailsArray = new String[4]
					def lineLevelDetails = discountLineParams.remove("discountableItem.${discountLineIndex}.lineLevelDetails")
					def discountId = discountLineParams.remove("discount.${discountLineIndex}.id")
					
					log.debug "**********************"
					log.debug "discountable ${lineLevelDetails}"
					log.debug "discountId ${discountId}"
					log.debug "**********************"

					dline.lineLevelDetails = lineLevelDetails
					
					lineLevelDetailsArray = lineLevelDetails?.split("\\" + Constants.PIPE)
					def itemId = ((lineLevelDetailsArray && lineLevelDetailsArray.length > 0) ? lineLevelDetailsArray[0] : null)
	        		def discountLevel = ((lineLevelDetailsArray && lineLevelDetailsArray.length > 1) ? lineLevelDetailsArray[1] : null)
	        		def lineAmount = ((lineLevelDetailsArray && lineLevelDetailsArray.length > 2) ? lineLevelDetailsArray[2] : null)
	        		def lineDescription = ((lineLevelDetailsArray && lineLevelDetailsArray.length > 3) ? lineLevelDetailsArray[3] : null)
	        		
	        		dline.discountId = discountId ? new Integer(discountId) : null
			        dline.orderId = conversation.order?.id
			        dline.orderLineAmount = (null == lineAmount || 'null' == lineAmount) ? null : lineAmount
			        dline.description = lineDescription 
			        
			        if ("item".equals(discountLevel)) {
			        	dline.itemId = itemId ? new Integer(itemId) : null
			        } else if ("planItem".equals(discountLevel)) {
			        	dline.planItemId = itemId ? new Integer(itemId) : null
			        }
			        
			        dlines?.add(dline)
			        
			        order.discountLines = dlines?.toArray()
			        conversation.order = order
			        
		        } else if (params.discountLineWhatToDo == "removeDiscountLine") {
		        	// New Discounts : Used for removing a discount line from discounts tab.
					def idx= params.int('discountLineIndex')
					log.debug "remove discountLineIndex ${idx}"
		    		def dline = conversation.order.discountLines? conversation.order.discountLines[idx] : null
					log.debug "discount line to remove is ${dline}"
					if ( null != dline ) {
						def dlines = conversation.order.discountLines as List
						dlines.remove(dline)
						conversation.order.discountLines = dlines? dlines.toArray() : null
					}
		        }
		        
		        params.template = 'discounts'
		        
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

                if (null == params['filterBy'])
                    params['filterBy'] = ""

				def customerCo = UserDTO.get(conversation.order.userId)?.company
                def accountType = flow.user?.customer?.accountType
                params.template = 'products'
                conversation.products = productService.getFilteredProductsForCustomer(flow.company, null, params, accountType, false, true, customerCo)
                conversation.products?.each{it.getEntities().size(); it.getDefaultPrices().size(); it.getItemTypes().size(); }
                conversation.maxProductsShown = params.max
            }
            on("success").to("build")
        }

        /**
         * Renders the plans list tab panel, filtering the plans list by the given criteria.
         */
        showPlans {
            action {
                params.max = params?.max?.toInteger() ?: pagination.max
                params.offset = params?.offset?.toInteger() ?: pagination.offset

                if (null == params['filterBy'])
                    params['filterBy'] = ""

                if (params.boolean('forSwapPlan')) {
                    params.template = 'planSwapTable'
                } else {
                    params.template = 'plans'
                }
                conversation.plans = productService.getFilteredPlans(flow.company, params, true)
                conversation.plans?.each{it.getEntities().size(); it.getDefaultPrices().size(); it.getItemTypes()?.size();
                    for (def planItem : it.plans.asList().first()?.planItems) {
                        planItem?.item.assetManagementEnabled
                        planItem?.bundle?.quantity
                    }
                }
                conversation.existedPlanItemIdForFilter = params.existedPlanItemIdForFilter
                conversation.maxPlansShown = params.max
            }
            on("success").to("build")
        }

        /**
         * Adds a product to the order as a new order change and renders the review panel.
         */
        addOrderLine {
            action {
                flash.errorMessages = null
                flash.error = null
                // build line
                def line = new OrderLineWS()
                // set fake id to hierarchy build and navigate
                def isApplyNow = PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'],Constants.PREFERENCE_APPLY_NOW)
                line.id = conversation.nextFakeId;
                conversation.nextFakeId = conversation.nextFakeId - 1
                line.typeId = Constants.ORDER_LINE_TYPE_ITEM
                line.quantity = BigDecimal.ONE
                line.itemId = params.int('id')
                line.useItem = true
                line.metaFields = new MetaFieldValueWS[0]
				
                def product = findProduct(line.itemId, conversation.products, conversation.plans)

				if(!conversation.hasSubscriptionProduct) {
                    for (def type : product.itemTypes) {
                        if (type.orderLineTypeId == Constants.ORDER_LINE_TYPE_SUBSCRIPTION.intValue()) {
                            conversation.hasSubscriptionProduct = true
                        }
                    }
                }

				if(product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id']).getType() == PriceModelStrategy.LINE_PERCENTAGE) {
					line.isPercentage = true
				}

				OrderBL bl = new OrderBL();
				Boolean success = true;
				if(!bl.isPeriodValid(product, conversation.order.activeSince, conversation.order.activeUntil)) {
					success = false;
					String[] errors = ["validation.order.line.not.added.valdidity.period"]
                    conversation.errorMessages = [g.message(code: "validation.order.line.not.added.valdidity.period")]
                    viewUtils.resolveException(flash, session.locale,
						new SessionInternalError("Validity period of order should be within validity period of plan/product", errors))
                    return
				}

				if(!bl.isCompatible(conversation.order.userId, product, conversation.order.activeSince, conversation.order.activeUntil, conversation.usedCategories, line)) {
                    success = false;
					String[] errors = ["validation.order.line.not.added.not.compatible"]
                    conversation.errorMessages = [g.message(code: "validation.order.line.not.added.not.compatible")]
					viewUtils.resolveException(flash, session.locale,
						new SessionInternalError("User can subscribe only to one plan/product from given category", errors))
					return;
				}

                Date date = TimezoneHelper.convertToTimezoneByEntityId(!conversation.plan ? conversation.pricingDate : conversation.order.activeSince ?: conversation.order.createDate ?: new Date(), session['company_id'])
                line.description = product.getDescription(session['language_id'])
                if (!conversation.plan) {
                    line.price =  new ItemBL(line.itemId).getPrice(conversation.order?.userId,
                                                                   conversation.order?.currencyId,
                                                                   BigDecimal.ONE,
                                                                   session['company_id'],
                                                                   null, null, true, date);

                    if (line.price) {
                        line.setAmount(line.getQuantityAsDecimal().multiply(line.priceAsDecimal))
                    }
                } else {
                    if (!product.percentage) {
                        def priceModelDto = product.getPrice(date, session['company_id'])
                        line.price = priceModelDto?.rate?:0
                    }
                }

                if(conversation.plan) {
                    params.plan = true
                }
                conversation.plan = false

                //add the assets to the line
                def assetIds = []
                def lineProduct = findProduct(line.itemId, conversation.products, conversation.plans)
                conversation.selectedAssets?.each {
                    assetIds << it.id
                    if (PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_CALL_UMS_ON_CREATE_ORDER)) {
                        assetList.add(it.getSubscriberNumber())
                    }
                }
                if (lineProduct.assetManagementEnabled == 1 ) {
                    //set the quantity on the line equal to the number of assets
                    line.quantity = assetIds.size()
                    line.assetIds = assetIds.toArray(new Integer[assetIds.size()])

                    /* Some assets have been added to an order, due that, they must be reserved */
                    if(!assetIds.isEmpty()) {
                        line.assetIds = assetIds.toArray(new Integer[assetIds.size()])
                    }
                }

                //clear the selected assets from the conversation
                conversation.selectedAssets = []
                def order = conversation.order
                def parentLine = params.parentLineId ? order.orderLines.find{ it.id == params.int('parentLineId') } : null
                def parentChange = params.parentChangeId ? conversation.allChanges.get(params.int('parentChangeId'))?.change : null
                def targetOrder = params.orderId ? OrderHelper.findOrderInHierarchy(order, params.int('orderId')) : order

                line.orderId = targetOrder.id

                if (parentLine) {
                    line.parentLine = parentLine as OrderLineWS
                }

                //if asset management is enabled and the line has no assets, don't add it
                if(!line.hasAssets() && lineProduct.assetManagementEnabled == 1 && !conversation.plan
                        && (!parentChange && !parentLine)) { //add orderChange with ZERO quantity to dependency line
                    String[] errors = ["OrderLineWS,assetIds,validation.order.line.not.added.no.assets"]
                    viewUtils.resolveException(flash, session.locale,
                            new SessionInternalError("Line with no assets not added to order.", errors))
                } else {
                    // add new order change for line
                    List<OrderChangeWS> newChanges = addOrderChangesForLine(line, targetOrder, conversation.allChanges,
                            conversation.products, conversation.plans,
                            conversation.nextFakeId, conversation.orderChangeUserStatuses,
                            conversation.defaultOrderChangeType?.id, conversation.pricingDate, assetIds,isApplyNow
                    )

                    def newChange = newChanges.first()
                    conversation.selectedAssets = []
                    conversation.nextFakeId = newChanges.min{ it.id }.id - 1
                    conversation.currentOrderChangeId = [newChange.id]

                    if (parentChange) {
                        if (parentChange.id && parentChange.id > 0) {
                            newChange.parentOrderChangeId = parentChange.id
                        } else {
                            newChange.parentOrderChange = parentChange
                        }
                    }
                    // assets were not selected for dependent line - force show assets
                    if (lineProduct.assetManagementEnabled == 1 && (parentChange || parentLine)) {
                        conversation.forceDisplayAssetsDialogForChangeId = newChange.id
                    }
                }

                def discountLines = conversation.order?.discountLines

                // put initial order
                conversation.order = rateOrderIfPossible(OrderHelper.findOrderInHierarchy(targetOrder, order.id), flash,
                        findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
                conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)
                if (params.newOrder) {
                    params.newOrder = false;
                    flash.displayEditChangesTab = true
                    String methodName = "goToNewOrder"
                    conversation.errorMessages = flash.errorMessages

                    return "$methodName"()
                } else {
                    params.template = 'editOrderChange'
                }

				// remove premature meta fields validation error
				def remove = new ArrayList<Integer>()
				int i = 0
				for(def message : flash.errorMessages) {
					if(message.contains("required")) {
						remove.add(i)
					}
					i++
				}
				i = 0 
				for(def r : remove) {
					flash.errorMessages.remove(r - i)
					i++
				}
				
                conversation.order.discountLines = discountLines
                
                order.discountLines = discountLines
				
				disableIsProrate(order, flow.user)
				
                conversation.order = order
				conversation.plan=false
                params.template = 'review'

            }

            on("success") {
                if (flash.errorMessages)
                    conversation.errorMessages = flash.errorMessages
            }.to("showEditOrderChanges")
            on("goToNewOrder").to("changeDisplayOrder")
        }

        /**
         * Adds a plan to the order as a new order change and renders the editOrderChange panel.
         *
         * The builder does not allow multiple plans to be added to the same order. If the order
         * already contains a plan subscription item, an error message will be displayed.
         */
        addOrderPlanLine {
            action {
                def order = conversation.order
                def plans = conversation.plans // list of ItemDTO objects
				def billingConfiguration = conversation.billingConfiguration
				def user = UserDTO.get(order?.userId ?: params.int('userId'))
				
                conversation.plan = true
                // update the order to reflect the subscribed plan
                def itemId = params.int('id')
                def plan = plans.find{ it.id == itemId }?.plans?.asList()?.first() // PlanDTO object

				boolean success = true;

                def alreadyHasPlan = order.orderLines.find{ l -> plans.find{ p -> p.id == l.itemId && l.deleted == 0 }} != null
                if (!alreadyHasPlan) {
                    // force order period to the plan's period
                    order.period = plan.period.id
                }

                if (OrderBL.countPlan(conversation.order.orderLines) > 0) {
                    success = false
                    String msg = "validation.order.should.not.contain.multiple.plans"
                    conversation.errorMessages = [g.message(code: msg)]
                    viewUtils.resolveException(flash, session.locale,
                            new SessionInternalError("Order should not contain multiple plans",[msg] as String[]))
                    return
                }

				// Check if the customer has been subscribed to other plans that have any of the products repeated in them.
				// We have to show a warning message saying that the pricing will be overridden by the last plan.
				boolean hasRepeatedProductsInPlans = false
				plan.planItems.item.id.each { planItemId ->
					new PlanBL().getPlansByAffectedItem(planItemId).each {
						if (!hasRepeatedProductsInPlans && new PlanBL(it.id).isSubscribed(flow.user.id, order.activeSince)) {
							hasRepeatedProductsInPlans = true
						}
					}
				}

				if (hasRepeatedProductsInPlans) {
					conversation.repeatedProductError = [message(code: 'validation.error.repeated.products.in.plans')]
					conversation.repeatedProductPlanId = plan.item.id
				}

				ProratingType companyLevelProratingType = billingConfiguration.getProratingType();

				def billingCycleUnit = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId();
				def billingCycleValue = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getValue();

				Integer planPeriodUnit = plan.getPeriod().getPeriodUnit().getId();
				Integer planPeriodValue = plan.getPeriod().getValue();

				if (order.getProrateFlag() && companyLevelProratingType.isProratingAutoOn()) {
					if(!planPeriodUnit.equals(billingCycleUnit) && planPeriodValue.equals(billingCycleValue)) {
						conversation.notProratingMessage = [message(code: 'plan.period.unit.should.equal')]
						conversation.proratePlanId = plan.item.id
					}
				}

				if(!success) {
					params.template = 'editOrderChange'
					error()
				}
            }
            on("success").to("addOrderLine")
            on("error").to("build")
        }

        /**
         * Adds dependent product to new order as a new child order line and renders all panels.
         */
        addOrderLineToNewOrder {
            action {
                OrderWS parentOrder = conversation.order
                flash.errorMessages = null
                flash.error = null

                if (OrderHelper.findAllChildren(OrderHelper.findRootOrderIfPossible(parentOrder)).size() >
                        (OrderHierarchyValidator.MAX_ORDERS_COUNT - 1)) {
                    flash.errorMessages = [ message(code: 'error.order.hierarchy.too.big') ];
                    error()
                } else {
                    OrderWS newOrder = new OrderWS()
                    newOrder.userId = parentOrder.userId
                    newOrder.currencyId = parentOrder.currencyId

					//#7853 - If no order statuses are configured thro' the configuration menu an exception is shown on the 'create order' UI.
					//Following exception handling added to take care of the issue.
					try {
						def orderStatus = webServicesSession.findOrderStatusById ( webServicesSession.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, session['company_id'].toInteger()) )
						newOrder.orderStatusWS   = orderStatus
					} catch (SessionInternalError e) {
						viewUtils.resolveException(flash, session.locale, e)
					}

                    newOrder.period = Constants.ORDER_PERIOD_ONCE
                    newOrder.billingTypeId = Constants.ORDER_BILLING_POST_PAID
                    newOrder.activeSince = TimezoneHelper.currentDateForTimezone(session['company_timezone'])
                    newOrder.orderLines = []
                    newOrder.userCode = parentOrder.userCode

                    // set fake id to hierarchy build and navigate
                    newOrder.id = conversation.nextFakeId;
                    conversation.nextFakeId = conversation.nextFakeId - 1

                    newOrder.parentOrder = parentOrder
                    def newChildren = parentOrder.childOrders as List
                    if (!newChildren) newChildren = []
                    newChildren.add(newOrder)
                    parentOrder.childOrders = newChildren.toArray(new OrderWS[newChildren.size()])

                    params.orderId = newOrder.id
                    params.newOrder = true
                }
            }
            on("success").to("addOrderLine")
            on("error").to("build")
        }

        /**
         * Updates an order line own fields and renders the review panel.
         */
        updateOrderLine {
            action {
                flash.errorMessages = null
                flash.error = null
                def order = conversation.order
                def discountLines = order?.discountLines ? new ArrayList<DiscountLineWS>(order.discountLines as List) : new ArrayList<DiscountLineWS>()
				
                // get existing line
                def lineId = params.int('lineId')
                def line = conversation.persistedOrderOrderLinesMap.get(order.id).find{it.id == lineId}
                if (!line) {
                    line = order.orderLines.find{it.id == lineId}
                }
                def item = ItemDTO.get(line.itemId)

                // update line
                bindData(line, params["line-${lineId}"])

                //bind the Order Line meta fields defined per product
                line.metaFields = MetaFieldBindHelper.bindMetaFields(item.orderLineMetaFields, params)

                params.template = 'review'
                conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
                conversation.order.discountLines = discountLines?.toArray()
				
            }
            on("success").to("build")
        }

        /**
         * Updates an order change field and renders editOrderChanges panel
         */
        updateOrderChange {
            action {
                conversation.errorMessages = null
                flash.errorMessages = null
                flash.error = null
                def order = conversation.order

                // get existing line
                def changeId = params.int('changeId')
                OrderChangeWS change = conversation.allChanges.get(changeId).change
                // update line
                bindData(change, params["change-${changeId}"])
                change.startDate=change.startDate?:TimezoneHelper.currentDateForTimezone(session['company_timezone'])
                if (params["change-${changeId}.useItem"] == "on") {
                    change.useItem = 1
                } else {
                    change.useItem = 0
                }
                if (params["change-${changeId}.appliedManually"] == "on"){
                    change.appliedManually = 1
                } else {
                    change.appliedManually = 0
                }
                if (null != order.nextBillableDay && change.startDate.after(order.nextBillableDay)) {
					change.nextBillableDate = change.startDate
				}

                def userAssignedStatus = conversation.orderChangeUserStatuses.find {it.id == change.userAssignedStatusId}
                change.userAssignedStatus = userAssignedStatus?.getDescription(session['language_id'])?.content
                // update plan item changes for new line
                if (change.orderLineId < 0 && params["change-${changeId}.itemIds"]) {
                    def changePlanItems = []
                    def itemIds = params["change-${changeId}.itemIds"] instanceof String ? Arrays.asList(params["change-${changeId}.itemIds"]) : params["change-${changeId}.itemIds"];
                    if (change.orderChangePlanItems) {
                        change.orderChangePlanItems.each {
                            def found = itemIds.any {itemIdStr -> Integer.valueOf(itemIdStr.toString()) == it.itemId }
                            if (!found) {
                                changePlanItems << it;
                            }
                        }
                    }
                    itemIds.each {
                        int itemId = Integer.valueOf(it.toString())
                        def orderChangePlanItem = change.orderChangePlanItems?.find {it.itemId == itemId}
                        if (!orderChangePlanItem) {
                            orderChangePlanItem = new OrderChangePlanItemWS()
                            PlanItemDTO planItem = PlanDTO.get(change.itemId).findPlanItem(itemId)
                            orderChangePlanItem.bundledQuantity = planItem?.bundle.quantity
                        }
                        Integer changePlanItemId = params.int("change-${changeId}-${itemId}.changePlanItemId")
                        Integer changePlanItemVersion = params.int("change-${changeId}-${itemId}.optLock")
                        def useItem = params["change-${changeId}-${itemId}.useItem"] ? true : false
                        def description = params["change-${changeId}-${itemId}.description"]
                        def assetIdx = 0
                        def newAssets = []
                        if (orderChangePlanItem && orderChangePlanItem.assetIds) {
                            newAssets.addAll(orderChangePlanItem.assetIds)
                        }
                        while(params["change-${changeId}-${itemId}.asset.${assetIdx}.id"]) {
                            if(!params["change-${changeId}-${itemId}.asset.${assetIdx}.status"]) {
                                newAssets = newAssets - params.int("change-${changeId}-${itemId}.asset.${assetIdx}.id")
                            }
                            assetIdx++
                        }
                        def planItemProduct = ItemDTO.get(itemId)
                        orderChangePlanItem.assetIds = newAssets.toArray(new int[newAssets.size()])
                        orderChangePlanItem.itemId = itemId
                        orderChangePlanItem.id = changePlanItemId ?: 0
                        orderChangePlanItem.optlock = changePlanItemVersion ?: 0
                        orderChangePlanItem.description = !useItem && description ? description : planItemProduct?.description
						Collection<MetaField> metaFields = planItemProduct?.orderLineMetaFields?.findAll { it.getDataType() != DataType.STATIC_TEXT }
                        orderChangePlanItem.metaFields = MetaFieldBindHelper.bindMetaFields((Collection<MetaField>)metaFields, (GrailsParameterMap)params)
                        if (!useItem || orderChangePlanItem.assetIds || orderChangePlanItem.metaFields) {
                            changePlanItems << orderChangePlanItem
                        }
                    }
                    change.orderChangePlanItems = changePlanItems.toArray(new OrderChangePlanItemWS[changePlanItems.size()])
                }
                // must have a quantity
                if (!change.quantity) {
                    change.quantity = BigDecimal.ZERO
                }

                // if product does not support decimals, drop scale of the given quantity
                boolean isPlan = false;
                def product = findProduct(change.itemId, conversation.products, conversation.plans)
                if (product.isPlan()) {
                    isPlan = true;
                }
                if(isPlan && change.quantityAsDecimal.remainder(BigDecimal.ONE) > 0){
                    try {
                        String [] errors = ["OrderLineWS,planQuantity,bean.OrderLineWS.validation.error.partial.quantity"]
                        throw new SessionInternalError("Partial quantity is not allowed for a plan.",
                                errors);
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flash, session.locale, e)
                        return
                    }
                } else if (product?.hasDecimals == 0) {
                    change.quantity = change.getQuantityAsDecimal().setScale(0, RoundingMode.HALF_UP)
                }

                //bind the Order Line meta fields defined per product
                if(!isPlan){
                    change.metaFields = MetaFieldBindHelper.bindMetaFields(product?.orderLineMetaFields?.findAll { it.getDataType() != DataType.STATIC_TEXT }, params)
                }
                //bind the Order Change Type meta fields
                if (change.orderChangeTypeId) {
					def orderChangeTypeMetaFields = MetaFieldBindHelper.bindMetaFields(OrderChangeTypeDTO.get(change.orderChangeTypeId).orderChangeTypeMetaFields, params)
                    change.metaFields = ArrayUtils.addAll(change.metaFields, orderChangeTypeMetaFields.toArray(new MetaFieldValueWS[orderChangeTypeMetaFields.size()]))
                }

                //remove assets the user deselected
                def assetIdx = 0
                def newAssets = []
                if (change.assetIds != null) newAssets.addAll(change.assetIds)
                while(params["change-${changeId}.asset.${assetIdx}.id"]) {
                    if(!params["change-${changeId}.asset.${assetIdx}.status"]) {
                        newAssets = newAssets - params.int("change-${changeId}.asset.${assetIdx}.id")
                        change.quantity = change.getQuantityAsDecimal() - 1
                    }
                    assetIdx++
                }

                change.assetIds = newAssets.toArray(new Integer[newAssets.size()])
                params.template = 'review'
                // recalculate dependencies: possibly not display dependencies for 'removed' lines
                conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
				// recalculate used categories

                try {
                    recalculateUsedCategories(conversation)
                } catch (SessionInternalError e) {
                    if (!viewUtils.resolveException(flash, session.locale, e)) {
                        flash.errorMessages = [flash.error]
                    }
                }
            }
            on("success"){
                if(!(flash.error || flash.errorMessages)){
                    conversation.message=g.message(code: 'order.line.update.success.message')
                }
            }.to("showEditOrderChanges")
        }
        /*
         * User wanted to add new assets to the change
         */
        updateOrderChangeAssets {
            action {
                flash.errorMessages = null
                flash.error = null

                // get order change
                def changeId = params.int('id')
                def itemId = params.int('assetFlowExitPlanItemId')
                OrderChangeWS change = conversation.allChanges.get(changeId).change
                OrderChangePlanItemWS orderChangePlanItem = null
                if (itemId) {
                    orderChangePlanItem = change.orderChangePlanItems.find {it.itemId == itemId}
                    if (!orderChangePlanItem) {
                        List<OrderChangePlanItemWS> planItems = new LinkedList<OrderChangePlanItemWS>();
                        if (change.orderChangePlanItems) {
                            planItems.addAll(change.orderChangePlanItems.toList());
                        }
                        orderChangePlanItem = new OrderChangePlanItemWS();
                        orderChangePlanItem.itemId = itemId
                        orderChangePlanItem.description = ItemDTO.get(itemId)?.description
                        orderChangePlanItem.assetIds = new int[0]

                        PlanItemDTO planItem = PlanDTO.get(change.itemId).findPlanItem(itemId)
                        orderChangePlanItem.bundledQuantity = planItem?.bundle.quantity
                        planItems.add(orderChangePlanItem);

                        change.orderChangePlanItems = planItems.toArray(new OrderChangePlanItemWS[planItems.size()])
                    }
                }

                int oldAssetsCount = orderChangePlanItem  ? orderChangePlanItem.assetIds.length : change.assetIds.length

                //add the assets to the line
                def assetIds = []
                conversation.selectedAssets?.each {
                    assetIds << it.id
                }
                if (orderChangePlanItem) {
                    orderChangePlanItem.assetIds = assetIds.toArray(new Integer[assetIds.size()])
                } else {
                    change.assetIds = assetIds.toArray(new Integer[assetIds.size()])
                }
                //clear the selected assets in the conversation
                conversation.selectedAssets = []

                if (!orderChangePlanItem && oldAssetsCount != change.assetIds.length) {
                    //set the quantity equal to the number of assets
                    change.setQuantity(change.getQuantityAsDecimal().add(BigDecimal.valueOf(change.assetIds.length - oldAssetsCount)))
                }

                conversation.productDependencies = recalculateProductDependencies(conversation.order, conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
                
            }
            on("success").to("showEditOrderChanges")
        }

        /**
         * Creates the change for remove line from the order and renders the editOrderChanges panel.
         */
        removeOrderLine {
            action {
                def order = conversation.order

                def lineId = params.int('lineId')
                def lines = order.orderLines as List

                // remove or mark as deleted if already saved to the DB
                def line = lines.find {it.id == lineId}
                List<OrderChangeWS> newChanges = addOrderChangesForLine(line, order, conversation.allChanges,
                        conversation.products, conversation.plans,
                        conversation.nextFakeId, conversation.orderChangeUserStatuses,
                        conversation.defaultOrderChangeType?.id, conversation.pricingDate, [],null)
                def newChange = newChanges.first()
                newChange.setQuantityAsDecimal(line.getQuantityAsDecimal().negate())
                newChange.assetIds = []
                newChange.removal = 1
                conversation.nextFakeId = newChanges.min{ it.id }.id - 1
                conversation.currentOrderChangeId = [newChange.id]
                // recalculate dependencies: possibly not display dependencies for 'removed' lines
                conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )

				// If line was a plan and plan period is diffent than customer period then we show note message. we need to remove the message.
				if(line.itemId == conversation.proratePlanId){
					conversation.remove("notProratingMessage")
					conversation.remove("proratePlanId")
				}
				
                // If line was a plan that had repeated products resulting in a warning message, we need to remove the message.
                if(line.itemId == conversation.repeatedProductPlanId){
                    conversation.remove("repeatedProductError")
                    conversation.remove("repeatedProductPlanId")
                }
            }
            on("success").to("showEditOrderChanges")
        }

        swapOrderPlan {
            action {
                def order = conversation.order

                def existedPlanItemId = params.int('existedPlanItemId')
                def swapPlanItemId = params.int('swapPlanItemId')
                def fromDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.effectiveDate)
                def swapMethod = SwapMethod.valueOf(params.swapMethod)

                def newChanges = webServicesSession.calculateSwapPlanChanges(order, existedPlanItemId, swapPlanItemId, swapMethod, fromDate)
                def nextId = conversation.nextFakeId
                newChanges.each {
                    it.id = nextId;
                    nextId--;
                    conversation.allChanges.put(it.id, [change: it, productDescription: it.getDescription(), changed: true]);
                }
                conversation.nextFakeId = nextId;
                conversation.currentOrderChangeId = newChanges.collect{it.id}
                // recalculate dependencies
                conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
            }
            on("success").to("showEditOrderChanges")
        }

        /**
         * Updates order attributes (period, billing type, active dates etc.) and
         * renders the order review panel.
         */
        updateOrder {
            action {
                def order = conversation.order
                conversation.errorMessages =  null
                def discountLines = conversation.order?.discountLines ?: []
                bindData(order, params)

                bindMetaFields(order, params);

                order.notify = params.notify ? 1 : 0
                order.ownInvoice = params.ownInvoice ? 1 : 0
                order.notesInInvoice = params.notesInInvoice ? 1 : 0

				//Changing an order's 'Active Since' date doesn't reflect in OrderChangeWS.java after a product is updated.
				//Added following fix to set Active since date in OrderChangeWS.java instances after an order is updated.
				conversation.allChanges.each{key, value -> value?.change?.getOrderWS()?.setActiveSince(order.getActiveSince())}

                // one time orders are ALWAYS post-paid
                if (order.period == Constants.ORDER_PERIOD_ONCE)
                    order.billingTypeId = Constants.ORDER_BILLING_POST_PAID

                order = rateOrderIfPossible(order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )

				disableIsProrate(order, flow.user);
				order.discountLines = discountLines
                conversation.order = order

                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * User wants to add assets to a product on his saved order.
         * Entry method into the asset selection screens.
         */
        updateProductWithAsset {
            action {
                // display select assets dialog only once for new dependency.
                // so, reset flag
                conversation.forceDisplayAssetsDialogForChangeId = null
                //get the current assets from the order line
                def changeId = params.int('id')
                def planItemId = params.int('itemId')
                OrderChangeWS change = conversation.allChanges.get(changeId)?.change
                def orderChangePlanItem = change.orderChangePlanItems?.find {it.itemId == planItemId}
                conversation.selectedAssets = []
                if (orderChangePlanItem) {
                    orderChangePlanItem.assetIds.each {
                        conversation.selectedAssets << AssetBL.getWS(AssetDTO.get(it))
                    }
                    conversation.maxAssets = orderChangePlanItem.bundledQuantity
                } else {
                    change.assetIds.each {
                        conversation.selectedAssets << AssetBL.getWS(AssetDTO.get(it))
                    }
                    conversation.maxAssets = -1
                }
                //get the product which is the source of the assets
                def itemId = change.itemId
                if (planItemId) {
                    itemId = ItemDTO.get(planItemId)?.id
                }

                def assetsIdsForItemOnOrder = []
                //get the assets for the same item not in this line (change)
                conversation.assetsForItemOnOrder = []
                // get assets from order lines
                conversation.order.orderLines.each {
                    if(it.itemId == itemId && it.id != change.orderLineId) {
                        boolean hasChanges = conversation.allChanges.any { key, value ->
                            OrderChangeWS changeWS = value.change
                            return changeWS.delete == 0 && changeWS.orderLineId == it.id && !changeWS.isAppliedSuccessfully()
                        }
                        // get assets from line only if orderChange for this line does not exist
                        if (!hasChanges && it.id > 0) {
                            it.assetIds.each {
                                conversation.assetsForItemOnOrder << AssetBL.getWS(AssetDTO.get(it))
                                assetsIdsForItemOnOrder << it
                            }
                        }
                    }
                }
                // get assets from order changes
                conversation.allChanges.each {key, value ->
                   OrderChangeWS changeWS = value.change
                   if (changeWS.delete == 0 && !changeWS.isAppliedSuccessfully() && changeWS.itemId == itemId && changeWS.id != change.id) {
                       changeWS.assetIds.each {
                           if (!assetsIdsForItemOnOrder.contains(it)) {
                               conversation.assetsForItemOnOrder << AssetBL.getWS(AssetDTO.get(it))
                               assetsIdsForItemOnOrder << it
                           }
                       }
                       if (changeWS.orderChangePlanItems) {
                           changeWS.orderChangePlanItems.each { chPlanItem ->
                               chPlanItem.assetIds.each {
                                   if (!assetsIdsForItemOnOrder.contains(it)) {
                                       conversation.assetsForItemOnOrder << AssetBL.getWS(AssetDTO.get(it))
                                       assetsIdsForItemOnOrder << it
                                   }
                               }
                           }
                       }
                   }
                }
                //assets removed from the saved order

                conversation.assetsToInclude = [] + conversation.orderItemAssetsMap[itemId] - assetsIdsForItemOnOrder

                if(conversation.productWithAsset != itemId) {
                    conversation.productWithAsset   = itemId
                    def item = ItemDTO.get(conversation.productWithAsset);
                    conversation.assetStatuses      = productService.getAvailableStatusesForProduct(item.id)
                    conversation.assetMetaFields    = new ItemTypeBL().findItemTypeWithAssetManagementForItem(item.id).assetMetaFields.findAll{it.dataType != DataType.STATIC_TEXT}
                }

                //display info message on max nr assets
                if (conversation.maxAssets > 0) {
                    conversation.infoMessage = message(code: 'order.max.assets.in.plan', args: [conversation.maxAssets])
                } else if (conversation.maxAssets == 0) {
                    conversation.infoMessage = message(code: 'order.info.asset')
                } else {
                    conversation.infoMessage = null
                }


                //define if we are add or updating assets with this flow - this case add
                conversation.assetFlow = 'update'

                //these two parameters define the action we will execute after assets have been picked
                conversation.assetFlowExitId = params.id
                conversation.assetFlowExitPlanItemId = params.int('itemId')
                conversation.assetFlowExitEvent = 'updateChangeAssets'
            }
            on("success").to("showAssets")
        }

        /**
         * User has selected a product which has asset management to add to the order.
         * Entry method into the asset selection screens.
         */
        selectProductWithAsset {
            action {
                try {
                    def itemId=params.int('id')
                    //get the product which is the source of the assets
                    if(params.isPlan=='true'){
                        def plans = conversation.plans // list of ItemDTO objects
                        def plan = plans.find{ it.id == itemId }?.plans?.asList()?.first()
                        plan.planItems.item.each{ ItemDTO item ->
                            if (item.assetManagementEnabled==1) {
                                itemId=item.id
                                return
                            }
                        }
                    }
                    if(conversation.productWithAsset != itemId) {
                        conversation.productWithAsset= itemId
                        def item = ItemDTO.get(conversation.productWithAsset);
                        conversation.assetStatuses      = productService.getAvailableStatusesForProduct(item.id)
                        conversation.assetMetaFields    = new ItemTypeBL().findItemTypeWithAssetManagementForItem(item.id).assetMetaFields
                        conversation.assetMetaFields?.size()
                    }
                    //conversation.selectedAssets = []
                    //max nr of assets the user may add -1 = unlimited
                    conversation.maxAssets = -1
                    conversation.infoMessage = null

                    def assetsIdsForItemOnOrder = []
                    //get the assets for the same item not in this line (change)
                    conversation.assetsForItemOnOrder = []
                    // get assets from order lines
                    conversation.order.orderLines.each {
                        if(it.itemId == conversation.productWithAsset) {
                            boolean hasChanges = conversation.allChanges.any { key, value ->
                                OrderChangeWS change = value.change
                                return change.delete == 0 && change.orderLineId == it.id && !change.isAppliedSuccessfully()
                            }
                            // get assets only if change does not exist for this line
                            if (!hasChanges) {
                                it.assetIds.each {
                                    conversation.assetsForItemOnOrder << AssetBL.getWS(AssetDTO.get(it))
                                    assetsIdsForItemOnOrder << it
                                }
                            }
                        }
                    }
                    // get assets from order changes
                    conversation.allChanges.each {key, value ->
                        OrderChangeWS changeWS = value.change
                        if (changeWS.delete == 0 && !changeWS.isAppliedSuccessfully() && changeWS.itemId == conversation.productWithAsset) {
                            changeWS.assetIds.each {
                                if (!assetsIdsForItemOnOrder.contains(it)) {
                                    conversation.assetsForItemOnOrder << AssetBL.getWS(AssetDTO.get(it))
                                    assetsIdsForItemOnOrder << it
                                }
                            }
                        }
                    }
                    //assets removed from the saved order
                    conversation.assetsToInclude = [] + conversation.orderItemAssetsMap[conversation.productWithAsset] - assetsIdsForItemOnOrder

                    //define if we are add or updating assets with this flow - this case add
                    conversation.assetFlow = 'add'

                    //these two parameters define the action we will execute after assets have been picked
                    conversation.assetFlowExitId = params.id
                    conversation.assetFlowExitEvent = (params.isPlan == "true" ? 'addPlan' : 'addLine')
                } catch (Exception e) {
                    log.error("Errore")
                }
            }
            on("success").to("showAssets")
        }

        /**
         * Search for assets. Filter by identifier, id or status for a given product
         */
        showAssets {
            action {
                try {
                    //customer's user id
                    params.userId = conversation.order.userId

                    params.max = params?.max?.toInteger() ?: pagination.max
                    params.offset = params?.offset?.toInteger() ?: pagination.offset

                    if (null == params['filterBy']) {
                        params['filterBy'] = ""
                    }
					
					if(conversation.assetStatuses?.findAll{it.id==0}.isEmpty()) conversation.assetStatuses << new AssetStatusDTOEx(0,Constants.RESERVAED_STATUS, 0,0,0,0)
                    params.template = params['partial'] ? 'assetsResults' : 'assets'
                    params.itemId = conversation.productWithAsset

                    //do the search
                    try {
                        //get the asset ids we don't want in the results
                        def filteredAssetIds = []
                        (conversation.selectedAssets + conversation.assetsForItemOnOrder).each {
                            filteredAssetIds << it.id
                        }

                        conversation.assets = productService.getFilteredAssets(session['company_id'],
                                filteredAssetIds, conversation.assetsToInclude, params, true)
                        // on order creation, choose Assets window should not open for the product that have no asset associated to it.(#6152)
                        def found = false
                        for (AssetDTO asset in conversation.assets){
                            if(${it?.entity?.id == session['company_id']}){
                                found = true
                                break
                            }
                        }
                        if(!found){
                            //Empty the list so that the UI can show an error
                            conversation.assets = []
                        }
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flash, session.locale, e);
                    }

                    conversation.maxAssetsShown = params.max

                    //set parameters needed in view from the conversation
                    params.assetFlowExitId = conversation.assetFlowExitId
                    params.assetFlowExitPlanItemId = conversation.assetFlowExitPlanItemId
                    params.assetFlowExitEvent = conversation.assetFlowExitEvent
                    params.assetFlow = conversation.assetFlow
                } catch (Exception e) {
                    log.error("Errore")
                }
            }
            on("success").to("build")
        }

        /**
         * User selected an asset in the UI. Add it to his list of selected assets
         */
        addAssetToSelected {
            action {
                //if this is plan and the user already added the nr of bundled items
                if(conversation.maxAssets < 0 || conversation.selectedAssets.size() < conversation.maxAssets) {
                    //get the asset the user selected
                    def newAsset = AssetBL.getWS(AssetDTO.get(params.int('id')));

                    //add the asset to the list of selected assets
                    if(!conversation.selectedAssets.contains(newAsset)) {
                        conversation.selectedAssets << newAsset
                    }
                }
                params['partial'] = true
            }
            on("success").to("showAssets")
        }

        /**
         * User selected assets in the UI. Add them to the list of selected assets
         */
        addAssetsToSelected {
            action {
                int idx = 0;
                while(params['asset.'+idx]) {
                    if(params['asset.select.'+idx]) {
                        //this is plan and the user already added the nr of bundled items
                        if(conversation.maxAssets >= 0 && conversation.selectedAssets.size() >= conversation.maxAssets) {
                            break
                        }

                        //get the asset the user selected
                        def newAsset = AssetBL.getWS(AssetDTO.get(params.int('asset.'+idx)));

                        //add the asset to the list of selected assets
                        if(!conversation.selectedAssets.contains(newAsset)) {
                            conversation.selectedAssets << newAsset
                        }
                    }

                    idx++
                }
                params['partial'] = true
            }
            on("success").to("showAssets")
        }

        /**
         * Remove an asset from the list of selected assets
         */
        removeAssetFromSelected {
            action {
                //find the asset the user selected and remove it from the list
                conversation.selectedAssets -= AssetBL.getWS(AssetDTO.get(params.int('id')))
                params['partial'] = true
            }
            on("success").to("showAssets")
        }

        /**
         * Clear the list of selected assets
         */
        clearSelectedAssets {
            action {
                //find the asset the user selected and remove it from the list
                conversation.selectedAssets = []
                params['partial'] = true
            }
            on("success").to("showAssets")
        }
        /**
         * Change order displayed on review tab
         */
        changeDisplayOrder {
            action {
                def targetOrderId = params.orderId ? params.int("orderId") : params.int("id")
                def targetOrder = OrderHelper.findOrderInHierarchy(conversation.order as OrderWS, targetOrderId)
                if (!targetOrder || targetOrder.deleted > 0) {
                    log.error("Could not fetch WS object")
                    orderNotFoundErrorRedirect(params.id)
                    return
                }
                conversation.order = targetOrder
                def dependencies = recalculateProductDependencies(targetOrder, conversation.allChanges,
                        conversation.products, conversation.plans)
                dependencies.find { key, value -> key.contains('change_') }?.each {
                    if (it.value?.any { it.type == 'mandatory' && !it.met }) {
                        flash.displayEditChangesTab = true
                    }
                }
            }
            on("success").to("build")
        }
        /**
         * Show suborders tab
         */
        showSuborders {
            action {
                params.template = 'suborders'
            }
            on("success").to("build")
        }
        /**
         * Show changes tab on left with filtering and pagination
         */
        showChanges {
            action {
                params.max = params?.max?.toInteger() ?: (conversation.maxChangesShow ?: pagination.max)
                params.offset = params?.offset?.toInteger() ?: pagination.offset
                params.sort = params?.sort ?: pagination.sort
                params.order = params?.order ?: pagination.order

                def description = params?.filterBy ?: conversation.changesFilterBy
                def defaultFilter = messageSource.getMessage('orderChanges.filter.by.product', (Object[]) [], session.locale)
                if (description == defaultFilter) {
                    description = null
                }
                def statusId = params?.statusId?.toInteger() ?: conversation.changesFilterStatusId
                if (statusId == -1) {
                    statusId = null;
                }

                params.template = 'changes'
                conversation.orderChanges = findCurrentOrderChanges(conversation.order,
                        conversation.allChanges, description, statusId, false,
                        params.offset, params.max, params.sort, params.order
                )
                conversation.maxChangesShown = params.max
                conversation.changesFilterBy = description
                conversation.changesFilterStatusId = statusId
            }
            on("success").to("build")
        }

        /**
         * Show review tab
         */
        showReview {
            action {
                //get error message from conservation object and then set to null so that it will not display next time.
                params.errorMessages = conversation.errorMessages
                conversation.errorMessages =  null
                params.message = conversation.message
                conversation.message=null
                params.template = 'review'
            }
            on("success").to("build")
        }
        /**
         * Show edit order changes tab
         */
        showEditOrderChanges {
            action {
                Map allChanges = conversation.allChanges

                conversation.editedOrderChanges = findCurrentOrderChanges(conversation.order,
                        allChanges, null, null, true,
                        0, Integer.MAX_VALUE, null, null
                )

                conversation.planItemMap = getPlanItemMapFromOrderChanges(allChanges)

                params.template = 'editOrderChange'

            }
            on("success").to("build")
        }
        /**
         * Start edit order change, display edit order changes tab
         */
        showEditChange {
            action {
                def id = params.int('changeId')
                def changeObject = conversation.allChanges.get(id)
                changeObject.changed = true
                conversation.currentOrderChangeId = [id]

            }
            on("success").to("showEditOrderChanges")
        }

        updateOrderChangeType {
            action {
                def id = params.int('changeId')
                def typeId = params.int('changeTypeId')
                OrderChangeTypeWS type = conversation.orderChangeTypes.find {it.id == typeId};
                OrderChangeWS change = conversation.allChanges.get(id).change;
                change.setOrderChangeTypeId(typeId)
                populateOrderChangeFieldsByType(change, type)
            }
            on("success").to("showEditOrderChanges")
        }
        /**
         * Create order change for order line update, display edit order changes tab
         */
        createChangeForUpdateLine {
            action {
                def id = params.int('id')
                def order = conversation.order
                def line = order.orderLines.find {it.id == id}
                def needCreateChanges = true

                def isApplyNow = PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'],Constants.PREFERENCE_APPLY_NOW)
                for (changeMap in conversation.allChanges) {
                    def realChange = changeMap.value.get("change")
                    if(realChange.id < 0 && realChange.getOrderLineId() == line.getId()){
                        needCreateChanges = false
                        conversation.currentOrderChangeId = [realChange.id]
                        break
                    }
                }
                if (needCreateChanges) {
                    List<OrderChangeWS> newChanges = addOrderChangesForLine(line, order, conversation.allChanges,
                            conversation.products, conversation.plans,
                            conversation.nextFakeId, conversation.orderChangeUserStatuses, conversation.defaultOrderChangeType?.id,
                            conversation.pricingDate, [],isApplyNow
                    )
                    OrderChangeWS newChange = newChanges.first()
                    conversation.nextFakeId = newChanges.min { it.id }.id - 1
                    conversation.currentOrderChangeId = [newChange.id]
                }
            }
            on("success").to("showEditOrderChanges")
        }
        /**
         * Display edit order changes tab for selected order line (changes for selected line will be expanded)
         */
        showChangeForOrderLine {
            action {
                def id = params.int('id')
                def line = conversation.order.orderLines.find {it.id == id}
                def lineChanges = findOrderChangesForLine(line, findChangedOrderChanges(conversation.allChanges))
                conversation.currentOrderChangeId = lineChanges.collect { it.id }
            }
            on("success").to("showEditOrderChanges")
        }
        /**
         * Remove order change, display edit order changes tab
         */
        removeOrderChange {
            action {
                def changeId = params.int('changeId')
                def changeObject = conversation.allChanges.get(changeId)
                if (changeObject) {
                    if (changeObject.change.id > 0) { //persisted change
                        changeObject.change.delete = 1
                    } else { //new change - delete from conversation
                        conversation.allChanges.remove(changeId)
                    }
                }
                conversation.productDependencies = recalculateProductDependencies(OrderHelper.findRootOrderIfPossible(conversation.order), conversation.allChanges, conversation.products, conversation.plans)
                conversation.order = rateOrderIfPossible(conversation.order, flash, findChangedOrderChanges(conversation.allChanges), conversation.persistedOrderOrderLinesMap )
				
				// recalculate used categories
                try {
                    recalculateUsedCategories(conversation)
                } catch (SessionInternalError e) {
                    if (!viewUtils.resolveException(flash, session.locale, e)) {
                        flash.errorMessages = [flash.error]
                    }
                }
            }
            on("success").to("showEditOrderChanges")
        }

        /**
         * Show subscription dialog if user has already subscribed to a subscription item
         */
        showSubscriptionDialog {
            action {
                if(params.isPlan == true) {
                    flash.plan = true
                } else {
                    flash.plan = false
                }
                
                if(params.isAssetMgmt == true) {
                    flash.isAssetMgmt = true
                } else {
                    flash.isAssetMgmt = true
                }
                
                flash.productId = params.int("id")
                params.template = "subscription"
            }
            on("success").to("build")
        }

        /**
         * Shows the order builder. This is the "waiting" state that branches out to the rest
         * of the flow. All AJAX actions and other states that build on the order should
         * return here when complete.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            on("details").to("showDetails")
            on("discounts").to("showDiscounts")
            on("products").to("showProducts")
            on("plans").to("showPlans")
            on("orderChanges").to("showChanges")
            on("addLine").to("addOrderLine")
            on("addRemoveDiscountLine").to("addRemoveDiscountLine")
            on("addPlan").to("addOrderPlanLine")
            on("swapPlan").to("swapOrderPlan")
            on("addLineToNewOrder").to("addOrderLineToNewOrder")
            on("updateLine").to("updateOrderLine")
            on("updateChangeAssets").to("updateOrderChangeAssets")
            on("removeLine").to("removeOrderLine")
            on("update").to("updateOrder")
            on("initAssets").to("selectProductWithAsset")
            on("initUpdateAssets").to("updateProductWithAsset")
            on("assets").to("showAssets")
            on("addAsset").to("addAssetToSelected")
            on("addAssets").to("addAssetsToSelected")
            on("removeAsset").to("removeAssetFromSelected")
            on("clearAssets").to("clearSelectedAssets")
            on("changeOrder").to("changeDisplayOrder")
            on("review").to("showReview")
            on("editOrderChanges").to("showEditOrderChanges")
            on("editChange").to("showEditChange")
            on("initUpdateLine").to("createChangeForUpdateLine")
            on("showChangeForLine").to("showChangeForOrderLine")
            on("removeChange").to("removeOrderChange")
            on("updateChange").to("updateOrderChange")
            on("updateChangeType").to("updateOrderChangeType")
            on("suborders").to("showSuborders")
			on("subscription").to("showSubscriptionDialog")
            on("save").to("saveOrder")
            // on("save").to("checkItem")  // check to see if an item exists, and show an information page before saving
            // on("save").to("beforeSave") // show an information page before saving

            on("cancel").to("finish")
            on("reserve").to("reserveAsset")
        }

        /**
         * Example action that shows a static page before saving if the order contains
         * a Lemonade item.
         *
         * Uncomment the "save" to "checkItem" transition in the builder() state to use.
         */
        checkItem {
            action {
                def order = conversation.order
                if (order.orderLines.find{ it.itemId == 2602}) {
                    // order contains lemonade, show beforeSave.gsp
                    hasItem();
                } else {
                    // order does not contain lemonade, goto save
                    save();
                }
            }
            on("hasItem").to("beforeSave")
            on("save").to("saveOrder")
        }

        /**
         * Example action that shows a static page before the order is saved.
         *
         * Uncomment the "save" to "beforeSave" transition in the builder() state to use.
         */
        beforeSave {
            on("save").to("saveOrder")
            on("cancel").to("build")
        }

        /**
         * Saves the order and exits the builder flow.
         */
        saveOrder {
            action {
                params.plan = conversation.plan
                def order = OrderHelper.findRootOrderIfPossible(conversation.order as OrderWS)
				order.createDate	= TimezoneHelper.currentDateForTimezone(Constants.DEFAULT_TIMEZONE)
                def orderChanges = []
                def idsMap = [:]
                try {

                    conversation.allChanges.each {key, value -> if (value.changed) { orderChanges << value.change }}
                    // propagate orders to changes from order hierarchy (needed because of rateIfPossible changes refs)
                    for (c in orderChanges) {
                        if (!c.orderId && c.orderWS) {
                            c.orderWS = OrderHelper.findOrderInHierarchy(order, c.orderWS.id)
                        }

                    }
                    // api consider new entities as new only if they have NULL or ZERO ids. Otherwise entity considered as persisted.
                    // we user negative ids for orders, lines and changes identity in UI. So, clear negative ids before processing by api.
                    clearIdsBeforeSaving(orderChanges, idsMap)
                    clearIdsBeforeSaving(order, idsMap)
                    conversation.discountableItems = []

					// Removing child orders if they don't have and order line.
					if (order.getChildOrders()){
                        def childOrders = Arrays.asList(order.getChildOrders());
                        for(child in childOrders.asList()){
							if((child.getOrderLines() == null || child.getOrderLines().size() == 0) &&
                                    !conversation.allChanges.find{key, value -> value.change.orderWS.objectId.equals(child.objectId)}){
								childOrders = childOrders - child
							}
						}
						order.setChildOrders(childOrders.toArray(new OrderWS[childOrders.size()]))
					}

                    // try to rate order, store it only if rating is ok
                    rateOrderOrThrow(order)

                    if (!order.userCode) {
                        // #7043 - Agents && Commissions - Get the ids of the logged in user if its a Partner.
                        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
                        def partnerId = []
                        if (loggedInUser.getPartner() != null) {
                            partnerId << loggedInUser.partner.user.id
                        }

                        // If the logged in user is a partner and the user code of the order is blank we have to automatically set it to
                        // the Partners user code.
                        def userCodes = []
                        if (partnerId) {
                            userCodes = new UserCodeDAS().findActiveForPartner(partnerId).collect { it.identifier }
                        }

                        order.userCode = (userCodes) ? userCodes.first() : null
                    }

                    if (!order.id || order.id <= 0) {
                        if (SpringSecurityUtils.ifAllGranted("ORDER_20")) {
                            log.debug("creating order ${order}")
                            order.id = webServicesSession.createUpdateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]))
                            // set success message in session, contents of the flash scope doesn't survive
                            // the redirect to the order list when the web-flow finishes

                            /*
                             Call UMS to create transaction for order
                             This method will called on the create order from customer page
                            */
                            if (PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_CALL_UMS_ON_CREATE_ORDER)) {

                                def isSimIssued = false
                                List<FeeWS> feesWSList = new ArrayList<>()
                                PrimaryPlanWS primaryPlanWS
                                FeeWS feeWS
                                BigDecimal rechargeAmount = BigDecimal.ZERO
                                def rechargeCreatedBy = adennetHelperService.getUserNameByUserId(session['user_id'] as Integer)
                                def governorate = adennetHelperService.getLoggedInUserGovernorate(session['user_id'] as Integer)
                                Integer entityId = adennetHelperService.entityIdByUserId(order.getUserId())
                                Integer SimPriceId = adennetHelperService.getValueFromExternalConfigParams(SIM_PRICE_ID) as Integer

                                def orderLine = order.getOrderLines()

                                for (OrderLineWS lineWS : orderLine) {
                                    if (lineWS.isPlan) {
                                        primaryPlanWS = adennetHelperService.getPrimaryPlanWS(lineWS.getPlanId())
                                        rechargeAmount = rechargeAmount.add(primaryPlanWS.getPrice())
                                    } else {
                                        if (lineWS.getItemId() == SimPriceId) {
                                            isSimIssued = true
                                        }

                                        feeWS = FeeWS.builder()
                                                .id(lineWS.getItemId())
                                                .description(lineWS.getDescription())
                                                .amount(new BigDecimal(lineWS.getPrice())).build()

                                        rechargeAmount = rechargeAmount.add(feeWS.getAmount())
                                        feesWSList.add(feeWS)
                                    }
                                    primaryPlanWS.setCashPrice(rechargeAmount)
                                }

                                if (assetList.size() > 1) {
                                    session.message = "Order contain more than one asset"
                                    throw SessionInternalError("Order contain more than one asset", HttpStatus.SC_BAD_REQUEST)
                                }
                                RechargeRequestWS rechargeRequestWS = RechargeRequestWS.builder()
                                        .entityId(entityId)
                                        .userId(order.getUserId())
                                        .isSimIssued(isSimIssued)
                                        .subscriberNumber(assetList.get(0))
                                        .primaryPlan(primaryPlanWS)
                                        .fees(feesWSList)
                                        .rechargeAmount(rechargeAmount)
                                        .rechargeDateTime(OffsetDateTime.now().toString())
                                        .rechargedBy(rechargeCreatedBy)
                                        .source(SOURCE_POS)
                                        .governorate(governorate as String)
                                        .orderId(order.id)
                                        .build()

                                def transactionId = adennetHelperService.callUmsToMakeRecharge(rechargeRequestWS)
                                log.debug "TransactionID : " + transactionId
                            }

                            session.message = 'order.created'
                            session.args = [ order.id, order.userId ]
                        } else {
                            redirect controller: 'login', action: 'denied'
                        }
                    } else {
                        if (SpringSecurityUtils.ifAllGranted("ORDER_21")) {
                            // save changes
                            log.debug("saving changes to order ${order.id}")
                            webServicesSession.createUpdateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]))

                            session.message = 'order.updated'
                            session.args = [ order.id, order.userId ]
                        } else {
                            redirect controller: 'login', action: 'denied'
                        }
                    }

                } catch (SessionInternalError e) {
                    log.error "Error is: " + e
					if (!viewUtils.resolveException(conversation, session.locale, e)) {
                        conversation.errorMessages = [conversation.error]
                    }
                    // restore ids to maintain identity of new orders, orderLines and orderChanges that was cleared before call to api
                    // identity is needed to store links between objects in views (http requests)
                    restoreFakeIds(order, idsMap)
                    restoreFakeIds(orderChanges, idsMap)
                    conversation.productDependencies = recalculateProductDependencies(order, conversation.allChanges, conversation.products, conversation.plans)

                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        reserveAsset {
            action {
                List<Integer> assetsIDSList = params.list('assetsIDSList')
                if (assetsIDSList) {
                    try {
                        doAssetReservation(assetsIDSList)
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flash, session.locale, e)
                    } catch (Exception e) {
                        println e
                        flash.errorMessages = [message(code: 'bean.AssetReservationWS.validation.error.reservation.fail')]
                    }
                } else {
                    flash.errorMessages = [message(code: 'bean.AssetReservationWS.validation.error.assetID')]
                }
                def item = ItemDTO.get(conversation.productWithAsset);
                conversation.assetStatuses = productService.getAvailableStatusesForProduct(item.id)
                conversation.assetMetaFields = new ItemTypeBL().findItemTypeWithAssetManagementForItem(item.id).assetMetaFields
                conversation.assetMetaFields?.size()
                params['partial'] = true
            }
            on("success").to("showAssets")
            on("error").to("build")
        }

        finish {
            redirect controller: 'order', action: 'list', id: conversation.order?.id
        }
    }

	private void cannotEditDiscountOrder() {
		session.error = 'order.error.edit.discount'
		redirect controller: 'order', action: 'list'
		return
	}
		
	
	private void orderNotFoundErrorRedirect(orderId) {
		session.error = 'order.not.found'
        session.args = [ orderId as String ]
        redirect controller: 'order', action: 'list'
    }

    private void removeDeletedOrdersFromHierarchy(OrderWS order) {
        if (order.getChildOrders() != null && order.getChildOrders().length > 0) {
            List<OrderWS> nonDeletedChildren = new LinkedList<OrderWS>();
            for (OrderWS childOrder : order.getChildOrders()) {
                if (childOrder.deleted == 0) {
                    nonDeletedChildren.add(childOrder);
                    removeDeletedOrdersFromHierarchy(childOrder);
                }
            }
            if (nonDeletedChildren.size() < order.getChildOrders().length) {
                order.setChildOrders(nonDeletedChildren.toArray(new OrderWS[nonDeletedChildren.size()]));
            }
        }
    }
    /**
     * Find mandatory and optional dependencies for products in order lines and order changes, find is they met or not.
     * @param order any order from hierarchy to start products with dependencies search
     * @param allChanges order changes for search products with dependencies
     * @param products all possible products (aka cache to prevent search in DB)
     * @param plans all possible plans (aka cache to prevent search in DB)
     * @return map of dependencies with format
     *          key : line|change_<id>
     *          value = {type: mandatory|optional, productId: <item id of line or change>, met: <true|false (is dependency satisfied or not)>}
     */
    private def recalculateProductDependencies(OrderWS order, Map allChanges, List products, List plans) {
        def dependencies = [:]
        def unAppliedChanges = []
        allChanges.each {key, value ->
            OrderChangeWS change = value.change
            if (change.delete == 0 && !change.isAppliedSuccessfully()) {
                unAppliedChanges << change
            }
        }
        recalculateDependenciesForOrder(OrderHelper.findRootOrderIfPossible(order), dependencies, products, plans, unAppliedChanges)
        unAppliedChanges.each{
            OrderChangeWS change = it
            // dependencies is actual only for 'newLine' changes
            if (change.getOrderLineId() == null || change.getOrderLineId() <= 0) {
                List<ItemDTO> checkDependencyProducts = []

                ItemDTO product = findProduct(change.itemId, products, plans)
                checkDependencyProducts = addDependencies(product)
                checkDependencyProducts.each { ItemDTO checkDependencyProduct ->
                    if (checkDependencyProduct && checkDependencyProduct.dependencies) {
                        def changeDependencies = []
                        def changeDependenciesAux = []
                        checkDependencyProduct.dependencies.each {
                            /* if the dependency is a category, then it will be required fetch all of its items */
                            def categoryItemsList
                            ItemTypeDTO categoryDependent = ItemTypeDTO.get(it.getDependentObjectId())
                            if(null!=categoryDependent && null!=categoryDependent.getId() && it.getType() == ItemDependencyType.ITEM_TYPE) {
                                categoryItemsList = webServicesSession.getItemByCategory(categoryDependent.getId())
                                if(null!=categoryItemsList && categoryItemsList.length>0) {
                                    List<Boolean> isMet = new ArrayList<Boolean>()
                                    categoryItemsList.eachWithIndex { item, index ->
                                        isMet.add(findDependencyForChange(item.id, it.minimum, it.maximum, change, unAppliedChanges))
                                        changeDependenciesAux << [type: it.minimum == 0 ? 'optional' : 'mandatory', productId: item.id, min: it.minimum, max: it.maximum,
                                                               met : isMet.get(index)]
                                    }
                                    /* if some of their items (any combination of them) reach the MIN, MAX value, then the conditions are met,
                                       and the category dependency is satisfied.
                                       Please refer to the  Jbilling User Guide Telco Edition 4.1 for further details */
                                    if(isMet.contains(Boolean.TRUE)) {
                                        changeDependenciesAux = []
                                        categoryItemsList.each { item ->
                                            changeDependenciesAux << [type: it.minimum == 0 ? 'optional' : 'mandatory', productId: item.id, min: it.minimum, max: it.maximum,
                                                                   met : true]
                                        }
                                    }
                                    changeDependenciesAux.each { prod ->
                                        if(!changeDependencies.contains(prod)) {
                                            changeDependencies << prod
                                        }
                                    }
                                }
                            }
                            else {
                                changeDependencies << [type: it.minimum == 0 ? 'optional' : 'mandatory', productId: it.getDependentObjectId(), min: it.minimum, max: it.maximum,
                                                       met : findDependencyForChange(it.getDependentObjectId(), it.minimum, it.maximum, change, unAppliedChanges)]
                            }
                        }
                        dependencies.put("change_" + change.id, changeDependencies)
                    }
                }
            }
        }
        return dependencies
    }

    private def recalculateDependenciesForOrder(OrderWS order, Map dependencies, List products, List plans, List unAppliedChanges) {
        if (order.orderLines) {
            for (OrderLineWS line : order.getOrderLines()) {
                List<ItemDTO> checkDependencyProducts = []
                ItemDTO product = findProduct(line.itemId, products, plans)
                checkDependencyProducts = addDependencies(product)

                checkDependencyProducts.each { ItemDTO checkDependencyProduct ->
                    if (checkDependencyProduct && checkDependencyProduct.dependencies) {
                        def lineDependencies = []
                        checkDependencyProduct.dependencies.each {
                            lineDependencies << [type: it.minimum == 0 ? 'optional' : 'mandatory', productId: it.getDependentObjectId(), min: it.minimum, max: it.maximum,
                                                 met: findDependencyForLine(line, it.getDependentObjectId(), it.minimum, it.maximum, line.childLines, unAppliedChanges)]
                        }
                        dependencies.put("line_" + line.id, lineDependencies)
                    }
                }
            }
        }
        if (order.childOrders) {
            for (OrderWS childOrder : order.childOrders) {
                recalculateDependenciesForOrder(childOrder, dependencies, products, plans, unAppliedChanges)
            }
        }
    }

    private def addDependencies(ItemDTO product) {
        List<ItemDTO> checkDependencies = []
        if (product && product.isPlan()) {
//                    We consider that if plan item is a plan, only one plan will be associated with them.
            PlanDTO planForProduct = product?.plans?.first()
            planForProduct.planItems.each { PlanItemDTO planItemDTO ->
                checkDependencies.add(planItemDTO.item)
            }
        } else {
            checkDependencies.add(product)
        }
        return checkDependencies
    }

    /**
     * Find dependencies for product in child lines of current order line and order changes for current line.
     * If the qty found is >= minimum and <= maximum, the dependency is met
     * @param currentLine Line to start search dependencies
     * @param targetProductId product (dependency) to search
     * @param minimum Minimum qty required
     * @param maximum Maximum qty required
     * @param childLines child lines of current line to search targetProductId
     * @param unAppliedChanges order changes to search targetProductId
     * @return true if targetProductId was found, false otherwise
     */
    private def findDependencyForLine(currentLine, targetProductId, minimum, maximum, childLines, unAppliedChanges) {
        def qty = 0
        if(childLines) {
            childLines.each{
                if(it.itemId == targetProductId && it.deleted == 0) {
                    qty += it.getQuantityAsDecimal().intValue()
                }
            }
        }

        //if we met the min and there is no max qty
        if (qty > 0 && qty >= minimum && (!maximum || maximum <= 0) ) {
            return true
        }

        //go through the unapplied order changes to see if quantities are met
        if(unAppliedChanges) {
            unAppliedChanges.each{
                if(it.itemId == targetProductId && it.delete == 0 && it.parentOrderLineId == currentLine.id) {
                    qty += it.getQuantityAsDecimal().intValue()
                }
            }
        }

        //check if we found >= minimum and <= maximum
        if (qty > 0 && qty >= minimum) {
            if (!maximum || maximum <= 0) {
                return true
            } else {
                return maximum >= qty
            }

        }

        return false;
    }
    /**
     * Find dependencies for product in child order changes for current order change.
     * If the qty found is >= minimum and <= maximum, the dependency is met
     * @param targetProductId product (dependency) to search
     * @param minimum Minimum qty required
     * @param maximum Maximum qty required
     * @param change current order change to start search
     * @param unAppliedChanges order changes to search targetProductId
     * @return true if targetProductId was found, false otherwise
     */
    private def findDependencyForChange(Integer targetProductId, minimum, maximum, change, unAppliedChanges) {
        def qty = 0
        if(unAppliedChanges) {
            unAppliedChanges.each{
                if(it.itemId == targetProductId && it.delete == 0
                        && (it.parentOrderChange == change || it.parentOrderChangeId == change.id) ) {
                    qty += it.getQuantityAsDecimal().intValue()
                }
            }
        }

        //check if we found >= minimum and <= maximum
        if (qty > 0 && qty >= minimum) {
            if (!maximum || maximum <= 0) {
                return true
            } else {
                return maximum >= qty
            }

        }

        return false;
    }
    /**
     * Collect all order changes (applied and unapplied) for all orders in hierarchy.
     * @param order Order to start search changes
     * @param products All possible products (aka cache to prevent search in DB)
     * @param plans All possible plans (aka cache to prevent search in DB)
     * @return map of order changes in form
     *      key : orderChangeId,
     *      value : {change: <order_change>, productDescription: <description of the product of order change>, changed: <is this change was edited during current session>}
     */
    private def collectOrderChanges(OrderWS order, List products, List plans) {
        def result = [:]
        if (order.id) {
            List<OrderChangeWS> orderChanges = webServicesSession.getOrderChanges(order.id) as List;
            for (OrderChangeWS change : orderChanges) {
                def product = findProduct(change.itemId, products, plans)
                result.put(change.id, [change: change, productDescription: product.getDescription(session['language_id']), changed: false])
            }
            if (order.getChildOrders()) {
                for (OrderWS child : order.getChildOrders()) {
                    result.putAll(collectOrderChanges(child, products, plans));
                }
            }
        }
        return result;
    }

    /**
     * Search product by id in items and plans
     * @param id Target product id to search
     * @param products All possible products (aka cache to prevent search in DB)
     * @param plans All possible plans (aka cache to prevent search in DB)
     * @return
     */
    private def findProduct(Integer id, List products, List plans) {
        ItemDTO product = ItemDTO.get(id)
        if (product != null){
            product.getDefaultPrices().size()
            product.getDependencies().each {it.getDependent()}
            if(product.isPlan()) {
                PlanItemBL.collectPlanItemsIncludingNested(product.plans.asList().get(0))
            }
        }
        
        return product
    }
    /**
     * Filter and sort orderChanges to display on OrderChanges tab or Edit Order Changes tab for current order
     * @param order Target order to search order changes
     * @param allChanges All changes map
     * @param description Description to filter order changes
     * @param statusId Status to filter order changes
     * @param changed Filter only edited or non edited during current session order changes
     * @param offset offset for paging
     * @param max max records count for paging
     * @param orderBy Sort condition
     * @param sortOrder Sort direction (asc|desc)
     * @return Suitable orders changes with paging info in format
     *      {totalCound: <total count of suitable records>,
     *      data: <List of filtered order changes>}
     */
    private Map findCurrentOrderChanges(OrderWS order, Map allChanges,
                                        String description, Integer statusId, boolean changed,
                                        Integer offset, Integer max,
                                        String orderBy, String sortOrder) {
        def result = [];
        // filter order changes by input order, description, status
        // consider only parent order changes, that are not deleted in the conversation
        result.addAll(allChanges.findAll{key, value ->
                return (value.change.orderId == order.id || value.change.orderWS?.id == order.id) &&
                        value.changed == changed && value.change.delete == 0 &&
                        (!description || value?.productDescription?.contains(description)) &&
                        (!statusId || value.change.statusId == statusId)
        }.values())
        // perform sorting and paging
        if (result && orderBy && sortOrder) {
            result.sort{a, b ->
                def r = 0
                if ("description".equals(orderBy)) {
                    r = a.productDescription.compareTo(b.productDescription);
                } else if ("applicationDate".equals(orderBy)) {
                    r = a.change.applicationDate != null ? (b.change.applicationDate ? a.change.applicationDate.compareTo(b.change.applicationDate) : -1 ) : (b.change.applicationDate ? 1 : 0)
                } else if ("status".equals(orderBy)) {
                    r = a.change.status != null ? ( b.change.status ? a.change.status.compareTo(b.change.status) : -1) : (b.change.status ? 1 : 0)
                } else if ("type".equals(orderBy)) {
                    r = a.change.type != null ? ( b.change.type ? a.change.type.compareTo(b.change.type) : -1) : (b.change.type ? 1 : 0)
                }
                if (sortOrder.equals('desc')) {
                    r = -1 * r;
                }
                return r;
            }
        }
        int totalCount = result.size()
        int from = 0
        int to = result.size()
        if (offset) {
            from = Math.max(0, Math.min(offset, result.size()))
        }
        if (max) {
            to = Math.max(0, Math.min(max + from, result.size()))
        }
        if (offset || max) {
            result = result.subList(from, to)
        }

        return [totalCount: totalCount, data: result]
    }
    /**
     * Restore fake ids for order changes from previously saved
     * @param orderChanges Order changes for ids restore
     * @param idsMap previous ids map
     */
    private void restoreFakeIds(List orderChanges, Map idsMap) {
        for (OrderChangeWS orderChange : orderChanges) {
            if (orderChange.id == null || orderChange.id == 0) {
                orderChange.id = (Integer) idsMap.get(orderChange)
            }
        }
    }

    /**
     * Restore fake ids for orders and order lines hierarchy from previously saved
     * @param order target order to start ids restore
     * @param idsMap previously saved ids map
     */
    private void restoreFakeIds(OrderWS order, Map idsMap) {
        if (order.id == null || order.id == 0) {
            order.id = (Integer) idsMap.get(order);
        }
        if (order.orderLines) {
            order.orderLines.each {
                if (it.id == 0) {
                    it.id = idsMap.containsKey(it) ? (Integer) idsMap.get(it) : 0
                }
            }
        }
        if (order.discountLines) {
            order.discountLines.each {
                if (it.orderId == null) {
                    it.orderId = order.id
                }
            }
        }
        if (order.childOrders) {
            order.childOrders.each {
                restoreFakeIds(it, idsMap)
            }
        }
    }
    /**
     * Set negative ids in orderChanges to null to consider order changes as new in api.
     * Save cleared ids in idsMap
     * @param orderChanges Order changes to clear ids
     * @param idsMap Map to store cleared ids
     */
    private void clearIdsBeforeSaving(List orderChanges, Map idsMap) {
        def tmpId
        for (OrderChangeWS orderChange : orderChanges) {
            if (orderChange.id != null && orderChange.id <= 0) {
                tmpId = orderChange.id
                orderChange.id = null
                idsMap.put(orderChange, tmpId)
            }
        }
    }
    /**
     * Set negative ids in orders and lines in hierarchy to null or 0 to consider them as new entities in api.
     * Save cleared ids in idsMap
     * @param order Order from hierarchy to start ids clear
     * @param idsMap Map to store cleared ids
     */
    private void clearIdsBeforeSaving(OrderWS order, Map idsMap) {
        def tmpId
        if (order.id < 0) {
            tmpId = order.id
            order.id = null
            idsMap.put(order, tmpId)
        };
        if (order.orderLines) {
            order.orderLines.each {
                if (it.id < 0) {
                    tmpId = it.id
                    it.id = 0
                    idsMap.put(it, tmpId)
                }
            }
        }
        if (order.discountLines) {
            order.discountLines.each {
                if (it.orderId < 0) {
                    it.orderId = null
                }
            }
        }
        if (order.childOrders) {
            order.childOrders.each {
                clearIdsBeforeSaving(it, idsMap)
            }
        }
    }

    /**
     * Call api to rate order (calculate price and description), catch exception if exists
     * @param order target order for rating
     * @param flash flash to store errors
     * @param orderChanges order changes to apply to order before rate
     * @param orderOrderLinesMap order persisted order lines (similar to DB state)
     * @return rated order if rating is success, previous state of order and error message if exception occure
     */
    private def rateOrderIfPossible(order, flash, List orderChanges, Map orderOrderLinesMap) {
        try {
            def oldLinesMap = [:]
            OrderHelper.findAllOrdersInHierarchy(order).each {
                def lines = it.id && it.id > 0 ? orderOrderLinesMap.get(it.id) : null
                if (!lines) {
                    lines = []
                }
                oldLinesMap.put(it.id, it.orderLines)
                it.orderLines = lines as OrderLineWS[]
            }
            try {
                if (order.orderLines || orderChanges) {
                    OrderWS ratedOrder = webServicesSession.rateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]))
                    // try to associate orderChange with order line
                    ratedOrder.orderLines.each {
                        if (it.id == 0) {
                            def changesForLine = findOrderChangesForLine(it, orderChanges)
                            if (changesForLine) {
                                if (changesForLine.size() == 1) {
                                    it.id = changesForLine.first().id
                                } else {
                                    // try to remove already associated lines
                                    changesForLine.removeAll { ratedOrder.orderLines.collect { it.id }.contains( it.id) }
                                    if (changesForLine) {
                                        it.id = changesForLine.first().id
                                    }
                                }
                            }
                        }
                    }
                    // In case of a single order line having quantity set to zero, total of the order should be zero
                    if(ratedOrder.orderLines.size() == 1 && ratedOrder.orderLines[0].quantity == '0'){
                        ratedOrder.total = BigDecimal.ZERO
                    }

                    Comparator lineComp = {a,b->((a.parentLine == null && b.parentLine == null) ? (a.hashCode() <=> b.hashCode()) :
                        ((a.parentLine && b.parentLine) ? (a.parentLine.hashCode() == b.parentLine.hashCode() ? (a.description <=> b.description) : a.parentLine.hashCode() <=> b.parentLine.hashCode()) :
                            (a.parentLine == null ? (a.hashCode() == b.parentLine.hashCode() ?  -1 : a.hashCode() <=> b.parentLine.hashCode()) :
                                (a.parentLine.hashCode() == b.hashCode() ? 1 : a.parentLine.hashCode() <=> b.hashCode()) )))}

                    // sort order lines
                    ratedOrder.orderLines.sort( lineComp )
                    return ratedOrder;
                } else {
                    oldLinesMap.put(order.id, order.orderLines)
                    return order;
                }
            } finally {
                OrderHelper.findAllOrdersInHierarchy(order).each {
                    def lines = oldLinesMap.get(it.id)
                    it.orderLines = lines as OrderLineWS[]
                }
            }
        } catch (SessionInternalError e) {
            if (!viewUtils.resolveException(flash, session.locale, e)) {
                flash.errorMessages = [flash.error]
            }
        }
        return order;
    }

    private def rateOrderOrThrow(order) {
        if (order.orderLines) {
            return webServicesSession.rateOrder(order)
        }
        return order
    }
    /**
     * Find order changes for target order line
     * @param line Target Order Line that was updated/created from order changes
     * @param orderChanges All order changes to search
     * @return List of found order changes what was applyed to target order line
     */
    private static List findOrderChangesForLine(line, orderChanges) {
        def result
        if (line.id && line.id > 0) {
            result = orderChanges.findAll {it.orderLineId == line.id}
        } else {
            if (line.id != 0) {
                result = orderChanges.findAll { it.id == line.id }
            } else {
                // new line: test item, quantity, useItem
                result = orderChanges.findAll {
                    it.itemId == line.itemId && it.quantityAsDecimal == line.quantityAsDecimal && (it.useItem == (line.useItem ? 1 : 0 ))
                }
            }
        }
        return result
    }

    private static def findChangedOrderChanges(allOrderChanges) {
        def orderChanges = []
        allOrderChanges.each {key, value -> if (value.changed) { orderChanges << value.change }}
        return orderChanges
    }
	
    /**
     * Create order changes for line. For product will be created one order change,
     * for plan can be created multiple order changed. Order changes for non-editable plan will be linked in hierarchy.
     * Order changes for editable plan will be independent (not linked in hierarchy)
     * @param line input order line
     * @param order target order
     * @param allChanges map of all order changes to add created changes
     * @param products all possible products (aka cache to prevent search in DB)
     * @param plans all possible plans (aka cache to prevent search in DB)
     * @param nextId first fake id to set to new order change
     * @param orderChangeUserStatuses order change statuses list
     * @return List of created order changes
     */
    private List<OrderChangeWS> addOrderChangesForLine(OrderLineWS line, OrderWS order, Map allChanges,
                                      List products, List plans, Integer nextId, List orderChangeUserStatuses,
                                      Integer defaultOrderChangeTypeId, Date pricingDate, List assetIds=[],Integer isApplyNow) {
        ItemDTO product = findProduct(line.itemId, products, plans)
        
        def defaultStatus = orderChangeUserStatuses ? orderChangeUserStatuses.get(0) : null
        // create order change for line product
        OrderChangeWS newChange = OrderChangeBL.buildFromLine(line, order, defaultStatus?.id, isApplyNow)
        def changeId = nextId
        newChange.id = changeId
        changeId--
        newChange.userAssignedStatus = defaultStatus?.getDescription(session['language_id'])?.content
        newChange.startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone'])
        newChange.orderChangeTypeId = defaultOrderChangeTypeId

        // quantity is disabled for asset managed products
        if (product.assetManagementEnabled == 1 && newChange.orderLineId > 0) {
            newChange.setQuantity(BigDecimal.ZERO);
        }
        allChanges.put(newChange.id, [change: newChange, productDescription: product.getDescription(session['language_id']), changed: true])
        def result = []
        result << newChange

        // create changes for plan bundle items if needed
        boolean editablePlan = false;
        def planDto = product?.plans ? product?.plans?.asList()?.first() : null
        if (planDto && planDto?.editable) {
            editablePlan = true;
        }

        // create changes for bundle items for editable plans for new order lines
        if (line.id <= 0 && planDto && planDto.getPlanItems()) {
            def planItemDtos = planDto.getPlanItems()
            def changePlanItems = []
            def addNonBundled = !editablePlan && PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_SHOW_NO_IMPACT_PLAN_ITEMS) > 0;
            for (PlanItemDTO planItemDto : planItemDtos) {
                product = findProduct(planItemDto.item.id, products, plans)
                if (editablePlan) {
                    OrderChangeWS ws = new OrderChangeWS();
                    ws.id = changeId
                    changeId--
                    ws.setOptLock(1);
                    ws.setUserAssignedStatusId(newChange.userAssignedStatusId);
                    ws.setUserAssignedStatus(newChange.userAssignedStatus);
                    ws.setStartDate(newChange.startDate);
                    ws.orderWS = newChange.orderWS
                    ws.orderId = newChange.orderId
                    ws.itemId = planItemDto.item.id
                    ws.orderChangeTypeId=defaultOrderChangeTypeId
                    ws.description = planItemDto.getItem().getDescription()
                    if (planItemDto.bundle) {
                        ws.quantity =  planItemDto.bundle.quantity ?: BigDecimal.ZERO
                    }
                    PriceModelDTO priceModelDto = PriceModelBL.getPriceForDate(planItemDto.models, pricingDate ?: TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                    if (priceModelDto) {
                        ws.price = priceModelDto.rate
                    }

                    ws.useItem = 0
                    ws.assetIds = []

                    if (product.assetManagementEnabled == 1) {
                        ws.quantity = BigDecimal.ZERO;
                    }
                    allChanges.put(ws.id, [change: ws, productDescription: product.getDescription(session['language_id']), changed: true])
                    result << ws
                } else {
                    if (product.assetManagementEnabled == 1 || product.orderLineMetaFields || addNonBundled ||
                            (planItemDto.bundle && planItemDto.bundle.quantity && planItemDto.bundle.quantity.compareTo(BigDecimal.ZERO) > 0)) {
                        def orderChangePlanItem = new OrderChangePlanItemWS()
                        orderChangePlanItem.assetIds = product.assetManagementEnabled==1 ? assetIds.toArray(new Integer[assetIds.size()]):new int[0]
                        orderChangePlanItem.itemId = planItemDto.item.id
                        orderChangePlanItem.id = 0
                        orderChangePlanItem.optlock = 0
                        orderChangePlanItem.bundledQuantity = planItemDto.bundle?.quantity
                        orderChangePlanItem.description = planItemDto.getItem().getDescription(webServicesSession.getUserWS(order.getUserId()).getLanguageId())
                        orderChangePlanItem.metaFields = new MetaFieldValueWS[0]
                        changePlanItems << orderChangePlanItem
                    }
                }
            }
            if (!editablePlan) {
                newChange.orderChangePlanItems = changePlanItems.toArray(new OrderChangePlanItemWS[changePlanItems.size()])
            }
        }
        return result
    }

    /**
     * Set or reset metaFields and orderStatusIdToApply according to orderChangeType selected
     * @param orderChangeWS order change WS object for type change
     * @param orderChangeTypeWS target order change type
     * @return set fields in orderChangeWS input object
     */
    private def populateOrderChangeFieldsByType(OrderChangeWS orderChangeWS, OrderChangeTypeWS orderChangeTypeWS) {
        if (!orderChangeTypeWS.allowOrderStatusChange) {
            orderChangeWS.orderStatusIdToApply = null
        }
        if (orderChangeTypeWS.getOrderChangeTypeMetaFields() != null) {
            def newMetaFieldValues = []
            orderChangeTypeWS.orderChangeTypeMetaFields.each {
                MetaFieldValueWS val = MetaFieldBL.createValue(it, null)
                if (val) {
                    val.getMetaField().setDataType(it.dataType)
                    val.setFieldName(it.name)
                    newMetaFieldValues << val
                }
            };
            if (orderChangeWS.metaFields != null) {
                MetaFieldWS[] orderChangeFields = webServicesSession.getMetaFieldsForEntity(EntityType.ORDER_CHANGE.name())
                for (MetaFieldValueWS fieldValue : orderChangeWS.metaFields) {
                    if (fieldValue.getFieldName() != null) {
                        boolean shouldBeDeleted = false;
                        for (MetaFieldWS orderChangeField : orderChangeFields) {
                            if (fieldValue.getFieldName().equals(orderChangeField.name)) {
                                shouldBeDeleted = true;
                                break;
                            }
                        }
                        if (!shouldBeDeleted) {
                            newMetaFieldValues << fieldValue
                        }
                    }
                }
            }
            orderChangeWS.setMetaFields(newMetaFieldValues.toArray(new MetaFieldValueWS[newMetaFieldValues.size()]));
        } else {
            orderChangeWS.setMetaFields(new MetaFieldValueWS[0]);
        }
    }

    def getAvailableMetaFields() {
        List<MetaField> metaFields =  MetaFieldBL.getAvailableFieldsList(session['company_id'], EntityType.ORDER);
        //get all dependencies. If we only load in the view it causes a connection leak
        for(MetaField metaField : metaFields) {
            metaField.getDependentMetaFields().size()
        }
        return metaFields
    }

    def bindMetaFields(orderWS, params) {
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(availableMetaFields, params);
        orderWS.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])
    }

    /**
     * Return true if line with given item exists among given order lines
     *
     * @param lines order lines
     * @param item item whose presence in order lines is to be tested
     */
    def subscribedToPlan(lines, item) {
        if (!item) throw new IllegalArgumentException("Parameter 'item' can't be null")
        def line = lines?.find{ it.itemId == item.id }
        return line && line.deleted == 0
    }
	
	def recalculateUsedCategories(conversation) {
		conversation.usedCategories = new ArrayList<Integer>()
		OrderBL bl  = new OrderBL()
		for(def line : conversation.order.orderLines) {
			bl.isCompatible(conversation.order.userId, ItemDTO.get(line.itemId), conversation.order.activeSince, conversation.order.activeUntil, conversation.usedCategories, line)
		}
	}
	
	def validateOrderPeriod () {
		
		Boolean prorateFlag =false
		
		if (new Integer(1) != params.period.toInteger()) {
			 OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.period.toInteger());
			 Integer billingCycleUnit = params.customerBillingCycleUnit ? params.customerBillingCycleUnit.toInteger() : 0
			 Integer billingCycleValue = params.customerBillingCycleValue ? params.customerBillingCycleValue.toInteger() : 0
			
			  if(orderPeriod.periodUnitId.compareTo(billingCycleUnit) == 0 && orderPeriod.value.compareTo(billingCycleValue) == 0) {
				 prorateFlag = true;
			 }
		} else {
			// for one-time order, prorating should be false
			prorateFlag = false;
		}
		
		render(contentType: "text/json") { ['prorateFlag': prorateFlag] }
	}

	private void disableIsProrate(order, user) {
		def billingConfiguration = webServicesSession.getBillingProcessConfiguration();
	
		if (billingConfiguration.proratingType.equals(ProratingType.PRORATING_AUTO_ON.getOptionText())) {
			order.isDisable = true;
			order.prorateFlag	= true;
			order.proratingOption = ProratingType.PRORATING_AUTO_ON.getOptionText();
		} else if (billingConfiguration.proratingType.equals(ProratingType.PRORATING_AUTO_OFF.getOptionText())) {
			order.isDisable = true;
			order.prorateFlag	= false;
			order.proratingOption = ProratingType.PRORATING_AUTO_OFF.getOptionText();
		} else {
			order.isDisable = false;
			order.proratingOption = ProratingType.PRORATING_MANUAL.getOptionText();
		}
		
		if (order.period) {
			OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(order.period);
            def customerBillingCycleUnit = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId();
            def customerBillingCycleValue = user.getCustomer().getMainSubscription().getSubscriptionPeriod().getValue();
            order.customerBillingCycleUnit = customerBillingCycleUnit
            order.customerBillingCycleValue = customerBillingCycleValue

			// for one-time order, the period unit and value are null, hence this check.
			if (orderPeriod.periodUnitId == null || orderPeriod.value == null) {
				order.prorateFlag = false;
				return;
			}

			// prorate flag is set off if the order period is different than customer billing cycle period. Notice the ! in this condition.
			if (!billingConfiguration.proratingType.equals(ProratingType.PRORATING_MANUAL.getOptionText())) {
				if (!(orderPeriod.periodUnitId.compareTo(customerBillingCycleUnit) == 0 && orderPeriod.value.compareTo(customerBillingCycleValue) == 0)) {
					order.prorateFlag = false;
				}
			}
		}
	}

    def renderOrderLineCharges () {
        render(template: 'orderLineCharges', model: [lineId: params.lineId])
    }

    /*
    * This method will reserve the assets whose ids are provided in
    * @param assetIds
    * @param skipIfNotReserved : If these is false then throw exception
    * if assets were already reserved for that customer else it will skip those assets
    * */

    private def doAssetReservation(def assetIds) {
        int userID = params.int('userID')
        int creatorID = session['user_id']
        List<Integer> selectedAssetIds = new ArrayList<Integer>()
        assetIds.each {
            selectedAssetIds.add(it as Integer)
        }
        // Find all assets which are already reserved for this customer
        List<AssetReservationDTO> reservedAssets = AssetReservationDTO.createCriteria().list() {
            gt('endDate', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
            createAlias("user", "user")
            eq('user.id', userID)
            createAlias("asset", "asset")
            'in'('asset.id', selectedAssetIds)
        }
        def unReservedAssets = selectedAssetIds - reservedAssets*.asset.id
        // Reserve unreserved assets only
        if (unReservedAssets.size() > 0) {
            for (String assetID : unReservedAssets) {
                webServicesSession.reserveAsset(Integer.valueOf(assetID), userID)
            }
        } else {
            String[] errors = ["AssetReservationWS,assetId,bean.AssetReservationWS.validation.error.assetID.already.reserved"]
            throw new SessionInternalError("Selected assets are already reserved.", errors);
        }
    }

    def recalculatePriceByDate(){
        Integer itemId = params.int('itemId')
        Integer userId = params.int('userId')
        Integer currencyId = params.int('currencyId')
        BigDecimal quantity = new BigDecimal(params.quantity)
        Date date = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.date)

        BigDecimal price = new ItemBL(itemId).getPrice(userId, currencyId, quantity, session['company_id'],
                                                       null, null, true, date);

        render(contentType: "text/json") { ['price': price] }
    }
	
    /**
     * Collects all PlanItems from order changes
     * @param allChanges
     * @return
     */
    private def getPlanItemMapFromOrderChanges(Map allChanges) {
        def planItemMap = [:]

        allChanges.each { key, value ->
            OrderChangeWS change = value.change
            def plans = ItemDTO.get(change.itemId).plans?.asList()
            if(null!=plans && !plans.isEmpty()) {
                PlanDTO plan = plans.first()
                planItemMap.put(plan.id , productService.getPlanItemsIncludingNested(plan))
            }

        }
        return planItemMap
    }
}
