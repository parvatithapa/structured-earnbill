<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
%{--
    Disabled when
     - Not (this role is a top level role)
     - Parent role does not have this permission
     - Permission can not be assigned to a role
     - User does not have permission to assign it to a role
--}%

<g:set var="userHasPermission" value="${permission.roleAssignable &&
        (permission.requiredToAssign ? SpringSecurityUtils.ifAllGranted(permission.requiredToAssign.authority) : true)}" />

<g:set var="permissionDisabled" value="${ typeDisablesPermissions[role?.roleTypeId]?.contains(permission.id) ||  !role.id && !parentRole ? true :
    (role.id && !parentRole ? !userHasPermission :
    ((parentRole && !(parentRole.permissions.find{ it.id == permission.id }) ) ||
            !userHasPermission )) }"/>

<g:applyLayout name="form/checkbox">
    <content tag="group.label">${permission.id}:</content>
    <content tag="label">${permission.getDescription(session['language_id']) ?: permission.authority}</content>
    <content tag="label.for">permission.${permission.id}</content>

    <g:checkBox name="permission.${permission.id}" class="check cb" checked="${role.id == 0 ? !permissionDisabled : rolePermission}" disabled="${viewOnly ?: permissionDisabled}"/>
    <g:if test="${viewOnly ?: permissionDisabled}">
        <input type="hidden" name="permission.${permission.id}" value="${(role.id == 0 ? !permissionDisabled : rolePermission) ? '1' : ''}" />
    </g:if>
</g:applyLayout>