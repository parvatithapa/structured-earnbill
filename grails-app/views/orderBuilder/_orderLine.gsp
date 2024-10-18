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

<%@page import="com.sapienter.jbilling.common.Constants;"%>
<%@page import="com.sapienter.jbilling.server.item.db.AssetDTO;"%>
<%@page import="com.sapienter.jbilling.server.item.db.ItemDTO;"%>
<%@page import="com.sapienter.jbilling.server.item.db.PlanDTO;"%>
<%@page import="com.sapienter.jbilling.server.item.db.PlanItemBundleDTO;"%>
<%@page import="com.sapienter.jbilling.server.pricing.PriceModelBL;"%>
<%@page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;"%>
<%@page import="com.sapienter.jbilling.server.process.db.ProratingType;"%>
<%@page import="com.sapienter.jbilling.server.timezone.TimezoneHelper;" %>
<%@page import="com.sapienter.jbilling.server.user.db.UserDTO;"%>
<%@page import="com.sapienter.jbilling.server.util.Constants;"%>
<%@page import="com.sapienter.jbilling.server.util.PreferenceBL;"%>
<%@page import="org.apache.commons.lang.WordUtils;"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils;"%>

<%--
  Renders an OrderLineWS as an editable row for the order builder preview pane.

  @author Brian Cowdery
  @since 24-Jan-2011
--%>

<g:set var="product" value="${ItemDTO.get(line.itemId)}"/>
<g:set var="quantityNumberFormat" value="${product?.hasDecimals ? 'money.format' : 'default.number.format'}"/>
<g:set var="editable" value="${line.id > 0 && Constants.ORDER_LINE_TYPE_TAX_QUOTE != line.typeId}"/>
<g:set var="taxLine" value="${Constants.ORDER_LINE_TYPE_TAX_QUOTE == line.typeId}"/>
<g:set var="lineId" value="${line.id}" />
<g:set var="lineDependencies" value="${productDependencies["line_" + lineId]}" />
<g:set var="mandatoryNotMet" value="${lineDependencies?.any{it.type == 'mandatory' && !it.met}}"/>
<g:set var="optionalNotMet" value="${lineDependencies?.any{it.type == 'optional' && !it.met}}"/>
<g:set var="isNew" value="${line.id < 0 ? true : persistedOrderOrderLinesMap[line.orderId]?.every {ln -> ln.id != line.id } }" />
<g:set var="pricingDate" value="${order.activeSince}" />

<g:formRemote name="line-${lineId}-update-form" url="[action: 'edit']" update="ui-tabs-review" method="GET">

<g:hiddenField name="_eventId" value="updateLine"/>
<g:hiddenField name="execution" value="${flowExecutionKey}"/>
<g:hiddenField name="plan" value="${product?.plans ? 'true' : 'false'}"/>
<g:set var="planId" value="${product?.plans?.id}"/>
<g:set var="planPeriod" value="${PlanDTO.get(planId)}"/>

<li ${taxLine ? "" : "normal"} id="line-${lineId}" class="line ${editable && !taxLine ? 'active' : ''} ${mandatoryNotMet ? 'mandatory-dependency' : (optionalNotMet ? 'optional-dependency' : '')}">
    <span class="description">
        ${line.description}${planPeriod?.period ?' ('+planPeriod?.period?.getDescription(session['language_id'])+')' : ''}
        <g:if test="${isNew}">
            <span class="newOrderLine">(<g:message code="prompt.id.new"/>)</span>
        </g:if>
    </span>

    <span class="sub-total">
        <g:set var="subTotal" value="${formatNumber(number: line.getAmountAsDecimal(), type: 'currency', currencySymbol: currency.symbol, maxFractionDigits: 4)}"/>
        <g:message code="order.review.line.total" args="[subTotal]"   encodeAs="None"/>
    </span>
    <span class="qty-price">
        <g:set var="quantity" value="${formatNumber(number: line.getQuantityAsDecimal(), formatName: quantityNumberFormat)}"/>
        <g:set var="price" value="${ product?.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
        <g:if test="${line?.isPercentage}" >
            <g:set var="percentage" value="%${formatNumber(number: line?.priceAsDecimal)}"/>
            <g:message code="order.review.quantity.by.price" args="[quantity, percentage]"/>
        </g:if>
        <g:else>
            <g:set var="price" value="${formatNumber(number: line.getPriceAsDecimal(), type: 'currency', currencySymbol: currency.symbol, maxFractionDigits: 4)}"/>
            <g:message code="order.review.quantity.by.price" args="[quantity, price]"  encodeAs="None"/>
        </g:else>
    </span>
    <div style="clear: both;"></div>
