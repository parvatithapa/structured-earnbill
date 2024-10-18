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

<%@ page import="com.sapienter.jbilling.server.metafields.DataType; org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.util.Util"%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%--
  Plan details template.

  @author Brian Cowdery
  @since  1-Feb-2010
--%>

<div class="column-hold">
    <div class="heading">
	    <strong>${plan.item.internalNumber}</strong>
	</div>

    <!-- plan details -->
	<div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="plan.id"/></td>
                        <td class="value">${plan.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.internal.number"/></td>
                        <td class="value">${plan.item.internalNumber}</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.description"/></td>
                        <td class="value">${plan.item.getDescription(session['language_id'])}</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.editable"/></td>
                        <td class="value">${plan.editable > 0}</td>
                    </tr>
                    <tr>
                        <td><g:message code="order.label.period"/></td>
                        <td class="value">${plan.period.getDescription(session['language_id'])}</td>
                    </tr>
                    <g:if test="${plan.item.defaultPrices}">
                        <g:set var="price" value="${plan.item.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'] as Integer)}"/>
                        <tr>
                            <td>${price?.currency?.code}</td>
                            <td class="value">
                               <g:formatPriceForDisplay price="${price}" />
                            </td>
                        </tr>
                    </g:if>
                    <tr>
                    	<td><g:message code="product.detail.availability.start.date"/>:</td>
	                    <td class="value">
	                        <g:formatDate date="${plan.item.activeSince}" formatName="date.pretty.format"/>
	                    </td>
	                </tr>
	                <tr>
	                	<td><g:message code="product.detail.availability.end.date"/>:</td>
	                    <td class="value">
	                        <g:formatDate date="${plan.item.activeUntil}" formatName="date.pretty.format"/>
	                    </td>
	                </tr>
                    <tr>
                         <td><g:message code="filters.plan.free.trial"/></td>
                        <td class="value">${plan.freeTrial}</td>
                    </tr>

                <g:if test="${plan?.metaFields}">
                    <!-- empty spacer row -->
                    <tr>
                        <td colspan="2"><br/></td>
                    </tr>
                    <g:render template="/metaFields/metaFields" model="[metaFields: plan?.metaFields]"/>
                </g:if>
                </tbody>
            </table>
    
            <p class="description">
                ${plan.description}
            </p>
            
            <!-- product categories cloud -->
            <div class="box-cards box-cards-open">
                <div class="box-cards-title">
                    <span><g:message code="product.detail.categories.title"/></span>
                </div>
                <div class="box-card-hold">
                    <div class="content">
                        <ul class="cloud">
                            <g:each var="category" in="${plan.item.itemTypes.sort{ it.description }}">
                                <li>
                                    ${category.description}
                                </li>
                            </g:each>
                        </ul>
                    </div>
                </div>
            </div>
            
            <!-- product orderLineMetaFields -->
            <div class="box-cards box-cards-open">
                <div class="box-cards-title">
                    <span><g:message code="product.orderLineMetafields.description"/></span>
                </div>
                <div class="box-card-hold">
                    <div class="content">
                        <table class="dataTable" width="100%">
                            <tbody>
                            <g:each var="metaField" in="${plan.item.orderLineMetaFields.sort{ it.displayOrder }}">
                                <tr>
                                    <td><g:message code="metaField.label.name"/></td>
                                    <td class="value">${metaField.name}</td>
                                    <td nowrap="nowrap"><g:message code="metaField.label.dataType"/></td>
                                    <td class="value">${metaField.dataType}</td>
                                    <td><g:message code="metaField.label.mandatory"/></td>
                                    <td class="value">
                                        <g:if test="${metaField.mandatory}">
                                            <g:message code="prompt.yes"/>
                                        </g:if>
                                        <g:else>
                                            <g:message code="prompt.no"/>
                                        </g:else>
                                    </td>
                                </tr>
                            </g:each>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- plan prices -->
    <g:if test="${plan.planItems}">
    <div class="heading">
        <strong><g:message code="builder.products.title"/></strong>
    </div>
    <div class="box">
        <div class="sub-box"><table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <tbody>

            <g:each var="planItem" status="index" in="${plan.planItems.sort{ it.precedence }}">

                <tr>
                    <td><g:message code="product.internal.number"/></td>
                    <td class="value" colspan="3">
                        <sec:access url="/product/show">
                            <g:remoteLink controller="product" action="show" id="${planItem.item.id}" params="[template: 'show']" before="register(this);" onSuccess="render(data, next);">
                                ${StringEscapeUtils.escapeHtml(planItem?.item?.internalNumber)}
                            </g:remoteLink>
                        </sec:access>
                        <sec:noAccess url="/product/show">
                            ${planItem.item.internalNumber}
                        </sec:noAccess>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="product.description"/></td>
                    <td class="value" colspan="3">
                        ${planItem.item.getDescription(session['language_id'])}
                    </td>
                </tr>

                <tr>
                    <td><g:message code="plan.item.precedence"/></td>
                    <td class="value">${planItem.precedence}</td>
                </tr>

                <g:if test="${planItem.bundle && planItem.bundle.quantity?.compareTo(BigDecimal.ZERO) > 0}">
                    <tr>
                        <td><g:message code="plan.item.bundled.quantity"/></td>
                        <td class="value"><g:formatNumber number="${planItem.bundle.quantity}"/></td>

                        <td><g:message code="plan.bundle.label.add.if.exists"/></td>
                        <td class="value"><g:message code="plan.bundle.add.if.exists.${planItem.bundle.addIfExists}"/></td>
                    </tr>

                    <tr>
                        <td><g:message code="plan.bundle.period"/></td>
                        <td class="value">${planItem.bundle.period.getDescription(session['language_id'])}</td>

                        <td><g:message code="plan.bundle.target.customer"/></td>
                        <td class="value"><g:message code="bundle.target.customer.${planItem.bundle.targetCustomer}"/></td>
                    </tr>

                </g:if>

                <!-- price model -->
                <tr><td colspan="4">&nbsp;</td></tr>
                <g:render template="/plan/priceModel" model="[model: planItem.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']))]"/>


                <!-- separator line -->
                <g:if test="${index < plan.planItems.size()-1}">
                    <tr><td colspan="4"><hr/></td></tr>
                </g:if>
            </g:each>

            </tbody>
        </table>
    </div>
    </div>
    </g:if>
    
     <g:if test="${plan.usagePools}">
    <div class="heading">
        <strong><g:message code="builder.usage.pools.title"/></strong>
    </div>
    <div class="box">
        <div class="sub-box"><table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <tbody>
    			<tr>
                    <td><g:message code="plan.label.usagePool.name"/></td>
                    <td><g:message code="plan.label.usagePool.quantity"/></td>
                    <td><g:message code="plan.label.usagePool.billing.cycle"/></td>
                </tr>
            	<g:each var="usagePools" in="${plan.usagePools}">
	                <tr>
	                  <td style="color:#858585;">${usagePools.getDescription(session['language_id'], 'name')}</td>
	                  <td style="color:#858585;"><g:formatNumber number="${usagePools.getQuantity()}" formatName="money.format"/></td>
	                  <td style="color:#858585;">${usagePools.getCyclePeriodValue()} ${usagePools.getCyclePeriodUnit()}</td>
	                </tr>
				 </g:each>
            </tbody>
        </table>
    </div>
    </div>
    </g:if>

    <div class="btn-box">
        <sec:ifAllGranted roles="PLAN_61">
            <g:link controller="planBuilder" action="edit" id="${plan.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
        </sec:ifAllGranted>

        <sec:ifAllGranted roles="PLAN_62">
            <g:if test="${canBeDeleted}">
            <a onclick="showConfirm('delete-${plan.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </g:if>
        </sec:ifAllGranted>
    </div>

    <g:render template="/confirm"
              model="['message': 'plan.delete.confirm',
                      'controller': 'plan',
                      'action': 'delete',
                      'id': plan.id,
                     ]"/>
</div>

