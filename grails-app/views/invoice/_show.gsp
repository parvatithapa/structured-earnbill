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

<%@page import="com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO; com.sapienter.jbilling.server.pricing.db.PriceModelStrategy"%>
<%@ page import="com.sapienter.jbilling.server.invoice.InvoiceLineComparator; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.timezone.TimezoneHelper"%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<g:set var="currency" value="${selected.currency}"/>
<g:set var="user" value="${selected.baseUser}"/>

<div class="column-hold">

    <div class="heading">
        <strong>
            <g:if test="${selected.isReview == 1}">
                <g:message code="invoice.label.review.details"/>
            </g:if>
            <g:else>
                <g:message code="invoice.label.details"/>
            </g:else>
            <em>${selected.publicNumber}</em>
        </strong>
    </div>

    <!-- Invoice details -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable">
                    <tr>
                        <td colspan="2">
                            <strong>
                                <g:if test="${user.contact?.firstName || user.contact?.lastName}">
                                    ${user.contact?.firstName}&nbsp;${user.contact?.lastName}
                                </g:if>
                                <g:else>
                                    ${displayer?.getDisplayName(user)}
                                </g:else>
                            </strong><br>
                            <em>${user.contact?.organizationName}</em>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.user.id"/></td>
                        <td class="value">
                            <sec:access url="/customer/show">
                                <g:remoteLink controller="customer" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                                    ${user.id}
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/customer/show">
                                ${user.id}
                            </sec:noAccess>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.user.name"/>:</td>
                        <td class="value">${displayer?.getDisplayName(user)}</td>
                    </tr>
                    <g:isRoot>
                    	<tr>
                            	<td><g:message code="invoice.label.company.name"/></td>
                            	<td class="value">${user?.company.description}</td>
                        	</tr>
                    </g:isRoot>
                </table>
        
                <table class="dataTable">
                    <tr>
                        <td><g:message code="invoice.label.id"/></td>
                        <td class="value">${selected.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.number"/></td>
                        <td class="value">${selected.number}</td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.status"/></td>
                        <td class="value">
                            <g:if test="${selected.isReview == 1}">
                                <g:message code="invoice.status.review"/>
                            </g:if>
                            <g:else>
                                ${selected.invoiceStatus.getDescription(session['language_id'])}
                            </g:else>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.date"/></td>
                        <td class="value">
                            <g:formatDate date="${selected?.createDatetime}" formatName="date.pretty.format"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.duedate"/></td>
                        <td class="value">
                            <g:formatDate date="${selected?.dueDate}" formatName="date.pretty.format"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.gen.date"/></td>
                        <td class="value">
                            <g:formatDate date="${selected?.createTimestamp}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.amount"/></td>
                        <td class="value">
                            <g:formatNumber number="${selected?.total ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.balance"/></td>
                        <td class="value">
                            <g:formatNumber number="${selected?.balance ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.carried.bal"/></td>
                        <td class="value">
                            <g:formatNumber number="${selected?.carriedBalance ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}"/>
                        </td>
                    </tr>
                    <g:if test="${irn}">
						<tr>
	                      <td><g:message code="IRN"/></td>
	                      <td style="font-size: smaller;" class="value">${irn}</td></tr>
	                    <tr>
                    </g:if>
                    <tr>
                        <td><g:message code="invoice.label.payment.attempts"/></td>
                        <td class="value">${selected.paymentAttempts}</td></tr>
                    <tr>
                        <td><g:message code="invoice.label.orders"/></td>
                        <td class="value">
                            <g:each var="orderProcess" status="i" in="${selected.orderProcesses}">
                                <sec:access url="/order/show">
                                <g:remoteLink breadcrumb="id" controller="order" action="show" id="${orderProcess.purchaseOrder.id}" params="['template': 'order']" before="register(this);" onSuccess="render(data, next);">
                                    ${orderProcess.purchaseOrder.id}
                                </g:remoteLink>
                                </sec:access>
                                <sec:noAccess url="/order/show">
                                    ${orderProcess.purchaseOrder.id}
                                </sec:noAccess>
                                <g:if test="${i < selected.orderProcesses.size() -1}">,</g:if>
                            </g:each>
                        </td>
                    </tr>
                    <g:if test="${selected.creditNoteGenerated}">
                        <tr>
                            <td><g:message code="invoice.label.creditNote"/></td>
                            <td class="value">

                                <sec:access url="/creditNote/show">
                                    <g:remoteLink breadcrumb="id" controller="creditNote" action="show" id="${selected.creditNoteGenerated.id}"  before="register(this);" onSuccess="render(data, next);">
                                        ${selected.creditNoteGenerated.id}
                                    </g:remoteLink>
                                </sec:access>
                                <sec:noAccess url="/creditNote/show">
                                    ${selected.creditNoteGenerated.id}
                                </sec:noAccess>
                            </td>
                        </tr>
                    </g:if>
                    <g:if test="${selected.invoice}">
                        <tr>
                            <td><g:message code="invoice.label.delegated.to"/></td>
                            <td class="value">
                                <g:remoteLink controller="invoice" action="show" id="${selected.invoice.id}" before="register(this);" onSuccess="render(data, next);">
                                    ${selected.invoice.id}
                                </g:remoteLink>
                            </td>
                        </tr>
                    </g:if>
                    <g:if test="${selected?.metaFields}">
                        <!-- empty spacer row -->
                        <tr>
                            <td colspan="2"><br/></td>
                        </tr>
                        <g:render template="/metaFields/metaFields" model="[metaFields: selected?.metaFields]"/>
                    </g:if>            
                </table>
            </div>
    </div>

    <!-- delegated invoice -->
    <g:if test="${selected.invoices}">
        <div class="heading">
            <strong><g:message code="invoice.title.delegated"/></strong>
        </div>
        <div class="box">

            <div class="sub-box">
                <g:each var="delegatedInvoice" status="i" in="${selected.invoices}">
                    <table class="dataTable">
                    <tr>
                        <td><g:message code="invoice.label.id"/></td>
                        <td class="value">
                            <g:remoteLink controller="invoice" action="show" id="${delegatedInvoice.id}" before="register(this);" onSuccess="render(data, next);">
                                ${delegatedInvoice.id}
                            </g:remoteLink>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.duedate"/></td>
                        <td class="value">
                            <g:formatDate date="${delegatedInvoice.dueDate}" formatName="date.pretty.format"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.balance"/></td>
                        <td class="value">
                            <g:formatNumber number="${delegatedInvoice.balance ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}"/>
                        </td>
                    </tr>
                    </table>
    
                    <g:if test="${i < selected.invoices.size() -1}">
                        <div><hr/></div>
                    </g:if>
                </g:each>
            </div>
        </div>
    </g:if>


    <!-- invoice lines -->
    <div class="heading">
        <strong><g:message code="invoice.label.lines"/></strong>
    </div>
    <div class="box">
        <div class="sub-box">
            <table class="innerTable" >
                <thead class="innerHeader">
                <tr>
                    <th class="first"><g:message code="label.gui.description"/></th>
                    <th><g:message code="label.gui.quantity"/></th>
                    <th><g:message code="label.gui.price"/></th>
                    <th><g:message code="label.gui.tax"/></th>
                    <th class="last"><g:message code="label.gui.amount"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="line" in="${lines}" status="idx">
                    <tr>
                        <td class="${line.id==0 ? 'strong' : ''} innerContent">
                            <g:if test="${line.parentLine}">&nbsp;&nbsp;</g:if>${line?.description}
                            <g:if test="${line.id==0}">
                                <sec:access url="/customer/show">
                                    <g:remoteLink controller="customer" action="show" id="${line?.sourceUserId}" before="register(this);" onSuccess="render(data, next);">
                                        (${line?.sourceUserId})
                                    </g:remoteLink>
                                </sec:access>
                                <sec:noAccess url="/customer/show">
                                    (${line?.sourceUserId})
                                </sec:noAccess>
                            </g:if>
                        </td>
                        <td class="${line.id==0 ? 'hide' : ''} innerContent">
                            <g:if test="${((line?.item?.percentage ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) && (! line?.dueInvoiceLine()) && (! line?.isPercentage)}">
                                <g:formatNumber number="${line?.quantity}" formatName="decimal.format"/>
                            </g:if>
                        </td>
                        <td class="${line.id==0 ? 'hide' : ''} innerContent">
                            <g:set var="price" value="${ line?.item?.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
                            <g:if test="${line.isPercentage}">
                                <g:formatNumber number="${line?.price ?: BigDecimal.ZERO}" type="number" maxFractionDigits="5"/>%
                            </g:if>
                            <g:else>
                                <g:if test="${! line?.dueInvoiceLine()}">
                                    <g:formatNumber number="${line?.price ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="5"/>
                                </g:if>
                            </g:else>
                        </td>
                        <td class="${line.id==0 ? 'hide' : ''} innerContent">
                            <g:formatNumber number="${line?.taxAmount ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="2"/>
                        </td>
                        <td class="${line.id==0 ? 'hide' : ''} innerContent">
                            <g:formatNumber number="${line?.amount ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="2"/>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
    </div>
 

    <div class="btn-box">
        <div class="row">
        <g:if test="${selected.isReview == 0}">
            <sec:ifAllGranted roles="PAYMENT_30">
                <g:if test="${session['company_id']?.equals(selected.baseUser?.company?.id)}">
                    <a href="${createLink (controller: 'payment', action: 'edit', params: [userId: user?.id, invoiceId: selected.id])}" class="submit payment button-primary">
                        <span><g:message code="button.invoice.pay"/></span>
                    </a>
                </g:if>
            </sec:ifAllGranted>

            <a href="${createLink (action: 'downloadPdf', id: selected.id)}" class="submit save">
                <span><g:message code="button.invoice.downloadPdf"/></span>
            </a>
         </g:if>
        </div>

        <div class="row">
            <sec:access url="/mediation/invoice">
                <g:link class="submit show" controller="mediation" action="invoice" id="${selected.id}" params="${params + ['first': 'true']}">
                    <span><g:message code="button.view.events"/></span>
                </g:link>
            </sec:access>
            <g:if test="${selected.isReview == 0}">
                <sec:access url="/invoice/email">
                    <a href="${createLink(action: 'email', id: selected.id)}" class="submit email">
                        <span><g:message code="button.invoice.sendEmail"/></span>
                    </a>
                </sec:access>
            </g:if>
        <!-- Generate E-invoice button -->
            <sec:ifAllGranted roles="CUSTOMER_2120">
                 <g:if test="${irn}">
                    <button type="button" disabled="disabled" class="einvoiceGen">
                        <g:message code="generate.e.invoice"/>
                    </button>
                 </g:if>
                 <g:else>
                    <a href="${createLink (action: 'generateEInvoice', id: selected.id)}" class="submit">
                       <span><g:message code="generate.e.invoice"/></span>
                    </a>
                 </g:else>
            </sec:ifAllGranted>

            <g:settingEnabled property="hbase.audit.logging">
                    <sec:access url="/invoice/history">
                        <g:link controller="invoice" action="history" id="${selected.id}" class="submit show"><span><g:message code="button.view.history"/></span></g:link>
                    </sec:access>
            </g:settingEnabled>
        </div>
        <g:if test="${selected.isReview == 0 && distributelMCFFile}">
            <div class="row">
                    <a href="${createLink (action: 'downloadDistributelMCFFile', id: selected.id)}" class="submit save">
                        <span><g:message code="button.invoice.downloadDistrributelMCFFile"/></span>
                    </a>
            </div>
        </g:if>
    </div>


<div class="heading">
    <strong><g:message code="invoice.label.available.credit"/></strong>
</div>
<div class="box">
    <div class="sub-box">
<g:if test="${availableCredits}">
        <g:hiddenField name="link_creditNote_id" value="-1"/>
        <table class="innerTable" >
            <thead class="innerHeader">
            <tr>
                <th class="first"><g:message code="label.gui.payment.id"/></th>
                <th><g:message code="label.gui.date"/></th>
                <th><g:message code="label.gui.payment.refunds"/></th>
                <th><g:message code="label.gui.amountPaid"/></th>

                <th class="last"></th>
            </tr>
            </thead>
            <tbody>
            <g:each var="creditNote" in="${availableCredits}" status="idx">
                <tr>
                    <td class="innerContent">
                        <sec:access url="/creditNote/show">
                            <g:remoteLink breadcrumb="id" controller="creditNote" action="show" id="${creditNote.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                ${creditNote.id}
                            </g:remoteLink>
                        </sec:access>
                        <sec:noAccess url="/creditNote/show">
                            ${creditNote.id}
                        </sec:noAccess>
                    </td>
                    <td class="innerContent">
                        <g:formatDate date="${creditNote.createDateTime}" formatName="date.pretty.format"/>
                    </td>
                    <td class="innerContent">
                        ${"C"}
                    </td>
                    <td class="innerContent">
                        <g:formatNumber number="${new BigDecimal(creditNote.balance ?: 0)}" type="currency" currencySymbol="${currency?.symbol}"/>
                    </td>

                    <td class="innerContent">
                        <sec:access url="/invoice/linkCreditNote">
                            <a onclick="setLinkCreditNoteId(${selected.id}, ${creditNote.id});">
                                <span><g:message code="invoice.prompt.link.creditNote"/></span>
                            </a>
                        </sec:access>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </g:if>
        <g:else>
            <em><g:message code="invoice.prompt.no.available.credit"/></em>
        </g:else>
    </div>
</div>
    <!-- payments -->
    <div class="heading">
        <strong><g:message code="invoice.label.payment.refunds"/></strong>
    </div>


    <div class="box">
        <div class="sub-box">
            <g:if test="${selected.paymentMap || selected.creditNoteMap}">
            <table class="innerTable" >
            <g:if test="${selected.paymentMap }">
                <g:hiddenField name="unlink_payment_id" value="-1"/>
                    <thead class="innerHeader">
                    <tr>
                        <th class="first"><g:message code="label.gui.payment.id"/></th>
                        <th><g:message code="label.gui.date"/></th>
                        <th><g:message code="label.gui.payment.refunds"/></th>
                        <th><g:message code="label.gui.amountPaid"/></th>
                        <th><g:message code="label.gui.method"/></th>
                        <th><g:message code="label.gui.result"/></th>
                        <th class="last"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="paymentInvoice" in="${selected.paymentMap}" status="idx">
                        <tr>
                            <td class="innerContent">
                                <sec:access url="/payment/show">
                                    <g:remoteLink breadcrumb="id" controller="payment" action="show" id="${paymentInvoice.payment.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                        ${paymentInvoice.payment.id}
                                    </g:remoteLink>
                                </sec:access>
                                <sec:noAccess url="/payment/show">
                                    ${paymentInvoice.payment.id}
                                </sec:noAccess>
                            </td>
                            <td class="innerContent">
                                <g:formatDate date="${paymentInvoice.payment.paymentDate}" formatName="date.pretty.format"/>
                            </td>
                            <td class="innerContent">
                                ${paymentInvoice.payment.isRefund? "R":"P"}
                            </td>
                            <td class="innerContent">
                                <g:formatNumber number="${new BigDecimal(paymentInvoice.amount ?: 0)}" type="currency" currencySymbol="${currency?.symbol}"/>
                            </td>
                            <td class="innerContent">
                                ${paymentInvoice.payment.paymentMethod.getDescription(paymentInvoice.payment,session['language_id'])}
                            </td>
                            <td class="innerContent">
                                ${paymentInvoice.payment.paymentResult.getDescription(session['language_id'])}
                            </td>
                            <td class="innerContent">
                                <sec:access url="/invoice/unlink">
                                    <a onclick="setUnlinkPaymentId(${selected.id}, ${paymentInvoice.payment.id});">
                                        <span><g:message code="invoice.prompt.unlink.payment"/></span>
                                    </a>
                                </sec:access>
                            </td>
                        </tr>
                    </g:each>
                    </tbody>
            </g:if>
            <g:if test="${selected.creditNoteMap }">
                <g:hiddenField name="unlink_creditNote_id" value="-1"/>
                    <thead class="innerHeader">
                    <tr>
                        <th class="first"><g:message code="label.gui.payment.id"/></th>
                        <th><g:message code="label.gui.date"/></th>
                        <th><g:message code="label.gui.payment.refunds"/></th>
                        <th><g:message code="label.gui.amountPaid"/></th>
                        <th></th>
                        <th></th>
                        <th class="last"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="creditNoteInvoice" in="${selected.creditNoteMap}" status="idx">
                        <tr>
                            <td class="innerContent">
                                <sec:access url="/creditNote/show">
                                    <g:remoteLink breadcrumb="id" controller="creditNote" action="show" id="${creditNoteInvoice.creditNote.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                        ${creditNoteInvoice.creditNote.id}
                                    </g:remoteLink>
                                </sec:access>
                                <sec:noAccess url="/creditNote/show">
                                    ${creditNoteInvoice.creditNote.id}
                                </sec:noAccess>
                            </td>
                            <td class="innerContent">
                                <g:formatDate date="${creditNoteInvoice.creditNote.createDateTime}" formatName="date.pretty.format"/>
                            </td>
                            <td class="innerContent">
                                ${"C"}
                            </td>
                            <td class="innerContent">
                                <g:formatNumber number="${new BigDecimal(creditNoteInvoice.amount ?: 0)}" type="currency" currencySymbol="${currency?.symbol}"/>
                            </td>
                            <td></td>
                            <td></td>
                            <td class="innerContent">
                                <sec:access url="/invoice/unlinkCreditNote">
                                    <a onclick="setUnlinkcreditNoteId(${selected.id}, ${creditNoteInvoice.creditNote.id});">
                                        <span><g:message code="invoice.prompt.unlink.creditNote"/></span>
                                    </a>
                                </sec:access>
                            </td>
                        </tr>
                    </g:each>
                    </tbody>
            </g:if>
            </table>
            </g:if>
            <g:else>
                <em><g:message code="invoice.prompt.no.payments.refunds"/></em>
            </g:else>
        </div>
    </div>

    <!-- Invoice Notes -->
    <g:if test="${selected.customerNotes}">
        <div class="heading">
            <strong><g:message code="invoice.label.note"/></strong>
        </div>
        <div class="box">
           <div class="sub-box"><p>${selected.customerNotes}</p></div>
        </div>
    </g:if>
   <g:preferenceIsNullOrEquals preferenceId="${Constants.PREFERENCE_DISPLAY_PAYMENT_URL_LINK_NOTIFICATION}" value="1">
        <div class="heading">
            <strong><g:message code="payment.link.header"/></strong>
        </div>
        <div class="box" id="paymentUrlLogDiv">
            <div class="sub-box">
                <g:render template="/invoice/paymentUrlTemplate"
                        model="[selected: selected,
                                paymentUrlLogs: paymentUrlLogs,
                                ]"/>
            </div>
        </div>
    </g:preferenceIsNullOrEquals>
    <div class="btn-box">
        <g:if test="${!selected.creditNoteGenerated && !selected.creditNoteMap}">
        <sec:ifAllGranted roles="INVOICE_70">
            <g:preferenceIsNullOrEquals preferenceId="${Constants.PREFERENCE_INVOICE_DELETE}" value="1">
                <g:if test="${selected.id && selected.isReview == 0}">
                    <a onclick="showConfirm('delete-'+${selected.id});" class="submit delete">
                        <span><g:message code="button.delete.invoice"/></span>
                    </a>
                </g:if>
            </g:preferenceIsNullOrEquals>
        </sec:ifAllGranted>
        </g:if>
    </div>
