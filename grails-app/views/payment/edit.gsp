%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.common.Constants" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<g:set var="isNew" value="${!payment || !payment?.id || payment?.id == 0}"/>

<html>
<head>
    <meta name="layout" content="main"/>

    <r:script disposition="head">
	    $(document).ready(function() {
	        // disable the cheque box of Submit To Payment Gateway
	        <g:if test="${!isRealtimePayment}">
	        	$('#submitToPaymentGateway').attr({disabled:true});
	        </g:if>
	        <g:elseif test="${isRealtimePayment}">
	        var submitToPaymentGatewayPreference = $("#submitToPaymentGatewayPreference").attr('value');
	        if (submitToPaymentGatewayPreference == 1) {
	        	$('#submitToPaymentGateway').prop('checked', true);
	        } else {
	        	$('#submitToPaymentGateway').prop('checked', false);
	        }
	        </g:elseif>
	    });

    	function togglePaymentType(element) {
            $('.box-cards.payment-type').not(element).each(function () {
                // toggle slide
                closeSlide(this);
                $(this).find(':input').prop('disabled','true');

                // toggle "process now" for cheque payments
                if ($(element).attr('id') == 'cheque') {
                    $('#submitToPaymentGateway').prop('checked','').prop('disabled','true');
                } else {
                    $('#submitToPaymentGateway').prop('disabled','');
                }
            });

            $(element).find(':input').prop('disabled','');
        }

        function clearInvoiceSelection() {
            $(':input[type=radio][name=invoiceId]').prop('checked','');
            $("#payment_amountAsDecimal").val('');
            $("#payment_amountAsDecimal").prop('disabled', '');
            $("#paymentContainer").slideDown(1000);
        }

        function storeValues(index) {
            // update the value of amount
            var amount = $("#payment-amount-"+index).attr('value');
            $("#payment_amountAsDecimal").attr('value', amount).prop('disabled', '');
            $('#refund_cb').prop('checked', true);
            $("#invoicesContainer").slideUp(1000);
            
        }

        function storeInvoiceValues(index) {
            //alert(index);
            // update the value of amount
            var amount = $("#invoice-" + index + "-amount").attr('value');
            //alert(amount);
            $("#payment_amountAsDecimal").attr('value', amount).prop('disabled', '');
            $("#paymentContainer").slideUp(1000);
        }
        
        function clearPaymentSelection() {
            // clear the selected payment
            // reset the amount back to zero
            $("#payment_amountAsDecimal").val('').prop('disabled', '');
            $('#refund_cb').prop('checked', false).prop('disabled', '');
            $(".paymentRadio").each(function(){
                $(this).prop('checked',false);
            });
            $("#invoicesContainer").slideDown(1000);
        }
        function toggleCVV() {
            if($('#refund_cb').is(":checked")){
                $('#payment-method-cvv').hide();
            } else {
                $('#payment-method-cvv').show();

            }
        }
        function validateCVV(element) {
        if($('#payment-method-cvv').is(":visible")) {
            if($(element).val() == "") { //validate blank
                $("#error-messages").css("display","block");
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="invalid.cvv.blank"/></li>");
                element.focus();
                $("html, body").animate({ scrollTop: 0 }, "slow");
                return false;
            } else {
               if(isNaN($(element).val())) {
	                $("#error-messages").css("display","block");
	                $("#error-messages ul").css("display","block");
	                $("#error-messages ul").html("<li><g:message code="invalid.cvv.insert.numbers"/></li>");
	                element.focus();
	                $("html, body").animate({ scrollTop: 0 }, "slow");
	                return false;
                }
                if($(element).val().length < 3) {
	                $("#error-messages").css("display","block");
	                $("#error-messages ul").css("display","block");
	                $("#error-messages ul").html("<li><g:message code="invalid.cvv.length"/></li>");
	                element.focus();
	                $("html, body").animate({ scrollTop: 0 }, "slow");
	                return false;
                } else {
                    $('#payment-edit-form').submit()
                }
            }
            } else {
                $('#payment-edit-form').submit()
            }
        }
    </r:script>
