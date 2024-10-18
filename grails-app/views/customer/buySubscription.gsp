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

<%@ page import="com.sapienter.jbilling.server.item.db.PlanDAS"%>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.TRN_STATUS_BUY_SUBSCRIPTION;" %>

<html>
<head>
    <meta name="layout" content="main"/>
    <style>
        span.select-value{max-width: fit-content;}
        #id{width: 510px;}
        #primaryPlanId{max-width: inherit;}
    </style>
</head>
<body>
<div class="form-edit">
    <div class="heading" data-cy="buySubscription">
        <strong>
            <g:message code="label.recharge.subscription"/>
        </strong>
    </div>
    <div class="form-hold">
            <g:form name="recharge-form" action="doRecharge" params='[caller: "${TRN_STATUS_BUY_SUBSCRIPTION}"]' useToken="true" onSubmit="return validateRechargeForm()">
                <fieldset>
                    <div class="form-columns">
                        <!-- Hidden field to pass current plan id and to decide if new plan is selected or not.-->
                        <g:hiddenField class="field" name="activePlanId" id="hiddenActivePlanId" value="0"/>

                        <!-- Hidden field for JSON representation of each plan to the list-->
                        <g:hiddenField class="field" name="planAsJson" id="planAsJson" value="${plans.collect { it.toJson() }}"/>

                        <div class="column"  id="id">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="label.adennet.iccid"/></content>
                                <content tag="label.for">iccid</content>
                                    ${user.userName}
                                   <g:hiddenField id="iccId"
                                                  name="iccId"
                                                  value="${user.userName}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="user.subscriber.number"/></content>
                                <content tag="label.for">subscriberNumber</content>
                                    ${subscriberNumber}
                                   <g:hiddenField id="subscriberNumber"
                                                  name="subscriberNumber"
                                                  value="${subscriberNumber}"/>
                            </g:applyLayout>
                            <g:applyLayout name="form/select" >
                                <content tag="label"><g:message code="recharge.primary.plan"/></content>
                                <content tag="label.for">recharge.primary.plan</content>
                                    <g:select  id="primaryPlanId"
                                               name = "primaryPlanId"
                                               from="${plans}"
                                               optionKey = "id"
                                               optionValue ="description"
                                               noSelection="${['0':message(code: 'select.option.default.value.name')]}"
                                               onchange="fetchPlanPrice(this.value)"
                                               disabled = "true"
                                               />
                            </g:applyLayout>
                            <g:applyLayout name="form/select_multiple" >
                                 <content tag="label"><g:message code="recharge.add.on.product"/></content>
                                 <content tag="label.for">addOnProduct</content>
                                    <g:select  id="addOnProductId"
                                        name="addOnProductId"
                                        multiple="true"
                                        from="${items}"
                                        optionKey="id"
                                        optionValue="${{it.description}}"
                                        value="${id}"
                                        onchange="getAddOnPrice(this.value)"
                                       />
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="recharge.sim.fee"/></content>
                                <content tag="label.for">newSimFee</content>
                                    <span id="newSimFees">0.0</span>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="recharge.plan.fee"/></content>
                                 <content tag="label.for">planFee</content>
                                    <span id="labelPlanFee">0.0</span>
                            </g:applyLayout>


                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="add.on.product.price"/></content>
                                <content tag="label.for">addOnProductPrice</content>
                                    <span id="addOnProductPrice">0.0</span>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><span id="mandatory-meta-field">*</span><g:message code="recharge.total.amount"/></content>
                                <content tag="label.for">totalRechargeAmount</content>
                                    <span id="labelRechargeAmount">0.0</span>
                                        <g:hiddenField  name="rechargeAmount" id="rechargeAmount" value="${0}"/>
                            </g:applyLayout>

                        </div>
                        <div class="column">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="recharge.user.id"/></content>
                                <content tag="label.for">userId</content>
                                    <span><g:link controller="customer" action="list"
                                                id="${userId}">${user.userId}</g:link></span>
                                       <g:hiddenField name="userId" value="${user.userId}"/>
                            </g:applyLayout>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="recharge.user.name"/></content>
                                <content tag="label.for">userName</content>
                                <g:each var="metaField" in="${user.getMetaFields()}">
                                    <g:if test="${metaField.getFieldName() == 'First Name'}">
                                        ${metaField.getValue()}
                                        <g:hiddenField id="userName"
                                            name="userName"
                                            value="${metaField.getValue()}"/>
                                    </g:if>
                                </g:each>

                            </g:applyLayout>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="user.wallet.balance"/></content>
                                <content tag="label.for">walletBalance</content>
                                    ${walletBalance}
                                   <g:hiddenField id="walletBalance"
                                                  name="walletBalance"
                                                  value="${walletBalance}"/>
                            </g:applyLayout>
                        </div>
                    </div>

                    <div><br/></div>

                    <div class="buttons">
                        <ul>
                            <li>
                                <g:actionSubmit id="btnRecharge" class="submit save button-primary"
                                value="${message(code:'recharge.buy.subscription.button')}" />
                            </li>
                            <li>
                                <a onclick= "reloadRechargeForm()"
                                class="submit "><span><g:message code="recharge.button.reset"/></span></a>
                            </li>
                        </ul>
                    </div>
                </fieldset>
            </g:form>
    </div>
