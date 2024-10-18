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
    <meta name="layout" content="main" />
</head>
<body>
    <div class="form-edit">
        <g:set var="isNew" value="${!role || !role?.id || role?.id == 0}"/>
        <div class="heading">
            <strong>
                <g:if test="${viewOnly}">
                    <g:message code="role.view.title"/>
                </g:if>
                <g:elseif test="${isNew}">
                    <g:message code="role.add.title"/>
                </g:elseif>
                <g:else>
                    <g:message code="role.edit.title"/>
                </g:else>
            </strong>
        </div>

        <div class="form-hold">
            <g:form name="role-edit-form" action="save" useToken="true">
                <g:hiddenField name="isParent" value="${isParent}"/>
                <g:hiddenField name="permissionsToRemove" value=""/>
                <fieldset>
                    <!-- role information -->
                    <div class="form-columns">
                        <div class="column">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="role.label.id"/></content>
                                <g:if test="${!isNew}">
                                    <span>${role.id}</span>
                                </g:if>
                                <g:else>
                                    <em><g:message code="prompt.id.new"/></em>
                                </g:else>

                                <g:hiddenField name="role.id" value="${role?.id}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="role.label.name"/><span id="mandatory-meta-field">*</span></content>
                                <content tag="label.for">role.title</content>
                                <g:textField    class = "field"
                                                 name = "role.title"
                                                value = "${validationError ? roleTitle : role?.getTitle(session['language_id'] as Integer)}"
                                             disabled = "${viewOnly}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="role.label.description"/></content>
                                <content tag="label.for">role.description</content>
                                <g:textField    class = "field"
                                                 name = "role.description"
                                                value = "${validationError ? roleDescription : role?.getDescription(session['language_id'] as Integer)}"
                                             disabled = "${viewOnly}"/>
                            </g:applyLayout>

                            <g:if test="${parentRoleIsEditable}">
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="role.label.parentRole"/><span id="mandatory-meta-field">*</span></content>
                                    <content tag="label.for">role.roleType</content>
                                    <g:select        name = "parentRoleId"
                                                     from = "${parentRoles}"
                                                    value = "${role.parentRole?.id}"
                                              noSelection = "['': message(code: 'role.parentRole.selectBox')]"
                                                 disabled = "${role.final || viewOnly}"
                                                optionKey = "id"
                                              optionValue = "${{it.getTitle(session['language_id'])}}"
                                                 onchange = "refreshPermissions(this)"/>
                                </g:applyLayout>
                            </g:if>
                            <g:else>
                                <g:hiddenField name="roleType" value="${selectedRoleType}" />
                            </g:else>

                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="role.label.expire.password"/></content>
                                <content tag="label.for">role.expire.password</content>
                                <g:checkBox      id = "role.expire.password"
                                              class = "cb checkbox"
                                               name = "role.expire.password"
                                            checked = "${role?.expirePassword}"/>

                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="role.label.expire.password.days"/></content>
                                <content tag="label.for">role.expire.days</content>
                                <g:textField    id = "role.expire.days"
                                             class = "field"
                                              name = "role.expire.days"
                                             value = "${role?.passwordExpireDays}"/>
                            </g:applyLayout>

                        </div>
                    </div>

                    <!-- role permissions -->
                    <div id="permissionsList">
                        <g:render template = "permissions"
                                     model = "[permissionTypes: permissionTypes,
                                                          role: role,
                                                    parentRole: role.parentRole,
                                                      viewOnly: viewOnly]" />
                    </div>

                    <!-- spacer -->
                    <div>
                        &nbsp;<br/>
                    </div>

                    <div class="buttons">
                        <ul>
                            <g:if test="${!viewOnly}">
                                <li>
                                    <a onclick="savePermissions();" class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                                </li>
                                <g:if test="${parentRoleIsEditable}">
                                    <li>
                                        <a onclick="refreshPermissions($('#parentRoleId'))" class="submit sync"><span><g:message code="button.reset"/></span></a>
                                    </li>
                                </g:if>
                                <g:else>
                                    <li>
                                        <a onclick="refreshPermissionsForType()" class="submit sync"><span><g:message code="button.reset"/></span></a>
                                    </li>
                                </g:else>
                                <li>
                                    <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                                </li>
                            </g:if>
                            <g:else>
                                <li>
                                    <g:link action="list" class="submit back button-primary"><span><g:message code="button.back"/></span></g:link>
                                </li>
                            </g:else>
                        </ul>
                    </div>

                </fieldset>
            </g:form>
        </div>
    </div>
    <g:render template="/role/confirmDialog" model="[]"/>

    <script type="application/javascript">
        var oldPermissions;
        $(document).ready(function () {
            oldPermissions = $('.check.cb:checked').map(function() { return this.id.replace('permission.', ''); }).get();
        });

        function refreshPermissions(selector){
            var parentRoleId = $(selector).val();

            if(parentRoleId != null) {
                $.ajax({
                       type: 'POST',
                        url: '${createLink(action: 'refreshPermissions')}',
                       data: 'parentRoleId='+parentRoleId,
                    success: function(data) {
                        $('#permissionsList').html(data);
                    }
                });
            }
        }

        function refreshPermissionsForType() {
            $.ajax({
                   type: 'POST',
                    url: '${createLink(action: 'refreshPermissions')}',
                   data: 'roleTypeId=${role.roleTypeId}',
                success: function(data) {
                    $('#permissionsList').html(data);
                }
            });
        }

        function savePermissions() {
            var newPermissions = $('.check.cb:checked').map(function() { return this.id.replace('permission.', ''); }).get();
            var permissionsToRemove = oldPermissions.filter( function( el ) {
                return !newPermissions.includes( el );
            });

            if (permissionsToRemove.length > 0 && $('#isParent').val() === 'true') {
                $('#permissionsToRemove').val(permissionsToRemove.join());
                $('#confirm-dialog-save-permissions').dialog('open');
            } else {
                $('#role-edit-form').submit();
            }
        }
    </script>
</body>
</html>
