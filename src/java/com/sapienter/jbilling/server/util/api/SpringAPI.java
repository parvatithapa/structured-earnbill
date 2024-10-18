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

package com.sapienter.jbilling.server.util.api;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.resources.OrderMetaFieldValueWS;
import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS;
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
import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetRestWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
import com.sapienter.jbilling.server.item.AssetTransitionDTOEx;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.SwapAssetWS;
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
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.order.Usage;
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
import com.sapienter.jbilling.server.sql.api.QueryResultWS;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.SwapPlanHistoryWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.UsageRatingSchemeType;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.ContactInformationWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.CreateResponseWS;
import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.user.UserCodeWS;
import com.sapienter.jbilling.server.user.UserProfileWS;
import com.sapienter.jbilling.server.user.UserTransitionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS;
import com.sapienter.jbilling.server.user.partner.CommissionWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.JobExecutionHeaderWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;

public class SpringAPI implements JbillingAPI {

    private IWebServicesSessionBean session = null;

    public SpringAPI() {
        this(RemoteContext.Name.API_CLIENT);
    }

    public SpringAPI(String beanName) {
        session = RemoteContext.getBean(beanName);
    }

    public SpringAPI(RemoteContext.Name bean) {
        session = RemoteContext.getBean(bean);
    }

    @Override
    public Integer applyPayment(PaymentWS payment, Integer invoiceId) {
        return session.applyPayment(payment, invoiceId);
    }

    @Override
    public PaymentAuthorizationDTOEx processPayment(PaymentWS payment, Integer invoiceId) {
        return session.processPayment(payment, invoiceId);
    }

    @Override
    public PaymentAuthorizationDTOEx[] processPayments(PaymentWS[] payments, Integer invoiceId) {
        return session.processPayments(payments, invoiceId);
    }


    @Override
    public PartnerWS getPartner(Integer partnerId) {
        return session.getPartner(partnerId);
    }

    @Override
    public Integer createPartner(UserWS newUser, PartnerWS partner) {
        return session.createPartner(newUser, partner);
    }

    @Override
    public void updatePartner(UserWS newUser, PartnerWS partner) {
        session.updatePartner(newUser, partner);
    }

    @Override
    public void deletePartner (Integer partnerId){
        session.deletePartner(partnerId);
    }

    @Override
    public CreateResponseWS create(UserWS user, OrderWS order, OrderChangeWS[] orderChanges) {
        return session.create(user, order, orderChanges);
    }

    @Override
    public Integer createUserCode(UserCodeWS userCode) {
        return session.createUserCode(userCode);
    }

    @Override
    public UserCodeWS[] getUserCodesForUser(Integer userId) {
        return session.getUserCodesForUser(userId);
    }
    @Override
    public void updateUserCode(UserCodeWS userCode) {
        session.updateUserCode(userCode);
    }
    @Override
    public Integer[] getCustomersByUserCode(String userCode) {
        return session.getCustomersByUserCode(userCode);
    }

    @Override
    public Integer[] getOrdersByUserCode(String userCode) {
        return session.getOrdersByUserCode(userCode);
    }

    @Override
    public Integer[] getOrdersLinkedToUser(Integer userId) {
        return session.getOrdersLinkedToUser(userId);
    }

    @Override
    public Integer[] getCustomersLinkedToUser(Integer userId) {
        return session.getCustomersLinkedToUser(userId);
    }

    @Override
    public Integer createItem(ItemDTOEx dto) {
        return session.createItem(dto);
    }

