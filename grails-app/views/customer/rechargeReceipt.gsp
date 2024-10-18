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

<%@ page import="java.util.Date;" %>

<html>
<head>
    <meta name="layout" content="main"/>
    <r:external file="js/form.js"/>
    <r:external file="js/receipt.js"/>
    <style>
    #cssTable{margin-left: auto; margin-right: auto; width: 100%; height: 100%;}
    #cssTable2{float: right;}
    html[dir="rtl"] #cssTable2{float: left;}
    .common{border: 1px solid gray; padding: 10px; border-radius: 10px;}
    th, td{padding: 10px; text-align: left;}
    html[dir="rtl"] th, html[dir="rtl"]  td{text-align: right;}
    .commonTable{margin-right: 0; margin-left: auto; width: 100%;}
    html[dir="rtl"] .commonTable {margin-right: auto; margin-left: 0;}
    td{width: 55%; word-break: break-word;}
    th{width: 45%;}
    .heading{text-align: left;}
    html[dir="rtl"] .heading{text-align: right;}
    </style>
</head>
<body>
    <div class="form-edit">
        <div class="heading">
            <strong>
                <g:message code="recharge.receipt"/>
            </strong>
        </div>
        <div class="form-hold">
            <g:form name="user-edit-form" >
                <fieldset>
                   <g:set var="receiptNumber" value="${0}" />
                    <g:if test="${receiptWS?.primaryPlanWS?.cashPrice == null && receiptWS?.totalReceiptAmount != null}">
                        <g:render template="/customer/receipt" model="[receiptWS : receiptWS, cashAmount: "${String.format("%.2f",receiptWS?.totalReceiptAmount)}", paymentType: "${receiptWS?.operationType}", receiptNumber: receiptNumber + 1 ]"/>
                        <g:set var="receiptNumber" value="${receiptNumber + 1}" />
                    </g:if>
                    <g:elseif test="${receiptWS?.primaryPlanWS?.cashPrice > 0}">
                        <g:render template="/customer/receipt" model="[receiptWS : receiptWS,
                                                                       walletAmount : "${String.format("%.2f",receiptWS?.primaryPlanWS?.walletPrice)}",
                                                                       cashAmount: "${String.format("%.2f",receiptWS?.primaryPlanWS?.cashPrice)}",
                                                                       actualAmount : "${String.format("%.2f",receiptWS?.primaryPlanWS?.price)}",
                                                                       paymentType: "${receiptWS?.operationType}",
                                                                        receiptNumber: receiptNumber + 1]"/>
                        <g:set var="receiptNumber" value="${receiptNumber + 1}" />
                    </g:elseif>
                    <g:each var="addOnProduct" in="${receiptWS?.addOnProductWS}" >
                        <g:render template="/customer/receipt" model="[receiptWS : receiptWS, cashAmount: "${String.format("%.2f",addOnProduct?.price)}", paymentType: "${addOnProduct?.name}",receiptNumber: receiptNumber + 1 ]"/>
                        <g:set var="receiptNumber" value="${receiptNumber + 1}" />
                    </g:each>
                    <g:each var="feeWS" in="${receiptWS?.feeWSList}" >
                        <g:render template="/customer/receipt" model="[receiptWS : receiptWS, cashAmount: "${String.format("%.2f",feeWS?.amount)}", paymentType: "${feeWS?.description}", receiptNumber: receiptNumber + 1 ]"/>
                        <g:set var="receiptNumber" value="${receiptNumber + 1}" />
                    </g:each>
                    <g:if test="${receiptWS?.topUpAmount != null && receiptWS?.topUpAmount > 0}">
                        <g:render template="/customer/receipt" model="[receiptWS : receiptWS, cashAmount: "${String.format("%.2f",receiptWS?.topUpAmount)}", paymentType: "${ receiptWS?.receiptType.equals(message(code:'receipt.operation.type.refund')) ? message(code:'receipt.operation.type.refund') : message(code:'receipt.operation.type.top.up') }", receiptNumber: receiptNumber + 1 ]"/>
                        <g:set var="receiptNumber" value="${receiptNumber + 1}" />
                    </g:if>

                    <div class="buttons">
                        <ul>
                            <li>
                                <a onclick="printReceipt()"
                                   class="submit save button-primary"><span><g:message code="receipt.print"/></span></a>
                            </li>
                            <li>
                                <g:link action="list" class="submit cancel" data-cy="closeButton"><span><g:message
                                        code="button.close"/></span></g:link>
                            </li>

                        </ul>
                    </div>
                </fieldset>
            </g:form>
        </div>
    </div>
</body>
</html>
