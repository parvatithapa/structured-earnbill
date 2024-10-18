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

<%@ page import="com.sapienter.jbilling.server.item.AssetWS;"%>
<%@ page import="com.sapienter.jbilling.server.item.ItemTypeBL;"%>
<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO;"%>
<%@ page import="com.sapienter.jbilling.server.item.db.ItemDAS;"%>
<%@ page import="com.sapienter.jbilling.server.item.db.ItemDTO;"%>
<%@ page import="com.sapienter.jbilling.server.metafields.DataType;"%>
<%@ page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;"%>
<%@ page import="com.sapienter.jbilling.server.order.OrderBL;"%>
<%@ page import="com.sapienter.jbilling.server.order.OrderStatusFlag;"%>
<%@ page import="com.sapienter.jbilling.server.order.db.OrderStatusDTO;"%>
<%@ page import="com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;"%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper;" %>
<%@ page import="com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;"%>
<%@ page import="com.sapienter.jbilling.server.usagePool.UsagePoolWS;"%>
<%@ page import="com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;"%>
<%@ page import="com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;"%>
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;"%>
<%@ page import="com.sapienter.jbilling.server.util.Constants;" %>
<%@ page import="com.sapienter.jbilling.server.util.Util;" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils;"%>

<g:if test="${(drawFilter == null || drawFilter) && order?.childOrders}">
    <div class="column-hold">
        <div class="sub-box">
            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="order.filter.label.status"/></content>
                <content tag="label.for">filterStatusId</content>
                <content tag="include.script">true</content>
                <g:select
                        from="${OrderStatusDTO.findAllByEntity(CompanyDTO.get(session['company_id']))}"
                        optionKey="id" optionValue="${{it?.getDescription(session['language_id'])}}"
                        name="filterStatusId"
                        noSelection="${['':'']}"
                        onchange="${remoteFunction(controller: 'order', action: 'show',id: order.id,
                                                    before: 'register(this)',
                                                    onSuccess: 'render(data, current)',
                                                    params: '\'filterStatusId=\'+' + '$(this).val()' )}"
                        value="${filterStatusId}"/>
            </g:applyLayout>
        </div>
    </div>
</g:if>

