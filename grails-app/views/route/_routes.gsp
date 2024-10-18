<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div class="table-box">
    <table id="routes" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th>
                <g:remoteSort controller="route" action="list"
                              sort="id" update="column1">
                    <g:message code="route.th.id"/>
                </g:remoteSort>
            </th>
            <th>
                <g:remoteSort controller="route" action="list"
                              sort="name" update="column1">
                    <g:message code="route.th.name"/>
                </g:remoteSort>
            </th>
            <th class="small">
                <g:remoteSort controller="route" action="list"
                              sort="tableName" update="column1">
                    <g:message code="route.table.th.name"/>
                </g:remoteSort>
            </th>
        </tr>
        </thead>

        <tbody>
        <g:each var="route" in="${routes}">

            <tr id="route-${route.id}" class="${selected?.id == route.id ? 'active' : ''}">
                <td>
                    <g:remoteLink class="cell" action="show" id="${route.id}"
                                  before="register(this);" onSuccess="render(data, next);">
                        <span>${route.id}</span>
                    </g:remoteLink>
                </td>
                <td>
                    <g:remoteLink class="cell" action="show" id="${route.id}"
                                  before="register(this);" onSuccess="render(data, next);">
                        <strong>${StringEscapeUtils.escapeHtml(route?.name)}</strong>
                    </g:remoteLink>
                </td>
                <td class="center">
                    <g:remoteLink class="cell" action="show" id="${route.id}"
                                  before="register(this);" onSuccess="render(data, next);">
                        <strong>${StringEscapeUtils.escapeHtml(route?.tableName)}</strong>
                    </g:remoteLink>
                </td>
            </tr>

        </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']" />
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate controller="route"
                             action="list"
                             params="${sortableParams(params: [partial: true, max: params.max])}"
                             total="${routes?.totalCount ?: 0}"
                             update="column1"
                             method="GET"/>
    </div>
</div>

<div class="btn-box">
        <g:remoteLink controller="route" action="add" update="column2" class="submit add button-primary">
            <span><g:message code="route.add.button"/></span>
        </g:remoteLink>
</div>
