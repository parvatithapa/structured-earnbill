%{--
  JBILLING CONFIDENTIAL
s  _____________________

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

<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%--
Parameters for the Cancellation Requests report.

  @author Pranay Raherkar
  @since  22-June-2018
--%>

<div class="form-columns">
<g:message code="scheduled_process_run_history"/>
<br/>&nbsp;
 <g:applyLayout name="form/date">
        <content tag="label"><g:message code="scheduled_process_run_date_start"/></content>
        <content tag="label.for">scheduled_process_run_date_start</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="scheduled_process_run_date_end"/></content>
        <content tag="label.for">scheduled_process_run_date_end</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>
</div>