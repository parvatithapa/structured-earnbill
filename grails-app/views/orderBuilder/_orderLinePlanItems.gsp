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
<%@ page import="com.sapienter.jbilling.server.item.db.PlanItemBundleDTO; org.apache.commons.lang.WordUtils" %>

<g:if test="${item.plans}">
    <g:each in="${item.plans}" var="plan">
        <g:each in="${plan.planItems}" var="planItem">
            <g:render template="planItemDetails" model="[planItem: planItem]"/>
            <g:if test="${planItem.item.plans}">
                <g:render template="orderLinePlanItems" model="[item: planItem.item, level: level + 1]"/>
            </g:if>
        </g:each>
    </g:each>
</g:if>