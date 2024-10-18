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

<%@ page import="com.sapienter.jbilling.server.user.MainSubscriptionWS" %>
<%@ page import="com.sapienter.jbilling.server.report.builder.AbstractReportBuilderRevenue" %>

<%--
  Parameters for the Deferred Revenue Detailed report.

  @author Leandro Bagur
  @since  07-Jun-2017
--%>

<div class="form-columns">
    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.deferred.revenue.detailed.year"/></content>
        <content tag="label.for">year</content>
        <content tag="include.script">true</content>
        <g:select from="${AbstractReportBuilderRevenue.getLastYearsFromCurrent(10)}"
                  name="year"
                  value="${Calendar.getInstance().get(Calendar.YEAR)}"/>
    </g:applyLayout>

    <div class="row">
        <label for="month"><g:message code="report.deferred.revenue.detailed.month"/></label>
        <div class="select-holder"><span class="select-value"></span>
            <g:select id="month" from="${MainSubscriptionWS.yearMonthsMap.entrySet()}"
                      optionKey="key" optionValue="value"
                      name="month"/>
        </div>
    </div>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.deferred.revenue.detailed.invoice.entered"/></content>
        <content tag="label.for">invoice_entered</content>
        <content tag="include.script">true</content>
        <g:select from="${["Current", "Previous"]}"
                  name="invoice_entered"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.deferred.revenue.detailed.revenue.type"/></content>
        <content tag="label.for">revenue_type</content>
        <content tag="include.script">true</content>
        <g:select from="${["Earned", "Deferred"]}"
                  name="revenue_type"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.deferred.revenue.detailed.group"/></content>
        <content tag="label.for">group</content>
        <content tag="include.script">true</content>
        <g:select from="${AbstractReportBuilderRevenue.getReportGroupCategoryNamesByEntity((int) session['company_id'])}"
                  name="group"/>
    </g:applyLayout>

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