<g:if test="${(!filterStatusId || order.statusId == filterStatusId) && !order.deleted}">
<div class="column-hold">

    <g:set var="currency" value="${currencies.find{ it.id == order.currencyId}}"/>

    <div class="heading">
        <strong>
        	<g:message code="order.label.details"/>&nbsp;<em>${order?.id}</em>
        	<g:if test="${order.deleted}">
                <span style="color: #ff0000;">(<g:message code="object.deleted.title"/>)</span>
            </g:if>
        </strong>
    </div>

    <!-- Order Details -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable">
                <tr>
                    <td colspan="2">
                        <strong>
                            <g:if test="${user?.contact?.firstName || user?.contact?.lastName}">
                                ${user.contact.firstName}&nbsp;${user.contact.lastName}
                            </g:if>
                            <g:else>
                                ${displayer.getDisplayName(user)}
                            </g:else>
                        </strong><br>
                        <em>${user?.contact?.organizationName}</em>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td><td></td>
                </tr>
                <tr>
                    <td><g:message code="order.label.user.id"/>:</td>
                    <td class="value">
                        <sec:access url="/customer/show">
                            <g:remoteLink controller="customer" action="show" id="${user?.id}" before="register(this);" onSuccess="render(data, next);">
                                ${user?.id}
                            </g:remoteLink>
                        </sec:access>
                        <sec:noAccess url="/customer/show">
                            ${user?.id}
                        </sec:noAccess>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="order.label.user.name" />:</td>
                    <td class="value">${displayer.getDisplayName(user)}</td>
                </tr>
                <g:isRoot>
                	<tr>
                    	<td><g:message code="order.label.company"/></td>
                        <td class="value">${user?.companyName}</td>
                    </tr>
                </g:isRoot>
            </table>

            <table class="dataTable">
                <g:if test="${order?.parentOrderId != null}">
                    <tr>
                        <td><g:message code="order.label.parentOrder"/>:</td>
                        <td class="value">
                            <g:remoteLink action="show" params="['template': 'show', 'singleOrder': 'true']" before="register(this);" onSuccess="render(data, next);" id="${order?.parentOrderId}" method="GET">
                                <span>${order?.parentOrderId}</span>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:if>
                <tr><td><g:message code="order.label.create.date"/>:</td>
                    <td class="value">
                        <g:formatDate date="${order?.createDate}" formatName="date.pretty.format"/>
                    </td>
                </tr>
                <tr><td><g:message code="order.label.active.since"/>:</td>
                    <td class="value">
                        <g:formatDate date="${order?.activeSince}" formatName="date.pretty.format"/>
                    </td>
                </tr>
                <tr><td><g:message code="order.label.active.until"/>:</td>
                    <td class="value">
                        <g:formatDate date="${order?.activeUntil}" formatName="date.pretty.format"/>
                    </td>
                </tr>
            	<tr><td><g:message code="order.label.due.date"/>:</td>
                	<td class="value">
	                    <g:if test="${order != null && order.dueDateValue != null && periodUnit != null}">
	                        ${order?.dueDateValue + ' ' +  periodUnit?.getDescription(session['language_id'])}
	                    </g:if>
                	</td>
            	</tr>
                <tr><td><g:message code="order.label.next.billable"/>:</td>
                    <td class="value">
                        <g:if test="${order?.nextBillableDay}">
                            <g:formatDate date="${order?.nextBillableDay}"  formatName="date.pretty.format"/>
                        </g:if>
                        <g:elseif test="${!(OrderStatusFlag.FINISHED == order?.orderStatusWS?.orderStatusFlag)}">
                            <g:formatDate date="${order?.activeSince ?: order?.createDate}"  formatName="date.pretty.format"/>
                        </g:elseif>
                    </td>
                </tr>

                <tr>
                    <td><g:message code="order.label.period"/>:</td><td class="value">${order.periodStr}</td>
                </tr>
                <tr>
                    <td><g:message code="order.label.total"/>:</td>
                    <td class="value">
                        <g:formatNumber number="${order.totalAsDecimal}" type="currency" currencySymbol="${currency?.symbol}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="order.label.status"/>:</td>
                    <g:if test="${order?.activeSince?.after(TimezoneHelper.currentDateForTimezone(session['company_timezone']))}">
                        <td class="value">${Constants.ORDER_STATUS_PENDING}</td>
                    </g:if>
                    <g:else>
                        <td class="value">${order?.orderStatusWS?.description}</td>
                    </g:else>
                </tr>
                <tr>
                    <td><g:message code="order.label.billing.type"/>:</td>
                    <td class="value">${order?.billingTypeStr}</td>
                </tr>
                <tr>
                    <td><g:message code="order.label.cancellation.minimum.period"/>:</td>
                    <td class="value">${order?.cancellationMinimumPeriod}</td>
                </tr>
                <tr>
                    <td><g:message code="order.label.cancellation.fee.type"/>:</td>
                   	<td class="value">${order?.cancellationFeeType}</td>
               	</tr>
                <g:if test="${order.cancellationFeeType=='FLAT'}">
                	<tr>
                    	<td><g:message code="order.label.cancellation.fee"/>:</td>
                    	<td class="value"><g:formatNumber number="${order.cancellationFee}" type="currency" currencySymbol="${currency?.symbol}"/></td>
                	</tr>
                </g:if>

                <g:else>
                	<tr>
                    	<td><g:message code="order.label.cancellation.fee.percentage"/>:</td>
                    	<g:if test="${(order.cancellationFeePercentage)}">
                    	<td class="value">${order?.cancellationFeePercentage}%</td></g:if>
               		 </tr>
               		 <tr>

                    <td><g:message code="order.label.cancellation.fee.maximum"/>:</td>
                    <g:if test="${order.cancellationMaximumFee}">
                    <td class="value"><g:formatNumber number="${order.cancellationMaximumFee}" type="currency" currencySymbol="${currency?.symbol}"/></td>
                    </g:if>
                </tr>
                </g:else>
                <tr>
                    <td><g:message code="order.label.userCodes"/>:</td>
                    <g:if test="${order.userCode}">
                        <td class="value">
                            <jB:secRemoteLink controller="user" action="show" params="[userCode: order.userCode, partial: true]"
                                          before="register(this);" onSuccess="render(data, next);"
                                          method="GET">${StringEscapeUtils.escapeHtml(order?.userCode)}</jB:secRemoteLink>
                        </td>
                    </g:if>
                </tr>
                    <td><g:message code="order.label.prorating.flag"/>:</td>
                    <td class="value">
	                    <g:if test="${order?.prorateFlag == true}">
	                    	<g:message code="order.label.prorating.flag.true"/>
	                    </g:if>
	                    <g:else>
	                    	<g:message code="order.label.prorating.flag.false"/>
	                    </g:else>
                    </td>
                </tr>

                <tr>
                    <td><g:message code="order.label.auto.renew.order"/>:</td>
                    <td class="value">
                        <g:if test="${order?.autoRenew}">
                            <g:message code="default.boolean.true"/>
                        </g:if>
                        <g:else>
                            <g:message code="default.boolean.false"/>
                        </g:else>
                    </td>
                </tr>

                <g:if test="${order?.isMediated}">
                    <tr>
                        <td><g:message code="order.label.free.call.counter"/>:</td>
                        <td class="value">
                            <g:formatNumber number="${order.getAllFreeCallCount()}" type="number"/>
                        </td>
                    </tr>
                </g:if>

                <g:if test="${order?.isFreeTrial}">
                    <tr>
                        <td><g:message code="order.label.free.trial.status"/>:</td>
                        <td class="value">
                            <g:if test="${order?.isDiscountOrderFinished}">
                                <g:message code="order.label.free.plan.expired"/>
                            </g:if>
                            <g:else>
                                <g:message code="order.label.free.plan.active"/>
                            </g:else>
                        </td>
                    </tr>
                </g:if>

                <g:if test="${order?.metaFields}">
                    <!-- empty spacer row -->
                    <tr>
                        <td colspan="2"><br/></td>
                    </tr>
                    <g:render template="/metaFields/metaFieldsWS" model="[metaFields: order?.metaFields]"/>
                    <sec:ifAllGranted roles="EDI_922">
                        <g:if test="${order?.period==Constants.ORDER_PERIOD_ONCE}">
                            <tr>
                                <td><g:message code="customer.enrollment.edi.files"/></td>
                                <td class="value">
                                    <g:link controller="ediFile" action="showOrderEDIFile"
                                            params="[orderId: order?.id]"><g:message
                                            code="customer.enrollment.edi.files"/></g:link>
                                </td>
                            </tr>
                        </g:if>
                    </sec:ifAllGranted>
                </g:if>
            </table>
        </div>
    </div>

    <div class="heading">
        <strong><g:message code="order.label.notes"/></strong>
    </div>

    <!-- Order Notes -->
    <div class="box">
        <div class="sub-box">
            <g:if test="${order?.notes}">
                <p>${raw(order?.notes.replaceAll('\n', '<br/>'))}</p>
            </g:if>
            <g:else>
                <p><em><g:message code="order.prompt.no.notes"/></em></p>
            </g:else>
        </div>
    </div>

    <div class="heading">
        <strong><g:message code="order.label.lines"/></strong>
    </div>

    <!-- Order Lines -->
    <div class="box">
        <div class="sub-box">
            <g:if test="${order?.orderLines}">
                <g:set var="hasMetaFields" value="${false}" />
                <table class="innerTable" >
                    <thead class="innerHeader">
                         <tr>
                            <th class="first" style="min-width: 75px;"><g:message code="order.label.line.item"/></th>
                            <th><g:message code="order.label.line.descr"/></th>
                            <th><g:message code="order.label.line.qty"/></th>
                            <th><g:message code="order.label.line.price"/></th>
                            <th class="last"><g:message code="order.label.line.total"/></th>
                         </tr>
                    </thead>
                    <tbody>
                         <g:each var="line" in="${order.orderLines}" status="idx">
                         %{--if this is not a tax line item --}%
                             <g:if test="${line.getTypeId().intValue() != Constants.ORDER_LINE_TYPE_TAX_QUOTE}">
                             <tr>
                                <td class="innerContent">
                                    <g:set var="itemDto" value="${new ItemDAS().find(line?.itemId)}"/>
                                    <g:if test="${itemDto?.plans?.size() == 0}">
                                        <sec:access url="/product/show">
                                           <g:remoteLink controller="product" action="show" id="${line?.itemId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                                ${line?.itemId}
                                           </g:remoteLink>
                                        </sec:access>
                                        <sec:noAccess url="/product/show">
                                            ${line?.itemId}
                                        </sec:noAccess>
                                    </g:if>
                                    <g:else>
                                        <sec:access url="/plan/show">
                                            <g:set var="planId" value="${itemDto?.plans?.iterator()?.next()?.id}" />
                                            <g:remoteLink controller="plan" action="show" id="${planId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                                ${planId}
                                            </g:remoteLink>
                                        </sec:access>
                                        <sec:noAccess url="/plan/show">
                                            ${planId}
                                        </sec:noAccess>
                                    </g:else>
                                </td>
                                <td class="innerContent">
                                    ${line.description}
                                </td>
                                <td class="innerContent">
                                    <g:formatNumber number="${line.quantityAsDecimal ?: BigDecimal.ZERO}" formatName="decimal.format"/>
                                </td>
                                <td class="innerContent">
                                    <g:set var="product" value="${ItemDTO.get(line.itemId)}"/>
                                    <g:set var="price" value="${ product?.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
	                        		<g:if test="${line?.isPercentage}" >
                                        <g:formatNumber number="${line.priceAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="%" maxFractionDigits="5"/>
                                    </g:if>
                                    <g:else>
                                        <g:formatNumber number="${line.priceAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="5"/>
                                    </g:else>
                                </td>
                                <td class="innerContent">
                                    <g:formatNumber number="${line.amountAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="4"/>
                                </td>
                             </tr>

                                 <%-- Display the meta fields in the next tr --%>
                                 <g:if test="${line.callIdentifier != null}" >
                                   <tr lineMetaField>
                                       <td colspan="5">
                                           <table style="width: 100%;" class="dataTable narrow" >
                                              <tr style="width: 100%;">
									            <td style="width: 40%;">
									                <g:message code="${product.description}"/> <g:message code="identifier"/>
									            </td>
									            <td style="width: 60%;" class="value">
									                ${line.callIdentifier}
									            </td>
								              </tr>
                                           </table>
                                       </td>
                                   </tr>
                                  </g:if>


                                 %{--Display the taxes linked to this line--}%
                                 <g:each var="taxLine" in="${line.childLines.sort{ it.description } }" status="taxIdx">
                                     <g:if test="${taxLine.deleted == 0 && taxLine.getTypeId().intValue() == Constants.ORDER_LINE_TYPE_TAX_QUOTE}">
                                         <tr>
                                             <td class="innerContent"></td>
                                             <td class="innerContent">${taxLine.description}</td>
                                             <td class="innerContent"></td>
                                             <td class="innerContent"></td>
                                             <td class="innerContent">
                                                 <g:formatNumber number="${taxLine.amountAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="4"/>
                                             </td>
                                         </tr>
                                     </g:if>
                                 </g:each>
                             </g:if>
                         </g:each>
                    </tbody>
                </table>
                <g:if test="${hasMetaFields}" >
                    <div class="row">&nbsp;</div>
                    <div class="btn-box row">
                        <a href="" id="metaFieldBtnSpan" class="submit" onclick="toggleMetaFields();return false;" ><g:message code="order.button.metafields.hide"/></a>
                    </div>
                </g:if>
            </g:if>
            <g:else>
                <em><g:message code="order.prompt.no.lines"/></em>
            </g:else>
            <g:if test="${new BigDecimal(order?.freeUsageQuantity ?: 0).compareTo(BigDecimal.ZERO) > 0}">
				<div class="box-cards box-cards-no-margin">
					<div class="box-cards-title">
						<a class="btn-open" href="#"><span><g:message
							code="order.label.orderLineUsagePools"/></span></a>
					</div>
		        	<div class="box-card-hold">
                    	<table style="width: 100%;" cellpadding="0" cellspacing="0" class="innerTable">
                     		<thead  style="width: 100%;" class="innerHeader">
		                        <tr>
		                        	<th class="first" style="width: 30%;"><g:message code="order.label.line.item"/></th>
		                        	<th style="width: 40%;"><g:message code="order.label.usage.pool.descr"/></th>
		                            <th class="last" style="width: 30%;"><g:message code="order.label.utilized.quantity"/></th>
		                        </tr>
                  		</thead>
                     	<tbody>
                       		<g:each var="line" in="${order?.orderLines}" status="idx">
              			 		<g:each var="orderLineUsagePool" in="${line?.orderLineUsagePools}" status="id">
								<tr>
									<td class="innerContent">
	                                    <g:set var="itemDto" value="${new ItemDAS().find(line?.itemId)}"/>
	                                    <g:if test="${itemDto?.plans?.size() == 0}">
	                                        <sec:access url="/product/show">
	                                           <g:remoteLink controller="product" action="show" id="${line?.itemId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
	                                                ${line?.itemId}
	                                           </g:remoteLink>
	                                        </sec:access>
	                                        <sec:noAccess url="/product/show">
	                                            ${line?.itemId}
	                                        </sec:noAccess>
	                                    </g:if>
	                                    <g:else>
	                                        <sec:access url="/plan/show">
	                                            <g:set var="planId" value="${itemDto?.plans?.iterator()?.next()?.id}" />
	                                            <g:remoteLink controller="plan" action="show" id="${planId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
	                                                ${planId}
	                                            </g:remoteLink>
	                                        </sec:access>
	                                        <sec:noAccess url="/plan/show">
	                                            ${planId}
	                                        </sec:noAccess>
	                                    </g:else>
	                                </td>
									<g:set var="customerUsagePool" value="${CustomerUsagePoolDTO.get(orderLineUsagePool.customerUsagePoolId)}"/>
									 <g:set var="usagePool" value="${UsagePoolDTO.get(customerUsagePool?.usagePoolId)}"/>
                                    <td class="innerContent">
                                         ${usagePool.getDescription(session['language_id'], 'name')}
                                    </td>
                                    <td class="innerContent" style="padding-right: 20px;">
                                    	<g:formatNumber number="${orderLineUsagePool.quantity}" formatName="decimal.format"/>
                                    </td>
								</tr>
							</g:each>
							</g:each>
						</tbody>
					</table>
                    </div>
                </div>
            </g:if>
        </div>
    </div>

    <g:if test="${swapPlanLogs}">
	    <div class="box-cards box-cards-no-margin box-cards-open">
		    <div class="box-cards-title">
		            <a class="btn-open" href="#"><span><g:message code="order.label.swaplog"/></span></a>
		    </div>
			<div class="box-card-hold">
		    	<table style="width: 100%;" cellpadding="0" cellspacing="0" class="innerTable">
		     		<thead  style="width: 100%;" class="innerHeader">
		                <tr>
		                	<th class="first" style="width: 25%;"><g:message code="swap.plan.oldPlanid"/>
		                	<th style="width: 25%;"><g:message code="swap.plan.newPlanid"/>
		                    <th style="width: 25%;"><g:message code="swap.plan.swapDate"/>
		                    <th class="last" style="width: 25%;"><g:message code="swap.plan.oldPlanOverageQuantity"/>
		                </tr>
		      		</thead>
		         	<tbody>
		           		<g:each var="swapplanlog" in="${swapPlanLogs}" status="idx">
							<tr>
								<td class="innerContent">
		                        	<g:remoteLink controller="plan" action="show" id="${swapplanlog?.oldPlanId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                        ${swapplanlog?.oldPlanId}
                                    </g:remoteLink>
		                        </td>

		                        <td class="innerContent">
									<g:remoteLink controller="plan" action="show" id="${swapplanlog?.newPlanID}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                        ${swapplanlog?.newPlanID}
                                    </g:remoteLink>
		                        </td>
								<td class="innerContent">
                                    <g:formatDate formatName="date.format" date="${swapplanlog?.swapDate}"/>
                                </td>

		                        <td class="innerContent">
		                        	${swapplanlog?.oldPlanOverageQuantity}
		                        </td>

							</tr>
						</g:each>
					</tbody>
				</table>
			</div>
		</div>
    </g:if>

    <!-- #11776 - Order Tax Lines -->
    <g:if test="${order?.getTaxQuoteLines() ? order.getTaxQuoteLines()[0].getParentLine() == null : false}">
        <g:set var="hasMetaFields" value="${false}" />
        <div class="heading">
            <strong><g:message code="order.label.tax.lines"/></strong>
        </div>
        <div class="box">
            <div class="sub-box">
                <table class="innerTable" >
                    <thead class="innerHeader">
                    <tr>
                        <th class="first"><g:message code="order.label.line.descr"/></th>
                        <th><g:message code="order.label.line.qty"/></th>
                        <th><g:message code="order.label.line.price"/></th>
                        <th class="last"><g:message code="order.label.line.total"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="line" in="${order.getTaxQuoteLines()}" status="idx">
                        <tr>
                            <td class="innerContent">
                                ${line.description}
                            </td>
                            <td class="innerContent">
                                <g:formatNumber number="${line.quantityAsDecimal ?: BigDecimal.ZERO}" formatName="decimal.format"/>
                            </td>
                            <td class="innerContent">
                                <g:formatNumber number="${line.priceAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="4"/>
                            </td>
                            <td class="innerContent">
                                <g:formatNumber number="${line.amountAsDecimal ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="4"/>
                            </td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
        </div>
    </g:if>

