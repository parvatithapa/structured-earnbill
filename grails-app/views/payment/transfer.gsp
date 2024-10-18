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

<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.util.db.CountryDTO; com.sapienter.jbilling.common.Constants" contentType="text/html;charset=UTF-8" %>

<g:set var="isNew" value="${!payment || !payment?.id || payment?.id == 0}"/>

<html>
<head>
    <meta name="layout" content="main"/>
</head>
<body>
<div class="form-edit">

    <div class="heading">
        <strong>
            <g:message code="payment.transfer.payment.title"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="payment-transfer-form" action="confirmTransfer">
            <fieldset>
                <div class="box-card-hold">
                    <div class="form-columns">
                        <div class="column">
                            <g:hiddenField name="bank.id" value="${bank?.id}"/>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.payment.transfer.id"/></content>

                                ${payment?.id}
                                <g:hiddenField name="payment.id" value="${payment?.id}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.payment.transfer.user"/></content>

                                ${user?.userName}
                                <g:hiddenField name="user.userName" value="${user?.userName}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.payment.transfer.user.id"/></content>

                                ${payment?.userId}
                                <g:hiddenField name="payment.userId" value="${payment?.userId}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.payment.transfer.to.user.id"/></content>
                                <content tag="label.for">toUserId</content>
                                <g:textField class="field" name="toUserId"/>
                            </g:applyLayout>

                        </div>
                    </div>
                </div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="$('#payment-transfer-form').submit()" class="submit payment">
                                <span><g:message code="button.transfer.payment"/></span>
                            </a>
                        </li>
                        <li>
                            <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>
</div>
</body>
</html>