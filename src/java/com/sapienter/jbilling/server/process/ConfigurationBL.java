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

package com.sapienter.jbilling.server.process;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;

public class ConfigurationBL {
    private BillingProcessConfigurationDAS configurationDas = null;
    private BillingProcessConfigurationDTO configuration = null;
    private EventLogger eLogger = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ConfigurationBL.class));

    public ConfigurationBL(Integer entityId)  {
        init();
        configuration = configurationDas.findByEntity(new CompanyDAS().find(entityId));
    }

    public ConfigurationBL() {
        init();
    }

    public ConfigurationBL(BillingProcessConfigurationDTO cfg) {
        init();
        configuration = cfg;
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        configurationDas = new BillingProcessConfigurationDAS();

    }

    public BillingProcessConfigurationDTO getEntity() {
        return configuration;
    }

    public void set(Integer entityId) {
        configuration = configurationDas.findByEntity(new CompanyDAS().find(entityId));
    }

    public Integer createUpdate(Integer executorId,
            BillingProcessConfigurationDTO dto) {
        configuration = configurationDas.findByEntity(dto.getEntity());
        if (configuration != null) {

            if (!configuration.getGenerateReport().equals(
                    dto.getGenerateReport())) {
                eLogger.audit(executorId, null,
                        Constants.TABLE_BILLING_PROCESS_CONFIGURATION,
                        configuration.getId(),
                        EventLogger.MODULE_BILLING_PROCESS,
                        EventLogger.ROW_UPDATED, new Integer(configuration
                                .getGenerateReport()), null, null);
                configuration.setGenerateReport(dto.getGenerateReport());
                configuration
                        .setReviewStatus(dto.getGenerateReport() == 1 ? Constants.REVIEW_STATUS_GENERATED
                                : Constants.REVIEW_STATUS_APPROVED);
            } else {
                eLogger.audit(executorId, null,
                        Constants.TABLE_BILLING_PROCESS_CONFIGURATION,
                        configuration.getId(),
                        EventLogger.MODULE_BILLING_PROCESS,
                        EventLogger.ROW_UPDATED, null, null, null);
            }

            configuration.setNextRunDate(dto.getNextRunDate());
        } else {
            configuration = configurationDas.create(dto.getEntity(), dto
                    .getNextRunDate(), dto.getGenerateReport());
        }

        configuration.setDaysForReport(dto.getDaysForReport());
        configuration.setDaysForRetry(dto.getDaysForRetry());
        configuration.setPeriodUnit(dto.getPeriodUnit());
        configuration.setDueDateUnitId(dto.getDueDateUnitId());
        configuration.setDueDateValue(dto.getDueDateValue());
        configuration.setDfFm(dto.getDfFm());
        configuration.setOnlyRecurring(dto.getOnlyRecurring());
        configuration.setInvoiceDateProcess(dto.getInvoiceDateProcess());
        configuration.setAutoPaymentApplication(dto.getAutoPaymentApplication());
        configuration.setMaximumPeriods(dto.getMaximumPeriods());
        configuration.setLastDayOfMonth(dto.getLastDayOfMonth());
        configuration.setProratingType(dto.getProratingType());
        configuration.setAutoCreditNoteApplication(dto.getAutoCreditNoteApplication());
        configuration.setApplyCreditNotesBeforePayments(dto.getApplyCreditNotesBeforePayments());

        configuration.setAutoPayment(dto.getAutoPayment());
        configuration.setRetryCount(dto.getRetryCount());
        configuration.setSkipEmails(dto.getSkipEmails());
        configuration.setSkipEmailsDays(dto.getSkipEmailsDays());
        return configuration.getId();
    }

    public BillingProcessConfigurationDTO getDTO() {
        LOG.debug("Billing Process Configuration "+ configuration);
        BillingProcessConfigurationDTO dto = new BillingProcessConfigurationDTO();
        dto.setDaysForReport(configuration.getDaysForReport());
        dto.setDaysForRetry(configuration.getDaysForRetry());
        dto.setEntity(configuration.getEntity());
        dto.setGenerateReport(configuration.getGenerateReport());
        dto.setId(configuration.getId());
        dto.setNextRunDate(configuration.getNextRunDate());
        dto.setPeriodUnit(configuration.getPeriodUnit());
        dto.setReviewStatus(configuration.getReviewStatus());
        dto.setDueDateUnitId(configuration.getDueDateUnitId());
        dto.setDueDateValue(configuration.getDueDateValue());
        dto.setDfFm(configuration.getDfFm());
        dto.setOnlyRecurring(configuration.getOnlyRecurring());
        dto.setInvoiceDateProcess(configuration.getInvoiceDateProcess());
        dto.setMaximumPeriods(configuration.getMaximumPeriods());
        dto.setAutoPaymentApplication(configuration.getAutoPaymentApplication());
        dto.setLastDayOfMonth(configuration.getLastDayOfMonth());
        dto.setProratingType(configuration.getProratingType());
        dto.setAutoCreditNoteApplication(configuration.getAutoCreditNoteApplication());
        dto.setApplyCreditNotesBeforePayments(configuration.getApplyCreditNotesBeforePayments());

        dto.setAutoPayment(configuration.getAutoPayment());
        dto.setRetryCount(configuration.getRetryCount());
        dto.setSkipEmails(configuration.getSkipEmails());
        dto.setSkipEmailsDays(configuration.getSkipEmailsDays());
        return dto;
    }

    public void setReviewApproval(Integer executorId, boolean flag) {

        eLogger.audit(executorId, null,
                Constants.TABLE_BILLING_PROCESS_CONFIGURATION, configuration
                        .getId(), EventLogger.MODULE_BILLING_PROCESS,
                EventLogger.ROW_UPDATED, configuration.getReviewStatus(), null,
                null);
        configuration.setReviewStatus(flag ? Constants.REVIEW_STATUS_APPROVED
                : Constants.REVIEW_STATUS_DISAPPROVED);

    }

    /**
     * Convert a given BillingProcessConfigurationDTO into a BillingProcessConfigurationWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static BillingProcessConfigurationWS getWS(BillingProcessConfigurationDTO dto) {
		if (null == dto)
			return null;

		BillingProcessConfigurationWS ws = new BillingProcessConfigurationWS();
		ws.setId(dto.getId());
		ws.setPeriodUnitId(dto.getPeriodUnit() != null ? dto.getPeriodUnit()
				.getId() : null);
		ws.setEntityId(dto.getEntity() != null ? dto.getEntity().getId() : null);
		ws.setNextRunDate(dto.getNextRunDate());
		ws.setGenerateReport(dto.getGenerateReport());
		ws.setRetries(dto.getRetries());
		ws.setDaysForRetry(dto.getDaysForRetry());
		ws.setDaysForReport(dto.getDaysForReport());
		ws.setReviewStatus(dto.getReviewStatus());
		ws.setDueDateUnitId(dto.getDueDateUnitId());
		ws.setDueDateValue(dto.getDueDateValue());
		ws.setDfFm(dto.getDfFm());
		ws.setOnlyRecurring(dto.getOnlyRecurring());
		ws.setInvoiceDateProcess(dto.getInvoiceDateProcess());
		ws.setMaximumPeriods(dto.getMaximumPeriods());
		ws.setAutoPaymentApplication(dto.getAutoPaymentApplication());
		ws.setLastDayOfMonth(dto.getLastDayOfMonth());
        ws.setApplyCreditNotesBeforePayments(dto.getApplyCreditNotesBeforePayments());
        ws.setAutoCreditNoteApplication(dto.getAutoCreditNoteApplication());
		ws.setProratingType(null != dto.getProratingType() ? dto
				.getProratingType().getOptionText() : Constants.BLANK_STRING);
		ws.setAutoPayment(dto.getAutoPayment());
		ws.setRetryCount(dto.getRetryCount());
		ws.setSkipEmails(dto.getSkipEmails());
		ws.setSkipEmailsDays(dto.getSkipEmailsDays());
		return ws;
    }

    /**
     * Convert a given BillingProcessConfigurationWS web-service object into a BillingProcessConfigurationDTO entity.
     *
     * The BillingProcessConfigurationWS must have an entity and period unit ID or an exception will be thrown.
     *
     * @param ws ws object to convert
     * @return converted DTO object
     * @throws SessionInternalError if required field is missing
     */
    public static BillingProcessConfigurationDTO getDTO(BillingProcessConfigurationWS ws) {
        if (ws != null) {

            if (ws.getEntityId() == null)
                    throw new SessionInternalError("BillingProcessConfigurationDTO must have an entity id.");
            
            if (ws.getPeriodUnitId() == null)
            	throw new SessionInternalError("BillingProcessConfigurationDTO must have a period unit id.");
            
            // billing process entity
            CompanyDTO entity = new EntityBL(ws.getEntityId()).getEntity();
            
            // billing process period unit
            PeriodUnitDTO periodUnit = new PeriodUnitDAS().find(ws.getPeriodUnitId());
            
            return new BillingProcessConfigurationDTO(ws, entity, periodUnit);
        }
        return null;
    }

    public static boolean validate(BillingProcessConfigurationWS ws) {
    	boolean retValue = true;
 
    	//validate nextRunDate - Unique if there is already a successful run for that date 
    	//(if a process failed, it is fine to run it again)
    	//TODO Should I Util.truncateDate before using the ws.nextRunDate?
    	BillingProcessDTO billingProcessDTO=new BillingProcessDAS().isPresent(ws.getEntityId(), 0, ws.getNextRunDate()); 
    	if ( billingProcessDTO != null) {
    		for (ProcessRunDTO run: billingProcessDTO.getProcessRuns()) {
    			//if status is not failed i.e. for the same date, if the process is either running or finished
    			if (!Constants.PROCESS_RUN_STATUS_FAILED.equals(run.getStatus().getId()) ) {
    			    LOG.error("Trying to set this configuration: " + ws + " but already has this: " + run);
                    throw new SessionInternalError("There is already a billing process for the give date." + ws.getNextRunDate(),
                            new String[]{"BillingProcessConfigurationWS,nextRunDate,billing.configuration.error.unique.nextrundate,"});
    			}
    		}
    	}
    	
    	ProcessRunDTO run = new ProcessRunDAS().getLatestSuccessful(ws.getEntityId());
    	
    	//The nextRunDate must be greater than the latest successful one
    	if (run != null
            && run.getBillingProcess().getBillingDate() != null
            && !run.getBillingProcess().getBillingDate().before(ws.getNextRunDate())) {

    		LOG.error("Trying to set this configuration: " + ws + " but the it should be in the future " + run.getBillingProcess());
			String messages[] = new String[1];
            messages[0] = new String("BillingProcessConfigurationWS,nextRunDate,"
                                     + "billing.configuration.error.past.nextrundate,"
                                     + run.getBillingProcess().getBillingDate());

            throw new SessionInternalError("The new next date needs to be in the future from the last successful run", messages);
		}
    	if(ws.getAutoPayment() == 1){
    		if(ws.getRetryCount() == null){
    			String messages[] = new String[1];
    			messages[0] = new String("BillingProcessConfigurationWS,retryCount,"
                                         + "billing.configuration.error.empty.retrycount"
                                         );

                SessionInternalError exception = new SessionInternalError("Retry Count is compulsory with Auto Payment option", messages);
    			throw exception;
    		}
    	}
    	return retValue;
    }
    
    private static Calendar getPeriodEndDate(BillingProcessConfigurationWS ws, Date givenDate){
    	Calendar startTime = Calendar.getInstance();
		startTime.setTime(givenDate);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(ws.getNextRunDate());
		startDate.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
		startDate.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));
		
		if(Constants.PERIOD_UNIT_MONTH.equals(ws.getPeriodUnitId())){
			startDate.add(Calendar.MONTH, 1);
		}else if(Constants.PERIOD_UNIT_SEMI_MONTHLY.equals(ws.getPeriodUnitId())){
			startDate.add(Calendar.WEEK_OF_MONTH, 2);
		}else if(Constants.PERIOD_UNIT_WEEK.equals(ws.getPeriodUnitId())){
			startDate.add(Calendar.WEEK_OF_MONTH, 1);
		}else if(Constants.PERIOD_UNIT_YEAR.equals(ws.getPeriodUnitId())){
			startDate.add(Calendar.YEAR, 1);
		}else if(Constants.PERIOD_UNIT_DAY.equals(ws.getPeriodUnitId())){
			startDate.add(Calendar.DAY_OF_MONTH, 1);
		}
    	return startDate;
    }
    public static Calendar getStartDateTime(Date startTime, Date billingDate, Integer interval){
    	Calendar start = Calendar.getInstance();
    	start.setTime(billingDate);
    	Calendar given = Calendar.getInstance();
    	given.setTime(startTime);
    	start.set(Calendar.HOUR_OF_DAY, given.get(Calendar.HOUR_OF_DAY));
    	start.set(Calendar.MINUTE, given.get(Calendar.MINUTE));
    	start.add(Calendar.DAY_OF_MONTH, interval);// 
    	return start;
    }
    
    public static PluggableTaskDTO getBillingTask(Integer entityId){
    	List<PluggableTaskDTO> allTasks = null;
    	List<PluggableTaskDTO> billingTasks = new ArrayList<PluggableTaskDTO>();
    	allTasks = new PluggableTaskBL().findAllByEntityId(entityId);
    	
    	for(PluggableTaskDTO taskDTO: allTasks){
    		if(taskDTO.getType()!= null && taskDTO.getType().getClassName().contains("Billing")){
    			billingTasks.add(taskDTO);
    		}
    	}
    	int order = 9999;
    	PluggableTaskDTO billingTask = null;
    	for(PluggableTaskDTO billingDTO: billingTasks){
    		if(billingDTO.getProcessingOrder() < order){
    			order = billingDTO.getProcessingOrder();
    			billingTask = billingDTO;
    		}
    	}
    	return billingTask;
    }
    
    public static void removeParamValue(PluggableTaskDTO task, String paramName, String value, Integer executorId){
    	for(PluggableTaskParameterDTO param: task.getParameters()){
			if(paramName.equals(param.getName())){
				if(param.getStrValue().contains(value)){
					param.setStrValue(param.getStrValue().replaceAll(value, ""));
					new PluggableTaskBL().update(executorId, task);
				}
				break;
			}
		}
    }

    public static boolean doesBillingProcessHaveAlwaysEnableProrating(Integer entityId) {
        ConfigurationBL configurationBL = new ConfigurationBL(entityId);
        BillingProcessConfigurationDTO billingConfiguration = configurationBL.getDTO();
        return billingConfiguration.getProratingType().isProratingAutoOn();
    }
}
