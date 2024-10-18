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

<%@ page import="com.sapienter.jbilling.server.item.db.ItemTypeDTO;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.PlanDTO"%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.Constants" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils;" %>

<%--
  Shows the plans list and provides some basic filtering capabilities.

  @author Brian Cowdery
  @since 02-Feb-2011
--%>

<div id="product-box">

    <!-- filter -->
    <div class="form-columns">
        <g:formRemote name="plans-filter-form" url="[action: 'edit']" update="ui-tabs-plans" method="GET">
            <g:hiddenField name="_eventId" value="plans"/>
            <g:hiddenField name="execution" value="${flowExecutionKey}"/>

            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="filters.title"/></content>
                <content tag="label.for">filterBy</content>
                <g:textField name="filterBy" class="field default" placeholder="${message(code: 'products.filter.by.default')}" value="${params.filterBy}"/>
            </g:applyLayout>
        </g:formRemote>

        <script type="text/javascript">
            $('#plans-filter-form :input[name=filterBy]').blur(function() { $('#plans-filter-form').submit(); });
            placeholder();
        </script>
    </div>

    <!-- product list -->
    <div class="table-box tab-table">
        <div class="table-scroll">
            <table id="plans" cellspacing="0" cellpadding="0">
                <tbody>

                <g:each var="plan" in="${plans}">

                    <g:set var="itemId" value="${g.hasAssetProduct(plan: plan)}"/>
                    <g:set var="hasAssetProduct" value="${itemId ? true : false}"/>
                    <g:set var="isSubsProd" value="${g.isSubsProd(plan: plan)}" />
                    <tr>
                        <td>
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId:(hasAssetProduct? 'initAssets' :'addPlan'), isPlan : true, isAssetMgmt : hasAssetProduct, itemId:itemId]" update="${(hasAssetProduct? 'assets-box-add' : 'ui-tabs-edit-changes')}" method="GET" >
                                <strong>${StringEscapeUtils.escapeHtml(plan?.getDescription(session['language_id']))}</strong> <g:if test="${plan?.plans?.toArray()[0]?.editable == 1}">&nbsp;(<g:message code="plan.editable.mark"/>)</g:if>
                                <em><g:message code="table.id.format" args="[plan.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}"
                                          params="[_eventId: (isSubsProd) ? 'subscription' : (hasAssetProduct? 'initAssets' :'addPlan'), isPlan : true, isAssetMgmt : hasAssetProduct, itemId:itemId]"
                                          update="${(isSubsProd) ? 'subscription-box-add' : (hasAssetProduct ? 'assets-box-add' : 'ui-tabs-edit-changes')}" method="GET" >
                                <span>${StringEscapeUtils.escapeHtml(plan?.internalNumber)}</span>
                            </g:remoteLink>
                        </td>
                        <g:set var="planId" value="${plan?.plans?.id}"/>
                        <g:set var="planPeriod" value="${PlanDTO.get(planId)}"/>
                        <td class="medium">
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId: 'addPlan']" update="column2" method="GET">
                                <span>${StringEscapeUtils.escapeHtml(planPeriod?.period?.getDescription(session['language_id']))}</span>
                            </g:remoteLink>
                        </td>
                        
                        <td class="medium">
                            <g:remoteLink class="cell double" action="edit" id="${plan.id}" params="[_eventId: (isSubsProd) ? 'subscription' : (hasAssetProduct? 'initAssets' :'addPlan'), isPlan : true, isAssetMgmt : hasAssetProduct, itemId:itemId]" update="${(isSubsProd) ? 'subscription-box-add' :(hasAssetProduct ? 'assets-box-add' : 'ui-tabs-edit-changes')}" method="GET" >
                                    <g:set var="price" value="${plan.getPrice(order.activeSince ?: order.createDate ?: TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
                                    <g:formatNumber number="${price?.rate}" type="currency" formatName="price.format" currencySymbol="${price?.currency?.symbol}"/>
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
                    <g:remoteLink     id = "page-size-plan-${max}"
                                  action = "edit"
                                  params = "${sortableParams(params: [ partial: true,
                                                                           max: max,
                                                                      _eventId: 'plans',
                                                                      filterBy: params.filterBy ])}"
                                  update = "ui-tabs-plans"
                                  method = "GET">
                        ${max}
                    </g:remoteLink>
                </g:each>
            </div>

            <div class="select-holder select-holder_small"><span class="select-value"></span>
                <g:select        name = "page-size-plans"
                                 from = "${steps}"
                                value = "${params.max?.toInteger()}"
                             onchange = "pageSizeChange(this);"
                          optionValue = "${{it + " " + message(code:"pager.show.max.results")}}" >
                </g:select>
            </div>

        </div>

        <div class="row-right">
            <jB:remotePaginate action = "edit"
                               params = "${sortableParams(params: [ partial: true,
                                                                   _eventId: 'plans',
                                                                        max: maxPlansShown,
                                                                   filterBy: params.filterBy ?: ""])}"
                                total = "${plans.totalCount ?: 0}"
                               update = "ui-tabs-plans"
                               method = "GET"/>
        </div>
    </div>
</div>

<script type="text/javascript">
    function pageSizeChange(obj) {
        $('#page-size-plan-'+obj.value).click();
    }

    $(document).ready(function() {
        $("select[name='page-size-plans']").each(function () {
            updateSelectLabel(this);
        });

        $("select[name='page-size-plans']").change(function () {
            updateSelectLabel(this);
        });
    });
</script>

<g:render template="assetDialogs"/>
<g:render template="subscriptionDialog"/>

