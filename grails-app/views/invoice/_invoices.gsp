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

<%@ page import="com.sapienter.jbilling.server.item.db.ItemDAS; com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.server.invoice.db.InvoiceDTO; org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO; com.sapienter.jbilling.server.user.db.CompanyDAS" %>
<%--
	Invoice list template. 
	
	@author Vikas Bodani
	@since 24-Dec-2010
 --%>
 <div id="success-message" class="msg-box successfully" style="display: none;">
    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="${message(code:'info.icon.alt',default:'Information')}"/>
     <strong><g:message code="flash.request.sent.title"/></strong>
     <p><g:message code="records.csv.file.generate"/></p>
 </div>
<div class="table-box">
	<div class="table-scroll">
		<table id="invoices" cellspacing="0" cellpadding="0">
			<thead>
                <tr>
                    <th class="small first header-sortable">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="invoice.label.id"/>
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
                        <g:remoteSort action="list" sort="dueDate" update="column1">
                            <g:message code="invoice.label.duedate"/>
                        </g:remoteSort>
                    </th>
                    <th class="tiny2 header-sortable">
                        <g:remoteSort action="list" sort="invoiceStatus" update="column1">
                            <g:message code="invoice.label.status"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="total" update="column1">
                            <g:message code="invoice.label.amount"/>
                        </g:remoteSort>
                    </th>
                    <th class="small last header-sortable">
                        <g:remoteSort action="list" sort="balance" update="column1">
                            <sec:ifNotGranted roles="EDI_923">
                                <g:message code="invoice.label.balance"/>
                            </sec:ifNotGranted>
                            <sec:ifAllGranted roles="EDI_923">
                                <g:message code="invoice.label.rate"/>
                            </sec:ifAllGranted>
                        </g:remoteSort>
                    </th>
                </tr>
	        </thead>
	        
	        <tbody>
			<g:each var="inv" in="${invoices}">
            
                <g:set var="currency" value="${currencies.find{ it.id == inv?.currencyId}}"/>
                <g:set var="contact" value="${ContactDTO.findByUserId(inv?.baseUser?.id)}"/>
                
				<tr id="invoice-${inv.id}" class="${invoice?.id == inv.id ? 'active' : ''}">
					<td class="medium">
						<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(inv?.publicNumber)}</strong>
                            <em><g:message code="table.id.format" args="[inv.id as String]"/></em>
						</jB:secRemoteLink>
					</td>
                    <td>
                        <jB:secRemoteLink breadcrumb="id" class="cell double" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <strong>
                                <g:if test="${contact?.firstName || contact?.lastName}">
                                    ${StringEscapeUtils.escapeHtml(contact?.firstName)} &nbsp;${StringEscapeUtils.escapeHtml(contact?.lastName)}
                                </g:if>
                                <g:else>
                                    ${StringEscapeUtils.escapeHtml(displayer?.getDisplayName(inv?.baseUser))}
                                </g:else>
                            </strong>
                            <em>${StringEscapeUtils.escapeHtml(contact?.organizationName)}</em>
                        </jB:secRemoteLink>
                    </td>
                    <g:isRoot>
                		<td>
                    		<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                        		<strong>${StringEscapeUtils.escapeHtml(inv?.baseUser?.company?.description)}</strong>
                   			</jB:secRemoteLink>
                		</td>
                	</g:isRoot>
	            	<td>
						<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <g:formatDate date="${inv?.dueDate}" formatName="date.pretty.format"/>
						</jB:secRemoteLink>
					</td>
					<td>
						<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <g:if test="${inv.isReview == 1}">
                                <g:message code="invoice.status.review"/>
                            </g:if>
                            <g:else>
                                ${StringEscapeUtils.escapeHtml(inv?.getInvoiceStatus()?.getDescription(session['language_id']))}
                            </g:else>
						</jB:secRemoteLink>
					</td>
					<td>
						<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <g:formatNumber number="${inv.total}"  type="currency" currencySymbol="${currency?.symbol}"/>
						</jB:secRemoteLink>
					</td>
					<td>
						<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${inv.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                            <sec:ifNotGranted roles="EDI_923">
                                <g:formatNumber number="${inv.balance}" type="currency" currencySymbol="${currency?.symbol}"/>
                            </sec:ifNotGranted>
                            <sec:ifAllGranted roles="EDI_923">
                                <g:set var="invoiceWS" value="${(com.sapienter.jbilling.server.invoice.db.InvoiceDTO) inv}"/>
                                <g:set var="value" value="${invoiceWS.invoiceLines.find {
                                    [FileConstants.COMMODITY_ELECTRICITY.toLowerCase(), com.sapienter.jbilling.server.fileProcessing.FileConstants.COMMODITY_GAS.toLowerCase()].contains(new com.sapienter.jbilling.server.item.db.ItemDAS().find(it.itemId)?.internalNumber)
                                }?.price}"/>
                                <g:formatNumber number="${value}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="5" />
                            </sec:ifAllGranted>
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
        <sec:access url="/invoice/csv">
            <div class="download">
                <g:link action="batchPdf" id="${invoice?.id}" params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}" class="pager-button">
                    <g:message code="download.batch.pdf.link"/>
                </g:link>
            </div>
        </sec:access>
        <sec:ifAnyGranted roles="INVOICE_73">
        <div id="download-div" class="download">
			<g:if test="${csvExportFlag != 0}">
				<div class="pager-button" id="generateCsv">
					<a onclick="generateCSV(); showMessage()">
					<g:message code="generate.csv.link" /></a>
                </div>
			</g:if>
            <g:else>
                 <g:link action="csv" id="${invoice?.id}"
                 params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}" class="pager-button">
                 <g:message code="download.csv.link" />
                 </g:link>
            </g:else>
            </div>
        </sec:ifAnyGranted>
    </div>
    <jB:isPaginationAvailable total="${invoices?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="invoice" action="list" params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}" total="${invoices?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<div class="btn-box">
    <div class="row"></div>
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
