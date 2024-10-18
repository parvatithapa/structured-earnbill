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

package com.sapienter.jbilling.server.util;


import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

import javax.jws.WebService;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.resources.OrderMetaFieldValueWS;
import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS;
import com.sapienter.jbilling.server.company.CompanyInformationTypeWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.diameter.DiameterResultWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.ediFile.ldc.OrphanEDIFile;
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.PagedResultList;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoiceSummary.CreditAdjustmentWS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryWS;
import com.sapienter.jbilling.server.invoiceSummary.ItemizedAccountWS;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.mediation.RecordCountWS;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentTransferWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.DataTableQueryWS;
import com.sapienter.jbilling.server.pricing.RateCardWS;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.process.signup.SignupRequestWS;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestWS;
import com.sapienter.jbilling.server.security.Validator;
import com.sapienter.jbilling.server.sql.api.QueryResultWS;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.SwapPlanHistoryWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.UsageRatingSchemeType;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS;
import com.sapienter.jbilling.server.user.partner.CommissionWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;


/**
 * Web service bean interface.
 * {@see com.sapienter.jbilling.server.util.WebServicesSessionSpringBean} for documentation.
 */
@WebService
public interface IWebServicesSessionBean {

    public Integer getCallerId();
    public Integer getCallerCompanyId();
    public Integer getCallerLanguageId();
    public Integer getCallerCurrencyId();

    /*
        Users
     */
    public UserWS getUserWS(Integer userId) ;
    public Integer createUser(UserWS newUser) ;
    public Integer createUserWithCompanyId(UserWS newUser, Integer entityId) ;
    public Integer createUserForAppDirect(UserWS newUser, Integer entityId, boolean isCreator) ;
    public void updateUser(UserWS user) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateUserWithCompanyId(UserWS user, Integer entityId) ;
    @Validator(type = Validator.Type.EDIT)
    public void deleteUser(Integer userId) ;
    public void deleteAppDirectUser(Integer executorId, Integer userId) ;
    public void initiateTermination(Integer userId, String reasonCode, Date terminationDate) ;

    public boolean userExistsWithName(String userName);
    public boolean userExistsWithId(Integer userId);

    public ContactWS[] getUserContactsWS(Integer userId) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateUserContact(Integer userId, ContactWS contact) ;

    @Validator(type = Validator.Type.EDIT)
    public void setAuthPaymentType(Integer userId, Integer autoPaymentType, boolean use) ;
    public Integer getAuthPaymentType(Integer userId) ;

    public Integer[] getUsersByStatus(Integer statusId, boolean in) ;
    public Integer[] getUsersInStatus(Integer statusId) ;
    public Integer[] getUsersNotInStatus(Integer statusId) ;

    public Integer getUserId(String username) ;
    public Integer getUserIdByEmail(String email) ;
    public UserWS getUserBySupplierID(String supplierId) ;

    public UserTransitionResponseWS[] getUserTransitions(Date from, Date to) ;
    public UserTransitionResponseWS[] getUserTransitionsAfterId(Integer id) ;

    @Validator(type = Validator.Type.EDIT)
    public CreateResponseWS create(UserWS user, OrderWS order, OrderChangeWS[] orderChanges) ;

    @Validator(type = Validator.Type.EDIT)
    public Integer createUserCode(UserCodeWS userCode) ;
    public UserCodeWS[] getUserCodesForUser(Integer userId) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateUserCode(UserCodeWS userCode) ;
    public Integer[] getCustomersByUserCode(String userCode) ;
    public Integer[] getOrdersByUserCode(String userCode) ;
    public Integer[] getOrdersLinkedToUser(Integer userId) ;
    public Integer[] getCustomersLinkedToUser(Integer userId) ;
    @Validator(type = Validator.Type.NONE)
    public void resetPassword(int userId) ;
    @Validator(type = Validator.Type.NONE)
    public void resetPasswordByUserName(String userName);
    public UserWS getUserByCustomerMetaField(String metaFieldValue, String metaFieldName);
    public UserWS getUserByCustomerMetaFieldAndCompanyId(String metaFieldValue, String metaFieldName, Integer callerCompanyId);

    /*
        Partners
     */

    public PartnerWS getPartner(Integer partnerId) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer createPartner(UserWS newUser, PartnerWS partner) ;
    @Validator(type = Validator.Type.EDIT)
    public void updatePartner(UserWS newUser, PartnerWS partner) ;
    @Validator(type = Validator.Type.EDIT)
    public void deletePartner (Integer partnerId) ;


    /*
        Items
     */

    // categories parentness
    public ItemTypeWS[] getItemCategoriesByPartner(String partner, boolean parentCategoriesOnly);
    public ItemTypeWS[] getChildItemCategories(Integer itemTypeId);

    public ItemDTOEx getItem(Integer itemId, Integer userId, String pricing);
    public ItemDTOEx[] getAllItems() ;
    public Integer createItem(ItemDTOEx item) ;
    public void updateItem(ItemDTOEx item);
    @Validator(type = Validator.Type.EDIT)
    public void deleteItem(Integer itemId);

    public ItemDTOEx[] getAddonItems(Integer itemId);

    public ItemDTOEx[] getItemByCategory(Integer itemTypeId);
    public Integer[] getUserItemsByCategory(Integer userId, Integer categoryId);

    public ItemTypeWS getItemCategoryById(Integer id);
    public ItemTypeWS[] getAllItemCategories();
    public Integer createItemCategory(ItemTypeWS itemType) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateItemCategory(ItemTypeWS itemType) ;
    @Validator(type = Validator.Type.EDIT)
    public void deleteItemCategory(Integer itemCategoryId);

    public ItemTypeWS[] getAllItemCategoriesByEntityId(Integer entityId);
    public ItemDTOEx[] getAllItemsByEntityId(Integer entityId);

    public String isUserSubscribedTo(Integer userId, Integer itemId);

