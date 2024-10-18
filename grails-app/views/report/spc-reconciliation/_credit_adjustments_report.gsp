<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Credit Adjustments Report

  @author Dipak Kardel
  @since  1-Feb-2019
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