</head>
<body>
<div class="form-edit">
<g:hiddenField id="submitToPaymentGatewayPreference" name="submitToPaymentGatewayPreference" value="${submitToPaymentGatewayPreference}"/>
<g:hiddenField id="changeSubmitToPaymentGatewayPermission" name="changeSubmitToPaymentGatewayPermission" value="${changeSubmitToPaymentGatewayPermission}"/>
<g:hiddenField id="isRealtimePayment" name="isRealtimePayment" value="${isRealtimePayment}"/>    
    <div class="heading">
        <strong>
            <g:if test="${!isNew}">
                <g:if test="${payment.isRefund > 0}">
                    <g:message code="payment.edit.refund.title"/>
                </g:if>
                <g:else>
                    <g:message code="payment.edit.payment.title"/>
                </g:else>
            </g:if>
            <g:else>
                <g:message code="payment.new.payment.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="payment-edit-form" action="confirm" useToken="true">
            <fieldset>
            	<g:if test="${payOwned }">
            	
					<g:hiddenField name="payOwned" value="true"/>
            	</g:if>
                <!-- invoices to pay -->
                <g:hiddenField name="paymentId" value="${payment.paymentId}"/>
            <div id="invoicesContainer">
                <g:if test="${invoices}">
                    <div id="invoices" class="box-cards box-cards-open">
                        <div class="box-cards-title">
                            <a class="btn-open"><span><g:message code="payment.payable.invoices.title"/></span></a>
                        </div>
                        <div class="box-card-hold">

                            <table cellpadding="0" cellspacing="0" class="innerTable">
                                <thead class="innerHeader">
                                <tr>
                                    <th><g:message code="invoice.label.number"/></th>
                                    <th><g:message code="invoice.label.payment.attempts"/></th>
                                    <th><g:message code="invoice.label.total"/></th>
                                    <th><g:message code="invoice.label.balance"/></th>
                                    <th><g:message code="invoice.label.duedate"/></th>
                                    <th><!-- action --> &nbsp;</th>
                                </tr>
                                </thead>
                                <tbody>
                                <g:set var="selectedInvoiceCurrencyId" value=""/>
                                <g:set var="currencyInvoice" value=""/>
                                <g:each var="invoice" in="${invoices}" status="counter">
                                    <g:set var="currency" value="${currencies.find { it.id == invoice.currencyId }}"/>
                                    <g:set var="currencyInvoice" value="${currencies.find { it.id == invoice.currencyId }}"/>
                                    <g:if test="${invoice.id == invoiceId}">
                                        <g:set var="selectedInvoiceCurrencyId" value="${invoice.currencyId}"/>
                                    </g:if>
                                    <tr>
                                        <td class="innerContent">
                                            <g:applyLayout name="form/radio">
                                                <g:radio id="invoice-${invoice.id}" name="invoiceId" value="${invoice.id}" checked="${invoice.id == invoiceId}" onclick="storeInvoiceValues(${invoice.id})"/>
                                                <label for="invoice-${invoice.id}" class="rb">
                                                    <g:message code= "payment.link.invoice" args="[invoice.number]"/>
                                                </label>
                                            </g:applyLayout>
                                        </td>
                                        <td class="innerContent">
                                            ${invoice.paymentAttempts}
                                            <g:hiddenField name="invoice-${invoice.id}-curid" value="${currency?.id}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${invoice.getTotalAsDecimal()}" type="currency" currencySymbol="${currency?.symbol}"/>
                                            <g:hiddenField name="invoice-${invoice.id}-amount" value="${formatNumber(number: invoice.total, formatName: 'money.format')}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${invoice.getBalanceAsDecimal()}" type="currency" currencySymbol="${currency?.symbol}"/>
                                            <g:hiddenField name="invoice-${invoice.id}-balance" value="${formatNumber(number: invoice.balance, formatName: 'money.format')}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatDate date="${invoice.dueDate}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:link controller="invoice" action="list" id="${invoice.id}">
                                                <g:message code= "payment.link.view.invoice" args="[invoice.number]"/>
                                            </g:link>
                                        </td>
                                    </tr>
                                </g:each>
                                </tbody>
                            </table>

                            <div class="btn-row">
                                <a onclick="clearInvoiceSelection();" class="submit delete"><span><g:message code="button.clear"/></span></a>
                            </div>

                        </div>
                    </div>
                </g:if>
            </div>

            %{--Payments made --}%
            <div id="paymentContainer">
            <g:if test="${refundablePayments}">
                    <div id="payments" class="box-cards box-cards-open">
                        <div class="box-cards-title">
                            <a class="btn-open"><span><g:message code="payment.paid.title"/></span></a>
                        </div>
                        <div class="box-card-hold">

                            <table cellpadding="0" cellspacing="0" class="innerTable">
                                <thead class="innerHeader">
                                <tr>
                                    <th><g:message code="payment.id"/></th>
                                    <th><g:message code="payment.date"/></th>
                                    <th><g:message code="payment.amount"/></th>
                                    <th><g:message code="payment.method"/></th>
                                    <th><g:message code="payment.notes"/></th>
                                    <th><!-- action --> &nbsp;</th>
                                </tr>
                                </thead>
                                <tbody>
                                <g:set var="selectedPaymentCurrencyId" value=""/>
                                <g:each var="refundPayment" in="${refundablePayments}" status="counter">
                                    <g:set var="currency" value="${currencies.find { it.id == refundPayment?.getCurrency()?.getId()}}"/>
                                    <g:if test="${refundPayment?.id == refundPaymentId}">
                                        <g:set var="selectedPaymentCurrencyId" value="${refundPayment?.getCurrency()?.getId()}"/>
                                    </g:if>
                                    <tr>
                                        <td class="innerContent">
                                            <g:applyLayout name="form/radio">
                                                <g:radio id="payment-${refundPayment.id}" class="paymentRadio" name="payment.paymentId" value="${refundPayment.id}"
                                                         checked="${refundPayment.id == payment.paymentId}"
                                                         onclick="storeValues(${counter});toggleCVV()" />
                                                <label for="payment-${refundPayment.id}" class="rb">
                                                    ${refundPayment.id}
                                                </label>
                                            </g:applyLayout>
                                        </td>
                                        <td class="innerContent">
                                           <g:formatDate date="${refundPayment.getCreateDatetime()}"/>
                                            <g:hiddenField name="payment-${refundPayment.id}-curid" value="${currency.id}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${refundPayment.getBalance()}" type="currency" currencySymbol="${currency.symbol}"/>
                                            <g:hiddenField id="payment-amount-${counter}" name="payment-amount-${counter}" value="${formatNumber(number: refundPayment.getBalance(), formatName: 'money.format')}"/>
                                        </td>
                                        <td class="innerContent">
                                            ${refundPayment?.getPaymentMethod()?.getDescription()}
                                        </td>
                                        <td class="innerContent">
                                           <div style="width:200px;margin:auto;white-space: pre;white-space: pre-wrap;white-space: pre-line;white-space: -o-pre-wrap;white-space: -moz-pre-wrap;word-wrap: break-word;    ">
	                                           ${refundPayment.getPaymentNotes()}
                                           </div>
                                        </td>
                                    </tr>
                                </g:each>
                                </tbody>
                            </table>

                            <div class="btn-row">
                                <a onclick="clearPaymentSelection();" class="submit delete"><span><g:message code="button.clear"/></span></a>
                            </div>

                        </div>
                    </div>
            </g:if>
            </div>

                <!-- payment details  -->
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="payment.id"/></content>

                            <g:if test="${!isNew}"><span>${payment.id}</span></g:if>
                            <g:else><span><em><g:message code="prompt.id.new"/></em></span></g:else>

                            <g:hiddenField name="payment.id" value="${payment?.id}"/>
                        </g:applyLayout>

                        <g:if test="${!isNew}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="payment.attempt"/></content>
                                <span>${payment.attempt}</span>
                                <g:hiddenField name="payment.attempt" value="${payment?.attempt}"/>
                            </g:applyLayout>
                        </g:if>

                        <g:if test="${!isNew}">
                            <g:set var="currency" value="${currencies.find { it.id == payment?.currencyId }}"/>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.user.currency"/></content>
                                <span>${currency?.getDescription(session['language_id']) ?: payment.currencyId}</span>
                                <g:hiddenField name="payment.currencyId" value="${payment?.currencyId}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="prompt.user.currency"/></content>
                                <content tag="label.for">payment.currencyId</content>
                                <g:select name="payment.currencyId"
                                          from="${currencyInvoice ? currencyInvoice : currencies.find { it.id == user?.currencyId }}"
                                          value="${selectedInvoiceCurrencyId}" 
                                          optionKey="id"
                                          optionValue="${{it.getDescription(session['language_id'])}}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:if test="${payment.isRefund == 1}">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="payment.amount"/><span id="mandatory-meta-field">*</span></content>
                                <content tag="label.for">payment.amountAsDecimal</content>
                                <g:set var="paymentAmount" value="${payment?.amount ?: invoices?.find{ it.id == invoiceId }?.balance }"/>
                                <g:textField class="field" id="payment_amountAsDecimal" name="payment.amountAsDecimal" readonly="readonly" value="${formatNumber(number: paymentAmount, formatName: 'money.format')}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="payment.amount"/><span id="mandatory-meta-field">*</span></content>
                                <content tag="label.for">payment.amountAsDecimal</content>
                                <g:set var="paymentAmount" value="${payment?.amount ?: invoices?.find{ it.id == invoiceId }?.balance }"/>
                                <g:textField class="field" id="payment_amountAsDecimal" name="payment.amountAsDecimal" value="${formatNumber(number: paymentAmount, formatName: 'money.format')}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="payment.date"/></content>
                            <content tag="label.for">payment.paymentDate</content>
                            <g:set var="paymentDate" value="${payment?.paymentDate ?: TimezoneHelper.currentDateForTimezone(session['company_timezone'])}"/>
                            <g:textField class="field" name="payment.paymentDate" value="${formatDate(date: paymentDate, formatName: 'datepicker.format')}"/>
                        </g:applyLayout>

                        <g:if test="${isNew}">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="payment.is.refund.payment"/></content>
                                <content tag="label.for">refund_cb</content>
                                <g:checkBox id="refund_cb" class="cb checkbox" name="isRefund" checked="${payment?.isRefund > 0}" onChange = "toggleCVV()"/>
                                %{--<g:hiddenField id="refund_cb_hidden" name="isRefund" value=""/>--}%
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="payment.is.refund.payment"/></content>
                                <span><g:formatBoolean boolean="${payment?.isRefund > 0}"/></span>
                                <g:hiddenField name="payment.isRefund" value="${payment?.isRefund?.intValue()}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:if test="${isNew}">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="submit.to.payment.gateway"/></content>
                                <content tag="label.for">submitToPaymentGateway</content>
                                <g:checkBox id="submitToPaymentGateway" class="cb checkbox" name="submitToPaymentGateway" value="${submitToPaymentGateway}"/>
                            </g:applyLayout>
                        </g:if>

                        <!-- meta fields -->
                        <g:render template="/metaFields/editMetaFields" model="[ availableFields: availableFields, fieldValues: payment?.metaFields ]"/>
                    </div>

                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="payment.user.id"/></content>
                            <span><g:link controller="customer" action="list" id="${user.userId}">${user.userId}</g:link></span>
                            <g:hiddenField name="payment.userId" value="${user.userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.login.name"/></content>
                            <span>${displayer?.getDisplayName(user)}</span>
                        </g:applyLayout>

                        <g:if test="${user.contact?.firstName || user.contact?.lastName}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.customer.name"/></content>
                                <em>${user.contact.firstName} ${user.contact.lastName}</em>
                            </g:applyLayout>
                        </g:if>

                        <g:if test="${user.contact?.organizationName}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.organization.name"/></content>
                                <em>${user.contact.organizationName}</em>
                            </g:applyLayout>
                        </g:if>

                    </div>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

				<!-- Payment Methods -->
				<div id="payment-methods" class="box-cards box-cards-open" >
                   	    <div class="box-cards-title">
                       	    <a class="btn-open"><span>
                           	    <label><g:message code="promt.payment.methods"/></label>
                           	</span></a>
                       	</div>
                       	<div id= "payment-method-main" class="box-card-hold">
                       		<g:if test="${paymentMethods?.size() > 0}">
								<g:render template="/payment/paymentMethods" model="[ paymentMethods: paymentMethods, paymentInstruments : paymentInstruments ]"/>
							</g:if>
							<g:else>
								<g:message code="prompt.payment.method.types.not.available"/>
							</g:else>
						</div>
           		</div>
           		
                <!-- box text -->
                <div class="box-text">
                    <label for="payment.paymentNotes"><g:message code="payment.notes"/></label>
                    <g:textArea name="payment.paymentNotes" value="${payment?.paymentNotes}" rows="5" cols="60"/>
                </div>

                <div class="buttons">
                    <ul>
                    	<g:if test="${paymentMethods?.size() > 0}">
	                        <li>
	                            <a onclick="validateCVV($('#cvv'))" class="submit payment button-primary">
	                                <span><g:message code="button.review.payment"/></span>
	                            </a>
	                        </li>
                        </g:if>
                        <li>
                            <g:settingEnabled property="hbase.audit.logging">
                                <g:if test="${!isNew}">
                                    <sec:access url="/payment/history">
                                        <g:link controller="payment" action="history" id="${payment?.id}" class="submit show"><span><g:message code="button.view.history"/></span></g:link>
                                    </sec:access>
                                </g:if>
                            </g:settingEnabled>
                        </li>
                        <li>
                            <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>

</div>
<script type="text/javascript">
    $(document).ready(function() {
        removePaymentMethodIdOnChange();
    });

    function removePaymentMethodIdOnChange(){
        var inputEles = $("#payment-method-main").find("input");
        for(var j=0;j<inputEles.length;j++) {
            if($(inputEles[j]).attr("id").match(/[0-9]+/) != null) {
                $(inputEles[j]).change(function() {
                    var idx = $(this).attr("id").match(/[0-9]+/)[0];
                    var idName = "#paymentMethod_"+idx+"\\.id";
                    $(idName).val("");
                });
            }
        }

        var selectEles = $("#payment-method-main").find("select");
        for(var j=0;j<selectEles.length;j++) {
            if($(selectEles[j]).attr("id").match(/[0-9]+/) != null) {
                $(selectEles[j]).change(function() {
                    var idx = $(this).attr("id").match(/[0-9]+/)[0];
                    var idName = "#paymentMethod_"+idx+"\\.id";
                    $(idName).val("");
                });
            }
        }

        $("#payment-method-add").hide();
    }
</script>
</body>
</html>
