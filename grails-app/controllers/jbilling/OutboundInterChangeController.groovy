package jbilling

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.integration.db.OutBoundInterchange
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL

import grails.plugin.springsecurity.annotation.Secured

import org.apache.commons.lang.StringUtils
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions

/**
 * OutBoundInterChange Controller
 *
 * @author Satyendra Soni
 * @since 17-May-2019
 */
@Secured(["MENU_1919"])
class OutboundInterChangeController {

    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [max: 25]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['outboundId' : 'id',
             'userId'     : 'userId',
             'date'       : 'createdDate',
             'requestType': 'methodName',
             'status'     : 'status'
            ]

    IWebServicesSessionBean webServicesSession
    def filterService
    def recentItemService
    def breadcrumbService
    def viewUtils

    def index() {
        list()
    }

    def getList(filters, params, boolean projection) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return OutBoundInterchange.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            if (projection) {
                projections {
                    property('id')
                }
            }
            def outboundFilters = []
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        if (filter.field == 'outboundId') {
                            outboundFilters.add(filter)
                        } else if (filter.field == 'status') {
                            addToCriteria(Restrictions.ilike("status", filter.stringValue, MatchMode.ANYWHERE))
                        } else {
                            addToCriteria(filter.getRestrictions())
                        }
                    }
                }
                if (params.outboundId) {
                    eq('id', params.int('outboundId'))
                }
                if (params.status) {
                    addToCriteria(Restrictions.ilike("status", filter.stringValue, MatchMode.ANYWHERE));
                }
            }
            // apply sorting
            SortableCriteria.buildSortNoAlias(params, delegate)
        }
    }

    /**
     * Gets a list of outbounds and renders the the list page. If the "applyFilters" parameter is given,
     * the partial "_outboundInterChange.gsp" template will be rendered instead of the complete _outboundInterChange list page.
     */
    def list() {

        def filters = filterService.getFilters(FilterType.OUTBOUNDINTERCHANGE, params)
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID)
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            if (params.applyFilter || params.partial) {
                render template: 'outboundInterChangeTemplate', model: [filters: filters]
            } else {
                render view: 'list', model: [filters: filters]
            }
            return
        }

        def selected = null
        if (params.id) {
            def paramsClone = params.clone()
            paramsClone.clear()
            paramsClone['outboundId'] = params.int('id')
            def obdTmp = getList([], paramsClone, false)
            if (obdTmp.size() > 0) {
                selected = obdTmp[0]
            }
        }
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)
        def outboundInterChange
        try {
            outboundInterChange = getList(filters, params, false)

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if (params.id?.isInteger() && selected == null) {
            flash.error = message(code: 'flash.outboundInterChange.not.found')
        }
        if (params.applyFilter || params.partial) {
            render template: 'outboundInterChangeTemplate', model: [outboundInterChange: outboundInterChange, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        } else {
            render view: 'list', model: [outboundInterChange: outboundInterChange, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        }
    }

    /**
     * Show details of the selected Outbound.
     */
    def show() {
        def outboundInterChange = OutBoundInterchange.get(params.int('id'))
        if (!outboundInterChange) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
		if(outboundInterChange?.request?.trim()) {
			def jsonString = convertStringToJsonFormat(outboundInterChange?.request);
			outboundInterChange.request = jsonString;
		}
        recentItemService.addRecentItem(params.int('id'), RecentItemType.OUTBOUNDINTERCHANGE)
        breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, params.int('id'))
        render template: 'show', model: [selected: outboundInterChange, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
    }

	/**
	 * 
     * @param request: Json request in string format
     * @return: String into Json format
     */
	private String convertStringToJsonFormat(String request) {
        JsonParser parser = new JsonParser();
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonElement el = parser.parse(request);
        return gson.toJson(el)
    }

    /**
     * Convenience shortcut, this action shows all outbounds for the given user id.
     */
    def user () {
        def filter =  new Filter(type: FilterType.OUTBOUNDINTERCHANGE, constraintType: FilterConstraint.EQ, field: 'userId', template: 'id', visible: true, integerValue: params.id)
        filterService.setFilter(FilterType.OUTBOUNDINTERCHANGE, filter)
        redirect (action: "list")
    }

    def getPreference(Integer preferenceTypeId) {
        def preferenceValue = webServicesSession.getPreference(preferenceTypeId).getValue()
        return !StringUtils.isEmpty(preferenceValue) ? Integer.valueOf(preferenceValue) : new Integer(0)
    }
}
