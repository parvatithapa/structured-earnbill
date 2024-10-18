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

import java.util.List;

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.filter.JbillingFilterConverter
import com.sapienter.jbilling.server.invoice.InvoiceBL
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.mediation.converter.MediationJobs;
import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.invoiceTemplate.report.InvoiceTemplateBL
import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.MediationProcess
import com.sapienter.jbilling.server.mediation.MediationProcessCDRCountInfo;
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.process.ProcessStatusWS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter

import org.apache.commons.lang.NotImplementedException
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

/**
 * MediationController
 *
 * @author Vikas Bodani
 * @since 17/02/2011
 */
@Secured(["MENU_95"])
class MediationController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static int CDR_PAGE_SIZE = 25

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['processId': 'id',
             'startDate': 'startDate',
             'endDate': 'endDate',
             'orders': 'recordsProcessed']


    IWebServicesSessionBean webServicesSession
	IMediationSessionBean mediationSession
    ViewUtils viewUtils

    def recentItemService
    def breadcrumbService
    def filterService
    def mediationService
    def mediationProcessService
    def companyService
    SecurityValidator securityValidator


    def index (){
        list()
    }

    def list () {
        def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)
        if (params['filters.MEDIATIONPROCESS-EQ_Id.stringValue']) {
            filters.each {
                if (it.field == 'id') {
                    it.type = FilterType.MEDIATIONPROCESS
                    it.stringValue = params['filters.MEDIATIONPROCESS-EQ_Id.stringValue']
                }
            }
        }

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def isMediationProcessRunning = webServicesSession.isMediationProcessRunning();
        if (isMediationProcessRunning) {
            flash.info = 'mediation.config.prompt.running'
        }

        def configurations = webServicesSession.getAllMediationConfigurations() as List
        def hasNonGlobalConfig = false
        for (MediationConfigurationWS config : configurations) {
            hasNonGlobalConfig |= !config.global
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'processesTemplate', model: [filters: filters,
                                                              isMediationProcessRunning: isMediationProcessRunning,
                                                              hasNonGlobalConfig: hasNonGlobalConfig]
            } else {
                render view: "list", model: [filters: filters,
                                             isMediationProcessRunning: isMediationProcessRunning,
                                             hasNonGlobalConfig: hasNonGlobalConfig]
            }

            return
        }

        List<com.sapienter.jbilling.server.filter.Filter> convertedFilters = JbillingFilterConverter.convert(filters);
        params.max = params.max ? Integer.parseInt(params.max): 10
        params.sort = params.sort ? params.sort : "startDate"
        params.order = params.order ? params.order : "desc"

        List<MediationProcess> processes = mediationProcessService.findMediationProcessByFilters(session['company_id'], params.int('offset') ?: 0, params.max, params.sort, params.order, convertedFilters)
        def size = mediationProcessService.countMediationProcessByFilters(session['company_id'], convertedFilters)

        if (params.applyFilter || params.partial) {
            render template: 'processesTemplate', model: [processes: processes, filters: filters,
                                                          isMediationProcessRunning: isMediationProcessRunning,
                                                          hasNonGlobalConfig: hasNonGlobalConfig, size: size]
        } else {
            render view: "list", model: [processes: processes, filters: filters,
                                         isMediationProcessRunning: isMediationProcessRunning,
                                         hasNonGlobalConfig: hasNonGlobalConfig, size: size]
        }
    }

    def findProcesses (){
        def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        List<MediationProcess> processes = getFilteredProcesses(filters, params)
        try {
            render getProcessesJsonData(processes, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Mediation processes to JSon
     */
    private def Object getProcessesJsonData(processes, GrailsParameterMap params) {
        def jsonCells = processes
        def currentPage = params.page ? params.int('page') : 1
        def rowsNumber = params.rows ? params.int('rows'): 1
        def numberOfPages = Math.ceil(jsonCells.size / rowsNumber.intValue())

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.size, total: numberOfPages]

        jsonData
    }

    def getFilteredProcesses (filters, params) {
		params.max = (params?.max?.toInteger()) ?: pagination.max
        params.page = (params?.page?.toInteger()) ? ((params.page > 0) ? (params.page - 1): params.page): 0
        params.sort = params.sort ? params.sort : "startDate"
        params.order = params.order ? params.order : "desc"

        List<com.sapienter.jbilling.server.filter.Filter> convertedFilters = JbillingFilterConverter.convert(filters);
        convertedFilters.add(com.sapienter.jbilling.server.filter.Filter.integer("entityId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, session['company_id']));

        List<MediationProcess> processes = mediationProcessService.findMediationProcessByFilters(session['company_id'], 0, params.max, params.sort, params.order, convertedFilters)

        return processes
    }

    def show (){
        MediationProcess process = webServicesSession.getMediationProcess(uuid(params.get('id')))
        if (!process) {
            flash.args = [ uuid(params.get('id')) ]
            flash.error = 'mediation.process.not.exist'
            redirect action: 'list'
        } else {
            securityValidator.validateCompany(process?.entityId, Validator.Type.VIEW)
            def canBeUndone = canBeUndone(process)

            recentItemService.addRecentItem(process.id, RecentItemType.MEDIATIONPROCESS)
            breadcrumbService.addBreadcrumb(controllerName, actionName, null, process.id)

            def processId = uuid(params.get('id'))
            def entityId = session['company_id']
            List<MediationProcessCDRCountInfo> cdrCountInfos = mediationProcessService.getCdrCountForMediationProcessAndStatus(processId,JbillingMediationRecordDao.STATUS.PROCESSED.toString());
            List<MediationProcessCDRCountInfo> cdrCountInfosNB = mediationProcessService.getCdrCountForMediationProcessAndStatus(processId,JbillingMediationRecordDao.STATUS.NOT_BILLABLE.toString());
            if (params.template) {
                render template: params.template, model: [
                        selected: process, canBeUndone: canBeUndone,
                        invoicesCreatedCount: 0,//TODO MODULARIZATION, THIS WAS RETRIEVED BY THE MEDIATION SYSTEM
                        ordersCreatedCount: process.getOrderIds().length,
                        cdrCountInfos: cdrCountInfos,
                        cdrCountInfosNB: cdrCountInfosNB]
            } else {
                def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)
                def processes = mediationProcessService.findLatestMediationProcess(entityId, 0, 100) //TODO MODULARIZATION HERE THE FILTERING WAS NOT WORKING IN THE RIGHT WAY
                def size = mediationProcessService.countMediationProcessByFilters(entityId, new ArrayList<>())

                render view: 'list', model: [
                        selected: process, canBeUndone: canBeUndone,
                        processes: processes, filters: filters, size: size,
                        invoicesCreatedCount: 0,//TODO MODULARIZATION, THIS WAS RETRIEVED BY THE MEDIATION SYSTEM
                        ordersCreatedCount: process.getOrderIds().length,
                        cdrCountInfos: cdrCountInfos,
                        cdrCountInfosNB: cdrCountInfosNB]
            }
        }
    }

    def showMediationRecords (){
        def statusId = params.int('status')
        def processId = uuid(params.get('id'))
        def entityId = params.int('entityId') ?: session['company_id']
        def offset, size

        def selectedCallType = params.selectedCallType

        securityValidator.validateCompany(params.int('entityId'), Validator.Type.VIEW)
        log.debug "Submitting for entityId {$entityId}"
        def currency = CompanyDTO.get(session['company_id']).currency

        if (params.first == 'true' || params.offset == null) {
            offset = 0
        } else if(params.back == 'true') {
            offset = params.int('offset') - CDR_PAGE_SIZE
        } else {
            offset = params.int('offset') + CDR_PAGE_SIZE
        }

        DateTimeFormatter dtf = DateTimeFormat.forPattern(message(code: 'date.format'))
        def startDate= params.event_start_date ? dtf.parseDateTime(params.event_start_date).toDate() : null
        def endDate= params.event_end_date ? dtf.parseDateTime(params.event_end_date).toDate() : null

        def defaultStatus = JbillingMediationRecordDao.STATUS.PROCESSED
        if (statusId == Constants.MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE.intValue()) {
            defaultStatus = JbillingMediationRecordDao.STATUS.NOT_BILLABLE
        }

        def filters = Arrays.asList(
                com.sapienter.jbilling.server.filter.Filter.integer("jBillingCompanyId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, entityId),
                com.sapienter.jbilling.server.filter.Filter.uuid("processId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, processId),
                com.sapienter.jbilling.server.filter.Filter.betweenDates("eventDate", startDate, endDate),
                com.sapienter.jbilling.server.filter.Filter.enumFilter("status", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, defaultStatus),
                com.sapienter.jbilling.server.filter.Filter.string("cdrType", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, selectedCallType.equals("") ? null :selectedCallType)
        );
        def records = mediationService.findMediationRecordsByFilters(offset, CDR_PAGE_SIZE, filters)

        String callTypeStr = params.get('allCallTypes')
        Set<String> allCallTypes = getAllCdrTypes(processId, callTypeStr)

        if(params.int('size')){
            size = params.int('size')
        } else {
            size = mediationService.countMediationRecordsByFilters(filters)
        }

        def record
        if (records) {
            record = records?.get(0)
        } else {
                flash.info = message(code: 'event.mediation.records.not.available.for.cdr')
                flash.args = [params.id, params.status, params.selectedCallType]
            }
        render view: 'events', model: [records: records, record: record, currency: currency, selectionEntityId: entityId, processId: processId,
                                       offset: offset, next: (size > offset + records.size), size: size, allCallTypes: allCallTypes, selectedCallType: selectedCallType]
	}

    def showMediationErrors () {
        def processId = uuid(params.get('id'))
        def statusId = params.int('status')
        def entityId = params?.selectedEntity ? params.int('selectedEntity'):session['company_id']
        def offset, size

        securityValidator.validateCompany(params.int('selectedEntity'), Validator.Type.VIEW)

        if (params.first == 'true' || params.offset == null) {
            offset = 0
        } else if(params.back == 'true') {
            offset = params.int('offset') - CDR_PAGE_SIZE
        } else {
            offset = params.int('offset') + CDR_PAGE_SIZE
        }

        def filters = new ArrayList<>()
        filters.addAll(Arrays.asList(
                com.sapienter.jbilling.server.filter.Filter.integer("jBillingCompanyId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, entityId),
                com.sapienter.jbilling.server.filter.Filter.uuid("processId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, processId),
        ));

        def mediationErrorRecords
        def record
        if(statusId == Constants.MEDIATION_RECORD_STATUS_DUPLICATE.intValue()){
            mediationErrorRecords = mediationService.findMediationDuplicateRecordsByFilters(offset, CDR_PAGE_SIZE, filters)
            if(params.int('size')){
                size = params.int('size')
            } else {
                filters.add(com.sapienter.jbilling.server.filter.Filter.string("errorCodes", com.sapienter.jbilling.server.filter.FilterConstraint.LIKE, "JB-DUPLICATE"))
                size = mediationService.countMediationErrorsByFilters(filters)
            }
        } else {
            mediationErrorRecords = mediationService.findMediationErrorRecordsByFilters(offset, CDR_PAGE_SIZE, filters)
            if(params.int('size')){
                size = params.int('size')
            } else {
                filters.add(com.sapienter.jbilling.server.filter.Filter.string("errorCodes", com.sapienter.jbilling.server.filter.FilterConstraint.NOT_LIKE, "JB-DUPLICATE"))
                size = mediationService.countMediationErrorsByFilters(filters)
            }
        }

        def pricingFieldsHeader = JbillingMediationErrorRecord.getPricingHeaders(mediationErrorRecords).sort()
        if (mediationErrorRecords) {
            record = mediationErrorRecords?.get(0)
        }

        List companies = []
        def currentCompany = CompanyDTO.get(session['company_id'] as Integer)
        if (new CompanyDAS().isRoot(session['company_id'] as Integer)) {
            companies = CompanyDTO.findAllByParent(currentCompany)
            companies?.sort({ a, b -> a.description <=> b.description } as Comparator)
            companies.add(0, currentCompany)
        }
        render view: 'errors', model: [            records : mediationErrorRecords,
                                       pricingFieldsHeader : pricingFieldsHeader,
                                                    record : record,
                                                    offset : params.offset?:0,
                                                 companies : companies,
                                                  selected : entityId,
                                                    offset : offset,
                                                      next : (size > offset + mediationErrorRecords.size),
                                                      size : size]
    }

    def mediationRecordsCsv (){
        def processId = uuid(params.get('id'))
        def orderId = params.get('orderId');
		def selectedCallType = params.selectedCallType
		def filters = new ArrayList<>();
        filters.addAll(Arrays.asList(
			com.sapienter.jbilling.server.filter.Filter.enumFilter("status", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, JbillingMediationRecordDao.STATUS.valueOf(params.get('status'))),
			com.sapienter.jbilling.server.filter.Filter.uuid("processId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, processId),
			com.sapienter.jbilling.server.filter.Filter.string("cdrType", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, selectedCallType.equals("") ? null :selectedCallType)
		));

        if(StringUtils.isNotBlank(orderId)) {
            filters.add(com.sapienter.jbilling.server.filter.Filter.string("orderId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, orderId));
        }

        def records = mediationService.findMediationRecordsByFilters(0, CsvExporter.MAX_RESULTS, filters)

        if (records.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
                redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "mediation_records.csv")
            Exporter<JbillingMediationRecord> exporter = CsvExporter.createExporter(JbillingMediationRecord.class);
            render text: exporter.export(records), contentType: "text/csv"
        }
    }

    def mediationErrorsCsv (){
        def processId = uuid(params.get('id'))
		List<JbillingMediationErrorRecord> mediationErrorRecords = new ArrayList<>()
		if(params.get('isDuplicate').equals("true")) {
			mediationErrorRecords.addAll mediationService.getMediationDuplicatesRecordsForProcess(processId);
		} else {
			mediationErrorRecords.addAll mediationService.getMediationErrorRecordsForProcess(processId);
		}
        if (mediationErrorRecords.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
                redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "mediation_errors.csv")
            Exporter<JbillingMediationErrorRecord> exporter = CsvExporter.createExporter(JbillingMediationErrorRecord.class);
            render text: exporter.export(mediationErrorRecords), contentType: "text/csv"
        }
    }

    def invoice (){
        def invoiceId = params.get('id')
        def invoice, records, record, processId
        params.status=null
		def entityId = params.int('entityId') ?: session['company_id']
		Set<String> allCallTypes
        try {
            invoice = InvoiceDTO.get(invoiceId)
            securityValidator.validateUserAndCompany(InvoiceBL.getWS(invoice), Validator.Type.VIEW)
            records = InvoiceTemplateBL.retrieveMediationRecordLinesForInvoice(invoice)
            log.debug ("Events found ${records.size}")
            processId= null;
            String callTypeStr = params.get('allCallTypes')
            if (records) {
                record = records?.get(0)
                processId = record.getProcessId()
                allCallTypes = getAllCdrTypes(processId, callTypeStr)
            } else {
                flash.info = message(code: 'event.mediation.records.not.available')
                flash.args = [params.id, params.status]
            }
        } catch (Exception e) {
            flash.info = message(code: 'error.mediation.events.none')
            flash.args = [params.id]
        }
        render view: 'events', model: [invoice: invoice, records: records, record: record, processId: processId, allCallTypes: allCallTypes, selectionEntityId: entityId]
    }

    def order (){

        def orderId = params.int('orderId') ?: params.int('id')
        params.status= null
        def order, records, record, offset, size
        def entityId = params.int('entityId') ?: session['company_id']

        def startDate= params.event_start_date ? TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.event_start_date) : null
        def endDate= params.event_end_date ? TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.event_end_date) : null

		Set<String> allCallTypes;
        def processId= null;
        try {
            order = OrderDTO.get(orderId)
            securityValidator.validateUserAndCompany(webServicesSession.getOrder(order.id), Validator.Type.VIEW)

            if (params.first == 'true' || params.offset == null) {
                offset = 0
            } else if(params.back == 'true') {
                offset = params.int('offset') - CDR_PAGE_SIZE
            } else {
                offset = params.int('offset') + CDR_PAGE_SIZE
            }

            def filters = Arrays.asList(
                    com.sapienter.jbilling.server.filter.Filter.integer("orderId",  com.sapienter.jbilling.server.filter.FilterConstraint.EQ, order.id),
                    com.sapienter.jbilling.server.filter.Filter.betweenDates("eventDate", startDate, endDate)//,
            );

            records = mediationService.findMediationRecordsByFilters(offset, CDR_PAGE_SIZE, filters)
            if(params.int('size')){
                size = params.int('size')
            } else {
                size = mediationService.countMediationRecordsByFilters(filters)
            }

            log.debug ("Events found ${records.size}")
            String callTypeStr = params.get('allCallTypes')
			if (records) {
            	record = records?.get(0)
                processId = record.getProcessId()
                allCallTypes = getAllCdrTypes(processId, callTypeStr)
			} else {
                flash.info = message(code: 'event.mediation.records.not.available')
                flash.args = [orderId, params.status]
            }
        } catch (Exception e) {
            flash.info = message(code: 'error.mediation.events.none')
            flash.args = [params.id]
        }
        render view: 'events', model: [order: order, records: records, event_start_date: startDate, event_end_date: endDate, CDR_PAGE_SIZE: CDR_PAGE_SIZE,
                                       record: record, orderEvents: true, processId: processId, offset: offset, next: (size > offset + records.size), size: size,
                                       allCallTypes: allCallTypes, selectionEntityId: entityId]
    }

    def orderRecordsCsv = {
        //TODO MODULARIZATION: FIX THIS
        throw new NotImplementedException();
//        def orderId = params.int('id')
//        def records = mediationSession.getMediationRecordLinesForOrder(orderId)
//
//        params.max = CsvExporter.MAX_RESULTS
//
//        if (records.size() > CsvExporter.MAX_RESULTS) {
//            flash.error = message(code: 'error.export.exceeds.maximum')
//            flash.args = [CsvExporter.MAX_RESULTS]
//            redirect action: 'list', id: params.id
//        } else {
//            DownloadHelper.setResponseHeader(response, "mediation_records.csv")
//            Exporter<MediationRecordLineDTO> exporter = CsvExporter.createExporter(MediationRecordLineDTO.class);
//            render text: exporter.export(records), contentType: "text/csv"
//        }
    }

    def csv (){
        def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        List<MediationProcess> processes = getFilteredProcesses(filters, params)

        if (processes.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "mediationProcesses.csv")
            Exporter<MediationProcess> exporter = CsvExporter.createExporter(MediationProcess.class);
            render text: exporter.export(processes), contentType: "text/csv"
        }
    }

    def undo (){
        MediationProcess process = webServicesSession.getMediationProcess(uuid(params.get('id')))
        if (!process) {
            flash.args = [ uuid(params.get('id')) ]
            flash.error = 'mediation.process.not.exist'
            redirect action: 'list'
            return
        } else {
            securityValidator.validateCompany(process?.entityId, Validator.Type.VIEW)
        }

        try {
            if(canBeUndone(process)) {
                webServicesSession.undoMediation(process.id)
                flash.info = 'mediation.process.undone.successfully'
            } else {
                flash.args = [ uuid(params.get('id')) ]
                flash.error = 'mediation.process.cannot.be.undone'
            }
        } catch (Exception e) {
            log.debug("mediation process can not be undone")
            flash.clear()
            viewUtils.resolveException(flash, session.locale, e)
        }

        redirect action: 'list'
    }

	/**
	 * COPIED from MediationRecordLineDTO
	 * @param entityId
	 * @param orderId
	 * @param orderLineId
	 * @param userId
	 * @param key
	 * @return
	 */
	private getHBaseKey(OrderDTO order, JbillingMediationRecord record) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(order?.baseUserByUserId?.company?.id).append("-");
		if (order?.baseUserByUserId?.id) {
			keyBuilder.append("usr-").append(order.baseUserByUserId.id).append("-");

			if (null != order && null != record) {
				keyBuilder.append("ord-").append(order.id).append("-");
				keyBuilder.append("orl-").append(record.orderLineId).append("-");
			}
		}
		return keyBuilder.toString();
	}

    def recycleProcessCDRs() {
        try {
            UUID processId = uuid(params.get('id'))
            log.debug "Triggering recycle mediation for processId ID ${processId}"
            webServicesSession.runRecycleForMediationProcess (processId)
        } catch (SessionInternalError e){
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'mediation.config.recycle.failure'
            return
        }

        redirect action: 'list'
    }

    def refreshMediationCounter(){
        try {
            UUID processId = uuid(params.get('id'))
            MediationProcess process = mediationProcessService.getMediationProcess(processId)
            securityValidator.validateCompany(process?.entityId, Validator.Type.VIEW)
            mediationProcessService.updateMediationProcessCounters(processId)
        } catch (SessionInternalError e){
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'mediation.config.refresh.counter.failure'
            return
        }
        redirect action: 'show', params:params
    }

    def uuid(String uuidString) {
        return uuidString == null ? null: UUID.fromString(uuidString)
    }

    private boolean canBeUndone(MediationProcess mediationProcess){
        def latestProcessStatus = webServicesSession.getMediationProcessStatus()

        return latestProcessStatus?.getMediationProcessId()?.compareTo(mediationProcess.id) == 0 &&
               !latestProcessStatus?.getState()?.equals(ProcessStatusWS.State.RUNNING) &&
               mediationProcess.entityId == session['company_id']
    }

	private Set<String> getAllCdrTypes(UUID processId, String callTypeStr) {
		Set<String> allCallTypes = new HashSet()
		// add a blank call type for ALL call types
		allCallTypes.add("");

		if(null != callTypeStr && !callTypeStr.trim().isEmpty()) {
			String[] callTypeArr = StringUtils.substring(callTypeStr, 1, callTypeStr.length()-1).split(",\\s")
			for (String callType : callTypeArr){
				if(!StringUtils.isEmpty(callType)) allCallTypes.add(callType)
			}
		} else {
			MediationProcess process = webServicesSession.getMediationProcess(processId);
			def configId = process.configurationId;
			MediationConfiguration mediationConfig = mediationSession.getMediationConfiguration(configId);
			if(null!=mediationConfig){
				def mediationJobLauncher = mediationConfig.getMediationJobLauncher();
				MediationJob mediationJob = MediationJobs.getJobForName(mediationJobLauncher);
				def cdrTypes = mediationJob.getCdrTypes();
				for(String cdrType : cdrTypes){
					if(!StringUtils.isEmpty(cdrType))allCallTypes.add(cdrType)
				}
			}
		}
		return allCallTypes;
	}

}
