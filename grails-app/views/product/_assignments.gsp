%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div class="heading">
    <strong><g:message code="asset.heading.assignments"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <g:if test="${assignments}">
            <table class="innerTable">
                <thead class="innerHeader">
                <tr>
                	<th><g:message code="asset.label.assign.order"/></th>
                    <th><g:message code="asset.label.assign.userId"/></th>
                    <th><g:message code="asset.label.assign.start.date"/></th>
                    <th><g:message code="asset.label.assign.end.date"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="assignment" in="${assignments}" status="idx">
                <g:set var="userId" value="${assignment?.orderLine.purchaseOrder.userId}"/>
                    <tr>
                        <td class="innerContent">
                            <sec:access url="/order/show">
                                <g:remoteLink breadcrumb="id" controller="order" action="show"
                                              id="${assignment.orderId}" params="['template': 'order']"
                                              before="register(this);" onSuccess="render(data, next);">
                                    ${assignment.orderId}
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/order/show">
                                ${assignment.orderId}
                            </sec:noAccess>
                        </td>
                        
                        <td class="innerContent">
                            <sec:access url="/customer/show">
                                <g:remoteLink breadcrumb="id" controller="customer" action="show"
                                              id="${userId}" params="['template': 'show']"
                                              before="register(this);" onSuccess="render(data, next);">
                                    ${userId}
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/customer/show">
                                ${userId}
                            </sec:noAccess>
                        </td>
                        
                        <td class="innerContent">
                            <g:formatDate formatName="date.format" date="${assignment.startDatetime}"/>
                        </td>
                        <td class="innerContent">
                            <g:if test="${assignment.endDatetime}">
                                <g:formatDate formatName="date.format" date="${assignment.endDatetime}"/>
                            </g:if>
                            <g:else>-</g:else>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <em><g:message code="asset.prompt.no.assignments"/></em>
        </g:else>
    </div>
</div>