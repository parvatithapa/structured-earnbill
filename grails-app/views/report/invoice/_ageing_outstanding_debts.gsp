<%-- Parameters for the AAgeing Outstanding Debts report --%>
<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

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