</li>

<g:if test="${!taxLine}">
    <li id="line-${lineId}-editor" class="editor">
        <div class="box">
            <div class="form-columns">

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="order.label.quantity"/></content>

                    <content tag="label.for">line-${index}.quantityAsDecimal</content>
                    ${formatNumber(number: line.getQuantityAsDecimal()!=null? line.getQuantityAsDecimal(): BigDecimal.ONE, formatName: quantityNumberFormat)}

                </g:applyLayout>

                <sec:ifAllGranted roles="ORDER_26">
                    <g:applyLayout name="form/text">
                        <g:if test="${line?.isPercentage}" >
                            <content tag="label"><g:message code="order.label.line.price.percentage"/></content>
                        </g:if>
                        <g:elseif test="${line?.hasOrderLineTiers()}" >
                            <content tag="label"><g:message code="order.label.line.average.price"/></content>
                        </g:elseif>
                        <g:else>
                            <content tag="label"><g:message code="order.label.line.price"/></content>
                        </g:else>
                        <content tag="label.for">line-${lineId}.priceAsDecimal</content>
                        ${formatNumber(number: line.getPriceAsDecimal() ?: BigDecimal.ZERO, formatName: 'money.format', maxFractionDigits: 4)}
                    </g:applyLayout>
                </sec:ifAllGranted>

                <g:if test="${editable}">
                    <sec:ifAllGranted roles="ORDER_27">
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="order.label.line.descr"/></content>
                            <content tag="label.for">line-${lineId}.description</content>
                            <g:textField name="line-${lineId}.description" class="field description" value="${line.description}" disabled="${line.useItem}"/>
                        </g:applyLayout>
                    </sec:ifAllGranted>
                </g:if>
                <g:else>
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="order.label.line.descr"/></content>
                        <content tag="label.for">line-${lineId}.description</content>
                        ${line.description}
                    </g:applyLayout>
                </g:else>

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
                        <content tag="label.for">line-${lineId}.useItem</content>
                        <g:checkBox name="line-${lineId}.useItem" line="${lineId}" class="cb check" value="${line.useItem}" disabled="${!editable}" />

                        <script type="text/javascript">
                            $('#line-${lineId}\\.useItem').change(function() {
                                var line = $(this).attr('line');

                                if ($(this).is(':checked')) {
                                    $('#line-' + line + '\\.description').prop('disabled', 'true');
                                } else {
                                    $('#line-' + line + '\\.description').prop('disabled', '');
                                }
                            }).change();

                            var id="";
                            $('.line,.line .active').click(function(){
                                if($('#'+$(this).closest('form').attr('id')).find('li.bundled-price').hasClass('bundled-price')){
                                    //alert("test");
                                    setTimeout("showLabelOrTextFields(this)",100);
                                }
                                id = $(this).attr('id');
                            });

                            //This script to show label/text field in create/edit the order page.
                            function showLabelOrTextFields(){
                                if($('#'+id).attr('class').replace(/\s/g, "") =='lineactive'){
                                    $("[id^='textField_']").each(function(){

                                        $(this).show();
                                    });
                                    $("[id^='label_']").each(function(){
                                        $(this).hide();
                                    });
                                } else {
                                    $("[id^='textField_']").each(function(){
                                        $(this).hide();
                                    });
                                    $("[id^='label_']").each(function(){
                                        $(this).show();
                                    });
                                }
                            }

                        </script>
                    </g:applyLayout>
                </sec:ifAnyGranted>

                <g:hiddenField name="lineId" value="${lineId}"/>

            %{-- Show/Edit the 'order line metafields' --}%
                <g:if test="${editable && product}">
                    <g:render template="/metaFields/editMetaFields" model="[availableFields: product.orderLineMetaFields, fieldValues: line.metaFields]"/>
                </g:if>
                <g:else>
                    <g:each var="metaField" in="${line.metaFields?.sort{ it.displayOrder }}">
                        <g:render template="/metaFields/displayMetaFieldWS" model="[metaField: metaField]"/>
                    </g:each>
                </g:else>

                <g:if test="${params.plan != "true" && line.hasAssets()}">
                    <g:each var="assetId" in="${line.assetIds}" status="assetIdx">
                        <g:set var="assetObj" value="${AssetDTO.get(assetId)}" />
                        <div title="${assetObj.identifier}" >
                            <g:applyLayout name="form/checkbox">
                                <content tag="label">
                                    <jB:truncateLabel label="${assetObj.identifier}" max="${15}" suffix="..." />
                                </content>
                                <content tag="label.for">line-${lineId}.asset.${assetIdx}</content>
                                <g:if test="${assetIdx == 0}">
                                    <content tag="group.label"><g:message code="order.label.assets"/></content>
                                </g:if>
                            </g:applyLayout>
                        </div>
                    </g:each>
                </g:if>

            <%-- Order line Tiers linked to order lines --%>

                <g:if test="${line?.hasOrderLineTiers()}">
                    <g:applyLayout name="form/text">
                        <div class="box-cards box-cards-open">
                            <div class="box-cards-title">
                                <span><g:message code="order.label.order.line.details"/></span>
                            </div>
                            <div class="box-card-hold">
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
                                                        ${tier.tierFrom ? (null == tier.tierTo ? tier.tierFrom.setScale(2, BigDecimal.ROUND_HALF_UP).toString().concat("+") : tier.tierFrom): BigDecimal.ZERO}
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
                            </div>
                        </div>
                    </g:applyLayout>
                </g:if>
                <div class="btn-box">
                    <g:if test="${editable}">
                        <a class="submit save" onclick="$('#line-${lineId}-update-form').submit();"><span><g:message
                                code="button.update"/></span></a>
                        <g:remoteLink class="submit cancel" action="edit" params="[_eventId: 'removeLine', lineId: line.id]"
                                      update="ui-tabs-edit-changes" method="GET">
                            <span><g:message code="button.remove"/></span>
                        </g:remoteLink>
                        <g:if test="${line.isAllowedToUpdateOrderChange()}">
                            <g:remoteLink class="submit add" action="edit" id="${line.id}" params="[_eventId: 'initUpdateLine' ]" update="ui-tabs-edit-changes" method="GET" >
                                <span><g:message code="button.change"/></span>
                            </g:remoteLink>
                        </g:if>
                        <a class="submit show" onclick="showOrderLineCharges('${line?.id}');">
                            <span><g:message code="button.details"/></span>
                        </a>
                        <g:if test="${lineDependencies}">
                            <a onclick="showDependencies_line('line_${line?.id}');" class="submit add">
                                <span><g:message code="button.show.dependencies"/></span>
                            </a>
                        </g:if>
                        <g:if test="${product?.plans && product?.plans?.asList()?.first() && !product?.plans?.asList()?.first()?.editable}">
                            <a onclick="swapPlan('${product.id}');" class="submit add">
                                <span><g:message code="button.plan.swap"/></span>
                            </a>
                        </g:if>
                    </g:if>
                    <g:else>
                        <g:remoteLink class="submit" action="edit" id="${line.id}" params="[_eventId: 'showChangeForLine' ]" update="ui-tabs-edit-changes" method="GET">
                            <span><g:message code="button.change"/></span>
                        </g:remoteLink>
                    </g:else>
                </div>
            </div>
        </div>
    </li>
