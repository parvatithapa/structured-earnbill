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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>

<%--
  Credit note list table.

  @author Usman Malik
  @since  22-July-2015
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="creditNotes" cellspacing="0" cellpadding="0">
            %{--id username date amount balance--}%
            <thead>
                <tr>
                    <th class="small first header-sortable">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="creditNote.th.id"/>
                        </g:remoteSort>
                    </th>
                    <th class="large header-sortable">
                        <g:remoteSort action="list" sort="u.userName" update="column1">
                            <g:message code="invoice.label.customer"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium header-sortable">
                        <g:remoteSort action="list" sort="createDateTime" update="column1">
                            <g:message code="payment.th.date"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="amount" update="column1">
                            <g:message code="creditNote.th.amount"/>
                        </g:remoteSort>
                    </th>
                    <th class="large last header-sortable">
                        <g:remoteSort action="list" sort="balance" update="column1">
                            <g:message code="creditNote.th.balance"/>
                        </g:remoteSort>
                    </th>
                </tr>
            </thead>

            <tbody>
            <g:each var="creditNote" in="${creditNotes}">
                <g:set var="contact" value="${creditNote?.user?.contact}" />

                <tr id="creditNote-${creditNote.id}" class="${selected?.id == creditNote.id ? 'active' : ''}">

                    <td>
                        <g:remoteLink class="cell" action="show" id="${creditNote.id}" before="register(this);" onSuccess="render(data, next);">
                            <span>${creditNote.id}</span>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink breadcrumb="id" class="cell double" action="show" id="${creditNote.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <strong>
                            <g:if test="${contact?.firstName || contact?.lastName}">
                                ${contact.firstName} &nbsp;${contact.lastName}
                            </g:if>
                            <g:else>
                            ${StringEscapeUtils.escapeHtml(displayer?.getDisplayName(creditNote?.user))}
                            </g:else>
                            </strong>
                            <em>${contact?.organizationName}</em>
                        </g:remoteLink>
                    </td>

                    <td class="medium">
                        <g:remoteLink class="cell" action="show" id="${creditNote.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatDate date="${creditNote.creditNoteDate}" formatName="date.pretty.format"/></span>
                        </g:remoteLink>
                    </td>

                    <td class="small">
                        <g:remoteLink class="cell" action="show" id="${creditNote.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatNumber number="${creditNote.amount}" type="currency" currencySymbol="${creditNote.currencySymbol}"/></span>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" id="${creditNote.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatNumber number="${creditNote.balance}" type="currency" currencySymbol="${creditNote.currencySymbol}"/></span>
                        </g:remoteLink>
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
            <sec:access url="/creditNote/csv">
                <g:link action="csv" id="${selected?.id}" class="pager-button" params="${sortableParams(params: [partial: true])}">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </div>
    </div>

    <jB:isPaginationAvailable total="${creditNotes?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="creditNote" action="list" params="${sortableParams(params: [partial: true])}" total="${creditNotes?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>
