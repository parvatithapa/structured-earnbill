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

<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper; com.sapienter.jbilling.server.metafields.db.MetaField; com.sapienter.jbilling.server.user.db.UserDTO; com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO; com.sapienter.jbilling.server.user.db.AccountTypeDTO; com.sapienter.jbilling.server.invoice.InvoiceLineComparator; com.sapienter.jbilling.server.metafields.DataType; com.sapienter.jbilling.server.process.db.PeriodUnitDTO;	com.sapienter.jbilling.server.customer.CustomerBL; com.sapienter.jbilling.server.user.UserBL; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.util.Util; com.sapienter.jbilling.server.user.contact.db.ContactDTO"%>

<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="form-edit">

    <g:set var="customer" value="${user.customer}"/>

    <div class="heading">
        <strong>
            ${displayer.getDisplayName(user)}
            <g:if test="${user.deleted}">
                <span style="color: #ff0000;">(<g:message code="object.deleted.title"/>)</span>
            </g:if>
        </strong>
    </div>

    <div class="form-hold">
        <g:if test="${configurationPreview}">
            <div class="btn-row">
                <g:if test="${null!=customerInformation}">
                    <span><g:message code="customer.view.configuration.preview.title"/></span>
                    <g:link action="approveConfiguration" params="[id: user.id]" class="submit apply"><span><g:message code="customer.view.configuration.preview.approve.button"/></span></g:link>
                    <g:link action="discardConfiguration" params="[id: user.id, previewConfiguration: false]" class="submit delete"><span><g:message code="customer.view.configuration.preview.discard.button"/></span></g:link>
                </g:if>
                <g:else>
                    <span><g:link action="discardConfiguration" params="[id: user.id, previewConfiguration: false]" class="submit delete"><g:message code="customer.view.configuration.preview.discard.button"/></g:link></span>
                </g:else>
            </div>
        </g:if>

        <fieldset>
            <g:if test="${customerInformation}">
                <!-- configurable section -->
                <div id="configurable-section" class="column">
                    <g:render template="configurable"/>
                </div>
            </g:if>

            <!-- buttons -->
            <div style="margin: 20px 0;">
                <div class="btn-row fix-width">
                    <sec:access url="/auditLog/user">
                        <g:link controller="auditLog" action="user" id="${user.id}" class="submit show"><span><g:message code="customer.view.audit.log.button"/></span></g:link>
                    </sec:access>

                    <sec:access url="/invoice/user">
                        <g:link controller="invoice" action="user" id="${user.id}" class="submit show"><span><g:message code="customer.view.invoices.button"/></span></g:link>
                    </sec:access>

                    <sec:access url="/payment/user">
                        <g:link controller="payment" action="user" id="${user.id}" class="submit payment"><span><g:message code="customer.view.payments.button"/></span></g:link>
                    </sec:access>

                    <sec:access url="/order/user">
                        <g:link controller="order" action="user" id="${user.id}" class="submit order"><span><g:message code="customer.view.orders.button"/></span></g:link>
                    </sec:access>
                </div>
                <div class="btn-row fix-width">
                <g:if test="${!user.deleted}">

                    <g:settingEnabled property="hbase.audit.logging">
                        <sec:access url="/customer/history">
                            <g:link controller="customer" action="history" id="${user.id}" class="submit show"><span><g:message code="customer.view.history.button"/></span></g:link>
                        </sec:access>
                    </g:settingEnabled>

                    <sec:ifAllGranted roles="CUSTOMER_11">
                        <g:if test="${isCurrentCompanyOwning}">
                            <g:link controller="customer" action="edit" id="${user.id}" class="submit edit"><span><g:message code="customer.edit.customer.button"/></span></g:link>
                        </g:if>
                    </sec:ifAllGranted>

                    <sec:ifAllGranted roles="PAYMENT_30">
                        <g:if test="${isCurrentCompanyOwning}">
                            <g:link controller="payment" action="edit" params="[userId: user.id]" class="submit payment"><span><g:message code="button.make.payment"/></span></g:link>
                        </g:if>
                    </sec:ifAllGranted>

                    <sec:ifAllGranted roles="ORDER_20">
                        <g:if test="${isCurrentCompanyOwning}">
                            <g:link controller="orderBuilder" action="edit" params="[userId: user.id]" class="submit order"><span><g:message code="button.create.order"/></span></g:link>
                        </g:if>
                    </sec:ifAllGranted>
                    
                    <sec:access url="/blacklist/user">
                        <g:link controller="blacklist" action="user" id="${user.id}" class="submit add"><span><g:message code="customer.blacklist.button"/></span></g:link>
                    </sec:access>
                    
                </g:if>
                </div>
            </div>

            <!-- blacklist matches -->
            <g:if test="${blacklistMatches}">
                <div id="blacklist" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.blacklist.title"/></span></a>
                    </div>
                    <div class="box-card-hold">
                        <div class="form-columns">
                            <table cellpadding="0" cellspacing="0" class="dataTable">
                                <tbody>

                                    <g:each var="match" status="i" in="${blacklistMatches}">
                                        <tr>
                                            <td class="value">
                                                ${match}
                                            </td>
                                        </tr>
                                    </g:each>

                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </g:if>

            <!-- last payment -->
            <g:if test="${payment}">
                <div id="payment" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.last.payment.title"/></span></a>
                    </div>
                    <div class="box-card-hold">
                        <div class="form-columns">
                            <g:render template="payment" model="[payment: payment]"/>
                        </div>
                    </div>
                </div>
            </g:if>

            <!-- last invoice -->
            <g:if test="${invoice}">
                <div id="invoice" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.last.invoice.title"/></span></a>
                    </div>
                    <div class="box-card-hold">

                        <div class="form-columns">
                            <table cellpadding="0" cellspacing="0" class="dataTable">
                                <tbody>
                                    <tr>
                                        <td><g:message code="invoice.label.id"/></td>
                                        <td class="value"><jB:secLink url="/invoice/show" controller="invoice" action="list" id="${invoice.id}">${invoice.id}</jB:secLink></td>

                                        <td><g:message code="invoice.label.date"/></td>
                                        <td class="value"><g:formatDate date="${invoice.createDatetime}" formatName="date.pretty.format"/></td>

                                        <td><g:message code="invoice.amount.date"/></td>
                                        <td class="value"><g:formatNumber number="${invoice.total}" type="currency" currencySymbol="${invoice.currency.symbol}"/></td>

                                        <td><g:message code="invoice.label.delegation"/></td>
                                        <td class="value">
                                            <g:each var="delegated" in="${invoice.invoices}" status="i">
                                                <jB:secLink url="/invoice/show" controller="invoice" action="list" id="${delegated.id}">${delegated.id}</jB:secLink>
                                                <g:if test="${i < invoice.invoices.size()-1}">, </g:if>
                                            </g:each>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><g:message code="invoice.label.status"/></td>
                                        <td class="value">
                                            <g:if test="${invoice.invoiceStatus.id == Constants.INVOICE_STATUS_UNPAID}">
                                                <jB:secLink controller="payment" action="edit" params="[userId: user.id, invoiceId: invoice.id]" title="${message(code: 'invoice.pay.link')}">
                                                    ${invoice.invoiceStatus.getDescription(session['language_id'])}
                                                </jB:secLink>
                                            </g:if>
                                            <g:else>
                                                ${invoice.invoiceStatus.getDescription(session['language_id'])}
                                            </g:else>
                                        </td>

                                        <td><g:message code="invoice.label.duedate"/></td>
                                        <td class="value"><g:formatDate date="${invoice.dueDate}" formatName="date.pretty.format"/></td>

                                        <td><g:message code="invoice.label.balance"/></td>
                                        <td class="value"><g:formatNumber number="${invoice.balance}" type="currency" currencySymbol="${invoice.currency.symbol}"/></td>

                                        <td><g:message code="invoice.label.orders"/></td>
                                        <td class="value">
                                            <g:each var="process" in="${invoice.orderProcesses}" status="i">
                                                <jB:secLink url="/order/show"  controller="order" action="list" id="${process.purchaseOrder.id}">${process.purchaseOrder.id}</jB:secLink>
                                                <g:if test="${i < invoice.orderProcesses.size()-1}">, </g:if>
                                            </g:each>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><g:message code="invoice.label.payment.attempts"/></td>
                                        <td class="value">${invoice.paymentAttempts}</td>

                                        <td><g:message code="invoice.label.gen.date"/></td>
                                        <td class="value"><g:formatDate date="${invoice.createTimestamp}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/></td>

                                        <td><g:message code="invoice.label.carried.bal"/></td>
                                        <td class="value"><g:formatNumber number="${invoice.carriedBalance}" type="currency" currencySymbol="${invoice.currency.symbol}"/></td>

                                        <!-- spacer -->
                                        <td></td><td></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <table cellpadding="0" cellspacing="0" class="innerTable">
                            <thead class="innerHeader">
                            <tr>
                                <th class="first"><g:message code="label.gui.description"/></th>
                                <th><g:message code="label.gui.quantity"/></th>
                                <th><g:message code="label.gui.price"/></th>
                                <th class="last"><g:message code="label.gui.amount"/></th>
                            </tr>
                            </thead>
                            <tbody>
                                <%
                                    def invoiceLines = new ArrayList(invoice.invoiceLines)
                                    Collections.sort(invoiceLines, new InvoiceLineComparator())
                                %>
                                <g:each var="invoiceLine" in="${invoiceLines}">
                                    <tr>
                                        <td class="innerContent">
                                            ${invoiceLine.description}
                                        </td>

                                        <td class="innerContent">
                                            <g:formatNumber number="${invoiceLine.quantity}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:set var="price" value="${ invoiceLine?.item?.getPrice(com.sapienter.jbilling.server.timezone.TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
                                            <g:if test="${invoiceLine.isPercentage}">
                                                <g:formatNumber number="${invoiceLine?.price ?: BigDecimal.ZERO}" type="number" maxFractionDigits="5"/>%
                                            </g:if>
                                            <g:else>
                                                <g:if test="${! invoiceLine?.dueInvoiceLine()}">
                                                    <g:formatNumber number="${invoiceLine?.price ?: BigDecimal.ZERO}" type="currency" currencySymbol="${invoice.currency.symbol}" maxFractionDigits="5"/>
                                                </g:if>
                                            </g:else>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${invoiceLine.amount}" type="currency" currencySymbol="${invoice.currency.symbol}"/>
                                        </td>
                                    </tr>
                                </g:each>
                            </tbody>
                        </table>

                        <g:if test="${invoice.paymentMap}">
                            <div class="box-cards">
                                <div class="box-cards-title">
                                    <span><g:message code="invoice.label.payment.refunds"/></span>
                                </div>
                                <div class="box-card-hold">

                                    <g:each var="invoicePayment" in="${invoice.paymentMap}" status="i">
                                        <g:render template="payment" model="[payment: invoicePayment.payment]"/>
                                        <g:if test="${i < invoice.paymentMap.size()-1}"><hr/></g:if>
                                    </g:each>
                                </div>
                            </div>
                        </g:if>

                    </div>
                </div>
            </g:if>

            <!-- subscriptions -->
            <g:if test="${subscriptions}">
                <div id="subscriptions" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.subscriptions.title"/></span></a>
                    </div>
                    <div class="box-card-hold">

                        <table cellpadding="0" cellspacing="0" class="innerTable">
                            <thead class="innerHeader">
                            <tr>
                                <th class="first"><g:message code="order.label.id"/></th>
                                <th><g:message code="label.gui.planOrProduct"/></th>
                                <th><g:message code="label.gui.description"/></th>
                                <th><g:message code="order.label.active.since"/></th>
                                <th><g:message code="order.label.active.until"/></th>
                                <th><g:message code="label.gui.period"/></th>
                                <th><g:message code="label.gui.quantity"/></th>
                                <th><g:message code="label.gui.price"/></th>
                                <th class="last"><g:message code="label.gui.amount"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <g:each var="order" in="${subscriptions}">
                                <g:set var="currency" value="${currencies.find { it.id == order.currencyId }}"/>

                                <g:each var="orderLine" in="${order.orderLines}">
                                    <tr>
                                        <td class="innerContent">
                                            <jB:secLink url="/order/show" controller="order" action="list" id="${order.id}">${order.id}</jB:secLink>
                                        </td>
                                        <td class="innerContent">
                                            <g:if test=" ${null != orderLine.itemId && retrieveItems.get(orderLine.id).isPlan}">
                                                <g:message code="item.type.plan"/>
                                            </g:if>
                                            <g:else>
                                                <g:message code="item.type.product"/>
                                            </g:else>
                                        </td>
                                        <td class="innerContent">
                                            ${orderLine.description}
                                        </td>
                                        <td class="innerContent">
                                            <g:formatDate date="${order?.activeSince}" formatName="date.pretty.format"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatDate date="${order?.activeUntil}" formatName="date.pretty.format"/>
                                        </td>
                                        <td class="innerContent">
                                            ${order.periodStr}
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getQuantityAsDecimal()}" formatName="decimal.format"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getPriceAsDecimal()}" type="currency" currencySymbol="${currency.symbol}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getAmountAsDecimal()}" type="currency" currencySymbol="${currency.symbol}"/>
                                        </td>
                                    </tr>
                                </g:each>
                            </g:each>
                            </tbody>
                        </table>

                    </div>
                    <div class="box-card-hold">
                        <table cellpadding="0" cellspacing="0" class="innerTable">
                            <thead class="innerHeader">
                            <tr>
                                <th class="first"><g:message code="subscription.label.id"/></th>
                                <th><g:message code="subscription.account.name"/></th>
                                <th><g:message code="label.gui.description"/></th>
                              	<th><g:message code="subscription.asset.id"/></th>
                                <th><g:message code="label.gui.period"/></th>
                                <th><g:message code="label.gui.quantity"/></th>
                                <th><g:message code="label.gui.price"/></th>
                                <th class="last"><g:message code="label.gui.amount"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <g:each var="order" in="${internalSubscriptions}">
                                <g:set var="currency" value="${currencies.find { it.id == order.currencyId }}"/>

                                <g:each var="orderLine" in="${order.orderLines}">
                                    <tr>
                                        <td class="innerContent">
                                            <jB:secLink url="/order/show" controller="order" action="list" id="${order.id}">${order.id}</jB:secLink>
                                        </td>
                                        <td class="innerContent">
                                            ${UserDTO.get(order.userId).userName}
                                        </td>
                                        <td class="innerContent">
                                            ${orderLine.description}
                                        </td>
                                        <td class="innerContent">
                                        	<g:each var="asset" in="${orderLine.assetIds}">
                                        		${asset}
                                        	</g:each>
                                        </td>
                                        <td class="innerContent">
                                            ${order.periodStr}
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getQuantityAsDecimal()}" formatName="decimal.format"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getPriceAsDecimal()}" type="currency" currencySymbol="${currency.symbol}"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${orderLine.getAmountAsDecimal()}" type="currency" currencySymbol="${currency.symbol}"/>
                                        </td>
                                    </tr>
                                </g:each>
                            </g:each>
                            </tbody>
                        </table>

                    </div>
                </div>
            </g:if>

            <g:if test="${!user.deleted}">
                <!-- special pricing -->
                <div id="prices" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.prices.title"/></span></a>
                    </div>
                    <div class="box-card-hold">
                        <div class="form-columns">
                            <div id="products-column" class="column">
                                <g:render template="products"/>
                            </div>

                            <div id="prices-column" class="column">
                                <g:render template="prices" model="[priceExpiryMap: priceExpiryMap]"/>
                            </div>
                        </div>
                    </div>
                </div>
            </g:if>

            <!-- payment instruments -->
		    <g:if test="${user?.paymentInstruments.size() > 0}">
				<div class="box-cards">
		        <div class="box-cards-title">
                    <a class="btn-open"><span><g:message code="payment.instrument.list"/></span></a>
                </div>
		        <div class="box-card-hold">
		        	 <g:each var="instrument" in="${user.paymentInstruments}">
				            <div class="sub-box">
				                <table class="dataTable" cellspacing="0" cellpadding="0">
				                    <tbody>
				                        <tr>
				                            <td><g:message code="payment.instrument.name"/></td>
				                            <td class="value">${instrument.paymentMethodType.methodName}</td>
				                        </tr>
				                        <tr>
				                            <td>
                                               <g:render template="/metaFields/metaFields" model="[metaFields: instrument.metaFields, instrument:instrument]"/>
				                            </td>
				                        </tr>
				                    </tbody>
				                </table>
				            </div>
				     </g:each>
		        </div>
		        </div>
		    </g:if>
                        
            <!-- customer usage pools -->
            <g:if test="${customerUsagePools}">
                <div id="customerUsagePools" class="box-cards">
                    <div class="box-cards-title">
                        <a class="btn-open"><span><g:message code="customer.inspect.customerUsagePools.title"/></span></a>
                    </div>
                    <div class="box-card-hold">

                        <table cellpadding="0" cellspacing="0" class="innerTable">
                            <thead class="innerHeader">
                            <tr>
                                <th class="first" style="text-align:center;width:5%;"><g:message code="customerUsagePools.label.id"/></th>
                                <th style="text-align:center;width:11%;"><g:message code="customerUsagePools.usage.pool.id"/></th>
                                <th style="text-align:center;width:18%;"><g:message code="customerUsagePools.label.free.usage.name"/></th>
                                <th style="text-align:center;width:10%;"><g:message code="customerUsagePools.label.cycleStartDate"/></th>
                                <th style="text-align:center;width:8%;"><g:message code="customerUsagePools.label.quantity"/></th>
                                <th style="text-align:center;width:10%;"><g:message code="customerUsagePools.label.consumption"/></th>
                                <th style="text-align:center;width:10%;"><g:message code="customerUsagePools.label.cycle.end.date"/></th>
                                <th style="text-align:center;width:13%;"><g:message code="customerUsagePools.label.cycle.period"/></th>
                                <th class="last" style="text-align:center;width:11%;"><g:message code="customerUsagePools.label.reset.value"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <g:each var="customerusagePools" in="${customerUsagePools}">
                                <g:each var="customerusagePool" in="${customerusagePools}">
                                    <tr>
                                        <td class="innerContent" style="text-align:right;width:5%;padding-right: 1%;">
                                            ${customerusagePool.id}
                                        </td>
                                        <td class="innerContent" style="text-align:right;width:11%;padding-right: 3%;">
                                             <jB:secLink url="/usagePool/show" controller="usagePool" action="list" id="${customerusagePool.usagePool.id}">${customerusagePool.usagePool.id}</jB:secLink>
                                        </td>
                                        <td class="innerContent" style="text-align:center;width:18%;">
                                       		${customerusagePool.usagePool.getDescription(session['language_id'], 'name')}
                                        </td>
                                        <td class="innerContent" style="text-align:center;width:10%;">
                                             <span><g:formatDate date="${customerusagePool.cycleStartDate}" formatName="date.timeSecsAMPM.format"/></span>
                                        </td>
                                        <td class="innerContent" style="text-align:right;width:8%;padding-right: 2%;">
                                        	<g:formatNumber number="${customerusagePool.quantity}" formatName="decimal.format"/>
                                        </td>
                                        <td class="innerContent" style="text-align:right;width:10%;padding-right: 2%;">
                                            <g:if test="${(customerusagePool.initialQuantity.compareTo(BigDecimal.ZERO) != 0)}">
                                               <g:formatNumber number="${((customerusagePool.initialQuantity - customerusagePool.quantity)/customerusagePool.initialQuantity)*100}" formatName="decimal.format"/>
                                            </g:if>
                                            <g:else>
                                               <g:formatNumber number="${BigDecimal.ZERO}" formatName="decimal.format"/>
                                            </g:else>
                                        	%
                                        </td>
                                        <td class="innerContent" style="text-align:center;width:10%;">
                                        	 <span><g:formatDate date="${customerusagePool.cycleEndDate}" formatName="date.timeSecsAMPM.format"/></span>
                                        </td>
                                        <td class="innerContent" style="text-align:center;width:13%;">
                                            ${customerusagePool.usagePool.cyclePeriodValue} ${customerusagePool.usagePool.cyclePeriodUnit} 
                                        </td>
                                        <td class="innerContent" style="text-align:center;width:11%;">
                                            ${customerusagePool.usagePool.usagePoolResetValue}
                                        </td>
                                    </tr>
                                </g:each>
                            </g:each>
                            </tbody>
                        </table>

                    </div>
                </div>
            </g:if>

            <!-- notes -->
            <div id="notes" class="box-cards">
                <div class="box-cards-title">
                    <a class="btn-open" href="#"><span><g:message code="prompt.notes"/></span></a>
                </div>
                <div class="box-card-hold">
                    <div id="users-contain"  style="position:relative">
                        <div id="test">
                            <g:render template="customerNotes" model="[isNew:isNew, customerNotes:customerNotes, customerNotesTotal:customerNotesTotal, user:user]" />
                        </div>
                    </div>
                </div>
            </div>

            <!-- spacer -->
            <div><br/>&nbsp;</div>

            <!-- configuration upload -->
            <div class="box-cards" align="right">
                <g:uploadForm name="configurationForm" action="uploadConfiguration">
                    <table>
                        <tr>
                            <td>
                                <g:hiddenField name="id" value="${user.id}"/>

                                <g:applyLayout name="form/fileupload">
                                    <content tag="input.name">configurationFile</content>
                                </g:applyLayout>

                            </td>
                            <td>
                                <g:submitButton name="configurationFile2" value="Upload" class="submit apply"/>
                            </td>
                        </tr>
                    </table>
                </g:uploadForm>
            </div>

            <!-- spacer -->
            <div><br/>&nbsp;</div>

        </fieldset>
    </div> <!-- end form-hold -->

</div> <!-- end form-edit -->
</body>
</html>
