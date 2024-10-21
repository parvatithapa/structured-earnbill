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

<%@ page import="com.sapienter.jbilling.server.user.ContactWS; com.sapienter.jbilling.server.user.UserDTOEx; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.user.permisson.db.RoleDTO; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<html>
<head>
    <meta name="layout" content="main" />

    <r:script disposition="head">
        $(document).ready(function() {
            $('#contactType').change(function() {
                var selected = $('#contact-' + $(this).val());
                $(selected).show();
                $('div.contact').not(selected).hide();
            }).change();
        });

        function goBack() {
			window.history.back()
		}
    </r:script>
</head>
<body>
<div class="form-edit">

    <g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>
    <g:set var="fieldsDisabled" value="true"/>
    <g:if test="${isNew}">
        <sec:ifAnyGranted roles="USER_1405">
            <g:set var="fieldsDisabled" value="false"/>
        </sec:ifAnyGranted>
    </g:if>
    <g:else>
        <sec:ifAnyGranted roles="USER_147">
            <g:set var="fieldsDisabled" value="false"/>
        </sec:ifAnyGranted>
    </g:else>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="prompt.new.user"/>
            </g:if>
            <g:else>
                <g:message code="prompt.edit.user"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="user-edit-form" action="save" useToken="true">
            <fieldset>
                <div class="form-columns">

                    <!-- user details column -->
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.customer.number"/></content>

                            <g:if test="${!isNew}">
                                <span>${user.userId}</span>
                            </g:if>
                            <g:else>
                                <em><g:message code="prompt.id.new"/></em>
                            </g:else>

                            <g:hiddenField name="user.userId" value="${user?.userId}"/>
                        </g:applyLayout>

                        <g:if test="${isNew}">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.login.name"/><span id="mandatory-meta-field">*</span></content>
                                <content tag="label.for">user.userName</content>
                                <g:textField class="field" name="user.userName" id="loginName" value="${user?.userName}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.login.name"/></content>

                                ${user?.userName}
                                <g:hiddenField name="user.userName" value="${user?.userName}"/>
                            </g:applyLayout>
                        </g:else>

                        <!-- USER CREDENTIALS -->
                        <g:if test="${isNew}">
                            <g:preferenceEquals preferenceId="${Constants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT}" value="0">
                                <g:applyLayout name="form/checkbox">
                                    <content tag="label"><g:message code="prompt.create.credentials"/></content>
                                    <content tag="label.for">user.createCredentials</content>
                                    <g:checkBox class="cb checkbox" name="user.createCredentials" checked="${user?.createCredentials}"/>
                                </g:applyLayout>
                            </g:preferenceEquals>
                        </g:if>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.status"/></content>
                            <content tag="label.for">user.statusId</content>

                            <g:if test="${params.id}">
                                <g:userStatus name="user.statusId" value="${user?.statusId}" languageId="${session['language_id']}" disabled="${fieldsDisabled}"/>
                            </g:if>
                            <g:else>
                                <g:userStatus name="user.statusId" value="${user?.statusId}" languageId="${session['language_id']}"
                                                                                             disabled="${fieldsDisabled}"/>
                            </g:else>
                            <g:if test="${fieldsDisabled == 'true'}">
	                            <g:hiddenField name="user.statusId" value="${user?.statusId}"/>
                            </g:if>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.language"/></content>
                            <content tag="label.for">user.languageId</content>
                            <g:if test="${isNew}">
                                <sec:ifAnyGranted roles="USER_1405">
                                    <g:select name="user.languageId" from="${LanguageDTO.list(sort : "id",order :"asc")}"
                                              optionKey="id" optionValue="description" value="${user?.languageId}" />
                                </sec:ifAnyGranted>
                            </g:if>
                            <g:else>
                                <sec:ifAnyGranted roles="USER_147">
                                    <g:select name="user.languageId" from="${LanguageDTO.list(sort : "id",order :"asc")}"
                                              optionKey="id" optionValue="description" value="${user?.languageId}"
                                              disabled="${fieldsDisabled}"/>
                                </sec:ifAnyGranted>
                            </g:else>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.role"/></content>
                            <content tag="label.for">user.mainRoleId</content>
                            <g:if test="${isNew}">
                                <sec:ifAnyGranted roles="USER_1405">
                                    <g:select name="user.mainRoleId"
                                              from="${roles}"
                                              optionKey="id"
                                              optionValue="${{ it.getTitle(session['language_id']) }}"
                                              value="${user?.mainRoleId}" />
                                </sec:ifAnyGranted>
                            </g:if>
                            <g:else>
                                <sec:ifAnyGranted roles="USER_147">
                                    <g:select name="user.mainRoleId"
                                              from="${roles}"
                                              optionKey="id"
                                              optionValue="${{ it.getTitle(session['language_id']) }}"
                                              value="${user?.mainRoleId}" disabled="${fieldsDisabled == 'true'  || f == 'myAccount'}"/>
                                    <g:if test="${fieldsDisabled == 'true' || f == 'myAccount'}">
                                        <g:hiddenField name="user.mainRoleId" value="${user?.mainRoleId}"/>
                                    </g:if>
                                </sec:ifAnyGranted>
                            </g:else>
                        </g:applyLayout>

                        <g:set var="isReadOnly" value="true"/>
                        <sec:ifAllGranted roles="CUSTOMER_11">
                            <g:set var="isReadOnly" value="false"/>
                        </sec:ifAllGranted>
                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="user.account.lock"/></content>
                            <content tag="label.for">user.isAccountLocked</content>
                            <g:checkBox class="cb checkbox" name="user.isAccountLocked" checked="${user?.isAccountLocked}" disabled="${isReadOnly}"/>
                        </g:applyLayout>
                        <g:set var="isReadOnly" value="true"/>
                        <sec:ifAllGranted roles="CUSTOMER_11">
                            <g:set var="isReadOnly" value="false"/>
                        </sec:ifAllGranted>
                        <g:applyLayout name="form/checkbox">
                        <content tag="label"><g:message code="prompt.user.inactive"/></content>
                        <content tag="label.for">user.accountExpired</content>
                        <g:checkBox class="cb checkbox" name="user.accountExpired" checked="${user?.accountDisabledDate}" disabled="${isReadOnly}"/>
                        </g:applyLayout>

                            <sec:ifAllGranted roles="USER_158">
                                <!-- meta fields -->
                                <div id="user-metafields">
                                    <g:render template="/metaFields/editMetaFields" model="[availableFields: availableFields, fieldValues: user?.metaFields]"/>
                                </div>
                                <g:if test="${availableFields.size()>0 && companyInfoTypes.size() > 0}">
                                    <g:applyLayout name="form/select">
                                        <content tag="label"><g:message code="prompt.idp.configurations"/></content>
                                        <content tag="label.for">idpConfigurationIds</content>
                                        <content tag="include.script">true</content>
                                        <g:select id="idp-configuration-select" name="idpConfigurationIds"
                                                  from="${companyInfoTypes}"
                                                  optionKey="id"
                                                  optionValue="name"
                                                  value="${defaultIdp}"/>
                                    </g:applyLayout>
                                </g:if>

                            </sec:ifAllGranted>

                    </div>

                    <!-- contact information column -->
                    <div class="column">
                        <g:set var="contact" value="${contacts && contacts.size()>0 ? contacts[0] : new ContactWS()}"/>
                        <g:render template="/user/contact" model="[contact: contact]"/>
                        <br/>&nbsp;
                    </div>

                    <div class="buttons">
                        <ul>
                            <li>
                                <a onclick="submitUserEditForm()" class="submit save button-primary" data-cy="saveChanges"><span><g:message code="button.save"/></span></a>
                            </li>
                            <li>
                                <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                            </li>
                        </ul>
                    </div>
                </div>

            </fieldset>
        </g:form>
    </div>
