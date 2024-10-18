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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.PricingField; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.util.PreferenceBL" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper;" %>
<%
    def company = CompanyDTO.load(session['company_id'])
    def childEntities = CompanyDTO.findAllByParent(company)
    def allEntities = childEntities + company
%>

<html>
<head>
	<meta name="layout" content="main"/>
	<r:script disposition='head'>
        function validateDate(element) {
            var dateFormat= "<g:message code="date.format"/>";
            if(!isValidDate(element, dateFormat)) {
                $("#error-messages").css("display","block");
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
                element.focus();
                return false;
            } else {
                return true;
            }
        }

        function submitForFirst() {
            //alert($('#selectionEntityId').val());
        	$('#first').val('true');
        	$('#order-events-form').submit();
        }

        function submitForBack() {
            //alert($('#selectionEntityId').val());
        	$('#back').val('true');
        	$('#order-events-form').submit();
        }

        function showEntitySpecificRecords(){
            var selected=$('#selectionEntityId').val();
            var selectedCallType=$('#selectionCallType').val();
            document.location.href = '${createLink(controller: 'mediation' , action:'showMediationRecords')}?entityId=' + selected+'&id=${processId}'+'&status=${params.status}'
                +'&selectedCallType='+selectedCallType+'&allCallTypes=${allCallTypes}';
        }
    </r:script>
</head>
<body>
    <g:set var="currency" value="${invoice?.currency ?: order?.currency ?: currency}"/>
     <g:set var="maxFractionDigits" value="${PreferenceBL.getPreferenceValue(company.getId(), Constants.PREFERENCE_DECIMAL_DIGITS_FOR_MEDIATION_EVENTS)}"/>
    <div class="table-info">
        <em><g:message code="event.mediation.heading"/></em>
    </div>

    %{-- Invoice summary if invoice set --}%
    <g:if test="${invoice}">
        <div>
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td class="small left-pad"><g:message code="event.summary.invoice.id"/></td>
                        <td class="value small">${invoice.id}</td>

                        <td class="small"><g:message code="event.summary.invoice.due.date"/></td>
                        <td class="value small"><g:formatDate date="${invoice.dueDate}" formatName="date.pretty.format"/></td>

                        <td class="small"><g:message code="event.summary.invoice.total"/></td>
                        <td class="value small"><g:formatNumber number="${invoice.total}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/></td>
                        <td/>
                    </tr>
                </tbody>
            </table>
        </div>
    </g:if>

    %{-- Order summary if order set --}%
    <g:if test="${order}">
        <div>
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td class="small left-pad"><g:message code="event.summary.order.id"/></td>
                        <td class="value small">${order.id}</td>

                        <td class="small"><g:message code="event.summary.order.total"/></td>
                        <td class="value small"><g:formatNumber number="${order.total}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/></td>

                        <td/>
                    </tr>
                </tbody>
            </table>
        </div>
    </g:if>
    %{-- Record summary set --}%
        <div>
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td class="left-pad"><g:message code="event.summary.record.mediation.id"/></td>
                        <td class="value">${processId}</td>

                        <td class="small"><g:message code="event.summary.record.status.id"/></td>
                        <td class="value small">${params?.status}</td>

                        <g:if test="${ null == company.getParent()}">
                            <td class="small"><g:message code="event.summary.record.entity.id"/></td>
                            <td class="value small">
                                <g:applyLayout name="form/select_holder">
                                    <content tag="label.for">selectionEntityId</content>
                                    <g:select id="selectionEntityId" name="selectionEntityId"
                                          from="${allEntities}"
                                          optionKey="id"
                                          optionValue="${{it?.description?.decodeHTML()}}"
                                          value="${selectionEntityId}"
                                          onchange="showEntitySpecificRecords();"
                                    />
                                 </g:applyLayout>
                            </td>
                        </g:if>
                        <g:if test="${ null != allCallTypes}">
                            <td class="small"><g:message code="event.summary.call.type"/></td>
                            <td class="value small">
                                <g:applyLayout name="form/select_holder">
                                    <g:select id="selectionCallType" name="selectionCallType"
                                          from="${allCallTypes}"
                                          value="${selectedCallType}"
                                          onchange="showEntitySpecificRecords();"
                                    />
                                 </g:applyLayout>
                            </td>
                        </g:if>
                        <td/>
                    </tr>
                </tbody>
            </table>
        </div>


