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

<%--
  Shows user role.

  @author Brian Cowdery
  @since  02-Jun-2011
--%>


%{-- initialize the authority name used in the security context --}%
%{
    selected.initializeAuthority()
}%


<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.getTitle(session['language_id'])}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="role.label.id"/></td>
                <td class="value">${selected.id}</td>
            </tr>
            <g:if test="${selected.parentRole}">
                <tr>
                    <td><g:message code="role.label.parentRole"/></td>
                    <td class="value">${selected.parentRole.getTitle(session['language_id'])}</td>
                </tr>
            </g:if>
            <tr>
                <td><g:message code="role.label.description"/></td>
                <td class="value">${selected.getDescription(session['language_id'])}</td>
            </tr>
             <tr>
                <td><g:message code="role.label.roleType"/></td>
                <td class="value">${selectedRoleType.toUpperCase()}</td>
            </tr>
            <tr>
                <td><g:message code="role.label.expire.password"/></td>
                <td class="value"><g:formatBoolean boolean = "${selected.expirePassword}"
                                                      true = "${message(code:'prompt.yes')}"
                                                     false = "${message(code:'prompt.no')}"/>
                </td>
            </tr>
            <g:if test="${selected.expirePassword}">
            <tr>
                <td><g:message code="role.label.expire.password.days"/></td>
                <td class="value">${selected.passwordExpireDays}</td>
            </tr>
            </g:if>
            </tbody>
        </table>
      </div>
    </div>

    <div class="btn-box">
        <div class="row">
            <sec:ifAllGranted roles="CONFIGURATION_1903">
                <g:if test="${editable}">
                    <g:link action="edit" id="${selected.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
                    <g:if test="${selected.parentRole}">
                        <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
                    </g:if>
                </g:if>
                <g:else>
                    <g:link action="edit" id="${selected.id}" params="[viewOnly : true]" class="submit show"><span><g:message code="button.view"/></span></g:link>
                </g:else>
            </sec:ifAllGranted>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'role.delete.confirm',
                      'controller': 'role',
                      'action': 'delete',
                      'id': selected.id,
                     ]"/>
</div>
