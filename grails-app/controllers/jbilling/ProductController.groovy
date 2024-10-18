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
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.item.AssetBL
import com.sapienter.jbilling.server.item.AssetStatusBL
import com.sapienter.jbilling.server.item.AssetStatusDTOEx
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.ItemBL
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.ItemDependencyDTOEx
import com.sapienter.jbilling.server.item.ItemDependencyType
import com.sapienter.jbilling.server.item.ItemTypeBL
import com.sapienter.jbilling.server.item.ItemTypeWS
import com.sapienter.jbilling.server.item.RatingConfigurationBL
import com.sapienter.jbilling.server.item.RatingConfigurationWS
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.AssetReservationDAS
import com.sapienter.jbilling.server.item.db.AssetStatusDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.pricing.PriceModelBL
import com.sapienter.jbilling.server.pricing.PriceModelWS
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.collections.MapUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.criterion.*
import org.joda.time.format.DateTimeFormat

import java.util.regex.Pattern

@Secured(["MENU_97"])
class ProductController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
    static versions = [ max: 25 ]

    static final viewColumnsToFields =
            ['categoryId': 'id',
             'productId': 'id',
             'lineType': 'orderLineTypeId',
             'number': 'internalNumber',
             'company': 'ce.description']

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def productService
	def companyService
    SecurityValidator securityValidator


    def auditBL

    def index () {
        list()
    }

    /**
     * Get a list of categories and render the "_categories.gsp" template. If a category ID is given as the
     * "id" parameter, the corresponding list of products will also be rendered.
     */
    def list () {
        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

        /*
         * If the category is global, should validate it by the hierarchy of the owner entity
         * If the category is not global, should validate it by the entities added for category
         */
		if (category && !companyService.isAvailable(category.global, category.entity?.id,
                                                    category.global ? companyService.getHierarchyEntities(category.entity?.id)*.id : category.entities*.id)) {
			category= null
			flash.info = "validation.error.company.hierarchy.invalid.categoryid"
			flash.args = [ categoryId ]
		}

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), category?.description)

        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'categoriesTemplate', model: [selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts']
            }else {
                render view: 'list', model: [selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts']
            }
            return
        }

        def categories = getProductCategories(true, null)
        def products
        try {
            products = category ? getProducts(category.id, filters) : null
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        if (params.applyFilter || params.partial) {
            render template: 'productsTemplate', model: [ products: products, selectedCategoryId: category?.id, contactFieldTypes: contactFieldTypes ]
        } else {
            render view: 'list', model: [ categories: categories, products: products, selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts' ]
        }
    }

    def categories () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'categoriesTemplate', model: []
            }else {
                render view: 'list', model: []
            }
            return
        }
        def categories = getProductCategories(true, null)
        render template: 'categoriesTemplate', model: [ categories: categories ]
    }

    def findCategories () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def categories = getProductCategories(true, null)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

        try {
            render getItemsJsonData(categories, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def findProducts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

        try {
            def products = category ? getProducts(category.id, filters) : null
            render getItemsJsonData(products, params) as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

    def findAllProducts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        try {
            def products =  getProducts(null, filters)
            render getItemsJsonData(products, params) as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

    /**
     * Converts Products and Categories to JSon
     */
    private def Object getItemsJsonData(items, GrailsParameterMap params) {
        def jsonCells = items
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def getProductCategories(def paged, excludeCategoryId) {
        if (paged) {
            params.max = params?.max?.toInteger() ?: pagination.max
            params.offset = params?.offset?.toInteger() ?: pagination.offset
            params.sort = params?.sort ?: pagination.sort
            params.order = params?.order ?: pagination.order
        }

        def childEntities = companyService.getEntityAndChildEntities()
        def company_id = session['company_id'] as Integer

        List result = ItemTypeDTO.createCriteria().list(
                max: paged ? params.max : null,
                offset: paged ? params.offset : null
        ) {
            createAlias("entities", "ce", CriteriaSpecification.LEFT_JOIN)
            and {
                eq('internal', false)
                or {
                    'in'('ce.id', companyService.getEntityAndChildEntities()*.id)
                    //list all global entities as well BUT only if they were created by me or my parent NOT other root companies.
                    and {
                        eq('global', true)
                        'in'('entity.id', [session['company_id'], companyService.getRootCompanyId()])
                    }
                }
                if (null != excludeCategoryId) {
                    notEqual('id', excludeCategoryId)
                }
                if (params.categoryId) {
                    eq('id', params.int('categoryId'))
                }
                if (params.company) {
                    addToCriteria(Restrictions.ilike("entity.description", params.company, MatchMode.ANYWHERE));
                }
            }
            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            SortableCriteria.sort(params, delegate)
        }

        return result
    }

    def getAvailableAccountTypes() {

        return AccountTypeDTO.createCriteria().list() {
            and {
                eq('company', new CompanyDTO(session['company_id']))
            }
            order('id', 'desc')
        }
    }

    def getDependencyItemTypes(excludedTypeIds){
        return ItemTypeDTO.createCriteria().list() {
        	createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
			and {
				or {
                    'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
				}
                eq('internal', false)
                if( null != excludedTypeIds && excludedTypeIds.size() > 0 ){
                   not { 'in'("id", excludedTypeIds) }
                }
            }
            order('id', 'desc')
        }
    }

    def getDependencyItems(typeId, excludedItemIds) {
        productService.getDependencyItems(typeId, excludedItemIds)
    }

    def getItems(itemIds) {

		Integer company_id = session['company_id'] as Integer
        return ItemDTO.createCriteria().list() {
        	createAlias("entities","ce")
            and {
                or {
					//query based on item_entity_map always
					'in'('ce.id', company_id)
					//list all gloal entities as well
					eq('global', true)
				}
                isEmpty('plans')
                eq('deleted', 0)

                if(null != itemIds && itemIds.size()>0){
                    'in'('id', itemIds)
                } else {
                    eq('id', null)
                }
            }
            order('id', 'desc')
        }
    }

    def getItemsByItemType () {
        def typeId = params.int('typeId')
        List<Integer> toExcludeItemIds = []
        params["toExcludeItemIds[]"].grep{it}.each{
            toExcludeItemIds << Integer.valueOf(it)
        }
        render g.select(
                from: getDependencyItems(typeId, toExcludeItemIds),
                id: 'product.dependencyItems',
                name: 'product.dependencyItems',
                optionKey: 'id',
                noSelection: ['':'-'])
    }

    def addDependencyRow () {
        def typeId = (params.typeId!=null && !StringUtils.isBlank(params.typeId))?params.int('typeId'):null
        def itemId = (params.itemId!=null && !StringUtils.isBlank(params.itemId))?params.int('itemId'):null
        def min = (params.min!=null && !StringUtils.isBlank(params.min))?params.int('min'):null
        def max = (params.max!=null && !StringUtils.isBlank(params.max))?params.int('max'):null

        ItemDependencyDTOEx dep = new ItemDependencyDTOEx(type: typeId?ItemDependencyType.ITEM_TYPE : ItemDependencyType.ITEM,
                            dependentId: itemId?:typeId, minimum: min, maximum: max)

        if(typeId!=null && itemId==null){
            ItemTypeBL bl = new ItemTypeBL()
            bl.set(typeId)
            def obj = bl.getEntity()
            dep.dependentDescription = obj.description
            render template: 'dependencyRow', model: [obj:dep, type: true]
        } else if(typeId!=null && itemId!=null){
            ItemBL bl = new ItemBL()
            bl.set(itemId)
            def obj = bl.getEntity()
            dep.dependentDescription = obj.description
            render template: 'dependencyRow', model: [obj:dep, type: false]
        } else {
            render ''
        }
    }

    def getDependencyList () {
        Integer typeId = (params.typeId!=null && !StringUtils.isBlank(params.typeId))?params.int('typeId'):null
        Integer itemId = (params.itemId!=null && !StringUtils.isBlank(params.itemId))?params.int('itemId'):null

        List<Integer> typeIds = [], itemIds = []
        List<Integer> toExcludeTypeIds = [], toExcludeItemIds = []
        params["typeIds[]"].grep{it}.each{
            typeIds << Integer.valueOf(it)
        }
        params["itemIds[]"].grep{it}.each{
            if(it.isNumber()) {
                itemIds << Integer.valueOf(it)
            }
        }
        params["toExcludeTypeIds[]"].grep{it}.each{
            toExcludeTypeIds << Integer.valueOf(it)
        }
        toExcludeTypeIds << typeId

        params["toExcludeItemIds[]"].grep{it}.each{
            toExcludeItemIds << Integer.valueOf(it)
        }
        toExcludeItemIds << itemId

        if(typeId!=null && itemId==null){
            typeIds.removeAll(toExcludeTypeIds)
            render g.select(
                    from: productService.getItemTypes(session['company_id'], typeIds),
                    id: 'product.dependencyItemTypes',
                    name: 'product.dependencyItemTypes',
                    optionValue: "description",
                    optionKey: 'id',
                    noSelection: ['':'-'])
        } else if(typeId!=null && itemId!=null){
            itemIds.removeAll(toExcludeItemIds)
            render g.select(
                    from: getItems(itemIds),
                    id: 'product.dependencyItems',
                    name: 'product.dependencyItems',
                    optionKey: 'id',
                    noSelection: ['':'-'])
        }
    }
    /**
     * Get a list of products for the given item type id and render the "_products.gsp" template.
     */
    def products () {
        if (params.id) {
            def filters = filterService.getFilters(FilterType.PRODUCT, params)
            def category = ItemTypeDTO.get(params.int('id'))
            def contactFieldTypes = params['contactFieldTypes']

            breadcrumbService.addBreadcrumb(controllerName, 'list', null, category?.id, category?.description)

            def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
            //If JQGrid is showing, the data will be retrieved when the template renders
            if (usingJQGrid){
                render template: 'productsTemplate', model: [selectedCategory: category, contactFieldTypes: contactFieldTypes ]
            }else {
                def products
                try {
                    products = getProducts(category?.id, filters)
                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flash, session.locale, e)
                }

                render template: 'productsTemplate', model: [ products: products, selectedCategory: category, contactFieldTypes: contactFieldTypes ]
            }

        }
    }

    /**
     * Applies the set filters to the product list, and exports it as a CSV for download.
     */
    @Secured(["PRODUCT_44"])
    def csv () {
        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS
        params.max = CsvExporter.MAX_RESULTS

        def products = getProducts(params.int('id'), filters)

        if (products.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "products.csv")
            Exporter<ItemDTO> exporter = CsvExporter.createExporter(ItemDTO.class);
            render text: exporter.export(products), contentType: "text/csv"
        }
    }

    def getProducts(Integer id, filters) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
		def language_id = session['language_id'] as Integer
		def company_id = session['company_id'] as Integer
        def products = ItemDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
		    createAlias("entities","ce", CriteriaSpecification.LEFT_JOIN)
            def productMetaFieldFilters= []
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        if (filter.field == 'description') {
                            def description = filter.stringValue?.toLowerCase()
                            sqlRestriction(
                                    """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_ITEM, language_id, "%" + description + "%"]
                            )
                        } else if (filter.field == 'i.global') {
                            eq('global', filter.booleanValue)
                        } else if (filter.field == 'contact.fields') {
                            productMetaFieldFilters.add(filter)
                        } else if (filter.field == 'price.type') {
                            sqlRestriction(
                                    """ exists (
                                        select epmm1.item_id
                                        from entity_item_price_map epmm1
                                        inner join item_price_timeline ipt1 on epmm1.id = ipt1.model_map_id
                                        inner join price_model pm1 on ipt1.price_model_id = pm1.id
                                        where epmm1.item_id = {alias}.id
                                        and pm1.strategy_type=?
                                    )
                                    """, [PriceModelStrategy.valueOf(filter.stringValue).toString()]
                            )
                        } else if (filter.field == 'price.rate') {
                            if (filter.decimalValue != null || filter.decimalHighValue != null) {
                                def parameters = []
                                filter.decimalValue ? parameters << filter.decimalValue : ""
                                filter.decimalHighValue ? parameters << filter.decimalHighValue : ""
                                sqlRestriction(
                                        """ exists (
                                        select epmm2.item_id
                                        from entity_item_price_map epmm2
                                        inner join item_price_timeline ipt2 on epmm2.id = ipt2.model_map_id
                                        inner join price_model pm2 on ipt2.price_model_id = pm2.id
                                        where epmm2.item_id = {alias}.id
                                        ${filter.decimalValue != null ? "and pm2.rate>=?" : ""}
                                        ${filter.decimalHighValue != null ? "and pm2.rate<=?" : ""}

                                        )
                                        """, parameters
                                )
                            }
                        } else if (filter.field == 'u.company.description') {
                            ilike('ce.description', "%${filter.stringValue}%")
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }

                if (id != null) {
                    itemTypes {
                        eq('id', id)
                    }
                }

                isEmpty('plans')
                eq('deleted', 0)
                or {
                    'in'('ce.id', companyService.getEntityAndChildEntities()*.id)
                    //list all gloal entities as well
                    and {
						eq('global', true)
                        'in'('entity.id', [session['company_id'], companyService.getRootCompanyId()])
                    }
                }
                if(params.productId) {
                    eq('id', params.int('productId'))
                }
                if(params.company) {
                    addToCriteria(Restrictions.ilike("ce.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.number) {
                    addToCriteria(Restrictions.ilike("internalNumber",  params.number, MatchMode.ANYWHERE))
                }
            }
            if (productMetaFieldFilters.size() > 0) {
                productMetaFieldFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(ItemDTO.class, "item")
                            .setProjection(Projections.property('item.id'))
                            .createAlias("item.metaFields", "metaFieldValue")
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
			resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }

        params.totalCount = products.totalCount
        log.debug "Products from filter: ${products}"
        log.debug "Entities in first product: ${products[0]?.entities}"

        return products.unique()
    }

    /**
     * Get a list of ALL products regardless of the item type selected, and render the "_products.gsp" template.
     */
    def allProducts () {
		def filters = filterService.getFilters(FilterType.PRODUCT, params)
		def item = ItemDTO.get(params.int('id'))
		def catList = item?.getItemTypes();
		def category = catList?.getAt(0)
        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            render template: 'productsTemplate', model: [contactFieldTypes: contactFieldTypes ]
        } else if (category) {
            redirect action: 'list', params: [id:category.id]
        } else {
            def products
            try {
                products = getProducts(null, filters)
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }

            render template: 'productsTemplate', model: [ products: products, contactFieldTypes: contactFieldTypes ]
        }

    }

    /**
     * Show details of the selected product. By default, this action renders the entire list view
     * with the product category list, product list, and product details rendered. When rendering
     * for an AJAX request the template defined by the "template" parameter will be rendered.
     */
    @Secured(["PRODUCT_43"])
    def show () {
        ItemDTO product = ItemDTO.get(params.int('id'))
        if (!product) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }

        if (product.deleted == 1) {
            if (params.template) {
                render template: params.template,
                          model: [ message: 'product.deleted.error' ]
            } else {
                log.debug "redirecting to list"
                flash.error = 'product.deleted.error'
                redirect action: 'list'
            }
            return
        }

        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global, true)

        recentItemService.addRecentItem(product?.id, RecentItemType.PRODUCT)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), product?.internalNumber)

        //check if asset management is possible by checking if a linked item type allows asset management
        def assetManagementPossible = false
        product?.itemTypes?.each{
            if(it.allowAssetManagement) {
                assetManagementPossible = true
            }
        }

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: params.template, model: [ selectedProduct: product, selectedCategoryId: params.category, assetManagementPossible: assetManagementPossible ]

        } else {
            // render default "list" view - needed so a breadcrumb can link to a product by id
            def filters = filterService.getFilters(FilterType.PRODUCT, params)
            def categories = getProductCategories(false, null);

            def productCategory = params.category ?: product?.itemTypes?.asList()?.get(0)
            def products
            try {
                products = getProducts(productCategory.id, filters);
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }

            render view: 'list', model: [ categories: categories, products: products, selectedProduct: product,
                    assetManagementPossible: assetManagementPossible, selectedCategory: productCategory, selectedCategoryId:productCategory.id, filters: filters]
        }
    }

    /**
     * Delete the given category id
     */
    @Secured(["PRODUCT_CATEGORY_52"])
    def deleteCategory () {

		def category = params.id ? ItemTypeDTO.get(params.id) : null

        if (params.id && !category) {
            flash.error = 'product.category.not.found'
            flash.args = [ params.id  as String]
            render template: 'productsTemplate', model: [ products: products ]
            return
        }

        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.category.not.selected'
            flash.args = [ params.id  as String]

            render template: 'productsTemplate', model: [ products: products ]
            return
        }

        if (params.id) {
            securityValidator.validateCompanyHierarchy(category?.entities*.id, category?.entity?.id, category?.global)
            try {
                webServicesSession.deleteItemCategory(params.int('id'))

                log.debug("Deleted item category ${params.id}.");

                flash.message = 'product.category.deleted'
                flash.args = [ params.id as String]

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                flash.error = 'product.category.delete.error'
                flash.args = [ params.id as String ]
            }
        }

		params.id = null
		redirect action: 'index'
    }

    /**
     * Delete the given product id
     */
    @Secured(["PRODUCT_42"])
    def deleteProduct () {

        if (params.id) {
        	ItemDTO product = ItemDTO.get(params.int('id'))

            securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global)
            try {
                webServicesSession.deleteItem(params.int('id'))

                log.debug("Deleted item ${params.id}.");

                flash.message = 'product.deleted'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                flash.error = 'product.delete.errorr'
                flash.args = [ params.id as String ]
            }
        }
        // call the rendering action directly instead of using 'chain' or 'redirect' which results
        // in a second request that clears the flash messages.

        // return the products list, pass the category so the correct set of products is returned.
        redirect action:  "list", params: [id : params.category ? params.category : (ItemDTO.get(params.int('id')))?.getItemTypes()?.getAt(0)?.id]

    }

    /**
     * List assets linked to a product. ID parameter identifies a product
     */
    def assets () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        if(!params.id){
            list()
            return
        }

        ItemDTO product = ItemDTO.get(params.int('id'))
        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global, true)
        if(!product){
            return response.sendError(Constants.ERROR_CODE_404)
        }
        ItemTypeDTO assetManType = new ItemTypeBL().findItemTypeWithAssetManagementForItem(product.id);
        assetManType.assetMetaFields.size();
        assetManType.assetStatuses.size();

        //add breadcrumb
        breadcrumbService.addBreadcrumb(controllerName, 'assets', null, params.int('id'), product?.internalNumber)

        params.put("itemId", product?.id)
        params.put("deleted", 'on' == params.showDeleted ? 1 : 0)

        def assets
        try {
            assets = productService.getFilteredAssets(session['company_id'], [] as List, [] as List, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }

        def assetStatuses = assetManType.assetStatuses.findAll{ it.deleted == 0 } as List
        assetStatuses << new AssetStatusDTOEx(0,Constants.RESERVAED_STATUS, 0,0,0,0)
        def model = [        assets: assets,
                            product: product,
                         metaFields: assetManType.assetMetaFields.findAll{ it.getDataType() != DataType.STATIC_TEXT },
                      assetStatuses: assetStatuses]
        if(usingJQGrid) {
            render view: 'assetList', model: model
            return
        }

        if (params.applyFilter || params.partial) {
            render template: 'assets', model: model
        } else {
            render view: 'assetList', model: model
        }
    }

    def findAssets () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        ItemDTO product = ItemDTO.get(params.int('id'))

        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global)

        params.put("itemId", product?.id)
        params.put("deleted", 'on' == params.showDeleted ? 1 : 0)
        def assets
        try {
            assets = productService.getFilteredAssets(session['company_id'], [] as List, [] as List, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }
        try {
            render getItemsJsonData(assets, params) as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Delete the selected asset as specified by the 'id' parameter
     */
    @Secured(["PRODUCT_CATEGORY_STATUS_AND_ASSETS_132"])
    def deleteAsset () {
        if (params.id) {
            def asset = AssetDTO.get(params.id)
            securityValidator.validateCompanyHierarchy(asset?.item?.entities*.id, asset?.item?.entity?.id, asset?.item?.global)

            try {
                webServicesSession.deleteAsset(params.int('id'))

                log.debug("Deleted asset ${params.id}.");

                flash.message = 'asset.deleted'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            }
        }

        params.id = params.itemId
        //reload and display the assets
        assets()

    }

    /**
     * Release reservation of the selected asset as specified by the 'id' parameter
     */
    @Secured(["PRODUCT_CATEGORY_STATUS_AND_ASSETS_133"])
    def releaseAssetReservation (){
        def assetID = params.int('id')
        if (assetID) {
            try {
                webServicesSession.releaseAsset(assetID, null);
                log.debug("Reservation released for asset id: ${params.id}.");
                flash.message = 'asset.reservation.released'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            }
        }

        redirect action: 'showAsset', params: [id: assetID]
    }

    /**
     * Load an asset for editing or display an empty screen for creating an asset.
     */
    @Secured(["hasAnyRole('PRODUCT_CATEGORY_STATUS_AND_ASSETS_130', 'PRODUCT_CATEGORY_STATUS_AND_ASSETS_131')"])
    def editAsset () {
        def asset;
        if (!params.boolean('paste')) {
            asset = params.id ? AssetDTO.get(params.id) : new AssetDTO()
        } else {
            asset = getCopiedAsset()
        }

		ItemDTO itemDTO = ItemDTO.get(params.prodId)

        if(asset.id) {
		    securityValidator.validateCompanyHierarchy(asset.item?.entities*.id, asset.item?.entity?.id, asset.item?.global)
        }

        //if param id is provided we are editing
        if (params.id && !asset) {
            flash.error = 'product.asset.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        //if id is not provided we must be adding
        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.asset.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        //if we are adding we must know which product it belongs to
        if (params.boolean('add') && !params.prodId) {
            flash.error = 'product.not.selected'
            flash.args = [ params.prodId  as String]
            redirect controller: 'product', action: 'list'
            return
        }

        if (params.boolean('add')) {
            securityValidator.validateCompanyHierarchy(itemDTO?.entities*.id, itemDTO?.entity?.id, itemDTO?.global)
            if(!itemDTO){
                return response.sendError(Constants.ERROR_CODE_404)
            }
            asset.item = itemDTO
        }

        //find the item type which allows asset management. The asset identifier label is defined in the type.
        def categoryWithAssetManagement = asset.item.findItemTypeWithAssetManagement();

        //get the alowed statuses from the item type
        List orderedStatuses = new AssetStatusBL().getStatuses(categoryWithAssetManagement.id, false);
        def allowedStatuses = []
        orderedStatuses.each {
            if(it.isOrderSaved==0 && it.deleted==0) {
                allowedStatuses << it
            }
        }
        if(!asset.id) {
            asset.assetStatus = orderedStatuses.find {it.isDefault}
        }
        //if we are editing we can create a breadcrumb
        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'update', params.int('id'), asset.identifier)
        }

		def entities = new ArrayList<Integer>(0);
		if(!asset?.isGlobal()) {
			if(asset?.entity) {
				entities.add(asset?.entityId)
			}
			if(asset?.entities?.size() > 0) {
				for(def entity : asset.entities) {
					entities.add(entity)
				}
			}
		}

        def companies = []

        if(asset.item.isGlobal()){
            //if this asset can belong to other companies besides the user's
            if((params.userCompanyMandatory ? !params.boolean('userCompanyMandatory') : true)) {
                companies = retrieveChildCompanies()
            }
            companies << CompanyDTO.get(session['company_id'])
        }else{
            // only show the companies which are visible to the product.
            companies = asset.item.getEntities()
        }

		def availableCategories= productService.getItemTypes(session['company_id'], null)

        if(params.partial) {
            render template: 'editAssetContent',
                      model: [               asset: asset,
                               availableCategories: availableCategories,
                                          statuses: allowedStatuses,
                                 categoryAssetMgmt: categoryWithAssetManagement,
                                         companies: companies,
                                           partial: true,
                                           isGroup: params.isGroup,
                              userCompanyMandatory: params.userCompanyMandatory ?: 'false']
        } else {
            [                asset: asset,
               availableCategories: availableCategories,
                          statuses: allowedStatuses,
                 categoryAssetMgmt: categoryWithAssetManagement,
                         companies: companies,
                           isGroup: params.isGroup,
              userCompanyMandatory: params.userCompanyMandatory ?: 'false']
        }
    }
    
    def getCopiedAsset() {
        def copiedAsset = AssetDTO.get(session.getAttribute(Constants.COPIED_ASSET))
        if (params.categoryId != copiedAsset.item.findItemTypeWithAssetManagement().id as String) {
            flash.error = 'validation.asset.paste.different.category'
            return new AssetDTO()
        }
        AssetDTO asset = new AssetDTO()
        asset.identifier = copiedAsset.identifier + Constants.COPIED
        asset.metaFields = copiedAsset.metaFields
        asset.metaFields.each {
            it.id = null
        }
        return asset
    }
    
    def showProvisioning () {

    }

    /**
     * Display an asset. Asset ID is required.
     */
    def showAsset () {
        if (!params.id) {
            flash.error = 'product.asset.not.selected'
            flash.args = [ params.id  as String]
            redirect controller: 'product', action: 'list'
            return
        }

        //load the asset
        def asset = AssetDTO.get(params.id)

		securityValidator.validateCompanyHierarchy(asset.item.entities*.id, asset.item.entity?.id, asset.item.global)

        def reservation = asset?.getId()?new AssetReservationDAS().findActiveReservationByAsset(asset?.getId()):null
        asset.setReserved((reservation?true:false) as Boolean)

        //find the category the asset belongs to
        def categories = ItemTypeDTO.createCriteria().list() {
            eq("allowAssetManagement", 1)
            createAlias("items","its")
            eq("its.id", asset.item.id)
        }

        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'show', params.int('id'), asset.identifier)
        }

        //if we must show a template
        if(params.template) {
            render template: 'showAsset', model: [ asset : asset, category: categories?.first(), reservation : reservation]

        //else show the asset list
        } else {

            def itemId = asset.item.id

            def assets = AssetDTO.createCriteria().list(max: params.max) {
                eq("item.id", itemId)
                eq("entity.id", session['company_id'])
                eq("deleted", 0)
            }

            ItemTypeDTO assetManType = new ItemTypeBL().findItemTypeWithAssetManagementForItem(itemId)
            render view: 'assetList', model: [assets: productService.setReservedFlag(assets), selectedAsset: asset, product: asset.item, id: itemId, category: categories?.first(),
                    metaFields:  assetManType.assetMetaFields.findAll{it.getDataType() != DataType.STATIC_TEXT}, assetStatuses:  assetManType.assetStatuses, reservation:reservation]
        }
    }

    /**
     * Copy an asset. Asset ID is required.
     */
    def copyAsset () {
        session.setAttribute(Constants.COPIED_ASSET, params.id)
        redirect action: 'showAsset', params: [id: params.id] 
    }
    
    /**
     * Validate and save an asset
     */
    @Secured(["hasAnyRole('PRODUCT_CATEGORY_STATUS_AND_ASSETS_130', 'PRODUCT_CATEGORY_STATUS_AND_ASSETS_131')"])
    saveAsset() {
        def availableFields = new ArrayList<MetaField>()
        def asset = new AssetWS()
        //bind the parameters to the asset
        bindData(asset, params, [exclude: ['id', 'identifier']])
        //bind the meta fields

        boolean isGlobal = asset?.global ?: false
        def isRoot = new CompanyDAS().isRoot(session['company_id'] as Integer)

        if (!isRoot) {
            asset.global = false
            asset.entities = [session['company_id'] as Integer]
        } else if (isGlobal) {
            asset.global = true
            asset.entities = new ArrayList<>(0)
        }

        //select meta fields
        if (isGlobal) {
            for (Integer companyId : retrieveCompaniesIds()) {
                availableFields.addAll(MetaFieldBL.getAvailableFieldsList(companyId, EntityType.ASSET))
            }
        } else {
            for (Integer companyId : asset.entities) {
                availableFields.addAll(MetaFieldBL.getAvailableFieldsList(companyId, EntityType.ASSET))
            }
        }

        ItemTypeDTO itemType = ItemDTO.get(asset.itemId).findItemTypeWithAssetManagement()
        asset.metaFields = MetaFieldBindHelper.bindMetaFields(itemType.assetMetaFields, params)

        asset.id = !params.id?.equals('') ? params.int('id') : null

        if(asset.id) {
            ItemDTO itemDTO = AssetDTO.get(asset.id).item
            securityValidator.validateCompanyHierarchy(itemDTO?.entities*.id, itemDTO?.entity?.id, itemDTO?.global)
        }

        log.debug "entity id ${params['asset.entityId']}"

        asset.entityId = session['company_id'].toInteger()
        asset.identifier = params.identifier.trim()

        //if this is an asset group bind the contained assets
        if (params.isGroup) {
            asset.containedAssetIds = params.containedAssetIds.split(',')
                    .findAll { it.length() > 0 }
                    .collect { new Integer(it) } as Integer[]
        } else {
            asset.containedAssetIds = []
        }

        try {
            if (asset.id) {
                //if the user has access update the asset
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_CATEGORY_STATUS_AND_ASSETS_130")) {
                    webServicesSession.updateAsset(asset)
                } else {
                    render view: '/login/denied'
                    return
                }
            } else {
                //if the user has permission add the asset
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_CATEGORY_STATUS_AND_ASSETS_131")) {
                    asset.id = webServicesSession.createAsset(asset)
                } else {
                    render view: '/login/denied'
                    return
                }
            }
        } catch (SessionInternalError e) {
            //got an exception, show the edit page again
            viewUtils.resolveException(flash, session.locale, e)
            if (flash.error) {
                flash.errorMessages.add(g.message(message: flash.error))
                flash.error = null
            }

            List orderedStatuses = new AssetStatusBL().getStatuses(params.int('categoryId'), false)
            def dto = new AssetBL().getDTO(asset)
            dto.discard()

            def companies = []
            //if this asset can belong to other companies besides the user's
            if ((params.userCompanyMandatory ? !params.boolean('userCompanyMandatory') : true)) {
                companies = retrieveChildCompanies()
            }

            companies << CompanyDTO.get(session['company_id'])
            def availableCategories = productService.getItemTypes(session['company_id'], null)
            render template: 'editAssetContent',
                    model: [asset               : dto,
                            statuses            : orderedStatuses,
                            categoryAssetMgmt   : ItemTypeDTO.get(params.categoryId),
                            availableCategories : availableCategories,
                            companies           : companies,
                            partial             : true,
                            isGroup             : params.isGroup,
                            userCompanyMandatory: params.userCompanyMandatory ?: 'false']
            return
        }

        render "<asset id='" + asset.id + "' itemId='" + asset.itemId + "' />"
    }

    /**
     * Display the uploadAssets template.
     *
     * @param prodId    ItemDTO id that will be linked to the new assets
     */
    def showUploadAssets () {
        ItemDTO itemDTO = ItemDTO.get(params.prodId)
        securityValidator.validateCompanyHierarchy(itemDTO?.entities*.id, itemDTO?.entity?.id, itemDTO?.global)
        ItemTypeDTO itemTypeDTO = itemDTO.findItemTypeWithAssetManagement()
        AssetStatusDTO defaultStatus = itemTypeDTO.findDefaultAssetStatus() //status that the new assets will have

        render template: 'uploadAssets', model: [product:  itemDTO, category: itemTypeDTO, defaultStatus: defaultStatus]
    }

    /**
     *  When searching for asset to add to the group, this page will load the status, product and meta fields filter for
     *  a given category.
     *
     *  @param categoryId   ItemTypeDTO id.
     */
    def loadAssetGroupFilters () {
        ItemTypeDTO itemTypeDTO = ItemTypeDTO.get(params.categoryId)
        securityValidator.validateCompanyHierarchy(itemTypeDTO?.entities*.id, itemTypeDTO?.entity?.id, itemTypeDTO?.global)
        render template: 'groupSearchFilter',
                  model: [assetStatuses: itemTypeDTO?.assetStatuses?.findAll { it.deleted == 0 },
                               products: itemTypeDTO?.items?.findAll { it.deleted == 0 && !it.isPlan() },
                             metaFields: itemTypeDTO?.assetMetaFields]
    }

    /**
     * Search function when trying to add assets to a group.
     */
    def groupAssetSearch () {
        def assets
        try {
            params.max = params?.int('max') ?: pagination.max
            params.offset = params?.int('offset') ?: pagination.offset
            params.sort = params?.sort ?: pagination.sort
            params.order = params?.order ?: pagination.order

            params['groupIdNull'] = 'true'
            params['orderLineId'] = 'NULL'
            params['categoryId'] = params['searchCategoryId']
            params['itemId'] = params['searchItemId']
            params['statusId'] = params['searchStatusId']

            //always the exclude the group we are editing from the search results.
            def assetsToExclude = (params.searchAssetId ? [params.int('searchAssetId')] : []) as List

            //exclude all currently selected assets
            if(params['searchExcludedAssetId']) {
                assetsToExclude.addAll(params.searchExcludedAssetId.split(',').collect{new Integer(it)})
            }

            //include the assets which are contained in the persisted group in case the user removed it in the UI and wants to add it back
            //if an ID is in the include and exclude set, the exclude will take precedence
            def assetsToInclude = (params.searchIncludedAssetId ? params.searchIncludedAssetId.split(',').collect{new Integer(it)} : []) as List

            //include assets which are part of this persisted asset group.
            if (params.searchAssetId) {
                params['groupId'] = params['searchAssetId']
            }

            assets = productService.getFilteredAssets(session['company_id'], assetsToExclude, assetsToInclude, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }
        render template:  'groupSearchResults', model:  [assets: assets]
    }

    /**
     * Upload a file containing new assets. Start a batch job to import the assets.
     *
     * @param assetFile - CSV file containing asset definitions
     * @param prodId - product the assets will belong to
     */
    @Secured(["hasAnyRole('PRODUCT_CATEGORY_STATUS_AND_ASSETS_133')"])
    def uploadAssets () {
        def file = request.getFile('assetFile');
        def reportAssetDir = new File(Util.getSysProp("base_dir") + File.separator + "reports" + File.separator + "assets");

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb
        def csvFile = File.createTempFile("assets", ".csv", reportAssetDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId, showAssetUploadTemplate: true])
            return
        } else if(!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId, showAssetUploadTemplate: true])
            return
        }
        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile("assetsError", ".csv", reportAssetDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        ItemDTO itemDTO = ItemDTO.get(params.int('prodId'))

        securityValidator.validateCompanyHierarchy(itemDTO?.entities*.id, itemDTO?.entity?.id, itemDTO?.global)

        def executionId = 0
		try {
	        //start a batch job to import the assets
			executionId = webServicesSession.startImportAssetJob(itemDTO.id,
				itemDTO.findItemTypeWithAssetManagement().assetIdentifierLabel ?: message([code:  'asset.detail.identifier']),
				message([code:  'asset.detail.notes']),message([code:  'asset.detail.global']),message([code:  'asset.detail.entities'])
				,csvFile.absolutePath, csvErrorFile.absolutePath)
		} catch (SessionInternalError e) {
			viewUtils.resolveException(flash, session.locale, e);
			render view: 'uploadAssets'
		}
        render view: 'processAssets', model: [jobId: executionId, jobStatus: 'busy']
    }

    /**
     * Validate and save a category.
     */
    @Secured(["hasAnyRole('PRODUCT_CATEGORY_50', 'PRODUCT_CATEGORY_51')"])
    @RequiresValidFormToken
    def saveCategory () {
        def category = new ItemTypeWS()
        def metaFieldIdxs = []
        def metafields = []
        def statuses = []

        def isRoot = new CompanyDAS().isRoot(session['company_id'])

        // grails has issues binding the ID for ItemTypeWS object...
        // bind category ID manually
        bindData(category, params, 'id')
        bindMetaFields(category,params,isRoot,EntityType.PRODUCT_CATEGORY)
        category.id = !params.id?.equals('') ? params.int('id') : null

		def isNew = false
		def rootCreated = false

		if(!category.id || category.id == 0) {
			isNew = true
		} else {
            def oldCategory = ItemTypeDTO.get(params.id)
            securityValidator.validateCompanyHierarchy(oldCategory?.entities*.id, oldCategory?.entity?.id, oldCategory?.global, true)
        }

		try {
			//Validation during edit category.
			if(!isNew && !category.global){
				//Load all product for this category to make sure none of the products refer a company thats not part of this category
				def itemTypesArr = new ArrayList()
				itemTypesArr.add(ItemTypeDTO.get(category.id))
				log.debug "TYPES:"+itemTypesArr
		        def items = ItemDTO.createCriteria().list() {
		            createAlias("itemTypes", "itemTypes")
		            and {
						'in'('itemTypes.id', itemTypesArr?.id)
		                eq('deleted', 0)
		            }
		        }
		        def companies = new java.util.HashSet()
		        for(ItemDTO item: items){
		        	companies.addAll(item.entities)
		        }

		        for(CompanyDTO co: companies){
		        	def found = false
		        	def notFoundEntity = null
		        	for(Integer entId: category.entities){
		        		if(co.id==entId){
		        			found = true
		        		}
		        	}

		        	if(!found){
	                    String[] errmsgs = new String[1]
                        errmsgs[0] = "ItemTypeWS,companies,validation.error.wrong.company.selected.category," + CompanyDTO.get(co.id)?.description
                        throw new SessionInternalError("Validation of Entities", errmsgs);
					}
		        }
			}

            category.allowAssetManagement = params.allowAssetManagement ? 1 : 0

            //BIND THE STATUSES
            def assetIdxs = []
            Pattern pattern = Pattern.compile(/assetStatus.(\d+).id/)
            //get all the ids in an array
            params.each{
                def m = pattern.matcher(it.key)
                if( m.matches()) {
                    assetIdxs << m.group(1)
                }
            }

            //get the status values for each id and create the statuses
            assetIdxs.each {
                def name = params['assetStatus.'+it+'.description']
                AssetStatusDTOEx status = new AssetStatusDTOEx(
                        description: params['assetStatus.'+it+'.description']
                )

                BindHelper.bindPropertyPresentToInteger(params, status, ["isDefault", "isAvailable", "isOrderSaved", "isActive", "isPending", "isOrderFinished"], 'assetStatus.'+it+'.')
                BindHelper.bindInteger(params, status, ["id"], 'assetStatus.'+it+'.')

                if(status.description.length() > 0 || status.id > 0) {
                    statuses << status
                }
            }

            //BIND THE META FIELDS
            pattern = Pattern.compile(/metaField(\d+).id/)
            //get all the ids in an array
            params.each{
                def m = pattern.matcher(it.key)
                if( m.matches()) {
                    metaFieldIdxs << m.group(1)
                }
            }

            //get the meta field values for each id a
            metaFieldIdxs.each {
                MetaFieldWS metaField = MetaFieldBindHelper.bindMetaFieldName(params, it)
                metaField.primary = false
                metaField.entityId = session['company_id']

                metafields << metaField
            }

            //if asset management is enabled, set statuses and meta fields
            if(category.allowAssetManagement == 1) {
                category.assetStatuses.addAll(statuses)
                category.assetMetaFields.addAll(metafields)
            }

			if(category.isGlobal()) {
				//Empty entities
				category.entities = new ArrayList<Integer>(0);
			} else {
				//Validate for entities
				if(category.getEntities() == null || category.getEntities().size() == 0) {
					String [] errors = ["ItemTypeWS,companies,validation.error.no.company.selected"]
					throw new SessionInternalError("validation.error.no.company.selected", errors)
				}
			}
		} catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            renderEditCategoryView(category, statuses, metafields)
            return
        }

        // save or update
        try {

			if(category.description) {
				category.description= category.description.trim()
			}

            if (!category.id || category.id == 0) {
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_CATEGORY_50")) {
                    if (category.description?.trim()) {
                        log.debug("creating product category ${category}")


						category.id = webServicesSession.createItemCategory(category)

                        flash.message = 'product.category.created'
                        flash.args = [category.id as String]
                    } else {
                        log.debug("there was an error in the product category data.")

                        category.description = StringUtils.EMPTY

                        flash.error = message(code: 'product.category.error.name.blank')

                        renderEditCategoryView(category, statuses, metafields)
                        return
                    }
                } else {
                    render view: '/login/denied'
                    return
                }
            } else {
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_CATEGORY_51")) {
                    if (category.description?.trim()) {
                        log.debug("saving changes to product category ${category.id}, ${category.isGlobal()}")

						webServicesSession.updateItemCategory(category)

                        flash.message = 'product.category.updated'
                        flash.args = [category.id as String]
                    } else {
                        log.debug("there was an error in the product category data.")

                        category.description = StringUtils.EMPTY

                        flash.error = message(code: 'product.category.error.name.blank')
                        renderEditCategoryView(category, statuses, metafields)
                        return
                    }

                } else {
                    render view: '/login/denied'
                    return
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            renderEditCategoryView(category, statuses, metafields)
            return
        }

        chain action: 'list', params: [id: category.id]
    }

    /**
     * copy input fields onto the category before rendering the editCategory view.
     *
     * @param category      category to display
     * @param metaFieldIdxs meta field indexes as passed in through the params
     */
    private void renderEditCategoryView(ItemTypeWS category, Collection statuses, Collection metafields) {
        List<MetaField> availableFields = new ArrayList<MetaField>()
		ItemTypeDTO categoryForUI = new ItemTypeDTO();
		bindData(categoryForUI, params, 'id')
		categoryForUI.setDescription(category.getDescription());
		categoryForUI.setGlobal(category.isGlobal());
		categoryForUI.setOrderLineTypeId(category.getOrderLineTypeId());
		categoryForUI.setAssetIdentifierLabel(category.getAssetIdentifierLabel());
		categoryForUI.setOnePerCustomer(category.isOnePerCustomer());
		categoryForUI.setOnePerOrder(category.isOnePerOrder());

		Set<CompanyDTO> childEntities = new HashSet<CompanyDTO>(0);

		for(Integer entity : category.getEntities()){
			childEntities.add(new CompanyDAS().find(entity));
		}

        //select meta fields
        if (category.isGlobal()) {
            for(Integer companyId : retrieveCompaniesIds()) {
                availableFields.addAll(retrieveAvailableCategoryMetaFields(companyId))
            }
        } else if(category.entities){
            for(def company : category.entities) {
                availableFields.addAll(retrieveAvailableCategoryMetaFields(company))
            }
        }else{
            availableFields.addAll(retrieveAvailableCategoryMetaFields(session['company_id']))
        }

		categoryForUI.setEntities(childEntities)

        categoryForUI.allowAssetManagement = params.allowAssetManagement ? 1 : 0

        categoryForUI.assetStatuses = new AssetStatusBL().convertAssetStatusDTOExes(statuses)
        categoryForUI.assetMetaFields = MetaFieldBL.convertMetaFieldsToDTO(metafields, session['company_id']);

        ItemTypeBL.fillMetaFieldsFromWS(categoryForUI, category)

        render view: "editCategory", model: [category: categoryForUI,companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(),
                orderedStatuses: (params.id ? categoryForUI.assetStatuses.findAll { it.isInternal == 0 } : []),
                availableFields: availableFields, availableFieldValues:category.metaFields,
                parentCategories: getProductCategories(false, category?.id ?: null), entityId: category.entityId]
    }

    /**
     * Get the item category to be edited and show the "editCategory.gsp" view. If no ID is given
     * this view will allow creation of a new category.
     */
    @Secured(["hasAnyRole('PRODUCT_CATEGORY_50', 'PRODUCT_CATEGORY_51')"])
    def editCategory () {
        def category = params.id ? ItemTypeDTO.get(params.id) : new ItemTypeDTO()

        if(category.id) {
            securityValidator.validateCompanyHierarchy(category?.entities*.id, category?.entity?.id, category?.global, true)
        }

        List<MetaField> availableFields
        if (params.id && !category) {
            flash.error = 'product.category.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.category.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        if(category.isGlobal()){
            availableFields  = MetaFieldBL.getMetaFields(retrieveCompaniesIds(), EntityType.PRODUCT_CATEGORY)
        }else if (params.id && new CompanyDAS().isRoot(Integer.parseInt(session['company_id'].toString())) ){
            availableFields = MetaFieldBL.getMetaFields(category.getEntities()*.id, EntityType.PRODUCT_CATEGORY)
        }else if (params.id){
            availableFields = MetaFieldBL.getMetaFields([session['company_id'] as Integer], EntityType.PRODUCT_CATEGORY)
        }else{
            availableFields = retrieveAvailableCategoryMetaFields(session['company_id'])
        }

        MetaFieldValueWS[] availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        List orderedStatuses = (params.id ? new AssetStatusBL().getStatuses(category.id, false) : [])

        breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), category?.description)

        [category : category, orderedStatuses: orderedStatuses, parentCategories: getProductCategories(false, category?.id ?: null),
         companies: retrieveChildCompanies(), allCompanies: retrieveCompanies(), entityId: category?.entity?.id,
         availableFields: availableFields,availableFieldValues:availableFieldValues]
    }

    /**
     * Use the meta fields which are part of a metafield group to act as template for category meta fields
     *
     * @param groupId - MetaFieldGroup id
     */
    def populateCategoryMetaFieldsForEdit () {
        List<MetaFieldWS> metaFields;
        if(params.groupId && params.groupId != 'null') {
            MetaFieldGroup group = MetaFieldGroup.get(params.int('groupId'))
            metaFields = group.metaFields.collect {MetaFieldBL.getWS(it)}
            metaFields.each {
                it.id = 0
            }
        } else {
            metaFields = new ArrayList<>(0);
        }
        render template: 'editCategoryMetaFieldsCollection', model: [ metaFields: metaFields, startIdx: params.startIdx ? params.int('startIdx'): 0, moveMetaFields: true]
    }

    /**
     * Use the meta field specified by 'mfId' to act as template for category meta field
     *
     * @param mfId - MetaField id
     */
    def populateMetaFieldForEdit () {
        MetaFieldWS metaField;
        if(params.mfId && params.mfId != 'null') {
            metaField = MetaFieldBL.getWS(MetaField.read(params.int('mfId')))
            metaField.id = 0
        } else {
            metaField = null
        }
        render template: 'editCategoryMetaField', model: [ metaField: metaField, metaFieldIdx: params.startIdx ?: 0, moveMetaFields: true ]
    }

    /**
     * Get the item to be edited and show the "editProduct.gsp" view. If no ID is given
     * this screen will allow creation of a new item.
     */
    @Secured(["hasAnyRole('PRODUCT_40', 'PRODUCT_41')"])
    def editProduct () {
        def product
        def availableFields

        if (params.id) {
            def dto = ItemDTO.get(params.id)
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global, true)
        }

        try {
            product = params.id ? webServicesSession.getItem(params.int('id'), session['user_id'] as Integer, null) : null

            if (product && product.deleted == 1) {
                productNotFoundErrorRedirect(params.id)
                return
            }
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
            productNotFoundErrorRedirect(params.id)
            return
        }

        // Combine child entities and root entity into a single list
        def entities = new ArrayList<Integer>(0);
        if (!product?.isGlobal()) {
            if (product?.entities?.size() > 0) {
                for (def entity : product.entities) {
                    entities.add(entity)
                }
            }
        }

        if (params.copyFrom) {
            cleanProduct(product)
        }

        availableFields = MetaFieldBL.getMetaFields([session['company_id'] as Integer], EntityType.PRODUCT)

		def priceModel = product?.defaultPrice

        if (params.int('id')) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), product?.number)
        } else {
            String parameters = "category:$params.category"
            breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), product?.number, parameters)
        }
        def categories = getProductCategories(false, null)
        def typeSet = (product ? product.types : []) as Set
        def allowAssetManagement = false
		def subscriptionCategory = false
        List<CompanyDTO> categoriesRelatedCompanies

        List<ItemTypeDTO> selectedItemTypes
        try{
            selectedItemTypes = categories.findAll {
                params.list('category').collect { Integer.valueOf(it as String) }.contains(it.id)
            } as List<ItemTypeDTO>
        } catch (NumberFormatException nfe){
            return response.sendError(Constants.ERROR_CODE_404)
        }

        if(selectedItemTypes){
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
        }else{
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies((product ? product.types.collect { ItemTypeDTO.get(it) } : []) as List<ItemTypeDTO>)
        }

        categories.each {
            if(typeSet.contains(it.id) && it.allowAssetManagement) {
                allowAssetManagement = true
            }
            if(it.orderLineTypeId == Constants.ORDER_LINE_TYPE_SUBSCRIPTION) {
                def currentCategoryId = it.id;
                if (params.category == "$currentCategoryId") {
                    subscriptionCategory = true
                }

                product?.types?.each {
                    if ("$it" == currentCategoryId) {
                        subscriptionCategory = true
                    }
                }
			}
        }

        Integer[] excludedItemTypeIds = [] as Integer[]
        if(product) {
            excludedItemTypeIds = product.getDependencyIdsOfType(ItemDependencyType.ITEM_TYPE)
        }

		def showEntityListAndGlobal = CompanyDTO.get(product?.entityId)?.parent == null
        def isCategoryGlobal

        if( product?.id || params.copyFrom ) {
            isCategoryGlobal = categories.any {typeSet.contains(it.id) && it.global}
        } else {
            ItemTypeDTO category = ItemTypeDTO.get(params.int('category'))
            if(!category){
                return response.sendError(Constants.ERROR_CODE_404)
            }
            isCategoryGlobal = category?.global ?: false
            allowAssetManagement = category.allowAssetManagement > 0
        }
        def allRatingUnits = getRatingUnitsForEntity()
        def allusageRatingSchemes = getUsageRatingSchemes()

        def prodRatingConfigurations = product?.ratingConfigurations
        def currentDateRating
        try {
            if (!MapUtils.isEmpty(prodRatingConfigurations)) {
                currentDateRating = prodRatingConfigurations.lastKey()
            }
        } catch (NoSuchElementException x) { }


        Integer assetReservationDefaultValue = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_ASSET_RESERVATION_DURATION) as int

        [product                     : product,
         currencies                  : retrieveCurrencies(),
         categories                  : categories,
         categoryId                  : params.int('category'),
         availableFields             : availableFields,
         defaultPrices               : product?.defaultPrices,
         priceModelData              : priceModel ? initPriceModelData(priceModel) : null,
         dependencyItemTypes         : getDependencyItemTypes(excludedItemTypeIds),
         dependencyItems             : null,
         dependentTypes              : product?.getDependenciesOfType(ItemDependencyType.ITEM_TYPE),
         dependentItems              : product?.getDependenciesOfType(ItemDependencyType.ITEM),
         availableAccountTypes       : getAvailableAccountTypes(),
         allowAssetManagement        : allowAssetManagement,
         subscriptionCategory        : subscriptionCategory,
         orderLineMetaFields         : product?.orderLineMetaFields,
         companies                   : retrieveChildCompanies(),
         allCompanies                : categoriesRelatedCompanies,
         entities                    : entities,
         showEntityListAndGlobal     : showEntityListAndGlobal,
         isCategoryGlobal            : isCategoryGlobal,
         assetReservationDefaultValue: assetReservationDefaultValue,
         allRatingUnits              : allRatingUnits,
         allusageRatingSchemes       : allusageRatingSchemes,
         ratingConfigurations        : product?.ratingConfigurations,
         startDateRating             : currentDateRating

        ]
    }

    @Secured(["hasAnyRole('PRODUCT_40', 'PRODUCT_41')"])
    def getCategoriesCompanies() {

        def categories = getProductCategories(false, null)
        List<CompanyDTO> categoriesRelatedCompanies

        List<ItemTypeDTO> selectedItemTypes = categories.findAll {
            params.list('productTypes[]').collect { Integer.valueOf(it) }.contains(it.id)
        }

        if(selectedItemTypes){
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
        }else{
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(categories)
        }

        StringBuilder options = new StringBuilder()

        categoriesRelatedCompanies.each{ CompanyDTO company ->
            options.append("<option value='${company.id}'>${company.description} </option>")
        }

        render( options.toString() )
    }

    /**
     * Remove all entities Id, in this way the product will be saved like a new entity
     * (with all new subentities associated to it)
     * @param product
     */
    def cleanProduct(product) {
        if (product != null) {
            product.id = null
            params.id = null

            cleanPrice(product?.defaultPrice)
            for (price in product.defaultPrices) {
                cleanPrice(price.value)
            }

            for (orderLineMetafield in product.orderLineMetaFields) {
                orderLineMetafield.id = 0
                orderLineMetafield.entityId = null
            }
            for (dependency in product.dependencies) {
                dependency.id = null
            }
            for (metaField in product.metaFields) {
                metaField.id = null
            }
            for (metaFieldMapEntry in product.metaFieldsMap) {
                for (metaField in metaFieldMapEntry.value) {
                    metaField.id = null
                }
            }
        }
    }

    def cleanPrice(price) {
        if (price != null) {
            price.id = 0
            cleanPrice(price.next)
        }
    }
    /**
     * Use the meta field specified by 'mfId' to act as template for product orderline meta field
     *
     * @param mfId - MetaField id
     */
    def populateProductOrderLineMetaFieldForEdit (){
        MetaFieldWS metaField;
        if(params.mfId && params.mfId != 'null') {
            metaField = MetaFieldBL.getWS(MetaField.read(params.int('mfId')))
            metaField.id = 0
        } else {
            metaField = null
        }
        render template: 'editProductMetaField', model: [ metaField: metaField, metaFieldIdx: params.startIdx ?: 0, moveMetaFields: true ]
    }


    private void productNotFoundErrorRedirect(productId) {
    	flash.error = 'product.not.found'
		flash.args = [ productId as String ]
		redirect controller: 'product', action: 'list'
    }

    def updateStrategy () {
        def priceModel = PlanHelper.bindPriceModel(params)
        priceModel?.attributes = null;

		// Keep default prices of selected company
		def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId
		product.defaultPrices = defaultPrices

		def startDate = params.startDate ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate() : null;
        render template: '/priceModel/model',
		model: [ model: priceModel, startDate: startDate, models: product?.defaultPrices,
				 currencies: retrieveCurrencies(),allCompanies : retrieveCompanies(), priceModelData: initPriceModelData(priceModel), product : product]

    }

    def addChainModel () {
        // Keep default prices of selected company
		def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        // add new price model to end of chain
        def model = priceModel
        while (model.next) {
            model = model.next
        }
        model.next = new PriceModelWS();

        render template: '/priceModel/model',
				  model: [ model: priceModel, startDate: startDate, models: defaultPrices,
						   currencies: retrieveCurrencies(), product : product, allCompanies : retrieveCompanies(),
						   priceModelData: priceModel ? setPriceModelData(priceModel) : null ]
    }

    def removeChainModel () {

        // Keep default prices of selected company
		def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

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

        render template: '/priceModel/model',
				  model: [ model: priceModel, startDate: startDate, models: defaultPrices, currencies: retrieveCurrencies(),
					  	   companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(), product : product,
						   priceModelData: priceModel ? setPriceModelData(priceModel) : null  ]
    }

	def refreshChainModel () {

		def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)
		def entityId = params.int('product.priceModelCompanyId')
		def price = new PriceModelWS()
		def defaultPrices = null
        boolean unsaved = false
		boolean forceRefresh = params.int("forceRefreshModel") == 0
		if(forceRefresh) {
			//verify if there are some unsaved prices
			def priceModel = PlanHelper.bindPriceModel(params)
			def model = priceModel
			while (model) {
				if(model.id == null || model.id == 0) {
					unsaved = true
					break
				}
				model = model.next
			}
		}

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global, true)
            defaultPrices = dto.getPricesForSelectedEntity(entityId)
			if(!defaultPrices.isEmpty()) {
				price = defaultPrices?.get(startDate)
			}
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

		// if this action has been redirected from updated that means update was successful
		if(params.int("updated") == 1) {
			flash.message = 'product.updated'
			flash.args = [ product.id ]
		}

        def allCompanies = params.list('product.entities') ? params.list('product.entities').collect { CompanyDTO.get(it) } :
                                                             params.product.global == "on" ? retrieveCompanies() :
                                                                                             CompanyDTO.get(session['entity_id'])
		render template: '/priceModel/model',
				  model: [           model: price,
                            priceModelData: price ? initPriceModelData(price) : null,
					  		     startDate: startDate,
                                    models: defaultPrices,
                                currencies: retrieveCurrencies(),
                                   unsaved: unsaved,
							     companies: retrieveChildCompanies(),
                              allCompanies: allCompanies,
                                   product: product]
	}

    def addAttribute () {
        def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

        def priceModel = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        def modelIndex = params.int('modelIndex')

        // find the model in the chain, and add a new attribute
        def model = priceModel
        for (int i = 0; model != null; i++) {
            if (i == modelIndex) {
                int newIndex = params.int('attributeIndex')
                // workaround for proper attributes indexing
                model.attributes.eachWithIndex { key, value, index ->
                    if (index == newIndex - 1) {
                        if (key) {
                            ++newIndex
                        }
                    }
                }
                // Empty key-value pair inserted
                def attribute = message(code: 'plan.new.attribute.key', args: [newIndex])
                while (model.attributes.containsKey(attribute)) {
                    newIndex++
                    attribute = message(code: 'plan.new.attribute.key', args: [newIndex])
                }
                model.attributes.put(attribute, '')
            }
            model = model.next
        }

        render template: '/priceModel/model',
				  model: [model: priceModel, startDate: startDate, models: defaultPrices,
					  	  currencies: retrieveCurrencies(), priceModelData: priceModel ? initPriceModelData(priceModel) : null,
						  companies : retrieveChildCompanies(), product : product, allCompanies : retrieveCompanies()]
    }

    def removeAttribute () {
        def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

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

        render template: '/priceModel/model',
				  model: [ model: priceModel, startDate: startDate, models: defaultPrices, currencies: retrieveCurrencies(),
					  	   companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(), product : product  ]
    }

    def editDate () {
        def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

        render template: '/priceModel/model',
				  model: [ startDate: startDate, models: defaultPrices, currencies: retrieveCurrencies(),
					  		companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(), product : product  ]
    }

    def addDate () {
        def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		def product = new ItemDTOEx()
		product.id = params.int('product.id')
		product.priceModelCompanyId = entityId

        render template: '/priceModel/model',
				  model: [ model: new PriceModelWS(), models: defaultPrices, currencies: retrieveCurrencies(),
					  		companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(), product : product  ]
    }

    def removeDate () {
        def product = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()

		def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

		product.priceModelCompanyId = entityId

		defaultPrices.remove(startDate)
		product?.defaultPrices?.clear()
		product?.defaultPrices = defaultPrices

        if (SpringSecurityUtils.ifAllGranted("PRODUCT_41")) {
            log.debug("saving changes to product ${product.id}")

            webServicesSession.updateItem(product)

            flash.message = 'product.updated'
            flash.args = [ product.id ]

        } else {
            flash.message = 'product.update.access.denied'
            flash.args = [ product.id ]
        }
        // Passing startDate in model to make sure that if no model is present in product then set default date to EPOCH_DATE
        render template: '/priceModel/model',
				  model: [  startDate: (defaultPrices?.size()>0?null:CommonConstants.EPOCH_DATE) , models: defaultPrices, currencies: retrieveCurrencies(), companies : retrieveChildCompanies(),
					  		allCompanies : retrieveCompanies(), product : product  ]
    }

    def saveDate () {
        def product = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null
		def price = PlanHelper.bindPriceModel(params)
        def startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()
        def originalStartDate = params.originalStartDate ? TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.originalStartDate) : null

		def entityId = params.int('product.priceModelCompanyId')
		def defaultPrices = null

		if(params.int('product.id')) {
			def dto = ItemDTO.get(params.int('product.id'))
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global)
			defaultPrices = dto.getPricesForSelectedEntity(entityId)
		}

        boolean shouldSetEntityOnPrice = product.getPriceModelCompanyId() == null && new CompanyDAS().find(webServicesSession.getCallerCompanyId()).isInvoiceAsReseller();
        if (shouldSetEntityOnPrice) {
            entityId = webServicesSession.getCallerCompanyId();
            price.setId(null);
        }

		product.priceModelCompanyId = entityId

		if(defaultPrices == null || defaultPrices.isEmpty()) {
			defaultPrices = new TreeMap<Date, PriceModelWS>();
		}

		if(originalStartDate != null) {
			defaultPrices.remove(originalStartDate)
		}

		defaultPrices.put(startDate, price)

		product?.defaultPrices = defaultPrices

        try {
            if (SpringSecurityUtils.ifAllGranted("PRODUCT_41")) {
                log.debug("saving changes to product ${product.id}")

                webServicesSession.updateItem(product)

                // re get saved price models
                redirect action: 'refreshChainModel',
                        params: [                    startDate: params.startDate,
                                 'product.priceModelCompanyId': entityId,
                                             forceRefreshModel: 1,
                                                  'product.id': params.int('product.id'),
                                                      entityId: entityId,
                                                       updated: 1]
            } else {
                flash.message = 'product.update.access.denied'
                flash.args = [product.id]
            }

        } catch (SessionInternalError ex) {
            log.error("Error is: " + ex)
            viewUtils.resolveException(flash, session.locale, ex)
        }

        render template: '/priceModel/model',
				  model: [          model: price,
                                startDate: startDate,
                                   models: defaultPrices,
                               currencies: retrieveCurrencies(),
					  	        companies: retrieveChildCompanies(),
                             allCompanies: retrieveCompanies(),
                                  product: product,
							priceModelData: product?.defaultPrices ? setPriceModelData(price) : null  ]
    }
    /**
    * Validate the selected companies for the product have a visibility for one of the selected categories
    */
	def validateProductSave(product) {
		if(product.global) {
            //If product is global.. then check if it is associated with a global category, else render validation error
            boolean flag = false;
            Integer[] types = product.types

            List<CompanyDTO> companyDTOList = retrieveCompanies()
            List<ItemTypeDTO> itemTypeDTOList = ItemTypeDTO.getAll(types)
            List<CompanyDTO> associatedCompanyDTOList = itemTypeDTOList*.entities.flatten()
            associatedCompanyDTOList = associatedCompanyDTOList.unique()

            if(!itemTypeDTOList.any { it.isGlobal() }){
                String [] errors = ["ItemDTOEx,companies,validation.error.no.company.category.mismatch," + ((companyDTOList - associatedCompanyDTOList)*.description)?.first()]
                throw new SessionInternalError("validation.error.no.company.category.mismatch", errors)
            }
			return;
		}
        List<Integer> entityIds = product.entities
        Integer[] types = product.types
        for(Integer entityId : entityIds){
        	CompanyDTO co = CompanyDTO.get(entityId)
    		boolean flag = false;
        	for(Integer typeId : types){
                flag = false;
	        	ItemTypeDTO itemType = ItemTypeDTO.get(typeId)
	        	if(itemType.global){
	        		//If any of the selected types is global.. then the product will have visibility.. no need to validate
	        		flag=true;
	        	}
	        	Set<CompanyDTO> entities = itemType.entities
	        	for(CompanyDTO compDTO : entities){
		        	 if(compDTO.id.equals(co.id)){
	        	    	flag = true;
	        	    	break;
	        	    }
	        	}
                //if any category is neither global nor visible to the selected entity then show validation message
                if (!flag) {
                    break;
                }
        	}
        	if(!flag){
        	    String [] errors = ["ItemDTOEx,companies,validation.error.no.company.category.mismatch," + co?.description]
						throw new SessionInternalError("validation.error.no.company.category.mismatch", errors)
    	    }
        }
	}

    /**
     * Validate and save a product.
     */
    @Secured(["hasAnyRole('PRODUCT_40', 'PRODUCT_41')"])
    @RequiresValidFormToken
    def saveProduct () {

        def oldProduct = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null
        def product = new ItemDTOEx()

        if(oldProduct) {
            def dto = ItemDTO.get(oldProduct.id)
            securityValidator.validateCompanyHierarchy(dto?.entities*.id, dto?.entity?.id, dto?.global, true)
        }

		def availableFields = new ArrayList<MetaField>()

        //BIND THE META FIELDS
        def metaFieldIdxs = []
        def pattern = Pattern.compile(/metaField(\d+).id/)
        //get all the ids in an array
        params.each{
            def m = pattern.matcher(it.key)
            if( m.matches()) {
                metaFieldIdxs << m.group(1)
            }
        }

        product.orderLineMetaFields = new MetaFieldWS[metaFieldIdxs.size()];
        int index = 0;
        //get the meta field values for each id
        metaFieldIdxs.each {
            MetaFieldWS metaField = MetaFieldBindHelper.bindMetaFieldName(params, it)
            metaField.primary = false
            metaField.entityType = EntityType.ORDER_LINE
            metaField.entityId = session['company_id']
            product.orderLineMetaFields[index] = metaField;
            index++;
        }

        try {

			def isRoot = new CompanyDAS().isRoot(session['company_id'])

			bindProduct(product, oldProduct, params, isRoot)

            / * #11258 need to check if there is a comma or dot, because grails data binder removes the numbers after the dot
            and, in the case of the comma, grails removes the comma, i.e.: 56,9 is 569 */
            validateReservationDuration(params.product.reservationDuration, product)

			validateProductSave(product)

			boolean isGlobal = product?.global
			def isNew = false
			def rootCreated = false
			def existing

			if(!product.id || product.id == 0) {
				isNew = true
			} else {
				existing = ItemDTO.get(product?.id)?.entity
				rootCreated =existing == null || existing?.parent == null
			}

			if(isGlobal) {
				product.entities = new ArrayList<Integer>(0);
			} else {
				if( org.apache.commons.collections.CollectionUtils.isEmpty(product.entities) ) {
					String [] errors = ["ItemDTOEx,companies,validation.error.no.company.selected"]
					throw new SessionInternalError("validation.error.no.company.selected", errors)
				}
				//Find all entities belonging to the categories of this product
				def catEntities = new java.util.HashSet()
				for (def typeId in product?.types){
					def itemType = ItemTypeDTO.get(typeId)
					if(itemType.isGlobal()){
                        if(new CompanyDAS().isRoot(Integer.valueOf(session['company_id']))){
                            // if current company is root then find children
                            catEntities.addAll(retrieveCompanies())
                        }else{
                            // else find all the companies from root of caller company
                            catEntities.addAll(retrieveCompaniesForNonRootCompany())
                        }
                        break
					}
					catEntities.addAll(itemType.entities)
				}

				log.debug "CAT ENT:"+catEntities
				//Now ensure the entities selected is present in the list of companies for the category
				for(Integer entId : product.entities){
					def found=false
					for(CompanyDTO entity: catEntities){
						if(entId==entity.id){
							found=true
						}
					}

					if(!found){
                        String[] errmsgs = new String[1]
                        errmsgs[0] = "ItemDTOEx,companies,validation.error.wrong.company.selected," + CompanyDTO.get(entId)?.description;
                        throw new SessionInternalError("Validation of Entities", errmsgs);
					}
				}

			}

			//select meta fields
			if (isGlobal) {
				for(Integer companyId : retrieveCompaniesIds()) {
					availableFields.addAll(retrieveAvailableMetaFields(companyId))
				}
			} else {
				for(def company : product.entities) {
					availableFields.addAll(retrieveAvailableMetaFields(company))
				}
			}

			if(oldProduct?.global && !isGlobal) {
				if(new OrderDAS().findOrdersOfChildsByItem(product?.id) > 0) {
					String [] errors = ["ProductWS,global,validation.error.cannot.restrict.visibility"]
					throw new SessionInternalError("validation.error.cannot.restrict.visibility", errors)
				}
			}

            // validate cycle in dependencies only for product edit
            def mandatoryItems = product.getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM)
            if (product.id && product.id > 0 && mandatoryItems) {
                 if (findCycleInDependenciesTree(product.id, Arrays.asList(mandatoryItems) )) {
                     String[] errmsgs= new String[1];
                     errmsgs[0]= "ItemDTOEx,mandatoryItems,product.error.dependencies.cycle"
                     throw new SessionInternalError("There is an error in product data.", errmsgs );
                 }
            }

            // save or update
            if (!product.id || product.id == 0) {
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_40")) {
                    log.debug("creating product ${product}")
                    product.id = webServicesSession.createItem(product)
                    flash.message = 'product.created'
                    flash.args = [product.id]
                } else {
                    render view: '/login/denied'
                    return;
                }
            } else {
                if (SpringSecurityUtils.ifAllGranted("PRODUCT_41")) {
                    log.debug("saving changes to product ${product.id}")
                    log.debug("Child entities =  ${product.entities}")
					webServicesSession.updateItem(product)
                    flash.message = 'product.updated'
                    flash.args = [product.id]
                } else {
                    render view: '/login/denied'
                    return;
                }
            }

        } catch (SessionInternalError e) {
            log.error("Error is: " + e)
            viewUtils.resolveException(flash, session.locale, e);

            if(product.standardPartnerPercentage){
                try{
                    Double.parseDouble(product.standardPartnerPercentage)
                }
                catch(NumberFormatException ex){
                    product.standardPartnerPercentage = null
                }
            }

            if(product.masterPartnerPercentage){
                try{
                    Double.parseDouble(product.masterPartnerPercentage)
                }
                catch(NumberFormatException ex){
                    product.masterPartnerPercentage = null
                }
            }

			def startDate = params.startDate ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate() : TimezoneHelper.currentDateForTimezone(session['company_timezone']);
			def selectedPriceModel=product?.defaultPrices ? PriceModelBL.getWsPriceForDate(product.defaultPrices, startDate) : null

            def startDateRating = params.startDateRating ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDateRating).toDate() : TimezoneHelper.currentDateForTimezone(session['company_timezone']);
            def currentRatingConfiguration = product?.ratingConfigurations ? RatingConfigurationBL.getWsRatingConfigurationForDate(product.ratingConfigurations, startDateRating) : null;


            Integer[] excludedItemTypeIds = new ArrayList() as Integer[]
            if(product) {
                excludedItemTypeIds = product.getDependencyIdsOfType(ItemDependencyType.ITEM_TYPE)
            }

			def showEntityListAndGlobal = CompanyDTO.get(oldProduct?.entityId)?.parent == null
            def categories = getProductCategories(false, null)
            List<CompanyDTO> categoriesRelatedCompanies

            List<ItemTypeDTO> selectedItemTypes = categories.findAll {
                params.list('product.types').collect { Integer.valueOf(it) }.contains(it.id)
            }

            if(selectedItemTypes){
                categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
            }else{
                categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(categories)
            }

            def isCategoryGlobal=selectedItemTypes.any{it.global}
            Boolean allowAssetManagement = selectedItemTypes.any { it.allowAssetManagement > 0 }
            for (price in product.defaultPrices) {
                cleanPrice(price.value)
            }
            def allRatingUnits = getRatingUnitsForEntity()
            def allusageRatingSchemes = getUsageRatingSchemes()


            render view: 'editProduct',
                    model: [product                   : product,
                            categories                : categories,
                            currencies                : retrieveCurrencies(),
                            companies                 : retrieveChildCompanies(),
                            allCompanies              : categoriesRelatedCompanies,
                            category                  : params?.selectedCategoryId,
                            availableAccountTypes     : getAvailableAccountTypes(),
                            availableFields           : availableFields,
                            dependencyItemTypes       : getDependencyItemTypes(excludedItemTypeIds),
                            dependencyItems           : null,
                            dependentTypes            : product?.getDependenciesOfType(ItemDependencyType.ITEM_TYPE),
                            dependentItems            : product?.getDependenciesOfType(ItemDependencyType.ITEM),
                            orderLineMetaFields       : product.orderLineMetaFields,
                            priceModelData            : initPriceModelData(selectedPriceModel),
                            defaultPrices             : product?.defaultPrices,
                            selectedPriceModel        : PlanHelper.bindPriceModel(params),
                            startDate                 : startDate,
                            entities                  : product?.entities,
                            showEntityListAndGlobal   : showEntityListAndGlobal,
                            allowAssetManagement      : allowAssetManagement,
                            isCategoryGlobal          : isCategoryGlobal,
                            allRatingUnits            : allRatingUnits,
                            allusageRatingSchemes     : allusageRatingSchemes,
                            startDateRating           : startDateRating,
                            currentRatingConfiguration: currentRatingConfiguration,
                            ratingConfigurations      : product?.ratingConfigurations


                    ]
            return
        }

		chain action: 'show', params: [id: product.id, selectedCategory: product.types[0]]
    }

	def retrieveMetaFields () {
		List entities = params['entities'].tokenize(",")
        EntityType entityType = params['entityType'] ? EntityType.valueOf(params['entityType']) : null
        MetaFieldValueWS[] availableFieldValues = null
        ItemTypeDTO category = params.int('categoryId') ? ItemTypeDTO.get(params.int('categoryId')) : null

		def availableFields
        List<Integer> entityIds = entities.collect { Integer.parseInt(it) }
        availableFields = MetaFieldBL.getMetaFields(entityIds, entityType)

        if(category){
            availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        }

		render template : '/metaFields/editMetaFields',
			   model : [availableFields: availableFields, fieldValues: availableFieldValues]
	}

	def retrieveAllMetaFields (){
        EntityType entityType = params['entityType'] ? EntityType.valueOf(params['entityType']) : null
        MetaFieldValueWS[] availableFieldValues = null
        ItemTypeDTO category = params.int('categoryId') ? ItemTypeDTO.get(params.int('categoryId')) : null

		def availableFields  = MetaFieldBL.getMetaFields(retrieveCompaniesIds(), entityType)

        if(category){
            availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        }

		render template : '/metaFields/editMetaFields',
				model : [availableFields: availableFields, fieldValues: availableFieldValues]
	}

	def getAvailableMetaFields () {
		render template : '/metaFields/editMetaFields',
				model : [availableFields: null, fieldValues: null]
	}

    def bindProduct(product, oldProduct, params, isRoot) {
		bindData(product, params, 'product')

		// set new product's map is equal to old one, so in case of child values are preserved
		if (oldProduct != null) {
			product.metaFieldsMap = oldProduct.metaFieldsMap
    	}

        bindMetaFields(product, params, isRoot);

        //bind dependencies
        def dependencies = []
        params.each { key, value ->
            if (key.startsWith("dependency.")) {
                String[] tokens = key.substring(key.indexOf('.')+1).split(":")
                dependencies << new ItemDependencyDTOEx(type: tokens[0]=='Types'?ItemDependencyType.ITEM_TYPE : ItemDependencyType.ITEM,
                        dependentId: new Integer(tokens[3]),  dependentDescription: params[(key)],
                        minimum: (tokens[1].length() > 0 && !tokens[1].equals("null") ? new Integer(tokens[1]) : 0),
                        maximum: (tokens[2].length() > 0 && !tokens[2].equals("null") ? new Integer(tokens[2]) : null) )
            }
        }
        product.dependencies = dependencies as ItemDependencyDTOEx[]
        // if a non-numeric value is entered for product's percentage, standardPartnerPercentage and masterPercentage

        if(params?.product?.standardPartnerPercentageAsDecimal && !product?.standardPartnerPercentage){
            product.standardPartnerPercentage = params?.product?.standardPartnerPercentageAsDecimal
        }

        if(params?.product?.masterPartnerPercentageAsDecimal && !product?.masterPartnerPercentage){
            product.masterPartnerPercentage = params?.product?.masterPartnerPercentageAsDecimal
        }

        // bind parameters with odd types (integer booleans, string integers  etc.)
        product.hasDecimals = params.product.hasDecimals ? 1 : 0
        product.assetManagementEnabled = params.product.assetManagementEnabled ? 1 : 0

        // default price model if not a percentage item
        // only bind the price being shown in the UI, other date/prices will need to be added AFTER the product
        // has been created by using AJAX calls ("+ Add Date", "Save Changes", "Delete" buttons in UI).
        def price = PlanHelper.bindPriceModel(params)
        def startDate = validateStartDate(params)
        def entityId = session['company_id']
        def startDateRating=validateStartDateRating(params);


        if (oldProduct) {
            def dto = ItemDTO.get(oldProduct.id)

            def prices
            if (params['priceModelCompanyId']?.isNumber()) {
                product.priceModelCompanyId = params['priceModelCompanyId'] as Integer
                prices = dto.getDefaultPricesByCompany(product.priceModelCompanyId)
            
            } else {
                prices = dto.getGlobalDefaultPrices()
            }
            def defaultPrices = PriceModelBL.getWS((SortedMap<Date, PriceModelDTO>) prices)

            if (!defaultPrices.isEmpty()) {
                product.defaultPrices = defaultPrices
            } else {
                product.defaultPrices = new TreeMap<Date, PriceModelWS>()
            }
            if (!isRoot && priceIsChanged(prices, price, startDate)) {
                //If the child company create a new price you have to create all the
                //history for the child company and create a new price model for the new
                //price linked to the child company
                product.priceModelCompanyId = entityId
                price.id = null
                product.defaultPrices.values().each {
                    it.id = null
                }
            }
            def ratingConfigurations = RatingConfigurationBL.convertMapDTOToWS(dto.getRatingConfigurations());

            product.ratingConfigurations = ratingConfigurations.isEmpty() ? new TreeMap<Date, RatingConfigurationWS>() :(SortedMap<Date, RatingConfigurationWS>) ratingConfigurations;

        }

        if (startDate) {
            product.defaultPrices.put(startDate, price)
        }
        bindRatingConfiguration(product,params,startDateRating);
    }

    def bindRatingConfiguration(def product,def params,def startDateRating){

        Integer index = 0
        if (startDateRating && (params?.ratingUnitId || params?.ratingSchemeId || params?.pricingUnit)) {
            List<InternationalDescriptionWS> pricingDescriptions = new ArrayList<>()
            if (params?.pricingUnit) {
                while (params.pricingUnit.containsKey(Integer.toString(index))) {
                    def pricingFields = params.pricingUnit.get(Integer.toString(index))
                    InternationalDescriptionWS ws = new InternationalDescriptionWS("pricing_unit",
                            pricingFields.languageId ? Integer.parseInt(pricingFields.languageId): 1,
                            pricingFields.content)

                    ws.setDeleted(pricingFields.deleted ? Boolean.parseBoolean(pricingFields.deleted) : false)
                    pricingDescriptions.add(ws)
                    index++
                }
            }

            RatingConfigurationWS ratingConfigurationWS = RatingConfigurationBL.getWS(
                    params.ratingUnitId ? Integer.valueOf(params.ratingUnitId) : null,
                    params.ratingSchemeId ? Integer.valueOf(params.ratingSchemeId) : null,
                    pricingDescriptions)

            if(ratingConfigurationWS!=null)
                product.ratingConfigurations.put(startDateRating,ratingConfigurationWS)

        } else if (startDateRating)
            product.ratingConfigurations.remove(startDateRating);

    }



    def priceIsChanged(def pricesDateMap, def price, def startDateForPrice) {
        if (pricesDateMap?.containsKey(startDateForPrice)) {
            def oldPrice = PriceModelBL.getWS(pricesDateMap.get(startDateForPrice))
            if (oldPrice &&
                    oldPrice.getType().equals(price.getType()) &&
                    oldPrice.getRateAsDecimal()?.compareTo(price.getRateAsDecimal()) == 0 &&
                    oldPrice.getCurrencyId().equals(price.getCurrencyId())) {
                def oldPricesAttributes = oldPrice.getAttributes()
                if (oldPricesAttributes.isEmpty() && price.getAttributes().isEmpty()) {
                    return false;
                } else if (oldPricesAttributes.size() != price.getAttributes().size()) {
                    return true;
                } else {
                    for (String oldAttributKey: oldPricesAttributes.keySet()) {
                        if (!(price.getAttributes().containsKey(oldAttributKey) &&
                                price.getAttributes().get(oldAttributKey).equals(oldPricesAttributes.get(oldAttributKey)))) {
                            return true
                        }
                    }
                }
                return false
            }
        }
        return true
    }

    private def isMappingAlreadyDefined(def mappings, def otherMapping){
        for(mapping in mappings){
            if (mapping.routeId == otherMapping.routeId &&
                    mapping.routeValue == otherMapping.routeValue){
                return true
            }
        }
        return false
    }

    def retrieveCurrencies() {
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

	def retrieveChildCompanyIds() {
		def ids = new ArrayList<Integer>(0);
		for(CompanyDTO company : retrieveChildCompanies()){
			ids.add(company.getId())
		}
		return ids;
	}

	def retrieveChildCompanies() {
        List<CompanyDTO> companies = CompanyDTO.findAllByParent(CompanyDTO.get(session['company_id']))
        return companies
	}

	def retrieveCompanies() {
		def companies = retrieveChildCompanies()
		companies.add(CompanyDTO.get(session['company_id']))

		return companies
	}

    def retrieveCompaniesForNonRootCompany() {
        CompanyDTO childCompany = CompanyDTO.get(session['company_id'])
        List<CompanyDTO> companies = CompanyDTO.findAllByParent(childCompany.parent)
        companies.add(childCompany.parent)
        return companies
    }

    private List<CompanyDTO> retrieveCategoryRelatedCompanies(List<ItemTypeDTO> itemTypeDTOList){

        if(itemTypeDTOList.any {it.isGlobal()}){
             retrieveCompanies()
        }else{
            CompanyDTO.createCriteria().list(){
                createAlias("itemTypes", "itemTypes",CriteriaSpecification.LEFT_JOIN);
                'in'('itemTypes.id',itemTypeDTOList*.id)
            }.unique()
        }

    }

	def retrieveCompaniesIds() {
		def ids = new ArrayList<Integer>();
		for(CompanyDTO dto : retrieveCompanies()){
			ids.add(dto.getId())
		}
		return ids
	}

    def retrieveAvailableMetaFields(entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.PRODUCT)
    }

    List<MetaField> retrieveAvailableCategoryMetaFields(entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.PRODUCT_CATEGORY)
    }

	private def bindMetaFields(product, params, isRoot, EntityType entityType) {
		def fieldsArray
		MetaFieldValueWS[] metaFields = null
		List<MetaFieldValueWS> values = new ArrayList<MetaFieldValueWS>()
		if(isRoot) {
			for(Integer entityId : retrieveCompaniesIds()) {
                if(entityType && (entityType == EntityType.PRODUCT_CATEGORY)){
                    fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryMetaFields(entityId), params)
                }else{
                    fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(entityId), params)
                }
				metaFields = fieldsArray
				values.addAll(fieldsArray)
				product.metaFieldsMap.put(entityId, metaFields)
			}
		} else {
            if(entityType && (entityType == EntityType.PRODUCT_CATEGORY)){
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryMetaFields(session["company_id"]), params)
            }else{
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(session["company_id"]), params)
            }
			metaFields = fieldsArray
			values.addAll(fieldsArray)
			product.metaFieldsMap.put(session["company_id"], metaFields)
		}
		product.metaFields = values
    }

    private def bindMetaFields(product, params, isRoot) {
        bindMetaFields(product, params, isRoot, null)
    }

    private def validateStartDate(params) {
        def startDate
        try {
            startDate = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate()
        } catch(IllegalArgumentException e) {
            log.error e
            String [] errors = [
                    "ProductWS,startdate,product.invalid.startdate.format"
            ]
            throw new SessionInternalError("product.invalid.startdate.format", errors)
        }
        return startDate
    }

    private def validateStartDateRating(params) {
        def startDateRating
        try {
            startDateRating = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDateRating).toDate()
        } catch (IllegalArgumentException e) {
            log.error e
            String[] errors = [
                    "ProductWS,startDateRating,product.invalid.startdate.rating.format"
            ]
            throw new SessionInternalError("product.invalid.startdate.rating.format", errors)
        }
        return startDateRating
    }


    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields(session["company_id"])) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

    /**
     * Check if the product uses a price model that requires
     * extra initialization. If yes than initialize the required data.
     */
    private def productPriceModelData(def product) {
        def priceModelData = []
        if (product) {
            product.getDefaultPrices().find { date, priceModel ->
                if (priceModel?.type == PriceModelStrategy.POOLED.toString()) {
                    priceModelData = initPriceModelData(priceModel)
                    return true //break out of find {}
                } else if (priceModel?.type == PriceModelStrategy.ITEM_SELECTOR.toString()
                        || priceModel?.type == PriceModelStrategy.ITEM_PERCENTAGE_SELECTOR.toString()){
                    priceModelData = initPriceModelData(priceModel)
                    return true //break out of find {}
                }
                return false //break out of find {}
            }
        }
        return priceModelData
    }

    /**
     * Some Price model require special data in attributes.
     * Propagate it in priceModelData map.
     * PooledItems has been changed to this map
     */
    def initPriceModelData(def priceModel) {
        params.filterBy = params?.filterBy ?: ""
        def priceModelData = []
        if (priceModel?.type == PriceModelStrategy.POOLED.toString()) {
            priceModelData = productService.getFilteredProducts(CompanyDTO.get(session['company_id']),params, null, true, true)
        } else if (priceModel?.type == PriceModelStrategy.ITEM_SELECTOR.toString()
                || priceModel?.type == PriceModelStrategy.ITEM_PERCENTAGE_SELECTOR.toString()) {
            priceModelData = getProductCategories(false, null)
        }
        return priceModelData
    }

    @Secured(["PRODUCT_43"])
    def history (){
        def product = ItemDTO.get(params.int('id'))
        securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global)
        def currentProduct = auditBL.getColumnValues(product)
        def productVersions = auditBL.get(ItemDTO.class, product.getAuditKey(product.id), versions.max)

        def price = product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']))
        def currentPrice = auditBL.getColumnValues(price)
        def priceVersions = auditBL.get(PriceModelDTO.class, price.getAuditKey(price.id), versions.max)

        def records = [
                [ name: 'product', id: product.id, current: currentProduct, versions: productVersions ],
                [ name: 'price', id: price.id, current:  currentPrice, versions: priceVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: product.id]
    }

    def restore (){
        def chainAction = 'history'

        switch (params.record) {
            case "product":
                def product = ItemDTO.get(params.int('id'));
                securityValidator.validateCompanyHierarchy(product?.entities*.id, product?.entity?.id, product?.global)
                auditBL.restore(product, product.id, params.long('timestamp'))
                break;

            case "price":
                def price = PriceModelDTO.get(params.int('id'));
                auditBL.restore(price, price.id, params.long('timestamp'))
                break;

            case "asset":
                def asset = AssetDTO.get(params.int('id'));
                securityValidator.validateCompany(asset?.entity?.id, Validator.Type.EDIT)
                auditBL.restore(asset, asset.id, params.long('timestamp'))
                chainAction = 'assetHistory'
                break;
        }

        chain action: chainAction, params: [ id: params.historyid ]
    }

    @Secured(["PRODUCT_CATEGORY_STATUS_AND_ASSETS_131"])
    def assetHistory (){
        def asset = AssetDTO.get(params.int('id'))

		ItemDTO itemDTO = ItemDTO.get(params.prodId)
		securityValidator.validateCompanyHierarchy(itemDTO?.entities*.id, itemDTO?.entity?.id, itemDTO?.global)

        def currentAsset = auditBL.getColumnValues(asset)
        def assetVersions = auditBL.get(AssetDTO.class, asset.getAuditKey(asset.id), versions.max)

        def records = [
            [ name: 'asset', id: asset.id, current: currentAsset, versions: assetVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: asset.id ]
    }


	/**
	 * Some Price model required special data in attrbutes.
	 * Propagate it in priceModelData map.
	 * pooledItems has been changed to this map
	 */
	def setPriceModelData(def priceModel){

		def priceModelData= []
		// def pooledItems = []
		 if (priceModel?.type==PriceModelStrategy.POOLED.toString()) {
			 priceModelData = productService.getFilteredProducts(CompanyDTO.get(session['company_id']),params, null, true, true)
		 }else if (priceModel?.type==PriceModelStrategy.ITEM_SELECTOR.toString()
					 ||priceModel?.type==PriceModelStrategy.ITEM_PERCENTAGE_SELECTOR.toString()) {
			 priceModelData = getProductCategories(false, null)
		 }
		 return priceModelData.sort {it?.description}

	}

	/**
	 * This call returns meta fields according to an entity
	 * @param product
	 * @return
	 */
	def getMetaFields (product) {
		def isRoot = new CompanyDAS().isRoot(session['company_id'])
		def availableFields = new HashSet<MetaField>();
		if(!product || !product?.id || product?.id == 0) {
			availableFields.addAll(retrieveAvailableMetaFields(session['company_id']))
		} else {
			if(product.global) { //TODO Global Meta Fields only?
				for(Integer entityId : retrieveCompaniesIds()) {
					availableFields.addAll(retrieveAvailableMetaFields(entityId))
				}
			} else if (isRoot) { //TODO Root specific product meta fields copied to child entities?
				if(product?.entityId) {
					availableFields.addAll(retrieveAvailableMetaFields(product.entityId))
				} else {
					for(Integer entityId : product.entities) {
						availableFields.addAll(retrieveAvailableMetaFields(entityId))
					}
                }
			} else  {
				availableFields.addAll(retrieveAvailableMetaFields(session['company_id']))
			}
		}

		return availableFields;
	}

	def copyPrices(product) {
		def filterPrice = new ItemDTOEx()

		for(Map.Entry<Date, PriceModelWS> entry : product?.defaultPrices?.entrySet()) {
			def model = entry.getValue()
			List<PriceModelWS> wsList = new LinkedList<PriceModelWS>()
			while(model) {
				PriceModelWS ws = new PriceModelWS()
				ws.type = model.type
				ws.id = model.id
				ws.attributes = model.attributes
				ws.currencyId = model.currencyId
				ws.entityId = model.entityId
				ws.rate = model.rate
				ws.next = model.next
				wsList.add(ws)

				model = model.next
			}
			def init = new ItemBL().convertToWSChain(wsList)

			filterPrice.addDefaultPrice(entry.getKey(), init)
		}

		return filterPrice
	}

	def mergePrices(product, price, startDate) {
		if(!new CompanyDAS().isRoot(session['company_id'] as Integer)) {
			if(product.defaultPrices.containsKey(startDate)) {
				// remove deleted models from product and add new ones

				ItemBL itemBl = new ItemBL()
				// find what models been deleted
				def defaultPrices = product?.defaultPrices?.get(startDate)
				Map<Integer, PriceModelWS> priceMap = new HashMap<Integer, PriceModelWS>()

				def model = defaultPrices
				while(model) {
					priceMap.put(model.id,model)
					model = model.next
				}

				model = price
				while(model) {
					if(model.id != null) {
						priceMap.put(model.id,model)
					}
					model = model.next
				}

				// find what models been deleted
				def filteredPriceProduct = copyPrices(product)
				itemBl.filterPricesByCompany(filteredPriceProduct, session['company_id'] as Integer)

				def priceIds = new ArrayList<Integer>()
				def filteredIds = new ArrayList<Integer>()
				def deleted = new ArrayList<Integer>()

				def oldModel = product.defaultPrices.get(startDate)
				def filteredModel = filteredPriceProduct?.defaultPrices?.get(startDate)

				model = price
				while(model) {
					priceIds.add(model.id)
					model = model.next
				}

				model = filteredModel
				while(model) {
					filteredIds.add(model.id)
					model = model.next
				}

				//figure out what models are deleted
				for(Integer identifier : filteredIds) {
					if(!priceIds.contains(identifier)) {
						deleted.add(identifier)
					}
				}

				List<PriceModelWS> prepared = new ArrayList<PriceModelWS>()

				// keep only those models that are not deleted
				model = oldModel
				while(model) {
					if(!deleted.contains(model.id)) {
						prepared.add(priceMap.get(model.id))
					}
					model = model.next
				}

				// add new models at the end of the model list
				model = price
				while(model) {
					if(null == model.id) {
						prepared.add(model)
					}
					model = model.next
				}

				// add new models
				price = itemBl.convertToWSChain(prepared)

			}
		}
		return price
	}

	def filterPrices(product) {
		def filtered
		//filter prices by company if it aint root
		if(!new CompanyDAS().isRoot(session['company_id'] as Integer)) {
			def filteredPriceProduct = copyPrices(product)
			new ItemBL().filterPricesByCompany(filteredPriceProduct, session['company_id'] as Integer)
			filtered = filteredPriceProduct
		} else {
			filtered = product
		}
		return filtered
	}

    private def boolean findCycleInDependenciesTree(Integer targetProductId, List dependencies) {
        if (!dependencies) return false
        if (dependencies.contains(targetProductId)) {
            return true
        }

        for (ItemDTO item : ItemDTO.getAll(dependencies)) {
            if (findCycleInDependenciesTree(targetProductId, Arrays.asList(item.getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM)))) {
                return true
            }
        }
        return false
    }

    def validateReservationDuration(String assetReservation, def product) {
        if (product.assetManagementEnabled != 0) {
            if (assetReservation && !assetReservation.isInteger()) {
                product.reservationDuration = 0
                String[] errors = ["ProductWS,reservationDuration,validation.error.reservation.duration.not.integer"]
                throw new SessionInternalError("validation.error.reservation.duration.not.integer", errors)
            }
        }
    }

    def getRatingUnitsForEntity () {
      def ratingUnits = []
      try {
        ratingUnits = webServicesSession.getAllRatingUnits();

      } catch (SessionInternalError e) {
          viewUtils.resolveException(flash, session.locale, e);
      } catch (Exception e) {
          flash.error = 'product.category.delete.error'
          flash.args = [ params.id as String ]
      }
      return ratingUnits
    }

    def getUsageRatingSchemes () {
        def usageRatingSchemes = []
        try {
            usageRatingSchemes = webServicesSession.findAllUsageRatingSchemes()

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            flash.error = 'product.category.delete.error'
            flash.args = [ params.id as String ]
        }
        return usageRatingSchemes
    }


    def saveRatingDate() {
        def product = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null

        def startDateRating = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDateRating).toDate()

        def ratingConfigurations = null

        if (params.int('product.id')) {
            def dto = ItemDTO.get(params.int('product.id'))

            ratingConfigurations = RatingConfigurationBL.convertMapDTOToWS(dto.getRatingConfigurations());
        }

        if (ratingConfigurations == null || ratingConfigurations.isEmpty()) {
            ratingConfigurations = new TreeMap<Date, RatingConfigurationWS>()
        }

        product.ratingConfigurations = ratingConfigurations

        bindRatingConfiguration(product,params,startDateRating)

        try {
            if (SpringSecurityUtils.ifAllGranted("PRODUCT_41")) {
                log.debug("saving changes to product ${product.id}")

                webServicesSession.updateItem(product)

            } else {
                flash.message = 'product.update.access.denied'
                flash.args = [product.id]
            }

        } catch (SessionInternalError ex) {
            log.error("Error is: " + ex)
            viewUtils.resolveException(flash, session.locale, ex)
        }

        product = webServicesSession.getItem(params.int('product.id'), session['user_id'], null)
        render template: '/product/ratingConfiguration',
                model: [ startDateRating: startDateRating,allRatingConfig:product?.ratingConfigurations,
                         product: product,currentRatingConfig:product?.ratingConfigurations&&startDateRating?product.ratingConfigurations.get(startDateRating):null,allRatingUnits:getRatingUnitsForEntity(),allusageRatingSchemes:getUsageRatingSchemes()]
    }


    def addRatingDate() {

        def ratingConfigurations = null

        if (params.int('product.id')) {
            def dto = ItemDTO.get(params.int('product.id'))
            ratingConfigurations = RatingConfigurationBL.convertMapDTOToWS(dto.getRatingConfigurations());
        }

        def product = new ItemDTOEx()
        product.id = params.int('product.id')

        render template: '/product/ratingConfiguration',
                model: [allRatingConfig:ratingConfigurations,
                         product: product,currentRatingConfig:new RatingConfigurationWS(),allRatingUnits:getRatingUnitsForEntity(),allusageRatingSchemes:getUsageRatingSchemes() ]
    }


    def editRatingDate() {
        
        def ratingConfigurations = null

        if (params.int('product.id')) {
            def dto = ItemDTO.get(params.int('product.id'))

            ratingConfigurations = RatingConfigurationBL.convertMapDTOToWS(dto.getRatingConfigurations());
        }

        def product = new ItemDTOEx()
        product.id = params.int('product.id')

        def startDateRating = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDateRating).toDate()

        render template: '/product/ratingConfiguration',
                model: [startDateRating: startDateRating,allRatingConfig:ratingConfigurations,
                        product: product,allRatingUnits:getRatingUnitsForEntity(),allusageRatingSchemes:getUsageRatingSchemes()]
    }


    def removeRatingDate() {
        def product = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null
        def startDateRating = DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDateRating).toDate()

        def ratingConfigurations = null;

        if (params.int('product.id')) {
            def dto = ItemDTO.get(params.int('product.id'))
            ratingConfigurations = RatingConfigurationBL.convertMapDTOToWS(dto.getRatingConfigurations());
        }


        ratingConfigurations.remove(startDateRating)
        product?.ratingConfigurations?.clear()
        product?.ratingConfigurations = ratingConfigurations

        if (SpringSecurityUtils.ifAllGranted("PRODUCT_41")) {
            log.debug("saving changes to product ${product.id}")

            webServicesSession.updateItem(product)

            flash.message = 'product.updated'
            flash.args = [product.id]

        } else {
            flash.message = 'product.update.access.denied'
            flash.args = [product.id]
        }


        render template: '/product/ratingConfiguration',
                model: [allRatingConfig:ratingConfigurations,
                        product: product,startDateRating: (ratingConfigurations?.size() > 0 ? null : CommonConstants.EPOCH_DATE),allRatingUnits:getRatingUnitsForEntity(),allusageRatingSchemes:getUsageRatingSchemes() ]

    }

}
