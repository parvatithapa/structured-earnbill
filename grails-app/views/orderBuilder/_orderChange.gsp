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

<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldBL; com.sapienter.jbilling.server.pricing.db.PriceModelStrategy"%>
<%@ page import="com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.metafields.db.MetaField; com.sapienter.jbilling.server.item.db.AssetDTO; com.sapienter.jbilling.server.pricing.PriceModelBL; org.apache.commons.lang.WordUtils; com.sapienter.jbilling.server.item.db.PlanItemBundleDTO; com.sapienter.jbilling.server.item.db.ItemDTO" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%--
  Renders an OrderChangeWS as an editable row for the order builder editOrderChange pane.
--%>
<g:set var="changeId" value="${orderChange.change.id}"/>
<g:set var="allowUpdate" value="${orderChange.change.isAllowedToUpdateOrderChange()}"/>
<g:set var="changeDependencies" value="${productDependencies["change_" + changeId]}" />
<g:set var="mandatoryNotMet" value="${changeDependencies?.any{it.type == 'mandatory' && !it.met}}"/>
<g:set var="optionalNotMet" value="${changeDependencies?.any{it.type == 'optional' && !it.met}}"/>

<tr class="change_header ${mandatoryNotMet ? 'mandatory-dependency' : (optionalNotMet ? 'optional-dependency' : '')}" id="edit_change_header_${changeId}"
    onclick="showEditChangeDetails(this);">
    <td>
        <strong>${orderChange.productDescription}</strong>
        <em><g:message code="table.id.format" args="[orderChange.change.itemId as String]"/></em>
    </td>
    <td class="small">
        <span><g:formatDate date="${orderChange.change.startDate}"
                            formatName="date.format"/></span>
    </td>
    <td class="medium">
        <span>${orderChange.change.userAssignedStatus}</span>
    </td>
