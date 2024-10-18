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

<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>

<%--
  Payment transfer list table.

  @author Ashok Kale
  @since  11-Feb-2014
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="paymentTransfer" cellspacing="0" cellpadding="0">
            <thead>
            <tr>
                <th class="small">
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="payment.transfer.th.id"/>
                    </g:remoteSort>
                </th>
                <th class="small">
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="payment.transfer.th.payment.id"/>
                    </g:remoteSort>
                </th>
                <th class="medium">
                    <g:remoteSort action="list" sort="createDateTime" update="column1">
                        <g:message code="payment.transfer.th.date"/>
                    </g:remoteSort>
                </th>
                <th class="small">
                    <g:remoteSort action="list" sort="amount" update="column1">
                        <g:message code="payment.transfer.th.amount"/>
                    </g:remoteSort>
                </th>
                <th class="small">
                    <g:remoteSort action="list" sort="fromUserId" update="column1">
                        <g:message code="payment.transfer.th.from.userId"/>
                    </g:remoteSort>
                </th>
            </tr>
            </thead>

            <tbody>
            <g:each var="paymentTransfer" in="${paymentTransfers}">
                <g:set var="contact" value="${ContactDTO.findByUserId(paymentTransfer?.payment?.baseUser?.id)}"/>

                <tr id="payment-${paymentTransfer.id}" class="${selected?.id == paymentTransfer.id ? 'active' : ''}">

                    <td>
                        <g:remoteLink class="cell" action="show" id="${paymentTransfer.id}" before="register(this);" onSuccess="render(data, next);">
                            <span>${paymentTransfer.id}</span>
                        </g:remoteLink>
                    </td>
                    <td>
                        <span>${paymentTransfer?.payment?.id}</span>
                    </td>
                    <td class="medium">
                        <g:remoteLink class="cell" action="show" id="${paymentTransfer.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatDate date="${paymentTransfer.createDatetime}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/></span>
                        </g:remoteLink>
                    </td>
                    <td class="small">
                        <g:remoteLink class="cell" action="show" id="${paymentTransfer.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatNumber number="${paymentTransfer?.amount}" type="currency" currencySymbol="${paymentTransfer?.payment?.getCurrency()?.getSymbol()}"/></span>
                        </g:remoteLink>
                    </td>
                    <td>
                        ${paymentTransfer?.fromUserId}
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
            <sec:access url="/paymentTransfer/csv">
                <g:link action="csv" id="${selected?.id}" params="${sortableParams(params: [partial: true])}">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate controller="paymentTransfer" action="list" params="${sortableParams(params: [partial: true])}" total="${paymentTransfer?.totalCount ?: 0}" update="column1"/>
    </div>
</div>
