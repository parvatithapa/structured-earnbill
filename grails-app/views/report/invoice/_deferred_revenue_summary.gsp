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

<%@ page import="com.sapienter.jbilling.server.user.MainSubscriptionWS; java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.report.builder.AbstractReportBuilderRevenue" %>

<%--
  Parameters for the Deferred Revenue Summary report.

  @author Leandro Bagur
  @since  07-Jun-2017
--%>

<div class="form-columns">
    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.deferred.revenue.summary.year"/></content>
        <content tag="label.for">year</content>
        <content tag="include.script">true</content>
        <g:select from="${AbstractReportBuilderRevenue.getLastYearsFromCurrent(10)}"
                  name="year"
                  value="${Calendar.getInstance().get(Calendar.YEAR)}"/>
    </g:applyLayout>
    
    <div class="row">
        <label class="" title="" for="month"><g:message code="report.deferred.revenue.summary.month"/></label>
        <div class="select-holder"><span class="select-value"></span>
            <g:select id="month" from="${MainSubscriptionWS.yearMonthsMap.entrySet()}"
                      optionKey="key" optionValue="value"
                      name="month"/>
        </div>
    </div>
</div>

<script type="text/javascript">
    $(document).ready(function() {
        var select = $("select[name='month']");
        select.each(function () {
            updateSelectLabel(this);
        });

        select.change(function () {
            updateSelectLabel(this);
        });
    });
</script>