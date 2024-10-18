package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.customer.CustomerBL
import com.sapienter.jbilling.server.ediTransaction.EDIStatisticWS
import com.sapienter.jbilling.server.ediTransaction.TransactionType
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileExceptionCodeDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDAS
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO
import com.sapienter.jbilling.server.fileProcessing.FileConstants
import com.sapienter.jbilling.server.item.db.PlanDAS
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.DateMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.pricing.RouteBeanFactory
import com.sapienter.jbilling.server.pricing.db.RouteDAS
import com.sapienter.jbilling.server.pricing.db.RouteDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO
import com.sapienter.jbilling.server.util.search.SearchResult
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.hibernate.FetchMode
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

@Secured(["MENU_903"])
class EdiReportController {

    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]
    def messageSource
    def companyService

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService

    def routeService

    public static DateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")

    def index () {
        list()
    }

    def list () {
        render view: 'list'
    }

    def ediStatistics () {
        DateTime firstDayOfMonth = new DateTime().dayOfMonth().withMinimumValue();
        DateTime lastDayOfMonth = new DateTime().dayOfMonth().withMaximumValue();
        if (!params.startDate) {
            params.startDate = simpleDateFormat.format(firstDayOfMonth.toDate())
        }

        if (!params.endDate) {
            params.endDate = simpleDateFormat.format(lastDayOfMonth.toDate())
        }

        Date startDate, endDate;
        try {
            startDate = simpleDateFormat.parse(params.startDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.from.date.invalid'
            startDate = firstDayOfMonth.toDate()
        }

        try {
            endDate = simpleDateFormat.parse(params.endDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.to.date.invalid'
            endDate = lastDayOfMonth.toDate()
        }

        if (endDate < startDate) {
            flash.error = 'edi.report.to.date.can.not.earlier.from.date'
            render view: 'ediStatistics', model: [startDate: startDate, endDate: endDate]
            return;
        }

        Integer ediTypeId = params.ediTypeId ? params.ediTypeId as Integer : null

        List<EDIStatisticWS> statistics = new EDIFileDAS().getEdiStatistics(session['company_id'] as Integer, startDate, endDate, ediTypeId)

        render view: 'ediStatistics', model: [statistics: statistics, ediTypes: getEdiTypes(), ediTypeId: ediTypeId, startDate: startDate, endDate: endDate]
    }

    def ediStatisticsWithExceptions () {
        DateTime firstDayOfMonth = new DateTime().dayOfMonth().withMinimumValue();
        DateTime lastDayOfMonth = new DateTime().dayOfMonth().withMaximumValue();
        if (!params.startDate) {
            params.startDate = simpleDateFormat.format(firstDayOfMonth.toDate())
        }

        if (!params.endDate) {
            params.endDate = simpleDateFormat.format(lastDayOfMonth.toDate())
        }

        Date startDate, endDate;
        try {
            startDate = simpleDateFormat.parse(params.startDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.from.date.invalid'
            startDate = firstDayOfMonth.toDate()
        }

        try {
            endDate = simpleDateFormat.parse(params.endDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.to.date.invalid'
            endDate = lastDayOfMonth.toDate()
        }

        if (endDate < startDate) {
            flash.error = 'edi.report.to.date.can.not.earlier.from.date'
            render view: 'ediStatisticsWithExceptions', model: [startDate: startDate, endDate: endDate]
            return;
        }

        Integer ediTypeId = params.ediTypeId ? params.ediTypeId as Integer : null

        List<EDIStatisticWS> statistics = new EDIFileDAS().getEdiStatisticsWithExceptions(session['company_id'] as Integer, startDate, endDate, ediTypeId)

        render view: 'ediStatisticsWithExceptions', model: [statistics: statistics, ediTypes: getEdiTypes(), ediTypeId: ediTypeId,startDate: startDate, endDate: endDate]
    }

    def currentEdiExceptions () {
        EDIFileDAS ediFileDAS = new EDIFileDAS()
        Integer companyId = session['company_id'] as Integer
        Integer ediTypeId = params.ediTypeId ? params.ediTypeId as Integer : null

        List<EDIFileDTO> ediFiles = ediFileDAS.getEDIFilesWithExceptions(companyId, ediTypeId, params.int('max') ?: 10, params.int('offset') ?: 0)
        Integer ediFilesTotalCount = ediFileDAS.getEDIFilesWithExceptionsCount(companyId, ediTypeId)

        render view: 'currentEdiExceptions', model: [ediFiles: ediFiles, ediFilesTotalCount: ediFilesTotalCount, ediTypes: getEdiTypes(), ediTypeId: ediTypeId]
    }

    def customersNotInvoiced () {
        UserDAS userDAS = new UserDAS();
        Integer companyId = session['company_id'] as Integer

        DateTime firstDayOfMonth = new DateTime().dayOfMonth().withMinimumValue().withTimeAtStartOfDay();
        DateTime startOfNextMont = firstDayOfMonth.plusMonths( 1 ).dayOfMonth().withMinimumValue().withTimeAtStartOfDay();

        Date startDate, endDate

        if(!params.startDate){
            params.startDate = simpleDateFormat.format(firstDayOfMonth.toDate());
        }

        if(!params.endDate){
            params.endDate = simpleDateFormat.format(startOfNextMont.toDate());
        }

        try {
            startDate = simpleDateFormat.parse(params.startDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.from.date.invalid'
            startDate = firstDayOfMonth.toDate()
        }

        try {
            endDate = simpleDateFormat.parse(params.endDate)
        }
        catch (ParseException e) {
            flash.error = 'edi.report.to.date.invalid'
            endDate = startOfNextMont.toDate()
        }

        if (endDate < startDate) {
            flash.error = 'edi.report.to.date.can.not.earlier.from.date'
            render view: 'customersNotInvoiced', model: [usersTotal: 0, startDate: startDate, endDate: endDate]
            return;
        }

        int max = params.int('max') ?: 10
        int offset = params.int('offset') ?: 0

        List<UserDTO> users = userDAS.getUsersNotInvoiced(companyId,startDate, endDate, max, offset)
        Integer usersTotals = userDAS.getUsersNotInvoicedCount(companyId,startDate, endDate)

        render view: 'customersNotInvoiced', model: [users: users, usersTotal: usersTotals, startDate: startDate, endDate: endDate]
    }

    def downloadRegulatoryComplianceReport() {
        Integer companyId = session['company_id'] as Integer
        CustomerBL bl = new CustomerBL()
        File report = bl.createRegulatoryComplianceReport(companyId)
        DownloadHelper.setResponseHeader(response, "Regulatory_Compliance_Report.txt")

        render file: report , contentType: "text/txt"
    }

    private List<EDITypeDTO> getEdiTypes() {
        return EDITypeDTO.findAllByEntity(CompanyDTO.get(session['company_id'] as Integer), [sort: "name"])
    }

    def autoRenewedCustomer () {

        List<CustomerDTO> customerDTOList=CustomerDTO.createCriteria().list(
                max: params.max?: 10,
                offset: params.offset?: 0
        ) {
            and {
                createAlias("baseUser", "user")
                createAlias("user.company", "company")
                createAlias("metaFields", "mfv")
                createAlias("mfv.field", "metaField")
                eq("metaField.name", FileConstants.RENEWED_DATE)
                eq("user.deleted", 0)
                eq("company.id", session['company_id'])

                //excluding the drop customers
                def dropCustomerCriteria = DetachedCriteria.forClass(CustomerDTO.class, "customer2")
                        .setProjection(Projections.property('customer2.id'))
                        .createAlias("customer2.metaFields", "metaFieldValue")
                        .createAlias("metaFieldValue.field", "metaField")
                        .setFetchMode("metaField", FetchMode.JOIN)
                def metaFieldSubCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                        .setProjection(Projections.property('id'))
                        .add(Restrictions.eq('stringValue.value', FileConstants.DROPPED))
                dropCustomerCriteria.add(Restrictions.eq("metaField.name", FileConstants.TERMINATION_META_FIELD))
                dropCustomerCriteria.add(Property.forName("metaFieldValue.id").in(metaFieldSubCriteria))
                addToCriteria(Property.forName("id").notIn(dropCustomerCriteria))


                def subCriteria = DetachedCriteria.forClass(DateMetaFieldValue.class, "dateValue")
                        .setProjection(Projections.property('id'))
                        .add(Restrictions.isNotNull('dateValue.value'))

                addToCriteria(Property.forName("mfv.id").in(subCriteria))
            }
            SortableCriteria.sort(params, delegate)
        }
        render view: 'autoRenewedCustomers', model: [customers: customerDTOList]
    }


    def subscriptionGoingEnd () {

        Date subscriptionEndDate= TimezoneHelper.currentDateForTimezone(session['company_timezone'])+30

        if(!params.subscriptionEndDate){
            subscriptionEndDate = simpleDateFormat.format(params.subscriptionEndDate.toDate());
        }
        List<Integer> customerIds=CustomerDTO.createCriteria().list() {
            projections {
                property('id')
            }
            and {
                createAlias("baseUser", "user")
                createAlias("user.company", "company")
                createAlias("metaFields", "mfv")
                createAlias("mfv.field", "metaField")
                eq("metaField.name", FileConstants.RENEWED_DATE)
                eq("user.deleted", 0)
                eq("company.id", session['company_id'])

                def subCriteria = DetachedCriteria.forClass(DateMetaFieldValue.class, "dateValue")
                        .setProjection(Projections.property('id'))
                        .add(Restrictions.isNull('dateValue.value'))

                addToCriteria(Property.forName("mfv.id").in(subCriteria))
            }
            SortableCriteria.sort(params, delegate)
        }

        List<CustomerDTO> customerDTOList=CustomerDTO.createCriteria().list(
                max: params.max?: 10,
                offset: params.offset?: 0
        ) {
            and {
                createAlias("baseUser", "user")
                createAlias("user.company", "company")
                createAlias("metaFields", "mfv")
                createAlias("mfv.field", "metaField")
                eq("metaField.name", FileConstants.CUSTOMER_COMPLETION_DATE_METAFIELD)
                eq("user.deleted", 0)
                eq("company.id", session['company_id'])
                'in'('id', customerIds?:[0])
                def subCriteria = DetachedCriteria.forClass(DateMetaFieldValue.class, "dateValue")
                        .setProjection(Projections.property('id'))
                        .add(Restrictions.le('dateValue.value', subscriptionEndDate))

                addToCriteria(Property.forName("mfv.id").in(subCriteria))
            }
            SortableCriteria.sort(params, delegate)
        }
        render view: 'subscriptionGoingToEnd', model: [customers: customerDTOList, subscriptionEndDate:subscriptionEndDate]
    }

    def billingAdministratorEdiFile() {

        List<Integer> companyIds = new ArrayList<>()
        List<String> ediTypeSuffixes = new ArrayList<>()
        List<String> statues = new ArrayList<>()
        List<String> exceptionCodes = new ArrayList<>()

        DateTime firstDayOfMonth = new DateTime().dayOfMonth().withMinimumValue();

        params.startDate = params.startDate ?: simpleDateFormat.format(firstDayOfMonth.toDate())
        params.endDate = params.endDate ?: simpleDateFormat.format(new Date())
        String searchStatus = params?.searchStatus

        params.list('entities').each {
            if (it) companyIds.add(Integer.parseInt(it + ""))
        }

        params.list('ediTypeSuffixes').each {
            if (it) ediTypeSuffixes.add(it)
        }

        params.list('statues').each {
            if (it) statues.add(it)
        }

        params.list('exceptionCodes').each {
            if (it) exceptionCodes.add(it)
        }

        if (companyIds.size() == 0) {
            companyIds.addAll(companyService.getEntityAndChildEntities()*.id)
        }

        try {
            Date startDate = params.date("startDate", "MM/dd/yyyy");
            Date endDate = params.date("endDate", "MM/dd/yyyy");
            if (!startDate || !endDate) {
                throw new SessionInternalError("Unable to parse date", ["Unable to parse date. Please provide date in  " + message(code: "datepicker.format") + " format"] as String[])
            }

            List<EDIFileDTO> ediFileDTOList = EDIFileDTO.createCriteria().list(
                    max: params.max ?: 10,
                    offset: params.offset ?: 0

            ) {
                createAlias("fileStatus", "fileStatus")

                and {
                    between('createDatetime', startDate, endDate+1)
                    if (companyIds && companyIds.size() > 0) {
                        createAlias("entity", "entity")
                        'in'('entity.id', companyIds)
                    }
                    if (ediTypeSuffixes && ediTypeSuffixes.size() > 0) {
                        createAlias("ediType", "ediType")
                        'in'('ediType.ediSuffix', ediTypeSuffixes)
                    }
                    if (statues && statues.size() > 0) {
                        'in'('fileStatus.name', statues)
                    }
                    if (searchStatus) {
                        switch (searchStatus) {
                            case "processed":
                                eq('fileStatus.error', false)
                                break
                            case "criticalError":
                                inList('fileStatus.name', ['Rejected'])
                                break;
                            case "onHold":
                                    List<String> holdStatus = ["Invalid Data", "EXP002", "Invalid File", "On Hold"]
                                    inList('fileStatus.name', holdStatus)
                                break
                            case "unProcessable":
                                eq('fileStatus.error', true)
                                 isEmpty("fileStatus.associatedEDIStatuses")
                                break
                            default:
                                break
                        }
                    }
                    if (exceptionCodes && exceptionCodes.size() > 0) {
                        createAlias("exceptionCode", "exceptionCodes")
                        'in'('exceptionCodes.exceptionCode', exceptionCodes)
                    }
                }
            }
            exceptionCodes = findExceptionCodeByStatues(statues)
            statues = findStatusBySuffix(ediTypeSuffixes, searchStatus)
            ediTypeSuffixes = findEdiTypeSuffixByEntities(companyIds)

            render view: 'billingAdministratorEdiFile', model: [ediFiles: ediFileDTOList, allCompanies: retrieveChildCompanies(), ediTypeSuffixes: ediTypeSuffixes, statues: statues, exceptionCodes: exceptionCodes, searchStatus: searchStatus]
        } catch (Exception e) {
            viewUtils.resolveException(flash, session.locale, e)
            params.startDate = simpleDateFormat.format(firstDayOfMonth.toDate())
            params.endDate = simpleDateFormat.format(new Date())
            exceptionCodes = findExceptionCodeByStatues(statues)
            statues = findStatusBySuffix(ediTypeSuffixes, searchStatus)
            ediTypeSuffixes = findEdiTypeSuffixByEntities(companyIds)
            render view: 'billingAdministratorEdiFile', model: [ediFiles: [], allCompanies: retrieveChildCompanies(), ediTypeSuffixes: ediTypeSuffixes, statues: statues, exceptionCodes: exceptionCodes, searchStatus: searchStatus]
            return
        }
    }

    def retrieveChildCompanies() {
        List<CompanyDTO> companies = CompanyDTO.findAllByParent(CompanyDTO.get(session['company_id']))
        return companies
    }

    def billingAdministrator(){

        Integer companyId=params.int('companyId')
        if(companyId==null){
            render view: 'billingAdministrator', model: [ediFiles:[], users: [], allCompanies: retrieveChildCompanies()]
            return
        }


        try{
            DateTime selectedDate=new DateTime(new Date());
            if(params.end_date){
                Date date= params.date("end_date", "MM/dd/yyyy")
                if(date==null){
                    //if selected date is invalid then seting current date
                    params.end_date = simpleDateFormat.format(new Date())
                    throw new SessionInternalError("Unable to parse date", ["Unable to parse date. Please provide date in  "+message(code:"datepicker.format")+" format"] as String[])
                }
                selectedDate=new DateTime(date)
            }

            MetaFieldValue<String> calendarNameMetaField=CompanyDTO.get(companyId).getMetaField(FileConstants.COMPANY_CALENDAR_META_FIELD_NAME);
            if(calendarNameMetaField.getValue()==null)
                throw new SessionInternalError("Configuration issue : Calendar is not configured", ["Configuration issue : Calendar is not configured"] as String[] );

            String calendarName=calendarNameMetaField.getValue();
            RouteDTO routeDTO=new RouteDAS().getRoute(companyId, calendarName);
            if(routeDTO==null){
                throw new SessionInternalError("Configuration Issue: No Calender configured with name "+calendarName, ["Configuration Issue: No Calender configured with name "+calendarName] as String[]);
            }

            String cycleNumber=findCycleNumber(routeDTO, selectedDate);

            Integer cycle=Integer.parseInt(cycleNumber)
            List<UserDTO> users= UserDTO.createCriteria().list(
                    max:    params.max ?: 10,
                    offset: params.offset ?: 0
            ) {
                createAlias("customer", "customer")
                createAlias("customer.accountType", "accountType")
                createAlias("company", "company")
                eq('deleted', 0)
                eq('company.id', companyId)

                if(params.accountType){
                    def accountCriteria = DetachedCriteria.forClass(InternationalDescriptionDTO.class, "internationalDescription")
                            .setProjection(Projections.property('internationalDescription.id.foreignId'))
                            .add(Restrictions.eq('internationalDescription.content', params.accountType))
                    addToCriteria(Property.forName("accountType.id").in(accountCriteria))
                }

                if(params.plan){
                    def planCriteria = DetachedCriteria.forClass(OrderDTO.class, "order")
                    .setProjection(Projections.property('order.baseUserByUserId.id'))
                    .createAlias("lines", 'line')
                    .createAlias("line.item", 'item')
                    .add(Restrictions.eq('item.internalNumber', params.plan))
                    addToCriteria(Property.forName("id").in(planCriteria))
                }

                def sameCycelCustomerCriteria = DetachedCriteria.forClass(CustomerDTO.class, "customer2")
                        .setProjection(Projections.property('customer2.id'))
                        .createAlias("customer2.metaFields", "metaFieldValue")
                        .createAlias("metaFieldValue.field", "metaField")
                        .setFetchMode("metaField", FetchMode.JOIN)
                def metaFieldSubCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                        .setProjection(Projections.property('id'))
                        .add(Restrictions.eq('integerValue.value', cycle))
                sameCycelCustomerCriteria.add(Restrictions.eq("metaField.name", FileConstants.CUSTOMER_METER_CYCLE_METAFIELD_NAME))
                sameCycelCustomerCriteria.add(Property.forName("metaFieldValue.id").in(metaFieldSubCriteria))
                addToCriteria(Property.forName("customer.id").in(sameCycelCustomerCriteria))

            }

            // find all the edi files of the customers whose date created is between the  start date and end date
            List<EDIFileDTO> ediFiles=[]

            Date cycleStartDate=findCycleStartDate(routeDTO, cycleNumber, selectedDate)

            if(users){
                ediFiles= EDIFileDTO.createCriteria().list() {
                    createAlias("entity", "entity")
                    createAlias("ediType", "ediType")
                    createAlias("fileStatus", "fileStatus")
                    createAlias("user", "user")
                    and {
                        'in'('user.id', users.id)

                        // finding meter/invoice EDI file by their end date and others EDI file by their date creation
                        or{
                            and{
                                'in'('ediType.ediSuffix', ['810', '867'])
                                between('endDate', cycleStartDate.plus(1), selectedDate.plusDays(1).toDate())
                            }
                            and{
                               not{'in'('ediType.ediSuffix', ['810', '867'])}
                                between('createDatetime', cycleStartDate, selectedDate.plusDays(1).toDate())
                            }
                        }

                        if(params.status ){
                            if(params.status=='processed'){
                                eq('fileStatus.error', false)
                            }
                            if(params.status=='onHold'){
                                List<String> holdStatus=["Invalid Data", "EXP002", "Invalid File"]
                                eq('fileStatus.error', true)
                                inList('fileStatus.name', holdStatus)
                            }
                            if(params.status=='unProcessable'){
                                eq('fileStatus.error', true)
                                isEmpty("fileStatus.associatedEDIStatuses")
                            }
                        }
                        if(params.suffix){
                            eq("ediType.ediSuffix", params.suffix)
                        }
                    }
                }
            }

            render view: 'billingAdministrator', model: [ediFiles:ediFiles, users: users, allCompanies: retrieveChildCompanies()]
        }catch (SessionInternalError e){
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'billingAdministrator', model: [ediFiles:[], users: [], allCompanies: retrieveChildCompanies()]
            return
        }
    }

    /**
     * This method return the cycle number form the Cycel calendar(Tata Table) on a given date.
     * @param routeDTO
     * @param selectedDate
     * @return
     */
    private String findCycleNumber(RouteDTO routeDTO, DateTime selectedDate){

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("M/d").withLocale(Locale.ENGLISH);
        String cycleCalendarDate=dateFormatter.print(selectedDate).toUpperCase();

        DateTimeFormatter tripFormatter = DateTimeFormat.forPattern("yyyy").withLocale(Locale.ENGLISH);
        String trip=tripFormatter.print(selectedDate).toUpperCase();

        Map<String, Map<String, Object>> filters = new HashMap<String, Map<String, Object>>();
        Map<String, Object> tripFilter = new HashMap<String, Object>();
        tripFilter.put("key", "trip");
        tripFilter.put("value", "%"+trip+"%");
        tripFilter.put("constraint", com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.ILIKE.toString());
        filters.put("trip", tripFilter)

        Map<String, Object> otherFilter = new HashMap<String, Object>();
        otherFilter.put("key", "date");
        otherFilter.put("value", cycleCalendarDate);
        otherFilter.put("constraint", com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ.toString());
        filters.put("date", otherFilter)

        SearchResult<String> result = routeService.getFilteredRecords(routeDTO.getId(), filters);
        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();
        String columnName="cycle_number"
        Integer searchNameIdx = columnNames.indexOf(columnName);
        if(searchNameIdx.equals(-1)){
            throw new SessionInternalError("Calendar "+routeDTO.getName() +" have not "+columnName+" column ", ["Calendar "+routeDTO.getName() +" have not "+columnName+" column "] as String[]);
        }

        if(!result.getRows()){
            throw new SessionInternalError("cycle calender does not contains value", ["cycle calender does not contains value for "+cycleCalendarDate+"/"+trip] as String[] );
        }

        return result.getRows().get(0).get(searchNameIdx);

    }

    /**
     * This method search the cycle start date according the customer cycle number from cycle calendar.
     * For example if selected date it 3/3/2016 and cycle number is 20 then the closest but small date for cycle number 20 will be the start date of then cycle.
     *
     * @param routeDTO calender
     * @param cycleNumber customer cycle number
     * @param selectedDate cycle end date.
     * @return
     */
    private Date findCycleStartDate(RouteDTO routeDTO, String cycleNumber, DateTime selectedDate){
        Map<String, Map<String, Object>> filters = new HashMap<String, Map<String, Object>>();

        Map<String, Object> otherFilter = new HashMap<String, Object>();
        otherFilter.put("key", "cycle_number");
        otherFilter.put("value", cycleNumber);
        otherFilter.put("constraint", com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ.toString());
        filters.put("cycle_number", otherFilter)

        SearchResult<String> result = routeService.getFilteredRecords(routeDTO.getId(), filters);
        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();
        String columnName="date"
        Integer searchNameIdx = columnNames.indexOf(columnName);
        if(searchNameIdx.equals(-1)){
            throw new SessionInternalError("Calendar "+routeDTO.getName() +" have not "+columnName+" column ", ["Calendar "+routeDTO.getName() +" have not "+columnName+" column "] as String[]);
        }

        if(!result.getRows()){
            throw new SessionInternalError("No record find for cycle number "+cycleNumber, ["No record find for cycle number "+cycleNumber] as String[]);
        }

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("M/d").withLocale(Locale.ENGLISH);
        String cycleCalendarDate=dateFormatter.print(selectedDate).toUpperCase();

        List searchedDate=[];
        for(List row:result.getRows()){
           if(row.get(3).equals(cycleCalendarDate) && row.get(2).equals(cycleNumber) && row.get(1).toString().contains(selectedDate.getYear()+"")){
               break;
           }
            searchedDate=row;
        }

        if(!searchedDate){
           throw new SessionInternalError("No record found", ["Cycle start date not found for "+formatDate(date: selectedDate.toDate(), formatName: 'date.pretty.format') + " in cycle calendar"] as String[])
        }

        String cycleDate = searchedDate.get(searchNameIdx);
        return new DateTime(Date.parse('MM/dd/yyyy', cycleDate+"/"+searchedDate.get(1).split("-")[1])).toDate()
    }

    def findPlanByCompany() {
        List<PlanDTO> plans = new ArrayList<>();
        PlanDAS planDAS = new PlanDAS()
        plans = planDAS.findAllActive(params.int('companyId'))
        render g.select(from: plans, optionKey: { it.item.internalNumber }, optionValue: {
            it.item.internalNumber + '(' + it.id + ')'
        }, name: 'plan', id: 'planSelect', noSelection: ['': 'All'])
    }

    def findEdiStatuses() {
        String suffix = params?.suffixes
        String status = params?.status
        List<String> statues = findStatusBySuffix([suffix], status)
        render g.select(from: statues, name: 'statues', id: 'status-select', multiple: 'multiple')
    }

    def findEdiExceptionCode() {
        List<String> statues = JSON.parse(params.statues)
        List<String> exceptionCode = findExceptionCodeByStatues(statues)
        render g.select(from: exceptionCode, name: 'exceptionCodes', id: 'exception-codes-select', multiple: 'multiple')
    }

    def searchStatus() {
        String status = params?.status
        String ediTypeSuffix = params?.ediTypeSuffix
        List<String> statues = findStatusBySuffix([ediTypeSuffix], status)
        render g.select(from: statues, name: 'statues', id: 'status-select', multiple: 'multiple')
    }

    def findEdiType() {
        CompanyDTO companyDTO=CompanyDTO.findById(session['company_id'] as Integer)
        List<String> types = new ArrayList<>();

        if(companyDTO.getParent()){
            types = new EDITypeDAS().getEDITypesByEntity(companyDTO.id).ediSuffix
        }else{
            types = EDITypeDTO.createCriteria().list() {
                projections {
                    distinct("ediSuffix")
                }
                inList("entity", retrieveChildCompanies())
            }
        }
        render g.select(from: types, optionKey: { it }, optionValue: {it}, name: 'suffix', id: 'type-select', noSelection: ['': 'Please select EDI Type'])
    }

    private List<String> findEdiTypeSuffixByEntities(List<Integer> companyIds) {
        if(!(companyIds != null && companyIds.size() > 0)) return null
        return EDITypeDTO.createCriteria().list() {
            createAlias("entities", "ce",)
            projections {
                distinct("ediSuffix")
            }
            'in'('ce.id', companyIds)
        }
    }


    private List<String> findStatusBySuffix(List<String> suffixes, String filterStatus) {
        if (!(suffixes != null && suffixes.size() > 0)) return null
        return EDITypeDTO.createCriteria().list() {
            createAlias("statuses", "status")
            projections {
                distinct("status.name")
            }
            and {
                inList("ediSuffix", suffixes)
                switch (filterStatus) {
                    case "processed":
                        eq('status.error', false)
                        break
                    case "criticalError":
                        inList('status.name', ['Rejected', 'Error Detected'])
                    case "onHold":
                        if (suffixes.size() > 0 && ("867".equals(suffixes.get(0)) || "810".equals(suffixes.get(0)))) {
                            List<String> holdStatus = ["Invalid Data", "EXP002", "Invalid File","On Hold"]
                            eq('status.error', true)
                            inList('status.name', holdStatus)
                        }
                        break
                    case "unProcessable":
                        eq('status.error', true)
                        isEmpty("status.associatedEDIStatuses")
                        break
                    default:
                        break
                }
            }
        }
    }

    private List<String> findExceptionCodeByStatues(List<String> statues) {
        if (!(statues != null && statues.size() > 0)) return null
        return EDIFileExceptionCodeDTO.createCriteria().list() {
            createAlias("status", "status")
            projections {
                distinct("exceptionCode")
            }
            inList("status.name", statues)
        }
    }

    def findAccountType() {
        CompanyDTO companyDTO=CompanyDTO.findById(session['company_id'] as Integer)

        List<CompanyDTO> companyDTOs=new ArrayList<>();
        if(companyDTO.getParent()){
            companyDTOs.add(companyDTO)
        }else{
            companyDTOs.addAll(retrieveChildCompanies())
        }
        List<String> accountTypes = AccountTypeDTO.findAllByCompanyInList(companyDTOs).description.unique()
        render g.select(from: accountTypes, optionKey: { it }, optionValue: {it}, name: 'accountType', noSelection: ['': 'Any'])
    }

    /**
     * This will render the view for displaying orphan ldc files. Type will be provided as params.id.
     * If not exist then redirect to list page.
     */
    def ldcFiles() {
        if (params.id) {
            render view: 'ldcFiles'
            return
        }
        flash.error = 'ldc.files.invalid.data'
        redirect action: 'list'
    }

    /**
     * Return file information as JSON object. It will be called by jqgrid.
     */
    def getLdcFiles() {
        TransactionType type = TransactionType.valueOf(params.id)
        def orphanEDIFiles = webServicesSession.getLDCFiles(type)
        def jsonData = [rows: orphanEDIFiles]
        render jsonData as JSON
    }

    /**
     * Download the orphan edi file based on type and file name.
     */
    def download() {
        TransactionType type = TransactionType.valueOf(params.id)
        def fileName = params.fileName
        if (!fileName) {
            flash.error = 'ldc.files.invalid.data'
        }
        def orphanEDIFile = webServicesSession.getOrphanLDCFile(type, fileName)
        if (orphanEDIFile.exists()) {
            response.setContentType("application/octet-stream")
            response.setCharacterEncoding("UTF-8")
            response.setHeader("Content-disposition", "attachment;filename=\"${orphanEDIFile.name}\"")
            response.outputStream << orphanEDIFile.bytes
            return
        } else {
            flash.error = message(code: "edi.file.file.download.fail")
        }
        redirect action: 'list'
    }

    /**
     * Can delete multiple files. Take comma separated files names as input.
     */
    def delete() {
        TransactionType type = TransactionType.valueOf(params.id)
        List<String> fileNames = params['fileNames'].tokenize(",").findAll { !it?.trim()?.isEmpty() }
        try {
            if(fileNames.size()==0){
                throw new SessionInternalError("Please select an EDI file", ["No file selected to delete. Please select file"] as String[]);
            }
            webServicesSession.deleteOrphanEDIFile(type, fileNames)
            flash.message = 'ldc.files.deleted'
        } catch (Exception ex) {
            log.error("Could not delete file", ex)
            viewUtils.resolveException(flash, session.locale, ex)
        }
        redirect(action: 'ldcFiles', params: [id: params.id])
    }
}