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

<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO" %>
<%@ page import="com.sapienter.jbilling.server.process.db.BillingProcessDTO"%>
<%@ page import="com.sapienter.jbilling.server.invoice.db.InvoiceDAS"%>
<%@ page import="com.sapienter.jbilling.server.process.BillingProcessBL"%>
	
<div class="table-box">
	<div class="table-scroll">
    	<table id="processes" cellspacing="0" cellpadding="0">
			<thead>
				<tr>
					<th class="small first header-sortable">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="label.billing.cycle.id"/>
                        </g:remoteSort>
                    </th>
					<th class="medium header-sortable">
                        <g:remoteSort action="list" sort="billingDate" update="column1">
                            <g:message code="label.billing.cycle.date"/>
                        </g:remoteSort>
                    </th>
                    <th class="small">
                        <g:message code="label.billing.order.count"/>
                    </th>
					<th class="small">
                        <g:message code="label.billing.invoice.count"/>
                    </th>
					<th class="medium">
                        <g:message code="label.billing.total.invoiced"/>
                    </th>
                    <th class="medium last">
                        <g:message code="label.billing.total.carried"/>
                    </th>
				</tr>
			</thead>
	
			<tbody>
                <g:each var="process" in="${processes}">
                    <tr id="process-${process.id}" class="${selected?.id == process.id ? 'active' : ''} ${process?.isReview > 0 ? 'isReview' : ''}">
                        <td class="small">
                            <g:link class="cell" action="show" id="${process.id}">
                                ${process.id}
                            </g:link>
                        </td>
                        <td class="medium">
                            <g:link class="cell" action="show" id="${process.id}">
                                <g:formatDate date="${process.billingDate}" formatName="date.pretty.format"/>
                            </g:link>
                        </td>
                        <td class="small">
                            <g:link class="cell" action="show" id="${process.id}">
                                ${new BillingProcessBL().getOrderProcessCount(process.id as int)}
                            </g:link>
                        </td>
                        <td class="small">
                            <%
								def invoices = 0
								def totalInvoiced
								def totalCarried
								def currencySymbol
								def invoicesDataMap = new InvoiceDAS().findInvoiceDetailsByProcessId(process.id)
								invoicesDataMap.each {currency ->
									currency.value.each {invoice ->
										if (invoice.key == 'invoices') {
											invoices = invoices + invoice.value
										}
										if (invoicesDataMap.keySet().size() == 1) {
											if (invoice.key == 'totalInvoiced') {
												totalInvoiced = invoice.value
											}
											else if (invoice.key == 'totalCarried') {
												totalCarried = invoice.value
											}
											else if (invoice.key == 'symbol') {
												currencySymbol = invoice.value
											}
										}
									}
								}
                            %>
                            <g:link class="cell" action="show" id="${process.id}">
                                ${invoices}
                            </g:link>
                        </td>
                        <td class="medium">
                            <g:link class="cell" action="show" id="${process.id}">
                                <g:if test="${invoicesDataMap.keySet().size() == 1}">
		                             <g:formatNumber number="${totalInvoiced}" type="currency" currencySymbol="${currencySymbol}"/> <br/>
                                </g:if>
                                <g:if test="${invoicesDataMap.keySet().size() > 1}">
                                    <em><g:message code="label.billing.multi.currency"/></em>
                                </g:if>
                            </g:link>
                        </td>
                        <td class="medium">
                            <g:link class="cell" action="show" id="${process.id}">
                                <g:if test="${invoicesDataMap.keySet().size() == 1}">
                                    <g:formatNumber number="${totalCarried}" type="currency" currencySymbol="${currencySymbol}"/> <br/>
                                </g:if>
                            </g:link>
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
    </div>

    <div class="row-center">
        <jB:remotePaginate controller="billing" action="index" params="[applyFilter: true]" total="${processes?.totalCount}" update="column1"/>
    </div>
</div>