</div>
</body>

<r:script>
var hiddenActivePlanId =0 ;
var activePlanPrice =0;

    $( document ).ready(function() {
        $("#subscriberNumber").val(${subscriberNumber});
        var subscriberNumber = document.forms["recharge-form"]["subscriberNumber"].value;

        if(hiddenActivePlanId > 0  ) {
            //Enable the plan dropdown for selection
            $("#primaryPlanId").prop('disabled', false);
            $('#btnRecharge').prop('disabled', false);

            // dropdown plan show on top
            $('#primaryPlanId').val(hiddenActivePlanId);

            $("#id .select-value").text($( "#primaryPlanId option:selected" ).text());

            activePlanPrice=parseFloat(getPriceByPlanId(hiddenActivePlanId))
            $('#labelPlanFee').html(activePlanPrice);
            $('#totalAmount').html(activePlanPrice);
            $('#rechargeAmount').val($('#totalAmount').html());

        }else if(hiddenActivePlanId < 0){
            resetValue();
            $("#primaryPlanId").prop('disabled', true);
            $('#btnRecharge').prop('disabled', true);
            $("#addOnProductId").prop('disabled', true);
        }
        else if(hiddenActivePlanId == 0){
            resetValue();
            $('#primaryPlanId').val(hiddenActivePlanId);
            $("#primaryPlanId").prop('disabled', false);
            $('#btnRecharge').prop('disabled', false);
        }
    });

    function resetValue(){
        $('#labelPlanFee').html(0.0);
        $('#newSimFees').html(0.0);
        $('#labelRechargeAmount').html(0.0);
        $('#rechargeAmount').val(0.0);
        $('#addOnProductPrice').html(0.0);
    }

    function fetchPlanPrice(planId) {
        if(planId == 0){
            $('#labelPlanFee').html(0.0);
            $('#addOnProductId').val(0);
            $('#newSimFees').html(0.0);
            $('#labelRechargeAmount').html(0.0);
            $('#rechargeAmount').val(0.0);
            $('#addOnProductPrice').html(0.0);
        }

        if ( planId !== undefined && planId > 0 ) {
            activePlanPrice = parseFloat(getPriceByPlanId(planId))
            $('#labelPlanFee').html(activePlanPrice);

            if(hiddenActivePlanId == 0 ) {
                $('#newSimFees').html(${simPrice});
            }

            let planFee = parseFloat(parseNumber($('#labelPlanFee').html()));
            let addOnProductPrice = parseFloat(parseNumber($('#addOnProductPrice').html()));
            let simFees = parseFloat(parseNumber($('#newSimFees').html()));
            let rechargeAmount = parseFloat(planFee  + addOnProductPrice + simFees);

            $('#labelRechargeAmount').html(rechargeAmount);
        }
    }

    function getAddOnPrice(){
        var currentAddOnProductPrice = parseFloat($('#addOnProductPrice').html());
        var totalRechargeAmount = parseFloat($('#labelRechargeAmount').html());
        var selectedAddOnProductIds = $("#addOnProductId").val();

        if (selectedAddOnProductIds != null && selectedAddOnProductIds.length > 0) {
            var totalAddOnPrice = 0;

            for (let i = 0; i < selectedAddOnProductIds.length; i++) {
                $.ajax({
                    url: '${createLink(action: 'getAddOnProductPrice')}',
                    async: false,
                    data: {itemId :  selectedAddOnProductIds[i]},
                    success: function(data) {
                        totalAddOnPrice += parseFloat(data);
                    }
                });
            }
            $('#addOnProductPrice').html(totalAddOnPrice);
            $('#labelRechargeAmount').html(totalRechargeAmount - currentAddOnProductPrice + totalAddOnPrice);
        } else {
            $('#labelRechargeAmount').html(totalRechargeAmount - currentAddOnProductPrice);
            $('#addOnProductPrice').html(0.0);
        }
        //In any scenario totalRechargeAmount should be populated as totalRechargeAmount
        $("#labelRechargeAmount").html($('#labelRechargeAmount').html());
    }

    function parseNumber(number) {
        if (number === undefined || number === null || number === '' || isNaN(number) || number < 0) {
            return undefined;
        }
    return number;
    }

    function showMessage(errorField, isError=true) {
        if(isError) {
            $("#error-messages").removeClass("info");
            $("#error-messages").addClass("error");
            $('#error-messages > strong').html('<g:message code="flash.error.title"/>');
        } else {
            $("#error-messages").removeClass("error");
            $("#error-messages").addClass("info");
            $('#error-messages > strong').html('<g:message code="flash.info.title"/>');
        }

        $("#error-messages").css("display","block");
        $("#error-messages ul").css("display","block");
        $("#error-messages ul").html(errorField);
        $("html, body").animate({ scrollTop: 0 }, "slow");
    }

    function validateRechargeForm() {
        var validated = false;
        $("#rechargeAmount").val($('#labelRechargeAmount').html());

        let subscriberNumber = parseNumber($('#subscriberNumber').val());
        let primaryPlanId = parseNumber($('#primaryPlanId').val());
        let planFee = parseFloat(parseNumber($('#labelPlanFee').html()));
        let totalRechargeAmount = parseFloat(parseNumber($('#labelRechargeAmount').html()));

        if( subscriberNumber === undefined && primaryPlanId == 0 && totalRechargeAmount > 0 ) {
            validated = true;
        } else if ( subscriberNumber !== undefined && primaryPlanId > 0 )  {
            if( subscriberNumber !== undefined && primaryPlanId === 0) {
                showMessage("<li><g:message code="recharge.primary.plan.is.selected"/></li>");
            } else if( totalRechargeAmount === undefined || totalRechargeAmount < 0){
                showMessage("<li><g:message code="recharge.amount.valid"/></li>");
            } else {
                validated = true;
            }
        } else if ( subscriberNumber !== undefined && primaryPlanId == 0 ){
            showMessage("<li><g:message code="recharge.select.plan"/></li>");
        }

        //Form should be submitted only when it is validated
        if(validated){
            $('#btnRecharge').prop('disabled', true);
        }
        return validated;
    }

    function reloadRechargeForm(){
        location.reload();
    }

    function getPriceByPlanId(planId) {
        const plans = JSON.parse($('#planAsJson').val());
        const plan = plans.find(plan => plan.id == planId);
        return plan ? plan.price : 0;
    }
</r:script>
</html>
