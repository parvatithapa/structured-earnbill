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

<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO; grails.plugin.springsecurity.SpringSecurityUtils" contentType="text/html;charset=UTF-8" %>

<%--
  Shows an internal user.

  @author Brian Cowdery
  @since  04-Apr-2011
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            <g:if test="${contact?.firstName || contact?.lastName}">
                ${String.valueOf(contact?.firstName + " " + contact?.lastName).trim()}
            </g:if>
            <g:else>
                ${selected.userName}
            </g:else>
            <em><g:if test="${contact}">${contact.organizationName}</g:if></em>
        </strong>
    </div>

    <!-- user details -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="customer.detail.user.user.id"/></td>
                    <td class="value" data-cy="userId">${selected.id}</td>
                </tr>
                <tr>
                    <td><g:message code="customer.detail.user.username"/></td>
                    <td class="value" data-cy="userLoginName">
                        ${selected.userName}
                    </td>
                </tr>
                <tr>
                    <td><g:message code="customer.detail.user.status"/></td>
                    <td class="value">${selected.userStatus.description}</td>
                </tr>
                <tr>
                    <td><g:message code="user.language"/></td>
                    <td class="value">${selected.language.getDescription()}</td>
                </tr>

                <tr>
                    <td><g:message code="customer.detail.user.created.date"/></td>
                    <td class="value"><g:formatDate date="${selected.createDatetime}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/></td>
                </tr>
                <tr>
                    <td><g:message code="user.last.login"/></td>
                    <td class="value"><g:formatDate date="${selected.lastLogin}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/></td>
                </tr>
                <tr>
                    <td><g:message code="user.locked"/></td>
                    <td class="value"><g:formatBoolean boolean="${selected.isAccountLocked()}" true="Yes" false="No"/></td>
                </tr>
                <tr>
                    <td><g:message code="user.inactive"/></td>
                    <td class="value">${selected.accountDisabledDate != null ? message(code: 'prompt.yes') : message(code: 'prompt.no')}</td>
                </tr>
                <g:if test="${selected?.metaFields}">
                    <!-- empty spacer row -->
                    <tr>
                        <td colspan="2"><br/></td>
                    </tr>
                    <tr>
                        <g:render template="/metaFields/metaFields" model="[metaFields: selected?.metaFields]"/>
                    </tr>
                </g:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- contact details -->
    <div class="heading">
        <strong><g:message code="customer.detail.contact.title"/></strong>
    </div>
    <g:if test="${contact}">
    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="customer.detail.user.email"/></td>
                        <td class="value"><a href="mailto:${contact?.email}">${contact?.email}</a></td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.telephone"/></td>
                        <td class="value">
                            <g:phoneNumber countryCode="${contact?.phoneCountryCode}" 
                                    areaCode="${contact?.phoneAreaCode}" number="${contact?.phoneNumber}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.address"/></td>
                        <td class="value">${contact.address1} ${contact.address2}</td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.city"/></td>
                        <td class="value">${contact.city}</td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.state"/></td>
                        <td class="value">${contact.stateProvince}</td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.country"/></td>
                        <td class="value">${contact.countryCode}</td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.contact.zip"/></td>
                        <td class="value">${contact.postalCode}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    </g:if>

    <div class="btn-box">
        <g:set var="editPermissionsAllowed" value="${false}" />
        <sec:ifAllGranted roles="MENU_99,USER_149">
            <g:if test="${session['user_id'] != selected.id || grails.plugin.springsecurity.SpringSecurityUtils.ifAllGranted('USER_1401')}">
                <div class="row">
                    <g:link controller="user" action="permissions" id="${selected.id}" class="submit edit"><span><g:message code="button.edit.permissions"/></span></g:link>
                </div>
                <g:set var="editPermissionsAllowed" value="${true}" />
            </g:if>
        </sec:ifAllGranted>
        <g:if test="${!editPermissionsAllowed && (SpringSecurityUtils.ifAllGranted('USER_1404') && session['user_id'] == selected.id || SpringSecurityUtils.ifAllGranted('USER_1403') && session['user_id'] != selected.id)}">
            <div class="row">
                <g:link controller="user" action="viewPermissions" id="${selected.id}" params="[f:f]" class="submit show"><span><g:message code="button.view.permissions"/></span></g:link>
            </div>
        </g:if>
        <div class="row">
            <g:set var="editUserAllowed" value="${false}" />
            <g:if test="${selected}">
                <g:if test="${currentUser.id != selected.id}">
                    <sec:ifAnyGranted roles="USER_147">
                       <g:set var="editUserAllowed" value="${true}" />
                    </sec:ifAnyGranted>
                </g:if>
                <g:elseif test="${f == 'myAccount'}">
                    <sec:ifAnyGranted roles="MY_ACCOUNT_161,MY_ACCOUNT_162">
                        <g:set var="editUserAllowed" value="${true}" />
                    </sec:ifAnyGranted>
                </g:elseif>
                <g:else>
                    <sec:ifAnyGranted roles="USER_1401">
                        <g:set var="editUserAllowed" value="${true}" />
                    </sec:ifAnyGranted>
                </g:else>
            </g:if>

            <g:if test="${editUserAllowed && selected && selected.deleted != 1}">
                <g:set var="editLink" value="${jB.property([name: 'editLink'])}"/>
                <g:if test="${editLink}">
                    <g:link url="${resource(file: editLink)+'/'+selected.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
                </g:if>
                <g:else>
                    <g:link action="edit" id="${selected.id}" class="submit edit" data-cy="editUser"><span><g:message code="button.edit"/></span></g:link>
                </g:else>
            </g:if>
            <sec:ifAllGranted roles="MENU_99">
                <g:if test="${currentUser.id != selected.id}">
                    <a onclick="showConfirm('delete-${selected.id}');" class="submit delete" data-cy="deleteUser"><span><g:message code="button.delete"/></span></a>
                </g:if>
            </sec:ifAllGranted>
            <sec:ifAllGranted roles="USER_142">
                <g:link controller="user" action="userCodeList" id="${selected.id}" class="submit show"><span><g:message code="button.userCode.list"/></span></g:link>
            </sec:ifAllGranted>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'user.delete.confirm',
                      'controller': 'user',
                      'action': 'delete',
                      'id': selected.id,
                     ]"/>

</div>