<%-- Order line Tiers linked to order lines --%>
    <g:if test="${order?.hasOrderLineTiers()}">
        <div class="box-cards box-cards-no-margin box-cards-open">
            <div class="box-cards-title">
                <a class="btn-open" href="#"><span><g:message
                        code="order.label.order.line.details"/></span></a>
            </div>

            <div class="box-card-hold">

                <g:each var="line" in="${order.orderLines}" status="idx">
                    <g:if test="${line?.hasOrderLineTiers()}">
                        ${line.description}
                        <table class="innerTable" >
                            <thead class="innerHeader">
                                <tr>
                                    <th class="first"><g:message code="order.label.order.line.tiers.detail.tier.number"/></th>
                                    <th><g:message code="order.label.order.line.tiers.detail.tier.from"/></th>
                                    <th><g:message code="order.label.order.line.tiers.detail.tier.to"/></th>
                                    <th><g:message code="order.label.order.line.tiers.detail.tier.quantity"/></th>
                                    <th><g:message code="order.label.order.line.tiers.detail.tier.price"/></th>
                                    <th class="last"><g:message code="order.label.order.line.tiers.detail.tier.amount"/></th>
                                </tr>
                            </thead>
                            <tbody>
                                <g:each var="tier" in="${line?.orderLineTiers?.sort {it.tierNumber}}">
                                    <tr>
                                        <td class="first">
                                            ${tier.tierNumber}
                                        </td>
                                        <g:if test="${null == tier.tierTo}">
                                            <td class="innerContent">
                                                ${tier.tierFrom ? tier.tierFrom.setScale(2, BigDecimal.ROUND_HALF_UP).toString().concat("+") : BigDecimal.ZERO}
                                            </td>
                                        </g:if>
                                        <g:else>
                                            <td class="innerContent">
                                                <g:formatNumber number="${tier.tierFrom ?: BigDecimal.ZERO}" formatName="decimal.format"/>
                                            </td>
                                        </g:else>
                                        <td class="innerContent">
                                            <g:formatNumber number="${tier.tierTo}" formatName="decimal.format"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${tier.quantity ?: BigDecimal.ZERO}" formatName="decimal.format"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${tier.price ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="2"/>
                                        </td>
                                        <td class="innerContent">
                                            <g:formatNumber number="${tier.amount ?: BigDecimal.ZERO}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="2"/>
                                        </td>
                                    </tr>
                                </g:each>
                            </tbody>
                        </table>
                    </g:if>
                </g:each>

            </div>
        </div>
    </g:if>

