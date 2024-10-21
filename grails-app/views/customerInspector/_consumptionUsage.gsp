%{--
 SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 _____________________

 [2024] Sarathi Softech Pvt. Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is and remains
 the property of Sarathi Softech.
 The intellectual and technical concepts contained
 herein are proprietary to Sarathi Softech
 and are protected by IP copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
--}%

<%@ page import="java.time.LocalDateTime; java.time.format.DateTimeFormatter; java.time.ZoneId;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.MAP_TYPE_DATA;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.MAP_TYPE_VOICE;" %>
<div class="table-box tab-table">
      <div class="table-scroll">
          <table id="products" cellspacing="0" cellpadding="0">
              <thead>
                  <tr>
                    <th class="header-sortable">
                        <g:message code="user.subscriber.number"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.name"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.start.date"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.end.date"/>
                    </th>
                      <th class="header-sortable">
                          <g:message code="consumption.usage.plan.map.type"/>
                      </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.initial.quantity"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.used.quantity"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.plan.available.quantity"/>
                    </th>
                    <th class="header-sortable">
                        <g:message code="consumption.usage.status"/>
                    </th>
                  </tr>
              </thead>
              <tbody>
                <g:each var="consumptionUsage" in="${consumptionUsageMapResponse?.getConsumptionUsageDetails()}" >
                  <tr>
                    <td data-cy="bhmrRecordLink">
                       <g:link controller="customer" action="showBhmrRecordPage" params="[selected : consumptionUsage?.id]" style="color: #e0c561;">
                            ${consumptionUsage?.subscriberNumber}
                            <span style="font-family: AppDirectIcons; color: #bfbfbf">&#xe03e;</span>
                       </g:link>
                    </td>
                    <td data-cy="planName">
                        ${consumptionUsage?.planDescription}
                    </td>
                    <td data-cy="startDate">
                        <g:formatDate date="${Date.from(LocalDateTime.parse(consumptionUsage?.startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                   .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>
                    </td>
                    <td data-cy="endDate">
                        <g:formatDate date="${Date.from(LocalDateTime.parse(consumptionUsage?.endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                   .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>
                    </td>
                      <g:if test="${consumptionUsage.dataType.toLowerCase().contains(MAP_TYPE_DATA)}">
                          <td>
                              ${consumptionUsage?.dataType}(GB)
                          </td>
                          <td data-cy="initialQuantity">
                              ${(consumptionUsage?.initialQuantity/1024).toBigInteger()}
                          </td>
                          <td data-cy="usedQuantity">
                              ${((consumptionUsage?.initialQuantity/1024)-((consumptionUsage?.availableQuantity/1024).setScale(4,
                              BigDecimal.ROUND_DOWN))).setScale(4, BigDecimal.ROUND_HALF_UP)}
                          </td>
                          <td data-cy="availableQuantity">
                              ${(consumptionUsage?.availableQuantity/1024).setScale(4, BigDecimal.ROUND_DOWN)}
                          </td>
                      </g:if>
                      <g:elseif test="${consumptionUsage.dataType.toLowerCase().contains(MAP_TYPE_VOICE)}">
                          <td>
                              ${consumptionUsage?.dataType}(Min)
                          </td>
                          <td data-cy="initialQuantity">
                              ${(consumptionUsage?.initialQuantity).toBigInteger()}
                          </td>
                          <td data-cy="usedQuantity">
                              ${((consumptionUsage?.initialQuantity)-((consumptionUsage?.availableQuantity).setScale(4,
                              BigDecimal.ROUND_DOWN))).setScale(4, BigDecimal.ROUND_HALF_UP)}
                          </td>
                          <td data-cy="availableQuantity">
                              ${(consumptionUsage?.availableQuantity).setScale(4, BigDecimal.ROUND_DOWN)}
                          </td>
                      </g:elseif>
                      <g:else>
                          <td>
                              ${consumptionUsage?.dataType}
                          </td>
                          <td data-cy="initialQuantity">
                              ${(consumptionUsage?.initialQuantity).toBigInteger()}
                          </td>
                          <td data-cy="usedQuantity">
                              ${((consumptionUsage?.initialQuantity)-((consumptionUsage?.availableQuantity).setScale(4,
                              BigDecimal.ROUND_DOWN))).setScale(4, BigDecimal.ROUND_HALF_UP)}
                          </td>
                          <td data-cy="availableQuantity">
                              ${(consumptionUsage?.availableQuantity).setScale(4, BigDecimal.ROUND_DOWN)}
                          </td>
                      </g:else>
                      <td data-cy="status">
                          ${consumptionUsage?.status}
                    </td>
                  </tr>
                </g:each>
              </tbody>
          </table>
      </div>
</div>
<div class="pager-box">
    <div class="results">
        <g:render template="/layouts/includes/pagerShowResults"
                  model="[steps: [10, 20, 50], update: 'consumption-column', action: 'filterConsumptionUsages',
                          extraParams: [
                          userId: user?.id ?: params.userId,
                  ]]"/>
    </div>
    <jB:isPaginationAvailable total="${consumptionUsageMapResponse?.getTotal() ?: 0}">
        <div class="row-center">
           <jB:remotePaginate action="filterConsumptionUsages"
                         params="${sortableParams(params: [partial: true, userId: user?.id ?: params.userId, max: params.max])}"
                         total="${consumptionUsageMapResponse?.getTotal() ?: 0}"
                         update="consumption-column"
                         method="GET"/>
        </div>
    </jB:isPaginationAvailable>
</div>

