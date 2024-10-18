<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
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

<div id="plan-box">

    <!-- filter -->
    <div class="form-columns">
        <g:formRemote name="plans-filter-form" url="[action: 'edit']" update="ui-tabs-plans" method="GET">
            <g:hiddenField name="_eventId" value="plans" id="_plansEventId"/>
            <g:hiddenField name="execution" value="${flowExecutionKey}" id="plansExecution"/>

            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="filters.title"/></content>
                <content tag="label.for">filterBy</content>
                <g:textField id="plansFilterBy" name="filterBy" class="field default" placeholder="${message(code: 'products.filter.by.default')}" value="${params.filterBy}"/>
            </g:applyLayout>
        </g:formRemote>

        <script type="text/javascript">
            $('#plansFilterBy').blur(function() { $('#plans-filter-form').submit(); });
            placeholder();
        </script>
    </div>

    <!-- plan list -->
    <div class="table-box tab-table">
        <div class="table-scroll">
            <table id="plans" cellspacing="0" cellpadding="0">
                <tbody>

                <g:each var="plan" in="${plans}">
                    <tr>
                        <td>
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId: 'addPlanPrice']" update="ui-tabs-review" method="GET">
                                <strong>${StringEscapeUtils.escapeHtml(plan?.getDescription(session['language_id']))}</strong>
                                <em><g:message code="table.id.format" args="[plan.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <td class="small">
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId: 'addPlanPrice']" update="ui-tabs-review" method="GET">
                                <span>${StringEscapeUtils.escapeHtml(plan?.internalNumber)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId: 'addPlanPrice']" update="ui-tabs-review" method="GET">
                                    <g:set var="price" value="${plan.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']))}"/>
                                    <g:formatPriceForDisplay price="${price}" />	
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
    </div>

    <div class="pager-box">
        <div class="results">
            <div style="display: none; visibility: hidden">
                <g:set var="steps" value="${[10,20,50]}" />
                <g:each var="max" in="${steps}">
                    <g:set var="extraParams" value="${extraParams?extraParams:[:]}"/>
                    <g:remoteLink id="page-size-plans-${max}"
                                  action="edit"
                                  params="${sortableParams(params: [partial: true, max: max, _eventId: 'plans', typeId: params.typeId ?: "", filterBy: params.filterBy ])}"
                                  update="ui-tabs-plans"
                                  method="GET">${max}</g:remoteLink>
                </g:each>
            </div>
            ${params.max}
            <g:select name="page-size" from="${steps}" value="${params.max}" onchange="pageSizeChangePlan(this);" optionValue="${{it + " " + message(code:"pager.show.max.results")}}">
            </g:select>

        </div>
        <div class="row">
            <jB:remotePaginate action="edit"
                                 params="${sortableParams(params: [partial: true, _eventId: 'plans', max: maxProductsShown, typeId: params.typeId ?: "", filterBy: params.filterBy ?: ""])}"
                                 total="${plans.totalCount ?: 0}"
                                 update="ui-tabs-plans"
                                 method="GET"/>
        </div>
    </div>

</div>

<script type="text/javascript">
    function pageSizeChangePlan(obj) {
        $('#page-size-plans-'+obj.value).click();
    }

</script>
