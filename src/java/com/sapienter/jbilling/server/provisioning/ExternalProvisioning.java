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

package com.sapienter.jbilling.server.provisioning;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

import com.sapienter.jbilling.server.provisioning.db.*;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.log4j.Logger;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.provisioning.config.Command;
import com.sapienter.jbilling.server.provisioning.config.Field;
import com.sapienter.jbilling.server.provisioning.config.Processor;
import com.sapienter.jbilling.server.provisioning.config.Provisioning;
import com.sapienter.jbilling.server.provisioning.config.Request;
import com.sapienter.jbilling.server.provisioning.task.IExternalProvisioning;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Logic for external provisioning module. Receives a command from the
 * commands rules task via JMS. The configuration file 
 * jbilling-provisioning.xml is used to map this command to command
 * strings for specific external provisioning processors. Publishes 
 * results in a JMS topic.
 */
public class ExternalProvisioning implements IRequestProvisioningCallback {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(
            ExternalProvisioning.class));

    private MapMessage message;
    private ProvisioningRequestDAS provisioningRequestDAS = new ProvisioningRequestDAS();
    private ProvisioningCommandDAS provisioningCommandDAS = new ProvisioningCommandDAS();

    /**
     * Receives and processes a command message from the command rules task.
     * This method is called through the ProvisioningProcessSessionBean
     * so that it runs in a transaction. ExternalProvisioningMDB is
     * the class that actually receives the message.
     */
    public void onMessage(Message myMessage) {
        try {
            message = (MapMessage) myMessage;

            Provisioning config = (Provisioning) Context.getBean(
                    Context.Name.PROVISIONING);

            List<Command> commandsConfig = config.getCommands();
            String command = message.getStringProperty("command");

            // find command config
            for(Command commandConfig : commandsConfig) {
                if (command.equals(commandConfig.getId())) {
                    LOG.debug("Found a command configuration for command: " + command);
                    processCommand(commandConfig);
                    break; // no more configurations for this command?
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Processes a command according to the given configuration.
     */
    private void processCommand(Command config) 
            throws JMSException, PluggableTaskException {
        // process fields
        List<Field> fieldConfig = config.getFields();
        Map<String, String> fields = new HashMap<String, String>();

        for (Field field : fieldConfig) {
            String value = message.getStringProperty(field.getName());
            if (value == null) {
                value = field.getDefaultValue();
            }
            fields.put(field.getName(), value);
        }
        LOG.debug("Externalprovisioning.ProcessCommand()-> List of Command Fields:");
        LOG.debug(fields);

        // call each configured processor
        for (Processor processor : config.getProcessors()) {
            PluggableTaskManager<IExternalProvisioning> taskManager = new
                    PluggableTaskManager<IExternalProvisioning>(
                    message.getIntProperty("entityId"), 
                    Constants.PLUGGABLE_TASK_EXTERNAL_PROVISIONING);
            IExternalProvisioning task = taskManager.getNextClass();

            while (task != null) {
                if (task.getId().equals(processor.getId())) {
                    callProcessor(task, processor, fields, 
                            message.getStringProperty("id"));

                    break;
                }
                task = taskManager.getNextClass();
            }

            if (task == null) {
                throw new SessionInternalError("Couldn't find external " +
                        "provisioining task with id: " + processor.getId());
            }
        }
    }

    /**
     * Processes each request to the given external provisioning task
     * as specified by the processor configuration. 
     */
    private void callProcessor(IExternalProvisioning task, 
            Processor processor, Map<String, String> fields, String id) 
            throws JMSException {
        List<Request> requests = processor.getRequests();
        Collections.sort(requests); // sort by order

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        TransactionStatus transaction = transactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

        ProvisioningCommandDTO provisioningCommandDTO = provisioningCommandDAS.findNow(message.getIntProperty("commandId"));

        if (provisioningCommandDTO == null)
        {
            throw new SessionInternalError("Didn't find provisioning command : " + message.getIntProperty("commandId"));
        }

        List<ProvisioningRequestDTO> provisioingRequests = new ArrayList<ProvisioningRequestDTO>();

        for (Request request : requests) {
            LOG.debug("Submit string pattern: " + request.getSubmit());

            UUID uid = UUID.randomUUID();

            ProvisioningRequestDTO provisioningRequestDTO = new ProvisioningRequestDTO();
            provisioningRequestDTO.setIdentifier(uid.toString());
            provisioningRequestDTO.setProvisioningCommand(provisioningCommandDTO);
            provisioningRequestDTO.setProcessor(processor.getId());
            provisioningRequestDTO.setExecutionOrder(request.getOrder());
            provisioningRequestDTO.setCreateDate(TimezoneHelper.serverCurrentDate());
            provisioningRequestDTO.setSubmitRequest(request.getSubmit());
            provisioningRequestDTO.setContinueOnType(request.getContinueOnType());
            provisioningRequestDTO.setRollbackRequest(request.getRollback());
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.SUBMITTED);
            provisioningRequestDTO = provisioningRequestDAS.save(provisioningRequestDTO);
            provisioingRequests.add(provisioningRequestDTO);
        }

        for (ProvisioningRequestDTO request : provisioingRequests) {

            // insert fields into submit string
            StringBuilder submit = new StringBuilder(request.getSubmitRequest());
            boolean keepLooking = true;
            while (keepLooking) {
                int barStartIndex = submit.indexOf("|");
                int barEndIndex = submit.indexOf("|", barStartIndex + 1);
                if (barStartIndex == -1) {
                    keepLooking = false;
                } else if (barEndIndex == -1) {
                    throw new SessionInternalError("Mismatched '|' in submit " +
                            "string. Index: " + barStartIndex);
                } else {
                    String fieldName = submit.substring(barStartIndex + 1,
                            barEndIndex);
                    String fieldValue = fields.get(fieldName);
                    LOG.debug("Replacing field name '" + fieldName +
                            "' with value '" + fieldValue + "'");

                    submit.replace(barStartIndex, barEndIndex + 1, fieldValue);
                }
            }
            String submitString = submit.toString();
            LOG.debug("Command string: " + submitString);

            // call external provisioning processor task
            String result = null;
            try {
                request.setSubmitDate(TimezoneHelper.serverCurrentDate());
                request.setSubmitRequest(submitString);
                request = provisioningRequestDAS.save(request);
                result = task.sendRequest(request.getIdentifier(), submitString, this);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                LOG.error("External provisioning processor error: " +
                        e.getMessage() + "\n" + sw.toString());

                postErrorResult(request.getIdentifier(), e.getMessage());
            }

            // only continue with other requests if correct result
            String continueOnType = request.getContinueOnType();
            if (continueOnType != null && (result == null ||
                    !result.equals(continueOnType))) {
                LOG.debug("Skipping other results.");
                break;
            }
        }

        TransactionStatus innerTransaction = transactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

        try {
            provisioningCommandDTO.setCommandStatus(ProvisioningCommandStatus.PROCESSED);
            provisioningCommandDTO.setLastUpdateDate(TimezoneHelper.serverCurrentDate());
            provisioningCommandDAS.save(provisioningCommandDTO);
            transactionManager.commit(innerTransaction);
        } catch (Exception e) {
            LOG.error("An exception occurred.", e);
            if (!innerTransaction.isCompleted()) {
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(innerTransaction);
            }
            throw new SessionInternalError(e);
        }

        if (!transaction.isCompleted()) {

            try {
                transactionManager.commit(transaction);
            } catch (Exception e) {
                LOG.error("An exception occurred.", e);
                if (!transaction.isCompleted()) {
                    LOG.debug("Transaction not completed, initiate rollback");
                    transactionManager.rollback(transaction);
                }
                throw new SessionInternalError(e);
            }
        }
    }

    private void postErrorResult(String id, final String error) {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("result", "fail");

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        TransactionStatus transaction = transactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

        if (updateProvisioningRequest(id, results)) {

            try {

                transactionManager.commit(transaction);
            } catch (Exception e) {
                LOG.error("2-An exception occurred.", e);
                if(!transaction.isCompleted()){
                    LOG.debug("Transaction not completed, initiate rollback");
                    transactionManager.rollback(transaction);
                }
                throw new SessionInternalError(e);
            }

            JmsTemplate jmsTemplate = (JmsTemplate) Context.getBean(
                    Context.Name.JMS_TEMPLATE);

            Destination destination = (Destination) Context.getBean(
                    Context.Name.PROVISIONING_COMMANDS_REPLY_DESTINATION);

            jmsTemplate.send(destination, new MessageCreator() {
                public Message createMessage(Session session)
                        throws JMSException {
                    MapMessage replyMessage = session.createMapMessage();

                    // add the original properties (names prefixed with 'in_')
                    Enumeration originalPropNames = message.getPropertyNames();
                    while (originalPropNames.hasMoreElements()) {
                        String propName = (String) originalPropNames.nextElement();
                        Object propValue = message.getObjectProperty(propName);
                        replyMessage.setObjectProperty("in_" + propName, propValue);
                    }

                    // there was an error
                    replyMessage.setStringProperty("out_result", "unavailable");
                    replyMessage.setStringProperty("exception", error);

                    return replyMessage;
                }
            });
        }
    }

    private boolean updateProvisioningRequest(String id, final Map<String, Object> result) {
        ProvisioningRequestDTO provisioningRequestDTO = provisioningRequestDAS.findByIdentifier(id);
        if (provisioningRequestDTO == null) {
            throw new SessionInternalError("Didn't find provisioning request : " + id);
        }

        provisioningRequestDTO.setResultReceivedDate(TimezoneHelper.serverCurrentDate());

        if (result.get("result").toString().equals("success") || result.get("result").toString().equals("successful"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.SUCCESSFUL);
        else if (result.get("result").toString().equals("fail") || result.get("result").toString().equals("failed"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.FAILED);
        else if (result.get("result").toString().equals("submit") || result.get("result").toString().equals("submitted"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.SUBMITTED);
        else if (result.get("result").toString().equals("cancel") || result.get("result").toString().equals("canceled"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.CANCELLED);
        else if (result.get("result").toString().equals("retry"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.RETRY);
        else if (result.get("result").toString().equals("rollback"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.ROLLBACK);
        else if (result.get("result").toString().equals("unavailable"))
            provisioningRequestDTO.setRequestStatus(ProvisioningRequestStatus.UNAVAILABLE);

        provisioningRequestDTO.setResultMap((Map) result);
        provisioningRequestDAS.save(provisioningRequestDTO);
        Integer commandId = provisioningRequestDTO.getProvisioningCommand().getId();

        LOG.debug("Command: %s, Provisioning request id: %s status: %s", commandId, provisioningRequestDTO.getId(), provisioningRequestDTO.getRequestStatus());

        Integer retryRequestsCount = provisioningRequestDAS.getRequestByCommandIdAndStatus(commandId, ProvisioningRequestStatus.RETRY);
        List<ProvisioningRequestDTO> requests = provisioningRequestDAS.findUnprocessedRequestsByCommandId(commandId);
        LOG.debug("Retry requests: %s; Unprocessed requests: %s", retryRequestsCount, requests.size());

        List<ProvisioningRequestDTO> commandRequests = provisioningRequestDAS.findRequestsByCommandId(commandId);
        for (ProvisioningRequestDTO request : commandRequests) {
            LOG.debug("Request: %s; Status: %s", request.getId(), request.getRequestStatus());
        }


        return requests.size() == 0 && retryRequestsCount == 0;
    }

    /**
     * Posts results of external provisioning processing tasks.
     */
    @Override
    public void postResult(String id, final Map<String, Object> result) {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        TransactionStatus transaction = transactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

        if (updateProvisioningRequest(id, result)) {
            try {

                transactionManager.commit(transaction);
            } catch (Exception e) {
                LOG.error("2-An exception occurred.", e);
                if(!transaction.isCompleted()){
                    LOG.debug("Transaction not completed, initiate rollback");
                    transactionManager.rollback(transaction);
                }
                throw new SessionInternalError(e);
            }

            JmsTemplate jmsTemplate = (JmsTemplate) Context.getBean(
                    Context.Name.JMS_TEMPLATE);

            Destination destination = (Destination) Context.getBean(
                    Context.Name.PROVISIONING_COMMANDS_REPLY_DESTINATION);

            jmsTemplate.send(destination, new MessageCreator() {
                public Message createMessage(Session session)
                        throws JMSException {
                    MapMessage replyMessage = session.createMapMessage();

                    // add the original properties (names prefixed with 'in_')
                    Enumeration originalPropNames = message.getPropertyNames();
                    while (originalPropNames.hasMoreElements()) {
                        String propName = (String) originalPropNames.nextElement();
                        Object propValue = message.getObjectProperty(propName);
                        replyMessage.setObjectProperty("in_" + propName, propValue);
                    }

                    // add the properties returned by the processor
                    Set<Map.Entry<String, Object>> entrySet =
                            result.entrySet();
                    for (Map.Entry<String, Object> entry : entrySet) {
                        replyMessage.setObjectProperty("out_" + entry.getKey(),
                                entry.getValue());
                    }

                    return replyMessage;
                }
            });
        }
    }
}