<g:if test="${record}">
    <div class="table-box-below">
        <table class="innerTable">
            <thead class="innerHeader">
                <tr>
                    <th class="first"><g:message code="event.th.id"/></th>
                    <th><g:message code="event.th.key"/></th>
                    <th><g:message code="event.th.date"/></th>
                    <th><g:message code="event.th.description"/></th>
                    <th><g:message code="event.th.cdrType"/></th>
                    <th><g:message code="event.th.original.quantity"/></th>
                    <th><g:message code="event.th.quantity"/></th>
                    <th class=""><g:message code="event.th.cost.amount"/></th>
                    <th class="last"><g:message code="event.th.amount"/></th>
                </tr>
            </thead>
            <tbody class="innerContent">

            <!-- events list -->
            <g:set var="totalQuantity" value="${BigDecimal.ZERO}"/>
            <g:set var="totalAmount" value="${BigDecimal.ZERO}"/>
            <g:set var="totalCostAmount" value="${BigDecimal.ZERO}"/>

            <g:each var="recordLine" in="${records}">
                <g:if test="${recordLine?.quantity}" >
                    <g:set var="totalQuantity" value="${totalQuantity.add(recordLine.quantity)}"/>
                </g:if>
                <g:if test="${recordLine?.ratedPrice}" >
                    <g:set var="totalAmount" value="${totalAmount.add(recordLine.ratedPrice)}"/>
                </g:if>
                <g:if test="${totalCostAmount}" >
                    <g:set var="totalCostAmount" value="${totalAmount.add(recordLine.ratedCostPrice)}"/>
                </g:if>
                <tr>
                    <td class="left-pad">
                        ${recordLine.recordKey} -
                        <g:if test="${session['company_timezone'].equals('Australia/Sydney')}">
                            <g:formatDate date="${recordLine.eventDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                        </g:if>
                        <g:else>
                            <g:formatDate date="${recordLine.eventDate}" formatName="date.timeSecsAMPM.format"/>
                        </g:else>
                    </td>
                    <td>
                        ${recordLine.recordKey}
                    </td>
                    <td>
                        <g:if test="${session['company_timezone'].equals('Australia/Sydney')}">
                            <g:formatDate date="${recordLine.eventDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                        </g:if>
                        <g:else>
                            <g:formatDate date="${recordLine.eventDate}" formatName="date.timeSecsAMPM.format"/>
                        </g:else>
                    </td>
                    <td>
                        ${recordLine.description}
                    </td>
                    <td>
                        ${recordLine.cdrType}
                    </td>
                    <td>
                        <strong>
							<g:set var="originalQtyValue" value="${recordLine.originalQuantity}"/>
	                        <g:if test="${recordLine.quantity.compareTo(BigDecimal.ZERO) == 0}">
	                        	<g:set var="pricingFields" value="${PricingField.getPricingFieldsValue(recordLine.pricingFields)}"/>  
								<g:set var="originalQtyField" value="${PricingField.find(Arrays.asList(pricingFields), "Original_Quantity")}"/>
								<g:if test="${originalQtyField != null}">
									<g:set var="originalQtyValue" value="${originalQtyField.value}"/>
								</g:if>
	                        </g:if>
	                        <g:formatNumber number="${originalQtyValue}" formatName="decimal.format"/>
                        </strong>
                    </td>
                    <td>
                        <strong>
                            <g:formatNumber number="${recordLine.quantity}" formatName="decimal.format"/>
                        </strong>
                    </td>
                    <td>
                        <strong>
                            <g:formatNumber number="${recordLine.ratedCostPrice}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/>
                        </strong>
                    </td>
                    <td>
                        <strong>
                            <g:formatNumber number="${recordLine.ratedPrice}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/>
                        </strong>
                    </td>
                </tr>
            </g:each>

                <!-- subtotals -->
                <tr class="bg">
                    <td class="col02"></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td>
                        <strong><g:formatNumber number="${totalQuantity}" formatName="decimal.format"/></strong>
                    </td>
                    <td>
                        <strong><g:formatNumber number="${totalCostAmount}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/></strong>
                    </td>
                    <td>
                        <strong><g:formatNumber number="${totalAmount}" type="currency" maxFractionDigits="${maxFractionDigits}" currencySymbol="${currency?.symbol}"/></strong>
                    </td>
                </tr>

            </tbody>
        </table>
    </div>

	<div class="form-hold">
	        <g:form name="order-events-form" controller="mediation" action="${params.action}" id="${processId}">
	            <fieldset>
	                <div class="form-columns">
						<div class="row">
							<g:applyLayout name="form/date">
						          <content tag="label"><g:message code="event.summary.start.date"/></content>
						          <content tag="label.for">event_start_date</content>
						          <g:textField class="field" name="event_start_date" value="${formatDate(date: params?.event_start_date?new Date(params?.event_start_date):null, formatName:'datepicker.format')}" onblur="validateDate(this)"/>
						     </g:applyLayout>
						     		<g:applyLayout name="form/date">
						          <content tag="label"><g:message code="event.summary.end.date"/></content>
						          <content tag="label.for">event_end_date</content>
						          <g:textField class="field" name="event_end_date" value="${formatDate(date: params?.event_end_date?new Date(params?.event_end_date):null, formatName:'datepicker.format')}" onblur="validateDate(this)"/>
						     </g:applyLayout>
						     <g:hiddenField name="first" value="false"/>
                             <g:hiddenField name="back" value="false"/>
						     <g:hiddenField name="status" value="${record?.status}"/>
                             <g:hiddenField name="offset" value="${offset}"/>
                             <g:hiddenField name="size" value="${size}"/>
                             <g:hiddenField name="orderId" value="${order?.id}"/>
                             <g:hiddenField name="entityId" value="${selectionEntityId}"/>
					    </div>

						<div class="row">
                            <g:if test="${offset > 0}">
                                <a onclick="submitForBack()" class="submit show">
                                    <span><g:message code="button.view.back.events"/></span>
                                </a>
                                <a onclick="submitForFirst();" class="submit show">
                                    <span><g:message code="button.view.first.events" /></span>
                                </a>
                            </g:if>
                            <g:if test="${next}">
                                <a onclick="$('#order-events-form').submit();" class="submit show">
                                    <span><g:message code="button.view.next.events"/></span>
                                </a>
                            </g:if>
					   </div>
				   </div>
			   </fieldset>
		   </g:form>
	</div>

        <div class="pager-box">
            <div class="row">
                <div class="download">
                        <sec:access url="/mediation/mediationRecordsCsv">
                            <g:link action="mediationRecordsCsv" id="${record?.processId}" class="pager-button"
                                    params="${params + ['status': record?.status, 'orderId':order?.id]}">
                                <g:message code="download.csv.link"/>
                            </g:link>
                        </sec:access>
                </div>
            </div>
        </div>

</g:if>
</body>
</html>
