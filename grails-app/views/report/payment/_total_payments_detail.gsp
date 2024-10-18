<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%--
  Parameters for the Total Payments Detail report.

  @author Leandro Bagur
  @since  04-Oct-2017
--%>

<div class="form-columns">
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start_date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="period"/></content>
        <content tag="label.for">period</content>
        <content tag="include.script">true</content>
        <g:select name="period" from="[1, 2, 3]" valueMessagePrefix="period"/>
    </g:applyLayout>
</div>