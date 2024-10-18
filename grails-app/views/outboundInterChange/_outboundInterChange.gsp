%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
--}%

<%--
  OutboundInterChange  list table.

  @author Satyendra Soni
  @since  16-May-2019
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="outBoundInterchange" cellspacing="0" cellpadding="0">
            <thead>
            <tr>
                <th class="small first header-sortable">
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="outBoundInterchange.th.id"/>
                    </g:remoteSort>
                </th>
                <th class="small first header-sortable">
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="outBoundInterchange.th.userId"/>
                    </g:remoteSort>
                </th>
                <th class="large header-sortable">
                    <g:remoteSort action="list" sort="methodName" update="column1">
                        <g:message code="outBoundInterchange.th.methodName"/>
                    </g:remoteSort>
                </th>
                <th class="medium header-sortable">
                    <g:remoteSort action="list" sort="createDateTime" update="column1">
                        <g:message code="outBoundInterchange.th.createDateTime"/>
                    </g:remoteSort>
                </th>
                <th class="medium header-sortable">
                    <g:remoteSort action="list" sort="status" update="column1">
                        <g:message code="outBoundInterchange.th.status"/>
                    </g:remoteSort>
                </th>
            </tr>
            </thead>
            <tbody>
            <g:each var="outbounds" in="${outboundInterChange}">
                <tr id="record-${outbounds.id}" class="${selected?.id == outbounds.id ? 'active' : ''}">
                <td>
                    <g:remoteLink class="cell" action="show" id="${outbounds.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span>${outbounds.id}</span>
                    </g:remoteLink>
                </td>
                <td>
                    <g:remoteLink class="cell" action="show" id="${outbounds.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span>${outbounds.userId}</span>
                    </g:remoteLink>
                </td>

                <td class="small">
                    <g:remoteLink class="cell" action="show" id="${outbounds.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span>${outbounds.methodName}</span>
                    </g:remoteLink>
                </td>

                <td class="medium">
                    <g:remoteLink class="cell" action="show" id="${outbounds.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span><g:formatDate date="${outbounds.createDateTime}" formatName="date.pretty.format"/></span>
                    </g:remoteLink>
                </td>
                <td class="small">
                    <g:remoteLink class="cell" action="show" id="${outbounds.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span>${outbounds.status}</span>
                    </g:remoteLink>
                </td>
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
    </div>

    <jB:isPaginationAvailable total="${outboundInterChange?.totalCount}">
        <div class="row-center">
            <jB:remotePaginate controller="outboundInterChange" action="list"
                               params="${sortableParams(params: [partial: true])}"
                               total="${outboundInterChange?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

