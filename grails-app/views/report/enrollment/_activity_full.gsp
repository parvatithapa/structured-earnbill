<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%@ page import="com.sapienter.jbilling.server.report.util.EnrollmentScope" %>
<%--
  Parameters for the Activity Full report.

  @author Leandro Bagur
  @since  07-Jun-2017
--%>

<script type="text/javascript">
    function setEndMinDate() {
        var startDate = $("#start_date").datepicker("getDate");
        $("#end_date").datepicker("option", "minDate", startDate);
    }
</script>

<div class="form-columns">
    
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start_date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date"
                     maxDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now(), session['company_timezone']))}"
                     value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now(), session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
        <content tag="onClose">
            function(e) {
            setEndMinDate()
            }
        </content>
    </g:applyLayout>
    
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date"
                     value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now(), session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.activity.type"/></content>
        <content tag="label.for">type</content>
        <content tag="include.script">true</content>
        <g:select name="type" from="${EnrollmentScope.values()}"/>
    </g:applyLayout>
</div>
