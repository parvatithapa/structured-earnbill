%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see 
<http://www.gnu.org/licenses/>.
  --}%

<%@ page import="com.sapienter.jbilling.common.CommonConstants;" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.payment.PaymentMethodTypeBL;" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldType;" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldBL;" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL; com.sapienter.jbilling.common.Constants;"%>

<g:set var="modelIndex" value="${0}"/>

<div id="paymentMethod">
    <g:hiddenField name="modelIndex" value="${0}"/>
    <g:hiddenField name="currentIndex" value="${0}"/>
    <g:hiddenField name="accountTypeId" value="${accountTypeId}"/>
    <g:hiddenField id="submitToPaymentGatewayPreference" name="submitToPaymentGatewayPreference"
                   value="${submitToPaymentGatewayPreference}"/>
    <g:hiddenField id="changeSubmitToPaymentGatewayPermission" name="changeSubmitToPaymentGatewayPermission"
                   value="${changeSubmitToPaymentGatewayPermission}"/>
	<g:hiddenField id="isRealtimePayment" name="isRealtimePayment" value="${isRealtimePayment}"/>
	<g:if test="${(paymentInstruments && paymentInstruments?.size() > 0)}">
        <g:each in="${paymentInstruments}" var="instrument">
            <%
                def pmt = PaymentMethodTypeDTO.get(instrument.paymentMethodTypeId)
                def types = new ArrayList<MetaFieldType>()
                types.add(MetaFieldType.AUTO_PAYMENT_LIMIT)
                def metaFields = PaymentMethodTypeBL.getFilteredMetaFields(pmt, types)
            %>
            <div class="form-columns">
                <g:if test="${modelIndex != 0}">
                    <hr/>
                </g:if>
                <div class="column">
                    <g:hiddenField name="paymentMethod_${modelIndex}.id" value="${instrument?.id}"/>
                    <!-- payment method types drop down -->
                    <div class="row">
                        <div class="add-desc">
                            <label><g:message code='payment.method.type.name'/></label>
                            <g:applyLayout name="form/select_holder">
                                <content tag="include.script">true</content>
                                <content tag="label.for">paymentMethod_${modelIndex}.paymentMethodTypeId</content>
                                <g:select        name = "paymentMethod_${modelIndex}.paymentMethodTypeId"
                                                 from = "${paymentMethods}"
                                            optionKey = "id"
                                          optionValue = "methodName"
                                                value = "${instrument?.paymentMethodTypeId}"
                                             onChange = "refreshPaymentInstrument(${modelIndex});"/>
                            </g:applyLayout>
                            <g:if test="${modelIndex != 0}">
                                <a onclick="removePaymentInstrument(this, ${modelIndex});">&#xe000;</a>
                            </g:if>
                        </div>
                    </div>

                    <g:applyLayout name="form/input">
                        <content tag="label">
                            <g:message code="payment.method.processing.order"/>
                            <span id="mandatory-meta-field">*</span>
                        </content>
                        <content tag="label.for">paymentMethod_${modelIndex}.processingOrder</content>
                        <g:textField class = "field"
                                      name = "paymentMethod_${modelIndex}.processingOrder"
                                     value = "${instrument?.processingOrder >= 0 ? instrument?.processingOrder : 1}"/>
                    </g:applyLayout>

                        <g:set var="CVV_CONDITION" value="${PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_REQUIRE_CVV_FOR_ONE_TIME_PAYMENTS)}"/>
                        <g:if test="${CVV_CONDITION && instrument?.id == null && PaymentMethodTypeDTO.get(instrument?.paymentMethodTypeId).getPaymentMethodTemplate().getTemplateName().equals("Payment Card") }">
                        <div id = "payment-method-cvv">
                        <g:applyLayout name="form/input">
                            <content tag="label">
                                <g:message code="payment.method.cvv"/>
                                <span id="mandatory-meta-field">*</span>
                            </content>
                            <g:passwordField maxlength="4" class = "field" id = "cvv" name = "cvv" value = "${instrument?.cvv}"/>
                        </g:applyLayout>
                        </div>
                        </g:if>
                </div>
                <div id="payment-method-fields-${modelIndex}" class="column">
                    <g:render template = "/metaFields/editMetaFields"
                                 model = "[availableFields: metaFields, fieldValues: instrument?.metaFields]"/>
                </div>
            </div>
            <g:set var="modelIndex" value="${modelIndex + 1}"/>
        </g:each>
        <g:set var="modelIndex" value="${modelIndex - 1}"/>
    </g:if>
    <g:else>
        <!-- If payment instruments are null -->
        <%
            def metaFields = paymentMethods.iterator().next().metaFields?.sort { it.displayOrder }
        %>

        <div class="form-columns">
            <div class="column">
            <!-- payment method types drop down -->
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="payment.method.type.name"/></content>
                    <content tag="label.for">paymentMethod_${modelIndex}.paymentMethodTypeId</content>
                    <content tag="include.script">true</content>
                    <g:select        name = "paymentMethod_${modelIndex}.paymentMethodTypeId"
                                     from = "${paymentMethods}"
                                optionKey = "id"
                              optionValue = "methodName"
                                 onChange = "refreshPaymentInstrument(${modelIndex});"/>
                </g:applyLayout>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="payment.method.processing.order"/><span
                            id="mandatory-meta-field">*</span></content>
                    <content tag="label.for">paymentMethod_${modelIndex}.processingOrder</content>
                    <g:textField class = "field"
                                  name = "paymentMethod_${modelIndex}.processingOrder"
                                 value = "${1}"/>
                </g:applyLayout>
            </div>

            <div id="payment-method-fields-${modelIndex}" class="column">
                <g:render template="/metaFields/editMetaFields"
                          model="[availableFields: metaFields, fieldValues: null]"/>
            </div>
        </div>
    </g:else>

