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

package com.sapienter.jbilling.client.filters

import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS
import jbilling.Filter
import jbilling.FilterConstraint
import jbilling.FilterType

/**
 * FilterFactory

 * @author Brian Cowdery
 * @since  03-12-2010
 */
class FilterFactory {

    static baseFilters = ([
        ALL: [
            new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ, field: 'id', template: 'id', visible: true)
        ],
        INVOICE: [
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.DATE_BETWEEN, field: 'dueDate', template: 'date', visible: true),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.STATUS, field: 'invoiceStatus', template: 'invoice/status', visible: true),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDatetime', template: 'date', visible: false),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'baseUser.id', template: 'id', visible: true),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: false),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'currency.id', template: 'currency', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'contact.fields', template: 'invoice/ccf', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.IS_NOT_EMPTY, field: 'paymentMap', template: 'invoice/payments', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'total', template: 'range', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'balance', template: 'range', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'carriedBalance', template: 'range', visible: false),
            new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.LIKE, field: 'publicNumber', template: 'value', visible: false),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.LIKE, field: 'u.company.description', template: 'company', visible: true),
			/*this filter used to search invoice for customer subscribed plan*/
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.LIKE, field: 'planInternalNumber', template: 'value', visible: false),
			new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.LIKE, field: 'u.userName', template: 'customer/login', visible: true),
        ],
        ORDER: [
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'changeStatus', template: 'order/changeStatus', visible: true),
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'u.id', template: 'id', visible: true),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.LIKE, field: 'u.userName', template: 'customer/login', visible: false),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'activeSince', template: 'date', visible: false),
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'activeUntil', template: 'date', visible: false),
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.META_FIELD, field: 'contact.fields', template: 'order/ccf', visible: false),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDate', template: 'date', visible: true),
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'nextBillableDay', template: 'date', visible: false),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.STATUS, field: 'orderStatus', template: 'order/status', visible: false),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.STATUS, field: 'orderPeriod', template: 'order/period', visible: true),
            new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'userCodes.userCode.identifier', template: 'value', visible: false),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.LIKE, field: 'company.description', template: 'company', visible: true),
			new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.ORDER_DATE, field: 'orderDate', template: 'order/orderDate', visible: false)
        ],
        PRODUCT: [
        	new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.EQ, field: 'contact.fields', template: 'product/ccf', visible: false),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.LIKE, field: 'internalNumber', template: 'product/internalNumber', visible: true),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.LIKE, field: 'description', template: 'product/description', visible: true),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.EQ, field: 'hasDecimals', template: 'product/decimals', visible: false),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.LIKE, field: 'glCode', template: 'product/glCode', visible: false),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.EQ, field: 'price.type', template: 'product/priceStrategy', visible: true),
            new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'price.rate', template: 'range', visible: true),
			new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.EQ, field: 'i.global', template: 'isGlobal', visible: true),
			new Filter(type: FilterType.PRODUCT, constraintType: FilterConstraint.LIKE, field: 'u.company.description', template: 'company', visible: true),
        ],
        CUSTOMER: [
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.LIKE, field: 'contact.fields', template: 'customer/ccf', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDatetime', template: 'date', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.STATUS, field: 'userStatus', template: 'customer/status', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.LIKE, field: 'userName', template: 'customer/login', visible: true),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'language.id', template: 'customer/language', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'currency.id', template: 'currency', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.IS_NOT_EMPTY, field: 'orders', template: 'customer/orders', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.IS_NOT_EMPTY, field: 'invoices', template: 'customer/invoices', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.IS_NOT_EMPTY, field: 'payments', template: 'customer/payments', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.IS_NOT_NULL, field: 'customer.parent', template: 'customer/child', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'customer.isParent', template: 'trueOrFalse', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'customer.partner.id', template: 'id', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'userCodes.userCode.identifier', template: 'value', visible: false),
            new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'deleted', template: 'customer/deleted', visible: false, integerValue: 0),
			new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.LIKE, field: 'u.company.description', template: 'company', visible: false),
			new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.LIKE, field: 'accountTypeFields', template: 'customer/atf', visible: false),			
			new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.LIKE, field: 'as.subscriberNumber', template: 'customer/phone', visible: true),
			new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.DATE_BETWEEN, field: 'customer.nextInvoiceDate', template: 'date', visible: false),
		],
        PARTNER: [
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.LIKE, field: 'contact.firstName', template: 'value', visible: false),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.LIKE, field: 'contact.lastName', template: 'value', visible: false),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.LIKE, field: 'contact.email', template: 'value', visible: false),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.STATUS, field: 'baseUser.userStatus', template: 'customer/status', visible: true),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.LIKE, field: 'userName', template: 'customer/login', visible: true),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.EQ, field: 'deleted', template: 'customer/deleted', visible: true, integerValue: 0),
            new Filter(type: FilterType.PARTNER, constraintType: FilterConstraint.LIKE, field: 'brokerId', template: 'value', visible: true)
        ],
        PAYMENT: [
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'u.id', template: 'id', visible: true),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.LIKE, field: 'u.userName', template: 'customer/login', visible: true),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDatetime', template: 'date', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'isRefund', template: 'payment/refund', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'paymentMethod.id', template: 'payment/method', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'paymentResult.id', template: 'payment/result', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'i.invoiceEntity.id', template: 'id', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'amount', template: 'range', visible: true),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'balance', template: 'range', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.EQ, field: 'contact.fields', template: 'payment/ccf', visible: false),
            new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.IN, field: 'paymentMethod.credit.id', template: 'payment/credit', visible: true, stringValue: new PaymentMethodDAS().findAllValidMethods().collect {it.id}.join(',')),
			new Filter(type: FilterType.PAYMENT, constraintType: FilterConstraint.LIKE, field: 'u.company.description', template: 'company', visible: true),
        ],
		BILLINGPROCESS: [
			new Filter(type: FilterType.BILLINGPROCESS, constraintType: FilterConstraint.DATE_BETWEEN, field: 'billingDate', template: 'date', visible: true),
            new Filter(type: FilterType.BILLINGPROCESS, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'billing/review', visible: true)
		],
		MEDIATIONPROCESS:[
			new Filter(type: FilterType.MEDIATIONPROCESS, constraintType: FilterConstraint.DATE_BETWEEN, field: 'startDate', template: 'date', visible: true),
			new Filter(type: FilterType.MEDIATIONPROCESS, constraintType: FilterConstraint.EQ, field: 'orderId', template: 'id', visible: true),
			new Filter(type: FilterType.MEDIATIONPROCESS, constraintType: FilterConstraint.GREATER_THAN, field: 'errors', template: 'trueOrFalse', visible: true),
		],
		LOG:[
			new Filter(type: FilterType.LOG, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDatetime', template: 'date', visible: true),
            new Filter(type: FilterType.LOG, constraintType: FilterConstraint.LIKE, field: 'u.userName', template: 'customer/login', visible: false),
            new Filter(type: FilterType.LOG, constraintType: FilterConstraint.EQ, field: 'u.id', template: 'id', visible: true),
            new Filter(type: FilterType.LOG, constraintType: FilterConstraint.EQ, field: 'table.name', template: 'log/table', visible: true),
            new Filter(type: FilterType.LOG, constraintType: FilterConstraint.EQ, field: 'foreignId', template: 'id', visible: true),
        ],
        PROVISIONING_CMD: [
            new Filter(type: FilterType.PROVISIONING_CMD, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDate', template: 'date', visible: true),
            new Filter(type: FilterType.PROVISIONING_CMD, constraintType: FilterConstraint.EQ, field: 'commandStatus', template: 'provisioning/status_cmd', visible: true),
            new Filter(type: FilterType.PROVISIONING_CMD, constraintType: FilterConstraint.EQ, field: 'command_type', template: 'provisioning/type', visible: true),
            new Filter(type: FilterType.PROVISIONING_CMD, constraintType: FilterConstraint.EQ, field: 'type_identifier', template: 'id', visible: true),
        ],
        PROVISIONING_REQ: [
            new Filter(type: FilterType.PROVISIONING_REQ, constraintType: FilterConstraint.DATE_BETWEEN, field: 'provisioning.create_date', template: 'date', visible: true),
            new Filter(type: FilterType.PROVISIONING_REQ, constraintType: FilterConstraint.STATUS, field: 'provisioning.req_status', template: 'provisioning/status_req', visible: true),
            new Filter(type: FilterType.PROVISIONING_REQ, constraintType: FilterConstraint.EQ, field: 'provisioning.req_command_id', template: 'value', visible: true),
            new Filter(type: FilterType.PROVISIONING_REQ, constraintType: FilterConstraint.EQ, field: 'provisioning.req_processor', template: 'value', visible: true),
        ],
        CUSTOMER_ENROLLMENT: [
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.STATUS, field: 'enrollmentStatus', template: 'customerEnrollment/status', visible: true),
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.LIKE, field: 'contact.email', template: 'value', visible: false),
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.LIKE, field: 'accountTypeFields', template: 'customerEnrollment/atf', visible: false),
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.DATE_BETWEEN, field: 'createDatetime', template: 'date', visible: true),
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.LIKE, field: 'e.company.description', template: 'company', visible: true),
			new Filter(type: FilterType.CUSTOMER_ENROLLMENT, constraintType: FilterConstraint.LIKE, field: 'contact.fields', template: 'customerEnrollment/ccf', visible: false),
        ],
        EDI_TYPE: [
			new Filter(type: FilterType.EDI_TYPE, constraintType: FilterConstraint.LIKE, field: 'name', template: 'value', visible: true),
        ]
        ,EDI_FILE: [
            new Filter(type: FilterType.EDI_FILE, constraintType: FilterConstraint.LIKE, field: 'name', template: 'value', visible: true),
            new Filter(type: FilterType.EDI_FILE, constraintType: FilterConstraint.STATUS, field: 'type', template: 'edi/status', visible: true),
            new Filter(type: FilterType.EDI_FILE, constraintType: FilterConstraint.EQ, field: 'fileStatus.id', template: 'edi/fileStatus', visible: true),
		],
        CREDITNOTE:[
			new Filter(type: FilterType.CREDITNOTE, constraintType: FilterConstraint.EQ, field: 'user.id', template: 'id', visible: true),
			new Filter(type: FilterType.CREDITNOTE, constraintType: FilterConstraint.LIKE, field: 'u.userName', template: 'customer/login', visible: true),
			new Filter(type: FilterType.CREDITNOTE, constraintType: FilterConstraint.DATE_BETWEEN, field: 'creditNoteDate', template: 'date', visible: true),
			new Filter(type: FilterType.CREDITNOTE, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'amount', template: 'range', visible: true),
			new Filter(type: FilterType.CREDITNOTE, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'balance', template: 'range', visible: false),
        ],
		PLAN: [
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.LIKE, field: 'plan.i.internalNumber', template: 'plan/internalNumber', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.LIKE, field: 'description', template: 'plan/description', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.EQ, field: 'plan.period', template: 'plan/period', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.LIKE, field: 'u.company.description', template: 'company', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.NUMBER_BETWEEN, field: 'price.rate', template: 'range', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.EQ, field: 'i.global', template: 'isGlobal', visible: true),
			new Filter(type: FilterType.PLAN, constraintType: FilterConstraint.EQ, field: 'contact.fields', template: 'plan/ccf', visible: false),
		],
		RECHARGE_HISTORY:[
				new Filter(type: FilterType.RECHARGE_HISTORY, constraintType: FilterConstraint.EQ, field: 'TRANSACTION_ID', template: 'id', visible: false),
				new Filter(type: FilterType.RECHARGE_HISTORY, constraintType: FilterConstraint.EQ, field: 'SUBSCRIBER_NUMBER', template: 'recharge_history/phone', visible: true),
				new Filter(type: FilterType.RECHARGE_HISTORY, constraintType: FilterConstraint.DATE_BETWEEN, field: 'RECHARGE_DATE', template: 'date', visible: true),
				new Filter(type: FilterType.RECHARGE_HISTORY, constraintType: FilterConstraint.EQ, field: 'TRANSACTION_TYPE', template: 'recharge_history/transactionType', visible: true),

		]
    ] as Map).asImmutable()

    /**
     * Returns a list of filters for the given type.
     *
     * @param type filter type
     * @return list of filters
     */
    static def Object getFilters(FilterType type) {
        def filters = []
        baseFilters.findAll{ it.key == FilterType.ALL.name() || it.key == type.name() }.each{ filters << it?.value }

        return filters.flatten().collect{new Filter((Filter)it)}
    }
}