    public InvoiceWS getLatestInvoiceByItemType(Integer userId, Integer itemTypeId) ;
    public Integer[] getLastInvoicesByItemType(Integer userId, Integer itemTypeId, Integer number) ;

    public OrderWS getLatestOrderByItemType(Integer userId, Integer itemTypeId) ;
    public Integer[] getLastOrdersByItemType(Integer userId, Integer itemTypeId, Integer number) ;

    public ValidatePurchaseWS validatePurchase(Integer userId, Integer itemId, String fields);
    public ValidatePurchaseWS validateMultiPurchase(Integer userId, Integer[] itemId, String[] fields);
    public Integer getItemID(String productCode) ;


    /*
        Orders
     */

    public OrderWS getOrder(Integer orderId) ;
    public Integer createOrder(OrderWS order, OrderChangeWS[] orderChanges) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateOrder(OrderWS order, OrderChangeWS[] orderChanges) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer createUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer copyCreateUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges, Integer targetCompanyId,
            Integer targetCompanyLanguageId, Integer newUserId);
    @Validator(type = Validator.Type.EDIT)
    public String deleteOrder(Integer id) ;

    public Integer createOrderAndInvoice(OrderWS order, OrderChangeWS[] orderChanges) ;

    public OrderWS getCurrentOrder(Integer userId, Date date) ;
    @Validator(type = Validator.Type.EDIT)
    public OrderWS updateCurrentOrder(Integer userId, OrderLineWS[] lines, String pricing, Date date, String eventDescription) ;

    public OrderWS[] getUserSubscriptions(Integer userId) ;

    public OrderLineWS getOrderLine(Integer orderLineId) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateOrderLine(OrderLineWS line) ;

    public void upgradePlanOrder(Integer orderId, Integer orderToUpgradeId, Integer paymentId);

    public Integer[] getOrderByPeriod(Integer userId, Integer periodId) ;
    public OrderWS getLatestOrder(Integer userId) ;
    public Integer[] getLastOrders(Integer userId, Integer number) ;
    public Integer[] getOrdersByDate (Integer userId, Date since, Date until);
    public OrderWS[] getUserOrdersPage(Integer user, Integer limit, Integer offset) ;

    public Integer[] getLastOrdersPage(Integer userId, Integer limit, Integer offset) ;

    public OrderWS rateOrder(OrderWS order, OrderChangeWS[] orderChanges) ;
    public OrderWS[] rateOrders(OrderWS[] orders, OrderChangeWS[] orderChanges) ;

    public Map<String, BigDecimal> calculateUpgradePlan(Integer orderId, Integer planId, String discountCode);
    public OrderChangeWS[] calculateSwapPlanChanges(OrderWS order,  Integer existingPlanItemId, Integer swapPlanItemId, SwapMethod method, Date effectiveDate);
    public boolean swapPlan(Integer orderId, String existingPlanCode, String swapPlanCode, SwapMethod swapMethod);
    public void swapAssets(Integer orderId, SwapAssetWS[] swapRequests);

    public boolean updateOrderPeriods(OrderPeriodWS[] orderPeriods) ;
    public boolean updateOrCreateOrderPeriod(OrderPeriodWS orderPeriod) ;
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteOrderPeriod(Integer periodId) ;

    public PaymentAuthorizationDTOEx createOrderPreAuthorize(OrderWS order, OrderChangeWS[] orderChanges) ;

    public OrderPeriodWS[] getOrderPeriods() ;

    public OrderPeriodWS getOrderPeriodWS(Integer orderPeriodId) ;

    @Validator(type = Validator.Type.EDIT)
    public void updateOrders(OrderWS[] orders, OrderChangeWS[] orderChanges) ;

    public OrderWS[] filterOrders(Integer page, Integer size, Date activeSince, Integer productId, Integer orderStatusId);

    /*
        Account Type
     */
    public Integer createAccountType(AccountTypeWS accountType) ;
    @Validator(type = Validator.Type.EDIT)
    public boolean updateAccountType(AccountTypeWS accountType);
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteAccountType(Integer accountTypeId) ;
    public AccountTypeWS getAccountType(Integer accountTypeId) ;
    public AccountTypeWS[] getAllAccountTypes() ;
    public AccountTypeWS[] getAllAccountTypesByCompanyId(Integer companyId);

    /*
        Company Information Types
     */
    public CompanyInformationTypeWS[] getInformationTypesForCompany(Integer companyId);
    public Integer createCompanyInformationType(CompanyInformationTypeWS companyInformationType);
    public Integer createCompanyInformationTypeWithEntityId(CompanyInformationTypeWS companyInformationType, Integer entityId);
    @Validator(type = Validator.Type.EDIT)
    public void updateCompanyInformationType(CompanyInformationTypeWS companyInformationType);
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteCompanyInformationType(Integer companyInformationTypeId);
    public CompanyInformationTypeWS getCompanyInformationType(Integer companyInformationType);

    /*
        Account Information Types
     */
    public AccountInformationTypeWS[] getInformationTypesForAccountType(Integer accountTypeId);
    public Integer createAccountInformationType(AccountInformationTypeWS accountInformationType);
    @Validator(type = Validator.Type.EDIT)
    public void updateAccountInformationType(AccountInformationTypeWS accountInformationType);
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteAccountInformationType(Integer accountInformationTypeId);
    public AccountInformationTypeWS getAccountInformationType(Integer accountInformationType);

    public OrderWS[] getLinkedOrders(Integer primaryOrderId) ;
    public Integer createOrderPeriod(OrderPeriodWS orderPeriod) ;

    /*
        Invoices
     */

    public InvoiceWS getInvoiceWS(Integer invoiceId) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer[] createInvoice(Integer userId, boolean onlyRecurring);
    @Validator(type = Validator.Type.EDIT)
    public Integer[] createInvoiceWithDate(Integer userId, Date billingDate, Integer dueDatePeriodId, Integer dueDatePeriodValue, boolean onlyRecurring);
    @Validator(type = Validator.Type.EDIT)
    public Integer createInvoiceFromOrder(Integer orderId, Integer invoiceId) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer applyOrderToInvoice(Integer orderId, InvoiceWS invoiceWs);
    @Validator(type = Validator.Type.EDIT)
    public void deleteInvoice(Integer invoiceId);
    @Validator(type = Validator.Type.EDIT)
    public Integer saveLegacyInvoice(InvoiceWS invoiceWS);
    @Validator(type = Validator.Type.EDIT)
    public Integer saveLegacyPayment(PaymentWS paymentWS);
    @Validator(type = Validator.Type.EDIT)
    public Integer saveLegacyOrder(OrderWS orderWS);

    public InvoiceWS[] getAllInvoicesForUser(Integer userId);
    public Integer[] getAllInvoices(Integer userId);
    public InvoiceWS getLatestInvoice(Integer userId) ;
    public Integer[] getLastInvoices(Integer userId, Integer number) ;

    public Integer[] getInvoicesByDate(String since, String until) ;
    public Integer[] getUserInvoicesByDate(Integer userId, String since, String until) ;
    public Integer[] getUnpaidInvoices(Integer userId) ;
    public InvoiceWS[] getUserInvoicesPage(Integer userId, Integer limit, Integer offset) ;

    public byte[] getPaperInvoicePDF(Integer invoiceId) ;
    public boolean notifyInvoiceByEmail(Integer invoiceId);
    public boolean notifyPaymentByEmail(Integer paymentId);

    /*
        Payments
     */

    public PaymentWS getPayment(Integer paymentId) ;
    public PaymentWS getLatestPayment(Integer userId) ;
    public Integer[] getLastPayments(Integer userId, Integer number) ;

    public Integer[] getLastPaymentsPage(Integer userId, Integer limit, Integer offset) ;
    public Integer[] getPaymentsByDate(Integer userId, Date since, Date until) ;

    public BigDecimal getTotalRevenueByUser (Integer userId) ;

    public PaymentWS getUserPaymentInstrument(Integer userId) ;
    public PaymentWS[] getUserPaymentsPage(Integer userId, Integer limit, Integer offset) ;

    public Integer createPayment(PaymentWS payment);
    @Validator(type = Validator.Type.EDIT)
    public void updatePayment(PaymentWS payment);
    @Validator(type = Validator.Type.EDIT)
    public void deletePayment(Integer paymentId);

    public void removePaymentLink(Integer invoiceId, Integer paymentId) ;
    public void createPaymentLink(Integer invoiceId, Integer paymentId);
    @Validator(type = Validator.Type.EDIT)
    public void removeAllPaymentLinks(Integer paymentId) ;

    @Validator(type = Validator.Type.EDIT)
    public PaymentAuthorizationDTOEx payInvoice(Integer invoiceId) ;
    @Validator(type = Validator.Type.EDIT)
    public Integer applyPayment(PaymentWS payment, Integer invoiceId) ;
    public PaymentAuthorizationDTOEx processPayment(PaymentWS payment, Integer invoiceId);

    public PaymentAuthorizationDTOEx[] processPayments(PaymentWS[] payments, Integer invoiceId);

    public Integer[] createPayments(PaymentWS[] payment);

    /*
     * Payment Transfer
     */
    public void transferPayment(PaymentTransferWS paymentTransfer);

    /*
        Billing process
     */

    public boolean isBillingRunning(Integer entityId);
    public ProcessStatusWS getBillingProcessStatus();
    public void triggerBillingAsync(final Date runDate);
    public boolean triggerBilling(Date runDate);

    public void triggerAgeing(Date runDate);
    public void triggerCollectionsAsync (final Date runDate);
    public boolean isAgeingProcessRunning();
    public ProcessStatusWS getAgeingProcessStatus();

    public BillingProcessConfigurationWS getBillingProcessConfiguration() ;
    public Integer createUpdateBillingProcessConfiguration(BillingProcessConfigurationWS ws) ;

    public Integer createUpdateCommissionProcessConfiguration(CommissionProcessConfigurationWS ws) ;
    public void calculatePartnerCommissions() ;
    public void calculatePartnerCommissionsAsync() ;
    public boolean isPartnerCommissionRunning();
    public CommissionProcessRunWS[] getAllCommissionRuns() ;
    public CommissionWS[] getCommissionsByProcessRunId(Integer processRunId) ;

    public BillingProcessWS getBillingProcess(Integer processId);
    public Integer getLastBillingProcess() ;

    public OrderProcessWS[] getOrderProcesses(Integer orderId);
    public OrderProcessWS[] getOrderProcessesByInvoice(Integer invoiceId);

    public BillingProcessWS getReviewBillingProcess();
    public BillingProcessConfigurationWS setReviewApproval(Boolean flag) ;

    public Integer[] getBillingProcessGeneratedInvoices(Integer processId);

    public AgeingWS[] getAgeingConfiguration(Integer languageId);
    public void saveAgeingConfiguration(AgeingWS[] steps, Integer languageId);

    public AgeingWS[] getAgeingConfigurationWithCollectionType(Integer languageId, CollectionType collectionType);
    public void saveAgeingConfigurationWithCollectionType(AgeingWS[] steps, Integer languageId, CollectionType collectionType);

    /*
        Mediation process
     */

    public void triggerMediation();
    public UUID triggerMediationByConfiguration(Integer cfgId);
    public UUID launchMediation(Integer mediationCfgId, String jobName, File file);
    public void undoMediation(UUID processId) ;
    public boolean isMediationProcessRunning();
    public UUID triggerMediationByConfigurationByFile(Integer cfgId, File file);
    public ProcessStatusWS getMediationProcessStatus();

    public MediationProcess getMediationProcess(UUID mediationProcessId);
    public MediationProcess[] getAllMediationProcesses();
    public JbillingMediationRecord[] getMediationEventsForOrder(Integer orderId);
    public JbillingMediationRecord[] getMediationEventsForOrderDateRange(Integer orderId,Date startDate, Date endDate, int offset, int limit);
    public JbillingMediationRecord[] getMediationEventsForInvoice(Integer invoiceId);
    public JbillingMediationRecord[] getMediationRecordsByMediationProcess(UUID mediationProcessId, Integer page, Integer size, Date startDate, Date endDate);
    public RecordCountWS[] getNumberOfMediationRecordsByStatuses();
    public RecordCountWS[] getNumberOfMediationRecordsByStatusesByMediationProcess(UUID mediationProcess);
    public JbillingMediationRecord[] getMediationRecordsByMediationProcessAndStatus(String mediationProcessId, Integer statusId);
    public JbillingMediationRecord[] getMediationRecordsByStatusAndCdrType(UUID mediationProcessId, Integer page, Integer size, Date startDate, Date endDate, String status, String cdrType);

    public MediationConfigurationWS[] getAllMediationConfigurations();
    public Integer createMediationConfiguration(MediationConfigurationWS cfg);
    @Validator(type = Validator.Type.EDIT)
    public Integer[] updateAllMediationConfigurations(List<MediationConfigurationWS> configurations) ;
    @Validator(type = Validator.Type.EDIT)
    public void deleteMediationConfiguration(Integer cfgId);

    public OrderWS processJMRData(
            UUID processId, String recordKey, Integer userId,
            Integer currencyId, Date eventDate, String description,
            Integer productId, String quantity, String pricing);
    public JbillingMediationErrorRecord[] getMediationErrorRecordsByMediationProcess(UUID mediationProcessId, Integer mediationRecordStatusId);

    public JbillingMediationErrorRecord[] getErrorsByMediationProcess(String mediationProcessId, int offset, int limit);
    public OrderWS processJMRRecord(UUID processId, JbillingMediationRecord jmr);

    public UUID processCDR(Integer configId, List<String> callDataRecords);
    public UUID processCDRChecked(Integer configId, List<String> callDataRecords);

    /*
        Provisioning process
     */

    public void triggerProvisioning();

    @Validator(type = Validator.Type.EDIT)
    public void updateOrderAndLineProvisioningStatus(Integer inOrderId, Integer inLineId, String result);
    @Validator(type = Validator.Type.EDIT)
    public void updateLineProvisioningStatus(Integer orderLineId, Integer provisioningStatus);

    public ProvisioningCommandWS[] getProvisioningCommands(ProvisioningCommandType type, Integer id);
    public ProvisioningCommandWS getProvisioningCommandById(Integer provisioningCommandId);

    public ProvisioningRequestWS[] getProvisioningRequests(Integer provisioningCommandId);
    public ProvisioningRequestWS getProvisioningRequestById(Integer provisioningRequestId);

    /*
        Preferences
     */

    public void updatePreferences(PreferenceWS[] prefList);
    public void updatePreference(PreferenceWS preference);
    public PreferenceWS getPreference(Integer preferenceTypeId);


    /*
        Currencies
     */

    public CurrencyWS[] getCurrencies();
    public void updateCurrencies(CurrencyWS[] currencies);
    public void updateCurrency(CurrencyWS currency);
    public Integer createCurrency(CurrencyWS currency);
    public boolean deleteCurrency(Integer currencyId);

    public CompanyWS getCompany();
    public CompanyWS[] getCompanies();
    public void updateCompany(CompanyWS companyWS);
    public void updateCompanyWithEntityId(CompanyWS companyWS, Integer entityId, Integer userId);
    public CompanyWS getCompanyByEntityId(Integer entityId);
    public CompanyWS getCompanyByMetaFieldValue(String entityId);
    public MetaFieldWS[] getMetaFieldsByEntityId(Integer entityId, String entityType);

    /*
        Notifications
     */

    public void createUpdateNotification(Integer messageId, MessageDTO dto);


    /*
        Plug-ins
     */

    public PluggableTaskWS getPluginWS(Integer pluginId);
    public PluggableTaskWS[] getPluginsWS(Integer entityId, String className);
    public Integer createPlugin(PluggableTaskWS plugin);
    @Validator(type = Validator.Type.EDIT)
    public void updatePlugin(PluggableTaskWS plugin);
    @Validator(type = Validator.Type.EDIT)
    public void deletePlugin(Integer plugin);
    public PluggableTaskWS getPluginWSByTypeId(Integer typeId);

    /*
     * Quartz jobs
     */
    public void rescheduleScheduledPlugin(Integer pluginId);
    public void unscheduleScheduledPlugin(Integer pluginId);
    public void triggerScheduledTask(Integer pluginId, Date date);

    /*
        Plans and special pricing
     */

    public PlanWS getPlanWS(Integer planId);
    public PlanWS[] getAllPlans();
    public Integer createPlan(PlanWS plan);
    @Validator(type = Validator.Type.EDIT)
    public void updatePlan(PlanWS plan);
    @Validator(type = Validator.Type.EDIT)
    public void deletePlan(Integer planId);
    @Validator(type = Validator.Type.EDIT)
    public void addPlanPrice(Integer planId, PlanItemWS price);

    public boolean isCustomerSubscribed(Integer planId, Integer userId);
    public boolean isCustomerSubscribedForDate(Integer planId, Integer userId, Date eventDate);

    public Integer[] getSubscribedCustomers(Integer planId);
    public Integer[] getPlansBySubscriptionItem(Integer itemId);
    public Integer[] getPlansByAffectedItem(Integer itemId);

    public Usage getItemUsage(Integer excludedOrderId, Integer itemId, Integer owner, List<Integer> userIds , Date startDate, Date endDate);

    @Validator(type = Validator.Type.EDIT)
    public PlanItemWS createCustomerPrice(Integer userId, PlanItemWS planItem, Date expiryDate);
    public void updateCustomerPrice(Integer userId, PlanItemWS planItem, Date expiryDate);
    public void deleteCustomerPrice(Integer userId, Integer planItemId);

    public PlanItemWS[] getCustomerPrices(Integer userId);
    public PlanItemWS getCustomerPrice(Integer userId, Integer itemId);
    public PlanItemWS getCustomerPriceForDate(Integer userId, Integer itemId, Date pricingDate, Boolean planPricingOnly);

    public PlanItemWS createAccountTypePrice(Integer accountTypeId, PlanItemWS planItem, Date expiryDate);
    public void updateAccountTypePrice(Integer accountTypeId, PlanItemWS planItem, Date expiryDate);
    public void deleteAccountTypePrice(Integer accountTypeId, Integer planItemId);

    public PlanItemWS[] getAccountTypePrices(Integer accountTypeId);
    public PlanItemWS getAccountTypePrice(Integer accountTypeId, Integer itemId);


    public void createCustomerNote(CustomerNoteWS note);
    /*
     * Assets
     */

    public Integer createAsset(AssetWS asset)  ;
    @Validator(type = Validator.Type.EDIT)
    public void updateAsset(AssetWS asset)  ;
    public AssetWS getAsset(Integer assetId);
    public AssetWS getAssetByIdentifier(String assetIdentifier);
    @Validator(type = Validator.Type.EDIT)
    public void deleteAsset(Integer assetId)  ;
    public Integer[] getAssetsForCategory(Integer categoryId);
    public Integer[] getAssetsForItem(Integer itemId) ;
    public AssetTransitionDTOEx[] getAssetTransitions(Integer assetId);
    public Long startImportAssetJob(int itemId, String identifierColumnName, String notesColumnName, String globalColumnName, String entitiesColumnName, String sourceFilePath, String errorFilePath) ;
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria)  ;
    public AssetSearchResult findProductAssetsByStatus(int productId, SearchCriteria criteria)  ;
    public List<AssetWS> getAssetsByUserId(Integer userId);
    public AssetWS[] findAssetsByProductCode(String productCode)  ;
    public AssetStatusDTOEx[] findAssetStatuses(String identifier)  ;
    public AssetWS findAssetByProductCodeAndIdentifier(String productCode, String identifier)  ;
    public AssetWS[] findAssetsByProductCodeAndStatus(String productCode, Integer assetStatusId)  ;
    public AssetWS[] findAssetsForOrderChanges(Integer[] ids)  ;
    /*
     *  Rate Card
     */

    public Integer createRateCard(RateCardWS rateCard, File rateCardFile);
    public void updateRateCard(RateCardWS rateCard, File rateCardFile);
    public void deleteRateCard(Integer rateCardId);

    public Integer createRouteRecord(RouteRecordWS routeRecord, Integer routeId)  ;
    public Integer createRouteRateCardRecord(RouteRateCardWS routeRateCardRecord, Integer routeRateCardId)  ;
    public void updateRouteRecord(RouteRecordWS routeRecord, Integer routeId)  ;
    public void updateRouteRateCardRecord(RouteRateCardWS record, Integer routeRateCardId)  ;
    public void deleteRouteRecord(Integer routeId, Integer recordId)  ;
    public void deleteRateCardRecord(Integer routeRateCardId, Integer recordId)  ;
    public String getRouteTable(Integer routeId)  ;
    public SearchResultString searchDataTable(Integer routeId, SearchCriteria criteria) ;
    public Set<String> searchDataTableWithFilter(Integer routeId, String filters, String searchName) ;
    public SearchResultString searchRouteRateCard(Integer routeRateCardId, SearchCriteria criteria) ;
    public Integer createDataTableQuery(DataTableQueryWS queryWS) ;
    public DataTableQueryWS getDataTableQuery(int id) ;
    public void deleteDataTableQuery(int id) ;
    public DataTableQueryWS[] findDataTableQueriesForTable(int routeId) ;

    /*
     * Trigger RE-Cycle for Mediation configuration
     */
    public UUID runRecycleForConfiguration(Integer configId);
    public UUID runRecycleForMediationProcess(UUID processId);

    public Integer reserveAsset(Integer assetId, Integer userId);
    public void releaseAsset(Integer assetId, Integer userId);

    public AssetAssignmentWS[] getAssetAssignmentsForAsset(Integer assetId);
    public AssetAssignmentWS[] getAssetAssignmentsForOrder(Integer orderId);
    public Integer findOrderForAsset(Integer assetId, Date date);
    public Integer[] findOrdersForAssetAndDateRange(Integer assetId, Date startDate, Date endDate);

    /*
     *  MetaField Group
     */

    public Integer createMetaFieldGroup(MetaFieldGroupWS metafieldGroup);
    @Validator(type = Validator.Type.EDIT)
    public void updateMetaFieldGroup(MetaFieldGroupWS metafieldGroupWs);
    @Validator(type = Validator.Type.EDIT)
    public void deleteMetaFieldGroup(Integer metafieldGroupId);
    public MetaFieldGroupWS getMetaFieldGroup(Integer metafieldGroupId);
    public MetaFieldGroupWS[] getMetaFieldGroupsForEntity(String entityType);

    public Integer createMetaField(MetaFieldWS metafield);
    public Integer createMetaFieldWithEntityId(MetaFieldWS metafieldWs, Integer entityId);
    @Validator(type = Validator.Type.EDIT)
    public void updateMetaField(MetaFieldWS metafieldWs);
    @Validator(type = Validator.Type.EDIT)
    public void deleteMetaField(Integer metafieldId);
    public MetaFieldWS getMetaField(Integer metafieldId);
    public MetaFieldWS[] getMetaFieldsForEntity(String entityType);

    public Integer createOrUpdateDiscount(DiscountWS discount);
    public DiscountWS getDiscountWS(Integer discountId);
    public DiscountWS getDiscountWSByCode(String discountCode);
    @Validator(type = Validator.Type.EDIT)
    public void deleteDiscount(Integer discountId);

    /*
     * OrderChangeStatus
     */
    public OrderChangeStatusWS[] getOrderChangeStatusesForCompany();
    public Integer createOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) ;
    @Validator(type = Validator.Type.EDIT)
    public void updateOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) ;
    @Validator(type = Validator.Type.EDIT)
    public void deleteOrderChangeStatus(Integer id) ;
    public void saveOrderChangeStatuses(OrderChangeStatusWS[] orderChangeStatuses) ;

    /*
     * OrderChangeType
     */
    public OrderChangeTypeWS[] getOrderChangeTypesForCompany();
    public OrderChangeTypeWS getOrderChangeTypeByName(String name);
    public OrderChangeTypeWS getOrderChangeTypeById(Integer orderChangeTypeId);
    public Integer createUpdateOrderChangeType(OrderChangeTypeWS orderChangeTypeWS) throws SessionInternalError;
    @Validator(type = Validator.Type.EDIT)
    public void deleteOrderChangeType(Integer orderChangeTypeId);

    /*
     * OrderChange
     */
    public OrderChangeWS[] getOrderChanges(Integer orderId);
    public void updateOrderChangeEndDate(Integer orderChangeId, Date endDate);

    /*
       Diameter Protocol
     */
    DiameterResultWS createSession(String sessionId, Date timestamp, BigDecimal units,
            String string) ;
    DiameterResultWS reserveUnits(String sessionId, Date timestamp, int units,
            String string) ;
    DiameterResultWS updateSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            BigDecimal reqUnits, String string) ;
    DiameterResultWS extendSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            BigDecimal reqUnits) ;
    DiameterResultWS endSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            int causeCode) ;
    DiameterResultWS consumeReservedUnits(String sessionId, Date timestamp, int usedUnits,
            int causeCode) ;
    /*
      Route Based Rating
     */
    public Integer createRoute(RouteWS routeWS, File routeFile);
    @Validator(type = Validator.Type.EDIT)
    public void deleteRoute(Integer routeId);
    public RouteWS getRoute(Integer routeId);
    public Integer createMatchingField(MatchingFieldWS matchingFieldWS);
    @Validator(type = Validator.Type.EDIT)
    public void deleteMatchingField(Integer matchingFieldId);
    public MatchingFieldWS getMatchingField(Integer matchingFieldId);
    @Validator(type = Validator.Type.EDIT)
    public boolean updateMatchingField(MatchingFieldWS matchingFieldWS);
    public Integer createRouteRateCard(RouteRateCardWS routeRateCardWS, File routeRateCardFile);
    @Validator(type = Validator.Type.EDIT)
    public void deleteRouteRateCard(Integer routeId);
    @Validator(type = Validator.Type.EDIT)
    public void updateRouteRateCard(RouteRateCardWS routeRateCardWS, File routeRateCardFile);
    public RouteRateCardWS getRouteRateCard(Integer routeRateCardId);

    public Integer createRatingUnit(RatingUnitWS ratingUnitWS) ;
    public void updateRatingUnit(RatingUnitWS ratingUnitWS) ;
    public boolean deleteRatingUnit(Integer ratingUnitId) ;
    public RatingUnitWS getRatingUnit(Integer ratingUnitId) ;
    public RatingUnitWS[] getAllRatingUnits() ;

    /*
     * UsagePool
     */
    public Integer createUsagePool(UsagePoolWS usagePool);
    @Validator(type = Validator.Type.EDIT)
    public void updateUsagePool(UsagePoolWS usagePool);
    public UsagePoolWS getUsagePoolWS(Integer usagePoolId);
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteUsagePool(Integer usagePoolId);
    public UsagePoolWS[] getAllUsagePools();
    public UsagePoolWS[] getUsagePoolsByPlanId(Integer planId);
    public CustomerUsagePoolWS getCustomerUsagePoolById(Integer customerUsagePoolId);
    public CustomerUsagePoolWS[] getCustomerUsagePoolsByCustomerId(Integer customerId);

    /*
     *Payment Method
     */
    public PaymentMethodTemplateWS getPaymentMethodTemplate(Integer templateId);

    public Integer createPaymentMethodType(PaymentMethodTypeWS paymentMethod);
    public void updatePaymentMethodType(PaymentMethodTypeWS paymentMethod);
    public boolean deletePaymentMethodType(Integer paymentMethodTypeId);
    public PaymentMethodTypeWS getPaymentMethodType(Integer paymentMethodTypeId);

    public boolean removePaymentInstrument(Integer instrumentId);

    /*
     *  Order status
     */

    public Integer createUpdateOrderStatus(OrderStatusWS newOrderStatus) ;
    public Integer createUpdateEdiStatus(EDIFileStatusWS ediFileStatusWS) ;
    @Validator(type = Validator.Type.EDIT)
    public void deleteOrderStatus(OrderStatusWS orderStatus);
    public void deleteEdiFileStatus(Integer ediFileStatusId);
    public OrderStatusWS findOrderStatusById(Integer orderStatusId);
    public Integer[] findAllOrderStatusIds();
    public EDIFileStatusWS findEdiStatusById(Integer orderStatusId);
    public int getDefaultOrderStatusId(OrderStatusFlag flag, Integer entityId);

    /*
     * Plugin
     */

    public PluggableTaskTypeWS getPluginTypeWS(Integer id);
    public PluggableTaskTypeWS getPluginTypeWSByClassName(String className);
    public PluggableTaskTypeCategoryWS getPluginTypeCategory(Integer id);
    public PluggableTaskTypeCategoryWS getPluginTypeCategoryByInterfaceName(String interfaceName);

    /*
     * Subscription category
     */

    public Integer[] createSubscriptionAccountAndOrder(Integer parentAccountId, OrderWS order, boolean createInvoice, List<OrderChangeWS> orderChanges);

    /* Language */
    public Integer createOrEditLanguage(LanguageWS languageWS);
    public Long getMediationErrorRecordsCount(Integer mediationConfigurationId);

    /*
     * Enumerations
     */
    public EnumerationWS getEnumeration(Integer enumerationId);
    public EnumerationWS getEnumerationByName(String name);
    public EnumerationWS getEnumerationByNameAndCompanyId(String name, Integer companyId) ;
    public List<EnumerationWS> getAllEnumerations(Integer max, Integer offset);
    public Long getAllEnumerationsCount();
    public Integer createUpdateEnumeration(EnumerationWS enumerationWS) ;
    @Validator(type = Validator.Type.EDIT)
    public boolean deleteEnumeration(Integer enumerationId) ;

    /*
     * Copy Company
     * */
    public UserWS copyCompany(String childCompanyTemplateName, Integer entityId, List<String> importEntities,
            boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail);
    public UserWS copyCompanyInSaas(String childCompanyTemplateName, Integer entityId, List<String> importEntities,
                              boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail, String systemAdminLoginName);

    public UserWS createUserWithCIMProfileValidation(UserWS newUser) ;
    public UserWS updateUserWithCIMProfileValidation(UserWS newUser) ;
    public Integer[] getUnpaidInvoicesOldestFirst(Integer userId);
    public void applyPaymentsToInvoices(Integer userId) ;

    /* Mediation Rating Schemes */
    public MediationRatingSchemeWS getRatingScheme(Integer mediationRatingSchemeId) ;
    public MediationRatingSchemeWS[] getRatingSchemesForEntity ();
    public MediationRatingSchemeWS[] getRatingSchemesPagedForEntity (Integer max, Integer offset);
    public Long countRatingSchemesPagedForEntity();
    public boolean deleteRatingScheme(Integer ratingSchemeId) ;
    public Integer createRatingScheme(MediationRatingSchemeWS ws) ;
    public Integer getRatingSchemeForMediationAndCompany(Integer mediationCfgId, Integer companyId);
    public BigDecimal getQuantity(Integer ratingSchemeId, Integer callDuration);
    public Integer[] getPaymentsByUserId(Integer userId) ;

    /* Swap Plan History api*/
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderId(Integer orderId);
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderAndSwapDate(Integer orderId, Date from, Date to);

    //Customer Enrollment
    public CustomerEnrollmentWS getCustomerEnrollment(Integer enrollmentId) ;
    public Integer createUpdateEnrollment(CustomerEnrollmentWS customerEnrollmentWS) ;
    public CustomerEnrollmentWS validateCustomerEnrollment(CustomerEnrollmentWS customerEnrollmentWS) ;
    public void deleteEnrollment(Integer customerEnrollmentId) ;

    // EDI File processor
    public EDITypeWS getEDIType(Integer ediTypeId) ;
    public Integer createEDIType(EDITypeWS ediTypeWS, File ediFormatFile) ;
    public void deleteEDIType(Integer ediTypeId) ;

    public  int generateEDIFile(Integer ediTypeId, Integer entityId, String fileName, Collection input) ;
    public int parseEDIFile(Integer ediTypeId, Integer entityId, File parserFile) ;

    public int saveEDIFileRecord(EDIFileWS ediFileWS) ;
    public void updateEDIFileStatus(Integer fileId, String statusName, String comment) ;

    public List<Integer> getEDIFiles(Integer ediTypeId, String fieldKey, String fieldValue, TransactionType transactionType, String statusName);
    public EDIFileWS getEDIFileById(Integer ediFileId);

    public List<CompanyWS> getAllChildEntities(Integer parentId) ;
    public void updateEDIStatus(EDIFileWS ediFileWS, EDIFileStatusWS ediFileStatusWS, Boolean escapeValidation) ;
    public List<OrphanEDIFile> getLDCFiles(TransactionType type);
    public File getOrphanLDCFile(TransactionType type, String fileName);
    public void deleteOrphanEDIFile(TransactionType type, List<String> fileName);
    public void uploadEDIFile(File ediFile);

    /* migration related api */
    public Integer createAdjustmentOrderAndInvoice(String customerPrimaryAccount, OrderWS order, OrderChangeWS[] orderChanges);
    public String createPaymentForHistoricalMigration(String customerPrimaryAccount, String primaryAccountMetaFieldName, Integer[][] chequePmMap,String amount, String date);
    public String createPaymentForHistoricalDateMigration(String customerPrimaryAccount, Integer chequePmId, String amount, String date);
    public void processMigrationPayment(PaymentWS paymentWS) ;
    public String adjustUserBalance(String customerPrimaryAccount, String amount, Integer chequePmId, String date);

    public QueryResultWS getQueryResult(String queryCode, QueryParameterWS[] parameters, Integer limit, Integer offSet);
    public QueryParameterWS[] getParametersByQueryCode(String queryCode);

    public Integer processSignupPayment(UserWS user, PaymentWS payment);

    /*
     * Credit Note
     */
    public CreditNoteWS[] getAllCreditNotes(Integer entityId);
    public CreditNoteWS getCreditNote(Integer creditNoteId);
    public void updateCreditNote(CreditNoteWS creditNoteWs);
    public void deleteCreditNote(Integer creditNoteId);
    public Integer[] getLastCreditNotes(Integer userId, Integer number) ;

    public void applyCreditNote(Integer creditNoteId);
    public void applyExistingCreditNotesToUnpaidInvoices(Integer userId);
    public void applyExistingCreditNotesToInvoice(Integer invoiceId);
    public void applyCreditNoteToInvoice(Integer creditNoteId, Integer debitInvoiceId);
    public void removeCreditNoteLink(Integer invoiceId, Integer creditNoteId);
    public CreditNoteWS[] getCreditNotesByUser(Integer userId, Integer offset, Integer limit);

    /* Entity Filters */
    public PagedResultList findOrdersByFilters(int page, int size, String sort, String order, List<Filter> filters);
    public PagedResultList findProvisioningCommandsByFilters(int page, int size, String sort, String order, List<Filter> filters);

    /**
     * Invoice Summary
     */
    public ItemizedAccountWS getItemizedAccountByInvoiceId(Integer invoiceId);
    public InvoiceSummaryWS getInvoiceSummary(Integer invoiceId);
    public InvoiceLineDTO[] getRecurringChargesByInvoiceId(Integer invoiceId);
    public InvoiceLineDTO[] getUsageChargesByInvoiceId(Integer invoiceId);
    public InvoiceLineDTO[] getFeesByInvoiceId(Integer invoiceId);
    public InvoiceLineDTO[] getTaxesByInvoiceId(Integer invoiceId);
    public PaymentWS[] getPaymentsAndRefundsByInvoiceId(Integer invoiceId);
    public CreditAdjustmentWS[] getCreditAdjustmentsByInvoiceId(Integer invoiceId);

    /*
      Job Execution Statistics
     */
    public JobExecutionHeaderWS[] getJobExecutionsForDateRange(String jobType,Date startDate, Date endDate, int offset, int limit, String sort, String order );

    /*
     * Cancellation Request
     */
    public Integer createCancellationRequest(CancellationRequestWS cancellationRequest);
    public void updateCancellationRequest(CancellationRequestWS cancellationRequest);
    public CancellationRequestWS[] getAllCancellationRequests(Integer entityId, Date startDate, Date endDate);
    public CancellationRequestWS getCancellationRequestById(Integer cancellationRequestId);
    public CancellationRequestWS[] getCancellationRequestsByUserId(Integer userId);
    public void deleteCancellationRequest(Integer cancellationId);

    /*
     * CreateOrUpdateOrderChange Request
     */
    void createUpdateOrderChange(Integer userId, String productCode,
            BigDecimal newPrice, BigDecimal newQuantity, Date changeEffectiveDate) ;

    public boolean notifyUserByEmail(Integer userId, Integer notificationId) ;
    public Integer getIdFromCreateUpdateNotification(Integer messageId, MessageDTO dto);
    public Integer createMessageNotificationType(Integer notificationCategoryId, String description, Integer languageId );

    /* Usage Rating Scheme */
    public Integer createUsageRatingScheme(UsageRatingSchemeWS ws) ;
    public boolean deleteUsageRatingScheme(Integer usageRatingSchemeId) ;
    public UsageRatingSchemeWS getUsageRatingScheme(Integer usageRatingSchemeId);
    public Long countUsageRatingSchemes ();
    public List<UsageRatingSchemeWS> findAllUsageRatingSchemes();
    public List<UsageRatingSchemeWS> getAllUsageRatingSchemes(Integer max, Integer offset);
    public List<UsageRatingSchemeType> findAllRatingSchemeTypeValues();

    // API user detail
    public String createApiUserDetail(ApiUserDetailWS ws) ;
    public Long countApiUserDetails ();
    public List<ApiUserDetailWS> findAllApiUserDetails();
    public List<ApiUserDetailWS> getAllApiUserDetails(Integer max, Integer offset);
    public ApiUserDetailWS getUserDetails(String accessCode);
    public PlanWS getPlanByInternalNumber(String internalNumber, Integer entityId);

    // API to get CreditNoteInvoiceMap by date
    public CreditNoteInvoiceMapWS[] getCreditNoteInvoiceMaps(Date invoiceCreationStartDate, Date invoiceCreationEndDate);
    public PaymentMethodTypeWS[] getAllPaymentMethodTypes() throws SessionInternalError;

    // Signup Request API.
    public SignupResponseWS processSignupRequest(SignupRequestWS request);

    // validate user credentials
    public UserWS validateLogin(String userName, String password);
    public UserProfileWS getUserProfile(Integer userId);

    public JbillingMediationRecord[] getMediationEventsForUserDateRange(Integer userId, Date startDate,
            Date endDate, int offset, int limit);
    public AssetRestWS[] getAllAssetsForUser(Integer userId);
    public OrderWS[] getOrderMetaFieldMap(OrderWS[] orderWS);

    public Integer createOrderWithAssets(OrderWS order, OrderChangeWS[] orderChanges, AssetWS[] assets);

    //update order level meta field values
    public void updateOrderMetaFields(Integer orderId, MetaFieldValueWS[] orderMetaFieldValues);
    // get Order level Meta field values
    public OrderMetaFieldValueWS getOrderMetaFieldValueWS(Integer orderId);
    //update Asset level meta field values
    public void updateAssetMetaFields(Integer assetId, MetaFieldValueWS[] assetMetaFieldValues);
    public PaymentInformationWS[] getPaymentInstruments(Integer userId);
    public CustomerMetaFieldValueWS getCustomerMetaFields(Integer userId);
    public UserWS updateCustomerContactInfo(ContactInformationWS contactInformation);
    public SecurePaymentWS addPaymentInstrument(PaymentInformationWS instrument);

    // updates customer meta fields values
    public void updateCustomerMetaFields(Integer userId, MetaFieldValueWS[] customerMetaFieldValues);

    public JbillingMediationRecord[] getUnBilledMediationEventsByUser(Integer userId, int offset, int limit);
    public OrderWS[] getUsersAllSubscriptions(Integer userId);
    /**
     * fetches payments for given userId
     * @param userId
     * @param offset
     * @param limit
     * @return {@link PaymentWS[]}
     */
    public PaymentWS[] findPaymentsForUser(Integer userId, int offset, int limit);
    void updatePassword(Integer userId, String currentPassword, String newPassword);
    public Integer[] createCustomInvoice(File csvFile);

    public CreateResponseWS createWithExistingUser(Integer userId, OrderWS order,  OrderChangeWS[] orderChanges);

    public PaymentUrlLogDTO createPaymentUrl(Map<String, Object> map);

    public String generateGstR1JSONFileReport(String startDate, String endDate) throws Exception;

    public boolean notifyPaymentLinkByEmail(Integer invoiceId);

    public Optional<Object> executePaymentTask(Integer paymentLogUrlLogId, String payerVPA,
                                               String action) throws PluggableTaskException;
}
