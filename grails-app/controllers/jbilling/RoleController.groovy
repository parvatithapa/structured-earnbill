/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.RoleBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO
import com.sapienter.jbilling.server.user.permisson.db.PermissionTypeDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleType
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.audit.LogMessage
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.transaction.annotation.Transactional

/**
 * RoleController 
 *
 * @author Brian Cowdery
 * @since 02/06/11
 */
@Secured(["MENU_99"])
class RoleController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static final viewColumnsToFields = ['roleId': 'id']
    static scope = "prototype"

    def springSecurityService
    def userService
    def breadcrumbService
    def viewUtils
    SecurityValidator securityValidator

    def index () {
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def getList(params) {

        def company_id = session['company_id'] as Integer

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        def languageId = session['language_id']

        RoleDTO loggedInUserRole = UserDTO.get(springSecurityService.principal.id).roles?.first()
        return RoleDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(company_id))
            if ( params.roleId ) {
                def searchParam = params.roleId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and a.psudo_column = 'title'
                                            and lower(a.content) like ?
                                        )
                                    """,[Constants.TABLE_ROLE,languageId,searchParam]
                    )
                }
            }
            if (loggedInUserRole.roleTypeId != Constants.TYPE_SYSTEM_ADMIN) {
                ne('roleTypeId', Constants.TYPE_SYSTEM_ADMIN)
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def list () {

        def selected = params.id ? RoleDTO.get(params.int('id')) : null
        securityValidator.validateCompany(selected?.company?.id, Validator.Type.VIEW)
        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'role.not.found'
            flash.args = [params.id]
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', selected?.getTitle(session['language_id']), selected?.id, selected?.getDescription(session['language_id']))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'] as Integer, Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'rolesTemplate', model: [selected: selected ]
            }else {
                render view: 'list', model: [selected: selected ]
            }
            return
        }
        def selectedRoleType
        def editable = true

        if(selected) {
            RoleType roleType = RoleType.getRoleTypeById(selected.roleTypeId)
            if(roleType) {
                selectedRoleType = g.message(code: 'role.type.' + roleType.title)
            }
            if(selected.requiredToModify) {
                selected.requiredToModify.initializeAuthority()
                if(SpringSecurityUtils.ifNotGranted(selected.requiredToModify.authority)) {
                    editable = false
                }
            }
        }
        def roles = getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'rolesTemplate',
                      model: [            roles: roles,
                                       selected: selected,
                               selectedRoleType: selectedRoleType,
                                       editable: editable ]
        } else {
            render  view: 'list',
                   model: [            roles: roles,
                                    selected: selected,
                            selectedRoleType: selectedRoleType,
                                    editable: editable ]
        }
    }

    def findRoles () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def roles = getList(params)

        try {
            def jsonData = getRolesJsonData(roles, params)
            render jsonData as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Roles to JSon
     */
    private static def Object getRolesJsonData(roles, GrailsParameterMap params) {
        def jsonCells = roles
        def currentPage = params.page ?: 1
        def rowsNumber = params.rows ?: 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show () {
        def role = RoleDTO.get(params.int('id'))
        securityValidator.validateCompany(role?.company?.id, Validator.Type.VIEW)

        breadcrumbService.addBreadcrumb(controllerName, 'list', role.getTitle(session['language_id']), role.id, role.getDescription(session['language_id']))

        RoleType roleType = RoleType.getRoleTypeById(role.roleTypeId)
        String selectedRoleType = ""
        if(roleType) {
            selectedRoleType = g.message(code: 'role.type.' + roleType.title)
        }
        def editable = true
        if(role.requiredToModify) {
            role.requiredToModify.initializeAuthority()
            if(SpringSecurityUtils.ifNotGranted(role.requiredToModify.authority)) {
                editable = false
            }
        }
        render template: 'show', model: [ selected: role, selectedRoleType: selectedRoleType, editable: editable ]
    }

    @Secured(["CONFIGURATION_1903"])
    def edit () {
        RoleDAS roleDAS = new RoleDAS();
        def role = chainModel?.role
        def viewOnly = params.viewOnly != null ? params.boolean('viewOnly') : false
        if (!role) {
            if (params.id) {
                role = RoleDTO.get(params.int('id'))
                if(role) {
                    securityValidator.validateCompany(role?.company?.id, Validator.Type.EDIT)
                    if(!viewOnly && role.requiredToModify) {
                        role.requiredToModify.initializeAuthority()
                        if(SpringSecurityUtils.ifNotGranted(role.requiredToModify.authority)) {
                            throw new SecurityException(String.format("Unauthorized access to role %s by caller with id %s",
                                    role.id, session['user_id']));
                        }
                    }
                } else {
                    flash.error = 'role.not.found'
                    flash.args = [params.id]
                }
            }
            else {
                role = new RoleDTO()
            }
        }

        if (role == null) {
            redirect action: 'list', params:params
            return
        }

        def roles = roleDAS.findAllRolesByEntity(session['company_id'] as Integer)
        roles = roles.findAll {
            if(role.id == it.id) {
                return false
            } else if(!it.final && it.requiredToModify != null) {
                it.requiredToModify.initializeAuthority()
                return SpringSecurityUtils.ifAllGranted(it.authority)
            } else {
                return !it.final
            }

        }

        def permissionImpliedMap = buidPermissionImpliedMap()
        def permissionTypes = PermissionTypeDTO.list(order: 'asc')
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? role?.getTitle(session['language_id'] as Integer) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

        def roleTitle = chainModel?.roleTitle
        def roleDescription = chainModel?.roleDescription
        def validationError = chainModel?.validationError ? true : false;

        [                 role : role,
               permissionTypes : permissionTypes,
                     roleTitle : roleTitle,
               roleDescription : roleDescription,
               validationError : validationError,
                   parentRoles : roles,
                      viewOnly : viewOnly,
          permissionImpliedMap : permissionImpliedMap,
          parentRoleIsEditable : role.id == 0 || role.parentRole != null,
                      isParent : role.id == 0 ? false : roleDAS.hasChildRoles(role.id)]
    }

    private static Map buidPermissionImpliedMap() {
        def permissionImpliedMap = [:]
        PermissionDTO.list().each { p ->
            def impliedList = "["
            p.rollUpImpliedPermissions().each { p2 ->
                if(impliedList.length() > 1) impliedList += ","
                impliedList += p2.id
            }
            impliedList += "]"
            permissionImpliedMap.put(p.id, impliedList)
        }
        return permissionImpliedMap
    }

    @Secured(["CONFIGURATION_1903"])
    @RequiresValidFormToken
    def save () {
        def role = new RoleDTO()
        def oldRole = null
        if (params.'role.id' && params.int('role.id') != 0) {
            oldRole = RoleDTO.get(params.int('role.id'))
            securityValidator.validateCompany(oldRole?.company?.id, Validator.Type.EDIT)
            if(oldRole.requiredToModify) {
                oldRole.requiredToModify.initializeAuthority()
                if(SpringSecurityUtils.ifNotGranted(oldRole.requiredToModify.authority)) {
                    throw new SecurityException(String.format("Unauthorized access to role %s by caller with id %s",
                            oldRole.id, session['user_id']));
                }
            }
        }

        role.company = CompanyDTO.get(session['company_id'])
        bindData(role, params, 'role')
        def roleTitle = params.role.title == null ?: params.role.title.trim()
        def roleDescription = params.role.description == null ?: params.role.description.trim()
        def languageId = session['language_id']

        try {

            def expirePassword = params.role.expire.password as boolean
            int passwordExpireDays = getPasswordExpireDaysParam()

            Integer parentRoleId = (params.parentRoleId ? params.parentRoleId as Integer : 0)
            //all new roles must have a parent and if a role had a prent is must still have a parent
            if((role.id == 0 || oldRole?.parentRole != null) && (null == parentRoleId || parentRoleId == 0)) {
                String [] errors = ["RoleDTO,parentRoleId,role.error.parentRoleId"]
                throw new SessionInternalError("Parent Role is missing ", errors);
            } else if(null != parentRoleId && parentRoleId != 0) {
                role.parentRole = RoleDTO.get(parentRoleId)
                role.roleTypeId = role.parentRole.roleTypeId
                role.requiredToCreateUser = role.parentRole.requiredToCreateUser
                role.setExpirePassword(expirePassword)
                role.setPasswordExpireDays(passwordExpireDays)
                securityValidator.validateCompany(role.parentRole?.company?.id, Validator.Type.EDIT)

            }

            List<PermissionDTO> allPermissions = role.parentRole == null ? PermissionDTO.list() : new ArrayList<PermissionDTO>(role.parentRole.permissions)
            params.permission.each { id, granted ->
                if (granted) {
                    Set<PermissionDTO> rolePermissions = role.permissions
                    rolePermissions.add(allPermissions.find { it.id == id as Integer })

                    String msg = String.format("%d overridden permissions for role %s", rolePermissions.size(), role.id)
                    String fullPermissionsMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                            .status(LogConstants.STATUS_SUCCESS.toString())
                            .action(LogConstants.ACTION_UPDATE.toString())
                            .message(msg).build().toString()
                    log.info(fullPermissionsMsg)
                }
            }

            def isNonEmptyRoleTitle = params.role.title ? !params.role.title.trim().isEmpty() : false;
            if (isNonEmptyRoleTitle) {
                def roleService = new RoleBL()
                def addedPermissions = (roleService.addImpliedPermissions(role) as PermissionDTO[]).collect({it.description}).join(', ')

                // save or update
                if (!role.id || role.id == 0) {
                    log.debug("saving new role ${role}")
                    roleService.validateDuplicateRoleName(roleTitle as String,
                                                          languageId as Integer,
                                                          role.company.id as int)

                    role.id = roleService.create(role)

                    String createRoleMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                                                                   .status(LogConstants.STATUS_SUCCESS.toString())
                                                                   .action(LogConstants.ACTION_CREATE.toString())
                                                                   .message("Created new role ${role.id}").build().toString()
                    log.info(createRoleMsg)

                    if(addedPermissions.length() > 0) {
                        flash.message = 'role.created.with.permissions'
                        flash.args = [role.id as String, addedPermissions]
                    } else {
                        flash.message = 'role.created'
                        flash.args = [role.id as String]
                    }

                } else {
                    log.debug("updating role ${role.id}")

                    roleService.set(role.id)

                    if (!roleService.getEntity()?.getDescription(languageId as Integer, Constants.PSUDO_COLUMN_TITLE)?.equalsIgnoreCase(roleTitle)) {
                        roleService.validateDuplicateRoleName(roleTitle as String,
                                                             languageId as Integer,
                                                             role.company.id as int)
                    }
                    if(role.roleTypeId != null) {
                        roleService.updateRoleType(role.roleTypeId)
                    }
                    if(role.parentRole != null) {
                        roleService.setParent(role.parentRole)
                        roleService.setRequiredToCreateUser(role.parentRole.requiredToCreateUser)
                    }

                    roleService.setExpirePassword(expirePassword)
                    roleService.setExpirePasswordDays(passwordExpireDays)

                    roleService.update(role)

                    if (params.permissionsToRemove) {
                        RoleDAS roleDAS = new RoleDAS();
                        def rolesToDelete = params.permissionsToRemove.split(',')*.asType(Integer)
                        List<RoleDTO> childRoles = new ArrayList<>()

                        // Iterating over each child role to also add its children, which add support for grandchild
                        // hierarchy
                        roleDAS.findChildRolesByParentId(role.id).each { childRole ->
                            childRoles.addAll(roleDAS.findChildRolesByParentId(childRole.id))
                            childRoles.add(childRole)
                        }

                        def childRoleService = new RoleBL()
                        childRoles.each { r ->
                            List<InternationalDescriptionDTO> descriptions = roleDAS.getDescriptions(r.id)
                            List<Integer> rolePermissions = r.permissions.findAll { !(it.id in rolesToDelete) }.collect { it.id }
                            List<PermissionDTO> allParentPermissions = new ArrayList<PermissionDTO>(r.parentRole.permissions)
                            allParentPermissions.removeAll { p -> !(p.id in rolePermissions)}

                            childRoleService.set(r.id)
                            childRoleService.setParent(r.parentRole)
                            childRoleService.setRequiredToCreateUser(r.parentRole.requiredToCreateUser)
                            childRoleService.setPermissions(allParentPermissions as Set)
                            childRoleService.setInternationalDescriptions(descriptions.get(0).content,
                                                                          descriptions.get(1).content,
                                                                          languageId as Integer);
                        }
                    }

                    String updateRoleMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                                                                   .status(LogConstants.STATUS_SUCCESS.toString())
                                                                   .action(LogConstants.ACTION_UPDATE.toString())
                                                                   .message("Updated role ${role.id}").build().toString()
                    log.info(updateRoleMsg)
                    if(addedPermissions.length() > 0) {
                        flash.message = 'role.updated.with.permissions'
                        flash.args = [role.id as String, addedPermissions]
                    } else {
                        flash.message = 'role.updated'
                        flash.args = [role.id as String]
                    }
                }

                // set/update international descriptions
                roleService.setInternationalDescriptions(roleTitle as String,
                                                         roleDescription as String,
                                                         languageId as Integer);
                chain action: 'list', params: [id: role.id]
            } else {

                String [] errors = ["RoleDTO,title,role.error.title.empty"]
                throw new SessionInternalError("Description is missing ", errors);
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)

            if(oldRole) {
                if(role.parentRole == null && oldRole.parentRole != null) {
                    role.permissions = oldRole.permissions
                    role.permissions.each {it.roleAssignable}
                }
                role.parentRole = oldRole.parentRole
            }

            def permissionTypes = PermissionTypeDTO.list(order: 'asc')
            def roles = new RoleDAS().findAllRolesByEntity(session['company_id'] as Integer)
            roles = roles.findAll {
                if(role.id == it.id) {
                    return false
                } else if(!it.final && it.requiredToModify != null) {
                    it.requiredToModify.initializeAuthority()
                    return SpringSecurityUtils.ifAllGranted(it.authority)
                } else {
                    return !it.final
                }

            }

            chain action: 'edit',
                   model: [                 role : role,
                                 permissionTypes : permissionTypes,
                                       roleTitle : roleTitle,
                                 roleDescription : roleDescription,
                                 validationError : true,
                                     parentRoles : roles,
                                        viewOnly : false,
                            permissionImpliedMap : buidPermissionImpliedMap(),
                            parentRoleIsEditable : role.id == 0 || role.parentRole != null]
        }
    }

    private Integer getPasswordExpireDaysParam() {
        Integer passwordExpireDays
        try {
            passwordExpireDays = Integer.valueOf(params.role.expire.days)
            if (passwordExpireDays < 0) {
                String[] errors = ["RoleDTO,passwordExpireDays,role.expire.password.days.error"]
                throw new SessionInternalError("Invalid number ", errors)
            }
            return passwordExpireDays
        } catch (NumberFormatException nfe) {
            String[] errors = ["RoleDTO,passwordExpireDays,role.expire.password.days.error"]
            throw new SessionInternalError("Invalid number ", errors)
        }
    }

    @Secured(["CONFIGURATION_1903"])
    def delete () {
        securityValidator.validateCompany(RoleDTO.get(params.int('id'))?.company?.id, Validator.Type.EDIT)
        try {
            if (params.id) {
                userService.deleteRole(params.int('id'), session['language_id'] as Integer);

                log.info("Deleted role ${params.id}.")
            }

            flash.message = 'role.deleted'
            flash.args = [params.id]

            // render the partial role list
            params.applyFilter = true
            params.id = null
            redirect action: 'list'
        } catch (SessionInternalError e) {
            flash.error = 'Can not delete role '+params.id+', it is in use.'
            redirect action: 'list'
        }
    }

    @Transactional(readOnly = true)
    def refreshPermissions(){
        def permissionTypes = PermissionTypeDTO.list(order: 'asc')

        def permissionImpliedMap = buidPermissionImpliedMap()

        def parentRole = null
        def role = new RoleDTO()
        if (params.parentRoleId) {
            parentRole = RoleDTO.get(params.parentRoleId as Integer);
        } else if (params.roleTypeId) {
            role = new RoleDAS().findByRoleTypeIdAndCompanyId(params.roleTypeId as Integer, null);
            if(role == null) {
                role = new RoleDAS().findByRoleTypeIdAndCompanyId(params.roleTypeId as Integer, session['company_id'] as Integer);
            }
        }

        render(template: 'permissions',
                  model: [     permissionTypes: permissionTypes,
                                          role: role,
                                    parentRole: parentRole,
                                       partial: true,
                          permissionImpliedMap: permissionImpliedMap,
                                      viewOnly: false]) as JSON
    }
}
