package jbilling

import com.sapienter.jbilling.server.filter.JbillingFilterConverter
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.item.db.AssetDAS
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.order.db.OrderLineDAS
import com.sapienter.jbilling.server.payment.db.PaymentDAS
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestStatus

import grails.plugin.springsecurity.annotation.Secured

@Secured(["MENU_900"])
class ProvisioningController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    def webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService

    def index () {
        list()
    }

    def showCommands () {
        store_params()
        checkFilters()

        session.provisioningShow = "CMD"
        def filters = filterService.getFilters(FilterType.PROVISIONING_CMD, params)
        def selected = null
        def commands = null

        try {
            commands = getFilteredCommands(filters)
        } catch (Exception ignored){
            return response.sendError(Constants.ERROR_CODE_404)
        }

        if (commands.size() > 0) {
            breadcrumbService.addBreadcrumb(controllerName, 'showCommands', null, null)
            if (commands != null ) {
				if (params.id != null) {
					selected = commands.find {it.id == params.int('id')}
					if(params.show){
						//called by clicking any particular command.
						render template: "showCommand",
                                  model: [commands: commands,
                                           filters: filters,
                                          selected: selected,
                                            typeId: selected?.getOwningEntityId()]
						return
					}
				}
            }
        }

        if (params.applyFilter || params.partial) {
            render template: "listCommands",
                      model: [commands: commands,
                               filters: filters,
                              selected: selected,
                                typeId: selected?.getOwningEntityId()]
        } else {
            render  view: "showProvisioningCommand",
                   model: [commands: commands,
                            filters: filters,
                           selected: selected,
                             typeId: selected?.getOwningEntityId()]
        }
    }

    def callCommandsList (){
        store_params()
        session.provisioningShow = "CMD"
        breadcrumbService.addBreadcrumb(controllerName, 'showCommands', null, null)
        def filters = filterService.getFilters(FilterType.PROVISIONING_CMD, params)
        def commands = getFilteredCommands(filters)

        render template: "listCommands",
                  model: [commands: commands,
                           filters: filters,
                            typeId: null]
    }

    def callRequestsList (){
        store_params()
        session.provisioningShow = "REQ"

        def filters = filterService.getFilters(FilterType.PROVISIONING_REQ, params)
        def commands = getFilteredCommands(filters);
        def selected = null

        if (commands == null || commands.size() <= 0) {
            redirect(action: "callCommandsList")
            return
        }

        def provisioningRequests = webServicesSession.getProvisioningRequests((session.provisioningSelectedId as int)) as List
        def size = provisioningRequests.size()

        provisioningRequests = provisioningRequests.subList(params.offset, (params.offset + (params.max>=provisioningRequests.size()?provisioningRequests.size():params.max)<provisioningRequests.size())?(params.offset + (params.max>=provisioningRequests.size()?provisioningRequests.size():params.max)):provisioningRequests.size())

        if (provisioningRequests.size() > 0) {
            selected = provisioningRequests.get(0)
            if (params.id != null) {
                selected = provisioningRequests.find {it.id == params.int('id')}
                render template: "showRequest",
                          model: [ filters: filters,
                                  selected: selected]
                return
            }
        }

        render template: "listRequests",
                  model: [  requests: provisioningRequests,
                             filters: filters,
                            selected: selected,
                              typeId: selected?.getOwningEntityId(),
                          totalCount: size]
    }

    def showRequests (){

        store_params()

        session.provisioningShow = "REQ"
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def filters = filterService.getFilters(FilterType.PROVISIONING_REQ, params)

        def commands = null;
        ProvisioningCommandType commandType = getParamType()
        if (commandType && session.provisioningTypeId) {
            commands = webServicesSession.getProvisioningCommands(commandType, (session.provisioningTypeId as int)) as List
        } else {
            def companyId = session['company_id'].toInteger()
            commands = companyId ? webServicesSession.getCommandsByEntityId(companyId) : null
        }

        if (commands == null || commands.size() <= 0) {
            redirect(action: "showCommands")
            return
        }
        ProvisioningCommandWS command = webServicesSession.getProvisioningCommandById(session.provisioningSelectedId as int)

        breadcrumbService.addBreadcrumb(controllerName, 'showRequests', null, null, command.getName())

        def provisioningRequests = null
        provisioningRequests = getFilteredRequests(filters) as List
        def size = provisioningRequests.size()
        if (size != null){
            provisioningRequests = provisioningRequests.subList(params.offset, params.offset + (params.max>=provisioningRequests.size()?provisioningRequests.size():params.max))
        }
        def selected = null

        if (provisioningRequests.size() > 0) {
            selected = provisioningRequests.get(0)
            if (params.id != null) {
                selected = provisioningRequests.find {it.id == params.int('id')}
                render template: "showRequest", model:[filters: filters, selected: selected]
                return
            }
        }

        if (params.applyFilter || params.partial) {
            render template: "listRequests", model: [requests: provisioningRequests, filters: filters, selected: selected, totalCount: size]
        } else {
            render view: "showProvisioningRequest", model: [requests: provisioningRequests, filters: filters, selected: selected, totalCount: size]
        }

    }

    private def getFilteredCommands(filters) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return webServicesSession.findProvisioningCommandsByFilters(params.offset as int,
                                                                    params.max as int,
                                                                    params.sort.toString(),
                                                                    params.order.toString(),
                                                                    JbillingFilterConverter.convert(filters as List))
    }

    private def getFilteredRequests(filters) {
        params.max = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def commands = null
        ProvisioningCommandType commandType = getParamType()
        if (commandType && session.provisioningTypeId) {
            commands = webServicesSession.getProvisioningCommands(commandType, (session.provisioningTypeId as int)) as List
        } else {
            def companyId = session['company_id'].toInteger()
            commands = companyId ? webServicesSession.getCommandsByEntityId(companyId) : null
        }
        def commandId = session.provisioningSelectedId as int

        filters.each { filter ->
            if (filter.value != null) {
                if(filter.field == 'provisioning.req_command_id') {
                    commandId = filter.stringValue as int
                }
            }
        }

        def requests = null
        if (commandId != null){
            requests = webServicesSession.getProvisioningRequests(commandId)
        }
        filters.each { filter ->
            if (filter.value != null) {
                if(filter.field == 'id') {
                    requests = requests.findAll { it.getId() == filter.integerValue }
                }
                else if(filter.field == 'provisioning.create_date') {
                    requests = requests.findAll { it.getCreateDate() >= filter.startDateValue && it.getCreateDate() <= filter.endDateValue }
                }
                else if(filter.field == 'provisioning.req_status') {
                    requests = requests.findAll { it.getRequestStatus() == ProvisioningRequestStatus.values()[filter.integerValue] }
                }
                else if(filter.field == 'provisioning.req_processor') {
                    requests = requests.findAll { it.getProcessor() == filter.stringValue }
                }
            }
        }
        return requests
    }

    def list (){

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        session.provisioningType = null
        session.provisioningShow == null

        try {
            showCommands()
        } catch (Exception ex) {
            ex.message
        }
    }

    private def store_params (){
        session.provisioningType = params.type ? params.type : null
        session.provisioningTypeId = params.typeId ? params.typeId : null
        session.provisioningSelectedId = params.int('selectedId') ? params.int('selectedId') : null
    }

    private ProvisioningCommandType getParamType (){
        return ProvisioningCommandType.getEnum(session.provisioningType)
    }

    private def getObjectDTO(typeId = -1) {

        def das = null

        if (session.provisioningType == "ASSET") {
            das = new AssetDAS();
        } else if (session.provisioningType == "ORDER") {
            das = new OrderDAS();
        } else if (session.provisioningType == "ORDER_LINE") {
            das = new OrderLineDAS();
        } else if (session.provisioningType == "PAYMENT") {
            das = new PaymentDAS();
        }

        def objDTO = typeId == -1 ? das.findNow(session.provisioningTypeId as int) : das.findNow(typeId)

        objDTO
    }

    private def checkFilters(){
        ProvisioningCommandType commandType = getParamType();
        if (commandType) {
            def filter = new Filter(          type: FilterType.PROVISIONING_CMD,
                                    constraintType: FilterConstraint.EQ,
                                             field: 'command_type',
                                          template: 'provisioning/type',
                                      integerValue: commandType.toInteger(),
                                           visible: true)

            filterService.setFilter(FilterType.PROVISIONING_CMD, filter)
        }

        if (params['typeIdentifier']) {
            def filter = new Filter(          type: FilterType.PROVISIONING_CMD,
                                    constraintType: FilterConstraint.EQ,
                                             field: 'type_identifier',
                                      integerValue: params['typeIdentifier'] as Integer,
                                          template: 'id',
                                           visible: true)

            filterService.setFilter(FilterType.PROVISIONING_CMD, filter, !commandType)
        }
    }
}
