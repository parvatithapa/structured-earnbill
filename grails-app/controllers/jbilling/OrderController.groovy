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
import grails.gorm.PagedResultList
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Holders

import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.IMethodTransactionalWrapper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.customer.CustomerBL
import com.sapienter.jbilling.server.filter.FilterFactory
import com.sapienter.jbilling.server.filter.JbillingFilterConverter
import com.sapienter.jbilling.server.invoice.InvoiceBL
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.order.OrderBL
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS
import com.sapienter.jbilling.server.order.OrderChangeWS
import com.sapienter.jbilling.server.order.OrderLineItemizedUsageWS
import com.sapienter.jbilling.server.order.OrderStatusFlag
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.order.db.OrderExportableWrapper
import com.sapienter.jbilling.server.order.db.OrderLineDTO
import com.sapienter.jbilling.server.order.db.OrderProcessDAS
import com.sapienter.jbilling.server.order.db.OrderStatusDAS
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.usagePool.SwapPlanHistoryWS
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.CsvFileGeneratorUtil
import com.sapienter.jbilling.server.util.csv.DynamicExport
import com.sapienter.jbilling.server.util.csv.Exporter
/**
 *
 * @author vikas bodani
 * @since  20-Jan-2011
 *
 */

@Secured(["MENU_92"])
class OrderController {
    static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
    static versions = [ max: 25 ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
    ['customer': 'user.userName',
        'company': 'company.description',
        'orderid': 'id',
        'date': 'createDate',
        'amount': 'total']

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def subAccountService
    def springSecurityService
    def mediationService
    SecurityValidator securityValidator

    def auditBL

    def index () {
        list()
    }

    def getFilteredOrders(filters, params, ids) {
        List<OrderWS> orderWSList = filterOrders(filters, params, ids)
        List<OrderDTO> orderDTOList = new com.sapienter.jbilling.server.filter.PagedResultList()
        for (OrderWS ws : orderWSList) {
            orderDTOList.add(new OrderBL(ws.getId()).getEntity())
        }

        orderDTOList.totalCount = orderWSList.totalCount;

        return orderDTOList;
    }

    def getFilteredOrderIds(filters, params, ids) {
        return getOrderIds(filters, params, ids)
    }


    private def createFilters(filters, params, ids) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        def partnerDtos = PartnerDTO.createCriteria().list(){ eq('baseUser.id', session['user_id']) }
        log.debug "### partner:$partnerDtos"

        def customersForUser = new ArrayList()
        if( partnerDtos.size > 0 ){
            customersForUser =  CustomerDTO.createCriteria()
                    .list(){
                        createAlias("partners", "partners")
                        'in'('partners.id', partnerDtos*.id)
                    }
        }

        if (ids) filters.add(new Filter(constraintType: FilterConstraint.IN, field: 'id', listValue: ids))
        if (params.company) filters.add(new Filter(constraintType: FilterConstraint.LIKE, field: 'company.description', stringValue: params.company))
        if (params.orderid) filters.add(new Filter(constraintType: FilterConstraint.EQ, field: 'id', integerValue: params.orderid))
        if (params.customer) filters.add(new Filter(constraintType: FilterConstraint.LIKE, field: 'user.userName', stringValue: params.customer))
        if (params.processId) {
            List<Integer> orderProcess = new OrderProcessDAS().findByBillingProcess(params.processId as Integer)
            if(!orderProcess.isEmpty()) {
                filters.add(new Filter(constraintType: FilterConstraint.IN, field: 'orderProcesses.id', listValue: orderProcess))
            }
        }

        if (SpringSecurityUtils.ifNotGranted("ORDER_28")) {
            UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

            if (loggedInUser.getPartner()) {
                // #7043 - Agents && Commissions - A logged in Partner should only see its orders and the ones of his children.
                // A child Partner should only see its orders.
                def partnerIds = []
                if (loggedInUser.getPartner() != null) {
                    partnerIds << loggedInUser.partner.user.id
                    if (loggedInUser.partner.children) {
                        partnerIds += loggedInUser.partner.children.user.id
                    }
                }

                filters.add(new Filter(constraintType: FilterConstraint.IN, field: 'createdBy.id', listValue: partnerIds))
            } else if (loggedInUser.getCustomer()) {
                filters.add(new Filter(constraintType: FilterConstraint.EQ, field: 'u.id', integerValue: session['user_id'] as Integer))
            } else if (SpringSecurityUtils.ifAnyGranted("ORDER_29")) {
                // restrict query to sub-account user-ids
                filters.add(new Filter(constraintType: FilterConstraint.IN, field: 'createdBy.id', listValue: subAccountService.subAccountUserIds))
            } else {
                // limit list to only this customer
                filters.add(
                        new Filter(constraintType: FilterConstraint.OR, field: 'createdBy.id', filters: [
                            new Filter(constraintType: FilterConstraint.IN, field: 'user.id', listValue: customersForUser.baseUser.userId),
                            new Filter(constraintType: FilterConstraint.EQ, field: 'user.id', integerValue: session['user_id'] as Integer)
                        ]
                        ));
            }
        }

        filters.each {
            filter -> if (filter.field.equals("contact.fields") && !filter.fieldKeyData) {
                filter.fieldKeyData = params['contact.fields.fieldKeyData']
            }
        }
        log.debug "### customersForUser:$customersForUser"
        return filters
    }

