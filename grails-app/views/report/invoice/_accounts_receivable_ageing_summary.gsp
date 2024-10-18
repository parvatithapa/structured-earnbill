<%@ page import="com.sapienter.jbilling.server.util.EnumerationBL" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%@ page import="java.time.LocalDateTime;" %>
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

<%-- Parameters for the Accounts Receivable Ageing Summary report --%>

<script type="text/javascript">
    function validateAsOfValue() {
        var asOfEl = $("#as_of"),
                asOf = asOfEl.datepicker('getDate'),
                todayDate = new Date();

        if (asOf > todayDate) {
            asOfEl.datepicker('option', 'maxDate', todayDate);
        }
    }
</script>

<div class="form-columns">
    <g:applyLayout name="form/select_multiple">
        <content tag="label"><g:message code="report.ageing.balance.division.label"/></content>
        <content tag="label.for">divisions</content>
        <g:select name="divisions" multiple="true" optionKey="value" optionValue="value" from="${new EnumerationBL().getEnumerationByName('DIVISION', session['company_id'] as Integer)?.values}"/>
    </g:applyLayout>

    <g:applyLayout name="form/select_multiple">
        <content tag="label"><g:message code="report.ageing.balance.customerStatuses.label"/></content>
        <content tag="label.for">customer_statuses</content>
        <g:select name="customer_statuses" multiple="true" optionKey="value" optionValue="value" from="${new EnumerationBL().getEnumerationByName('Termination', session['company_id'] as Integer)?.values}" noSelection="['Active':'Active']"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="as_of"/></content>
        <content tag="label.for">as_of</content>
        <g:textField class="field"
                     name="as_of"
                     maxDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now(), session['company_timezone']))}"
                     value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
        <content tag="onClose">
            function(e) {
                validateAsOfValue()
            }
        </content>
    </g:applyLayout>
</div>