<%-- Assets linked to lines --%>
    <g:if test="${order?.hasLinkedAssets()}">
        <div class="box-cards box-cards-no-margin box-cards-open">
        <div class="box-cards-title">
            <a class="btn-open" href="#"><span><g:message
                    code="order.label.assets"/></span></a>
        </div>

        <div class="box-card-hold">

            <g:each var="line" in="${order.orderLines}" status="idx">
                <g:if test="${line?.hasLinkedAssets()}">
                    <g:set var="itemDto" value="${new ItemDAS().find(line?.itemId)}"/>
                    <g:set var="itemType" value="${new ItemTypeBL().findItemTypeWithAssetManagementForItem(line.itemId)}" />
                    ${itemDto.description}
                    <table class="innerTable" >
                        <thead class="innerHeader">
                        <tr>
                            <th class="first" style="min-width: 75px;"><g:message code="asset.detail.id"/></th>
                            <th class="last"><g:if test="${itemType.assetIdentifierLabel?.length() > 0}">
                                ${itemType.assetIdentifierLabel}
                                </g:if>
                                <g:else><g:message code="asset.detail.identifier"/></g:else>
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each var="assetId" in="${line.assetIds}">
                            <g:set var="asset" value="${AssetDTO.get(assetId)}" />
                            <tr>
                                <td class="innerContent">
                                    <sec:access url="/product/showAsset">
                                        <g:remoteLink controller="product" action="showAsset" id="${asset.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                            ${asset.id}
                                        </g:remoteLink>
                                    </sec:access>
                                    <sec:noAccess url="/product/showAsset">
                                        ${asset.id}
                                    </sec:noAccess>
                                </td>
                                <td class="innerContent">
                                    ${asset.identifier}
                                </td>

                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </g:if>
            </g:each>

        </div>
        </div>
    </g:if>

    <br/>

    <g:if test="${futureOrderChanges}">
        <div class="heading">
            <strong><g:message code="order.label.pending.lines"/></strong>
        </div>

        <!-- Pending Order Lines -->
        <div class="box">
            <div class="sub-box">
                <g:if test="${orderChanges}">
                    <table class="innerTable" >
                        <thead class="innerHeader">
                        <tr>
                            <th class="first" style="min-width: 75px;"><g:message code="order.label.line.plan.item"/></th>
                            <th><g:message code="order.label.line.descr"/></th>
                            <th><g:message code="order.label.line.qty"/></th>
                            <th><g:message code="order.label.line.status"/></th>
                            <th class="last"><g:message code="order.label.line.start.date"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each var="line" in="${orderChanges}" status="idx">
                            <g:if test="${line.status.equalsIgnoreCase(Constants.ORDER_STATUS_PENDING) }">
                                <tr>
                                    <td class="innerContent">
                                        <g:set var="itemDto" value="${new ItemDAS().find(line?.itemId)}"/>
                                        <g:if test="${!itemDto?.plans}">
                                            <sec:access url="/product/show">
                                                <g:remoteLink controller="product" action="show" id="${line?.itemId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                                    ${line?.itemId}
                                                </g:remoteLink>
                                            </sec:access>
                                            <sec:noAccess url="/product/show">
                                                ${line?.itemId}
                                            </sec:noAccess>
                                        </g:if>
                                        <g:else>
                                            <sec:access url="/plan/show">
                                                <g:set var="planId" value="${itemDto?.plans?.iterator()?.next()?.id}" />
                                                <g:remoteLink controller="plan" action="show" id="${planId}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                                    ${planId}
                                                </g:remoteLink>
                                            </sec:access>
                                            <sec:noAccess url="/plan/show">
                                                ${planId}
                                            </sec:noAccess>
                                        </g:else>
                                    </td>
                                    <td class="innerContent">
                                        ${line.description}
                                    </td>
                                    <td class="innerContent">
                                        <g:formatNumber number="${line.quantityAsDecimal ?: BigDecimal.ZERO}" formatName="decimal.format"/>
                                    </td>
                                    <td class="innerContent">
                                        ${line.status}
                                    </td>
                                    <td class="innerContent">
                                        <g:formatDate date="${line.startDate}" format="dd-MMM-yyyy"/>
                                    </td>
                                </tr>
                            </g:if>
                        </g:each>
                        </tbody>
                    </table>
                </g:if>
            </div>
        </div>
    </g:if>
    <g:if test="${futureOrderAssetsChanges}">
        <!-- Order Line Assets -->
        <div id="orderLineAssets" class="box-cards box-cards-no-margin box-cards-open" >
            <div class="box-cards-title">
                <a class="btn-open" href="#"><span>
                    <g:message code="order.label.pending.assets"/>
                </span></a>
            </div>
            <div class="box-card-hold">
                <!-- Pending Assets -->
                <div class="sub-box">
                    <g:if test="${orderChanges}">
                        <g:each var="assetMap" in="${assetsMap}" status="idx">
                            <div class="innerContent">
                                ${assetMap.key.toString().split('\\|')[1]}
                            </div>
                            <table class="innerTable" >
                                <thead class="innerHeader">
                                <tr>
                                    <th class="first" style="min-width: 75px;"><g:message code="order.label.pending.asset.id"/></th>
                                    <th><g:message code="order.label.pending.asset.identifier"/></th>
                                    <th><g:message code="order.label.pending.asset.status"/></th>
                                    <th class="last"><g:message code="order.label.pending.asset.start.date"/></th>
                                </tr>
                                </thead>
                                <tbody>
                                <g:each var="assets" in="${assetMap.value}">
                                    <g:set var="effectiveDate" value="${(Date)assets.key}"/>
                                    <g:set var="assetArray" value="${assets.value.toArray(new AssetWS[assets.value.size()])}"/>
                                    <g:each var="asset" in="${assetArray}">
                                        <g:set var="assetDTO" value="${AssetDTO.get(asset.id)}" />
                                        <tr>
                                            <td class="innerContent">
                                                <sec:access url="/product/showAsset">
                                                    <g:remoteLink controller="product" action="showAsset" id="${asset.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                                        ${asset.id}
                                                    </g:remoteLink>
                                                </sec:access>
                                                <sec:noAccess url="/product/showAsset">
                                                    ${asset.id}
                                                </sec:noAccess>
                                            </td>
                                            <td class="innerContent">
                                                ${asset.identifier}
                                            </td>
                                            <td class="innerContent">
                                                ${assetDTO.assetStatus?.description}
                                            </td>
                                            <td class="innerContent">
                                                <g:formatDate date="${effectiveDate}" format="dd-MMM-yyyy"/>
                                            </td>
                                        </tr>
                                    </g:each>
                                </g:each>
                                </tbody>
                            </table>
                        </g:each>
                    </g:if>
                </div>
            </div>
        </div>
    </g:if>


    <br/>
    <!-- Invoices Generated -->
    <g:if test="${order?.generatedInvoices}">
        <div class="heading">
            <strong><g:message code="order.label.invoices.generated"/></strong>
        </div>

        <div class="box">
            <div class="sub-box">
                <table class="innerTable" >
                    <thead class="innerHeader">
                         <tr>
                            <th class="first"><g:message code="order.invoices.id"/></th>
                            <th><g:message code="order.invoices.date"/></th>
                            <th class="last"><g:message code="order.invoices.total"/></th>
                         </tr>
                    </thead>
                    <tbody>
                         <g:each var="invoice" in="${order.generatedInvoices}" status="idx">
                             <g:set var="currency" value="${currencies.find{ it.id == invoice.currencyId}}"/>

                             <tr>
                                <td class="innerContent">
                                    <sec:access url="/invoice/show">
                                        <g:remoteLink controller="invoice" action="show" id="${invoice.id}" before="register(this);" onSuccess="render(data, next);">
                                            ${invoice.id}
                                        </g:remoteLink>
                                    </sec:access>
                                    <sec:noAccess url="/invoice/show">
                                        ${invoice.id}
                                    </sec:noAccess>
                                </td>
                                <td class="innerContent">
                                    <g:formatDate format="dd-MMM-yyyy" date="${invoice?.createDatetime}"/>
                                </td>
                                <td class="innerContent">
                                    <g:formatNumber number="${invoice.totalAsDecimal}" type="currency" currencySymbol="${currency?.symbol}"/>
                                </td>
                             </tr>
                         </g:each>
                    </tbody>
                </table>
            </div>
        </div>
    </g:if>

    <div class="btn-box">
    <g:if test="${!order.deleted}">
        <div class="row">
            <sec:ifAllGranted roles="ORDER_23">
                <g:if test="${com.sapienter.jbilling.server.order.OrderStatusFlag.INVOICE == order?.orderStatusWS?.orderStatusFlag}">
                    <a href="${createLink (action: 'generateInvoice', params: [id: order?.id])}" class="submit order">
                        <span><g:message code="order.button.generate"/></span>
                    </a>
                    <a href="${createLink (action: 'applyToInvoice', params: [id: order?.id, userId: user?.id])}" class="submit order">
                        <span><g:message code="order.button.apply.invoice"/></span>
                    </a>
                </g:if>
            </sec:ifAllGranted>

            <sec:ifAllGranted roles="ORDER_21">
	            <g:set var="lineTypeIds" value="${order.orderLines.collect {it?.typeId}.unique()}"/>
	            <g:if test="${lineTypeIds}">
		            <g:if test="${!(lineTypeIds.get(0)==Constants.ORDER_LINE_TYPE_DISCOUNT) && isCurrentCompanyOwning}">
		                <a href="${createLink (controller: 'orderBuilder', action: 'edit', params: [id: order?.id])}" class="submit edit">
		                    <span><g:message code="order.button.edit"/></span>
		                </a>
		             </g:if>
	             </g:if>
                <g:elseif test="${isCurrentCompanyOwning}">
                    <a href="${createLink (controller: 'orderBuilder', action: 'edit', params: [id: order?.id])}" class="submit edit">
                        <span><g:message code="order.button.edit"/></span>
                    </a>
                </g:elseif>
            </sec:ifAllGranted>
        </div>
        <div class="row">
            <sec:ifAllGranted roles="ORDER_22">
                <g:set var="validator" value="${new OrderHierarchyValidator()}"/>
                <g:set var="hierarchy" value="${validator.buildHierarchy(new OrderBL().getDTO(order))}"/>
                <g:set var="dependencyCount" value="${validator.subOrderCount(order.id)}"/>
                <g:if test="${isCurrentCompanyOwning && !order.generatedInvoices}">
                    <a onclick="checkDependency(${order?.id}, ${dependencyCount});" class="submit delete">
                        <span><g:message code="order.button.delete"/></span>
                    </a>
                </g:if>
            </sec:ifAllGranted>

                <g:link class="submit show" controller="mediation" action="order" id="${order.id}" params="${params + ['first': 'true']}">
                    <span><g:message code="button.view.events" /></span>
                </g:link>
                <g:link      class = "submit show"
                        controller = "provisioning"
                            action = "showCommands"
                            params = "[          type: 'ORDER',
                                       typeIdentifier: order.id]">
                    <span><g:message code="button.view.commands" /></span>
                </g:link>
           </div>
        </g:if>
        </div>
    </div>
