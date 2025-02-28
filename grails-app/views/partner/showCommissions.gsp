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

<html>
<head>
	<meta name="layout" content="main"/>
</head>

<body>

    <div class="table-info">
        <em><strong><g:message code="label.partner.commissionRun.id"/> ${commissionRun.id}</strong></em>
        <em><strong><g:message code="label.partner.commissionRun.periodStart"/> <g:formatDate date="${commissionRun.periodStart}" formatName="date.pretty.format"/></strong></em>
        <em><strong><g:message code="label.partner.commissionRun.periodEnd"/> <g:formatDate date="${commissionRun.periodEnd}" formatName="date.pretty.format"/></strong></em>
    </div>

    <div class="table-area">
        <table class="innerTable">
            <thead class="innerHeader">
                <tr>
                    <td class="first"><g:message code="label.commission.partner"/></td>
                    <td><g:message code="label.commission.amount"/></td>
                    <td class="last"><g:message code="label.commission.Type"/></td>
                </tr>
            </thead>
            <tbody class="innerContent">
                <g:each var="commission" status="i" in="${commissions}">
                    <tr>
                        <td class="col02">
                            <g:link class="cell" action="showCommissionDetail" params="[commissionId:commission.id]">
                                ${commission.partner.baseUser.userName}
                            </g:link>
                        </td>
                        <td>
                            <g:formatNumber number="${commission.amount}" type="currency" currencySymbol="${commission.currency.symbol}"/>
                        </td>
                        <td>
                            <g:message code="CommissionType.${commission.type}"/>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>

    <div class="pager-box">
        <div class="row">
            <g:link action="commissionCsv" id="${commissionRun.id}"  class="pager-button">
                <span><g:message code="download.csv.link"/></span>
            </g:link>
            <a href="${createLink (action: 'downloadPdf', id: commissionRun.id)}" class="pager-button">
                <span><g:message code="button.invoice.downloadPdf"/></span>
            </a>
        </div>
        <br/>
        <jB:isPaginationAvailable total="${users?.totalCount ?: 0}">
            <div class="row-center">
                <g:paginate controller="partner" action="showCommissions" total="${users?.totalCount ?: 0}" params="${sortableParams(params: [partial: true, id: commissionRun.id])}"/>
            </div>
        </jB:isPaginationAvailable>
   </div>
</body>