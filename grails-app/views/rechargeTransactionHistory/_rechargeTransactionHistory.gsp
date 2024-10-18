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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; java.time.ZoneId; com.sapienter.jbilling.server.user.UserBL; com.sapienter.jbilling.server.user.contact.db.ContactDTO; java.time.LocalDateTime; java.time.format.DateTimeFormatter;" %>

<div class="table-box">
    <div class="table-scroll">
        <table id="rechargeHistoryTB">
            %{--id subscriberNumber userId date amount action--}%
            <thead>
                <tr>
                    <th class=" header-sortable">
                            <g:message code="recharge.history.userId"/>
                    </th>

                    <th class=" header-sortable">
                            <g:message code="recharge.history.subscriber"/>
                    </th>

                    <th class="header-sortable">
                            <g:message code="recharge.history.rechargeDate"/>
                    </th>

                    <th class=" header-sortable">
                            <g:message code="recharge.history.transaction.type"/>
                    </th>

                    <th class="header-sortable">
                            <g:message code="recharge.history.amount"/>
                    </th>

                    <th class="header-sortable ">
                            <g:message code="recharge.history.status"/>
                    </th>

                    <th class=" header-sortable">
                            <g:message code="recharge.history.refundable"/>
                    </th>
                </tr>
            </thead>
            <tbody>
            <g:each var="rechargeHistory" in="${rechargeHistoryData?.getData()}">
                <tr>
                    <td>
                        <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate: rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            <span>${rechargeHistory.userId}</span>
                        </g:remoteLink>
                    </td>

                    <td>
                       <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            ${rechargeHistory.subscriberNumber}
                       </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                           <g:formatDate date="${Date.from(LocalDateTime.parse(rechargeHistory.transactionDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                              .atZone(ZoneId.of('UTC')).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>
                        </g:remoteLink>
                    </td>

                    <td>
                       <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            ${rechargeHistory.type}
                       </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            <g:if test='${rechargeHistory.isRefund.equals(true)}'>
                                ${rechargeHistory.refundAmount}
                            </g:if>
                            <g:elseif test="${rechargeHistory.rechargeAmount > rechargeHistory.totalRechargeAmount}">
                                ${rechargeHistory.rechargeAmount}
                            </g:elseif>
                            <g:else>
                                ${rechargeHistory.totalRechargeAmount}
                            </g:else>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            ${rechargeHistory.status}
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" params="[transactionId: rechargeHistory.id, rechargeDate:rechargeHistory.transactionDate]" id="${rechargeHistory.id}"  before="register(this);"  onSuccess="render(data, next);">
                            <g:if test='${rechargeHistory.isRefundable.equals(true)}'>
                                Yes
                           </g:if>
                           <g:else>
                                No
                           </g:else>
                       </g:remoteLink>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                       model="[steps: [10, 20, 50], update: 'rechargeTransactionHistory', action : 'filterRechargeTransactions']"/>
        </div>
    </div>
    <jB:isPaginationAvailable total="${rechargeHistoryData?.getTotal()?: 0}">
        <div class="row-center">
            <jB:remotePaginate action="filterRechargeTransactions"
                update="rechargeTransactionHistory"
                params="${sortableParams(params: [partial: true])}"
                total="${rechargeHistoryData?.getTotal() ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>
