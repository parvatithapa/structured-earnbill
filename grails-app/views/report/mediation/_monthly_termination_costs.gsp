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
<%@ page import="com.sapienter.jbilling.server.mediation.movius.CDRType" %>
<%@ page import="com.sapienter.jbilling.server.report.BackgroundReportExportUtil" %>

<div class="form-columns">
<g:message code="mediation_report_date_parameter_description"/>
<br/>&nbsp;
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="event_date_start"/></content>
        <content tag="label.for">event_date_start</content>
        <g:textField class="field" name="event_date_start" value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="event_date_end"/></content>
        <content tag="label.for">event_date_end</content>
        <g:textField class="field" name="event_date_end" value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

	<g:applyLayout name="form/input">
        <content tag="label"><g:message code="report.mediation.userId.label"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">user_id</content>
        <g:textField name="user_id" class="field"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label">
        <g:message code="label.export.format"/></content><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">format</content>
        <content tag="include.script">true</content>
        <g:select name="format"  id = "export_format" from = "${BackgroundReportExportUtil.Format.values()}" onchange="toggleCdrType()"/>
    </g:applyLayout>

    <div id="cdrtype_div">
        <g:applyLayout name="form/select">
            <content tag="label">
            <g:message code="label.cdr.type"/></content><span id="mandatory-meta-field">*</span></content>
            <content tag="label.for">cdrType</content>
            <content tag="include.script">true</content>
            <g:select name               = "cdrType"
                      from               = "${CDRType.values()}"
                      valueMessagePrefix = "cdr.type"/>
        </g:applyLayout>
    </div>

</div>
