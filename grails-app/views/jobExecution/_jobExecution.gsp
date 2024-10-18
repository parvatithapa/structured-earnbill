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
  
<%@page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>
<%@page import="com.sapienter.jbilling.server.order.OrderStatusFlag" %>
<%@page import="com.sapienter.jbilling.client.util.Constants" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<!-- filter -->
<div class="form-columns single box-standalone">
    <div class="column">
    <g:formRemote name="job-exec-list-form" url="[action: 'list']" update="job-exec-list" method="GET">
        <g:hiddenField name="partial" value="true"/>
        <g:applyLayout name="form/select">
            <content tag="label"><g:message code="jobExecution.label.jobType"/></content>
            <content tag="label.for">jobType</content>
            <content tag="include.script">true</content>
            <g:select id="jobTypes" name="jobType" from="${jobTypes}"/>
        </g:applyLayout>

        <g:applyLayout name="form/date">
            <content tag="label"><g:message code="jobExecution.label.startFromDate"/></content>
            <content tag="label.for">startDate</content>
            <g:textField  class = "field"
                          name = "startDate"
                          value = "${formatDate(date: new Date()-1, formatName:'datepicker.format')}"
                          onblur = "validateDate(this);"/>
        </g:applyLayout>

        <g:applyLayout name="form/date">
            <content tag="label"><g:message code="jobExecution.label.startUntilDate"/></content>
            <content tag="label.for">startBeforeDate</content>
            <g:textField  class = "field"
                          name = "startBeforeDate"
                          onblur = "validateDate(this);"/>
        </g:applyLayout>

    </g:formRemote>

    </div>
</div>
<script type="text/javascript">

    $('#job-exec-list-form :input[name=startDate]').change(function() { $('#job-exec-list-form').submit(); });
    $('#job-exec-list-form :input[name=startBeforeDate]').change(function() { $('#job-exec-list-form').submit(); });

    $('#job-exec-list-form :input[name=jobType]').change(function() { $('#job-exec-list-form').submit(); });
</script>

<div id="job-exec-list">
    <g:render template="jobExecutionList" />
</div>