</div>

<script type="text/javascript">
    function setLinkCreditNoteId(invId, pymId) {
        $('#link_creditNote_id').val(pymId);
        showConfirm("linkCreditNote-" + invId);
        return true;
    }
    function setLinkCreditId() {
        $('#confirm-command-form-linkCreditNote-${selected.id} [name=linkCreditNoteId]').val($('#link_creditNote_id').val());
    }
    function setUnlinkPaymentId(invId, pymId) {
        $('#unlink_payment_id').val(pymId);
        showConfirm("unlink-" + invId);
        return true;
    }
    function setPaymentId() {
        $('#confirm-command-form-unlink-${selected.id} [name=paymentId]').val($('#unlink_payment_id').val());
    }

    function setUnlinkcreditNoteId(invId, pymId) {
        $('#unlink_creditNote_id').val(pymId);
        showConfirm("unlinkCreditNote-" + invId);
        return true;
    }
    function setcreditNoteId() {
        $('#confirm-command-form-unlinkCreditNote-${selected.id} [name=creditNoteId]').val($('#unlink_creditNote_id').val());
    }

</script>


<g:render template="/confirm"
          model="[message: 'invoice.prompt.confirm.link.creditNote',
                  controller: 'invoice',
                  action: 'linkCreditNote',
                  id: selected.id,
                  formParams: [ 'linkCreditNoteId': '-1' ],
                  onYes: 'setLinkCreditId()',
          ]"/>
<g:render template="/confirm"
          model="[message: 'invoice.prompt.confirm.remove.payment.link',
                  controller: 'invoice',
                  action: 'unlink',
                  id: selected.id,
                  formParams: [ 'paymentId': '-1' ],
                  onYes: 'setPaymentId()',
                 ]"/>

<g:render template="/confirm"
          model="[message: 'creditNote.prompt.confirm.remove.creditNote.link',
                  controller: 'invoice',
                  action: 'unlinkCreditNote',
                  id: selected.id,
                  formParams: [ 'creditNoteId': '-1' ],
                  onYes: 'setcreditNoteId()',
          ]"/>

<g:render template="/confirm"
          model="[message: 'invoice.prompt.are.you.sure',
                  controller: 'invoice',
                  action: 'delete',
                  id: selected.id,
                 ]"/>
