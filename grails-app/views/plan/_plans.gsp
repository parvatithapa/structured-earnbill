<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>

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

<%--
  Plans list table.

  @author Brian Cowdery
  @since  01-Feb-2011
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="plans" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th>
                        <g:message code="plan.th.name"/>
                    </th>
                    <g:isRoot>
                    <th class="medium header-sortable">
                            <g:message code="product.label.available.company.name"/>
                    </th>
                    </g:isRoot>
                    <th class="medium header-sortable">
                        <g:remoteSort action="list" sort="i.internalNumber" update="column1">
                            <g:message code="plan.th.item.number"/>
                        </g:remoteSort>
                    </th>
                    <th class="small">
                        <g:message code="plan.th.products"/>
                    </th>
                </tr>
            </thead>

            <tbody>
                <g:each var="plan" in="${plans}">
                    <tr id="plan-${plan.id}" class="${selected?.id == plan.id ? 'active' : ''}">

                        <td>
                            <jB:secRemoteLink class="cell double" action="show" id="${plan.id}" before="register(this);" onSuccess="render(data, next);">
                                <strong>${StringEscapeUtils.escapeHtml(plan?.item?.getDescription(session['language_id']))}</strong>
                                <em><g:message code="table.id.format" args="[plan.id as String]"/></em>
                            </jB:secRemoteLink>
                        </td>
                        <g:isRoot>
                            <td class="medium">
                                <%
                                def totalChilds = plan?.item?.entities?.size()
                                def multiple = false
                                if(totalChilds > 1) {
                                    multiple = true
                                }
                                %>
                                <jB:secRemoteLink class="cell" action="show" id="${plan.id}" before="register(this);" onSuccess="render(data, next);">
                                    <g:if test="${plan?.item?.global}">
                                        <strong><g:message code="product.label.company.global"/></strong>
                                    </g:if>
                                    <g:elseif test="${multiple}">
                                        <strong><g:message code="product.label.company.multiple"/></strong>
                                    </g:elseif>
                                    <g:elseif test="${totalChilds==1}">
                                        <strong>${StringEscapeUtils.escapeHtml(plan?.item?.entities?.toArray()[0]?.description)}</strong>
                                    </g:elseif>
                                    <g:else>
                                        <strong>-</strong>
                                    </g:else>
                                </jB:secRemoteLink>
                            </td>
                        </g:isRoot>
                        <td>
                            <jB:secRemoteLink class="cell" action="show" id="${plan.id}" before="register(this);" onSuccess="render(data, next);">
                                <strong>${StringEscapeUtils.escapeHtml(plan?.item?.internalNumber)}</strong>
                            </jB:secRemoteLink>
                        </td>
                        <td>
                            <jB:secRemoteLink class="cell" action="show" id="${plan.id}" before="register(this);" onSuccess="render(data, next);">
                                <span>${plan.planItems?.size()}</span>
                            </jB:secRemoteLink>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
        <div class="download">
            <sec:access url="/plan/csv">
                <g:link class="pager-button" action="csv" id="${selected?.id}" params="${[params] + ['ids': ids]}">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </div>
    </div>
    <jB:isPaginationAvailable total="${plans?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller = "plan"
                                   action = "list"
                                   params = "${sortableParams(params: [partial: true])}"
                                    total = "${plans.totalCount}"
                                   update = "column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<div class="btn-box">
    <sec:ifAllGranted roles="PLAN_60">
        <g:link controller="planBuilder" action="edit" class="submit add button-primary">
            <span><g:message code="button.create"/></span>
        </g:link>
    </sec:ifAllGranted>
</div>
<script type="text/javascript">
    function generateCSV() {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'csv')}',
            data: $(this).parents('form').serialize(),
            error: function(data) {}
        });
    }
</script>