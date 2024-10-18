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

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.Util
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO
import com.sapienter.jbilling.server.payment.db.PaymentDAS
import com.sapienter.jbilling.server.process.BillingProcessInfoBL
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.batch.billing.BillingBatchJobService
import com.sapienter.jbilling.server.process.BillingProcessFailedUserBL

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.sapienter.jbilling.batch.email.EmailBatchService

import com.sapienter.jbilling.batch.email.EmailBatchJobService

/**
* BillingController
*
* @author Vikas Bodani
* @since 07/01/11
*/
@Secured(["MENU_94"])
class BillingController {
    static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['billingId': 'id',
             'date': 'billingDate',
             'orderCount': 'orderProcesses',
             'invoiceCount': 'invoices']

    IWebServicesSessionBean webServicesSession
    BillingBatchJobService  billingBatchJobService
    EmailBatchJobService emailBatchJobService
    EmailBatchService emailBatchService
    BillingProcessInfoBL    billingProcessInfoBL
    def recentItemService
    def breadcrumbService
    def filterService
    SecurityValidator securityValidator


    def index () {
        list()
    }

    /*
     * Renders/display list of Billing Processes Ordered by Process Id descending
     * so that the lastest process shows first.
     */
    def list () {
        def filters = filterService.getFilters(FilterType.BILLINGPROCESS, params)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.client.util.Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'billingTemplate', model: [ filters:filters ]
            }else {
                render view: "index", model: [ filters:filters ]
            }
            return
        }

        def processes = getProcesses(filters, params)

        if (params.applyFilter || params.partial) {
            render template: 'billingTemplate', model: [ processes: processes, filters:filters ]
        } else {
            render view: "index", model: [ processes: processes, filters:filters ]
        }
    }

    def findProcesses (){
        def filters = filterService.getFilters(FilterType.BILLINGPROCESS, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def processes = getProcesses(filters, params)

        try {
            render getBillingProcessesJsonData(processes, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Billing processes to JSon
     */
    private def Object getBillingProcessesJsonData(processes, GrailsParameterMap params) {
        def jsonCells = processes
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

    /*
     * Filter the process results based on the parameter filter values
     */
    private def getProcesses (filters, GrailsParameterMap params) {
        params.max    = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort   = params?.sort  ?: pagination.sort
        params.order  = params?.order ?: pagination.order
        def company_id = session['company_id']
        return BillingProcessDTO.createCriteria().list(
            max:    params.max,
            offset: params.offset
        ) {
            and {
                filters.each { filter ->
                    if (filter.value) {
                        addToCriteria(filter.getRestrictions());
                    }
                }
                eq('entity', new CompanyDTO(session['company_id']))
                if (params.billingId) {
                    eq('id', params.getInt('billingId'))
                }
           }
           // apply sorting
           SortableCriteria.sort(params, delegate)
        }
    }

    /*
     * To display the run details of a given Process Id
     */
    def show () {
        Integer processId = params.int('id')

        // get billing process record
        BillingProcessDTO process = BillingProcessDTO.get(processId)
        if (!process) {
            flash.error = 'billing.process.review.doesnotexist'
            flash.args = [processId]
            redirect action:'list'
            return
        }

        securityValidator.validateCompany(process?.entity?.id, Validator.Type.VIEW)

        def configuration = BillingProcessConfigurationDTO.findByEntity(new CompanyDTO(session['company_id']))

        // main billing process run (not a retry!)
        def processRuns = process?.processRuns?.asList()?.sort{ it.started }
        def processRun =  processRuns?.size() > 0 ? processRuns.first() : null 

        // all payments made to generated invoices between process start & end
        def generatedPayments = []
        if (processRun) {
            generatedPayments = new PaymentDAS().findBillingProcessGeneratedPayments(processId, processRun.started, processRun.finished)
        }
        // all payments made to generated invoice after the process end
        def invoicePayments = []
        if (processRun) {
            invoicePayments = new PaymentDAS().findBillingProcessPayments(processId, processRun.finished)
        }

        // all invoices for the billing process. Avoiding using the associations
        def invoices = process ? new InvoiceDAS().findByProcess(process) : [] 

        recentItemService.addRecentItem(processId, RecentItemType.BILLINGPROCESS)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, processId)

        def canRestart = billingProcessInfoBL.canRestart(processId)
        List jobs = billingProcessInfoBL.findExecutionsInfoByBillingProcessId(processId)

        BillingProcessBL processBL =  new BillingProcessBL()
        def orderProcessCount = processBL.getOrderProcessCount(processId)
        def invoiceProcessCount = processBL.getInvoiceProcessCount(processId)        
        
        List emailProcessInfo = Collections.emptyList();
        def cutOfBillingProcess = 0
        
        if( process?.isReview == 0) {
            emailProcessInfo = processBL.getAllInvoiceEmailProcessInfo(processId)
            if(null != emailProcessInfo && emailProcessInfo.size == 0) {
                def cutOfBillingProcessStr = new MetaFieldDAS().getComapanyLevelMetaFieldValue(Constants.CUT_OFF_BILLING_PROCESS_ID, session['company_id']);
                cutOfBillingProcess = cutOfBillingProcessStr? Integer.valueOf(cutOfBillingProcessStr) : 0
            }

            if(null !=  emailProcessInfo ) {
                for (emailProcess in emailProcessInfo) {
                    emailProcess.setEmailsSent(processBL.getEmailsSentCount(processId, emailProcess.getJobExecutionId()))
                    emailProcess.setEmailsFailed(processBL.getEmailsFailedCount(processId, emailProcess.getJobExecutionId()))
                }
            }
        }
            
        
        
        if (params['emailJobTriggerred']?.toBoolean()) {
            flash.info = 'prompt.email.job.running'
        }
        
        def isBillingRunning = webServicesSession.isBillingRunning(session['company_id']);
        
        def isEmailJobRunning = emailBatchService.isEmailJobRunning();
        
        [ process: process, processRun: processRun, generatedPayments: generatedPayments, invoicePayments: invoicePayments,
          configuration: configuration, invoices: invoices,
          formattedPeriod: getFormattedPeriod(process?.periodUnit.id, process?.periodValue, session['language_id']),
          jobs: jobs, canRestart: canRestart, orderProcessCount: orderProcessCount, invoiceProcessCount: invoiceProcessCount,
          emailProcessInfo : emailProcessInfo, cutOfBillingProcess: cutOfBillingProcess, isBillingRunning : isBillingRunning, 
          isEmailJobRunning : isEmailJobRunning, 'emailJobTriggerred': false]
    }

    def failed () {
        def processId = params.int('id')

        def process = BillingProcessDTO.get(processId)

        securityValidator.validateCompany(process?.entity?.id, Validator.Type.VIEW)

        def users = new BillingProcessFailedUserBL().getUsersByExecutionId(processId)

        [users : users]
    }

    private String getFormattedPeriod(Integer periodUnitId, Integer periodValue, Integer languageId) {
        String periodUnitStr = Util.getPeriodUnitStr(periodUnitId, languageId)
        return periodValue + Constants.SINGLE_SPACE + periodUnitStr;
    }

    def showInvoices () {
        redirect controller: 'invoice', action: 'byProcess', id: params.id, params: [ isReview : params.isReview ]
    }

    def showOrders () {
        redirect controller: 'order', action: 'byProcess', params: [processId: params.id]
    }

    def restart () {
        def processId = params.int('id')

        def process = BillingProcessDTO.get(processId)

        securityValidator.validateCompany(process?.entity?.id, Validator.Type.EDIT)

        billingBatchJobService.restartFailedJobByBillingProcessId(processId, session['company_id'])

        redirect controller: 'billing', action: 'show', id: params.id
    }

    @Secured(["BILLING_80"])
    def approve () {
        try {
            webServicesSession.setReviewApproval(Boolean.TRUE)
        } catch (Exception e) {
            throw new SessionInternalError(e)
        }
        flash.message = 'billing.review.approve.success'
        redirect action: 'list'
    }

    @Secured(["BILLING_80"])
    def disapprove () {
        try {
            webServicesSession.setReviewApproval(Boolean.FALSE)
        } catch (Exception e) {
            throw new SessionInternalError(e)
        }
        flash.message = 'billing.review.disapprove.success'
        redirect action: 'list'
    }
    
    @Secured(["BILLING_1920"])
    def sendInvoiceEmails() {
        def processId = params.int('id')

        def process = BillingProcessDTO.get(processId)

        securityValidator.validateCompany(process?.entity?.id, Validator.Type.EDIT)
        try {
            emailBatchJobService.triggerAsync(processId, session['company_id'])       
        } catch (Exception e) {
            log.error e.getMessage()
            viewUtils.resolveException(flash, session.locale, e);
        }
        chain action: 'show', params: ['id': processId, 'emailJobTriggerred': true]
    }
}