    private filterOrders(filters, params, ids) {
        filters = createFilters(filters, params, ids);
        return webServicesSession.findOrdersByFilters(params.offset as int,
                params.max as int,
                params.sort as String,
                params.order.equals("null") ? "asc" : params.order as String,
                JbillingFilterConverter.convert(filters))
    }

    private List<Integer> getOrderIds(filters, params, ids) {
        filters = createFilters(filters, params, ids);
        List<Integer> orderIds = new ArrayList<>()
        IMethodTransactionalWrapper txWrapper = Holders.getApplicationContext().getBean(IMethodTransactionalWrapper.class)
        txWrapper.execute({->
            orderIds.addAll(FilterFactory.orderFilterDAS().findIdByFilters(params.offset as int,
                    params.max as int,
                    params.sort as String,
                    params.order.equals("null") ? "asc" : params.order as String,
                    session["company_id"] as Integer,
                    JbillingFilterConverter.convert(filters)))

        })
        return orderIds
    }

    List<OrderExportableWrapper> getOrders(def orderIds,  def dynamicExport) {
        List<OrderExportableWrapper> orders = new ArrayList<>()
        for(Integer orderId: orderIds) {
            orders.add(new OrderExportableWrapper(orderId, dynamicExport))
        }
        return orders
    }


    def childrenMap(orders) {
        if (!orders) return [:]
        def queryResults = OrderDTO.executeQuery(
                "select ord.parentOrder.id as id, count(*) as childCount from ${OrderDTO.class.getSimpleName()} ord " +
                " where ord.parentOrder.id in (:orderIds) and ord.deleted = 0 group by ord.parentOrder.id ",
                [orderIds: orders.collect { it.id }]
                )
        def results = [:];
        queryResults.each({ record -> results.put(record[0], record[1]) })
        PagedResultList
        return results;
    }

