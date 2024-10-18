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

/*
 * Created on Jan 27, 2005
 * One session bean to expose as a single web service, thus, one wsdl
 */
package com.sapienter.jbilling.server.util;


import static com.sapienter.jbilling.server.order.CancellationFeeType.FLAT;
import static com.sapienter.jbilling.server.order.CancellationFeeType.PERCENTAGE;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.sql.rowset.CachedRowSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.hibernate.LockMode;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.DtReserveInstanceCache;
import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.CurrencyInUseSessionInternalError;
import com.sapienter.jbilling.common.InvalidArgumentException;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlType;
import com.sapienter.jbilling.paymentUrl.db.Status;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.resources.OrderMetaFieldValueWS;
import com.sapienter.jbilling.saml.SamlUtil;
import com.sapienter.jbilling.server.account.AccountInformationTypeBL;
import com.sapienter.jbilling.server.account.AccountTypeBL;
import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS;
import com.sapienter.jbilling.server.apiUserDetail.db.ApiUserDetailDTO;
import com.sapienter.jbilling.server.apiUserDetail.service.ApiUserDetailBL;
import com.sapienter.jbilling.server.company.CompanyInformationTypeBL;
import com.sapienter.jbilling.server.company.CompanyInformationTypeWS;
import com.sapienter.jbilling.server.company.CopyCompanyBL;
import com.sapienter.jbilling.server.creditnote.CreditNoteBL;
import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.customer.CustomerSignupResponseWS;
import com.sapienter.jbilling.server.customer.event.NewCustomerEvent;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentBL;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.diameter.DiameterBL;
import com.sapienter.jbilling.server.diameter.DiameterItemLocator;
import com.sapienter.jbilling.server.diameter.DiameterResultWS;
import com.sapienter.jbilling.server.diameter.DiameterUserLocator;
import com.sapienter.jbilling.server.diameter.PricingFieldsHelper;
import com.sapienter.jbilling.server.discount.DiscountBL;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.db.DiscountDAS;
import com.sapienter.jbilling.server.discount.db.DiscountDTO;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.ediFile.ldc.OrphanEDIFile;
import com.sapienter.jbilling.server.ediFile.ldc.OrphanLDCFiles;
import com.sapienter.jbilling.server.ediTransaction.EDIFileBL;
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusBL;
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.EDITypeBL;
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.FlatFileGenerator;
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.IFileGenerator;
import com.sapienter.jbilling.server.fileProcessing.fileParser.FlatFileParser;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.filter.FilterFactory;
import com.sapienter.jbilling.server.filter.PagedResultList;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.invoice.IInvoiceSessionBean;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineTypeDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDTO;
import com.sapienter.jbilling.server.invoiceSummary.CreditAdjustmentWS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryBL;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryWS;
import com.sapienter.jbilling.server.invoiceSummary.ItemizedAccountWS;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.item.batch.AssetImportConstants;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.mediation.*;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.mediation.task.IMediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupBL;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.IPluginsSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.notification.db.NotificationMessageDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO;
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.order.event.AssetStatusUpdateEvent;
import com.sapienter.jbilling.server.order.event.OrderPreAuthorizedEvent;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.order.validator.IsNotEmptyOrDeletedValidator;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.payment.*;
import com.sapienter.jbilling.server.payment.db.*;
import com.sapienter.jbilling.server.payment.event.CustomPaymentEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUrlInitiatedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUrlRegenerateEvent;
import com.sapienter.jbilling.server.payment.tasks.GeneratePaymentURLTask;
import com.sapienter.jbilling.server.payment.tasks.stripe.util.StripeHelper;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.*;
import com.sapienter.jbilling.server.pricing.*;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.*;
import com.sapienter.jbilling.server.process.*;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.process.signup.SignupRequestBL;
import com.sapienter.jbilling.server.process.signup.SignupRequestWS;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.provisioning.IProvisioningProcessSessionBean;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandBL;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestBL;
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestWS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.spa.Distributel911AddressUpdateEvent;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportBL;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLService;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLValidator;
import com.sapienter.jbilling.server.sql.api.QueryResultWS;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.SwapPlanHistoryBL;
import com.sapienter.jbilling.server.usagePool.SwapPlanHistoryWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolBL;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDAS;
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.ReserveCacheEvent;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.UsageRatingSchemeType;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeDTO;
import com.sapienter.jbilling.server.usageratingscheme.service.UsageRatingSchemeBL;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.event.AchUpdateEvent;
import com.sapienter.jbilling.server.user.event.NewCreditCardEvent;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationBL;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS;
import com.sapienter.jbilling.server.user.partner.CommissionWS;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.credentials.EmailResetPasswordService;
import com.sapienter.jbilling.server.util.csv.CsvExporter;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.csv.ExportableMap;
import com.sapienter.jbilling.server.util.db.*;
import com.sapienter.jbilling.server.util.mapper.GSTR1JSONMapper;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import com.sapienter.jbilling.tools.JArrays;
import grails.plugin.springsecurity.SpringSecurityService;
import grails.util.Holders;

@Transactional(propagation = Propagation.REQUIRED)
public class WebServicesSessionSpringBean implements IWebServicesSessionBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean billingProcessSession;
    @Resource
    private OrderDAS orderDAS;
    @Resource
    private AssetDAS assetDAS;
    @Resource
    private UserDAS userDAS;
    @Resource(name = MediationService.BEAN_NAME)
    private MediationService mediationService;
    @Resource(name = "mediationProcessService")
    private MediationProcessService mediationProcessService;
    private SpringSecurityService springSecurityService;
    private ApiUserDetailBL apiUserDetailBL;
    private final Boolean UNIQUE_LOGIN_NAME = Boolean.parseBoolean(Holders.getFlatConfig().get("useUniqueLoginName").toString());

    public void setApiUserDetailBL(ApiUserDetailBL apiUserDetailBL) {
        this.apiUserDetailBL = apiUserDetailBL;
    }

    WebServicesSessionSpringBean () {
    }

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null) {
            this.springSecurityService = Context
                    .getBean(Context.Name.SPRING_SECURITY_SERVICE);
        }
        return springSecurityService;
    }

    public void setSpringSecurityService(
            SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    /*
     * Returns the user ID of the authenticated user account making the web
     * service call.
     *
     * @return caller user ID
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getCallerId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService()
                .getPrincipal();
        if(details == null) {
            return null;
        }
        return details.getUserId();
    }

    /**
     * Returns the company ID of the authenticated user account making the web
     * service call.
     *
     * @return caller company ID
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getCallerCompanyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService()
                .getPrincipal();
        return details.getCompanyId();
    }

    /**
     * Returns the language ID of the authenticated user account making the web
     * service call.
     *
     * @return caller language ID
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getCallerLanguageId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService()
                .getPrincipal();
        return details.getLanguageId();
    }

    /**
     * Returns the currency ID of the authenticated user account making the web
     * service call.
     *
     * @return caller currency ID
     */
    @Override
    public Integer getCallerCurrencyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService()
                .getPrincipal();
        return details.getCurrencyId();
    }

    // todo: reorganize methods and reformat code. should match the structure of
    // the interface to make things readable.

    /*
     * Invoices
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceWS getInvoiceWS(Integer invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        InvoiceDTO invoice = new InvoiceDAS().findNow(invoiceId);
        if (null == invoice) {
            throw new SessionInternalError("Invoice with id:" + invoiceId + " does not exist.", HttpStatus.SC_NOT_FOUND);
        }
        if (invoice.getDeleted() == 1) {
            return null;
        }

        InvoiceWS wsDto = InvoiceBL.getWS(invoice);
        if (null != invoice.getInvoiceStatus()) {
            wsDto.setStatusDescr(invoice.getInvoiceStatus().getDescription(
                    getCallerLanguageId()));
        }
        return wsDto;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceWS[] getAllInvoicesForUser(Integer userId) {
        return new InvoiceBL().getAllInvoicesForUser(userId, getCallerLanguageId());
    }

    @Transactional(readOnly = true)
    public InvoiceWS[] getAllInvoices() {
        return new InvoiceBL().getAllInvoices(getCallerLanguageId());
    }

    @Override
    public boolean notifyInvoiceByEmail(Integer invoiceId) {
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        boolean emailInvoice;
        try{
            emailInvoice = notificationSession.emailInvoice(invoiceId);
        } catch (SessionInternalError sie) {
            throw sie;
        } catch (Exception e){
            logger.warn("Exception in web service: notifying invoice by email", e);
            emailInvoice = false;
        }
        return emailInvoice;
    }

    @Override
    public boolean notifyPaymentByEmail(Integer paymentId) {
        INotificationSessionBean notificationSession = (INotificationSessionBean) Context
                .getBean(Context.Name.NOTIFICATION_SESSION);
        return notificationSession.emailPayment(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getAllInvoices(Integer userId) {
        IInvoiceSessionBean invoiceBean = Context
                .getBean(Context.Name.INVOICE_SESSION);
        return invoiceBean.getAllInvoices(userId).toArray(new Integer[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceWS getLatestInvoice(Integer userId) {
        try {
            if (userId == null) {
                return null;
            }
            InvoiceBL bl = new InvoiceBL();
            Integer invoiceId = bl.getLastByUser(userId);
            if(invoiceId == null) {
                return null;
            }
            return InvoiceBL.getWS(new InvoiceDAS().find(invoiceId));
        } catch (Exception e) { // needed because the sql exception :(
            logger.error("Exception in web service: getting latest invoice"
                    + " for user " + userId, e);
            throw new SessionInternalError("Error getting latest invoice");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getLastInvoices(Integer userId, Integer number) {
        if (null == userId) {
            throw new SessionInternalError("Null value for userId not allowed.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != number && number < 0) {
            throw new SessionInternalError("Invalid value:" + number + " for parameter 'number'." +
                    " Must be a non-negative number.", HttpStatus.SC_BAD_REQUEST);
        }

        InvoiceBL bl = new InvoiceBL();
        return bl.getManyWS(userId, number);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getInvoicesByDate(String since, String until) {
        try {
            Date dSince = com.sapienter.jbilling.common.Util.parseDate(since);
            Date dUntil = com.sapienter.jbilling.common.Util.parseDate(until);
            if (since == null || until == null) {
                return new Integer[0];
            }

            Integer entityId = getCallerCompanyId();

            InvoiceBL invoiceBl = new InvoiceBL();
            return invoiceBl.getInvoicesByCreateDateArray(entityId, dSince,
                    dUntil);
        } catch (Exception e) { // needed for the SQLException :(
            logger.error("Exception in web service: getting invoices by date"
                    + since + until, e);
            throw new SessionInternalError("Error getting last invoices");
        }
    }

    /**
     * Returns the invoices for the user within the given date range.
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getUserInvoicesByDate(Integer userId, String since, String until) {
        if (userId == null || since == null || until == null) {
            return new Integer[0];
        }
        Date dSince = com.sapienter.jbilling.common.Util.parseDate(since);
        Date dUntil = com.sapienter.jbilling.common.Util.parseDate(until);
        InvoiceBL invoiceBl = new InvoiceBL();
        return invoiceBl.getUserInvoicesByDate(userId, dSince, dUntil);
    }

    /**
     * Returns an array of IDs for all unpaid invoices under the given user ID.
     *
     * @param userId
     *            user IDs
     * @return array of un-paid invoice IDs
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getUnpaidInvoices(Integer userId) {
        try (CachedRowSet rs = new InvoiceBL().getPayableInvoicesByUser(userId)){
            Integer[] invoiceIds = new Integer[rs.size()];
            int i = 0;
            while (rs.next()) {
                invoiceIds[i++] = rs.getInt(1);
            }
            return invoiceIds;
        } catch (SQLException e) {
            throw new SessionInternalError(
                    "Exception occurred querying payable invoices.");
        } catch (Exception e) {
            throw new SessionInternalError(
                    "An un-handled exception occurred querying payable invoices.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceWS[] getUserInvoicesPage(Integer userId, Integer limit, Integer offset)  {

        if (null == userId) {
            throw new SessionInternalError("Null value for userId not allowed.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && limit < 1) {
            throw new SessionInternalError("Invalid value:" + limit + " for parameter 'limit'." +
                    " Must be greater than zero.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != offset && offset < 0) {
            throw new SessionInternalError("Invalid value:" + offset + " for parameter 'offset'." +
                    " Must be a non-negative number.", HttpStatus.SC_BAD_REQUEST);
        }

        List<InvoiceDTO> invoicesPaged = new InvoiceBL().getListInvoicesPaged(
                getCallerCompanyId(), userId, limit, offset);

        if (invoicesPaged == null) {
            return new InvoiceWS[0];
        }

        List<InvoiceWS> invoicesWS = new ArrayList<>(invoicesPaged.size());
        for (InvoiceDTO invoice : invoicesPaged) {
            InvoiceWS wsdto = InvoiceBL.getWS(invoice);
            if (null != invoice.getInvoiceStatus()) {
                wsdto.setStatusDescr(invoice.getInvoiceStatus().getDescription(getCallerLanguageId()));
            }

            invoicesWS.add(wsdto);
        }
        return invoicesWS.toArray(new InvoiceWS[invoicesWS.size()]);

    }

    /**
     * Generates and returns the paper invoice PDF for the given invoiceId.
     *
     * @param invoiceId
     *            invoice to generate PDF for
     * @return PDF invoice bytes
     * @
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] getPaperInvoicePDF(Integer invoiceId) {
        if(null == invoiceId) {
            throw new SessionInternalError("invoice id parameter is null!",
                    new String[] { "enter non null value as invoice id" }, HttpStatus.SC_BAD_REQUEST);
        }

        if(!new InvoiceDAS().isIdPersisted(invoiceId)) {
            throw new SessionInternalError("invoice id not found",
                    new String[] { "invalid invoice id passed" }, HttpStatus.SC_NOT_FOUND);
        }
        IInvoiceSessionBean invoiceSession = Context.getBean(Context.Name.INVOICE_SESSION);

        UserWS user = getUserWS(getCallerId());

        if (Constants.TYPE_CUSTOMER.equals(user.getMainRoleId())) {
            try {
                if (!getInvoiceWS(invoiceId).getUserId().equals(
                        user.getUserId())) {
                    String msg = String
                            .format("Invalid access to download Invoice ID %s by user %s",
                                    invoiceId, user.getUserId());
                    logger.warn(msg);
                    throw new SessionInternalError(msg);
                }
            } catch (Exception e) {
                throw new SessionInternalError(
                        "Invalid Invoice ID or the user does not own this Invoice.",
                        new String[] { "InvoiceDTO,id,invoice.error.invalid.download,"
                                + invoiceId });
            }
        }
        return invoiceSession.getPDFInvoice(invoiceId);
    }

    /**
     * Un-links a payment from an invoice, effectivley making the invoice
     * "unpaid" by removing the payment balance.
     *
     * If either invoiceId or paymentId parameters are null, no operation will
     * be performed.
     *
     * @param invoiceId
     *            target Invoice
     * @param paymentId
     *            payment to be unlink
     */
    @Override
    public void removePaymentLink(Integer invoiceId, Integer paymentId) {
        if (invoiceId == null || paymentId == null) {
            throw new SessionInternalError("Payment link chain missing!", HttpStatus.SC_NOT_FOUND);
        }
        PaymentBL paymentBL = new PaymentBL(paymentId);
        if (paymentBL.getEntity() == null) {
            throw new SessionInternalError("Payment link chain missing!", HttpStatus.SC_NOT_FOUND);
        }
        // check if the payment is a refund , if it is do not allow it
        if (paymentBL.getEntity().getIsRefund() == 1) {
            String msg = String.format("This payment id %s is a refund so we cannot unlink it from the invoice",paymentId);
            String logMsg = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_DELETE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(logMsg);
            throw new SessionInternalError(
                    "This payment is a refund and hence cannot be unlinked from any invoice",
                    new String[] { "PaymentWS,unlink,validation.error.payment.unlink" }, HttpStatus.SC_CONFLICT);
        }

        // if the payment has been refunded
        // #A Partially refunded Payment can be unlinked from an Invoice.
        /*
         * if(paymentBL.ifRefunded()) { throw new SessionInternalError(
         * "This payment has been refunded and hence cannot be unlinked from the invoice"
         * , new String[]
         * {"PaymentWS,unlink,validation.error.delete.refunded.payment"}); }
         */

        boolean result = paymentBL.unLinkFromInvoice(invoiceId);
        if (!result) {
            String msg = "Unable to find the Invoice with ID: " + invoiceId + " linked to Payment with ID: " + paymentId;
            String logMsg = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_DELETE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(logMsg);
            throw new SessionInternalError("validation failed",
                    new String [] {msg}, HttpStatus.SC_CONFLICT);
        }else{
            String msg = "Invoice with ID: " + invoiceId + " is unlinked from Payment with ID: " + paymentId;
            String logMsg = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_DELETE,LogConstants.STATUS_SUCCESS);
            logger.info(logMsg);
        }
    }

    /**
     * Applies an existing payment to an invoice.
     *
     * If either invoiceId or paymentId parameters are null, no operation will
     * be performed.
     *
     * @param invoiceId
     *            target invoice
     * @param paymentId
     *            payment to apply
     */
    @Override
    public void createPaymentLink(Integer invoiceId, Integer paymentId) {
        String msg = "In createPaymentLink...";
        String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_SUCCESS);
        logger.debug(message);
        if (invoiceId == null || paymentId == null) {
            msg = "Payment link chain missing!";
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError(msg, HttpStatus.SC_BAD_REQUEST);
        }

        Integer entityId = getCallerCompanyId();

        PaymentWS payment = getPayment(paymentId);

        // Guard against npe
        if (payment == null) {
            msg = String.format("No payment found for paymentId: %d.", paymentId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Payment not found!", HttpStatus.SC_BAD_REQUEST);
        }
        // Check if the payment owing user is from the same entity as the caller
        // user.
        Integer userId = payment.getOwningUserId();
        UserDTO user = userDAS.find(userId);
        if (null == user) {
            msg = String.format("No owning user for payment id: %d", paymentId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("There is not user for the supplied payment.", HttpStatus.SC_BAD_REQUEST);
        }
        Integer userCompanyId = user.getEntity().getId();
        if (!entityId.equals(userCompanyId)) {
            msg = String.format("Payment owing user entity id: %d not equals with invoking user entity id: %d",userCompanyId, entityId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Can not create link for non owing payment!!", HttpStatus.SC_BAD_REQUEST);
        }

        // Check if the invoice for the invoice id has the same entity id as the
        // caller entity id.
        InvoiceDTO invoice = findInvoice(invoiceId);
        if (null == invoice) {
            msg = String.format("No invoice found invoice id: %d", invoiceId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Invoice not found!!", HttpStatus.SC_BAD_REQUEST);
        }
        if (!entityId.equals(invoice.getBaseUser().getEntity().getId())) {
            msg = String.format("Invoice entity id: %d not equals with invoking user entity id: %d",userCompanyId, entityId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT_LINK,LogConstants.ACTION_CREATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Can not create link for non owing invoice!!", HttpStatus.SC_BAD_REQUEST);
        }

        IPaymentSessionBean session = Context.getBean(Context.Name.PAYMENT_SESSION);
        session.applyPayment(paymentId, invoiceId);
    }

    @Override
    public void removeAllPaymentLinks(Integer paymentId) {
        if (paymentId == null) {
            throw new SessionInternalError("Payment not found!", HttpStatus.SC_NOT_FOUND);
        }
        PaymentBL bl = new PaymentBL(paymentId);
        if (bl.getEntity() == null) {
            throw new SessionInternalError("Payment not found!", HttpStatus.SC_NOT_FOUND);
        }

        Iterator<PaymentInvoiceMapDTO> it = bl.getEntity().getInvoicesMap().iterator();
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = it.next();
            boolean result = bl.unLinkFromInvoice(map.getInvoiceEntity().getId());
            if (!result) {
                String msg = "Unable to find the Invoice Id " + map.getInvoiceEntity().getId() + " linked to Payment Id " + paymentId;
                String logMsg = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT_LINK, LogConstants.ACTION_DELETE, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(logMsg);
                throw new SessionInternalError(msg, HttpStatus.SC_CONFLICT);
            }
            String msg = "Invoice Id " + map.getInvoiceEntity().getId() + " is unlinked from Payment Id " + paymentId;
            String logMsg = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT_LINK, LogConstants.ACTION_DELETE, LogConstants.STATUS_SUCCESS);
            logger.info(logMsg);

            bl = new PaymentBL(paymentId);
            it = bl.getEntity().getInvoicesMap().iterator();
        }
    }

    /**
     * Deletes an invoice
     *
     * @param invoiceId
     *            The id of the invoice to delete
     */
    @Override
    public void deleteInvoice(Integer invoiceId) {
        IInvoiceSessionBean session = Context.getBean(Context.Name.INVOICE_SESSION);
        session.delete(invoiceId, getCallerId());
    }

    /**
     * Saves an invoiceWS instance from legacy data without linking to any
     * available order with special comments of legacy mark.
     *
     * @param invoiceWS
     *            The instance of desired invoiceWS
     */
    @Override
    public Integer saveLegacyInvoice(InvoiceWS invoiceWS) {
        IInvoiceSessionBean session = Context
                .getBean(Context.Name.INVOICE_SESSION);

        NewInvoiceContext newInvoiceDTO = new NewInvoiceContext();

        UserDTO userDTO = userDAS.find(invoiceWS.getUserId());
        newInvoiceDTO.setBaseUser(userDTO);
        CurrencyDTO currency = new CurrencyDAS()
        .find(invoiceWS.getCurrencyId());
        newInvoiceDTO.setCurrency(currency);
        newInvoiceDTO.setCreateDatetime(invoiceWS.getCreateDatetime());
        newInvoiceDTO.setDueDate(invoiceWS.getDueDate());
        newInvoiceDTO.setTotal(new BigDecimal(invoiceWS.getTotal()));
        if (invoiceWS.getPaymentAttempts() != null) {
            newInvoiceDTO.setPaymentAttempts(invoiceWS.getPaymentAttempts());
        }
        if (invoiceWS.getStatusId() != null) {
            InvoiceStatusDTO invoiceStatusDTO = new InvoiceStatusDAS()
            .find(invoiceWS.getStatusId());
            newInvoiceDTO.setInvoiceStatus(invoiceStatusDTO);
        }
        newInvoiceDTO.setToProcess(invoiceWS.getToProcess());
        newInvoiceDTO.setBalance(new BigDecimal(invoiceWS.getBalance()));
        newInvoiceDTO.setCarriedBalance(invoiceWS.getCarriedBalanceAsDecimal());
        if (invoiceWS.getInProcessPayment() != null) {
            newInvoiceDTO.setInProcessPayment(invoiceWS.getInProcessPayment());
        }
        newInvoiceDTO.setIsReview(invoiceWS.getIsReview() == null ? 0
                : invoiceWS.getIsReview()); // set fake value if null
        newInvoiceDTO.setDeleted(invoiceWS.getDeleted());
        newInvoiceDTO
        .setCustomerNotes((invoiceWS.getCustomerNotes() == null ? ""
                : (invoiceWS.getCustomerNotes() + " "))
                + "This invoice is migrated from legacy system.");
        newInvoiceDTO.setPublicNumber(invoiceWS.getNumber());
        newInvoiceDTO.setLastReminder(invoiceWS.getLastReminder());
        newInvoiceDTO.setOverdueStep(invoiceWS.getOverdueStep());
        newInvoiceDTO.setCreateTimestamp(invoiceWS.getCreateTimeStamp());

        // if create date time is given then we can assume that that
        // is the billing date, otherwise we will fake the billing date
        Date billingDate = null != invoiceWS.getCreateDatetime() ? invoiceWS.getCreateDatetime() : TimezoneHelper.serverCurrentDate();
        newInvoiceDTO.setBillingDate(TimezoneHelper.convertToTimezone(billingDate, getCompany().getTimezone()));

        for (InvoiceLineDTO invoiceLineDTO : invoiceWS.getInvoiceLines()) {

            com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO dbInvoiceLineDTO = new com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO();

            if (invoiceLineDTO.getId() != null) {
                dbInvoiceLineDTO.setId(invoiceLineDTO.getId());
            }
            ItemDTO itemDTO = new ItemDAS().find(invoiceLineDTO.getItemId());
            dbInvoiceLineDTO.setItem(itemDTO);
            dbInvoiceLineDTO.setAmount(invoiceLineDTO.getAmountAsDecimal());
            dbInvoiceLineDTO.setQuantity(invoiceLineDTO.getQuantityAsDecimal());
            dbInvoiceLineDTO.setPrice(invoiceLineDTO.getPriceAsDecimal());
            dbInvoiceLineDTO.setDeleted(invoiceLineDTO.getDeleted());
            dbInvoiceLineDTO.setDescription(invoiceLineDTO.getDescription());
            dbInvoiceLineDTO.setSourceUserId(invoiceLineDTO.getSourceUserId());
            dbInvoiceLineDTO.setIsPercentage(invoiceLineDTO.getPercentage());
            dbInvoiceLineDTO.setInvoiceLineType(new InvoiceLineTypeDTO(3)); // Due
            // invoice
            // line
            // type

            newInvoiceDTO.getResultLines().add(dbInvoiceLineDTO);
        }

        InvoiceDTO newInvoice = session.create(getCompany().getId(),
                invoiceWS.getUserId(), newInvoiceDTO);
        return newInvoice.getId();
    }

    /**
     * Saves a paymentWS instance from legacy data without linking to any
     * available invoice with special comments of legacy mark.
     *
     * @param paymentWS
     *            The instance of desired paymentWS
     */
    @Override
    public Integer saveLegacyPayment(PaymentWS paymentWS) {

        if( null == paymentWS) {
            throw new SessionInternalError("Please Provide PaymentWS");
        }

        PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(paymentWS.getPaymentInstruments());
        com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO paymentAuthorization = null;

        if (StringUtils.isEmpty(paymentWS.getPaymentNotes())) {
            paymentWS.setPaymentNotes("This payment is migrated from legacy system.");
        }

        // Have to copy the object and set null in original object because in PaymentDTOEx it tries to use it to find values in DB
        if(paymentWS.getAuthorizationId() != null) {
            paymentAuthorization = paymentWS.getAuthorizationId();
            paymentWS.setAuthorizationId(null);
        }

        Integer paymentId = applyPayment(paymentWS, null);

        if( paymentAuthorization != null && StringUtils.isNotEmpty(paymentAuthorization.getTransactionId())) {

            PaymentAuthorizationDTO authorizationDTO = new PaymentAuthorizationDTO();

            authorizationDTO.setTransactionId(paymentAuthorization.getTransactionId());
            authorizationDTO.setProcessor(paymentAuthorization.getProcessor());
            authorizationDTO.setCode1(paymentAuthorization.getCode1());
            authorizationDTO.setCreateDate(paymentWS.getPaymentDate());
            authorizationDTO.setResponseMessage(paymentAuthorization.getResponseMessage());
            authorizationDTO.setApprovalCode(paymentAuthorization.getApprovalCode());
            authorizationDTO.setAvs(paymentAuthorization.getAVS());
            authorizationDTO.setCode2(paymentAuthorization.getCode2());
            new PaymentAuthorizationBL().create(authorizationDTO, paymentId);
        }
        return paymentId;
    }

    /**
     * Saves an orderWS instance from legacy data used in migration tool.
     *
     * @param orderWS
     *            The instance of desired orderWS
     * @return id of saved orderWS object
     */
    @Override
    public Integer saveLegacyOrder(OrderWS orderWS) {

        UserWS userWS = getUserWS(orderWS.getUserId());
        OrderBL orderBL = new OrderBL();

        return orderBL.create(userWS.getEntityId(), null,
                orderBL.getDTO(orderWS));
    }

    /**
     * Deletes an Item
     *
     * @param itemId
     *            The id of the item to delete
     */
    @Override
    public void deleteItem(Integer itemId)  {

        ItemBL bl = new ItemBL(itemId);
        // only root entity can delete global item/category, otherwise owning
        // entity can
        CompanyDAS companyDas = new CompanyDAS();
        if (bl.getEntity().isGlobal()) {
            if (!companyDas.isRoot(getCallerCompanyId())) {
                String[] errors = new String[] { "ItemTypeWS,global,validation.only.root.can.delete.global.item" };
                throw new SessionInternalError(
                        "validation.only.root.can.delete.global.item", errors, HttpStatus.SC_CONFLICT);
            }
        }

        if (!getCallerCompanyId().equals(bl.getEntity().getEntityId())) {
            String[] errors = new String[] { "ItemTypeWS,entity,validation.only.owner.can.delete.item" };
            throw new SessionInternalError(
                    "validation.only.owner.can.delete.item", errors, HttpStatus.SC_CONFLICT);
        }

        // todo - item may be in use, prevent deletion

        IItemSessionBean itemSession = (IItemSessionBean) Context
                .getBean(Context.Name.ITEM_SESSION);
        try {
            itemSession.delete(getCallerId(), itemId);
        } catch (Exception e){
            throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
        }
        logger.debug("Deleted Item Id {}", itemId);
    }

    /**
     * Deletes an Item Category
     *
     * @param itemCategoryId
     *            The id of the Item Category to delete
     */
    @Override
    public void deleteItemCategory(Integer itemCategoryId)
    {

        ItemTypeBL bl = new ItemTypeBL(itemCategoryId);

        // only root entity can delete global item/category, otherwise owning
        // entity can
        CompanyDAS companyDas = new CompanyDAS();
        if (bl.getEntity().isGlobal()) {
            if (!companyDas.isRoot(getCallerCompanyId())) {
                String[] errors = new String[] { "ItemTypeWS,global,validation.only.root.can.delete.global.category" };
                throw new SessionInternalError(
                        "validation.only.root.can.delete.global.category",
                        errors, HttpStatus.SC_CONFLICT);
            }
        }

        if (!getCallerCompanyId().equals(bl.getEntity().getEntityId())) {
            String[] errors = new String[] { "ItemTypeWS,entity,validation.only.owner.can.delete.category" };
            throw new SessionInternalError(
                    "validation.only.owner.can.delete.category", errors, HttpStatus.SC_CONFLICT);
        }
        try {
            bl.delete(getCallerId());
        } catch (Exception e){
            throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
        }
    }

    /**
     * List all item categories for the given entity It includes global and
     * child entity product categories too
     *
     * @param entityId
     *            : company id for which item types will be retrieved
     * @return : List of item categories
     */
    @Override
    @Transactional(readOnly = true)
    public ItemTypeWS[] getAllItemCategoriesByEntityId(Integer entityId) {
        ItemTypeBL bl = new ItemTypeBL();
        return bl.getItemCategoriesByEntity(entityId);
    }

    /**
     * List all items for a given entity
     */
    @Override
    @Transactional(readOnly = true)
    public ItemDTOEx[] getAllItemsByEntityId(Integer entityId) {
        ItemBL bl = new ItemBL();
        List<ItemDTOEx> ws = bl.getAllItemsByEntity(entityId);
        return ws.toArray(new ItemDTOEx[ws.size()]);
    }

    /**
     * Generates invoices for orders not yet invoiced for this user. Optionally
     * only allow recurring orders to generate invoices. Returns the ids of the
     * invoices generated.
     */
    @Override
    public Integer[] createInvoice(Integer userId, boolean onlyRecurring) {
        return createInvoiceWithDate(userId, null, null, null, onlyRecurring);
    }

    /**
     * Generates an invoice for a customer using an explicit billing date & due
     * date period.
     *
     * If the billing date is left blank, the invoice will be generated for
     * today.
     *
     * If the due date period unit or value is left blank, then the due date
     * will be calculated from the order period, or from the customer due date
     * period if set.
     *
     * @param userId
     *            user id to generate an invoice for.
     * @param billingDate
     *            billing date for the invoice generation run
     * @param dueDatePeriodId
     *            due date period unit
     * @param dueDatePeriodValue
     *            due date period value
     * @param onlyRecurring
     *            only include recurring orders? false to include all orders in
     *            invoice.
     * @return array of generated invoice ids.
     */
    @Override
    public Integer[] createInvoiceWithDate(Integer userId, Date billingDate,
            Integer dueDatePeriodId, Integer dueDatePeriodValue,
            boolean onlyRecurring) {
        UserDTO user = userDAS.find(userId);

        // Create a mock billing process object, because the method
        // we are calling was meant to be called by the billing process.
        BillingProcessDTO billingProcess = new BillingProcessDTO();
        billingProcess.setId(0);
        billingProcess.setEntity(user.getCompany());
        billingProcess.setBillingDate(billingDate != null ? billingDate
                : TimezoneHelper.companyCurrentDateByUserId(userId));
        billingProcess.setIsReview(0);
        billingProcess.setRetriesToDo(0);

        // optional target due date
        TimePeriod dueDatePeriod = null;
        if (dueDatePeriodId != null && dueDatePeriodValue != null) {
            dueDatePeriod = new TimePeriod();
            dueDatePeriod.setUnitId(dueDatePeriodId);
            dueDatePeriod.setValue(dueDatePeriodValue);
            logger.debug("Using provided due date {}", dueDatePeriod);
        }
        // generate invoices
        InvoiceDTO[] invoices = new BillingProcessBL().generateInvoice(
                billingProcess, dueDatePeriod, user, false, onlyRecurring,
                getCallerId());
        // generate invoices should return an empty array instead of null... bad
        // design :(
        if (invoices == null) {
            return new Integer[0];
        }

        // build the list of generated ID's and return
        List<Integer> invoiceIds = new ArrayList<>(invoices.length);
        for (InvoiceDTO invoice : invoices) {
            invoiceIds.add(invoice.getId());
        }
        return invoiceIds.toArray(new Integer[invoiceIds.size()]);
    }

    @Override
    public Integer applyOrderToInvoice(Integer orderId, InvoiceWS invoiceWs) {
        if (orderId == null) {
            throw new SessionInternalError("Order id cannot be null.");
        }

        // validate order to be processed
        OrderDTO order = orderDAS.find(orderId);
        if (order == null
                || !OrderStatusFlag.INVOICE.equals(order.getOrderStatus()
                        .getOrderStatusFlag())) {
            logger.debug("Order must exist and be active to generate an invoice.");
            return null;
        }

        // create an invoice template that contains the meta field values
        NewInvoiceContext template = new NewInvoiceContext();
        MetaFieldBL.fillMetaFieldsFromWS(getCallerCompanyId(), template,
                invoiceWs.getMetaFields());

        logger.debug("Updating invoice with order: {}", orderId);
        logger.debug("Invoice WS: {}", invoiceWs);
        logger.debug("Invoice template fields: {}", template.getMetaFields());

        // update the invoice
        try {
            BillingProcessBL process = new BillingProcessBL();
            InvoiceDTO invoice = process.generateInvoice(order.getId(),
                    invoiceWs.getId(), template, getCallerId());
            if (null != invoice && invoice.getToProcess() == 0) {
                new AgeingBL().out(invoice.getBaseUser(), null, invoice.getCreateDatetime());
            }
            return invoice != null ? invoice.getId() : null;

        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            logger.debug("apply order to invoice. ", e);
            throw new SessionInternalError(
                    "Error while generating a new invoice", e);
        }
    }

    /**
     * Generates a new invoice for an order, or adds the order to an existing
     * invoice.
     *
     * @param orderId
     *            order id to generate an invoice for
     * @param invoiceId
     *            optional invoice id to add the order to. If null, a new
     *            invoice will be created.
     * @return id of generated invoice, null if no invoice generated.
     * @
     *             if user id or order id is null.
     */
    @Override
    public Integer createInvoiceFromOrder(Integer orderId, Integer invoiceId)
    {
        if (orderId == null) {
            throw new SessionInternalError("Order id cannot be null.");
        }

        OrderDTO order = orderDAS.findNow(orderId);
        orderDAS.refresh(order);

        // validate order to be processed
        if (order == null
                || !OrderStatusFlag.INVOICE.equals(order.getOrderStatus()
                        .getOrderStatusFlag())) {
            logger.debug("Order must exist and be active to generate an invoice.");
            return null;
        }
        // Set the pessimistic lock (select for update) for a order DTO to
        // ensure no any concurrent process can update it while creating new
        // invoice
        orderDAS.getHibernateTemplate().lock(order, LockMode.UPGRADE);
        // create new invoice, or add to an existing invoice
        InvoiceDTO invoice;
        if (invoiceId == null) {
            logger.debug("Creating a new invoice for order {}", order.getId());
            invoice = doCreateInvoice(order.getId());
            if (null == invoice) {
                throw new SessionInternalError(
                        "Invoice could not be generated. The purchase order may not have any applicable periods to be invoiced.");
            }
        } else {
            logger.debug("Adding order {} to invoice {}", order.getId(), invoiceId);
            invoice = billingProcessSession.generateInvoice(order.getId(), invoiceId, null, getCallerId());
        }

        return invoice == null ? null : invoice.getId();
    }

    /*
     * USERS
     */
    /**
     * Creates a new user. The user to be created has to be of the roles
     * customer or partner. The username has to be unique, otherwise the
     * creating won't go through. If that is the case, the return value will be
     * null.
     *
     * @param newUser
     *            The user object with all the information of the new user. If
     *            contact or credit card information are present, they will be
     *            included in the creation although they are not mandatory.
     * @return The id of the new user, or null if non was created
     */
    @Override
    public Integer createUser(UserWS newUser)  {
        return createUserWithCompanyId(newUser, newUser.getEntityId() != null ? newUser.getEntityId() : getCallerCompanyId());
    }

    /* Method delete the user without checking the current logged in customer details
     * @param userId : user id of the user that needs to be deleted
     * comapnyId : Company id of the user*/
    public void removeUser(Integer userId,Integer companyId)  {
        UserBL bl = new UserBL();
        try{
            bl.set(userId);
            bl.delete(companyId);
        }
        catch (Exception e){
            if (e instanceof SessionInternalError) {
                throw (SessionInternalError) e;
            }

            throw new SessionInternalError(e, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Integer createUserForAppDirect(UserWS newUser, Integer companyId, boolean isCreator)
    {

        String msg, log;

        newUser.setUserId(0);
        Integer entityId = companyId;
        UserBL bl = new UserBL();
        UserDTO parentUser = userDAS.findNow(newUser.getParentId());
        Integer parentCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        int parentChildCustomerPreference = PreferenceBL.getPreferenceValueAsIntegerOrZero(companyId, Constants.PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY);
        if (parentChildCustomerPreference == 1) {
            if (!(bl.exists(newUser.getParentId(), entityId) || (parentCompanyId != null && bl.exists(newUser.getParentId(), parentCompanyId)))) {
                msg = "There doesn't exist a parent with the supplied id." + newUser.getParentId();
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError(
                        "There doesn't exist a parent with the supplied id."
                                + newUser.getParentId(),
                                new String[]{"UserWS,parentId,validation.error.parent.does.not.exist"});
            }
        } else if (!bl.exists(newUser.getParentId(), entityId)) {
            msg = "There doesn't exist a parent with the supplied id."
                    + newUser.getParentId();
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError(
                    "There doesn't exist a parent with the supplied id."
                            + newUser.getParentId(),
                            new String[]{"UserWS,parentId,validation.error.parent.does.not.exist"});
        }

        if (null != parentUser && parentUser.getCustomer().getIsParent() == 0) {
            msg = "The selected parent id {0} is not set to allow sub-accounts."
                    + newUser.getParentId();
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError(
                    "The selected parent id {0} is not set to allow sub-accounts."
                            + newUser.getParentId(),
                            new String[] { "UserWS,parentId,validation.error.not.allowed.parentId,"
                                    + newUser.getParentId() });
        }

        logger.info("Checking if user with name already exist");

        if (bl.exists(newUser.getUserName(), entityId)) {
            msg = "User already exists with username " + newUser.getUserName();
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError("User already exists with username " + newUser.getUserName(),
                    new String[] { "UserWS,userName,validation.error.user.already.exists" });
        }

        logger.info("Checking if user with email already exist");
        //forcing unique email in the systems for all users
        if (forceUniqueEmails(entityId)) {
            List<String> emails = new ArrayList<String>();

            if (0 != newUser.getMainRoleId().compareTo(Constants.TYPE_CUSTOMER)) {
                MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

                if(newUser.getMetaFields() != null) {
                    for (MetaFieldValueWS value : newUser.getMetaFields()) {
                        if (null != value.getFieldName() && null != value.getGroupId()) {
                            MetaField field = metaFieldDAS.getFieldByNameAndGroup(
                                    entityId, value.getFieldName(), value.getGroupId());

                            if (field.getFieldUsage() == MetaFieldType.EMAIL) {
                                emails.add(value.getStringValue());
                            }
                        }
                    }
                }
            } else if (null != newUser.getContact()) {
                emails.add(newUser.getContact().getEmail());
            }

            for (String email : emails) {
                if (new UserBL().findUsersByEmail(email, entityId).size() > 0) {
                    msg = "User already exists with email " + email;
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                    logger.error(log);
                    throw new SessionInternalError("User already exists with email " + email,
                            new String[]{"ContactWS,email,validation.error.email.already.exists"});
                }
            }
        }

        if (0 == newUser.getMainRoleId().compareTo(Constants.TYPE_CUSTOMER)){
            if(null == newUser.getAccountTypeId()){
                msg = "Customer users must have account type id defined.";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError("Customer users must have account type id defined.",
                        new String[] {"UserWS,accountTypeId,validation.error.account.type.not.defined"});
            }

            AccountTypeDTO accountType = new AccountTypeDAS()
            .find(newUser.getAccountTypeId(), entityId);

            if(null == accountType){
                msg = "Customer users must have account type that exists.";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError("Customer users must have account type that exists.",
                        new String[] {"UserWS,accountTypeId,validation.error.account.type.not.exist"});
            }
        }

        // The Payment Instruments must be unique
        validateUniquePaymentInstruments(newUser.getPaymentInstruments());

        ContactBL cBl = new ContactBL();
        UserDTOEx dto = new UserDTOEx(newUser, entityId);

        Integer userId;
        try {
            userId = bl.create(dto, null);
            // if the user is not customer do not create
            // a contact for that user
            if (newUser.getContact() != null
                    && 0 != newUser.getMainRoleId().compareTo(
                            Constants.TYPE_CUSTOMER)) {
                newUser.getContact().setId(0);
                cBl.createForUser(new ContactDTOEx(newUser.getContact()),
                        userId, null);
            }

            if (newUser.getCustomerNotes() != null) {
                for (CustomerNoteWS customerNotes : JArrays.toArrayList(newUser
                        .getCustomerNotes())) {
                    customerNotes.setCustomerId(UserBL.getUserEntity(userId)
                            .getCustomer().getId());
                    createCustomerNote(customerNotes);
                }
            }
            bl.createCredentialsFromDTO(dto);

            boolean isSSOEnabled = SamlUtil.getUserSSOEnabledStatus(userId);
            if(isSSOEnabled) {
                if (isCreator) {
                    bl.setSSOEnabledPCICompliantPassword();
                } else {
                    try {
                        bl.sendSSOEnabledUserCreatedEmailMessage(entityId, userId, 1);
                        bl.setSSOEnabledPCICompliantPassword();
                    } catch (NotificationNotFoundException e) {
                        msg = "Notification for SSOEnabledUserCreatedEmail not found"
                                + userId;
                        log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_EVENT);
                        logger.error(log);
                    }
                }
            }
        } catch (Exception e) {
            msg = "Failed to create new user.";
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError(e);
        }
        return userId;
    }

    @Override
    public Integer createUserWithCompanyId(UserWS newUser, Integer companyId)
    {

        if(newUser != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(newUser.getPaymentInstruments());
        }

        String msg, log;

        newUser.setUserId(0);
        Integer entityId = companyId;
        UserBL bl = new UserBL();
        UserDTO parentUser = userDAS.findNow(newUser.getParentId());
        Integer parentCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        int parentChildCustomerPreference = PreferenceBL.getPreferenceValueAsIntegerOrZero(companyId, Constants.PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY);
        if (parentChildCustomerPreference == 1) {
            if (!(bl.exists(newUser.getParentId(), entityId) || (parentCompanyId != null && bl.exists(newUser.getParentId(), parentCompanyId)))) {
                msg = "There doesn't exist a parent with the supplied id." + newUser.getParentId();
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError(
                        "There doesn't exist a parent with the supplied id."
                                + newUser.getParentId(),
                                new String[]{"UserWS,parentId,validation.error.parent.does.not.exist"}, HttpStatus.SC_BAD_REQUEST);
            }
        } else if (!bl.exists(newUser.getParentId(), entityId)) {
            msg = "There doesn't exist a parent with the supplied id."
                    + newUser.getParentId();
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError(
                    "There doesn't exist a parent with the supplied id."
                            + newUser.getParentId(),
                            new String[]{"UserWS,parentId,validation.error.parent.does.not.exist"}, HttpStatus.SC_BAD_REQUEST);
        }

        if (null != parentUser && parentUser.getCustomer().getIsParent() == 0) {
            msg = "The selected parent id {0} is not set to allow sub-accounts."
                    + newUser.getParentId();
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            throw new SessionInternalError(
                    "The selected parent id {0} is not set to allow sub-accounts."
                            + newUser.getParentId(),
                            new String[] { "UserWS,parentId,validation.error.not.allowed.parentId,"
                                    + newUser.getParentId()}, HttpStatus.SC_BAD_REQUEST);
        }

        logger.info("Checking if user with name already exist");
        if (!StringUtils.isBlank(newUser.getUserName())) {
            /* This code block checks the uniqueness of login name (user name) across all companies if
             * useUniqueLoginName config property is set to TRUE otherwise it checks uniqueness in the same company
             * only as core feature.
             */
            if( Boolean.parseBoolean(Holders.getFlatConfig().get("useUniqueLoginName").toString())
                    ? new UserBL().findUsersByUserName(newUser.getUserName()) != null
                    : bl.exists(newUser.getUserName(), entityId) ) {
                handleUserExistsError(newUser.getUserName());

            }
        }

        logger.info("Checking if user with email already exist");
        //forcing unique email in the systems for all users
        if (forceUniqueEmails(entityId)) {
            List<String> emails = new ArrayList<String>();

            if (0 != newUser.getMainRoleId().compareTo(Constants.TYPE_CUSTOMER)) {
                MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

                if(newUser.getMetaFields() != null) {
                    for (MetaFieldValueWS value : newUser.getMetaFields()) {
                        if (null != value.getFieldName() && null != value.getGroupId()) {
                            MetaField field = metaFieldDAS.getFieldByNameAndGroup(
                                    entityId, value.getFieldName(), value.getGroupId());

                            if (field.getFieldUsage() == MetaFieldType.EMAIL) {
                                emails.add(value.getStringValue());
                            }
                        }
                    }
                }
            } else if (null != newUser.getContact()) {
                emails.add(newUser.getContact().getEmail());
            }

            for (String email : emails) {
                if (new UserBL().findUsersByEmail(email, entityId).size() > 0) {
                    msg = "User already exists with email " + email;
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                    logger.error(log);
                    throw new SessionInternalError("User already exists with email " + email,
                            new String[]{"ContactWS,email,validation.error.email.already.exists"}, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        if (0 == newUser.getMainRoleId().compareTo(Constants.TYPE_CUSTOMER)){
            if(null == newUser.getAccountTypeId()){
                msg = "Customer users must have account type id defined.";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError("Customer users must have account type id defined.",
                        new String[] {"UserWS,accountTypeId,validation.error.account.type.not.defined"}, HttpStatus.SC_BAD_REQUEST);
            }

            AccountTypeDTO accountType = new AccountTypeDAS()
            .find(newUser.getAccountTypeId(), entityId);

            if(null == accountType){
                msg = "Customer users must have account type that exists.";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError("Customer users must have account type that exists.",
                        new String[] {"UserWS,accountTypeId,validation.error.account.type.not.exist"}, HttpStatus.SC_BAD_REQUEST);
            }
        }

        // The Payment Instruments must be unique
        validateUniquePaymentInstruments(newUser.getPaymentInstruments());

        ContactBL cBl = new ContactBL();
        UserDTOEx dto = new UserDTOEx(newUser, entityId);

        Integer userId;
        try {
            userId = bl.create(dto, getCallerId());
            // if the user is not customer do not create
            // a contact for that user
            if (newUser.getContact() != null
                    && 0 != newUser.getMainRoleId().compareTo(
                            Constants.TYPE_CUSTOMER)) {
                newUser.getContact().setId(0);
                cBl.createForUser(new ContactDTOEx(newUser.getContact()),
                        userId, getCallerId());
            }

            if (newUser.getCustomerNotes() != null) {
                for (CustomerNoteWS customerNotes : JArrays.toArrayList(newUser
                        .getCustomerNotes())) {
                    customerNotes.setCustomerId(UserBL.getUserEntity(userId)
                            .getCustomer().getId());
                    createCustomerNote(customerNotes);
                }
            }
            bl.createCredentialsFromDTO(dto);

            boolean isSSOEnabled = SamlUtil.getUserSSOEnabledStatus(userId);
            if(isSSOEnabled){
                try {
                    bl.sendSSOEnabledUserCreatedEmailMessage(entityId,userId,1);
                    bl.setSSOEnabledPCICompliantPassword();
                }catch (NotificationNotFoundException e){
                    msg = "Notification for SSOEnabledUserCreatedEmail not found"
                            + userId;
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_EVENT);
                    logger.error(log);
                }
            }

            // New Customer Event for newly created customer's.
            UserDTO user = new UserBL(userId).getDto();
            NewCustomerEvent newCustomerEvent = new NewCustomerEvent(user.getCompany().getId(), user);
            EventManager.process(newCustomerEvent);

        } catch (Exception e) {
            msg = "Failed to create new user.";
            log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
            logger.error(log);
            Throwable cause = e.getCause();
            if (cause instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                if(constraintViolationException.getConstraintName().equals("base_user_uq_user_name_per_entity")){
                    handleUserExistsError(newUser.getUserName());
                }
            }
            throw new SessionInternalError(e);
        }
        return userId;
    }

    /**
     * Creates a reseller customer with company information
     *
     * @param newUser
     *            : an instance of UserWS
     * @return : id of the created reseller
     */
    public Integer createReseller(UserWS newUser, Integer parentId) {
        newUser.setUserId(0);
        Integer entityId = parentId;
        UserBL bl = new UserBL();

        ContactBL cBl = new ContactBL();
        UserDTOEx dto = new UserDTOEx(newUser, entityId);
        Integer userId;
        try {
            userId = bl.create(dto, getCallerId());
            // if the user is not customer do not create
            // a contatct for that user
            if (newUser.getContact() != null
                    && 0 != newUser.getMainRoleId().compareTo(
                            Constants.TYPE_CUSTOMER)) {
                newUser.getContact().setId(0);
                cBl.createForUser(new ContactDTOEx(newUser.getContact()),
                        userId, getCallerId());
            }
            bl.createCredentialsFromDTO(dto);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
        return userId;
    }

    private boolean forceUniqueEmails(Integer entityId) {
        int preferenceForceUniqueEmails = 0;
        try {
            preferenceForceUniqueEmails = PreferenceBL
                    .getPreferenceValueAsIntegerOrZero(entityId,
                            Constants.PREFERENCE_FORCE_UNIQUE_EMAILS);
        } catch (EmptyResultDataAccessException e) {
            // default will be used
        }
        return 1 == preferenceForceUniqueEmails;
    }

    @Override
    public void deleteUser(Integer userId)  {
        UserBL bl = new UserBL();
        if (getCallerId().equals(userId)) {
            String msg = "User with ID: " + userId + " cannot delete itself";
            String log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_DELETE);
            logger.error(log);
            throw new SessionInternalError("User cannot delete itself", HttpStatus.SC_CONFLICT);
        }
        Integer executorId = getCallerId();
        try {
            bl.set(userId);
            bl.delete(executorId);
        } catch (Exception e){
            throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
        }
    }

    @Override
    public void deleteAppDirectUser(Integer executorId, Integer userId)  {
        UserBL bl = new UserBL();
        if (executorId.equals(userId)) {
            String msg = "User with ID: " + userId + " cannot delete itself";
            String log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_DELETE);
            logger.error(log);
            throw new SessionInternalError("User cannot delete itself");
        }
        bl.set(userId);
        bl.delete(executorId);
    }

    @Override
    public void initiateTermination(Integer userId, String reasonCode, Date terminationDate)  {
        IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        ediTransactionBean.initiateTermination(userId, reasonCode, terminationDate);
    }

    /**
     * Returns true if a user exists with the given user name, false if not.
     *
     * @param userName
     *            user name
     * @return true if user exists, false if not.
     */
    @Override
    public boolean userExistsWithName(String userName) {
        return new UserBL().exists(userName, getCallerCompanyId());
    }

    /**
     * Returns true if a user with the given ID exists and is accessible by the
     * caller, false if not.
     *
     * @param userId
     *            user id
     * @return true if user exists, false if not.
     */
    @Override
    public boolean userExistsWithId(Integer userId) {
        return new UserBL().exists(userId, getCallerCompanyId());
    }

    @Override
    public void updateUserContact(Integer userId, ContactWS contact)
    {
        // todo: support multiple WS method param validations through
        // WSSecurityMethodMapper
        UserBL userBL = new UserBL(userId);
        Integer entityId = getCallerCompanyId();

        if (forceUniqueEmails(entityId) && null != contact.getEmail()
                && (userBL.isEmailUsedByOthers(contact.getEmail()))) {
            throw new SessionInternalError(
                    "User already exists with email " + contact.getEmail(),
                    new String[] { "ContactWS,email,validation.error.email.already.exists" });
        }

        // update the contact
        ContactBL cBl = new ContactBL();
        cBl.updateForUser(new ContactDTOEx(contact), userId, getCallerId());
    }

    /**
     * @param user
     */
    @Override
    public void updateUser(UserWS user) {
        updateUserWithCompanyId(user, getCallerCompanyId());
    }

    @Override
    public void updateUserWithCompanyId(UserWS user, Integer entityId)
    {

        if(user != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(user.getPaymentInstruments());
        }

        // TODO commenting validate user for create/edit customer grails impl. -
        // vikasb
        // validateUser(user);

        UserBL bl = new UserBL(user.getUserId());
        boolean isSSOEnabled = SamlUtil.getUserSSOEnabledStatus(bl.getDto().getId());
        String msg, log;
        // get the entity
        Integer executorId = getCallerId();

        // convert user WS to a DTO that includes customer data
        UserDTOEx dto = new UserDTOEx(user, entityId);
        if (dto.getCustomer() != null) {
            if (dto.getCustomer().getParent() != null
                    && dto.getCustomer().getParent().getId() == dto.getId()) {
                msg = "The parent id cannot be the same as user id for this customer.";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                logger.error(log);
                throw new SessionInternalError(
                        "The parent id cannot be the same as user id for this customer.",
                        new String[] { "UserWS,parentId,validation.error.parent.customer.id.same" }, HttpStatus.SC_BAD_REQUEST);
            }

            if (dto.getCustomer().getParent() != null) {

                UserDTO parentUser = userDAS.findNow(dto.getCustomer()
                        .getParent().getId());
                if (null != parentUser
                        && parentUser.getCustomer().getIsParent() == 0) {
                    msg = "The selected parent id {0} is not set to allow sub-accounts."
                            + dto.getCustomer().getParent().getId();
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                    logger.error(log);
                    throw new SessionInternalError(
                            "The selected parent id {0} is not set to allow sub-accounts."
                                    + dto.getCustomer().getParent().getId(),
                                    new String[] { "UserWS,parentId,validation.error.not.allowed.parentId,"
                                            + dto.getCustomer().getParent().getId() }, HttpStatus.SC_BAD_REQUEST);
                }
            }

            if (dto.getCustomer().getParent() != null) {
                if (!new UserBL().okToAddAsParent(dto.getId(), dto
                        .getCustomer().getParent().getId())) {
                    msg = "Cannot set the parent to the Customer's own child in account hierarchy.";
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                    logger.error(log);
                    throw new SessionInternalError(
                            "Cannot set the parent to the Customer's own child in account hierarchy.",
                            new String[] { "UserWS,parentId,customer.error.hierachy" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        // The Payment Instruments must be unique
        validateUniquePaymentInstruments(user.getPaymentInstruments());

        // update the user info and customer data
        bl.getEntity().touch();

        String changedPassword = user.getPassword();
        if (null != changedPassword && !changedPassword.trim().isEmpty()) {

            Integer methodId = bl.getEntity().getEncryptionScheme();
            boolean matches = JBCrypto.passwordsMatch(methodId, bl.getEntity()
                    .getPassword(), changedPassword);

            if (matches) {
                // password is not changed and in attempts to update password we
                // must use different
                msg = "The new password must be different from the previous one";
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                logger.error(log);
                throw new SessionInternalError(
                        "The new password must be different from the previous one",
                        new String[] { "UserWS,password,validation.error.password.same.as.previous" }, HttpStatus.SC_BAD_REQUEST);
            } else {
                // password changed so do additional validation on the new
                // password

                if (!user.getPassword().matches(
                        Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES)) {
                    msg = "User's password must match required conditions.";
                    log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                    logger.error(log);
                    throw new SessionInternalError(
                            "User's password must match required conditions.",
                            new String[] { "UserWS,password,validation.error.password.size,8,40" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        if (dto.getCustomer() != null) {
            // Create Request to Update 911 Emergency Address
            Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.
                    createEventForCustomerUpdate(entityId, user.getId(), user, dto, new AssetBL().getAllAssetsByUserId(user.getId()));
            EventManager.process(addressUpdateEvent);
        }

        bl.update(executorId, dto);
        boolean updatedSSOStatus = SamlUtil.getUserSSOEnabledStatus(bl.getDto().getId());
        if(isSSOEnabled && isSSOEnabled!=updatedSSOStatus){
            resetPassword(dto.getId());
        }else if(!isSSOEnabled && isSSOEnabled!=updatedSSOStatus){
            try {
                bl.sendSSOEnabledUserCreatedEmailMessage(dto.getEntityId(),dto.getId(),1);
                bl.setSSOEnabledPCICompliantPassword();
            }catch (NotificationNotFoundException e){
                msg = "Notification for SSOEnabledUserCreatedEmail not found"
                        + dto.getId();
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_EVENT);
                logger.error(log);
            }
        }
        // now update the contact info
        if (user.getContact() != null
                && 0 != user.getMainRoleId().compareTo(Constants.TYPE_CUSTOMER)) {
            String email = user.getContact().getEmail();
            if (forceUniqueEmails(entityId) && null != email
                    && (bl.isEmailUsedByOthers(email))) {
                msg = "User already exists with email: " + email;
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                logger.error(log);
                throw new SessionInternalError(
                        "User already exists with email " + email,
                        new String[] { "ContactWS,email,validation.error.email.already.exists" }, HttpStatus.SC_BAD_REQUEST);
            }

            ContactDTOEx contact = new ContactDTOEx(user.getContact());
            new ContactBL().updateForUser(contact, user.getUserId(),
                    getCallerId());
        }
        for (CustomerNoteWS customerNotes : JArrays.toArrayList(user
                .getCustomerNotes())) {
            customerNotes.setCustomerId(UserBL.getUserEntity(user.getUserId())
                    .getCustomer().getId());
            createCustomerNote(customerNotes);
        }
    }

    /**
     * Retrieves a user with its contact and credit card information.
     *
     * @param userId
     *            The id of the user to be returned
     */
    @Override
    @Transactional(readOnly = true)
    public UserWS getUserWS(Integer userId)  {
        UserDTO user = userDAS.findNow(userId);
        if (null == user){
            throw new SessionInternalError("validation failed",
                    new String [] {String.format("User with id %d not found!",userId)}, HttpStatus.SC_NOT_FOUND);
        }
        UserBL bl = new UserBL(user);
        return bl.getUserWS();
    }

    /**
     * Retrieves all the contacts of a user
     *
     * @param userId
     *            The id of the user to be returned
     */
    @Override
    @Transactional(readOnly = true)
    public ContactWS[] getUserContactsWS(Integer userId) {
        ContactWS[] dtos = null;
        ContactBL contact = new ContactBL();
        List<ContactDTOEx> result = contact.getAll(userId);
        dtos = new ContactWS[result.size()];
        for (int f = 0; f < result.size(); f++) {
            dtos[f] = ContactBL.getContactWS(result.get(f));
        }

        return dtos;
    }

    /**
     * Retrieves the user id for the given username
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getUserId(String username)  {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        UserDTO dto = userDAS.findByUserName(username, getCallerCompanyId());
        if (dto == null) {
            return null;
        } else {
            return dto.getId();
        }
    }

    /**
     * Retrieves user by the user's email. This is only valid if Jbilling is
     * configured to force unique emails per user/customers in the company. If
     * unique emails are not forced then an exception is thrown and in such case
     * this method should not be used.
     *
     * @param email
     *            - email of the user
     * @return ID of the user with given email
     * @
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getUserIdByEmail(String email)  {
        if (null == email || 0 == email.trim().length()) {
            throw new SessionInternalError(
                    "User email can not be null or empty");
        }

        Integer entityId = getCallerCompanyId();

        if (forceUniqueEmails(entityId)) {
            UserBL userDas = new UserBL();
            List<UserDTO> users = userDas.findUsersByEmail(email, entityId);
            if (CollectionUtils.isEmpty(users)) {
                return null;
            } else if (1 == users.size()) {
                return users.iterator().next().getId();
            } else {
                throw new SessionInternalError(
                        "Multiple users found with the same email.");
            }
        } else {
            throw new SessionInternalError(
                    "Not configured to force unique emails per users.");
        }
    }

    public UserWS getUserByAccountNumber(String customerAccountNumber) {
        UserDTO user = userDAS.findByMetaFieldNameAndValue(getCallerCompanyId(), FileConstants.UTILITY_CUST_ACCT_NR, customerAccountNumber);
        return user != null ? UserBL.getWS(DTOFactory.getUserDTOEx(user)) : null;
    }

    @Override
    public UserWS getUserBySupplierID(String supplierId) {
        UserDTO user = userDAS.findSingleByMetaFieldNameAndValue(FileConstants.CUSTOMER_SUPPLIER_ID_META_FIELD_NAME, supplierId, getCallerCompanyId());
        return user != null ? UserBL.getWS(DTOFactory.getUserDTOEx(user)) : null;
    }

    /**
     * Retrieves an array of users in the required status
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getUsersInStatus(Integer statusId)
    {
        return getUsersByStatus(statusId, true);
    }

    /**
     * Retrieves an array of users in the required status
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getUsersNotInStatus(Integer statusId)
    {
        return getUsersByStatus(statusId, false);
    }

    /**
     * Retrieves an array of users in the required status
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getUsersByStatus(Integer statusId, boolean in)
    {
        try {
            UserBL bl = new UserBL();
            CachedRowSet users = bl.getByStatus(getCallerCompanyId(), statusId,
                    in);
            logger.debug("got collection. Now converting");
            Integer[] ret = new Integer[users.size()];
            int f = 0;
            while (users.next()) {
                ret[f] = users.getInt(1);
                f++;
            }
            users.close();
            return ret;
        } catch (Exception e) { // can't remove because of SQLException :(
            throw new SessionInternalError(e);
        }
    }

    @Override
    public CreateResponseWS createWithExistingUser(Integer userId, OrderWS order,  OrderChangeWS[] orderChanges){
        return create(getUserWS(userId), order, orderChanges);
    }

    /**
     * Creates a user, then an order for it, an invoice out the order and tries
     * the invoice to be paid by an online payment This is ... the mega call !!!
     */
    @Override
    public CreateResponseWS create(UserWS user, OrderWS order, OrderChangeWS[] orderChanges) {
        return create(user,order,orderChanges,true);
    }


    public CreateResponseWS create(UserWS user, OrderWS order,
            OrderChangeWS[] orderChanges, boolean doCCPayment)  {

        if(user != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(user.getPaymentInstruments());
        }

        CreateResponseWS retValue = new CreateResponseWS();

        // the user first if user is not present
        final Integer userId = user.getId() > 0 ? user.getId() : createUser(user);
        retValue.setUserId(userId);

        if (userId == null) {
            return retValue;
        }

        // the order and (if needed) invoice
        order.setUserId(userId);
        validateLines(order);

        /*
         * #7899 - The order being created is evaluated for subscription lines,
         * if order is containing any subscription products then internal
         * account and order are created for each subscription line, if all the
         * lines are subscription lines then main order is not created and we
         * get only internal accounts and orders
         */
        List<OrderChangeWS> changes = JArrays.toArrayList(orderChanges);
        createSubscriptionAccountAndOrder(order.getUserId(), order, false,
                changes);
        orderChanges = changes != null ? changes
                .toArray(new OrderChangeWS[changes.size()]) : null;

                Integer orderId = null;
                InvoiceDTO invoice = null;
                if (order.getOrderLines().length > 0) {
                    orderId = doCreateOrder(order, orderChanges, true).getId();
                    invoice = doCreateInvoice(orderId);
                }

                retValue.setOrderId(orderId);

                if (invoice != null) {
                    retValue.setInvoiceId(invoice.getId());

                    if (doCCPayment){
                        // find credit card
                        try (PaymentInformationDTO creditCardInstrument = getCreditCard(userId)) {

                            // the payment, if we have a credit card
                            if (creditCardInstrument != null) {
                                PaymentDTOEx payment = doPayInvoice(invoice,
                                        creditCardInstrument);
                                PaymentAuthorizationDTOEx result = null;
                                if (payment != null) {
                                    result = new PaymentAuthorizationDTOEx(payment
                                            .getAuthorization().getOldDTO());
                                    result.setResult(new Integer(payment.getPaymentResult()
                                            .getId()).equals(Constants.RESULT_OK));
                                }
                                retValue.setPaymentResult(result);
                                retValue.setPaymentId(null != payment ? payment.getId() : null);
                            }
                        } catch (Exception exception) {
                            logger.debug("Exception: " + exception);
                            throw new SessionInternalError(exception);
                        }
                    }

                } else {
                    throw new SessionInternalError("Invoice expected for order: "
                            + orderId);
                }

                return retValue;
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerWS getPartner(Integer partnerId)  {
        IUserSessionBean userSession = Context
                .getBean(Context.Name.USER_SESSION);
        PartnerDTO dto = userSession.getPartnerDTO(partnerId);

        return PartnerBL.getWS(dto);
    }

    @Override
    public Integer createPartner(UserWS newUser, PartnerWS partner)
    {
        String msg, log;
        UserBL bl = new UserBL();
        newUser.setUserId(0);
        Integer entityId = getCallerCompanyId();

        if (bl.exists(newUser.getUserName(), entityId)) {
            throw new SessionInternalError(
                    "User already exists with username "
                            + newUser.getUserName(),
                            new String[] { "UserWS,userName,validation.error.user.already.exists" });
        }

        PartnerDTO partnerDto = PartnerBL.getPartnerDTO(partner);
        MetaFieldBL.fillMetaFieldsFromWS(entityId, partnerDto,
                newUser.getMetaFields());

        UserDTOEx dto = new UserDTOEx(newUser, entityId);
        dto.setPartner(partnerDto);

        Integer userId = bl.create(dto, getCallerId());

        ContactBL cBl = new ContactBL();
        if (newUser.getContact() != null) {
            newUser.getContact().setId(0);
            cBl.createForUser(new ContactDTOEx(newUser.getContact()), userId,
                    getCallerId());
        }

        bl.createCredentialsFromDTO(dto);

        boolean isSSOEnabled = SamlUtil.getUserSSOEnabledStatus(userId);
        if(isSSOEnabled){
            try {
                bl.sendSSOEnabledUserCreatedEmailMessage(entityId,userId,1);
                bl.setSSOEnabledPCICompliantPassword();
            }catch (NotificationNotFoundException e){
                msg = "Notification for SSOEnabledUserCreatedEmail not found"
                        + userId;
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_EVENT);
                logger.error(log);
            }
        }
        return bl.getDto().getPartner().getId();

    }

    @Override
    public void updatePartner(UserWS user, PartnerWS partner)
    {
        String msg, log;
        IUserSessionBean userSession = Context
                .getBean(Context.Name.USER_SESSION);
        Integer entityId = getCallerCompanyId();

        Integer userId = null;
        if (null != user) {
            userId = user.getId();
        } else if (null != partner.getUserId()) {
            userId = partner.getUserId();
        }

        boolean isSSOEnabled = SamlUtil.getUserSSOEnabledStatus(userId);

        if (user != null) {
            UserDTOEx userDto = new UserDTOEx(user, entityId);
            userSession.update(getCallerId(), userDto);
        }

        if (partner != null) {
            PartnerDTO partnerDto = PartnerBL.getPartnerDTO(partner);

            if (user != null) {
                MetaFieldBL.fillMetaFieldsFromWS(entityId, partnerDto,
                        user.getMetaFields());
            }

            userSession.updatePartner(getCallerId(), partnerDto);
        }

        boolean updatedSSOStatus = SamlUtil.getUserSSOEnabledStatus(userId);

        UserBL bl = new UserBL(userId);
        if(isSSOEnabled && isSSOEnabled!=updatedSSOStatus){
            resetPassword(userId);
        }else if(!isSSOEnabled && isSSOEnabled!=updatedSSOStatus){
            try {
                bl.sendSSOEnabledUserCreatedEmailMessage(entityId,userId,1);
                bl.setSSOEnabledPCICompliantPassword();
            }catch (NotificationNotFoundException e){
                msg = "Notification for SSOEnabledUserCreatedEmail not found"
                        + user.getId();
                log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_EVENT);
                logger.error(log);
            }
        }
    }

    @Override
    public void deletePartner(Integer partnerId)  {
        PartnerBL bl = new PartnerBL(partnerId);
        bl.delete(getCallerId());
    }

    /**
     * Return the UserCode objects linked to a user.
     *
     * @param userId
     * @return
     * @
     */
    @Override
    @Transactional(readOnly = true)
    public UserCodeWS[] getUserCodesForUser(Integer userId)  {
        List<UserCodeDTO> userCodes = new UserBL().getUserCodesForUser(userId);
        return UserBL.convertUserCodeToWS(userCodes);
    }

    /**
     * Create a UserCode
     *
     * @param userCode
     * @return
     * @
     */
    @Override
    public Integer createUserCode(UserCodeWS userCode)
    {
        UserBL userBL = new UserBL();
        return userBL.createUserCode(userCode);
    }

    /**
     * Update a UserCode
     *
     * @param userCode
     * @return
     * @
     */
    @Override
    public void updateUserCode(UserCodeWS userCode)  {
        UserBL userBL = new UserBL();
        userBL.updateUserCode(userCode);
    }

    /**
     * Return ids of objects of the specified type linked to the User Code.
     *
     * @param userCode
     * @return
     */
    private Integer[] getAssociatedObjectsByUserCodeAndType(String userCode,
            UserCodeObjectType objectType) {
        List<Integer> objectIds = new UserBL()
        .getAssociatedObjectsByUserCodeAndType(userCode, objectType);
        return objectIds.toArray(new Integer[objectIds.size()]);
    }

    /**
     * Return ids of customers linked to the User Code.
     *
     * @param userCode
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getCustomersByUserCode(String userCode)
    {
        return getAssociatedObjectsByUserCodeAndType(userCode,
                UserCodeObjectType.CUSTOMER);
    }

    /**
     * Return ids of orders linked to the User Code.
     *
     * @param userCode
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getOrdersByUserCode(String userCode)
    {
        return getAssociatedObjectsByUserCodeAndType(userCode,
                UserCodeObjectType.ORDER);
    }

    /**
     * Return ids of objects of the specified type linked to the user.
     *
     * @param userId
     * @param objectType
     * @return
     */
    private Integer[] getAssociatedObjectsByUserAndType(int userId,
            UserCodeObjectType objectType)  {
        List<Integer> objectIds = new UserBL()
        .getAssociatedObjectsByUserAndType(userId, objectType);
        return objectIds.toArray(new Integer[objectIds.size()]);
    }

    /**
     * Return ids of customers linked to the user through a user code.
     *
     * @param userId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getCustomersLinkedToUser(Integer userId)  {
        return getAssociatedObjectsByUserAndType(userId, UserCodeObjectType.CUSTOMER);
    }

    /**
     * Return ids of orders linked to the user through a user code.
     *
     * @param userId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getOrdersLinkedToUser(Integer userId)  {
        return getAssociatedObjectsByUserAndType(userId, UserCodeObjectType.ORDER);
    }

    /**
     * Retrieves all item categories that are being used for specific partner
     * Item category is considered to belong to a partner it it has at least one
     * item that belongs to the provided partner The partner belonging is
     * determined by a metafield for each item. If the metafield value matches
     * the provided partner name the item is considered to be used by the
     * partner
     *
     * @param partner
     * @param parentCategoriesOnly
     *            - if set to true it will take into consideration the
     *            parent-child category relation and will only include
     *            parent(top) categories
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public ItemTypeWS[] getItemCategoriesByPartner(String partner,
            boolean parentCategoriesOnly) {

        if (null == partner) {
            throw new SessionInternalError(
                    "Null value for partner is not allowed");
        }

        return new ItemTypeBL().getItemCategoriesByPartner(partner,
                parentCategoriesOnly);
    }

    /**
     * Uses the parent-child item category relation to determine the child item
     * types from the provided parent item Type Id
     *
     * @param itemTypeId
     *            - id of the parent category
     * @return Child categories for the provided parent categories
     */
    @Override
    @Transactional(readOnly = true)
    public ItemTypeWS[] getChildItemCategories(Integer itemTypeId) {

        if (null == itemTypeId) {
            throw new SessionInternalError(
                    "Null value for itemTypeId is not allowed");
        }

        if (null == (new ItemTypeDAS()).findNow(itemTypeId)) {
            logger.debug("Category with the given ID does not exist");
            return null;
        }

        return new ItemTypeBL().getChildItemCategories(itemTypeId);
    }

    /**
     * Retrieves addon item defined for the provided itemId Item is considered
     * to be an Addon item if it belongs to a category: ADDON-productCode where
     * the product code is retrieved from the provided itemId
     *
     * @param itemId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public ItemDTOEx[] getAddonItems(Integer itemId) {

        if (null == itemId) {
            throw new SessionInternalError(
                    "Null value for itemId is not allowed");
        }

        ItemDTO item = new ItemDAS().findNow(itemId);
        if (null == item) {
            throw new SessionInternalError("Item with given id does not exist");
        }

        String addonCategoryName = "ADDON-" + item.getInternalNumber();

        ItemTypeDTO addonCategory = new ItemTypeDAS().findByDescription(
                getCallerCompanyId(), addonCategoryName);

        if (null == addonCategory) {
            logger.debug("Addon category with description " + addonCategoryName
                    + " does not exist");
            return null;
        }

        return new ItemBL().getAllItemsByType(addonCategory.getId(),
                getCallerCompanyId());
    }

    /**
     * Pays given invoice, using the first credit card available for invoice'd
     * user.
     *
     * @return <code>null</code> if invoice has not positive balance, or if user
     *         does not have credit card
     * @return resulting authorization record. The payment itself can be found
     *         by calling getLatestPayment
     */
    @Override
    // this method does not start a transaction since transaction
    // during payment processing is managed manually
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentAuthorizationDTOEx payInvoice(Integer invoiceId)
    {
        logger.debug("In payInvoice..");
        if (invoiceId == null) {
            logger.debug("Invoice id null.");
            throw new SessionInternalError("Can not pay null invoice", HttpStatus.SC_BAD_REQUEST);
        }

        final InvoiceDTO invoice = findInvoice(invoiceId);
        if (null == invoice) {
            logger.debug("No invoice found invoice id: %d", invoiceId);
            throw new SessionInternalError("Invoice not found!!", HttpStatus.SC_NOT_FOUND);
        }
        if (!getCallerCompanyId().equals(
                invoice.getBaseUser().getEntity().getId())) {
            throw new SessionInternalError(
                    "Applying invoices from another entity not supported!!", HttpStatus.SC_BAD_REQUEST);
        }
        PaymentInformationDTO creditCardInstrument = getCreditCard(invoice
                .getBaseUser().getUserId());
        if (creditCardInstrument == null) {
            return null;
        }

        PaymentDTOEx payment = doPayInvoice(invoice, creditCardInstrument);

        try{
            creditCardInstrument.close();
        }catch (Exception exception){
            logger.debug("Exception: " + exception);
        }

        PaymentAuthorizationDTOEx result = null;
        if (payment != null) {
            result = new PaymentAuthorizationDTOEx(payment.getAuthorization()
                    .getOldDTO());
            result.setResult(new Integer(payment.getPaymentResult().getId())
            .equals(Constants.RESULT_OK));
        }

        return result;
    }

    /*
     * ORDERS
     */
    /**
     * @return the information of the payment aurhotization, or NULL if the user
     *         does not have a credit card
     */
    @Override
    public PaymentAuthorizationDTOEx createOrderPreAuthorize(OrderWS order,
            OrderChangeWS[] orderChanges)  {

        PaymentAuthorizationDTOEx retValue = null;
        // start by creating the order. It'll do the checks as well
        Integer orderId = createOrder(order, orderChanges);

        Integer userId = order.getUserId();
        PaymentInformationDTO cc = getCreditCard(userId);
        UserBL user = new UserBL();
        Integer entityId = user.getEntityId(userId);
        OrderDTO dbOrder = orderDAS.find(orderId);
        if (cc != null) {
            PaymentInformationBL piBl = new PaymentInformationBL();

            try {
                retValue = piBl.validatePreAuthorization(entityId, userId, cc,
                        dbOrder.getTotal(), dbOrder.getCurrencyId(),
                        getCallerId());
            } catch (PluggableTaskException e) {
                throw new SessionInternalError("doing validation",
                        WebServicesSessionSpringBean.class, e);
            }
        }

        try{
            cc.close();
        }catch(Exception exception){
            logger.debug("Exception: " + exception);
        }

        // order has been pre-authorized. Informing the tasks
        if (retValue != null && retValue.getResult().equals(Boolean.TRUE)) {
            EventManager
            .process(new OrderPreAuthorizedEvent(entityId, dbOrder));
        }

        return retValue;
    }

    /**
     * When a plan order is created, multiple orders can get created and these
     * are linked together by primaryOrderId. This is a Function that returns
     * all the linked orders if the primary order id is provided.
     *
     * @param primaryOrderId
     * @return List<OrderWS> - The list of linked orders including the primary
     *         order itself.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderWS[] getLinkedOrders(Integer primaryOrderId)
    {
        List<OrderDTO> linkedOrders = orderDAS
                .findByPrimaryOrderId(primaryOrderId);
        if (null == linkedOrders) {
            // If no linked orders are found, return an empty array.
            return new OrderWS[0];
        }

        List<OrderWS> orders = new ArrayList<>();

        OrderBL bl = null;
        for (OrderDTO dto : linkedOrders) {
            bl = new OrderBL(dto);
            orders.add(bl.getWS(getCallerLanguageId()));
        }

        return orders.toArray(new OrderWS[orders.size()]);
    }

    /**
     * Update the given order, or create it if it doesn't already exist.
     *
     * @param order
     *            order to update or create
     * @return order id
     * @
     */
    @Override
    @Transactional
    public Integer createUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        return createUpdateOrder(order, orderChanges, false);
    }

    @Transactional
    public Integer createUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges, boolean skipFinishOrderValidation) {

        IOrderSessionBean orderSession = Context
                .getBean(Context.Name.ORDER_SESSION);
        setOrderOnOrderChanges(order, orderChanges);
        validateOrder(order, orderChanges, skipFinishOrderValidation);
        validateLines(order);

        if (order.getId() != null) {
            validateActiveSinceDate(order);
        }

        /*
         * #7899 - The order being created is evaluated for subscription lines,
         * if order is containing any subscription products then internal
         * account and order are created for each subscription line
         */
        List<OrderChangeWS> changes = JArrays.toArrayList(orderChanges);
        createSubscriptionAccountAndOrder(order.getUserId(), order, false,
                changes);
        orderChanges = changes != null ? changes
                .toArray(new OrderChangeWS[changes.size()]) : null;

                // if order has some lines left (that are non subscription) then create
                // the order
                if (order.getOrderLines().length > 0 || orderChanges.length > 0) {
                    if (null != orderChanges && orderChanges.length > 0) {
                        for (OrderChangeWS change : orderChanges) {
                            if (null != change.getItemId()) {
                                boolean isTeaserPricing = OrderHelper.itemHasTeaserPricing(order.getUserId(),
                                        change.getItemId(), new Date(), getCallerCompanyId());
                                if (isTeaserPricing)
                                    change.setUseItem(0);
                            }
                        }
                    }

                    OrderChangeWS[] oldOrderChanges = getOrderChanges(order.getId());
                    // do some transformation from WS to DTO
                    Map<OrderWS, OrderDTO> wsToDtoOrdersMap = new HashMap<OrderWS, OrderDTO>();
                    Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap = new HashMap<OrderLineWS, OrderLineDTO>();
                    OrderBL orderBL = new OrderBL();
                    OrderDTO dto = orderBL.getDTO(order, wsToDtoOrdersMap,
                            wsToDtoLinesMap);
                    OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(dto);
                    List<OrderChangeDTO> changeDtos = new LinkedList<OrderChangeDTO>();
                    List<Integer> deletedChanges = new LinkedList<Integer>();
                    convertOrderChangeWsToDto(orderChanges, changeDtos, deletedChanges,
                            wsToDtoOrdersMap, wsToDtoLinesMap);
                    validateDiscountLines(rootOrder, changeDtos);
                    Integer rootOrderId = orderSession.createUpdate(
                            getCallerCompanyId(), getCallerId(), rootOrder.getUser().getLanguageIdField(),
                            rootOrder, changeDtos, deletedChanges);
                    if (null != rootOrderId) {
                        Integer userId = order.getUserId();
                        Integer entityId = new UserBL(userId).getDto().getEntity().getId();
                        OrderChangeWS[] newOrderChanges = getOrderChanges(rootOrderId);
                        EventManager.process(new AssetStatusUpdateEvent(userId, newOrderChanges, oldOrderChanges, entityId));
                    }
                    //cleaning reserve instance cache
                    clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId() +
                            "-UserID-" + order.getUserId());

                    return wsToDtoOrdersMap.get(order).getId();
                }

                return null;
    }

    private void setOrderOnOrderChanges(OrderWS order, OrderChangeWS[] orderChanges) {
        for (OrderChangeWS orderChange: orderChanges) {
            if (orderChange.getOrderId() == null && orderChange.getOrderWS() == null) {
                orderChange.setOrderWS(order);
            }
        }
    }

    private void validateUpdateOrder(OrderWS order) {
        //cannot edit FINISHED orders
        if ( null != order.getId() && order.getId().intValue() > 0 ) {
            OrderDTO dbOrder= orderDAS.findNow(order.getId());
            if ( dbOrder.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED) ) {
                throw new SessionInternalError("An order whose status is FINISHED, is non-editable.",
                        new String[]{"validation.error.finished.order.status"});
            }
        }
    }

    @Override
    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public OrderWS rateOrder(OrderWS order, OrderChangeWS[] orderChanges)
    {

        return doCreateOrder(order, orderChanges, false);
    }

    @Override
    public OrderWS[] rateOrders(OrderWS orders[], OrderChangeWS[] orderChanges)
    {

        if (orders == null || orders.length == 0) {
            logger.debug("Call to rateOrders without orders to rate");
            return null;
        }

        OrderWS retValue[] = new OrderWS[orders.length];
        for (int index = 0; index < orders.length; index++) {
            OrderWS currentOrder = orders[index];
            List<OrderChangeWS> currentOrderChanges = new LinkedList<>();
            if (orderChanges != null) {
                LinkedHashSet<OrderWS> currentOrders = OrderHelper
                        .findAllChildren(currentOrder);
                currentOrders.add(currentOrder);
                // find order changes for current order
                for (OrderChangeWS orderChange : orderChanges) {
                    if (orderChange.getOrderWS() != null) {
                        if (currentOrders.contains(orderChange.getOrderWS())) {
                            currentOrderChanges.add(orderChange);
                        }
                    } else if (orderChange.getOrderId() != null) {
                        for (OrderWS childOrder : currentOrders) {
                            if (orderChange.getOrderId().equals(
                                    childOrder.getId())) {
                                currentOrderChanges.add(orderChange);
                            }
                        }
                    }
                }
            }
            retValue[index] = doCreateOrder(currentOrder,
                    currentOrderChanges
                    .toArray(new OrderChangeWS[currentOrderChanges
                                               .size()]), false);
        }
        return retValue;
    }

    @Override
    public Map<String, BigDecimal> calculateUpgradePlan(Integer orderId, Integer planId, String discountCode) {
        DiscountDTO discount = null;
        if (discountCode != null) {
            DiscountBL discountBL = new DiscountBL(discountCode, getCallerCompanyId());
            if (discountBL.getEntity() == null) {
                throw new SessionInternalError("The discount must be exist");
            }

            discount = discountBL.getEntity();
        }

        return new OrderBL(orderId).calculateUpgradePlan(planId, discount);
    }

    /**
     * Calculate diff in OrderChange objects between two plans for order on
     * effective date. Difference will be calculated according to rules of
     * SwapMethod selected
     *
     * @param order
     *            Order to change plan
     * @param existingPlanItemId
     *            Existed plan item ID
     * @param swapPlanItemId
     *            Target plan item ID
     * @param method
     *            Swap method. Possible values: - DEFAULT swap method will
     *            calculate the order changes as the existing plan was removed
     *            and the new plan item was added to the order - DIFF swap
     *            method will calculate the difference between the existing plan
     *            item and swap plan item and generate order changes only for
     *            that difference.
     * @param effectiveDate
     *            Target date to apply changes
     * @return array of order changes to be applied to swap existingPlan to
     *         targetPlan
     */
    @Override
    public OrderChangeWS[] calculateSwapPlanChanges(OrderWS order,
            Integer existingPlanItemId, Integer swapPlanItemId,
            SwapMethod method, Date effectiveDate) {
        OrderLineWS existedPlanLine = OrderHelper.find(order.getOrderLines(),
                existingPlanItemId);
        if (existedPlanLine == null) {
            return new OrderChangeWS[] {};
        }
        /* JB-3169 : based on PREFERNCE, when a user swaps a plan, the effective day of the new plan ,
         * subscription start date & end date of the old plan become equal to either nextBillable date or Active since date if former is null.
         */
        try {
            if (1 == PreferenceBL.getPreferenceValueAsIntegerOrZero(getCallerCompanyId(),
                    Constants.PREFERENCE_SWAP_PLAN)) {
                if (null != order.getNextBillableDay()) {
                    effectiveDate = order.getNextBillableDay();
                    if(Constants.ORDER_BILLING_PRE_PAID.equals(order.getBillingTypeId())) {
                        OrderPeriodDTO orderPeriodDTO =new OrderPeriodDAS().find(order.getPeriod());
                        int periodUnitId = orderPeriodDTO.getUnitId();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(order.getNextBillableDay());
                        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                        PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
                        effectiveDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(effectiveDate),
                                orderPeriodDTO.getValue() * -1L));
                        if (effectiveDate.before(order.getActiveSince())) {
                            effectiveDate = order.getActiveSince();
                        }
                    }
                } else {
                    effectiveDate = order.getActiveSince();
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        Map<Integer, BigDecimal> itemsQuantityMap = PlanBL
                .calculateItemDiffQuantityForPlans(existingPlanItemId,
                        swapPlanItemId);

        List<OrderChangeWS> result = new LinkedList<OrderChangeWS>();
        OrderChangeStatusDTO applyOrderChangeStatus = new OrderChangeStatusDAS()
        .findApplyStatus(getCallerCompanyId());
        OrderChangeTypeWS defaultChangeType = getOrderChangeTypeById(Constants.ORDER_CHANGE_TYPE_DEFAULT);
        for (Integer itemId : itemsQuantityMap.keySet()) {
            BigDecimal diffQuantity = itemsQuantityMap.get(itemId);
            int scale = diffQuantity.scale();
            BigDecimal quantityCoef = BigDecimal.ONE;
            if (itemId.equals(existingPlanItemId)
                    || itemId.equals(swapPlanItemId)) {
                quantityCoef = existedPlanLine.getQuantityAsDecimal();
            }
            BigDecimal subtractQuantity = diffQuantity
                    .compareTo(BigDecimal.ZERO) < 0 ? diffQuantity.multiply(
                            quantityCoef).setScale(scale, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                            BigDecimal addQuantity = diffQuantity.compareTo(BigDecimal.ZERO) > 0 ? diffQuantity
                                    .multiply(quantityCoef).setScale(scale,
                                            RoundingMode.HALF_UP) : BigDecimal.ZERO;
                                    OrderLineWS line = OrderHelper.find(order.getOrderLines(), itemId);
                                    if (line != null && SwapMethod.DEFAULT.equals(method)) {
                                        addQuantity = addQuantity.add(line.getQuantityAsDecimal().add(
                                                subtractQuantity)); // subtract quantity is negative
                                        // number
                                        subtractQuantity = line.getQuantityAsDecimal().negate();
                                    }
                                    if (line != null
                                            && subtractQuantity.compareTo(BigDecimal.ZERO) != 0) {
                                        OrderChangeWS subtractChange = OrderChangeBL.buildFromLine(
                                                line, order, applyOrderChangeStatus.getId());
                                        subtractChange.setOrderChangeTypeId(defaultChangeType.getId());
                                        subtractChange.setQuantityAsDecimal(subtractQuantity);
                                        subtractChange.setStartDate(effectiveDate);
                                        // JB-3169: setting end date of the old plan
                                        subtractChange.setEndDate(effectiveDate);
                                        result.add(subtractChange);
                                    }
                                    if (addQuantity.compareTo(BigDecimal.ZERO) != 0) {
                                        OrderLineWS lineForAdd = null;
                                        if (line != null && SwapMethod.DIFF.equals(method)) {
                                            lineForAdd = line;
                                        }
                                        if (lineForAdd == null) {
                                            lineForAdd = new OrderLineWS();
                                            lineForAdd.setOrderId(order.getId());
                                            lineForAdd.setItemId(itemId);
                                            lineForAdd.setQuantityAsDecimal(addQuantity);
                                            lineForAdd.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                                            lineForAdd.setUseItem(true);
                                            lineForAdd.setAssetIds(new Integer[] {});
                                            ItemDTO item = new ItemDAS().find(itemId);
                                            lineForAdd.setDescription(item
                                                    .getDescription(getCallerLanguageId()));
                                            PriceModelDTO priceModelDto = item.getPrice(effectiveDate,
                                                    getCallerCompanyId());
                                            if (priceModelDto != null
                                                    && priceModelDto.getRate() != null) {
                                                scale = priceModelDto.getRate().scale();
                                                lineForAdd.setPrice(lineForAdd.getQuantityAsDecimal()
                                                        .multiply(priceModelDto.getRate())
                                                        .setScale(scale, RoundingMode.HALF_UP));
                                            }
                                        }
                                        OrderChangeWS addChange = OrderChangeBL.buildFromLine(
                                                lineForAdd, order, applyOrderChangeStatus.getId());
                                        addChange.setOrderChangeTypeId(defaultChangeType.getId());
                                        addChange.setQuantityAsDecimal(addQuantity);
                                        addChange.setStartDate(effectiveDate);
                                        result.add(addChange);
                                    }
        }

        OrderChangeWS addPlanChange = null;
        for (OrderChangeWS change : result) {
            if (change.getItemId() != null
                    && change.getItemId().equals(swapPlanItemId)) {
                addPlanChange = change;
                break;
            }
        }
        if (addPlanChange != null) {
            List<OrderChangePlanItemWS> orderChangePlanItems = new LinkedList<OrderChangePlanItemWS>();
            Iterator<OrderChangeWS> changesIterator = result.iterator();
            while (changesIterator.hasNext()) {
                OrderChangeWS change = changesIterator.next();
                if ((change.getOrderLineId() == null || change.getOrderLineId() <= 0)
                        && !change.equals(addPlanChange)) {
                    changesIterator.remove();
                    OrderChangePlanItemWS changePlanItemWS = new OrderChangePlanItemWS();
                    changePlanItemWS.setItemId(change.getItemId());
                    changePlanItemWS.setDescription(change.getDescription());
                    orderChangePlanItems.add(changePlanItemWS);
                }
            }
            addPlanChange.setOrderChangePlanItems(orderChangePlanItems
                    .toArray(new OrderChangePlanItemWS[orderChangePlanItems
                                                       .size()]));
        }
        return result.toArray(new OrderChangeWS[result.size()]);
    }

    /**
     * Swap the existing plan with swap plan for the order as per the SwapMethod provided
     *
     * @param order
     *            OrderId to change plan
     * @param existingPlanItemId
     *            Existed plan code
     * @param swapPlanItemId
     *            Target plan code
     * @param method
     *            Swap method. Possible values: - DEFAULT swap method will
     *            calculate the order changes as the existing plan was removed
     *            and the new plan item was added to the order - DIFF swap
     *            method will calculate the difference between the existing plan
     *            item and swap plan item and generate order changes only for
     *            that difference.
     * @return boolean : TRUE if the swap plan is successful
     */
    @Override
    public boolean swapPlan(Integer orderId, String existingPlanCode, String swapPlanCode, SwapMethod swapMethod) {
        logger.debug(
                "Swap Plan with following parameters : orderid: {} existingPlanCode: {} swapPlanCode: {} swapMethod: {}",
                orderId, existingPlanCode, swapPlanCode, swapMethod);
        OrderWS orderWS = getOrder(orderId);

        PlanWS existingPlan = getPlanByInternalNumber(existingPlanCode, getCallerCompanyId());
        PlanWS swapPlan = getPlanByInternalNumber(swapPlanCode, getCallerCompanyId());
        validateSwapPlan(orderWS, existingPlan, swapPlan);
        Date effectiveDate = null != orderWS.getNextBillableDay() ? orderWS.getNextBillableDay() : orderWS
                .getActiveSince();
        logger.debug("Calculate swap plan changes for effectiveDate: {}", effectiveDate);
        OrderChangeWS[] orderChanges = calculateSwapPlanChanges(orderWS, existingPlan.getItemId(), swapPlan.getItemId(),
                swapMethod, effectiveDate);
        if (ArrayUtils.isEmpty(orderChanges)) {
            logger.debug("Order changes not found");
            throw new SessionInternalError("Order changes not found.",
                    new String[]{"validation.error.order.changes.not.found"});
        }
        updateOrder(orderWS, orderChanges);
        return true;
    }

    /**
     * Helper method to validate the {@link:swapPlan}
     * @param orderWS
     * @param existingPlan
     * @param swapPlan
     */
    private void validateSwapPlan(OrderWS orderWS, PlanWS existingPlan, PlanWS swapPlan) {
        if (null == orderWS) {
            logger.debug("Order not found");
            throw new SessionInternalError("Order not found.",
                    new String[]{"validation.error.order.not.found"});
        }
        if (null == existingPlan) {
            logger.debug("Existing plan not found");
            throw new SessionInternalError("Existing Plan not found.",
                    new String[]{"validation.error.existing.plan.not.found"});
        }
        if (null == swapPlan) {
            logger.debug("Swap plan not found");
            throw new SessionInternalError("Swap Plan not found.",
                    new String[]{"validation.error.swap.plan.not.found"});
        }
    }

    @Override
    public void swapAssets(Integer orderId, SwapAssetWS[] swapRequests) {
        if(null == orderId) {
            logger.error("Order parameter is null");
            throw new SessionInternalError("Please provide non null orderId.", "Order id is null", HttpStatus.SC_BAD_REQUEST);
        }
        if(ArrayUtils.isEmpty(swapRequests)) {
            logger.error("swapRequest parameter is null or empty");
            throw new SessionInternalError("swapRequest is null or empty", "Please enter swapRequest.", HttpStatus.SC_BAD_REQUEST);
        }
        try {
            //validates Order and swapAssetRequest
            validateSwapAssetRequest(orderId, swapRequests);
            OrderWS order = getOrder(orderId);
            for(SwapAssetWS swapAsset : swapRequests) {
                for(OrderLineWS line : order.getOrderLines()) {
                    Integer existingAssetId = assetDAS.getAssetByIdentifier(swapAsset.getExistingIdentifier()).getId();
                    Integer[] assets = line.getAssetIds();
                    if(ArrayUtils.isEmpty(assets) || !ArrayUtils.contains(assets, existingAssetId)) {
                        continue;
                    }
                    Integer newAssetId = assetDAS.getAssetByIdentifier(swapAsset.getNewIdentifier()).getId();
                    logger.debug("replacing old asset {} with new asset {}", existingAssetId, newAssetId);
                    assets[ArrayUtils.indexOf(assets, existingAssetId)] = newAssetId;
                    line.setAssetIds(assets);
                }
            }
            //updating order
            createUpdateOrder(order, OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus(getCallerCompanyId())), true);
        } catch(SessionInternalError ex) {
            throw ex;
        } catch(Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in swapAssets"},
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Validates {@link SwapAssetWS} request and given order
     * @param orderId
     * @param swapRequests
     */
    private void validateSwapAssetRequest(Integer orderId, SwapAssetWS[] swapRequests) {
        OrderDTO order = orderDAS.findNow(orderId);
        if(null == order) {
            logger.error("order {} not found for entity {}", order, getCallerCompanyId());
            throw new SessionInternalError("validation failed", "Please enter a valid order id.", HttpStatus.SC_NOT_FOUND);
        }
        if(!order.containsAssets()) {
            logger.error("order {} has no assets on it", orderId);
            throw new SessionInternalError("validation failed", "order has no assets on it.", HttpStatus.SC_BAD_REQUEST);
        }
        List<String> errors = new ArrayList<>();
        for(SwapAssetWS swapAsset : swapRequests) {
            String existingIdentifier = swapAsset.getExistingIdentifier();
            AssetDTO existingAsset = null;
            AssetDTO newAsset = null;
            if(StringUtils.isEmpty(existingIdentifier)) {
                logger.debug("existingIdentifier parameter is null or empty!");
                errors.add("existingIdentifier parameter is null or empty!");
            } else {
                existingAsset = assetDAS.getAssetByIdentifier(existingIdentifier);
                if(null == existingAsset) {
                    logger.error("Asset Identifier {} not found in system", existingIdentifier);
                    errors.add("Asset Identifier [" + existingIdentifier + "] not found in system");
                }
            }

            String newIdentifier = swapAsset.getNewIdentifier();
            if(StringUtils.isEmpty(newIdentifier)) {
                logger.debug("newIdentifier parameter is null or empty!");
                errors.add("newIdentifier parameter is null or empty!");
            } else {
                newAsset = assetDAS.getAssetByIdentifier(newIdentifier);
                if(null == newAsset) {
                    logger.error("Asset Identifier {} not found in system", newIdentifier);
                    errors.add("Asset Identifier [" + newIdentifier + "] not found in system");
                }
            }

            if(null!= existingAsset && null!= newAsset) {
                if(existingIdentifier.equals(newIdentifier)) {
                    logger.error("Both assets [{}, {}] are equal", existingIdentifier, newIdentifier);
                    String errorMessage = "Both Assets [%s, %s] are equal";
                    throw new SessionInternalError("validation failed", String.format(errorMessage, existingIdentifier, newIdentifier), HttpStatus.SC_BAD_REQUEST);

                }
                if(null!= newAsset.getOrderLine()) {
                    logger.error("Asset {} alreday assigned to other order", newAsset.getId());
                    throw new SessionInternalError("validation failed", "newIdentifier "+ newAsset.getIdentifier() + " alreday assigned to other order",
                            HttpStatus.SC_BAD_REQUEST);
                }
                if(!order.isAssetPresent(existingAsset.getId())) {
                    logger.error("Asset identifier {} not found on order {}", existingIdentifier, orderId);
                    errors.add("Asset Identifier [" + existingIdentifier + "] not found on order [" + orderId + "]");
                }
                if(existingAsset.getItem().getId()!= newAsset.getItem().getId()) {
                    logger.debug("existing {} and new {} asset identfiers belongs to different items [{}, {}]", existingIdentifier, newIdentifier,
                            existingAsset.getItem().getId(), newAsset.getItem().getId());
                    String message = "Existing [%s] and New [%s]asset identfiers belongs to different items [%d, %d]";
                    errors.add(String.format(message, existingIdentifier, newIdentifier,
                            existingAsset.getItem().getId(), newAsset.getItem().getId()));
                }
            }
        }

        if(CollectionUtils.isNotEmpty(errors)) {
            logger.error("SwapAsset Validation failed with errors {}", errors);
            throw new SessionInternalError("validation failed",
                    errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Override
    public void updateItem(ItemDTOEx item) {
        // do validation
        validateItem(item);
        updateItem(item, false);
    }

    public void updateItem(ItemDTOEx item, boolean isPlan) {
        // check if all descriptions are to delete
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();
        boolean noDescriptions = true;
        for (InternationalDescriptionWS description : descriptions) {
            if (!description.isDeleted()) {
                noDescriptions = false;
                break;
            }
        }
        //ToDo VCA, this is duplicate, this is validated in validateItem()
        if (noDescriptions) {
            throw new SessionInternalError(
                    "Must have a description",
                    new String[] { "ItemDTOEx,descriptions,validation.error.is.required" }, HttpStatus.SC_BAD_REQUEST);
        }

        //for getting pricingunits
        SortedMap<Date,RatingConfigurationWS> ratingConfiguration=item.getRatingConfigurations();

        Integer executorId = getCallerId();
        Integer languageId = getCallerLanguageId();

        // do some transformation from WS to DTO :(
        ItemBL itemBL = new ItemBL(item.getId());
        // Set the creator entity id to the one stored in DB to prevent stealing
        // of the Item when editing as a child for example.
        item.setEntityId(itemBL.getEntity().getEntityId());
        ItemDTO dto = ItemBL.getDTO(item);
        validateAssetManagementForItem(dto, itemBL.getEntity());
        new PlanBL().validateContainingPlans(dto);

        // Set description to null
        dto.setDescription(null);
        IItemSessionBean itemSession = (IItemSessionBean) Context
                .getBean(Context.Name.ITEM_SESSION);
        itemSession.update(executorId, dto, languageId, isPlan);

        // save-delete descriptions
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() != null) {
                if (description.isDeleted()) {
                    dto.deleteDescription(description.getLanguageId());
                } else {
                    dto.setDescription(description.getContent(),
                            description.getLanguageId());
                }
            }
        }

        //saving pricingUnit descriptions
        if(ratingConfiguration!=null) {
            for (Date date : ratingConfiguration.keySet()) {
                RatingConfigurationDTO ratingConfigDTO = itemBL.getEntity().getRatingConfigurations() != null ? itemBL.getEntity().getRatingConfigurations().get(date) : null;
                List<InternationalDescriptionWS> pricingUnit = ratingConfiguration.get(date) != null ? ratingConfiguration.get(date).getPricingUnit() : null;
                if (ratingConfigDTO != null && !CollectionUtils.isEmpty(pricingUnit)) {
                    new RatingConfigurationBL(ratingConfigDTO).savePricingUnit(pricingUnit);
                }
            }
        }

    }

    /**
     * Validate an ItemDTOEx before saving
     *
     * @param item
     */
    private void validateItem(ItemDTOEx item) {

        // item may be shared - company hierarchies
        if (item.isGlobal()) {
            if (null == item.getEntityId()) {
                item.setEntityId(getCallerCompanyId());
            }
            item.setEntities(Collections.<Integer> emptyList());
        } else {
            if (CollectionUtils.isEmpty(item.getEntities())) {
                List<Integer> list = new ArrayList<Integer>(1);
                list.add(getCallerCompanyId());
                item.setEntities(list);
            }
        }

        Integer[] mandatoryItems = item
                .getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM);
        validateItemMandatoryDependenciesCycle(item.getId(),
                JArrays.toArrayList(mandatoryItems));
        // check if all descriptions are to delete
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();
        boolean noDescriptions = true;
        for (InternationalDescriptionWS description : descriptions) {
            if (!description.isDeleted() && StringUtils.isNotEmpty(description.getContent().trim())) {
                noDescriptions = false;
                break;
            }
        }
        if (noDescriptions) {
            throw new SessionInternalError(
                    "Must have a description",
                    new String[] { "ItemDTOEx,descriptions,validation.error.is.required" }, HttpStatus.SC_BAD_REQUEST);
        }

        if (item.getOrderLineMetaFields() != null) {
            for (MetaFieldWS field : item.getOrderLineMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "ItemDTOEx,orderLineMetaFields,product.validation.orderLineMetaFields.script.no.file,"
                                    + field.getName() }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        // validate dependency quantities
        ItemDependencyDTOEx[] dependencies = item.getDependencies();
        if (dependencies != null) {
            for (ItemDependencyDTOEx dependency : dependencies) {
                if (dependency.getMaximum() != null) {
                    if (dependency.getMaximum() < dependency.getMinimum()) {
                        throw new SessionInternalError(
                                "Maximum quantity must be more than minimum",
                                new String[] { "ItemDTOEx,dependencies,product.validation.dependencies.max.lessthan.min" }, HttpStatus.SC_BAD_REQUEST);
                    }
                }
            }
        }
    }

    /**
     * This method will update all orders in hierarchy provided. Deletion orders
     * from hierarchy is not possible in this method, user deleteOrder()
     * instead. If some order is not provided in hierarchy, it will not be
     * changed at all, references for this order from other orders will not be
     * changed too
     *
     * @param order
     *            order with hierarchy
     * @
     */
    @Override
    public void updateOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        updateOrder(order, orderChanges, getCallerCompanyId());
    }

    public void updateOrder(OrderWS order, OrderChangeWS[] orderChanges,
            Integer entityId)  {
        validateOrder(order, orderChanges, false);
        validateActiveSinceDate(order);

        // do some transformation from WS to DTO
        Map<OrderWS, OrderDTO> wsToDtoOrdersMap = new HashMap<OrderWS, OrderDTO>();
        Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap = new HashMap<OrderLineWS, OrderLineDTO>();
        OrderBL orderBL = new OrderBL();
        OrderDTO dto = orderBL.getDTO(order, wsToDtoOrdersMap, wsToDtoLinesMap);
        OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(dto);
        List<OrderChangeDTO> changeDtos = new LinkedList<OrderChangeDTO>();
        List<Integer> deletedChanges = new LinkedList<Integer>();
        convertOrderChangeWsToDto(orderChanges, changeDtos, deletedChanges,
                wsToDtoOrdersMap, wsToDtoLinesMap);
        try {
            orderBL.update(rootOrder, changeDtos, deletedChanges, entityId,
                    getCallerId(), getCallerLanguageId());
        } catch (SessionInternalError e) {
            logger.error("WS - updateOrder", e);
            throw e;
        } catch (Exception e) {
            logger.error("WS - updateOrder", e);
            throw new SessionInternalError("Error updating order", e);
        }
    }

    @Override
    public void updateOrders(OrderWS[] orders, OrderChangeWS[] orderChanges)
    {
        for (OrderWS order : orders) {
            List<OrderChangeWS> currentOrderChanges = new LinkedList<OrderChangeWS>();
            if (orderChanges != null) {
                LinkedHashSet<OrderWS> currentOrders = OrderHelper
                        .findAllChildren(order);
                currentOrders.add(order);
                // find order changes for current order
                for (OrderChangeWS orderChange : orderChanges) {
                    if (orderChange.getOrderWS() != null) {
                        if (currentOrders.contains(orderChange.getOrderWS())) {
                            currentOrderChanges.add(orderChange);
                        }
                    } else if (orderChange.getOrderId() != null) {
                        for (OrderWS childOrder : currentOrders) {
                            if (orderChange.getOrderId().equals(
                                    childOrder.getId())) {
                                currentOrderChanges.add(orderChange);
                            }
                        }
                    }
                }
            }
            updateOrder(order,
                    currentOrderChanges
                    .toArray(new OrderChangeWS[currentOrderChanges
                                               .size()]));
        }
    }

    @Override
    public void upgradePlanOrder(Integer orderId, Integer orderToUpgradeId, Integer paymentId) {
        if (orderId == null || orderToUpgradeId == null) {
            throw new SessionInternalError("Calling method with null parameters");
        }

        EventManager.process(new UpgradeOrderEvent(orderId, orderToUpgradeId, paymentId, getCallerCompanyId()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS getOrder(Integer orderId)  {
        // get the info from the caller
        Integer languageId = getCallerLanguageId();
        // now get the order. Avoid the proxy since this is for the client
        OrderDTO order = orderDAS.findNow(orderId);
        if (order == null) { // not found
            return null;
        }
        OrderBL bl = new OrderBL(order);
        if (order.getDeleted() == 1) {
            logger.debug("Returning deleted order {}", orderId);
        }
        return bl.getWS(languageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getOrderByPeriod(Integer userId, Integer periodId)
    {
        if (userId == null || periodId == null) {
            return null;
        }
        // now get the order
        OrderBL bl = new OrderBL();
        return bl.getByUserAndPeriod(userId, periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderLineWS getOrderLine(Integer orderLineId)
    {
        // now get the order
        OrderBL bl = new OrderBL();
        OrderLineWS retValue = null;

        retValue = bl.getOrderLineWS(orderLineId);

        return retValue;
    }

    @Override
    public void updateOrderLine(OrderLineWS line)  {
        // now get the order
        OrderBL bl = new OrderBL();
        bl.updateOrderLine(line, getCallerId());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS getLatestOrder(Integer userId)  {
        if (userId == null) {
            throw new SessionInternalError("User id can not be null");
        }
        OrderWS retValue = null;

        // now get the order
        OrderBL bl = new OrderBL();
        Integer orderId = bl.getLatest(userId);
        if (orderId != null) {
            bl.set(orderId);
            retValue = bl.getWS(getCallerLanguageId());
        }
        return retValue;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getLastOrders(Integer userId, Integer number)  {

        if (userId == null) {
            throw new SessionInternalError("Null value for userId not allowed", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != number && number < 1) {
            throw new SessionInternalError("Parameter number:" + number + " must have value greater than zero",
                    HttpStatus.SC_BAD_REQUEST);
        }
        UserBL userbl = new UserBL();

        OrderBL order = new OrderBL();
        return order.getListIds(userId, number, userbl.getEntityId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS[] getUserOrdersPage(Integer user, Integer limit, Integer offset)  {

        if (null == user) {
            throw new SessionInternalError("Null value for userId not allowed", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && limit < 1) {
            throw new SessionInternalError("Parameter limit:" + limit + " must have value greater than zero",
                    HttpStatus.SC_BAD_REQUEST);
        }
        if (null != offset && offset < 0) {
            throw new SessionInternalError("Parameter offset:" + offset + " must have non-negative value",
                    HttpStatus.SC_BAD_REQUEST);
        }

        List<OrderDTO> userOrdersPaged = new OrderBL().getListOrdersPaged(getCallerCompanyId(), user, limit, offset);

        if (null == userOrdersPaged) {
            return new OrderWS[0];
        }

        OrderWS[] ordersWs = new OrderWS[userOrdersPaged.size()];
        OrderBL bl = null;
        for (OrderDTO dto : userOrdersPaged) {
            bl = new OrderBL(dto);
            ordersWs[userOrdersPaged.indexOf(dto)] = bl.getWS(getCallerLanguageId());
        }
        return ordersWs;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getLastOrdersPage(Integer userId, Integer limit,
            Integer offset)  {
        if (userId == null || limit == null || offset == null) {
            return null;
        }
        UserBL userbl = new UserBL();

        OrderBL order = new OrderBL();
        return order.getListIds(userId, limit, offset,
                userbl.getEntityId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getOrdersByDate(Integer userId, Date since, Date until) {
        if (userId == null || since == null || until == null) {
            return null;
        }
        UserBL userbl = new UserBL();
        OrderBL order = new OrderBL();
        return order.getListIdsByDate(userId, since, until,
                userbl.getEntityId(userId));
    }

    @Override
    public String deleteOrder(Integer id)  {
        // now get the order
        OrderBL bl = new OrderBL();
        bl.setForUpdate(id);

        OrderWS orderToDelete = getOrder(id);
        OrderChangeWS[] orderChanges = getOrderChanges(id);
        if(orderToDelete.getGeneratedInvoices().length >0){
            throw new SessionInternalError("Error on delete Order ", new String[] { "order.have.invoice.cannot.be.delete,"+id});
        }
        String orderIds = bl.delete(getCallerId());
        Integer userId = orderToDelete.getUserId();
        Integer entityId = new UserBL(userId).getDto().getEntity().getId();
        EventManager.process(new AssetStatusUpdateEvent(userId, null, orderChanges, entityId));
        // Create Request to Update 911 Emergency Address
        Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.
                createEventForDeletePhoneNumberOnOrderDelete(getCallerCompanyId(), orderToDelete);
        EventManager.process(addressUpdateEvent);

        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId() + "-UserID-" +
                orderToDelete.getUserId());

        return orderIds;
    }

    /**
     * Returns the current order (order collecting current one-time charges) for
     * the period of the given date and the given user. Returns null for users
     * with no main subscription order.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderWS getCurrentOrder(Integer userId, Date date) {
        OrderWS retValue = null;
        // get the info from the caller
        Integer languageId = getCallerLanguageId();

        // now get the current order
        OrderBL bl = new OrderBL();
        if (bl.getCurrentOrder(userId, date) != null) {
            retValue = bl.getWS(languageId);
        }

        return retValue;
    }

    /**
     * Updates the uesr's current one-time order for the given date. Returns the
     * updated current order. Throws an exception for users with no main
     * subscription order.
     */
    @Override
    public OrderWS updateCurrentOrder(Integer userId, OrderLineWS[] lines,
            String pricing, Date eventDate, String eventDescription) {
        try {
            UserBL userbl = new UserBL(userId);

            // check if user has main subscription order
            if (userbl.getEntity().getCustomer().getMainSubscription() == null) {
                throw new SessionInternalError(
                        "No main subscription order for userId: " + userId);
            }

            // get currency from the user
            Integer currencyId = userbl.getCurrencyId();

            // get language from the caller
            Integer languageId = getCallerLanguageId();

            // pricing fields
            List<CallDataRecord> records = null;
            PricingField[] fieldsArray = PricingField
                    .getPricingFieldsValue(pricing);
            if (fieldsArray != null) {
                CallDataRecord record = new CallDataRecord();
                for (PricingField field : fieldsArray) {
                    record.addField(field, false); // don't care about isKey
                }
                records = new ArrayList<CallDataRecord>(1);
                records.add(record);
            }

            List<OrderLineDTO> diffLines = null;
            OrderBL bl = new OrderBL();
            if (lines != null) {
                // get the current order
                bl.set(OrderBL.getOrCreateCurrentOrder(userId, eventDate,
                        null, currencyId, true));
                List<OrderLineDTO> oldLines = OrderLineBL.copy(bl.getDTO()
                        .getLines());

                // add the line to the current order
                for (OrderLineWS line : lines) {
                    bl.addItem(line.getItemId(), line.getQuantityAsDecimal(),
                            languageId, userId, getCallerCompanyId(),
                            currencyId, records, eventDate);
                }

                // process lines to update prices and details from the source
                // items
                bl.processLines(bl.getDTO(), languageId, getCallerCompanyId(),
                        userId, currencyId, pricing);
                diffLines = OrderLineBL.diffOrderLines(oldLines, bl.getDTO()
                        .getLines());

                // generate NewQuantityEvents
                bl.checkOrderLineQuantities(oldLines, bl.getDTO().getLines(),
                        getCallerCompanyId(), bl.getDTO().getId(), true, false);

            } else if (records != null) {
                // Since there are no lines, run the mediation process
                // rules to create them.
                //TODO MODULARIZATION ISSUE 1: Check if needed, this method should be deprecated
                PluggableTaskManager<IMediationProcess> tm = new PluggableTaskManager<IMediationProcess>(
                        getCallerCompanyId(),
                        Constants.PLUGGABLE_TASK_MEDIATION_PROCESS);
                IMediationProcess processTask = tm.getNextClass();

                MediationStepResult result = new MediationStepResult();
                result.setUserId(userId);
                result.setCurrencyId(currencyId);
                result.setEventDate(eventDate);
                result.setPersist(true);
                for (CallDataRecord record : records) {
                    processTask.process(record, result);
                }
                // the mediation process might not have anything for you...
                if (result.getCurrentOrder() == null) {
                    logger.debug(
                            "Call to updateOrder did not resolve to a current order lines = {} fields= {}",
                            Arrays.toString(lines),
                            Arrays.toString(fieldsArray));
                    return null;
                }
                bl.set((OrderDTO) result.getCurrentOrder());
                //TODO MODULARIZATION: Check if needed, this method should be deprecated
            } else {
                throw new SessionInternalError("Both the order lines and "
                        + "pricing fields were null. At least one of either "
                        + "must be provided.");
            }

            // save the event
            // assign to record DONE and BILLABLE status
            //TODO MODULARIZATION: Check if needed, this method should be deprecated
            //          IMediationSessionBean mediation = (IMediationSessionBean) Context
            //                  .getBean(Context.Name.MEDIATION_SESSION);
            //          mediation.saveEventRecordLines(new ArrayList<OrderLineDTO>(
            //                  diffLines), getCallerCompanyId(), userbl.getEntity()
            //                  .getId(), null,
            //                  Constants.MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE, String
            //                          .valueOf(eventDate.getTime()), eventDate,
            //                  eventDescription, null, null);
            //TODO MODULARIZATION: Check if needed, this method should be deprecated
            // return the updated order
            return bl.getWS(languageId);

        } catch (Exception e) {
            logger.error("WS - getCurrentOrder", e);
            throw new SessionInternalError("Error updating current order", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS[] getUserSubscriptions(Integer userId)
    {
        if (userId == null) {
            throw new SessionInternalError("User Id cannot be null.");
        }

        List<OrderDTO> subscriptions = orderDAS
                .findByUserSubscriptions(userId);
        if (null == subscriptions) {
            return new OrderWS[0];
        }
        OrderWS[] orderArr = new OrderWS[subscriptions.size()];
        OrderBL bl = null;
        for (OrderDTO dto : subscriptions) {
            bl = new OrderBL(dto);
            orderArr[subscriptions.indexOf(dto)] = bl
                    .getWS(getCallerLanguageId());
        }
        return orderArr;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS[] getOrderMetaFieldMap(OrderWS[] orderWS) {
        OrderBL bl = new OrderBL();
        return bl.getOrderMetaFieldMap(orderWS);
    }

    @Override
    public boolean updateOrderPeriods(OrderPeriodWS[] orderPeriods) {
        OrderPeriodDAS periodDas = new OrderPeriodDAS();
        OrderPeriodDTO periodDto = null;
        for (OrderPeriodWS periodWS : orderPeriods) {
            if (null != periodWS.getId()) {
                periodDto = periodDas.find(periodWS.getId());
            }
            if (null == periodDto) {
                periodDto = new OrderPeriodDTO();
                periodDto.setCompany(new CompanyDAS()
                .find(getCallerCompanyId()));
            }
            periodDto.setValue(periodWS.getValue());
            if (null != periodWS.getPeriodUnitId()) {
                periodDto.setUnitId(periodWS.getPeriodUnitId().intValue());
            }
            periodDto = periodDas.save(periodDto);
            if (CollectionUtils.isNotEmpty(periodWS.getDescriptions())) {
                periodDto.setDescription(periodWS
                        .getDescriptions().get(0).getContent(),
                        periodWS
                        .getDescriptions().get(0).getLanguageId());
            }
            logger.debug("Converted to DTO: {}", periodDto);
            periodDas.flush();
            periodDas.clear();
            periodDto = null;
        }
        return true;
    }

    @Override
    public boolean updateOrCreateOrderPeriod(OrderPeriodWS orderPeriod)
    {

        Integer entityId = getCallerCompanyId();

        /*
         * TODO - Instead of below, We should use Hibernate Validator @Size on
         * 'content' field of InternationalDescriptionWS.java
         */
        if (orderPeriod.getDescriptions() != null
                && orderPeriod.getDescriptions().size() > 0) {
            int descriptionLength = orderPeriod.getDescriptions().get(0)
                    .getContent().length();
            if (descriptionLength < 1 || descriptionLength > 4000) {
                throw new SessionInternalError(
                        "Description should be between 1 and 4000 characters long", HttpStatus.SC_BAD_REQUEST);
            }
        }

        OrderPeriodDAS periodDas = new OrderPeriodDAS();
        OrderPeriodDTO periodDto = null;
        if (null != orderPeriod.getId()) {
            periodDto = periodDas.find(orderPeriod.getId());
        }

        if (null == periodDto) {
            periodDto = new OrderPeriodDTO();
            periodDto.setCompany(new CompanyDAS().find(entityId));
            // periodDto.setVersionNum(new Integer(0));
        }
        periodDto.setValue(orderPeriod.getValue());
        if (null != orderPeriod.getPeriodUnitId()) {
            periodDto.setUnitId(orderPeriod.getPeriodUnitId().intValue());
        }
        periodDto = periodDas.save(periodDto);
        if (orderPeriod.getDescriptions() != null
                && orderPeriod.getDescriptions().size() > 0) {
            periodDto.setDescription(orderPeriod
                    .getDescriptions().get(0).getContent(),
                    orderPeriod.getDescriptions()
                    .get(0).getLanguageId());
        }
        logger.debug("Converted to DTO: {}", periodDto);
        periodDas.flush();
        periodDas.clear();
        return true;
    }

    @Override
    public boolean deleteOrderPeriod(Integer periodId)
    {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            return bl.deletePeriod(periodId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /*
     * Account Type
     */
    @Override
    public boolean updateAccountType(AccountTypeWS accountType)
    {
        Integer entityId = getCallerCompanyId();
        PaymentInformationDAS paymentInformationDAS = new PaymentInformationDAS();
        AccountTypeWS existing = AccountTypeBL.getWS(
                new AccountTypeDAS().find(accountType.getId()));
        Integer[] existingPaymentMethodTypeIds = existing
                .getPaymentMethodTypeIds();
        existingPaymentMethodTypeIds = (null == existingPaymentMethodTypeIds) ? new Integer[0]
                : existingPaymentMethodTypeIds;
        Integer[] newPaymentMethodTypeIds = accountType
                .getPaymentMethodTypeIds();
        newPaymentMethodTypeIds = (null == newPaymentMethodTypeIds) ? new Integer[0]
                : newPaymentMethodTypeIds;
        List<Integer> removedPaymentMethodType = (List<Integer>) CollectionUtils
                .subtract(Arrays.asList(existingPaymentMethodTypeIds),
                        Arrays.asList(newPaymentMethodTypeIds));
        for (Integer paymentMethodType : removedPaymentMethodType) {
            long l = paymentInformationDAS
                    .findByAccountTypeAndPaymentMethodType(accountType.getId(),
                            paymentMethodType);
            if (l > 0) {
                throw new SessionInternalError(
                        "",
                        new String[] { "AccountTypeWS,paymentMethod,validation.error.payment.inUse" }, HttpStatus.SC_BAD_REQUEST);
            }
        }

        AccountTypeDTO accountTypeDTO = AccountTypeBL.getDTO(accountType,entityId);
        logger.debug("Payments: " + accountTypeDTO.getPaymentMethodTypes());
        new AccountTypeBL().update(accountTypeDTO);

        if (accountType.getDescriptions() != null
                && accountType.getDescriptions().size() > 0) {

            for (InternationalDescriptionWS desc : accountType
                    .getDescriptions()) {
                // verify if description is non empty and unique
                if (desc.getContent().trim().isEmpty()) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.blank.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
                } else if (!new AccountTypeBL().isAccountTypeUnique(entityId,
                        desc.getContent(), false)) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.unique.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
                }
                accountTypeDTO.setDescription(desc.getContent(),
                        desc.getLanguageId());
            }
            BigDecimal creditLimit = accountType.getCreditLimitAsDecimal();
            BigDecimal notification1 = accountType
                    .getCreditNotificationLimit1AsDecimal();
            if (creditLimit != null && notification1 != null
                    && !(creditLimit.compareTo(notification1) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit1,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.",errmsgs, HttpStatus.SC_BAD_REQUEST);
            }
            BigDecimal notification2 = accountType
                    .getCreditNotificationLimit2AsDecimal();
            if (creditLimit != null && notification2 != null
                    && !(creditLimit.compareTo(notification2) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit2,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
            }
        }
        return true;
    }

    @Override
    public Integer createOrder(OrderWS order, OrderChangeWS[] orderChanges)
    {
        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId() + "-UserID-" +
                order.getUserId());

        setOrderOnOrderChanges(order, orderChanges);
        validateLines(order);
        /*
         * #7899 - The order being created is evaluated for subscription lines,
         * if order is containing any subscription products then internal
         * account and order are created for each subscription line
         */
        List<OrderChangeWS> changes = JArrays.toArrayList(orderChanges);
        createSubscriptionAccountAndOrder(order.getUserId(), order, false,
                changes);
        orderChanges = changes != null ? changes
                .toArray(new OrderChangeWS[changes.size()]) : null;

                // If order only contained subscription lines then now order do not have
                // any lines left, no need to create order with no lines
                if ((order.getOrderLines() != null && order.getOrderLines().length > 0)
                        || (null != orderChanges && orderChanges.length > 0)) {
                    OrderWS ows = doCreateOrder(order, orderChanges, true);
                    Integer orderId = ows != null ? ows.getId() : null;
                    return orderId;
                }
                return null;
    }

    @Override
    public Integer createAccountType(AccountTypeWS accountType)
    {
        Integer entityId = getCallerCompanyId();
        AccountTypeDTO accountTypeDTO = AccountTypeBL.getDTO(accountType, entityId);
        accountTypeDTO = new AccountTypeBL().create(accountTypeDTO);

        if (accountType.getDescriptions() != null
                && accountType.getDescriptions().size() > 0) {

            for (InternationalDescriptionWS desc : accountType
                    .getDescriptions()) {
                if (desc.getContent().trim().isEmpty()) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.blank.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
                } else if (!new AccountTypeBL().isAccountTypeUnique(entityId,
                        desc.getContent(), true)) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.unique.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);

                }
                accountTypeDTO.setDescription(desc.getContent(),
                        desc.getLanguageId());
            }
            BigDecimal creditLimit = accountType.getCreditLimitAsDecimal();
            BigDecimal notification1 = accountType
                    .getCreditNotificationLimit1AsDecimal();
            if (creditLimit != null && notification1 != null
                    && !(creditLimit.compareTo(notification1) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit1,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
            }
            BigDecimal notification2 = accountType
                    .getCreditNotificationLimit2AsDecimal();
            if (creditLimit != null && notification2 != null
                    && !(creditLimit.compareTo(notification2) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit2,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.", errmsgs, HttpStatus.SC_BAD_REQUEST);
            }
        }

        return accountTypeDTO.getId();
    }

    @Override
    public boolean deleteAccountType(Integer accountTypeId)
    {
        try {
            AccountTypeBL bl = new AccountTypeBL(accountTypeId);
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AccountTypeWS getAccountType(Integer accountTypeId)
    {

        AccountTypeDAS das = new AccountTypeDAS();
        AccountTypeDTO accountTypeDTO = das.findNow(accountTypeId);
        if (accountTypeId == null || accountTypeDTO == null) { // not found
            return null;
        }
        AccountTypeBL bl = new AccountTypeBL(accountTypeId);
        return bl.getWS(accountTypeDTO.getLanguageId());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountTypeWS[] getAllAccountTypes()  {
        return getAllAccountTypesByCompanyId(getCallerCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountTypeWS[] getAllAccountTypesByCompanyId(Integer companyId) {
        AccountTypeDAS das = new AccountTypeDAS();
        List<AccountTypeDTO> types = das.findAll(companyId);
        AccountTypeWS[] wsTypes = new AccountTypeWS[types.size()];
        for (int i = 0; i < types.size(); i++) {
            wsTypes[i] = AccountTypeBL.getWS(types.get(i));
        }
        return wsTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyInformationTypeWS[] getInformationTypesForCompany(Integer companyId) {

        List<CompanyInformationTypeDTO> companyInformationTypes = new CompanyInformationTypeBL()
        .getCompanyInformationTypes(companyId);

        if (companyInformationTypes == null) {
            return new CompanyInformationTypeWS[0];
        }

        List<CompanyInformationTypeWS> informationTypesWS = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(companyInformationTypes)) {
            for (CompanyInformationTypeDTO cit : companyInformationTypes) {
                CompanyInformationTypeWS companyInformationTypeWS =CompanyInformationTypeBL.getWS(cit);
                informationTypesWS.add(companyInformationTypeWS);
            }
        }
        return informationTypesWS
                .toArray(new CompanyInformationTypeWS[informationTypesWS.size()]);
    }

    @Override
    public Integer createCompanyInformationType(
            CompanyInformationTypeWS companyInformationType) {
        if (companyInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : companyInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "CompanyInformationTypeWS,metaFields,metafield.validation.filename.required" });
                }
            }
        }
        CompanyInformationTypeDTO dto = CompanyInformationTypeBL.getDTO(companyInformationType,getCallerCompanyId());
        Map<Integer, List<Integer>> dependencyMetaFieldMap = CompanyInformationTypeBL.getMetaFieldDependency(companyInformationType);
        dto = new CompanyInformationTypeBL().create(dto, dependencyMetaFieldMap);

        return dto.getId();
    }

    @Override
    public Integer createCompanyInformationTypeWithEntityId(
            CompanyInformationTypeWS companyInformationType, Integer entityId) {
        if (companyInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : companyInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "CompanyInformationTypeWS,metaFields,metafield.validation.filename.required" });
                }
            }
        }
        CompanyInformationTypeDTO dto = CompanyInformationTypeBL.getDTO(companyInformationType,entityId);
        Map<Integer, List<Integer>> dependencyMetaFieldMap = CompanyInformationTypeBL.getMetaFieldDependency(companyInformationType);
        dto = new CompanyInformationTypeBL().create(dto, dependencyMetaFieldMap);

        return dto.getId();
    }

    @Override
    public void updateCompanyInformationType(
            CompanyInformationTypeWS companyInformationType) {

        if (companyInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : companyInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "CompanyInformationTypeWS,metaFields,metafield.validation.filename.required" });
                }
            }
        }

        CompanyInformationTypeDTO dto = CompanyInformationTypeBL.getDTO(companyInformationType, getCallerCompanyId());
        Map<Integer, List<Integer>> dependencyMetaFieldMap = CompanyInformationTypeBL.getMetaFieldDependency(companyInformationType);
        new CompanyInformationTypeBL().update(dto, dependencyMetaFieldMap);
    }

    @Override
    public boolean deleteCompanyInformationType(Integer companyInformationTypeId) {
        try {
            CompanyInformationTypeBL bl = new CompanyInformationTypeBL(
                    companyInformationTypeId);
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyInformationTypeWS getCompanyInformationType(
            Integer companyInformationTypeId) {

        CompanyInformationTypeDAS das = new CompanyInformationTypeDAS();
        CompanyInformationTypeDTO companyInformationType = das
                .findNow(companyInformationTypeId);
        if (companyInformationTypeId == null || companyInformationType == null) {
            return null;
        }
        CompanyInformationTypeBL bl = new CompanyInformationTypeBL(
                companyInformationTypeId);
        return bl.getWS();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInformationTypeWS[] getInformationTypesForAccountType(
            Integer accountTypeId) {

        List<AccountInformationTypeDTO> accountInformationTypes = new AccountInformationTypeBL()
        .getAccountInformationTypes(accountTypeId);

        if (accountInformationTypes == null) {
            return new AccountInformationTypeWS[0];
        }

        List<AccountInformationTypeWS> informationTypesWS = new ArrayList<AccountInformationTypeWS>(
                accountInformationTypes.size());

        if (accountInformationTypes.size() > 0) {
            for (AccountInformationTypeDTO ait : accountInformationTypes) {
                AccountInformationTypeWS accountInformationTypeWS =AccountInformationTypeBL.getWS(ait);
                informationTypesWS.add(accountInformationTypeWS);
            }
        }
        return informationTypesWS
                .toArray(new AccountInformationTypeWS[informationTypesWS.size()]);
    }

    @Override
    public Integer createAccountInformationType(
            AccountInformationTypeWS accountInformationType) {
        if (accountInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : accountInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "AccountInformationTypeWS,metaFields,metafield.validation.filename.required" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }
        AccountInformationTypeDTO dto = AccountInformationTypeBL.getDTO(accountInformationType, getCallerCompanyId());
        Map<Integer, List<Integer>> dependencyMetaFieldMap = AccountInformationTypeBL.getMetaFieldDependency(accountInformationType);
        dto = new AccountInformationTypeBL().create(dto, dependencyMetaFieldMap);

        return dto.getId();
    }

    @Override
    public void updateAccountInformationType(
            AccountInformationTypeWS accountInformationType) {

        if (accountInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : accountInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "AccountInformationTypeWS,metaFields,metafield.validation.filename.required" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        AccountInformationTypeDTO dto = AccountInformationTypeBL.getDTO(accountInformationType,getCallerCompanyId());
        Map<Integer, List<Integer>> dependencyMetaFieldMap = AccountInformationTypeBL.getMetaFieldDependency(accountInformationType);
        new AccountInformationTypeBL().update(dto, dependencyMetaFieldMap);
    }

    /**
     * Creates the given Order in jBilling, generates an Invoice for the same.
     * Returns the generated Invoice ID
     */
    @Override
    public Integer createOrderAndInvoice(OrderWS order, OrderChangeWS[] orderChanges)
    {
        validateLines(order, orderChanges);
        Integer orderId = doCreateOrder(order, orderChanges, true).getId();
        InvoiceDTO invoice = doCreateInvoice(orderId);
        return invoice == null ? null : invoice.getId();
    }

    @Override
    public boolean deleteAccountInformationType(Integer accountInformationTypeId) {
        try {
            AccountInformationTypeBL bl = new AccountInformationTypeBL(
                    accountInformationTypeId);
            if (bl.getAccountInformationType().isUseForNotifications()) {
                throw new SessionInternalError(
                        "Account information type is being used for notifications",
                        new String[] { "config.account.information.type.delete.failure" }, HttpStatus.SC_CONFLICT);
            }
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInformationTypeWS getAccountInformationType(
            Integer accountInformationTypeId) {

        AccountInformationTypeDAS das = new AccountInformationTypeDAS();
        AccountInformationTypeDTO accountInformationType = das
                .findNow(accountInformationTypeId);
        if (accountInformationTypeId == null || accountInformationType == null) {
            return null;
        }
        AccountInformationTypeBL bl = new AccountInformationTypeBL(
                accountInformationTypeId);
        return bl.getWS();
    }

    /*
     * PAYMENT
     */

    @Override
    public Integer createPayment(PaymentWS payment) {
        return applyPayment(payment, null);
    }

    @Override
    public Integer[] createPayments(PaymentWS[] payments) {
        Integer[] paymentIds = new Integer[payments.length];

        for (int i = 0; i < payments.length; i++) {
            paymentIds[i] = applyPayment(payments[i], null);
        }

        return paymentIds;
    }

    @Override
    public void updatePayment(PaymentWS payment) {

        if(payment != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(payment.getPaymentInstruments());
        }

        Integer entityId = getCallerCompanyId();
        if (payment == null) {
            String msg = "Can not update null payment!!";
            String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_UPDATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Can not update null payment!!", HttpStatus.SC_BAD_REQUEST);
        }
        Integer userId = payment.getOwningUserId();
        UserDTO user = userDAS.findNow(userId);
        if (null == user) {
            String msg = String.format("No owning user for payment id: %d", payment.getId());
            String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_UPDATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("There is not user for the supplied payment.", HttpStatus.SC_BAD_REQUEST);
        }
        Integer userCompanyId = user.getEntity().getId();
        if (!userCompanyId.equals(entityId)) {
            String msg = String.format("Payment owing entity id: %d not equals with invoking entity id: %d",userCompanyId, entityId);
            String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_UPDATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Updating another entity's payments not supported!!", HttpStatus.SC_BAD_REQUEST);
        }

        PaymentDTOEx dto = new PaymentDTOEx(payment);
        PaymentBL paymentBL = new PaymentBL(payment.getId());
        if (null == paymentBL.getEntity()){
            throw new SessionInternalError(String.format("No payment found for %d id!", payment.getId()), HttpStatus.SC_NOT_FOUND);
        }

        if(dto.getPaymentResult() == null){
            dto.setPaymentResult(paymentBL.getEntity().getPaymentResult());
        }

        // check if payment has been refunded
        if (paymentBL.ifRefunded()) {
            String msg = "This payment has been refunded and hence cannot be updated.";
            String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_UPDATE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError(msg,new String[] { "validation.error.update.refunded.payment" }, HttpStatus.SC_BAD_REQUEST);
        }
        paymentBL.update(getCallerId(), dto);
    }

    @Override
    public void deletePayment(Integer paymentId)  {

        PaymentBL paymentBL = new PaymentBL(paymentId);
        if (null == paymentBL.getEntity()){
            throw new SessionInternalError(String.format("No payment found for %d id!", paymentId), HttpStatus.SC_NOT_FOUND);
        }
        // check if the payment is a refund & not Entered status, if it is do not allow it
        if (paymentBL.getEntity().getIsRefund() == 1 &&
                paymentBL.getEntity().getResultId() != null &&
                paymentBL.getEntity().getResultId().intValue() != 4) {
            String msg = String.format("This payment %s is a refund so we cannot delete it.",paymentId);
            String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_DELETE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("A Refund cannot be deleted",new String[] { "validation.error.delete.refund.payment" }, HttpStatus.SC_CONFLICT);
        }

        // check if payment has been refunded
        if (paymentBL.ifRefunded()) {
            String msg = "This payment has been refunded and hence cannot be deleted.";
            String logMsg = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_DELETE,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(logMsg);
            throw new SessionInternalError(msg,new String[] { "validation.error.delete.refunded.payment" }, HttpStatus.SC_CONFLICT);
        }

        paymentBL.delete();
    }

    /**
     * Enters a payment and applies it to the given invoice. This method DOES
     * NOT process the payment but only creates it as 'Entered'. The entered
     * payment will later be processed by the billing process.
     *
     * Invoice ID is optional. If no invoice ID is given the payment will be
     * applied to the payment user's account according to the configured entity
     * preferences.
     *
     * @param payment
     *            payment to apply
     * @param invoiceId
     *            invoice id
     * @return created payment id
     * @
     */
    @Override
    public Integer applyPayment(PaymentWS payment, Integer invoiceId)  {
        // payment.setIsRefund(0);

        if(payment != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(payment.getPaymentInstruments());
        }

        String msg;
        String message;
        Integer entityId = getCallerCompanyId();
        // Guard against npe
        if (payment == null) {
            msg = "Supplied Payment is null.";
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_APPLY,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Can not apply null payment!!", HttpStatus.SC_BAD_REQUEST);
        }
        // Check if the payment owing user is from the same entity as the caller user.
        Integer userId = payment.getOwningUserId();
        UserDTO user = userDAS.find(userId);
        if (null == user) {
            msg = String.format("No owning user for payment id: %d", payment.getId());
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_APPLY,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("There is not user for the supplied payment.", HttpStatus.SC_BAD_REQUEST);
        }
        Integer userCompanyId = user.getEntity().getId();
        if (!entityId.equals(userCompanyId)) {
            msg = String.format("Payment owing user entity id: %d not equals with invoking user entity id: %d", userCompanyId, entityId);
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_APPLY, LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Can not apply payments from another entity!!", HttpStatus.SC_BAD_REQUEST);
        }

        // Check if the invoice for the invoice id has the same entity id as the
        // caller entity id.
        if (invoiceId != null) {
            InvoiceDTO invoice = findInvoice(invoiceId);
            if (null == invoice) {
                msg = String.format("No invoice found invoice id: %d", invoiceId);
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_APPLY, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError("Invoice not found!!", HttpStatus.SC_NOT_FOUND);
            }
            if (!entityId.equals(invoice.getBaseUser().getEntity().getId())) {
                msg = String.format("Invoice entity id: %d not equals with invoking user entity id: %d",userCompanyId, entityId);
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_APPLY, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError("Applying invoices from another entity not supported!!", HttpStatus.SC_BAD_REQUEST);
            }
        }

        // apply validations for refund payments
        if (payment.getIsRefund() == 1) {
            // check for validations
            if (!PaymentBL.validateRefund(payment)) {
                throw new SessionInternalError(
                        "Either refund payment was not linked to any payment or the refund amount is in-correct.",
                        new String[] { "PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount" }, HttpStatus.SC_BAD_REQUEST);
            }
        }

        // can not check payment id, more than 0 zero instruments should be
        // checked
        if (CollectionUtils.isEmpty(payment.getPaymentInstruments())) {
            msg = "Cannot apply a payment without a payment method.";
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_APPLY, LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError(msg,new String[] { "PaymentWS,paymentMethodId,validation.error.apply.without.method" }, HttpStatus.SC_BAD_REQUEST);
        }

        Set<Integer> paymentInformationSet = new HashSet<>();
        for(PaymentInformationWS paymentInformationWS : payment.getPaymentInstruments()) {
            if(paymentInformationWS.getId() != null) {
                paymentInformationSet.add(paymentInformationWS.getId());
            }
        }

        IPaymentSessionBean session = Context.getBean(Context.Name.PAYMENT_SESSION);
        PaymentDTOEx pmtDtoEx = new PaymentDTOEx(payment);
        Integer pmtId = session.applyPayment(pmtDtoEx, invoiceId,getCallerId());

        PaymentBL paymentBL = new PaymentBL(pmtId);
        payment = PaymentBL.getWS(paymentBL.getDTOEx(getCallerLanguageId()));
        //fire events for new payment instruments
        for(PaymentInstrumentInfoDTO instrumentInfoDTO : paymentBL.getEntity().getPaymentInstrumentsInfo()) {
            PaymentInformationDTO paymentInformationDTO = instrumentInfoDTO.getPaymentInformation();
            if(paymentInformationSet.contains(paymentInformationDTO.getId())) {
                continue;
            }

            String templateName = paymentInformationDTO.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName();
            if("ACH".equals(templateName)) {
                EventManager.process(new AchUpdateEvent(paymentInformationDTO, user.getEntity().getId()));
            }else if("Payment Card".equals(templateName)){
                Map<String, String> paymentMetaFieldMap = getPaymentInstrumentMetaFields(paymentInformationDTO);
                String creditCardNumber = paymentMetaFieldMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
                String gateWayKey = paymentMetaFieldMap.get(MetaFieldType.GATEWAY_KEY.name());
                Integer creditCardType = null != creditCardNumber ? Util.getPaymentMethod(creditCardNumber.toCharArray()) : null;
                if((null!= gateWayKey && !gateWayKey.isEmpty()) ||
                        (null!=creditCardType && creditCardType!=Constants.PAYMENT_METHOD_GATEWAY_KEY)) {
                    EventManager.process(new NewCreditCardEvent(paymentInformationDTO, user.getEntity().getId(), userId));
                }
            }else if(Constants.CUSTOM.equals(templateName)) {

                // Raising Event to Handle Custom Manual Payment
                if(null != payment.getPaymentNotes() && !payment.getPaymentNotes().contains(IgnitionConstants.IGNITION_SCHEDULED_PAYMENT_NOTE)){
                    payment.setId(pmtId);
                    if (invoiceId != null) {
                        payment.setInvoiceIds(new Integer[]{invoiceId});
                    }
                    List<OrderDTO> orders = orderDAS.findByUserSubscriptions(userId);
                    if(CollectionUtils.isNotEmpty(orders)) {
                        EventManager.process(new CustomPaymentEvent(payment, entityId, orders.get(0).getId()));
                    }
                }
            }
        }
        return pmtId;
    }

    private Map<String, String> getPaymentInstrumentMetaFields(PaymentInformationDTO creditCard) {
        Map<String, String> creditCardFieldMap = new HashMap<>();
        creditCard.getMetaFields().forEach(metaFieldValue -> {
            MetaFieldType type = metaFieldValue.getField().getFieldUsage();
            Object value = metaFieldValue.getValue();
            if(null!=type && null!=value) {
                if(metaFieldValue.getField().getDataType().equals(DataType.CHAR)) {
                    creditCardFieldMap.put(type.name(), new String((char[])value));
                } else {
                    creditCardFieldMap.put(type.name(), value.toString());
                }
            }
        });
        return creditCardFieldMap;
    }

    private String getEnhancedLogMessage(String msg, LogConstants module,LogConstants action,LogConstants status){
        return new LogMessage.Builder().module(module.toString()).action(action.toString())
                .message(msg).status(status.toString()).build().toString();
    }

    private void validateCvv(String cvv) {
        if (StringUtils.isEmpty(cvv)){
            throw new SessionInternalError("validation failed",
                    new String [] {"CVV should not be blank for one-time payment"},HttpStatus.SC_CONFLICT);
        }
        if (cvv.length() > 4 || cvv.length() < 3 ){
            throw new SessionInternalError("validation failed",
                    new String [] {"CVV should not be greater than 4 digits and less than 3 digits"},HttpStatus.SC_CONFLICT);
        }
        if (!cvv.matches("[0-9]+")) {
            throw new SessionInternalError("validation failed",
                    new String [] {"CVV should be numeric value only"},HttpStatus.SC_CONFLICT);
        }
    }

    /**
     * Processes a payment and applies it to the given invoice. This method will
     * actively processes the payment using the configured payment plug-in.
     *
     * Payment is optional when an invoice ID is provided. If no payment is
     * given, the payment will be processed using the invoiced user's configured
     * "automatic payment" instrument.
     *
     * Invoice ID is optional. If no invoice ID is given the payment will be
     * applied to the payment user's account according to the configured entity
     * preferences.
     *
     * @param payment
     *            payment to process
     * @param invoiceId
     *            invoice id
     * @return payment authorization from the payment processor
     */
    @Override
    // this method does not start a transaction since transaction
    // during payment processing is managed manually
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentAuthorizationDTOEx processPayment(PaymentWS payment,Integer invoiceId) {

        if(payment != null){
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(payment.getPaymentInstruments());
            validateOneTimePayment(payment);
        }
        String msg = "In process payment";
        String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_EVENT,LogConstants.STATUS_SUCCESS);
        logger.debug(message);
        Integer entityId = getCallerCompanyId();
        if (payment == null && invoiceId != null) {
            msg = String.format("Payment is null, requesting Payment for Invoice ID: %s",invoiceId);
            message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_PROCESS,LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            PaymentAuthorizationDTOEx auth = payInvoice(invoiceId);

            if(auth!=null){
            	SecurePaymentWS securePaymentWS = new SecurePaymentWS(0,auth.getPaymentId(),false, null, (auth.getResult()? "succeeded" : "failed"), null );
            	auth.setSecurePaymentWS(securePaymentWS);
            }
            return auth;
        }
        // Guard against npe
        if (payment == null) {
            msg = String.format("Supplied Payment is null.");
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Payment processing parameters not found!", HttpStatus.SC_BAD_REQUEST);
        }

        Integer userId = payment.getOwningUserId();
        UserDTO user = userDAS.find(userId);
        if (null == user) {
            msg = String.format("Supplied Payment is null.");
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("There is no user for the supplied payment.", HttpStatus.SC_BAD_REQUEST);
        }

        Integer userCompanyId = user.getEntity().getId();
        if (!userCompanyId.equals(entityId)) {
            msg = String.format("Payment owing entity id: %d not equals with invoking entity id: %d", userCompanyId, entityId);
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
            logger.error(message);
            throw new SessionInternalError("Processing another entity's payments not supported!!", HttpStatus.SC_BAD_REQUEST);
        }
        // apply validations for refund payment
        if (payment.getIsRefund() == 1) {
            if (!PaymentBL.validateRefund(payment)) {
                msg = "Either refund payment was not linked to any payment or the refund amount is in-correct";
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError(msg,new String[]
                        { "PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount" }, HttpStatus.SC_BAD_REQUEST);
            }
            /*
             * In case of refund against one time payment, payment information of original payment should be used to refund the payment
            */
            if (payment.getPaymentInstruments().size() < 1){
            	if(!StripeHelper.isObjectEmpty(payment.getPaymentId())){
            		PaymentDTO originalPayment = new PaymentBL(payment.getPaymentId()).getEntity();

            		if(!StripeHelper.isObjectEmpty(originalPayment)){
            			List<PaymentInstrumentInfoDTO> paymentInstrumentInfoDtos = originalPayment.getPaymentInstrumentsInfo(); // it returns multiple if more than one payment instrument is used to process the payment

            			if(!StripeHelper.isObjectEmpty(paymentInstrumentInfoDtos) && paymentInstrumentInfoDtos.size()>0){
            				PaymentInstrumentInfoDTO dto = paymentInstrumentInfoDtos
    								.stream().filter(instrument -> instrument.getResult().getId() == CommonConstants.RESULT_OK.intValue()).findFirst().orElse(null);
                			if(!StripeHelper.isObjectEmpty(dto)){
                				payment.setPaymentInstruments(new ArrayList<PaymentInformationWS>(Arrays.asList(PaymentInformationBL.getWS(dto.getPaymentInformation()))));
                			}
            			}
            		}
            	}else{
            		logger.warn("Refund, could not fetch payment instrument that was used with orignal payment by provided reference payment id, " + payment.getPaymentId());
            	}
            }
        }

        PaymentDTOEx dto = new PaymentDTOEx(payment);
        PaymentAuthorizationDTOEx auth = null;
        SecurePaymentWS securePaymentWS = null;

        //Strong customer Authentication (SCA) - authenticating with 3D Secure
        if((payment.getIsRefund()== null || payment.getIsRefund()== 0) && (payment.getPaymentId() == null ||  payment.getPaymentId() == 0  )){ // refund should not be verified against 3DS/SCA
        	try {
            	securePaymentWS = perform3DSecurityCheck( user, null, dto);
            } catch (PluggableTaskException e) {
                msg = "Exception occurred fetching payment info plug-in.";
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError(msg,new String[] { "PaymentWS,baseUserId,validation.error.no.payment.instrument" }, HttpStatus.SC_BAD_REQUEST);
            }
            if(securePaymentWS != null && !securePaymentWS.isSucceeded()){
            	securePaymentWS.setUserId(userId);
            	auth = new PaymentAuthorizationDTOEx();
                auth.setPaymentId(dto.getId());
                auth.setResult(false);
                auth.setSecurePaymentWS(securePaymentWS);

                return auth;
            }
        }
        // payment without Credit Card or ACH, fetch the users primary payment
        // instrument for use
        if (payment.getPaymentInstruments().size() < 1 && dto.getPaymentInstruments().size() < 1) {
            msg = "processPayment() called without payment method, fetching users automatic payment instrument.";
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_SUCCESS);
            logger.info(message);
            PaymentDTOEx instrument;
            try {
                instrument = PaymentBL.findPaymentInstrument(entityId,payment.getUserId());

            } catch (PluggableTaskException e) {
                msg = "Exception occurred fetching payment info plug-in.";
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError(msg,new String[] { "PaymentWS,baseUserId,validation.error.no.payment.instrument" }, HttpStatus.SC_BAD_REQUEST);

            } catch (TaskException e) {
                msg = "Exception occurred with plug-in when fetching payment instrument.";
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError(msg,new String[] { "PaymentWS,baseUserId,validation.error.no.payment.instrument" }, HttpStatus.SC_BAD_REQUEST);
            }

            if (instrument == null
                    || instrument.getPaymentInstruments() == null || instrument.getPaymentInstruments().size() < 1) {
                msg = "User " + payment.getUserId() + "does not have a default payment instrument.";
                message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(message);
                throw new SessionInternalError(msg,new String[] { "PaymentWS,baseUserId,validation.error.no.payment.instrument" }, HttpStatus.SC_BAD_REQUEST);
            }

            dto.setPaymentInstruments(instrument.getPaymentInstruments());
        }

        // TODO: multiple payment instruments method can not be set
        // populate payment method based on the payment instrument
        // logger.debug("Payment method before: {}", payment.getMethodId());
        // if (null == dto.getPaymentMethod()) {
        // if (dto.getCreditCard() != null) {
        // dto.setPaymentMethod(new
        // PaymentMethodDTO(dto.getCreditCard().getCcType()));

        // } else if (dto.getAch() != null) {
        // dto.setPaymentMethod(new
        // PaymentMethodDTO(Constants.PAYMENT_METHOD_ACH));
        // }
        // }
        // logger.debug("Payment method after {}",
        // dto.getPaymentMethod().getDescription());

        // process payment
        IPaymentSessionBean session = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        Integer result = session.processAndUpdateInvoice(dto, invoiceId, entityId, getCallerId());
        msg = String.format("paymentBean.processAndUpdateInvoice() Id= %s", result);
        message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_SUCCESS);
        logger.debug(message);


        if (dto != null && dto.getAuthorization() != null) {
            msg = String.format("PaymentAuthorizationDTO Id = %s", dto.getAuthorization().getId());
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_SUCCESS);
            logger.debug(message);
            auth = new PaymentAuthorizationDTOEx(dto.getAuthorization().getOldDTO());
            msg = String.format("PaymentAuthorizationDTOEx Id =%s", auth.getId());
            message = getEnhancedLogMessage(msg, LogConstants.MODULE_PAYMENT, LogConstants.ACTION_PROCESS, LogConstants.STATUS_SUCCESS);
            logger.debug(message);
            auth.setResult(result.equals(Constants.RESULT_OK));
            //Strong customer Authentication (SCA) - authenticating with 3D Secure
            if(securePaymentWS != null && securePaymentWS.isSucceeded()){
            	securePaymentWS.setBillingHubRefId(dto.getAuthorization().getPayment().getId());
            	auth.setSecurePaymentWS(securePaymentWS);
            }else if(securePaymentWS == null){
            	auth.setSecurePaymentWS(new SecurePaymentWS(userId, dto.getId(), false, null, "succeeded", null));
            }
        } else {
            auth = new PaymentAuthorizationDTOEx();
            auth.setPaymentId(dto.getId());
            auth.setResult(result.equals(Constants.RESULT_FAIL));
            //Strong customer Authentication (SCA) - authenticating with 3D Secure
            auth.setSecurePaymentWS(new SecurePaymentWS(userId, dto.getId(), false, null, (dto.isAuthenticationRequired()? "requires_action" : "failed"), new ErrorWS(result.toString(), "Unable to process payment")));
        }
        return auth;
    }

    private void validateOneTimePayment(PaymentWS payment){
        List<PaymentInformationWS> informationWSs = payment.getPaymentInstruments();
        for (PaymentInformationWS paymentInformationWS : informationWSs) {
            String templateName = new PaymentMethodTypeDAS().
                    findNow(paymentInformationWS.getPaymentMethodTypeId()).getPaymentMethodTemplate().getTemplateName();
            Integer prefValue =  PreferenceBL.getPreferenceValueAsIntegerOrZero(getCallerCompanyId(), Constants.PREFERENCE_REQUIRE_CVV_FOR_ONE_TIME_PAYMENTS);
            char[] cardNumber = null;
            if (prefValue == 1) {
                for ( MetaFieldValueWS metaFieldValueWS : paymentInformationWS.getMetaFields()) {
                    if (Constants.METAFIELD_NAME_CARD_NUMBER.equals(metaFieldValueWS.getFieldName())) {
                        cardNumber = metaFieldValueWS.getCharValue();
                        break;
                    }
                    boolean isCreditCardObscurred = ArrayUtils.isEmpty(cardNumber) ? false : new String(cardNumber).contains("*");
                    if (templateName.equals("Payment Card") && null == paymentInformationWS.getId() && !isCreditCardObscurred) {
                        validateCvv(paymentInformationWS.getCvv());
                    }
                }
            }}
    }
    @Override
    public PaymentAuthorizationDTOEx[] processPayments(PaymentWS[] payments,Integer invoiceId) {
        PaymentAuthorizationDTOEx[] paymentAuthorizations = new PaymentAuthorizationDTOEx[payments.length];

        for (int i = 0; i < payments.length; i++) {
            paymentAuthorizations[i] = processPayment(payments[i], invoiceId);
        }

        return paymentAuthorizations;
    }

    /*
     * Validate credit card information using pre-Auth call to the Payment
     * plugin for level 3 (non-Javadoc)
     *
     * Level 1 - Simple checks on Credit Card Number, name, and mod10 Level 2 -
     * Address and Security Code validation Level 3 - Check number against a
     * payment gateway using pre-auth transaction
     */
    // public CardValidationWS
    // validateCreditCard(com.sapienter.jbilling.server.entity.CreditCardDTO
    // creditCard, ContactWS contact, int level) {
    // CardValidationWS validation = new CardValidationWS(level);
    //
    // /*
    // Level 1 validations (default), card has a name & number, number passes
    // mod10 luhn check
    // */
    //
    // if (StringUtils.isBlank(creditCard.getName())) {
    // validation.addError("Credit card name is missing.", 1);
    // }
    //
    // if (StringUtils.isBlank(creditCard.getNumber())) {
    // validation.addError("Credit card number is missing.", 1);
    //
    // } else {
    // if (creditCard.getNumber().matches("^\\D+$")) {
    // validation.addError("Credit card number is not a valid number.", 1);
    // }
    //
    // if
    // (!com.sapienter.jbilling.common.Util.luhnCheck(creditCard.getNumber())) {
    // validation.addError("Credit card mod10 validation failed.", 1);
    // }
    // }
    //
    //
    // /*
    // Level 2 validations, card has an address & a valid CVV security code
    // */
    // if (level > 1) {
    // if (StringUtils.isBlank(contact.getAddress1())) {
    // validation.addError("Customer address is missing.", 2);
    // }
    //
    // if (StringUtils.isBlank(creditCard.getSecurityCode())) {
    // validation.addError("Credit card CVV security code is missing.", 2);
    //
    // } else {
    // if (creditCard.getSecurityCode().matches("^\\D+$")) {
    // validation.addError("Credit card CVV security code is not a valid number.",
    // 2);
    // }
    // }
    // }
    //
    //
    // /*
    // Level 3 validations, attempted live pre-authorization against payment
    // gateway
    // */
    // if (level > 2) {
    // PaymentAuthorizationDTOEx auth = null;
    //
    // try {
    // // entity id, user id, credit card, amount, currency id, executor
    // auth = new CreditCardBL().validatePreAuthorization(getCallerCompanyId(),
    // new ContactBL(contact.getId()).getEntity().getUserId(),
    // new CreditCardDTO(creditCard),
    // new BigDecimal("0.01"),
    // 1,
    // getCallerId());
    // } catch (PluggableTaskException e) {
    // // log plug-in exception and ignore
    // logger.error("Exception occurred processing pre-authorization", e);
    // } catch (NamingException e){
    // logger.error("Exception occurred processing pre-authorization", e);
    // }
    //
    // if (auth == null || !auth.getResult()) {
    // validation.addError("Credit card pre-authorization failed.", 3);
    // }
    // validation.setPreAuthorization(auth);
    // }
    //
    // return validation;
    // }

    @Override
    @Transactional(readOnly = true)
    public PaymentWS getPayment(Integer paymentId)  {
        // get the info from the caller
        Integer languageId = getCallerLanguageId();

        PaymentBL bl = new PaymentBL(paymentId);
        return PaymentBL.getWS(bl.getDTOEx(languageId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentWS getLatestPayment(Integer userId) {
        PaymentWS retValue = null;
        // get the info from the caller
        Integer languageId = getCallerLanguageId();

        PaymentBL bl = new PaymentBL();
        Integer paymentId = bl.getLatest(userId);
        if (paymentId != null) {
            bl.set(paymentId);
            retValue = PaymentBL.getWS(bl.getDTOEx(languageId));
        }
        return retValue;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getLastPayments(Integer userId, Integer number) {
        return getLastPaymentsPage(userId, number, 0);
    }

    @Override
    public Integer[] getLastPaymentsPage(Integer userId, Integer limit,
            Integer offset)  {
        if (userId == null) {
            throw new SessionInternalError("User id can not be null!", HttpStatus.SC_BAD_REQUEST);
        }
        if (null == userDAS.findNow(userId)){
            throw new SessionInternalError("User does not exist!", HttpStatus.SC_NOT_FOUND);
        }

        validOffsetAndLimit(offset, limit);

        PaymentBL payment = new PaymentBL();
        return payment.getManyWS(userId, limit, offset, getCallerLanguageId());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getPaymentsByDate(Integer userId, Date since, Date until)
    {
        if (userId == null || since == null || until == null) {
            return null;
        }

        PaymentBL payment = new PaymentBL();
        return payment.getListIdsByDate(userId, since, until);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentWS getUserPaymentInstrument(Integer userId)
    {
        return getUserPaymentInstrument(userId, getCallerCompanyId());
    }

    @Transactional(readOnly = true)
    public PaymentWS getUserPaymentInstrument(Integer userId, Integer entityId)
    {
        PaymentDTOEx instrument;
        try {
            instrument = PaymentBL.findPaymentInstrument(entityId, userId);
        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Exception occurred fetching payment info plug-in.", e);
        } catch (TaskException e) {
            throw new SessionInternalError("Exception occurred with plug-in when fetching payment instrument.",e);
        }

        if (instrument == null) {
            return null;
        }
        // PaymentDTOEx paymentDTOEx = new PaymentDTOEx(instrument);
        String msg = "Instruments are: "+ instrument.getPaymentInstruments().size();
        String message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_GET,LogConstants.STATUS_SUCCESS);
        logger.debug(message);
        msg = "Instrument payment method is: "+ instrument.getPaymentInstruments().iterator().next().getPaymentMethod().getId();
        message = getEnhancedLogMessage(msg,LogConstants.MODULE_PAYMENT,LogConstants.ACTION_GET,LogConstants.STATUS_SUCCESS);
        logger.debug(message);
        instrument.setUserId(userId);
        return PaymentBL.getWS(instrument);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentWS[] getUserPaymentsPage(Integer userId, Integer limit,
            Integer offset)  {

        List<PaymentDTO> paymentsPaged = new PaymentBL().findUserPaymentsPaged(
                getCallerCompanyId(), userId, limit, offset);

        if (paymentsPaged == null) {
            return new PaymentWS[0];
        }

        List<PaymentWS> paymentsWs = new ArrayList<PaymentWS>(
                paymentsPaged.size());
        PaymentBL bl = null;
        for (PaymentDTO dto : paymentsPaged) {
            bl = new PaymentBL(dto.getId());
            PaymentWS wsdto = PaymentBL.getWS(bl
                    .getDTOEx(getCallerLanguageId()));
            paymentsWs.add(wsdto);
        }

        return paymentsWs.toArray(new PaymentWS[paymentsWs.size()]);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueByUser(Integer userId)
    {
        return new PaymentDAS().findTotalRevenueByUser(userId, null, null);
    }

    /**
     * Update the given order, or create it if it doesn't already exist.
     *
     * @param order order to update or create
     * @return order id
     * @
     */
    @Override
    public Integer copyCreateUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges, Integer targetCompanyId,
            Integer targetCompanyLanguageId, Integer newUserId) {
        IOrderSessionBean orderSession = Context.getBean(Context.Name.ORDER_SESSION);
        setOrderOnOrderChanges(order, orderChanges);
        validateOrder(order, orderChanges, false);
        validateLines(order);

        /*#7899 - The order being created is evaluated for subscription lines, if order is containing any subscription products
                  then internal account and order are created for each subscription line
         */
        List<OrderChangeWS> changes = JArrays.toArrayList(orderChanges);
        createSubscriptionAccountAndOrder(order.getUserId(), order, false, changes);
        orderChanges = changes != null ? changes.toArray(new OrderChangeWS[changes.size()]) : null;

        //if order has some lines left (that are non subscription) then create the order
        if(order.getOrderLines().length > 0 || orderChanges.length > 0) {
            // do some transformation from WS to DTO
            Map<OrderWS, OrderDTO> wsToDtoOrdersMap = new HashMap<OrderWS, OrderDTO>();
            Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap = new HashMap<OrderLineWS, OrderLineDTO>();
            OrderBL orderBL = new OrderBL();
            OrderDTO dto = orderBL.getDTO(order, wsToDtoOrdersMap, wsToDtoLinesMap);

            OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(dto);
            List<OrderChangeDTO> changeDtos = new LinkedList<OrderChangeDTO>();
            List<Integer> deletedChanges = new LinkedList<Integer>();
            convertOrderChangeWsToDto(orderChanges, changeDtos, deletedChanges, wsToDtoOrdersMap, wsToDtoLinesMap);

            Integer rootOrderId = orderSession.createUpdate(targetCompanyId, newUserId, targetCompanyLanguageId, rootOrder, changeDtos, deletedChanges);
            return wsToDtoOrdersMap.get(order).getId();
        }

        return null;
    }

    /*
     * ITEM
     */
    @Override
    public Integer createItem(ItemDTOEx item)  {

        // Get all descriptions to save-delete them afterwards.
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();
        SortedMap<Date,RatingConfigurationWS> ratingConfiguration=item.getRatingConfigurations();

        validateItem(item);

        ItemBL itemBL = new ItemBL();

        // Set the creator entity id before creating the DTO object.
        item.setEntityId(getCallerCompanyId());

        ItemDTO dto = ItemBL.getDTO(item);

        // Set description to null
        dto.setDescription(null);

        // get the info from the caller
        Integer languageId = getCallerLanguageId();

        validateAssetManagementForItem(dto, null);

        // call the creation
        Integer id = null;

        dto.setGlobal(item.isGlobal());

        try {
            id = itemBL.create(dto, languageId);
        } catch (Exception e){
            throw new SessionInternalError(e, HttpStatus.SC_BAD_REQUEST);
        }

        dto = itemBL.getEntity();

        // save-delete descriptions
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() != null
                    && description.getContent() != null) {
                if (description.isDeleted()) {
                    dto.deleteDescription(description.getLanguageId());
                } else {
                    dto.setDescription(description.getContent(),
                            description.getLanguageId());
                }
            }
        }

        if(ratingConfiguration!=null) {
            for (Date date : ratingConfiguration.keySet()) {
                RatingConfigurationDTO ratingConfigDTO = dto.getRatingConfigurations() != null ? dto.getRatingConfigurations().get(date) : null;
                List<InternationalDescriptionWS> pricingUnit = ratingConfiguration.get(date) != null ? ratingConfiguration.get(date).getPricingUnit() : null;
                if (ratingConfigDTO != null && !CollectionUtils.isEmpty(pricingUnit)) {
                    new RatingConfigurationBL(ratingConfigDTO).savePricingUnit(pricingUnit);
                }

            }
        }
        return id;
    }

    /*
     * ITEM FOR PLAN
     */
    public Integer createItem(ItemDTOEx item, boolean isPlan)
    {
        // check if all descriptions are to delete
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();
        boolean noDescriptions = true;
        for (InternationalDescriptionWS description : descriptions) {
            if (!description.isDeleted()) {
                noDescriptions = false;
                break;
            }
        }
        if (noDescriptions) {
            throw new SessionInternalError(
                    "Must have a description",
                    new String[] { "ItemDTOEx,descriptions,validation.error.is.required" });
        }

        SortedMap<Date,RatingConfigurationWS> ratingConfiguration=item.getRatingConfigurations();

        item.setEntityId(getCallerCompanyId());

        ItemBL itemBL = new ItemBL();
        ItemDTO dto = itemBL.getDTO(item);

        // Set description to null
        dto.setDescription(null);

        // get the info from the caller
        Integer languageId = getCallerLanguageId();
        Integer entityId = getCallerCompanyId();
        dto.setEntity(new CompanyDTO(entityId));

        // call the creation
        Integer id = itemBL.create(dto, languageId, isPlan);

        dto = itemBL.getEntity();

        // save-delete descriptions
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() != null
                    && description.getContent() != null) {
                if (description.isDeleted()) {
                    dto.deleteDescription(description.getLanguageId());
                } else {
                    dto.setDescription(description.getContent(),
                            description.getLanguageId());
                }
            }
        }

        if(ratingConfiguration!=null) {

            for (Date date : ratingConfiguration.keySet()) {

                RatingConfigurationDTO ratingConfigDTO = dto.getRatingConfigurations() != null ? dto.getRatingConfigurations().get(date) : null;
                List<InternationalDescriptionWS> pricingUnit = ratingConfiguration.get(date) != null ? ratingConfiguration.get(date).getPricingUnit() : null;
                if (ratingConfigDTO != null && !CollectionUtils.isEmpty(pricingUnit)) {
                    new RatingConfigurationBL(ratingConfigDTO).savePricingUnit(pricingUnit);
                }

            }
        }
        return id;
    }

    /**
     * Retrieves an array of items for the caller's entity.
     *
     * @return an array of items from the caller's entity
     */
    @Override
    @Transactional(readOnly = true)
    public ItemDTOEx[] getAllItems()  {
        return getAllItemsByEntityId(getCallerCompanyId());
    }

    /**
     * Implementation of the User Transitions List webservice. This accepts a
     * start and end date as arguments, and produces an array of data containing
     * the user transitions logged in the requested time range.
     *
     * @param from
     *            Date indicating the lower limit for the extraction of
     *            transition logs. It can be <code>null</code>, in such a case,
     *            the extraction will start where the last extraction left off.
     *            If no extractions have been done so far and this parameter is
     *            null, the function will extract from the oldest transition
     *            logged.
     * @param to
     *            Date indicatin the upper limit for the extraction of
     *            transition logs. It can be <code>null</code>, in which case
     *            the extraction will have no upper limit.
     * @return UserTransitionResponseWS[] an array of objects containing the
     *         result of the extraction, or <code>null</code> if there is no
     *         data thas satisfies the extraction parameters.
     */
    @Override
    public UserTransitionResponseWS[] getUserTransitions(Date from, Date to)
    {

        UserTransitionResponseWS[] result = null;
        Integer last = null;
        // Obtain the current entity and language Ids

        UserBL user = new UserBL();
        Integer callerId = getCallerId();
        Integer entityId = getCallerCompanyId();
        EventLogger evLog = EventLogger.getInstance();

        if (from == null) {
            last = evLog.getLastTransitionEvent(entityId);
        }

        if (last != null) {
            result = user.getUserTransitionsById(entityId, last, to);
        } else {
            result = user.getUserTransitionsByDate(entityId, from, to);
        }

        if (result == null) {
            logger.info("Data retrieved but resultset is null");
        } else {
            logger.info("Data retrieved. Result size = {}", result.length);
        }

        // Log the last value returned if there was any. This happens always,
        // unless the returned array is empty.
        if (result != null && result.length > 0) {
            logger.info("Registering transition list event");
            evLog.audit(callerId, null, Constants.TABLE_EVENT_LOG, callerId, EventLogger.MODULE_WEBSERVICES,
                    EventLogger.USER_TRANSITIONS_LIST, result[result.length - 1].getId(),
                    result[0].getId().toString(), null);
        }
        return result;
    }

    /**
     * @return UserTransitionResponseWS[] an array of objects containing the
     *         result of the extraction, or <code>null</code> if there is no
     *         data thas satisfies the extraction parameters.
     */
    @Override
    public UserTransitionResponseWS[] getUserTransitionsAfterId(Integer id)
    {

        UserTransitionResponseWS[] result = null;
        // Obtain the current entity and language Ids

        UserBL user = new UserBL();
        Integer callerId = getCallerId();
        Integer entityId = getCallerCompanyId();
        EventLogger evLog = EventLogger.getInstance();

        result = user.getUserTransitionsById(entityId, id, null);

        if (result == null) {
            logger.debug("Data retrieved but resultset is null");
        } else {
            logger.debug("Data retrieved. Result size = {}", result.length);
        }

        // Log the last value returned if there was any. This happens always,
        // unless the returned array is empty.
        if (result != null && result.length > 0) {
            logger.debug("Registering transition list event");
            evLog.audit(callerId, null, Constants.TABLE_EVENT_LOG, callerId,
                    EventLogger.MODULE_WEBSERVICES,
                    EventLogger.USER_TRANSITIONS_LIST,
                    result[result.length - 1].getId(), result[0].getId()
                    .toString(), null);
        }
        return result;
    }

    @Override
    @Transactional(readOnly=true)
    public ItemDTOEx getItem(Integer itemId, Integer userId, String pricing) {
        PricingField[] fields = PricingField.getPricingFieldsValue(pricing);
        Integer entityId = getCallerCompanyId();
        ItemDTO itemDTO = new ItemDAS().findNow(itemId);
        if (null == itemDTO){
            throw new SessionInternalError("Item not found", HttpStatus.SC_NOT_FOUND);
        }

        ItemBL helper = new ItemBL(itemDTO);
        List<PricingField> f = JArrays.toArrayList(fields);
        helper.setPricingFields(f);

        Integer languageId = getCallerLanguageId();

        // use the currency of the given user if provided, otherwise
        // default to the currency of the caller (admin user)
        Integer currencyId = (userId != null
                ? new UserBL(userId).getCurrencyId()
                        : getCallerCurrencyId());

        ItemDTOEx retValue = helper.getWS(helper.getDTO(languageId, userId, entityId, currencyId));

        // get descriptions
        retValue.setDescriptions(getAllItemDescriptions(retValue.getId()));
        return retValue;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountWS getDiscountWS(Integer discountId) {
        if(discountId == null) {
            return null;
        }
        DiscountDTO discountDTO = new DiscountDAS().findNow(discountId);
        if (null == discountDTO) {
            throw new SessionInternalError("Discount with id:" + discountId + " does not exist.", HttpStatus.SC_NOT_FOUND);
        }
        DiscountWS discountWS = new DiscountBL().getWS(discountDTO);
        discountWS.setDescriptions(getAllDiscountDescriptions(discountId));
        return discountWS;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountWS getDiscountWSByCode(String discountCode) {
        DiscountBL discountBl = new DiscountBL(discountCode, getCallerCompanyId());
        DiscountWS discountWS = discountBl.getWS(discountBl.getEntity());
        discountWS.setDescriptions(getAllDiscountDescriptions(discountWS.getId()));
        return discountWS;
    }

    @Override
    public void deleteDiscount(Integer discountId)  {
        DiscountBL bl = new DiscountBL(discountId);
        bl.delete();
    }

    private List<InternationalDescriptionWS> getAllDiscountDescriptions(
            int discountId) {
        JbillingTableDAS tableDas = Context
                .getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_DISCOUNT);

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas
                .findAll(table.getId(), discountId, "description");

        List<InternationalDescriptionWS> descriptionsWS = new ArrayList<InternationalDescriptionWS>();
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            descriptionsWS.add(DescriptionBL.getInternationalDescriptionWS(descriptionDTO));
        }
        return descriptionsWS;
    }

    @Override
    @Transactional(readOnly = true)
    public UsagePoolWS getUsagePoolWS(Integer usagePoolId) {
        UsagePoolBL usagePoolBl = new UsagePoolBL(usagePoolId);
        UsagePoolWS usagePoolWS = usagePoolBl.getWS(usagePoolBl.getEntity());
        usagePoolWS.setNames(getAllUsagePoolNames(usagePoolId));
        return usagePoolWS;
    }

    private List<InternationalDescriptionWS> getAllUsagePoolNames(
            int usagePoolId) {
        JbillingTableDAS tableDas = Context
                .getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_USAGE_POOL);

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas
                .findAll(table.getId(), usagePoolId, "name");

        List<InternationalDescriptionWS> names = new ArrayList<InternationalDescriptionWS>();
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            names.add(DescriptionBL.getInternationalDescriptionWS(descriptionDTO));
        }
        logger.debug("names: " + names);
        return names;
    }

    @Override
    public Integer createUsagePool(UsagePoolWS ws) {
        Integer languageId = getCallerLanguageId();
        return new UsagePoolBL().createOrUpdate(ws, languageId);
    }

    @Override
    public void updateUsagePool(UsagePoolWS ws) {
        Integer languageId = getCallerLanguageId();
        new UsagePoolBL().createOrUpdate(ws, languageId);
    }

    @Override
    @Transactional(readOnly = true)
    public UsagePoolWS[] getAllUsagePools() {
        Integer entityId = getCallerCompanyId();
        List<UsagePoolDTO> usagePools = new UsagePoolDAS()
        .findByEntityId(entityId);
        List<UsagePoolWS> usagePoolWSs = new ArrayList<UsagePoolWS>();
        for (UsagePoolDTO usagePool : usagePools) {
            UsagePoolWS ws = UsagePoolBL.getUsagePoolWS(usagePool);
            ws.setNames(getAllUsagePoolNames(usagePool.getId()));
            usagePoolWSs.add(ws);

        }
        return usagePoolWSs.toArray(new UsagePoolWS[usagePoolWSs.size()]);
    }

    @Override
    public boolean deleteUsagePool(Integer usagePoolId) {
        UsagePoolBL usagePoolBl = new UsagePoolBL(usagePoolId);
        return usagePoolBl.delete();
    }

    @Override
    @Transactional(readOnly = true)
    public UsagePoolWS[] getUsagePoolsByPlanId(Integer planId) {
        PlanDTO planDto = new PlanDAS().find(planId);
        List<UsagePoolWS> usagePoolWSs = new ArrayList<UsagePoolWS>();
        for (UsagePoolDTO usagePoolDto : planDto.getUsagePools()) {
            UsagePoolWS ws = UsagePoolBL.getUsagePoolWS(usagePoolDto);
            ws.setNames(getAllUsagePoolNames(usagePoolDto.getId()));
            usagePoolWSs.add(ws);
        }
        Collections.sort(usagePoolWSs, new Comparator<UsagePoolWS>() {
            @Override
            public int compare(UsagePoolWS usagePool1, UsagePoolWS usagePool2) {
                return usagePool1.getCreatedDate().compareTo(usagePool2.getCreatedDate());
            }
        });
        return usagePoolWSs.toArray(new UsagePoolWS[usagePoolWSs.size()]);
    }

    private List<InternationalDescriptionWS> getAllItemDescriptions(int itemId) {
        JbillingTableDAS tableDas = Context
                .getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_ITEM);

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas
                .findAll(table.getId(), itemId, "description");

        List<InternationalDescriptionWS> descriptionsWS = new ArrayList<InternationalDescriptionWS>();
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            descriptionsWS.add(DescriptionBL.getInternationalDescriptionWS(descriptionDTO));
        }
        return descriptionsWS;
    }

    @Override
    public Integer createItemCategory(ItemTypeWS itemType)
    {

        if (itemType.getAssetMetaFields() != null) {
            for (MetaFieldWS field : itemType.getAssetMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "ItemTypeWS,assetMetaFields,metafield.validation.filename.required" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        Integer entityId = getCallerCompanyId();
        if (!itemType.isGlobal()
                && CollectionUtils.isEmpty(itemType.getEntities())) {
            ArrayList ents = new ArrayList();
            ents.add(entityId);
            itemType.setEntities(ents);
        }

        AssetStatusBL assetStatusBL = new AssetStatusBL();

        ItemTypeDTO dto = new ItemTypeDTO();
        dto.setDescription(itemType.getDescription());
        dto.setOrderLineTypeId(itemType.getOrderLineTypeId());
        dto.setParent(new ItemTypeDAS().find(itemType.getParentItemTypeId()));
        dto.setGlobal(itemType.isGlobal());
        dto.setOnePerOrder(itemType.isOnePerOrder());
        dto.setOnePerCustomer(itemType.isOnePerCustomer());
        dto.setEntity(new CompanyDAS().find(getCallerCompanyId()));

        List<Integer> entities = new ArrayList<Integer>(0);

        CompanyDAS companyDAS = new CompanyDAS();
        if (!itemType.isGlobal()) {
            entities.addAll(itemType.getEntities());
        }

        dto.setEntities(AssetBL.convertToCompanyDTO(itemType.getEntities()));

        dto.setAllowAssetManagement(itemType.getAllowAssetManagement());
        dto.setAssetIdentifierLabel(itemType.getAssetIdentifierLabel());
        dto.setAssetStatuses(assetStatusBL.convertAssetStatusDTOExes(itemType
                .getAssetStatuses()));

        // Assign asset meta fields to the company that created the category.
        dto.setAssetMetaFields(MetaFieldBL.convertMetaFieldsToDTO(
                itemType.getAssetMetaFields(), getCallerCompanyId()));

        ItemTypeBL.fillMetaFieldsFromWS(dto, itemType);

        validateAssetMetaFields(new HashSet<MetaField>(0),
                dto.getAssetMetaFields());
        validateItemCategoryStatuses(dto);

        entities = new ArrayList<Integer>(0);
        entities.add(getCallerCompanyId());
        entities.addAll(companyDAS.getChildEntitiesIds(getCallerCompanyId()));

        ItemTypeBL itemTypeBL = new ItemTypeBL();
        itemTypeBL.setCallerCompanyId(getCallerCompanyId());
        // Check if the category already exists to throw an error to the user.
        if (itemTypeBL.existsGlobal(getCallerCompanyId(), dto.getDescription())
                || itemTypeBL.exists(entities, dto.getDescription())) {
            throw new SessionInternalError(
                    "The product category already exists with name "
                            + dto.getDescription(),
                            new String[] { "ItemTypeWS,name,validation.error.category.already.exists" }, HttpStatus.SC_BAD_REQUEST);
        }

        // a subscription product must allow asset management
        if (dto.getOrderLineTypeId() == Constants.ORDER_LINE_TYPE_SUBSCRIPTION
                && dto.getAllowAssetManagement() != 1) {
            throw new SessionInternalError(
                    "Subscription product category must allow asset management",
                    new String[] { "ItemTypeWS,allowAssetManagement,validation.error.subscription.category.asset.management" }, HttpStatus.SC_BAD_REQUEST);
        }

        itemTypeBL.create(dto);

        // we need ids to create descriptions. Can only do it after flush
        for (AssetStatusDTO statusDTO : itemTypeBL.getEntity()
                .getAssetStatuses()) {
            statusDTO.setDescription(statusDTO.getDescription(),
                    Constants.LANGUAGE_ENGLISH_ID);
        }

        return itemTypeBL.getEntity().getId();
    }

    @Override
    public void updateItemCategory(ItemTypeWS itemType)
    {

        if (itemType.getAssetMetaFields() != null) {
            for (MetaFieldWS field : itemType.getAssetMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] { "ItemTypeWS,assetMetaFields,metafield.validation.filename.required" }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        UserBL bl = new UserBL(getCallerId());
        Integer executorId = bl.getEntity().getUserId();

        ItemTypeBL itemTypeBL = new ItemTypeBL(itemType.getId(),
                getCallerCompanyId());
        if (!itemType.isGlobal()
                && CollectionUtils.isEmpty(itemType.getEntities())) {
            List<Integer> ents = new ArrayList<Integer>();
            ents.add(getCallerCompanyId());
            itemType.setEntities(ents);
        }

        AssetStatusBL assetStatusBL = new AssetStatusBL();

        ItemTypeDTO dto = new ItemTypeDTO();
        dto.setDescription(itemType.getDescription());
        dto.setGlobal(itemType.isGlobal());
        dto.setOrderLineTypeId(itemType.getOrderLineTypeId());
        dto.setAllowAssetManagement(itemType.getAllowAssetManagement());
        dto.setAssetIdentifierLabel(itemType.getAssetIdentifierLabel());
        dto.setAssetStatuses(assetStatusBL.convertAssetStatusDTOExes(itemType
                .getAssetStatuses()));
        dto.setOnePerCustomer(itemType.isOnePerCustomer());
        dto.setOnePerOrder(itemType.isOnePerOrder());
        dto.setEntity(new CompanyDAS().find(itemType.getEntityId()));

        List<Integer> entities = new ArrayList<Integer>(0);

        CompanyDAS companyDAS = new CompanyDAS();
        if (!itemType.isGlobal()) {
            entities.addAll(itemType.getEntities());
        }
        dto.setEntities(AssetBL.convertToCompanyDTO(itemType.getEntities()));

        // Assign asset meta fields to the company that created the category.
        dto.setAssetMetaFields(MetaFieldBL.convertMetaFieldsToDTO(
                itemType.getAssetMetaFields(), itemType.getEntityId()));

        ItemTypeBL.fillMetaFieldsFromWS(dto, itemType);

        // check if global category has been marked non-global when their are
        // metafields for children
        if (companyDAS.isRoot(getCallerCompanyId()) // is caller company root?
                && (itemTypeBL.getEntity().isGlobal() && !itemType.isGlobal()) // has
                // visibility
                // of
                // itemType
                // decreased?
                && ItemTypeBL.isChildMetaFieldPresent(itemType,
                        getCallerCompanyId())) {
            throw new SessionInternalError(
                    "Cannot decrease visibility when child metafields are set",
                    new String[] { "ItemTypeWS,global,metafield.validation.global.changed" }, HttpStatus.SC_BAD_REQUEST);
        }

        // validate statuses and meta fields
        validateItemCategoryStatuses(dto);
        validateAssetMetaFields(itemTypeBL.getEntity().getAssetMetaFields(),
                dto.getAssetMetaFields());

        entities = new ArrayList<Integer>(0);
        entities.add(getCallerCompanyId());
        entities.addAll(companyDAS.getChildEntitiesIds(getCallerCompanyId()));

        // make sure that item category names are unique. If the name was
        // changed, then check
        // that the new name isn't a duplicate of an existing category.
        if (!itemTypeBL.getEntity().getDescription()
                .equalsIgnoreCase(itemType.getDescription())) {
            if (itemTypeBL.existsGlobal(getCallerCompanyId(),
                    dto.getDescription())
                    || itemTypeBL.exists(entities, dto.getDescription())) {
                throw new SessionInternalError(
                        "The product category already exists with name "
                                + dto.getDescription(),
                                new String[] { "ItemTypeWS,name,validation.error.category.already.exists" }, HttpStatus.SC_BAD_REQUEST);
            }
        }

        // a subscription product must allow asset management
        if (dto.getOrderLineTypeId() == Constants.ORDER_LINE_TYPE_SUBSCRIPTION
                && dto.getAllowAssetManagement() != 1) {
            throw new SessionInternalError(
                    "Subscription product category must allow asset management",
                    new String[] { "ItemTypeWS,allowAssetManagement,validation.error.subscription.category.asset.management" }, HttpStatus.SC_BAD_REQUEST);
        }

        // if the type changed from not allowing asset management to allowing it
        // we check that there is not a
        // product already linked to the category which is already linked
        // another category which allows asset management
        if (dto.getAllowAssetManagement() == 1
                && itemTypeBL.getEntity().getAllowAssetManagement() == 0) {
            List<Integer> typeIds = itemTypeBL
                    .findAllTypesLinkedThroughProduct(itemType.getId());
            if (typeIds.size() > 1) {
                throw new SessionInternalError(
                        "The category is linked to a product which can already do asset management",
                        new String[] { "ItemTypeWS,allowAssetManagement,product.category.validation.multiple.linked.assetmanagement.types.error" }, HttpStatus.SC_BAD_REQUEST);
            }
        }

        // if the type changed from allowing asset management to not allowing it
        // we check that there is not a
        // product linked to it which has asset management enabled
        if (dto.getAllowAssetManagement() == 0 && itemTypeBL.getEntity().getAllowAssetManagement() == 1) {
            itemTypeBL.getEntity().getItems().forEach( item -> {
                if (item.getAssetManagementEnabled() == 1 && item.getDeleted() == 0) {
                    throw new SessionInternalError( "The category is linked to a product which can already do asset management",
                            new String[] { "ItemTypeWS,allowAssetManagement,product.category.validation.product.assetmanagement.enabled" }, HttpStatus.SC_BAD_REQUEST);
                }
            });
        }

        itemTypeBL.update(executorId, dto);

        // we need ids to create descriptions. Can only do it after flush
        for (AssetStatusDTO statusDTO : dto.getAssetStatuses()) {
            statusDTO.setDescription(statusDTO.getDescription(),
                    Constants.LANGUAGE_ENGLISH_ID);
        }
    }

    /**
     * Validation for AssetDTO MetaFields
     *
     * @param currentMetaFields
     *            - current list of meta fields attached to the asset
     * @param newMetaFields
     *            - new meta fields that will be attached to the asset
     * @
     */
    private void validateAssetMetaFields(
            Collection<MetaField> currentMetaFields,
            Collection<MetaField> newMetaFields)  {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        Map currentMetaFieldMap = new HashMap(currentMetaFields.size() * 2);
        Set names = new HashSet(currentMetaFields.size() * 2);

        // collect the current meta fields
        for (MetaField dto : currentMetaFields) {
            currentMetaFieldMap.put(dto.getId(), dto);
        }

        // loop through the new metaFields
        for (MetaField metaField : newMetaFields) {
            if (names.contains(metaField.getName())) {
                throw new SessionInternalError(
                        "Meta field names must be unique ["
                                + metaField.getName() + "]",
                                new String[] { "MetaFieldWS,name,metaField.validation.name.unique,"
                                        + metaField.getName() }, HttpStatus.SC_BAD_REQUEST);
            }
            names.add(metaField.getName());

            // if it is already in the DB validate the changes
            if (metaField.getId() > 0) {
                MetaField currentMetaField = (MetaField) currentMetaFieldMap
                        .get(metaField.getId());

                // if the type change we have to make sure it is not already
                // used
                boolean checkUsage = !currentMetaField.getDataType().equals(
                        metaField.getDataType());
                if (checkUsage
                        && MetaFieldBL.isMetaFieldUsed(EntityType.ASSET,
                                metaField.getId())) {
                    throw new SessionInternalError(
                            "Data Type may not be changes is meta field is used ["
                                    + metaField.getName() + "]",
                                    new String[] { "MetaFieldWS,dataType,metaField.validation.type.change.not.allowed,"
                                            + metaField.getName() }, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }
    }

    private Integer zero2null(Integer var) {
        if (var != null && var.intValue() == 0) {
            return null;
        } else {
            return var;
        }
    }

    private Date zero2null(Date var) {
        if (var != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(var);
            if (cal.get(Calendar.YEAR) == 1) {
                return null;
            }
        }

        return var;

    }

    private void validateUser(UserWS newUser) throws SessionInternalError {
        // do the validation
        if (newUser == null) {
            throw new SessionInternalError("Null parameter");
        }
        // C# sends a 0 when it is null ...
        newUser.setCurrencyId(zero2null(newUser.getCurrencyId()));
        newUser.setPartnerRoleId(zero2null(newUser.getPartnerRoleId()));
        newUser.setParentId(zero2null(newUser.getParentId()));
        newUser.setMainRoleId(zero2null(newUser.getMainRoleId()));
        newUser.setLanguageId(zero2null(newUser.getLanguageId()));
        newUser.setStatusId(zero2null(newUser.getStatusId()));

        // todo: additional hibernate validations
        // additional validation
        if (newUser.getMainRoleId().equals(Constants.TYPE_CUSTOMER)
                || newUser.getMainRoleId().equals(Constants.TYPE_PARTNER)) {
        } else {
            throw new SessionInternalError("Valid user roles are customer (5) "
                    + "and partner (4)");
        }
        if (newUser.getCurrencyId() != null
                && newUser.getCurrencyId().intValue() <= 0) {
            throw new SessionInternalError("Invalid currency code");
        }
        if (newUser.getStatusId().intValue() <= 0) {
            throw new SessionInternalError("Invalid status code");
        }
    }

    private boolean isPlanSwap(OrderChangeWS[] changes) {
        return Arrays.stream(changes)
                .filter(change -> {
                    ItemDTO item = new ItemDAS().find(change.getItemId());
                    return item.hasPlans();
                }).count() > 1;
    }

    /**
     * Validate all orders in hierarchy and order changes
     *
     * @param order
     *            orders hierarchy for validation
     * @param orderChanges
     *            order changes for validation
     * @
     *             if validation was failed
     */
    private void validateOrder(OrderWS order, OrderChangeWS[] orderChanges, boolean skipOrderFinishValidation) {
        Map<OrderWS, Boolean> ordersWithChanges = new HashMap<>();

        if (orderChanges != null) {
            for (OrderChangeWS change : orderChanges) {
                validateOrderChange(order, change, isPlanSwap(orderChanges));
                OrderWS changeOrder;
                if (change.getOrderId() != null) {
                    changeOrder = OrderHelper.findOrderInHierarchy(order,
                            change.getOrderId());
                } else {
                    changeOrder = change.getOrderWS();
                }
                if (changeOrder != null) {
                    ordersWithChanges.put(changeOrder, true);
                }
            }
        }
        validateOrder(order, new HashSet<OrderWS>(), ordersWithChanges);
        if(!skipOrderFinishValidation) {
            validateUpdateOrder(order);
        }
    }

    public void validateDiscountLines(OrderDTO order, List<OrderChangeDTO> orderChanges) {

        if(!order.hasDiscountLines()){
            return;
        }

        for(DiscountLineDTO discountLine : order.getDiscountLines()){
            discountLine.setPurchaseOrder(order);
            DiscountDTO discount = discountLine.getDiscount();

            if(discountLine.isOrderLevelDiscount()){
                if (discount.getStartDate() != null &&
                        //discount should not be active compared to order
                        discount.getStartDate().after(order.getActiveSince())) {
                    throw new SessionInternalError("Discount Start Date is in future w.r.t. order's active since date.",
                            new String []{ "DiscountWS,startDate,discount.startDate.in.future," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getStartDate(), order.getUserId()) });
                }

                if (discount.getEndDate() != null &&
                        // Discount should be active till order active since date
                        discount.getEndDate().before(order.getActiveSince())) {
                    throw new SessionInternalError("Discount End Date is in before the order active since date.",
                            new String []{ "DiscountWS,endDate,discount.endDate.in.before," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getEndDate(), order.getUserId()) });
                }

            }else if(discountLine.isProductLevelDiscount()){
                OrderChangeDTO orderChange = null;
                ItemDTO item = discountLine.getItem();

                for(OrderChangeDTO changeDTO :orderChanges){
                    if(changeDTO.getItem().getId() == item.getId()){
                        orderChange = changeDTO;
                        break;
                    }
                }

                if ((discount.getStartDate() != null && orderChange != null) &&
                        discount.getStartDate().after(orderChange.getStartDate())) {
                    throw new SessionInternalError("Discount Start Date is in future w.r.t. order change's start-date of item.",
                            new String []{ "DiscountWS,startDate,discount.startDate.in.future.item," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getStartDate(), order.getUserId())+","+item.getDescription() });
                }

                if ((discount.getEndDate() != null && orderChange != null) &&
                        discount.getEndDate().before(orderChange.getStartDate())) {
                    throw new SessionInternalError("Discount End Date is in before the order-change start date of item.",
                            new String []{ "DiscountWS,endDate,discount.endDate.in.before.item," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getEndDate(), order.getUserId())+","+item.getDescription() });
                }

            }else if(discountLine.isPlanItemLevelDiscount()){
                OrderChangeDTO orderChange = null;
                PlanItemDTO planItem  = discountLine.getPlanItem();

                orderChangeLoop:
                    for(OrderChangeDTO changeDTO :orderChanges){
                        Set<OrderChangePlanItemDTO> orderChangePlanItems = changeDTO.getOrderChangePlanItems();
                        for(OrderChangePlanItemDTO orderChangePlanItem: orderChangePlanItems){
                            if(orderChangePlanItem.getItem().getId() == planItem.getItem().getId()){
                                orderChange = changeDTO;
                                break orderChangeLoop;
                            }
                        }
                    }

                if ((discount.getStartDate() != null && orderChange != null) &&
                        discount.getStartDate().after(orderChange.getStartDate())) {
                    throw new SessionInternalError("Discount Start Date is in future w.r.t. order change's start-date of plan item.",
                            new String []{ "DiscountWS,startDate,discount.startDate.in.future.plan.item," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getStartDate(), order.getUserId())+","+planItem.getItem().getDescription()+
                                    ","+planItem.getPlan().getItem().getDescription() });
                }

                if ((discount.getEndDate() != null && orderChange != null) &&
                        discount.getEndDate().before(orderChange.getStartDate())) {
                    throw new SessionInternalError("Discount End Date is in before the order-change start date of plan item.",
                            new String []{ "DiscountWS,endDate,discount.endDate.in.before.plan.item," +
                                    discount.getCode() + "," + com.sapienter.jbilling.server.util.Util.formatDate
                                    (discount.getEndDate(), order.getUserId())+","+planItem.getItem().getDescription()+
                                    ","+planItem.getPlan().getItem().getDescription()});
                }
            }
        }
    }

    private BillingProcessDTO getLastBillingProcessByEntityId(Integer entityId) {
        try {
            Integer lastBillingProcessId = new BillingProcessBL().getLast(entityId);
            if(lastBillingProcessId == -1) {
                return null;
            }
            return new BillingProcessDAS().find(lastBillingProcessId);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }
    private void validateOrderChange(OrderWS order, OrderChangeWS orderChange, boolean isPlanSwap) {

        BillingProcessDTO lastBillingProcess = getLastBillingProcessByEntityId(getCallerCompanyId());

        if (orderChange.getStartDate() != null
                && com.sapienter.jbilling.common.Util.truncateDate(
                        orderChange.getStartDate()).before(
                                com.sapienter.jbilling.common.Util.truncateDate(order
                                        .getActiveSince()))) {
            String error = "OrderChangeWS,startDate,validation.error.incorrect.start.date";
            throw new SessionInternalError(
                    String.format(
                            "Order ActiveSince %s, Incorrect start date %s for order change",
                            order.getActiveSince(), orderChange.getStartDate()),
                            new String[] { error });
        }

        if (orderChange.getStartDate() != null
                && null != order.getActiveUntil()
                && !com.sapienter.jbilling.common.Util.truncateDate(
                        order.getActiveUntil()).after(
                                com.sapienter.jbilling.common.Util
                                .truncateDate(orderChange.getStartDate()))) {
            String error = "OrderChangeWS,startDate,validation.error.incorrect.start.date.expiry";
            throw new SessionInternalError(
                    String.format(
                            "Order Active Until %s, Incorrect start date %s for order change",
                            order.getActiveUntil(), orderChange.getStartDate()),
                            new String[] { error });
        }

        Function<Integer, Boolean> invoicesForOrder = orderId  -> CollectionUtils.isNotEmpty(new OrderProcessDAS().findActiveInvoicesForOrder(order.getId()));
        if (lastBillingProcess != null && invoicesForOrder.apply(order.getId())) {
            Date endOfProcessPeriod = BillingProcessBL.getEndOfProcessPeriod(lastBillingProcess);
            if (order.getBillingTypeId().equals(Constants.ORDER_BILLING_POST_PAID)){
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endOfProcessPeriod);
                calendar.set(Calendar.MONTH, endOfProcessPeriod.getMonth() - 1);
                endOfProcessPeriod = calendar.getTime();
            }

            if(!isPlanSwap && orderChange.getStartDate() != null && com.sapienter.jbilling.common.Util.truncateDate(
                    endOfProcessPeriod).after(com.sapienter.jbilling.common.Util.truncateDate(
                            orderChange.getStartDate()))) {
                String error = "validation.error.incorrect.effective.date";
                throw new SessionInternalError(
                        String.format(
                                "Order ActiveSince %s, Incorrect start date %s for order change",
                                order.getActiveSince(), orderChange.getStartDate()),
                                new String[] { error });
            }
        }

        if (orderChange.getItemId() == null) {
            String error = "OrderChangeWS,itemId,validation.error.is.required";
            throw new SessionInternalError("Item is required for order change",
                    new String[] { error });
        }
        if (orderChange.getUserAssignedStatusId() == null) {
            String error = "OrderChangeWS,userAssignedStatus,validation.error.is.required";
            throw new SessionInternalError(
                    "User assigned status is required for order change",
                    new String[] { error });
        }
        if (orderChange.getOrderId() == null
                && orderChange.getOrderWS() == null) {
            String error = "OrderChangeWS.order.validation.error.is.required";
            throw new SessionInternalError("OrderChange validation error",
                    new String[] { error });
        }
        ItemDTO itemDTO = new ItemBL(orderChange.getItemId()).getEntity();
        MetaFieldBL.validateMetaFields(itemDTO.getEntity().getLanguageId(), itemDTO.getOrderLineMetaFields(),
                orderChange.getMetaFields());
    }

    private void validateOrder(OrderWS order, Set<OrderWS> alreadyValidated,
            Map<OrderWS, Boolean> ordersWithChanges)
    {
        if (alreadyValidated.contains(order)) {
            return;
        }
        // prevent cycles in initial hierarchy if exists
        alreadyValidated.add(order);

        logger.debug("Validating order: " + order);
        if (order == null) {
            throw new SessionInternalError("Null parameter");
        }

        boolean orderCreate = order.getId() == null;

        if (order.getOrderLines().length == 1) {

            ItemDTO checkItem = new ItemDAS().find(order.getOrderLines()[0].getItemId());
            if (null != checkItem && null != checkItem.getPrice(companyCurrentDate(), getCallerCompanyId())
                    && checkItem.getPrice(companyCurrentDate(), getCallerCompanyId()).getType() == PriceModelStrategy.LINE_PERCENTAGE) {
                throw new SessionInternalError(
                        "Order can not create for line percentage product",
                        new String[] { "validation.error.order.linePercentage.product" });
            }
        }

        if (order.getUserCode() != null && order.getUserCode().length() > 0) {
            UserBL userBL = new UserBL();
            if (userBL.findUserCodeForIdentifier(order.getUserCode(),
                    getCallerCompanyId()) == null) {
                throw new SessionInternalError(
                        "Order validation failed. User Code does not exist",
                        new String[] { "OrderWS,userCode,validation.error.userCode.not.exist,"
                                + order.getUserCode() });
            }
        }

        for (OrderLineWS orderLineWs : order.getOrderLines()) {
            if (orderLineWs.getChildLines() != null
                    && orderLineWs.getChildLines().length == 1
                    && orderLineWs.getChildLines()[0].isPercentage()) {
                throw new SessionInternalError(
                        "Line percentage item can not added as a sub order",
                        new String[] { "OrderLineWS,itemId,validation.order.line.not.added.line.percentage.item,"
                                + orderLineWs.getItemId() });
            }
        }

        if (order.getCancellationMinimumPeriod() != null || (order.getCancellationFeeType() != null && !order.getCancellationFeeType().trim().isEmpty())) {
            List<String> errmsgsList = new ArrayList<>();
            if (order.getCancellationMinimumPeriod() == null) {
                errmsgsList.add("OrderWS,cancellationMinimumPeriod,cancellationFee.minimum.empty");
            }

            if (order.getCancellationFeeType() == null || order.getCancellationFeeType().trim().isEmpty()) {
                errmsgsList.add("OrderWS,cancellationFeeType,cancellationFee.type.empty");
            }

            if (order.getCancellationFeeType().equalsIgnoreCase(FLAT.getCancellationFeeType()) && order.getCancellationFee() == null) {
                errmsgsList.add("OrderWS,cancellationFee,cancellationFee.fee.empty");

            }
            if (order.getCancellationFeeType().equalsIgnoreCase(PERCENTAGE.getCancellationFeeType()) && order.getCancellationFeePercentage() == null) {
                errmsgsList.add("OrderWS,cancellationFeePercentage,cancellationFee.fee.percentage.empty");
            }
            if (errmsgsList.size() > 0) {
                throw new SessionInternalError("Error setting cancellation Fee field(s)", errmsgsList.toArray(new String[errmsgsList.size()]));
            }
        }

        if (OrderBL.countPlan(order.getOrderLines()) > 1) {
            throw new SessionInternalError("Order should not contain multiple plans",
                    new String[] { "validation.order.should.not.contain.multiple.plans" });
        }

        validateProrating(order);
        // meta fields validation
        MetaFieldBL.validateMetaFields(getCallerLanguageId(), getCallerCompanyId(), EntityType.ORDER,
                order.getMetaFields());

        order.setUserId(zero2null(order.getUserId()));
        order.setPeriod(zero2null(order.getPeriod()));
        order.setBillingTypeId(zero2null(order.getBillingTypeId()));

        // Setup a default order status if there isnt one
        if (order.getOrderStatusWS() == null
                || order.getOrderStatusWS().getId() == null) {
            OrderStatusWS os = new OrderStatusWS();
            // #7853 - If no order statuses are configured thro' the
            // configuration menu an exception is shown on the 'create order'
            // UI.
            // Following exception handling added to take care of the issue.
            try {
                os.setId(new OrderStatusDAS().getDefaultOrderStatusId(
                        OrderStatusFlag.INVOICE, getCallerCompanyId()));
            } catch (Exception e) {
                throw new SessionInternalError(
                        "Order validation failed. No order status found for the order",
                        new String[] { "OrderWS,orderStatus,No order status found for the order" });
            }
            order.setOrderStatusWS(os);
        }

        order.setCurrencyId(zero2null(order.getCurrencyId()));
        order.setNotificationStep(zero2null(order.getNotificationStep()));
        order.setDueDateUnitId(zero2null(order.getDueDateUnitId()));
        order.setPrimaryOrderId(zero2null(order.getPrimaryOrderId()));
        // Bug Fix: 1385: Due Date may be zero
        // order.setDueDateValue(zero2null(order.getDueDateValue()));
        order.setDfFm(zero2null(order.getDfFm()));
        order.setAnticipatePeriods(zero2null(order.getAnticipatePeriods()));
        order.setActiveSince(zero2null(order.getActiveSince()));
        order.setActiveUntil(zero2null(order.getActiveUntil()));
        order.setNextBillableDay(zero2null(order.getNextBillableDay()));
        order.setLastNotified(null);

        // CXF seems to pass empty array as null
        if (order.getOrderLines() == null) {
            logger.debug("Order Lines == null");
            order.setOrderLines(new OrderLineWS[0]);
        }
        if (order.getChildOrders() == null) {
            order.setChildOrders(null);
        }

        // todo: additional hibernate validations
        // the lines
        if (orderCreate && !ordersWithChanges.containsKey(order)) {
            IsNotEmptyOrDeletedValidator validator = new IsNotEmptyOrDeletedValidator();
            if (!validator.isValid(order.getOrderLines(), null)) {
                throw new SessionInternalError(
                        "Order validation failed",
                        new String[] { "OrderWS,orderLines,validation.error.empty.lines" });
            }
        }

        for (int f = 0; f < order.getOrderLines().length; f++) {
            OrderLineWS line = order.getOrderLines()[f];
            if (line.getUseItem() == null) {
                line.setUseItem(false);
            }
            line.setItemId(zero2null(line.getItemId()));
            String error = "";
            // if use the item, I need the item id
            if (line.getUseItem()) {
                if (line.getItemId() == null
                        || line.getItemId().intValue() == 0) {
                    error += "OrderLineWS: if useItem == true the itemId is required - ";
                }
                /*
                 * if (line.getQuantityAsDecimal() == null ||
                 * BigDecimal.ZERO.compareTo(line.getQuantityAsDecimal()) == 0)
                 * { error +=
                 * "OrderLineWS: if useItem == true the quantity is required - "
                 * ; }
                 */
            } else {
                // I need the amount and description
                if (line.getAmount() == null) {
                    error += "OrderLineWS: if useItem == false the item amount "
                            + "is required - ";
                }
                if (line.getDescription() == null
                        || line.getDescription().length() == 0) {
                    error += "OrderLineWS: if useItem == false the description "
                            + "is required - ";
                }
            }

            // validate meta fields
            if (line.getItemId() != null) {
                ItemDTO item = new ItemBL(line.getItemId())
                .getEntity();
                MetaFieldBL.validateMetaFields(item.getEntity().getLanguageId(), item.getOrderLineMetaFields(), line
                        .getMetaFields());
            }

            // validation needed only during order creation
            if (orderCreate) {
                // check if the number of assets equals the quantity
                PlanDAS planDAS = new PlanDAS();
                // if order line is not a plan
                List<PlanDTO> plans = planDAS.findByItemId(line.getItemId());
                if (CollectionUtils.isNotEmpty(plans)) {
                    if (line.getDeleted() == 0
                            && ArrayUtils.isNotEmpty(line.getAssetIds())
                            && Math.abs(line.getAssetIds().length
                                    - new BigDecimal(line.getQuantity())
                            .floatValue()) > 0.0001) {
                        error += "OrderLineWS: number of assets != quantity - ";
                    }
                }

            }

            ItemBL itemBL = new ItemBL(line.getItemId());
            ItemDTO item = itemBL.getEntity();
            if (item != null) {
                UserBL userBL = new UserBL(order.getUserId());
                if (!item.isStandardAvailability()
                        && !item.getAccountTypeAvailability().contains(
                                userBL.getAccountType())) {

                    error += "OrderLineWS: The item is not available for the selected customer";

                }
            }

            if (error.length() > 0) {
                logger.debug("Error occurred processing order lines");
                throw new SessionInternalError(error);
            }
        }
        if (order.getParentOrder() != null) {
            validateOrder(order.getParentOrder(), alreadyValidated,
                    ordersWithChanges);
        }
        if (order.getChildOrders() != null) {
            for (OrderWS childOrder : order.getChildOrders()) {
                validateOrder(childOrder, alreadyValidated, ordersWithChanges);
            }
        }
    }

    private void validateHierarchy(OrderDTO orderDTO) {
        OrderHierarchyValidator validator = new OrderHierarchyValidator();
        validator.buildHierarchy(orderDTO);
        String error = validator.validate();
        if (error != null) {
            throw new SessionInternalError(
                    "Error in order hierarchy: " + error,
                    new String[] { error });
        }
    }

    private InvoiceDTO doCreateInvoice(Integer orderId) {
        try {
            BillingProcessBL process = new BillingProcessBL();
            InvoiceDTO invoice = process.generateInvoice(orderId, null, null,
                    getCallerId(), companyCurrentDate());
            // logger.debug("Invoice=== " +invoice);
            return invoice;
        } catch (Exception e) {
            logger.debug("WS - create invoice:", e);
            throw new SessionInternalError(
                    "Error while generating a new invoice");
        }
    }

    private PaymentDTOEx doPayInvoice(InvoiceDTO invoice,
            PaymentInformationDTO creditCardInstrument)
    {

        if (invoice.getBalance() == null
                || BigDecimal.ZERO.compareTo(invoice.getBalance()) >= 0) {
            logger.warn("Can not pay invoice: {}, balance: {}", invoice.getId(),
                    invoice.getBalance());
            return null;
        }

        IPaymentSessionBean payment = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        PaymentDTOEx paymentDto = new PaymentDTOEx();
        paymentDto.setIsRefund(0);
        paymentDto.setAmount(invoice.getBalance());
        paymentDto.setCurrency(new CurrencyDAS().find(invoice.getCurrency().getId()));
        paymentDto.setUserId(invoice.getBaseUser().getUserId());

        creditCardInstrument.setPaymentMethod(new PaymentMethodDAS()
        .find(com.sapienter.jbilling.common.Util
                .getPaymentMethod(new PaymentInformationBL()
                .getCharMetaFieldByType(creditCardInstrument,
                        MetaFieldType.PAYMENT_CARD_NUMBER))));

        paymentDto.getPaymentInstruments().clear();
        paymentDto.getPaymentInstruments().add(creditCardInstrument);

        paymentDto.setPaymentDate(companyCurrentDate());

        // make the call
        payment.processAndUpdateInvoice(paymentDto, invoice.getId(),getCallerId());

        return paymentDto;
    }

    /**
     * Conveniance method to find a credit card
     */
    private PaymentInformationDTO getCreditCard(Integer userId) {
        logger.debug("Finding credit card for user [{}]", userId);
        if (userId == null) {
            return null;
        }

        PaymentInformationDTO result = null;
        try {
            UserBL user = new UserBL(userId);
            Integer entityId = user.getEntityId(userId);
            if (user.getDto().getPaymentInstruments().size() > 0) {
                // find it
                PaymentDTOEx paymentDto = PaymentBL.findPaymentInstrument(
                        entityId, userId);
                // it might have a credit card, but it might not be valid or
                // just not found by the plug-in
                if (paymentDto != null) {
                    logger.debug("Found payment [{}] instruments", paymentDto
                            .getPaymentInstruments().size());
                    result = new PaymentInformationBL()
                    .findCreditCard(paymentDto.getPaymentInstruments());
                    logger.debug("Found credit card {}",
                            result != null ? result.getId() : result);
                }
            }
        } catch (Exception e) { // forced by checked exceptions :(
            logger.error("WS - finding a credit card", e);
            throw new SessionInternalError(
                    "Error finding a credit card for user: " + userId);
        }

        return result;
    }

    private OrderWS doCreateOrder(OrderWS order, OrderChangeWS[] orderChanges,
            boolean create)  {
        logger.debug("Entering doCreateOrder()");
        validateOrder(order, orderChanges, false);

        // do some transformation from WS to DTO
        Map<OrderWS, OrderDTO> wsToDtoOrdersMap = new HashMap<OrderWS, OrderDTO>();
        Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap = new HashMap<OrderLineWS, OrderLineDTO>();
        OrderBL orderBL = new OrderBL();
        OrderDTO dto = orderBL.getDTO(order, wsToDtoOrdersMap, wsToDtoLinesMap);

        OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(dto);
        List<OrderChangeDTO> orderChangeDtos = new LinkedList<OrderChangeDTO>();
        convertOrderChangeWsToDto(orderChanges, orderChangeDtos, null,
                wsToDtoOrdersMap, wsToDtoLinesMap);

        Date onDate = com.sapienter.jbilling.common.Util
                .truncateDate(TimezoneHelper.currentDateForTimezone(this.getCompany().getTimezone()));
        OrderDTO targetOrder = OrderBL.updateOrdersFromDto(null, rootOrder);
        Map<OrderLineDTO, OrderChangeDTO> appliedChanges = OrderChangeBL
                .applyChangesToOrderHierarchy(targetOrder, orderChangeDtos,
                        onDate, true, getCallerCompanyId());

        // get the info from the caller
        Integer executorId = getCallerId();
        Integer entityId = getCallerCompanyId();
        UserBL userBl = new UserBL(order.getUserId());
        Integer languageId = userBl.getEntity().getLanguageIdField();

        // linked set to preserve hierarchy order in collection, from root to
        // child
        LinkedHashSet<OrderDTO> ordersForUpdate = OrderHelper
                .findOrdersInHierarchyFromRootToChild(targetOrder);

        // process the lines and let the items provide the order line details
        logger.debug("Processing order lines");
        // recalculate from root order to child orders
        for (OrderDTO updatedOrder : ordersForUpdate) {
            OrderBL bl = new OrderBL();
            List<PricingField> pricingFields = updatedOrder.getPricingFields();
            bl.processLines(
                    updatedOrder,
                    languageId,
                    entityId,
                    updatedOrder.getBaseUserByUserId().getId(),
                    updatedOrder.getCurrencyId(),
                    updatedOrder.getPricingFields() != null ? PricingField
                            .setPricingFieldsValue(pricingFields
                                    .toArray(new PricingField[pricingFields
                                                              .size()])) : null);
            bl.set(updatedOrder);
            bl.recalculate(entityId);
        }
        OrderDTO inputOrder = wsToDtoOrdersMap.get(order);
        OrderWS resultWs = null;
        if (create) {
            logger.debug("creating order");
            OrderChangeWS[] oldOrderChanges = getOrderChanges(order.getId());
            // validate final hierarchy
            validateHierarchy(targetOrder);
            validateDiscountLines(rootOrder, orderChangeDtos);
            Integer id = orderBL.create(entityId, executorId, inputOrder,
                    appliedChanges);
            orderBL.validateDiscountLines();
            // save order changes
            OrderChangeBL orderChangeBL = new OrderChangeBL();
            if (appliedChanges != null) {
                orderChangeDtos.addAll(appliedChanges.values().stream()
                        .filter(orderChange -> !orderChangeDtos.contains(orderChange))
                        .collect(Collectors.toList()));
            }
            //synchronize order changes with database state
            orderChangeBL.updateOrderChanges(entityId, orderChangeDtos,
                    new LinkedList<Integer>(), onDate);

            orderBL.set(id);
            resultWs = orderBL.getWS(languageId);
            if (null != id) {
                Integer userId = order.getUserId();
                Integer companyId = new UserBL(userId).getDto().getEntity().getId();
                OrderChangeWS[] newOrderChanges = getOrderChanges(id);
                EventManager.process(new AssetStatusUpdateEvent(userId, newOrderChanges, oldOrderChanges, companyId));
            }
        } else {
            orderBL.set(inputOrder);
            resultWs = getWSFromOrder(orderBL, languageId);
        }
        // create discount order lines in case of amount and percentage
        // discounts
        // applied at order level or line level, adds to order.orderLines
        createDiscountOrderLines(resultWs, languageId);

        resultWs = ratePlanBundledItems(resultWs, userBl.getEntity());
        return resultWs;
    }

    /**
     * Convert input array of order change ws objects to dto object, collect
     * deleted changes ids
     *
     * @param orderChanges
     *            input order change ws objects
     * @param changeDtos
     *            output order change dto objects
     * @param deletedChanges
     *            output deleted order change ids
     * @param wsToDtoOrdersMap
     *            map from ws to dto for orders
     * @param wsToDtoLinesMap
     *            map from ws to dto for order lines
     */
    private void convertOrderChangeWsToDto(OrderChangeWS[] orderChanges,
            List<OrderChangeDTO> changeDtos, List<Integer> deletedChanges,
            Map<OrderWS, OrderDTO> wsToDtoOrdersMap,
            Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap) {
        if (orderChanges != null) {
            // process parent changes before child
            Arrays.sort(orderChanges, new Comparator<OrderChangeWS>() {
                @Override
                public int compare(OrderChangeWS left, OrderChangeWS right) {
                    if (left.getParentOrderChange() == null
                            && left.getParentOrderChangeId() == null
                            && (right.getParentOrderChange() != null || right
                            .getParentOrderChangeId() != null)) {
                        return -1;
                    }
                    if ((left.getParentOrderChange() != null || left
                            .getParentOrderChangeId() != null)
                            && right.getParentOrderChange() == null
                            && right.getParentOrderChangeId() == null) {
                        return 1;
                    }
                    return 0;
                }
            });
            Map<OrderChangeWS, OrderChangeDTO> wsToDtoChangesMap = new HashMap<OrderChangeWS, OrderChangeDTO>();
            for (OrderChangeWS change : orderChanges) {
                if (change.getId() != null && change.getDelete() > 0) {
                    if (deletedChanges != null) {
                        deletedChanges.add(change.getId());
                    }
                } else {
                    OrderChangeDTO orderChange = OrderChangeBL.getDTO(change,
                            wsToDtoChangesMap, wsToDtoOrdersMap,
                            wsToDtoLinesMap);
                    orderChange.setUser(userDAS.find(getCallerId()));
                    orderChange.setStatus(null);
                    changeDtos.add(orderChange);
                }
            }
        }
    }

    /**
     * This function will set adjusted price on order lines post discount. It
     * will be used to update orderWs.lines array which excludes any lines for
     * plan bundle items and will contain lines for products and plan
     * subscription item.
     *
     * @param order
     * @param languageId
     * @return
     */
    private void createDiscountOrderLines(OrderWS order, Integer languageId) {

        // TODO List<OrderLineWS> discountOrderLines = new
        // ArrayList<OrderLineWS>(0);
        BigDecimal adjustedTotal = order.getTotalAsDecimal();

        for (DiscountLineWS discountLine : order.getDiscountLines()) {
            DiscountDTO discount = new DiscountBL(discountLine.getDiscountId())
            .getEntity();
            if (discount != null
                    && (discount.isAmountBased() || discount
                            .isPercentageBased())) {
                if (discountLine.isOrderLevelDiscount()
                        || discountLine.isProductLevelDiscount()) {

                    BigDecimal discountAmount = BigDecimal.ZERO;

                    if (discountLine.isProductLevelDiscount()) {

                        ItemDTO itemDto = new ItemDAS().find(discountLine
                                .getItemId());
                        // need to pick up line level amount to find out
                        // discount amount
                        for (OrderLineWS orderLine : order.getOrderLines()) {
                            if (orderLine.getItemId() == itemDto.getId()) {
                                // as 1st iteration, we go by taking 1st line
                                // with matching item
                                if (discount.isPercentageBased()) {
                                    discountAmount = orderLine
                                            .getAmountAsDecimal()
                                            .multiply(discount.getRate())
                                            .divide(new BigDecimal(100));
                                } else {
                                    // amount based
                                    discountAmount = discount.getRate();
                                }
                                BigDecimal adjustedPrice = orderLine
                                        .getAmountAsDecimal().subtract(
                                                discountAmount);
                                adjustedPrice = adjustedPrice.setScale(
                                        Constants.BIGDECIMAL_SCALE_STR,
                                        Constants.BIGDECIMAL_ROUND);
                                orderLine.setAdjustedPrice(adjustedPrice);
                                break;
                            }
                        }

                    } else if (discountLine.isOrderLevelDiscount()) {

                        if (discount.isPercentageBased()) {
                            discountAmount = order.getTotalAsDecimal()
                                    .multiply(discount.getRate())
                                    .divide(new BigDecimal(100));
                        } else {
                            // amount based
                            discountAmount = discount.getRate();
                        }
                        // 4328 - discount amount is set in negative
                        discountLine.setDiscountAmount(discountAmount.negate()
                                .toString());
                    }

                    // keep decrementing adjustedTotal for each discount at
                    // order/product line level
                    adjustedTotal = adjustedTotal.subtract(discountAmount);
                    adjustedTotal = adjustedTotal.setScale(
                            Constants.BIGDECIMAL_SCALE_STR,
                            Constants.BIGDECIMAL_ROUND);
                    order.setAdjustedTotal(adjustedTotal);
                }
            }
        }
    }

    /**
     * This function will get the OrderWS[] populated in the OrderWS parameter
     * for bundleItems. It will look at planBundleItems from OrderWS, loop on
     * the same and rate each line looking at the item rate or percentage.
     *
     * @param ws
     * @return
     */
    private OrderWS ratePlanBundledItems(OrderWS ws, UserDTO user) {

        List<OrderLineWS> retValue = new ArrayList<OrderLineWS>();

        OrderLineWS temp = null;
        for (OrderLineWS line : ws.getOrderLines()) {

            ItemDTO item = new ItemDAS().find(line.getItemId());

            if (item != null && item.hasPlanItems()) {

                // determined that this is plan
                List<PlanItemDTO> planBundledItems = new PlanDAS()
                .findPlanByItemId(line.getItemId()).getPlanItems();
                for (PlanItemDTO planItem : planBundledItems) {

                    ItemDTO bundleItem = planItem.getItem();

                    OrderLineWS orderLine = new OrderLineWS();
                    orderLine.setItemId(planItem.getItem().getId());
                    BigDecimal bundleItemQuantity = planItem.getBundle()
                            .getQuantity()
                            .multiply(line.getQuantityAsDecimal());
                    orderLine.setQuantity(bundleItemQuantity);

                    bundleItem = new ItemBL(bundleItem.getId()).getDTO(user
                            .getLanguageIdField(), user.getId(), user
                            .getCompany().getId(), user.getCurrencyId());

                    // First we take all plan items with rate.
                    PriceModelDTO priceModelDTO = bundleItem.getPrice(
                            companyCurrentDate(), getCallerCompanyId());
                    if (priceModelDTO == null
                            || !(priceModelDTO.getType() == PriceModelStrategy.LINE_PERCENTAGE)) {

                        BigDecimal bundleItemPrice = null != planItem.getPrice(
                                companyCurrentDate()).getRate() ? planItem.getPrice(
                                        companyCurrentDate()).getRate() : (null != bundleItem
                                        .getPrice() ? bundleItem.getPrice()
                                                : BigDecimal.ZERO);
                                        BigDecimal bundleItemAmount = bundleItemPrice
                                                .multiply(bundleItemQuantity);
                                        orderLine.setPrice(bundleItemPrice);
                                        orderLine.setAmount(bundleItemAmount);

                                        BigDecimal adjustedPrice = bundleItemAmount;
                                        // one bundle item id can have multiple discounts, so
                                        // check all discount lines and adjust the price
                                        if (ws.hasDiscountLines()) {

                                            for (DiscountLineWS dline : ws.getDiscountLines()) {
                                                if (dline.isPlanItemLevelDiscount()) {
                                                    if (dline.getPlanItemId().intValue() == planItem
                                                            .getId().intValue()) {
                                                        DiscountDTO discount = new DiscountDAS()
                                                        .find(dline.getDiscountId());
                                                        if (discount.isAmountBased()) {
                                                            adjustedPrice = adjustedPrice
                                                                    .subtract(discount
                                                                            .getRate()
                                                                            .multiply(
                                                                                    bundleItemQuantity));
                                                            orderLine
                                                            .setAdjustedPrice(adjustedPrice
                                                                    .setScale(
                                                                            Constants.BIGDECIMAL_SCALE_STR,
                                                                            Constants.BIGDECIMAL_ROUND)
                                                                            .toString());
                                                        } else if (discount.isPercentageBased()) {
                                                            adjustedPrice = adjustedPrice
                                                                    .subtract((bundleItemAmount
                                                                            .multiply(discount
                                                                                    .getRate()
                                                                                    .divide(new BigDecimal(
                                                                                            100)))));
                                                            orderLine
                                                            .setAdjustedPrice(adjustedPrice
                                                                    .setScale(
                                                                            Constants.BIGDECIMAL_SCALE_STR,
                                                                            Constants.BIGDECIMAL_ROUND)
                                                                            .toString());
                                                        }
                                                    }
                                                }
                                            }
                                        }

                    } else {
                        // percentage item, only set the price, we dont have
                        // amount and hence adjusted price here.
                        orderLine.setPrice(bundleItem.getPrice());
                    }

                    retValue.add(orderLine);
                }
            }
        }

        OrderLineWS[] bundleItems = new OrderLineWS[1];
        ws.setPlanBundledItems(retValue.toArray(bundleItems));
        return ws;
    }

    private OrderWS getWSFromOrder(OrderBL bl, Integer languageId) {
        OrderWS retValue = bl.getWS(languageId);
        // todo: clear invoices to fit original code of current method to
        // OrderBL.convertToWS
        // possible we can remove this array cleaning
        retValue.setGeneratedInvoices(new InvoiceWS[] {});
        return retValue;
    }

    private InvoiceDTO findInvoice(Integer invoiceId) {
        return new InvoiceDAS().findNow(invoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    // TODO: This method is not secured or in a jUnit test
    public InvoiceWS getLatestInvoiceByItemType(Integer userId,
            Integer itemTypeId)  {
        InvoiceWS retValue = null;
        try {
            if (userId == null) {
                return null;
            }
            InvoiceBL bl = new InvoiceBL();
            Integer invoiceId = bl.getLastByUserAndItemType(userId, itemTypeId);
            if (invoiceId != null) {
                retValue = bl.getWS(new InvoiceDAS().find(invoiceId));
            }
            return retValue;
        } catch (Exception e) { // forced by SQLException
            logger.error("Exception in web service: getting latest invoice"
                    + " for user " + userId, e);
            throw new SessionInternalError("Error getting latest invoice");
        }
    }

    /**
     * Return 'number' most recent invoices that contain a line item with an
     * item of the given item type.
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getLastInvoicesByItemType(Integer userId,
            Integer itemTypeId, Integer number)  {
        if (userId == null || itemTypeId == null || number == null) {
            return null;
        }

        InvoiceBL bl = new InvoiceBL();
        return bl.getManyByItemTypeWS(userId, itemTypeId, number);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS getLatestOrderByItemType(Integer userId, Integer itemTypeId)
    {
        if (userId == null) {
            throw new SessionInternalError("User id can not be null");
        }
        if (itemTypeId == null) {
            throw new SessionInternalError("itemTypeId can not be null");
        }
        OrderWS retValue = null;
        // get the info from the caller
        Integer languageId = getCallerLanguageId();

        // now get the order
        OrderBL bl = new OrderBL();
        Integer orderId = bl.getLatestByItemType(userId, itemTypeId);
        if (orderId != null) {
            bl.set(orderId);
            retValue = bl.getWS(languageId);
        }
        return retValue;
    }

    // TODO: This method is not secured or in a jUnit test
    @Override
    @Transactional(readOnly=true)
    public Integer[] getLastOrdersByItemType(Integer userId, Integer itemTypeId, Integer number)
    {
        if (userId == null || number == null) {
            return null;
        }
        OrderBL order = new OrderBL();
        return order.getListIdsByItemType(userId, itemTypeId, number);
    }

    @Override
    @Transactional(readOnly = true)
    public String isUserSubscribedTo(Integer userId, Integer itemId) {
        BigDecimal quantity = orderDAS.findIsUserSubscribedTo(userId, itemId);
        return quantity != null ? quantity.toString() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getUserItemsByCategory(Integer userId, Integer categoryId) {
        return orderDAS.findUserItemsByCategory(userId, categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDTOEx[] getItemByCategory(Integer itemTypeId) {
        return new ItemBL().getAllItemsByType(itemTypeId, getCallerCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public ItemTypeWS getItemCategoryById(Integer id) {
        ItemTypeBL itemTypeBL = new ItemTypeBL();
        ItemTypeDTO itemTypeDTO = itemTypeBL.getById(id, getCallerCompanyId(),
                true);
        if(itemTypeDTO==null) {
            return null;
        }
        return ItemTypeBL.toWS(itemTypeDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemTypeWS[] getAllItemCategories() {
        return getAllItemCategoriesByEntityId(getCallerCompanyId());
    }

    @Override
    public ValidatePurchaseWS validatePurchase(Integer userId, Integer itemId,
            String fields) {
        Integer[] itemIds = null;
        if (itemId != null) {
            itemIds = new Integer[] { itemId };
        }

        String[] fieldsArray = null;
        if (fields != null) {
            fieldsArray = new String[] { fields };
        }

        return doValidatePurchase(userId, itemIds, fieldsArray);
    }

    @Override
    public ValidatePurchaseWS validateMultiPurchase(Integer userId,
            Integer[] itemIds, String[] fields) {

        return doValidatePurchase(userId, itemIds, fields);
    }

    private ValidatePurchaseWS doValidatePurchase(Integer userId,
            Integer[] itemIds, String[] fields) {

        if (userId == null || (itemIds == null && fields == null)) {
            return null;
        }

        UserBL user = new UserBL(userId);

        List<List<PricingField>> fieldsList = null;
        if (fields != null) {
            fieldsList = new ArrayList<List<PricingField>>(fields.length);
            for (int i = 0; i < fields.length; i++) {
                fieldsList.add(JArrays.toArrayList(PricingField
                        .getPricingFieldsValue(fields[i])));
            }
        }

        List<Integer> itemIdsList = null;
        List<BigDecimal> prices = new ArrayList<BigDecimal>();
        List<ItemDTO> items = new ArrayList<ItemDTO>();

        if (itemIds != null) {
            itemIdsList = JArrays.toArrayList(itemIds);
        } else if (fields != null) {
            itemIdsList = new LinkedList<Integer>();

            for (List<PricingField> pricingFields : fieldsList) {
                try {
                    // Since there is no item, run the mediation process rules
                    // to create line/s.

                    //TODO MODULARIZATION: THIS LOGIC LAUNCH OLD MEDIATION 2 JOBS...
                    // fields need to be in records
                    CallDataRecord record = new CallDataRecord();
                    for (PricingField field : pricingFields) {
                        record.addField(field, false); // don't care about isKey
                    }
                    PluggableTaskManager<IMediationProcess> tm = new PluggableTaskManager<>(
                            getCallerCompanyId(),
                            Constants.PLUGGABLE_TASK_MEDIATION_PROCESS);

                    IMediationProcess processTask = tm.getNextClass();

                    MediationStepResult result = new MediationStepResult();
                    result.setUserId(user.getEntity().getUserId());
                    result.setCurrencyId(user.getEntity().getCurrencyId());
                    result.setEventDate(companyCurrentDate());
                    result.setPersist(false);

                    processTask.process(record, result);

                    // from the lines, get the items and prices
                    for (Object lineObject : result.getDiffLines()) {
                        OrderLineDTO line = (OrderLineDTO) lineObject;
                        items.add(new ItemBL(line.getItemId()).getEntity());
                        prices.add(line.getAmount());
                    }
                    //TODO MODULARIZATION: THIS LOGIC LAUNCH OLD MEDIATION 2 JOBS...
                } catch (Exception e) {
                    // log stacktrace
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.close();
                    logger.error("Validate Purchase error: {}\n{}",
                            e.getMessage(), sw.toString());

                    ValidatePurchaseWS result = new ValidatePurchaseWS();
                    result.setSuccess(false);
                    result.setAuthorized(false);
                    result.setQuantity(BigDecimal.ZERO);
                    result.setMessage(new String[] { "Error: " + e.getMessage() });

                    return result;
                }
            }
        } else {
            return null;
        }

        // find the prices first
        // this will do nothing if the mediation process was uses. In that case
        // the itemIdsList will be empty
        int itemNum = 0;
        for (Integer itemId : itemIdsList) {
            ItemBL item = new ItemBL(itemId);

            if (fieldsList != null && !fieldsList.isEmpty()) {
                int fieldsIndex = itemNum;
                // just get first set of fields if only one set
                // for many items
                if (fieldsIndex > fieldsList.size()) {
                    fieldsIndex = 0;
                }
                item.setPricingFields(fieldsList.get(fieldsIndex));
            }

            // todo: validate purchase should include the quantity purchased for
            // validations
            BigDecimal price = item.getPrice(userId, BigDecimal.ONE,
                    getCallerCompanyId());
            // if the price can not be determined than use 0 since it will
            // contribute to the total amount of purchase and will not affect
            // validation
            prices.add(null != price ? price : BigDecimal.ZERO);
            items.add(item.getEntity());
            itemNum++;
        }

        ValidatePurchaseWS ret = new UserBL(userId).validatePurchase(items,
                prices, fieldsList);
        return ret;
    }

    /**
     * Return the item id for the product with the productCode, if this is
     * visible from the company who made the call or if it is a global product
     *
     * @param productCode
     * @return
     * @
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getItemID(String productCode)  {
        Integer companyId = getCallerCompanyId();
        Integer parentCompany = new CompanyDAS().getParentCompanyId(companyId);
        ItemDAS itemDAS = new ItemDAS();
        ItemDTO itemFound = itemDAS.findItemByInternalNumber(productCode,
                companyId);
        if (itemFound != null
                && itemDAS.isProductVisibleToCompany(itemFound.getId(),
                        companyId, parentCompany)) {
            return itemFound.getId();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getAuthPaymentType(Integer userId) {
        IUserSessionBean sess = Context.getBean(Context.Name.USER_SESSION);
        return sess.getAuthPaymentType(userId);
    }

    @Override
    public void setAuthPaymentType(Integer userId, Integer autoPaymentType,
            boolean use)  {
        IUserSessionBean sess = Context.getBean(Context.Name.USER_SESSION);
        sess.setAuthPaymentType(userId, autoPaymentType, use);
    }

    @Override
    @Transactional(readOnly = true)
    public AgeingWS[] getAgeingConfiguration(Integer languageId) {
        return getAgeingConfigurationWithCollectionType(languageId, CollectionType.REGULAR);
    }

    @Override
    public void saveAgeingConfiguration(AgeingWS[] steps, Integer languageId) {
        saveAgeingConfigurationWithCollectionType(steps, languageId, CollectionType.REGULAR);
    }

    /*
     * Billing process
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isBillingRunning (Integer entityId) {
        return billingProcessSession.isBillingRunning(entityId);
    }

    @Override
    public void triggerBillingAsync (final Date runDate) {
        billingProcessSession.triggerAsync(runDate, getCallerCompanyId());
    }

    @Override
    @Transactional( propagation = Propagation.NOT_SUPPORTED )
    public void triggerCollectionsAsync(final Date runDate) {
        final Integer companyId = getCallerCompanyId();
        new Thread(() -> billingProcessSession.reviewUsersStatus(companyId, runDate)).start();
    }

    @Override
    public boolean triggerBilling(Date runDate) {
        return billingProcessSession.trigger(runDate, getCallerCompanyId());
    }

    @Override
    public void triggerAgeing(Date runDate) {
        billingProcessSession.reviewUsersStatus(getCallerCompanyId(), runDate);
    }

    /**
     * Returns true if the ageing process is currently running for the caller's
     * entity, false if not.
     *
     * @return true if ageing process is running, false if not
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAgeingProcessRunning() {
        return billingProcessSession.isAgeingProcessRunning(getCallerCompanyId());
    }

    /**
     * Returns the status of the last run (or currently running) billing
     * process.
     *
     * @return billing process status
     */
    @Override
    @Transactional(readOnly = true)
    public ProcessStatusWS getBillingProcessStatus() {
        return billingProcessSession.getBillingProcessStatus(getCallerCompanyId());
    }

    /**
     * Returns the status of the last run (or currently running) ageing process.
     *
     * That the ageing process currently does not report a start date, end date,
     * or process id. The status returned by this method will only report the
     * RUNNING/FINISHED/FAILED state of the process.
     *
     * @return ageing process status
     */
    @Override
    @Transactional(readOnly = true)
    public ProcessStatusWS getAgeingProcessStatus() {
        return billingProcessSession.getAgeingProcessStatus(getCallerCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public BillingProcessConfigurationWS getBillingProcessConfiguration() {
        BillingProcessConfigurationDTO configuration = billingProcessSession.getConfigurationDto(getCallerCompanyId());
        return ConfigurationBL.getWS(configuration);
    }

    @Override
    public Integer createUpdateBillingProcessConfiguration(
            BillingProcessConfigurationWS ws)  {

        // validation
        if (!ConfigurationBL.validate(ws)) {
            throw new SessionInternalError("Error: Invalid Next Run Date.");
        }
        BillingProcessConfigurationDTO dto = ConfigurationBL.getDTO(ws);

        return billingProcessSession.createUpdateConfiguration(getCallerId(), dto);
    }

    /**
     * This method creates or updates the commission process configuration.
     *
     * @param ws
     * @return
     * @
     */
    @Override
    public Integer createUpdateCommissionProcessConfiguration(
            CommissionProcessConfigurationWS ws)  {

        // validation
        try {
            if (!CommissionProcessConfigurationBL.validate(ws)) {
                throw new SessionInternalError(
                        "Error: Invalid configuration",
                        new String[] { "partner.error.commissionProcess.invalidDate" }, HttpStatus.SC_BAD_REQUEST);
            }
        } catch (SessionInternalError e) {
            throw e;
        }

        CommissionProcessConfigurationDTO dto = CommissionProcessConfigurationBL.getDTO(ws);

        return CommissionProcessConfigurationBL.createUpdate(dto);
    }

    /**
     * Triggers the partner commission process.
     */
    @Override
    public void calculatePartnerCommissions() {
        IUserSessionBean userSession = Context
                .getBean(Context.Name.USER_SESSION);
        userSession.calculatePartnerCommissions(getCallerCompanyId());
    }

    /**
     * Triggers the partner commission process asynchronously.
     */
    @Override
    public void calculatePartnerCommissionsAsync() {
        final Integer companyId = getCallerCompanyId();
        Thread t = new Thread(new Runnable() {
            IUserSessionBean userSession = Context
                    .getBean(Context.Name.USER_SESSION);

            @Override
            public void run() {
                userSession.calculatePartnerCommissions(getCallerCompanyId());
            }
        });

        t.start();
    }

    /**
     * Checks if the partner commission process is running or not.
     *
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPartnerCommissionRunning() {
        IUserSessionBean userSession = Context
                .getBean(Context.Name.USER_SESSION);
        return userSession.isPartnerCommissionRunning(getCallerCompanyId());
    }

    /**
     * Gets all the partner commission runs
     *
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public CommissionProcessRunWS[] getAllCommissionRuns() {
        List<CommissionProcessRunDTO> commissionProcessRuns = new CommissionProcessRunDAS()
        .findAllByEntity(new CompanyDAS().find(getCallerCompanyId()));

        if (commissionProcessRuns != null && commissionProcessRuns.size() > 0) {
            CommissionProcessRunWS[] commissionProcessRunWSes = new CommissionProcessRunWS[commissionProcessRuns
                                                                                           .size()];
            int index = 0;
            for (CommissionProcessRunDTO commissionProcessRun : commissionProcessRuns) {
                commissionProcessRunWSes[index] = CommissionProcessConfigurationBL.getCommissionProcessRunWS(
                        commissionProcessRun);
                index++;
            }
            return commissionProcessRunWSes;
        } else {
            return null;
        }
    }

    /**
     * Gets all the commissions for a given processRunId
     *
     * @param processRunId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public CommissionWS[] getCommissionsByProcessRunId(Integer processRunId) {
        CommissionProcessRunDTO commissionProcessRunDTO = new CommissionProcessRunDAS().findNow(processRunId);
        if(commissionProcessRunDTO == null) {
            throw new SessionInternalError(
                    "Error: Process Run not found",
                    new String[] { "partner.error.commissionProcess.run.not.found" }, HttpStatus.SC_NOT_FOUND);
        }
        List<CommissionDTO> commissions = new CommissionDAS()
        .findAllByProcessRun(
                commissionProcessRunDTO,
                getCallerCompanyId());

        if (commissions != null && commissions.size() > 0) {
            CommissionWS[] commissionWSes = new CommissionWS[commissions.size()];
            int index = 0;
            for (CommissionDTO commission : commissions) {
                commissionWSes[index] = CommissionProcessConfigurationBL.getCommissionWS((commission));
                index++;
            }
            return commissionWSes;
        } else {
            return new CommissionWS[0];
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BillingProcessWS getBillingProcess(Integer processId) {

        if (null == processId) {
            throw new SessionInternalError("Process id must have non-null value", HttpStatus.SC_BAD_REQUEST);
        }
        BillingProcessDTOEx dto = billingProcessSession.getDto(processId, getCallerLanguageId());
        return BillingProcessBL.getWS(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getLastBillingProcess()  {
        return billingProcessSession.getLast(getCallerCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderProcessWS[] getOrderProcesses(Integer orderId) {
        OrderDTO order = new OrderBL(orderId).getDTO();
        if (order == null) {
            return new OrderProcessWS[0];
        }
        List<OrderProcessWS> ws = new ArrayList<>();
        for (OrderProcessDTO process : order.getOrderProcesses()) {
            ws.add(OrderBL.getOrderProcessWS(process));
        }
        return ws.toArray(new OrderProcessWS[ws.size()]);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderProcessWS[] getOrderProcessesByInvoice(Integer invoiceId) {
        InvoiceDTO invoice = new InvoiceBL(invoiceId).getDTO();

        if (invoice == null) {
            return new OrderProcessWS[0];
        }

        List<OrderProcessWS> ws = new ArrayList<OrderProcessWS>(invoice
                .getOrderProcesses().size());
        for (OrderProcessDTO process : invoice.getOrderProcesses()) {
            ws.add(OrderBL.getOrderProcessWS(process));
        }

        return ws.toArray(new OrderProcessWS[ws.size()]);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingProcessWS getReviewBillingProcess() {
        BillingProcessDTOEx dto = billingProcessSession.getReviewDto(getCallerCompanyId(), getCallerLanguageId());
        return BillingProcessBL.getWS(dto);
    }

    @Override
    public BillingProcessConfigurationWS setReviewApproval(Boolean flag)
    {

        if (null == flag) {
            throw new SessionInternalError("Review approval flag must have non-null value", HttpStatus.SC_BAD_REQUEST);
        }
        BillingProcessConfigurationDTO dto = billingProcessSession.setReviewApproval(getCallerId(), getCallerCompanyId(), flag);

        return ConfigurationBL.getWS(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getBillingProcessGeneratedInvoices(Integer processId) {

        // todo: IBillingProcessSessionBean#getGeneratedInvoices() should have a
        // proper generic return type
        @SuppressWarnings("unchecked")
        Collection<InvoiceDTO> invoices = billingProcessSession.getGeneratedInvoices(processId);

        List<Integer> ids = new ArrayList<Integer>(invoices.size());
        for (InvoiceDTO invoice : invoices) {
            ids.add(invoice.getId());
        }
        return ids.toArray(new Integer[ids.size()]);
    }

    /*
     * Mediation process
     */
    @Override
    public void triggerMediation() {
        //TODO CAN WE TRIGGER ALL MEDIATION TOGETHER? PERFORMANCE ISSUES
        IMediationSessionBean mediationBean = Context.getBean(Context.Name.MEDIATION_SESSION);
        Integer callerCompanyId = getCallerCompanyId();
        List<MediationConfiguration> mediationConfigurations = mediationBean.getAllConfigurations(callerCompanyId, true);
        for(MediationConfiguration mediationConfiguration: mediationConfigurations){
            new Thread(() -> mediationService.launchMediation(callerCompanyId, mediationConfiguration.getId(), mediationConfiguration.getMediationJobLauncher())).start();
        }
    }

    /**
     * Triggers the mediation process for a specific configuration and returns
     * the mediation process id of the running process.
     *
     * @param cfgId
     *            mediation configuration id
     * @return mediation process id
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID triggerMediationByConfiguration(Integer cfgId) {
        IMediationSessionBean mediationBean = Context
                .getBean(Context.Name.MEDIATION_SESSION);
        MediationConfiguration configuration = mediationBean.getMediationConfiguration(cfgId);

        mediationService.launchMediation(getCallerCompanyId(), cfgId,
                configuration.getMediationJobLauncher());
        return mediationProcessService.getLastMediationProcessId(getCallerCompanyId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID launchMediation(Integer mediationCfgId, String jobName, File file) {
        mediationService.launchMediation(getCallerCompanyId(), mediationCfgId, jobName, file);
        return mediationProcessService.getLastMediationProcessId(getCallerCompanyId());
    }

    /**
     * Undo mediation for process id
     *
     * @
     */
    @Override
    public void undoMediation(UUID processId)  {
        OrderService orderService = Context.getBean(OrderService.BEAN_NAME);
        orderService.undoMediation(processId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMediationProcessRunning()  {
        return mediationProcessService.isMediationProcessRunning(getCallerCompanyId());
    }

    /**
     * Returns the status of the last run (or currently running) mediation
     * process.
     *
     * @return mediation process status
     */
    @Override
    @Transactional(readOnly = true)
    public ProcessStatusWS getMediationProcessStatus() {
        MediationProcessService processService = Context.getBean(MediationProcessService.BEAN_NAME);
        List<MediationProcess> latestMediationProcess =
                processService.findLatestMediationProcess(getCallerCompanyId(), 0, 1);
        if (!latestMediationProcess.isEmpty()) {
            if (mediationService.mediationProcessStatus().equals(MediationProcessStatus.COMPLETED)) {
                return new ProcessStatusWS(ProcessStatusWS.State.FINISHED, latestMediationProcess.get(0));
            }
            return new ProcessStatusWS(ProcessStatusWS.State.RUNNING, latestMediationProcess.get(0));
        }
        return null;
    }

    /**
     * Returns the mediation process for the given process id.
     *
     * @param mediationProcessId
     *            mediation process id
     * @return mediation process, or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public MediationProcess getMediationProcess(UUID mediationProcessId) {
        return mediationProcessService.getMediationProcess(mediationProcessId);
    }

    @Override
    @Transactional(readOnly = true)
    public MediationProcess[] getAllMediationProcesses() {
        //TODO MODULARIZATION: Check this logic
        return mediationProcessService.findLatestMediationProcess(getCallerCompanyId(), 0, 10000).toArray(new MediationProcess[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationEventsForOrder(Integer orderId) {
        return mediationService.getMediationRecordsForOrder(orderId).toArray(new JbillingMediationRecord[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationEventsForOrderDateRange(
            Integer orderId, Date startDate, Date endDate, int offset,
            int limit) {
        List<Filter> filters = new ArrayList<>();
        if (orderId != null) {
            filters.add(Filter.integer("orderId", FilterConstraint.EQ, orderId));
        }
        if (startDate != null && endDate != null) {
            filters.add(Filter.betweenDates("eventDate", startDate, endDate));
        }
        return mediationService.findMediationRecordsByFilters(offset, limit, filters).toArray(new JbillingMediationRecord[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationEventsForInvoice(
            Integer invoiceId) {
        InvoiceWS invoiceWS = getInvoiceWS(invoiceId);
        List<JbillingMediationRecord> records = new LinkedList<>();
        for (Integer orderId: invoiceWS.getOrders()) {
            records.addAll(mediationService.getMediationRecordsForOrder(orderId));
        }
        return records.toArray(new JbillingMediationRecord[0]);
    }

    /**
     * Returns the Mediation Records for a given mediation process id.
     *
     * @param mediationProcessId
     *            mediation process Id : mandatory field
     * @param page
     *            if provided then records next to the offset will be returned
     *            else return from the start.
     * @param size
     *            maximum number of records accepted
     * @param startDate
     *            filter by start date
     * @param endDate
     *            filter by end date
     * @return mediation records
     */
    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationRecordsByMediationProcess(
            UUID mediationProcessId, Integer page,
            Integer size, Date startDate, Date endDate) {
        List<Filter> filters = new ArrayList<>();
        if (mediationProcessId != null) {
            filters.add(Filter.uuid("processId", FilterConstraint.EQ, mediationProcessId));
        }
        if (startDate != null && endDate != null) {
            filters.add(Filter.betweenDates("eventDate", startDate, endDate));
        }
        return mediationService.findMediationRecordsByFilters(page, size, filters).toArray(new JbillingMediationRecord[0]);
    }

    /**
     * Returns the Mediation Records for a given status and cdrType
     *
     * @param mediationProcessId
     *            mediation process Id : mandatory field
     * @param page
     *            if provided then records next to the offset will be returned
     *            else return from the start.
     * @param size
     *            maximum number of records accepted
     * @param startDate
     *            filter by start date
     * @param endDate
     *            filter by end date
     * @param status
     *            if provided then filter using records status.
     * @param cdrType
     *            if provided then filter using records cdrType.
     *
     * @return mediation records
     */
    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationRecordsByStatusAndCdrType(
            UUID mediationProcessId, Integer page, Integer size, Date startDate,
            Date endDate, String status, String cdrType) {
        List<Filter> filters = new ArrayList<>();
        if (mediationProcessId != null) {
            filters.add(Filter.uuid("processId", FilterConstraint.EQ, mediationProcessId));
        }
        if (startDate != null && endDate != null) {
            filters.add(Filter.betweenDates("eventDate", startDate, endDate));
        }
        if (!StringUtils.isEmpty(status)) {
            filters.add(Filter.enumFilter("status", FilterConstraint.EQ, JbillingMediationRecordDao.STATUS.valueOf(status)));
        }
        if (!StringUtils.isEmpty(cdrType)) {
            filters.add(Filter.string("cdrType", FilterConstraint.EQ, cdrType));
        }
        return mediationService.findMediationRecordsByFilters(page, size, filters).toArray(new JbillingMediationRecord[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public RecordCountWS[] getNumberOfMediationRecordsByStatuses() {
        MediationProcessService processService = Context.getBean(MediationProcessService.BEAN_NAME);
        List<MediationProcess> latestMediationProcess = processService.findLatestMediationProcess(getCallerCompanyId(), 0, 1);
        if (CollectionUtils.isEmpty(latestMediationProcess)) {
            return new RecordCountWS[0];
        }
        MediationProcess process = latestMediationProcess.get(0);
        return getNumberOfMediationRecordsByStatusesByMediationProcess(process.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RecordCountWS[] getNumberOfMediationRecordsByStatusesByMediationProcess(
            UUID mediationProcessId) {
        List<JbillingMediationRecord> mediationRecordsForProcess = mediationService.getMediationRecordsForProcess(mediationProcessId);
        List<RecordCountWS> recordCountWSes = new ArrayList<>();
        for (JbillingMediationRecord.STATUS status: JbillingMediationRecord.STATUS.values()) {
            long count = mediationRecordsForProcess.stream().filter(mr -> mr.getStatus().equals(status)).count();
            recordCountWSes.add(new RecordCountWS(status.getId(), count));
        }
        long errorCount = mediationService.getMediationErrorRecordsForProcess(mediationProcessId).size();
        recordCountWSes.add(new RecordCountWS(Constants.MEDIATION_RECORD_STATUS_ERROR_DETECTED, errorCount));
        return recordCountWSes.toArray(new RecordCountWS[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public MediationConfigurationWS[] getAllMediationConfigurations() {
        IMediationSessionBean mediationBean = Context
                .getBean(Context.Name.MEDIATION_SESSION);

        List<MediationConfiguration> configurations = mediationBean
                .getAllConfigurations(getCallerCompanyId(), true);
        List<MediationConfigurationWS> ws = MediationConfigurationBL
                .getWS(configurations);
        return ws.toArray(new MediationConfigurationWS[ws.size()]);
    }

    @Override
    public Integer createMediationConfiguration(MediationConfigurationWS cfg) {
        IMediationSessionBean mediationBean = Context
                .getBean(Context.Name.MEDIATION_SESSION);
        MediationConfiguration dto = MediationConfigurationBL.getDTO(cfg);
        // force the calling company to be the owner of this mediation config
        dto.setEntityId(getCallerCompanyId());
        return mediationBean.createConfiguration(dto, getCallerCompanyId(),
                getCallerId());
    }

    @Override
    public Integer[] updateAllMediationConfigurations(
            List<MediationConfigurationWS> configurations)
    {

        for (MediationConfigurationWS configuration : configurations) {
            if (configuration.getName() == null
                    || configuration.getName().length() < 1) {
                throw new SessionInternalError(
                        "Name can not be empty.",
                        new String[] { "MediationConfigurationWS,name,validation.mediation.config.name.empty" }, HttpStatus.SC_BAD_REQUEST);
            } else if (configuration.getName().length() > 50) {
                throw new SessionInternalError(
                        "Name is more than 50 characters",
                        new String[] { "MediationConfigurationWS,name,validation.mediation.config.name.long" }, HttpStatus.SC_BAD_REQUEST);
            }
        }
        // update all configurations
        List<MediationConfiguration> dtos = MediationConfigurationBL
                .getDTO(configurations);
        List<MediationConfiguration> updated;
        try {
            IMediationSessionBean mediationBean = Context
                    .getBean(Context.Name.MEDIATION_SESSION);
            updated = mediationBean.updateAllConfiguration(dtos,
                    getCallerCompanyId(), getCallerId());
        } catch (InvalidArgumentException e) {
            throw new SessionInternalError(e);
        }

        // return list of updated ids
        List<Integer> ids = new ArrayList<Integer>(updated.size());
        for (MediationConfiguration cfg : updated) {
            ids.add(cfg.getId());
        }
        return ids.toArray(new Integer[ids.size()]);
    }

    @Override
    public void deleteMediationConfiguration(Integer cfgId)
    {

        IMediationSessionBean mediationBean = Context
                .getBean(Context.Name.MEDIATION_SESSION);
        try {
            mediationBean.delete(cfgId, getCallerCompanyId(), getCallerId());
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

    }

    /*
     * Provisioning
     */

    @Override
    public void triggerProvisioning() {
        IProvisioningProcessSessionBean provisioningBean = Context
                .getBean(Context.Name.PROVISIONING_PROCESS_SESSION);
        provisioningBean.trigger(getCallerCompanyId());
    }

    @Override
    public void updateOrderAndLineProvisioningStatus(Integer inOrderId,
            Integer inLineId, String result)  {
        IProvisioningProcessSessionBean provisioningBean = Context
                .getBean(Context.Name.PROVISIONING_PROCESS_SESSION);
        provisioningBean.updateProvisioningStatus(inOrderId, inLineId, result);
    }

    @Override
    public void updateLineProvisioningStatus(Integer orderLineId,
            Integer provisioningStatus)  {
        IProvisioningProcessSessionBean provisioningBean = Context
                .getBean(Context.Name.PROVISIONING_PROCESS_SESSION);
        provisioningBean.updateProvisioningStatus(orderLineId,
                provisioningStatus);
    }

    /*
     * Preferences
     */
    @Override
    public void updatePreferences(PreferenceWS[] prefList) {
        PreferenceBL bl = new PreferenceBL();
        for (PreferenceWS pref : prefList) {
            bl.createUpdateForEntity(getCallerCompanyId(), pref
                    .getPreferenceType().getId(), pref.getValue());
        }
    }

    @Override
    public void updatePreference(PreferenceWS preference) {

        // User InActive Management Feature: restrict maximum number of
        // in-active days to 90
        // defined in
        // Constants.MAX_VALUE_FOR_PREFERENCE_EXPIRE_INACTIVE_ACCOUNTS_IN_DAYS
        if (Constants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS.equals(preference
                .getPreferenceType().getId())
                && Constants.MAX_VALUE_FOR_PREFERENCE_EXPIRE_INACTIVE_ACCOUNTS_IN_DAYS
                .compareTo(Integer.parseInt(preference.getValue())) < 0) {
            logger.debug("Preference type : "
                    + preference.getPreferenceType().getId()
                    + " value obtained = "
                    + preference.getValue()
                    + " is greater then max value allowed ("
                    + Constants.MAX_VALUE_FOR_PREFERENCE_EXPIRE_INACTIVE_ACCOUNTS_IN_DAYS
                    + ")");
            String errorMessages[] = new String[1];
            errorMessages[0] = "PreferenceWS,value,preferences.update.max.value.error,"
                    + Constants.MAX_VALUE_FOR_PREFERENCE_EXPIRE_INACTIVE_ACCOUNTS_IN_DAYS;
            throw new SessionInternalError("Preference Type "
                    + preference.getPreferenceType().getId() + ": ",
                    errorMessages);
        }

        if (preference.getPreferenceType().getId() == Constants.PREFERENCE_PARTNER_DEFAULT_COMMISSION_TYPE && StringUtils.isNotBlank(preference.getValue().trim())) {
            preference.setValue(StringUtils.trim(preference.getValue().toUpperCase()));
            if(!(PartnerCommissionType.INVOICE.name().equals(preference.getValue()) ? true : (PartnerCommissionType.PAYMENT.name().equals(preference.getValue()) ? true : false))) {
                String errorMessages[] = new String[1];
                errorMessages[0] = "PreferenceWS,value,validation.error.agent.commisiontype.preference,"
                        + preference.getValue();
                throw new SessionInternalError("Error in preference value "
                        + preference.getValue() + ": ", errorMessages);
            }
        }

        if (preference.getPreferenceType().getId() == Constants.PREFERENCE_USE_BLACKLIST) {
            PluggableTaskDTO dto = ((PluggableTaskDAS) Context
                    .getBean(Context.Name.PLUGGABLE_TASK_DAS)).findNow(Integer
                            .valueOf(preference.getValue()));
            if (dto == null
                    || (dto != null && (dto.getType().getCategory().getId() != Constants.PLUGGABLE_TASK_PAYMENT || !dto
                    .getEntityId().equals(getCallerCompanyId())))) {
                String errorMessages[] = new String[1];
                errorMessages[0] = "PreferenceWS,value,validation.error.email.preference.use.blacklist,"
                        + preference.getValue();
                throw new SessionInternalError("Error in preference value "
                        + preference.getValue() + ": ", errorMessages);
            }
        }

        PreferenceBL preferenceBL = new PreferenceBL();
        preferenceBL.validatePreferenceValue(preference);

        preferenceBL.createUpdateForEntity(getCallerCompanyId(),
                preference.getPreferenceType().getId(), preference.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenceWS getPreference(Integer preferenceTypeId) {
        PreferenceDTO preference = null;
        try {
            preference = new PreferenceBL(getCallerCompanyId(), preferenceTypeId).getEntity();
        } catch (DataAccessException e) {
            /* ignore */
        }

        if (preference != null) {
            // return preference if set
            return PreferenceBL.getWS(preference);

        } else {
            // preference is not set, return empty
            PreferenceTypeDTO preferenceType = new PreferenceTypeDAS().findNow(preferenceTypeId);
            if (null == preferenceType) {
                throw new SessionInternalError(
                        "Preference type with id " + preferenceTypeId + " not found!", HttpStatus.SC_NOT_FOUND);
            }
            return PreferenceBL.getWS(preferenceType);
        }
    }

    /*
     * Currencies
     */
    @Override
    @Transactional(readOnly = true)
    public CurrencyWS[] getCurrencies() {
        CurrencyBL currencyBl = new CurrencyBL();

        CurrencyDTO[] currencies;
        try {
            currencies = currencyBl.getCurrencies(getCallerLanguageId(),
                    getCallerCompanyId());
        } catch (SQLException e) {
            throw new SessionInternalError(
                    "Exception fetching currencies for entity "
                            + getCallerCompanyId(), e);
        }

        // Id of the default currency for this entity
        Integer entityDefault = currencyBl
                .getEntityCurrency(getCallerCompanyId());

        // convert to WS
        List<CurrencyWS> ws = new ArrayList<CurrencyWS>(currencies.length);
        for (CurrencyDTO currency : currencies) {
            ws.add(CurrencyBL.getCurrencyWS(currency, (currency.getId() == entityDefault)));
        }

        return ws.toArray(new CurrencyWS[ws.size()]);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { CurrencyInUseSessionInternalError.class })
    public void updateCurrencies(CurrencyWS[] currencies) {
        ItemDAS itemDAS = new ItemDAS();
        PlanItemDAS planItemDAS = new PlanItemDAS();

        List<CurrencyWS> inActiveInUseCurrencies = new ArrayList<CurrencyWS>();

        Integer entityId = getCallerCompanyId();
        for (CurrencyWS currency : currencies) {

            if (!currency.getInUse()) {
                // This is a possible de-activation triggered by un-checking the
                // active flag on UI.
                Long inUseCount = 0l;

                Integer currencyId = currency.getId();

                // currency in use for users
                inUseCount += userDAS.findUserCountByCurrencyAndEntity(
                        currencyId, entityId);

                // currrency in use for orders
                inUseCount += orderDAS.findOrderCountByCurrencyAndEntity(
                        currencyId, entityId);

                // currrency in use for products and plans (plan is a product,
                // so no separate handling required)
                inUseCount += itemDAS.findProductCountByCurrencyAndEntity(
                        currencyId, entityId);

                // currrency in use for planItem
                inUseCount += planItemDAS.findPlanItemCountByCurrencyAndEntity(
                        currencyId, entityId);

                if (inUseCount > 0) {
                    logger.debug("Currency " + currency.getCode() + " is in use.");
                    inActiveInUseCurrencies.add(currency);
                } else {
                    updateCurrency(currency);
                }
            } else {
                updateCurrency(currency);
            }
        }

        if (!inActiveInUseCurrencies.isEmpty()) {
            String inUseCurrencies = "";
            for (CurrencyWS ws : inActiveInUseCurrencies) {
                inUseCurrencies += ws.getCode() + Constants.SINGLE_SPACE;
            }
            String errorMessages[] = new String[1];
            if (inActiveInUseCurrencies.size() > 1) {
                // there is more than one inactive in use currency, so use
                // plural form of the message
                errorMessages[0] = "CurrencyWS,inUse,currencies.updated.currencies.inactive.yet.in.use,"
                        + inUseCurrencies;
            } else {
                errorMessages[0] = "CurrencyWS,inUse,currencies.updated.currency.inactive.yet.in.use,"
                        + inUseCurrencies;
            }

            logger.debug("Currency(s) {} is in use.", inUseCurrencies);
            throw new SessionInternalError("Currency(s) " + inUseCurrencies
                    + " is in use.", errorMessages);
        }
    }

    @Override
    public void updateCurrency(CurrencyWS ws)  {
        final Integer entityId = getCallerCompanyId();
        Integer companyId = getCallerCompanyId();
        CurrencyDTO currency = new CurrencyDTO(ws);

        CurrencyBL currencyBl = new CurrencyBL(currency.getId());

        if (currency.getRate() != null) {
            if (currency.getRateAsDecimal().compareTo(BigDecimal.ZERO) <= 0) {
                String errorMessages[] = new String[1];
                errorMessages[0] = "CurrencyWS,rate,currencies.updated.error.rate.can.not.be.zero.or.less,"
                        + currency.getDescription();
                throw new SessionInternalError("Currency " + currency.getId()
                        + ": ", errorMessages);
            }
        }

        if (currency.getSysRate() != null) {
            if (currency.getSysRate().compareTo(BigDecimal.ZERO) <= 0) {
                String errorMessages[] = new String[1];
                errorMessages[0] = "CurrencyWS,sysRate,currencies.updated.error.sys.rate.can.not.be.zero.or.less,"
                        + currency.getDescription();
                throw new SessionInternalError("Currency " + currency.getId()
                        + ":", errorMessages);
            }
        }

        // update currency
        currencyBl.update(currency, companyId);

        // set as entity currency if flagged as default
        if (ws.isDefaultCurrency()) {
            CurrencyBL.setEntityCurrency(entityId, currency.getId());
        }

        // update the description if its changed
        if ((ws.getDescription() != null && !ws.getDescription().equals(
                currency.getDescription()))) {
            currency.setDescription(ws.getDescription(), getCallerLanguageId());
        }

        // update exchange rates for date
        final Date fromDate = ws.getFromDate();
        currencyBl.setOrUpdateExchangeRate(ws.getRateAsDecimal(), entityId,
                null != fromDate ? fromDate : companyCurrentDate());
    }

    @Override
    public Integer createCurrency(CurrencyWS ws) {
        CurrencyDTO currency = new CurrencyDTO(ws);
        Integer entityId = getCallerCompanyId();

        // save new currency
        CurrencyBL currencyBl = new CurrencyBL(currency.getId());

        currencyBl.create(currency, entityId);
        if (ws.getRate() != null) {
            currencyBl.setOrUpdateExchangeRate(ws.getRateAsDecimal(), entityId,
                    companyCurrentDate());
        }
        currency = currencyBl.getEntity();

        // set as entity currency if flagged as default
        if (ws.isDefaultCurrency()) {
            currencyBl.setEntityCurrency(entityId, currency.getId());
        }

        // set description
        if (ws.getDescription() != null) {
            currency.setDescription(ws.getDescription(), getCallerLanguageId());
        }

        return currency.getId();
    }

    @Override
    public boolean deleteCurrency(Integer currencyId) {
        CurrencyBL currencyBl = new CurrencyBL(currencyId);
        try {
            return currencyBl.delete();
        } catch (DataIntegrityViolationException dve){
            throw new SessionInternalError("Cannot delete, CurrencyDTO not deletable",
                    new String[]{"currency.delete.failure,"+currencyBl.getEntity().getCode()});
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyWS getCompany() {
        CompanyDTO company = new CompanyDAS().find(getCallerCompanyId());
        logger.debug("Company: {}", company);
        return EntityBL.getCompanyWS(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyWS getCompanyByEntityId(Integer entityId) {
        CompanyDTO company = new CompanyDAS().find(entityId);
        logger.debug("Company: {}", company);
        return (null != company) ? EntityBL.getCompanyWS(company) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyWS getCompanyByMetaFieldValue(String value) {
        CompanyDTO company = new CompanyDAS().findEntityByMetaFieldValue(value);
        logger.debug("Company: {}", company);
        return EntityBL.getCompanyWS(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyWS[] getCompanies() {
        try {
            Integer mainRoleType = new UserBL(getCallerId()).getMainRole();

            if (Constants.TYPE_SYSTEM_ADMIN.equals(mainRoleType)) {
                return EntityBL.getCompaniesWS(new CompanyDAS().findEntities());
            } else {
                throw new SessionInternalError("You are not authorized to execute this action");
            }
        } catch (Exception ex) {
            throw new SessionInternalError(ex);
        }
    }

    @Override
    public void updateCompany(CompanyWS companyWS) {
        new EntityBL().updateEntityAndContact(companyWS, getCallerCompanyId(),
                getCallerId());
    }

    @Override
    public void updateCompanyWithEntityId(CompanyWS companyWS, Integer entityId, Integer userId) {
        new EntityBL().updateEntityAndContact(companyWS, entityId,
                userId);
    }

    /*
     * Notifications
     */
    @Override
    public void createUpdateNotification(Integer messageId, MessageDTO dto) {
        if (null == messageId) {
            new NotificationBL().createUpdate(getCallerCompanyId(), dto);
        } else {
            new NotificationBL(messageId).createUpdate(getCallerCompanyId(),
                    dto);
        }
    }

    /* Secured via WSSecurityMethodMapper entry. */
    @Override
    public void createCustomerNote(CustomerNoteWS note) {
        if (note.getNoteId() == 0) {

            CustomerNoteDTO customerNoteDTO = new CustomerNoteDTO();
            CustomerNoteDAS customerNoteDAS = new CustomerNoteDAS();
            customerNoteDTO.setCreationTime(TimezoneHelper.serverCurrentDate());
            customerNoteDTO.setNoteTitle(note.getNoteTitle());
            customerNoteDTO.setNoteContent(note.getNoteContent());
            customerNoteDTO.setCustomer(new CustomerDAS().find(note
                    .getCustomerId()));
            customerNoteDTO.setUser(userDAS.find(note.getUserId()));
            customerNoteDTO.setCompany(new CompanyDAS()
            .find(getCallerCompanyId()));
            customerNoteDTO.setNotesInInvoice(note.getNotesInInvoice());
            customerNoteDAS.save(customerNoteDTO);
            customerNoteDAS.flush();
            customerNoteDAS.clear();
        }
    }

    /*
     * Plug-ins
     */
    @Override
    @Transactional(readOnly = true)
    public PluggableTaskWS getPluginWS(Integer pluginId) {

        PluggableTaskDAS pluggableTaskDAS = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        PluggableTaskDTO dto = pluggableTaskDAS.findNow(pluginId);
        if (null == dto) {
            throw new SessionInternalError("Plugin with id " + pluginId + " not found!", HttpStatus.SC_NOT_FOUND);
        }
        return PluggableTaskBL.getWS(dto);
    }

    @Override
    public Integer createPlugin(PluggableTaskWS plugin) {
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        return pluginsSessionBean.createPlugin(getCallerId(), getCallerCompanyId(), plugin);
    }

    @Override
    @Transactional(readOnly = true)
    public PluggableTaskWS[] getPluginsWS(Integer entityId, String className){
        if (null == entityId || null == className){
            throw new SessionInternalError("Required parameters not found!!");
        }
        return PluggableTaskBL.getByClassAndEntity(entityId, className);
    }

    @Override
    public void updatePlugin(PluggableTaskWS plugin) {
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        pluginsSessionBean.updatePlugin(getCallerId(), getCallerCompanyId(), plugin);
    }

    @Override
    public void deletePlugin(Integer id) {
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        pluginsSessionBean.deletePlugin(getCallerId(), getCallerCompanyId(), id);
    }

    @Override
    public PluggableTaskWS getPluginWSByTypeId(Integer typeId) {
        PluggableTaskDTO dto = new PluggableTaskBL().findByEntityType(typeId, getCallerCompanyId());
        return PluggableTaskBL.getWS(dto);
    }

    /*
     * Quartz jobs
     */
    /**
     * This method reschedules an existing scheduled task that got changed. If
     * not existing, the new plugin may need to be scheduled only if it is an
     * instance of {@link com.sapienter.jbilling.server.process.task.IScheduledTask}
     */
    @Override
    public void rescheduleScheduledPlugin(Integer pluginId) {
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        pluginsSessionBean.rescheduleScheduledPlugin(getCallerId(), getCallerCompanyId(), pluginId);
    }

    @Override
    public void triggerScheduledTask(Integer pluginId, Date date){
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        pluginsSessionBean.triggerScheduledTask(getCallerId(), getCallerCompanyId(), pluginId, date);
    }

    /*
     * Quartz jobs
     */
    /**
     * This method unschedules an existing scheduled task before it is deleted
     */
    @Override
    public void unscheduleScheduledPlugin(Integer pluginId) {
        IPluginsSessionBean pluginsSessionBean = Context.getBean(Context.Name.PLUGINS_SESSION);
        pluginsSessionBean.unscheduleScheduledPlugin(getCallerId(), getCallerCompanyId(), pluginId);
    }

    /*
     * Plans and special pricing
     */
    @Override
    @Transactional(readOnly = true)
    public PlanWS getPlanWS(Integer planId) {
        PlanBL bl = new PlanBL(planId);
        return PlanBL.getWS(bl.getEntity());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanWS[] getAllPlans() {
        List<PlanDTO> plans = new PlanDAS().findAll(getCallerCompanyId());
        List<PlanWS> ws = PlanBL.getWS(plans);
        return ws.toArray(new PlanWS[ws.size()]);
    }

    @Override
    public Integer createPlan(PlanWS plan) {
        PlanDTO planDTO;
        try {
            planDTO = PlanBL.getDTO(plan);
        } catch (SessionInternalError e){
            throw new SessionInternalError(e, HttpStatus.SC_BAD_REQUEST);
        }

        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId());

        return new PlanBL().create(planDTO);
    }

    @Transactional
    @Override
    public void updatePlan(PlanWS plan) {
        PlanBL bl = new PlanBL(plan.getId());
        if (null == bl.getEntity()){
            throw new SessionInternalError("Plan not found", HttpStatus.SC_NOT_FOUND);
        }
        PlanDTO planDTO;
        try {
            planDTO = PlanBL.getDTO(plan);
        } catch (SessionInternalError e){
            throw new SessionInternalError(e, HttpStatus.SC_BAD_REQUEST);
        }

        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId());

        bl.update(planDTO);
    }

    @Override
    public void deletePlan(Integer planId) {
        PlanBL bl = new PlanBL(planId);
        if (null == bl.getEntity()){
            throw new SessionInternalError("Plan not found", HttpStatus.SC_NOT_FOUND);
        }
        bl.delete();
        deleteItem(bl.getEntity().getItemId());

        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId());
    }

    @Override
    public void addPlanPrice(Integer planId, PlanItemWS price) {
        PlanBL bl = new PlanBL(planId);
        bl.addPrice(PlanItemBL.getDTO(price));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCustomerSubscribed(Integer planId, Integer userId) {
        PlanBL bl = new PlanBL(planId);
        return bl.isSubscribed(userId, companyCurrentDate());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCustomerSubscribedForDate(Integer planId, Integer userId,
            Date eventDate) {
        PlanBL bl = new PlanBL(planId);
        return bl.isSubscribed(userId, eventDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Usage getItemUsage(Integer excludedOrderId, Integer itemId,
            Integer owner, List<Integer> userIds, Date startDate, Date endDate) {
        return new UsageDAS().findUsageByItem(excludedOrderId, itemId, owner,
                userIds, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getSubscribedCustomers(Integer planId) {
        List<CustomerDTO> customers = new PlanBL().getCustomersByPlan(planId);

        int i = 0;
        Integer[] customerIds = new Integer[customers.size()];
        for (CustomerDTO customer : customers) {
            customerIds[i++] = customer.getId();
        }
        return customerIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getPlansBySubscriptionItem(Integer itemId) {
        List<PlanDTO> plans = new PlanBL().getPlansBySubscriptionItem(itemId);

        int i = 0;
        Integer[] planIds = new Integer[plans.size()];
        for (PlanDTO plan : plans) {
            planIds[i++] = plan.getId();
        }
        return planIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer[] getPlansByAffectedItem(Integer itemId) {
        List<PlanDTO> plans = new PlanBL().getPlansByAffectedItem(itemId);

        int i = 0;
        Integer[] planIds = new Integer[plans.size()];
        for (PlanDTO plan : plans) {
            planIds[i++] = plan.getId();
        }
        return planIds;
    }

    @Override
    public PlanItemWS createCustomerPrice(Integer userId, PlanItemWS planItem,
            Date expiryDate) {
        PlanItemDTO dto = PlanItemBL.getDTO(planItem);
        CustomerPriceDTO price = new CustomerPriceBL(userId).create(dto);
        price.setPriceExpiryDate(expiryDate);
        return PlanItemBL.getWS(price.getPlanItem());
    }

    @Override
    public void updateCustomerPrice(Integer userId, PlanItemWS planItem,
            Date expiryDate) {
        PlanItemDTO dto = PlanItemBL.getDTO(planItem);
        CustomerPriceBL customerPrice = new CustomerPriceBL(userId, dto.getId());
        customerPrice.update(dto);
        customerPrice.getEntity().setPriceExpiryDate(expiryDate);
    }

    @Override
    public void deleteCustomerPrice(Integer userId, Integer planItemId) {
        new CustomerPriceBL(userId, planItemId).delete();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItemWS[] getCustomerPrices(Integer userId) {
        List<PlanItemDTO> prices = new CustomerPriceBL(userId)
        .getCustomerPrices();
        List<PlanItemWS> ws = PlanItemBL.getWS(prices);
        return ws.toArray(new PlanItemWS[ws.size()]);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItemWS getCustomerPrice(Integer userId, Integer itemId) {
        CustomerPriceBL bl = new CustomerPriceBL(userId);
        return PlanItemBL.getWS(bl.getPrice(itemId, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItemWS getCustomerPriceForDate(Integer userId, Integer itemId,
            Date pricingDate, Boolean planPricingOnly) {
        CustomerPriceBL bl = new CustomerPriceBL(userId);
        // TODO update this method to set planId instead of null
        return PlanItemBL.getWS(bl.getPriceForDate(itemId, planPricingOnly,
                pricingDate, null));
    }

    // account types
    @Override
    public PlanItemWS createAccountTypePrice(Integer accountTypeId,
            PlanItemWS planItem, Date expiryDate) {
        PlanItemDTO dto = PlanItemBL.getDTO(planItem);
        AccountTypePriceDTO price = new AccountTypePriceBL(accountTypeId)
        .create(dto);
        price.setPriceExpiryDate(expiryDate);

        return PlanItemBL.getWS(price.getPlanItem());
    }

    @Override
    public void updateAccountTypePrice(Integer accountTypeId,
            PlanItemWS planItem, Date expiryDate) {
        PlanItemDTO dto = PlanItemBL.getDTO(planItem);
        AccountTypePriceBL accountTypePriceBl = new AccountTypePriceBL(
                accountTypeId, dto.getId());
        accountTypePriceBl.update(dto);
        accountTypePriceBl.getPrice().setPriceExpiryDate(expiryDate);

    }

    @Override
    public void deleteAccountTypePrice(Integer accountTypeId, Integer planItemId) {
        new AccountTypePriceBL(accountTypeId, planItemId).delete();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItemWS[] getAccountTypePrices(Integer accountTypeId) {
        List<PlanItemDTO> prices = new AccountTypePriceBL(accountTypeId)
        .getAccountTypePrices();
        List<PlanItemWS> ws = PlanItemBL.getWS(prices);
        return ws.toArray(new PlanItemWS[ws.size()]);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanItemWS getAccountTypePrice(Integer accountTypeId, Integer itemId) {
        AccountTypePriceBL bl = new AccountTypePriceBL(accountTypeId);
        return PlanItemBL.getWS(bl.getPrice(itemId));
    }

    /*
     * Asset
     */
    @Override
    public Integer createAsset(AssetWS asset)  {

        validateAsset(asset);
        AssetBL assetBL = new AssetBL();
        AssetDTO dto = assetBL.getDTO(asset);

        // do validation
        checkItemAllowsAssetManagement(dto);

        // set default meta field values and validate
        MetaFieldHelper.updateMetaFieldDefaultValuesWithValidation(dto.getEntity().getLanguageId(),
                dto.getItem().findItemTypeWithAssetManagement().getAssetMetaFields(), dto);

        assetBL.checkForDuplicateIdentifier(dto);
        checkOrderAndStatus(dto);
        assetBL.checkContainedAssets(dto.getContainedAssets(), 0);

        return assetBL.create(dto, getCallerId());
    }

    @Override
    public void updateAsset(AssetWS asset)  {
        updateAsset(asset, getCallerCompanyId());
    }

    public void validateAsset(AssetWS asset) {
        boolean foundAsset = true;
        boolean isRoot = new CompanyDAS().isRoot(getCallerCompanyId());
        if (asset.getContainedAssetIds() != null && isRoot && !asset.isGlobal()) {
            for (Integer assetIds : asset.getContainedAssetIds()) {
                AssetDTO assets = assetDAS.find(assetIds);
                for (Integer assetEntities : asset.getEntities()) {
                    Set<CompanyDTO> assetEntity = new HashSet<CompanyDTO>(
                            assets.getEntities());
                    for (CompanyDTO entity : assetEntity) {
                        if (assetEntities.equals(entity.getId())
                                || assets.isGlobal()) {
                            foundAsset = true;
                        } else {
                            foundAsset = false;
                            throw new SessionInternalError(
                                    "The child company asset can not available for root company ",
                                    new String[] { "AssetWS,containedAssets,validation.child.asset.not.add.root.asset" }, HttpStatus.SC_BAD_REQUEST);
                        }
                    }
                }
            }
        }
        SpaImportBL.validateDistributelAsset(asset);
    }

    public void updateAsset(AssetWS asset, Integer entityId)
    {

        validateAsset(asset);

        AssetBL assetBL = new AssetBL(asset.getId());
        AssetDTO persistentAsset = assetBL.getEntity();

        AssetDTO dto = new AssetBL().getDTO(asset);
        // VALIDATION
        // can not change the status if it is internal e.g. 'Belongs to Group'
        if (persistentAsset.getAssetStatus().getIsInternal() == 1
                && (persistentAsset.getAssetStatus().getId() != dto
                .getAssetStatus().getId())) {
            throw new SessionInternalError(
                    "Asset has an internal status which may not be changed",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.change.internal" }, HttpStatus.SC_BAD_REQUEST);
        }
        if (persistentAsset.getAssetStatus().getIsOrderSaved() == 1
                && (persistentAsset.getAssetStatus().getId() != dto
                .getAssetStatus().getId())) {
            throw new SessionInternalError(
                    "Asset belongs to an order and  the status may not be changed",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.change.fromordersaved" }, HttpStatus.SC_BAD_REQUEST);
        }
        if (dto.getAssetStatus().getIsOrderSaved() == 1
                && (persistentAsset.getAssetStatus().getId() != dto
                .getAssetStatus().getId())) {
            throw new SessionInternalError(
                    "Asset status can not be changed to Ordered Status",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.change.toordersaved" }, HttpStatus.SC_BAD_REQUEST);
        }
        checkItemAllowsAssetManagement(dto);
        ItemDTO item = dto.getItem();
        MetaFieldHelper.updateMetaFieldDefaultValuesWithValidation(item.getEntity().getId(),
                item.findItemTypeWithAssetManagement().getAssetMetaFields(), dto);
        assetBL.checkForDuplicateIdentifier(dto);
        checkOrderAndStatus(dto);
        assetBL.checkContainedAssets(dto.getContainedAssets(), dto.getId());

        Integer userId = null;

        OrderLineDTO orderLineDTO = persistentAsset.getOrderLine();
        if (orderLineDTO != null) {
            userId = orderLineDTO.getPurchaseOrder().getUserId();
        } else {
            OrderChangeDTO orderChange = new OrderChangeDAS().findByOrderChangeByAssetIdInPlanItems(persistentAsset.getId());
            if (orderChange != null) {
                userId = orderChange.getOrder().getUser().getId();
            }
        }

        if (userId != null) {
            String oldPhoneNumber = "";
            String newPhoneNumber = "";

            for (MetaFieldValue metaFieldValue : persistentAsset.getMetaFields()) {
                if (metaFieldValue.getField().getName().equals("Phone Number")) {
                    oldPhoneNumber = String.valueOf(metaFieldValue.getValue());
                    break;
                }
            }

            for (MetaFieldValueWS metaFieldValue : asset.getMetaFields()) {
                if (metaFieldValue.getFieldName().equals("Phone Number")) {
                    newPhoneNumber = String.valueOf(metaFieldValue.getValue());
                    break;
                }
            }

            // Create Request to Update 911 Emergency Address
            Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.
                    createEventForAssetUpdate(getCallerCompanyId(), userId, oldPhoneNumber, newPhoneNumber);
            if(addressUpdateEvent != null) {
                EventManager.process(addressUpdateEvent);
            }
        }

        assetBL.update(dto, getCallerId());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetWS getAsset(Integer assetId) {
        AssetDTO assetDTO = checkAssetById(assetId);
        if(null == assetDTO){
            throw new SessionInternalError(
                    "Asset do not exist.",
                    new String[] { "AssetWS,identifier,asset.validation.resource.entity.not.found"}, HttpStatus.SC_NOT_FOUND);
        }
        AssetBL bl = new AssetBL(assetDTO);
        return AssetBL.getWS(bl.getEntity());
    }

    @Transactional(readOnly = true)
    @Override
    public AssetWS getAssetByIdentifier(String assetIdentifier) {
        Integer assetId = assetDAS
                .getAssetsForIdentifier(assetIdentifier);
        Optional<Integer> assetIdOptional = Optional.ofNullable(assetId);
        if (assetIdOptional.isPresent()) {
            AssetBL bl = new AssetBL();
            return bl.getWS(bl.find(assetId));
        }

        throw new SessionInternalError(
                "Asset Identifier :" + assetIdentifier + " do not exist.",
                new String[] {
                        "AssetWS,identifier,asset.validation.asset.identifier.not.found",
                        assetIdentifier }, HttpStatus.SC_NOT_FOUND);
    }

    @Override
    public void deleteAsset(Integer assetId)  {
        AssetDTO asset = checkAssetById(assetId);
        if(null == asset){
            throw new SessionInternalError(
                    "Asset do not exist.",
                    new String[] { "AssetWS,identifier,asset.validation.resource.entity.not.found"}, HttpStatus.SC_NOT_FOUND);
        }
        AssetBL bl = new AssetBL(asset);
        asset = bl.getEntity();
        AssetReservationDTO activeReservation = new AssetReservationDAS()
        .findActiveReservationByAsset(assetId);
        if (activeReservation != null) {
            throw new SessionInternalError(
                    "Asset can not be deleted.Its reserved for customer",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.reserved" }, HttpStatus.SC_CONFLICT);
        }
        if (asset.getAssetStatus().getIsInternal() == 1) {
            throw new SessionInternalError(
                    "Asset has an internal status which may not be changed",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.change.internal" }, HttpStatus.SC_CONFLICT);
        }
        if (asset.getAssetStatus().getIsOrderSaved() == 1) {
            throw new SessionInternalError(
                    "Asset can not be deleted.Its already in use.",
                    new String[] { "AssetWS,asset,asset.validation.status.order.saved.already" }, HttpStatus.SC_CONFLICT);
        }
        new AssetBL().delete(assetId, getCallerId());
    }

    private AssetDTO checkAssetById(Integer id){
        return assetDAS.findNow(id);
    }

    /**
     * Gets all assets linked to the category (ItemTypeDTO) through products
     * (ItemDTO).
     *
     * @param categoryId
     *            Category (ItemTypeDTO) identifier.
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getAssetsForCategory(Integer categoryId) {
        return new AssetBL().getAssetsForCategory(categoryId);
    }

    /**
     * Gets all asset ids linked to the product (ItemDTO)
     *
     * @param itemId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Integer[] getAssetsForItem(Integer itemId) {
        return new AssetBL().getAssetsForItem(itemId);
    }

    @Transactional(readOnly = true)
    public AssetWS[] getAssetsForItemId(Integer itemId, Integer offset, Integer limit) {
        if (null == itemId){
            throw new SessionInternalError("ItemId can not be null!", HttpStatus.SC_BAD_REQUEST);
        }
        validOffsetAndLimit(offset, limit);
        return AssetBL.getWS(new AssetBL().getAssetsForItem(itemId, getCallerCompanyId(), offset, limit));
    }

    @Transactional(readOnly = true)
    public AssetWS[] getAssetsForCategoryId(Integer itemTypeId, Integer offset, Integer limit) {
        if (null == itemTypeId){
            throw new SessionInternalError("ItemTypeId can not be null!", HttpStatus.SC_BAD_REQUEST);
        }
        validOffsetAndLimit(offset, limit);
        return AssetBL.getWS(new AssetBL().getAssetsForCategory(itemTypeId, getCallerCompanyId(), offset, limit));
    }

    private void validOffsetAndLimit(Integer offset, Integer limit){
        if (null != offset && offset.intValue() < 0){
            throw new SessionInternalError("Offset value can not be negative number.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && limit.intValue() < 0){
            throw new SessionInternalError("Limit value must be greater than zero.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && null != offset && offset.intValue() > limit.intValue()){
            throw new SessionInternalError("Offset value can not be greater than limit value.", HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Import a file containing assets.
     *
     *
     * @param itemId
     *            ItemDTO id the assets will be linked to
     * @param identifierColumnName
     *            column name of the asset 'identifier' attribute
     * @param notesColumnName
     *            column name of 'notes' attribute
     * @param sourceFilePath
     *            path to the input file
     * @param errorFilePath
     *            path to the error file
     * @return job execution id
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long startImportAssetJob(int itemId, String identifierColumnName,
            String notesColumnName, String globalColumnName,
            String entitiesColumnName, String sourceFilePath,
            String errorFilePath)  {
        ItemDTO item = new ItemBL(itemId).getEntity();
        int entityId = getCallerCompanyId();
        if (!item.isGlobal()
                && !item.getEntities()
                .contains(new CompanyDAS().find(entityId))) {
            throw new SessionInternalError(
                    "Item not available/shared with the caller Entity.");
        }

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job assetLoadJob = Context.getBean(Context.Name.BATCH_ASSET_LOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();
        jobParams.put(Constants.BATCH_JOB_PARAM_ENTITY_ID, new JobParameter(
                Integer.toString(entityId)));

        jobParams.put(AssetImportConstants.JOB_PARM_ITEM_ID, new JobParameter(
                new Long(itemId)));

        jobParams.put(AssetImportConstants.JOB_PARM_INPUT_FILE,
                new JobParameter(sourceFilePath));
        jobParams.put(AssetImportConstants.JOB_PARM_ERROR_FILE,
                new JobParameter(errorFilePath));
        jobParams.put(AssetImportConstants.JOB_PARM_ID_COLUMN,
                new JobParameter(identifierColumnName));
        jobParams.put(AssetImportConstants.JOB_PARM_NOTES_COLUMN,
                new JobParameter(notesColumnName));
        jobParams.put(AssetImportConstants.JOB_PARM_USER_ID, new JobParameter(
                new Long(getCallerId())));
        jobParams.put(AssetImportConstants.JOB_PARM_GLOBAL, new JobParameter(
                globalColumnName));
        jobParams.put(AssetImportConstants.JOB_PARM_ENTITIES, new JobParameter(
                entitiesColumnName));
        jobParams.put(AssetImportConstants.JOB_PARM_ENTITY_ID,
                new JobParameter(new Long(entityId)));
        jobParams.put("startDate", new JobParameter(TimezoneHelper.serverCurrentDate()));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(assetLoadJob,
                    new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start asset import job", e);
            throw new SessionInternalError("Unable to start asset import job",
                    e);
        }
    }

    /**
     * Gets all the transitions for the asset
     *
     * @param assetId
     *            - AssetDTO id
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public AssetTransitionDTOEx[] getAssetTransitions(Integer assetId) {
        return AssetTransitionBL.getWS(new AssetTransitionBL()
        .getTransitions(assetId));
    }

    /**
     * Find all assets which match search criteria. Filters with field names
     * 'id', 'status' and 'identifier' will be used as filters on asset
     * attributes, any other field names will be used to match meta fields. You
     * can order by any of the properties of AssetDTO.
     *
     * @see AssetDAS#findAssets(int,
     *      com.sapienter.jbilling.server.util.search.SearchCriteria)
     *
     * @param criteria
     * @return
     */
    @Override
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria) {
        return new AssetBL().findAssets(productId, criteria);
    }

    /*
     * Asset Helper Methods
     */

    /**
     * Validations for asset manager type linked to the item.
     * Do the following checks
     *  - if the item allows asset management, it must be linked to a category which allows asset management
     *  - item may never be linked to more than 1 category allowing asset management
     *  - if assets are already linked to this item, the type allowing asset management may not be removed or changed.
     * @param newDto - with changes applied
     * @param oldDto - currrent persistent object
     */
    private void validateAssetManagementForItem(ItemDTO newDto, ItemDTO oldDto) {
        List<Integer> assetManagementTypes = extractAssetManagementTypes(newDto.getTypes());

        //if the item allows asset management, it must be linked to one category which allows asset management
        if(newDto.getAssetManagementEnabled() == 1) {
            if(assetManagementTypes.size() < 1) {
                throw new SessionInternalError("Product must belong to a category which allows asset management", new String[] {
                        "ItemDTOEx,types,product.validation.no.assetmanagement.type.error"
                }, HttpStatus.SC_BAD_REQUEST);
            }
        }
        //only 1 asset management type allowed
        if(assetManagementTypes.size() > 1) {
            throw new SessionInternalError("Product belongs to more than one category which allows asset management", new String[] {
                    "ItemDTOEx,types,product.validation.multiple.assetmanagement.types.error"
            }, HttpStatus.SC_BAD_REQUEST);
        }

        //checks only if this is an update
        if(oldDto != null) {
            //in the current persisted object, find the item type which allows asset management
            Integer currentAssetManagementType = null;
            for(ItemTypeDTO typeDTO: oldDto.getItemTypes()) {
                if(typeDTO.getAllowAssetManagement() == 1) {
                    currentAssetManagementType = typeDTO.getId();
                    break;
                }
            }

            if(currentAssetManagementType != null) {
                int assetCount = new AssetBL().countAssetsForItem(oldDto.getId());
                if(assetCount > 0) {
                    //asset management type may not be removed
                    if(assetManagementTypes.isEmpty()) {
                        throw new SessionInternalError("Asset management category may not be removed", new String[] {
                                "ItemDTOEx,types,product.validation.assetmanagement.removed.error"
                        }, HttpStatus.SC_BAD_REQUEST);
                    }

                    //asset management type may not be changed
                    if(!currentAssetManagementType.equals(assetManagementTypes.get(0))) {
                        throw new SessionInternalError("Asset management category may not be changed", new String[] {
                                "ItemDTOEx,types,product.validation.assetmanagement.changed.error"
                        }, HttpStatus.SC_BAD_REQUEST);
                    }
                }
            }
        }
    }

    /**
     * Extract all ItemTypes which allows asset management from the list of provided ItemType ids.
     * This method loads all the ItemTypes for the provded ids and checks if they allow asset management.
     * The ones that do will be returned.
     *
     * @param types - ItemType ids
     * @return Ids of ItemTypes allowing asset management.
     */
    private List<Integer> extractAssetManagementTypes(Integer[] types) {
        List<Integer> typeIds = new ArrayList<Integer>(2);

        ItemTypeBL itemTypeBL = new ItemTypeBL();
        for(Integer typeId : types) {
            itemTypeBL.set(typeId);
            ItemTypeDTO itemTypeDTO = itemTypeBL.getEntity();
            if(itemTypeDTO.getAllowAssetManagement() == 1) {
                typeIds.add(typeId);
            }
        }
        return typeIds;
    }

    /**
     * Check that the type has only one status which is 'default' and one which
     * has 'order saved' checked. Check that status names are unique
     */
    private void validateItemCategoryStatuses(ItemTypeDTO dto)
    {
        // no need to do further checking if the type doesn't allow asset
        // management
        if (dto.getAllowAssetManagement() == 0) {
            return;
        }

        // status names must be unique
        Set<String> statusNames = new HashSet<String>(dto.getAssetStatuses()
                .size() * 2);
        // list of errors found
        List<String> errors = new ArrayList<String>(2);

        // keep count of the number of 'default' and 'order create' statuses
        int defaultCount = 0;
        int createOrderCount = 0;
        int activeCount = 0;
        int pendingCount = 0;

        for (AssetStatusDTO statusDTO : dto.getAssetStatuses()) {
            if (statusDTO.getDeleted() == 0) {
                if (statusDTO.getIsDefault() == 1) {
                    defaultCount++;
                }
                if (statusDTO.getIsOrderSaved() == 1) {
                    createOrderCount++;
                }
                if(statusDTO.getIsPending() == 1){
                    pendingCount++;
                }
                if(statusDTO.getIsActive() == 1){
                    activeCount++;
                }
                if (statusNames.contains(statusDTO.getDescription())) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.description.unique,"
                            + statusDTO.getDescription());
                }
                if (statusDTO.getIsActive() == 1
                        && statusDTO.getIsOrderSaved() == 0) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.active.not.ordersaved");
                }
                if (statusDTO.getIsPending() == 1
                        && statusDTO.getIsOrderSaved() == 0) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.pending.not.ordersaved");
                }
                if (statusDTO.getIsAvailable() == 1
                        && statusDTO.getIsOrderSaved() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.available.ordersaved");
                }
                if (statusDTO.getIsAvailable() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.available.pending");
                }
                if (statusDTO.getIsAvailable() == 1
                        && statusDTO.getIsActive() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.available.active");
                }
                if (statusDTO.getIsDefault() == 1
                        && statusDTO.getIsActive() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.default.active");
                }
                if (statusDTO.getIsDefault() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.default.pending");
                }
                if (statusDTO.getIsActive() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.active.pending");
                }
                if (statusDTO.getIsOrderSaved() == 1
                        && statusDTO.getIsPending() == 0 && statusDTO.getIsActive() == 0){
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.ordersaved.pending.active");
                }
                if (statusDTO.getIsDefault() == 1
                        && statusDTO.getIsOrderSaved() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.default.ordersaved");
                } else {
                    statusNames.add(statusDTO.getDescription());
                }
            }
        }

        if (defaultCount != 1) {
            errors.add("ItemTypeWS,statuses,validation.error.category.status.default.one");
        }

        if (0 == createOrderCount) {
            errors.add("ItemTypeWS,statuses,validation.error.category.status.order.saved.one");
        }

        if(1 != pendingCount){
            errors.add("ItemTypeWS,statuses,validation.error.category.status.pending.one");
        }

        if(1 != activeCount){
            errors.add("ItemTypeWS,statuses,validation.error.category.status.active.one");
        }

        if (errors.size() > 0) {
            throw new SessionInternalError(
                    "Category Status validation failed.",
                    errors.toArray(new String[errors.size()]), HttpStatus.SC_BAD_REQUEST);

        }
    }

    /**
     * If the asset belongs to an order, it must have a status of unavailable
     *
     * @param dto
     * @
     */
    private void checkOrderAndStatus(AssetDTO dto)  {
        if (dto.getOrderLine() != null
                && dto.getAssetStatus().getIsAvailable() == 1) {
            throw new SessionInternalError(
                    "An asset belonging to an order must have an unavailable status",
                    new String[] { "AssetWS,assetStatus,asset.validation.status.not.unavailable" }, HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Check that the item linked to the asset allows asset management.
     *
     * @param dto
     * @
     */
    private void checkItemAllowsAssetManagement(AssetDTO dto)
    {
        if (dto.getItem().getAssetManagementEnabled() == 0) {
            throw new SessionInternalError(
                    "The item does not allow asset management",
                    new String[] { "AssetWS,itemId,asset.validation.item.not.assetmanagement" }, HttpStatus.SC_BAD_REQUEST);
        }
    }

    /*
     * Route Based Rating
     */
    @Override
    public Integer createRoute(RouteWS routeWS, File routeFile)
    {
        RouteBL routeBL = new RouteBL();

        CompanyDTO company = EntityBL.getDTO(getCompany());

        RouteDTO routeDTO = routeBL.toDTO(routeWS);
        routeDTO.setCompany(company);
        routeDTO.setName(routeWS.getName());
        routeDTO.setTableName(routeBL.createRouteTableName(company.getId(),
                routeDTO.getName()));

        RouteDTO rootRoute = routeBL.getRootRouteTable(company.getId());
        if (routeWS.getRootTable() != null
                && routeWS.getRootTable().booleanValue()) {
            if ((null != rootRoute && (null == routeWS.getId() || routeWS
                    .getId() <= 0))
                    ||

                    (null != rootRoute && null != routeWS.getId()
                    && routeWS.getId() > 0 && rootRoute.getId() != routeWS
                    .getId().intValue())) {

                throw new SessionInternalError(
                        "There can be only one root table per company",
                        new String[] { "RouteWS,rootTable,route.validation.only.one.root.table.allowed" });

            }
        }

        if (routeWS.getId() != null) {
            new RouteBL(routeDTO.getId()).update(routeDTO, routeFile);
            return routeWS.getId();
        } else {
            return new RouteBL().create(routeDTO, routeFile);
        }
    }

    @Override
    public void deleteRoute(Integer routeId)  {
        new RouteBL(routeId).delete();
    }

    @Override
    public RouteWS getRoute(Integer routeId) {
        RouteDAS das = new RouteDAS();
        RouteDTO routeDTO = das.findNow(routeId);
        if (routeId == null || routeDTO == null) {
            return null;
        }
        RouteBL bl = new RouteBL(routeId);
        return bl.toWS();
    }

    @Override
    public Integer createMatchingField(MatchingFieldWS matchingFieldWS)  {
        validateMatchingFieldData(matchingFieldWS);
        MatchingFieldDTO matchingFieldDTO = new MatchingFieldDTO();
        matchingFieldDTO.setDescription(matchingFieldWS.getDescription());
        matchingFieldDTO.setOrderSequence(Integer.parseInt(matchingFieldWS.getOrderSequence()));
        matchingFieldDTO.setMediationField(matchingFieldWS.getMediationField());
        matchingFieldDTO.setMatchingField(matchingFieldWS.getMatchingField());
        matchingFieldDTO.setRequired(matchingFieldWS.getRequired());
        matchingFieldDTO.setType(MatchingFieldType.valueOf(matchingFieldWS.getType()));
        matchingFieldDTO.setRoute(new RouteDAS().find(matchingFieldWS.getRouteId()));
        matchingFieldDTO.setRouteRateCard(new RouteRateCardDAS().find(matchingFieldWS.getRouteRateCardId()));
        matchingFieldDTO.setMandatoryFieldsQuery("obsoleted");
        matchingFieldDTO = new MatchingFieldDAS().save(matchingFieldDTO);

        return matchingFieldDTO.getId();

    }

    @Override
    public void deleteMatchingField(Integer matchingFieldId)
    {
        try {
            MatchingFieldDAS matchingFieldDAS = new MatchingFieldDAS();
            MatchingFieldDTO matchingFieldDTO = matchingFieldDAS
                    .getMatchingFieldById(matchingFieldId);
            matchingFieldDAS.delete(matchingFieldDTO);
            matchingFieldDAS.flush();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MatchingFieldWS getMatchingField(Integer matchingFieldId)
    {
        MatchingFieldDAS matchingFieldDAS = new MatchingFieldDAS();
        MatchingFieldDTO matchingFieldDTO = matchingFieldDAS
                .findNow(matchingFieldId);
        if (matchingFieldDTO != null) {
            MatchingFieldWS matchingFieldWS = new RouteBL()
            .convertMatchingFieldDTOToMatchingFieldWS(matchingFieldDTO);
            return matchingFieldWS;
        }
        return null;
    }

    @Override
    public boolean updateMatchingField(MatchingFieldWS matchingFieldWS)  {
        validateMatchingFieldData(matchingFieldWS);
        MatchingFieldDAS matchingFieldDAS = new MatchingFieldDAS();
        MatchingFieldDTO matchingFieldDTO = matchingFieldDAS.findNow(matchingFieldWS.getId());
        matchingFieldDTO.setDescription(matchingFieldWS.getDescription());
        matchingFieldDTO.setOrderSequence(Integer.parseInt(matchingFieldWS.getOrderSequence()));
        matchingFieldDTO.setMediationField(matchingFieldWS.getMediationField());
        matchingFieldDTO.setMatchingField(matchingFieldWS.getMatchingField());
        matchingFieldDTO.setRequired(matchingFieldWS.getRequired());
        matchingFieldDTO.setType(MatchingFieldType.valueOf(matchingFieldWS.getType()));
        matchingFieldDTO.setMandatoryFieldsQuery("obsoleted");
        matchingFieldDTO = matchingFieldDAS.save(matchingFieldDTO);

        return true;
    }

    /**
     * Calculate the matching field data that will be used for performance
     * optimization
     *
     * @param matchingFieldWS
     */
    private void validateMatchingFieldData(MatchingFieldWS matchingFieldWS) {
        if (matchingFieldWS.getRouteId() == null && matchingFieldWS.getRouteRateCardId() == null) {
            throw new SessionInternalError("Either route or route rate card needs to be specified for a matching field",
                    new String[] { "MatchingFieldWS,routeId,matching.field.route.mandatory" });
        }
    }

    /**
     * Create a record in a route table. An id may be specified for the record.
     * If it is not specified the max + 1 id will used.
     *
     *
     *
     * @param routeRecord
     *            RouteDTO id
     * @param routeId
     * @return
     */
    @Override
    public Integer createRouteRecord(RouteRecordWS routeRecord, Integer routeId)
    {
        validateRouteRecord(routeRecord);
        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();
        return updater.create(routeRecordToMap(routeRecord));
    }

    @Override
    public Integer createRouteRateCardRecord(RouteRateCardWS record,
            Integer routeRateCardId)  {
        record.validate();
        RouteRateCardDTO dto = new RouteBasedRateCardBL(routeRateCardId)
        .getEntity();
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(dto);
        ITableUpdater updater = factory.getRouteRateCardUpdaterInstance();
        return updater.create(record.routeRateCardRecordToMap());
    }

    /**
     * Update and entry in a route table.
     *
     * @param routeRecord
     *            - RouteDTO.id
     * @param routeId
     *
     */
    @Override
    public void updateRouteRecord(RouteRecordWS routeRecord, Integer routeId)
    {
        validateRouteRecord(routeRecord);
        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();
        updater.update(routeRecordToMap(routeRecord));
    }

    @Override
    public void updateRouteRateCardRecord(RouteRateCardWS record,
            Integer routeRateCardId)  {
        record.validate();
        RouteRateCardDTO dto = new RouteBasedRateCardBL(routeRateCardId)
        .getEntity();
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(dto);
        ITableUpdater updater = factory.getRouteRateCardUpdaterInstance();
        updater.update(record.routeRateCardRecordToMap());
    }

    /**
     * Delete a route record.
     *
     * @param routeId
     *            RouteDTO.id
     * @param recordId
     *            record id.
     */
    @Override
    public void deleteRouteRecord(Integer routeId, Integer recordId)
    {
        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();
        updater.delete(recordId);
    }

    /**
     * Delete a route rate card record.
     *
     * @param routeRateCardId
     *            RouteRateCardDTO.id
     * @param recordId
     *            record id.
     */

    @Override
    public void deleteRateCardRecord(Integer routeRateCardId, Integer recordId)
    {
        RouteRateCardDTO dto = new RouteBasedRateCardBL(routeRateCardId)
        .getEntity();
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(dto);
        ITableUpdater updater = factory.getRouteRateCardUpdaterInstance();
        updater.delete(recordId);
    }

    /**
     * Get the contents of a route table in CSV format.
     *
     * @param routeId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public String getRouteTable(Integer routeId) {
        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();
        List<Map<String, String>> table = updater.list();
        if (table.isEmpty()) {
            return "";
        }

        List exportableList = new ArrayList<Exportable>();
        exportableList.add(new ExportableMap(table));

        CsvExporter<ExportableMap> exporter = CsvExporter
                .createExporter(ExportableMap.class);
        return exporter.export(exportableList);

    }

    @Override
    public SearchResultString searchDataTable(Integer routeId,
            SearchCriteria criteria) {
        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();
        SearchResultString result = updater.search(criteria);
        return result;
    }

    @Override
    public Set<String> searchDataTableWithFilter(Integer routeId, String filters, String searchName) {

        String sortIndex = "id";
        String sortOrder = "ASC";
        Integer maxRows = 500;
        int currentPage = 1;

        int rowOffset = currentPage == 1 ? 0 : (currentPage - 1) * maxRows;

        //build the criteria object
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(rowOffset);
        criteria.setMax(maxRows);
        criteria.setSort(sortIndex);
        criteria.setDirection(SearchCriteria.SortDirection.ASC);
        criteria.setTotal(-1);

        List<com.sapienter.jbilling.server.util.search.BasicFilter> criteriaFilters = new ArrayList<com.sapienter.jbilling.server.util.search.BasicFilter>();

        //build the search filters
        logger.debug("Filters come from parameter:  " + filters);

        //      String should be join using com.google.common.base.Joiner
        Map<String, String> filtersMap;
        if(StringUtils.trimToNull(filters) != null) {
            filtersMap = Splitter.on("~~").withKeyValueSeparator("=").split(filters);
        } else {
            filtersMap = new HashMap<String, String>();
        }

        for(Map.Entry<String, String> entry : filtersMap.entrySet()) {
            if(StringUtils.trimToNull(entry.getValue()) != null) {
                com.sapienter.jbilling.server.util.search.Filter.FilterConstraint  constraint = com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.ILIKE;
                criteriaFilters.add(new com.sapienter.jbilling.server.util.search.BasicFilter(entry.getKey(), constraint, entry.getValue()));
            }
        }
        criteria.setFilters(criteriaFilters.toArray(new com.sapienter.jbilling.server.util.search.BasicFilter[criteriaFilters.size()]));

        logger.debug("Criteria filter in new method is:  " + criteria);

        RouteDTO dto = new RouteBL(routeId).getEntity();
        RouteBeanFactory factory = new RouteBeanFactory(dto);

        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();
        SearchResultString result = searchDataTable(1, criteria);
        int searchNameIdx = columnNames.indexOf(searchName.toLowerCase());

        Set<String> filteredResult = new HashSet<String>();
        for(List<String> resultRow : result.getRows()) {
            filteredResult.add(resultRow.get(searchNameIdx));
        }

        return filteredResult;
    }

    @Override
    public SearchResultString searchRouteRateCard(Integer routeRateCardId,
            SearchCriteria criteria) {
        RouteRateCardDTO dto = new RouteBasedRateCardBL(routeRateCardId)
        .getEntity();
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(dto);
        ITableUpdater updater = factory.getRouteRateCardUpdaterInstance();
        SearchResultString result = updater.search(criteria);
        return result;
    }

    @Override
    public Integer createDataTableQuery(DataTableQueryWS queryWS)
    {
        DataTableQueryBL bl = new DataTableQueryBL();
        queryWS.setUserId(getCallerId());
        DataTableQueryDTO dto = DataTableQueryBL.convertToDto(queryWS);
        dto = bl.create(dto);
        return dto.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableQueryWS getDataTableQuery(int id)
    {
        DataTableQueryBL bl = new DataTableQueryBL();
        bl.set(id);
        DataTableQueryDTO dto = bl.getEntity();
        return DataTableQueryBL.convertToWS(dto);
    }

    @Override
    public void deleteDataTableQuery(int id)  {
        DataTableQueryBL bl = new DataTableQueryBL();
        bl.delete(id);
    }

    @Override
    public DataTableQueryWS[] findDataTableQueriesForTable(int routeId)
    {
        DataTableQueryBL bl = new DataTableQueryBL();
        List<DataTableQueryDTO> result = bl.findDataTableQueriesForTable(
                routeId, getCallerId());
        return bl.convertToWsArray(result, false);

    }

    /**
     * Validates that - at least one entry has a value. - all names are not
     * empty
     *
     * @param record
     */
    private void validateRouteRecord(RouteRecordWS record) {
        for (NameValueString nv : record.getAttributes()) {
            if (nv.getName().trim().length() == 0) {
                throw new SessionInternalError(
                        "Route record field has empty name",
                        new String[] { "RouteRecordWS,fields,route.record.validation.attr.no.name" });
            }
            if (nv.getValue().trim().length() > 0) {
                return;
            }
        }
        throw new SessionInternalError(
                "Route record can not have all empty values",
                new String[] { "RouteRecordWS,fields,route.record.validation.no.data" });
    }

    /**
     * Convert an array of name value pairs to a map with the key the name.
     *
     *
     * @param record
     * @return
     */
    private Map<String, String> routeRecordToMap(RouteRecordWS record) {
        Map<String, String> result = new HashMap<>(
                record.getAttributes().length * 2);
        if (record.getId() != null) {
            result.put("id", record.getId().toString());
        }
        if (record.getRouteId() != null) {
            result.put("routeid", record.getRouteId());
        }
        if (record.getName() != null) {
            result.put("name", record.getName());
        }

        for (NameValueString nv : record.getAttributes()) {
            result.put(nv.getName(), nv.getValue());
        }
        return result;
    }

    /**
     * This is AnswerConnect specific new API method which allows to
     * search assets for a product based on status of the assets.
     * Caller of this API can only specify one BasicFilter inside the criteria that of status.
     * Any additional filter will be removed. Also the status filter is required to be specified.
     *
     * @param productId
     * @param criteria
     * @return AssetSearchResult
     */
    @Override
    public AssetSearchResult findProductAssetsByStatus(int productId, SearchCriteria criteria)  {

        if (null != criteria && null != criteria.getFilters()) {

            if (criteria.getFilters().length == 0) {
                // no filter has been specified, hence return without any result
                return null;
            }

            // replace the field name in filter as status since this API is just filtering by status
            criteria.getFilters()[0].setField("status");

            // make sure any other filters sent to this API are removed before invoking generic findAssets API.
            for (int i=criteria.getFilters().length; i>1; i--) {
                criteria.getFilters()[i-1] = null;
            }

        } else {

            // either no criteria or no filter is specified in the criteria
            // return without any results
            return null;
        }

        // finally call the generic findAssets API with status filter
        return new AssetBL().findAssets(productId, criteria);
    }

    /*
     *   Asset Helper Methods
     */
    @Override
    public Integer createRouteRateCard(RouteRateCardWS routeRateCardWS,
            File routeRateCardFile)  {
        RouteRateCardDTO routeRateCardDTO = RouteBasedRateCardBL.getRouteRateCardDTO(routeRateCardWS, getCallerCompanyId());
        routeRateCardDTO.setCompany(EntityBL.getDTO(getCompany()));
        routeRateCardDTO.setName(routeRateCardWS.getName());
        routeRateCardDTO.setTableName(new RouteBasedRateCardBL()
        .createRouteRateCardTableName(getCallerCompanyId(),
                routeRateCardWS.getName()));
        return new RouteBasedRateCardBL().create(routeRateCardDTO,
                routeRateCardFile);
    }

    @Override
    public void deleteRouteRateCard(Integer routerRateCardId) {
        new RouteBasedRateCardBL(routerRateCardId).delete();
    }

    @Override
    public void updateRouteRateCard(RouteRateCardWS routeRateCardWS,
            File routeRateCardFile) {

        RouteRateCardDTO routeRateCardDTO = RouteBasedRateCardBL
                .getRouteRateCardDTO(routeRateCardWS, getCallerCompanyId());

        routeRateCardDTO.setName(routeRateCardWS.getName());
        routeRateCardDTO.setTableName(new RouteBasedRateCardBL()
        .createRouteRateCardTableName(getCallerCompanyId(),
                routeRateCardWS.getName()));
        routeRateCardDTO.setCompany(EntityBL.getDTO(getCompany()));
        new RouteBasedRateCardBL(routeRateCardDTO.getId()).update(
                routeRateCardDTO, routeRateCardFile);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteRateCardWS getRouteRateCard(Integer routeRateCardId) {
        RouteRateCardDAS das = new RouteRateCardDAS();
        RouteRateCardDTO routeRateCardDTO = das.findNow(routeRateCardId);
        if (routeRateCardId == null || routeRateCardDTO == null) {
            return null;
        }
        RouteBasedRateCardBL bl = new RouteBasedRateCardBL(routeRateCardId);
        return bl.getWS();
    }

    /*
     * Rate Card
     */
    @Override
    public Integer createRateCard(RateCardWS rateCardWs, File rateCardFile) {
        RateCardDTO rateCardDTO = RateCardBL.getDTO(rateCardWs);
        rateCardDTO.setCompany(new CompanyDAS().find(getCallerCompanyId()));
        rateCardDTO.setGlobal(rateCardWs.isGlobal());
        rateCardDTO.setChildCompanies(convertToCompanyDTO(rateCardWs
                .getChildCompanies()));
        return new RateCardBL().create(rateCardDTO, rateCardFile);
    }

    @Override
    public void updateRateCard(RateCardWS rateCardWs, File rateCardFile)
    {
        // Not changing the company of the rate card. It will remain the company
        // that created the ratecard
        RateCardDTO rateCardDTO = RateCardBL.getDTO(rateCardWs);
        rateCardDTO.setGlobal(rateCardWs.isGlobal());
        rateCardDTO.setChildCompanies(convertToCompanyDTO(rateCardWs
                .getChildCompanies()));
        try {
            new RateCardBL(rateCardDTO.getId()).update(rateCardDTO,
                    rateCardFile);
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) { // needed because the sql exception :(
            logger.error("Exception in web service: Updating ratecard", e);
            throw new SessionInternalError("Error updating ratecard");
        }
    }

    @Override
    public void deleteRateCard(Integer rateCardId) {
        new RateCardBL(rateCardId).delete();
    }

    /*
     * Rating Unit
     */

    @Override
    public Integer createRatingUnit(RatingUnitWS ratingUnitWS)
    {

        RatingUnitDTO ratingUnitDTO = RatingUnitBL.getDTO(ratingUnitWS,getCallerCompanyId());
        ratingUnitDTO = new RatingUnitBL().create(ratingUnitDTO);

        return ratingUnitDTO.getId();
    }

    @Override
    public void updateRatingUnit(RatingUnitWS ratingUnitWS)
    {

        RatingUnitDTO ratingUnitDTO = RatingUnitBL.getDTO(ratingUnitWS,getCallerCompanyId());
        new RatingUnitBL().update(ratingUnitDTO);

    }

    @Override
    public boolean deleteRatingUnit(Integer ratingUnitId)
    {
        try {
            RatingUnitBL bl = new RatingUnitBL(ratingUnitId);
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RatingUnitWS getRatingUnit(Integer ratingUnitId)
    {

        RatingUnitDAS das = new RatingUnitDAS();
        RatingUnitDTO ratingUnitDTO = das.findNow(ratingUnitId);
        if (ratingUnitId == null || ratingUnitDTO == null) {
            return null;
        }
        RatingUnitBL bl = new RatingUnitBL(ratingUnitId);
        return bl.getWS();
    }

    @Override
    public RatingUnitWS[] getAllRatingUnits()  {
        RatingUnitDAS das = new RatingUnitDAS();
        List<RatingUnitDTO> types = das.findAll(getCallerCompanyId());
        RatingUnitWS[] wsTypes = new RatingUnitWS[types.size()];
        for (int i = 0; i < types.size(); i++) {
            wsTypes[i] = RatingUnitBL.getWS(types.get(i));
        }
        return wsTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public AssetAssignmentWS[] getAssetAssignmentsForAsset(Integer assetId) {
        AssetAssignmentDAS assignmentDAS = new AssetAssignmentDAS();
        List<AssetAssignmentDTO> assignments = assignmentDAS
                .getAssignmentsForAsset(assetId);
        return AssetAssignmentBL.toWS(assignments);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetAssignmentWS[] getAssetAssignmentsForOrder(Integer orderId) {
        AssetAssignmentDAS assignmentDAS = new AssetAssignmentDAS();
        List<AssetAssignmentDTO> assignments = assignmentDAS
                .getAssignmentsForOrder(orderId);
        return AssetAssignmentBL.toWS(assignments);
    }

    @Override
    public Integer findOrderForAsset(Integer assetId, Date date) {
        if (null == assetId)
        {
            return null;// mandatory parameter
        }
        OrderDTO order = (new AssetAssignmentBL()).findOrderForAsset(assetId,
                date);
        return null != order ? order.getId() : null;
    }

    @Override
    public Integer[] findOrdersForAssetAndDateRange(Integer assetId,
            Date startDate, Date endDate) {
        if (null == assetId || null == startDate || null == endDate) {
            return null;
        }
        List<OrderDTO> orders = (new AssetAssignmentBL())
                .findOrdersForAssetAndDateRange(assetId, startDate, endDate);
        Integer[] ids = new Integer[orders.size()];
        for (int i = 0; i < orders.size(); i++) {
            ids[i] = orders.get(i).getId();
        }
        return ids;
    }

    @Override
    public Integer createMetaFieldGroup(MetaFieldGroupWS metafieldGroupWs) {

        MetaFieldGroup mfGroup = MetaFieldGroupBL.getDTO(metafieldGroupWs);
        mfGroup.setDateCreated(companyCurrentDate());
        mfGroup.setDateUpdated(companyCurrentDate());
        mfGroup.setEntityId(getCallerCompanyId());

        Integer id = new MetaFieldGroupBL(mfGroup).save();
        mfGroup.setId(id);
        List<InternationalDescriptionWS> descriptions = metafieldGroupWs
                .getDescriptions();
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() != null
                    && description.getContent() != null) {
                if (description.isDeleted()) {
                    mfGroup.deleteDescription(description.getLanguageId());
                } else {
                    mfGroup.setDescription(description.getContent(),
                            description.getLanguageId());
                }
            }
        }

        return id;

    }

    @Override
    public void updateMetaFieldGroup(MetaFieldGroupWS metafieldGroupWs) {
        MetaFieldGroup mfGroup = MetaFieldGroupBL.getDTO(metafieldGroupWs);
        mfGroup.setEntityId(getCallerCompanyId());
        new MetaFieldGroupBL().update(mfGroup);

    }

    @Override
    public void deleteMetaFieldGroup(Integer metafieldGroupId) {
        MetaFieldGroupBL metafieldGroupBL = new MetaFieldGroupBL();
        metafieldGroupBL.set(metafieldGroupId);
        // security check for metafieldGroup belongs to caller company
        if (metafieldGroupBL.getEntity() != null
                && metafieldGroupBL.getEntity().getEntityId().equals(getCallerCompanyId())) {
            metafieldGroupBL.delete();
        } else {
            throw new SessionInternalError("MetaField Group not found [" + metafieldGroupId + "]", new String[]
                    {"MetaFieldGroup,metafieldgroup,cannot.delete.metafieldgroup.error," + metafieldGroupId});

        }
    }

    @Override
    @Transactional(readOnly = true)
    public MetaFieldGroupWS getMetaFieldGroup(Integer metafieldGroupId) {
        MetaFieldGroupBL metafieldGroupBL = new MetaFieldGroupBL();
        metafieldGroupBL.set(metafieldGroupId);
        MetaFieldGroupWS metaFieldGroupWS = null;
        try {
            // security check for metafieldGroup belongs to caller company
            if (metafieldGroupBL.getEntity() != null
                    && Objects.equals(metafieldGroupBL.getEntity().getEntityId(), getCallerCompanyId())) {
                metaFieldGroupWS = MetaFieldGroupBL.getWS(metafieldGroupBL
                        .getEntity());
            }
        } catch (Exception e) {
            throw new SessionInternalError(
                    "Exception retrieving MetaFieldGroup object",
                    e,
                    new String[] { "MetaFieldGroup,,cannot.get.metafieldgroup.error" });

        }
        return metaFieldGroupWS;
    }

    @Override
    public Integer createMetaField(MetaFieldWS metafieldWs) {

        if (metafieldWs.getDataType().equals(DataType.SCRIPT)
                && (null == metafieldWs.getFilename() || metafieldWs
                .getFilename().isEmpty())) {
            throw new SessionInternalError(
                    "Script Meta Fields must define filename",
                    new String[] { "MetaFieldWS,filename,metafield.validation.filename.required" },
                    HttpStatus.SC_BAD_REQUEST);
        }

        MetaField metafield = MetaFieldBL.getDTO(metafieldWs,
                getCallerCompanyId());
        metafield = new MetaFieldBL().create(metafield);

        if (metafield != null) {
            return metafield.getId();
        } else {
            throw new SessionInternalError(
                    "MetaField can't be created",
                    new String[] { "MetaField,metafield,cannot.save.metafield.error" });
        }
    }

    @Override
    public Integer createMetaFieldWithEntityId(MetaFieldWS metafieldWs, Integer entityId) {
        if (metafieldWs.getDataType().equals(DataType.SCRIPT)
                && (null == metafieldWs.getFilename() || metafieldWs
                .getFilename().isEmpty())) {
            throw new SessionInternalError(
                    "Script Meta Fields must define filename",
                    new String[] { "MetaFieldWS,filename,metafield.validation.filename.required" });
        }

        MetaField metafield = MetaFieldBL.getDTO(metafieldWs,
                entityId);
        metafield = new MetaFieldBL().create(metafield);

        if (metafield != null) {
            return metafield.getId();
        } else {
            throw new SessionInternalError(
                    "MetaField can't be created",
                    new String[] { "MetaField,metafield,cannot.save.metafield.error" });
        }
    }

    @Override
    public void updateMetaField(MetaFieldWS metafieldWs) {

        if (metafieldWs.getDataType().equals(DataType.SCRIPT)
                && (null == metafieldWs.getFilename() || metafieldWs
                .getFilename().isEmpty())) {
            throw new SessionInternalError(
                    "Script Meta Fields must define filename",
                    new String[] { "MetaFieldWS,filename,metafield.validation.filename.required" },
                    HttpStatus.SC_BAD_REQUEST);
        }

        MetaField metafield = MetaFieldBL.getDTO(metafieldWs,
                getCallerCompanyId());
        new MetaFieldBL().update(metafield);
    }

    @Override
    public void deleteMetaField(Integer metafieldId) {
        MetaFieldBL metafieldBL = new MetaFieldBL();
        MetaField metafield = MetaFieldBL.getMetaField(metafieldId);
        try {

            // security check for metafieldGroup belongs to caller company
            if (metafield != null
                    && metafield.getEntityId().equals(getCallerCompanyId())) {
                metafieldBL.deleteIfNotParticipant(metafieldId);

            } else {
                throw new SessionInternalError("MetaField not found [" + metafield.getId() + "]", new String[]
                        {"MetaField,value,not.found.metafield.error," + metafield.getId()});

            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MetaFieldWS getMetaField(Integer metafieldId) {

        MetaField metafield = new MetaFieldDAS().findNow(metafieldId);
        try {
            // security check for metafieldGroup belongs to caller company
            if (metafield != null && Objects.equals(metafield.getEntityId(), getCallerCompanyId())) {
                return MetaFieldBL.getWS(metafield);
            }
        } catch (Exception e) {
            throw new SessionInternalError(
                    "Exception converting MetaField to WS object",
                    new String[]{"MetaField,metafield,cannot.convert.metafield.error"},
                    HttpStatus.SC_NOT_FOUND);
        }

        // A meta field with the provided id was not found
        throw new SessionInternalError(
                "MetaField not found",
                new String[]{"MetaField,metafield,not.found.metafield.error"},
                HttpStatus.SC_NOT_FOUND);
    }

    @Override
    @Transactional(readOnly = true)
    public MetaFieldWS[] getMetaFieldsForEntity(String entityType) {
        List<MetaField> metaFields = MetaFieldBL.getAvailableFieldsList(
                getCallerCompanyId(),
                new EntityType[]{EntityType.valueOf(entityType)});
        return MetaFieldBL.convertMetaFieldsToWS(metaFields);
    }

    @Override
    @Transactional(readOnly = true)
    public MetaFieldGroupWS[] getMetaFieldGroupsForEntity(String entityType) {
        List<MetaFieldGroup> metaFieldGroups = new MetaFieldGroupBL()
        .getAvailableFieldGroups(getCallerCompanyId(),
                EntityType.valueOf(entityType));
        return MetaFieldGroupBL.convertMetaFieldGroupsToWS(metaFieldGroups);
    }

    @Override
    @Transactional(readOnly = true)
    public MetaFieldWS[] getMetaFieldsByEntityId(Integer entityId, String entityType) {
        List<MetaField> metaFields = MetaFieldBL.getAvailableFieldsList(
                entityId,
                new EntityType[]{EntityType.valueOf(entityType)});
        return MetaFieldBL.convertMetaFieldsToWS(metaFields);
    }

    @Override
    public OrderWS processJMRData(UUID processId, String recordKey,
            Integer userId, Integer currencyId, Date eventDate,
            String description, Integer productId, String quantity,
            String pricing) {
        Integer configId = mediationProcessService.getCfgIdForMediattionProcessId(processId);
        JbillingMediationRecord jmr = new JbillingMediationRecord(JbillingMediationRecord.STATUS.UNPROCESSED, JbillingMediationRecord.TYPE.MEDIATION, getCallerCompanyId()
                , configId, recordKey, userId, eventDate, new BigDecimal(quantity), description, currencyId, productId, null, null, null, new BigDecimal(pricing), null, processId, null, null
                , null, null);
        return processJMRRecord(processId, jmr);
    }

    @Override
    public OrderWS processJMRRecord(UUID processId,
            JbillingMediationRecord JMR) {
        MediationProcess mediationProcess = mediationProcessService.getMediationProcess(processId);
        if (mediationProcess != null) {
            throw new IllegalArgumentException("Mediation process for id:" + processId + " don't exist.");
        }
        MediationConfiguration mediationConfiguration = new MediationConfigurationDAS().find(mediationProcess.getConfigurationId());
        MediationContext mediationContext = new MediationContext();
        mediationContext.setMediationCfgId(mediationConfiguration.getId());
        mediationContext.setJobName(mediationConfiguration.getMediationJobLauncher());
        mediationContext.setProcessIdForMediation(processId);
        mediationContext.setRecordToProcess(JMR);
        return getOrder(mediationService.launchMediation(mediationContext).get(0).getOrderId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID processCDR(Integer configId, List<String> callDataRecords) {
        MediationConfiguration mediationConfiguration = new MediationConfigurationDAS().find(configId);
        mediationService.processCdr(getCallerCompanyId(), configId,
                mediationConfiguration.getMediationJobLauncher(), StringUtils.join(callDataRecords, "\n"));
        return mediationProcessService.getLastMediationProcessId(getCallerCompanyId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID processCDRChecked(Integer configId, List<String> callDataRecords) {
        if (isMediationProcessRunning()) {
            throw new SessionInternalError("The mediation process is currently running",
                    new String[] { "error.mediation.running" }, HttpStatus.SC_CONFLICT);
        }
        return processCDR(configId, callDataRecords);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID runRecycleForConfiguration(Integer configId) {
        String jobName = getJobNameForMediationCfg(configId);
        Integer entityId = getCallerCompanyId();
        return mediationService.triggerRecycleCdrAsync(entityId, configId, jobName, null);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID runRecycleForMediationProcess(UUID processId) {
        int cfgId = mediationProcessService.getCfgIdForMediattionProcessId(processId);
        String jobName = getJobNameForMediationCfg(cfgId);
        Integer entityId = getCallerCompanyId();

        return mediationService.triggerRecycleCdrAsync(entityId, cfgId, jobName, processId);
    }

    private String getJobNameForMediationCfg(int configId) {
        MediationConfigurationDAS mediationConfigurationDAS = new MediationConfigurationDAS();
        MediationConfiguration mediationConfiguration = mediationConfigurationDAS.find(configId);
        return mediationConfiguration.getMediationJobLauncher();
    }

    /*
     * Diameter Protocol
     */

    @Override
    public DiameterResultWS createSession(String sessionId, Date timestamp,
            BigDecimal units, String data)  {
        logger.debug(
                "Parameters received: sessionId - {}, timestamp - {}, units - {}, data - {}",
                sessionId, timestamp, units, data);

        try {
            DiameterUserLocator userLoc = Context
                    .getBean(Context.Name.DIAMETER_USER_LOCATOR);
            DiameterItemLocator itemLoc = Context
                    .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
            DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                    getCallerCompanyId());

            PricingFieldsHelper fieldsHelper = new PricingFieldsHelper(data);

            return diameter.createSession(sessionId, timestamp, units,
                    fieldsHelper.getFields());
        } catch (Exception ex) {
            return new DiameterResultWS(
                    DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
        }
    }

    @Override
    public DiameterResultWS reserveUnits(String sessionId, Date timestamp,
            int units, String data)  {
        try {
            DiameterUserLocator userLoc = Context
                    .getBean(Context.Name.DIAMETER_USER_LOCATOR);
            DiameterItemLocator itemLoc = Context
                    .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
            DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                    getCallerCompanyId());

            PricingFieldsHelper fieldsHelper = new PricingFieldsHelper(data);

            return diameter.reserveUnits(sessionId, timestamp, units,
                    fieldsHelper.getFields());
        } catch (Exception ex) {
            return new DiameterResultWS(
                    DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
        }
    }

    @Override
    public DiameterResultWS updateSession(String sessionId, Date timestamp,
            BigDecimal usedUnits, BigDecimal reqUnits, String data)
    {
        try {
            DiameterUserLocator userLoc = Context
                    .getBean(Context.Name.DIAMETER_USER_LOCATOR);
            DiameterItemLocator itemLoc = Context
                    .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
            DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                    getCallerCompanyId());

            PricingFieldsHelper fieldsHelper = new PricingFieldsHelper(data);

            return diameter.updateSession(sessionId, timestamp, usedUnits,
                    reqUnits, fieldsHelper.getFields());
        } catch (Exception ex) {
            return new DiameterResultWS(
                    DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
        }
    }

    @Override
    public DiameterResultWS extendSession(String sessionId, Date timestamp,
            BigDecimal usedUnits, BigDecimal reqUnits)
    {
        try {
            DiameterUserLocator userLoc = Context
                    .getBean(Context.Name.DIAMETER_USER_LOCATOR);
            DiameterItemLocator itemLoc = Context
                    .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
            DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                    getCallerCompanyId());
            return diameter.extendSession(sessionId, timestamp, usedUnits,
                    reqUnits);
        } catch (Exception ex) {
            return new DiameterResultWS(
                    DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
        }
    }

    @Override
    public DiameterResultWS endSession(String sessionId, Date timestamp,
            BigDecimal usedUnits, int causeCode)  {
        DiameterUserLocator userLoc = Context
                .getBean(Context.Name.DIAMETER_USER_LOCATOR);
        DiameterItemLocator itemLoc = Context
                .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
        DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                getCallerCompanyId());
        return diameter.endSession(sessionId, timestamp, usedUnits, causeCode);
    }

    @Override
    public DiameterResultWS consumeReservedUnits(String sessionId,
            Date timestamp, int usedUnits, int causeCode)
    {
        DiameterUserLocator userLoc = Context
                .getBean(Context.Name.DIAMETER_USER_LOCATOR);
        DiameterItemLocator itemLoc = Context
                .getBean(Context.Name.DIAMETER_ITEM_LOCATOR);
        DiameterBL diameter = new DiameterBL(userLoc, itemLoc,
                getCallerCompanyId());
        return diameter.consumeReservedUnits(sessionId, timestamp, usedUnits,
                causeCode);
    }

    /**
     *
     * @param entities
     * @return
     */
    private Set<CompanyDTO> convertToCompanyDTO(List<Integer> entities) {
        Set<CompanyDTO> childEntities = new HashSet<CompanyDTO>(0);

        for (Integer entity : entities) {
            childEntities.add(new CompanyDAS().find(entity));
        }

        return childEntities;
    }

    private void validateItemMandatoryDependenciesCycle(Integer rootItemId,
            Collection<Integer> dependencies) {
        if (dependencies == null || dependencies.isEmpty()
                || rootItemId == null) {
            return;
        }
        if (dependencies.contains(rootItemId)) {
            String errorCode = "ItemDTOEx,mandatoryItems,product.error.dependencies.cycle";
            throw new SessionInternalError(
                    "Cycle in product mandatory dependencies was found",
                    new String[] { errorCode }, HttpStatus.SC_BAD_REQUEST);
        }
        ItemDAS itemDas = new ItemDAS();
        for (Integer dependentItemId : dependencies) {
            ItemDTO item = itemDas.find(dependentItemId);
            if (item != null && item.getDependencies() != null
                    && !item.getDependencies().isEmpty()) {
                List<Integer> childDependencies = new LinkedList<Integer>();
                for (ItemDependencyDTO dependencyDTO : item.getDependencies()) {
                    if (dependencyDTO.getType().equals(ItemDependencyType.ITEM)
                            && dependencyDTO.getMinimum() > 0) {
                        childDependencies.add(dependencyDTO
                                .getDependentObjectId());
                    }
                }
                validateItemMandatoryDependenciesCycle(rootItemId,
                        childDependencies);
            }
        }
    }

    @Override
    public Integer createOrUpdateDiscount(DiscountWS discount) {
        Integer languageId = getCallerLanguageId();
        discount.setEntityId(getCallerCompanyId());

        DiscountBL bl = new DiscountBL();
        return bl.createOrUpdate(discount, languageId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPeriodWS[] getOrderPeriods()  {

        Integer entityId = getCallerCompanyId();

        OrderPeriodDAS periodDas = new OrderPeriodDAS();
        List<OrderPeriodDTO> orderPeriods = periodDas.getOrderPeriods(entityId);
        OrderPeriodWS[] periods = new OrderPeriodWS[orderPeriods.size()];
        int index = 0;
        for (OrderPeriodDTO periodDto : orderPeriods) {
            periods[index++] =OrderBL.getOrderPeriodWS(periodDto);
        }

        return periods;
    }

    /**
     * Select orderChangeStatuses for current entity
     *
     * @return List of orderChangeStatuses
     */
    @Override
    @Transactional(readOnly = true)
    public OrderChangeStatusWS[] getOrderChangeStatusesForCompany() {
        List<OrderChangeStatusDTO> statusDTOs = new OrderChangeStatusDAS()
        .findOrderChangeStatuses(getCallerCompanyId());
        List<OrderChangeStatusWS> results = new LinkedList<OrderChangeStatusWS>();
        List<LanguageDTO> languages = new LanguageDAS().findAll();
        for (OrderChangeStatusDTO status : statusDTOs) {
            OrderChangeStatusWS ws = OrderChangeStatusBL.getWS(status);
            for (LanguageDTO lang : languages) {
                if (ws.getDescription(lang.getId()) != null) {
                    continue;
                }
                InternationalDescriptionDTO descriptionDTO = status
                        .getDescriptionDTO(lang.getId());
                if (descriptionDTO != null
                        && descriptionDTO.getContent() != null) {
                    ws.addDescription(DescriptionBL.getInternationalDescriptionWS(
                            descriptionDTO));
                }
            }
            results.add(ws);
        }
        return results.toArray(new OrderChangeStatusWS[results.size()]);
    }

    /**
     * Create orderChangeStatus with validation
     *
     * @param orderChangeStatusWS
     *            input OrderChangeStatus
     * @return id of OrderChangeStatus created
     * @
     *             if validation fails
     */
    @Override
    public Integer createOrderChangeStatus(
            OrderChangeStatusWS orderChangeStatusWS)
    {
        OrderChangeStatusDTO orderChangeStatusDTO = OrderChangeStatusBL.getDTO(orderChangeStatusWS);
        orderChangeStatusDTO = OrderChangeStatusBL.createOrderChangeStatus(
                orderChangeStatusDTO, getCallerCompanyId());

        if (orderChangeStatusWS.getDescriptions() != null
                && orderChangeStatusWS.getDescriptions().size() > 0) {
            for (InternationalDescriptionWS desc : orderChangeStatusWS
                    .getDescriptions()) {
                if (!OrderChangeStatusBL.isDescriptionUnique(
                        getCallerCompanyId(), orderChangeStatusDTO.getId(),
                        desc.getLanguageId(), desc.getContent())) {
                    String[] errorMessages = new String[] { "OrderChangeStatusWS,descriptions,orderChangeStatusWS.error.unique.name" };
                    throw new SessionInternalError(
                            "Order Change Status validation error",
                            errorMessages,
                            HttpStatus.SC_BAD_REQUEST);
                }
                orderChangeStatusDTO.setDescription(desc.getContent(),
                        desc.getLanguageId());
            }
        }

        return orderChangeStatusDTO.getId();
    }

    /**
     * Update orderChangeStatus with validation
     *
     * @param orderChangeStatusWS
     *            input updated OrderChangeStatus
     * @
     *             if validation fails
     */

    @Override
    public void updateOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS)
    {
        OrderChangeStatusDTO orderChangeStatusDTO = OrderChangeStatusBL.getDTO(orderChangeStatusWS);
        OrderChangeStatusBL.updateOrderChangeStatus(orderChangeStatusDTO,
                getCallerCompanyId());
        orderChangeStatusDTO = new OrderChangeStatusDAS()
        .find(orderChangeStatusDTO.getId());

        if (CollectionUtils.isNotEmpty(orderChangeStatusWS.getDescriptions())) {
            for (InternationalDescriptionWS desc : orderChangeStatusWS
                    .getDescriptions()) {
                if (!OrderChangeStatusBL.isDescriptionUnique(
                        getCallerCompanyId(), orderChangeStatusDTO.getId(), desc.getLanguageId(), desc.getContent())) {
                    String[] errorMessages = new String[] { "OrderChangeStatusWS,descriptions,orderChangeStatusWS.error.unique.name" };
                    throw new SessionInternalError(
                            "Order Change Status validation error",
                            errorMessages,
                            HttpStatus.SC_BAD_REQUEST);
                }
                orderChangeStatusDTO.setDescription(desc.getContent(),
                        desc.getLanguageId());
            }
        }
    }

    @Override
    public void deleteOrderChangeStatus(Integer id)  {
        OrderChangeStatusBL.deleteOrderChangeStatus(id, getCallerCompanyId());
    }

    /**
     * Create, update or delete orderChangeSatuses
     *
     * @param orderChangeStatuses
     *            array of ws objects for create/update/delete
     * @
     *             if some operation fails
     */
    @Override
    public void saveOrderChangeStatuses(
            OrderChangeStatusWS[] orderChangeStatuses)
    {
        for (OrderChangeStatusWS ws : orderChangeStatuses) {
            if (ws.getId() != null && ws.getId() > 0) {
                if (ws.getDeleted() > 0) {
                    deleteOrderChangeStatus(ws.getId());
                } else {
                    updateOrderChangeStatus(ws);
                }
            } else {
                createOrderChangeStatus(ws);
            }
        }
    }

    /**
     * This method checks whether the paymentInstruments are unique or not. It
     * tries to add all the paymentInstruments in a HashSet. If any one of the
     * additions fails, this means that the payment instrument already exists
     *
     * @param paymentInstruments
     * @
     */
    private void validateUniquePaymentInstruments(List<PaymentInformationWS> paymentInstruments) {
        Set<PaymentInformationWS> validatorSet = new HashSet<>();
        try {
            for (PaymentInformationWS paymentInstrument : paymentInstruments) {
                if (!validatorSet.add(paymentInstrument)) {
                    throw new SessionInternalError(
                            "Duplicate payment method not allowed",
                            new String[]{"PaymentWS,paymentMethodId,validation.error.duplicate.payment.method"}, HttpStatus.SC_BAD_REQUEST);
                }
            }
        } catch (Exception exception) {
            logger.debug("Exception: "+ exception);
        }
    }

    /**
     * Select OrderChangeTypes for current entity
     *
     * @return List of OrderChangeTypeWS found
     */
    @Override
    @Transactional(readOnly = true)
    public OrderChangeTypeWS[] getOrderChangeTypesForCompany() {
        List<OrderChangeTypeWS> result = new LinkedList<>();
        for (OrderChangeTypeDTO dto : new OrderChangeTypeDAS()
        .findOrderChangeTypes(getCallerCompanyId())) {
            result.add(OrderChangeTypeBL.getWS(dto));
        }
        return result.toArray(new OrderChangeTypeWS[result.size()]);
    }

    /**
     * Find OrderChangeType by name for current entity
     *
     * @param name
     *            name for search
     * @return OrderChangeType found or null
     */
    @Override
    @Transactional(readOnly = true)
    public OrderChangeTypeWS getOrderChangeTypeByName(String name) {
        OrderChangeTypeDTO dto = new OrderChangeTypeDAS()
        .findOrderChangeTypeByName(name, getCallerCompanyId());
        return dto != null ? OrderChangeTypeBL.getWS(dto) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderChangeTypeWS getOrderChangeTypeById(Integer orderChangeTypeId) {

        OrderChangeTypeDTO dto = new OrderChangeTypeDAS().findNow(orderChangeTypeId);
        return dto != null ? OrderChangeTypeBL.getWS(dto) : null;
    }

    @Override
    public Integer createUpdateOrderChangeType(
            OrderChangeTypeWS orderChangeTypeWS) {
        OrderChangeTypeDTO dto = OrderChangeTypeBL.getDTO(orderChangeTypeWS,
                getCallerCompanyId());
        OrderChangeTypeBL changeTypeBL = new OrderChangeTypeBL();
        OrderChangeTypeWS existedTypeWithSameName = getOrderChangeTypeByName(dto
                .getName());
        // name should be unique within entity
        if (existedTypeWithSameName != null
                && (dto.getId() == null || !dto.getId().equals(
                        existedTypeWithSameName.getId()))) {
            throw new SessionInternalError(
                    "Order Change Type validation failed: name is not unique",
                    new String[] { "OrderChangeTypeWS,name,OrderChangeTypeWS.validation.error.name.not.unique,"
                            + dto.getName()},
                            HttpStatus.SC_BAD_REQUEST);
        }

        return changeTypeBL.createUpdateOrderChangeType(dto,
                getCallerCompanyId());
    }

    @Override
    public void deleteOrderChangeType(Integer orderChangeTypeId) {
        OrderChangeTypeDAS das = new OrderChangeTypeDAS();
        if (das.isOrderChangeTypeInUse(orderChangeTypeId)) {
            throw new SessionInternalError(
                    "Order Change Type validation failed: name is not unique",
                    new String[] { "OrderChangeTypeWS.delete.error.type.in.use" });
        }
        new OrderChangeTypeBL().delete(orderChangeTypeId, getCallerCompanyId());
    }

    /**
     * Select order changes for order
     *
     * @param orderId
     *            target order id for changes select
     * @return List of orderChangeWS objects
     */
    @Override
    @Transactional(readOnly = true)
    public OrderChangeWS[] getOrderChanges(Integer orderId) {
        List<OrderChangeDTO> orderChanges = new OrderChangeDAS()
        .findByOrder(orderId);
        List<OrderChangeWS> result = new LinkedList<>();
        Map<OrderChangeDTO, OrderChangeWS> dtoToWsMap = new HashMap<>();
        OrderWS orderWS = (!orderChanges.isEmpty()) ? new OrderBL(orderId).getWS(getCallerLanguageId()) : null;
        for (OrderChangeDTO dto : orderChanges) {
            OrderChangeWS ws = dtoToWsMap.get(dto);
            if (ws == null) {
                ws = OrderChangeBL
                        .getWS(dto, getCallerLanguageId(), dtoToWsMap);
                ws.setOrderWS(orderWS);
            }
            result.add(ws);
        }
        return result.toArray(new OrderChangeWS[result.size()]);
    }

    /*
     * Payment Methods
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentMethodTemplateWS getPaymentMethodTemplate(Integer templateId) {
        PaymentMethodTemplateDAS das = new PaymentMethodTemplateDAS();
        PaymentMethodTypeBL bl = new PaymentMethodTypeBL();

        PaymentMethodTemplateDTO dto = das.findNow(templateId);

        if (templateId == null || dto == null) {
            return null;
        }
        return bl.getWS(dto, getCallerCompanyId());
    }

    /**
     * Create a payment method type
     *
     * @param paymentMethodType
     *            instance
     * @return Id of created payment method type
     */
    @Override
    public Integer createPaymentMethodType(PaymentMethodTypeWS paymentMethodType) {
        if (paymentMethodType.getMetaFields() != null) {
            for (MetaFieldWS field : paymentMethodType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] {"PaymentMethodTypeWS,metaFields,metafield.validation.filename.required"}, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        PaymentMethodTypeBL bl = new PaymentMethodTypeBL(paymentMethodType);

        List<PaymentMethodTypeDTO> paymentMethodTypeDTOs = new PaymentMethodTypeDAS()
        .findByMethodName(paymentMethodType.getMethodName().trim(),
                getCallerCompanyId());

        if (paymentMethodTypeDTOs != null && paymentMethodTypeDTOs.size() > 0) {
            throw new SessionInternalError(
                    "Payment Method Type already exists with method name "
                            + paymentMethodType.getMethodName(),
                            new String[] {"PaymentMethodTypeWS,methodName,validation.error.methodname.already.exists"}, HttpStatus.SC_BAD_REQUEST);
        }

        PaymentMethodTypeDTO dto = bl.getDTO(getCallerCompanyId());
        dto = bl.create(dto);

        return dto.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningCommandWS[] getProvisioningCommands(
            ProvisioningCommandType type, Integer typeId) {
        if (type != null) {
            ProvisioningCommandBL bl = new ProvisioningCommandBL();
            List<ProvisioningCommandWS> commandsList = bl.getCommandWSList(
                    type, typeId);
            return (null == commandsList) ? null : commandsList
                    .toArray(new ProvisioningCommandWS[commandsList.size()]);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningCommandWS getProvisioningCommandById(
            Integer provisioningCommandId) {

        ProvisioningCommandBL bl = new ProvisioningCommandBL(
                provisioningCommandId);
        return bl.getCommandWS(bl.getProvisioningCommandDTO());
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningRequestWS[] getProvisioningRequests(
            Integer provisioningCommandId) {
        if (provisioningCommandId != null) {
            ProvisioningCommandBL cmdBL = new ProvisioningCommandBL();
            List<ProvisioningRequestWS> requestList = cmdBL
                    .getRequestList(provisioningCommandId);
            return (null == requestList) ? null : requestList
                    .toArray(new ProvisioningRequestWS[requestList.size()]);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningRequestWS getProvisioningRequestById(
            Integer provisioningRequestId) {

        ProvisioningRequestBL bl = new ProvisioningRequestBL(
                provisioningRequestId);
        return bl.getProvisioningRequestWS(bl.getProvisioningRequest());
    }

    @Transactional(readOnly = true)
    public List<ProvisioningCommandWS> getCommandsByEntityId(Integer entityId)
    {
        ProvisioningCommandDAS das = new ProvisioningCommandDAS();
        ProvisioningCommandBL bl = new ProvisioningCommandBL();
        List<ProvisioningCommandDTO> cmdList = das
                .findCommandsByEntityId(entityId);
        List<ProvisioningCommandWS> cmdWSList = new ArrayList<ProvisioningCommandWS>();
        if (cmdList != null) {
            for (ProvisioningCommandDTO dto : cmdList) {
                ProvisioningCommandWS ws = bl.getCommandWS(dto);
                cmdWSList.add(ws);
            }
        }
        return cmdWSList;
    }

    @Transactional(readOnly = true)
    public List<ProvisioningCommandWS> getConvertedProvisioningTypeWS(
            List<ProvisioningCommandDTO> cmdList) {
        ProvisioningCommandBL bl = new ProvisioningCommandBL();
        List<ProvisioningCommandWS> cmdWSList = new ArrayList<ProvisioningCommandWS>();
        if (cmdList != null) {
            for (ProvisioningCommandDTO dto : cmdList) {
                ProvisioningCommandWS ws = bl.getCommandWS(dto);
                cmdWSList.add(ws);
            }
        }
        return cmdWSList;
    }

    public CustomerUsagePoolDTO createCustomerUsagePool(CustomerUsagePoolWS ws) {
        CustomerUsagePoolDTO customerUsagePoolDto = CustomerUsagePoolBL.getDTO(ws);
        CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
        return bl.createOrUpdateCustomerUsagePool(customerUsagePoolDto);
    }

    public CustomerUsagePoolDTO updateCustomerUsagePool(
            CustomerUsagePoolWS customerUsagePoolWs) {
        CustomerUsagePoolDTO customerUsagePoolDto = CustomerUsagePoolBL.getDTO(customerUsagePoolWs);
        CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
        return bl.createOrUpdateCustomerUsagePool(customerUsagePoolDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerUsagePoolDTO> getCustomerUsagePools(Integer customerId) {

        if (customerId == null) {
            throw new SessionInternalError("Customer Id cannot be null.");
        }
        List<CustomerUsagePoolDTO> customerUsagePools = new CustomerUsagePoolDAS()
        .findCustomerUsagePoolByCustomerId(customerId);

        Collections
        .sort(customerUsagePools,
                CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);

        return customerUsagePools;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerUsagePoolWS getCustomerUsagePoolById(
            Integer customerUsagePoolId) {
        CustomerUsagePoolBL customerUsagePoolBl = new CustomerUsagePoolBL(
                customerUsagePoolId);
        CustomerUsagePoolWS customerUsagePoolWS = customerUsagePoolBl
                .getWS(customerUsagePoolBl.getEntity());
        return customerUsagePoolWS;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerUsagePoolWS[] getCustomerUsagePoolsByCustomerId(Integer customerId) {
        if(!new CustomerDAS().isIdPersisted(customerId)) {
            logger.error("Customer {}, not found for entity {}", customerId, getCallerCompanyId());
            throw new SessionInternalError("customer id not found for entity " + getCallerCompanyId(),
                    new String [] { "Please enter valid customerId." },
                    HttpStatus.SC_NOT_FOUND);
        }
        try {
            List<CustomerUsagePoolDTO> customerUsagePools = new CustomerUsagePoolBL()
            .getCustomerUsagePoolsByCustomerId(customerId);
            if(CollectionUtils.isEmpty(customerUsagePools)) {
                return new CustomerUsagePoolWS[0];
            }
            return customerUsagePools.stream()
                    .map(CustomerUsagePoolBL::getCustomerUsagePoolWS)
                    .toArray(CustomerUsagePoolWS[]::new);
        } catch(Exception ex) {
            logger.error("Error in getCustomerUsagePoolsByCustomerId", ex);
            throw new SessionInternalError("Error in getCustomerUsagePoolsByCustomerId", ex);
        }
    }

    @Override
    public void updatePaymentMethodType(PaymentMethodTypeWS paymentMethodType) {
        if (paymentMethodType.getMetaFields() != null) {
            for (MetaFieldWS field : paymentMethodType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT)
                        && (null == field.getFilename() || field.getFilename()
                        .isEmpty())) {
                    throw new SessionInternalError(
                            "Script Meta Fields must define filename",
                            new String[] {"PaymentMethodTypeWS,metaFields,metafield.validation.filename.required"}, HttpStatus.SC_BAD_REQUEST);
                }
            }
        }

        PaymentMethodTypeBL bl = new PaymentMethodTypeBL(paymentMethodType);
        List<PaymentMethodTypeDTO> paymentMethodTypeDTOs = new PaymentMethodTypeDAS()
        .findByMethodName(paymentMethodType.getMethodName(),
                getCallerCompanyId());
        String originalMethodName = new PaymentMethodTypeDAS().find(
                paymentMethodType.getId()).getMethodName();

        if ((originalMethodName.equals(paymentMethodType.getMethodName())
                && paymentMethodTypeDTOs != null && paymentMethodTypeDTOs
                .size() > 1)
                || (!originalMethodName.equals(paymentMethodType
                        .getMethodName()) && paymentMethodTypeDTOs != null && paymentMethodTypeDTOs
                        .size() > 0)) {
            throw new SessionInternalError(
                    "Payment Method Type already exists with method name "
                            + paymentMethodType.getMethodName(),
                            new String[] {"PaymentMethodTypeWS,methodName,validation.error.methodname.already.exists"}, HttpStatus.SC_BAD_REQUEST);
        }
        PaymentInformationDAS paymentInformationDAS = new PaymentInformationDAS();
        PaymentMethodTypeWS existing = new PaymentMethodTypeBL(
                paymentMethodType.getId()).getWS();
        List<Integer> removedMethodType = (List<Integer>) CollectionUtils
                .subtract(existing.getAccountTypes(),
                        paymentMethodType.getAccountTypes());
        for (Integer accountTypeId : removedMethodType) {
            long l = paymentInformationDAS
                    .findByAccountTypeAndPaymentMethodType(accountTypeId,
                            paymentMethodType.getId());
            if (l > 0) {
                throw new SessionInternalError(
                        "",
                        new String[] {"PaymentMethodTypeWS,accountType,validation.error.account.inUse"}, HttpStatus.SC_BAD_REQUEST);
            }
        }
        PaymentMethodTypeDTO dto = bl.getDTO(getCallerCompanyId());

        bl.update(dto);
    }

    /**
     * Gets payment method type and return it after converting to ws object if
     * none is found then null is returned
     *
     * @return PaymentMethodTypeWS
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentMethodTypeWS getPaymentMethodType(Integer paymentMethodTypeId) {
        if (paymentMethodTypeId == null) {
            return null;
        }

        PaymentMethodTypeBL bl = new PaymentMethodTypeBL(paymentMethodTypeId);
        return bl.getWS();
    }

    @Override
    public boolean deletePaymentMethodType(Integer paymentMethodTypeId)
    {
        try {
            PaymentMethodTypeBL bl = new PaymentMethodTypeBL(paymentMethodTypeId);
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
        }
    }

    /**
     * Remove an existing payment instrument
     */
    @Override
    public boolean removePaymentInstrument(Integer instrumentId) {
        PaymentInformationDAS das = new PaymentInformationDAS();
        boolean removed = false;

        PaymentInformationDTO dto = das.findNow(instrumentId);
        if (dto != null) {
            try {
                Integer userId = dto.getUser().getId();
                das.delete(dto);
                removed = true;
                String msg = "Payment instrument: " + instrumentId + " has been removed from user: " + userId;
                String log = getEnhancedLogMessage(msg, LogConstants.MODULE_CUSTOMER,
                        LogConstants.ACTION_DELETE, LogConstants.STATUS_SUCCESS);
                logger.info(log);
            } catch (Exception e) {
                String msg = "Could not delete payment instrument with ID: " + instrumentId;
                String log = getEnhancedLogMessage(msg, LogConstants.MODULE_CUSTOMER,
                        LogConstants.ACTION_DELETE, LogConstants.STATUS_NOT_SUCCESS);
                logger.error(log, "Exception is: " + e);
            }
        } else {
            throw new SessionInternalError("validation failed",
                    new String [] {"Payment instrument not found with given Id"}, HttpStatus.SC_CONFLICT);
        }

        try{
            dto.close();
        }catch(Exception exception){
            logger.debug("Exception: "+exception);
        }
        return removed;
    }

    /*
     * Method added to fix #7375 #7375 - A product gets added by the name of
     * plan when a plan is unsuccessful getting saved, after a FUP is being
     * selected-
     */
    @Transactional
    public Integer createPlan(PlanWS plan, ItemDTOEx product) {
        // #12253 - setting this value to skip product level metafield
        // validation while creating a plan.
        product.setIsPlan(true);
        Integer id = createItem(product);
        product.setId(id);
        plan.setItemId(id);
        return createPlan(plan);
    }

    /*
     * Method added to fix #7375 #7375 - A product gets added by the name of
     * plan when a plan is unsuccessful getting saved, after a FUP is being
     * selected-
     */
    @Transactional
    public void updatePlan(PlanWS plan, ItemDTOEx product) {
        // #12253-setting this value to skip product level metafield
        // validation while creating a plan.
        product.setIsPlan(true);
        updateItem(product);
        updatePlan(plan);

        //cleaning reserve instance cache
        clearReserveCache(DtReserveInstanceCache.RESERVE_CACHE_KEY + getCallerCompanyId());
    }

    @Override
    public void deleteOrderStatus(OrderStatusWS orderStatus) {

        Integer orderStatusId = orderStatus.getId();
        Integer callerCompanyId = getCallerCompanyId();
        if (isOrderStatusAssigned(orderStatusId, callerCompanyId)) {
            throw new SessionInternalError("Cannot Delete. Order Status currently in use.", HttpStatus.SC_CONFLICT);
        } else {
            try {
                OrderStatusBL bl = new OrderStatusBL(orderStatusId);
                bl.delete(callerCompanyId);
            } catch (Exception e) {
                throw new SessionInternalError(e, HttpStatus.SC_CONFLICT);
            }
        }
    }

    private boolean isOrderStatusAssigned(Integer orderStatusId, Integer callerCompanyId) {
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        try {
            return ((orderDAS.orderHasStatus(orderStatusId, callerCompanyId))
                    || (orderChangeDAS.orderChangeHasStatus(orderStatusId)));
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public void deleteEdiFileStatus(Integer ediStatusId)
    {

        if(new EDITypeDAS().countByStatusId(ediStatusId)>0){
            throw new SessionInternalError("Could not delete EDI Status Type",
                    new String[]{"cannot.delete.edi.status.edi.type.using"});
        }

        EDIFileStatusDAS ediFileStatusDAS =new EDIFileStatusDAS();
        EDIFileStatusDTO ediFileStatusDTO= ediFileStatusDAS.find(ediStatusId);
        new EDIFileStatusDAS().delete(ediFileStatusDTO);

    }

    @Override
    public Integer createUpdateOrderStatus(OrderStatusWS orderStatusWS)
    {
        OrderStatusBL orderStatusBL = new OrderStatusBL();
        try {
            if (!orderStatusBL.isOrderStatusValid(orderStatusWS, getCallerCompanyId(), orderStatusWS.getDescription())) {
                throw new SessionInternalError(
                        "Order status exist ",
                        new String[] { "OrderStatusWS,status,validation.error.status.already.exists" });
            }
        } catch (Exception e) {
            throw new SessionInternalError(e, HttpStatus.SC_BAD_REQUEST);
        }
        Integer orderId = orderStatusBL.create(orderStatusWS, getCallerCompanyId(), getCompany().getLanguageId());
        return orderId;
    }

    @Override
    public Integer createUpdateEdiStatus(EDIFileStatusWS ediFileStatusWS)
    {
        EDIFileStatusBL ediFileStatusBL = new EDIFileStatusBL();
        Integer ediStatusId = ediFileStatusBL.create(ediFileStatusWS);
        return ediStatusId;
    }

    @Override
    public EDIFileStatusWS findEdiStatusById(Integer ediStatusId) {
        EDIFileStatusDTO ediFileStatusDTO = new EDIFileStatusDAS().find(ediStatusId);
        return new EDIFileStatusBL().getWS(ediFileStatusDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public int getDefaultOrderStatusId(OrderStatusFlag flag, Integer entityId) {
        // #7853 - If no order statuses are configured via the configuration
        // menu an exception is shown on the 'create order' UI. Following
        // is exception handling added to take care of the issue.
        try {
            return new OrderStatusDAS().getDefaultOrderStatusId(flag, entityId);
        } catch (Exception e) {
            throw new SessionInternalError(
                    "Order validation failed. No order status found for the order",
                    new String[] { "OrderWS,orderStatus,No order status found for the order" });
        }
    }

    /**
     * Check if the order line must have an item based on the type of line item.
     * @param line
     * @return
     */
    private boolean orderLineMustHaveItem(OrderLineWS line) {
        return line.getTypeId() == Constants.ORDER_LINE_TYPE_ITEM
                || line.getTypeId() == Constants.ORDER_LINE_TYPE_PENALTY
                || line.getTypeId() == Constants.ORDER_LINE_TYPE_SUBSCRIPTION
                || line.getTypeId() == Constants.ORDER_LINE_TYPE_ADJUSTMENT;
    }

    private void validateLines(OrderWS order) {
        // #7761 - moved out of validateOrder function due to problem in
        // removing order line
        List<Integer> usedCategories = new ArrayList<>();
        OrderBL orderBl = new OrderBL();
        ItemBL itemBl = new ItemBL();

        if (order.getOrderLines() != null) {

            if (order.getOrderLines().length == 1) {
                OrderLineWS line = order.getOrderLines()[0];
                if (orderLineMustHaveItem(line) && new ItemDAS().find(line.getItemId())
                        .getPercentage() != null) {
                    throw new SessionInternalError(
                            "Order can not create for line percentage product",
                            new String[] { "validation.error.order.linePercentage.product" });
                }
            }

            if(OrderBL.countPlan(order.getOrderLines()) > 1) {
                throw new SessionInternalError("Order can not create with more than one plan",
                        new String[] { "validation.order.should.not.contain.multiple.plans" });
            }

            for (OrderLineWS line : order.getOrderLines()) {
                if(!orderLineMustHaveItem(line)) {
                    continue;
                }
                itemBl.set(line.getItemId());
                if (line.getId() == 0 && !orderBl.isPeriodValid(itemBl.getEntity(),
                        order.getActiveSince(), order.getActiveUntil())) {
                    throw new SessionInternalError(
                            "Validity period of order should be within validity period of plan/product",
                            new String[] { "validation.order.line.not.added.valdidity.period" });
                }

                if (!orderBl.isCompatible(order.getUserId(),
                        itemBl.getEntity(), order.getActiveSince(),
                        order.getActiveUntil(), usedCategories, line)) {
                    throw new SessionInternalError(
                            "User can subscribe only to one plan/product from given category",
                            new String[] { "validation.order.line.not.added.not.compatible" });
                }
            }
        }
    }

    @Override
    public Integer reserveAsset(Integer assetId, Integer userId) {
        final AssetReservationBL assetReservationBL = new AssetReservationBL();
        return assetReservationBL.reserveAsset(assetId, getCallerId(), userId);
    }

    @Override
    public void releaseAsset(Integer assetId, Integer userId) {
        new AssetReservationBL().releaseAsset(assetId, getCallerId(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PluggableTaskTypeWS getPluginTypeWS(Integer id) {
        PluggableTaskTypeDAS das = new PluggableTaskTypeDAS();
        PluggableTaskTypeDTO dto = das.findNow(id);
        if (null == dto) {
            throw new SessionInternalError("Plugin type with id " + id + " not found!", HttpStatus.SC_NOT_FOUND);
        }
        return PluggableTaskBL.getPluggableTaskTypeWS(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public PluggableTaskTypeWS getPluginTypeWSByClassName(String className) {
        PluggableTaskTypeDAS das = new PluggableTaskTypeDAS();
        PluggableTaskTypeDTO dto = das.findByClassName(className);
        if (null == dto) {
            throw new SessionInternalError(
                    "Plugin type with class name " + className + " not found!", HttpStatus.SC_NOT_FOUND);
        }
        return PluggableTaskBL.getPluggableTaskTypeWS(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public PluggableTaskTypeCategoryWS getPluginTypeCategory(Integer id) {
        PluggableTaskTypeCategoryDAS das = new PluggableTaskTypeCategoryDAS();
        PluggableTaskTypeCategoryDTO dto = das.findNow(id);
        if (null == dto ) {
            throw new SessionInternalError(
                    "Plugin type category with id " + id + " not found!", HttpStatus.SC_NOT_FOUND);
        }
        return PluggableTaskBL.getPluggableTaskTypeCategoryWS(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public PluggableTaskTypeCategoryWS getPluginTypeCategoryByInterfaceName(String interfaceName) {
        PluggableTaskTypeCategoryDAS das = new PluggableTaskTypeCategoryDAS();
        PluggableTaskTypeCategoryDTO dto = das.findByInterfaceName(interfaceName);
        if (null == dto) {
            throw new SessionInternalError(
                    "Plugin type category with interface name " + interfaceName + " not found!", HttpStatus.SC_NOT_FOUND);
        }
        return PluggableTaskBL.getPluggableTaskTypeCategoryWS(dto);
    }

    /**
     * Iterates over subscription lines of each order and creates a subaccount
     * having a subscription order for each subscription lines, Then the
     * subscription line is removed from the original order and change related
     * to that line is also removed from changes list
     *
     * @param parentAccountId
     *            id of the user account for which order is being created
     * @param order
     *            original order containing subscription products
     * @param createInvoice
     *            a flag to indicate if invoices for subscription orders should
     *            be geneated
     * @param orderChanges
     *            a modfiable list of changes for order
     *
     * @return Integer list a list of created subscription order ids
     */
    @Override
    public Integer[] createSubscriptionAccountAndOrder(Integer parentAccountId,
            OrderWS order, boolean createInvoice,
            List<OrderChangeWS> orderChanges) {
        if (parentAccountId == null || order == null) {
            logger.error("To create subscription orders, user or order can not be null");
        }

        List<OrderLineWS> nslines = new ArrayList<>();
        List<OrderLineWS> slines = new ArrayList<>();

        List<OrderChangeWS> schanges = new ArrayList<>();

        if (orderChanges != null) {
            schanges = new ArrayList<>();
        }

        List<Integer> subscriptionItems = new ArrayList<>();
        // separate subscription lines/changes from non subscription
        // lines/changes
        if (order.getOrderLines() != null) {
            for (OrderLineWS line : order.getOrderLines()) {

                int typeId;
                // if type id is not given in the line then evaluate if from
                // product
                if (line.getTypeId() != null) {
                    typeId = line.getTypeId();
                } else {
                    ItemBL itemBl = new ItemBL(line.getItemId());
                    typeId = itemBl.getEntity().getItemTypes().iterator()
                            .next().getOrderLineTypeId();
                }

                if (typeId == Constants.ORDER_LINE_TYPE_SUBSCRIPTION) {
                    slines.add(line);

                    if (orderChanges != null) {
                        OrderChangeWS change = null;
                        for (OrderChangeWS c : orderChanges) {
                            if (c.getItemId().intValue() == line.getItemId()
                                    .intValue()) {
                                change = c;
                            }
                        }

                        if (change != null) {
                            orderChanges.remove(change);
                            schanges.add(change);
                        }
                    }

                    if (subscriptionItems.contains(line.getItemId())
                            || orderDAS.isSubscribed(order.getUserId(),
                                    line.getItemId(), order.getActiveSince(),
                                    order.getActiveUntil())) {

                        SessionInternalError sie = new SessionInternalError(
                                "Already subscribed, Can not subscribe to a subscription item twice.",
                                new String[] { "subscription.item.already.subscribed,"
                                        + line.getItemId() });
                        throw sie;

                    } else {
                        subscriptionItems.add(line.getItemId());
                    }

                } else {
                    nslines.add(line);
                }
            }
        }

        // if no subscription lines found
        if (slines.size() < 1) {
            return null;
        }

        // set parent account to allow subaccounts as we are going to create
        // internal subaccounts
        UserWS parentUser = getUserWS(parentAccountId);

        if (!parentUser.getIsParent()) {
            logger.debug("Allowing user {} to have subaccounts.",
                    parentUser.getId());
            parentUser.setIsParent(true);
            updateUser(parentUser);
        }

        int childs = parentUser.getChildIds().length;

        List<Integer> sorders = new ArrayList<Integer>(slines.size());
        Integer userId = order.getUserId();

        for (OrderLineWS sl : slines) {
            childs++;

            // create a sub account
            UserWS child = cloneChildUser(parentUser);
            child.setUserName(parentUser.getUserName() + "-"
                    + String.format("%03d", childs));
            child.setId(createUser(child));

            // create subscription order for subaccount
            order.setUserId(child.getUserId());

            OrderLineWS[] orderLine = new OrderLineWS[1];
            orderLine[0] = sl;
            order.setOrderLines(orderLine);

            OrderChangeWS[] orderChange = new OrderChangeWS[1];
            for (OrderChangeWS change : schanges) {
                if (change.getItemId().intValue() == sl.getItemId()) {
                    orderChange[0] = change;
                }
            }

            OrderWS created = doCreateOrder(order, orderChange, true);
            sorders.add(created.getId());

            if (createInvoice) {
                doCreateInvoice(created.getId());
            }
        }

        // set only non subscription lines in original order
        order.setUserId(userId);
        logger.debug("Non subscription lines are: {}", nslines.size());
        order.setOrderLines(nslines.toArray(new OrderLineWS[nslines.size()]));

        return sorders.toArray(new Integer[sorders.size()]);
    }

    private UserWS cloneChildUser(UserWS user) {
        UserWS clone = new UserWS();

        // clone as less information as required to create a user
        clone.setPassword(Util
                .getSysProp(Constants.SUBSCRIPTION_ACCOUNT_PASSWORD));
        clone.setCurrencyId(user.getCurrencyId());
        clone.setDeleted(user.getDeleted());
        clone.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        clone.setLanguageId(user.getLanguageId());
        clone.setParentId(user.getId());
        clone.setStatus(user.getStatus());
        clone.setStatusId(user.getStatusId());
        clone.setIsParent(false);
        clone.setUseParentPricing(false);
        clone.setExcludeAgeing(user.getExcludeAgeing());
        clone.setCompanyName(user.getCompanyName());
        clone.setEntityId(user.getEntityId());
        clone.setMainRoleId(Constants.TYPE_CUSTOMER);

        clone.setAccountTypeId(user.getAccountTypeId());
        // clone meta fields
        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>();
        for (MetaFieldValueWS ws : user.getMetaFields()) {
            metaFields.add(ws.clone());
        }
        clone.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields
                                                                    .size()]));

        clone.setTimelineDatesMap(user.getTimelineDatesMap());

        return clone;
    }

    @Override
    public Integer createOrEditLanguage(LanguageWS languageWS) {
        return new LanguageDAS().save(DescriptionBL
                .getLanguageDTO(languageWS))
                .getId();
    }

    @Override
    public AssetWS[] findAssetsByProductCode(String productCode) {
        Integer companyId = getCallerCompanyId();
        return new AssetBL().findAssetsByProductCode(productCode, companyId);
    }

    @Override
    public AssetStatusDTOEx[] findAssetStatuses(String identifier) {
        return new AssetBL().findAssetStatuses(identifier);
    }

    @Override
    public AssetWS findAssetByProductCodeAndIdentifier(String productCode,
            String identifier) {
        Integer companyId = getCallerCompanyId();
        return new AssetBL().findAssetByProductCodeAndIdentifier(productCode,
                identifier, companyId);
    }

    @Override
    public AssetWS[] findAssetsByProductCodeAndStatus(String productCode,
            Integer assetStatusId) {
        Integer companyId = getCallerCompanyId();
        return new AssetBL().findAssetsByProductCode(productCode,
                assetStatusId, companyId);
    }

    @Override
    public AssetWS[] findAssetsForOrderChanges(Integer[] ids)  {
        return new AssetBL().findAssetsForOrderChanges(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationErrorRecord[] getMediationErrorRecordsByMediationProcess(UUID mediationProcessId, Integer mediationRecordStatusId) {
        return mediationService.getMediationErrorRecordsForProcess(mediationProcessId).toArray(new JbillingMediationErrorRecord[0]);
    }
    @Override
    public UserWS copyCompany(String childCompanyTemplateName, Integer entityId, List<String> importEntities,
                              boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail) {
        return copyCompanyInSaas(childCompanyTemplateName, entityId, importEntities, isCompanyChild, copyProducts, copyPlans, adminEmail, null);
    }

    @Override
    public UserWS copyCompanyInSaas(String childCompanyTemplateName, Integer entityId, List<String> importEntities,
                              boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail, String systemAdminLoginName) {
        if(isCompanyChild && (childCompanyTemplateName == null || childCompanyTemplateName.trim().isEmpty())) {
            throw new SessionInternalError("The template company shouldn't be blank.", new String[]{"copy.company.child.template.not.blank"});
        }

        if(UNIQUE_LOGIN_NAME && StringUtils.isEmpty(systemAdminLoginName)){
            throw new SessionInternalError("The System Admin Name shouldn't be blank", new String[]{"copy.company.system.admin.name.not.blank"});
        }

        if(StringUtils.isEmpty(adminEmail.trim())){
            throw new SessionInternalError("The Admin Email shouldn't be blank", new String[]{"copy.company.admin.email.not.blank"});
        }

        Matcher matcher =  Pattern.compile(CommonConstants.EMAIL_VALIDATION_REGEX).matcher(adminEmail);
        if(!matcher.matches()) {
            throw new SessionInternalError("Admin email is not valid, please enter valid email address.", new String[]{"copy.company.admin.email.not.valid"});
        }
        return (UNIQUE_LOGIN_NAME && StringUtils.isNotBlank(systemAdminLoginName)) ?
                new CopyCompanyBL().copyCompany(childCompanyTemplateName, entityId, importEntities, isCompanyChild, copyProducts, copyPlans, adminEmail, systemAdminLoginName) :
                new CopyCompanyBL().copyCompany(childCompanyTemplateName, entityId, importEntities, isCompanyChild, copyProducts, copyPlans, adminEmail);
    }

    /**
     * Retrieves a orderPeriod with its period unit and other details.
     *
     * @param orderPeriodId
     *            The id of the orderPeriod to be returned
     */
    @Override
    @Transactional(readOnly = true)
    public OrderPeriodWS getOrderPeriodWS(Integer orderPeriodId) {
        OrderPeriodDTO orderPeriod = new OrderPeriodDAS().findNow(orderPeriodId);
        if (null == orderPeriod){
            throw new SessionInternalError(String.format("Order period with id %d not found!", orderPeriodId), HttpStatus.SC_NOT_FOUND);
        }
        return OrderBL.getOrderPeriodWS(orderPeriod);
    }

    @Override
    public Integer createOrderPeriod(OrderPeriodWS orderPeriod)
    {
        if (orderPeriod.getDescriptions() != null
                && orderPeriod.getDescriptions().size() > 0) {
            int descriptionLength = orderPeriod.getDescriptions().get(0)
                    .getContent().length();
            if (descriptionLength < 1 || descriptionLength > 4000) {
                throw new SessionInternalError(
                        "Description should be between 1 and 4000 characters long", HttpStatus.SC_BAD_REQUEST);
            }
        }

        OrderPeriodDAS periodDas = new OrderPeriodDAS();
        OrderPeriodDTO periodDto = new OrderPeriodDTO();
        periodDto.setCompany(new CompanyDAS().find(getCallerCompanyId()));
        periodDto.setValue(orderPeriod.getValue());
        if (null != orderPeriod.getPeriodUnitId()) {
            periodDto.setUnitId(orderPeriod.getPeriodUnitId().intValue());
        }
        periodDto = periodDas.save(periodDto);

        if (orderPeriod.getDescriptions() != null
                && orderPeriod.getDescriptions().size() > 0) {
            periodDto.setDescription(orderPeriod
                    .getDescriptions().get(0).getContent(),
                    orderPeriod.getDescriptions()
                    .get(0).getLanguageId());
        }
        logger.debug("Converted to DTO: {}", periodDto);
        periodDas.flush();
        periodDas.clear();
        return periodDto.getId();
    }

    /**
     * Validate if pro-rate box is checked and order period and billing cycle period should be the same.
     * If not same show validation message.
     */
    private void validateProrating(OrderWS order)  {
        if (order == null) {
            throw new SessionInternalError("Null parameter");
        }

        OrderDTO orderDto = new OrderBL().getDTO(order);

        MainSubscriptionDTO mainSubscription = orderDto.getUser().getCustomer().getMainSubscription();
        Integer billingCycleUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer billingCycleValue = mainSubscription.getSubscriptionPeriod().getValue();

        BillingProcessConfigurationDTO billingConfiguration =
                new ConfigurationBL(orderDto.getUser().getEntity().getId()).getDTO();
        ProratingType companyLevelProratingType = billingConfiguration.getProratingType();

        boolean planProrateFlag = true;

        //validation for plan throught API.
        if (orderDto.getProrateFlagValue()) {

            for (OrderLineDTO lineDto : orderDto.getLines()) {
                if (null != lineDto.getItem() && lineDto.getItem().getPlans().size() > 0 && lineDto.getDeleted() == 0) {
                    PlanDTO planDto= lineDto.getItem().getPlans().iterator().next();
                    Integer planPeriodUnit = planDto.getPeriod().getPeriodUnit().getId();
                    Integer planPeriodValue = planDto.getPeriod().getValue();
                    if (companyLevelProratingType.isProratingAutoOn()) {
                        if (!planPeriodUnit.equals(billingCycleUnit) && planPeriodValue.equals(billingCycleValue)) {
                            planProrateFlag = false;
                        }
                    }
                }
            }
        }

        if (orderDto.getProrateFlagValue() && planProrateFlag) {

            if (null != orderDto && null != orderDto.getOrderPeriod().getUnitId()) {
                if (!(orderDto.getOrderPeriod().getUnitId().equals(billingCycleUnit) && orderDto.getOrderPeriod().getValue().equals(billingCycleValue))) {
                    throw new SessionInternalError("Order Period unit should equal to Customer billing period unit", new String[] {"OrderWS,billingCycleUnit,order.period.unit.should.equal"});
                }
            }
        }
    }

    /**
     * Validate order period description should not be duplicate.
     *
     * @param orderPeriod
     * @param orderPeriodDto
     */
    private void validateOrderPeriod(OrderPeriodWS orderPeriod,
            OrderPeriodDTO orderPeriodDto) {

        Integer entityId = getCallerCompanyId();
        Integer languageId = getCallerLanguageId();

        OrderPeriodDAS periodDas = new OrderPeriodDAS();
        List<OrderPeriodDTO> orderPeriods = periodDas.getOrderPeriods(entityId);

        for (OrderPeriodDTO orderPeriodObj : orderPeriods) {
            if (null == orderPeriodDto
                    && (orderPeriod.getDescription(languageId).getContent()
                            .trim().equals(orderPeriodObj.getDescription()))) {
                throw new SessionInternalError(
                        "Duplicate Description ",
                        new String[] { "OrderPeriodWS,content,order.period.description.already.exists" });
            } else if (null != orderPeriodDto
                    && !orderPeriod.getDescription(languageId).getContent()
                    .trim().equals(orderPeriodDto.getDescription())) {
                if (orderPeriod.getDescription(languageId).getContent()
                        .equals(orderPeriodObj.getDescription())) {
                    throw new SessionInternalError(
                            "Duplicate Description ",
                            new String[] { "OrderPeriodWS,content,order.period.description.already.exists" });
                }
            }
        }
    }

    /**
     * Validate Order Active Since Date if An invoice is already generated for
     * given order, then not allowed to change the Active since date.
     *
     * @param order
     * @
     */
    private void validateActiveSinceDate(OrderWS order)
    {
        if (order == null) {
            throw new SessionInternalError("Null parameter");
        }

        OrderDTO orderDto = orderDAS.find(order.getId());

        // Get Minimum Period start date of order for non-review records.
        Date firstInvoicePeriodStartDate = new OrderProcessDAS()
        .getFirstInvoicePeriodStartDateByOrderId(order.getId());
        if (null != firstInvoicePeriodStartDate
                && !firstInvoicePeriodStartDate.equals(order.getActiveSince())) {
            if (order.getActiveSince().compareTo(orderDto.getActiveSince()) != 0) {
                throw new SessionInternalError(
                        "Not allowed to changes Active since date",
                        new String[]{"OrderWS,activeSince,order.acitve.since.date.not.allowed.to.changes"});
            }
        }
        //TODO here we checked  old orderchange start date with new active since date which is not ne
        /*OrderChangeWS[] oldChanges = getOrderChanges(order.getId());
        for (OrderChangeWS oldChange : oldChanges) {
            if (com.sapienter.jbilling.common.Util.truncateDate(
                    oldChange.getStartDate()).before(
                    com.sapienter.jbilling.common.Util.truncateDate(order
                            .getActiveSince()))) {
                throw new SessionInternalError(
                        "Not allowed to changes Active since date",
                        new String[]{"OrderWS,activeSince,validation.error.incorrect.start.date"});
            }
        }*/
    }

    @Override
    public OrderStatusWS findOrderStatusById(Integer orderStatusId) {
        OrderStatusWS orderStatusWS = new OrderStatusDAS().findOrderStatusById(orderStatusId);
        return orderStatusWS != null? orderStatusWS : null;
    }

    @Override
    public Integer[] findAllOrderStatusIds() {
        List<OrderStatusDTO> orderStatusDTOs = new OrderStatusDAS().findAll(getCallerCompanyId());
        return orderStatusDTOs.stream().map(OrderStatusDTO::getId).toArray(Integer[]::new);
    }

    private void validateLines(OrderWS order, OrderChangeWS[] orderChanges) {
        // #7761 - moved out of validateOrder function due to problem in removing order line
        List<Integer> usedCategories = new ArrayList<Integer>();
        OrderBL orderBl = new OrderBL();
        ItemBL itemBl = new ItemBL();

        if (order.getOrderLines() != null) {

            if(order.getOrderLines().length==1) {
                if(new ItemDAS().find(order.getOrderLines()[0].getItemId()).getPercentage() != null ) {
                    throw new SessionInternalError("Order can not create for line percentage product", new String[]{"validation.error.order.linePercentage.product"});
                }
            }

            for (OrderLineWS line : order.getOrderLines()) {

                itemBl.set(line.getItemId());
                if(!orderBl.isPeriodValid(itemBl.getEntity(), order.getActiveSince(), order.getActiveUntil())) {
                    throw new SessionInternalError("Validity period of order should be within validity period of plan/product", new String[]{"validation.order.line.not.added.valdidity.period"});
                }

                //If one per order/customer category and swap plan happen then isCompatible validation should not occurs.
                Integer swapPlanItemId = null;
                Integer existingPlanItemId = null;

                for (OrderChangeWS orderChange : orderChanges) {

                    ItemBL itemBlObj = new ItemBL(orderChange.getItemId());
                    ItemDTO item = itemBlObj.getEntity();
                    if (null == swapPlanItemId && null != item && item.isPlan() && orderChange.getQuantityAsDecimal().compareTo(BigDecimal.ZERO) > 0) {
                        swapPlanItemId = item.getId();
                    } else if (null == existingPlanItemId && null != item && item.isPlan() && orderChange.getQuantityAsDecimal().compareTo(BigDecimal.ZERO) < 0) {
                        existingPlanItemId = item.getId();
                    }

                    if (null != swapPlanItemId && null != existingPlanItemId) {
                        line.setIsSwapPlanCondition(true);
                        break;
                    }
                }

                if(!orderBl.isCompatible(order.getUserId(), itemBl.getEntity(), order.getActiveSince(), order.getActiveUntil(), usedCategories, line)) {
                    throw new SessionInternalError("User can subscribe only to one plan/product from given category", new String[]{"validation.order.line.not.added.not.compatible"});
                }
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Long getMediationErrorRecordsCount(Integer mediationConfigurationId) {
        return Long.valueOf(mediationService.getMediationErrorRecordCountForMediationConfigId(mediationConfigurationId));
    }

    /**
     * Queries the data source for a Enumeration entity filtered by
     * <code>enumerationId</code>.
     *
     * @param enumerationId
     *            representing the desired enumeration entity.
     * @return {@link com.sapienter.jbilling.server.util.EnumerationWS} object
     *         representing the result set or null if record does not exist.
     * @throws com.sapienter.jbilling.common.SessionInternalError
     *             if invalid input parameters!
     */
    @Override
    @Transactional(readOnly = true)
    public EnumerationWS getEnumeration(Integer enumerationId)
    {
        if (null == enumerationId || Integer.valueOf(0) >= enumerationId) {
            String[] errors = new String[] { "EnumerationWS,id,enumeration.id.null.or.negative" };
            throw new SessionInternalError("enumeration.Id.null.or.negative",
                    errors);
        }
        EnumerationBL enumerationBL = new EnumerationBL();
        EnumerationDTO enumerationDTO = enumerationBL.getEnumeration(
                enumerationId, getCallerCompanyId());
        if (null == enumerationDTO) {
            return null;
        }
        return EnumerationBL.convertToWS(enumerationDTO);
    }

    /**
     * Queries the data source for a Enumeration entity with exact name match.
     *
     * @param name
     *            of the entity.
     * @return {@link com.sapienter.jbilling.server.util.EnumerationWS} object
     *         representing the result set or null if record does not exist.
     * @throws com.sapienter.jbilling.common.SessionInternalError
     *             if invalid input parameters!
     */
    @Override
    @Transactional(readOnly = true)
    public EnumerationWS getEnumerationByName(String name)
    {
        return getEnumerationByNameAndCompanyId(name, getCallerCompanyId());
    }

    /**
     * Queries the data source for a Enumeration entity with exact name match and company id.
     * As mulitple companies can have same name enumeration.
     *
     * @param name
     *            of the entity.
     * @return {@link com.sapienter.jbilling.server.util.EnumerationWS} object
     *         representing the result set or null if record does not exist.
     * @throws com.sapienter.jbilling.common.SessionInternalError
     *             if invalid input parameters!
     */
    @Override
    @Transactional(readOnly = true)
    public EnumerationWS getEnumerationByNameAndCompanyId(String name, Integer companyId)
    {
        if (null == name || Integer.valueOf(0).equals(name.length())) {
            String[] errors = new String[] { "EnumerationWS,name,enumeration.name.empty" };
            throw new SessionInternalError("enumeration.name.empty", errors);
        }
        EnumerationBL enumerationBL = new EnumerationBL();
        EnumerationDTO enumerationDTO = enumerationBL.getEnumerationByName(
                name, companyId);
        if (null == enumerationDTO) {
            return null;
        }
        return EnumerationBL.convertToWS(enumerationDTO);
    }

    /**
     *
     * Queries the data source for all
     * {@link com.sapienter.jbilling.server.util.EnumerationWS} entities
     * filtered by <code>entityId</code>. Optionally the result set can be
     * constrained with a <code>max</code> number of entities or all will be
     * fetched. Also starting from <code>offset</code> position is optional.
     *
     * @param max
     *            representing maximum number of rows (optional).
     * @param offset
     *            representing the offset (optional).
     *
     * @return list of {@link com.sapienter.jbilling.server.util.EnumerationWS}
     *         entities, representing the result set.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EnumerationWS> getAllEnumerations(Integer max, Integer offset) {

        if (null != max && max < 1) {
            throw new SessionInternalError("Invalid value: " + max + " for parameter 'max'. Must be greater than zero.",
                    HttpStatus.SC_BAD_REQUEST);
        }
        if (null != offset && offset < 0) {
            throw new SessionInternalError("Invalid value: " + offset + " for parameter 'offset'. Must be a non-negative number.",
                    HttpStatus.SC_BAD_REQUEST);
        }

        EnumerationBL enumerationBL = new EnumerationBL();
        List<EnumerationDTO> allDTOs = enumerationBL.getAllEnumerations(getCallerCompanyId(), max, offset);
        if (null == allDTOs) {
            return null;
        }
        List<EnumerationWS> enumerationsList = new ArrayList<EnumerationWS>();
        for (EnumerationDTO enumerationDTO : allDTOs) {
            if (null != enumerationDTO) {
                enumerationsList.add(EnumerationBL.convertToWS(enumerationDTO));
            }
        }
        return enumerationsList;
    }

    /**
     * Queries the data source for a number representing the count of all
     * persisted {@link com.sapienter.jbilling.server.util.EnumerationWS}
     * entities.
     *
     * @return number of persisted entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Long getAllEnumerationsCount() {
        return new EnumerationBL()
        .getAllEnumerationsCount(getCallerCompanyId());
    }

    /**
     * New Enumeration entity is created or persisted to the data source. Also
     * used for updating an existing <code>Enumeration</code> entity. However if
     * we try to persist entity with the name already persisted, exception is
     * thrown. Also validation is involved.
     *
     * @param enumeration
     *            {@link com.sapienter.jbilling.server.util.EnumerationWS}
     *            entity that is going to be saved.
     * @return Id of the created or updated Enumeration entity.
     * @
     *             if entity with the same name is already persisted or invalid
     *             input parameters.
     */
    @Override
    public Integer createUpdateEnumeration(EnumerationWS enumeration)
    {
        validateEnumeration(enumeration);
        EnumerationBL enumerationBL = new EnumerationBL();
        if (null == enumeration.getEntityId()
                || Integer.valueOf(0).equals(enumeration.getEntityId())) {
            enumeration.setEntityId(getCallerCompanyId());
        }
        EnumerationDTO dto = EnumerationBL.convertToDTO(enumeration);
        // Take care of enumerations with duplicate names
        if (enumerationBL.exists(dto.getId(), dto.getName(), dto.getEntityId())) {
            String[] errors = new String[] { "EnumerationWS,name,enumeration.name.exists,"
                    + enumeration.getName() };
            throw new SessionInternalError("enumeration.name.exists", errors);
        }

        // Save or update
        if (dto.getId() > 0) {
            enumerationBL.set(dto.getId());
            return enumerationBL.update(dto);
        } else {
            return enumerationBL.create(dto);
        }
    }

    /**
     * Deletes the {@link com.sapienter.jbilling.server.util.db.EnumerationDTO}
     * entity from the data source, identified by <code>enumerationId</code>.
     *
     * @param enumerationId
     *            representing the enumeration entity that is going to be
     *            deleted.
     * @return true or false depending if the deletion was successful or not.
     * @
     *             if enumeration exists in some meta field or invalid input
     *             parameter.
     */

    @Override
    public boolean deleteEnumeration(Integer enumerationId)  {
        if (null == enumerationId || Integer.valueOf(0) >= enumerationId) {
            String[] errors = new String[]{"EnumerationWS,id,enumeration.id.null.or.negative"};
            throw new SessionInternalError("enumeration.id.null.or.negative", errors);
        }
        EnumerationBL enumerationBL = new EnumerationBL(enumerationId);
        if (new MetaFieldDAS().getFieldCountByDataTypeAndName(DataType.ENUMERATION, enumerationBL.getEntity().getName(),getCallerCompanyId()) > 0 ||
                new MetaFieldDAS().getFieldCountByDataTypeAndName(DataType.LIST, enumerationBL.getEntity().getName(),getCallerCompanyId()) > 0) {
            String[] errors = new String[]{"EnumerationWS,id,enumeration.delete.failed," + enumerationId};
            throw new SessionInternalError("enumeration.delete.failed", errors);
        }
        enumerationBL.delete();
        return true;
    }

    /**
     * Validates the {@link com.sapienter.jbilling.server.util.EnumerationWS}
     * object and its values.
     *
     * @param enumeration
     *            validated object.
     * @throws com.sapienter.jbilling.common.SessionInternalError
     *             if some validation fails.
     */
    private void validateEnumeration(EnumerationWS enumeration)
    {
        if (null == enumeration) {
            String[] errors = new String[] { "EnumerationWS,EnumerationWS,enumeration.null" };
            throw new SessionInternalError("enumeration.null", errors);
        }

        // validate name
        String name = enumeration.getName();
        if (null == name || Integer.valueOf(0).equals(name.length())) {
            String[] errors = new String[] { "EnumerationWS,name,enumeration.name.empty" };
            throw new SessionInternalError("enumeration.name.empty", errors);
        }

        // validate at least one enum-value
        List<EnumerationValueWS> values = enumeration.getValues();
        if (null == values || Integer.valueOf(0).equals(values.size())) {
            String[] errors = new String[] { "EnumerationWS,values,enumeration.values.missing" };
            throw new SessionInternalError("enumeration.value.missing", errors);
        }

        // validate enumeration values
        Set<String> valuesSet = new HashSet<String>();
        for (EnumerationValueWS value : enumeration.getValues()) {
            logger.debug("value = " + value);

            // empty value
            String val = value.getValue();
            if (null == val || Integer.valueOf(0).equals(val.length())) {
                String[] errors = new String[] { "EnumerationWS,values.value,enumeration.value.missing" };
                throw new SessionInternalError("enumeration.value.missing",
                        errors);
            }

            // max length
            if (val.length() > Constants.ENUMERATION_VALUE_MAX_LENGTH) {
                String[] errors = new String[] { "EnumerationWS,values.value,enumeration.value.max.length" };
                throw new SessionInternalError("enumeration.value.max.length",
                        errors);
            }

            // duplicate
            if (valuesSet.contains(val)) {
                String[] errors = new String[] { "EnumerationWS,values.value,enumeration.value.duplicated" };
                throw new SessionInternalError("enumeration.value.duplicated",
                        errors);
            }

            valuesSet.add(value.getValue());
        }

    }

    @Transactional(readOnly = true)
    @Override
    public List<AssetWS> getAssetsByUserId(Integer userId) {
        List<AssetWS> assets = new ArrayList<>();
        List<AssetDTO> assetDtos = assetDAS.findAssetsByUser(userId);
        for (AssetDTO assetDto : assetDtos) {
            assets.add(AssetBL.getWS(assetDto));
        }
        logger.debug("Assets :" + assets.size());
        return assets;
    }

    @Transactional(readOnly = true)
    @Override
    public void resetPassword(int userId) {
        UserDTO user = userDAS.findNow(userId);
        if( null == user) {
            throw new SessionInternalError("user not found!",
                    new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
        }
        EmailResetPasswordService emailResetPasswordService = Context
                .getBean(Context.Name.PASSWORD_SERVICE);

        emailResetPasswordService.resetPassword(user);

    }

    @Transactional(readOnly = true)
    @Override
    public void resetPasswordByUserName(String userName) {
        UserDTO user = userDAS.findByUserName(userName);
        if( null == user) {
            throw new SessionInternalError("user not found!", new String [] { "Please enter a valid user name." },
                    HttpStatus.SC_NOT_FOUND);
        }
        EmailResetPasswordService emailResetPasswordService = Context
                .getBean(Context.Name.PASSWORD_SERVICE);

        emailResetPasswordService.resetPassword(user);
    }

    @Override
    public UserWS getUserByCustomerMetaField(String metaFieldValue, String metaFieldName) {
        return getUserByCustomerMetaFieldAndCompanyId(metaFieldValue, metaFieldName, getCallerCompanyId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public UserWS getUserByCustomerMetaFieldAndCompanyId(String metaFieldValue, String metaFieldName, Integer companyId) {

        logger.debug("In getUserByCustomerMetaField...");
        if(StringUtils.isEmpty(metaFieldValue) || StringUtils.isEmpty(metaFieldName)) {
            throw new SessionInternalError(
                    "Meta Field Value or Name should not be null or empty",
                    new String[] { "UserWS,metaFieldValue,user.validation.metafield.value.or.name.null.or.empty" });
        }

        UserBL userBl = new UserBL();
        List<CustomerDTO> cutomerList = userBl.getUserByCustomerMetaField(metaFieldValue, metaFieldName, companyId);
        if (CollectionUtils.isEmpty(cutomerList)) {
            throw new SessionInternalError("Customer does not exist with the supplied Meta Field Value: "+metaFieldValue+" in the entity: "+companyId);
        } else if (cutomerList.size() > 1) {
            throw new SessionInternalError("More than one matching customer for supplied Meta Field Value",
                    new String[] { "customer.meta.field.value.more.then.one.match" });
        }
        return UserBL.getWS(DTOFactory.getUserDTOEx(cutomerList.get(0).getBaseUser()));
    }

    public List<UserWS> getUsersByParentId(Integer parentId) {

        List<UserWS> childUsers = new ArrayList<UserWS>();
        logger.debug("getUsersByParentId...");
        if (parentId == null) {
            throw new SessionInternalError("ParentId should not be null");
        }

        UserBL userBl = new UserBL();
        List<UserDTO> customerList = userBl.getUserByParentId(parentId);
        if (null == customerList) {
            throw new SessionInternalError("Customer does not exist with parentId: " + parentId, HttpStatus.SC_NOT_FOUND);
        }

        for (UserDTO childCustomer : customerList) {
            childUsers.add(UserBL.getWS(DTOFactory.getUserDTOEx(childCustomer)));
        }
        return childUsers;
    }

    /**
     * This API will not just create the user in jBilling but will also validate
     * if CIM profile has been created successfully on the gateway.
     * If there is any error in CIM profile creation on gateway, the jB user will be created,
     * and an informational message set in UserWS.cimProfileErrorMessage
     */
    @Override
    public UserWS createUserWithCIMProfileValidation(UserWS newUser)  {

        // first call createUser
        Integer userId = createUser(newUser);

        // get the user ws object of just created user
        UserWS userWS = getUserWS(userId);

        UserBL.setCimProfileError(userWS);

        return userWS;

    }

    /**
     * This API will not just update the user in jBilling but will also validate
     * if CIM profile has been created successfully on the gateway if credit card info has changed.
     * If there is any error in CIM profile creation on gateway, the jB user will be updated,
     * and an informational message set in UserWS.cimProfileErrorMessage
     */
    @Override
    public UserWS updateUserWithCIMProfileValidation(UserWS newUser)  {

        updateUser(newUser);
        // get the user ws object of just updated user
        UserWS userWS = getUserWS(newUser.getId());
        UserBL.setCimProfileError(userWS);

        return userWS;

    }

    @Override
    public CustomerEnrollmentWS getCustomerEnrollment(Integer customerEnrollmentId) {
        return new CustomerEnrollmentBL().getCustomerEnrollmentWS(customerEnrollmentId);
    }

    @Override
    public EDITypeWS getEDIType(Integer ediTypeId) {

        EDITypeDTO ediTypeDTO = new EDITypeDAS().find(ediTypeId);
        EDITypeWS ediTypeWS = new EDITypeBL().getWS(ediTypeDTO);
        return ediTypeWS;
    }

    @Override
    public Integer createUpdateEnrollment(CustomerEnrollmentWS customerEnrollmentWS)  {
        CustomerEnrollmentBL customerEnrollmentBL=new CustomerEnrollmentBL();
        CustomerEnrollmentDTO customerEnrollmentDTO=customerEnrollmentBL.getDTO(customerEnrollmentWS);
        return new CustomerEnrollmentBL().save(customerEnrollmentDTO);
    }

    @Override
    public CustomerEnrollmentWS validateCustomerEnrollment(CustomerEnrollmentWS customerEnrollmentWS)  {
        CustomerEnrollmentBL customerEnrollmentBL=new CustomerEnrollmentBL();
        logger.debug("customerEnrollmentWS is:  " + customerEnrollmentWS);
        CustomerEnrollmentDTO customerEnrollmentDTO=customerEnrollmentBL.getDTO(customerEnrollmentWS);
        new CustomerEnrollmentBL().validateEnrollment(customerEnrollmentDTO);
        return customerEnrollmentBL.getWS(customerEnrollmentDTO);

    }

    @Override
    public void deleteEnrollment(Integer customerEnrollmentId) {
        new CustomerEnrollmentBL().delete(customerEnrollmentId);
    }

    @Override
    public Integer createEDIType(EDITypeWS ediTypeWS, File ediFormatFile){
        return new EDITypeBL().createEDIType(ediTypeWS, ediFormatFile);
    }

    @Override
    public void deleteEDIType(Integer ediTypeId) {
        new EDITypeBL().deleteEDIType(ediTypeId, getCallerCompanyId());
    }

    @Override
    public  int generateEDIFile(Integer ediTypeId, Integer entityId, String fileName, Collection input)  {
        FileFormat fileFormat = FileFormat.getFileFormat(ediTypeId);
        IFileGenerator generator = new FlatFileGenerator(fileFormat, entityId, fileName, input);
        return generator.validateAndSaveInput().getId();
    }

    @Override
    public int parseEDIFile(Integer ediTypeId, Integer entityId, File parserFile)  {
        FileFormat fileFormat = FileFormat.getFileFormat(ediTypeId);
        FlatFileParser fileParser = new FlatFileParser(fileFormat, parserFile, entityId);
        return fileParser.parseAndSaveFile().getId();
    }

    @Override
    public int saveEDIFileRecord(EDIFileWS ediFileWS)  {
        return new EDIFileBL().saveEDIFile(ediFileWS);
    }

    @Override
    public void updateEDIFileStatus(Integer fileId, String statusName, String comment) {
        new EDIFileBL().updateEDIFileStatus(fileId, statusName, comment);
    }

    @Override
    public List<CompanyWS> getAllChildEntities(Integer parentId)  {
        return new EntityBL().getChildEntities(parentId);
    }

    /*
     * Payment Transfer API
     */
    @Override
    public void transferPayment(PaymentTransferWS paymentTransfer) {
        //Validate transfer payment API parameters.
        validateTrasferPayment(paymentTransfer);

        PaymentBL paymentBL = new PaymentBL(paymentTransfer.getPaymentId());
        PaymentTransferBL paymentTransferBL = new PaymentTransferBL();
        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(getCallerLanguageId());
        paymentDTOEx.setBaseUser(paymentBL.getDTO().getBaseUser());
        Integer paymentTransferId = paymentTransferBL.createPaymentTransfer(paymentDTOEx, paymentTransfer);

        if (null != paymentTransferId) {
            PaymentDTO payment = paymentBL.transferPaymentToUser(paymentTransfer);
            // let know about this payment with an event
            paymentBL = new PaymentBL(payment.getId());
            if (paymentBL.getEntity() == null) {
                return;
            }

            paymentDTOEx = paymentBL.getDTOEx(getCallerLanguageId());
            PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(
                    getCallerCompanyId(),paymentDTOEx);
            EventManager.process(event);
        }
    }

    @Transactional(readOnly=true)
    public PaymentTransferWS getLatestPaymentTransfer(Integer userId) {
        try {
            if (userId == null) {
                return null;
            }
            PaymentTransferWS paymentTransfer = null;
            PaymentTransferBL paymentTransferBL = new PaymentTransferBL();
            Integer latestPaymentTransferId = paymentTransferBL.getLatestPaymentTransfer(userId);

            if (latestPaymentTransferId != null) {
                paymentTransferBL.setPaymentTransferDTO(latestPaymentTransferId);
                paymentTransfer = paymentTransferBL.getWS(paymentTransferBL.getPaymentTransferDTO());
            }

            return paymentTransfer;

        } catch(Exception e) {
            logger.error("Exception in web service: getting latest payment transfer" + " for user " + userId, e);
            throw new SessionInternalError("Error getting latest payment transfer");
        }
    }

    /**
     * Validate payment transfer methods parameters
     * @param paymentTransfer
     */
    private void validateTrasferPayment(PaymentTransferWS paymentTransfer) {

        if (paymentTransfer.getPaymentId() == null) {
            throw new SessionInternalError("Payment Id is required", new String[] {"PaymentTransferDTO,paymentId,validation.error.is.required"});
        } else if (null == paymentTransfer.getFromUserId()) {
            throw new SessionInternalError("From UserId is required", new String[] {"PaymentTransferDTO,fromUserId,validation.error.is.required"});
        } else if (null == paymentTransfer.getToUserId()) {
            throw new SessionInternalError("To UserId is required", new String[] {"PaymentTransferDTO,toUserId,validation.error.is.required"});
        }

        // validate if payment linked to invoices
        PaymentBL bl = new PaymentBL(paymentTransfer.getPaymentId());

        if (bl.getEntity() == null) {
            return;
        }

        if (bl.getEntity().getDeleted() == 1) {
            throw new SessionInternalError("Payment is not found.", new String[] {"PaymentTransferDTO,paymentId,payment.is.not.found"});
        }
        // apply validations for refund payments
        if(bl.getEntity().getIsRefund() == 1) {
            logger.debug("This payment {} is a refund so we cannot transfer it.", paymentTransfer.getPaymentId());
            throw new SessionInternalError("A Refund cannot be transfer",
                    new String[] {"validation.error.transfer.refund.payment"});
        }

        // Validate payment method should be Bank
        if (bl.getEntity().getPaymentMethod().getId() != Constants.PAYMENT_METHOD_BANK_WIRE) {
            throw new SessionInternalError("Bank payment method applicable only for payment transfer.", new String[] {"PaymentTransferDTO,toUserId,validation.bank.payment.method.applicable.only"});
        }

        // Validate payment not linked to any invoices.
        if (null != bl.getEntity().getInvoicesMap() && bl.getEntity().getInvoicesMap().size() > 0) {
            throw new SessionInternalError("Please remove linked invoices to transfer this payment", new String[] {"PaymentTransferDTO,paymentId,payment.cant.transfer.linked"});
        }
    }

    /**
     * get Last Payment Transfer By Payment Id
     */
    public PaymentTransferWS getLastPaymentTransferByPaymentId(Integer paymentId) {
        PaymentTransferBL paymentTransferBL = new PaymentTransferBL();
        return paymentTransferBL.getWS(paymentTransferBL.getLastPaymentTransferByPaymentId(paymentId));
    }

    /*
     * get all payment transfers in between dates
     */
    public List<Integer> getAllPaymentTransfersByDateRange(Integer entityId, Date fromDate, Date toDate) {
        return new PaymentTransferBL().getAllPaymentTransfersByDateRange(entityId, fromDate, toDate);
    }

    /*
     * get all payment transfers by user id
     */
    public List<PaymentTransferWS> getAllPaymentTransfersByUserId(Integer userId) {
        return new PaymentTransferBL().getAllPaymentTransfersByUserId(userId);
    }

    /**
     *  This method update the EDI File status and also trigger an Event called UpdateEDIStatus.
     *  Whenever UpdateEDIStatus Event trigger, It re-process an EDI file.
     *  Also if escapeValidation value is true then it will escape the non mandatory validation on processing of EDI File.
     *
     * @param ediFileWS
     * @param statusWS
     * @param escapeValidation if value is true then escape the non mandatory validation rule
     * @
     */
    @Override
    public void updateEDIStatus(EDIFileWS ediFileWS, EDIFileStatusWS statusWS, Boolean escapeValidation) {
        new EDIFileBL().updateStatus(ediFileWS, statusWS, escapeValidation);
    }

    @Override
    public void processMigrationPayment(PaymentWS paymentWS) {

        if(paymentWS != null) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(paymentWS.getPaymentInstruments());
        }

        IPaymentSessionBean session = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        Integer[] invoiceIds = getUnpaidInvoicesOldestFirst(paymentWS.getUserId());
        PaymentBL paymentBL = new PaymentBL();
        InvoiceBL invoiceBL = new InvoiceBL();
        PaymentDTOEx payment = new PaymentDTOEx(paymentWS);
        payment.setPaymentResult(new PaymentResultDAS().find(CommonConstants.RESULT_ENTERED));
        payment.setBalance(payment.getAmount());
        paymentBL.create(payment, paymentWS.getUserId());
        PaymentDTO paymentDTO = paymentBL.getDTO();
        for(Integer invoiceId: invoiceIds){
            invoiceBL.set(invoiceId);
            session.applyPayment(paymentDTO.getId(), invoiceBL.getDTO().getId());
            paymentBL.set(paymentDTO.getId());
            paymentDTO = paymentBL.getDTO();
            if(paymentDTO.getBalance().compareTo(BigDecimal.ZERO) <= 0){
                break;
            }
        }
    }

    @Override
    public void applyPaymentsToInvoices(Integer userId) {
        IPaymentSessionBean session = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        Integer[] paymentIds = null;
        Integer[] invoiceIds = null;
        paymentIds = new PaymentBL().getPaymentByUserId(userId);

        for (Integer paymentId: paymentIds) {
            invoiceIds = new InvoiceBL().getUnpaidInvoicesByUserId(userId);
            PaymentDTO paymentDTO = new PaymentDAS().find(paymentId);

            InvoiceBL invoiceBL = new InvoiceBL();
            for(Integer invoiceId: invoiceIds){
                invoiceBL.set(invoiceId);
                createPaymentLink(invoiceBL.getDTO().getId(), paymentDTO.getId());
                paymentDTO = new PaymentDAS().find(paymentDTO.getId());
                if(paymentDTO.getBalance().compareTo(BigDecimal.ZERO) <= 0){
                    break;
                }
            }
        }
    }

    /**
     * Returns an array of IDs for all unpaid invoices under the given user ID.
     *
     * @param userId user IDs
     * @return array of un-paid invoice IDs with oldest first
     */
    @Override
    @Transactional(readOnly=true)
    public Integer[] getUnpaidInvoicesOldestFirst(Integer userId) {
        try {
            CachedRowSet rs = new InvoiceBL().getPayableInvoicesByUserOldestFirst(userId);
            Integer[] invoiceIds = new Integer[rs.size()];
            int i = 0;
            while (rs.next()) {
                invoiceIds[i++] = rs.getInt(1);
            }

            rs.close();
            return invoiceIds;

        } catch (SQLException e) {
            throw new SessionInternalError("Exception occurred querying payable invoices.");
        } catch (Exception e) {
            throw new SessionInternalError("An un-handled exception occurred querying payable invoices.");
        }
    }

    @Override
    public Integer createAdjustmentOrderAndInvoice(String customerPrimaryAccount, OrderWS order, OrderChangeWS[] orderChanges){
        UserWS user = getUserBySupplierID(customerPrimaryAccount);
        if(user == null){
            return null;
        }

        order.setUserId(user.getId());
        order.setOrderStatusWS(findOrderStatusById(getDefaultOrderStatusId(OrderStatusFlag.INVOICE, user.getEntityId())));

        Integer orderChangeStatus = getOrderChangeApplyStatus(user.getEntityId());
        for(OrderChangeWS orderChange : orderChanges) {
            orderChange.setStatusId(orderChangeStatus);
        }

        OrderBL orderBL = new OrderBL();
        Integer orderId = orderBL.create(user.getEntityId(), null,
                orderBL.getDTO(order));
        //      Integer orderId = createUpdateOrder(order, orderChanges);
        Integer invoiceId = createInvoiceFromOrder(orderId, null);
        InvoiceBL invoiceBL = new InvoiceBL(invoiceId);
        invoiceBL.getEntity().setCreateDatetime(order.getActiveSince());
        invoiceBL.getEntity().setDueDate(order.getActiveSince());
        return invoiceId;
    }

    private OrderWS getOrderWSForHistoricalDataMigration(Integer userId, Integer itemId, OrderStatusWS orderStatusWS, Integer orderPeriod, String date,
            String amount, String adjustmentType, String referenceNumber){
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(userId);
        newOrder.setBillingTypeId(2);
        newOrder.setCurrencyId(1);
        newOrder.setNotes("Reference Number:" + referenceNumber + ", Adj Type:" + adjustmentType);
        Date convertedDate = convertDateForMigration(date);
        newOrder.setActiveSince(convertedDate);
        newOrder.setNextBillableDay(convertedDate);
        newOrder.setOrderStatusWS(orderStatusWS);
        newOrder.setPeriod(orderPeriod);
        OrderLineWS line  = new OrderLineWS();
        line.setPrice(amount);
        line.setTypeId(1);
        line.setQuantity("1");
        line.setAmount(amount);
        line.setDescription("Adjustment Order line");
        line.setItemId(itemId);
        line.setUseItem(false);
        OrderLineWS[] lines = new OrderLineWS[1];
        lines[0] = line;
        newOrder.setOrderLines(lines);
        return newOrder;
    }

    private Date convertDateForMigration(String date){
        if(date == null || date.isEmpty()) {
            return null;
        }
        SimpleDateFormat sf = new SimpleDateFormat("MM/dd/yyyy");
        try {
            return (Date) sf.parseObject(date);
        } catch (ParseException e) {
            logger.warn("Unparsable date "+date);
            return null;
        }
    }

    private OrderChangeWS createOrderChangeForAdjustmentOrder(Integer itemId, String description, OrderWS orderSaved, String quantity, Integer entityId, String amount) {
        OrderChangeWS orderChange = new OrderChangeWS();
        orderChange.setItemId(itemId);
        orderChange.setStartDate(orderSaved.getActiveSince());
        orderChange.setNextBillableDate(orderSaved.getNextBillableDay());
        orderChange.setApplicationDate(orderSaved.getActiveSince());
        orderChange.setQuantity(quantity);
        orderChange.setPrice(amount);
        orderChange.setUserAssignedStatusId(getOrderChangeApplyStatus(entityId));
        orderChange.setDescription(description);
        orderChange.setOrderChangeTypeId(1);
        if(orderSaved.getId()!=null) {
            orderChange.setOrderId(orderSaved.getId());
        }
        orderChange.setUseItem(0);
        return orderChange;
    }

    private Integer getOrderChangeApplyStatus(Integer entityId){
        OrderChangeStatusWS[] list = getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for(OrderChangeStatusWS orderChangeStatus : list){
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if(statusId != null){
            return statusId;
        }else{
            return null;
        }
    }

    @Override
    public String createPaymentForHistoricalMigration(String customerPrimaryAccount, String primaryAccountMetaFieldName, Integer[][] chequePmMap,String amount, String date){
        UserWS user = getUserByCustomerMetaField(customerPrimaryAccount, primaryAccountMetaFieldName);
        if(user == null){
            return null;
        }
        PaymentWS payment = createPaymentForMigration(user.getUserId(), getMap(chequePmMap).get(user.getEntityId()), amount, date);
        processMigrationPayment(payment);
        return "created";
    }

    private HashMap<Integer, Integer> getMap(Integer[][] chequePmMap){
        Map<Integer, Integer> map = new HashMap<Integer, Integer>(chequePmMap.length);
        for (Integer[] array : chequePmMap)
        {
            map.put(array[0], array[1]);
        }
        return (HashMap<Integer, Integer>)map;
    }

    //  private PaymentInformationWS createACHForMigrationPayment(String customerName,
    //          String bankName, String routingNumber, String accountNumber,Integer chequePmId) {
    //      String ACH_MF_ROUTING_NUMBER = "Bank Routing Number";
    //      String ACH_MF_BANK_NAME = "Bank Name";
    //      String ACH_MF_CUSTOMER_NAME = "Customer Name";
    //      String ACH_MF_ACCOUNT_NUMBER = "Bank Account Number";
    //      String ACH_MF_ACCOUNT_TYPE = "Bank Account Type";
    //      PaymentInformationWS cc = new PaymentInformationWS();
    //      cc.setPaymentMethodTypeId(chequePmId);
    //      cc.setProcessingOrder(new Integer(1));
    //      cc.setPaymentMethodId(5);
    //
    //      List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
    //
    //      MetaFieldValueWS routing = createMetaField(ACH_MF_ROUTING_NUMBER, false, true, DataType.STRING, 1, routingNumber);
    //      metaFields.add(routing);
    //
    //      MetaFieldValueWS customer = createMetaField(ACH_MF_CUSTOMER_NAME, false, true, DataType.STRING, 2, customerName);
    //      metaFields.add(customer);
    //
    //      MetaFieldValueWS account = createMetaField(ACH_MF_ACCOUNT_NUMBER, false, true, DataType.STRING, 3, accountNumber);
    //      metaFields.add(account);
    //
    //      MetaFieldValueWS bank = createMetaField(ACH_MF_BANK_NAME, false, true, DataType.STRING, 4, bankName);
    //      metaFields.add(bank);
    //
    //      MetaFieldValueWS type = createMetaField(ACH_MF_ACCOUNT_TYPE, false, true, DataType.ENUMERATION, 5, "CHECKING");
    //      metaFields.add(type);
    //
    //      MetaFieldValueWS[] metaFieldsArray = new MetaFieldValueWS[metaFields.size()];
    //      cc.setMetaFields(metaFields.toArray(metaFieldsArray));
    //
    //      return cc;
    //  }

    @Override
    public String createPaymentForHistoricalDateMigration(String customerPrimaryAccount, Integer chequePmId, String amount, String date) {
        UserDTO user = userDAS.findByUserName(customerPrimaryAccount, getCallerCompanyId());
        if(user == null){
            return null;
        }
        PaymentWS payment = createPaymentForMigration(user.getUserId(), chequePmId, amount, date);
        processMigrationPayment(payment);
        return "created";
    }

    @Override
    public String adjustUserBalance(String customerPrimaryAccount, String amount, Integer chequePmId, String date) {
        UserWS user = getUserBySupplierID(customerPrimaryAccount);
        if(user == null){
            return "failure";
        }
        BigDecimal balance = user.getOwingBalanceAsDecimal();
        BigDecimal requiredBalance = new BigDecimal(amount);
        BigDecimal adjustment = requiredBalance.subtract(balance);
        Date adjustmentDate = convertDateForMigration(date);
        if(adjustmentDate == null) {
            adjustmentDate = companyCurrentDate();
        }

        logger.debug("Balance [{}], Required Balance [{}], Adjustment[{}]", balance, requiredBalance, adjustment);

        if(adjustment.compareTo(BigDecimal.ZERO) > 0) {
            IInvoiceSessionBean session = Context
                    .getBean(Context.Name.INVOICE_SESSION);

            NewInvoiceContext newInvoiceDTO = new NewInvoiceContext();

            UserDTO userDTO = userDAS.find(user.getId());
            newInvoiceDTO.setBaseUser(userDTO);
            CurrencyDTO currency = new CurrencyDAS()
            .find(1);
            newInvoiceDTO.setCurrency(currency);
            newInvoiceDTO.setCreateDatetime(adjustmentDate);
            newInvoiceDTO.setDueDate(adjustmentDate);
            newInvoiceDTO.setTotal(adjustment);
            newInvoiceDTO.setInvoiceStatus(new InvoiceStatusDAS().find(CommonConstants.INVOICE_STATUS_UNPAID));
            //            newInvoiceDTO.setToProcess(invoiceWS.getToProcess());
            newInvoiceDTO.setBalance(adjustment);
            newInvoiceDTO.setCarriedBalance(BigDecimal.ZERO);
            newInvoiceDTO.setIsReview(0); // set fake value if null
            newInvoiceDTO.setDeleted(0);
            newInvoiceDTO.setCustomerNotes("Adjustment for user balance.");
            //            newInvoiceDTO.setPublicNumber();
            newInvoiceDTO.setLastReminder(adjustmentDate);
            //            newInvoiceDTO.setOverdueStep(invoiceWS.getOverdueStep());
            newInvoiceDTO.setCreateTimestamp(TimezoneHelper.serverCurrentDate());

            // if create date time is given then we can assume that that
            // is the billing date, otherwise we will fake the billing date
            newInvoiceDTO.setBillingDate(adjustmentDate);

            com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO dbInvoiceLineDTO = new com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO();

            //            dbInvoiceLineDTO.setItem(itemDTO);
            dbInvoiceLineDTO.setAmount(adjustment);
            dbInvoiceLineDTO.setQuantity(BigDecimal.ONE);
            dbInvoiceLineDTO.setPrice(adjustment);
            dbInvoiceLineDTO.setDeleted(0);
            dbInvoiceLineDTO.setDescription("Adjustment for user balance");
            //            dbInvoiceLineDTO.setSourceUserId(invoiceLineDTO.getSourceUserId());
            dbInvoiceLineDTO.setIsPercentage(0);
            dbInvoiceLineDTO.setInvoiceLineType(new InvoiceLineTypeDTO(3)); // Due
            // invoice
            // line
            // type

            newInvoiceDTO.getResultLines().add(dbInvoiceLineDTO);

            logger.debug("Invoice: {}", newInvoiceDTO);
            InvoiceDTO newInvoice = session.create(user.getEntityId(),
                    user.getUserId(), newInvoiceDTO);
        } else if(adjustment.compareTo(BigDecimal.ZERO) < 0) {
            adjustment = adjustment.abs();
            PaymentWS payment = new PaymentWS();
            payment.setAmount(adjustment);
            payment.setIsRefund(0);
            payment.setMethodId(chequePmId);
            payment.setPaymentDate(adjustmentDate);
            payment.setCreateDatetime(adjustmentDate);
            payment.setResultId(4);
            payment.setCurrencyId(1);
            payment.setUserId(user.getId());
            payment.setPaymentNotes("Payment during migration");
            payment.setPaymentPeriod(1);

            logger.debug("Payment: {}", payment);
            processMigrationPayment(payment);
        } else {
            logger.debug("No adjustment required");
        }

        return "success";
    }

    private PaymentWS createPaymentForMigration(Integer userId, Integer chequePmId, String amount, String pmtDate){
        Date date = convertDateForMigration(pmtDate);
        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount.replace("-", ""));
        payment.setIsRefund(0);
        payment.setMethodId(chequePmId);
        payment.setPaymentDate(date);
        payment.setCreateDatetime(date);
        payment.setResultId(4);
        payment.setCurrencyId(1);
        payment.setUserId(userId);
        payment.setPaymentNotes("Payment during migration");
        payment.setPaymentPeriod(1);

        //      PaymentInformationWS ach = createACHForMigrationPayment("Frodo Baggins", "Shire Financial Bank", "123456789", "123456789", chequePmId);
        //      payment.getPaymentInstruments().add(ach);
        return payment;
    }

    //    private PaymentInformationWS createACHForMigrationPayment(String customerName,
    //          String bankName, String routingNumber, String accountNumber,Integer chequePmId) {
    //      String ACH_MF_ROUTING_NUMBER = "Bank Routing Number";
    //      String ACH_MF_BANK_NAME = "Bank Name";
    //      String ACH_MF_CUSTOMER_NAME = "Customer Name";
    //      String ACH_MF_ACCOUNT_NUMBER = "Bank Account Number";
    //      String ACH_MF_ACCOUNT_TYPE = "Bank Account Type";
    //      PaymentInformationWS cc = new PaymentInformationWS();
    //      cc.setPaymentMethodTypeId(chequePmId);
    //      cc.setProcessingOrder(new Integer(1));
    //      cc.setPaymentMethodId(5);
    //
    //      List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
    //
    //      MetaFieldValueWS routing = createMetaField(ACH_MF_ROUTING_NUMBER, false, true, DataType.STRING, 1, routingNumber);
    //      metaFields.add(routing);
    //
    //      MetaFieldValueWS customer = createMetaField(ACH_MF_CUSTOMER_NAME, false, true, DataType.STRING, 2, customerName);
    //      metaFields.add(customer);
    //
    //      MetaFieldValueWS account = createMetaField(ACH_MF_ACCOUNT_NUMBER, false, true, DataType.STRING, 3, accountNumber);
    //      metaFields.add(account);
    //
    //      MetaFieldValueWS bank = createMetaField(ACH_MF_BANK_NAME, false, true, DataType.STRING, 4, bankName);
    //      metaFields.add(bank);
    //
    //      MetaFieldValueWS type = createMetaField(ACH_MF_ACCOUNT_TYPE, false, true, DataType.ENUMERATION, 5, "CHECKING");
    //      metaFields.add(type);
    //
    //      MetaFieldValueWS[] metaFieldsArray = new MetaFieldValueWS[metaFields.size()];
    //      cc.setMetaFields(metaFields.toArray(metaFieldsArray));
    //
    //      return cc;
    //  }

    private MetaFieldValueWS createMetaField(String fieldName, boolean disabled, boolean mandatory,DataType dataType, Integer displayOrder, String value){
        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.getMetaField().setDataType(dataType);
        metaField.getMetaField().setDisabled(disabled);
        metaField.getMetaField().setMandatory(mandatory);
        metaField.getMetaField().setDisplayOrder(displayOrder);
        metaField.setFieldName(fieldName);
        metaField.setStringValue(value);
        return metaField;
    }

    @Override
    public MediationRatingSchemeWS getRatingScheme(Integer mediationRatingSchemeId)  {
        if (mediationRatingSchemeId == null) {
            return null;
        }
        RatingSchemeBL bl = new RatingSchemeBL(mediationRatingSchemeId);
        return bl.getWS();
    }

    @Override
    public MediationRatingSchemeWS[] getRatingSchemesForEntity () {
        return getRatingSchemesPagedForEntity(null, null);
    }

    @Override
    public MediationRatingSchemeWS[] getRatingSchemesPagedForEntity (Integer max, Integer offset) {
        List<MediationRatingSchemeDTO> ratingSchemes = new MediationRatingSchemeDAS().findAllByEntity(getCallerCompanyId(), max, offset);

        return ratingSchemes.stream().map(ws -> RatingSchemeBL.getWS(ws)).toArray(size -> new MediationRatingSchemeWS[size]);
    }

    @Override
    public Long countRatingSchemesPagedForEntity () {
        return new MediationRatingSchemeDAS().countAllByEntity(getCallerCompanyId());
    }

    @Override
    public boolean deleteRatingScheme(Integer ratingSchemeId)  {
        try {
            RatingSchemeBL bl = new RatingSchemeBL(ratingSchemeId);
            return bl.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public Integer createRatingScheme(MediationRatingSchemeWS ws) {
        validateRatingScheme(ws);
        ws.setEntity(getCallerCompanyId());
        MediationRatingSchemeDTO mediationRatingSchemeDTO = RatingSchemeBL.getDTO(ws);

        return new RatingSchemeBL().create(mediationRatingSchemeDTO).getId();
    }

    private void validateRatingScheme(MediationRatingSchemeWS ws) {
        List<String> errors = new ArrayList<>();
        MediationRatingSchemeDAS das = new MediationRatingSchemeDAS();

        if(ws.isGlobal() && das.findGlobalRatingScheme(getCallerCompanyId(), ws.getId()) != null) {
            errors.add("Operation fail. Can not be more than one rating scheme set.");
        }

        if(!das.isValidName(ws.getName(), ws.getId())) {
            errors.add("Operation fail. There is another rating scheme with name " + ws.getName());
        }

        if(ws.getInitialIncrement() == null) {
            errors.add("Initial increment is required");
        }

        if(ws.getMainIncrement() == null) {
            errors.add("Main increment is required");
        }

        if((ws.getMainIncrement() !=null && ws.getMainIncrement()==0) && (ws.getInitialIncrement() !=null && ws.getInitialIncrement() == 0)) {
            errors.add("Both initial increment and main increment can not be set to 0.");
        }

        // Validate associations
        List<RatingSchemeAssociationWS> associations = ws.getAssociations();
        for(RatingSchemeAssociationWS association: associations) {
            List<Integer> companiesForMediation = RatingSchemeBL.findAssociatedCompaniesForMediation(association.getMediation().getId(), association.getRatingScheme());
            if(companiesForMediation.contains(association.getCompany().getId())) {
                errors.add("Mediation " + association.getMediation().getId() + " for company " + association.getCompany().getId() + " already has a rating scheme associated.");
            }
        }

        if(errors.size() > 0) {
            throw new SessionInternalError("Rating Scheme validation failed.",
                    errors.toArray(new String[errors.size()]));
        }

    }

    @Override
    public Integer createUsageRatingScheme(UsageRatingSchemeWS ws)  {
        UsageRatingSchemeBL bl = new UsageRatingSchemeBL();
        ws.setEntityId(getCallerCompanyId());
        bl.validateUsageRatingScheme(ws);

        UsageRatingSchemeDTO usageRatingSchemeDTO = bl.getDTO(ws);
        return bl.create(usageRatingSchemeDTO).getId();
    }

    @Override
    public boolean deleteUsageRatingScheme(Integer usageRatingSchemeId)  {
        UsageRatingSchemeBL bl = new UsageRatingSchemeBL(usageRatingSchemeId);
        return bl.delete();
    }

    @Override
    public UsageRatingSchemeWS getUsageRatingScheme(Integer usageRatingSchemeId) {
        if (usageRatingSchemeId == null) {
            throw new SessionInternalError("Scheme Id is missing", HttpStatus.SC_BAD_REQUEST);
        }

        UsageRatingSchemeBL bl = new UsageRatingSchemeBL(usageRatingSchemeId);
        return bl.getWS();
    }

    @Override
    public Long countUsageRatingSchemes() {
        return new UsageRatingSchemeBL().countAllRatingSchemes(getCallerCompanyId());
    }

    @Override
    public List<UsageRatingSchemeWS> findAllUsageRatingSchemes() {
        return getAllUsageRatingSchemes(null, null);
    }

    @Override
    public List<UsageRatingSchemeWS> getAllUsageRatingSchemes(Integer max, Integer offset) {
        List<UsageRatingSchemeDTO> usageRatingSchemeDTOS = new UsageRatingSchemeBL()
        .findAll(getCallerCompanyId(), max, offset);

        return usageRatingSchemeDTOS.stream()
                .map(dto -> UsageRatingSchemeBL.getWS(dto))
                .collect(Collectors.toList());
    }

    @Override
    public List<UsageRatingSchemeType> findAllRatingSchemeTypeValues() {
        return new UsageRatingSchemeBL().findAllRatingSchemeTypeValues();
    }

    public IUsageRatingScheme findUsageRatingSchemeInstanceByName(String name) {
        return new UsageRatingSchemeBL().findUsageRatingSchemeInstanceByName(name);
    }

    @Override
    public String createApiUserDetail(ApiUserDetailWS ws)  {

        ws.setCompanyId(getCallerCompanyId());

        if(!apiUserDetailBL.authenticateUser(ws)){
            logger.debug("invalid combination of userId, password and companyId");
            throw new SessionInternalError("Invalid user details", HttpStatus.SC_NOT_FOUND);
        }

        ApiUserDetailDTO apiUserDetailDTO = apiUserDetailBL.getDTO(ws);
        String usernameWithCompanyId = ws.getUserName() + ";" + ws.getCompanyId();

        apiUserDetailDTO.setAccessCode(apiUserDetailBL.generateAccessCode(usernameWithCompanyId));
        logger.debug("Generated access code " + apiUserDetailDTO.getAccessCode());

        return apiUserDetailBL.create(apiUserDetailDTO).getAccessCode();
    }

    @Override
    public Long countApiUserDetails () {
        return apiUserDetailBL.countAllApiUserDetail();
    }

    @Override
    public List<ApiUserDetailWS> findAllApiUserDetails() {
        return getAllApiUserDetails(null, null);
    }
    @Override
    public List<ApiUserDetailWS> getAllApiUserDetails(Integer max, Integer offset) {
        return apiUserDetailBL.findAll(max, offset, getCallerCompanyId());
    }

    @Override
    public ApiUserDetailWS getUserDetails(String accessCode){ return apiUserDetailBL.getUserDetails(accessCode); }

    /**
     * Return payment ids of the user.
     *
     * @param userId
     * @return
     */
    @Override
    public Integer[] getPaymentsByUserId(Integer userId)  {
        return new PaymentBL().getAllPaymentsByUser(userId);
    }

    @Override
    public Integer getRatingSchemeForMediationAndCompany(Integer mediationCfgId, Integer companyId) {
        return  RatingSchemeBL.getRatingSchemeIdForMediation(mediationCfgId, companyId);
    }

    @Override
    public BigDecimal getQuantity(Integer ratingSchemeId, Integer callDuration) {
        return RatingSchemeBL.getQuantity(ratingSchemeId, callDuration);
    }

    @Override
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderId(Integer orderId) {
        return new SwapPlanHistoryBL().getSwapPlanHistroyByOrderId(orderId);
    }

    @Override
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderAndSwapDate(
            Integer orderId, Date from, Date to) {
        return new SwapPlanHistoryBL().getSwapPlanHistroyByOrderAndSwapDate(orderId, from, to);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID triggerMediationByConfigurationByFile(Integer cfgId, File file) {
        if(isMediationProcessRunning()) {
            throw new SessionInternalError("Mediation Process is Already Running",
                    new String [] {"Please wait to complete already running mediation process."});
        }

        IMediationSessionBean mediationBean = Context.getBean(Context.Name.MEDIATION_SESSION);
        MediationConfiguration configuration = mediationBean.getMediationConfiguration(cfgId);

        return mediationService.triggerMediationJobLauncherByConfiguration(getCallerCompanyId(), cfgId,
                configuration.getMediationJobLauncher(), file);
    }

    @Override
    public JbillingMediationRecord[] getMediationRecordsByMediationProcessAndStatus(String mediationProcessId, Integer statusId) {
        if(null==mediationProcessId) {
            throw new SessionInternalError("Mediation Process ID is required", new String[]{"Mediation Process ID Should Not Be NULL."});
        }
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.uuid("processId", FilterConstraint.EQ, UUID.fromString(mediationProcessId)));
        if(null!=statusId) {
            filters.add(new Filter("status", FilterConstraint.EQ, JbillingMediationRecordDao.STATUS.
                    valueOf(JbillingMediationRecord.getStatusByStatusId(statusId).name())));
        }
        return mediationService.findMediationRecordsByFilters(0, 0, filters).toArray(new JbillingMediationRecord[0]);
    }

    @Override
    public JbillingMediationErrorRecord[] getErrorsByMediationProcess(String mediationProcessId, int offset, int limit) {
        if(null==mediationProcessId) {
            throw new SessionInternalError("Mediation Process ID is required", new String[]{"Mediation Process ID Should Not Be NULL."});
        }
        List<Filter> filters = new ArrayList<>();
        if(null!=mediationProcessId) {
            filters.add(Filter.uuid("processId", FilterConstraint.EQ, UUID.fromString(mediationProcessId)));
        }
        return mediationService.findMediationErrorRecordsByFilters(offset, limit, filters).toArray(new JbillingMediationErrorRecord[0]);
    }

    @Override
    public QueryParameterWS[] getParametersByQueryCode(String queryCode) {
        if(null==queryCode || queryCode.isEmpty()) {
            throw new SessionInternalError("Query code is required", new String[]{"Query code  ID Should Not Be Null or Empty."});
        }
        PreEvaluatedSQLService preEvaluatedSQLService = Context.getBean(PreEvaluatedSQLService.BEAN_NAME);
        return preEvaluatedSQLService.getParametersByQueryCode(queryCode).toArray(new QueryParameterWS[0]);
    }

    @Override
    public QueryResultWS getQueryResult(String queryCode, QueryParameterWS[] parameters, Integer limit, Integer offSet) {
        if(null==queryCode || queryCode.isEmpty()) {
            throw new SessionInternalError("Query code is required", new String[]{"Query Code Should Not Be Null or Empty."});
        }
        PreEvaluatedSQLService preEvaluatedSQLService = Context.getBean(PreEvaluatedSQLService.BEAN_NAME);
        PreEvaluatedSQLDTO query = preEvaluatedSQLService.getPreEvaluatedSQLByQueryCode(queryCode);
        if(null==query) {
            throw new SessionInternalError("Invalid Query Code", new String[]{"You have passed Invalid Query Code: " +queryCode});
        }
        if(!query.getParentEntityId().equals(getCallerCompanyId())) {
            throw new SessionInternalError("Unauthorised User", new String[]{"You do not have permission to use :  "+queryCode +" Query Code "});
        }
        String errorMessage = PreEvaluatedSQLValidator.validateQuery(query);
        if(!errorMessage.isEmpty()) {
            throw new SessionInternalError("Execution of  :  "+queryCode +" Query Code is not allowed ", new String[]{errorMessage});
        }
        List<String> errorMessages = PreEvaluatedSQLValidator.validateParameters(query, parameters);
        if(!errorMessages.isEmpty()) {
            throw new SessionInternalError("Invalid Parameters Passed ", errorMessages.toArray(new String[0]));
        }
        int defaultLimit = 1000;
        if(null==limit || limit.intValue()<=0 || limit>defaultLimit ) {
            limit = defaultLimit;
        }
        try {
            return preEvaluatedSQLService.getQueryResult(query, parameters==null ? new QueryParameterWS[]{} : parameters, limit, offSet);
        } catch(Exception ex) {
            if(ex instanceof SQLException) {
                throw new SessionInternalError(ex.getMessage());
            }
            throw new SessionInternalError(ex.getMessage());
        }

    }
    @Override
    public List<Integer> getEDIFiles(Integer ediTypeId, String fieldKey, String fieldValue, TransactionType transactionType, String statusName){
        if(ediTypeId== null || fieldKey== null) {
            throw new SessionInternalError("Data is not valid to search edi file");
        }
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", ediTypeId));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", fieldKey));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", fieldValue));

        if(transactionType!=null) {
            conjunction.add(Restrictions.eq("type", transactionType));
        }

        if(statusName!=null) {
            conjunction.add(Restrictions.eq("status.name", statusName));
        }

        return new EDIFileDAS().findFileByData(conjunction);
    }

    @Override
    public EDIFileWS getEDIFileById(Integer ediFileId){
        return new EDIFileBL(ediFileId).getWS();
    }

    /**
     * Fetch all orphan edi files based on type.
     * @param type
     * @return List of view object for files
     */
    @Override
    public List<OrphanEDIFile> getLDCFiles(TransactionType type){
        return OrphanLDCFiles.getOrphanEDIFiles(getCallerCompanyId(), type);
    }

    /**
     * Get orphan edi file object from file system by file name.
     * @param type
     * @param fileName
     * @return File object
     */
    @Override
    public File getOrphanLDCFile(TransactionType type, String fileName){
        return OrphanLDCFiles.getOrphanEDIFile(getCallerCompanyId(), type, fileName);
    }

    /**
     * delete orphan edi files from file system.
     * @param type
     * @param fileNames List of file names
     */
    @Override
    public void deleteOrphanEDIFile(TransactionType type, List<String> fileNames){
        OrphanLDCFiles.delete(getCallerCompanyId(), type, fileNames);
    }

    /**
     * Upload file in ediCommunication folder. Directory path will be picked from company level meta field.
     * If it is not defined there then it will go for default i.e. 'resources/nges/ediCommunication/inbound'.
     *
     * @param ediFile text file which should belong to a type identified by extension in name.
     */
    @Override
    public void uploadEDIFile(File ediFile) {
        if (ediFile == null) {
            throw new SessionInternalError("File is not valid");
        }
        IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        File fileDir = new File(ediTransactionBean.getEDICommunicationPath(getCallerCompanyId(), TransactionType.INBOUND));
        String fileName;
        fileName = ediFile.getName().contains(".txt") ? ediFile.getName().replace(".txt", "") : ediFile.getName();
        File file = new File(fileDir.getAbsolutePath() + File.separator + fileName);
        try {
            FileUtils.copyFile(ediFile, file);
        } catch (IOException ex) {
            logger.error(ex.getLocalizedMessage());
            throw new SessionInternalError("Error occurred while copying file to folder : " + fileDir.getAbsolutePath());
        }
    }
    /**
     * This API will not just Process signup payment but it will create the user with payment instruments details.
     * if CIM profile has been created successfully on the gateway Then it will process signup payment.
     * If there is any error in CIM profile creation on gateway or while processing signup payment,
     * then it will throws exception and rollback tranction.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public Integer processSignupPayment(UserWS user, PaymentWS payment) {

        logger.debug("In processSignupPayment...");
        if (user == null || payment == null) {
            throw new SessionInternalError("To process signup payment, user or payment can not be null");
        }

        List<PaymentInformationWS> userPaymentInstrument = user.getPaymentInstruments();

        if (null == userPaymentInstrument || userPaymentInstrument.isEmpty()) {
            throw new SessionInternalError("To process signup payment, user must have payment instrument");
        }

        PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(userPaymentInstrument);

        Integer userId = createUserWithCompanyId(user, getCallerCompanyId());

        List<PaymentInformationWS> creditCards = new UserBL(userId).getAllUserPaymentInstrumentWS();

        // Check CIM profile is created successfully or not.

        PaymentInformationBL piBl = new PaymentInformationBL();
        boolean isProfileCreated = creditCards.stream()
                .filter(card -> (piBl.getCharMetaFieldByType(card, MetaFieldType.GATEWAY_KEY)!=null &&
                piBl.getCharMetaFieldByType(card, MetaFieldType.GATEWAY_KEY).length!=0))
                .findAny()
                .isPresent();
        if (!isProfileCreated) {
            throw new SessionInternalError("Card validation transaction using preauth failed, reverting transaction.");
        }

        payment.setUserId(userId);
        payment.setPaymentInstruments(creditCards);

        CustomerSignupResponseWS response = new PaymentBL().makeSignupPayment(payment, getCallerCompanyId(), getCallerId());

        if (null == response.getResult() || response.getResult().intValue() != Constants.RESULT_OK.intValue()) {
            throw new SessionInternalError("Signup Payment is not successful.",
                    new String[] { response.getResponseMessage() });
        }

        return userId;
    }

    /**
     * API method to fetch all credit notes for the given entity id.
     * Returns an array of CreditNoteWS objects.
     */
    @Override
    public CreditNoteWS[] getAllCreditNotes(Integer entityId) {
        List<CreditNoteWS> wsList =new ArrayList<CreditNoteWS>();
        CreditNoteBL creditNoteBl = new CreditNoteBL();
        List<CreditNoteDTO> creditNotes = creditNoteBl.getAllCreditNotes(entityId);
        if (creditNotes != null){
            for (CreditNoteDTO creditNoteDTO : creditNotes) {
                wsList.add(creditNoteBl.getWS(creditNoteDTO));
            }
        }
        return wsList.toArray(new CreditNoteWS[wsList.size()]);
    }

    /**
     * This API gets the Credit Note for the given identifier.
     * Returns the CreditNoteWS
     */
    @Override
    @Transactional(readOnly=true)
    public CreditNoteWS getCreditNote(Integer creditNoteId) {
        CreditNoteBL creditNoteBl = new CreditNoteBL(creditNoteId);
        return creditNoteBl.getCreditNoteWS();
    }

    /**
     * This API method updates the credit note with the given WS object.
     */
    @Override
    public void updateCreditNote(CreditNoteWS creditNoteWs) {
        if (creditNoteWs != null){
            CreditNoteBL creditNoteBl = new CreditNoteBL();
            creditNoteBl.save(creditNoteBl.getDTO(creditNoteWs));
        }
    }

    /**
     * This API method soft deletes the credit note and its lines are soft deleted as well.
     */
    @Override
    public void deleteCreditNote(Integer creditNoteId) {
        new CreditNoteBL().delete(creditNoteId);
    }

    /**
     * This API method returns the specified amount of IDs of the last Credit Notes that belong to one user
     */
    @Override
    @Transactional(readOnly=true)
    public Integer[] getLastCreditNotes(Integer userId, Integer number)  {
        if (userId == null || number == null) {
            return null;
        }

        CreditNoteBL bl = new CreditNoteBL();
        return bl.getLastCreditNotes(userId, number);
    }

    /**
     * This API method applies the given credit note on the unpaid debit invoices
     * starting with the oldest invoice first.
     * @param creditNoteId
     */
    @Override
    public void applyCreditNote(Integer creditNoteId) {
        new CreditNoteBL().applyCreditNote(creditNoteId);
    }

    /**
     * This API method applies any existing credit notes for the given user
     * to any unpaid debit invoices starting with the oldest credit note
     * and applying on the oldest invoice first.
     * @param userId
     */
    @Override
    public void applyExistingCreditNotesToUnpaidInvoices(Integer userId) {
        new CreditNoteBL().applyExistingCreditNotesToUnpaidInvoices(userId);
    }

    /**
     * This API method applies existing credit notes to the given invoice,
     * starting with the oldest credit note first.
     * @param invoiceId
     */
    @Override
    public void applyExistingCreditNotesToInvoice(Integer invoiceId) {
        new CreditNoteBL().applyExistingCreditNotesToInvoice(new InvoiceBL(invoiceId).getEntity());
    }

    /**
     * This API method applies the given credit note on the given debit invoice
     * and pays it down reducing the balance of both credit note and the invoce.
     * @param creditNoteId
     * @param debitInvoiceId
     */
    @Override
    public void applyCreditNoteToInvoice(Integer creditNoteId, Integer debitInvoiceId) {
        CreditNoteBL creditNoteBl = new CreditNoteBL(creditNoteId);
        creditNoteBl.applyCreditNoteToInvoice(creditNoteBl.getCreditNote(), new InvoiceBL(debitInvoiceId).getEntity());
    }

    @Override
    public void removeCreditNoteLink(Integer invoiceId, Integer creditNoteId){
        if (invoiceId == null || creditNoteId == null) {
            return;
        }

        CreditNoteBL creditNoteBl = new CreditNoteBL(creditNoteId);

        boolean result = creditNoteBl.unLinkFromInvoice(invoiceId);
        if (!result) {
            throw new SessionInternalError("Unable to find the Invoice Id " + invoiceId + " linked to creditNote Id " + creditNoteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResultList findOrdersByFilters(int page, int size, String sort, String order, List<Filter> filters) {
        filters = addCompanyFilter(filters);
        return getPagedResultListForLoggedCompany(FilterFactory.orderFilterDAS().findByFilters(page, size, sort, order, filters),
                dto -> new OrderBL((OrderDTO) dto).getWS(getCallerLanguageId()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS[] filterOrders(Integer page, Integer size, Date activeSince, Integer productId, Integer orderStatusId) {
        if (null == page || null == size || 0 >= page || 0 >= size || 50 < size) {
            throw new SessionInternalError("Invalid Pagination Parameters Received, Page must be greater than 0 and Size must be between 1 and 50",
                    new String [] {"Please enter the correct Page and Size values."});
        }

        OrderBL order = new OrderBL();
        List<OrderDTO> orderDTOs = order.filterOrdersByParams(page, size, activeSince, productId, orderStatusId, getCallerCompanyId());
        OrderWS[] orderWSes = new OrderWS[orderDTOs.size()];
        int i = 0;
        for (OrderDTO orderDTO : orderDTOs) {
            orderWSes[i++] = new OrderBL(orderDTO).getWS(getCallerLanguageId());
        }
        return orderWSes;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResultList findProvisioningCommandsByFilters(int page, int size, String sort, String order, List<Filter> filters) {
        filters = addCompanyFilter(filters);
        return getPagedResultListForLoggedCompany(FilterFactory.provisioningFilterDAS().findByFilters(page, size, sort, order, filters),
                dto -> ProvisioningCommandBL.getCommandWS((ProvisioningCommandDTO) dto));
    }

    private List<Filter> addCompanyFilter(List<Filter> filters) {
        filters = new ArrayList<>(filters);
        filters.add(Filter.integer("company.id", FilterConstraint.EQ, getCallerCompanyId()));
        return filters;
    }

    private PagedResultList getPagedResultListForLoggedCompany(PagedResultList dtoResultList, Function converterToWs) {
        Supplier<List> supplier = () -> new PagedResultList();
        PagedResultList wsResultList = (PagedResultList) dtoResultList.stream()
                .map(obj -> converterToWs.apply(obj)).collect(Collectors.toCollection(supplier));
        wsResultList.setTotalCount(dtoResultList.getTotalCount());
        return wsResultList;
    }

    private Date companyCurrentDate() {
        return TimezoneHelper.companyCurrentDate(getCallerCompanyId());
    }

    /**
     * This API gets the Itemized Account Summary for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public ItemizedAccountWS getItemizedAccountByInvoiceId(Integer invoiceId)  {
        Integer languageId = getCallerLanguageId();
        return new InvoiceSummaryBL().getItemizedAccountByInvoiceId(invoiceId, languageId);
    }

    /**
     * This API gets the Invoice Summary for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceSummaryWS getInvoiceSummary(Integer invoiceId)  {
        return new InvoiceSummaryBL().getInvoiceSummaryByInvoiceId(invoiceId);
    }

    /**
     * This API gets the Recurring Invoice Lines for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceLineDTO[] getRecurringChargesByInvoiceId(Integer invoiceId)  {
        return InvoiceSummaryBL.getInvoiceLinesByInvoiceAndType(invoiceId, Constants.INVOICE_LINE_TYPE_ITEM_RECURRING);
    }

    /**
     * This API gets the One Time Invoice Lines for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceLineDTO[] getUsageChargesByInvoiceId(Integer invoiceId)  {
        return InvoiceSummaryBL.getInvoiceLinesByInvoiceAndType(invoiceId, Constants.INVOICE_LINE_TYPE_ITEM_ONETIME);
    }

    /**
     * This API gets the Penalty/Fees Invoice Lines for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceLineDTO[] getFeesByInvoiceId(Integer invoiceId)  {
        return InvoiceSummaryBL.getInvoiceLinesByInvoiceAndType(invoiceId, Constants.INVOICE_LINE_TYPE_PENALTY);
    }

    /**
     * This API gets the Taxes Invoice Lines for the given invoice Id.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceLineDTO[] getTaxesByInvoiceId(Integer invoiceId)  {
        return InvoiceSummaryBL.getInvoiceLinesByInvoiceAndType(invoiceId, Constants.INVOICE_LINE_TYPE_TAX);
    }

    /**
     * This API gets the Payments And Refuns in between last and this invoice.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentWS[] getPaymentsAndRefundsByInvoiceId(Integer invoiceId)  {
        return new InvoiceSummaryBL().getpaymentsAndRefundsByInvoiceId(invoiceId, getCallerLanguageId());
    }

    /**
     * This API gets the payment of type Credit and Credit Notes in between last and this invoice.
     * Returns the InvoiceSummaryWS
     */
    @Override
    @Transactional(readOnly = true)
    public CreditAdjustmentWS[] getCreditAdjustmentsByInvoiceId(Integer invoiceId)  {
        return new InvoiceSummaryBL().getCreditAdjustmentsByInvoiceId(invoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    public JobExecutionHeaderWS[] getJobExecutionsForDateRange(String jobType,Date startDate, Date endDate, int offset, int limit, String sort, String order)  {
        return new JobExecutionBL().getJobExecutionsForDateRange(getCallerCompanyId(), jobType, startDate, endDate, offset, limit, sort, order);
    }

    @Override
    public Integer createCancellationRequest(CancellationRequestWS cancellationRequest){
        try {
            return new CancellationRequestBL().createCancellationRequest(cancellationRequest,getCallerId());
        } catch(Exception ex) {
            throw new SessionInternalError("Error In createCancellationRequest ",ex);
        }

    }

    @Override
    public void updateCancellationRequest(CancellationRequestWS cancellationRequest){
        new CancellationRequestBL().updateCancellationRequest(cancellationRequest,getCallerId());
    }

    @Override
    public CancellationRequestWS[] getAllCancellationRequests(Integer entityId, Date startDate, Date endDate) {
        return new CancellationRequestBL().getAllCancellationRequests(entityId, startDate, endDate);
    }

    @Override
    public CancellationRequestWS getCancellationRequestById(Integer cancellationRequestId){
        return new CancellationRequestBL().getCancellationRequestById(cancellationRequestId);
    }

    @Override
    public CancellationRequestWS[] getCancellationRequestsByUserId(Integer userId){
        return new CancellationRequestBL().getCancellationRequestsByUserId(userId);
    }

    @Override
    public void deleteCancellationRequest(Integer cancellationId){
        new CancellationRequestBL().deleteCancellationRequest(cancellationId,getCallerId());
    }

    @Override
    public void updateOrderChangeEndDate(Integer orderChangeId, Date endDate){
        if(null != orderChangeId && null != endDate) {
            OrderChangeBL orderChangeBL = new OrderChangeBL();
            orderChangeBL.updateOrderChangeEndDate(orderChangeId, endDate);
        } else {
            throw new SessionInternalError("OrderChangeId and EndDate can't be null in updateOrderChangeEndDate");
        }
    }

    @Override
    public AgeingWS[] getAgeingConfigurationWithCollectionType(Integer languageId, CollectionType collectionType) {
        try {
            AgeingDTOEx[] dtoArr = billingProcessSession.getAgeingSteps(
                    getCallerCompanyId(), getCallerLanguageId(), languageId, collectionType);
            AgeingBL bl = new AgeingBL();
            AgeingWS[] wsArr = Arrays.stream(dtoArr)
                    .map(ws -> bl.getWS(ws))
                    .toArray(AgeingWS[]::new);
            return wsArr;
        } catch (IllegalArgumentException iae){
            throw new SessionInternalError("Please provide a valid Collection Type. Ex:"+Arrays.asList(CollectionType.values()),iae);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public void saveAgeingConfigurationWithCollectionType(AgeingWS[] steps,Integer languageId, CollectionType collectionType) {
        try {
            AgeingBL bl = new AgeingBL();
            AgeingDTOEx[] dtoList = Arrays.stream(steps)
                    .map(step -> bl.getDTOEx(step))
                    .toArray(AgeingDTOEx[]::new);

            billingProcessSession.setAgeingSteps(getCallerCompanyId(), languageId, dtoList, collectionType);
        } catch (IllegalArgumentException iae){
            throw new SessionInternalError("Please provide a valid Collection Type. Ex:"+Arrays.asList(CollectionType.values()),iae);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public void createUpdateOrderChange(Integer userId, String productCode, BigDecimal newPrice, BigDecimal newQuantity,
            Date changeEffectiveDate)  {
        logger.debug("In createUpdateOrderChange API...");
        if (userId == null || productCode == null || changeEffectiveDate == null) {
            throw new SessionInternalError("userId or productCode or changeEffectiveDate can not be null");
        }

        if (newPrice == null && newQuantity == null) {
            throw new SessionInternalError("Price and Quantity can not be null at the same time");
        }

        Integer itemId = getItemID(productCode);
        if (itemId == null) {
            throw new SessionInternalError("No product found for provided product code");
        }

        UserWS userWS = getUserWS(userId);
        if (userWS == null) {
            throw new SessionInternalError("No user found for provided userId");
        }

        // fetch all associated orders of user
        List<OrderDTO> orderDTOList = orderDAS.findAllActiveSubscriptions(userId, itemId);
        if (orderDTOList.isEmpty()) {
            throw new SessionInternalError("No order found for provided user Id " + userId + " and product code " + productCode);
        }
        OrderChangeBL bl = new OrderChangeBL();
        for (OrderDTO orderDTO : orderDTOList) {
            OrderWS orderWS = new OrderBL(orderDTO).getWS(orderDTO.getBaseUserByUserId().getLanguage().getId());
            logger.debug("Processing order {} ", orderWS);
            if (bl.validateChangeEffectiveDate(changeEffectiveDate, orderWS, userWS)) {
                logger.debug("Validation is done, going to update the order");
                if (orderWS.getOrderStatusWS().getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)) {
                    for (OrderLineWS orderLineWS : orderWS.getOrderLines()) {
                        if (orderLineWS.getItemId().equals(itemId)) {
                            OrderChangeWS[] orderChangeWSArray = null;
                            boolean isUpdated = false;
                            OrderChangeWS[] oldOrderChanges = getOrderChanges(orderWS.getId());
                            OrderChangeWS currentOrderChange = bl.getCurrentOrderChangeByEffectiveDate(
                                    changeEffectiveDate, itemId,
                                    oldOrderChanges);
                            if (null != currentOrderChange) {
                                logger.debug("Going to update currentOrderChange {} ", currentOrderChange);
                                if (newPrice == null) {
                                    newPrice = currentOrderChange.getPriceAsDecimal();
                                }
                                if (newQuantity == null) {
                                    newQuantity = currentOrderChange.getQuantityAsDecimal();
                                }

                                if ((newPrice.compareTo(currentOrderChange.getPriceAsDecimal()) != 0)
                                        || (newQuantity.compareTo(currentOrderChange.getQuantityAsDecimal()) != 0)) {
                                    isUpdated = true;
                                }

                                if (isUpdated) {
                                    orderChangeWSArray = bl.updateCurrentOrderChange(orderWS, itemId, userWS, newPrice,
                                            newQuantity, changeEffectiveDate, currentOrderChange);
                                }
                            } else {
                                logger.debug("Going to create new order Change as there is no current order change found"
                                        + " as per provided effective date {}", changeEffectiveDate);
                                if (newPrice == null) {
                                    newPrice = bl.getCurrentPrice(oldOrderChanges, itemId);
                                }
                                if (newQuantity == null) {
                                    newQuantity = bl.getCurrentQuantity(oldOrderChanges, itemId);
                                }
                                isUpdated = bl.isPriceOrQuantityUpdated(oldOrderChanges, itemId, newPrice, newQuantity);

                                if (isUpdated) {
                                    orderChangeWSArray = bl.createOrderChange(orderWS, itemId, userWS, newPrice,
                                            newQuantity, changeEffectiveDate, oldOrderChanges);
                                }
                            }

                            if (isUpdated) {
                                orderWS.setProrateFlag(orderWS.getProrateFlag());
                                updateOrder(orderWS, orderChangeWSArray);
                                logger.debug("Order change updated successfully for order {} ", orderWS.getId());
                                Optional<OrderLineWS> existingOrderLine = bl.getLineByItemId(orderWS.getOrderLines(), itemId);
                                if (existingOrderLine.isPresent()) {
                                    OrderChangeWS change = null;
                                    oldOrderChanges = getOrderChanges(orderWS.getId());
                                    for (OrderChangeWS orderChangeWS : oldOrderChanges) {
                                        if (orderChangeWS.getItemId().equals(itemId)
                                                && null == orderChangeWS.getEndDate()) {
                                            change = orderChangeWS;
                                            break;
                                        }
                                    }
                                    existingOrderLine.get().setQuantityAsDecimal(change.getQuantityAsDecimal());
                                    existingOrderLine.get().setPriceAsDecimal(change.getPriceAsDecimal());
                                    existingOrderLine.get().setAmountAsDecimal(change.getQuantityAsDecimal()
                                            .multiply(change.getPriceAsDecimal()));
                                    updateOrderLine(existingOrderLine.get());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PlanWS getPlanByInternalNumber(String internalNumber, Integer entityId){
        List<PlanDTO> planList = new PlanBL().findPlanByPlanNumber(internalNumber, entityId);
        if(null != planList && planList.size() > 1){
            throw new SessionInternalError("There are more than one plan with this Plan Number",
                    new String[] { "There are more than one plan with this Plan Number" });
        } else if(CollectionUtils.isEmpty(planList)){
            throw new SessionInternalError("There are no plan with this Plan Number",
                    new String[] { "There are no plan with this Plan Number" });
        }
        PlanDTO planDto = planList.get(0);
        PlanWS planWS = PlanBL.getWS(planDto);
        planWS.setDescription(planDto.getItem().getDescription(planDto.getItem().getEntity().getLanguageId()));
        return planWS;
    }

    private void clearReserveCache(String key){
        ReserveCacheEvent reserveCacheEvent = new ReserveCacheEvent();
        reserveCacheEvent.setKey(key);
        reserveCacheEvent.setEntityID(getCallerCompanyId());
        EventManager.process(reserveCacheEvent);
    }

    @Override
    public boolean notifyUserByEmail(Integer userId, Integer notificationId)  {

        if(null == userId) {
            throw new SessionInternalError("User id can not be null");
        }

        getUserWS(userId);

        if(null == notificationId){
            throw new SessionInternalError("Notification id can not be null");
        }

        NotificationMessageDAS das = new NotificationMessageDAS();
        NotificationMessageDTO notificationMessageDTO = das.findIt(notificationId, getCallerCompanyId(),
                getCallerLanguageId());

        if(null == notificationMessageDTO || !notificationMessageDTO.getNotificationMessageType().getCategory().
                getDescription(getCallerLanguageId()).equals("Users")){
            throw new SessionInternalError("Provided Notification Id not found under User Category. " +
                    "Please provide correct User notification ID");
        }

        NotificationBL notificationBL = new NotificationBL();
        notificationBL.set(notificationId,
                SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE),
                getCallerCompanyId());

        MessageDTO message = null;

        try {
            message = notificationBL.getCustomNotificationMessage(notificationId,
                    getCallerCompanyId(),userId, getCallerLanguageId());
        } catch (NotificationNotFoundException e) {
            logger.error("Notification not found");
            return false;
        }

        SwapPlanHistoryDAS swapPlanHistoryDAS = new SwapPlanHistoryDAS();
        SwapPlanHistoryDTO swapPlanHistoryDTO = swapPlanHistoryDAS.getLatestSwapPlanHistoryByUserId(userId);

        if(null != swapPlanHistoryDTO) {
            PlanWS oldPlan = getPlanWS(swapPlanHistoryDTO.getOldPlanId());
            PlanWS newPlan = getPlanWS(swapPlanHistoryDTO.getNewPlanID());
            ItemDTO oldPlanItem = new ItemBL(oldPlan.getItemId()).getEntity();
            ItemDTO newPlanItem = new ItemBL(newPlan.getItemId()).getEntity();

            message.addParameter("old_plan_description", oldPlanItem.getDescription(getCallerLanguageId()));
            message.addParameter("new_plan_description", newPlanItem.getDescription(getCallerLanguageId()));
        }

        INotificationSessionBean notificationSessionBean = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        return notificationSessionBean.notify(new UserBL(userId).getDto(), message);
    }

    @Override
    public Integer getIdFromCreateUpdateNotification(Integer messageId, MessageDTO dto) {
        Integer result = null;

        if (null == messageId) {
            result = new NotificationBL().createUpdate(getCallerCompanyId(), dto);
        } else {
            result = new NotificationBL(messageId).createUpdate(getCallerCompanyId(),
                    dto);
        }

        return result;
    }

    @Override
    public Integer createMessageNotificationType(Integer notificationCategoryId, String description, Integer languageId ){
        NotificationCategoryDAS notificationCategoryDAS = new NotificationCategoryDAS();
        NotificationCategoryDTO notificationCategory = notificationCategoryDAS.find(notificationCategoryId);

        NotificationMessageTypeDTO notificationMessageType = new NotificationMessageTypeDTO();
        notificationMessageType.setCategory(notificationCategory);
        notificationMessageType.setDescription(description,languageId);
        notificationMessageType = new NotificationMessageTypeDAS().save(notificationMessageType);
        new NotificationMessageTypeDAS().flush();

        logger.debug("Notification message type saved successfully {}",  notificationMessageType.getId());

        return notificationMessageType.getId();
    }

    @Override
    public CreditNoteInvoiceMapWS[] getCreditNoteInvoiceMaps(Date invoiceCreationStartDate, Date invoiceCreationEndDate) {
        return new CreditNoteBL().getCreditNoteInvoiceMaps(invoiceCreationStartDate, invoiceCreationEndDate);
    }

    @Override
    public SignupResponseWS processSignupRequest(SignupRequestWS request) {
        try {
            SignupPlaceHolder holder = SignupPlaceHolder.of(request, getCallerCompanyId());
            logger.debug("Prcoessing Signup Request {} for entity {}", request, holder.getEntityId());
            return new SignupRequestBL(holder).processSignUpRequest();
        } catch(Exception ex) {
            logger.error("processSignupRequest failed", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.debug("RollBack Tx!");
            SignupResponseWS errorResponse = new SignupResponseWS();
            errorResponse.addErrorResponse("PROCESS-SIGNUP-REQUEST-FAILED");
            return errorResponse;
        }
    }

    /**
     * validates user with given credential and return {@link UserWS}
     * @param userName
     * @param password
     * @return
     */
    @Override
    public UserWS validateLogin(String userName, String password) {
        String validationErrorMessage = "validation failed";
        if(StringUtils.isEmpty(userName)) {
            throw new SessionInternalError(validationErrorMessage, new String [] { "Please enter userName." }, HttpStatus.SC_BAD_REQUEST);
        }
        if(StringUtils.isEmpty(password)) {
            throw new SessionInternalError(validationErrorMessage, new String [] { "Please enter password." }, HttpStatus.SC_BAD_REQUEST);
        }
        try {
            Integer entityId = getCallerCompanyId();
            logger.debug("Validating user {} for entity {}", userName, entityId);
            UserBL userBL = new UserBL(userName, getCallerCompanyId());
            if(null == userBL.getEntity()) {
                logger.error("User Name {} not found for entity {}", userName, entityId);
                throw new SessionInternalError(validationErrorMessage, new String [] { "Please enter a valid user name." },
                        HttpStatus.SC_NOT_FOUND);
            }
            UserDTO user = userBL.getEntity();
            if(StringUtils.isEmpty(user.getPassword())) {
                logger.debug("user {} does not have password for entity {}", userName, entityId);
                throw new SessionInternalError(validationErrorMessage,
                        new String[] {String.format("user %s does not have password.", userName)});
            }
            if(!userBL.matchPasswordForUser(user, password)) {
                logger.error("given passsword {} did not match for user {} for entity {}", password, userName, entityId);
                throw new SessionInternalError(validationErrorMessage,
                        new String[] {"The entered current password does not match the stored password."});
            }
            return userBL.getUserWS();
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            logger.error("error in validateLogin ", ex);
            throw new SessionInternalError("Error in validateLogin", ex);
        }
    }

    @Override
    public void updatePassword(Integer userId, String currentPassword, String newPassword) {
        if(null == userId) {
            logger.error("UserId is null.");
            throw new SessionInternalError("UserId is required", new String [] { "Invalid user Id." }, HttpStatus.SC_BAD_REQUEST);
        }
        if(StringUtils.isEmpty(currentPassword) || StringUtils.isEmpty(newPassword)) {
            logger.error("User {}, password is null or blank.", userId);
            throw new SessionInternalError("Password is required", new String [] { "Invalid password." }, HttpStatus.SC_BAD_REQUEST);
        }
        if (newPassword.length() < 8 || newPassword.length() > 40 ||
                !newPassword.matches(Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES)) {
            logger.error("User's password must match required criteria.");
            throw new SessionInternalError(
                    "User's password must match required criteria.",
                    new String[] { "password must contain at least one upper case, one lower case, one digit, one special character. "
                            + "The password should be between 8 and 40 characters long" },
                            HttpStatus.SC_BAD_REQUEST);
        }

        try {
            Integer entityId = getCallerCompanyId();
            logger.debug("UpdatePassword user {} for entity {}", userId, entityId);
            // If user does't exist with the provided ID, then UserBL code is not working.
            UserDTO user = userDAS.findNow(userId);
            if (null == user) {
                logger.error("User {} not found for entity {}", userId, entityId);
                throw new SessionInternalError("User not found", new String [] { "User not found." },
                        HttpStatus.SC_NOT_FOUND);
            }

            UserBL userBL = new UserBL(userId);
            if(!userBL.matchPasswordForUser(user, currentPassword)) {
                logger.error("Current passsword {} did not match with user {} password for entity {}", currentPassword, userId, entityId);
                throw new SessionInternalError("Current password does not match with users existing password.",
                        new String[] {"Invalid credential - the current password does not match."}, HttpStatus.SC_NOT_FOUND);
            }

            boolean passwordUpdated = userBL.updatePassword(user.getUserId(), entityId, newPassword);
            if (passwordUpdated) {
                logger.info("Password updated successfully for user: {}", userId);
            } else {
                logger.error("User {}, is failed to update the password", userId);
                throw new SessionInternalError("Password not updated", new String [] { "Failed to update the user's password." },
                        HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            logger.error("error in updatePassword ", ex);
            throw new SessionInternalError("Error in updatePassword", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodTypeWS[] getAllPaymentMethodTypes() throws SessionInternalError {
        PaymentMethodTypeBL bl = new PaymentMethodTypeBL();
        List<PaymentMethodTypeDTO> methodTypeDTOs = bl.getAllPaymentMethodTypes(getCallerCompanyId());
        if (null == methodTypeDTOs || methodTypeDTOs.isEmpty()) {
            return new PaymentMethodTypeWS[0];
        }

        List<PaymentMethodTypeWS> PaymentMethodTypeWSs = new ArrayList<>();
        methodTypeDTOs.stream()
        .sorted(Comparator.comparingInt((PaymentMethodTypeDTO :: getId)).reversed())
        .forEach( e -> {
            PaymentMethodTypeWSs.add(new PaymentMethodTypeBL(e.getId()).getWS());
        });
        return PaymentMethodTypeWSs.toArray((new PaymentMethodTypeWS[PaymentMethodTypeWSs.size()]));
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationEventsForUserDateRange(Integer userId, Date startDate, Date endDate, int offset, int limit) {
        if(null == userId) {
            throw new SessionInternalError("Please provide user id parameter", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            UserDTO user = userDAS.findNow(userId);
            if(null == user) {
                throw new SessionInternalError("user id not found for entity " + getCallerCompanyId(), new String [] { "Please enter valid user id." },
                        HttpStatus.SC_NOT_FOUND);
            }
            List<Filter> filters = new ArrayList<>();
            filters.add(Filter.integer("userId", FilterConstraint.EQ, userId));
            if (startDate != null && endDate != null) {
                filters.add(Filter.betweenDates("eventDate", startDate, endDate));
            }
            return mediationService.findMediationRecordsByFilters(offset, limit, filters).toArray(new JbillingMediationRecord[0]);
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            logger.error("error in getMediationEventsForUserDateRange ", ex);
            throw new SessionInternalError("Error in getMediationEventsForUserDateRange", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileWS getUserProfile(Integer userId) {
        if(null == userId) {
            throw new SessionInternalError("validation failed", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(!userExistsWithId(userId)) {
            logger.error("User {} not found for entity {}", userId, getCallerCompanyId());
            throw new SessionInternalError("validation failed",
                    new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
        }
        try {
            return UserBL.getUserProfile(userId);
        } catch (Exception ex) {
            logger.error("error in getUserProfile ", ex);
            throw new SessionInternalError("Error in getUserProfile", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssetRestWS[] getAllAssetsForUser(Integer userId) {
        if(null == userId) {
            throw new SessionInternalError("validation failed", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(!userExistsWithId(userId)) {
            logger.error("User {} not found for entity {}", userId, getCallerCompanyId());
            throw new SessionInternalError("validation failed",
                    new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
        }

        try {
            return AssetBL.getAssetListByUserId(assetDAS.findAssetsByUser(userId));
        } catch (Exception ex) {
            logger.error("error in getAssetListByUserId ", ex);
            throw new SessionInternalError("Error in getAssetListByUserId", ex);
        }
    }

    @Override
    public Integer createOrderWithAssets(OrderWS order, OrderChangeWS[] orderChanges, AssetWS[] assets){
        if(null == order){
            throw new SessionInternalError("validation failed", new String [] { "Order should not be null." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        if(null != assets){
            Map<Integer, List<Integer>> itemAssetMap = new HashMap<>();
            for (AssetWS assetWS : assets) {
                AssetWS asset = null;
                try {
                    asset = getAssetByIdentifier(assetWS.getIdentifier());
                } catch (Exception e) {
                    asset = null;
                    logger.debug("Asset not found with identifier: {}", assetWS.getIdentifier());
                }
                Integer assetId = (null != asset ? asset.getId() : createAsset(assetWS));
                List<Integer> assetList = itemAssetMap.get(assetWS.getItemId());
                if(CollectionUtils.isEmpty(assetList)){
                    assetList = new ArrayList<>();
                }
                assetList.add(assetId);
                itemAssetMap.put(assetWS.getItemId(), assetList);
            }
            List<OrderLineWS> newLines = new ArrayList<>();
            //Link asset to line
            for (OrderLineWS line : order.getOrderLines()){
                if(itemAssetMap.containsKey(line.getItemId())){
                    line.setAssetIds(itemAssetMap.get(line.getItemId()).toArray(new Integer[0]));
                    line.setQuantity(itemAssetMap.get(line.getItemId()).size());
                }
                newLines.add(line);
            }
            order.setOrderLines(newLines.toArray(new OrderLineWS[0]));

            List<OrderChangeWS> newOrderChangeWS = new ArrayList<>();
            for (OrderChangeWS orderchanges : orderChanges) {
                for (OrderChangePlanItemWS planItem : orderchanges.getOrderChangePlanItems()){
                    if (itemAssetMap.containsKey(planItem.getItemId())){
                        Integer bundledQuantity = planItem.getBundledQuantity();
                        List<Integer> assetList = new ArrayList<>();
                        for(int i=0;i<bundledQuantity;i++){
                            Integer assetId = itemAssetMap.get(planItem.getItemId()).get(i);
                            assetList.add(assetId);
                            itemAssetMap.get(planItem.getItemId()).remove(i);
                        }
                        int assetIdsArr[] = assetList.stream().mapToInt(Integer::intValue).toArray();
                        planItem.setAssetIds(assetIdsArr);
                    }
                }

            	if(itemAssetMap.containsKey(orderchanges.getItemId())){
                    orderchanges.setAssetIds(itemAssetMap.get(orderchanges.getItemId()).toArray(new Integer[0]));
                    orderchanges.setQuantity(new BigDecimal(itemAssetMap.get(orderchanges.getItemId()).size()));
                }
                newOrderChangeWS.add(orderchanges);
            }
            return createOrder(order, newOrderChangeWS.toArray(new OrderChangeWS[0]));
        }else{
            return createOrder(order, orderChanges);
        }
    }

    @Override
    public CustomerMetaFieldValueWS getCustomerMetaFields(Integer userId){
        if(!userExistsWithId(userId)) {
            logger.error("User {} not found for entity {}", userId, getCallerCompanyId());
            throw new SessionInternalError("validation failed",
                    new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
        }
        return CustomerBL.getCustomerMetaFieldValueWS(userId);
    }

    @Override
    public UserWS updateCustomerContactInfo(ContactInformationWS contactInformation)  {
        UserResourceHelperService userResourceHelperService = Context.getBean(UserResourceHelperService.class);
        return userResourceHelperService.updateAITMetaField(contactInformation);
    }

    @Override
    public SecurePaymentWS addPaymentInstrument(PaymentInformationWS instrument) {
        String validationErrMsgText = "Validation failed";
        if(instrument == null) {
            throw new SessionInternalError(validationErrMsgText,
                    new String [] {"Payment instrument cannot be null"}, HttpStatus.SC_BAD_REQUEST);
        }
        if(instrument.getUserId() == null) {
            throw new SessionInternalError(validationErrMsgText,
                    new String [] {"User id cannot be null"}, HttpStatus.SC_BAD_REQUEST);
        }

        Integer entityId = getCallerCompanyId();
        logger.debug("Validating user {} for adding payment instrument.", instrument.getUserId());
        UserDTO user = userDAS.findByUserId(instrument.getUserId(), entityId);
        if (null == user){
            throw new SessionInternalError(validationErrMsgText,
                    new String [] {"User not found"}, HttpStatus.SC_NOT_FOUND);
        }

        logger.debug("Validating payment method type id {} for entity {}.", instrument.getPaymentMethodTypeId(), entityId);
        PaymentMethodTypeDTO pmtDas =
                new PaymentMethodTypeDAS().findByPaymentMethodTypeId(entityId, instrument.getPaymentMethodTypeId());
        if(pmtDas == null) {
            throw new SessionInternalError(validationErrMsgText,
                    new String [] {"Payment method type not found"}, HttpStatus.SC_NOT_FOUND);
        }

        try {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(Arrays.asList(instrument));
            UserBL userBl = new UserBL(user);

            SecurePaymentWS securePaymentWS = perform3DSecurityCheck( user, instrument, null);

            if(securePaymentWS != null && !securePaymentWS.isSucceeded()){
            	return securePaymentWS;
            }else if(securePaymentWS == null){
            	securePaymentWS = new SecurePaymentWS(user.getId(), 0, false, null, "", null);
            }

            Integer paymentInstrumentId = userBl.addPaymentInstrument(instrument);
            securePaymentWS.setBillingHubRefId(paymentInstrumentId);
        	securePaymentWS.setStatus("succeeded");

            return securePaymentWS;
        } catch(Exception ex) {
            logger.error("Error occurred in addPaymentInstrument method", ex);
            throw new SessionInternalError("Error in addPaymentInstrument", ex);
        }
    }

    @Override
    public PaymentInformationWS[] getPaymentInstruments(Integer userId){
        UserWS userWS = getUserWS(userId);
        List<PaymentInformationWS> informationWSs= userWS.getPaymentInstruments();
        if (CollectionUtils.isEmpty(informationWSs)){
            throw new SessionInternalError("validation failed",
                    new String [] {"Payment instrument not found with given user Id"}, HttpStatus.SC_NOT_FOUND);
        }

        return informationWSs.toArray(new PaymentInformationWS[informationWSs.size()]);
    }

    @Override
    public void updateCustomerMetaFields(Integer userId, MetaFieldValueWS[] customerMetaFieldValues) {
        if(null == userId) {
            logger.error("user is paramter is null");
            throw new SessionInternalError("user id is null", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        if(ArrayUtils.isEmpty(customerMetaFieldValues)) {
            logger.error("MetaFieldValueWS parameter is null or empty");
            throw new SessionInternalError("customerMetaFieldValues is null or empty", new String [] { "Please enter customer meta fields values." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!userExistsWithId(userId)) {
                logger.error("User {} not found for entity {}", userId, getCallerCompanyId());
                throw new SessionInternalError("validation failed",
                        new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
            }
            CustomerBL customerBL = new CustomerBL();
            customerBL.updateCustomerMetaFields(userId, customerMetaFieldValues);
        } catch(SessionInternalError ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in updateCustomerMetaFields"}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateOrderMetaFields(Integer orderId, MetaFieldValueWS[] orderMetaFieldValues) {
        if(null == orderId) {
            logger.error("order parameter is null");
            throw new SessionInternalError("order id is null", new String [] { "Please enter orderId." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        if(ArrayUtils.isEmpty(orderMetaFieldValues)) {
            logger.error("MetaFieldValueWS parameter is null or empty");
            throw new SessionInternalError("orderMetaFieldValues is null or empty", new String [] { "Please enter order meta fields values." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!orderDAS.isIdPersisted(orderId)) {
                logger.error("Order {} not found for entity {}", orderId, getCallerCompanyId());
                throw new SessionInternalError("validation failed",
                        new String [] { "Please enter a valid order id." }, HttpStatus.SC_NOT_FOUND);
            }
            OrderBL orderBL = new OrderBL(orderId);
            orderBL.updateOrderMetaFields(orderId, orderMetaFieldValues);
        } catch(SessionInternalError ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in updateOrderMetaFields"}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public OrderMetaFieldValueWS getOrderMetaFieldValueWS(Integer orderId) {
        if(null == orderId) {
            logger.error("order parameter is null");
            throw new SessionInternalError("order id is null", new String [] { "Please enter orderId." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!orderDAS.isIdPersisted(orderId)) {
                logger.error("Order {} not found for entity {}", orderId, getCallerCompanyId());
                throw new SessionInternalError("validation failed",
                        new String [] { "Please enter a valid order id." }, HttpStatus.SC_NOT_FOUND);
            }
            OrderBL orderBL = new OrderBL();
            return orderBL.getOrderMetaFieldValueWS(orderId);
        } catch(SessionInternalError ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in getOrderMetaFieldValueWS"}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateAssetMetaFields(Integer assetId, MetaFieldValueWS[] assetMetaFieldValues) {
        if(null == assetId) {
            logger.error("assetId parameter is null");
            throw new SessionInternalError("assetId id is null", new String [] { "Please enter assetId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(ArrayUtils.isEmpty(assetMetaFieldValues)) {
            logger.error("MetaFieldValueWS parameter is null or empty");
            throw new SessionInternalError("assetMetaFieldValues is null or empty", new String [] { "Please enter asset meta fields values." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        try {
            if(!assetDAS.isIdPersisted(assetId)) {
                logger.error("Asset {} not found for entity {}", assetId, getCallerCompanyId());
                throw new SessionInternalError("validation failed",
                        new String [] { "Please enter a valid Asset id." }, HttpStatus.SC_NOT_FOUND);
            }
            AssetBL assetBL = new AssetBL();
            assetBL.updateAssetMetaFields(assetId, assetMetaFieldValues);
        } catch(SessionInternalError ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in updateAssetMetaFields"}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getUnBilledMediationEventsByUser(Integer userId, int offset, int limit) {
        if(null == userId) {
            throw new SessionInternalError("Please provide user id parameter",
                    new String [] { "Please enter userId." }, HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!userExistsWithId(userId)) {
                throw new SessionInternalError("user id not found for entity " + getCallerCompanyId(),
                        new String [] { "Please enter valid user id." }, HttpStatus.SC_NOT_FOUND);
            }
            return mediationService.getUnBilledMediationEventsByUser(userId, offset, limit)
                    .toArray(new JbillingMediationRecord[0]);
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError("Error in getUnBilledMediationEventsByUser", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWS[] getUsersAllSubscriptions(Integer userId) {
        if (userId == null) {
            throw new SessionInternalError("User Id cannot be null.");
        }

        List<OrderDTO> subscriptions = orderDAS
                .findUsersAllSubscriptions(userId);
        if (null == subscriptions) {
            return new OrderWS[0];
        }
        OrderWS[] orderArr = new OrderWS[subscriptions.size()];
        OrderBL bl = null;
        for (OrderDTO dto : subscriptions) {
            bl = new OrderBL(dto);
            orderArr[subscriptions.indexOf(dto)] = bl
                    .getWS(getCallerLanguageId());
        }
        return orderArr;
    }

    @Override
    public CreditNoteWS[] getCreditNotesByUser(Integer userId, Integer offset, Integer limit) {
        if(null == userId) {
            logger.error("User {}, is null or empty", userId);
            throw new SessionInternalError("Please provide user id parameter", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(null == offset || offset < 0) {
            logger.error("offset {}, is null or negative", offset);
            throw new SessionInternalError("Please provide offset parameter", new String [] { "Please enter offset." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(null == limit || limit < 0) {
            logger.error("limit {}, is null or negative", limit);
            throw new SessionInternalError("Please provide limit parameter", new String [] { "Please enter limit." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!userExistsWithId(userId)) {
                logger.error("User {}, not found for entity {}", userId, getCallerCompanyId());
                throw new SessionInternalError("user id not found for entity " + getCallerCompanyId(), new String [] { "Please enter valid user id." },
                        HttpStatus.SC_NOT_FOUND);
            }
            return new CreditNoteBL().findCreditNotesByUser(userId, offset, limit);
        } catch(SessionInternalError error) {
            logger.error("error", error);
            throw error;
        } catch(Exception ex) {
            logger.error("error in getCreditNotesByUser ", ex);
            throw new SessionInternalError("Error in getCreditNotesByUser", ex);
        }
    }

    @Override
    public PaymentWS[] findPaymentsForUser(Integer userId, int offset, int limit) {
        if(null == userId) {
            throw new SessionInternalError("Please provide user id parameter",
                    new String [] { "Please enter userId." }, HttpStatus.SC_BAD_REQUEST);
        }
        if(offset < 0 ) {
            throw new SessionInternalError("invalid offset value passed",
                    new String [] { "Please enter positive offset value." }, HttpStatus.SC_BAD_REQUEST);
        }
        if(limit < 0 ) {
            throw new SessionInternalError("invalid limit value passed",
                    new String [] { "Please enter positive limit value." }, HttpStatus.SC_BAD_REQUEST);
        }
        try {
            if(!userExistsWithId(userId)) {
                throw new SessionInternalError("user id not found for entity " + getCallerCompanyId(),
                        new String [] { "Please enter a valid user id." }, HttpStatus.SC_NOT_FOUND);
            }
            PaymentBL paymentBL = new PaymentBL();
            return paymentBL.findPaymentsByUserPagedSortedByAttribute(userId, limit, offset, "id", ListField.Order.DESC, getCallerLanguageId())
                    .toArray(new PaymentWS[0]);
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError("Error in findPaymentsForUser", ex);
        }
    }

    /* This method supports Strong Customer Authentication (SCA) where customer need to be authenticated at different level like
     * 1. Attaching a card with customer profile that can be used in future for recursive payment.
     * 2. One time payment, Card is eanbled for SCA and need to be authenticated
     *
	*/
    private SecurePaymentWS perform3DSecurityCheck(UserDTO userDTO ,PaymentInformationWS instrument, PaymentDTOEx paymentDTOEx) throws PluggableTaskException{
		SecurePaymentWS securePaymentWS = null;
		PluggableTaskManager taskManager = new PluggableTaskManager(getCallerCompanyId() , Constants.PLUGGABLE_TASK_PAYMENT);
        PaymentTask task = (PaymentTask) taskManager.getNextClass();

         if (task == null) {
             String errMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                     .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                     .action(LogConstants.ACTION_EVENT.toString())
                     .message("No payment pluggable tasks configured").build().toString();
             logger.warn(errMsg);
             return null;
         }


         while (task != null){
        	 if(task instanceof ISecurePayment){
        		 ISecurePayment securePayment = (ISecurePayment)task;
        		 if( instrument != null){
        			 PaymentInformationDTO piDto =  new PaymentInformationDTO(instrument, userDTO.getEntity().getId());
            		 piDto.setUser(userDTO);
            		 securePaymentWS = securePayment.perform3DSecurityCheck(piDto, null);
            		 // Setting metafield gateway key
            		 instrument.setMetaFields(PaymentInformationBL.getWS(piDto).getMetaFields());
        		 }else if(paymentDTOEx!=null){
        			 securePaymentWS = securePayment.perform3DSecurityCheck(null, paymentDTOEx);
        		 }
        	 }

        	 if(StripeHelper.isObjectEmpty(securePaymentWS)){
        		 task = (PaymentTask) taskManager.getNextClass();
        	 }else{
        		 task = null;
        	 }
         }

         return securePaymentWS;
	}

    public Integer[] createCustomInvoice(File csvFile) {
        Set<Integer> invoiceIds = new HashSet<>();
        List<Integer> orderIds = new ArrayList<>();
        Integer ORDER_CHANGE_STATUS_APPLY_ID = 3;
        OrderService orderService = Context.getBean(OrderService.BEAN_NAME);
        Map<Integer, List<OrderWS>> customerOrderList = orderService.getOrderForCustomInvoice(csvFile);
        try {
            for( Map.Entry<Integer, List<OrderWS>> entry : customerOrderList.entrySet() ) {
                InvoiceDTO invoice = null;
                List<OrderWS> orderList = entry.getValue();
                for( OrderWS order : orderList ) {
                    Integer invoiceId = null;
                    Integer orderId = 0;
                    OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
                    orderId = doCreateOrder(order, orderChanges, true).getId();
                    orderIds.add(orderId);
                    if( null == invoice ) {
                        invoice = doCreateInvoice(orderId);
                        invoiceId = null != invoice ? invoice.getId() : null;
                    } else {
                        invoiceId = applyOrderToInvoice(orderId, InvoiceBL.getWS(invoice));
                    }
                    invoiceIds.add(invoiceId);
                }
            }
        } catch (Exception e) {
            invoiceIds.forEach(this::deleteInvoice);
            orderIds.forEach(this::deleteOrder);
            throw new SessionInternalError(e, new String[] {e.getMessage()}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return invoiceIds.toArray(new Integer[invoiceIds.size()]);
    }

    public PaymentUrlLogDTO createPaymentUrl(Map<String, Object> map) {
        if (null == map) {
            logger.error("Provided input is null");
            return null;
        }
        String totalAmount = (String) map.getOrDefault("totalAmount", "0.00");
        Integer entityId = (Integer) map.getOrDefault("entityId", getCallerCompanyId());
        Integer invoiceId = (Integer) map.getOrDefault("invoiceId", null);
        List<Status> FAILED_STATUSES = Arrays.asList(Status.FAILED, Status.EXPIRED, Status.CANCELLED, Status.TIMEOUT);
        if (null != invoiceId) {
            List<PaymentUrlLogDTO> allByInvoiceId = new PaymentUrlLogDAS().findAllByInvoiceId(invoiceId);
            int numberOfPaymentUrls = allByInvoiceId.size();
            if (numberOfPaymentUrls >= 1) {
                PaymentUrlLogDTO previousPaymentUrlLogDTO = allByInvoiceId.get(numberOfPaymentUrls - 1);
                if (!FAILED_STATUSES.contains(previousPaymentUrlLogDTO.getStatus())) {
                    return null;
                }
            }
        }
        String webhookResponse = (String) map.getOrDefault("webhookResponse", null);
        String gatewayId = (String) map.getOrDefault("gatewayId", entityId + "-" + System.currentTimeMillis());
        String paymentUrlType = (String) map.getOrDefault("paymentUrlType", "link");
        PaymentUrlLogDTO paymentUrlLogDTO = new PaymentUrlLogDTO();
        paymentUrlLogDTO.setMobileRequestPayload(new Gson().toJson(map));
        paymentUrlLogDTO.setStatus(Status.INITIATED);
        paymentUrlLogDTO.setEntityId(entityId);
        paymentUrlLogDTO.setInvoiceId(invoiceId);
        paymentUrlLogDTO.setWebhookResponse(webhookResponse);
        paymentUrlLogDTO.setCreatedAt(new Date());
        paymentUrlLogDTO.setPaymentAmount(new BigDecimal(totalAmount));
        paymentUrlLogDTO.setGatewayId(gatewayId);
        paymentUrlLogDTO.setPaymentUrlType(PaymentUrlType.valueOf(paymentUrlType.toUpperCase()));
        PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
        PaymentUrlLogDTO dto = paymentUrlLogDAS.save(paymentUrlLogDTO);
        if (null != dto) {
            logger.info("Payment URL Log DTO object created successfully with id {}", dto.getId());
            if (StringUtils.isBlank(webhookResponse)) { // create payment url initiated event only when created from dynamic scenarios
                if(null != invoiceId) {
                    PaymentUrlRegenerateEvent event = new PaymentUrlRegenerateEvent(invoiceId, entityId);
                    EventManager.process(event);
                } else {
                    PaymentUrlInitiatedEvent event = new PaymentUrlInitiatedEvent(dto.getId(), dto.getEntityId());
                    EventManager.process(event);
                }
            }
        }
        return dto;
    }

    public String generateGstR1JSONFileReport(String startDate, String endDate) throws Exception {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate startDateAsLocalDate = LocalDate.parse(startDate, formatter);
        LocalDate endDateAsLocalDate = LocalDate.parse(endDate, formatter);

        // Calculate the difference in days between the start and end dates
        long daysDifference = ChronoUnit.DAYS.between(startDateAsLocalDate, endDateAsLocalDate);
        if (daysDifference > 89) { // count starts from 0(ZERO),  [So the count of 0-89 = 90]
            throw new SessionInternalError(new String[]{"Maximum Period Exceeded.The maximum period for generating GSTR-1 is 90 days." +
                    "Please choose a date range within the allowed 90 days period.You cannot select dates beyond this limit."});
        }

        Date startDateAsDate = Date.from(startDateAsLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateAsDate = Date.from(endDateAsLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<InvoiceWS> invoiceWSList = new ArrayList<>();
        List<OrderWS> orderWSList = new ArrayList<>();
        InvoiceDAS invoiceDAS = new InvoiceDAS();
        Integer companyId = getCallerCompanyId();

        List<Integer> invoiceIdList = invoiceDAS.getInvoiceIdBetweenTowDates(startDateAsDate, endDateAsDate, companyId);
        if (invoiceIdList.isEmpty()) {
            throw new SessionInternalError(new String[]{"Invoice not found between these period of time."});
        }

        for (Integer invoiceId : invoiceIdList) {
            invoiceWSList.add(getInvoiceWS(invoiceId));
            orderWSList.add(getOrder(invoiceDAS.getFirstOrderIdByInvoiceId(invoiceId)));
        }
        return new GSTR1JSONMapper().getGSTR1Json(invoiceWSList, orderWSList);
    }
    private void handleUserExistsError(String username) {
        String msg = "User already exists with username " + username;
        String log = UserBL.getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
        logger.error(log);
        throw new SessionInternalError(msg, new String[] { "UserWS,userName,validation.error.user.already.exists" }, HttpStatus.SC_BAD_REQUEST);
    }

    public boolean notifyPaymentLinkByEmail(Integer invoiceId) {
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        boolean emailLink;
        try {
            emailLink = notificationSession.sendPaymentLinkToCustomer(invoiceId);
        } catch (SessionInternalError sie) {
            throw sie;
        } catch (Exception e) {
            logger.error("Exception in web service: notifying payment link by email", e);
            emailLink = false;
        }
        return emailLink;
    }

    public Optional<Object> executePaymentTask(Integer paymentLogUrlLogId, String payerVPA,
                                               String action) throws PluggableTaskException {

        PluggableTaskManager<IInternalEventsTask> pluggableTaskManager = new PluggableTaskManager<>(getCallerCompanyId(), Constants.PLUGGABLE_TASK_INTERNAL_EVENT);
        Optional<IInternalEventsTask> taskOptional = pluggableTaskManager.getAllTasks()
                .stream()
                .map(pluggableTaskDTO -> {
                    IInternalEventsTask task = null;
                    try {
                        task = pluggableTaskManager.getInstance(pluggableTaskDTO.getType().getClassName(),
                                pluggableTaskDTO.getType().getCategory().getInterfaceName(),
                                pluggableTaskDTO);
                    } catch (PluggableTaskException e) {
                        throw new RuntimeException(e);
                    }
                    return task;
                })
                .filter(myClass -> myClass instanceof GeneratePaymentURLTask)
                .findFirst();

        if( taskOptional.isPresent() ) {
            GeneratePaymentURLTask generatePaymentURLTask = (GeneratePaymentURLTask) taskOptional.get();

            // Call specific method based on return type
            if( action.equals("checkPaymentUrlStatus") ) {
                return Optional.ofNullable(generatePaymentURLTask.checkPaymentStatus(paymentLogUrlLogId));
            } else if( action.equals("verifyPayerVPA") ) {
                return Optional.ofNullable(generatePaymentURLTask.getVerificationData(payerVPA));
            } else if( action.equals("cancelPaymentUrl") ) {
                return Optional.ofNullable(generatePaymentURLTask.cancelPaymentUrl(paymentLogUrlLogId));
            } else {
                throw new IllegalArgumentException("Unsupported action type: " + action);
            }
        }
        return Optional.empty();
    }
}
