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

%{-- <%@ page import="com.sapienter.jbilling.server.user.UserDTOEx; com.sapienter.jbilling.server.user.contact.db.ContactTypeDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.user.permisson.db.RoleDTO; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.util.db.LanguageDTO" %> --}%
<html>
<head>
    <meta name="layout" content="public" />
    <r:script disposition="head">
        $(document).ready(function () {
            $('input[name="newPassword"]').focus();
        });
    </r:script>
</head>
<body>
<div id="reset_password" class="form-edit">

    <g:render template="/layouts/includes/messages"/>
    <div class="heading">
        <strong>
            <g:message code="login.reset.password.title"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="user-password-update-form" action="updatePassword" useToken="true">
            <fieldset>
                <div class="form-columns">

                    <g:hiddenField name="token" value="${token}"/>
                    <!-- user details column -->

                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="prompt.password"/></content>
                        <content tag="label.for">newPassword</content>
                        <g:passwordField class="field" name="newPassword"/>
                    </g:applyLayout>

                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="prompt.verify.password"/></content>
                        <content tag="label.for">confirmedNewPassword</content>
                        <g:passwordField class="field" name="confirmedNewPassword"/>
                    </g:applyLayout>

                </div>
                <br/>

                <div class="buttons">
                    <a onclick="$('#user-password-update-form').submit()" class="submit save button-primary"><span><g:message code="button.reset"/></span></a>
                </div>

            </fieldset>
        </g:form>
    </div>
</div>
</body>
</html>
