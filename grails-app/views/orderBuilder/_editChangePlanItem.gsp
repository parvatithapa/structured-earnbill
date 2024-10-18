<%@page import="com.sapienter.jbilling.server.util.db.CurrencyDTO"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils;"%>
<%@page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy"%>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.pricing.PriceModelBL; com.sapienter.jbilling.server.item.db.AssetDTO; com.sapienter.jbilling.server.item.db.ItemDTO" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
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
  Shows the one child order change for selected parent order change
--%>
<g:set var="changeId" value="${changeObj?.id}"/>
<g:set var="itemId" value="${planItem?.itemId}"/>
<g:set var="planItemPrice" value="${PriceModelBL.getWsPriceForDate(planItem.models,pricingDate)}"/>
<g:set var="planItemProduct" value="${ItemDTO.get(itemId)}"/>
<g:set var="quantityNumberFormat" value="${planItemProduct?.hasDecimals ? 'money.format' : 'default.number.format'}"/>
<g:set var="showZeroQuantityBundle" value="${PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_SHOW_NO_IMPACT_PLAN_ITEMS) > 0}" />
<g:set var="currencySymbol" value="${CurrencyDTO.get(planItemPrice?.currencyId)?.symbol}" />
<g:if test="${planItem?.bundle?.quantity as BigDecimal || showZeroQuantityBundle || planItemProduct?.assetManagementEnabled || planItemProduct?.orderLineMetaFields}">
    <li id="edit_change_header_${changeId}_${itemId}" class="line active" onclick="showEditChangeDetails(this);">
        <span class="description">
            ${changePlanItem?.description ?: planItem?.item?.getDescription(session['language_id'])}
        </span>
        
        <span class="sub-total">
            <g:set var="changePlanItemSubTotal" value="${( planItemPrice?.rate as BigDecimal ?: BigDecimal.ZERO ).multiply(planItem?.bundle?.quantity as BigDecimal ?: BigDecimal.ZERO)}"/>
            <g:set var="subTotal"
                   value="${formatNumber(number: changePlanItemSubTotal, type: 'currency',
                           currencySymbol: StringEscapeUtils.unescapeHtml(currencySymbol), formatName: 'price.format')}"/>
            <g:message code="order.review.line.total" args="[subTotal]"/>
        </span>
        <span class="qty-price">
            <g:set var="quantity" value="${formatNumber(number: planItem?.bundle?.quantity as BigDecimal ?: BigDecimal.ZERO, formatName: quantityNumberFormat)}"/>
            <g:set var="price" value="${formatNumber(number: planItemPrice?.rate as BigDecimal ?: BigDecimal.ZERO, type: 'currency', currencySymbol: StringEscapeUtils.unescapeHtml(currencySymbol), formatName: 'price.format')}"/>
            <g:message code="order.review.quantity.by.price" args="[quantity, price]"/>
        </span>
        <div style="clear: both;"></div>
    </li>

    <li id="edit_change_details_${changeId}_${itemId}" class="editor">
    <div class="box">
        <div class="form-columns">
            <g:hiddenField name="change-${changeId}.itemIds" value="${itemId}"/>
            <g:hiddenField name="change-${changeId}-${itemId}.changePlanItemId" value="${changePlanItem?.id}"/>
            <g:hiddenField name="change-${changeId}-${itemId}.optLock" value="${changePlanItem?.optlock}"/>
            <g:applyLayout name="form/text">
                <content tag="label"><g:message code="orderChange.label.quantity"/></content>
                <content tag="label.for">change-${changeId}-${itemId}.quantityAsDecimal</content>
                ${formatNumber(number: planItem?.bundle?.quantity as BigDecimal, formatName: quantityNumberFormat)}
            </g:applyLayout>

            <sec:ifAllGranted roles="ORDER_26">
                <g:applyLayout name="form/text">
               		<g:set var="price" value="${planItemProduct.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
	                <g:if test="${price.type==PriceModelStrategy.LINE_PERCENTAGE}" >
                        <content tag="label"><g:message code="order.label.line.price.percentage"/></content>
                    </g:if>
                    <g:else>
                        <content tag="label"><g:message code="order.label.line.price"/></content>
                    </g:else>
                    <content tag="label.for">change-${changeId}-${itemId}.priceAsDecimal</content>
                    ${formatNumber(number: planItemPrice.rate ?: BigDecimal.ZERO, formatName: 'price.format')}
                </g:applyLayout>
            </sec:ifAllGranted>

            <sec:ifAllGranted roles="ORDER_27">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="orderChange.label.description"/></content>
                    <content tag="label.for">change-${changeId}-${itemId}.description</content>
                    <g:textField name="change-${changeId}-${itemId}.description" class="field description"
                                 value="${changePlanItem?.description ?: planItemProduct?.getDescription(session['language_id'])}"
                                 disabled="${changePlanItem == null || !changePlanItem.description}"/>
                </g:applyLayout>
            </sec:ifAllGranted>

            <sec:ifAnyGranted roles="ORDER_26, ORDER_27">
                <g:applyLayout name="form/checkbox">
                    <content tag="label">
                        <sec:ifNotGranted roles="ORDER_26">
                            <g:message code="order.label.line.use.item.description"/>
                        </sec:ifNotGranted>

                        <sec:ifNotGranted roles="ORDER_27">
                            <g:message code="order.label.line.use.item.price"/>
                        </sec:ifNotGranted>

                        <sec:ifAllGranted roles="ORDER_26, ORDER_27">
                            <g:message code="order.label.line.use.item"/>
                        </sec:ifAllGranted>
                    </content>
                    <content tag="label.for">change-${changeId}-${itemId}.useItem</content>
                    <g:checkBox name="change-${changeId}-${itemId}.useItem" changeId="${changeId}-${itemId}"
                                class="cb check" value="${changePlanItem == null || changePlanItem.description == planItemProduct?.getDescription(session['language_id']) }" />
                    <script type="text/javascript">
                        $('#change-${changeId}-${itemId}\\.useItem').change(
                                function () {
                                    var changeId = $(this).attr('changeId');
                                    if ($(this).is(':checked')) {
                                        $('#change-' + changeId + '\\.priceAsDecimal').prop('disabled', 'true');
                                        $('#change-' + changeId + '\\.description').prop('disabled', 'true');
                                    } else {
                                        $('#change-' + changeId + '\\.priceAsDecimal').prop('disabled', '');
                                        $('#change-' + changeId + '\\.description').prop('disabled', '');
                                    }
                                }
                        ).change();
                    </script>
                </g:applyLayout>
            </sec:ifAnyGranted>

            <g:if test="${changePlanItem?.assetIds}">
                <g:each var="assetId" in="${changePlanItem.assetIds}" status="assetIdx">
                    <g:applyLayout name="form/checkbox">
                        <content tag="label">${AssetDTO.get(assetId).identifier}</content>
                        <content tag="label.for">change-${changeId}-${itemId}.asset.${assetIdx}</content>
                        <g:if test="${assetIdx == 0}">
                            <content tag="group.label"><g:message code="order.label.assets"/></content>
                        </g:if>
                        <g:checkBox name="change-${changeId}-${itemId}.asset.${assetIdx}.status"
                                    line="${changeId}" class="cb check" value="${assetId}" checked="true"/>
                        <g:hiddenField name="change-${changeId}-${itemId}.asset.${assetIdx}.id" value="${assetId}"/>
                    </g:applyLayout>
                </g:each>
            </g:if>
            <g:if test="${planItemProduct?.assetManagementEnabled == 1}">
                <div class="btn-box">
                    <g:remoteLink class="submit add" action="edit" id="${changeId}"
                                  params="[_eventId: 'initUpdateAssets', itemId: itemId]" update="assets-box-update" method="GET">
                        <span><g:message code="button.add.assets"/></span>
                    </g:remoteLink>
                </div>
            </g:if>
            <g:if test="${planItemProduct?.orderLineMetaFields}" ><div class="row">&nbsp;</div>
                <g:render template="/metaFields/editMetaFields" model="[availableFields: planItemProduct.orderLineMetaFields, fieldValues: changePlanItem?.metaFields]"/>
            </g:if>
        </div>
    </div>
</li>
</g:if>