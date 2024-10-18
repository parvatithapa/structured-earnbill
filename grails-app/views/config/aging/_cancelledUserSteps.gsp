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

<%--
  Editor form for the cancelled users ageing steps

  @author Ashwinkumar
  @since  19-July-2017
--%>

<%@page import="com.sapienter.jbilling.server.process.CollectionType" %>

    <div id="cancel_ageing">
        <g:hiddenField name="recCnt" value="${cancelledAgeingSteps?.length}"/>
        <g:hiddenField name="disable" value="${disableCancelledTab}"/>
        <g:hiddenField name="collectionType" value="${CollectionType.CANCELLATION_INVOICE}"/>
        <g:if test="${disableCancelledTab}">
        <div class="msg-box error">
                <g:message code="config.ageing.warn.collections.cancelled.accounts"/>
        </div>
        </g:if>
        <fieldset>
            <div class="form-columns single width-auto pad-left-right">
                <table id="cancel_ageingStepTable" class="innerTable pad-below">
                    <thead class="innerHeader margins">
                    <tr>
                        <th class="left tiny2 first"><g:message code="config.ageing.step.id"/></th>
                        <th class="left tiny2"><g:message code="config.ageing.step"/></th>
                        <th class="left tiny2"><g:message code="config.ageing.forDays"/></th>
                        <th class="left medium"><g:message code="config.ageing.sendNotification"/></th>
                        <th class="left medium"><g:message code="config.ageing.retryPayment"/></th>
                        <th class="last"/>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each status="iter" var="cancel_step" in="${cancelledAgeingSteps}">
                        <tr>
                            <td class="first tiny2">
                                <strong>${cancel_step.statusId}</strong>
                            </td>
                            <td class="medium2">
                                <g:textField class="inp-bg inp-desc" name="obj[${iter}].statusStr" value="${cancel_step.statusStr}"/>
                            </td>
                            <td class="medium">
                                <g:field type="number" class="inp-bg inp4" name="obj[${iter}].days" value="${cancel_step.days}"/>
                            </td>
                            <td class="tiny">
                                <g:checkBox class="cb checkbox" name="obj[${iter}].sendNotification" checked="${cancel_step.sendNotification }"/>

                            </td>
                            <td class="tiny">
                                <g:checkBox class="cb checkbox" name="obj[${iter}].paymentRetry" checked="${cancel_step.paymentRetry }"/>
                                <g:hiddenField name="obj[${iter}].suspended" value="false"/>
                                <g:hiddenField value="${cancel_step?.statusId}" name="obj[${iter}].statusId"/>
                                <g:hiddenField value="placeholder_text" name="obj[${iter}].welcomeMessage"/>
                                <g:hiddenField value="placeholder_text" name="obj[${iter}].failedLoginMessage"/>
                                <g:hiddenField name="obj[${iter}].stopActivationOnPayment" value="false"/>
                            </td>
                                <td class="tiny">
                                <g:if test="${!cancel_step?.inUse && !disableCancelledTab}">
                                    <a class="plus-icon" onclick="removeCancelAgeingStep(this, ${iter});">&#xe000;</a>
                                </g:if>
                            </td>
                        </tr>
                    </g:each>

                    <g:set var="newCancelStepIndex" value="${cancelledAgeingSteps.size()}"/>
                    <tr>
                        <td class="tiny2">
                            <strong></strong>
                        </td>
                        <td class="medium2">
                            <g:textField class="inp-bg inp-desc" name="obj[${newCancelStepIndex}].statusStr" onchange="addCancelAgeingStep(this, ${newCancelStepIndex})"/>
                        </td>
                        <td class="medium">
                            <g:field type="number" class="inp-bg inp4" name="obj[${newCancelStepIndex}].days"/>
                        </td>
                        <td class="tiny">
                            <g:checkBox class="cb checkbox" name="obj[${newCancelStepIndex}].sendNotification" checked="${false}"/>

                        </td>
                        <td class="tiny">
                            <g:checkBox class="cb checkbox" name="obj[${newCancelStepIndex}].paymentRetry" checked="${false}"/>
                            <g:hiddenField name="obj[${newCancelStepIndex}].suspended" value="false"/>
                            <g:hiddenField name="obj[${newCancelStepIndex}].statusId"/>
                            <g:hiddenField value="placeholder_text" name="obj[${newCancelStepIndex}].welcomeMessage"/>
                            <g:hiddenField value="placeholder_text" name="obj[${newCancelStepIndex}].failedLoginMessage"/>
                            <g:hiddenField name="obj[${newCancelStepIndex}].stopActivationOnPayment" value="false"/>
                        </td>
                        <td class="tiny">
                        	<g:if test="${!disableCancelledTab}">
								<a class="plus-icon" onclick="addCancelAgeingStep(this, ${newCancelStepIndex})">&#xe026;</a>
                           	</g:if>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
        <div class="btn-row buttons margins">
        	<g:if test="${!disableCancelledTab}">
            	<a onclick="$('#save-cancel-aging-form').submit();" class="submit save button-primary" ><span><g:message code="button.save"/></span></a>
            </g:if>
            <g:link controller="config" action="index" class="submit cancel" ><span><g:message code="button.cancel"/></span></g:link>
        </div>

        <g:hiddenField name="cancelStepIndex"/>


        <script type="text/javascript">
            $(".numericOnly").keydown(function(event){
                // Allow only backspace, delete, left & right
                if ( event.keyCode==37 || event.keyCode== 39 || event.keyCode == 46 || event.keyCode == 8 || event.keyCode == 9 ) {
                    // let it happen, don't do anything
                }
                else {
                    // Ensure that it is a number and stop the keypress
                    if (event.keyCode < 48 || event.keyCode > 57 ) {
                        event.preventDefault();
                    }
                }
            });

            function addCancelAgeingStep(element, cancelStepIndex) {

                $('#cancelStepIndex').val(cancelStepIndex);

                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'addCancelAgeingStep')}',
                    data: $('#cancel_ageing').parents('form').serialize(),
                    success: function(data) {
                        $('#cancel_ageing').replaceWith(data);
                        $('input[name="obj['+cancelStepIndex+'].days"]').focus();
                    }
                });
            }

            function removeCancelAgeingStep(element, cancelStepIndex) {

                $('#cancelStepIndex').val(cancelStepIndex);

                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'removeCancelAgeingStep')}',
                    data: $('#cancel_ageing').parents('form').serialize(),
                    success: function(data) {
                        $('#cancel_ageing').replaceWith(data);
                    }
                });
            }
        </script>
</div>
