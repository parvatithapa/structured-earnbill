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

package com.sapienter.jbilling.server.security;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.ObjectNotFoundException;

import com.sapienter.jbilling.server.security.methods.SecuredMethodFactory;
import com.sapienter.jbilling.server.security.methods.SecuredMethodSignature;
import com.sapienter.jbilling.server.security.methods.SecuredMethodType;

/**
 * WSSecurityMethodMapper
 *
 * @author Brian Cowdery
 * @since 02-11-2010
 */
public class WSSecurityMethodMapper {

    static {
        SecuredMethodFactory.add("getUserWS", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("deleteUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserContactsWS", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("updateUserContact", 0, SecuredMethodType.USER);   // todo: should validate user and contact type ids
        SecuredMethodFactory.add("setAuthPaymentType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getAuthPaymentType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getPartner", 0, SecuredMethodType.PARTNER);

        SecuredMethodFactory.add("getItem", 0, SecuredMethodType.ITEM);             // todo: should validate item id and user id
        SecuredMethodFactory.add("deleteItem", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("deleteItemCategory", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getUserItemsByCategory", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("isUserSubscribedTo", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestInvoiceByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastInvoicesByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestOrderByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastOrdersByItemType", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("validatePurchase", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("validateMultiPurchase", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("getOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deleteOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getCurrentOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("updateCurrentOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getOrderLine", 0, SecuredMethodType.ORDER_LINE);
        SecuredMethodFactory.add("getOrderByPeriod", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastOrders", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("getInvoiceWS", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("createInvoice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("createInvoiceFromOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deleteInvoice", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getAllInvoices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestInvoice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastInvoices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserInvoicesByDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getPaperInvoicePDF", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getAllInvoicesForUser", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("getPayment", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("getLatestPayment", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastPayments", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("payInvoice", 0, SecuredMethodType.INVOICE);

        SecuredMethodFactory.add("getBillingProcess", 0, SecuredMethodType.BILLING_PROCESS);
        SecuredMethodFactory.add("getBillingProcessGeneratedInvoices", 0, SecuredMethodType.BILLING_PROCESS);

        SecuredMethodFactory.add("getMediationEventsForOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getMediationEventsForOrderDateRange", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getMediationRecordsByMediationProcess", 0, SecuredMethodType.MEDIATION_PROCESS);
        SecuredMethodFactory.add("deleteMediationConfiguration", 0, SecuredMethodType.MEDIATION_CONFIGURATION);
        SecuredMethodFactory.add("getMediationErrorRecordsCount", 0, SecuredMethodType.MEDIATION_CONFIGURATION);

        SecuredMethodFactory.add("updateOrderAndLineProvisioningStatus", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("updateLineProvisioningStatus", 0, SecuredMethodType.ORDER_LINE);
        SecuredMethodFactory.add("notifyInvoiceByEmail", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("notifyPaymentByEmail", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("deletePlugin", 0, SecuredMethodType.PLUG_IN);
        SecuredMethodFactory.add("rescheduleScheduledPlugin", 0, SecuredMethodType.PLUG_IN);

        SecuredMethodFactory.add("getPlanWS", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("deletePlan", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("addPlanPrice", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("isCustomerSubscribed", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("isCustomerSubscribedForDate", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("getSubscribedCustomers", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("getPlansBySubscriptionItem", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("getPlansByAffectedItem", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("getCustomerPrice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getCustomerPriceForDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("deleteAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("getAssetsForCategory", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getAssetsForItem", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("deleteDiscount", 0, SecuredMethodType.DISCOUNT);
        SecuredMethodFactory.add("removePaymentLink", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getUnpaidInvoices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserInvoicesPage", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("removeAllPaymentLinks", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("createInvoiceWithDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("applyOrderToInvoice", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deletePartner", 0, SecuredMethodType.PARTNER);
        SecuredMethodFactory.add("getChildItemCategories", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getLinkedOrders", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getUserOrdersPage", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastOrdersPage", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getOrdersByDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserSubscriptions", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("deleteAccountType", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("deletePayment", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("getPaymentsByDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserPaymentInstrument", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserPaymentsPage", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getTotalRevenueByUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getDiscountWS", 0, SecuredMethodType.DISCOUNT);
        SecuredMethodFactory.add("getUsagePoolWS", 0, SecuredMethodType.USAGE_POOL);
        SecuredMethodFactory.add("deleteUsagePool", 0, SecuredMethodType.USAGE_POOL);
        SecuredMethodFactory.add("getCustomerUsagePoolById", 0, SecuredMethodType.CUSTOMER_USAGE_POOL);
        SecuredMethodFactory.add("getCustomerUsagePoolsByCustomerId", 0, SecuredMethodType.CUSTOMER);
        SecuredMethodFactory.add("getUsagePoolsByPlanId", 0, SecuredMethodType.PLAN);
        SecuredMethodFactory.add("getOrderProcesses", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getOrderProcessesByInvoice", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getMediationEventsForInvoice", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getPluginWS", 0, SecuredMethodType.PLUG_IN);
        SecuredMethodFactory.add("deleteRoute", 0, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("getRoute", 0, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("searchDataTable", 0, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("getRouteTable", 0, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("deleteRouteRecord", 0, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("updateRouteRecord", 1, SecuredMethodType.ROUTE);
        SecuredMethodFactory.add("deleteRouteRateCard", 0, SecuredMethodType.ROUTE_RATE_CARD);
        SecuredMethodFactory.add("getRouteRateCard", 0, SecuredMethodType.ROUTE_RATE_CARD);
        SecuredMethodFactory.add("getAssetAssignmentsForAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("getAssetAssignmentsForOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("findOrderForAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("findOrdersForAssetAndDateRange", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("deleteMetaFieldGroup", 0, SecuredMethodType.META_FIELD_GROUP);
        SecuredMethodFactory.add("getMetaFieldGroup", 0, SecuredMethodType.META_FIELD_GROUP);
        SecuredMethodFactory.add("deleteMetaField", 0, SecuredMethodType.META_FIELD);
        SecuredMethodFactory.add("getMetaField", 0, SecuredMethodType.META_FIELD);
        SecuredMethodFactory.add("deleteOrderChangeStatus", 0, SecuredMethodType.ORDER_CHANGE_STATUS);
        SecuredMethodFactory.add("getOrderChanges", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deleteOrderChangeType", 0, SecuredMethodType.ORDER_CHANGE_TYPE);
        SecuredMethodFactory.add("getOrderChangeTypeById", 0, SecuredMethodType.ORDER_CHANGE_TYPE);
        SecuredMethodFactory.add("getAssetsByUserId", 0, SecuredMethodType.USER );
        SecuredMethodFactory.add("getEnumeration", 0, SecuredMethodType.ENUMERATION);
        SecuredMethodFactory.add("deleteEnumeration", 0, SecuredMethodType.ENUMERATION);
        SecuredMethodFactory.add("reserveAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("releaseAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("createSubscriptionAccountAndOrder", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("getAccountType", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("getInformationTypesForAccountType", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("getAccountInformationType", 0, SecuredMethodType.META_FIELD_GROUP);
        SecuredMethodFactory.add("deleteAccountInformationType", 0, SecuredMethodType.META_FIELD_GROUP);
        SecuredMethodFactory.add("deleteRateCard", 0, SecuredMethodType.RATE_CARD);
        SecuredMethodFactory.add("getPaymentMethodType", 0, SecuredMethodType.PAYMENT_METHOD_TYPE);
        SecuredMethodFactory.add("deleteRatingUnit", 0, SecuredMethodType.RATING_UNIT);
        SecuredMethodFactory.add("getRatingUnit", 0, SecuredMethodType.RATING_UNIT);
        SecuredMethodFactory.add("deleteOrderPeriod", 0, SecuredMethodType.ORDER_PERIOD);
        SecuredMethodFactory.add("findOrderStatusById", 0, SecuredMethodType.ORDER_STATUS);
        SecuredMethodFactory.add("getDefaultOrderStatusId", 1, SecuredMethodType.COMPANY);
        SecuredMethodFactory.add("getCustomersLinkedToUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("deletePaymentMethodType", 0, SecuredMethodType.PAYMENT_METHOD_TYPE);
        SecuredMethodFactory.add("removePaymentInstrument", 0, SecuredMethodType.PAYMENT_INFORMATION);
        SecuredMethodFactory.add("getAllItemsByEntityId", 0, SecuredMethodType.COMPANY);
        SecuredMethodFactory.add("getLastPaymentsPage", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("createPaymentLink", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("isBillingRunning", 0, SecuredMethodType.COMPANY);
        SecuredMethodFactory.add("undoMediation", 0, SecuredMethodType.MEDIATION_PROCESS);
        SecuredMethodFactory.add("getAssetTransitions", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("findAssets", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("createAccountTypePrice", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("updateAccountTypePrice", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("deleteAccountTypePrice", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("getAccountTypePrices", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("getAccountTypePrice", 0, SecuredMethodType.ACCOUNT_TYPE);
        SecuredMethodFactory.add("startImportAssetJob", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("getMatchingField", 0, SecuredMethodType.MATCHING_FIELD);
        SecuredMethodFactory.add("deleteMatchingField", 0, SecuredMethodType.MATCHING_FIELD);
        SecuredMethodFactory.add("getDataTableQuery", 0, SecuredMethodType.DATA_TABLE_QUERY);
        SecuredMethodFactory.add("deleteDataTableQuery", 0, SecuredMethodType.DATA_TABLE_QUERY);
        SecuredMethodFactory.add("getItemUsage", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("createCustomerPrice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("updateCustomerPrice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("deleteCustomerPrice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getCustomerPrices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("userExistsWithId", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUsersByStatus", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUsersInStatus", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUsersNotInStatus", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserCodesForUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getOrdersLinkedToUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getAddonItems", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("getAllItemCategoriesByEntityId", 0, SecuredMethodType.COMPANY);
        SecuredMethodFactory.add("getItemByCategory", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getItemCategoryById", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getProvisioningCommandById", 0, SecuredMethodType.PROVISIONING_COMMAND);
        SecuredMethodFactory.add("getProvisioningRequests", 0, SecuredMethodType.PROVISIONING_COMMAND);
        SecuredMethodFactory.add("getProvisioningRequestById", 0, SecuredMethodType.PROVISIONING_REQUEST);
    }

    /**
     * Return a WSSecured object mapped from the given method and method arguments for validation.
     * This produced a secure object for validation from web-service method calls that only accept and return
     * ID's instead of WS objects that can be individually validated.
     *
     * @param method method to map
     * @param args method arguments
     * @return instance of WSSecured mapped from the given entity, null if entity could not be mapped.
     */
    public static WSSecured getMappedSecuredWS(Method method, Object[] args) {
        if (method != null) {

            SecuredMethodSignature sig = SecuredMethodFactory.getSignature(method);
            if (sig != null && sig.getIdArgIndex() <= args.length) {
                try {
                    return sig.getType().getMappedSecuredWS((Serializable) args[sig.getIdArgIndex()]);
                } catch (ObjectNotFoundException e) {
                    // hibernate complains loudly... object does not exist, no reason to validate.
                    return null;
                }
            }
        }

        return null;
    }
}
