<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Discount By Account Report

  @author Dipak Kardel
  @since  1-Feb-2019
--%>

<script type="text/javascript">
    function validateAsOfValue() {
        var asOfEl = $("#invoice_date"),
                asOf = asOfEl.datepicker('getDate'),
                todayDate = new Date();

        if (asOf > todayDate) {
            asOfEl.datepicker('option', 'maxDate', todayDate);
        }
    }
</script>

<div class="form-columns">

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="invoice_date"/></content>
        <content tag="label.for">invoice_date</content>
        <g:textField class="field"
                     name="invoice_date"
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
