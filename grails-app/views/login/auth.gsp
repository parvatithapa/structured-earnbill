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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<html>
<head>
    <meta name="layout" content="public" />

    <title><g:message code="login.page.title"/> | <g:defaultTitle/></title>
    <r:require modules="jquery-validate"/>

    <r:script disposition="head">
        $(document).ready(function() {
            $('#login input[name="j_username"]').focus();
            $(document).keypress(function(e) {
                if(e.which == 13) {
                    $(this).blur();
                    $('#submitLink').focus().click();
                }
            });

            $("#login-form-sso").validate({
                // Enable validation on specific hidden field. By default hidden fields are not validated.
                ignore: "not:hidden('#j_client_id')",
                // Validation rules
                rules: {
                    j_client_id: {
                        required: true,
                        digits: true
                    }
                },
                // Validation messages
                messages: {
                    j_client_id: {
                        required: '${message(code: 'login.sso.required.error')}',
                          digits: '${message(code: 'login.field.number.error')}'
                    }
                },
                // Handles all invalid submit actions
                invalidHandler: function(event, validator){
                    clearFields();
                    if (validator.errorList.length > 0) {
                        for (var i = 0, l = validator.errorList.length; i < l; i++ ) {
                            validator.errorList[i].element.parentElement.style.setProperty('border-color', 'red');
                            $('span[name="sso_' + validator.errorList[i].element.name + '"]').html(validator.errorList[i].message);
                        }
                        validator.errorList[0].element.focus();
                    }
                },
                // Used to remove default error labels
                errorPlacement: function(error, element){}
            });
            $("#login-form").validate({
                // Enable validation on specific hidden field. By default hidden fields are not validated.
                ignore: "not:hidden('#j_client_id')",
                // Validation rules
                rules: {
                    j_username: {
                        required: true
                    },
                    j_password: {
                        required: true
                    },
                    j_client_id: {
                        required: true,
                        digits: true
                    }
                },
                // Validation messages
                messages: {
                    j_username: {
                        required: '${message(code: 'login.field.username.required.error')}'
                    },
                    j_password: {
                        required: '${message(code: 'login.field.password.required.error')}'
                    },
                    j_client_id: {
                        required: '${message(code: 'login.field.company.required.error')}',
                          digits: '${message(code: 'login.field.number.error')}'
                    }
                },
                // Handles all invalid submit actions
                invalidHandler: function(event, validator){
                    clearFields();
                    if (validator.errorList.length > 0) {
                        for (var i = 0, l = validator.errorList.length; i < l; i++ ) {
                            validator.errorList[i].element.parentElement.style.setProperty('border-color', 'red');
                            $('span[name="error_' + validator.errorList[i].element.name + '"]').html(validator.errorList[i].message);
                        }
                        validator.errorList[0].element.focus();
                    }
                },
                // Used to remove default error labels
                errorPlacement: function(error, element){}
            });
        });

        function clearFields(){
            $('.inp-bg').css('border-color', '#cbcbcb');
            $('.error-validate-login').html('');
        }
    </r:script>
</head>
<body>

    <g:render template="/layouts/includes/messages"/>

    <div id="login" class="form-edit">
        <div class="heading">
            <strong><g:message code="login.prompt.title"/></strong>
        </div>
        <div class="form-hold">
            <div class="form-columns">
                <div class="column" id="login-column-1">
                    <form method='POST' id='login-form-sso' autocomplete='off'>

                        <g:hiddenField name="interactive_login" value="true"/>

                        <fieldset>
                            <div class="form-columns login">
                                <br><br><br><br>

                                <div class="center-align" style="margin-bottom: 3px;">
                                    <g:message code="login.sso.label"/>
                                </div>

                                <g:if test="${companyId}">
                                    <div class="center-align">
                                        <g:hiddenField name="j_client_id" value="${companyId}"/>
                                    </div>
                                </g:if>
                                <g:else>
                                    <g:applyLayout name="form/input">
                                        <content tag="label">
                                            <g:message code="login.prompt.client.id"/>
                                            <span class="error-validate-login" name="sso_j_client_id"/>
                                        </content>
                                        <content tag="label.for">j_client_id</content>
                                        <g:textField class="field" name="j_client_id" />
                                    </g:applyLayout>
                                </g:else>
                                <br><br><br><br><br>
                            </div>
                            <div class="buttons">
                                <ul style="color: transparent">
                                    <li>
                                        <input id="ssoSubmitLink" type="submit" formaction="${request.contextPath}/login/authenticateViaIdp" value="<g:message code="login.sso.button.submit"/>" class="submit button-primary">
                                    </li>
                                </ul>
                            </div>
                        </fieldset>
                    </form>
                </div>
                <div class="column" id="login-column-3">
                    <form method='POST' id='login-form' autocomplete='off'>
                        <g:hiddenField name="interactive_login" value="true"/>
                        <fieldset>
                            <div class="form-columns login">
                                <g:applyLayout name="form/input">
                                    <content tag="label">
                                        <g:message code="login.prompt.username"/>
                                        <span class="error-validate-login" name="error_j_username"/>
                                    </content>
                                    <content tag="label.for">username</content>
                                    <g:textField class="field" name="j_username" value="${params.userName}"/>
                                </g:applyLayout>

                                <g:applyLayout name="form/input">
                                    <content tag="label">
                                        <g:message code="login.prompt.password"/>
                                        <span class="error-validate-login" name="error_j_password"/>
                                    </content>
                                    <content tag="label.for">password</content>
                                    <g:passwordField class="field" name="j_password"/>
                                </g:applyLayout>

                                <g:if test="${companyId}">
                                    <div class="center-align">
                                        <g:hiddenField name="j_client_id" value="${companyId}"/>
                                    </div>
                                </g:if>
                                <g:else>
                                    <g:applyLayout name="form/input">
                                        <content tag="label">
                                            <g:message code="login.prompt.client.id"/>
                                            <span class="error-validate-login" name="error_j_client_id"/>
                                        </content>
                                        <content tag="label.for">j_client_id</content>
                                        <g:textField class="field" name="j_client_id" />
                                    </g:applyLayout>
                                </g:else>

                                <g:applyLayout name="form/text">
                                    <content tag="label">&nbsp;</content>
                                    <content tag="label.row.class">no-label</content>
                                    <g:link controller="resetPassword" class="a-small"><g:message code="login.prompt.forgotPassword" /></g:link>
                                </g:applyLayout>
                            </div>
                            <div class="buttons">
                                <ul style="color: transparent">
                                    <li>
                                        <input id="submitLink" type="submit" formaction="${postUrl}" value="<g:message code="login.button.submit"/>"
                                               class="submit button-primary">
                                    </li>
                                </ul>
                            </div>
                        </fieldset>
                    </form>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
