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

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.sql.rowset.CachedRowSet;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunStatusDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunStatusDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunTotalDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunTotalDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunTotalPmDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunTotalPmDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDTO;

public class BillingProcessRunBL  extends ResultList implements ProcessSQL {
    private ProcessRunDAS processRunDas = null;
    private ProcessRunUserDAS processRunUserDas = null;
    private ProcessRunTotalDAS processRunTotalDas = null;
    private ProcessRunTotalPmDAS billingProcessRunTotalPmDas = null;
    private ProcessRunDTO billingProcessRun = null;
    
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BillingProcessRunBL.class));

    public class DateComparator implements Comparator {

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            ProcessRunDTO a = (ProcessRunDTO) o1;
            ProcessRunDTO b = (ProcessRunDTO) o2;
        
            if (a.getStarted().after(b.getStarted())) {
                return 1;
            } else if (a.getStarted().before(b.getStarted())) {
                return -1;
            }
            return 0;
        }

    }
    
    public BillingProcessRunBL(Integer billingProcessRunId) {
        init();
        set(billingProcessRunId);
    }
    
    public BillingProcessRunBL() {
        init();
    }
    
    public BillingProcessRunBL(ProcessRunDTO run)  {
        init();
        billingProcessRun = run;
    }
    
    private void init() {
        processRunDas = new ProcessRunDAS();
        processRunTotalDas = new ProcessRunTotalDAS();
        billingProcessRunTotalPmDas = new ProcessRunTotalPmDAS();
        processRunUserDas = new ProcessRunUserDAS();
    }

    public ProcessRunDTO getEntity() {
        return billingProcessRun;
    }
    
    public void set(Integer id) {
        billingProcessRun = processRunDas.find(id);
    }

    /**
     * Finds the run based on the process id. Assumes that there is
     * only one run associated with the process.
     * This method is called asynch by the MDBs.
     * @param id
     */
    public void setProcess(Integer id) {
        BillingProcessBL bl = new BillingProcessBL(id);
        if (bl.getEntity().getProcessRuns().size() != 1) {
            throw new SessionInternalError("Process " + id +
                        " should have 1 run. It has " + bl.getEntity().getProcessRuns().size());
        } else {
            billingProcessRun = bl.getEntity().getProcessRuns().iterator().next();
        }
    }
    
    public Integer create(BillingProcessDTO process, Date runDate) {
        if (runDate == null) {
            throw new SessionInternalError("run date can't be null");
        }

        billingProcessRun = processRunDas.create(process, runDate, 0,
                new ProcessRunStatusDAS().find(Constants.PROCESS_RUN_STATUS_RINNING));
        return billingProcessRun.getId();
    }


    public CachedRowSet findSucceededUsersList() throws SQLException {
        prepareStatement(ProcessSQL.findSucceededUsers);
        cachedResults.setInt(1, billingProcessRun.getId());
        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet findFailedUsersList() throws SQLException {
        prepareStatement(ProcessSQL.findFailedUsers);
        cachedResults.setInt(1, billingProcessRun.getId());
        execute();
        conn.close();
        return cachedResults;
    }

    public void notifyProcessRunFailure(Integer entityId, int failedUsersCount)
            throws SessionInternalError {

        LOG.debug("Sending process run failure notification.");
        try {
            new BillingProcessDAS().reset();

            ProcessRunDTO processRunDTO = processRunDas.find(billingProcessRun.getId());
            String[] params = new String[] {
                entityId.toString(),
                processRunDTO.getStarted().toString(),
                processRunDTO.getFinished().toString(),
                Integer.toString(failedUsersCount)
            };

            NotificationBL.sendSapienterEmail(entityId, "process.run_failed", null, params);

        } catch (MessagingException e) {
            LOG.warn("Could not send email.", e);
        } catch (IOException e) {
            LOG.warn("Could not send email.", e);
        }
    }    
    
    /**
     * Adds the payment total to the run totals
     * @param currencyId
     * @param methodId
     * @param total
     * @param ok
     */
    public void updateNewPayment(Integer currencyId, Integer methodId,
            BigDecimal total, boolean ok) {
        // update the payments total
        ProcessRunTotalDTO totalRow = findOrCreateTotal(currencyId);

        BigDecimal tmpValue = null;
        if (ok) {
            totalRow.setTotalPaid(totalRow.getTotalPaid().add(total));

            // the payment is good, update the method total as well
            ProcessRunTotalPmDTO pm = findOrCreateTotalPM(methodId, totalRow);
            pm.setTotal(pm.getTotal().add(total));

            // link it to the payment method table
            PaymentMethodDAS paymentMethodHome = new PaymentMethodDAS();
            pm.setPaymentMethod(paymentMethodHome.find(methodId));
        } else {
            totalRow.setTotalNotPaid(totalRow.getTotalNotPaid().add(total));
        }
    }

    
    /**
     * Adds an invoice to the run totals
     */
    public void updateTotals(Integer billingProcessId) {

        for (Object listObj: new BillingProcessDAS().getCountAndSum(billingProcessId)) {
            Object[] row = (Object[]) listObj;
            // add the total to the total invoiced
            ProcessRunTotalDTO totalRow = findOrCreateTotal((Integer) row[2]);

            billingProcessRun.setInvoicesGenerated(billingProcessRun.getInvoicesGenerated() + ((Long) row[0]).intValue());
            totalRow.setTotalInvoiced(((BigDecimal) row[1]));
            LOG.debug("updating invoice run total version " + totalRow.getVersionNum());
        }
    }

    private ProcessRunTotalDTO findOrCreateTotal(Integer currencyId) {
        ProcessRunTotalDTO ret = processRunTotalDas.getByCurrency(billingProcessRun, currencyId);
        
        if (ret == null) { // not present for this currency
            ret = processRunTotalDas.create(billingProcessRun, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, currencyId);
        }
        return ret;
    }

    private ProcessRunTotalPmDTO findOrCreateTotalPM(Integer methodId, ProcessRunTotalDTO total) {
        ProcessRunTotalPmDTO ret = billingProcessRunTotalPmDas.getByMethod(methodId, total);

        if (ret == null) { // not present for this currency
            ret = billingProcessRunTotalPmDas.create(BigDecimal.ZERO);
            // link it to the total row
            total.getTotalsPaymentMethod().add(ret);
            ret.setProcessRunTotal(total);
        }
        return ret;
    }

    // called when the run is over, to update the dates only
    public void updateFinished(Integer processRunStatusId) {
        // get the very latest version
        billingProcessRun = processRunDas.findForUpdate(billingProcessRun.getId());
        billingProcessRun.setFinished(Calendar.getInstance().getTime());
        billingProcessRun.setStatus(new ProcessRunStatusDAS().find(processRunStatusId));
        LOG.debug("updating run " + billingProcessRun.getId() +" version " + billingProcessRun.getVersionNum());
        billingProcessRun = processRunDas.save(billingProcessRun);
    }
    
    public void updatePaymentsFinished() {
        // get the very latest version
        billingProcessRun = processRunDas.findForUpdate(billingProcessRun.getId());
        billingProcessRun.setPaymentFinished(Calendar.getInstance().getTime());
        LOG.debug("updating payments run " + billingProcessRun.getId() +" version " + billingProcessRun.getVersionNum());
        billingProcessRun = processRunDas.save(billingProcessRun);
    }
    

    public ProcessRunUserDTO addProcessRunUser(Integer userId, Integer status) {

        ProcessRunUserDTO processRunUser = processRunUserDas.getUser(billingProcessRun.getId(), userId);
        if(processRunUser == null) {
            return processRunUserDas.create(billingProcessRun.getId(), userId,
                    status, Calendar.getInstance().getTime());
        }

        processRunUser.setStatus(status);
        return processRunUser;
    }
    
    public BillingProcessRunDTOEx getDTO(Integer language) {

        BillingProcessRunDTOEx dto = new BillingProcessRunDTOEx();
        
        dto.setId(billingProcessRun.getId());
        dto.setFinished(billingProcessRun.getFinished());
        dto.setInvoicesGenerated(billingProcessRun.getInvoicesGenerated());
        dto.setStarted(billingProcessRun.getStarted());
        dto.setRunDate(billingProcessRun.getRunDate());
        dto.setPaymentFinished(billingProcessRun.getPaymentFinished());
        ProcessRunStatusDTO statusRow = new ProcessRunStatusDAS().find(billingProcessRun.getStatus().getId());
        dto.setStatusStr(statusRow.getDescription(language));
        ProcessRunUserDAS processRunUserDAS = new ProcessRunUserDAS();
        dto.setUsersSucceeded(processRunUserDAS.findSuccessfullUsersCount(billingProcessRun.getId()));
        dto.setUsersFailed(processRunUserDAS.findFailedUsersCount(billingProcessRun.getId()));        
        // now the totals
        if (!billingProcessRun.getProcessRunTotals().isEmpty()) {
            for (Iterator tIt = billingProcessRun.getProcessRunTotals().iterator(); 
                    tIt.hasNext();) {
                ProcessRunTotalDTO totalRow = 
                        (ProcessRunTotalDTO) tIt.next();
                BillingProcessRunTotalDTOEx totalDto = getTotalDTO(totalRow,
                        language);
                dto.getTotals().add(totalDto);
            }
        }
 
        return dto;
    }
    
    public BillingProcessRunTotalDTOEx getTotalDTO(
            ProcessRunTotalDTO row, Integer languageId) {
        BillingProcessRunTotalDTOEx retValue = 
                new BillingProcessRunTotalDTOEx();
        retValue.setCurrency(row.getCurrency());
        retValue.setId(row.getId());
        retValue.setTotalInvoiced(row.getTotalInvoiced());
        retValue.setTotalNotPaid(row.getTotalNotPaid());
        retValue.setTotalPaid(row.getTotalPaid());
        
        // now go over the totals by payment method
        Hashtable totals = new Hashtable();
        for (Iterator it = row.getTotalsPaymentMethod().iterator(); 
                it.hasNext();) {
            ProcessRunTotalPmDTO pmTotal =
                    (ProcessRunTotalPmDTO) it.next();
            totals.put(pmTotal.getPaymentMethod().getDescription(languageId),
                    pmTotal.getTotal());
                    
        }
        retValue.setPmTotals(totals);
        
        // add the currency name, it's handy on the client side
        CurrencyBL currency = new CurrencyBL(retValue.getCurrency().getId());
        retValue.setCurrencyName(currency.getEntity().getDescription(
                languageId));
        
        return retValue;
    }

    public void updatePaymentsStatistic(Integer runId) {
        BillingProcessRunBL run = new BillingProcessRunBL(runId);
        for (Object invoicePymObj: new BillingProcessDAS().getSuccessfulProcessCurrencyMethodAndSum(run.getEntity().getBillingProcess().getId())) {
            Object[] row= (Object[]) invoicePymObj;
            run.updateNewPayment((Integer) row[0], (Integer) row[1], (BigDecimal) row[2], true);
        }

        for (Object unpayedObj: new BillingProcessDAS().getFailedProcessCurrencyAndSum(run.getEntity().getBillingProcess().getId())) {
            Object[] row = (Object[]) unpayedObj;
            run.updateNewPayment((Integer) row[0], null, (BigDecimal) row[1], false);
        }
    }

    public List<Integer> findSuccessfullUsers() {
        return new ProcessRunUserDAS().findSuccessfullUserIds(billingProcessRun.getId());
    }

    public boolean isUserAlreadySuccessful(Integer userId) { 
    	return new ProcessRunUserDAS().findIfUserIsSuccessful(billingProcessRun.getId(), userId);
    }
    
    public ProcessStatusWS getBillingProcessStatus(Integer entityId) {
        ProcessRunDTO processRunDTO = new ProcessRunDAS().getLatest(entityId);
        if (processRunDTO == null) {
            return null;
        } else {
            ProcessStatusWS result = new ProcessStatusWS();
            result.setStart(processRunDTO.getStarted());
            result.setProcessId(processRunDTO.getBillingProcess().getId());

            if (processRunDTO.getFinished() == null) {
                result.setState(ProcessStatusWS.State.RUNNING);
            } else if (processRunDTO.getStatus().getId() == Constants.PROCESS_RUN_STATUS_RINNING) {
                result.setState(ProcessStatusWS.State.RUNNING);
            } else if (processRunDTO.getStatus().getId() == Constants.PROCESS_RUN_STATUS_FAILED) {
                result.setState(ProcessStatusWS.State.FAILED);
                result.setEnd(processRunDTO.getFinished());
            } else {
                result.setState(ProcessStatusWS.State.FINISHED);
                result.setEnd(processRunDTO.getFinished());
            }
            return result;
        }
    }
}
