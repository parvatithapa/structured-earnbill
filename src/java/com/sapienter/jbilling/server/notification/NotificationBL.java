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

package com.sapienter.jbilling.server.notification;

import static com.sapienter.jbilling.common.Util.getSysProp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolboxFactory;
import org.apache.velocity.tools.config.EasyFactoryConfiguration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.lowagie.text.pdf.PdfWriter;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceLineComparator;
import com.sapienter.jbilling.server.invoice.PaperInvoiceNotificationPlugin;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoiceTemplate.report.FormatUtil;
import com.sapienter.jbilling.server.invoiceTemplate.report.InvoiceTemplateBL;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.db.NotificationMessageDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO;
import com.sapienter.jbilling.server.notification.db.NotificationMessageLineDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageLineDTO;
import com.sapienter.jbilling.server.notification.db.NotificationMessageSectionDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageSectionDTO;
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.pluggableTask.NotificationTask;
import com.sapienter.jbilling.server.pluggableTask.PaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.event.CustomEmailTokenEvent;
import com.sapienter.jbilling.server.process.event.CustomInvoiceFieldsEvent;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.CancellationRequestBL;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.LogoType;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;
import grails.util.Holders;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.ExporterInputItem;
import net.sf.jasperreports.export.OutputStreamExporterOutput;
import net.sf.jasperreports.export.ReportExportConfiguration;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import pl.allegro.finance.tradukisto.MoneyConverters;
import org.joda.time.DateTime;


