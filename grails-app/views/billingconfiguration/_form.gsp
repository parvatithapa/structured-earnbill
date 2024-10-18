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

<%@page import="com.sapienter.jbilling.server.process.db.ProratingType"%>
<%@page import="com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>

<%-- 
<g:javascript library="ui.core"/>
<g:javascript library="ui.spinner"/>
--%>

<div class="form-edit" style="width:650px">

    <div class="heading">
        <strong><g:message code="configuration.title.billing"/></strong>
    </div>

    <div class="form-hold">
        <g:form name="save-billing-form" action="saveConfig" useToken="true">
            <fieldset>
                <div class="form-columns">
                	<%--Use two columns --%>
                    <div class="one_column" style="width:650px">
                    	<div class="row">
							<g:applyLayout name="form/date">
	                             <content tag="label"><g:message code="billing.next.run.date"/></content>
	                             <content tag="label.for">nextRunDate</content>
	                             <g:textField class="field" name="nextRunDate" value="${formatDate(date: configuration?.nextRunDate, formatName:'datepicker.format')}" onblur="validateDate(this)"/>
	                        </g:applyLayout>
                        </div>

						<div class="row">
	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.generate.report"/></content>
	                            <content tag="label.for">generateReport</content>
	                            <g:checkBox class="cb checkbox" name="generateReport" checked="${configuration?.generateReport > 0}"/>
	                        </g:applyLayout>
                        </div>
                        <div class="row">
	                        <g:applyLayout name="form/input">
	                            <content tag="label"><g:message code="billing.days.to.review"/></content>
	                            <content tag="label.for">daysForReport</content>
	                            <content tag="style">inp4</content>
	                            <g:field type="number" class="field" name="daysForReport" value="${configuration?.daysForReport}" maxlength="2" size="2"/>
	                        </g:applyLayout>
                        </div>
                        
                        <div class="row">
  						  <g:applyLayout name="form/select">
   							   <content tag="label"><g:message code="billing.period"/></content>
   							   <content tag="label.for">periodValue</content>
   							   <g:select id="periodUnitId" class="field" name="periodUnitId" from="${PeriodUnitDTO.list()}" optionKey="id" optionValue="description" value="${configuration?.periodUnitId}" />
   						  </g:applyLayout>
						</div>
						
						<div id="lastDayOfMonthDiv" class="row" >
							<g:applyLayout name="form/checkbox">
					            <content tag="label"><g:message code="billing.period.lastDayOfMonth"/></content>
					            <content tag="label.for">lastDayOfMonth</content>
					            <g:checkBox class="cb checkbox" name="lastDayOfMonth" checked="${configuration?.lastDayOfMonth}"/>
					        </g:applyLayout>
				        </div>

                        <div class="row">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="billing.due.date"/></content>
                                <content tag="label.for">dueDateValue</content>

                                <div class="inp-bg inp-short">
                                    <g:field type="number" class="field" name="dueDateValue" value="${configuration?.dueDateValue}" maxlength="2" size="2"/>
                                </div>

                                <g:applyLayout name="form/select_holder">
                                    <content tag="label.for">dueDateUnitId</content>
                                    <g:select class="field" name="dueDateUnitId" from="${PeriodUnitDTO.list()}"
                                              optionKey="id" optionValue="description" value="${configuration?.dueDateUnitId}" />
                                </g:applyLayout>
                            </g:applyLayout>
                        </div>

                        <div class="row">
	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.require.recurring"/></content>
	                            <content tag="label.for">onlyRecurring</content>
	                            <g:checkBox class="cb checkbox" name="onlyRecurring" checked="${configuration?.onlyRecurring > 0}"/>
	                        </g:applyLayout>
                        </div>
                        
                        <div class="row">
	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.use.customer.next.invoice.date"/></content>
	                            <content tag="label.for">invoiceDateProcess</content>
	                            <g:checkBox class="cb checkbox" name="invoiceDateProcess" checked="${configuration?.invoiceDateProcess > 0}"/>
	                        </g:applyLayout>
                        </div>
                        
                        <div class="row">
	                        <g:applyLayout name="form/input">
	                            <content tag="label"><g:message code="billing.maximum.period"/></content>
	                            <content tag="label.for">maximumPeriods</content>
	                            <content tag="style">inp4</content>
	                            <g:field type="number" class="field" name="maximumPeriods" value="${configuration?.maximumPeriods}" maxlength="2" size="2"/>
	                        </g:applyLayout>
                        </div>
                        
                        <div class="row">
	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.auto.payment.application"/></content>
	                            <content tag="label.for">autoPaymentApplication</content>
	                            <g:checkBox class="cb checkbox" name="autoPaymentApplication" id="autoPaymentApplication"
	                            	checked="${configuration?.autoPaymentApplication > 0}"/>
	                        </g:applyLayout>
                        </div>

                    <div class="row">
                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="billing.auto.creditNote"/></content>
                            <content tag="label.for">autoCreditNoteApplication</content>
                            <g:checkBox class="cb checkbox" name="autoCreditNoteApplication" checked="${configuration?.autoCreditNoteApplication  > 0}"/>
                        </g:applyLayout>
                    </div>

                    <div class="row">
                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="billing.auto.creditNote.application"/></content>
                            <content tag="label.for">applyCreditNotesBeforePayments</content>
                            <g:checkBox class="cb checkbox" name="applyCreditNotesBeforePayments"
                                        checked="${configuration?.applyCreditNotesBeforePayments > 0}"/>
                        </g:applyLayout>
                    </div>
                    <div class="row">
                        <g:render template="autoPayment" />
                     </div>
                     <div class="row">
                    	<g:render template="skipEmails" />
                    </div>
                    </div>
                      <div id="form-div" align="center">
                    	  <table style="width: 90%;  margin: 0px; vertical-align: to">
                    	     <tr>
                    	       <td id="form-div-td">
                    	     	<g:message code="billing.prorating.options"/>&nbsp;&nbsp;
                    	       </td>
                    	       <td style="width: 67%;">
	                    	  	 <g:applyLayout name="form/radio">
									  <div class="row" align="center">	
		                              		<g:radio class="rb" id="billing.proratingType.alwaysProrating" name="proratingType" value="${ProratingType.PRORATING_AUTO_ON}" checked="${configuration?.proratingType?.equals(ProratingType.PRORATING_AUTO_ON.getOptionText())}"/>
		                              		<label class="rb" for="billing.proratingType.alwaysProrating"><g:message code="label.proratingType.alwaysProrating"/></label>
									   </div>
									   <div class="row">	
		                              		<g:radio class="rb" id="billing.proratingType.neverProrating" name="proratingType" value="${ProratingType.PRORATING_AUTO_OFF}" checked="${configuration?.proratingType?.equals(ProratingType.PRORATING_AUTO_OFF.getOptionText())}"/>
		                              		<label class="rb" for="billing.proratingType.neverProrating"><g:message code="label.proratingType.neverProrating"/></label>
		                              </div>
		                              <div class="row">	
		                               		<g:radio class="rb" id="billing.proratingType.manuallyProrating" name="proratingType" value="${ProratingType.PRORATING_MANUAL}" checked="${configuration?.proratingType?.equals(ProratingType.PRORATING_MANUAL.getOptionText())}"/>
		                              		<label class="rb" for="billing.proratingType.manuallyProrating"><g:message code="label.proratingType.manuallyProrating"/></label>
		                              </div>
		                          </g:applyLayout>
	                           </td>
                    	     </tr>
	                   </table>
	                   <table style="width: 605px;">
					    	<tr>
					    		<td id="custom-td5">
									<p style="font-size: 12px;"><strong><g:message code="billing.label.note"/>: </strong><g:message code="billing.prorating.note"/></p>
								<td>
							</tr>
					    </table>
	                </div>
                
                
            </fieldset>


            <div class="btn-box buttons">
                <ul>
                    <li>
                        <a onclick="$('#save-billing-form').submit();"
                           class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                    </li>
                    <li>
                        <g:link controller="config" action="index" class="submit cancel"><span><g:message
                                code="button.cancel"/></span></g:link>
                    </li>
                    <g:if test="${isBillingRunning}">
                    </g:if>
                    <g:else>
                        <li>
                            <g:link controller="billingconfiguration" action="runBilling"
                                    class="submit play"><span><g:message code="button.run.billing"/></span></g:link>
                        </li>
                    </g:else>
                </ul>
            </div>
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
                    
                $(document).ready(function() {
                	hideLastDayOfMonthDiv();
               	}); 
               	
                $(function() {
                    $('#periodUnitId').change(function() {
                    	hideLastDayOfMonthDiv();
                    });
                });

                function hideLastDayOfMonthDiv() {
               		if ($('#periodUnitId').val() == 1) {
             	   		$('#lastDayOfMonthDiv').show(); 
                   } else {
                	    $('#lastDayOfMonth').attr('checked', false); 
                    	$('#lastDayOfMonthDiv').hide();
                 	}
                }
            </script>
        </g:form>
    </div>
</div>