</g:if>
<%--
	line.orderLinePlanItems=  line.orderLinePlanItems.sort {it.description}
--%>

<g:set var="planIndex" value="0"/>
<g:if test="${product?.plans && isNew}">
    <g:set var="changeForCurrentLine" value="${ allChanges.find {key, value -> value.changed && (line.id != 0 ? value.change.id == line.id : value.change.itemId == line.itemId && value.change.quantityAsDecimal == line.quantityAsDecimal && (value.change.useItem == (line.useItem ? 1 : 0 )) )}?.value?.change }"/>
    <g:set var="showZeroQuantityBundle" value="${PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_SHOW_NO_IMPACT_PLAN_ITEMS) > 0}" />
    <g:each var="plan" in="${product?.plans}">
        <g:each var="planItem" in="${plan.planItems}">
            <g:set var="planItemPriceModel" value="${PriceModelBL.getPriceForDate(planItem.models, pricingDate)}"/>
            <g:set var="orderChangePlanItem" value="${changeForCurrentLine?.orderChangePlanItems?.find {it.itemId == planItem.item.id} }"/>

            <g:if test="${planItem?.bundle?.quantity || showZeroQuantityBundle || planItem?.item?.assetManagementEnabled || planItem?.item?.orderLineMetaFields}">
                <li class="bundled">
                    <span class="description">
                        <div id="label_description_${planIndex}">${orderChangePlanItem?.description ?: planItem.item.description}</div>
                    </span>
                    <span class="included-qty">
                        <span id="label_quantity_${planIndex}">${new BigDecimal(planItem.bundle?.quantity).setScale(0,BigDecimal.ROUND_DOWN)}</span>
                        <g:if test="${planItem.bundle?.period}">
                            ${WordUtils.capitalize(planItem.bundle?.period?.getDescription(session['language_id'])?.toLowerCase())}
                        </g:if>
                        <g:if test="${planItem.bundle?.targetCustomer != PlanItemBundleDTO.Customer.SELF}">
                            <g:message
                                    code="bundle.for.target.customer.${planItem.bundle?.targetCustomer}"/>
                        </g:if>
                    </span>

                    <div class="clear">&nbsp;</div>
                </li>
                <li class="bundled-price">
                    <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
                        <tbody>
                        <g:render template="/plan/priceModel" model="[model: planItemPriceModel, olpi: orderLinePlanItem]"/>
                        <g:set var="user" value="${UserDTO.get(order?.userId)}"/>
                        <g:set var="customerBillingCyclePeriod" value="${user?.getCustomer().getMainSubscription().getSubscriptionPeriod()}"/>
                        <g:set var="periodsEqual" value="${planItem.bundle?.period?.periodUnit?.id == customerBillingCyclePeriod?.getUnitId() && planItem.bundle?.period?.value == customerBillingCyclePeriod?.getValue()}"/>
                        <g:if test="${billingConfiguration.proratingType.equals(ProratingType.PRORATING_AUTO_ON.getOptionText())}">
                            <g:if test="${periodsEqual == true}">
                                <g:message code="plan.bundle.prorating.on"/>
                            </g:if>
                            <g:else>
                                <g:message code="plan.bundle.prorating.off"/>
                            </g:else>
                        </g:if>
                        <g:if test="${billingConfiguration.proratingType.equals(ProratingType.PRORATING_MANUAL.getOptionText())}">
                            <g:if test="${periodsEqual == true}">
                                <g:if test="${order?.prorateFlag}">
                                    <g:message code="plan.bundle.prorating.on"/>
                                </g:if>
                                <g:else>
                                    <g:message code="plan.bundle.prorating.off"/>
                                </g:else>
                            </g:if>
                            <g:else>
                                <g:message code="plan.bundle.prorating.off"/>
                            </g:else>
                        </g:if>
                        </tbody>
                    </table>
                </li>
                <g:if test="${line.hasAssets() && planItem?.item?.assetManagementEnabled == 1}">
                    <div>
                        <g:each var="assetId" in="${line.assetIds}" status="assetIdx">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label">${StringEscapeUtils.escapeHtml(AssetDTO.get(assetId).identifier)}</content>
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
                <g:if test="${planItem.item.plans}">
                    <g:render template="orderLinePlanItems" model="[item : planItem.item, level: 1]"/>
                </g:if>
            </g:if>
        </g:each>
    </g:each>
</g:if>

</g:formRemote>

<g:if test="${lineDependencies}">
    <g:render template="dependencies" model="[ line: line, type: 'line' ]" />
</g:if>

<g:if test="${product?.plans && product?.plans?.asList()?.first() && !product?.plans?.asList()?.first()?.editable}">
    <g:render template="planSwapDialog" model="[line: line]"/>
</g:if>