    def findOrders () {
        def filters = new ArrayList(filterService.getFilters(FilterType.ORDER, params))

        def orderIds = parameterIds

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def orders = getFilteredOrders(filters, params, orderIds)

        try {
            render getOrdersJsonData(orders, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Orders to JSon
     */
    private def Object getOrdersJsonData(orders, GrailsParameterMap params) {
        def jsonCells = orders
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(orders.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: orders.totalCount, total: numberOfPages]

        jsonData
    }

    def list () {
        def filters = new ArrayList(filterService.getFilters(FilterType.ORDER, params))

        def orderIds = parameterIds

        def selected = null
        def userAllowed = true
        if (params.int('id') > 0) {
            def ordersTmp = getFilteredOrders([], [:], [params.id as Integer])
            if (ordersTmp.size() > 0) {
                selected = new OrderBL(ordersTmp[0]).getWS(session['language_id'] as Integer)
                if (params.boolean('filterEntity')) {
                    def filter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ, field: 'id', template: 'id', visible: true, integerValue: params.id)
                    filterService.setFilter(FilterType.ORDER, filter)
                    filters = new ArrayList(filterService.getFilters(FilterType.ORDER, params))
                }
            }
        }

        if(selected) {
            if(SpringSecurityUtils.ifNotGranted("ORDER_24")) {
                selected = null
                userAllowed = false
            } else {
                securityValidator.validateUserAndCompany(selected, Validator.Type.VIEW)
            }
        }


        OrderChangeWS[] orderChanges = webServicesSession.getOrderChanges(selected?.id)
        boolean futureOrderChanges = hasFutureOrderChanges(orderChanges);
        Map<String,Map<Date, List<AssetWS>>> assetsMap = futureOrderChanges ? getAssetMap(orderChanges) : new HashMap<String,List<AssetWS>>();
        boolean futureOrderAssetsChanges = (assetsMap.size() > 0) ? true : false;

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'ordersTemplate', model: [currencies: retrieveCurrencies(), filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
            }else {
                render view: 'list', model: [currencies: retrieveCurrencies(), filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
                    orderChanges: orderChanges, assetsMap: assetsMap,
                    futureOrderChanges: futureOrderChanges, futureOrderAssetsChanges: futureOrderAssetsChanges]
            }
            return
        }

        def orders = getFilteredOrders(filters, params, orderIds)
        def user = selected ? webServicesSession.getUserWS(selected.userId) : null
        def isCurrentCompanyOwning = selected ? user.entityId?.equals(session['company_id']) ? true : false : false

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.int("id") > 0 && !selected && userAllowed){
            flash.error = message(code: 'flash.order.not.found')
        }

        if (params.applyFilter || params.partial) {
            render template: 'ordersTemplate', model: [ orders: orders, order: selected, user: user, currencies: retrieveCurrencies(),
                filters: filters, ids: params.ids, children: childrenMap(orders),
                isCurrentCompanyOwning: isCurrentCompanyOwning, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]

        } else {

            PeriodUnitDTO periodUnit = selected?.dueDateUnitId ? PeriodUnitDTO.get(selected.dueDateUnitId) : null
            render view: 'list', model: [ orders: orders, order: selected, user: user, currencies: retrieveCurrencies(),
                filters: filters, ids: params.ids, periodUnit: periodUnit, children: childrenMap(orders),
                isCurrentCompanyOwning: isCurrentCompanyOwning, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
                orderChanges: orderChanges, assetsMap: assetsMap,
                futureOrderChanges: futureOrderChanges, futureOrderAssetsChanges: futureOrderAssetsChanges]
        }
    }

    private def getChildren (GrailsParameterMap params, boolean withPagination) {
        def parent = OrderDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(webServicesSession.getOrder(parent.id), Validator.Type.VIEW)

        def paginationValues = [max: params.max]
        if (withPagination) {
            paginationValues.offset = params.offset
        }

        def children = OrderDTO.createCriteria().list(paginationValues) {
            and {
                createAlias('baseUserByUserId', 'u', Criteria.LEFT_JOIN)
                eq('parentOrder.id', params.int('id'))
                eq('deleted', 0)
                order("id", "desc")
                if(params.company) {
                    eq('u.company', CompanyDTO.findByDescriptionIlike('%' + params.company + '%'))
                }
                if(params.orderid) {
                    eq('id', params.int('orderid'))
                }
                if (params.customer) {
                    addToCriteria(Restrictions.ilike('user.userName', params.customer, MatchMode.ANYWHERE))
                }
            }
        }
        children
    }

    /**
     * Fetches a list of sub-orders for the given order id and renders the order list "_table.gsp" template.
     */
    def suborders (){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def parent = OrderDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(webServicesSession.getOrder(parent.id), Validator.Type.VIEW)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'ordersTemplate', model:[parent: parent]
            return
        }

        def children = getChildren(params, true)

        render template: 'ordersTemplate', model: [ orders: children, parent: parent, children: childrenMap(children),
            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
    }