    @Override
    public Integer createOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        return session.createOrder(order, orderChanges);
    }

    @Override
    public Integer createOrderAndInvoice(OrderWS order, OrderChangeWS[] orderChanges) {
        return session.createOrderAndInvoice(order, orderChanges);
    }

    @Override
    public PaymentAuthorizationDTOEx createOrderPreAuthorize(OrderWS order, OrderChangeWS[] orderChanges) {
        return session.createOrderPreAuthorize(order, orderChanges);
    }

    @Override
    public OrderWS[] getLinkedOrders(Integer primaryOrderId) {
        return session.getLinkedOrders(primaryOrderId);
    }

    @Override
    public Integer createUser(UserWS newUser) {
        return session.createUser(newUser);
    }

    @Override
    public String deleteOrder(Integer id) {
        return session.deleteOrder(id);
    }

    @Override
    public void deleteUser(Integer userId) {
        session.deleteUser(userId);
    }

    @Override
    public void initiateTermination(Integer userId, String reasonCode, Date terminationDate) {
        session.initiateTermination(userId, reasonCode, terminationDate);
    }
    @Override
    public boolean userExistsWithName(String userName) {
        return session.userExistsWithName(userName);
    }

    @Override
    public boolean userExistsWithId(Integer userId) {
        return session.userExistsWithId(userId);
    }

    @Override
    public void deleteInvoice(Integer invoiceId) {
        session.deleteInvoice(invoiceId);
    }

    @Override
    public Integer saveLegacyInvoice(InvoiceWS invoiceWS) {
        return session.saveLegacyInvoice(invoiceWS);
    }

    @Override
    public Integer saveLegacyPayment(PaymentWS paymentWS) {
        return session.saveLegacyPayment(paymentWS);
    }

    @Override
    public Integer saveLegacyOrder(OrderWS orderWS) {
        return session.saveLegacyOrder(orderWS);
    }

    @Override
    public ItemDTOEx[] getAllItems() {
        return session.getAllItems();
    }

    @Override
    public InvoiceWS getInvoiceWS(Integer invoiceId) {
        return session.getInvoiceWS(invoiceId);
    }

    @Override
    public Integer[] getInvoicesByDate(String since, String until) {
        return session.getInvoicesByDate(since, until);
    }

    @Override
    public byte[] getPaperInvoicePDF(Integer invoiceId) {
        return session.getPaperInvoicePDF(invoiceId);
    }

    @Override
    public boolean notifyInvoiceByEmail(Integer invoiceId) {
        return session.notifyInvoiceByEmail(invoiceId);
    }

    @Override
    public boolean notifyPaymentByEmail(Integer paymentId) {
        return session.notifyPaymentByEmail(paymentId);
    }

    @Override
    public Integer[] getLastInvoices(Integer userId, Integer number) {
        return session.getLastInvoices(userId, number);
    }

    @Override
    public Integer[] getUserInvoicesByDate(Integer userId, String since, String until) {
        return session.getUserInvoicesByDate(userId, since, until);
    }

    @Override
    public InvoiceWS[] getUserInvoicesPage(Integer userId, Integer limit, Integer offset) {
        return session.getUserInvoicesPage(userId, limit,offset);
    }

    @Override
    public Integer[] getUnpaidInvoices(Integer userId) {
        return session.getUnpaidInvoices(userId);
    }

    @Override
    public Integer[] getLastInvoicesByItemType(Integer userId, Integer itemTypeId, Integer number) {
        return session.getLastInvoicesByItemType(userId, itemTypeId, number);
    }

    @Override
    public Integer[] getLastOrders(Integer userId, Integer number) {
        return session.getLastOrders(userId, number);
    }

    @Override
    public Integer[] getLastOrdersPage(Integer userId, Integer limit, Integer offset) {
        return session.getLastOrdersPage(userId, limit, offset);
    }

    @Override
    public Integer[] getOrdersByDate(Integer userId, Date since, Date until) {
        return session.getOrdersByDate(userId, since, until);
    }


    @Override
    public Integer[] getLastOrdersByItemType(Integer userId, Integer itemTypeId, Integer number) {
        return session.getLastOrdersByItemType(userId, itemTypeId, number);
    }

    @Override
    public OrderWS[] getUserOrdersPage(Integer user, Integer limit, Integer offset) {
        return session.getUserOrdersPage(user, limit, offset);
    }

    @Override
    public OrderWS getCurrentOrder(Integer userId, Date date) {
        return session.getCurrentOrder(userId, date);
    }

    @Override
    public OrderWS updateCurrentOrder(Integer userId, OrderLineWS[] lines, PricingField[] fields, Date date,
            String eventDescription) {

        return session.updateCurrentOrder(userId, lines, PricingField.setPricingFieldsValue(fields), date,
                eventDescription);
    }

    @Override
    public Integer[] getLastPayments(Integer userId, Integer number) {
        return session.getLastPayments(userId, number);
    }

    @Override
    public Integer[] getLastPaymentsPage(Integer userId, Integer limit, Integer offset){
        return session.getLastPaymentsPage(userId, limit, offset);
    }

    @Override
    public Integer[] getPaymentsByDate(Integer userId, Date since, Date until){
        return session.getPaymentsByDate(userId, since, until);
    }

    @Override
    public PaymentWS getUserPaymentInstrument(Integer userId) {
        return session.getUserPaymentInstrument(userId);
    }

    @Override
    public PaymentWS[] getUserPaymentsPage(Integer userId, Integer limit, Integer offset) {
        return session.getUserPaymentsPage(userId, limit, offset);
    }

    @Override
    public Integer[] getAllInvoices(Integer userId) {
        return session.getAllInvoices(userId);
    }

    @Override
    public InvoiceWS getLatestInvoice(Integer userId) {
        return session.getLatestInvoice(userId);
    }

    @Override
    public InvoiceWS getLatestInvoiceByItemType(Integer userId, Integer itemTypeId) {
        return session.getLatestInvoiceByItemType(userId, itemTypeId);
    }

    @Override
    public OrderWS getLatestOrder(Integer userId) {
        return session.getLatestOrder(userId);
    }

    @Override
    public OrderWS getLatestOrderByItemType(Integer userId, Integer itemTypeId) {
        return session.getLatestOrderByItemType(userId, itemTypeId);
    }

    @Override
    public PaymentWS getLatestPayment(Integer userId) {
        return session.getLatestPayment(userId);
    }

    @Override
    public OrderWS getOrder(Integer orderId) {
        return session.getOrder(orderId);
    }

    @Override
    public Integer[] getOrderByPeriod(Integer userId, Integer periodId) {
        return session.getOrderByPeriod(userId, periodId);
    }

    @Override
    public OrderLineWS getOrderLine(Integer orderLineId) {
        return session.getOrderLine(orderLineId);
    }

    @Override
    public PaymentWS getPayment(Integer paymentId) {
        return session.getPayment(paymentId);
    }

    @Override
    public ContactWS[] getUserContactsWS(Integer userId) {
        return session.getUserContactsWS(userId);
    }

    @Override
    public Integer getUserId(String username) {
        return session.getUserId(username);
    }

    @Override
    public Integer getUserIdByEmail(String email){
        return session.getUserIdByEmail(email);
    }

    @Override
    public UserWS getUserBySupplierID(String supplierId) {
        return session.getUserBySupplierID(supplierId);
    }

    @Override
    public UserTransitionResponseWS[] getUserTransitions(Date from, Date to) {
        return session.getUserTransitions(from, to);
    }

    @Override
    public UserTransitionResponseWS[] getUserTransitionsAfterId(Integer id) {
        return session.getUserTransitionsAfterId(id);
    }

    @Override
    public UserWS getUserWS(Integer userId) {
        return session.getUserWS(userId);
    }

    @Override
    public Integer[] getUsersByStatus(Integer statusId, boolean in) {
        return session.getUsersByStatus(statusId, in);
    }

    @Override
    public Integer[] getUsersInStatus(Integer statusId) {
        return session.getUsersInStatus(statusId);
    }

    @Override
    public Integer[] getUsersNotInStatus(Integer statusId) {
        return session.getUsersNotInStatus(statusId);
    }

    @Override
    public void createPaymentLink(Integer invoiceId, Integer paymentId) {
        session.createPaymentLink(invoiceId, paymentId);
    }

    @Override
    public void removePaymentLink(Integer invoiceId, Integer paymentId) {
        session.removePaymentLink(invoiceId, paymentId);
    }

    @Override
    public void removeAllPaymentLinks(Integer paymentId) {
        session.removeAllPaymentLinks(paymentId);
    }

    @Override
    public PaymentAuthorizationDTOEx payInvoice(Integer invoiceId) {
        return session.payInvoice(invoiceId);
    }

    @Override
    public Integer createPayment(PaymentWS payment) {
        return session.createPayment(payment);
    }

    @Override
    public Integer[] createPayments(PaymentWS[] payments) {
        return session.createPayments(payments);
    }

    @Override
    public void updatePayment(PaymentWS payment) {
        session.updatePayment(payment);
    }

    @Override
    public void deletePayment(Integer paymentId) {
        session.deletePayment(paymentId);
    }

    @Override
    public void updateOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        session.updateOrder(order, orderChanges);
    }

    @Override
    public void updateOrders(OrderWS[] orders, OrderChangeWS[] orderChanges) {
        session.updateOrders(orders, orderChanges);
    }

    @Override
    public void upgradePlanOrder(Integer orderId, Integer orderToUpgradeId, Integer paymentId) {
        session.upgradePlanOrder(orderId, orderToUpgradeId, paymentId);
    }

    @Override
    public Integer createUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        return session.createUpdateOrder(order, orderChanges);
    }

    @Override
    public void updateOrderLine(OrderLineWS line) {
        session.updateOrderLine(line);
    }

    @Override
    public void updateUser(UserWS user) {
        session.updateUser(user);
    }

    @Override
    public void updateUserContact(Integer userId, ContactWS contact) {
        session.updateUserContact(userId, contact);
    }

    @Override
    public ItemDTOEx getItem(Integer itemId, Integer userId, PricingField[] fields) {
        return session.getItem(itemId, userId, PricingField.setPricingFieldsValue(fields));
    }

    @Override
    public ItemTypeWS[] getItemCategoriesByPartner(String partner, boolean parentCategoriesOnly) {
        return session.getItemCategoriesByPartner(partner, parentCategoriesOnly);
    }
    @Override
    public ItemTypeWS[] getChildItemCategories(Integer itemTypeId) {
        return session.getChildItemCategories(itemTypeId);
    }

    @Override
    public ItemDTOEx[] getAddonItems(Integer itemId) {
        return session.getAddonItems(itemId);
    }

    @Override
    public OrderWS rateOrder(OrderWS order, OrderChangeWS[] orderChanges) {
        return session.rateOrder(order, orderChanges);
    }

    @Override
    public OrderWS[] rateOrders(OrderWS orders[], OrderChangeWS[] orderChanges) {
        return session.rateOrders(orders, orderChanges);
    }

    @Override
    public OrderChangeWS[] calculateSwapPlanChanges(OrderWS order, Integer existingPlanItemId, Integer swapPlanItemId, SwapMethod method, Date effectiveDate) {
        return session.calculateSwapPlanChanges(order, existingPlanItemId, swapPlanItemId, method, effectiveDate);
    }

    @Override
    public boolean swapPlan(Integer orderId, String existingPlanCode, String swapPlanCode, SwapMethod swapMethod){
        return session.swapPlan(orderId, existingPlanCode, swapPlanCode, swapMethod);
    }

    @Override
    public void swapAssets(Integer orderId, SwapAssetWS[] swapRequests) {
        session.swapAssets(orderId, swapRequests);
    }

    @Override
    public Map<String, BigDecimal> calculateUpgradePlan(Integer orderId, Integer planId, String discountCode) {
        return session.calculateUpgradePlan(orderId, planId, discountCode);
    }

    @Override
    public void updateItem(ItemDTOEx item) {
        session.updateItem(item);
    }

    @Override
    public void deleteItem(Integer itemId) {
        session.deleteItem(itemId);
    }

    @Override
    public Integer[] createInvoice(Integer userId, boolean onlyRecurring) {
        return session.createInvoice(userId, onlyRecurring);
    }

    @Override
    public Integer[] createInvoiceWithDate(Integer userId, Date billingDate, Integer dueDatePeriodId, Integer dueDatePeriodValue, boolean onlyRecurring) {
        return session.createInvoiceWithDate(userId, billingDate, dueDatePeriodId, dueDatePeriodValue, onlyRecurring);
    }

    @Override
    public Integer createInvoiceFromOrder(Integer orderId, Integer invoiceId) {
        return session.createInvoiceFromOrder(orderId, invoiceId);
    }

    @Override
    public Integer applyOrderToInvoice(Integer orderId, InvoiceWS invoiceWs) {
        return session.applyOrderToInvoice(orderId, invoiceWs);
    }

    @Override
    public String isUserSubscribedTo(Integer userId, Integer itemId) {
        return session.isUserSubscribedTo(userId, itemId);
    }

    @Override
    public Integer[] getUserItemsByCategory(Integer userId, Integer categoryId) {
        return session.getUserItemsByCategory(userId, categoryId);
    }

    @Override
    public ItemDTOEx[] getItemByCategory(Integer itemTypeId) {
        return session.getItemByCategory(itemTypeId);
    }

    @Override
    public ItemTypeWS getItemCategoryById(Integer id) {
        return session.getItemCategoryById(id);
    }

    @Override
    public ItemTypeWS[] getAllItemCategories() {
        return session.getAllItemCategories();
    }

    @Override
    public ValidatePurchaseWS validatePurchase(Integer userId, Integer itemId, PricingField[] fields) {
        return session.validatePurchase(userId, itemId, PricingField.setPricingFieldsValue(fields));
    }

    @Override
    public ValidatePurchaseWS validateMultiPurchase(Integer userId, Integer[] itemIds, PricingField[][] fields) {
        String[] pricingFields = null;
        if (fields != null) {
            pricingFields = new String[fields.length];
            for (int i = 0; i < pricingFields.length; i++) {
                pricingFields[i] = PricingField.setPricingFieldsValue(fields[i]);
            }
        }
        return session.validateMultiPurchase(userId, itemIds, pricingFields);
    }

    @Override
    public Integer getItemID(String productCode) {
        return session.getItemID(productCode);
    }

    @Override
    public Integer createItemCategory(ItemTypeWS itemType) {
        return session.createItemCategory(itemType);
    }

    @Override
    public void updateItemCategory(ItemTypeWS itemType) {
        session.updateItemCategory(itemType);
    }

    @Override
    public void deleteItemCategory(Integer itemCategoryId) {
        session.deleteItemCategory(itemCategoryId);
    }

    @Override
    public ItemTypeWS[] getAllItemCategoriesByEntityId(Integer entityId) {
        return session.getAllItemCategoriesByEntityId(entityId);
    }

    @Override
    public ItemDTOEx[] getAllItemsByEntityId(Integer entityId) {
        return session.getAllItemsByEntityId(entityId);
    }

    @Override
    public Integer getAutoPaymentType(Integer userId) {
        return session.getAuthPaymentType(userId);
    }

    @Override
    public void setAutoPaymentType(Integer userId, Integer autoPaymentType, boolean use) {
        session.setAuthPaymentType(userId, autoPaymentType, use);
    }

    @Override
    public void resetPassword(int userId) {
        session.resetPassword(userId);
    }

    /*
        Billing process
     */

    @Override
    public void triggerBillingAsync(Date runDate) {
        session.triggerBillingAsync(runDate);
    }

    @Override
    public boolean triggerBilling(Date runDate) {
        return session.triggerBilling(runDate);
    }


    @Override
    public void triggerAgeing(Date runDate) {
        session.triggerAgeing(runDate);
    }

    @Override
    public boolean isAgeingProcessRunning() {
        return session.isAgeingProcessRunning();
    }

    @Override
    public ProcessStatusWS getAgeingProcessStatus() {
        return session.getAgeingProcessStatus();
    }

    @Override
    public BillingProcessConfigurationWS getBillingProcessConfiguration() {
        return session.getBillingProcessConfiguration();
    }

    @Override
    public Integer createUpdateBillingProcessConfiguration(BillingProcessConfigurationWS ws) {
        return session.createUpdateBillingProcessConfiguration(ws);
    }

    @Override
    public Integer createUpdateCommissionProcessConfiguration(CommissionProcessConfigurationWS ws){
        return session.createUpdateCommissionProcessConfiguration(ws);
    }

    @Override
    public void calculatePartnerCommissions(){
        session.calculatePartnerCommissions();
    }

    @Override
    public void calculatePartnerCommissionsAsync(){
        session.calculatePartnerCommissionsAsync();
    }

    @Override
    public boolean isPartnerCommissionRunning() {
        return session.isPartnerCommissionRunning();
    }

    @Override
    public CommissionProcessRunWS[] getAllCommissionRuns(){
        return session.getAllCommissionRuns();
    }

    @Override
    public CommissionWS[] getCommissionsByProcessRunId(Integer processRunId){
        return session.getCommissionsByProcessRunId(processRunId);
    }

    @Override
    public BillingProcessWS getBillingProcess(Integer processId) {
        return session.getBillingProcess(processId);
    }

    @Override
    public Integer getLastBillingProcess() {
        return session.getLastBillingProcess();
    }

    @Override
    public  OrderProcessWS[] getOrderProcesses(Integer orderId) {
        return session.getOrderProcesses(orderId);
    }

    @Override
    public OrderProcessWS[] getOrderProcessesByInvoice(Integer invoiceId) {
        return session.getOrderProcessesByInvoice(invoiceId);
    }

    @Override
    public BillingProcessWS getReviewBillingProcess() {
        return session.getReviewBillingProcess();
    }

    @Override
    public BillingProcessConfigurationWS setReviewApproval(Boolean flag) {
        return session.setReviewApproval(flag);
    }

    @Override
    public Integer[] getBillingProcessGeneratedInvoices(Integer processId) {
        return session.getBillingProcessGeneratedInvoices(processId);
    }

    @Override
    public AgeingWS[] getAgeingConfiguration(Integer languageId) {
        return session.getAgeingConfiguration(languageId);
    }

    @Override
    public void saveAgeingConfiguration(AgeingWS[] steps, Integer languageId) {
        session.saveAgeingConfiguration(steps, languageId);
    }

    /*
       Mediation process
     */

    @Override
    public void triggerMediation() {
        session.triggerMediation();
    }

    @Override
    public void undoMediation(UUID processId) {
        session.undoMediation(processId);
    }

    @Override
    public UUID triggerMediationByConfiguration(Integer cfgId) {
        return session.triggerMediationByConfiguration(cfgId);
    }

    @Override
    public UUID launchMediation(Integer mediationCfgId, String jobName, File file){
        return session.launchMediation(mediationCfgId, jobName, file);
    }

    @Override
    public boolean isMediationProcessRunning() {
        return session.isMediationProcessRunning();
    }

    @Override
    public ProcessStatusWS getMediationProcessStatus() {
        return session.getMediationProcessStatus();
    }

    @Override
    public MediationProcess getMediationProcess(UUID mediationProcessId) {
        return session.getMediationProcess(mediationProcessId);
    }

    @Override
    public MediationProcess[] getAllMediationProcesses() {
        return session.getAllMediationProcesses();
    }

    @Override
    public JbillingMediationRecord[] getMediationEventsForOrder(Integer orderId) {
        return session.getMediationEventsForOrder(orderId);
    }

    @Override
    public JbillingMediationRecord[] getMediationEventsForOrderDateRange(Integer orderId,
            Date startDate, Date endDate, int offset, int limit) {
        return session.getMediationEventsForOrderDateRange(orderId, startDate, endDate, offset, limit);
    }

    @Override
    public JbillingMediationRecord[] getMediationEventsForInvoice(Integer invoiceId) {
        return session.getMediationEventsForInvoice(invoiceId);
    }

    @Override
    public JbillingMediationRecord[] getMediationRecordsByMediationProcess(UUID mediationProcessId, Integer page, Integer size, Date startDate, Date endDate) {
        return session.getMediationRecordsByMediationProcess(mediationProcessId, page, size, startDate, endDate);
    }

    @Override
    public JbillingMediationRecord[] getMediationRecordsByStatusAndCdrType(UUID mediationProcessId, Integer page, Integer size, Date startDate, Date endDate, String status, String cdrType){
        return session.getMediationRecordsByStatusAndCdrType(mediationProcessId, page, size, startDate, endDate, status, cdrType);
    }

    @Override
    public RecordCountWS[] getNumberOfMediationRecordsByStatuses() {
        return session.getNumberOfMediationRecordsByStatuses();
    }

    @Override
    public RecordCountWS[] getNumberOfMediationRecordsByStatusesByMediationProcess(UUID mediationProcess){
        return session.getNumberOfMediationRecordsByStatusesByMediationProcess(mediationProcess);
    }

    @Override
    public MediationConfigurationWS[] getAllMediationConfigurations() {
        return session.getAllMediationConfigurations();
    }

    @Override
    public Integer createMediationConfiguration(MediationConfigurationWS cfg) {
        return session.createMediationConfiguration(cfg);
    }

    @Override
    public Integer[] updateAllMediationConfigurations(List<MediationConfigurationWS> configurations) {
        return session.updateAllMediationConfigurations(configurations);
    }

    @Override
    public void deleteMediationConfiguration(Integer cfgId) {
        session.deleteMediationConfiguration(cfgId);
    }

    @Override
    public JbillingMediationErrorRecord[] getMediationErrorRecordsByMediationProcess(UUID mediationProcessId, Integer mediationRecordStatusId) {
        return session.getMediationErrorRecordsByMediationProcess(mediationProcessId, mediationRecordStatusId);
    }

    /*
       Provisioning process
     */

    @Override
    public void triggerProvisioning() {
        session.triggerProvisioning();
    }

    @Override
    public void updateOrderAndLineProvisioningStatus(Integer inOrderId, Integer inLineId, String result) {
        session.updateOrderAndLineProvisioningStatus(inOrderId, inLineId, result);
    }

    @Override
    public void updateLineProvisioningStatus(Integer orderLineId, Integer provisioningStatus) {
        session.updateLineProvisioningStatus(orderLineId, provisioningStatus);
    }


    @Override
    public ProvisioningCommandWS[] getProvisioningCommands(ProvisioningCommandType typeId, Integer Id) {
        return session.getProvisioningCommands(typeId, Id);
    }

    @Override
    public ProvisioningCommandWS getProvisioningCommandById(Integer provisioningCommandId) {
        return session.getProvisioningCommandById(provisioningCommandId);
    }

    @Override
    public ProvisioningRequestWS[] getProvisioningRequests(Integer provisioningCommandId) {
        return session.getProvisioningRequests(provisioningCommandId);
    }

    @Override
    public ProvisioningRequestWS getProvisioningRequestById(Integer provisioningRequestId) {
        return session.getProvisioningRequestById(provisioningRequestId);
    }

    /*
        Preferences
     */

    @Override
    public void updatePreferences(PreferenceWS[] prefList) {
        session.updatePreferences(prefList);
    }

    @Override
    public void updatePreference(PreferenceWS preference) {
        session.updatePreference(preference);
    }

    @Override
    public PreferenceWS getPreference(Integer preferenceTypeId) {
        return session.getPreference(preferenceTypeId);
    }


    /*
        Currencies
     */

    @Override
    public CurrencyWS[] getCurrencies() {
        return session.getCurrencies();
    }

    @Override
    public void updateCurrencies(CurrencyWS[] currencies) {
        session.updateCurrencies(currencies);
    }

    @Override
    public void updateCurrency(CurrencyWS currency) {
        session.updateCurrency(currency);
    }

    @Override
    public Integer createCurrency(CurrencyWS currency) {
        return session.createCurrency(currency);
    }


    /*
       Plug-ins
     */

    @Override
    public PluggableTaskWS getPluginWS(Integer pluginId) {
        return session.getPluginWS(pluginId);
    }

    @Override
    public PluggableTaskWS[] getPluginsWS(Integer entityId, String className){
        return session.getPluginsWS(entityId, className);
    }

    @Override
    public Integer createPlugin(PluggableTaskWS plugin) {
        return session.createPlugin(plugin);
    }

    @Override
    public void updatePlugin(PluggableTaskWS plugin) {
        session.updatePlugin(plugin);
    }

    @Override
    public void deletePlugin(Integer plugin) {
        session.deletePlugin(plugin);
    }

    @Override
    public PluggableTaskWS getPluginWSByTypeId(Integer typeId) {
        return session.getPluginWSByTypeId(typeId);
    }

    /*
     * Quartz jobs
     */
    @Override
    public void rescheduleScheduledPlugin(Integer pluginId) {
        session.rescheduleScheduledPlugin(pluginId);
    }

    @Override
    public void triggerScheduledTask(Integer pluginId, Date date){session.triggerScheduledTask(pluginId, date);}


    /*
        Plans and special pricing
     */

    @Override
    public PlanWS getPlanWS(Integer planId) {
        return session.getPlanWS(planId);
    }

    @Override
    public PlanWS[] getAllPlans() {
        return session.getAllPlans();
    }

    @Override
    public Integer createPlan(PlanWS plan) {
        return session.createPlan(plan);
    }

    @Override
    public void updatePlan(PlanWS plan) {
        session.updatePlan(plan);
    }

    @Override
    public void deletePlan(Integer planId) {
        session.deletePlan(planId);
    }

    @Override
    public void addPlanPrice(Integer planId, PlanItemWS price) {
        session.addPlanPrice(planId, price);
    }

    @Override
    public boolean isCustomerSubscribed(Integer planId, Integer userId) {
        return session.isCustomerSubscribed(planId, userId);
    }

    @Override
    public boolean isCustomerSubscribedForDate(Integer planId, Integer userId, Date eventDate) {
        return session.isCustomerSubscribedForDate(planId, userId, eventDate);
    }

    @Override
    public Integer[] getSubscribedCustomers(Integer planId) {
        return session.getSubscribedCustomers(planId);
    }

    @Override
    public Integer[] getPlansBySubscriptionItem(Integer itemId) {
        return session.getPlansBySubscriptionItem(itemId);
    }

    @Override
    public Integer[] getPlansByAffectedItem(Integer itemId) {
        return session.getPlansByAffectedItem(itemId);
    }

    @Override
    public Usage getItemUsage(Integer excludedOrderId, Integer itemId, Integer owner, List<Integer> userIds , Date startDate, Date endDate) {
        return session.getItemUsage(excludedOrderId, itemId, owner, userIds, startDate, endDate);
    }

    @Override
    public PlanItemWS createCustomerPrice(Integer userId, PlanItemWS planItem, Date expiryDate) {
        return session.createCustomerPrice(userId, planItem, expiryDate);
    }

    @Override
    public void updateCustomerPrice(Integer userId, PlanItemWS planItem, Date expiryDate) {
        session.updateCustomerPrice(userId, planItem, expiryDate);
    }

    @Override
    public void deleteCustomerPrice(Integer userId, Integer planItemId) {
        session.deleteCustomerPrice(userId, planItemId);
    }

    @Override
    public PlanItemWS[] getCustomerPrices(Integer userId) {
        return session.getCustomerPrices(userId);
    }

    @Override
    public PlanItemWS getCustomerPrice(Integer userId, Integer itemId) {
        return session.getCustomerPrice(userId, itemId);
    }

    @Override
    public PlanItemWS getCustomerPriceForDate(Integer userId, Integer itemId, Date pricingDate, Boolean planPricingOnly) {
        return session.getCustomerPriceForDate(userId, itemId, pricingDate, planPricingOnly);
    }

    /*
        Assets
     */

    @Override
    public Integer createAsset(AssetWS asset) {
        return session.createAsset(asset);
    }

    @Override
    public void updateAsset(AssetWS asset) {
        session.updateAsset(asset);
    }

    @Override
    public AssetWS getAsset(Integer assetId) {
        return session.getAsset(assetId);
    }

    @Override
    public AssetWS getAssetByIdentifier(String assetIdentifier) {
        return session.getAssetByIdentifier(assetIdentifier);
    }

    @Override
    public void deleteAsset(Integer assetId) {
        session.deleteAsset(assetId);
    }

    @Override
    public AssetWS[] findAssetsForOrderChanges(Integer[] ids) throws SessionInternalError {
        return session.findAssetsForOrderChanges(ids);
    }
    @Override
    public Integer[] getAssetsForCategory(Integer categoryId) {
        return session.getAssetsForCategory(categoryId);
    }

    @Override
    public Integer[] getAssetsForItem(Integer itemId) {
        return session.getAssetsForItem(itemId);
    }

    @Override
    public AssetTransitionDTOEx[] getAssetTransitions(Integer assetId) {
        return session.getAssetTransitions(assetId);
    }

    @Override
    public Long startImportAssetJob(int itemId, String idColumnName, String notesColumnName, String globalColumnName,String entitiesColumnName, String sourceFilePath, String errorFilePath) {
        return session.startImportAssetJob(itemId, idColumnName, notesColumnName, globalColumnName, entitiesColumnName, sourceFilePath, errorFilePath);
    }

    @Override
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria) {
        return session.findAssets(productId, criteria);
    }

    @Override
    public AssetSearchResult findProductAssetsByStatus(int productId, SearchCriteria criteria) {
        return session.findProductAssetsByStatus(productId, criteria);
    }

    @Override
    public AssetAssignmentWS[] getAssetAssignmentsForAsset(Integer assetId) {
        return session.getAssetAssignmentsForAsset(assetId);
    }

    @Override
    public AssetAssignmentWS[] getAssetAssignmentsForOrder(Integer orderId) {
        return session.getAssetAssignmentsForOrder(orderId);
    }

    @Override
    public Integer findOrderForAsset(Integer assetId, Date date) {
        return session.findOrderForAsset(assetId, date);
    }

    @Override
    public Integer[] findOrdersForAssetAndDateRange(Integer assetId, Date startDate, Date endDate) {
        return session.findOrdersForAssetAndDateRange(assetId, startDate, endDate);
    }

    public AssetWS[] findAssetsByProductCode(String productCode){
        return session.findAssetsByProductCode(productCode);
    }
    @Override
    public AssetStatusDTOEx[] findAssetStatuses(String identifier){
        return session.findAssetStatuses(identifier);
    }
    public AssetWS findAssetByProductCodeAndIdentifier(String productCode, String identifier){
        return session.findAssetByProductCodeAndIdentifier(productCode, identifier);
    }

    public AssetWS[] findAssetsByProductCodeAndStatus(String productCode, Integer assetStatusId){
        return session.findAssetsByProductCodeAndStatus(productCode, assetStatusId);
    }

    @Override
    public PlanItemWS createAccountTypePrice(Integer accountTypeId, PlanItemWS planItem, Date expiryDate) {
        return session.createAccountTypePrice(accountTypeId, planItem, expiryDate);
    }

    @Override
    public void updateAccountTypePrice(Integer accountTypeId, PlanItemWS planItem, Date expiryDate) {
        session.updateAccountTypePrice(accountTypeId, planItem, expiryDate);
    }

    @Override
    public void deleteAccountTypePrice(Integer accountTypeId, Integer planItemId) {
        session.deleteAccountTypePrice(accountTypeId, planItemId);
    }

    @Override
    public PlanItemWS[] getAccountTypePrices(Integer accountTypeId) {
        return session.getAccountTypePrices(accountTypeId);
    }

    @Override
    public PlanItemWS getAccountTypePrice(Integer accountTypeId, Integer itemId) {
        return session.getAccountTypePrice(accountTypeId, itemId);
    }

    @Override
    public BigDecimal getTotalRevenueByUser(Integer userId) {
        return session.getTotalRevenueByUser(userId);
    }

    @Override
    public CompanyWS getCompany() {
        return session.getCompany();
    }

    @Override
    public CompanyWS[] getCompanies() {
        return session.getCompanies();
    }

    @Override
    public Integer getCallerCompanyId() {
        return session.getCallerCompanyId();
    }

    @Override
    public Integer getCallerId() {
        return session.getCallerId();
    }

    @Override
    public Integer getCallerLanguageId() {
        return session.getCallerLanguageId();
    }

    @Override
    public Integer getCallerCurrencyId() {
        return session.getCallerCurrencyId();
    }

    @Override
    public InvoiceWS[] getAllInvoicesForUser(Integer userId) {
        return session.getAllInvoicesForUser(userId);
    }

    @Override
    public OrderWS[] getUserSubscriptions(Integer userId) {
        return session.getUserSubscriptions(userId);
    }

    @Override
    public boolean deleteOrderPeriod(Integer periodId) {
        return session.deleteOrderPeriod(periodId);
    }

    @Override
    public boolean isBillingRunning(Integer entityId) {
        return session.isBillingRunning(entityId);
    }
    @Override
    public boolean updateOrderPeriods(OrderPeriodWS[] orderPeriods) {
        return session.updateOrderPeriods(orderPeriods);
    }

    @Override
    public boolean updateOrCreateOrderPeriod(OrderPeriodWS orderPeriod) {
        return session.updateOrCreateOrderPeriod(orderPeriod);
    }

    @Override
    public Integer createAccountType(AccountTypeWS accountType){
        return session.createAccountType(accountType);
    }
    @Override
    public boolean updateAccountType(AccountTypeWS accountType) {
        return session.updateAccountType(accountType);
    }
    @Override
    public AccountTypeWS getAccountType(Integer accountTypeId) {
        return session.getAccountType(accountTypeId);
    }
    @Override
    public AccountTypeWS[] getAllAccountTypes() {
        return session.getAllAccountTypes();
    }
    @Override
    public AccountTypeWS[] getAllAccountTypesByCompanyId(Integer companyId) {
        return session.getAllAccountTypesByCompanyId(companyId);
    }

    @Override
    public boolean deleteAccountType(Integer accountTypeId){
        return session.deleteAccountType(accountTypeId);
    }

    @Override
    public AccountInformationTypeWS[] getInformationTypesForAccountType(Integer accountTypeId) {
        return session.getInformationTypesForAccountType(accountTypeId);
    }

    @Override
    public Integer createAccountInformationType(AccountInformationTypeWS accountInformationType) {
        return session.createAccountInformationType(accountInformationType);
    }

    @Override
    public void updateAccountInformationType(AccountInformationTypeWS accountInformationType) {
        session.updateAccountInformationType(accountInformationType);
    }

    @Override
    public boolean deleteAccountInformationType(Integer accountInformationTypeId) {
        return session.deleteAccountInformationType(accountInformationTypeId);
    }

    @Override
    public AccountInformationTypeWS getAccountInformationType(Integer accountInformationType) {
        return session.getAccountInformationType(accountInformationType);
    }


    @Override
    public void createUpdateNotification(Integer messageId, MessageDTO dto) {
        session.createUpdateNotification(messageId, dto);
    }


    @Override
    public void updateCompany(CompanyWS companyWS) {
        session.updateCompany(companyWS);
    }

    @Override
    public Integer createOrUpdateDiscount(DiscountWS discount) {
        return session.createOrUpdateDiscount(discount);
    }
    @Override
    public DiscountWS getDiscountWS(Integer discountId) {
        return session.getDiscountWS(discountId);
    }
    @Override
    public DiscountWS getDiscountWSByCode(String discountCode) {
        return session.getDiscountWSByCode(discountCode);
    }
    @Override
    public void deleteDiscount(Integer discountId) {
        session.deleteDiscount(discountId);
    }

    @Override
    public ProcessStatusWS getBillingProcessStatus() {
        return session.getBillingProcessStatus();
    }

    @Override
    public OrderWS processJMRData(
            UUID processId, String recordKey, Integer userId,
            Integer currencyId, Date eventDate, String description,
            Integer productId, String quantity, String pricing) {

        return session.processJMRData(processId, recordKey, userId, currencyId, eventDate, description, productId, quantity, pricing);
    }

    @Override
    public OrderWS processJMRRecord(UUID processId, JbillingMediationRecord JMR) {
        return session.processJMRRecord(processId, JMR);
    }

    @Override
    public UUID processCDR(Integer configId, List<String> callDataRecords) {
        return session.processCDR(configId, callDataRecords);
    }

    @Override
    public UUID processCDRChecked(Integer configId, List<String> callDataRecords) {
        return session.processCDRChecked(configId, callDataRecords);
    }

    @Override
    public UUID runRecycleForConfiguration(Integer configId) {
        return session.runRecycleForConfiguration(configId);
    }

    @Override
    public UUID runRecycleForProcess(UUID processId) {
        return session.runRecycleForMediationProcess(processId);
    }

    @Override
    public void createCustomerNote(CustomerNoteWS note)
    {
        session.createCustomerNote(note);
    }

    @Override
    public Integer createMetaFieldGroup(MetaFieldGroupWS metafieldGroup) {
        return  session.createMetaFieldGroup(metafieldGroup);
    }

    @Override
    public MetaFieldGroupWS getMetaFieldGroup(Integer metafieldGroupId) {
        return session.getMetaFieldGroup(metafieldGroupId);

    }

    @Override
    public void updateMetaFieldGroup(MetaFieldGroupWS metafieldGroupWs) {
        session.updateMetaFieldGroup(metafieldGroupWs);

    }


    @Override
    public void deleteMetaFieldGroup(Integer metafieldGroupId) {
        session.deleteMetaFieldGroup(metafieldGroupId);

    }

    @Override
    public Integer createMetaField(MetaFieldWS metafieldWS) {
        return  session.createMetaField(metafieldWS);
    }

    @Override
    public void updateMetaField(MetaFieldWS metafieldWs){
        session.updateMetaField(metafieldWs);
    }

    @Override
    public void deleteMetaField(Integer metafieldId){
        session.deleteMetaField(metafieldId);

    }

    @Override
    public MetaFieldWS getMetaField(Integer metafieldId){
        return session.getMetaField(metafieldId);
    }

    @Override
    public MetaFieldGroupWS[] getMetaFieldGroupsForEntity(String entityType) {
        return session.getMetaFieldGroupsForEntity(entityType);
    }

    @Override
    public MetaFieldWS[] getMetaFieldsForEntity(String entityType) {
        return session.getMetaFieldsForEntity(entityType);
    }

    @Override
    public OrderPeriodWS[] getOrderPeriods(){
        return session.getOrderPeriods();
    }
    /*
       Diameter Protocol
     */

    @Override
    public OrderChangeStatusWS[] getOrderChangeStatusesForCompany() {
        return session.getOrderChangeStatusesForCompany();
    }

    @Override
    public Integer createOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) throws SessionInternalError {
        return session.createOrderChangeStatus(orderChangeStatusWS);
    }

    @Override
    public void updateOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) throws SessionInternalError {
        session.updateOrderChangeStatus(orderChangeStatusWS);
    }

    @Override
    public void deleteOrderChangeStatus(Integer id) throws SessionInternalError {
        session.deleteOrderChangeStatus(id);
    }

    @Override
    public void saveOrderChangeStatuses(OrderChangeStatusWS[] orderChangeStatuses) throws SessionInternalError {
        session.saveOrderChangeStatuses(orderChangeStatuses);
    }

    @Override
    public OrderChangeWS[] getOrderChanges(Integer orderId) {
        return session.getOrderChanges(orderId);
    }

    @Override
    public OrderChangeTypeWS[] getOrderChangeTypesForCompany() {
        return session.getOrderChangeTypesForCompany();
    }

    @Override
    public OrderChangeTypeWS getOrderChangeTypeByName(String name) {
        return session.getOrderChangeTypeByName(name);
    }

    @Override
    public OrderChangeTypeWS getOrderChangeTypeById(Integer orderChangeTypeId) {
        return session.getOrderChangeTypeById(orderChangeTypeId);
    }

    @Override
    public Integer createUpdateOrderChangeType(OrderChangeTypeWS orderChangeTypeWS) {
        return session.createUpdateOrderChangeType(orderChangeTypeWS);
    }

    @Override
    public void deleteOrderChangeType(Integer orderChangeTypeId) {
        session.deleteOrderChangeType(orderChangeTypeId);
    }

    @Override
    public Integer createUsagePool(UsagePoolWS usagePool) {
        return session.createUsagePool(usagePool);
    }

    @Override
    public void updateUsagePool(UsagePoolWS usagePool) {
        session.updateUsagePool(usagePool);
    }
    @Override
    public UsagePoolWS getUsagePoolWS(Integer usagePoolId) {
        return session.getUsagePoolWS(usagePoolId);
    }
    @Override
    public boolean deleteUsagePool(Integer usagePoolId) {
        return session.deleteUsagePool(usagePoolId);
    }

    @Override
    public UsagePoolWS[] getAllUsagePools() {
        return session.getAllUsagePools();
    }

    @Override
    public UsagePoolWS[] getUsagePoolsByPlanId(Integer planId) {
        return session.getUsagePoolsByPlanId(planId);
    }

    @Override
    public CustomerUsagePoolWS getCustomerUsagePoolById(Integer customerUsagePoolId) {
        return session.getCustomerUsagePoolById(customerUsagePoolId);
    }

    @Override
    public CustomerUsagePoolWS[] getCustomerUsagePoolsByCustomerId(Integer customerId) {
        return session.getCustomerUsagePoolsByCustomerId(customerId);
    }

    /*
       Diameter Protocol
     */

    @Override
    public DiameterResultWS createSession(String sessionId, Date timestamp, BigDecimal units,
            PricingField[] data) throws SessionInternalError {
        return session.createSession(sessionId, timestamp, units,
                PricingField.setPricingFieldsValue(data));
    }

    @Override
    public DiameterResultWS reserveUnits(String sessionId, Date timestamp, int units,
            PricingField[] data) throws SessionInternalError {
        return session.reserveUnits(sessionId, timestamp, units,
                PricingField.setPricingFieldsValue(data));
    }

    @Override
    public DiameterResultWS updateSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            BigDecimal reqUnits, PricingField[] data) throws SessionInternalError {
        return session.updateSession(sessionId, timestamp, usedUnits, reqUnits,
                PricingField.setPricingFieldsValue(data));
    }

    @Override
    public DiameterResultWS extendSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            BigDecimal reqUnits) throws SessionInternalError {
        return session.extendSession(sessionId, timestamp, usedUnits, reqUnits);
    }

    @Override
    public DiameterResultWS endSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            int causeCode) throws SessionInternalError {
        return session.endSession(sessionId, timestamp, usedUnits, causeCode);
    }

    @Override
    public DiameterResultWS consumeReservedUnits(String sessionId, Date timestamp, int usedUnits,
            int causeCode) throws SessionInternalError {
        return session.consumeReservedUnits(sessionId, timestamp, usedUnits, causeCode);
    }

    @Override
    public Integer createRoute(RouteWS routeWS, File routeFile) {
        return session.createRoute(routeWS,routeFile);
    }

    @Override
    public void deleteRoute(Integer routeId) {
        session.deleteRoute(routeId);
    }

    @Override
    public RouteWS getRoute(Integer routeId) {
        return session.getRoute(routeId);
    }

    @Override
    public Integer createMatchingField(MatchingFieldWS matchingFieldWS) {
        return session.createMatchingField(matchingFieldWS);
    }


    @Override
    public void deleteMatchingField(Integer matchingFieldId) {
        session.deleteMatchingField(matchingFieldId);
    }

    @Override
    public MatchingFieldWS getMatchingField(Integer matchingFieldId) {
        return  session.getMatchingField(matchingFieldId);
    }

    @Override
    public boolean updateMatchingField(MatchingFieldWS matchingFieldWS){
        return  session.updateMatchingField(matchingFieldWS);
    }


    @Override
    public Integer createRouteRateCard(RouteRateCardWS routeRateCardWS, File routeRateCardFile) {
        return session.createRouteRateCard(routeRateCardWS,routeRateCardFile);
    }

    @Override
    public void deleteRouteRateCard(Integer routeId) {
        session.deleteRouteRateCard(routeId);
    }
    @Override
    public void updateRouteRateCard(RouteRateCardWS routeRateCardWS, File routeRateCardFile){
        session.updateRouteRateCard(routeRateCardWS,routeRateCardFile);
    }

    @Override
    public RouteRateCardWS getRouteRateCard(Integer routeRateCardId) {
        return session.getRouteRateCard(routeRateCardId);
    }

    @Override
    public Integer createRouteRecord(RouteRecordWS record, Integer routeId) {
        return session.createRouteRecord(record, routeId );
    }

    @Override
    public void updateRouteRecord(RouteRecordWS record, Integer routeId) {
        session.updateRouteRecord(record, routeId);
    }

    @Override
    public void deleteRouteRecord(Integer routeId, Integer recordId) {
        session.deleteRouteRecord(routeId, recordId);
    }

    @Override
    public SearchResultString searchDataTable(Integer routeId, SearchCriteria criteria) {
        return session.searchDataTable(routeId, criteria);
    }

    @Override
    public Set<String> searchDataTableWithFilter(Integer routeId, String filters, String searchName) {
        return session.searchDataTableWithFilter(routeId, filters, searchName);
    }

    @Override
    public String getRouteTable(Integer routeId) {
        return session.getRouteTable(routeId);
    }

    @Override
    public Integer createDataTableQuery(DataTableQueryWS queryWS) {
        return session.createDataTableQuery(queryWS);
    }

    @Override
    public DataTableQueryWS getDataTableQuery(int id) {
        return session.getDataTableQuery(id);
    }

    @Override
    public void deleteDataTableQuery(int id) {
        session.deleteDataTableQuery(id);
    }

    @Override
    public DataTableQueryWS[] findDataTableQueriesForTable(int routeId) {
        return session.findDataTableQueriesForTable(routeId);
    }

    @Override
    public Integer createRatingUnit(RatingUnitWS ratingUnitWS) {
        return session.createRatingUnit(ratingUnitWS);
    }

    @Override
    public void updateRatingUnit(RatingUnitWS ratingUnitWS) {
        session.updateRatingUnit(ratingUnitWS);
    }

    @Override
    public boolean deleteRatingUnit(Integer ratingUnitId) {
        return session.deleteRatingUnit(ratingUnitId);
    }

    @Override
    public RatingUnitWS getRatingUnit(Integer ratingUnitId) {
        return session.getRatingUnit(ratingUnitId);
    }

    @Override
    public RatingUnitWS[] getAllRatingUnits() {
        return session.getAllRatingUnits();
    }

    /*
     *Payment Method
     */
    @Override
    public PaymentMethodTemplateWS getPaymentMethodTemplate(Integer templateId) {
        return session.getPaymentMethodTemplate(templateId);
    }

    @Override
    public Integer createPaymentMethodType(PaymentMethodTypeWS paymentMethod) {
        return session.createPaymentMethodType(paymentMethod);
    }
    @Override
    public void updatePaymentMethodType(PaymentMethodTypeWS paymentMethod){
        session.updatePaymentMethodType(paymentMethod);
    }
    @Override
    public boolean deletePaymentMethodType(Integer paymentMethodTypeId){
        return session.deletePaymentMethodType(paymentMethodTypeId);
    }

    @Override
    public PaymentMethodTypeWS getPaymentMethodType(Integer paymentMethodTypeId) {
        return session.getPaymentMethodType(paymentMethodTypeId);
    }

    @Override
    public boolean removePaymentInstrument(Integer instrumentId) {
        return session.removePaymentInstrument(instrumentId);
    }

    /* Customizable order status 7375 */
    @Override
    public void deleteOrderStatus(OrderStatusWS orderStatus) {
        session.deleteOrderStatus(orderStatus);
    }

    @Override
    public Integer createUpdateOrderStatus(OrderStatusWS orderStatusWS)
            throws SessionInternalError {
        return session.createUpdateOrderStatus(orderStatusWS);
    }

    @Override
    public OrderStatusWS findOrderStatusById(Integer orderStatusId) {
        return session.findOrderStatusById(orderStatusId);
    }

    @Override
    public int getDefaultOrderStatusId(OrderStatusFlag flag, Integer entityId){
        return session.getDefaultOrderStatusId(flag, entityId);
    }

    @Override
    public PluggableTaskTypeWS getPluginTypeWS(Integer id) {
        return session.getPluginTypeWS(id);
    }

    @Override
    public PluggableTaskTypeWS getPluginTypeWSByClassName(String className) {
        return session.getPluginTypeWSByClassName(className);
    }

    @Override
    public PluggableTaskTypeCategoryWS getPluginTypeCategory(Integer id) {
        return session.getPluginTypeCategory(id);
    }

    @Override
    public PluggableTaskTypeCategoryWS getPluginTypeCategoryByInterfaceName(
            String interfaceName) {
        return session.getPluginTypeCategoryByInterfaceName(interfaceName);
    }

    @Override
    public Integer[] createSubscriptionAccountAndOrder(Integer parentAccountId,
            OrderWS order, boolean createInvoice, List<OrderChangeWS> orderChanges) {
        return session.createSubscriptionAccountAndOrder(parentAccountId, order, createInvoice, orderChanges);
    }

    @Override
    public OrderPeriodWS getOrderPeriodWS(Integer orderPeriodId) {
        return session.getOrderPeriodWS(orderPeriodId);
    }

    @Override
    public Integer createOrderPeriod(OrderPeriodWS orderPeriod) {
        return session.createOrderPeriod(orderPeriod);
    }

    @Override
    public UserWS getUserByCustomerMetaField(String metaFieldValue, String metaFieldName) {
        return session.getUserByCustomerMetaField(metaFieldValue, metaFieldName);
    }

    @Override
    public UserWS getUserByCustomerMetaFieldAndCompanyId(String metaFieldValue, String metaFieldName, Integer callerCompanyId) {
        return session.getUserByCustomerMetaFieldAndCompanyId(metaFieldValue, metaFieldName, callerCompanyId);
    }

    @Override
    public Long getMediationErrorRecordsCount(Integer mediationConfigurationId){
        return session.getMediationErrorRecordsCount(mediationConfigurationId);
    }

    @Override
    public Integer reserveAsset(Integer assetId, Integer userId) {
        return this.session.reserveAsset(assetId, userId);
    }

    @Override
    public void releaseAsset(Integer assetId, Integer userId) {
        this.session.releaseAsset(assetId, userId);
    }


    // Enumerations

    @Override
    public EnumerationWS getEnumeration(Integer enumerationId) throws SessionInternalError {
        return session.getEnumeration(enumerationId);
    }

    @Override
    public EnumerationWS getEnumerationByName(String name) throws SessionInternalError {
        return session.getEnumerationByName(name);
    }

    @Override
    public EnumerationWS getEnumerationByNameAndCompanyId(String name, Integer companyId) throws SessionInternalError {
        return session.getEnumerationByNameAndCompanyId(name, companyId);
    }

    @Override
    public List<EnumerationWS> getAllEnumerations(Integer max, Integer offset) {
        return session.getAllEnumerations(max, offset);
    }

    @Override
    public Long getAllEnumerationsCount(){
        return session.getAllEnumerationsCount();
    }

    @Override
    public Integer createUpdateEnumeration(EnumerationWS enumerationWS) throws SessionInternalError{
        return session.createUpdateEnumeration(enumerationWS);
    }

    @Override
    public boolean deleteEnumeration(Integer enumerationId) throws SessionInternalError{
        return session.deleteEnumeration(enumerationId);
    }

    @Override
    public UserWS copyCompanyInSaas(String templateForChildCompany, Integer entityId, List<String> importEntities,
            boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail, String systemAdminLoginName) {
        return session.copyCompanyInSaas(templateForChildCompany, entityId,importEntities, isCompanyChild, copyProducts, copyPlans, adminEmail, systemAdminLoginName);
    }

    @Override
    public UserWS createUserWithCIMProfileValidation(UserWS newUser) throws SessionInternalError{
        return session.createUserWithCIMProfileValidation(newUser);
    }

    @Override
    public UserWS updateUserWithCIMProfileValidation(UserWS newUser) throws SessionInternalError {
        return session.updateUserWithCIMProfileValidation(newUser);
    }

    @Override
    public void applyPaymentsToInvoices(Integer userId) throws SessionInternalError {
        session.applyPaymentsToInvoices(userId);
    }

    @Override
    public void processMigrationPayment(PaymentWS paymentWS) throws SessionInternalError {
        session.processMigrationPayment(paymentWS);
    }

    @Override
    public MediationRatingSchemeWS getRatingScheme(Integer mediationRatingSchemeId) throws SessionInternalError {
        return session.getRatingScheme(mediationRatingSchemeId);
    }

    @Override
    public MediationRatingSchemeWS[] getRatingSchemesForEntity () {
        return session.getRatingSchemesForEntity ();
    }

    @Override
    public MediationRatingSchemeWS[] getRatingSchemesPagedForEntity (Integer max, Integer offset){
        return session.getRatingSchemesPagedForEntity(max, offset);
    }

    @Override
    public Long countRatingSchemesPagedForEntity(){
        return session.countRatingSchemesPagedForEntity();
    }

    @Override
    public boolean deleteRatingScheme(Integer ratingSchemeId) throws SessionInternalError {
        return session.deleteRatingScheme(ratingSchemeId);
    }

    @Override
    public Integer createRatingScheme(MediationRatingSchemeWS ws) throws SessionInternalError {
        return session.createRatingScheme(ws);
    }

    @Override
    public Integer getRatingSchemeForMediationAndCompany(Integer mediationCfgId, Integer companyId) {
        return session.getRatingSchemeForMediationAndCompany(mediationCfgId, companyId);
    }

    @Override
    public BigDecimal getQuantity(Integer ratingSchemeId, Integer callDuration) {
        return session.getQuantity(ratingSchemeId, callDuration);
    }

    @Override
    public Integer[] getPaymentsByUserId(Integer userId) {
        return session.getPaymentsByUserId(userId);
    }

    @Override
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderId(Integer orderId) {
        return session.getSwapPlanHistroyByOrderId(orderId);
    }

    @Override
    public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderAndSwapDate(
            Integer orderId, Date from, Date to) {
        return session.getSwapPlanHistroyByOrderAndSwapDate(orderId, from, to);
    }

    @Override
    public CustomerEnrollmentWS getCustomerEnrollment(Integer customerEnrollmentId) throws SessionInternalError{
        return session.getCustomerEnrollment(customerEnrollmentId);
    }

    @Override
    public Integer createUpdateEnrollment(CustomerEnrollmentWS customerEnrollmentWS) throws SessionInternalError{
        return session.createUpdateEnrollment(customerEnrollmentWS);
    }

    @Override
    public CustomerEnrollmentWS validateCustomerEnrollment(CustomerEnrollmentWS customerEnrollmentWS) throws SessionInternalError{
        return session.validateCustomerEnrollment(customerEnrollmentWS);
    }


    @Override
    public void deleteEnrollment(Integer customerEnrollmentId) throws SessionInternalError{
        session.deleteEnrollment(customerEnrollmentId);
    }

    @Override
    public int generateEDIFile(Integer ediTypeId, Integer entityId, String fileName, Collection input) throws SessionInternalError {
        return session.generateEDIFile(ediTypeId, entityId, fileName, input);
    }

    @Override
    public int parseEDIFile(Integer ediTypeId, Integer entityId, File parserFile) throws SessionInternalError {
        return session.parseEDIFile(ediTypeId, entityId, parserFile);
    }

    @Override
    public Integer createEDIType(EDITypeWS ediTypeWS, File ediFormatFile) {
        return session.createEDIType(ediTypeWS, ediFormatFile);
    }

    @Override
    public void deleteEDIType(Integer ediTypeId) {
        session.deleteEDIType(ediTypeId);
    }

    @Override
    public EDITypeWS getEDIType(Integer ediTypeId) {
        return session.getEDIType(ediTypeId);
    }

    @Override
    public List<Integer> getEDIFiles(Integer ediTypeId, String fieldKey, String fieldValue, TransactionType transactionType, String statusName){
        return session.getEDIFiles(ediTypeId, fieldKey, fieldValue, transactionType, statusName);
    }
    @Override
    public EDIFileWS getEDIFileById(Integer ediFileId){
        return session.getEDIFileById(ediFileId);
    }

    @Override
    public List<OrphanEDIFile> getLDCFiles(TransactionType type){
        return session.getLDCFiles(type);
    }

    @Override
    public File getOrphanLDCFile(TransactionType type, String fileName){
        return session.getOrphanLDCFile(type, fileName);
    }

    @Override
    public void deleteOrphanEDIFile(TransactionType type, List<String> fileName){
        session.deleteOrphanEDIFile(type, fileName);
    }

    @Override
    public void uploadEDIFile(File ediFile) {
        session.uploadEDIFile(ediFile);
    }

    @Override
    public List<CompanyWS> getAllChildEntities(Integer parentId) throws SessionInternalError {
        return session.getAllChildEntities(parentId);
    }

    /*
     * Payment Transfer
     */
    @Override
    public void transferPayment(PaymentTransferWS paymentTransfer) {
        session.transferPayment(paymentTransfer);
    }

    @Override
    public void updateEDIStatus(EDIFileWS ediFileWS, EDIFileStatusWS statusWS, Boolean escapeValidation) throws SessionInternalError{
        session.updateEDIStatus(ediFileWS, statusWS, escapeValidation);
    }

    @Override
    public Integer createAdjustmentOrderAndInvoice(String customerPrimaryAccount, OrderWS order, OrderChangeWS[] orderChanges) {
        return session.createAdjustmentOrderAndInvoice(customerPrimaryAccount, order, orderChanges);
    }

    @Override
    public String createPaymentForHistoricalMigration(
            String customerPrimaryAccount, String primaryAccountMetaFieldName, Integer[][] chequePmMap,
            String amount, String date) {
        return createPaymentForHistoricalMigration(customerPrimaryAccount, primaryAccountMetaFieldName, chequePmMap, amount, date);
    }

    @Override
    public String createPaymentForHistoricalDateMigration(String customerPrimaryAccount, Integer chequePmId, String amount, String date) {
        return session.createPaymentForHistoricalDateMigration(customerPrimaryAccount, chequePmId, amount, date);
    }

    @Override
    public String adjustUserBalance(String customerPrimaryAccount, String amount, Integer chequePmId, String date) {
        return session.adjustUserBalance(customerPrimaryAccount, amount, chequePmId, date);
    }

    @Override
    public EDIFileStatusWS findEdiStatusById(Integer ediStatusId){
        return session.findEdiStatusById(ediStatusId);
    }

    /*
     * Credit Note
     */

    @Override
    public CreditNoteWS[] getAllCreditNotes(Integer entityId) {
        return session.getAllCreditNotes(entityId);
    }

    @Override
    public CreditNoteWS getCreditNote(Integer creditNoteId) {
        return session.getCreditNote(creditNoteId);
    }

    @Override
    public void updateCreditNote(CreditNoteWS creditNoteWs) {
        session.updateCreditNote(creditNoteWs);
    }

    @Override
    public void deleteCreditNote(Integer creditNoteId) {
        session.deleteCreditNote(creditNoteId);
    }

    @Override
    public Integer[] getLastCreditNotes(Integer userId, Integer number) throws SessionInternalError {
        return session.getLastCreditNotes(userId, number);
    }

    @Override
    public void applyCreditNote(Integer creditNoteId) {
        session.applyCreditNote(creditNoteId);
    }

    @Override
    public void applyExistingCreditNotesToUnpaidInvoices(Integer userId) {
        session.applyExistingCreditNotesToUnpaidInvoices(userId);
    }

    @Override
    public void applyExistingCreditNotesToInvoice(Integer invoiceId) {
        session.applyExistingCreditNotesToInvoice(invoiceId);
    }

    @Override
    public void applyCreditNoteToInvoice(Integer creditNoteId, Integer debitInvoiceId) {
        session.applyCreditNoteToInvoice(creditNoteId, debitInvoiceId);
    }

    @Override
    public void removeCreditNoteLink(Integer invoiceId, Integer creditNoteId) {
        session.removeCreditNoteLink(invoiceId, creditNoteId);
    }

    @Override
    public JobExecutionHeaderWS[] getJobExecutionsForDateRange(String jobType,Date startDate, Date endDate, int offset, int limit, String sort, String order ) {
        return session.getJobExecutionsForDateRange(jobType, startDate, endDate, offset, limit, sort, order);
    }

    @Override
    public UUID triggerMediationByConfigurationByFile(Integer cfgId, File file) {
        return session.triggerMediationByConfigurationByFile(cfgId, file);
    }

    @Override
    public JbillingMediationRecord[] getMediationRecordsByMediationProcessAndStatus(String mediationProcessId, Integer statusId) {
        return session.getMediationRecordsByMediationProcessAndStatus(mediationProcessId, statusId);
    }

    @Override
    public JbillingMediationErrorRecord[] getErrorsByMediationProcess(String mediationProcessId, int offset, int limit) {
        return session.getErrorsByMediationProcess(mediationProcessId, offset, limit);
    }

    @Override
    public QueryResultWS getQueryResult(String queryCode, QueryParameterWS[] parameters, Integer limit, Integer offSet) {
        return session.getQueryResult(queryCode, parameters, limit, offSet);
    }

    @Override
    public QueryParameterWS[] getParametersByQueryCode(String queryCode) {
        return session.getParametersByQueryCode(queryCode);
    }

    @Override
    public PagedResultList findOrdersByFilters(int page, int size, String sort, String order, List<Filter> filters) {
        return session.findOrdersByFilters(page, size, sort, order, filters);
    }

    @Override
    public OrderWS[] filterOrders(Integer page, Integer size, Date activeSince, Integer productId, Integer orderStatusId) {
        return session.filterOrders(page, size, activeSince, productId, orderStatusId);
    }

    @Override
    public PagedResultList findProvisioningCommandsByFilters(int page, int size, String sort, String order, List<Filter> filters) {
        return session.findProvisioningCommandsByFilters(page, size, sort, order, filters);
    }

    @Override
    public Integer processSignupPayment(UserWS user, PaymentWS payment){
        return session.processSignupPayment(user,payment);
    }

    /**
     * Invoice Summary
     */
    @Override
    public ItemizedAccountWS getItemizedAccountByInvoiceId(Integer invoiceId) {
        return session.getItemizedAccountByInvoiceId(invoiceId);
    }

    @Override
    public InvoiceSummaryWS getInvoiceSummary(Integer invoiceId) {
        return session.getInvoiceSummary(invoiceId);
    }

    @Override
    public InvoiceLineDTO[] getRecurringChargesByInvoiceId(Integer invoiceId){
        return session.getRecurringChargesByInvoiceId(invoiceId);
    }

    @Override
    public InvoiceLineDTO[] getUsageChargesByInvoiceId(Integer invoiceId){
        return session.getUsageChargesByInvoiceId(invoiceId);
    }

    @Override
    public InvoiceLineDTO[] getFeesByInvoiceId(Integer invoiceId) {
        return session.getFeesByInvoiceId(invoiceId);
    }

    @Override
    public InvoiceLineDTO[] getTaxesByInvoiceId(Integer invoiceId) {
        return session.getTaxesByInvoiceId(invoiceId);
    }

    @Override
    public PaymentWS[] getPaymentsAndRefundsByInvoiceId(Integer invoiceId) {
        return session.getPaymentsAndRefundsByInvoiceId(invoiceId);
    }

    @Override
    public CreditAdjustmentWS[] getCreditAdjustmentsByInvoiceId(Integer invoiceId) {
        return session.getCreditAdjustmentsByInvoiceId(invoiceId);
    }

    /*
     * Cancellation Request
     */
    @Override
    public Integer createCancellationRequest(CancellationRequestWS cancellationRequest) {
        return session.createCancellationRequest(cancellationRequest);
    }
    @Override
    public void updateCancellationRequest(CancellationRequestWS cancellationRequest){
        session.updateCancellationRequest(cancellationRequest);
    }
    @Override
    public CancellationRequestWS[] getAllCancellationRequests(Integer entityId, Date startDate, Date endDate){
        return session.getAllCancellationRequests(entityId, startDate, endDate);
    }
    @Override
    public CancellationRequestWS getCancellationRequestById(Integer cancellationRequestId){
        return session.getCancellationRequestById(cancellationRequestId);
    }
    @Override
    public CancellationRequestWS[] getCancellationRequestsByUserId(Integer userId){
        return session.getCancellationRequestsByUserId(userId);
    }
    @Override
    public void deleteCancellationRequest(Integer cancellationId){
        session.deleteCancellationRequest(cancellationId);
    }

    @Override
    public void updateOrderChangeEndDate(Integer orderChangeId, Date endDate){
        session.updateOrderChangeEndDate(orderChangeId, endDate);
    }

    @Override
    public AgeingWS[] getAgeingConfigurationWithCollectionType(Integer languageId, CollectionType collectionType) {
        return session.getAgeingConfigurationWithCollectionType(languageId, collectionType);
    }

    @Override
    public void saveAgeingConfigurationWithCollectionType(AgeingWS[] steps,Integer languageId, CollectionType collectionType) {
        session.saveAgeingConfigurationWithCollectionType(steps, languageId, collectionType);
    }

    @Override
    public void createUpdateOrderChange(Integer userId, String productCode,
            BigDecimal newPrice, BigDecimal newQuantity, Date changeEffectiveDate) throws SessionInternalError {
        session.createUpdateOrderChange(userId, productCode, newPrice, newQuantity, changeEffectiveDate);
    }







    /* Usage rating scheme */
    @Override
    public Integer createUsageRatingScheme(UsageRatingSchemeWS ws) throws SessionInternalError{
        return session.createUsageRatingScheme(ws);
    }

    @Override
    public boolean deleteUsageRatingScheme(Integer usageRatingSchemeId) throws SessionInternalError{
        return session.deleteUsageRatingScheme(usageRatingSchemeId);
    }

    @Override
    public UsageRatingSchemeWS getUsageRatingScheme(Integer usageRatingSchemeId){
        return session.getUsageRatingScheme(usageRatingSchemeId);
    }

    @Override
    public Long countUsageRatingSchemes (){
        return session.countUsageRatingSchemes ();
    }

    @Override
    public List<UsageRatingSchemeWS> findAllUsageRatingSchemes(){
        return session.findAllUsageRatingSchemes();
    }

    @Override
    public List<UsageRatingSchemeWS> getAllUsageRatingSchemes(Integer max, Integer offset){
        return session.getAllUsageRatingSchemes( max, offset);
    }

    @Override
    public List<UsageRatingSchemeType> findAllRatingSchemeTypeValues(){
        return session.findAllRatingSchemeTypeValues();
    }

    // API user detail
    @Override
    public String createApiUserDetail(ApiUserDetailWS ws) throws SessionInternalError{
        return session.createApiUserDetail(ws);
    }

    @Override
    public Long countApiUserDetails(){
        return session.countApiUserDetails();
    }

    @Override
    public List<ApiUserDetailWS> findAllApiUserDetails(){
        return session.findAllApiUserDetails();
    }

    @Override
    public List<ApiUserDetailWS> getAllApiUserDetails(Integer max, Integer offset){
        return session.getAllApiUserDetails(max, offset);
    }

    @Override
    public ApiUserDetailWS getUserDetails(String accessCode){
        return session.getUserDetails(accessCode);
    }

    @Override
    public PlanWS getPlanByInternalNumber(String internalNumber, Integer entityId){
        return session.getPlanByInternalNumber(internalNumber,entityId);
    }

    @Override
    public boolean notifyUserByEmail(Integer userId, Integer notificationId) throws SessionInternalError{
        return session.notifyUserByEmail(userId, notificationId);
    }

    @Override
    public Integer getIdFromCreateUpdateNotification(Integer messageId, MessageDTO dto){
        return session.getIdFromCreateUpdateNotification(messageId,dto);
    }

    @Override
    public Integer createMessageNotificationType(Integer notificationCategoryId, String description, Integer languageId ){
        return session.createMessageNotificationType(notificationCategoryId, description, languageId);
    }

    @Override
    public CreditNoteInvoiceMapWS[] getCreditNoteInvoiceMaps(Date invoiceCreationStartDate, Date invoiceCreationEndDate) {
        return session.getCreditNoteInvoiceMaps(invoiceCreationStartDate, invoiceCreationEndDate);
    }

    @Override
    public SignupResponseWS processSignupRequest(SignupRequestWS request) {
        return session.processSignupRequest(request);
    }

    @Override
    public UserWS validateLogin(String userName, String password) {
        return session.validateLogin(userName, password);
    }

    @Override
    public void updatePassword(Integer userId, String currentPassword, String newPassword) {
        session.updatePassword(userId, currentPassword, newPassword);
    }

    @Override
    public void resetPasswordByUserName(String userName){
        session.resetPasswordByUserName(userName);
    }

    @Override
    public PaymentMethodTypeWS[] getAllPaymentMethodTypes() {
        return session.getAllPaymentMethodTypes();
    }

    @Override
    public JbillingMediationRecord[] getMediationEventsForUserDateRange(Integer userId, Date startDate, Date endDate,
            int offset, int limit) {
        return session.getMediationEventsForUserDateRange(userId, startDate, endDate, offset, limit);
    }

    @Override
    public UserProfileWS getUserProfile(Integer userId) {
        return session.getUserProfile(userId);
    }

    @Override
    public AssetRestWS[] getAllAssetsForUser(Integer userId) {
        return session.getAllAssetsForUser(userId);
    }

    @Override
    public Integer createOrderWithAssets(OrderWS order, OrderChangeWS[] orderChanges, AssetWS[] assets){
        return session.createOrderWithAssets(order, orderChanges, assets);
    }

    @Override
    public void updateOrderMetaFields(Integer orderId, MetaFieldValueWS[] orderMetaFieldValues) {
        session.updateOrderMetaFields(orderId, orderMetaFieldValues);
    }

    @Override
    public OrderMetaFieldValueWS getOrderMetaFieldValueWS(Integer orderId) {
        return session.getOrderMetaFieldValueWS(orderId);
    }

    @Override
    public void updateAssetMetaFields(Integer assetId, MetaFieldValueWS[] assetMetaFieldValues) {
        session.updateAssetMetaFields(assetId, assetMetaFieldValues);
    }

    @Override
    public PaymentInformationWS[] getPaymentInstruments(Integer userId){
        return session.getPaymentInstruments(userId);
    }

    @Override
    public CustomerMetaFieldValueWS getCustomerMetaFields(Integer userId){
        return session.getCustomerMetaFields(userId);
    }

    @Override
    public UserWS updateCustomerContactInfo(ContactInformationWS contactInformation) {
        return session.updateCustomerContactInfo(contactInformation);
    }

    @Override
    public SecurePaymentWS addPaymentInstrument(PaymentInformationWS instrument) {
        return session.addPaymentInstrument(instrument);
    }

    @Override
    public void updateCustomerMetaFields(Integer userId, MetaFieldValueWS[] customerMetaFieldValues) {
        session.updateCustomerMetaFields(userId, customerMetaFieldValues);
    }

    @Override
    public JbillingMediationRecord[] getUnBilledMediationEventsByUser(Integer userId, int offset, int limit) {
        return session.getUnBilledMediationEventsByUser(userId, offset, limit);
    }

    @Override
    public OrderWS[] getUsersAllSubscriptions(Integer userId) {
        return session.getUsersAllSubscriptions(userId);
    }

    @Override
    public CreditNoteWS[] getCreditNotesByUser(Integer userId, Integer offset, Integer limit) {
        return session.getCreditNotesByUser(userId, offset, limit);
    }

    @Override
    public PaymentWS[] findPaymentsForUser(Integer userId, int offset, int limit) {
        return session.findPaymentsForUser(userId, offset, limit);
    }

    @Override
    public Integer createRateCard(RateCardWS rateCardWS, File rateCardFile) {
        return session.createRateCard(rateCardWS, rateCardFile);
    }

    @Override
    public void updateRateCard(RateCardWS rateCardWS, File rateCardFile){
        session.updateRateCard(rateCardWS, rateCardFile);
    }

    @Override
    public void deleteRateCard(Integer rateCardId) {
        session.deleteRateCard(rateCardId);
    }

    public Integer[] createCustomInvoice(File csvFile){
        return session.createCustomInvoice(csvFile);
    }

    @Override
    public PaymentUrlLogDTO createPaymentUrl(Map<String, Object> map) {
        return session.createPaymentUrl(map);
    }

    @Override
    public String generateGstR1JSONFileReport(String startDate, String endDate) throws Exception{
        return session.generateGstR1JSONFileReport(startDate, endDate);
    }

    public Optional<Object> executePaymentTask(Integer paymentLogUrlLogId, String payerVPA,
                                               String action) throws PluggableTaskException {
        return session.executePaymentTask(paymentLogUrlLogId, payerVPA, action);
    }
}