</g:if>

<g:if test="${!singleOrder}">
<g:if test="${order?.childOrders}">
    <g:each var="childOrder" in="${order?.childOrders}">
        <g:render template="show" model="[order: childOrder, user: user, currencies: currencies, drawFilter: false, filterStatusId: filterStatusId]"/>
    </g:each>
</g:if>
</g:if>
<g:render template="/confirm"
     model="[message: 'order.prompt.are.you.sure',
             controller: 'order',
             action: 'deleteOrder',
             id: order.id,
            ]"/>

<script language="JavaScript" >
    <g:if test="${params.template}">
        <%-- register for the slide events if it is loaded as a template --%>
        registerSlideEvents();
    </g:if>

    <%-- toggle the text shown on the 'show order line meta fields' button--%>
    function toggleMetaFields() {
        $('[lineMetaField]').toggle();
        if($('#metaFieldBtnSpan').text() == '<g:message code="order.button.metafields.hide"/>') {
            $('#metaFieldBtnSpan').text('<g:message code="order.button.metafields.show"/>');
        } else {
            $('#metaFieldBtnSpan').text('<g:message code="order.button.metafields.hide"/>');
        }

    }

    $(function() {
        setTimeout(function() {
            $('#confirm-dialog-${name}.ui-dialog-content').remove();
            $('#dependencyDialog').dialog({
                autoOpen: false,
                height: 200,
                width: 375,
                modal: true,
                buttons: {
                    '<g:message code="prompt.yes"/>': function() {
                        <%out.println(onYes)%>;
                        showConfirm('deleteOrder-' + $(this).data("orderId"));
                        $(this).dialog('close');
                    },
                    '<g:message code="prompt.no"/>': function() {
                        <%out.println(onNo)%>;
                        $(this).dialog('close');
                    }
                }
            });
        }, 100);
    });

    function checkDependency(orderId, dependencyCount) {
        if(dependencyCount == 0) {
            showConfirm('deleteOrder-' + orderId);
        } else {
            $('#confirm-dialog-${name}.ui-dialog-content').remove();
            $('#dependencyDialog').data("orderId",orderId).dialog('open', orderId);
        }
    }
</script>
<table id="dependencyDialog" style="margin: 3px 0 0 10px">
    <tbody><tr>
        <td class="col2" style="padding-left: 7px">

            <p id="confirm-dialog-${name }-msg">
                <g:message code="order.prompt.dependency.label"/>
            </p>

        </td>
    </tr></tbody>
</table>