    /**
     * JQGrid will call this method to get the list as JSon data
     */
    def findSuborders (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0

        def children = getChildren(params, true)

        try {
            render getOrdersJsonData(children, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def getSelectedOrder(selected, orderIds) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
        field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getFilteredOrders([idFilter], params, orderIds)
    }

    @Secured(["ORDER_24"])
    def show () {
        OrderWS order = webServicesSession.getOrder(params.int('id'))
        UserWS user = webServicesSession.getUserWS(order.getUserId())
        OrderChangeWS[] orderChanges = webServicesSession.getOrderChanges(params.int('id'))
        boolean futureOrderChanges = hasFutureOrderChanges(orderChanges);
        Map<String,Map<Date, List<AssetWS>>> assetsMap = futureOrderChanges ? getAssetMap(orderChanges) : new HashMap<String,List<AssetWS>>();
        boolean futureOrderAssetsChanges = (assetsMap.size() > 0) ? true : false;

        securityValidator.validateUserAndCompany(order, Validator.Type.VIEW)

        def isCurrentCompanyOwning = user.entityId?.equals(session['company_id']) ? true : false

        recentItemService.addRecentItem(order.id, RecentItemType.ORDER)
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, order.id)

        SwapPlanHistoryWS[] swapPlanLogs = webServicesSession.getSwapPlanHistroyByOrderId(order?.id)
        OrderLineItemizedUsageWS[] itemizedUsages = webServicesSession.getItemizedUsagesForOrder(order?.id)
        PeriodUnitDTO periodUnit = order.dueDateUnitId ? PeriodUnitDTO.get(order.dueDateUnitId) : null
        render template:'show', model: [
            order: order,
            user: user,
            currencies: retrieveCurrencies(),
            periodUnit: periodUnit,
            filterStatusId : params.int('filterStatusId'),
            singleOrder : params.boolean("singleOrder"),
            isCurrentCompanyOwning: isCurrentCompanyOwning, swapPlanLogs:swapPlanLogs,
            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),
            orderChanges: orderChanges, assetsMap: assetsMap,
            futureOrderChanges: futureOrderChanges, futureOrderAssetsChanges: futureOrderAssetsChanges,
            itemizedUsages:itemizedUsages
        ]
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for generate.
     */
    @Secured(["ORDER_25"])
    def csv () {
        try {
            def filters = new ArrayList(filterService.getFilters(FilterType.ORDER, params))
            params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
            params.order = params.sord
            params.max = CsvExporter.MAX_RESULTS

            def orderIds = parameterIds
            List<Integer> filterOrderIds = getOrderIds(filters, params, orderIds)
            if(PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0) {
                def orders  = getOrders(filterOrderIds, DynamicExport.YES)
                if (orders.size() > CsvExporter.GENERATE_CSV_LIMIT) {
                    flash.error = message(code: 'error.export.exceeds.maximum')
                    flash.args = [ CsvExporter.GENERATE_CSV_LIMIT ]
                    render "failure"
                } else {
                    CsvFileGeneratorUtil.generateCSV(com.sapienter.jbilling.client.util.Constants.ORDER_CSV, orders, session['company_id'],session['user_id']);
                    render "success"
                }
            } else {
                def orders = getOrders(filterOrderIds, DynamicExport.NO)
                renderOrderCsvFor(orders)
            }
        } catch (SessionInternalError e) {
            log.error e.getMessage()
            viewUtils.resolveException(flash, session.locale, e);
        }
    }

    /**
     * Called from the suborders table
     */
    @Secured(["ORDER_25"])
    def subordersCsv (){
        // For when the csv is exported on JQGrid
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order  = (params.sord != null ? params.sord : params.order)
        params.max = CsvExporter.MAX_RESULTS
        def orders = getChildren(params, false)
        renderCsvFor(orders)
    }

    def renderCsvFor(orders) {
        if (orders.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "orders.csv")
            Exporter<OrderDTO> exporter = CsvExporter.createExporter(OrderDTO.class);
            render text: exporter.export(orders), contentType: "text/csv"
        }
    }

