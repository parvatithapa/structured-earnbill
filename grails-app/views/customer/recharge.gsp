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
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.TRN_STATUS_RECHARGE;" %>

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
    <div class="heading" data-cy="recharge">
        <strong>
            <g:message code="recharge.new"/>
        </strong>
    </div>
    <div class="form-hold">
            <g:form name="recharge-form" action="doRecharge" params='[caller: "${TRN_STATUS_RECHARGE}"]' useToken="true" onSubmit="return validateRechargeForm()">
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

                            <g:if test="${isAlternateRechargeSupported}">
                                <g:applyLayout name="form/checkbox">
                                    <content tag="label"><g:message code="recharge.active.now"/></content>
                                    <g:checkBox   class = "cb checkbox"
                                                    id ="activeNow"
                                                    name = "activeNow"
                                                    onclick="showPrompt()"
                                                    value="true"
                                                    disabled = 'true'
                                                checked = "${activeNow}"/>
                                </g:applyLayout>
                            </g:if>
                            <g:else>
                                <g:hiddenField name="activeNow" value="true"/>
                            </g:else>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="recharge.plan.fee"/></content>
                                <content tag="label.for">planFee</content>
                                    <span id="labelPlanFee">0</span>
                            </g:applyLayout>

                            <g:if test="${isDowngradeFeesApplicable}">
                                <g:applyLayout name="form/text">
                                    <content tag="label"><g:message code="recharge.downgrade.fees"/></content>
                                    <content tag="label.for">downgradeFee</content>
                                        <span id="downgradeFees">0</span>
                                </g:applyLayout>
                            </g:if>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="total.amount"/></content>
                                <content tag="label.for">totalAmount</content>
                                    <span id="totalAmount">0</span>
                            </g:applyLayout>

                            <g:applyLayout name="form/input"  >
                                <content tag="label"><span id="mandatory-meta-field">* </span><g:message code="recharge.total.amount"/></content>
                                <content tag="label.for">totalRechargeAmount</content>
                                       <g:textField
                                        class="field"
                                        id="rechargeAmount"
                                        type="number"
                                         name="rechargeAmount"
                                         required="true"
                                         value="${0}"
                                      />
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
                                value="${message(code:'recharge.button')}"  />
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
var hiddenActivePlanId = 0;
var activePlanPrice = 0;
var selectedPlan;
var isAlternateRechargeSupported = ${isAlternateRechargeSupported};
var isDowngradeFeesApplicable = ${isDowngradeFeesApplicable};

    $(document).ready(function() {
            $("#subscriberNumber").val(${subscriberNumber});
            var subscriberNumber = document.forms["recharge-form"]["subscriberNumber"].value;
                getPlanId();
        });

    function getPlanId(){
        $.ajax({
             url: '${createLink(action: 'getPlanId')}',
             data: { userId : ${user.userId}, iccId : '${user.userName}' },
                 success: function(data) {
                 hiddenActivePlanId = parseFloat(data);
                 $("#hiddenActivePlanId").val(hiddenActivePlanId);

                 if(hiddenActivePlanId > 0) {
                    //Enable the plan dropdown for selection
                     $("#primaryPlanId").prop('disabled', false);
                     if(isAlternateRechargeSupported) {
                        $("#activeNow").prop('disabled', false);
                     }
                     $('#btnRecharge').prop('disabled', false);

                    // dropdown plan show on top
                    $('#primaryPlanId').val(hiddenActivePlanId);

                    $("#id .select-value").text($( "#primaryPlanId option:selected" ).text());
                    activePlanPrice=parseFloat(getByPlanId(hiddenActivePlanId).price)
                    $('#labelPlanFee').html(activePlanPrice);
                    $('#totalAmount').html(activePlanPrice);
                    $('#rechargeAmount').val($('#totalAmount').html());

                 }else if(hiddenActivePlanId < 0){
                    resetValue();
                    $("#primaryPlanId").prop('disabled', true);
                    $('#btnRecharge').prop('disabled', true);
                 }
                 else if(hiddenActivePlanId == 0){
                    resetValue();
                    $('#primaryPlanId').val(hiddenActivePlanId);
                    $("#primaryPlanId").prop('disabled', false);
                    $('#btnRecharge').prop('disabled', false);
                 }
             }
        });
    }

    function resetValue(){
        if (isAlternateRechargeSupported) {
            $("#activeNow").prop('disabled', true);
        }
        $('#primaryPlanId').val(0);
        $("#id .select-value").text($( "#primaryPlanId option:selected" ).text());
        $('#labelPlanFee').html(0.0);
        if (isDowngradeFeesApplicable) {
            $('#downgradeFees').html(0.0);
        }
        $('#totalAmount').html(0.0);
        $('#rechargeAmount').val($('#totalAmount').html());
    }

    function fetchPlanPrice(planId) {
        var selectedPlanPrice;
        if(planId == 0){
            if(isAlternateRechargeSupported) {
                $("#activeNow").prop('checked',false);
            }
            $('#labelPlanFee').html(0.0);
            if (isDowngradeFeesApplicable) {
                $('#downgradeFees').html(0.0);
            }
            $('#totalAmount').html(0.0);
            $('#labelRechargeAmount').html(0.0);
            $('#rechargeAmount').val(0.0);
            $('#addOnProductPrice').html(0.0);
        }

        if ( planId !== undefined && planId > 0 ) {
            var downgradeFees = 0.0;
            selectedPlan = getByPlanId(planId)
            selectedPlanPrice = parseFloat(selectedPlan.price)

            $('#labelPlanFee').html(selectedPlanPrice);
            if (hiddenActivePlanId != 0){
                activePlanPrice = parseFloat(getByPlanId(hiddenActivePlanId).price)
                 if(activePlanPrice > selectedPlanPrice && !selectedPlan.isAddOn && isDowngradeFeesApplicable) {
                       $('#downgradeFees').html(${downgradeFees});
                       downgradeFees = parseFloat(${downgradeFees});
                 } else {
                    $('#downgradeFees').html(0);
                }
            }

            let planFee = parseFloat(parseNumber($('#labelPlanFee').html()));
            let totalAmount = parseFloat(planFee + downgradeFees );

            $('#totalAmount').html( totalAmount );
            $('#rechargeAmount').val(totalAmount);
        }
    }

    function parseNumber(number) {
        if (number === undefined || number === null || number === '' || isNaN(number) || number < 0) {
            return undefined;
        }
    return number;
    }

    function showPrompt() {
        if($('#activeNow').is(":checked")) {
            showMessage('<li><g:message code="recharge.active.now.message"/></li>',false);
        }
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
        if(!$('#rechargeAmount').val().match(/^[0-9]+[\.]?[0-9]+$/) ){
            showMessage("<li><g:message code="recharge.total.amount.valid"/></li>");
            return validated;
        }

        let subscriberNumber = parseNumber($('#subscriberNumber').val());
        let primaryPlanId = parseNumber($('#primaryPlanId').val());
        let planFee = parseFloat(parseNumber($('#labelPlanFee').html()));
        let downgradeFees = isDowngradeFeesApplicable ? parseFloat(parseNumber($('#downgradeFees').html())) : 0.0;
        let totalAmount = parseFloat(parseNumber($('#totalAmount').html()));
        let totalRechargeAmount = parseFloat(parseNumber($('#rechargeAmount').val()));
        let walletBalance = parseFloat(parseNumber($('#walletBalance').val()));

        var calculatedTotalAmount = walletBalance + totalRechargeAmount;

        if( subscriberNumber === undefined && primaryPlanId == 0 && totalRechargeAmount > 0 && totalRechargeAmount <= 100000) {
            validated = true;
        } else if ( subscriberNumber !== undefined && primaryPlanId > 0 )  {
            if( subscriberNumber !== undefined && primaryPlanId === 0) {
                showMessage("<li><g:message code="recharge.primary.plan.is.selected"/></li>");
            }else if ( calculatedTotalAmount < totalAmount )  {
                 showMessage("<li><g:message code="recharge.wallet.balance.insufficient"/></li>");
            }else if ( $("#hiddenActivePlanId").val() === primaryPlanId &&  totalRechargeAmount === 0 && calculatedTotalAmount >= totalAmount)  {
                 showMessage("<li><g:message code="validation.error.topup"/></li>");
            }else if ( totalRechargeAmount > ${maxRechargeLimit}) {
                showMessage("<li><g:message code="error.recharge.amount.validation" args="[maxRechargeLimit]"/></li>");
            }else if ( $("#hiddenActivePlanId").val() !== primaryPlanId && totalRechargeAmount > totalAmount) {
                showMessage("<li><g:message code="validation.error.recharge.amount.grater.than.total.amount"/></li>");
            }else if ( $("#hiddenActivePlanId").val() !== primaryPlanId && totalRechargeAmount < downgradeFees ) {
                showMessage("<li><g:message code="validation.error.recharge.amount.less.than.downgrade.fee"/></li>");
            }else {
               validated = true;
            }
        } else if ( subscriberNumber !== undefined && primaryPlanId == 0 ){
            showMessage("<li><g:message code="recharge.select.plan"/></li>");
        } else if ( totalRechargeAmount > ${maxRechargeLimit}) {
            showMessage("<li><g:message code="error.recharge.amount.validation" args="[maxRechargeLimit]"/></li>");
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

    function getByPlanId(planId) {
        const plans = JSON.parse($('#planAsJson').val());
        const plan = plans.find(plan => plan.id == planId);
        return plan;
    }

</r:script>
</html>
