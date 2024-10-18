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

package com.sapienter.jbilling.server.provisioning.task;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.provisioning.ClusterAwareProvisioningMDB;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestStatus;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningRequestDAS;
import com.sapienter.jbilling.server.provisioning.event.CommandStatusUpdateEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Date;

/*
 * The configuration needs to be done specifically for each installation/scenario
 * using the file jbilling-jms.xml
 */
@Transactional( propagation = Propagation.NOT_SUPPORTED )
public class ProvisioningCommandsMDB extends ClusterAwareProvisioningMDB {
    private final FormatLogger LOG = new FormatLogger(Logger.getLogger(ProvisioningCommandsMDB.class));

    @Override
    public void doOnMessage(Message message) {
        try {

            LOG.debug("Provisioning command MDB " + " command=" + message.getStringProperty("in_command") + "- entity="
                    + message.getIntProperty("in_entityId") + " - Processing message by  " + this.hashCode());

            MapMessage myMessage            = (MapMessage) message;

            String in_commandId_str = myMessage.getStringProperty("in_commandId");
            Integer in_commandId = null;

            String result = myMessage.getStringProperty("out_result");

            LOG.debug("Message property result value : " + result);

            try {
                in_commandId = Integer.parseInt(in_commandId_str.trim());
            } catch (Exception e) {}

            LOG.debug("Message property in_commandId value : " + in_commandId);

            PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
            TransactionStatus transaction = transactionManager.getTransaction(
                    new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

            try {

                ProvisioningRequestDAS provisioningRequestDAS = new ProvisioningRequestDAS();
                ProvisioningCommandDAS provisioningCommandDAS = new ProvisioningCommandDAS();

                ProvisioningCommandDTO provisioningCommandDTO = provisioningCommandDAS.findNow(in_commandId);

                if (provisioningCommandDTO != null) {

                    Integer requestsCount = provisioningRequestDAS.getRequestsCountByCommandId(in_commandId);

                    Integer successfulRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(in_commandId, ProvisioningRequestStatus.SUCCESSFUL);
                    Integer failedRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(in_commandId, ProvisioningRequestStatus.FAILED);
                    Integer canceledRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(in_commandId, ProvisioningRequestStatus.CANCELLED);
                    Integer unavailableRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(in_commandId, ProvisioningRequestStatus.UNAVAILABLE);
                    Integer rollbackRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(in_commandId, ProvisioningRequestStatus.ROLLBACK);

                    LOG.debug("Number of requests: %s; Succesfull: %s, Failed: %s. Canceled: %s, Unavailable: %s, Rollback: %s",
                            requestsCount, successfulRequestsCount, failedRequestsCount, canceledRequestsCount, unavailableRequestsCount,
                            rollbackRequestsCount);

                    if (requestsCount.equals(successfulRequestsCount))
                        provisioningCommandDTO.setCommandStatus(ProvisioningCommandStatus.SUCCESSFUL);
                    else if (failedRequestsCount > 0)
                        provisioningCommandDTO.setCommandStatus(ProvisioningCommandStatus.FAILED);
                    else if (canceledRequestsCount > 0 || rollbackRequestsCount > 0)
                        provisioningCommandDTO.setCommandStatus(ProvisioningCommandStatus.CANCELLED);
                    else if (unavailableRequestsCount > 0)
                        provisioningCommandDTO.setCommandStatus(ProvisioningCommandStatus.UNAVAILABLE);
                    else {
                        LOG.warn("Provisioning command status unmodified: " + in_commandId);
                    }

                    provisioningCommandDTO.setLastUpdateDate(TimezoneHelper.serverCurrentDate());
                    provisioningCommandDAS.save(provisioningCommandDTO);

                    CommandStatusUpdateEvent newEvent = new CommandStatusUpdateEvent(provisioningCommandDTO.getEntity().getId(), provisioningCommandDTO);
                    EventManager.process(newEvent);
                    LOG.debug("OrExternalProvisioning: generated CommandStatusUpdateEvent for provisioning command %s status update: %s",
                            in_commandId,
                            provisioningCommandDTO.getCommandStatus());

                    transactionManager.commit(transaction);

                }  else {
                    throw new SessionInternalError("Didn't find provisioning command : " + in_commandId);
                }

            } catch (Exception e) {
                LOG.error("2-An exception occurred.", e);
                if(!transaction.isCompleted()){
                    LOG.debug("Transaction not completed, initiate rollback");
                    transactionManager.rollback(transaction);
                }
                throw new SessionInternalError(e);
            }
        } catch (Exception e) {
            throw new SessionInternalError("processing provisioning command", e);
        }
    }
}
