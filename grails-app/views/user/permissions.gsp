<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
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

<html>
<head>
    <meta name="layout" content="main"/>
</head>
<body>
<div class="form-edit">

    <div class="heading">
        <strong>
            <g:if test="${viewOnly}">
                <g:message code="permissions.view.title"/>
            </g:if>
            <g:else>
                <g:message code="permissions.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="user-permission-edit-form" action="savePermissions" useToken="true">
            <fieldset>

                <!-- user information -->
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.customer.number"/></content>
                            ${user.userId}
                            <g:hiddenField name="id" value="${user.userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.login.name"/></content>
                            ${user?.userName}
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.user.role"/></content>
                            <g:if test="${role}">
                                ${role.getTitle(session['language_id'])}
                            </g:if>
                            <g:else>
                                 -
                            </g:else>
                        </g:applyLayout>
                    </div>

                    <div class="column">

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.organization.name"/></content>
                            ${contact?.organizationName}
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.user.name"/></content>
                            ${contact?.firstName} ${contact?.lastName}
                        </g:applyLayout>
                    </div>
                </div>

                <!-- user permissions -->

                <g:each var="permissionType" status="n" in="${permissionTypes.sort{ it.description }}">
                    <div class="box-cards">
                        <div class="box-cards-title">
                            <a class="btn-open"><span>${permissionType.description}</span></a>
                        </div>
                        <div class="box-card-hold">
                            <div class="form-columns">

                                <!-- column 1 -->
                                <div class="column" style="width: 45% !important">
                                    <g:each var="permission" status="i" in="${permissionType.permissions.sort()}">
                                        <g:set var="userPermission" value="${permissions.find{ it.id == permission.id }}"/>
                                        <g:set var="rolePermission" value="${role?.permissions?.find{ it.id == permission.id }}"/>

                                        <g:if test="${i < permissionType.permissions.size()/2}">
                                            %{
                                                permission.initializeAuthority();
                                                permission.requiredToAssign?.initializeAuthority();
                                            }%
                                            <g:applyLayout name="form/checkbox">
                                                <content tag="group.label">
                                                    <em>(<g:formatBoolean boolean="${userPermission != null}" true="${message(code: 'boolean.on')}" false="${message(code: 'boolean.off')}"/>)</em>:
                                                </content>
                                                <content tag="label">
                                                    <g:if test="${(userPermission && !rolePermission) || (!userPermission && rolePermission)}">
                                                        <strong>
                                                            ${permission.getDescription(session['language_id']) ?: permission.authority}
                                                        </strong>
                                                    </g:if>
                                                    <g:else>
                                                        ${permission.getDescription(session['language_id']) ?: permission.authority}
                                                    </g:else>
                                                </content>
                                                <content tag="label.for">permission.${permission.id}</content>
                                                <g:set var="permissionDisabled" value="${ viewOnly || !( (rolePermission) || (permission.userAssignable && (permission.requiredToAssign ? SpringSecurityUtils.ifAllGranted(permission.requiredToAssign.authority) : true)) )}" />
                                                <g:checkBox name="permission.${permission.id}" class="check cb" checked="${userPermission}"
                                                            disabled="${ permissionDisabled }"/>
                                                <g:if test="${permissionDisabled && userPermission}">
                                                    <g:hiddenField name="permission.${permission.id}" value="on" />
                                                </g:if>
                                            </g:applyLayout>
                                        </g:if>
                                    </g:each>
                                </div>

                                <!-- column 2 -->
                                <div class="column">
                                    <g:each var="permission" status="i" in="${permissionType.permissions.sort()}">
                                        <g:set var="userPermission" value="${permissions.find{ it.id == permission.id }}"/>
                                        <g:set var="rolePermission" value="${role?.permissions?.find{ it.id == permission.id }}"/>
                                        <g:if test="${i >= permissionType.permissions.size()/2}">

                                            %{
                                                permission.initializeAuthority();
                                                permission.requiredToAssign?.initializeAuthority();
                                            }%

                                            <g:applyLayout name="form/checkbox">
                                                <content tag="group.label">
                                                    <em>(<g:formatBoolean boolean="${userPermission != null}" true="${message(code: 'boolean.on')}" false="${message(code: 'boolean.off')}"/>)</em>:
                                                </content>
                                                <content tag="label">
                                                    <g:if test="${(userPermission && !rolePermission) || (!userPermission && rolePermission)}">
                                                        <strong>
                                                            ${permission.getDescription(session['language_id']) ?: permission.authority}
                                                        </strong>
                                                    </g:if>
                                                    <g:else>
                                                        ${permission.getDescription(session['language_id']) ?: permission.authority}
                                                    </g:else>
                                                </content>
                                                <content tag="label.for">permission.${permission.id}</content>
                                                <g:checkBox name="permission.${permission.id}" class="check cb" checked="${userPermission}"
                                                            disabled="${ viewOnly || !( (rolePermission != null) || (permission.userAssignable && (permission.requiredToAssign != null ? SpringSecurityUtils.ifAllGranted(permission.requiredToAssign.authority) : true)) )}"/>
                                            </g:applyLayout>

                                        </g:if>
                                    </g:each>
                                </div>
                            </div>
                        </div>
                    </div>

                </g:each>

                <!-- spacer -->
                <div>
                    &nbsp;<br/>
                </div>

                <div class="buttons">
                    <ul>
                        <g:if test="${viewOnly}">
                            <li>
                                <g:link url="${backUrl?backUrl:createLink(controller: 'user', action: 'list', id: user.userId)}" class="submit button-primary"><span><g:message code="button.back"/></span></g:link>
                            </li>
                        </g:if>
                        <g:else>
                            <li>
                                <a onclick="$('#user-permission-edit-form').submit()" class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                            </li>
                            <li>
                                <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                            </li>
                        </g:else>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>
</div>

<script language="JavaScript" >
    var permImpl = new Map();
    $(document).ready(function() {
        <g:each in="${permissionImpliedMap.entrySet()}" var="entry">
        permImpl.set("${entry.key}", ${entry.value});
        </g:each>

        $('.box-cards-title').dblclick(function() {
            var isOpen = $(this).parent('.box-cards').hasClass('box-cards-open');
            $('.box-cards').each(function(i, el) {
                if(isOpen) {
                    openSlide(el);
                } else {
                    closeSlide(el);
                }
            });
        });

        $('input[name^="permission."').change(function() {
            console.log($(this).prop('name'));
            console.log($(this).prop('name').substring(11));
            if(this.checked) {
                var idx = $(this).prop('name').substring(11);
                if(permImpl.has(idx)) {
                    var idxList = permImpl.get(idx);
                    for(var i=0; i<idxList.length; i++) {
                        $('input[name="permission.'+idxList[i]+'"]').prop('checked', true);
                    }
                }
            }
        });
    });
</script>
</body>
</html>