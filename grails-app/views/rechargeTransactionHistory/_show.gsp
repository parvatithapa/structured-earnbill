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

<%@page import="com.sapienter.jbilling.server.customer.CustomerBL; java.time.ZoneId;org.springframework.util.StringUtils;java.time.LocalDateTime; java.time.format.DateTimeFormatter;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_REFUND_RECHARGE_TRANSACTION;" %>
<html>
    <head>
            <meta name="layout" content="main"/>
            <style>
                #note{padding:5px;background:whitesmoke;}
            </style>
    </head>
    <body>
        <div class="heading">
            <strong><em><g:message code="recharge.history.selected.title"/></em></strong>
        </div>
        <div class="form-hold">
            <g:form name="recharge-history-form" useToken="true">
                <div class="sub-box">
                    <table class="dataTable" cellspacing="0" cellpadding="0">
                        <tbody>
                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.userId"/>
                                </td>
                                <td data-cy="userId">
                                    ${rechargeTransactionWS.userId}
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.subscriber"/>
                                </td>
                                <td data-cy="subscriberNumber">
                                    ${rechargeTransactionWS.subscriberNumber}
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.package.name"/>
                                </td>
                                <td data-cy="planDescription">
                                    <g:if test = '${null == rechargeTransactionWS.planDescription}'>
                                        NA
                                    </g:if>
                                    <g:else>
                                        ${rechargeTransactionWS.planDescription}
                                    </g:else>
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.rechargeDate"/>
                                </td>
                                <td data-cy="rechargeDate">
                                   <g:formatDate date="${Date.from(LocalDateTime.parse(rechargeTransactionWS.transactionDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                       .atZone(ZoneId.of('UTC')).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}" />
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.transaction.type"/>
                                </td>
                                <td data-cy="transactionType">
                                    ${rechargeTransactionWS.type}
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.amount"/>
                                </td>
                                <td data-cy="rechargeAmount">
                                    <g:if test="${rechargeTransactionWS.isRefund.equals(true)}">
                                        ${rechargeTransactionWS.refundAmount}
                                         <g:hiddenField name="amount" value="${rechargeTransactionWS.refundAmount}"/>
                                    </g:if>
                                    <g:elseif test="${rechargeTransactionWS.rechargeAmount > rechargeTransactionWS.totalRechargeAmount}">
                                        ${rechargeTransactionWS.rechargeAmount}
                                        <g:hiddenField name="amount" value="${rechargeTransactionWS.refundAmount}"/>
                                    </g:elseif>
                                    <g:else>
                                         ${rechargeTransactionWS.totalRechargeAmount}
                                         <g:hiddenField name="amount" value="${rechargeTransactionWS.totalRechargeAmount}"/>
                                    </g:else>
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.status"/>
                                </td>
                                <td data-cy="rechargeStatus">
                                   ${rechargeTransactionWS.status}
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.refundable"/>
                                </td>
                                <td data-cy="isRefundable">
                                    <g:if test='${rechargeTransactionWS.isRefundable.equals(true)}'>
                                            Yes
                                        </g:if>
                                        <g:else>
                                             No
                                        </g:else>
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.created"/>
                                </td>
                                <td data-cy="createdBy">
                                    ${rechargeTransactionWS.createdBy}
                                </td>
                            </tr>

                            <tr>
                                <td class="value wide-width">
                                    <g:message code="recharge.history.source"/>
                                </td>
                                <td data-cy="source">
                                   ${rechargeTransactionWS.source}
                                </td>
                            </tr>

                            <tr>
                              <td class="value wide-width">
                                    <g:if test='${rechargeTransactionWS.isRefundable.equals(true)}'>
                                        <g:message code="recharge.history.note"/><span id="mandatory-meta-field">*</span>
                                    </g:if>
                                    <g:else>
                                        <g:message code="recharge.history.note"/>
                                    </g:else>
                              </td>
                                <td>
                                    <g:applyLayout name="form/textarea">
                                        <g:textArea class="narrow" name="note" rows="5" cols="25" id="note"
                                            disabled="${rechargeTransactionWS.isRefundable.equals(false) || flag.equals(false) }"
                                            value="${StringUtils.hasLength(rechargeTransactionWS.note) ? rechargeTransactionWS.note : ""}" />
                                        <g:hiddenField name="source" value="${rechargeTransactionWS.source}"/>
                                        <g:hiddenField name="userId" value="${rechargeTransactionWS.userId}"/>
                                        <g:hiddenField name="transactionId" value="${rechargeTransactionWS.id}"/>
                                        <g:hiddenField name="subscriberNumber" value="${rechargeTransactionWS.subscriberNumber}"/>
                                        <g:hiddenField name="type" value="${rechargeTransactionWS.type}"/>
                                        <g:hiddenField name="isSimReIssued" value="${rechargeTransactionWS.isSimReIssued}"/>
                                        <g:hiddenField name="isSimIssued" value="${rechargeTransactionWS.isSimIssued}"/>
                                        <g:hiddenField name="isWalletTopUp" value="${rechargeTransactionWS.isWalletTopUp}"/>
                                        <g:hiddenField name="createdBy" value="${rechargeTransactionWS.createdBy}"/>
                                        <g:hiddenField name="transactionDate" value="${rechargeTransactionWS.transactionDate}"/>
                                    </g:applyLayout>
                                </td>
                            </tr>
                        <tbody>
                    </table>
                    </div>
                         <div class="buttons">
                             <ul>
                                  <g:if test= '${rechargeTransactionWS.isRefundable.equals(true) && flag.equals(true) }'>
                                       <li>
                                          <g:actionSubmit class="submit save button-primary" onclick = "return validateNote()" value="${message(code:'recharge.history.button.submit')}" action="cancelRechargeRequest" data-cy="refund"/>
                                      </li>
                                  </g:if>
                                  <li>
                                      <g:if test="${(rechargeTransactionWS?.rechargeAmount != null && rechargeTransactionWS.rechargeAmount > 0) || rechargeTransactionWS.type.equals("Refund")}">
                                          <g:link action="showReceipt" params="[userId: rechargeTransactionWS.userId, transactionId: rechargeTransactionWS.id, receiptType: "Original", isWalletTopUp: rechargeTransactionWS.isWalletTopUp]" class="submit save button-primary" data-cy="originalReceipt">
                                              <span><g:message code="label.show.original.receipt"/></span>
                                          </g:link>
                                      </g:if>
                                  </li>
                                  <g:if test= '${rechargeTransactionWS.getStatus().equals("REFUNDED") || rechargeTransactionWS.getStatus().equals("REVERSED_WALLET_REFUND")}'>
                                      <li>
                                          <g:if test="${(rechargeTransactionWS?.rechargeAmount != null && rechargeTransactionWS?.rechargeAmount > 0) || rechargeTransactionWS.type.equals("Refund")}">
                                              <g:link action="showReceipt" params="[userId: rechargeTransactionWS.userId, transactionId: rechargeTransactionWS.id, receiptType: "Cancelled", isWalletTopUp: rechargeTransactionWS.isWalletTopUp]" class="submit save button-primary" data-cy="cancelledReceipt">
                                                  <span><g:message code="label.show.cancelled.receipt"/></span>
                                              </g:link>
                                          </g:if>
                                      </li>
                                  </g:if>
                             </ul>
                         </div>
                    </div>
                </div>
            </g:form>
        </div>
    </body>
            <script>
                function showErrorMessage(errorField) {
                        $("#error-messages").css("display","block");
                        $("#error-messages ul").css("display","block");
                        $("#error-messages ul").html(errorField);
                        $("html, body").animate({ scrollTop: 0 }, "slow");
                    }
                function validateNote(){
                    let flag=false
                    if($('#note').val()=="") {
                        showErrorMessage("<li><g:message code="recharge.history.refund.note.mandatory"/></li>")
                    }
                    else{
                        flag=true;
                    }
                    return flag;
                }
            </script>
</html>
