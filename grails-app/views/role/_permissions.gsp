<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
%{--
Some permissions must be disabled based on type.
This maps PermissionType id to a list of permissions that must always be disabled.
--}%
<g:set var="typeDisablesPermissions" value="${[5 : [1404]]}" />
<g:each var="permissionType" status="n" in="${permissionTypes.sort{ it.description }}">
    <div class="box-cards">
        <div class="box-cards-title">
            <a class="btn-open"><span>${permissionType.description}</span></a>
        </div>
        <div class="box-card-hold">
            <div class="form-columns">
                <g:set var="permissionList" value="${permissions ?: role.permissions}" />

                <!-- column 1 -->
                <div class="column" style="width: 45% !important">
                    <g:each var="permission" status="i" in="${permissionType.permissions.sort()}">
                        <g:set var="rolePermission" value="${!role.id ? true : permissionList.find{ it.id == permission.id } != null }"/>

                        <g:if test="${i < permissionType.permissions.size()/2}">
                            %{
                                permission.initializeAuthority();
                            }%
                            <g:render template="../role/permission"
                                         model="[             permission: permission,
                                                                    role: role,
                                                              parentRole: parentRole,
                                                                viewOnly: viewOnly,
                                                 typeDisablesPermissions: typeDisablesPermissions]" />
                        </g:if>
                    </g:each>
                </div>

                <!-- column 2 -->
                <div class="column">
                    <g:each var="permission" status="i" in="${permissionType.permissions.sort()}">
                        <g:set var="rolePermission" value="${!role.id ? true : permissionList.find{ it.id == permission.id }  != null }"/>

                        <g:if test="${i >= permissionType.permissions.size()/2}">
                            %{
                                permission.initializeAuthority();
                            }%
                            <g:render template="../role/permission"
                                         model="[             permission: permission,
                                                                    role: role,
                                                              parentRole: parentRole,
                                                                viewOnly: viewOnly,
                                                 typeDisablesPermissions: typeDisablesPermissions]" />
                        </g:if>
                    </g:each>
                </div>
            </div>
        </div>
    </div>
</g:each>

<script language="JavaScript" >
    var permImpl;

    $(document).ready(function() {
        permImpl = {
        <g:each in="${permissionImpliedMap.entrySet()}" var="entry">
            "${entry.key}": ${entry.value},
        </g:each>
         "000" : ""
        };


        $('input[name^="permission."]').change(function() {
            if(this.checked) {
                var idx = $(this).prop('name').substring(11);
                if(permImpl[idx] != null) {
                    var idxList = permImpl[idx];
                    for(var i=0; i<idxList.length; i++) {
                        $('input[name="permission.'+idxList[i]+'"]').prop('checked', true);
                    }
                }
            }
        });

        <g:if test="${partial}">
            <%-- register for the slide events if it is loaded as a template --%>
            registerSlideEvents();
        </g:if>

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
    });
</script>