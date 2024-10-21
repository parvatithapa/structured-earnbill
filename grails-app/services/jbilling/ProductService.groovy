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

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.AssetStatusBL
import com.sapienter.jbilling.server.item.ItemBL
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.ItemTypeBL
import com.sapienter.jbilling.server.item.PlanItemBL;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.AssetReservationDTO
import com.sapienter.jbilling.server.item.db.AssetStatusDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.JbillingTableDAS

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.criterion.CriteriaSpecification
import org.joda.time.format.DateTimeFormat
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

import java.util.regex.Pattern

import org.apache.commons.lang.StringEscapeUtils

class ProductService implements Serializable {

    static transactional = true
    static String CRITERIA_INTERNATIONAL_DESCRIPTION =  """ EXISTS (SELECT a.foreign_id
                                                                    FROM international_description a
                                                                    WHERE a.foreign_id = {alias}.id
                                                                    AND a.table_id = (select b.id from jbilling_table b where b.name = ? )

                                                                    AND a.language_id = ?
                                                                    AND lower(a.content) like ?)
                                                        """;

    def messageSource
	def companyService

	def getFilteredProducts(CompanyDTO company, ItemDTOEx product, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination) {
		return getFilteredProductsForCustomer(company,product,params,accountType,includePlans,pagination,null)
	}

    def getFilteredProducts(CompanyDTO company, ItemDTO product, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination) {
        return getFilteredProductsForCustomer(company,ItemBL.getItemDTOEx(ItemDTO.get(product.id)),params,accountType,includePlans,pagination,null)
    }

    /**
     * Returns a list of products filtered by simple criteria. The given filterBy parameter will
     * be used match either the ID, internalNumber or description of the product. The typeId parameter
     * can be used to restrict results to a single product type.
     *
     * @param company company
     * @param params parameter map containing filter criteria
     * @param includePlans true if the filter should include plans, false otherwise
     * @param pagination true if the results should be paginated, false otherwise
     * @return filtered list of products
     */
    def getFilteredProductsForCustomer(CompanyDTO company, ItemDTOEx product, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination, CompanyDTO customerCo) {

        def hierachyEntityIds = companyService.getEntityAndChildEntities()*.id

        // filter on item type, item id and internal number
        def products = getProductList(false, includePlans, pagination, customerCo, accountType, params, product, hierachyEntityIds)

        // if no results found, try filtering by description
        if (!products  && params.filterBy) {
            products = getProductList(true, includePlans, pagination, customerCo, accountType, params, product, hierachyEntityIds)
        }

        return products.unique()
    }

	def getFilteredProducts(CompanyDTO company, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination){
		return getFilteredProducts(company, null, params, accountType, includePlans, pagination)
	}

    /**
     * Returns a list of plan subscription items filtered by simple criteria. The given filterBy parameter
     * will be used match either the ID, internalNumber or description of the product.
     *
     * @param company company
     * @param params parameter map containing filter criteria
     * @return filtered list of products
     */
    def getFilteredPlans(CompanyDTO company, GrailsParameterMap params, boolean pagination) {
        def hierachyEntityIds = companyService.getEntityAndChildEntities()*.id

        // filter on item type, item id and internal number
        def plans = getPlansList(false, params, pagination, hierachyEntityIds)

        // if no results found, try filtering by description
        if (!plans && params.filterBy) {
            plans = getPlansList(true, params, pagination, hierachyEntityIds)
        }

        return plans
    }

    /**
     * Returns a list of visible item types.
     *
     * @return list of item types
     */
    def getItemTypes(companyId, typeIds) {

		List result = ItemTypeDTO.createCriteria().list {
			 and {
				 eq('internal', false)
				 createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
				 or {
					 'in'('entities.id', companyId?companyId:companyService.getEntityAndChildEntities()*.id )
					 //list all gloal entities as well
					 and {
						 eq('global', true)
						 eq('entity.id', companyService.getRootCompanyId())
					 }
				 }
			 	 if (typeIds) {
					  'in'('id', typeIds)
				 }
			 }
			 order('id', 'desc')
		 }

		 return result.unique()
	}

	def getDependencyItems(typeId, excludedItemIds){
		ItemDTO.metaClass.toString = {return delegate.id + " : "+ delegate.description }
		return ItemDTO.createCriteria().list() {
			createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
			and {
				or {
					'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
					and {
						eq('global', true)
						eq('entity.id', companyService.getRootCompanyId())
					}
				}
				isEmpty('plans')
				eq('deleted', 0)

				if( null != excludedItemIds && excludedItemIds.size() > 0 ){
					not { 'in'("id", excludedItemIds) }
				}

				itemTypes {
					eq('id', typeId)
				}
			}
			order('id', 'desc')
		}
	}

