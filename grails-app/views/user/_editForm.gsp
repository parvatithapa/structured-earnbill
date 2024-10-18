%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
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

<div class="form-edit">

    <g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>
    <g:set var="canNotEditSelf" value="${true}"/>
    <g:set var="contactNotEditable" value="${notEditable? notEditable : false}"/>
    <sec:ifAllGranted roles="USER_1401">
        <g:set var="canNotEditSelf" value="${false}"/>
    </sec:ifAllGranted>

    <div class="heading">
        <strong>
            <g:message code="prompt.edit.user"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="user-edit-form" controller="myAccount" action="save" useToken="true">
            <fieldset>
                <jB:flow/>
                <div class="form-columns">
                    <!-- user details column -->
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.customer.number"/></content>
                            <span>${user.userId}</span>
                            <g:hiddenField name="user.userId" value="${user?.userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.login.name"/></content>

                            ${user?.userName}
                            <g:hiddenField name="user.userName" value="${user?.userName}"/>
                        </g:applyLayout>

                        <sec:ifAnyGranted roles="MY_ACCOUNT_161">
                            <g:if test="${isUserSSOEnabled && ssoActive}">
                                <g:applyLayout name="form/text">
                                    <content tag="label">
                                        <g:message code="prompt.sso.user.password"/>
                                    </content>
                                    <a href="${resetPasswordUrl}" target="_blank">Update Password on IDP</a>
                                </g:applyLayout>
                            </g:if>
                            <g:else>
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="prompt.current.password"/></content>
                                    <content tag="label.for">oldPassword</content>
                                    <g:passwordField class="field" name="oldPassword"/>
                                </g:applyLayout>

                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="prompt.password"/></content>
                                    <content tag="label.for">newPassword</content>
                                    <g:passwordField class="field" name="newPassword"/>
                                </g:applyLayout>

                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="prompt.verify.password"/></content>
                                    <content tag="label.for">verifiedPassword</content>
                                    <g:passwordField class="field" name="verifiedPassword"/>
                                </g:applyLayout>
                            </g:else>
                        </sec:ifAnyGranted>

                        <g:if test="${ssoActive}">
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
                        </g:if>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.status"/></content>
                            <content tag="label.for">user.statusId</content>
                            <g:userStatus name="user.statusId" value="${user?.statusId}" languageId="${session['language_id']}" disabled="${canNotEditSelf || contactNotEditable}"/>
                            <g:if test="${canNotEditSelf}">
                                <g:hiddenField name="user.statusId" value="${user?.statusId}"/>
                            </g:if>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.language"/></content>
                            <content tag="label.for">user.languageId</content>
                            <g:select name="user.languageId" from="${LanguageDTO.list(sort : "id",order :"asc")}"
                                      optionKey="id" optionValue="description" value="${user?.languageId}"
                                      disabled="${canNotEditSelf || contactNotEditable}"/>
                            <g:if test="${canNotEditSelf && contactEditable}">
                                <g:hiddenField name="user.languageId" value="${user?.languageId}"/>
                            </g:if>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.role"/></content>
                            <content tag="label.for">user.mainRoleId</content>
                            <g:select name="user.mainRoleId"
                                      from="${roles}"
                                      optionKey="id"
                                      optionValue="${{ it.getTitle(session['language_id']) }}"
                                      value="${user?.mainRoleId}" disabled="${canNotEditSelf || contactNotEditable}"/>
                            <g:if test="${canNotEditSelf || contactNotEditable}">
                                <g:hiddenField name="user.mainRoleId" value="${user?.mainRoleId}"/>
                            </g:if>
                        </g:applyLayout>
                    </div>

                    <!-- contact information column -->
                    <div class="column">
                        <g:set var="contact" value="${contacts && contacts.size() > 0 ? contacts[0] : new ContactWS()}"/>
                        <g:render template="/customer/contact" model="[contact: contact, hideFields : true, contactNotEditable: contactNotEditable]"/>
                        <br/>&nbsp;
                    </div>
                </div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="$('#user-edit-form').submit()" class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                        </li>
                        <li>
                            <g:link action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>
            </fieldset>
        </g:form>
    </div>
</div>
