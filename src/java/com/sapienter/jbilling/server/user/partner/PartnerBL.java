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

package com.sapienter.jbilling.server.user.partner;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.InvoiceCommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionExceptionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionLineDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionValueDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerReferralCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerReferralCommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.ReferralCommissionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.user.PartnerSQL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayoutDAS;
import com.sapienter.jbilling.server.user.partner.task.IPartnerCommissionTask;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Emil
 */
public class PartnerBL extends ResultList implements PartnerSQL {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PartnerBL.class));

    private PartnerDAS partnerDAS = null;
    private PartnerCommissionDAS partnerCommissionDAS = null;
    private PartnerReferralCommissionDAS partnerReferralCommissionDAS = null;
    private CommissionDAS commissionDAS = null;

    private PartnerDTO partner = null;
    private PartnerPayout payout = null;
    private EventLogger eLogger = null;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public PartnerBL(Integer partnerId) {
        init();
        set(partnerId);
    }

    public PartnerBL() {
        init();
    }

    public PartnerBL(PartnerDTO entity) {
        partner = entity;
        init();
    }

    public PartnerBL(boolean init) {
        if(init) {
            init();
        }
    }

    public void set(Integer partnerId) {
        partner = partnerDAS.find(partnerId);
    }

    public void setPayout(Integer payoutId) {
        payout = new PartnerPayoutDAS().find(payoutId);
    }

    public void setPartnerDAS(PartnerDAS partnerDAS) {
        this.partnerDAS = partnerDAS;
    }

    public void setPartnerCommissionDAS(PartnerCommissionDAS partnerCommissionDAS) {
        this.partnerCommissionDAS = partnerCommissionDAS;
    }

    public void setPartnerReferralCommissionDAS(PartnerReferralCommissionDAS partnerReferralCommissionDAS) {
        this.partnerReferralCommissionDAS = partnerReferralCommissionDAS;
    }

    public void setCommissionDAS(CommissionDAS commissionDAS) {
        this.commissionDAS = commissionDAS;
    }

    public void setELogger(EventLogger eLogger) {
        this.eLogger = eLogger;
    }

    private void init() {
        if(eLogger == null) eLogger = EventLogger.getInstance();
        payout = null;
        if(partnerDAS == null ) partnerDAS = new PartnerDAS();
        if(partnerCommissionDAS == null) partnerCommissionDAS = new PartnerCommissionDAS();
        if(partnerReferralCommissionDAS == null) partnerReferralCommissionDAS = new PartnerReferralCommissionDAS();
        if(commissionDAS == null) commissionDAS = new CommissionDAS();
    }

    public PartnerDTO getEntity() {
        return partner;
    }

    public Integer create(PartnerDTO dto) throws SessionInternalError {
        LOG.debug("creating partner");

        dto.setTotalPayments(BigDecimal.ZERO);
        dto.setTotalPayouts(BigDecimal.ZERO);
        dto.setTotalRefunds(BigDecimal.ZERO);
        dto.setDuePayout(BigDecimal.ZERO);
        partner = partnerDAS.save(dto);

        LOG.debug("created partner id " + partner.getId());

        return partner.getId();
    }

    public void update(Integer executorId, PartnerDTO dto) {
        dto.getBaseUser();
        dto.getBaseUser().getId();
        partner.getId();
        eLogger.audit(executorId, dto.getBaseUser().getId(),
                Constants.TABLE_PARTNER, partner.getId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null,
                null);

        // update meta fields and run validations
        partner.updateMetaFieldsWithValidation(dto.getBaseUser().getCompany().getLanguageId(), dto.getBaseUser().getCompany().getId(),
                null, dto);
    }


    /**
     * This will return the id of the lates payout that was successfull
     * @param partnerId
     * @return
     * @throws NamingException
     * @throws SQLException
     */
    private Integer getLastPayout(Integer partnerId)
            throws NamingException, SQLException {
        Integer retValue = null;
        Connection conn = ((DataSource) Context.getBean(Context.Name.DATA_SOURCE)).getConnection();
        PreparedStatement stmt = conn.prepareStatement(lastPayout);
        stmt.setInt(1, partnerId.intValue());
        ResultSet result = stmt.executeQuery();
        // since esql doesn't support max, a direct call is necessary
        if (result.next()) {
            retValue = new Integer(result.getInt(1));
        }
        result.close();
        stmt.close();
        conn.close();
        LOG.debug("Finding last payout ofr partner " + partnerId + " result = " + retValue);
        return retValue;
    }

    private int getCustomersCount()
            throws SQLException, NamingException {
        int retValue = 0;
        Connection conn = ((DataSource) Context.getBean(Context.Name.DATA_SOURCE)).getConnection();
        PreparedStatement stmt = conn.prepareStatement(countCustomers);
        stmt.setInt(1, partner.getId());
        ResultSet result = stmt.executeQuery();
        // since esql doesn't support max, a direct call is necessary
        if (result.next()) {
            retValue = result.getInt(1);
        }
        result.close();
        stmt.close();
        conn.close();
        return retValue;
    }

	public static final PartnerPayoutWS getPartnerPayoutWS(PartnerPayout dto) {

		PartnerPayoutWS ws = new PartnerPayoutWS();
		ws.setId(dto.getId());
		ws.setPartnerId(dto.getPartner() != null ? dto.getPartner().getId()
				: null);
		ws.setPaymentId(dto.getPayment() != null ? dto.getPayment().getId()
				: null);
		ws.setStartingDate(dto.getStartingDate());
		ws.setEndingDate(dto.getEndingDate());
		ws.setPaymentsAmount(dto.getPaymentsAmount());
		ws.setRefundsAmount(dto.getRefundsAmount());
		ws.setBalanceLeft(dto.getBalanceLeft());
		return ws;
	}
 
    public PartnerDTO getDTO() {
        return partner;
    }

    public PartnerPayout getLastPayoutDTO(Integer partnerId)
            throws SQLException, NamingException {
        PartnerPayout retValue = null;

        Integer payoutId = getLastPayout(partnerId);
        if (payoutId != null && payoutId.intValue() != 0) {
            payout = new PartnerPayoutDAS().find(payoutId);
            retValue = getPayoutDTO();
        }
        return retValue;
    }

    public PartnerPayout getPayoutDTO()
            throws NamingException {
        payout.touch();
        return payout;
    }
    
	public static final PartnerReferralCommissionWS getPartnerReferralCommissionWS(
			PartnerReferralCommissionDTO dto) {

		PartnerReferralCommissionWS ws = new PartnerReferralCommissionWS();
		ws.setId(dto.getId());
		ws.setReferralId(dto.getReferral() != null ? dto.getReferral().getId()
				: null);
		ws.setReferrerId(dto.getReferrer() != null ? dto.getReferrer().getId()
				: null);
		ws.setStartDate(dto.getStartDate());
		ws.setEndDate(dto.getEndDate());
		ws.setPercentage(dto.getPercentage());
		ws.setOwningUserId(getOwningUserId(ws));
		return ws;
	}
	
	 private static final Integer getOwningUserId (PartnerReferralCommissionWS ws) {
	        if (ws.getReferralId() != null && ws.getReferralId() > 0) {
	            return new PartnerBL(ws.getReferralId()).getEntity().getBaseUser().getId();
	        } else {
	            return null;
	        }
	    }
	
	public static final PartnerReferralCommissionDTO getDTO(PartnerReferralCommissionWS ws){
	        PartnerReferralCommissionDTO referralCommission = new PartnerReferralCommissionDTO();
	        referralCommission.setId(0);
	        referralCommission.setReferral(new PartnerDAS().find(ws.getReferralId()));
	        referralCommission.setReferrer(new PartnerDAS().find(ws.getReferrerId()));
	        referralCommission.setStartDate(ws.getStartDate());
	        referralCommission.setEndDate(ws.getEndDate());
	        referralCommission.setPercentage(ws.getPercentageAsDecimal());
	        return referralCommission;
	    }

    

    public CachedRowSet getList(Integer entityId)
            throws SQLException, Exception{

        prepareStatement(PartnerSQL.list);
        cachedResults.setInt(1,entityId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getPayoutList(Integer partnerId)
            throws SQLException, Exception{

        prepareStatement(PartnerSQL.listPayouts);
        cachedResults.setInt(1, partnerId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Deletes the composed Partner object from the system
     * by first deleting the associated user and then deleting the Partner record.
     * @param executorId
     * @throws SessionInternalError
     */
    public void delete(Integer executorId) throws SessionInternalError {
        validateDelete();
        Integer userId= partner.getBaseUser().getId();
        Integer partnerId=partner.getId();

        partner.setDeleted(1);

        UserBL userBl= new UserBL(userId);
        userBl.delete(executorId);
        
        for (CustomerDTO customer : partner.getCustomers()) {
        	customer.getPartners().remove(partner);
        }
        
        partner.getCustomers().clear();

        if (executorId != null) {
            eLogger.audit(executorId, userId, Constants.TABLE_BASE_USER,
                    partnerId, EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ROW_DELETED, null, null, null);
        }
    }

    private void validateDelete() {
        if (partner == null) {
            throw new SessionInternalError("The partner has to be set before delete");
        }

        List<Integer> activePartnerIds = new ArrayList<>();
        for (PartnerDTO child : partner.getChildren()) {
            if(child.getDeleted() == 0) {
                activePartnerIds.add(child.getId());
            }
        }

        if(!activePartnerIds.isEmpty()) {
            LOG.debug("Partner Id %s cannot be deleted. Child agents exists.", partner.getId());
            String errorMessages[] = new String[1];
            errorMessages[0] = "PartnerWS,childIds,partner.error.parent.cannot.be.deleted," + activePartnerIds;
            throw new SessionInternalError("Cannot delete Parent Partner. Child ID(s) " + activePartnerIds +" exists.", errorMessages);
        }
    }

    /**
     * This method triggers the commission process calculation.
     * @param entityId
     */
    public void calculateCommissions(Integer entityId){
        CommissionProcessConfigurationDTO configuration = new CommissionProcessConfigurationDAS().findByEntity(new CompanyDAS().find(entityId));
        if (configuration == null) {
            throw new SessionInternalError("Error: No configuration for this company",
                    new String[] { "partner.error.commissionProcess.no.configForEntity" });
        }

        try {
            PluggableTaskManager taskManager = new PluggableTaskManager(
                    entityId,
                    Constants.PLUGGABLE_TASK_PARTNER_COMMISSION);
            IPartnerCommissionTask task = (IPartnerCommissionTask) taskManager.getNextClass();
            if (task != null){
                task.calculateCommissions(entityId);
            } else {
                throw new SessionInternalError("Plugglable Task is not configured",
                                    new String[] {"partner.error.commissionProcess.no.configForPlugin"});
            }
        }catch (PluggableTaskException e){
            LOG.fatal("Problems handling partner commission task.", e);
            throw new SessionInternalError("Problems handling partner commission task.");
        }
    }

    /**
     * Convert a given Partner into a PartnerWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
	public static final PartnerWS getWS(PartnerDTO dto) {

		if (null == dto)
			return null;

		PartnerWS ws = new PartnerWS();

		ws.setId(dto.getId());
		ws.setUserId(dto.getUser() != null ? dto.getUser().getId() : null);
		ws.setTotalPayments(dto.getTotalPayments());
		ws.setTotalRefunds(dto.getTotalRefunds());
		ws.setTotalPayouts(dto.getTotalPayouts());
		ws.setDuePayout(dto.getDuePayout());
        ws.setBrokerId(dto.getBrokerId());
        ws.setDeleted(dto.getDeleted());

		// partner payouts
		ws.setPartnerPayouts(new ArrayList<PartnerPayoutWS>(dto
				.getPartnerPayouts().size()));
		for (PartnerPayout payout : dto.getPartnerPayouts())
			ws.getPartnerPayouts().add(PartnerBL.getPartnerPayoutWS(payout));

		// partner customer ID's
		ws.setCustomerIds(new ArrayList<Integer>(dto.getCustomers().size()));
		for (CustomerDTO customer : dto.getCustomers())
			ws.getCustomerIds().add(customer.getId());

		if (dto.getType() != null) {
			ws.setType(dto.getType().name());
		}

		ws.setParentId(dto.getParent() != null ? dto.getParent().getId() : null);

		ws.setChildIds(new Integer[dto.getChildren().size()]);
		int index = 0;
		for (PartnerDTO partner : dto.getChildren()) {
			ws.getChildIds()[index] = partner.getId();
			index++;
		}

		ws.setCommissions(new CommissionWS[dto.getCommissions().size()]);
		index = 0;
		for (CommissionDTO commission : dto.getCommissions()) {
			ws.getCommissions()[index] = CommissionProcessConfigurationBL
					.getCommissionWS(commission);
			index++;
		}

		ws.setCommissionExceptions(new PartnerCommissionExceptionWS[dto
				.getCommissionExceptions().size()]);
		index = 0;
		for (PartnerCommissionExceptionDTO commissionException : dto
				.getCommissionExceptions()) {
			ws.getCommissionExceptions()[index] = CommissionProcessConfigurationBL
					.getPartnerCommissionExceptionWS(commissionException);
			index++;
		}

		ws.setReferralCommissions(new PartnerReferralCommissionWS[dto
				.getReferralCommissions().size()]);
		index = 0;
		for (PartnerReferralCommissionDTO referralCommission : dto
				.getReferralCommissions()) {
			ws.getReferralCommissions()[index] = getPartnerReferralCommissionWS(referralCommission);
			index++;
		}

		ws.setReferrerCommissions(new PartnerReferralCommissionWS[dto
				.getReferrerCommissions().size()]);
		index = 0;
		for (PartnerReferralCommissionDTO referrerCommission : dto
				.getReferrerCommissions()) {
			ws.getReferrerCommissions()[index] = getPartnerReferralCommissionWS(referrerCommission);
			index++;
		}

        PartnerCommissionValueWS[] commissionValueWSes = new PartnerCommissionValueWS[dto
                .getCommissionValues().size()];
        ws.setCommissionValues(commissionValueWSes);
        index = 0;
        for (PartnerCommissionValueDTO commissionValueDTO : dto
                .getCommissionValues()) {
            commissionValueWSes[index++] = getCommissionValue(commissionValueDTO);
        }

		if (dto.getCommissionType() != null) {
			ws.setCommissionType(dto.getCommissionType().name());
		}
		return ws;
	}

    private static PartnerCommissionValueWS getCommissionValue(PartnerCommissionValueDTO commissionValueDTO) {
        PartnerCommissionValueWS valueWS = new PartnerCommissionValueWS();
        valueWS.setDays(commissionValueDTO.getDays());
        valueWS.setRate(commissionValueDTO.getRate().toString());
        return valueWS;
    }


    public static final Integer[] getPartnerIds(Collection<PartnerDTO> partners) {
        if(partners == null || partners.isEmpty()) {
            return new Integer[0];
        }

        Integer[] partnerIds = new Integer[partners.size()];
        int idx = 0;
        for(PartnerDTO partner : partners) {
            partnerIds[idx++] = partner.getId();
        }
        return partnerIds;
    }

    public static final PartnerDTO getPartnerDTO(PartnerWS ws) {

		PartnerDTO partner = null;
		if (ws.getId() != null) {
			PartnerBL partnerBl = new PartnerBL(ws.getId());
			partner = partnerBl.getEntity();
		} else {
			partner = new PartnerDTO();
			partner.setId(0);
		}

		if (null != ws.getUserId() && ws.getUserId().intValue() > 0) {
			partner.setBaseUser(new UserDAS().find(ws.getUserId()));
		}

        partner.setBrokerId(ws.getBrokerId());
		partner.setTotalPayments(ws.getTotalPaymentsAsBigDecimal());
		partner.setTotalRefunds(ws.getTotalRefundsAsDecimal());
		partner.setTotalPayouts(ws.getTotalPayoutsAsDecimal());
		partner.setDuePayout(ws.getDuePayoutAsDecimal());
		partner.setType(PartnerType.valueOf(ws.getType()));
		partner.setParent(new PartnerDAS().find(ws.getParentId()));

        //update the partner commission values
        List<PartnerCommissionValueDTO> commissionValueDTOs = partner.getCommissionValues();
        PartnerCommissionValueWS[] commissionValueWSes = ws.getCommissionValues();

        //update persistent values with ws values for the min length of the arrays
        int maxValueIdx = Integer.min(partner.getCommissionValues().size(), ws.getCommissionValues().length);
        for(int i=0; i<maxValueIdx; i++) {
            PartnerCommissionValueDTO valueDTO = commissionValueDTOs.get(i);
            valueDTO.setDays(commissionValueWSes[i].getDays());
            valueDTO.setRate(new BigDecimal(commissionValueWSes[i].getRate()));
        }

        //remove extra persistent values
        while(maxValueIdx < commissionValueDTOs.size()) {
            commissionValueDTOs.remove(maxValueIdx);
        };

        //add additional values to the persistent list
        for(int i=maxValueIdx; i<commissionValueWSes.length; i++) {
            PartnerCommissionValueDTO valueDTO = new PartnerCommissionValueDTO();
            valueDTO.setDays(commissionValueWSes[i].getDays());
            valueDTO.setRate(new BigDecimal(commissionValueWSes[i].getRate()));
            valueDTO.setPartner(partner);
            commissionValueDTOs.add(valueDTO);
        }

		if (ws.getCommissions() != null) {
			partner.getCommissions().clear();
			for (CommissionWS commissionWS : ws.getCommissions()) {
				CommissionDTO cm = CommissionProcessConfigurationBL
						.getDTO(commissionWS);
				cm.setPartner(partner);
				partner.getCommissions().add(cm);
			}
		}

		if (ws.getCommissionExceptions() != null) {
			partner.getCommissionExceptions().clear();
			for (PartnerCommissionExceptionWS commissionExceptionWS : ws
					.getCommissionExceptions()) {
				PartnerCommissionExceptionDTO commissionException = CommissionProcessConfigurationBL
						.getDTO(commissionExceptionWS);
				commissionException.setPartner(partner);
				partner.getCommissionExceptions().add(commissionException);
			}
		}

		if (ws.getReferralCommissions() != null) {
			partner.getReferralCommissions().clear();
			for (PartnerReferralCommissionWS referralCommissionWS : ws
					.getReferralCommissions()) {
				PartnerReferralCommissionDTO referralCommission = PartnerBL
						.getDTO(referralCommissionWS);
				referralCommission.setReferral(partner);
				partner.getReferralCommissions().add(referralCommission);
			}
		}

		if (ws.getReferrerCommissions() != null) {
			partner.getReferrerCommissions().clear();
			for (PartnerReferralCommissionWS referrerCommissionWS : ws
					.getReferrerCommissions()) {
				PartnerReferralCommissionDTO referrerCommission = PartnerBL
						.getDTO(referrerCommissionWS);
				referrerCommission.setReferrer(partner);
				partner.getReferrerCommissions().add(referrerCommission);
			}
		}

		if (StringUtils.isBlank(ws.getCommissionType())) {
			partner.setCommissionType(null);
		} else {
			partner.setCommissionType(PartnerCommissionType.valueOf(ws
					.getCommissionType()));
		}

		return partner;
	}

    /**
     * The method will calculate all the referral commission that must be paid for the other types of commissions in this run. When the method gets
     * called all other commission types must already be calculated.
     *
     * @param commissionProcessRun
     */
    public void calculateReferralCommissions(CommissionProcessRunDTO commissionProcessRun){
        List<PartnerReferralCommissionDTO> referralCommissions = partnerReferralCommissionDAS.findAllForCompany(commissionProcessRun.getEntity().getId());
        calculateReferralCommissions(commissionProcessRun, referralCommissions);
    }

    /**
     * The method will calculate all the referral commission that must be paid for the other types of commissions in this run. When the method gets
     * called all other commission types must already be calculated.
     *
     * @param commissionProcessRun
     * @param referralCommissions - only this collection of referral commissions will be checked.
     */
    public void calculateReferralCommissions(CommissionProcessRunDTO commissionProcessRun, Collection<PartnerReferralCommissionDTO> referralCommissions){
        LOG.debug("Started calculating the referral commissions.");

        for(PartnerReferralCommissionDTO referralCommission : referralCommissions){
            PartnerDTO referral = referralCommission.getReferral(); //the one that gives.

            BigDecimal referralAmount = BigDecimal.ZERO;

            List<InvoiceCommissionDTO> invoiceCommissions = partnerCommissionDAS.findInvoiceCommissionsByPartnerAndProcessRun(referral, commissionProcessRun);
            for(InvoiceCommissionDTO invoiceCommission : invoiceCommissions){
                if(isCommissionValid(referralCommission.getStartDate(), referralCommission.getEndDate(),
                        invoiceCommission.getInvoice().getCreateDatetime()) && referralCommission.getPercentage() != null) {
                    BigDecimal percentage = referralCommission.getPercentage().divide(ONE_HUNDRED);
                    //standard commission
                    if(invoiceCommission.getStandardAmount() != null) {
                        referralAmount = referralAmount.add(percentage.multiply(invoiceCommission.getStandardAmount()));
                    }
                    if(invoiceCommission.getMasterAmount() != null) {
                        referralAmount = referralAmount.add(percentage.multiply(invoiceCommission.getMasterAmount()));
                    }
                    if(invoiceCommission.getExceptionAmount() != null) {
                        referralAmount = referralAmount.add(percentage.multiply(invoiceCommission.getExceptionAmount()));
                    }
                }
            }

            PartnerDTO referrer = referralCommission.getReferrer(); //the one that receives.

            if(referralAmount != null && !referralAmount.equals(BigDecimal.ZERO)){
                ReferralCommissionDTO referralCommissionDTO = new ReferralCommissionDTO();
                referralCommissionDTO.setPartner(referrer);
                referralCommissionDTO.setReferralPartner(referral);
                referralCommissionDTO.setCommissionProcessRun(commissionProcessRun);
                referralCommissionDTO.setReferralAmount(referralAmount);
                partnerCommissionDAS.save(referralCommissionDTO);
                LOG.debug("Created referral invoice commission object, referrerPartner: %s, referralPartner: %s, amount: %s", referrer.getId(), referral.getId(), referralAmount);
            }
        }
    }

    /**
     * When the method gets called all the PartnerCommissionLineDTO objects must already exist for the commission run.
     * It will go through commission lines and calculate the total commission for each partner.
     *
     * @param commissionProcessRun
     */
    public void calculateCommissions(CommissionProcessRunDTO commissionProcessRun){
        LOG.debug("Started calculating the commissions.");
        List<PartnerDTO> partners = partnerDAS.findPartnersByCompany(commissionProcessRun.getEntity().getId());
        for(PartnerDTO partner : partners){
            List<PartnerCommissionLineDTO> partnerCommissions = partnerCommissionDAS.findByPartnerAndProcessRun(partner, commissionProcessRun);

            BigDecimal standardAmountTotal = BigDecimal.ZERO;
            BigDecimal masterAmountTotal = BigDecimal.ZERO;
            BigDecimal referralAmountTotal = BigDecimal.ZERO;
            BigDecimal exceptionAmountTotal = BigDecimal.ZERO;
            BigDecimal customerAmountTotal = BigDecimal.ZERO;

            CurrencyDTO invoiceCurrency = null;
            CurrencyDTO agentCurrency = null;
            CurrencyDTO referralCurrency = null;

            for(PartnerCommissionLineDTO partnerCommission : partnerCommissions) {

                try {
                    agentCurrency = partner.getBaseUser().getCurrency();
                } catch(Exception e) {
                    LOG.info("Cannot get currency from invoice/agent");
                }

                if(partnerCommission.getType().equals(PartnerCommissionLineDTO.Type.INVOICE)) {
                    InvoiceCommissionDTO invoiceCommission = (InvoiceCommissionDTO)partnerCommission;
                    invoiceCurrency = invoiceCommission.getInvoice().getCurrency();

                    BigDecimal standardAmount = convertToCurrency(invoiceCommission.getStandardAmount(),invoiceCurrency,agentCurrency, commissionProcessRun.getEntity().getId());
                    BigDecimal masterAmount = convertToCurrency(invoiceCommission.getMasterAmount(),invoiceCurrency,agentCurrency, commissionProcessRun.getEntity().getId());
                    BigDecimal exceptionAmount = convertToCurrency(invoiceCommission.getExceptionAmount(),invoiceCurrency,agentCurrency, commissionProcessRun.getEntity().getId());

                    standardAmountTotal = standardAmountTotal.add(standardAmount);
                    masterAmountTotal = masterAmountTotal.add(masterAmount);
                    exceptionAmountTotal = exceptionAmountTotal.add(exceptionAmount);

                    //update the invoice commision with the amounts converted to corresponding currency
                    invoiceCommission.setStandardAmount(standardAmount);
                    invoiceCommission.setMasterAmount(masterAmount);
                    invoiceCommission.setExceptionAmount(exceptionAmount);
                } else if(partnerCommission.getType().equals(PartnerCommissionLineDTO.Type.REFERRAL)) {
                    ReferralCommissionDTO referralCommission = (ReferralCommissionDTO) partnerCommission;
                    referralCurrency = referralCommission.getReferralPartner().getBaseUser().getCurrency();
                    BigDecimal referralAmount = convertToCurrency(referralCommission.getReferralAmount(),invoiceCurrency, referralCurrency, commissionProcessRun.getEntity().getId());
                    referralCommission.setReferralAmount(referralAmount);
                    referralAmountTotal = referralAmountTotal.add(referralAmount);
                } else if(partnerCommission.getType().equals(PartnerCommissionLineDTO.Type.CUSTOMER)) {
                    CustomerCommissionDTO customerCommission = (CustomerCommissionDTO)partnerCommission;
                    CurrencyDTO customerCurrency = customerCommission.getUser().getCurrency();

                    BigDecimal customerAmount = convertToCurrency(customerCommission.getAmount(), customerCurrency, agentCurrency, commissionProcessRun.getEntity().getId());
                    customerAmountTotal = customerAmountTotal.add(customerAmount);
                    //update the invoice commision with the amounts converted to corresponding currency
                    customerCommission.setAmount(customerAmount);
                }
                partnerCommissionDAS.reattach(partnerCommission);
            }

            BigDecimal commissionAmount = standardAmountTotal.add(masterAmountTotal).add(exceptionAmountTotal).add(referralAmountTotal).add(customerAmountTotal);

            if(commissionAmount != null && !commissionAmount.equals(BigDecimal.ZERO)){
                PartnerDTO parent = (partner.getParent() != null) ? partner.getParent() : partner;

                CommissionDTO commission = new CommissionDTO();
                commission.setPartner(parent);
                commission.setType(determineCommissionType(standardAmountTotal, masterAmountTotal, exceptionAmountTotal, referralAmountTotal, customerAmountTotal));
                commission.setAmount(commissionAmount);
                commission.setCommissionProcessRun(commissionProcessRun);
                commission.setCurrency(parent.getBaseUser().getCurrency());

                LOG.debug("Commission created, partner: %s, amount: %s, type: %s", partner.getId(), commissionAmount, commission.getType());

                commission = commissionDAS.save(commission);

                for(PartnerCommissionLineDTO partnerCommissionDTO : partnerCommissions){
                    partnerCommissionDTO.setCommission(commission);
                }

            }
        }
    }

    /**
     * This method determines the commissionType according to which type of commission contributed the most.
     * @param standardAmount
     * @param masterAmount
     * @param exceptionAmount
     * @param referralAmount
     * @return CommissionType
     */
    public static CommissionType determineCommissionType(BigDecimal standardAmount, BigDecimal masterAmount,
                                                   BigDecimal exceptionAmount, BigDecimal referralAmount, BigDecimal customerAmount){

        List<BigDecimal> amounts = Arrays.asList(standardAmount.abs(), masterAmount.abs(), exceptionAmount.abs(), referralAmount.abs(), customerAmount.abs());

        BigDecimal max = Collections.max(amounts);

        if(standardAmount.abs().equals(max)){
            return CommissionType.DEFAULT_STANDARD_COMMISSION;
        }else if(masterAmount.abs().equals(max)){
            return CommissionType.DEFAULT_MASTER_COMMISSION;
        }else if(customerAmount.abs().equals(max)){
            return CommissionType.CUSTOMER_COMMISSION;
        }else if(exceptionAmount.abs().equals(max)){
            return CommissionType.EXCEPTION_COMMISSION;
        }else{
            return CommissionType.REFERRAL_COMMISSION;
        }
    }

    /**
     * Convert the given amount, expresed in current currency to the target currency.
     * @param currentAmount
     * @param currentCurrency
     * @param targetCurrency
     * @return
     */
    public static BigDecimal convertToCurrency(BigDecimal currentAmount, CurrencyDTO currentCurrency, CurrencyDTO targetCurrency, Integer entityId) {
        if(currentCurrency!=null && targetCurrency!=null) {
            return new CurrencyBL().convert(currentCurrency.getId(), targetCurrency.getId(), currentAmount, TimezoneHelper.companyCurrentDate(entityId), entityId);
        }
        else return currentAmount;
    }

    /**
     * Determines if the commission is valid for the given period.
     * @param commissionStart
     * @param commissionEnd
     * @param invoiceDate
     * @return
     */
    public static boolean isCommissionValid(Date commissionStart, Date commissionEnd, Date invoiceDate) {
        if(commissionEnd == null){
            return (commissionStart.compareTo(invoiceDate) <= 0);
        }
        else {
            return (commissionStart.compareTo(invoiceDate)<=0)
                    && (commissionEnd.compareTo(invoiceDate)>=0);
        }
    }

    public void reverseCommissions(List<? extends PartnerCommissionLineDTO> linesToReverse, Integer entityId) {
        if(linesToReverse.isEmpty()) {
            return;
        }

        CompanyDAS companyDAS=new CompanyDAS();
        //Create the commissionProcessRun object and save it.
        CommissionProcessRunDTO commissionProcessRun = new CommissionProcessRunDTO();
        commissionProcessRun.setEntity(companyDAS.find(entityId));
        commissionProcessRun.setRunDate(TimezoneHelper.companyCurrentDate(entityId));
        CommissionProcessRunDAS commissionProcessRunDAS=new CommissionProcessRunDAS();

        commissionProcessRun = commissionProcessRunDAS.save(commissionProcessRun);

        List<PartnerReferralCommissionDTO> referralCommissionDTOs = new ArrayList<>();

        for(PartnerCommissionLineDTO commissionLine : linesToReverse) {
            PartnerCommissionLineDTO reversal = commissionLine.createReversal();
            commissionLine.setReversal(reversal);
            reversal.setOriginalCommissionLine(commissionLine);
            reversal.setCommissionProcessRun(commissionProcessRun);
            referralCommissionDTOs.addAll(reversal.getPartner().getReferralCommissions());
        }


       calculateReferralCommissions(commissionProcessRun, referralCommissionDTOs);

       calculateCommissions(commissionProcessRun);
    }
}