    /**
     * Returns the internal "plans" category used for storing plan subscription items.
     *
     * @return internal plans category
     */
    def getInternalPlansType() {
        new ItemTypeBL().getInternalPlansType(session['company_id'])
    }

    /**
     * Returns all assets filter by identifier, id or status for a given product.
     * Possible values in params
     *  - max                   (Required) max no of results to get
     *  - offset                (Required) start index in result list
     *  - sort                  Attribute of asset to sort by
     *  - order                 Sort direction
     *  - deleted               If the asset has been deleted
     *  - filterBy              Either an AssetDTO id or identifier
     *  - statusId              AssetStatusDTO id linked to the asset
     *  - filterByMetaFieldId-i   Id of meta field to filter by. i - index
     *  - filterByMetaFieldValue-i Value of meta field to filter by
     *  - itemId                ItemDTO id linked to the asset
     *  - orderLineId           id of line or 'NULL'
     *  - groupId               asset group id
     *  - groupIdNull           if true will filter by group id null or groupId
     *
     * @param companyId         only bring assets for this company
     * @param filteredAssets    assets filtered from the results
     * @param assetsToInclude   assets which must be excluded from the availability check
     * @param params
     * @return
     */
    def getFilteredAssets(Integer companyId, List filteredAssetIds, List assetsToInclude, GrailsParameterMap params, boolean available) {
        // default filterBy messages used in the UI

        def defaultFilter           = messageSource.resolveCode('assets.filter.by.default', session.locale).format((Object[]) [])
        def defaultMetaFieldValue   = messageSource.resolveCode('assets.filter.by.metafield.default', session.locale).format((Object[]) [])

		def customerCompany = null
		def orderUserId = params.int("userId")
		if(orderUserId) {
			// assets are being get for order
			customerCompany = UserDTO.get(params.int("userId"))?.company
		}

        // apply pagination arguments or not
        def pageArgs = [max: params.max, offset: params.offset,
                sort: (params.sort && params.sort != 'null') ? params.sort: 'id',
                order: (params.order && params.order != 'null') ? params.order : 'desc']

        //indexes of metafields we have filter data for
        def metaFieldIdxs = []
        Pattern pattern = Pattern.compile(/filterByMetaFieldId(\d+)/)
        //get all the ids in an array
        params.each{
            def m = pattern.matcher(it.key)
            if( m.matches()) {
                metaFieldIdxs << m.group(1)
            }
        }

        // filter on id, identifier and state
        def assets = AssetDTO.createCriteria().list(
                pageArgs
        ) {
			createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
            and {
					 if (params.filterBy && params.filterBy != defaultFilter) {
						 or {
								 eq('id', params.int('filterBy'))
								 ilike('identifier', "%${params.filterBy}%")
                                 ilike('subscriberNumber', "%${params.filterBy}%")
                                 ilike('imsi', "%${params.filterBy}%")
                         }
					 }
					 if (null != params.statusId && !params.statusId.toString().isEmpty() && params.statusId.toString()!='0') {
						 assetStatus {
							 eq('id', params.int('statusId'))
						 }

                         def reservedAssets = AssetReservationDTO.createCriteria().list() {
                             gt('endDate', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                         }
                         if (reservedAssets) {
                             not {
                                 'in'('id', reservedAssets*.asset.id as List)
                             }
                         }
					 }

				or {
					'in'('entities.id', session['company_id'] as Integer )
					and {
						eq('global', true)
						eq('entity.id', getRootCompanyId())
					}
					'in'('entity.id', companyService.getEntityAndChildEntities()*.id )
				}

				if(available) {
					or{
						'in'('entities.id', session['company_id'] as Integer )
						and {
							eq('global' , true)
							eq('entity.id', getRootCompanyId())
							//'in'('entity.id', companyService.getHierarchyEntities(session['company_id'] as Integer)*.id)
						}
					}
				}

                String[] metaFieldIdAndType
                DataType dataType = null
                String filterByMetaFieldValue
                metaFieldIdxs.each {
                    try {
                        if(params['filterByMetaFieldId'+it]) {
                            metaFieldIdAndType = params['filterByMetaFieldId'+it].split(":")
                            dataType = DataType.valueOf(metaFieldIdAndType[1])
                        }
                        if (dataType == DataType.BOOLEAN || (params['filterByMetaFieldValue'+it]
                                && params['filterByMetaFieldValue'+it] != defaultMetaFieldValue
                                && params['filterByMetaFieldValue'+it].toString().trim().length() > 0)) {
                            filterByMetaFieldValue = params['filterByMetaFieldValue'+it]

                            String join = "";
                            String where = "";
                            def parameter
                            if(dataType == DataType.STRING || dataType == DataType.JSON_OBJECT || dataType == DataType.ENUMERATION || dataType == DataType.TEXT_AREA) {
                                where = " lower(mv.string_value) like ?"
                                parameter = "%" + filterByMetaFieldValue.toLowerCase() + "%"
                            } else if(dataType == DataType.BOOLEAN) {
                                where = " mv.boolean_value=?"
                                parameter = (filterByMetaFieldValue ? true : false)
                            } else if(dataType == DataType.INTEGER) {
                                Integer.parseInt(filterByMetaFieldValue)
                                where = " mv.integer_value=?"
                                parameter = filterByMetaFieldValue as int
                            } else if(dataType == DataType.DECIMAL) {
                                new BigDecimal(filterByMetaFieldValue)
                                where = " mv.decimal_value=?"
                                parameter = filterByMetaFieldValue as double
                            } else if(dataType == DataType.DATE) {
                                Date theDate = DateTimeFormat.forPattern(messageSource.resolveCode('datepicker.format', session.locale).format((Object[]) [])).parseDateTime(filterByMetaFieldValue).toDate()
                                where = " mv.date_value=?"
                                parameter = theDate
                            } else if(dataType == DataType.LIST) {
                                join = "join list_meta_field_values lmv on lmv.meta_field_value_id=mv.id "
                                where = " lmv.list_value=?"
                                parameter = filterByMetaFieldValue
                            }

                            if(where.length() > 0) {
                                sqlRestriction(
                                    """exists (select mv.id from meta_field_value mv
                                    join asset_meta_field_map am on am.meta_field_value_id=mv.id
                                    ${join}
                                    where mv.meta_field_name_id=?
                                    and am.asset_id = {alias}.id
                                    and ${where})
                                    """,[metaFieldIdAndType[0] as int, parameter]
                                )
                            }

                        }
                    } catch (Throwable t) {
                        log.debug("Unable to parse meta field value", t)
                        throw new SessionInternalError("Unable to parse meta field value "+filterByMetaFieldValue, "asset.search.error.type.parse,"+filterByMetaFieldValue)
                    }
                }

                if(params.groupId || params.groupIdNull) {
                    or {
                        if(params.groupIdNull) {
                            isNull('group')
                        }

                        if(params.groupId) {
                            group {
                                eq('id', params.int('groupId'))
                            }
                        }
                    }
                }

                if(params.orderLineId) {
                    if(params.orderLineId == 'NULL') {
                        isNull('orderLine')
                    } else {
                        orderLine {
                            eq('id', params.int('orderLineId'))
                        }
                    }
                }

                if(filteredAssetIds?.size() > 0) {
                    not {
                        inList('id', filteredAssetIds)
                    }
                }
                if(assetsToInclude?.size() > 0) assetsToInclude = assetsToInclude - null

                if(assetsToInclude?.size() > 0) {
                    or {
                        assetStatus {
                            eq('isAvailable', 1)
                        }
                        inList('id', assetsToInclude)
                    }
                }

                if (params.int('statusId') == 0) {
                    List ownReservedAssets = AssetReservationDTO.createCriteria().list() {
                        if (orderUserId) eq('user.id', orderUserId)
                        gt('endDate', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                    }
                    'in'('id', (ownReservedAssets) ? (ownReservedAssets*.asset.id as List) : [null])
                } else if (orderUserId && StringUtils.trimToNull(params.statusId) == null) {
                    assetStatus {
                        eq('isAvailable', 1)
                    }
                }

                if (orderUserId && params.int('statusId') != 0) {
                        or {
                            eq('entity', customerCompany)
                            and {
                                //isNull('entity')
                                item {
                                    or {
                                        eq('id', params.int('itemId'))
                                    }
                                }
                            }
                        }

                        def reservedAssets = AssetReservationDTO.createCriteria().list() {
                            if(orderUserId) ne('user.id', orderUserId)
                            gt('endDate', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                        }
                        if (reservedAssets) {
                            not {
                                'in'('id', reservedAssets*.asset.id as List)
                            }
                        }

                    }
				if(params.int('itemId')) {
					item {
						  eq('id', params.int('itemId'))
				    }
				}
                if(params.int('categoryId')) {
                    item {
                        itemTypes {
                            eq('id', params.int('categoryId'))
                        }
						isEmpty('plans')
                    }
                }
                if (params.assetId) {
                    or {
                        eq('id', params.int('assetId'))
                        ilike('identifier', "%${params.assetId}%")
                    }
                }
                eq('deleted', params.deleted ? params.int('deleted') : 0)

                if('on' == params.showReIssuable){
                    isNull('subscriberNumber')
                    assetStatus {
                        eq('isAvailable', 1)
                    }
                }
                if('on' == params.showSuspended){
                    eq('suspended', true)
                }
            }
        }
        setReservedFlag(assets)
        // Sort on the basis of asset status
        if (pageArgs.sort == 'assetStatus.id') assets.sort({ a, b ->
            def aStatus = a.isReserved ? 'Reserved' : a.assetStatus?.description
            def bStatus = b.isReserved ? 'Reserved' : b.assetStatus?.description
            def comparator = aStatus <=> bStatus
            pageArgs.order=='desc'? -comparator:comparator
        })
        return assets.unique()
    }

    def setReservedFlag(def assets) {
        if(!assets) return;
        def activeReservationAssets = AssetReservationDTO.createCriteria().list(){
            projections{
                property("asset.id")
            }
            gt('endDate', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
        }
        assets.each {asset->
            if(activeReservationAssets.contains(asset.id)){
                asset.setReserved(true)
            }
        }
    }

    /**
     * List of all possible AssetStatusDTOEx objects linked to the
     * ItemTypes of the Item specified by itemId
     *
     * @param itemId    ItemDTO id.
     * @return
     */
    def getStatusesForProduct(Integer itemId) {
        def statusList = AssetStatusDTO.createCriteria().list() {
            and {
                itemType {
                    items {
                        eq('id', itemId)
                    }
                }

                eq('deleted', 0)
            }
        }

        return AssetStatusBL.convertAssetStatusDTOs(statusList);
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    /**
     * List of all AssetStatusDTOEx objects linked to the
     * ItemTypes of the Item specified by itemId and filtered by status = available
     *
     * @param itemId    ItemDTO id.
     * @return
     */
    def getAvailableStatusesForProduct(Integer itemId) {
        def statusList = AssetStatusDTO.createCriteria().list() {
            and {
                itemType {
                    items {
                        eq('id', itemId)
                    }
                }
                eq('deleted', 0)
                eq('isAvailable', 1)
            }
        }

        return AssetStatusBL.convertAssetStatusDTOs(statusList);
    }

	/**
	 * If the current user's company is the Root Company we return a list with its id PLUS all its children ids.
	 * If the logged in user's company is a child company then we return only its id.
	 */
	def getEntityAndChildEntities() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		def childEntities = []
		childEntities << loggedInUserCompany
		childEntities += CompanyDTO.findAllByParent(loggedInUserCompany)
	}

	def getRootCompanyId() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		loggedInUserCompany.parent == null ? loggedInUserCompany.id : loggedInUserCompany.parent.id
	}

    def createInternalTypeCategory(CompanyDTO company) {

        ItemTypeDTO type = new ItemTypeDTO();
        type.entity = company
        type.allowAssetManagement = 0
        type.description = Constants.PLANS_INTERNAL_CATEGORY_NAME
        type.internal = true
        type.orderLineTypeId = Constants.ORDER_LINE_TYPE_ITEM
        Set<CompanyDTO> entities = new HashSet<CompanyDTO>();
        entities.add(company);
        type.entities = entities
        type.save()
    }

    private def getProductList(boolean byDescription, boolean includePlans, boolean pagination, CompanyDTO customerCo,
                    AccountTypeDTO accountType, GrailsParameterMap params, ItemDTOEx product, List hierachyEntityIds){

        // default filterBy message used in the UI
        def defaultFilter = messageSource.resolveCode('products.filter.by.default', session.locale).format((Object[]) [])

        // apply pagination arguments or not
        def pageArgs = pagination ? [max: params.max, offset: params.offset] : [:]

        def products = ItemDTO.createCriteria().list(
            pageArgs
        ) {
            createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
            and {
                eq('deleted', 0)
                or {
                    'in'('entities.id', hierachyEntityIds)

                    //list all global entities as well
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId() )
                    }
                }

                if(!byDescription) {
                    if (params?.filterBy != defaultFilter) {
                        or {
                            eq('id', params.int('filterBy'))
                            ilike('internalNumber', "%${params.filterBy}%")
                        }
                    }
                }else{
                    sqlRestriction(CRITERIA_INTERNATIONAL_DESCRIPTION,[Constants.TABLE_ITEM, session['language_id'],
                                                                       "%"+params.filterBy.toLowerCase()+"%"])
                }

                if (params?.typeId) {
                    itemTypes {
                        eq('id', params.int('typeId'))
                    }
                }

                if(!includePlans){
                    isEmpty('plans')
                }

                if (accountType) {
                    createAlias('accountTypeAvailability', 'acc', Criteria.LEFT_JOIN)
                    or {
                        eq('standardAvailability', true)
                        eq('acc.id', accountType.id)
                    }
                }

                if(customerCo!=null){
                    or {
                        eq('entities.id', customerCo.id)
                        eq('global', true)
                        if (customerCo.parent) {
                            eq ('entity.id', customerCo.id)
                            and {
                                eq('global', true)
                                eq('entity.id', customerCo.parent.id )
                            }
                        }
                    }
                }

                //only products valid for the period of the plan
                if(product?.activeSince) {
                    or {
                        le('activeSince', product.activeSince)
                        isNull('activeSince')
                    }
                }

                if(product?.activeUntil) {
                    or {
                        ge('activeUntil', product.activeUntil)
                        isNull('activeUntil')
                    }
                }
            }

            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            order('id', 'desc')
        }

        return products
    }

    private def getPlansList(boolean byDescription, GrailsParameterMap params, boolean pagination, List hierachyEntityIds){

        // default filterBy message used in the UI
        def defaultFilter = messageSource.resolveCode('products.filter.by.default', session.locale).format((Object[]) [])

        // apply pagination arguments or not
        def pageArgs = pagination ? [max: params.max, offset: params.offset] : [:]

        return ItemDTO.createCriteria().list(
            pageArgs
        ) {
            createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
            and {
                isNotEmpty('plans')
                eq('deleted', 0)
                or {
                    'in'('entities.id', hierachyEntityIds)

                    //list all global entities as well
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId() )
                    }
                }

                if(!byDescription){
                    if (params?.filterBy != defaultFilter) {
                        or {
                            eq('id', params.int('filterBy'))
                            ilike('internalNumber', "%${params.filterBy}%")
                        }
                    }
                }else {
                    sqlRestriction(CRITERIA_INTERNATIONAL_DESCRIPTION, [Constants.TABLE_ITEM, session['language_id'],
                                                                        "%"+params.filterBy.toLowerCase()+"%"])
                }

                if (params.existedPlanItemIdForFilter) {
                    ne('id', params.int('existedPlanItemIdForFilter'))
                }
            }
            order('id', 'desc')
        }
    }

    List<PlanItemWS> getPlanItemsIncludingNested(PlanDTO plan) {
        return PlanItemBL.getWS(PlanItemBL.collectPlanItemsIncludingNested(plan) as List)
    }

    @Transactional(readOnly = true)
    PriceModelWS getPriceByItem(Integer itemId, Date today, Integer entityId) {
        return PriceModelBL.getPrice(ItemDTO.get(itemId), today, entityId)
    }

    @Transactional(readOnly = true)
    def getProductDetails(Integer itemId, com.sapienter.jbilling.server.order.OrderWS order) {
        def productMap = [:]
            ItemDTO product = ItemDTO.get(itemId)

            productMap["id"] = itemId
            productMap["isAssetMgmt"] = (product.assetManagementEnabled!=null && Integer.compare(product.assetManagementEnabled, 1) == 0 );

            def isSubsProd = false
            for(def type : product.itemTypes) {
                if(type.orderLineTypeId == Constants.ORDER_LINE_TYPE_SUBSCRIPTION.intValue()) {
                    isSubsProd = true;
                }
            }

            productMap["isSubsProd"] = isSubsProd
            productMap["description"] = StringEscapeUtils.escapeHtml(product?.getDescription(session['language_id']))

            def totalChilds = product?.entities?.size()
            def multiple = false
            if (totalChilds > 1 ) {
                multiple = true
            }

            productMap["multiple"] = multiple
            productMap["global"] = product?.global
            def entityDescription
            if(product?.entity == null) {
                entityDescription = StringEscapeUtils.escapeHtml(product?.entities?.toArray()[0]?.description)
            } else {
                entityDescription = StringEscapeUtils.escapeHtml(product?.entity?.description)
            }
            productMap["entityDescription"] = entityDescription
            productMap["internalNumber"] = StringEscapeUtils.escapeHtml(product?.internalNumber)
            productMap["price"] = getPriceByItem(itemId, order?.activeSince ?: order?.createDate ?:
                    TimezoneHelper.currentDateForTimezone(session['company_timezone']),
                    session['company_id'] as Integer)

        return productMap
    }
}
