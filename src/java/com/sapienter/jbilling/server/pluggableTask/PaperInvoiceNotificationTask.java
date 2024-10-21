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

package com.sapienter.jbilling.server.pluggableTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.sf.jasperreports.engine.JasperExportManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.groovy.runtime.InvokerHelper;

import com.lowagie.text.DocumentException;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoiceTemplate.report.InvoiceTemplateBL;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.db.PaperInvoiceBatchDTO;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;

/**
 * @author Emil
 */
public class PaperInvoiceNotificationTask extends PluggableTask implements NotificationTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaperInvoiceNotificationTask.class));
    // pluggable task parameters names
    public static final ParameterDescription PARAMETER_DESIGN =
            new ParameterDescription("design", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_TEMPLATE =
            new ParameterDescription("template", false, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_LANGUAGE_OPTIONAL =
            new ParameterDescription("language", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_SQL_QUERY_OPTIONAL =
            new ParameterDescription("sql_query", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_REMOVE_BLANK_PAGE =
            new ParameterDescription("remove_blank_page", false, ParameterDescription.Type.BOOLEAN);


    //initializer for pluggable params
    public PaperInvoiceNotificationTask() {
        descriptions.add(PARAMETER_DESIGN);
        descriptions.add(PARAMETER_TEMPLATE);
        descriptions.add(PARAMETER_LANGUAGE_OPTIONAL);
        descriptions.add(PARAMETER_SQL_QUERY_OPTIONAL);
        descriptions.add(PARAMETER_REMOVE_BLANK_PAGE);
    }



    private String design;
    private Integer templateId;
    private boolean language;
    private boolean sqlQuery;
    private ContactBL contact;
    private ContactDTOEx to;
    private Integer entityId;
    private InvoiceDTO invoice;
    private ContactDTOEx from;
    private boolean removeBlankPage;

    /* (non-Javadoc)
     * @see com.sapienter.jbilling.server.pluggableTask.NotificationTask#deliver(com.sapienter.betty.interfaces.UserEntityLocal, com.sapienter.betty.server.notification.MessageDTO)
     */
    protected void init(UserDTO user, MessageDTO message)
            throws TaskException {
        design = parameters.get(PARAMETER_DESIGN.getName());

        try {
            invoice = (InvoiceDTO) message.getParameters().get(
                    "invoiceDto");
            invoice = new InvoiceDAS().find(invoice.getId());

            String templateIdStr = parameters.get(PARAMETER_TEMPLATE.getName());
            templateId = ((templateIdStr != null) && !templateIdStr.isEmpty()) ? Integer.valueOf(parameters.get(PARAMETER_TEMPLATE.getName())) : null;
            language = Boolean.valueOf(parameters.get(
                    PARAMETER_LANGUAGE_OPTIONAL.getName()));
            sqlQuery = Boolean.valueOf(parameters.get(
                    PARAMETER_SQL_QUERY_OPTIONAL.getName()));
            removeBlankPage = Boolean.valueOf(parameters.get(
                    PARAMETER_REMOVE_BLANK_PAGE.getName()));

            contact = new ContactBL();
            contact.setInvoice(invoice.getId());
            if (contact.getEntity() != null) {
                to = contact.getDTO();
                if (to.getUserId() == null) {
                    to.setUserId(invoice.getBaseUser().getUserId());
                }
            }

            entityId = user.getEntity().getId();
            contact.setEntity(entityId);
            LOG.debug("Found Entity %s contact %s", entityId, contact.getEntity());
            if (contact.getEntity() != null) {
                from = contact.getDTO();
                LOG.debug("Retrieved entity contact i.e. from %s", from);
                if (from.getUserId() == null) {
                    from.setUserId(new EntityBL().getRootUser(entityId));
                }
                LOG.debug("Entity Contact User ID %s", from.getUserId());
            }
        } catch (Exception e) {
            throw new TaskException(e);
        }
    }

    protected void setDesign(String designName){
        design = designName;
    }

    protected InvoiceDTO getInvoice() {
        return invoice;
    }

    @Override
    public boolean deliver(UserDTO user, MessageDTO message)
            throws TaskException {
        if (!message.getTypeId().equals(MessageDTO.TYPE_INVOICE_PAPER)) {
            // this task is only to notify about invoices
            return false;
        }
        try {
            if(PaperInvoiceBatchBL.isCompileMessage(message)) {
                compileAndSendInvoiceBundle(message);
            } else {
                init(user, message);
                NotificationBL.generatePaperInvoiceAsFile(getDesign(user), sqlQuery,
                        invoice, from, to, message.getContent()[0].getContent(),
                        message.getContent()[1].getContent(), entityId,
                        UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user),
                        user.getPassword(),removeBlankPage());
                // update the batch record
                Integer processId = (Integer) message.getParameters().get(
                        "processId");
                PaperInvoiceBatchBL batchBL = new PaperInvoiceBatchBL();
                PaperInvoiceBatchDTO record = batchBL.createGet(processId);
                record.setTotalInvoices(record.getTotalInvoices() + 1);
                // link the batch to this invoice
                // lock the row, the payment MDB will update too
                InvoiceDTO myInvoice = new InvoiceDAS().findForUpdate(invoice.getId());
                myInvoice.setPaperInvoiceBatch(record);
                record.getInvoices().add(myInvoice);
            }
        } catch (Exception e) {
            throw new TaskException(e);
        }

        return true;
    }

    /**
     * The final message we will get is to create a bundle containing all message and email them
     * @throws InterruptedException
     * @throws IOException
     * @throws DocumentException
     */
    private void compileAndSendInvoiceBundle(MessageDTO message) throws InterruptedException, DocumentException, IOException {
        Integer entity = PaperInvoiceBatchBL.getEntityForBatch(message);
        Integer billingProcessId = PaperInvoiceBatchBL.getBillingProcessForBatch(message);
        LOG.debug("Compiling Invoices :: batch [%s] :: entityId [%s]", billingProcessId, entity);
        Thread.sleep(5000);
        BillingProcessBL process = new BillingProcessBL(billingProcessId);
        PaperInvoiceBatchDTO batch = process.getEntity().getPaperInvoiceBatch();
        LOG.debug("batch: %s", batch);
        if(batch != null && batch.getTotalInvoices() > 0) {
            PaperInvoiceBatchBL batchBl = new PaperInvoiceBatchBL(batch);
            batchBl.compileInvoiceFilesForProcess(entity);
            LOG.debug("Sending Emails");
            // send the file as an attachment
            batchBl.sendEmail();
        }
    }

    public byte[] getPDF(UserDTO user, MessageDTO message) {
        try {
            init(user, message);
            LOG.debug("now message1 = " + message.getContent()[0].getContent());

            if (PreferenceBL.getPreferenceValueAsBoolean(entityId, Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION)) {

                Integer resolveTemplate = this.getTemplateId(user.getCustomer());
                InvoiceTemplateDTO invoiceTemplate = (InvoiceTemplateDTO) InvokerHelper.invokeMethod(InvoiceTemplateDTO.class, "get", resolveTemplate);
                InvoiceTemplateBL.setTemplateVersionForInvoice(invoiceTemplate);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                JasperExportManager.exportReportToPdfStream(InvoiceTemplateBL.createInvoiceTemplateBL(invoiceTemplate, invoice).getJasperPrint(), outputStream);

                return outputStream.toByteArray();
            } else {
                return NotificationBL.generatePaperInvoiceAsStream(this.getDesign(user), sqlQuery, invoice, from, to,
                        message.getContent()[0].getContent(),
                        message.getContent()[1].getContent(), entityId,
                        UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user),
                        user.getPassword(),removeBlankPage());
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String getPDFFile(UserDTO user, MessageDTO message) {
        try {
            init(user, message);
            if (PreferenceBL.getPreferenceValueAsBoolean(entityId, Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION)) {
                InvoiceTemplateDTO invoiceTemplate = (InvoiceTemplateDTO) InvokerHelper.invokeMethod(InvoiceTemplateDTO.class, "get", this.getTemplateId(user.getCustomer()));
                InvoiceTemplateBL.setTemplateVersionForInvoice(invoiceTemplate);
                String filename = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "invoices" + File.separator + user.getEntity().getId() + "-" + invoice.getId() + "-invoice.pdf";

                JasperExportManager.exportReportToPdfFile(InvoiceTemplateBL.createInvoiceTemplateBL(invoiceTemplate, invoice).getJasperPrint(), filename);

                return filename;
            }
            else {
                return NotificationBL.generatePaperInvoiceAsFile(this.getDesign(user), sqlQuery, invoice, from, to,
                        message.getContent()[0].getContent(), message.getContent()[1].getContent(), entityId,
                        UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user),
                        user.getPassword(),removeBlankPage());
            }
        }
        catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public int getSections() {
        return 2;
    }

    @Override
    public List<NotificationMediumType> mediumHandled() {
        return Arrays.asList(NotificationMediumType.PDF);
    }

    private Integer getTemplateId(CustomerDTO customer) {
        Integer invoiceTemplateId = null;

        if (customer != null && customer.getInvoiceTemplate() != null) {
            invoiceTemplateId = customer.getInvoiceTemplate().getId();
        }

        if (invoiceTemplateId == null && customer != null && customer.getAccountType() != null && customer.getAccountType().getInvoiceTemplate() != null) {
            invoiceTemplateId = customer.getAccountType().getInvoiceTemplate().getId();
        }

        if (invoiceTemplateId == null) {
            invoiceTemplateId = templateId;
        }

        if (invoiceTemplateId == null) {
            invoiceTemplateId = InvoiceTemplateBL.getDefaultTemplateId();
        }

        return invoiceTemplateId;
    }

    private String getDesign(UserDTO user) {
        String customerDesign = null;
        if ( null != user.getCustomer()) {

            if ( !StringUtils.isEmpty(user.getCustomer().getInvoiceDesign())) {
                customerDesign = user.getCustomer().getInvoiceDesign();
            } else if (null != user.getCustomer().getAccountType()) {
                customerDesign= user.getCustomer().getAccountType().getInvoiceDesign();
            }
        }

        if (StringUtils.isBlank(customerDesign) && user.getCustomer() != null && user.getCustomer().getAccountType() != null) {
            customerDesign = user.getCustomer().getAccountType().getInvoiceDesign();
        }

        if (StringUtils.isBlank(customerDesign)) {
            customerDesign = design;
        }

        return language ? customerDesign + user.getLanguage().getCode() :  customerDesign;
    }

    /**
     * This method decides if the blank PDF page (with only header and last page footer)
     * will be removed from the generated PDF. The default core plugin does not remove blank PDF page
     * but this can be overriden by the extended classes to return true to remove blank PDF page
     * generated from the invoice design report.
     * @return boolean
     */
    protected boolean removeBlankPage() {
        return removeBlankPage;
    }
}
