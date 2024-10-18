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

import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.common.SessionInternalError;

/**
* BillingController
*
* @author Vikas Bodani
* @since 11/01/11
*/
@Secured(["MENU_99"])
class BillingconfigurationController {

	static scope = "prototype"
	IWebServicesSessionBean webServicesSession
	def viewUtils
	def recentItemService
	def breadcrumbService
    
    def index () {

		breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
		def configuration= webServicesSession.getBillingProcessConfiguration()
		boolean isBillingRunning= webServicesSession.isBillingRunning(webServicesSession.getCallerCompanyId())
		if (params['alreadyRunning']?.toBoolean()) {
			flash.error = 'prompt.billing.already.running'
			flash.info = 'prompt.billing.running'
		} else {
			if (isBillingRunning) {
				flash.info = 'prompt.billing.running'
			} else {
				if (params['isFutureRunAttempt']?.toBoolean()) {
					flash.error = 'billing.cannot.be.run.for.future.date'
				} else if ( params['trigger']?.toBoolean()) {
					flash.error = 'prompt.billing.trigger.fail'
				}
			}
		}
		[configuration:configuration, isBillingRunning: isBillingRunning]
	}

	@RequiresValidFormToken
	def saveConfig () {

		log.info "${params}"
		def configuration= new BillingProcessConfigurationWS() 
		bindData(configuration, params)

		//set all checkbox values as int
		configuration.setGenerateReport params.generateReport ? 1 : 0
		configuration.setInvoiceDateProcess params.invoiceDateProcess ? 1 : 0 
		configuration.setOnlyRecurring params.onlyRecurring ? 1 : 0
		configuration.setAutoPaymentApplication params.autoPaymentApplication ? 1 : 0
		configuration.setAutoCreditNoteApplication params.autoCreditNoteApplication ? 1 : 0
		configuration.setApplyCreditNotesBeforePayments params.applyCreditNotesBeforePayments ? 1 : 0
		configuration.setAutoPayment params.autoPayment ? 1 : 0
		if(configuration.autoPayment == 1){
			configuration.setRetryCount params.retryCount ? params.retryCount.toInteger() : 0
		}
		//configuration.setNextRunDate (new SimpleDateFormat("dd-MMM-yyyy").parse(params.nextRunDate) )
		configuration.setEntityId webServicesSession.getCallerCompanyId()
        configuration.setSkipEmails params.skipEmails ? 1 : 0
        
        if(0 == configuration.skipEmails) {
            configuration.setSkipEmailsDays ''
        } else {
            configuration.setSkipEmailsDays params.skipEmailsDays
        } 
		log.info "Generate Report ${params.generateReport}"
		
		try {
			webServicesSession.createUpdateBillingProcessConfiguration(configuration)
			flash.message = 'billing.configuration.save.success'
		} catch (SessionInternalError e){
			viewUtils.resolveException(flash, session.locale, e);
		} catch (Exception e) {
			log.info e.getMessage()
			flash.error = 'billing.configuration.save.fail'
		}
		
		chain action: 'index'
	}
	
	def runBilling () {
		def alreadyRunning= false
		def isFutureRunAttempt = false
		def configuration= webServicesSession.getBillingProcessConfiguration()
		
		//Check billing Run Date cannot be in future.
		if(configuration.nextRunDate.after(TimezoneHelper.currentDateForTimezone(session['company_timezone']))) {
			isFutureRunAttempt = true
		}
		try {
			if (!webServicesSession.isBillingRunning(webServicesSession.getCallerCompanyId())) {
				webServicesSession.triggerBillingAsync(TimezoneHelper.currentDateForTimezone(session['company_timezone']))
				//flash.message = 'prompt.billing.trigger'
			} else {
				flash.error = 'prompt.billing.already.running'
				alreadyRunning= true
			}
		} catch (Exception e) {
			log.error e.getMessage()
			viewUtils.resolveException(flash, session.locale, e);
		}

		chain action: 'index', params: ['trigger': true, 'alreadyRunning': alreadyRunning, 'isFutureRunAttempt': isFutureRunAttempt]
	}
}
