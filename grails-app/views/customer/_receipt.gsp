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

<div id="${Math.random().toString().substring(2)}" class="receipt" style="display:flow-root;">
    <table id="cssTable">
        <tr>
            <td>
                <div class="common">
                    <table class="commonTable">
                        <tr>
                            <th><div id="labelReceiptNumber" data-cy="labelReceiptNumber${receiptNumber}"><g:message code="receipt.number"/></div></th>
                            <td><div id="valueReceiptNumber" data-cy="valueReceiptNumber${receiptNumber}">${receiptWS?.receiptNumber}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelReceiptDate" data-cy="labelReceiptDate${receiptNumber}"><g:message code="receipt.date"/></div></th>
                            <td><div id="valueReceiptDate" data-cy="valueReceiptDate${receiptNumber}"><g:formatDate date="${Date.from(LocalDateTime.parse(receiptWS?.receiptDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                                                                              .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/></div>
                           </td>
                        </tr>
                        <tr>
                            <th><div id="labelCustAccNum" data-cy="labelCustAccNum${receiptNumber}"><g:message code="receipt.account.number"/></th>
                            <td><div id="valueCustAccNum" data-cy="valueCustAccNum${receiptNumber}">${receiptWS?.userId}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelSubDialNum" data-cy="labelSubDialNum${receiptNumber}"><g:message code="subscriber.dial.number"/></div></th>
                            <td><div id="valueSubDialNum" data-cy="valueSubDialNum${receiptNumber}">${receiptWS?.subscriberNumber}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelReceiptType" data-cy="labelReceiptType${receiptNumber}"><g:message code="receipent.type"/></div></th>
                            <td><div id="valueReceiptType" data-cy="valueReceiptType${receiptNumber}">${receiptWS?.receiptType}</div></td>

                        </tr>
                    </table>
                </div>
            </td>
            <td>
                <div class="common">
                    <table class="commonTable" >
                        <tr>
                            <th><div id="labelSubName" data-cy="labelSubName${receiptNumber}"><g:message code="subscriber.name"/></div></th>
                            <td><div id="valueSubName" class="charge-receipt" data-cy="valueSubName${receiptNumber}">${receiptWS?.userName}</div></td>
                        </tr>
                        <tr>
                            <th><div id="labelAddress" data-cy="labelAddress${receiptNumber}"><g:message code="subscriber.address"/></div></th>
                            <td><div id="valueAddress" data-cy="valueAddress${receiptNumber}">${receiptWS?.address}</div></td>
                        </tr>
                        <tr>
                            <th><div id="labelContactNumber" data-cy="labelContactNumber${receiptNumber}"><g:message code="subscriber.contact.number"/></div></th>
                            <td><div id="valueContactNumber" data-cy="valueContactNumber${receiptNumber}">${receiptWS?.contactNumber}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelEmail" data-cy="labelEmail${receiptNumber}"><g:message code="subscriber.email.address"/></div></th>
                            <td><div id="valueEmail" data-cy="valueEmail${receiptNumber}">${receiptWS?.email}</div></td>

                        </tr>
                        <tr>
                            <td> </td>
                            <th> </th>
                        </tr>
                    </table>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="common">
                    <table class="commonTable">
                        <tr>
                            <th><div id="labelPaymentAmount" data-cy="labelPaymentAmount${receiptNumber}"><g:message code="receipt.amount.in.cash"/></div></th>
                            <td><div id="valuePaymentAmount" data-cy="valuePaymentAmount${receiptNumber}">${cashAmount}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelOperationType" data-cy="labelOperationType${receiptNumber}"><g:message code="operation.type"/></div></th>
                            <td><div id="valueOperationType" data-cy="valueOperationType${receiptNumber}">${paymentType}</div></td>

                        </tr>
                    </table>
                </div>
            </td>
            <td>
                <div class="common">
                    <table class="commonTable">
                        <tr>
                            <g:if test="${receiptWS?.receiptType.equals("Pre-Paid")}">
                                <th><div id="labelWalletAmount" data-cy="labelWalletAmount${receiptNumber}"><g:message code="receipt.amount.from.wallet"/></div></th>
                            </g:if>
                            <g:else>
                                <th><div id="labelWalletAmount" data-cy="labelWalletAmount${receiptNumber}"><g:message code="receipt.amount.to.wallet"/></div></th>
                            </g:else>
                            <td><div id="valueWalletAmount" data-cy="valueWalletAmount${receiptNumber}">${walletAmount ?: BigDecimal.ZERO}</div></td>

                        </tr>
                        <tr>
                            <th><div id="labelActualPlanPrice" data-cy="labelActualPlanPrice${receiptNumber}"><g:message code="receipt.actual.price"/></div></th>
                            <td><div id="valueActualPlanPrice" data-cy="valueActualPlanPrice${receiptNumber}">${actualAmount ?: cashAmount}</div></td>

                        </tr>
                    </table>
                </div>
            </td>
        </tr>
    </table>
    <table id="cssTable2">
        <tr>
            <td>
                <div>
                    <table>
                        <tr>
                            <th><div id="labelCollectorName" data-cy="labelCollectorName${receiptNumber}"><g:message code="user.name"/></div></th>
                        </tr>
                        <tr>
                            <td>
                                <div id="valueCollectorName" data-cy="valueCollectorName${receiptNumber}">
                                    <g:if test="${receiptWS?.createdBy?.indexOf(":") >= 0}">
                                        ${receiptWS?.createdBy.substring(receiptWS.createdBy.indexOf(":")+1)}
                                    </g:if>
                                    <g:else>
                                        ${receiptWS?.createdBy}
                                    </g:else>
                                </div>
                            </td>
                        </tr>
                    </table>
                </div>
            </td>
        </tr>
    </table>
</div>