<!-- spacer -->
    <div>
        <br/>&nbsp;
    </div>

    <!-- controls -->
    <div id="payment-method-add" class="btn-row">
        <a class="submit add" onclick="addPaymentInstrument()">
            <span><g:message code="button.add.payment.instrument"/></span>
        </a>
    </div>
    <script type="text/javascript">
        $(document).ready(function () {
            modifyMetaFieldName();
            var submitToPaymentGatewayPreferenceValue = $("#submitToPaymentGatewayPreference").attr('value');
            var changeSubmitToPaymentGatewayPermission = $("#changeSubmitToPaymentGatewayPermission").attr('value');

            <g:if test="${!isRealtimePayment}">
            $('#submitToPaymentGateway').attr({disabled: true}).prop({checked: false});
            </g:if>
            <g:else >
            $('#submitToPaymentGateway').prop('disabled', changeSubmitToPaymentGatewayPermission == 'true');
            if (submitToPaymentGatewayPreferenceValue == 1) {
                $('#submitToPaymentGateway').prop({checked: true});
            }
            </g:else>
        });

        function modifyMetaFieldName() {
            $('#modelIndex').val(${modelIndex});
            for (var i = 0; i <=${modelIndex}; i++) {
                var inputEles = $("#payment-method-fields-" + i).find("input,select");
                // add instruction to find select and checkboxes too
                for (var j = 0; j < inputEles.length; j++) {
                    var elementName = inputEles[j].name
                    inputEles[j].setAttribute("name", "" + i + "_" + elementName);
                    inputEles[j].setAttribute("id", "" + i + "_" + elementName);

                    if ($(inputEles[j]).hasClass('dateobject')) {
                        $(inputEles[j]).datepicker({
                                 dateFormat: "${message(code: 'datepicker.jquery.ui.format')}",
                                     showOn: "both",
                                buttonImage: "${resource(dir:'images', file:'icon04.gif')}",
                            buttonImageOnly: true
                        });
                    }
                }
            }
        }

        function addPaymentInstrument() {
            $.ajax({
                type: 'POST',
                 url: '${createLink(controller: 'payment', action: 'addPaymentInstrument')}',
                data: $('#paymentMethod *').serialize(),
                success: function (data) {
                    $("#payment-method-main").html(data)
                }
            });
            setTimeout(function() { toggleCVV() }, 500);
        }

	    function refreshPaymentInstrument(currentIndex) {
	    	$('#currentIndex').val(currentIndex);
	    	$('#paymentMethod_' + currentIndex + '\\.id').val('');
			$.ajax({
                type: 'POST',
                 url: '${createLink(controller: 'payment', action: 'refreshPaymentInstrument')}',
                data: $('#paymentMethod *').serialize(),
                success: function (data) {
                    $("#payment-method-main").html(data)
                }
            });
        setTimeout(function() { toggleCVV() }, 500);
        }

        function removePaymentInstrument(element, modelIndex) {
            $('#currentIndex').val(modelIndex);
            $.ajax({
                type: 'POST',
                 url: '${createLink(controller: 'payment', action: 'removePaymentInstrument')}',
                data: $('#paymentMethod *').serialize(),
                success: function (data) {
                    $('#payment-method-main').html(data)
                }
            });
        }


    </script>
</div>