</div>
<div id="role-change-dialog" title="${message(code: 'popup.confirm.title')}">
    <table>
        <tbody>
        <tr>
            <td valign="top">
                <img src="${resource(dir:'images', file:'icon34.gif')}" alt="confirm">
            </td>
            <td id="roleChangeMsg" class="col2">
            </td>
        </tr>
        </tbody>
    </table>
</div>
<div id="roleChangeMsgTemplate" style="display: none;">
    <g:message code="user.role.change" /><br/>
    <p><g:message code="user.role.change.proceed" /></p>
</div>

<script type="text/javascript">
    $(document).ready(function(){
        $('#role-change-dialog').dialog({
            autoOpen: false,
            height: 200,
            width: 375,
            modal: true,
            buttons: {
                '<g:message code="prompt.yes"/>': function() {
                    $('#user-edit-form').submit();
                    $(this).dialog('close');
                },
                '<g:message code="prompt.no"/>': function() {
                    $(this).dialog('close');
                }
            }
        });
    });

    function showErrorMessage(errorField) {
            $("#error-messages").css("display","block");
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html(errorField);
            $("html, body").animate({ scrollTop: 0 }, "slow");
    }

    function submitUserEditForm() {
        //validation for login name field
        if (${isNew}) {
            var loginName = document.getElementById('loginName').value;
            if (loginName !== loginName.trim()) {
                showErrorMessage("<li><g:message code="login.name.whitespace.error"/></li>");
                return false;
            } else if (loginName === '') {
                showErrorMessage("<li><g:message code="login.name.blank.error"/></li>");
                return false;
            }
        }

        if(${user?.id?: 'null'} == null || ${user?.mainRoleId ?: 'null'} == $('#user\\.mainRoleId').val()) {
            $('#user-edit-form').submit();
        } else {
            var roleName = $("#user\\.mainRoleId option:selected" ).text();
            $('#roleChangeMsg').html($('#roleChangeMsgTemplate').html().replace(/_0_/g, roleName));
            $('#role-change-dialog').dialog('open');
        }
    }

</script>
</body>
</html>
