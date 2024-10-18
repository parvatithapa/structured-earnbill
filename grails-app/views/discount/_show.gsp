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
  This template shows discount details.

  @author Amol Gadre
  @since  28-Nov-2012
--%>

<%@ page import="com.sapienter.jbilling.server.order.db.OrderPeriodDTO;"%>

<div class="column-hold">
    <div class="heading">
	    <strong>${selected.description}</strong>
	</div>

	<div class="box">
        <div class="sub-box">
            <!-- discount info -->
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="discount.detail.id"/></td>
                        <td class="value">${selected?.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.code"/></td>
                        <td class="value">${selected?.code}</td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.description"/></td>
                        <td class="value">${selected?.getDescription(session['language_id'])}</td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.rate"/></td>
                        <td class="value">
                        <g:if test="${selected?.type?.name()?.equals('ONE_TIME_PERCENTAGE')}">
                            %<g:formatNumber number="${selectedDiscountRate}" formatName='money.format'/>
                        </g:if>
                        <g:elseif test="${selected?.type?.name()?.equals('RECURRING_PERIODBASED')}">
                            <g:if test="${Boolean.TRUE.equals(selected?.isPercentageRate())}">
                                %<g:formatNumber number="${selectedDiscountRate}" formatName='money.format'/>
                            </g:if>
                            <g:else>
                                <g:formatNumber number="${selectedDiscountRate}" type="currency" currencySymbol="${selected?.entity?.currency?.symbol}"/>
                            </g:else>
                        </g:elseif>
                        <g:else>
                            <g:formatNumber number="${selectedDiscountRate}" type="currency" currencySymbol="${selected?.entity?.currency?.symbol}"/>
                        </g:else>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.type"/></td>
                        <td class="value"><g:message code="${selected?.type?.messageKey}"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.startDate"/></td>
                        <td class="value"><g:formatDate date="${selected?.startDate}" formatName="date.pretty.format"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.detail.endDate"/></td>
                        <td class="value"><g:formatDate date="${selected?.endDate}" formatName="date.pretty.format"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="discount.apply.to.all.periods"/></td>
                        <td class="value">
                            <g:if test="${selected?.applyToAllPeriods}">
                                <g:message code="default.boolean.true"/>
                            </g:if>
                            <g:else>
                                <g:message code="default.boolean.false"/>
                            </g:else>
                        </td>
                    </tr>

                    <tr><td>&nbsp;</td><td>&nbsp;</td></tr>

                    <g:each var="attribute" in="${selected?.attributes?.entrySet()}" status="s">
                        <g:if test="${attribute.key.equals('periodUnit')}">
                            <tr class="attribute">
                                <td><g:if test="${s==0}"><g:message code="discount.detail.attributes"/></g:if><g:else></g:else></td>
                                <td><g:message code="discount.detail.${attribute.key}"/></td>
                                <td class="value">${selectedOrderPeriodDescription}</td>
                            </tr>
                        </g:if>
                        <g:elseif test="${attribute.key.equals('isPercentage')}">
                            <tr class="attribute">
                                <td><g:if test="${s==0}"><g:message code="discount.detail.attributes"/></g:if><g:else></g:else></td>
                                <td><g:message code="discount.detail.${attribute.key}"/></td>
                                <td class="value">${isPercentageValue}</td>
                            </tr>
                        </g:elseif>
                        <g:elseif test="${attribute.key.equals('periodValue')}">
                            <g:if test="${attribute.value}">
                                <tr class="attribute">
                                    <td><g:if test="${s==0}"><g:message code="discount.detail.attributes"/></g:if><g:else></g:else></td>
                                    <td><g:message code="discount.detail.${attribute.key}"/></td>
                                    <td class="value">${attribute.value}</td>
                                </tr>
                            </g:if>
                        </g:elseif>
                        <g:else>
                            <g:if test="${attribute.value}">
                                <tr class="attribute">
                                    <td><g:if test="${s==0}"><g:message code="discount.detail.attributes"/></g:if><g:else></g:else></td>
                                    <td><g:message code="${attribute.key}"/></td>
                                    <td class="value">${attribute.value}</td>
                                </tr>
                            </g:if>
                        </g:else>
                    </g:each>

                </tbody>
            </table>
        </div>
    </div>

    <div class="btn-box">
    	<sec:ifAllGranted roles="DISCOUNT_153">
        	<g:link action="edit" id="${selected.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
        </sec:ifAllGranted>
	<sec:ifAllGranted roles="DISCOUNT_152">
	        <a onclick="showConfirm('deleteDiscount-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </sec:ifAllGranted>
    </div>

    <g:render template="/confirm"
          model="[message: 'discount.prompt.are.you.sure',
                  controller: 'discount',
                  action: 'deleteDiscount',
                  id: selected.id,
                 ]"/>

</div>
