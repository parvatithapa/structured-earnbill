<%@ page import="com.sapienter.jbilling.server.pricing.PriceModelBL" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
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

<%--
  Shows a list of item pricing strategies and attributes.

  @author Brian Cowdery
  @since 28-Feb-2011
--%>
<g:if test="${product}">
    <div class="heading">
        <strong><g:message code="customer.inspect.default.price.title"/></strong>
    </div>
    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
                <tbody>
                    <tr>
                        <td><g:message code="product.internal.number"/></td>
                        <td class="value" colspan="3">
                            <jB:secLink controller="product" action="show" id="${product.id}">
                                ${product.internalNumber}
                            </jB:secLink>
                        </td>
                    </tr>

                    <tr>
                        <td><g:message code="product.description"/></td>
                        <td class="value" colspan="3">
                            ${product.getDescription(session['language_id'])}
                        </td>
                    </tr>

                    <!-- price model -->
                    <tr><td colspan="4">&nbsp;</td></tr>
                    <g:render template="/plan/priceModel" model="[model: product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']),session['company_id'] as Integer)]"/>
                </tbody>
            </table>
        </div>
    </div>
</g:if>

<div class="heading">
    <g:if test="${product}">
        <strong>
            <g:message code="customer.inspect.customer.prices.title"/>
        </strong>
    </g:if>
    <g:else>
        <strong><g:message code="customer.inspect.prices.all.title"/></strong>
    </g:else>
</div>

<div class="box">
    <div class="sub-box">
        <g:if test="${prices}">
    
            <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
                <tbody>
    
                <g:each var="price" status="index" in="${prices?.sort{ it.precedence }}">
                    <tr>
                        <td><g:message code="product.internal.number"/></td>
                        <td class="value" colspan="2">
                            <jB:secLink url="/product/show" controller="product" action="list" id="${price.item.id}">
                                ${price.item.internalNumber}
                            </jB:secLink>
                        </td>
                        <td class="right">
                            <g:if test="${!price.plan}">
                                <!-- edit customer-specific price -->
                                <g:link class="plus-icon" action="editCustomerPrice" id="${price.id}" params="[userId: user?.id ?: userId, itemId: price.item.id]">&#xe010;</g:link>
                            </g:if>
                        </td>
                        <td class="right">
                            <g:if test="${!price.plan}">
                                <!-- delete customer-specific price -->
                                &nbsp;<g:remoteLink class="plus-icon" action="deleteCustomerPrice" id="${price.id}" params="[userId: user?.id ?: userId, itemId: price.item.id]" update="prices-column">&#xe000;</g:remoteLink>
                            </g:if>
                        </td>
                    </tr>
    
                    <tr>
                        <td><g:message code="product.description"/></td>
                        <td class="value" colspan="3">
                            ${price.item.getDescription(session['language_id'])}
                        </td>
                    </tr>
    
                    <tr>
                        <td><g:message code="plan.item.precedence"/></td>
                        <td class="value" colspan="3">
                            ${price.precedence}
                        </td>
                    </tr>

                    <tr>
                        <td><g:message code="plan.item.price.expiry.date"/></td>
                        <td class="value" colspan="3">
                            ${priceExpiryMap[price?.id]? formatDate(date: priceExpiryMap[price?.id], formatName: 'datepicker.format') : ""}
                        </td>
                    </tr>

                    <!-- price model -->
                    <tr><td colspan="4">&nbsp;</td></tr>
                    <g:render template="/plan/priceModel" model="[model: PriceModelBL.getPriceForDate(price.models, TimezoneHelper.currentDateForTimezone(session['company_timezone']))]"/>
    
                    <!-- separator line -->
                    <g:if test="${index < prices.size()-1}">
                        <tr><td colspan="4"><hr/></td></tr>
                    </g:if>
                </g:each>
    
                </tbody>
            </table>
    
        </g:if>
        <g:else>
            <em><g:message code="customer.inspect.no.prices"/></em>
        </g:else>
    </div>
</div>

<div class="btn-box">
    <g:if test="${product}">
        <g:link class="submit add" action="editCustomerPrice" params="[userId: user?.id ?: userId, itemId: product.id]">
            <span><g:message code="button.add.customer.price"/></span>
        </g:link>

        <g:remoteLink action="allProductPrices" update="prices-column" params="[userId: user?.id ?: userId]" class="submit show">
            <span><g:message code="button.show.all"/></span>
        </g:remoteLink>
    </g:if>
</div>
