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
<html>
<head>
    <meta name="layout" content="main"/>
    <r:external file="js/form.js"/>
    <link type="text/css" href="${resource(file: '/css/adennet/icons/font-awesome.min.css')}" rel="stylesheet"/>
</head>

<body>
<div class="form-edit">

    <div class="heading">
        <strong data-cy="rechargeRequestTitle">
            <g:message code="recharge.request.hold"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="recharge-request-form" useToken="true">
            <fieldset>

                <div class="sub-box">
                    <g:message code="cancel.recharge.message"/>
                    <div class="table-box tab-table">
                        <div class="table-scroll">
                        <g:hiddenField name="userId" value="${userId}"/>
                            <table id="walletTransactions" cellspacing="0" cellpadding="0">
                                <thead>
                                    <tr>
                                        <th class="header-sortable">
                                          <g:message code="user.subscriber.number"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.request.plan.description"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.request.plan.price"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.date"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.amount"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.total.amount"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.source"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.created"/>
                                        </th>
                                        <th class="header-sortable">
                                          <g:message code="recharge.request.status"/>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <g:each var="data" in="${advanceRechargeResponseWS.getData()}" >
                                        <tr>
                                            <td>
                                                ${data?.subscriberNumber}
                                            </td>
                                            <td style="word-break: break-all;" data-cy="planDescription">
                                                ${data?.planDescription}
                                            </td>
                                            <td data-cy="planPrice">
                                                <g:formatNumber number="${data?.planPrice}" type="currency"  currencySymbol="${currencySymbol}"/>
                                            </td>
                                            <td>
                                                <g:formatDate date="${Date.from(LocalDateTime.parse(data?.rechargeDate)
                                                                                                   .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>

                                            </td>
                                            <td data-cy="rechargeAmount">
                                                <g:formatNumber number="${data?.rechargeAmount}" type="currency"  currencySymbol="${currencySymbol}"/>
                                            </td>
                                            <td data-cy="totalRechargeAmount">
                                                <g:formatNumber number="${data?.totalRechargeAmount}" type="currency"  currencySymbol="${currencySymbol}"/>
                                            </td>
                                            <td>
                                                ${data?.source}
                                            </td>
                                            <td>
                                                ${data?.createdBy}
                                            </td>
                                            <td style="word-break: break-all;" data-cy="status">
                                                ${data?.status}
                                            </td>
                                        </tr>
                                    </g:each>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                <div><br/></div>
                <div class="buttons">
                    <ul>
                        <li>
                            <g:link action="list" class="submit cancel"><span><g:message
                                    code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>
            </fieldset>
        </g:form>
    </div>
</div>
</body>
</html>