    def renderOrderCsvFor(orderExportables) {
        if (orderExportables.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "orders.csv")
            Exporter<OrderExportableWrapper> exporter = CsvExporter.createExporter(OrderExportableWrapper.class);
            render text: exporter.export(orderExportables), contentType: "text/csv"
        }
    }

    /**
     * Convenience shortcut, this action shows all invoices for the given user id.
     */
    def user () {
        def filter = new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'baseUserByUserId.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.ORDER, filter)
        redirect action: 'list'
    }

    @Secured(["ORDER_23"])
    def generateInvoice () {
        log.debug "generateInvoice for order ${params.id}"

        def orderId = params.id?.toInteger()

        Integer invoiceID= null;
        try {
            invoiceID = webServicesSession.createInvoiceFromOrder(orderId, null)

        } catch (SessionInternalError e) {
            flash.error= 'order.error.generating.invoice'
            redirect action: 'list', params: [ id: params.id ]
            return
        }

        if ( null != invoiceID) {
            flash.message ='order.geninvoice.success'
            flash.args = [orderId]
            redirect controller: 'invoice', action: 'list', params: [id: invoiceID]

        } else {
            flash.error ='order.error.geninvoice.inactive'
            redirect action: 'list', params: [ id: params.id ]
        }
    }

    @Secured(["ORDER_23"])
    def applyToInvoice () {
        def userId = params.int('userId')
        def bl = new UserBL(userId)
        securityValidator.validateUserAndCompany(bl.getUserWS(), Validator.Type.EDIT)

        def invoices = getApplicableInvoices(userId)

        if (!invoices || invoices.size() == 0) {
            flash.error = 'order.error.invoices.not.found'
            flash.args = [params.userId]
            redirect (action: 'list', params: [ id: params.id ])
        }

        session.applyToInvoiceOrderId = params.int('id')
        [ invoices:invoices, currencies: retrieveCurrencies(), orderId: params.id ]
    }

    @Secured(["ORDER_23"])
    def apply () {
        def order =  new OrderDAS().find(params.int('id'))
        def statusId =  new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE,session["company_id"])
        if (!order.getOrderStatus().getId().equals(statusId)) {
            flash.error = 'order.error.status.not.active'
        }

        // invoice with meta fields
        def invoiceTemplate = new InvoiceWS()
        bindData(invoiceTemplate, params, 'invoice')

        def invoiceMetaFields = retrieveInvoiceMetaFields();
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(invoiceMetaFields, params);
        invoiceTemplate.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])

        // apply invoice to order.
        try {
            def invoice = webServicesSession.applyOrderToInvoice(order.getId(), invoiceTemplate)
            if (!invoice) {
                flash.error = 'order.error.apply.invoice'
                render view: 'applyToInvoice', model: [ invoice: invoice, invoices: getApplicableInvoices(params.int('userId')), currencies:retrieveCurrencies(), availableMetaFields: invoiceMetaFields, fieldsArray: fieldsArray ]
                return
            }

            flash.message = 'order.succcessfully.applied.to.invoice'
            flash.args = [params.id, invoice]

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

            def invoice = webServicesSession.getInvoiceWS(params.int('invoice.id'))
            def invoices = getApplicableInvoices(params.int('userId'))
            render view: 'applyToInvoice', model: [ invoice: invoice, invoices: invoices, currencies:retrieveCurrencies(), availableMetaFields: invoiceMetaFields, fieldsArray: fieldsArray ]
            return
        }

        redirect action: 'list', params: [ id: params.id ]
    }

    def getApplicableInvoices(Integer userId) {

        CustomerDTO payingUser
        Integer _userId
        UserDTO user= new UserDAS().find(userId)
        if (user.getCustomer()?.getParent()) {
            payingUser= new CustomerBL(user.getCustomer().getId()).getInvoicableParent()
            _userId=payingUser.getBaseUser().getId()
        } else {
            _userId= user.getId()
        }
        InvoiceDAS das= new InvoiceDAS()
        List invoices =  new ArrayList()
        for (Iterator it= das.findAllApplicableInvoicesByUser(_userId ).iterator(); it.hasNext();) {
            invoices.add InvoiceBL.getWS(das.find (it.next()))
        }

        log.debug "Found ${invoices.size()} for user ${_userId}"

        invoices as List
    }

    def retrieveCompanies(){
        def parentCompany = CompanyDTO.get(session['company_id'])
        def childs = CompanyDTO.findAllByParent(parentCompany)
        childs.add(parentCompany)
        return childs;
    }

    def retrieveInvoiceMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }

    def retrieveCurrencies() {
        //in this controller we need only currencies objects with inUse=true without checking rates on date
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

    def byProcess () {
        // limit by billing process
        def filters = new ArrayList(filterService.getFilters(FilterType.ORDER, params))
        def processId = params.processId
        def orders = getFilteredOrders(filters, params, null)
        log.debug("Found ${orders.size()} orders.")
        if (params.applyFilter || params.partial) {
            render template: 'ordersTemplate', model: [orders: orders, filters: filters, children: childrenMap(orders), processId: processId,
                displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        } else {
            render view: 'list', model: [orders: orders, filters: filters, children: childrenMap(orders), processId: processId,
                displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        }
    }

    def byMediation () {

        def mediationId = UUID.fromString(params.get('id'))
        def filters= new ArrayList(filterService.getFilters(FilterType.ORDER, params))
        def orders=[]
        def orderIds = mediationService.getOrdersForMediationProcess(mediationId)
        if (orderIds) {
            orders = getFilteredOrders(filters, params, orderIds)
        } else {
            orders = new ArrayList<OrderDTO>()
        }
        log.debug("Found ${orders?.size()} orders.")
        if (params.applyFilter || params.partial) {
            render template: 'ordersTemplate', model: [orders: orders, filters: filters, ids: params.ids, children: childrenMap(orders),
                mediationId: mediationId, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        } else {
            render view: 'list', model: [orders: orders, filters: filters, ids: params.ids, children: childrenMap(orders),
                mediationId:mediationId, filterAction: 'byMediation', filterId:mediationId, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        }
    }

    @Secured(["ORDER_22"])
    def deleteOrder () {
        securityValidator.validateUserAndCompany(webServicesSession.getOrder(params.int('id')), Validator.Type.EDIT, true)
        String orderIds = webServicesSession.deleteOrder(params.int('id'))

        if(orderIds.equals(OrderHierarchyValidator.ERR_NON_LEAF_ORDER_DELETE)){
            flash.error = message('code':OrderHierarchyValidator.ERR_NON_LEAF_ORDER_DELETE.split(",").last())
        }else if(orderIds.equals(OrderHierarchyValidator.PRODUCT_PARENT_DEPENDENCY_EXIST)){
            flash.error = message('code':OrderHierarchyValidator.PRODUCT_PARENT_DEPENDENCY_EXIST.split(",").last())
        }else{
            flash.message = 'order.delete.success'
            flash.args = [orderIds]
        }

        redirect action: 'list'
    }

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.ORDER);
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
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

    @Secured(["ORDER_24"])
    def history (){
        def order = OrderDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(webServicesSession.getOrder(order.id), Validator.Type.VIEW)

        def currentOrder = auditBL.getColumnValues(order)
        def orderVersions = auditBL.get(OrderDTO.class, order.getAuditKey(order.id), versions.max)
        def lines = auditBL.find(OrderLineDTO.class, getOrderLineSearchPrefix(order))

        def records = [
            [ name: 'order', id: order.id, current: currentOrder, versions: orderVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: order.id, lines: lines, linecontroller: 'order', lineaction: 'linehistory' ]
    }

    def getOrderLineSearchPrefix(order) {
        return "${order.user.company.id}-usr-${order.user.id}-ord-${order.id}-"
    }

    @Secured(["ORDER_24"])
    def linehistory (){
        def line = OrderLineDTO.get(params.int('id'))
        securityValidator.validateUserAndCompany(webServicesSession.getOrder(line?.orderDTO?.id), Validator.Type.VIEW)

        def currentLine = auditBL.getColumnValues(line)
        def lineVersions = auditBL.get(OrderLineDTO.class, line.getAuditKey(line.id), versions.max)

        def records = [
            [ name: 'line', id: line.id, current: currentLine, versions: lineVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: line.purchaseOrder.id ]
    }

    def retrieveCompanyStatuses (){
        return webServicesSession.getOrderChangeStatusesForCompany() as List
    }

    private Map<String,Map<Date, List<AssetWS>>> getAssetMap(OrderChangeWS[] orderChanges){
        Map<String,Map<Date, List<AssetWS>>> assetsMap = new HashMap<String,List<AssetWS>>();
        if(orderChanges){
            for( OrderChangeWS orderChangeWS : orderChanges){
                if(orderChangeWS.status.equalsIgnoreCase(Constants.ORDER_STATUS_PENDING)) {
                    OrderChangePlanItemWS[] orderChangePlanItems = orderChangeWS.getOrderChangePlanItems();
                    if(orderChangePlanItems){
                        for(OrderChangePlanItemWS orderChangePlanItem : orderChangePlanItems){
                            if(orderChangePlanItem.assetIds){
                                Integer[] assetIds = ArrayUtils.toObject(orderChangePlanItem.assetIds);
                                AssetWS[] assetArray = webServicesSession.findAssetsForOrderChanges(assetIds);
                                if (assetArray) {
                                    List<AssetWS> assets = Arrays.asList(assetArray);
                                    Map<Date, List<AssetWS>> asset = new HashMap<Date, List<AssetWS>>();
                                    asset.put(orderChangeWS.startDate, assets);
                                    assetsMap.put(orderChangeWS.id + "|" + orderChangeWS.description, asset);
                                }
                            }
                        }
                    }else {
                        AssetWS[] assetArray = webServicesSession.findAssetsForOrderChanges(orderChangeWS.assetIds);
                        if (assetArray) {
                            List<AssetWS> assets = Arrays.asList(assetArray);
                            Map<Date, List<AssetWS>> asset = new HashMap<Date, List<AssetWS>>();
                            asset.put(orderChangeWS.startDate, assets);
                            assetsMap.put(orderChangeWS.id + "|" + orderChangeWS.description, asset);
                        }
                    }
                }
            }
        }

        return assetsMap;
    }

    private boolean hasFutureOrderChanges(OrderChangeWS[] orderChanges){
        if(orderChanges){
            return orderChanges.any {it.status.equalsIgnoreCase(Constants.ORDER_STATUS_PENDING)}
        }
        return false;
    }
}
