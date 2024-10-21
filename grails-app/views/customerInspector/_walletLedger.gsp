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
<div class="sub-box">
    <div class="table-box tab-table">
          <div class="table-scroll">
              <table id="walletTransactions" cellspacing="0" cellpadding="0">
                  <thead>
                      <tr>
                        <th class="header-sortable">
                            <g:message code="user.subscriber.number"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.transaction.date"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.transaction.action"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.refund.narration"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.refund.amount"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.remaining.balance"/>
                        </th>
                        <th class="header-sortable">
                            <g:message code="wallet.ledger.transaction.created"/>
                        </th>
                      </tr>
                  </thead>
                  <tbody>
                    <g:each var="walletTransaction" in="${walletTransactionResponse?.getWalletTransactions()}" >
                      <tr>
                        <td>
                            ${walletTransaction?.subscriberNumber}
                        </td>
                        <td>
                            <g:formatDate date="${Date.from(LocalDateTime.parse(walletTransaction?.getRechargeDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                      .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>
                        </td>
                        <td data-cy="action">
                            ${walletTransaction?.action}
                        </td>
                        <td data-cy="narration">
                            ${walletTransaction?.narration}
                        </td>
                        <td data-cy="amount">
                            ${String.format("%.2f",walletTransaction?.amount)}
                        </td>
                        <td data-cy="balance">
                            ${String.format("%.2f",walletTransaction?.balance)}
                        </td>
                        <td data-cy="createdBy">
                            ${walletTransaction?.createdBy}
                        </td>
                      </tr>
                    </g:each>
                  </tbody>
              </table>
          </div>
          <div class="pager-box">
              <div class="results">
                  <g:render template="/layouts/includes/pagerShowResults"
                            model="[steps: [10, 20, 50], update: 'wallet-column', action: 'filterWalletTransactions',
                                    extraParams: [
                                    userId: user?.id ?: params.userId,
                            ]]"/>
              </div>
              <jB:isPaginationAvailable total="${walletTransactionResponse?.getTotal() ?: 0}">
                  <div class="row-center">
                     <jB:remotePaginate action="filterWalletTransactions"
                                   params="${sortableParams(params: [partial: true, userId: user?.id ?: params.userId, max: params.max])}"
                                   total="${walletTransactionResponse?.getTotal() ?: 0}"
                                   update="wallet-column"
                                   method="GET"/>
                  </div>
              </jB:isPaginationAvailable>
          </div>
    </div>
</div>
