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
<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Parameters for the GL Detail report.

  @author Brian Cowdery
  @since  30-Mar-2011
--%>

<script type="text/javascript">
    function validateEndDate() {
        var endDateEl = $("#end_date"),
                endDate = endDateEl.datepicker('getDate'),
                startDate = $("#start_date").datepicker('getDate');
        // Set minDate for endDate element
        endDateEl.datepicker('option', 'minDate', startDate);
        // Validate if the start date is greater than the end date.
        if (startDate > endDate) {
            endDateEl.datepicker('setDate', startDate);
        }
    }
</script>

<div class="form-columns">
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start_date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field"
                     name="start_date"
                     value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
        <content tag="onClose">
            function(e) {
            validateEndDate()
            }
        </content>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field"
                     name="end_date"
                     minDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']))}"
                     value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
    </g:applyLayout>
</div>