</tr>
<tr id="edit_change_details_${changeId}" class="change_details"
    style="${!currentOrderChangeId?.contains(changeId) ? 'display: none;' : ''}">
    <td colspan="3">
        <g:formRemote name="change-${changeId}-update-form" url="[action: 'edit']" update="ui-tabs-edit-changes" method="GET">
            <g:hiddenField name="_eventId" value="updateChange"/>
            <g:hiddenField name="execution" value="${flowExecutionKey}"/>
            <g:hiddenField name="changeId" value="${changeId}"/>
            <g:hiddenField name="change-${changeId}.optLock" value="${orderChange.change.optLock}"/>

            <div class="box">
                <div class="form-columns">

                    <g:set var="product" value="${ItemDTO.get(orderChange.change.itemId)}"/>

                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="orderChange.label.orderChangeType"/></content>
                        <content tag="label.for">change-${changeId}.orderChangeTypeId</content>
                        <content tag="include.script">true</content>
                        <g:select name="change-${changeId}.orderChangeTypeId"
                                  from="${orderChangeTypes.findAll { cht ->
                                              return cht.defaultType || cht.itemTypes.any { typeId -> return product.itemTypes.collect { it2 -> it2.id }.contains( typeId ) }
                                          }.sort { it.id != Constants.ORDER_CHANGE_TYPE_DEFAULT } }"
                                  optionKey="${{it.id}}" optionValue="${{it.name}}"
                                  onchange="${remoteFunction(controller: 'orderBuilder',
                                                              action: 'edit',
                                                              update: 'ui-tabs-edit-changes',
                                                              method: 'GET',
                                                              params: '\'changeTypeId=\'+' + '$(this).val()' + ' + \'&_eventId=updateChangeType&changeId=' + orderChange.change.id +'\'')}"
                                  value="${orderChange.change.orderChangeTypeId}"
                                  disabled="${!allowUpdate}" />
                    </g:applyLayout>

                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="orderChange.label.purchase.order.period"/></content>
                        <content tag="label.for">change-${changeId}.order</content>
                        ${order.periodStr}
                    </g:applyLayout>

                    <g:applyLayout name="form/date">
                        <content tag="label"><g:message code="orderChange.label.effectiveDate"/></content>
                        <content tag="label.for">change-${changeId}.startDate</content>
                        <g:textField class="field" name="change-${changeId}.startDate"
                                     value="${formatDate(date: orderChange.change?.startDate, formatName: 'datepicker.format')}"
                                     disabled="${!allowUpdate}" />
                        <content tag="onClose">
                            function() {
                            // do nothing
                            }
                        </content>
                    </g:applyLayout>
                    <g:applyLayout name="form/checkbox">
                        <content tag="label"><g:message code="orderChange.label.apply"/></content>
                        <content tag="label.for">change-${changeId}.appliedManually</content>
                        <g:checkBox name="change-${changeId}.appliedManually"
                                    class="cb check"
                                    value="${orderChange.change.appliedManually}"
                                    disabled="${!allowUpdate}" />
                    </g:applyLayout>

                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="orderChange.label.status"/></content>
                        <content tag="label.for">change-${changeId}.userAssignedStatusId</content>
                        <content tag="include.script">true</content>
                        <g:select name="change-${changeId}.userAssignedStatusId"
                                  from="${orderChangeUserStatuses}"
                                  noSelection="['': '']"
                                  optionKey="id" optionValue="${{it.getDescription(session['language_id']).content}}"
                                  value="${orderChange.change.userAssignedStatusId}"
                                  disabled="${!allowUpdate}" />
                    </g:applyLayout>


                    <g:set var="product" value="${ItemDTO.get(orderChange.change.itemId)}"/>
                    <g:set var="existedLineForChange" value="${orderChange.change.orderLineId > 0 && orderChange.change.orderId > 0 ?
                            persistedOrderOrderLinesMap.get(orderChange.change.orderId)?.find{it.id == orderChange.change.orderLineId}
                            : null}"/>
                    <g:set var="disableEditItemFields" value="${existedLineForChange != null && existedLineForChange.useItem}"/>
                    <g:set var="quantityNumberFormat" value="${product?.hasDecimals ? 'money.format' : 'default.number.format'}"/>

                    <g:if test="${product.assetManagementEnabled == 1}">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="orderChange.label.quantity"/></content>
                            <content tag="label.for">change-${changeId}.quantityAsDecimal</content>
                            ${formatNumber(number: orderChange.change.getQuantityAsDecimal(), formatName: quantityNumberFormat)}
                        </g:applyLayout>
                    </g:if>
                    <g:else>
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="orderChange.label.quantity"/></content>
                            <content tag="label.for">change-${changeId}.quantityAsDecimal</content>
                            <g:textField name="change-${changeId}.quantityAsDecimal" class="field quantity"
                                         value="${formatNumber(number: orderChange.change.getQuantityAsDecimal() != null ? orderChange.change.getQuantityAsDecimal() : BigDecimal.ONE, formatName: quantityNumberFormat)}"
                                         readonly="${!allowUpdate}" />
                        </g:applyLayout>
                    </g:else>

                    <sec:ifAllGranted roles="ORDER_26">

                        <g:applyLayout name="form/${disableEditItemFields ? "text" : "input"}">
                        	<g:set var="price" value="${ product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'])}"/>
	                		<g:if test="${price.type==PriceModelStrategy.LINE_PERCENTAGE}" >
                                <content tag="label"><g:message code="order.label.line.price.percentage"/></content>
                            </g:if>
                            <g:else>
                                <content tag="label"><g:message code="order.label.line.price"/></content>
                            </g:else>
                            <content tag="label.for">change-${changeId}.priceAsDecimal</content>
                            <g:if test="${disableEditItemFields}">
                                ${formatNumber(number: price.type==PriceModelStrategy.TEASER_PRICING ? orderChange.change.getPriceAsDecimal() : existedLineForChange.getPriceAsDecimal() ?: BigDecimal.ZERO, formatName: 'price.format', maxFractionDigits: 4)}
                            </g:if>
                            <g:else>
                                <g:textField    name = "change-${changeId}.priceAsDecimal"
                                               class = "field price"
                                               value = "${formatNumber(           number : orderChange.change.getPriceAsDecimal() ?: BigDecimal.ZERO,
                                                                              formatName : 'price.format.edit',
                                                                       maxFractionDigits : 4)}"
                                            disabled = "${orderChange.change.useItem > 0}"/>
                            </g:else>
                        </g:applyLayout>
                    </sec:ifAllGranted>

                    <sec:ifAllGranted roles="ORDER_27">
                        <g:applyLayout name="form/${disableEditItemFields ? "text" : "input"}">
                            <content tag="label"><g:message code="orderChange.label.description"/></content>
                            <content tag="label.for">change-${changeId}.description</content>
                            <g:if test="${disableEditItemFields}">
                                ${existedLineForChange?.description}
                            </g:if>
                            <g:else>
                                <g:textField name="change-${changeId}.description" class="field description"
                                         value="${orderChange.change.description}"
                                         disabled="${orderChange.change.useItem > 0}"/>
                            </g:else>
                        </g:applyLayout>
                    </sec:ifAllGranted>

                    %{-- edit useItem only for new line changes--}%
                    <g:if test="${!orderChange.change.orderLineId || orderChange.change.orderLineId <= 0}">
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
                                <content tag="label.for">change-${changeId}.useItem</content>
                                <g:checkBox name="change-${changeId}.useItem" changeId="${changeId}"
                                            class="cb check" value="${orderChange.change.useItem > 0}"/>

                                <script type="text/javascript">
                                    $('#change-${changeId}\\.useItem').change(
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
                    </g:if>

                    %{-- Display the 'order line metafields' for the order change --}%
                    <g:if test="${product.orderLineMetaFields}" ><div class="row">&nbsp;</div></g:if>
                    <g:render template="/metaFields/editMetaFields" model="[availableFields: product.orderLineMetaFields, fieldValues: orderChange.change.metaFields]"/>

                    <g:if test="${orderChange.change.assetIds}">
                        <g:each var="assetId" in="${orderChange.change.assetIds}" status="assetIdx">
                            <g:set var="assetObj" value="${AssetDTO.get(assetId)}" />

                            <div title="${assetObj.identifier}" >
                                <g:applyLayout name="form/checkbox">
                                    <content tag="label">
                                        <jB:truncateLabel label="${assetObj.identifier}" max="${15}" suffix="..." />
                                    </content>
                                    <content tag="label.for">change-${changeId}.asset.${assetIdx}</content>
                                    <g:if test="${assetIdx == 0}">
                                        <content tag="group.label"><g:message code="order.label.assets"/></content>
                                    </g:if>
                                    <g:checkBox name="change-${changeId}.asset.${assetIdx}.status"
                                                line="${changeId}" class="cb check" value="${assetId}" checked="true"/>
                                    <g:hiddenField name="change-${changeId}.asset.${assetIdx}.id" value="${assetId}"/>
                                </g:applyLayout>
                            </div>

                        </g:each>
                    </g:if>

                    <g:set var="currentChangeType" value="${orderChangeTypes.find { return it.id == orderChange.change.orderChangeTypeId } }"/>
                    <g:if test="${currentChangeType?.allowOrderStatusChange}">
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="orderChange.label.orderStatusIdToApply"/></content>
                            <content tag="label.for">change-${changeId}.orderStatusIdToApply</content>
                            <content tag="include.script">true</content>
                            <g:select name="change-${changeId}.orderStatusIdToApply"
                                      from="${orderStatuses}"
                                      noSelection="['': '']"
                                      optionKey="${{it.getId()}}" optionValue="${{it.getDescription(session['language_id'])}}"
                                      value="${orderChange.change.orderStatusIdToApply}"/>
                        </g:applyLayout>
                    </g:if>

                     %{-- Display the 'order change type metafields' for the order change --}%
                    <g:if test="${currentChangeType?.orderChangeTypeMetaFields}" >
                        <g:set var="availableOrderChangeMetaFields" value="${currentChangeType?.orderChangeTypeMetaFields.collect {MetaFieldBL.getDTO(it, session['company_id'] )} }"/>
                        <g:render template="/metaFields/editMetaFields" model="[availableFields: availableOrderChangeMetaFields , fieldValues: orderChange.change.metaFields]"/>
                    </g:if>
                </div>
            </div>

            <g:set var="currentPlan" value="${plans.find{ it.id == orderChange.change.itemId }?.plans?.asList()?.first()}"/>
			<g:if
				test="${currentPlan && orderChange.change.orderLineId < 0 && currentPlan.editable == 0}">
				<ul class="plan-order-changes-list">
					<g:set var="displayedOrderChangePlanItems" value="${[]}" />
					<g:each var="planItem" in="${planItemMap.get(currentPlan.id)}">
						<g:set var="orderChangePlanItem"
							value="${orderChange.change.orderChangePlanItems?.find { it.itemId == planItem.itemId && !displayedOrderChangePlanItems.contains(it) }}" />
						<g:if test="${orderChangePlanItem}">
							<g:set var="putToDisplayed"
								value="${displayedOrderChangePlanItems << orderChangePlanItem}" />
							<g:render template="editChangePlanItem"
								model="[changeObj: orderChange.change, user: user, planItem: planItem, changePlanItem: orderChangePlanItem]" />
						</g:if>
					</g:each>
				</ul>
			</g:if>



            <div class="btn-box">
                <g:if test="${allowUpdate}">
                    <a class="submit save" onclick="$('#change-${changeId}-update-form').submit();"><span><g:message
                            code="button.update"/></span></a>
                </g:if>
                <g:remoteLink class="submit cancel" action="edit"
                              params="[_eventId: 'removeChange', changeId: changeId]"
                              update="ui-tabs-edit-changes" method="GET">
                    <span><g:message code="button.remove"/></span>
                </g:remoteLink>
                <g:if test="${product.assetManagementEnabled == 1}">
                    <g:remoteLink class="submit add" action="edit" id="${changeId}"
                                  params="[_eventId: 'initUpdateAssets']" update="assets-box-update" method="GET">
                        <span><g:message code="button.add.assets"/></span>
                    </g:remoteLink>
                    <g:if test="${product.assetManagementEnabled == 1 && orderChange.change.assetIds.length == 0 && orderChange?.change?.removal == 0}">
                        <script> $("#" + ${changeId}).click(); </script>
                    </g:if>
                </g:if>
                <g:if test="${changeDependencies}">
                    <a onclick="showDependencies_change('change_${changeId}');" class="submit add">
                        <span><g:message code="button.show.dependencies"/></span>
                    </a>
                </g:if>
            </div>
        </g:formRemote>
        <g:if test="${changeDependencies}">
            <g:render template="dependencies" model="[ change: orderChange.change, type: 'change' ]" />
        </g:if>
    </td>
</tr>
<script type="text/javascript">
    $('input[name="change-${changeId}.startDate"]').change(function() {
        if ($('input[name="change-${changeId}.useItem"]').is(':checked')) {
            var itemId = "${orderChange.change.itemId}";
            var userId = "${order?.userId}";
            var currencyId = "${order?.currencyId}";
            var quantity = 1;
            var date = $(this).val();

            $.ajax({
                type: 'POST',
                url: '${createLink(action: 'recalculatePriceByDate')}',
                data: {      date : date,
                           itemId : itemId,
                           userId : userId,
                         quantity : quantity,
                       currencyId : currencyId},
                success: function(data) {
                    $('input[name="change-${changeId}.priceAsDecimal"]').val(data.price);
                }
            });
        }
    });
</script>
