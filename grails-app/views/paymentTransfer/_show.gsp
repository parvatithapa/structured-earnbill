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

<%@ page import="com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>

<%--
  Shows details of a selected payment transfer.

  @author Ashok Kale
  @since 11-Feb-2015
--%>

<g:set var="customer" value="${selected?.payment?.baseUser?.customer}"/>
<g:set var="contact" value="${ContactDTO.findByUserId(selected?.payment?.baseUser?.id)}"/>

<div class="column-hold">
    <div class="heading">
        <strong>
<g:message code="payment.transfer.payment.title"/>
<em>${selected?.id}</em>
<g:if test="${selected?.deleted}">
    <span style="color: #ff0000;">(<g:message code="object.deleted.title"/>)</span>
</g:if>
</strong>
</div>

<div class="box">
    <div class="sub-box">
        <!-- user details -->
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <g:if test="${contact?.firstName || contact?.lastName}">
                <tr>
                    <td><g:message code="prompt.customer.name"/></td>
                    <td class="value">${contact.firstName} ${contact.lastName}</td>
                </tr>
            </g:if>

            <g:if test="${contact?.organizationName}">
                <tr>
                    <td><g:message code="prompt.organization.name"/></td>
                    <td class="value">${contact.organizationName}</td>
                </tr>
            </g:if>
            <tr>
                <td><g:message code="payment.transfer.from.userId"/></td>
                <td class="value">
                    <sec:access url="/customer/show">
                        <g:remoteLink controller="customer" action="show" id="${selected?.fromUserId}" before="register(this);" onSuccess="render(data, next);">
                            ${selected?.fromUserId}
                        </g:remoteLink>
                    </sec:access>
                    <sec:noAccess url="/customer/show">
                        ${selected?.fromUserId}
                    </sec:noAccess>
                </td>
            </tr>
            <tr>
                <td><g:message code="payment.transfer.to.userId"/></td>
                <td class="value">
                    <sec:access url="/customer/show">
                        <g:remoteLink controller="customer" action="show" id="${selected?.toUserId}" before="register(this);" onSuccess="render(data, next);">
                            ${selected?.toUserId}
                        </g:remoteLink>
                    </sec:access>
                    <sec:noAccess url="/customer/show">
                        ${selected?.toUserId}
                    </sec:noAccess>
                </td>
            </tr>
            <tr>
                <td><g:message code="payment.label.user.name"/></td>
                <td class="value">${selected?.payment?.baseUser?.userName}</td>
            </tr>
            <tr>
                <td><g:message code="payment.transfer.amount"/></td>
                <td class="value">
                    <g:formatNumber number="${selected?.amount}" type="currency" currencySymbol="${selected?.payment?.currency?.symbol}"/>
                </td>
            </tr>
            <tr>
                <td><g:message code="payment.transfer.createDatetime"/></td>
                <td class="value"><g:formatDate date="${selected?.createDatetime}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/></td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
<!-- payment notes -->
<g:if test="${selected?.paymentTransferNotes}">
    <div class="heading">
        <strong><g:message code="payment.transfer.notes"/></strong>
    </div>
    <div class="box">
        <div class="sub-box"><p>${selected?.paymentTransferNotes}</p></div>
    </div>
</g:if>