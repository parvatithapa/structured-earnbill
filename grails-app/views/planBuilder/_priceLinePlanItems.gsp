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
<%@ page import="org.apache.commons.lang.StringEscapeUtils;"%>
<%@ page import="com.sapienter.jbilling.server.item.PlanItemBL"%>
<%@ page import="com.sapienter.jbilling.server.item.db.ItemDTO; com.sapienter.jbilling.server.item.db.PlanItemDTO; com.sapienter.jbilling.server.pricing.PriceModelBL; com.sapienter.jbilling.server.item.PlanItemWS" %>

<g:if test="${ItemDTO.get(planItem.itemId)?.plans && level < 5}">
    <g:each var="plan" in="${ItemDTO.get(planItem.itemId).plans}">
        <ul>
            <g:each var="childPlanItemDto" in="${plan.planItems}">
                <g:set var="childPlanItem" value="${PlanItemBL.getWS(childPlanItemDto)}"/>
                <g:set var="model" value="${PriceModelBL.getWsPriceForDate(childPlanItem.models, startDate)}"/>
                <g:set var="currency" value="${currencies.find{ it.id == model.currencyId}}"/>

                <li>
                    <span>
                        ${childPlanItem.precedence} &nbsp; ${childPlanItemDto.item.description}
                    </span>
                    <span class="rate">
                        ${StringEscapeUtils.unescapeHtml(currency?.symbol)}<g:formatNumber number="${model.getRateAsDecimal()}" formatName= "price.format"/>
                    </span>
                    <span class="strategy">
                        <g:message code="price.strategy.${model.type}"/>
                    </span>
                    <div style="clear: both;"></div>
                    <g:if test="${childPlanItemDto.item.plans}">
                        <g:render template="priceLinePlanItems" model="[ planItem: childPlanItem, startDate: startDate, level: level + 1 ]" />
                    </g:if>
                </li>
            </g:each>
        </ul>
    </g:each>
</g:if>
