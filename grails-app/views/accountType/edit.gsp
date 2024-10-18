<%@ page import="com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO; com.sapienter.jbilling.server.process.db.PeriodUnitDTO;com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.common.Util" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.user.db.AccountTypeDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.UserDTOEx; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.user.permisson.db.RoleDTO; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.user.db.AccountTypeDAS" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <r:require module="errors"/>
</head>

<body>
<div class="column-hold">

    <g:set var="isNew" value="${!accountType || !accountType?.id || accountType?.id == 0}"/>
    <g:set var="defaultCurrency" value="${CompanyDTO.get(session['company_id']).getCurrency()}"/>

    <div class="heading">
        <strong>
            <g:if test="${params.clone}">
                <g:message code="account.type.config.clone"/>
            </g:if>
            <g:elseif test="${isNew}">
                <g:message code="account.type.config.add"/>
            </g:elseif>
            <g:else>
                <g:message code="account.type.config.edit.title"/>
            </g:else>
        </strong>
    </div>

    <g:form id="save-config-form" name="account-type-config-form" url="[controller: 'accountType', action: 'save']"
            useToken="true">
        <fieldset>
            <div class="form-hold">
                <div class="box">
                    <div class="form-columns">
                        <g:hiddenField name="id" value="${accountType?.id ?: null}"/>
                        <g:hiddenField name="clone" value="${clone}"/>
                        <g:hiddenField name="dateCreated" value="${accountType?.dateCreated ?
                                formatDate(date: accountType.dateCreated, formatName: 'date.format') : null}"/>
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="accountType.description"/><span
                                    id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">description</content>
                            <g:textField class="field" name="description"
                                         value="${accountType ? accountType.getDescription(session['language_id'])?.content : null}"/>
                        </g:applyLayout>
                        <g:set var="mainSubscription" value="${accountType?.mainSubscription}"/>
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="account.type.config.billingCycle"/><span
                                    id="mandatory-meta-field">*</span></content></content>
                            <content tag="label.for">mainSubscription.nextInvoiceDayOfPeriod</content>

                            <div class="inp-bg inp2">
                                <g:textField class="field" name="mainSubscription.nextInvoiceDayOfPeriod"
                                             value="${accountType?.mainSubscription?.nextInvoiceDayOfPeriod ?: 1}"
                                             maxlength="4" size="3"/>
                            </div>

                            <g:applyLayout name="form/select_holder">
                                <content tag="label.for">mainSubscription.periodId</content>
                                <g:select from="${orderPeriods}"
                                          optionKey="id" optionValue="${{ it.getDescription(session['language_id']) }}"
                                          name="mainSubscription.periodId"
                                          value="${accountType?.mainSubscription?.periodId}"/>
                            </g:applyLayout>
                        </g:applyLayout>

                        <g:preferenceEquals preferenceId="${Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION}" value="1">
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="account.type.config.invoiceTemplate"/></content>
                                <g:select name="invoiceTemplateId" from="${invoiceTemplate}"
                                          value="${accountType?.invoiceTemplateId}" optionKey="id" optionValue="name"/>
                            </g:applyLayout>
                        </g:preferenceEquals>
                        <g:preferenceIsNullOrEquals preferenceId="${Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION}"
                                                    value="0">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="account.type.config.invoiceDesign"/></content>
                                <content tag="label.for">invoiceDesign</content>
                                <g:textField class="field" name="invoiceDesign" value="${accountType?.invoiceDesign}"/>
                            </g:applyLayout>
                        </g:preferenceIsNullOrEquals>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="account.type.config.creditLimit"/></content>
                            <content tag="label.for">creditLimit</content>
                            <g:textField class="field" name="creditLimitAsDecimal"
                                         value="${formatNumber(number: accountType?.creditLimit ?: 0.0, formatName: 'money.format')}"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.currency"/></content>
                            <content tag="label.for">accountType.currencyId</content>
                            <g:select name="currencyId"
                                      from="${currencies}"
                                      optionKey="id"
                                      optionValue="${{ it.getDescription(session['language_id']) }}"
                                      value="${accountType?.currencyId ?: defaultCurrency?.id}"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.language"/></content>
                            <content tag="label.for">accountType.languageId</content>
                            <g:select name="languageId" from="${LanguageDTO.list(sort: "id", order: "asc")}"
                                      optionKey="id" optionValue="description" value="${accountType?.languageId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message
                                    code="account.type.config.creditNotificationLimit1"/></content>
                            <content tag="label.for">creditNotificationLimit1</content>
                        %{--Null if credit notification limit1 is turned off --}%
                            <g:textField class="field" name="creditNotificationLimit1AsDecimal"
                                         value="${accountType?.creditNotificationLimit1 ?
                                                 formatNumber(number: accountType?.creditNotificationLimit1 ?: 0, formatName: 'money.format') :
                                                 null}"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message
                                    code="account.type.config.creditNotificationLimit2"/></content>
                            <content tag="label.for">creditNotificationLimit2</content>
                        %{--Null if credit notification limit2 is turned off --}%
                        <g:textField class="field" name="creditNotificationLimit2AsDecimal"
                                     value="${accountType?.creditNotificationLimit2 ?
                                             formatNumber(number: accountType?.creditNotificationLimit2?:0, formatName: 'money.format') :
                                             null}" />
                    </g:applyLayout>
                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="prompt.invoice.delivery.method"/></content>
                        <content tag="label.for">invoiceDeliveryMethodId</content>
                        <g:select from="${CompanyDTO.get(session['company_id']).invoiceDeliveryMethods.sort{ it.id }}"
                                  optionKey="id"
                                  valueMessagePrefix="customer.invoice.delivery.method"
                                  name="invoiceDeliveryMethodId"
                                  value="${accountType?.invoiceDeliveryMethodId}"/>
                    </g:applyLayout>
                    <g:applyLayout name="form/select_multiple">
                        <content tag="label"><g:message code="prompt.payment.method.types"/></content>
                        <content tag="label.for">paymentMethodTypeIds</content>
                        <g:select id="payment-method-select" multiple="multiple" name="paymentMethodTypeIds"
                                  from="${paymentMethodTypes}"
                                  optionKey="id"
                                  optionValue="methodName"
                                  value="${selectedPaymentMethodTypeIds}" />
                    </g:applyLayout>
                </div>

                <div class="btn-box buttons">
                    <ul>
                        <li>
                            <a class="submit save button-primary" onclick="$('#save-config-form').submit();">
                                <span><g:message code="button.save"/></span>
                            </a>
                        </li>
                        <li>
                            <g:link action="list" class="submit cancel">
                                <span><g:message code="button.cancel"/></span>
                            </g:link>
                        </li>
                    </ul>
                </div>
            </div>
        </fieldset>

    </g:form>

</div>

<script type="text/javascript">
    $(document).ready(function(){

        <g:each in="${globalPaymentMethodIds}" var="globalPaymentMethodId">
        $("#payment-method-select").find("[value=${globalPaymentMethodId}]").attr("selected", "selected");
        $("#payment-method-select").find("[value=${globalPaymentMethodId}]").attr("disabled", "disabled");
        </g:each>

    });
</script>

</body>
</html>
