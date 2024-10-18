package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.EDIFileBL;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.db.*;
import com.sapienter.jbilling.server.ediTransaction.invoiceRead.InvoiceReadTask;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;


public class UpdateEDIStatusProcessTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UpdateEDIStatusProcessTask.class));

    protected static final ParameterDescription METER_READ_STATUS_NAME =
            new ParameterDescription("meter_read_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVOICE_READ_STATUS_NAME =
            new ParameterDescription("invoice_read_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PAYMENT_READ_STATUS_NAME =
            new ParameterDescription("payment_read_status", true, ParameterDescription.Type.STR);

    {
        descriptions.add(METER_READ_STATUS_NAME);
        descriptions.add(INVOICE_READ_STATUS_NAME);
        descriptions.add(PAYMENT_READ_STATUS_NAME);
    }

    private static final Class<Event> events[] = new Class[]{
            UpdateEDIFileStatusEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }


    public enum Type {
        METER_READ, INVOICE_READ, PAYMENT_READ;
    }

    CompanyDTO companyDTO = null;

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (!(event instanceof UpdateEDIFileStatusEvent)) {
            return;
        }
        LOG.debug("Manually updation of edi file status");

        UpdateEDIFileStatusEvent updateEDIFileStatusEvent = (UpdateEDIFileStatusEvent) event;
        EDIFileDTO ediFileDTO = updateEDIFileStatusEvent.getEdiFileDTO();
        companyDTO = new CompanyDAS().find(getEntityId());

        EDITypeDTO meterReadEDIType = findEDIType(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        LOG.debug("Meter read EDI Type : " + meterReadEDIType.getName());
        EDITypeDTO invoiceReadEDIType = findEDIType(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        LOG.debug("Invoice read EDI Type : " + meterReadEDIType.getName());

        EDITypeDTO paymentReadEDIType = findEDIType(FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME);
        LOG.debug("Payment EDI Type : " + meterReadEDIType.getName());

        LOG.debug("Escape Edi file statue : " + updateEDIFileStatusEvent.getEscapeStatus());
        LOG.debug("Edi file statue : " + ediFileDTO.getFileStatus().getName());
        Type type = null;
        if (ediFileDTO.getEdiType().getEdiSuffix().equals(meterReadEDIType.getEdiSuffix()) && ediFileDTO.getFileStatus().getName().equals(getParameter(METER_READ_STATUS_NAME.getName(), ""))) {
            type = Type.METER_READ;
        } else if (ediFileDTO.getEdiType().getEdiSuffix().equals(invoiceReadEDIType.getEdiSuffix()) && ediFileDTO.getFileStatus().getName().equals(getParameter(INVOICE_READ_STATUS_NAME.getName(), ""))) {
            type = Type.INVOICE_READ;
        } else if (ediFileDTO.getEdiType().getEdiSuffix().equals(paymentReadEDIType.getEdiSuffix()) && ediFileDTO.getFileStatus().getName().equals(getParameter(PAYMENT_READ_STATUS_NAME.getName(), ""))) {
            type = Type.PAYMENT_READ;
        } else {
            return;
        }

        //if need to process then find the plugin
        AbstractScheduledTransactionProcessor ediParser = null;
        PluggableTaskManager<IScheduledTask> taskManager = new PluggableTaskManager<IScheduledTask>(ediFileDTO.getEntity().getId(), 22);
        for (IScheduledTask task = taskManager.getNextClass(); task != null; task = taskManager.getNextClass()) {
            if (task instanceof MeterReadParserTask && type == Type.METER_READ) {
                ediParser = ((MeterReadParserTask) task);
                break;
            } else if (task instanceof InvoiceReadTask && type == Type.INVOICE_READ) {
                ediParser = ((InvoiceReadTask) task);
                break;
            }else if(task instanceof PaymentParserTask && type == Type.PAYMENT_READ){
                ediParser = ((PaymentParserTask) task);
                break;
            }
        }

        if (ediParser == null) {
            return;
        }
        // if plugin exist then process the file
        Map<String, String> pluginParameter = new HashMap<String, String>();
        pluginParameter = ediParser.getParameters();
        pluginParameter.put("companyId", ediFileDTO.getEntity().getId() + "");
        ediParser.bindPluginParameter(pluginParameter);
        try {
            LOG.debug("Calling Plugin to update edi file status");
            EDIFileWS ediFileWS = new EDIFileBL(ediFileDTO).getWS();
            ediParser.processFile(ediFileWS, updateEDIFileStatusEvent.getEscapeStatus());
            updateEDIFileStatus(ediFileDTO, ediFileWS);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    private EDITypeDTO findEDIType(String ediTypeMetaFieldName) {
        MetaFieldValue metaFieldValue = companyDTO.getMetaField(ediTypeMetaFieldName);
        if (metaFieldValue == null || metaFieldValue.getValue() == null) {
            throw new SessionInternalError("Configuration issue: no company level meta field for "+ediTypeMetaFieldName);
        }

        Integer metaFieldVal = (Integer) metaFieldValue.getValue();
        EDITypeDTO ediTypeDTO = new EDITypeDAS().find((Integer) metaFieldValue.getValue());

        if (ediTypeDTO == null) {
            throw new SessionInternalError("No EDI Type found for id " + metaFieldVal);
        }

        return ediTypeDTO;
    }

    private void updateEDIFileStatus(EDIFileDTO ediFileDTO, EDIFileWS ediFileWS) {
        ediFileDTO.setComment(ediFileWS.getComment());

        EDIFileStatusDTO updatedStatus = new EDIFileStatusDAS().find(ediFileWS.getEdiFileStatusWS().getId());
        if(updatedStatus==null){
            throw new SessionInternalError("EDI File status not found for id "+ediFileWS.getEdiFileStatusWS().getId());
        }
        ediFileDTO.setFileStatus(updatedStatus);

        if (ediFileWS.getExceptionCode()!=null) {
            EDIFileExceptionCodeDTO exceptionCodeDTO = new EDIFileExceptionCodeDAS().findExceptionCodeByStatus(ediFileWS.getExceptionCode(), updatedStatus.getId());
            if (exceptionCodeDTO == null) {
                ediFileDTO.setExceptionCode(null);
            }
            ediFileDTO.setExceptionCode(exceptionCodeDTO);
        }
    }

}
