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
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>

<%@ page import="com.sapienter.jbilling.common.Util; com.sapienter.jbilling.server.user.db.CompanyDTO" %>

<head>
    <meta name="layout" content="public"/>

    <title><g:message code="login.page.title"/></title>
    <script src="https://www.google.com/recaptcha/enterprise.js" async defer></script>

    <r:script disposition="head">
        var RecaptchaOptions = {
            theme:'white'
        };

        $(document).ready(function () {
            $('#reset_password input[name="email"]').focus();

            $(document).keypress(function (e) {
                if (e.which == 13) {

                    $(this).blur();
                    $('#reset_password form').submit();
                }
            });
        });

    </r:script>

    <style type="text/css">
    #recaptcha_widget_div label {
        float: none;
    }

    #recaptcha_widget_div a img {
        top: 0px;
        left: 0px;
    }

    #recaptcha_widget_div span {
        font-weight: normal;
        line-height: 0 !important;
    }

    #recaptcha_widget_div {
        margin-left: 85px;
        margin-top: 12px;
    }
    </style>
</head>

<body>

<g:render template="/layouts/includes/messages"/>

<div id="reset_password" class="form-edit">
    <div class="heading">
        <strong><g:message code="forgotPassword.prompt.title"/></strong>
    </div>

    <div class="form-hold">
        <g:form controller="resetPassword" action="captcha" useToken="true">
            <fieldset>

                <div class="form-columns">

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="forgotPassword.prompt.email"/></content>
                            <content tag="label.for">email</content>
                            <g:textField class="field" name="email" value="${email}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="forgotPassword.prompt.userName"/></content>
                            <content tag="label.for">userName</content>
                            <g:textField class="field" name="userName" value="${userName}"/>
                        </g:applyLayout>
                    <g:if test="${!grailsApplication.config.useUniqueLoginName}">
                    <g:if test="${companyId}">
                        <div class="center-align">
                            <g:hiddenField name="companyId" value="${companyId}"/>
                        </div>
                    </g:if>
                    <g:else>
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="login.prompt.client.id"/></content>
                            <content tag="label.for">companyId</content>
                            <g:textField class="field" name="companyId" value="${companyId}"/>
                        </g:applyLayout>
                    </g:else>
                    </g:if>
                    <g:else>
                    <g:if test="${companyId}">
                            <g:hiddenField name="companyId" value="${companyId}"/>
                    </g:if>
                    </g:else>
                    <br/>
                    <div align="center">
                        <recaptcha:ifEnabled>
                            <recaptcha:recaptcha/>
                            <recaptcha:ifFailed>CAPTCHA Failed: ${session["recaptcha_error"]}</recaptcha:ifFailed>
                        </recaptcha:ifEnabled>
                    </div>
                    <br/>
                </div>

                <div class="buttons">
                    <a href="#" class="submit button-primary" onclick="$('#reset_password form').submit();">
                        <span><g:message code="forgotPassword.button.submit"/></span>
                    </a>
                </div>
            </fieldset>
        </g:form>
    </div>
</div>

</body>
</html>
