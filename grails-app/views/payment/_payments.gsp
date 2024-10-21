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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO"%>
<%--
  Payment list table.

  @author Brian Cowdery
  @since  04-Jan-2011
--%>
<div id="success-message" class="msg-box successfully" style="display: none;">
    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="${message(code:'info.icon.alt',default:'Information')}"/>
     <strong><g:message code="flash.request.sent.title"/></strong>
     <p><g:message code="records.csv.file.generate"/></p>
 </div>
<div class="table-box">
    <div class="table-scroll">
        <table id="payments" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th class="small first header-sortable">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="payment.th.id"/>
                        </g:remoteSort>
                    </th>
                    <th class="large header-sortable">
                        <g:remoteSort action="list" sort="u.userName" update="column1">
                            <g:message code="invoice.label.customer"/>
                        </g:remoteSort>
                    </th>
                    <g:isRoot>
                		<th class="tiny3 header-sortable">
                			<g:remoteSort action="list" sort="company.description" alias="[company: 'baseUser.company']" update="column1">
                    	    	<g:message code="invoice.label.company.name"/>
                    		</g:remoteSort>
                		</th>
                	</g:isRoot>
                    <th class="medium header-sortable">
                        <g:remoteSort action="list" sort="paymentDate" update="column1">
                            <g:message code="payment.th.date"/>
                        </g:remoteSort>
                    </th>
                    <th class="tiny header-sortable">
                        <g:remoteSort action="list" sort="isRefund" update="column1">
                            <g:message code="payment.th.payment.or.refund"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="amount" update="column1">
                            <g:message code="payment.th.amount"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="paymentMethod.id" update="column1">
                            <g:message code="payment.th.method"/>
                        </g:remoteSort>
                    </th>
                    <th class="small last header-sortable">
                        <g:remoteSort action="list" sort="paymentResult.id" update="column1">
                            <g:message code="payment.th.result"/>
                        </g:remoteSort>
                    </th>
                </tr>
            </thead>

            <tbody>
            <g:each var="payment" in="${payments}">
                
                <g:set var="contact" value="${ContactDTO.findByUserId(payment?.baseUser?.id)}"/>
                
                <tr id="payment-${payment.id}" class="${selected?.id == payment.id ? 'active' : ''}">

                    <td>
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <span>${payment.id}</span>
                        </jB:secRemoteLink>
                    </td>
                    <td>
                        <jB:secRemoteLink permissions="PAYMENT_34" breadcrumb="id" class="cell double" action="show" id="${payment.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <strong>
                                <g:if test="${contact?.firstName || contact?.lastName}">
                                    ${StringEscapeUtils.escapeHtml(contact?.firstName)} &nbsp;${StringEscapeUtils.escapeHtml(contact?.lastName)}
                                </g:if>
                                <g:else>
                                    ${StringEscapeUtils.escapeHtml(displayer?.getDisplayName(payment?.baseUser))}
                                </g:else>
                            </strong>
                            <em>${StringEscapeUtils.escapeHtml(contact?.organizationName)}</em>
                        </jB:secRemoteLink>
                    </td>
                    <g:isRoot>
                		<td>
                    		<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${payment.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                        		<strong>${StringEscapeUtils.escapeHtml(payment?.baseUser?.company?.description)}</strong>
                   			</jB:secRemoteLink>
                		</td>
                	</g:isRoot>
                    <td class="medium">
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatDate date="${payment.paymentDate}" formatName="date.pretty.format"/></span>
                        </jB:secRemoteLink>
                    </td>
                    <td class="tiny">
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:if test="${payment.isRefund > 0}">
                                <span>R</span>
                            </g:if>
                            <g:else>
                                <span>P</span>
                            </g:else>
                        </jB:secRemoteLink>
                    </td>
                    <td class="small">
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <span><g:formatNumber number="${payment.amount}" type="currency" currencySymbol="${payment?.getCurrency()?.getSymbol()}"/></span>
                        </jB:secRemoteLink>
                    </td>
                    <td class="small">
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <span>${StringEscapeUtils.escapeHtml(payment?.paymentMethod?.getDescription(session['language_id']))}</span>
                        </jB:secRemoteLink>
                    </td>
                    <td class="small">
                        <jB:secRemoteLink class="cell" action="show" id="${payment.id}" before="register(this);" onSuccess="render(data, next);">
                            <span>${StringEscapeUtils.escapeHtml(payment?.paymentResult?.getDescription(session['language_id']))}</span>
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
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1', contactFieldTypes: contactFieldTypes]"/>
        </div>
        
         <div id="download-div" class="download">
			<g:if test="${csvExportFlag}">
				<div class="pager-button" id="generateCsv">
					<a onclick="generateCSV(); showMessage()"><g:message code="generate.csv.link" /></a>
                </div>
			</g:if>
            <g:else>
            <sec:access url="/payment/csv">
                <g:link action="csv" id="${selected?.id}" class="pager-button" params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </g:else>
		</div>
    </div>

    <jB:isPaginationAvailable total="${payments?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="payment" action="list" params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}" total="${payments?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<div class="btn-box">
    <sec:access url="/payment/create">
        <g:link action="create" class="submit payment button-secondary"><span><g:message code="button.create.payment"/></span></g:link>
    </sec:access>
</div>
<script type="text/javascript">
function showMessage() {
       $("#success-message").css("display","block");
}

function generateCSV() {
    $.ajax({
        type: 'POST',
        url: '${createLink(action: 'csv', params:[processId:params?.processId])}',
        data: $('#generateCsv').parents('form').serialize(),
        error: function(data) {}
    });
}
</script>