public class NotificationBL extends ResultList implements NotificationSQL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NotificationMessageDAS messageDas;
    private NotificationMessageDTO messageRow;
    private NotificationMessageSectionDAS messageSectionHome;
    private NotificationMessageLineDAS messageLineHome;

    private static CustomerDAS customerDAS = null;
    private static final String BASE_DIR = getSysProp("base_dir");
    private static final String DESIGNS_FOLDER =  BASE_DIR + "designs/";
    private static final String DESIGN_PATH =  DESIGNS_FOLDER + "%s.jasper";
    private static final BigDecimal HUNDRED = new BigDecimal(100);
    private boolean forceNotification = false;
    protected static final Comparator<AgeingEntityStepDTO> ByDays = (AgeingEntityStepDTO s1, AgeingEntityStepDTO s2) -> s1.getDays() - s2.getDays();

    public NotificationBL(Integer messageId)  {
        init();
        messageRow = messageDas.find(messageId);
    }

    public NotificationBL() {
        init();
    }

    private void init() {

        messageDas = new NotificationMessageDAS();
        messageSectionHome = new NotificationMessageSectionDAS();
        messageLineHome = new NotificationMessageLineDAS();
        customerDAS = new CustomerDAS();
    }

    public void setForceNotification(boolean forceNotification) {
        this.forceNotification = forceNotification;
    }

    public NotificationMessageDTO getEntity() {
        return messageRow;
    }

    public void set(Integer type, Integer languageId, Integer entityId) {
        messageRow = messageDas.findIt(type, entityId, languageId);
        if(messageRow == null){
            messageRow = new NotificationMessageDTO();
        }
    }

    public MessageDTO getDTO() throws SessionInternalError {
        MessageDTO retValue = new MessageDTO();

        retValue.setLanguageId(messageRow.getLanguage().getId());
        retValue.setTypeId(messageRow.getNotificationMessageType().getId());
        retValue.setUseFlag(new Boolean(messageRow.getUseFlag() == 1));

        setContent(retValue);

        return retValue;
    }

    public Integer createUpdate(Integer entityId, MessageDTO dto) {
        for (MessageSection section : dto.getContent()) {
            try {
                parseParameters(section.getContent(), dto.getParameters());
            } catch (SessionInternalError sie) {
                throw sie;
            }
        }

        if (dto.getIncludeAttachment() == 1 && !new File(String.format(DESIGN_PATH, dto.getAttachmentDesign())).exists()) {
            throw new SessionInternalError("Attachment Design not exist",
                    new String[] { "notification.enter.design.not.exists" });
        }

        set(dto.getTypeId(), dto.getLanguageId(), entityId);
        // it's just so easy to delete cascade and recreate ...:D
        if (messageRow != null) {
            messageDas.delete(messageRow);
        }

        messageRow = messageDas.create(dto.getTypeId(), entityId, dto
                .getLanguageId(), dto.getUseFlag());

        // add the sections with the lines to the message entity
        for (int f = 0; f < dto.getContent().length; f++) {
            MessageSection section = dto.getContent()[f];

            // create the section bean
            NotificationMessageSectionDTO sectionBean = messageSectionHome
                    .create(section.getSection());
            int index = 0;
            while (index < section.getContent().length()) {
                String line;
                if (index + MessageDTO.LINE_MAX.intValue() <= section
                        .getContent().length()) {
                    line = section.getContent().substring(index,
                            index + MessageDTO.LINE_MAX.intValue());
                } else {
                    line = section.getContent().substring(index);
                }
                index += MessageDTO.LINE_MAX.intValue();

                NotificationMessageLineDTO nml = messageLineHome.create(line);
                nml.setNotificationMessageSection(sectionBean);
                sectionBean.getNotificationMessageLines().add(nml);

            }
            sectionBean.setNotificationMessage(messageRow);

            messageRow.getNotificationMessageSections().add(sectionBean);

        }

        Set msjs = messageRow
                .getNotificationMessageSections();
        NotificationMessageSectionDTO nnnn = ((NotificationMessageSectionDTO) msjs
                .toArray()[0]);
        Set nm = nnnn.getNotificationMessageLines();

        messageRow.setIncludeAttachment(dto.getIncludeAttachment());
        messageRow.setAttachmentType(dto.getAttachmentType());
        messageRow.setAttachmentDesign(dto.getAttachmentDesign());

        messageRow.setNotifyAdmin(dto.getNotifyAdmin());
        messageRow.setNotifyPartner(dto.getNotifyPartner());
        messageRow.setNotifyParent(dto.getNotifyParent());
        messageRow.setNotifyAllParents(dto.getNotifyAllParents());
        messageRow.setMediumTypes(dto.getMediumTypes());
        messageDas.save(messageRow);

        return messageRow.getId();
    }

    /*
     * Getters. These provide easy generation of messages by their type. So each
     * getter kows which type will generate, and gets as parameters the
     * information to generate that particular type of message.
     */

    public MessageDTO[] getInvoiceMessages(Integer entityId, Integer processId,
            Integer languageId, InvoiceDTO invoice)
                    throws SessionInternalError, NotificationNotFoundException {
        MessageDTO retValue[] = null;
        Integer deliveryMethod;
        // now see what kind of invoice this customers wants
        if (invoice.getBaseUser().getCustomer() == null) {
            // this shouldn't be necessary. The only reason is here is
            // because the test data has invoices for root users. In
            // reality, all users that will get an invoice have to be
            // customers
            deliveryMethod = Constants.D_METHOD_EMAIL;
            logger.warn("A user that is not a customer is getting an invoice. User id = {}", invoice.getBaseUser().getUserId());
        } else {
            deliveryMethod = invoice.getBaseUser().getCustomer()
                    .getInvoiceDeliveryMethod().getId();
        }

        int index = 0;

        if(deliveryMethod.equals(Constants.D_METHOD_NONE)){
            retValue =  new MessageDTO[0];
        }else if (deliveryMethod.equals(Constants.D_METHOD_EMAIL_AND_PAPER)) {
            retValue = new MessageDTO[2];
        } else {
            retValue = new MessageDTO[1];
        }
        if (deliveryMethod.equals(Constants.D_METHOD_EMAIL)
                || deliveryMethod.equals(Constants.D_METHOD_EMAIL_AND_PAPER)) {
            retValue[index] = getInvoiceEmailMessage(entityId, languageId,
                    invoice);
            index++;
        }

        if (deliveryMethod.equals(Constants.D_METHOD_PAPER)
                || deliveryMethod.equals(Constants.D_METHOD_EMAIL_AND_PAPER)) {
            retValue[index] = getInvoicePaperMessage(entityId, processId,
                    languageId, invoice);
            index++;
        }

        return retValue;
    }

    public MessageDTO getInvoicePaperMessage(Integer entityId,
            Integer processId, Integer languageId, InvoiceDTO invoice)
                    throws SessionInternalError {
        MessageDTO retValue = new MessageDTO();

        retValue.setTypeId(MessageDTO.TYPE_INVOICE_PAPER);
        retValue.setDeliveryMethodId(Constants.D_METHOD_PAPER);

        // put the whole invoice as a parameter
        InvoiceBL invoiceBl = new InvoiceBL(invoice);
        InvoiceDTO invoiceDto = invoiceBl.getDTOEx(languageId, true);
        retValue.getParameters().put("invoiceDto", invoiceDto);
        // the process id is needed to maintain the batch record
        if (processId != null) {
            // single pdf invoices for the web-based app can ignore this
            retValue.getParameters().put("processId", processId);
        }
        try {
            setContent(retValue, MessageDTO.TYPE_INVOICE_PAPER, entityId,
                    languageId);
        } catch (NotificationNotFoundException e1) {
            // put blanks
            MessageSection sectionContent = new MessageSection(new Integer(1),
                    null);
            retValue.addSection(sectionContent);
            sectionContent = new MessageSection(new Integer(2), null);
            retValue.addSection(sectionContent);
        }

        return retValue;
    }

    public MessageDTO getPaymentMessage(Integer entityId, PaymentDTOEx dto, int result)
            throws SessionInternalError, NotificationNotFoundException {
        return getPaymentMessage(entityId, dto, result, null);
    }

    public MessageDTO getPaymentMessage(Integer entityId, PaymentDTOEx dto, int result, Integer messageTypeId)
            throws SessionInternalError, NotificationNotFoundException {

        logger.debug("Payment message for payment: {}" , dto.getPayoutId());
        MessageDTO message = initializeMessage(entityId, dto.getUserId());
        // We have two types of notifications, one for refunds and the other for a normal payment (successful or not).
        Integer typeID = 0;
        if (messageTypeId == null) {
            if (dto.getIsRefund() == 1 && (Constants.RESULT_ENTERED.equals(result) || Constants.RESULT_OK.equals(result))) {
                typeID = MessageDTO.TYPE_PAYMENT_REFUND;
            } else if (Constants.RESULT_ENTERED.equals(result)) {
                typeID = MessageDTO.TYPE_PAYMENT_ENTERED;
            } else if (Constants.RESULT_OK.equals(result)) {
                typeID = MessageDTO.TYPE_PAYMENT;
            } else if (Constants.RESULT_FAIL.equals(result)) {
                typeID = MessageDTO.TYPE_PAYMENT_FAILED;
            }
        } else {
            typeID = messageTypeId;
        }

        message.setTypeId(typeID);

        UserBL user = new UserBL(dto.getUserId());
        Integer languageId = user.getEntity().getLanguageIdField();
        setContent(message, message.getTypeId(), entityId, languageId);

        // find the description for the payment method
        // for payment notification
        Integer paymentMethodId = null;
        if (CollectionUtils.isNotEmpty(dto.getPaymentInstrumentsInfo())) {
            paymentMethodId = getPaymentMethodId(dto, dto.getPaymentInstrumentsInfo().get(0).getPaymentInformation());
        }

        PaymentMethodDTO paymentMethod = new PaymentMethodDAS().find(paymentMethodId);
        PaymentInformationDTO instrument = CollectionUtils.isNotEmpty(dto.getPaymentInstruments()) ?
                dto.getPaymentInstruments().get(0) : dto.getInstrument();
                if (paymentMethod != null) {
                    logger.debug("Payment instrument's payment method: {}", paymentMethod);
                    String method = paymentMethod.getDescription(languageId);
                    message.addParameter("method", method);
                    message.addParameter("method_name", (Constants.PAYMENT_METHOD_CREDIT == paymentMethod.getId() ?
                            paymentMethod.getDescription(languageId) : dto.getPaymentInstrumentsInfo().get(0).getPaymentInformation().getPaymentMethodType().getMethodName()));
                } else {
                    paymentMethod = dto.getInstrument().getPaymentMethod();
                    logger.debug("Payment instrument's payment method: {}", paymentMethod);
                    String method = paymentMethod.getDescription(languageId);
                    message.addParameter("method", method);

                    if (Constants.PAYMENT_CARD.equals(instrument.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())) {
                        paymentMethod = new PaymentMethodDAS().find(getCreditPaymentMethodId(instrument));
                        message.addParameter("method_name", (paymentMethod == null) ? instrument.getPaymentMethodType().getMethodName() : paymentMethod.getDescription(languageId));
                    } else {
                        message.addParameter("method_name", instrument.getPaymentMethodType().getMethodName());
                    }
                }

                message.addParameter("total", Util.formatMoney(dto.getAmount(), dto.getUserId(), dto.getCurrency().getId(), true));
                message.addParameter("payment", dto);
                message.addParameter("payment_id", dto.getId());
                message.addParameter("total_owed", Util.decimal2string(user.getBalance(user.getEntity().getId()), user.getLocale(), Util.AMOUNT_FORMAT_PATTERN));
                message.addParameter("total_without_currency", Util.decimal2string(dto.getAmount(), user.getLocale(), Util.AMOUNT_FORMAT_PATTERN));
                char[] number;
                char[] ccExpiryDate;
                PaymentInformationDAS piDAS = new PaymentInformationDAS();
                PaymentInformationBL piBl = new PaymentInformationBL();
                if (null != instrument && piBl.isCreditCard(instrument)) {

                    piBl.obscureCreditCardNumber(instrument);
                    message.addParameter("credit_card", instrument);

                    number = piBl.getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
                    ccExpiryDate = piBl.getCharMetaFieldByType(instrument, MetaFieldType.DATE);

                    message.addParameter("cc_number_plain", new String(number, number.length - 4, 4));
                    message.addParameter("cc_expiry_date", ccExpiryDate != null ? new String(ccExpiryDate) : "");
                }

                setPaymentMessageTokenValues(user, dto, message, paymentMethod);

                // find an invoice in the list of invoices id
                Integer lastInvoiceId = null;
                try {
                    lastInvoiceId = new InvoiceBL().getLastByUser(dto.getUserId());
                } catch (SQLException e) {
                    logger.info("Error getting last invoice id", e);
                }
                Integer invoiceId;
                if (CollectionUtils.isNotEmpty(dto.getInvoiceIds())) {
                    invoiceId = dto.getInvoiceIds().get(0);
                } else {
                    invoiceId = lastInvoiceId;
                }

                CustomEmailTokenEvent event = new CustomEmailTokenEvent(entityId, dto.getUserId(), message);
                EventManager.process(event);

                if (null == instrument) {
                    instrument = dto.getInstrument();
                    number = piBl.getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
                    ccExpiryDate = piBl.getCharMetaFieldByType(instrument, MetaFieldType.DATE);

                    message.addParameter("cc_number_plain", new String(number, number.length - 4, 4));
                    message.addParameter("cc_expiry_date", ccExpiryDate != null ? new String(ccExpiryDate) : "");
                }

                if (null != invoiceId) {
                    InvoiceDTO invoice = new InvoiceBL(invoiceId).getDTO();
                    message.addParameter("invoice_number", invoice.getPublicNumber());
                    message.addParameter("invoice", invoice);
                    message.addParameter("invoice", invoice);
                    message.addParameter("balance", Util.formatMoney(UserBL.getBalance(invoice.getUserId()),
                            invoice.getBaseUser().getUserId(), invoice.getCurrency().getId(), true));

                    message.addParameter("dueMonth", Util.getMonthName(invoice.getDueDate()));
                    message.addParameter("dueDayOfMonth", Util.getDayOfMonth(invoice.getDueDate()));

                    message.addParameter("due_date", Util.formatDate(invoice.getDueDate(),
                            invoice.getBaseUser().getUserId()));
                }

                message.addParameter("payment", dto);
                if (lastInvoiceId != null) {
                    InvoiceDTO lastInvoice = new InvoiceBL(lastInvoiceId).getDTO();
                    message.addParameter("invoice_amount", Util.decimal2string(lastInvoice.getTotal(), user.getLocale()));
                }
                logger.debug("Include Attachment: {}", message.getIncludeAttachment());
                if (message.getIncludeAttachment() != null && Integer.valueOf(1).equals(message.getIncludeAttachment())) {
                    ContactDTOEx contactDTOEx = (ContactDTOEx) message.getParameters().get("contact");
                    if (null == contactDTOEx) {
                        contactDTOEx = new ContactDTOEx();
                    }
                    message.setAttachmentFile(createPaymentAttachment(dto, user, message.getAttachmentDesign(), message.getAttachmentType(), contactDTOEx));
                    logger.debug("Set attachment {}", message.getAttachmentFile());
                }

                return message;
    }

    /**
     * get payment method ID from payment information
     * @param dto
     * @return
     */
    private Integer getPaymentMethodId(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument) {
        Integer paymentMethodId = null;

        if (null != paymentInstrument) {
            PaymentMethodTypeDTO paymentMethodTypeDTO = paymentInstrument.getPaymentMethodType();
            if (Constants.CHEQUE.equals(paymentMethodTypeDTO.getPaymentMethodTemplate().getTemplateName())) {
                paymentMethodId = new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_CHEQUE).getId();
            } else if (Constants.ACH.equals(paymentMethodTypeDTO.getPaymentMethodTemplate().getTemplateName())) {
                paymentMethodId = new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_ACH).getId();
            } else if (Constants.PAYMENT_CARD.equals(paymentMethodTypeDTO.getPaymentMethodTemplate().getTemplateName())) {
                paymentMethodId = getCreditPaymentMethodId(paymentInstrument);
            } else {
                paymentMethodId = new PaymentInformationBL().getPaymentMethodForPaymentMethodType(paymentInstrument);
            }

            if (null != paymentMethodId) {
                paymentInstrument.setPaymentMethod(new PaymentMethodDAS().find(paymentMethodId));
            } else {
                paymentMethodId = paymentInstrument.getPaymentMethodId();
            }
            //instead of getting payment method get it from payment instrument. As each instrument now has its own payment method.
            logger.debug("Payment instrument's payment method: {}" , paymentInstrument.getPaymentMethod());
        }

        dto.setCreditCard(paymentInstrument);
        return paymentMethodId;
    }

    private Integer getCreditPaymentMethodId(PaymentInformationDTO paymentInstrument) {
        char[] creditCardNumber = new PaymentInformationBL().getCharMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        if (null != creditCardNumber && !PaymentInformationBL.paymentCardObscured(creditCardNumber)) {
            return com.sapienter.jbilling.common.Util.getPaymentMethod(creditCardNumber);
        }
        return null;
    }

    public MessageDTO getInvoiceReminderMessage(Integer entityId,
            Integer userId, Integer days, Date dueDate, String number,
            BigDecimal total, Date date, Integer currencyId)
                    throws SessionInternalError, NotificationNotFoundException {
        UserBL user;
        Integer languageId;
        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(MessageDTO.TYPE_INVOICE_REMINDER);

        user = new UserBL(userId);
        languageId = user.getEntity().getLanguageIdField();
        setContent(message, message.getTypeId(), entityId, languageId);

        message.addParameter("days", days.toString());
        message.addParameter("dueDate", Util.formatDate(dueDate, userId));
        message.addParameter("number", number);
        message.addParameter("total", Util.formatMoney(total, userId,
                currencyId, true));
        message.addParameter("date", Util.formatDate(date, userId));


        return message;
    }

    public MessageDTO getForgetPasswordEmailMessage(Integer entityId,
            Integer userId, Integer languageId, String link) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.addParameter("newPasswordLink", link);

        message.setTypeId(MessageDTO.TYPE_FORGETPASSWORD_EMAIL);

        setContent(message, MessageDTO.TYPE_FORGETPASSWORD_EMAIL, entityId,
                languageId);

        return message;
    }

    public MessageDTO getInitialCredentialsEmailMessage(Integer entityId,
            Integer userId, Integer languageId, String link) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.addParameter("newPasswordLink", link);

        message.setTypeId(MessageDTO.TYPE_CREDENTIALS_EMAIL);

        setContent(message, MessageDTO.TYPE_CREDENTIALS_EMAIL, entityId,
                languageId);

        return message;
    }

    public MessageDTO getResetPasswordChangeEmailMessage(Integer entityId, Integer userId, Integer languageId) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(Constants.NOTIFY_PASSWORD_CHANGE);
        setContent(message, Constants.NOTIFY_PASSWORD_CHANGE, entityId, languageId);

        return message;
    }


    public MessageDTO getInvoiceEmailMessage(Integer entityId,
            Integer languageId, InvoiceDTO invoice)
                    throws SessionInternalError, NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, invoice.getBaseUser()
                .getUserId());

        message.setTypeId(MessageDTO.TYPE_INVOICE_EMAIL);

        setContent(message, MessageDTO.TYPE_INVOICE_EMAIL, entityId,
                languageId);

        message.addParameter("total", Util.formatMoney(invoice.getTotal(),
                invoice.getBaseUser().getUserId(), invoice.getCurrency().getId(), true));
        message.addParameter("id", invoice.getId() + "");
        message.addParameter("number", printable(invoice.getPublicNumber()));
        // format the date depending of the customers locale

        message.addParameter("due_date", Util.formatDate(invoice.getDueDate(),
                invoice.getBaseUser().getUserId()));
        String notes = invoice.getCustomerNotes();

        message.addParameter("notes", printable(notes));
        message.addParameter("invoice", invoice);

        // Added new parameters for HTML email message.
        message.addParameter("balance", Util.formatMoney(UserBL.getBalance(invoice.getUserId()),
                invoice.getBaseUser().getUserId(), invoice.getCurrency().getId(), true));

        BigDecimal totalTaxAmount = invoice.getInvoiceLines().stream().filter(invoiceLineDTO -> null != invoiceLineDTO.getTaxAmount())
                .map(InvoiceLineDTO :: getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGrossAmount = invoice.getInvoiceLines().stream().filter(invoiceLineDTO -> null != invoiceLineDTO.getGrossAmount())
                .map(InvoiceLineDTO :: getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        message.addParameter("totalTaxAmount", Util.formatMoney(totalTaxAmount, invoice.getBaseUser().getUserId(), invoice.getCurrency().getId(), true));

        message.addParameter("totalGrossAmount", Util.formatMoney(totalGrossAmount, invoice.getBaseUser().getUserId(), invoice.getCurrency().getId(), true));

        CustomEmailTokenEvent event = new CustomEmailTokenEvent(entityId, invoice.getUserId(), message);
        EventManager.process(event);

        message.addParameter("dueMonth", Util.getMonthName(invoice.getDueDate()));
        message.addParameter("dueDayOfMOnth", Util.getDayOfMonth(invoice.getDueDate()));
        UserBL user = new UserBL(invoice.getUserId());
        message.addParameter("total_owed", Util.decimal2string(user.getBalance(user.getEntity().getId()), user.getLocale(), Util.AMOUNT_FORMAT_PATTERN));
        // if the entity has the preference of pdf attachment, do it
        try {
            int preferencePDFAttachment = 0;

            try {
                preferencePDFAttachment =
                        PreferenceBL.getPreferenceValueAsIntegerOrZero(
                                entityId, Constants.PREFERENCE_PDF_ATTACHMENT);
            } catch (EmptyResultDataAccessException e1) {
                // no problem, I'll get the defaults
            }
            if (preferencePDFAttachment == 1) {
                setAttachmentFiles(invoice, message);
            }
        } catch (Exception e) {
            logger.error("Exception getting invoice email message", e);
        }

        return message;
    }

    private void setAttachmentFiles(InvoiceDTO invoice, MessageDTO message) {
        String fileName = generatePaperInvoiceAsFile(invoice);
        logger.debug("file generated {} for invoice {} for user {}", fileName, invoice.getId(), invoice.getBaseUser().getId());
        message.setAttachmentFile(fileName);
        logger.debug("Setted attachment {}", message.getAttachmentFile());
        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {
            if (invoiceLine.getOrder() != null) {
                MetaFieldValue detailFileNamesMF = MetaFieldHelper.getMetaField(invoiceLine.getOrder(), SpaConstants.DETAIL_FILE_NAMES);
                if (detailFileNamesMF != null && !StringUtils.isEmpty(detailFileNamesMF.getValue().toString())) {
                    for (String detailFileName : detailFileNamesMF.getValue().toString().split(";")) {

                        String initialPath = com.sapienter.jbilling.common.Util.getSysProp("base_dir") +
                                invoice.getBaseUser().getCompany().getMetaField(Constants.MF_DETAIL_FILE_FOLDER).getValue() +
                                File.separator;
                        String fileNameAndPath = initialPath + detailFileName;
                        String fileTmpNameAndPath = initialPath + File.separator + "tmp" + File.separator + detailFileName;

                        Path source = Paths.get(fileNameAndPath);
                        Path destination = Paths.get(fileTmpNameAndPath);

                        boolean copiedToTemporal = true;

                        try {
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            copiedToTemporal = false;
                            logger.error("Error copying source {} to destination {}", fileNameAndPath, fileTmpNameAndPath);
                        }

                        if (!message.getAttachmentFile().contains(fileNameAndPath) && copiedToTemporal) {
                            message.setAttachmentFile(message.getAttachmentFile() + ";" + fileTmpNameAndPath);
                            logger.debug("Setted attachment {}", fileTmpNameAndPath);
                        }
                    }
                }
            }
        }
    }

    public MessageDTO getAgeingMessage(Integer entityId, Integer languageId,
            Integer ageingNotificationId, Integer userId) throws NotificationNotFoundException {

        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(ageingNotificationId);
        try {
            setContent(message, message.getTypeId(), entityId, languageId);
            UserBL user = new UserBL(userId);
            InvoiceBL invoice = new InvoiceBL();
            Integer invoiceId = invoice.getLastByUser(userId);

            if (invoiceId != null) {
                invoice.set(invoiceId);

                message.addParameter("invoice_amount", Util.formatMoney(invoice.getEntity().getBalance(), userId, user.getCurrencyId(), true));
                message.addParameter("user_balance", Util.formatMoney(UserBL.getBalance(userId), userId, user.getCurrencyId(), true));
                message.addParameter("invoice_number", invoice.getEntity().getPublicNumber());
                message.addParameter("invoice", invoice.getEntity());
                message.addParameter("date", Util.formatDate(TimezoneHelper.companyCurrentDate(entityId), userId));
                message.addParameter("invoice_due_date", Util.formatDate(invoice.getEntity().getDueDate(), userId));

                UserDTO userDto = UserBL.getUserEntity(userId);
                CompanyDAS companyDas = new CompanyDAS();
                CompanyDTO company = companyDas.findEntityByName(companyDas.findCompanyNameByEntityId(entityId));

                boolean stepFound = false;
                boolean nextStepExist =false;
                int days = 0;

                List<AgeingEntityStepDTO> ageingSteps = new LinkedList<>(getAgeingStepsByUserStatus(userDto, company));
                Collections.sort(ageingSteps, ByDays);

                for (AgeingEntityStepDTO ageingEntityStepDTO : ageingSteps){
                    if(stepFound){
                        days = ageingEntityStepDTO.getDays();
                        nextStepExist = true;
                        break;
                    }

                    if(userDto.getUserStatus().equals(ageingEntityStepDTO.getUserStatus())){
                        stepFound = true;
                    }
                }
                if(nextStepExist) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(invoice.getEntity().getDueDate());
                    calendar.add(Calendar.DAY_OF_MONTH,days);
                    message.addParameter("invoice_next_step_date", Util.formatDate(calendar.getTime(), userId));
                }else{
                    message.addParameter("invoice_next_step_date", StringUtils.EMPTY);
                    logger.debug("Next step does not exist");
                }

                Date paymentDate = getLastPaymentDate(userId);
                if (paymentDate != null) {
                    message.addParameter("paymentDate", Util.formatDate(paymentDate, userId));
                }

                //Requirement #2718 - Overdue Invoice in notification
                try {
                    int preferenceAttachInvoiceToNotifications = PreferenceBL.getPreferenceValueAsIntegerOrZero(
                            entityId, Constants.PREFERENCE_ATTACH_INVOICE_TO_NOTIFICATIONS);
                    if (preferenceAttachInvoiceToNotifications == 1) {
                        invoice.set(invoiceId);
                        message.setAttachmentFile(generatePaperInvoiceAsFile(invoice.getEntity()));
                        logger.debug("attaching invoice {}", message.getAttachmentFile());
                    }
                } catch (EmptyResultDataAccessException e1) {
                    // no problem, I'll get the defaults
                } catch (Exception e) {
                    logger.error("Exception when attaching invoice for ageing message", e);
                }
            } else {
                logger.warn("user {} has no invoice but an ageing message is being sent", userId);
            }
        } catch (SQLException e1) {
            throw new SessionInternalError(e1);
        }

        return message;
    }

    private Set<AgeingEntityStepDTO> getAgeingStepsByUserStatus(UserDTO userDto, CompanyDTO company) {
        boolean isCancelledUser = new CancellationRequestBL().isUserCancelled(userDto);
        Set<AgeingEntityStepDTO> steps = company.getAgeingEntitySteps();
        if(isCancelledUser){
            return steps.stream()
                    .filter(step -> step.getCollectionType()
                            .equals(CollectionType.CANCELLATION_INVOICE))
                            .collect(Collectors.toSet());
        } else {
            return steps.stream()
                    .filter(step -> step.getCollectionType()
                            .equals(CollectionType.REGULAR))
                            .collect(Collectors.toSet());
        }
    }

    public MessageDTO getOrderNotification(Integer entityId, Integer step,
            Integer languageId, Date activeSince, Date activeUntil,
            Integer userId, BigDecimal total, Integer currencyId)
                    throws SessionInternalError,
                    NotificationNotFoundException {
        MessageDTO retValue = initializeMessage(entityId, userId);
        retValue.setTypeId(new Integer(MessageDTO.TYPE_ORDER_NOTIF.intValue()
                + step.intValue() - 1));
        try {
            setContent(retValue, retValue.getTypeId(), entityId, languageId);
            Locale locale;
            try {
                UserBL user = new UserBL(userId);
                locale = user.getLocale();
            } catch (Exception e) {
                throw new SessionInternalError(e);
            }
            ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            DateTimeFormatter formatter = DateTimeFormat.forPattern(bundle.getString("format.date"));

            retValue.addParameter("period_start", formatter.print(activeSince.getTime()));
            retValue.addParameter("period_end", formatter.print(activeUntil.getTime()));
            retValue.addParameter("total", Util.formatMoney(total, userId,
                    currencyId, true));
        } catch (ClassCastException e) {
            throw new SessionInternalError(e);
        }
        return retValue;
    }

    public MessageDTO getDeletedUSer(Integer entityId,
            Integer userId, Integer languageId) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(MessageDTO.TYPE_DELETED_USER);

        setContent(message, MessageDTO.TYPE_DELETED_USER, entityId, languageId);

        return message;
    }

    public MessageDTO getPayoutMessage(Integer entityId, Integer languageId, BigDecimal total, Date startDate,
            Date endDate, boolean clerk, Integer partnerId)
                    throws SessionInternalError, NotificationNotFoundException {

        MessageDTO message = new MessageDTO();
        if (!clerk) {
            message.setTypeId(MessageDTO.TYPE_PAYOUT);
        } else {
            message.setTypeId(MessageDTO.TYPE_CLERK_PAYOUT);
        }

        try {
            EntityBL en = new EntityBL(entityId);

            setContent(message, message.getTypeId(), entityId, languageId);
            message.addParameter("total", Util.decimal2string(total, en.getLocale()));

            message.addParameter("company", new CompanyDAS().find(entityId)
                    .getDescription());
            PartnerBL partner = new PartnerBL(partnerId);

            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            message.addParameter("period_end", Util.formatDate(cal.getTime(),
                    partner.getEntity().getUser().getUserId()));
            cal.setTime(startDate);
            message.addParameter("period_start", Util.formatDate(cal.getTime(),
                    partner.getEntity().getUser().getUserId()));
            message.addParameter("partner_id", partnerId.toString());
        } catch (ClassCastException e) {
            throw new SessionInternalError(e);
        }

        return message;
    }

    public MessageDTO getCreditCardMessage(Integer entityId,
            Integer languageId, Integer userId, Date ccExpiryDate, PaymentInformationDTO instrument)
                    throws SessionInternalError,
                    NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(MessageDTO.TYPE_CREDIT_CARD);

        setContent(message, message.getTypeId(), entityId, languageId);
        DateTimeFormatter format = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT);
        message.addParameter("expiry_date", format.print(ccExpiryDate.getTime()));
        message.addParameter("credit_card", instrument);

        return message;
    }

    public MessageDTO getCustomNotificationMessage(Integer notificationMessageTypeId, Integer entityId,
            Integer userId, Integer languageId)
                    throws SessionInternalError, NotificationNotFoundException {

        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(notificationMessageTypeId);

        setContent(message, notificationMessageTypeId, entityId,
                languageId);

        return message;
    }

    private void setContent(MessageDTO newMessage, Integer type,
            Integer entity, Integer language) throws SessionInternalError,
            NotificationNotFoundException {
        set(type, language, entity);
        if (messageRow != null) {
            if (messageRow.getUseFlag() == 0 && !forceNotification) {
                // if (messageRow.getUseFlag().intValue() == 0) {
                throw new NotificationNotFoundException("Notification " + "flaged for not use");
            }
            setContent(newMessage);
        } else {
            String message = "Looking for notification message type " + type + " for entity " +
                    entity + " language " + language + " but could not find it. This entity has " +
                    "to specify " + "this notification message.";
            logger.warn(message);
            throw new NotificationNotFoundException(message);
        }

    }

    private void setContent(MessageDTO newMessage) throws SessionInternalError {

        // go through the sections

        Set<NotificationMessageSectionDTO> sections = messageRow.getNotificationMessageSections();
        List<NotificationMessageSectionDTO> sectionsList = new LinkedList(sections);
        // Sort the section on the basis of "section" field which is actually an index to the section.
        // 1. means it is a subject
        // 2. means it is body text
        // 3. means it is body html
        Collections.sort(sectionsList, new Comparator<NotificationMessageSectionDTO>() {
            @Override
            public int compare(NotificationMessageSectionDTO o1, NotificationMessageSectionDTO o2) {
                int i1 = o1.getSection();
                int i2 = o2.getSection();
                return (i1 > i2 ? 1 : (i1 == i2 ? 0 : -1));
            }
        });
        for (NotificationMessageSectionDTO section : sectionsList ) {
            // then through the lines of this section
            StringBuffer completeLine = new StringBuffer();
            Collection lines = section.getNotificationMessageLines();
            int checkOrder = 0; // there's nothing to assume that the lines
            // will be retrived in order, but the have to!
            List vLines = new ArrayList<NotificationMessageSectionDTO>(lines);
            Collections.sort(vLines, new NotificationLineEntityComparator());
            for (Iterator it2 = vLines.iterator(); it2.hasNext(); ) {
                NotificationMessageLineDTO line = (NotificationMessageLineDTO) it2
                        .next();
                if (line.getId() <= checkOrder) {
                    // if (line.getId().intValue() <= checkOrder) {
                    logger.error("Lines have to be retreived in order. "
                            + "See class java.util.TreeSet for solution or "
                            + "Collections.sort()");
                    throw new SessionInternalError("Lines have to be "
                            + "retreived in order.");
                } else {
                    checkOrder = line.getId();
                    // checkOrder = line.getId().intValue();
                }
                completeLine.append(line.getContent());
            }
            // add the content of this section to the message
            MessageSection sectionContent = new MessageSection(section
                    .getSection(), completeLine.toString());
            newMessage.addSection(sectionContent);
            //populated properties in the MessageDTO using the corresponding values from the NotificationMessageDTO
            newMessage.setAttachmentDesign(messageRow.getAttachmentDesign());
            newMessage.setIncludeAttachment(messageRow.getIncludeAttachment());
            newMessage.setAttachmentType(messageRow.getAttachmentType());
        }

        newMessage.setNotifyAdmin((messageRow.getNotifyAdmin()!=null)?messageRow.getNotifyAdmin():0);
        newMessage.setNotifyPartner((messageRow.getNotifyPartner() != null) ? messageRow.getNotifyPartner() : 0);
        newMessage.setNotifyParent((messageRow.getNotifyParent() != null) ? messageRow.getNotifyParent() : 0);
        newMessage.setNotifyAllParents((messageRow.getNotifyAllParents() != null) ? messageRow.getNotifyAllParents() : 0);
        newMessage.setMediumTypes(new ArrayList<NotificationMediumType>(messageRow.getMediumTypes()));
    }

    static public String parseParameters(String content, HashMap parameters) {
        // get the engine from Spring
        VelocityEngine velocity = (VelocityEngine) Context.getBean(Context.Name.VELOCITY);

        //velocity tools
        ToolContext toolContext = new ToolContext(velocity);
        ToolboxFactory factory = new EasyFactoryConfiguration(true).createFactory();
        toolContext.addToolbox(factory.createToolbox(Scope.SESSION));
        toolContext.putAll(parameters);
        StringWriter result = new StringWriter();
        try {
            velocity.evaluate(toolContext, result, "Error template as string?", content);
        } catch (ParseErrorException pee) {
            throw new SessionInternalError("Error parsing the template",
                    new String[] { "notification.parse.template.error" });
        } catch (Exception e) {
            throw new SessionInternalError("Rendering email", NotificationBL.class, e);
        }

        return result.toString();

    }

    /**
     * A rather expensive call for what it achieves. It looks suitable for caching, but then
     * it is rarely called (only from the GUI)... and then the orm cache helps too.
     * @param entityId
     * @return
     */
    public int getSections(Integer entityId) {
        int higherSection = 0;
        try {
            PluggableTaskManager taskManager =
                    new PluggableTaskManager(
                            entityId,
                            Constants.PLUGGABLE_TASK_NOTIFICATION);
            NotificationTask task =
                    (NotificationTask) taskManager.getNextClass();

            while (task != null) {
                if (task.getSections() > higherSection) {
                    higherSection = task.getSections();
                }

                task = (NotificationTask) taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("Finding number of sections for notifications",
                    NotificationBL.class, e);
        }
        return higherSection;
    }

    public CachedRowSet getTypeList(Integer languageId) throws SQLException,
    Exception {

        prepareStatement(NotificationSQL.listTypes);
        cachedResults.setInt(1, languageId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    public String getEmails(String separator, Integer entityId) throws SQLException {
        StringBuilder retValue = new StringBuilder();
        DataSource dataSource = Context.getBean(Context.Name.DATA_SOURCE);
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement stmt = connection.prepareStatement(NotificationSQL.allEmails)) {
                stmt.setInt(1, entityId.intValue());
                try(ResultSet res = stmt.executeQuery()) {
                    boolean first = true;
                    while (res.next()) {
                        if (first) {
                            first = false;
                        } else {
                            retValue.append(separator);
                        }
                        retValue.append(res.getString(1));
                    }
                    return retValue.toString();
                }
            }
        }
    }

    public static byte[] generatePaperInvoiceAsStream(String design,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId,
            String username, String password) {
        JasperPrint report = generatePaperInvoice(design, useSqlQuery, invoice,
                from, to, message1, message2, entityId, username, password);
        try {
            return JasperExportManager.exportReportToPdf(report);
        } catch (JRException e) {
            logger.error("Exception generating paper invoice", e);
            throw new SessionInternalError(e);
        }
    }

    public static byte[] generatePaperInvoiceAsStream(String design,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId,
            String username, String password,boolean removeBlankPage) {
        JasperPrint report = generatePaperInvoice(design, useSqlQuery, invoice,
                from, to, message1, message2, entityId, username, password);
        try {
            if (null != report && removeBlankPage) {
                removeBlankPDFPage(report.getPages());
            }
            return JasperExportManager.exportReportToPdf(report);
        } catch (JRException e) {
            logger.error("Exception generating paper invoice", e);
            throw new SessionInternalError(e);
        }
    }

    public static String generatePaperInvoiceAsFile(String design,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId,
            String username, String password, boolean removeBlankPage) throws JRException {

        final JasperPrint report = generatePaperInvoice(design, useSqlQuery, invoice, from, to, message1, message2, entityId,
                username, password);
        if (null != report && removeBlankPage) {
            removeBlankPDFPage(report.getPages());
        }
        final String fileName = BASE_DIR
                + "invoices/"
                + entityId
                + "-"
                + invoice.getId()
                + "-invoice.pdf";
        try {
            JRPdfExporter exporter = new JRPdfExporter();


            final SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            configuration.setPermissions(PdfWriter.ALLOW_COPY | PdfWriter.ALLOW_PRINTING);
            exporter.setConfiguration(configuration);


            exporter.setExporterInput(new ExporterInput() {

                @Override
                public List<ExporterInputItem> getItems() {
                    // TODO Auto-generated method stub
                    List<ExporterInputItem> items = new ArrayList<ExporterInputItem>();
                    ExporterInputItem item = new ExporterInputItem() {

                        @Override
                        public JasperPrint getJasperPrint() {
                            // TODO Auto-generated method stub
                            return report;
                        }

                        @Override
                        public ReportExportConfiguration getConfiguration() {
                            // TODO Auto-generated method stub
                            return null;
                        }
                    };
                    items.add(item);
                    return items;
                }
            });

            exporter.setExporterOutput(new OutputStreamExporterOutput() {

                @Override
                public OutputStream getOutputStream() {
                    try{
                        File outputFile=new File(fileName);
                        FileOutputStream fos=new FileOutputStream(outputFile);
                        return fos;
                    } catch (FileNotFoundException fnfe) {
                        return null;
                    }
                }

                @Override
                public void close() {
                    // TODO Auto-generated method stub

                }
            });

            exporter.exportReport();

            //JasperExportManager.exportReportToPdfFile(report, fileName);
        } catch (JRException e) {
            logger.error("Exception generating paper invoice", e);
        }
        return fileName;
    }

    private static JasperPrint generatePaperInvoice(String design,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId,
            String username, String password) {
        try {
            // This is needed for JasperRerpots to work, for some twisted XWindows issue
            System.setProperty("java.awt.headless", "true");
            String designFile =String.format(DESIGN_PATH, design);

            File compiledDesign = new File(designFile);
            logger.debug("Generating paper invoice with design file : {}", designFile);

            //InvoiceTemplateBL.populateMediationRecordLines(invoice);

            Map<String, Object> parameters = InvoiceTemplateBL.createAitDynamicParameters(invoice, entityId);
            if(design.equals("invoice_design_beta_customer") || design.equals("b2b_license_amc_invoice_design")){
                UserDTO user = new UserDAS().findNow(invoice.getUserId());

                parameters.put("total_in_word", getCurrencyInWords(user.getCurrency().getCode(),invoice.getTotal()));

                Integer orderId = new InvoiceDAS().getFirstOrderIdByInvoiceId(invoice.getId());
                OrderDTO order = new OrderDAS().findNow(orderId);
                if (order != null) { // not found
                    MetaFieldValue value = order.getMetaField("Invoice Date");
                    if(value != null) {
                        String invoiceMonth = value.getValue().toString();
                        if(StringUtils.isNotBlank(invoiceMonth)) {
                            String[] splitValue = invoiceMonth.split("/");
                            if(splitValue.length > 2) {
                                LocalDate localDate = YearMonth.of(Integer.parseInt(splitValue[2]),Integer.parseInt(splitValue[0])).atEndOfMonth();
                                int lastDay = localDate.getDayOfMonth();
                                String month = localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
                                parameters.put("invoice_month", month + " " + splitValue[2]);
                                parameters.put("invoice_period", "1 - " + lastDay + " " + month + " " + splitValue[2]);
                            }
                        }
                    }
                }
                Optional<EInvoiceLogDTO> eInvoiceLogDTO = Optional.ofNullable(new EInvoiceLogDAS().findByInvoiceId(invoice.getId()));
                eInvoiceLogDTO.ifPresent(e -> {
                    try {
                        JsonNode rootNode = new ObjectMapper().readTree(e.geteInvoiceResponse());
                        String ackDate = rootNode.path("AckDt").asText();
                        String ackNumber = rootNode.path("AckNoStr").asText();
                        String signedQRCode = rootNode.path("SignedQRCode").asText();

                        if (StringUtils.isNotBlank(ackDate)) {
                            String formattedDateOnly = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                                    .format(LocalDate.parse(ackDate.split(" ")[0]));
                            parameters.put("ackDate", formattedDateOnly);
                        }

                        if (StringUtils.isNotBlank(ackNumber)) {
                            parameters.put("ackNumber", ackNumber);
                        }

                        if (StringUtils.isNotBlank(signedQRCode)) {
                            parameters.put("QR", signedQRCode);
                        }

                    }catch (Exception exception){
                        exception.printStackTrace();
                    }
                });
            }
            if(design.equals("invoice_design_earnbill")) {
                PaymentUrlLogDTO dto = new PaymentUrlLogDAS().findByInvoiceId(invoice.getId());
                if(null != dto) {
                    String paymentUrl = dto.getPaymentUrl();
                    if (!StringUtils.isBlank(paymentUrl)) {
                        parameters.put("QR", paymentUrl);
                    }
                }
            }

            List<String> designs = Arrays.asList("invoice_design","invoice_design_ac_uk","invoice_design_earnbill", "invoice_design_beta_customer", "b2b_license_amc_invoice_design");

            if(designs.contains(design)) {
                return generatePaperInvoiceNew(compiledDesign, parameters, useSqlQuery, invoice, from, to, message1, message2, entityId);
            } else {
                return generatePaperInvoiceDefault(compiledDesign, parameters, useSqlQuery, invoice, from, to, message1, message2, entityId, username, password);
            }

        }
        catch (SessionInternalError e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("Exception generating paper invoice", e);
            return null;
        }
    }

    private static JasperPrint generatePaperInvoiceDefault(File compiledDesign, Map<String, Object> parameters,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId,
            String username, String password) {
        Locale locale = (new UserBL(invoice.getUserId())).getLocale();

        //Entity for entity meta fields
        EntityBL entity = new EntityBL(entityId);

        // add all the invoice data
        parameters.put("invoiceNumber", printable(invoice.getPublicNumber()));
        parameters.put("invoiceId", invoice.getId());
        parameters.put("entityName", printable(from.getOrganizationName()));
        parameters.put("entityAddress", printable(from.getAddress1()));
        parameters.put("entityAddress2", printable(from.getAddress2()));
        parameters.put("entityPostalCode", printable(from.getPostalCode()));
        parameters.put("entityCity", printable(from.getCity()));
        parameters.put("entityProvince", printable(from.getStateProvince()));
        parameters.put("customerOrganization", printable(to.getOrganizationName()));
        parameters.put("customerName", printable(to.getFirstName(), to.getLastName()));
        parameters.put("customerAddress", printable(to.getAddress1()));
        parameters.put("customerAddress2", printable(to.getAddress2()));
        parameters.put("customerPostalCode", printable(to.getPostalCode()));
        parameters.put("customerCity", printable(to.getCity()));
        parameters.put("customerProvince", printable(to.getStateProvince()));
        parameters.put("customerUsername", username);
        parameters.put("customerPassword", password);
        parameters.put("customerId", printable(invoice.getUserId().toString()));
        parameters.put("invoiceDate", Util.formatDate(invoice.getCreateDatetime(), invoice.getUserId()));
        parameters.put("invoiceDueDate", Util.formatDate(invoice.getDueDate(), invoice.getUserId()));

        // customer message
        logger.debug("m1 = {} m2 = {}", message1, message2);
        parameters.put("customerMessage1", printable(message1));
        parameters.put("customerMessage2", printable(message2));

        //Values of certain preferences are passed as parameters
        if (entity.getEntity().getMetaFields() != null) {
            for (MetaFieldValue metaFieldValue : entity.getEntity().getMetaFields()) {
                parameters.put("company_" + metaFieldValue.getField().getName().replaceAll("\\W", "_").toLowerCase(), metaFieldValue.getValue());
            }
        }
        // invoice notes stripped of html line breaks
        String notes = invoice.getCustomerNotes();
        if (notes != null) {
            notes = notes.replaceAll("<br/>", "\r\n");
        }
        parameters.put("notes", printable(notes));

        Date prevInvoiceCreateTimestamp = Util.getEpochDate();

        // now some info about payments
        try {
            InvoiceBL invoiceBL = new InvoiceBL(invoice.getId());
            try {
                parameters.put("paid", Util.formatMoney(invoiceBL.getTotalPaid(),
                        invoice.getUserId(),
                        invoice.getCurrency().getId(), false));

                // find the previous invoice and its payment for extra info
                invoiceBL.setPrevious();
                parameters.put("prevInvoiceTotal", Util.formatMoney(invoiceBL.getEntity().getTotal(), invoice.getUserId(), invoice.getCurrency().getId(), false));
                parameters.put("prevInvoicePaid", Util.formatMoney(invoiceBL.getTotalPaid(), invoice.getUserId(), invoice.getCurrency().getId(), false));
                prevInvoiceCreateTimestamp = invoiceBL.getEntity().getCreateTimestamp();

            } catch (EmptyResultDataAccessException e1) {
                parameters.put("prevInvoiceTotal", "0");
                parameters.put("prevInvoicePaid", "0");
            }
        } catch (Exception e) {
            logger.error("Exception generating paper invoice", e);
            return null;
        }

        //get the total of all payments received since the last invoice
        PaymentBL paymentBL = new PaymentBL();
        BigDecimal paymentsTotal = paymentBL.findTotalRevenueByUser(invoice.getUserId(), prevInvoiceCreateTimestamp, invoice.getCreateTimestamp());
        parameters.put("paymentsTotal", Util.formatMoney(paymentsTotal, invoice
                .getUserId(), invoice.getCurrency().getId(), false));

        // symbol of the currency
        String symbol = evaluateCurrencySymbol(invoice);
        parameters.put("currency_symbol",symbol);

        //Calculate OCR for invoice header
        //add the due date
        char delimiter = ' ';
        StringBuilder ocr = new StringBuilder();
        DateTimeFormatter ocrDf = DateTimeFormat.forPattern("MMddyy");
        ocr.append(ocrDf.print(invoice.getDueDate().getTime()));
        ocr.append(delimiter);
        //add the user id
        DecimalFormat df = new DecimalFormat("000000000");
        ocr.append(df.format(invoice.getUserId()));
        ocr.append(delimiter);
        //add the invoice nr
        String paddedInvoiceNr = "000000000"+(invoice.isReviewInvoice() ? invoice.getId() : invoice.getPublicNumber());
        ocr.append(paddedInvoiceNr.substring(paddedInvoiceNr.length()-10));
        ocr.append(delimiter);
        //add the invoice total
        ocr.append(df.format(invoice.getTotal().multiply(HUNDRED)));
        ocr.append(delimiter);
        //append Luhn Check Digit
        ocr.append(com.sapienter.jbilling.common.Util.calcLuhnCheckDigit(ocr.toString().replaceAll("\\W", "")));
        parameters.put("ocr", ocr.toString());

        //Extract data EDI meter read records
        MetaFieldValue meterReadIdMf = invoice.getMetaField(FileConstants.META_FIELD_METER_READ_FILE );
        Object meterReadId = (meterReadIdMf == null ? Integer.valueOf(0) : meterReadIdMf.getValue());
        extractEdiMeterReadForInvoice(meterReadId != null ? Integer.valueOf(meterReadId.toString()) : 0, parameters, invoice.getUserId());

        // add all the custom contact fields
        // the from
        UserDTO fromUser = new UserDAS().find(from.getUserId());
        if (fromUser.getCustomer() != null && fromUser.getCustomer().getMetaFields() != null) {
            for (MetaFieldValue metaFieldValue : fromUser.getCustomer().getMetaFields()) {
                parameters.put("from_custom_" + metaFieldValue.getField().getName(), metaFieldValue.getValue());
            }
        }
        UserDTO toUser = new UserDAS().find(to.getUserId());
        if (toUser.getCustomer() != null && toUser.getCustomer().getMetaFields() != null) {
            for (MetaFieldValue metaFieldValue : toUser.getCustomer().getMetaFields()) {
                parameters.put("to_custom_" + metaFieldValue.getField().getName(), metaFieldValue.getValue());
            }
        }

        // the logo is a file
        File logo = LogoType.INVOICE.getFile(entityId);
        if (!logo.exists()) {
            logger.warn("Logo file (entity-{}.(png/jpg)) not found under {}logos/ folder", entityId, BASE_DIR);
            parameters.put("entityLogo", null);
        } else {
            parameters.put("entityLogo", logo);
        }

        // the invoice lines go as the data source for the report
        // we need to extract the taxes from them, put the taxes as
        // an independent parameter, and add the taxes rates as more
        // parameters
        BigDecimal taxTotal = new BigDecimal(0);
        int taxItemIndex = 0;
        // I need a copy, so to not affect the real invoice
        List<InvoiceLineDTO> lines = new ArrayList<>(invoice.getInvoiceLines());
        // Collections.copy(lines, invoice.getInvoiceLines());

        //we need to split the total for services and the rest (charges/fines/etc...)
        BigDecimal totalDebitsCredits = BigDecimal.ZERO;
        MetaFieldValue<String> productTypesMf = entity.getEntity().getMetaField(Constants.COMPANY_METAFIELD_INVOICE_LINES_PRODUCT_TYPES);
        String productCategoriesMf = (productTypesMf != null) ? productTypesMf.getValue() : null;
        Set<Integer> productCategories = new HashSet<>();
        if(productCategoriesMf != null && !productCategoriesMf.isEmpty()) {
            for(String categoryId : productCategoriesMf.split(",")) {
                productCategories.add(Integer.valueOf(categoryId.trim()));
            }
        }

        logger.debug("Categories with products: {}", productCategories);

        List<InvoiceLineDTO> linesRemoved = new ArrayList<>();
        for (InvoiceLineDTO line : lines) {
            // log.debug("Processing line " + line);
            // process the tax, if this line is one
            if (line.getInvoiceLineType() != null && // for headers/footers
                    line.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_TAX) {
                // update the total tax variable
                taxTotal = taxTotal.add(line.getAmount());
                // add the tax amount as an array parameter
                parameters.put("taxItem_" + taxItemIndex, printable(Util.decimal2string(line.getPrice(), locale)));
                taxItemIndex++;
                // taxes are not displayed as invoice lines
                linesRemoved.add(line); // can't do lines.remove(): ConcurrentModificationException
            } else if (line.getIsPercentage() != null && line.getIsPercentage().intValue() == 1) {
                // if the line is a percentage, remove the price
                line.setPrice(null);
            }

            //calculate total for charges
            if(!productCategories.isEmpty()) {
                boolean foundType = false;
                if(line.getItem() != null ||
                        (line.getInvoiceLineType() != null && //line item taxes must also be correctly attributed
                        line.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_TAX &&
                        line.getParentLine() != null && line.getParentLine().getItem() != null)) {
                    Set<ItemTypeDTO> itemTypes = line.getItem() != null
                            ? line.getItem().getItemTypes()
                                    : line.getParentLine().getItem().getItemTypes();
                            for(ItemTypeDTO type : itemTypes) {
                                if(productCategories.contains(Integer.valueOf(type.getId()))) {
                                    foundType = true;
                                    break;
                                }
                            }
                            if(!foundType) {
                                logger.debug("Line contains other debit/credit: {}", line);
                                totalDebitsCredits = totalDebitsCredits.add(line.getAmount());
                            } else {
                                logger.debug("Line contains regular product/service: {}", line);
                            }
                }
            }
        }

        lines.removeAll(linesRemoved); // removed them once out of the loop. Otherwise it will throw

        Collections.sort(lines, new InvoiceLineComparator());

        parameters.put("taxDecimal",taxTotal.setScale(2, RoundingMode.HALF_UP));
        // now add the tax
        parameters.put("tax", Util.formatMoney(taxTotal, invoice.getUserId(), invoice
                .getCurrency().getId(), false));
        parameters.put("totalWithTax", Util.formatMoney(invoice.getTotal(),
                invoice.getUserId(), invoice.getCurrency().getId(), false));
        parameters.put("totalWithoutTax", Util.formatMoney(invoice.getTotal().subtract(taxTotal),
                invoice.getUserId(), invoice.getCurrency().getId(), false));
        parameters.put("balance", Util.formatMoney(invoice.getBalance(),
                invoice.getUserId(), invoice.getCurrency().getId(), false));
        parameters.put("carriedBalance", Util.formatMoney(invoice.getCarriedBalance(),
                invoice.getUserId(), invoice.getCurrency().getId(), false));
        parameters.put("totalDebitsCredits", Util.formatMoney(totalDebitsCredits,
                invoice.getUserId(), invoice.getCurrency().getId(), false));
        parameters.put("balanceLessDebitsCredits", Util.formatMoney(invoice.getBalance().subtract(totalDebitsCredits),
                invoice.getUserId(), invoice.getCurrency().getId(), false));

        logger.debug("Parameter tax = {} totalWithTax = {} totalWithoutTax = {} balance = {}",
                parameters.get("tax"),
                parameters.get("totalWithTax"),
                parameters.get("totalWithoutTax"),
                parameters.get("balance"));

        // set report locale
        parameters.put(JRParameter.REPORT_LOCALE, locale);

        // set the subreport directory
        String subreportDir = DESIGNS_FOLDER;
        parameters.put("SUBREPORT_DIR", subreportDir);

        //JBFC-460 Handling FC specific invoice parameters through plugin
        CustomInvoiceFieldsEvent event =
                new CustomInvoiceFieldsEvent(entityId,invoice.getUserId(),parameters,null,to,invoice);
        EventManager.process(event);

        // at last, generate the report
        return fillReport(compiledDesign, useSqlQuery, parameters, lines);
    }

    private static JasperPrint fillReport(File design, boolean useQuery, Map<String, Object> parameters,  List<InvoiceLineDTO> lines) {
        try (FileInputStream stream = new FileInputStream(design)) {
            if(useQuery) {
                DataSource dataSource = Context.getBean(Context.Name.DATA_SOURCE);
                // get connection bind to current thread if no connection found then get connection from pool, no need to get new connection directly from pool
                Connection connection = DataSourceUtils.getConnection(dataSource);
                try {
                    logger.debug("Report parameters: {}", parameters);
                    return JasperFillManager.fillReport(stream, parameters, connection);
                } finally {
                    // releases connection if tx not activated else spring tx module take care of connection release.
                    DataSourceUtils.releaseConnection(connection, dataSource);
                }
            }

            JRBeanCollectionDataSource data = new JRBeanCollectionDataSource(lines);
            return JasperFillManager.fillReport(stream, parameters, data);

        } catch (IOException | JRException e) {
            logger.error("Exception generating paper invoice", e);
            throw new SessionInternalError("Could not resolve the invoice design file",
                    new String[]{"invoice.prompt.failure.downloadPdf"});
        }

    }

    private static String getPrimaryAccountNumberByCustomerIdAndEntityId(Integer userId, Integer entityId) {
        if(null == userId && null == entityId) {
            return null;
        }

        String primaryAccountNumber = customerDAS.getPrimaryAccountNumberByUserAndEntityId(userId, entityId);
        return primaryAccountNumber;
    }

    /**
     * Extract various paramteres from the EDI meter read file.
     * @param recordId
     * @param reportParameters
     * @param userId
     */
    private static void extractEdiMeterReadForInvoice(Integer recordId, Map<String, Object> reportParameters, int userId) {
        String sql =
                "select service_start.edi_file_field_value as service_start, service_end.edi_file_field_value as service_end, rate_class.edi_file_field_value as rate_class \n" +
                        "from edi_file f \n" +
                        "join edi_file_record umr on umr.edi_file_id=f.id and umr.edi_file_field_header='UMR' \n" +
                        "join edi_file_field service_start on service_start.edi_file_record_id=umr.id and service_start.edi_file_field_key='START_SERVICE_DT'\n" +
                        "join edi_file_field service_end on service_end.edi_file_record_id=umr.id and service_end.edi_file_field_key='END_SERVICE_DT'\n" +
                        "join edi_file_field summary on summary.edi_file_record_id=umr.id and summary.edi_file_field_key='USAGE_TYPE' and summary.edi_file_field_value in ('SUM', 'ISUM')\n" +
                        "join edi_file_field rate_class on rate_class.edi_file_record_id=umr.id and rate_class.edi_file_field_key='UTILITY_RATE_CLASS' \n" +
                        "where f.id=?";

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyyMMdd");

        DataSource dataSource = (DataSource) Context.getBean(Context.Name.DATA_SOURCE);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(sql);) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery();) {
                while(rs.next()) {
                    reportParameters.put("serviceStart", parseDateNullCheck(dateFormatter, rs.getString("service_start"), userId));
                    reportParameters.put("serviceEnd", parseDateNullCheck(dateFormatter, rs.getString("service_end"), userId));
                    reportParameters.put("rateClass", rs.getString("rate_class"));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new SessionInternalError("Unable to determine start and end date from meter read file.", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        if(!reportParameters.containsKey("serviceStart")) {
            reportParameters.put("serviceStart", "N/A");
            reportParameters.put("serviceEnd", "N/A");
            reportParameters.put("rateClass", "N/A");
        }
    }

    private static String parseDateNullCheck(DateTimeFormatter formatter, String dateString, int userId) {
        if(dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return Util.formatDate(formatter.parseDateTime(dateString).toDate(), userId);
    }

    public static byte[] generateDesignReport(String designName, Map<String, Object> parameters) {
        // This is needed for JasperRerpots to work, for some twisted XWindows issue
        System.setProperty("java.awt.headless", "true");
        String designFile = String.format(DESIGN_PATH, designName);
        File compiledDesign = new File(designFile);
        try(FileInputStream stream = new FileInputStream(compiledDesign)) {
            // at last, generate the report
            DataSource dataSource = Context.getBean(Context.Name.DATA_SOURCE);
            Connection connection = DataSourceUtils.getConnection(dataSource);
            try {
                JasperPrint report = JasperFillManager.fillReport(stream, parameters, connection);
                return JasperExportManager.exportReportToPdf(report);
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        } catch (IOException | JRException e) {
            logger.error("Exception generating paper report", e);
            throw new SessionInternalError("Exception generating paper report", e);
        } catch (RuntimeException e) {
            throw new SessionInternalError(e);
        }
    }

    private static JasperPrint generatePaperInvoiceNew(File compiledDesign, Map<String, Object> parameters,
            boolean useSqlQuery, InvoiceDTO invoice, ContactDTOEx from,
            ContactDTOEx to, String message1, String message2, Integer entityId) {
        Locale locale = (new UserBL(invoice.getUserId())).getLocale();

        fillMetaFields("receiver", new UserBL(to.getUserId()).getUserWS().getMetaFields(), parameters);
        fillMetaFields("owner", new UserBL(from.getUserId()).getUserWS().getMetaFields(), parameters);

        List<MetaFieldValueWS> invoiceMetaFields = new ArrayList<MetaFieldValueWS>();
        for (MetaFieldValue<?> mfv : invoice.getMetaFields()) {
            invoiceMetaFields.add(MetaFieldBL.getWS(mfv));
        }
        fillMetaFields("invoice", invoiceMetaFields, parameters);

        // invoice data
        parameters.put("invoice_id", invoice.getId());
        parameters.put("invoice_number", printable(invoice.getPublicNumber()));
        parameters.put("invoice_create_datetime", Util.formatDate(invoice.getCreateDatetime(), invoice.getUserId()));
        parameters.put("invoice_dueDate", Util.formatDate(invoice.getDueDate(), invoice.getUserId()));

        BillingProcessDTO bp = invoice.getBillingProcess();
        if (bp == null) {
            bp = invoice.getInvoice() == null ? null : invoice.getInvoice().getBillingProcess();
        }
        if (bp == null) {
            parameters.put("billing_date", Util.formatDate(TimezoneHelper.companyCurrentDate(entityId), invoice.getUserId()));
            parameters.put("billing_period_end_date", Util.formatDate(new Date(), invoice.getUserId()));
        } else {
            parameters.put("billing_date", Util.formatDate(bp.getBillingDate(), invoice.getUserId()));
            parameters.put("billing_period_end_date", Util.formatDate(bp.getBillingPeriodEndDate(), invoice.getUserId()));
        }

        // owner and receiver data
        setParametersForContact(from, parameters, "owner", false);

        setParametersForContact(to, parameters, "receiver", true);
        parameters.put("owner_company", printable(from.getOrganizationName()));
        parameters.put("owner_street_address", getAddress(from));
        parameters.put("owner_zip", printable(from.getPostalCode()));
        parameters.put("owner_city", printable(from.getCity()));
        parameters.put("owner_state", printable(from.getStateProvince()));
        parameters.put("owner_country", printable(from.getCountryCode()));
        parameters.put("owner_phone", getPhoneNumber(from));
        parameters.put("owner_email", printable(from.getEmail()));

        parameters.put("receiver_company", printable(to.getOrganizationName()));
        parameters.put("receiver_name", printable(to.getFirstName(), to.getLastName()));
        parameters.put("receiver_street1",printable(to.getAddress1()));
        parameters.put("receiver_street2",printable(to.getAddress2()));
        parameters.put("receiver_street_address",getAddress(to));
        parameters.put("receiver_zip", printable(to.getPostalCode()));
        parameters.put("receiver_city", printable(to.getCity()));
        parameters.put("receiver_state", printable(to.getStateProvince()));
        parameters.put("receiver_country", printable(to.getCountryCode()));
        parameters.put("receiver_phone", getPhoneNumber(to));
        parameters.put("receiver_email", printable(to.getEmail()));
        parameters.put("receiver_id", printable(String.valueOf(to.getId())));

        // symbol of the currency
        String symbol = evaluateCurrencySymbol(invoice);
        parameters.put("currency_symbol",symbol);

        // text coming from the notification parameters
        parameters.put("message1", printable(message1));
        parameters.put("message2", printable(message2));
        parameters.put("customer_notes", "HST: 884725441");            //todo: change this static value

        // invoice notes stripped of html line breaks
        String notes = invoice.getCustomerNotes();
        if (notes != null) {
            notes = notes.replaceAll("<br/>", "\r\n");
        }
        parameters.put("invoice_notes", printable(notes));

        // the logo is a file
        File logo = LogoType.INVOICE.getFile(entityId);
        if (!logo.exists()) {
            logger.warn("Logo file (entity-{}.(png/jpg)) not found under {}logos/ folder", entityId, BASE_DIR);
            parameters.put("LOGO", null);
        } else {
            parameters.put("LOGO", logo);
        }


        // tax calculated
        BigDecimal taxTotal = new BigDecimal(0);
        String tax_price = "";
        String tax_amount = "";
        String product_code;
        List<InvoiceLineDTO> lines = new ArrayList<InvoiceLineDTO>(invoice.getInvoiceLines());
        // Temp change: sort is leading to NPE
        //Collections.sort(lines, new InvoiceLineComparator());
        for (InvoiceLineDTO line: lines) {
            // process the tax, if this line is one
            if (line.getInvoiceLineType() != null && // for headers/footers
                    line.getInvoiceLineType().getId() ==
                    Constants.INVOICE_LINE_TYPE_TAX) {
                // update the total tax variable
                taxTotal = taxTotal.add(line.getAmount());
                product_code = line.getItem() != null ? line.getItem().getInternalNumber() : line.getDescription();
                tax_price += product_code+" "+line.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString()+" %\n";
                tax_amount += symbol+" "+line.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toString()+"\n" ;
            }
        }
        tax_price = (tax_price.equals(""))?"0.00 %":tax_price.substring(0,tax_price.lastIndexOf("\n"));
        tax_amount = (tax_amount.equals(""))?symbol+" 0.00":tax_amount.substring(0,tax_amount.lastIndexOf("\n"));
        parameters.put("sales_tax",taxTotal);
        parameters.put("tax_price", printable(tax_price));
        parameters.put("tax_amount", printable(tax_amount));

        // this parameter help in filter out tax items from invoice lines
        parameters.put("invoice_line_tax_id", Constants.INVOICE_LINE_TYPE_TAX);

        //payment term calculated
        parameters.put("payment_terms",new Long(((invoice.getDueDate().getTime()-invoice.getCreateDatetime().getTime())/(24*60*60*1000))).toString());

        // set report locale
        parameters.put(JRParameter.REPORT_LOCALE, locale);
        parameters.put(FormatUtil.PARAMETER_NAME, new FormatUtil(locale, symbol));

        // set the subreport directory
        String subreportDir = DESIGNS_FOLDER;
        parameters.put("SUBREPORT_DIR", subreportDir);

        logger.debug("Parameters passed to invoice design are : {}", parameters);
        //JBFC-460 Handling FC specific invoice parameters through plugin
        CustomInvoiceFieldsEvent event =
                new CustomInvoiceFieldsEvent(entityId,invoice.getUserId(),parameters,null,to,invoice);
        EventManager.process(event);

        return fillReport(compiledDesign, useSqlQuery, parameters, lines);
    }

    private static String evaluateCurrencySymbol(InvoiceDTO invoice) {
        CurrencyBL currency = new CurrencyBL(invoice.getCurrency().getId());
        String symbol = currency.getEntity().getSymbol();
        if (symbol.length() >= 4 && symbol.charAt(0) == '&' &&
                symbol.charAt(1) == '#') {
            // this is an html symbol
            // remove the first two digits
            symbol = symbol.substring(2);
            // remove the last digit (;)
            symbol = symbol.substring(0, symbol.length() - 1);
            // convert to a single char
            Character ch = new Character((char)
                    Integer.valueOf(symbol).intValue());
            symbol = ch.toString();
        }
        return symbol;
    }

    private static void setParametersForContact(ContactDTOEx contact, Map<String, Object> parameters, String contactRule, boolean showName) {
        parameters.put(contactRule + "_company", printable(contact == null ? "" : contact.getOrganizationName()));
        if (showName) {
            parameters.put(contactRule + "_name", printable(contact == null ? "" : contact.getFirstName(),
                    contact == null ? "" : contact.getLastName()));
        }
        parameters.put(contactRule + "_street_address", printable(contact == null ? "" : getAddress(contact)));
        parameters.put(contactRule + "_zip", printable(contact == null ? "" : contact.getPostalCode()));
        parameters.put(contactRule + "_city", printable(contact == null ? "" : contact.getCity()));
        parameters.put(contactRule + "_state", printable(contact == null ? "" : contact.getStateProvince()));
        parameters.put(contactRule + "_country", printable(contact == null ? "" : contact.getCountryCode()));
        parameters.put(contactRule + "_phone", printable(contact == null ? "" : getPhoneNumber(contact)));
        parameters.put(contactRule + "_email", printable(contact == null ? "" : contact.getEmail()));
    }

    private static void fillMetaFields(String prefix,  MetaFieldValueWS[] metaFields, Map<String, Object> parameters) {
        fillMetaFields(prefix, (null != metaFields ? Arrays.asList(metaFields) : Collections.emptyList()), parameters);
    }

    private static void fillMetaFields(String prefix, Collection<MetaFieldValueWS> metaFields, Map<String, Object> parameters) {
        if (metaFields == null) {
            return;
        }
        for (MetaFieldValueWS mfv : metaFields) {
            String name = mfv.getFieldName().replace('.', '_').replace(' ', '_');
            String value = mfv.getValue() == null ? "" : String.valueOf(mfv.getValue());
            parameters.put("__" + prefix + "__" + name, value);
        }
    }

    public static String printable(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    private static String getPhoneNumber(ContactDTOEx contact){
        if(contact.getPhoneCountryCode()!=null && contact.getPhoneAreaCode()!=null && (contact.getPhoneNumber()!=null && !contact.getPhoneNumber().trim().equals(""))) {
            return  contact.getPhoneCountryCode()+"-"+contact.getPhoneAreaCode()+"-"+contact.getPhoneNumber();
        } else {
            return "";
        }
    }

    private static String getAddress(ContactDTOEx contact){
        return printable(contact.getAddress1())+((contact.getAddress2()!=null && !contact.getAddress2().trim().equals(""))?(", "+contact.getAddress2()):(""));
    }

    /**
     * Safely concatenates 2 strings together with a blank space (" "). Null strings
     * are handled safely, and no extra concatenated character will be added if one
     * string is null.
     *
     * @param str
     * @param str2
     * @return concatenated, printable string
     */
    private static String printable(String str, String str2) {
        StringBuilder builder = new StringBuilder();

        if (str != null) {
            builder.append(printable(str)).append(' ');
        }
        if (str2 != null) {
            builder.append(printable(str2));
        }

        return builder.toString();
    }

    public static void sendSapienterEmail(Integer entityId, String messageKey,
            String attachmentFileName, String[] params)
                    throws MessagingException, IOException {
        String address = null;

        ContactBL contactBL = new ContactBL();
        contactBL.setEntity(entityId);

        EntityBL entityBL = new EntityBL(entityId);

        if(CommonConstants.NOTIFICATION_EMAIL_ERROR.equals(messageKey)) {
            address = entityBL.getEntity().getFailedEmailNotification();
        } else {
            address = contactBL.getEntity().getEmail();
        }

        if (address == null) {
            // can't send something to the ether
            logger.warn("Trying to send email to entity {} but no address was found", entityId);
            return;
        }
        sendSapienterEmail(address, entityId, messageKey, attachmentFileName,
                params);
    }

    /**
     * This method is intended to be used to send an email from the system to
     * the entity. This is different than from the entity to a customer, which
     * should use a notification pluggable task. The file
     * entityNotifications.properties has to have key + "_subject" and key +
     * "_body" Note: For any truble, the best documentation is the source code
     * of the MailTag of Jakarta taglibs
     */
    public static void sendSapienterEmail(String address, Integer entityId,
            String messageKey, String attachmentFileName, String[] params)
                    throws MessagingException, IOException {
        Properties prop = new Properties();

        logger.debug("sending sapienter email {} to {} of entity {}", messageKey, address, entityId);
        // tell the server that is has to authenticate to the maileer
        // (yikes, this was painfull to find out)
        String smtpUsername = getSysProp("smtp_username");
        if(smtpUsername != null && smtpUsername.trim().length() > 0) {
            prop.setProperty("mail.smtp.auth", "true");
        }

        // create the session & message
        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(getSysProp("email_from"), getSysProp("email_from_name")));
        // the to address
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
                address, false));
        // the subject and body are international
        EntityBL entity = new EntityBL(entityId);
        Locale locale = entity.getLocale();

        ResourceBundle rBundle = ResourceBundle.getBundle("entityNotifications", locale);
        String subject = rBundle.getString(messageKey + "_subject");
        String message = rBundle.getString(messageKey + "_body");

        // if there are parameters, replace them
        if (params != null) {
            for (int f = 0; f < params.length; f++) {
                message = message.replaceFirst("\\|X\\|", params[f]);
            }
        }

        msg.setSubject(subject);

        if (attachmentFileName == null) {
            msg.setText(message);
        } else {
            // it is a 'multi part' email
            MimeMultipart mp = new MimeMultipart();

            // the text message is one part
            MimeBodyPart text = new MimeBodyPart();
            text.setDisposition(Part.INLINE);
            text.setContent(message, "text/plain");
            mp.addBodyPart(text);

            // the attachement is another.
            MimeBodyPart file_part = new MimeBodyPart();
            File file = new File(attachmentFileName);
            FileDataSource fds = new FileDataSource(file);
            DataHandler dh = new DataHandler(fds);
            file_part.setFileName(file.getName());
            file_part.setDisposition(Part.ATTACHMENT);
            file_part.setDescription("Attached file: " + file.getName());
            file_part.setDataHandler(dh);
            mp.addBodyPart(file_part);

            msg.setContent(mp);
        }

        // the date
        msg.setSentDate(Calendar.getInstance().getTime());

        logger.debug("Message: {}", msg);
        logger.debug("MessageText: {}", message);
        logger.debug("Address: {}", address);

        Transport transport = session.getTransport("smtp");
        transport.connect(getSysProp("smtp_server"),
                Integer.parseInt(getSysProp("smtp_port")),
                getSysProp("smtp_username"),
                getSysProp("smtp_password"));
        InternetAddress addresses[] = InternetAddress.parse(address);
        transport.sendMessage(msg, addresses);
    }

    /**
     * Creates a message object with a set of standard parameters
     * @param entityId
     * @param userId
     * @return The message object with many useful parameters
     */
    private MessageDTO initializeMessage(Integer entityId, Integer userId)
            throws SessionInternalError {
        MessageDTO retValue = new MessageDTO();
        try {
            UserDAS userDAS = new UserDAS();
            UserDTO user = userDAS.findNow(userId);
            userDAS.reattach(user);

            ContactBL contact = new ContactBL();

            // this user's info
            contact.set(userId);

            ContactDTOEx userContact= ( null != contact.getEntity() ? contact.getDTO() : null );
            if ( null == userContact ) {
                userContact= ContactBL.buildFromMetaField(userId, TimezoneHelper.companyCurrentDate(entityId));
            }

            if (null != userContact ) {
                retValue.addParameter("contact", userContact);
                retValue.addParameter("email", printable(userContact.getEmail()));

                if ( null == StringUtils.trimToNull(userContact.getFirstName()) && null == StringUtils.trimToNull(userContact.getLastName()) ) {
                    retValue.addParameter("first_name", UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user));
                    retValue.addParameter("last_name", "");
                } else {
                    retValue.addParameter("first_name", printable(userContact.getFirstName()));
                    retValue.addParameter("last_name", printable(userContact.getLastName()));
                }

                retValue.addParameter("address1", printable(userContact.getAddress1()));
                retValue.addParameter("address2", printable(userContact.getAddress2()));
                retValue.addParameter("city", printable(userContact.getCity()));
                retValue.addParameter("organization_name", printable(userContact.getOrganizationName()));
                retValue.addParameter("postal_code", printable(userContact.getPostalCode()));
                retValue.addParameter("state_province", printable(userContact.getStateProvince()));
            }

            if (user.getEntity() != null) {
                retValue.addParameter("user", user);

                retValue.addParameter("username", UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user));
                //retValue.addParameter("password", user.getEntity().getPassword());
                retValue.addParameter("user_id", user.getUserId().toString());

                //payment instrument

                UserBL userBL = new UserBL(user);

                //1. credit card
                if ( CollectionUtils.isNotEmpty(userBL.getAllCreditCards()) ) {
                    PaymentInformationDTO instrument= userBL.getAllCreditCards().get(0);
                    if ( null != instrument && !(instrument.isMetaFieldEmpty()) ) {
                        PaymentInformationBL piBl = new PaymentInformationBL();
                        PaymentInformationWS paymentInformationWS = PaymentInformationBL.getWS(instrument);
                        piBl.obscureCreditCardNumber(paymentInformationWS);
                        retValue.addParameter("credit_card", paymentInformationWS);

                        char[] number = piBl.getCharMetaFieldByType(paymentInformationWS, MetaFieldType.PAYMENT_CARD_NUMBER);
                        if(number ==null || number.length<=0) {
                            retValue.addParameter("credit_card.ccNumberPlain","");
                        } else {
                            retValue.addParameter("credit_card.ccNumberPlain",new String(number,number.length-4, 4));
                        }

                        char[] expiryDate = piBl.getCharMetaFieldByType(paymentInformationWS, MetaFieldType.DATE);
                        if(piBl.hasCharMetaFieldOnlySpaces(expiryDate)
                                || expiryDate == null
                                || expiryDate.length<=0) {
                            retValue.addParameter("credit_card.ccExpiry", "");
                        } else {
                            retValue.addParameter("credit_card.ccExpiry", printable(piBl.get4digitExpiry(paymentInformationWS)));
                        }

                    }
                }
            }

            if (null != retValue) {
                //Token for Usage Minute Left.
                BigDecimal quantity = BigDecimal.ZERO;
                UserBL userBl = new UserBL(userId);
                UserDTO userdDto = userBl.getDto();
                quantity = getUsageUnitsLeftToken(userdDto);
                retValue.addParameter("usageMinutesLeft", quantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,Constants.BIGDECIMAL_ROUND));

                //Token for overageCharges which will currently not available on tokens list, this would be used as per the requirement.
                retValue.addParameter("overageCharges", Util.formatMoney(userBl.getOverageCharges(),
                        userdDto.getUserId(), userdDto.getCurrency().getId(), true));

                //Token for overage rate per minute rate.
                BigDecimal overageRatePerMinute = userBl.getOverageRatePerMinute();
                String overageRateValue = overageRatePerMinute != null ?
                        Util.formatMoney(overageRatePerMinute, userdDto.getUserId(), userdDto.getCurrencyId(), true) : "";
                retValue.addParameter("overageRatePerMinute", overageRateValue);
            }

            // the entity info
            contact.setEntity(entityId);
            if (contact.getEntity() != null) {
                retValue.addParameter("company_contact", contact.getEntity());

                retValue.addParameter("company_id", entityId.toString());
                retValue.addParameter("company_name", contact.getEntity().getOrganizationName());
                retValue.addParameter("url", Holders.getFlatConfig().get("grails.serverURL"));
                retValue.addParameter("resetPassExpHours", PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId,
                        CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION));
            }

            CustomerDTO customerDTO = user.getCustomer();
            if(null != customerDTO){
                Set<CustomerAccountInfoTypeMetaField> currentCustomerAitMetaFields = new HashSet<>();
                for (AccountInformationTypeDTO ait : customerDTO.getAccountType().getInformationTypes()) {
                    Date effectiveDate = customerDTO.getEffectiveDateByGroupIdAndDate(ait.getId(), new Date());
                    currentCustomerAitMetaFields.addAll(customerDTO.getCustomerAccountInfoTypeMetaFields(ait.getId(), effectiveDate));
                }

                for (CustomerAccountInfoTypeMetaField customerAitMetaField : currentCustomerAitMetaFields) {
                    String parameterName = customerAitMetaField.getMetaFieldValue().getField().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                    String parameterValue = customerAitMetaField.getMetaFieldValue().getValue() != null ? customerAitMetaField.getMetaFieldValue().getValue().toString() : "";
                    /*
                     * Adding all meta fields with account information name as prefix. So that if account type has different addresses for billing and service
                     * then those will be accessible via ait name as prefix.
                     * */
                    if (customerAitMetaField.getAccountInfoType() != null && customerAitMetaField.getAccountInfoType().getName() != null) {
                        String prefix = customerAitMetaField.getAccountInfoType().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                        retValue.addParameter(prefix + "_" + parameterName, parameterValue);
                    }
                }
            }

            //Adding a CCF Field to Email Template
            if (user.getCustomer() != null && user.getCustomer().getMetaFields() != null) {
                for (MetaFieldValue metaFieldValue : user.getEntity().getMetaFields()) {
                    retValue.addParameter(metaFieldValue.getField().getName(), metaFieldValue.getValue());
                }
            }

            //Adding a Customer Level Meta Fields to Email Template
            if (user.getCustomer() != null && user.getCustomer().getMetaFields() != null) {
                for (MetaFieldValue metaFieldValue : user.getCustomer().getMetaFields()) {
                    retValue.addParameter(metaFieldValue.getField().getName(), metaFieldValue.getValue());
                }
            }

            logger.debug("Retvalue >>>> {}", retValue.toString());

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
        return retValue;
    }

    public MessageDTO getBelowThresholdMessage(Integer entityId, Integer userId,
            BigDecimal thresholdAmt, BigDecimal balance) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(MessageDTO.TYPE_BAL_BELOW_THRESHOLD_EMAIL);

        try {
            UserBL user = new UserBL(userId);
            setContent(message, message.getTypeId(), entityId, user.getLanguage());

            Integer customerId = user.getEntity().getCustomer().getId();
            String firstName = getStringMetaFieldValue(customerId, MetaFieldType.FIRST_NAME);
            String lastName = getStringMetaFieldValue(customerId, MetaFieldType.LAST_NAME);

            String salutation = "";
            if (null != firstName && null != lastName
                    && !firstName.trim().isEmpty()
                    && !lastName.trim().isEmpty()) {
                salutation = firstName + " " + lastName;
            } else {
                salutation = UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getEntity().getCompany().getId())
                        .getDisplayName(user.getEntity());
            }

            message.addParameter("userSalutation", salutation);
            message.addParameter("thresholdAmt", Util.decimal2string(thresholdAmt, user.getLocale()));
            message.addParameter("lowBalanceThreshold", Util.decimal2string(balance, user.getLocale()));

        } catch (Exception e1) {
            throw new SessionInternalError(e1);
        }

        return message;

    }

    /**
     * Create {@code MessageDTO} instance for credit limitation 1 or 2 notification.
     *
     * @param messageType type of messages can be {@MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_1} or {@MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_2}
     * @param entityId the id of entity
     * @param userId the id of user
     * @param creditNotificationLimit the value of credit limitation for displaying at message notification
     * @param balance The value of balance
     * @return new created {@code MessageDTO} instance
     * @throws SessionInternalError
     * @throws NotificationNotFoundException
     */
    public MessageDTO getCreditLimitationMessage(Integer messageType, Integer entityId, Integer userId,
            BigDecimal creditNotificationLimit, BigDecimal balance)
                    throws SessionInternalError, NotificationNotFoundException {

        MessageDTO message = initializeMessage(entityId, userId);
        message.setTypeId(messageType);

        try {
            UserBL user = new UserBL(userId);
            setContent(message, message.getTypeId(), entityId, user.getLanguage());

            String salutation = "";
            ContactDTO contact = user.getEntity().getContact();
            if (contact != null && null != contact.getFirstName() && null != contact.getLastName()) {
                salutation = contact.getFirstName() + " " + contact.getLastName();
            } else {
                salutation = UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getEntity().getCompany().getId())
                        .getDisplayName(user.getEntity());
            }

            message.addParameter("userSalutation", salutation);
            message.addParameter("creditNotificationLimit", Util.decimal2string(creditNotificationLimit, user.getLocale()));
            message.addParameter("dynamicBalance", Util.decimal2string(balance, user.getLocale()));

        } catch (Exception e1) {
            throw new SessionInternalError(e1);
        }

        return message;

    }

    public String generatePaperInvoiceAsFile(InvoiceDTO invoice) {
        try {
            Integer entityId = invoice.getBaseUser().getEntity().getId();

            // the language doesn't matter when getting a paper invoice
            MessageDTO paperMsg = getInvoicePaperMessage(entityId, null,
                    invoice.getBaseUser().getLanguageIdField(), invoice);
            return loadPaperInvoiceNotificationTaskForEntity(entityId)
                    .getPDFFile(invoice.getBaseUser(), paperMsg);
        } catch(SessionInternalError error) {
            throw error;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Returns file name of the generated PDF File.
     */
    private String createPaymentAttachment(PaymentDTOEx dto, UserBL user, String attachmentDesign, String attachmentType, ContactDTOEx to) {
        //use PaymentDTOEx object bring payment information and populate in the design
        String fileName;
        try {
            UserDTO userDTO = user.getDto();
            // This is needed for JasperRerpots to work, for some twisted XWindows issue
            System.setProperty("java.awt.headless", "true");
            logger.debug("Base Dir: {}", BASE_DIR);
            String designFile = String.format(DESIGN_PATH, attachmentDesign);

            File compiledDesign = new File(designFile);
            logger.debug("Generating payment notification with design file : {}", designFile);
            logger.debug("User is {}", userDTO.getUserName());
            FileInputStream reportDesign = new FileInputStream(compiledDesign);

            HashMap<String, Object> parameters = new HashMap<String, Object>();
            logger.debug("Payment Amount: {}", dto.getAmount());

            logger.debug("Currency from user dto ---- {}", userDTO.getCurrency().getCode());

            logger.debug("The payment was made in currency {}", dto.getCurrency().getCode());

            logger.debug("payment date is {}", dto.getPaymentDate());
            parameters.put("payment_id", dto.getId());
            parameters.put("paymentDate", dto.getPaymentDate());
            parameters.put("paymentCurrency", userDTO.getCurrency().getSymbol());
            parameters.put("userId", userDTO.getId());
            //payment and deposit date would be same in this case
            parameters.put("depositDate", dto.getPaymentDate());
            parameters.putAll(createAitDynamicParameters(userDTO,userDTO.getEntity().getId(),dto));
            if (null != to) {
                parameters.put("customerAddress1", printable(to.getAddress1()));
                parameters.put("customerAddress2", printable(to.getAddress2()));
                parameters.put("customerCity", printable(to.getCity()));
                parameters.put("customerProvince", printable(to.getStateProvince()));
                parameters.put("customerPostalCode", printable(to.getPostalCode()));
                parameters.put("customerCountry", printable(to.getCountryCode()));
                parameters.put("customerName", printable(to.getFirstName(), to.getLastName()));

                logger.debug("Customer Address IS: {}", parameters.get("customerAddress"));

                parameters.put("organizationName", to.getOrganizationName());
            }
            // the logo is a file
            File logo = LogoType.INVOICE.getFile(userDTO.getEntity().getId());
            if (logo.exists()) {
                parameters.put("entityLogo", logo);
            } else {
                logger.warn("Logo file (entity-{}.(png/jpg)) not found under {}logos/ folder", userDTO.getEntity().getId(), BASE_DIR);
            }

            //JasperPrint jasperPrintReport = JasperFillManager.fillReport(reportDesign, parameters);
            final JasperPrint jasperPrintReport = fillReport(compiledDesign,true, parameters,null);
            fileName = BASE_DIR + "notifications" + File.separator
                    + userDTO.getUserName() + File.separator;

            if (!new File(fileName).exists()) {
                new File(fileName).mkdir();
            }

            //fileName += "Invoice-" + dto.getPublicNumber() + ".pdf";
            fileName += "Invoice-" + dto.getId() + ".pdf";

            JasperExportManager.exportReportToPdfFile(jasperPrintReport, fileName);

        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new SessionInternalError(e);
        }
        //the data required by the design is set into a HashMap see e.g. _generatePaperInvoiceAsFile_
        return fileName;
    }

    private String getStringMetaFieldValue(Integer customerId, MetaFieldType type){
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        List<Integer> values = metaFieldDAS.getCustomerFieldValues(customerId, type);
        Integer valueId = null != values && values.size() > 0 ? values.get(0) : null;
        MetaFieldValue valueField = null != valueId ? metaFieldDAS.getStringMetaFieldValue(valueId) : null;
        return null != valueField ? (String) valueField.getValue() : null;
    }

    /**
     * This method is intended to be used to send an email from the system to
     * the entity. This is different than from the entity to a customer, which
     * should use a notification pluggable task. The file
     * entityNotifications.properties has to have key + "_subject" and key +
     * "_body" Note: For any truble, the best documentation is the source code
     * of the MailTag of Jakarta taglibs
     */
    public static void sendSapienterGmail(String address, Integer entityId,
            String messageKey, String attachmentFileName, String[] params)
                    throws MessagingException, IOException {
        Properties prop = new Properties();

        logger.debug("seding sapienter email {} to {} of entity {}", messageKey, address, entityId);
        // tell the server that is has to authenticate to the maileer
        // (yikes, this was painfull to find out)
        prop.setProperty("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        // create the session & message
        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(getSysProp("email_from"), getSysProp("email_from_name")));
        // the to address
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
                address, false));
        // the subject and body are international
        EntityBL entity = new EntityBL(entityId);
        Locale locale = entity.getLocale();

        ResourceBundle rBundle = ResourceBundle.getBundle("entityNotifications", locale);
        String subject = rBundle.getString(messageKey + "_subject");
        String message = rBundle.getString(messageKey + "_body");

        // if there are parameters, replace them
        if (params != null) {
            for (int f = 0; f < params.length; f++) {
                message = message.replaceFirst("\\|X\\|", params[f]);
            }
        }

        msg.setSubject(subject);

        if (attachmentFileName == null) {
            msg.setText(message);
        } else {
            // it is a 'multi part' email
            MimeMultipart mp = new MimeMultipart();

            // the text message is one part
            MimeBodyPart text = new MimeBodyPart();
            text.setDisposition(Part.INLINE);
            text.setContent(message, "text/plain");
            mp.addBodyPart(text);

            // the attachement is another.
            MimeBodyPart file_part = new MimeBodyPart();
            File file = new File(attachmentFileName);
            FileDataSource fds = new FileDataSource(file);
            DataHandler dh = new DataHandler(fds);
            file_part.setFileName(file.getName());
            file_part.setDisposition(Part.ATTACHMENT);
            file_part.setDescription("Attached file: " + file.getName());
            file_part.setDataHandler(dh);
            mp.addBodyPart(file_part);

            msg.setContent(mp);
        }

        // the date
        msg.setSentDate(Calendar.getInstance().getTime());

        logger.debug("Message: {}", msg);
        logger.debug("MessageText: {}", message);
        logger.debug("Address: {}", address);

        Transport transport = session.getTransport("smtp");
        transport.connect(getSysProp("smtp_server"),
                Integer.parseInt(getSysProp("smtp_port")),
                getSysProp("smtp_username"),
                getSysProp("smtp_password"));
        InternetAddress addresses[] = new InternetAddress[1];
        addresses[0] = new InternetAddress(address);
        transport.sendMessage(msg, addresses);
    }

    private void setPaymentMessageTokenValues(UserBL user,PaymentDTOEx dto,MessageDTO message,PaymentMethodDTO paymentMethod){
        //Token for Payment/Refund Amount, Date.
        UserDTO userdDto = user.getDto();
        Date companyCurrentDate = TimezoneHelper.companyCurrentDate(userdDto.getCompany().getId());
        if (dto.getIsRefund() == 1) {
            message.addParameter("refundAmount", Util.formatMoney(dto.getAmount(),
                    dto.getUserId(), dto.getCurrency().getId(), true));
            message.addParameter("refundDate", Util.formatDate((null !=dto.getCreateDatetime() ?
                    dto.getCreateDatetime() : companyCurrentDate), dto.getUserId()));
        } else {
            message.addParameter("paymentAmount", Util.formatMoney(dto.getAmount(),
                    dto.getUserId(), dto.getCurrency().getId(), true));
            message.addParameter("paymentDate", Util.formatDate((null !=dto.getCreateDatetime() ?
                    dto.getCreateDatetime() : companyCurrentDate), dto.getUserId()));
        }

        //Token for Usage Minute Left.
        BigDecimal quantity = BigDecimal.ZERO;
        quantity = getUsageUnitsLeftToken(userdDto);
        message.addParameter("usageMinutesLeft", quantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,Constants.BIGDECIMAL_ROUND));

        //Token for Payment Info Cheque.
        String paymentInfoCheque = "";
        if (null != paymentMethod && Constants.CHEQUE.equals(paymentMethod.getDescription()) && null != dto.getCreditCard()) {
            PaymentInformationDTO instrument = dto.getCreditCard();
            if (null != instrument) {
                PaymentInformationBL piBl = new PaymentInformationBL();
                paymentInfoCheque = "Cheque Date \t"+
                        piBl.getDateMetaFieldByType(instrument,MetaFieldType.DATE)+
                        "\nCheque Number \t"+
                        piBl.getStringMetaFieldByType(instrument,MetaFieldType.CHEQUE_NUMBER);
            }
        }
        message.addParameter("paymentInfoCheque", paymentInfoCheque);

        //Token for overageCharges which will currently not available on tokens list, this would be used as per the requirement.
        message.addParameter("overageCharges", Util.formatMoney(user.getOverageCharges(),
                dto.getUserId(), dto.getCurrency().getId(), true));

        //Token for Overage rate per minute.
        BigDecimal overageRatePerMinute = user.getOverageRatePerMinute();
        String overageRateValue = overageRatePerMinute != null ?
                Util.formatMoney(overageRatePerMinute, dto.getUserId(), dto.getCurrency().getId(), true) : "";
        message.addParameter("overageRatePerMinute", overageRateValue);
    }

    private BigDecimal getUsageUnitsLeftToken(UserDTO userdDto ){
        BigDecimal quantity = BigDecimal.ZERO;
        if (null != userdDto.getCustomer()) {
            List<CustomerUsagePoolDTO> customerUsagePools = new CustomerUsagePoolBL().getCustomerUsagePoolsByCustomerId(userdDto.getCustomer().getId());
            for (CustomerUsagePoolDTO customerUsagePool : customerUsagePools) {
                if(!customerUsagePool.doesUsagePoolContainSkippedProduct(userdDto.getEntity().getId())) {
                    quantity = quantity.add(customerUsagePool.getQuantity());
                }
            }
        }
        return quantity;
    }

    public MessageDTO getSSOEnabledUserCreatedEmailMessage(Integer entityId,
            Integer userId, Integer languageId) throws SessionInternalError,
            NotificationNotFoundException {
        MessageDTO message = initializeMessage(entityId, userId);

        message.setTypeId(MessageDTO.TYPE_SSO_ENABLED_USER_CREATED_EMAIL);

        setContent(message, MessageDTO.TYPE_SSO_ENABLED_USER_CREATED_EMAIL, entityId,
                languageId);

        return message;
    }

    private Date getLastPaymentDate(Integer userId){
        PaymentBL paymentBL = new PaymentBL();
        Integer paymentId = paymentBL.getLatest(userId);
        if (paymentId != null) {
            return new PaymentDAS().find(paymentId).getPaymentDate();
        }

        return null;
    }

    public MessageDTO getAbsaPaymentsFailedNotificationMessage(List<Integer> paymentIds, String clientCode,
            Integer entityId, String transmissionDate) throws NotificationNotFoundException {

        MessageDTO message = new MessageDTO();
        message.addParameter("failed_payment_count", paymentIds.size());
        message.addParameter("client_code",clientCode);
        message.addParameter("transmission_date", null!= transmissionDate? transmissionDate:null);

        StringBuilder builder = new StringBuilder();

        CompanyDAS companyDAS = new CompanyDAS();
        CompanyDTO companyDTO = companyDAS.findEntityByName(companyDAS.findCompanyNameByEntityId(entityId));
        Integer languageId = companyDTO.getLanguage().getId();

        for (Integer paymentId: paymentIds) {
            PaymentBL paymentBL = new PaymentBL(paymentId);
            PaymentDTOEx payment = paymentBL.getDTOEx(languageId);

            builder.append("Payment id: " +payment.getId() +", ");
            builder.append("User id: " + payment.getUserId()+", ");
            builder.append("Payment amount: " + payment.getAmount() + ", ");

            for (MetaFieldValue metaFieldValue: payment.getMetaFields()){
                String metaFieldName = metaFieldValue.getField().getName();

                if(metaFieldName.equals(IgnitionConstants.PAYMENT_ACTION_DATE) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_CLIENT_CODE) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_SENT_ON) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_USER_REFERENCE)
                        ){
                    builder.append(metaFieldName + ": "+metaFieldValue.getValue()+", ");
                }
            }

            builder.append("Error Code: " + payment.getAuthorization().getCode1() +", ");
            builder.append("Error details: " + payment.getAuthorization().getResponseMessage());

            builder.append("\n");
        }

        message.addParameter("details",builder.toString());

        NotificationMessageDAS das = new NotificationMessageDAS();
        NotificationMessageTypeDTO notificationMessageTypeDTO = null;
        List<NotificationMessageDTO> list = das.findByCompanyId(entityId);
        for(NotificationMessageDTO dto: list){
            if(dto.getNotificationMessageType() != null && (IgnitionConstants.ABSA_PAYMENTS_FAILED_NOTIFICATION.equals(dto.getNotificationMessageType().getDescription(languageId)))) {
                notificationMessageTypeDTO = dto.getNotificationMessageType();
                break;
            }
        }
        if(notificationMessageTypeDTO != null) {
            message.setTypeId(notificationMessageTypeDTO.getId());
        }

        setContent(message, message.getTypeId(), entityId, companyDTO.getLanguage().getId());
        return message;
    }

    public MessageDTO getStandardBankPaymentsFailedNotificationMessage(List<Integer> paymentIds, Integer fileSequenceNo,
            Integer entityId) throws NotificationNotFoundException {

        MessageDTO message = new MessageDTO();
        message.addParameter("failed_payment_count", paymentIds.size());
        message.addParameter("file_sequence_number",fileSequenceNo);

        StringBuilder builder = new StringBuilder();

        CompanyDAS companyDAS = new CompanyDAS();
        CompanyDTO companyDTO = companyDAS.findEntityByName(companyDAS.findCompanyNameByEntityId(entityId));
        Integer languageId = companyDTO.getLanguage().getId();

        for (Integer paymentId: paymentIds) {

            PaymentBL paymentBL = new PaymentBL(paymentId);
            PaymentDTOEx payment = paymentBL.getDTOEx(languageId);

            builder.append("Payment id: " +payment.getId() +", ");
            builder.append("User id: " + payment.getUserId()+", ");
            builder.append("Payment amount: " + payment.getAmount()+", ");

            for (MetaFieldValue metaFieldValue: payment.getMetaFields()){
                String metaFieldName = metaFieldValue.getField().getName();
                if(metaFieldName.equals(IgnitionConstants.PAYMENT_ACTION_DATE) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_CLIENT_CODE) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_SENT_ON) ||
                        metaFieldName.equals(IgnitionConstants.PAYMENT_USER_REFERENCE)
                        ){
                    builder.append(metaFieldName +": "+ metaFieldValue.getValue() +", ");
                }
            }

            builder.append("Error Code: " + payment.getAuthorization().getCode1() +", ");
            builder.append("Error details: " + payment.getAuthorization().getResponseMessage());

            builder.append("\n");
        }

        message.addParameter("details",builder.toString());

        NotificationMessageDAS das = new NotificationMessageDAS();
        NotificationMessageTypeDTO notificationMessageTypeDTO = null;
        List<NotificationMessageDTO> list = das.findByCompanyId(entityId);
        for(NotificationMessageDTO dto: list){
            if(dto.getNotificationMessageType() != null &&
                    (IgnitionConstants.STANDARD_BANK_PAYMENTS_FAILED_NOTIFICATION.equals(dto.getNotificationMessageType().getDescription(languageId)))) {
                notificationMessageTypeDTO = dto.getNotificationMessageType();
                break;
            }
        }

        if(notificationMessageTypeDTO != null) {
            message.setTypeId(notificationMessageTypeDTO.getId());
        }
        setContent(message, message.getTypeId(), entityId, companyDTO.getLanguage().getId());
        return message;
    }

    /**
     * This method removes blank pdf page if exist.
     * This method should be called before you flush your JasperPrint instance to PDF.
     * @param pages
     */
    private static void removeBlankPDFPage(List<JRPrintPage> pages) {
        for (Iterator<JRPrintPage> i=pages.iterator(); i.hasNext();) {
            JRPrintPage page = i.next();
            // If size is zero means no elements in page
            // Or if page elements contains only Page Header & Last Page Footer in blank page
            boolean isHeaderPresent= false;
            boolean isFooterPresent= false;
            boolean isLastPageFooterPresent= false;
            boolean isOtherElementPresent= false;

            logger.debug("## page.getElements().size(): "+page.getElements().size());
            if (page.getElements().isEmpty()) {
                i.remove();
            } else {
                if (page.getElements().size() <= 3) {
                    for (JRPrintElement jrPrintElement:page.getElements()) {
                        if ("PAGE_HEADER".equals(jrPrintElement.getOrigin().getBandTypeValue().toString())){
                            isHeaderPresent = true;
                        } else if ("LAST_PAGE_FOOTER".equals(jrPrintElement.getOrigin().getBandTypeValue().toString())){
                            isLastPageFooterPresent = true;
                        } else if ("PAGE_FOOTER".equals(jrPrintElement.getOrigin().getBandTypeValue().toString())){
                            isFooterPresent = true;
                        } else {
                            isOtherElementPresent = true;
                            break;
                        }
                    }
                }
            }

            if (!isOtherElementPresent &&
                    ((isHeaderPresent && isLastPageFooterPresent)
                            || (isHeaderPresent && isFooterPresent))) {
                i.remove();
            }
        }
    }

    public MessageDTO getJobEventNotification(Integer entityId, Integer languageId,
            Integer jobNotificationId) throws SessionInternalError,
            NotificationNotFoundException {

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setTypeId(jobNotificationId);
        setContent(messageDTO, messageDTO.getTypeId(), entityId, languageId);

        return messageDTO;
    }

    private static Map<String, Object> createAitDynamicParameters(UserDTO user, Integer entityId,PaymentDTOEx dto) {
        // Set up dynamic AIT fields as parameters to send out to the ITG.
        Map<String, Object> dynamicParameters = new HashMap<String, Object>();
        Map<String, String> metaFieldsAndGroupMap = new HashMap<>();
        if(null != user && null != user.getCustomer() && null != user.getCustomer().getAccountType()) {
            user.getCustomer().getAccountType().getInformationTypes().stream().forEach(accountInformationTypeDTO -> {
                accountInformationTypeDTO.getMetaFields().stream().forEach(metaField -> {
                    String stringKey = (accountInformationTypeDTO.getName() + "_" + metaField.getName()).toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                    metaFieldsAndGroupMap.put(stringKey, "");
                });
            });

            Set<CustomerAccountInfoTypeMetaField> currentCustomerAitMetaFields = new HashSet<>();
            CustomerDTO customerDTO = user.getCustomer();
            Date paymentDate = dto.getPaymentDate();
            for (AccountInformationTypeDTO ait : customerDTO.getAccountType().getInformationTypes()) {
                Date invoiceEffectiveDate = customerDTO.getEffectiveDateByGroupIdAndDate(ait.getId(), paymentDate);
                currentCustomerAitMetaFields.addAll(customerDTO.getCustomerAccountInfoTypeMetaFields(ait.getId(), invoiceEffectiveDate));
            }

            for (CustomerAccountInfoTypeMetaField customerAitMetaField : currentCustomerAitMetaFields) {
                String parameterName = customerAitMetaField.getMetaFieldValue().getField().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                String parameterValue = customerAitMetaField.getMetaFieldValue().getValue() != null ? customerAitMetaField.getMetaFieldValue().getValue().toString() : "";
                dynamicParameters.put(parameterName, parameterValue);

                /*
                 * Adding all meta fields again. Now with account information name as prefix. So that if account type has different addresses for billing and service
                 * then those will be accessible via ait name as prefix.
                 * */
                if (customerAitMetaField.getAccountInfoType() != null && customerAitMetaField.getAccountInfoType().getName() != null) {
                    String prefix = customerAitMetaField.getAccountInfoType().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                    dynamicParameters.put(prefix + "_" + parameterName, parameterValue);
                }
            }

            for (Map.Entry<String, String> entry : metaFieldsAndGroupMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (dynamicParameters.get(key) == null) {
                    dynamicParameters.put(key, value);
                }
            }
        }
        return dynamicParameters;
    }

    /**
     * loads {@link PaperInvoiceNotificationTask} if configured for given entity.
     * @param entityId
     * @return
     * @throws PluggableTaskException
     */
    public static PaperInvoiceNotificationTask loadPaperInvoiceNotificationTaskForEntity(Integer entityId) throws PluggableTaskException {
        PluggableTaskManager<NotificationTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_NOTIFICATION);
        for (NotificationTask task = taskManager.getNextClass(); task != null; task = taskManager.getNextClass()) {
            for (PaperInvoiceNotificationPlugin plugin : PaperInvoiceNotificationPlugin.values()) {
                if (plugin.getTaskName().equals(task.getClass().getName())) {
                    return (PaperInvoiceNotificationTask) task;
                }
            }
        }
        throw new SessionInternalError("no paper invoice notification plugin configured for entity "+ entityId);
    }

    private static String getCurrencyInWords(String currencyCode, BigDecimal invoiceTotal){
        String splitter = "#";
        MoneyConverters converters = MoneyConverters.ENGLISH_BANKING_MONEY_VALUE;
       // BigDecimal total = invoiceTotal;
        int subunits = invoiceTotal.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        String amountOutput = converters.asWords(new BigDecimal(invoiceTotal.intValue()), splitter).split(splitter)[0];
        StringJoiner sj = new StringJoiner(" & Cents ", Currency.getInstance(currencyCode).getDisplayName()+" ", "");
        sj.add(amountOutput);
        if (subunits > 0){
            String centsOutput = converters.asWords(new BigDecimal(subunits), splitter).split(splitter)[0];
            sj.add(centsOutput);
        }

        String result = Arrays.stream(sj.toString().replace("-", " ")
                        .split("\\s+"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
        return result;

    }

    public MessageDTO getPaymentLinkEmailMessage(Integer entityId,
                                                 Integer languageId, InvoiceDTO invoice)
            throws SessionInternalError, NotificationNotFoundException {
        PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
        PaymentUrlLogDTO paymentUrlLogDTO = new PaymentUrlLogDAS().findByInvoiceId(invoice.getId());
        MessageDTO message = initializeMessage(entityId, invoice.getBaseUser()
                .getUserId());

        message.setTypeId(MessageDTO.TYPE_PAYMENT_LINK_EMAIL);

        setContent(message, MessageDTO.TYPE_PAYMENT_LINK_EMAIL, entityId,
                languageId);

        message.addParameter("currency", new CurrencyDAS().find(entityId).getSymbol());
        message.addParameter("paymentAmount", paymentUrlLogDTO.getPaymentAmount().doubleValue());
        message.addParameter("gateway_name", paymentUrlLogDTO.getPaymentProvider());
        message.addParameter("Reason_for_payment", paymentUrlLogDAS
                        .getRequestPayloadValueFromPaymentUrl(invoice.getId(),"linkPurpose", false));
        message.addParameter("gateway_id", paymentUrlLogDTO.getGatewayId());
        message.addParameter("payment_link", paymentUrlLogDTO.getPaymentUrl());
        message.addParameter("payment_expiry", DateTime.parse(paymentUrlLogDAS.
                getRequestPayloadValueFromPaymentUrl(invoice.getId(),"linkExpiry", true)).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")));

        return message;
}
}
