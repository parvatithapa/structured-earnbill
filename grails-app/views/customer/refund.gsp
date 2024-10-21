%{--
 SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 _____________________

 [2024] Sarathi Softech Pvt. Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is and remains
 the property of Sarathi Softech.
 The intellectual and technical concepts contained
 herein are proprietary to Sarathi Softech
 and are protected by IP copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
--}%

<%@page import="com.sapienter.jbilling.server.customer.CustomerBL;" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <r:external file="js/form.js"/>
</head>

<body>
<div class="form-edit">
    <div class="heading">
        <strong>
            <g:message code="refund.title"/>
        </strong>
    </div>
    <div class="form-hold">
        <g:form name="refund-form"  action="refundAmount" useToken="true" onSubmit="return validateRefundForm()">
            <fieldset>
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="refund.user.id"/></content>
                            <content tag="label.for">jbillingUserId</content>
                            <span><g:link controller="customer" action="list"
                                          id="${userId}">${userId}</g:link></span>
                            <g:hiddenField name="id" value="${userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="user.subscriber.number"/></content>
                            <content tag="label.for">subscriberNumber</content>
                                ${subscriberNumber}
                                <g:hiddenField id="subscriberNumber"
                                name="subscriberNumber"
                                value="${subscriberNumber}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="user.wallet.balance"/></content>
                            <content tag="label.for">walletBalance</content>
                            <g:formatNumber number="${walletBalance}" type="currency"  currencySymbol="${currencySymbol}" id="walletAmount"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><span id="mandatory-meta-field">*</span><g:message code="refund.amount"/></content>
                            <content tag="label.for">refundAmount</content>
                                <g:textField class="field"
                                    name="refundAmount"
                                    id="refundAmount"
                                    value="${null}"
                                    maxlength="10"/>
                        </g:applyLayout>
                    </div>
                        <div class="column">
                            <g:applyLayout name="form/textarea">
                            <content tag="label"><g:message code="refund.notes"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">notes</content>
                            <g:textArea class="narrow" id="notes" name="notes" rows="5" cols="45"/>
                        </g:applyLayout>
                    </div>
                </div>
                <div><br/></div>
                <div class="buttons">
                    <ul>
                        <li>
                            <g:actionSubmit id="btnRefund" class="submit save button-primary" value="${message(code:'recharge.history.button.submit')}" />
                        </li>
                        <li>
                            <g:link action="list" class="submit cancel"><span><g:message
                                    code="refund.button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>
            </fieldset>
        </g:form>
    </div>
</div>
</body>
<r:script >

    function showErrorMessage(errorField) {
        $("#error-messages").css("display","block");
        $("#error-messages ul").css("display","block");
        $("#error-messages ul").html(errorField);
        $("html, body").animate({ scrollTop: 0 }, "slow");
    }

    function validateRefundForm() {
        let isValid = false;


        if($('#refundAmount').val() == "") {
            showErrorMessage("<li><g:message code="refund.amount.mandatory"/></li>");
        }
        else if(isNaN($('#refundAmount').val())){
            showErrorMessage("<li><g:message code="refund.amount.integer.only"/></li>");
        }
        else if(parseFloat($('#refundAmount').val())<1){
            showErrorMessage("<li><g:message code="refund.amount.positive"/></li>");
        }
        else if(parseFloat($('#refundAmount').val()) > ${maxRefundLimit}){
            showErrorMessage("<li><g:message code="error.refund.amount.validation" args="[maxRefundLimit]"/></li>");
        }
        else if(parseFloat($('#refundAmount').val())>${walletBalance}){
            showErrorMessage("<li><g:message code="wallet.balance.insufficient"/></li>");
        }else if($('#notes').val() == "") {
            showErrorMessage("<li><g:message code="suspension.page.note"/></li>");
        }
        else{
            isValid = true;
        }
        if(isValid){
            $('#btnRefund').prop('disabled', true);
        }
        return isValid;
    }
    </r:script>
</html>




