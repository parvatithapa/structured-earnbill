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
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils

import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.lang3.time.DateUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.joda.time.format.DateTimeFormat

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.db.ItemDAS
import com.sapienter.jbilling.server.item.db.PlanDAS
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.mediation.movius.CDRType
import com.sapienter.jbilling.server.movius.MoviusReportGenerator
import com.sapienter.jbilling.server.report.ReportBL
import com.sapienter.jbilling.server.report.ReportExportFormat
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.report.db.ReportParameterDTO
import com.sapienter.jbilling.server.report.db.ReportTypeDTO
import com.sapienter.jbilling.server.report.db.parameter.IntegerReportParameterDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.IWebServicesSessionBean

import java.time.LocalDate
import java.time.ZoneId

import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_TYPE_ADENNET
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_BANK_USER
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_CRM_USER
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_FINANCE_TOTAL
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_INACTIVE_SUBSCRIBER_NUMBERS
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_BANK_USER_REPORT
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_CRM_USER_REPORT
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_FINANCE_TOTAL_REPORT
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_AUDIT_LOG
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_AUDIT_LOG_REPORT

/**
 * ReportController 
 *
 * @author Brian Cowdery
 * @since 07/03/11
 */
@Secured(["MENU_96"])
class ReportController {

    static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'name', order: 'asc']
    static final viewColumnsToFields =
            ['reportId': 'id']
    public static final String USER_ACTIVITY = "user_activity"
    public static final String TOP_CUSTOMERS = "top_customers"
    public static final String USER_SIGNUPS = "user_signups"
    public static final String TOTAL_INVOICED_PER_CUSTOMER = "total_invoiced_per_customer"
    public static final String TOTAL_INVOICED_PER_CUSTOMER_OVER_YEARS = "total_invoiced_per_customer_over_years"

    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService
    def adennetHelperService
    SecurityValidator securityValidator
    IWebServicesSessionBean webServicesSession


    def index () {
        list()
    }

    def getReportTypes() {
        params.max = pagination.max
        // This fixes an issue when retrieving the types from the database
        //If more than max appear, they will be left out.
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def flagAdennetReports = false
        def userRoleType = UserDTO.get(springSecurityService.principal.id).roles?.first()?.getRoleTypeId()
        if (userRoleType != Constants.TYPE_SYSTEM_ADMIN && userRoleType != Constants.TYPE_ROOT) {
            flagAdennetReports = true
        }

        return ReportTypeDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            if (flagAdennetReports) {
                eq("name", REPORT_TYPE_ADENNET)
            }
            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            SortableCriteria.sort(params, delegate)
        }
    }

    def getReports(Integer typeId) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
		def company_id = session['company_id']

        def userRoleType = UserDTO.get(springSecurityService.principal.id).roles?.first()?.getRoleTypeId()
        if (userRoleType != Constants.TYPE_SYSTEM_ADMIN && userRoleType != Constants.TYPE_ROOT) {
            params.exclude = 'platform_net_revenue'
            typeId = adennetHelperService.getReportTypeByName(REPORT_TYPE_ADENNET)
        }
        def flagBankUserReport = false
        def flagCrmUserReport = false
        def flagFinanceTotalReport = false
        def flagInactiveSubscriberNumbersReport = false
        def flagAuditLogReport = false
        if(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_BANK_USER_REPORT)) flagBankUserReport = true
        if(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_CRM_USER_REPORT)) flagCrmUserReport = true
        if(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_FINANCE_TOTAL_REPORT)) flagFinanceTotalReport = true
        if(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT)) flagInactiveSubscriberNumbersReport = true
        if(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_AUDIT_LOG_REPORT)) flagAuditLogReport = true

        return ReportDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            if(!flagBankUserReport) {
                ne("name", REPORT_BANK_USER)
            }
            if(!flagCrmUserReport) {
                ne("name", REPORT_CRM_USER)
            }
            if(!flagFinanceTotalReport) {
                ne("name", REPORT_FINANCE_TOTAL)
            }
            if(!flagInactiveSubscriberNumbersReport) {
                ne("name", REPORT_INACTIVE_SUBSCRIBER_NUMBERS)
            }
            if(!flagAuditLogReport){
                ne("name", REPORT_AUDIT_LOG)
            }

            if (typeId) {
                eq('type.id', typeId)
            }

            entities {
                eq('id', company_id)
            }
            if(params.name) {
                or{
                    addToCriteria(Restrictions.ilike("fileName",  params.name, MatchMode.ANYWHERE))
                    addToCriteria(Restrictions.ilike("name", params.name, MatchMode.ANYWHERE));
                }
            }
            if(params.exclude) {
                ne("name", params.exclude)
            }

            SortableCriteria.sort(params, delegate)
        }
    }

    def list () {
        def type = ReportTypeDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, params.int('id'), type?.getDescription(session['language_id']))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render view: 'list', model: [ selectedTypeId: type?.id ]
            return
        }

        def types = getReportTypes()
        def reports = params.id ? getReports(params.int('id')) : null

        render view: 'list', model: [ types: types, reports: reports, selectedTypeId: type?.id ]
    }

    def findTypes () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def types = getReportTypes()

        try {
            render getAsJsonData(types, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def findReports (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def type = params.int('id')
        def reports = getReports(type) // If type is null, then search for all

        try {
            render getAsJsonData(reports, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def reports () {
        def typeId = params.int('id')
        def type = ReportTypeDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, typeId, type?.getDescription(session['language_id']))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'reportsTemplate', model: [selectedTypeId: typeId]
            return
        }
        def reports = typeId ? getReports(typeId) : null
        render template: 'reportsTemplate', model: [ reports: reports, selectedTypeId: typeId ]
    }

    def allReports () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'reportsTemplate', model: []
            return
        }
        def reports = getReports(null)
        render template: 'reportsTemplate', model: [ reports: reports ]
    }

    def show () {
        ReportDTO report = ReportDTO.get(params.int('id'))

        securityValidator.validateCompanyHierarchy(report?.entities*.id);

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, report?.id, report ? message(code: report.name) : null)

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: params.template, model: [ selected: report ]

        } else {
            // render default "list" view - needed so a breadcrumb can link to a reports by id
            def typeId = report?.type?.id
            def types = getReportTypes()
            if (!report) {
                def reports = getReports(typeId)
            }
            render view: 'list', model: [ types: types, reports: reports, selected: report, selectedTypeId: typeId ]
        }
    }

    /**
     * Runs the given report using the entered report parameters. If no format is selected, the report
     * will be rendered as HTML. If an export format is selected, then the generated file will be sent
     * to the browser.
     */
    def run () {
        def report = ReportDTO.get(params.int('id'))
        def userRoleType = UserDTO.get(springSecurityService.principal.id).roles?.first()?.getRoleTypeId()
        if (report.getType().getName() != REPORT_TYPE_ADENNET && userRoleType != Constants.TYPE_SYSTEM_ADMIN && userRoleType != Constants.TYPE_ROOT) {
            render view: '/login/denied'
            return
        }
        else if (report.getType().getName() == REPORT_TYPE_ADENNET) {
            if ((report.getName() == REPORT_BANK_USER && SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_BANK_USER_REPORT)) ||
                (report.getName() == REPORT_CRM_USER && SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_CRM_USER_REPORT)) ||
                (report.getName() == REPORT_FINANCE_TOTAL && SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_FINANCE_TOTAL_REPORT)) ||
                (report.getName() == REPORT_INACTIVE_SUBSCRIBER_NUMBERS && SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT)) ||
                (report.getName() == REPORT_AUDIT_LOG && SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_AUDIT_LOG_REPORT))) {
                render view: '/login/denied'
                return
            }
        }
        if(report.getName() == REPORT_CRM_USER) {
            def loggedInUser = webServicesSession.getUserWS(session['user_id'] as Integer).userName
            def user_name = params.user_name
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS) && loggedInUser != user_name) {
                render view: '/login/denied'
                return
            }
        }

        securityValidator.validateCompanyHierarchy(report?.entities*.id);
        bindParameters(report, params)

        def runner = new ReportBL(report, session['locale'], session['company_id'], session['company_timezone'])

        if (report.name == USER_ACTIVITY) {
            runner.updateAdminRole(session['user_id'])
        }
        try {
            if (params.format) {
                // export to selected format
                def format = ReportExportFormat.valueOf(params.format)
                def export = runner.export(format);
                DownloadHelper.sendFile(response, export?.fileName, export?.contentType, export?.bytes)
            } else {
                // render as HTML
                def imageUrl = createLink(controller: 'report', action: 'images', params: [name: '']).toString()
                runner.renderHtml(response, session, imageUrl)
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def runExports () {
        def report = ReportDTO.get(params.int('id'))
        if(!params.user_id){
            flash.error = message(code: 'error.export.user.id.not.null')
            render "failure"
            return
        }
        if(!params?.user_id?.isInteger()){
            flash.error = message(code: 'error.export.user.id.invalid',args:[params.user_id])
            render "failure"
            return
        }
        if (!UserDTO.exists(params.user_id)){
            flash.error = message(code: 'error.export.user.id.not.found',args:[params.user_id])
            render "failure"
            return
        }
        if(params.event_date_end && params.event_date_start) {
            Date startDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.event_date_start).toDate()
            Date endDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.event_date_end).toDate()
            if(endDate.compareTo(startDate)<0) {
                flash.error = message(code: 'report.start.date.before.end.date.invalid')
                render "failure"
                return
            }
        }
        securityValidator.validateCompanyHierarchy(report?.entities*.id);
        bindParameters(report, params)

        ReportBL runner = new ReportBL(report, session['locale'], session['company_id'], session['company_timezone'])
        MoviusReportGenerator moviusReportGenerator = MoviusReportGenerator.of(runner)
        try {
            if (params.format) {
                // export to selected format
                def format = ReportExportFormat.valueOf(params.format)
                def cdrType = params.cdrType ? CDRType.valueOf(params.cdrType) : null
                moviusReportGenerator.exportBackgroundReport(format, cdrType, session['user_id'] as Integer);
            }
            render "success"
        } catch(SessionInternalError | RuntimeException e) {
            viewUtils.resolveException(flash, session.locale, e)
            render "failure"
        }

    }

    /**
     * Returns image data generated by the jasper report HTML rendering.
     *
     * Rendering a jasper report to HTML produces a map of images that is stored in the session. This action
     * retrieves images by name and returns the bytes to the browser. The jasper report HTML contains <code>img</code>
     * tags that look to this action as their source.
     */
    def images () {
        Map images = session[ReportBL.SESSION_IMAGE_MAP]
        response.outputStream << images.get(params.name)
    }

    def bindParameters(report, params) {
        params.each { name, value ->
            ReportParameterDTO<?> parameter = report.getParameter(name)
            if (parameter) {
                //If parameter is an instance of IntegerReportParameterDTO and value passed is greater than range of Integer or is a string,
                //when it tries to convert parameter to IntegerReportParameterDTO then there is an exception of Integer conversion.
                // So value is set to null. To overcome the issue we tried parsing it at controller level and set value to 0 in exception scenario.
                //Incase invalid integer value the exception is thrown. If All is selected in report parameter, it will skip checking for integer type.
                if(parameter instanceof IntegerReportParameterDTO && !value?.isEmpty() ){
                    try{
                        Integer.parseInt(value)
                    }catch (NumberFormatException e){
                        throw new SessionInternalError(value+" is not a valid integer");
                    }
                }
                bindData(parameter, ['value': value])
            }
        }

		try {
			report.childEntities = new ArrayList<Integer>()
			// bind childs to list
			params.list('childs').each { child ->
				report.childEntities.add(Integer.parseInt(child))
			}
		} catch(Exception e) {
			//string is null,
		}
    }

    def findPlanByProduct(){
        Integer itemId=params.int("item_id")
        List<PlanDTO> plans =new ArrayList<>();
        PlanDAS planDAS=new PlanDAS()
        if(itemId==0){
            plans=planDAS.findAllActive(session['company_id'])
        }else{

            plans = planDAS.findByAffectedItem(itemId);
        }
        render g.select(from: plans, optionKey: 'id', optionValue: {it.item.internalNumber +'(' + it.id+')'}, name: 'plan_id', id:'planSelect', noSelection:['':'All'])
    }

    def checkData(){
        def report = ReportDTO.get(params.int('id'))
        def map = [:]

        securityValidator.validateCompanyHierarchy(report?.entities*.id);

        try {
            bindParameters(report, params)
        }catch(SessionInternalError e){
            map.error = e.getMessage()
            render(status: HttpStatus.SC_OK, text: map as JSON)
            return
        }
        if(report.name == 'unearned_revenue_summary' || report.name == 'unbilled_revenue_summary'){
            if(!params.end_date && !params.start_date) {
                map.error = message(code: 'report.start.and.end.dates.are.required')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
            if(!params.start_date){
                map.error = message(code: 'report.start.date.is.required')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
            if(!params.end_date){
                map.error = message(code: 'report.end.date.is.required')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
        }
        if(params.end_date && params.start_date) {
            Date startDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.start_date).toDate()
            Date endDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.end_date).toDate()
            LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if(endDate.compareTo(startDate)<0) {
                map.error = message(code: 'report.start.date.before.end.date.invalid')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
            if(report.name == 'unearned_unbilled_detail_report' &&
                endDate.compareTo(DateUtils.addDays(startDate, 30)) > 0){
                map.error = message(code: 'report.start.date.end.date.period.invalid')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }

            if(report.name == REPORT_AUDIT_LOG &&
                    currentDate.minusMonths(3).isAfter(startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            ){
                map.error = message(code: 'report.start.date.period.invalid')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
        }

        if(report.type.name == 'order' && report.name == 'product_subscribers'){
            if (!params.item_id?.trim()) {
                map.error = message(code: 'report.item.invalid')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            } else if (!params.item_id?.isInteger()) {
                map.error = message(code: 'report.item.id.invalid')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }

            if (!new ItemDAS().findNow(params.item_id as Integer)) {
                map.error = message(code: 'item.not.exists')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            }
        }

        if (report.type.name == 'user') {
            if (report.name == TOTAL_INVOICED_PER_CUSTOMER_OVER_YEARS && (!params.start_year?.trim() || !params.end_year?.trim())) {
                map.error = message(code: 'report.start.end.year.not.blank')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            } else if (report.name in [TOTAL_INVOICED_PER_CUSTOMER, TOP_CUSTOMERS, USER_SIGNUPS] &&
                      (!params.start_date?.trim() || !params.end_date?.trim())) {
                map.error = message(code: 'report.start.end.date.not.blank')
                render(status: HttpStatus.SC_OK, text: map as JSON)
                return
            } else if (report.name == USER_ACTIVITY) {
                if(!params.activity_days?.trim()) {
                    map.error = message(code: 'report.number.of.days.not.blank')
                    render(status: HttpStatus.SC_OK, text: map as JSON)
                    return
                }
            }
        }

        render(status: HttpStatus.SC_OK, text: map as JSON)
        return
    }

    def getUsersLoginNameStatusAndGovernorate() {
        def usersInfo = new ArrayList<String>()
        try {
           usersInfo = adennetHelperService.getAllUsersLoginNameStatusAndGovernorate()
        } catch (Exception exception) {
            log.debug("Exception occurred in ReportController while filtering the users by governorate" + exception.getMessage())
        }
        render usersInfo
    }
}
