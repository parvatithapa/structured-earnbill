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
<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO; com.sapienter.jbilling.server.item.db.PlanItemBundleDTO; org.apache.commons.lang.WordUtils" %>

<g:if test="${planItem.bundle?.quantity}">

    <g:set var="planItemPriceModel" value="${planItem.getPrice(pricingDate)}"/>

    <li class="bundled">
        <span class="description">
            ${planItem.item.description}
        </span>
        <span class="included-qty">
            + <g:formatNumber number="${planItem.bundle?.quantity}"/>
            <g:if test="${planItem.bundle?.period}">
                ${WordUtils.capitalize(planItem.bundle?.period?.getDescription(session['language_id'])?.toLowerCase())}
            </g:if>
            <g:if test="${planItem.bundle?.targetCustomer != PlanItemBundleDTO.Customer.SELF}">
                <g:message code="bundle.for.target.customer.${planItem.bundle?.targetCustomer}"/>
            </g:if>
        </span>

        <div class="clear">&nbsp;</div>
    </li>
    <li class="bundled-price">
        <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <tbody>
            <g:render template="/plan/priceModel" model="[model: planItemPriceModel]"/>
            </tbody>
        </table>
    </li>
    <%--
    <g:if test="${line.hasAssets() && planItem?.item?.assetManagementEnabled == 1}">
        <div>
            <g:each var="assetId" in="${line.assetIds}" status="assetIdx">
                <g:applyLayout name="form/checkbox">
                    <content tag="label">${AssetDTO.get(assetId).identifier}</content>
                    <content tag="label.for">line-${index}.asset.${assetIdx}</content>
                    <g:if test="${assetIdx == 0}">
                        <content tag="group.label"><g:message code="order.label.assets"/></content>
                    </g:if>
                    <g:checkBox name="line-${index}.asset.${assetIdx}.status" line="${index}" class="cb check" value="${assetId}" checked="true"/>
                    <g:hiddenField name="line-${index}.asset.${assetIdx}.id" value="${assetId}" />
                </g:applyLayout>

            </g:each>
        </div>
        <p>&nbsp;</p>
    </g:if>
    --%>
    <div class="btn-box">
        <g:if test="${planItem?.item?.assetManagementEnabled == 1}">
            &nbsp;
            <g:remoteLink class="submit add" action="edit" id="${planItem?.item?.id}" params="[index: index, plan: params.plan, _eventId: 'initAssets' ]" update="assets-box-add" method="GET">
                <span><g:message code="button.add.assets"/></span>
            </g:remoteLink>

        </g:if>
    </div>
</g:if>
