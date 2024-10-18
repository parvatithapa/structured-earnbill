<%@page import="com.sapienter.jbilling.server.metafields.validation.ValidationRuleType"%>
<%@ page import="com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.metafields.MetaFieldType; com.sapienter.jbilling.common.Util;" %>
<g:if test="${paymentInstr.paymentMethodType.paymentMethodTemplate.templateName.equals("Payment Card")}">
    <table class="dataTable" cellspacing="0" cellpadding="0">
        <tbody>
        <tr>
            <td><g:message code="customer.detail.payment.credit.card"/></td>
            <td class="value">
                <g:set var="expireDate" value="${paymentInstr.metaFields.grep{it.field.fieldUsage?.equals(MetaFieldType.DATE)}?.value?.join()}"/>
                <g:set var="card" value="${paymentInstr.metaFields.grep{it.field?.fieldUsage?.equals(MetaFieldType.PAYMENT_CARD_NUMBER)}}"/>
                <g:if test="${card?.value}">
                %{-- obscure credit card by default, or if the preference is explicitly set --}%
                    <g:set var="creditCardNumber" value="${card?.value.join()}"/>
                    <g:if test="${card?.field?.validationRule?.ruleType[0]?.toString().equals(ValidationRuleType.PAYMENT_CARD_OBSCURED.toString())}">
                         <g:set var="creditCardNumber" value="${Util.getObscuredCardNumberNew(creditCardNumber.toCharArray())}"/>
                    </g:if>
                    <g:else>
                         <g:set var="creditCardNumber" value="${Util.getObscuredCardNumber(creditCardNumber.toCharArray())}"/>
                    </g:else>
                    ${creditCardNumber}
                </g:if>
            </td>
        </tr>

        <tr>
            <td><g:message code="customer.detail.payment.credit.card.expiry"/></td>
            <td class="value">${expireDate}</td>
        </tr>
        </tbody>
    </table>
</g:if>
