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

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * RoleBL
 *
 * @author Brian Cowdery
 * @since 03/06/11
 */
public class RoleBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RoleDAS roleDas;
    private RoleDTO role;

    public RoleBL() {
        _init();
    }

    public RoleBL(RoleDTO role) {
        _init();
        this.role = role;
    }

    public RoleBL(Integer roleId) {
        _init();
        set(roleId);
    }

    private void _init() {
        this.roleDas = new RoleDAS();
    }

    public void set(Integer roleId) {
        this.role = roleDas.find(roleId);
    }

    public RoleDTO getEntity() {
        return role;
    }

    /**
     * Saves a new role to and sets the BL entity to the newly created role. This method does not
     * save the description or title of a permission. Use {@link #setDescription(Integer, String)} and
     * {@link #setTitle(Integer, String)} to set the international descriptions.
     *
     * @param role role to save
     * @return id of the new role
     */
    public Integer create(RoleDTO role) {
        if (role != null) {
            this.role = roleDas.save(role);
            roleDas.flush();
            return this.role.getId();
        }

        String msg = "Cannot save a null RoleDTO!";
        String createRoleMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                .action(LogConstants.ACTION_CREATE.toString())
                .message(msg).build().toString();
        logger.error(createRoleMsg);

        return null;
    }

    /**
     * Add all permissions which are required by the current selected permissions.
     *
     * @param dto RoleDTO Object to be updated
     * @return list of extra permissions which were added
     */
    public List<PermissionDTO> addImpliedPermissions(RoleDTO dto) {
        List<PermissionDTO> permissionsToAdd = new ArrayList<>();
        for(PermissionDTO permission : dto.getPermissions()) {
            permissionsToAdd.addAll(permission.rollUpImpliedPermissions());
        }

        Set<PermissionDTO> currentPermissions = dto.getPermissions();
        List<PermissionDTO> impliedPermissions = new ArrayList<>();
        for(PermissionDTO permission : permissionsToAdd) {
            if(!currentPermissions.contains(permission) && !impliedPermissions.contains(permission)) {
                impliedPermissions.add(permission);
            }
        }
        dto.getPermissions().addAll(impliedPermissions);
        return impliedPermissions;
    }

    public void validateDuplicateRoleName(String roleName, Integer languageId, Integer companyId) {

        if (roleName != null && !roleName.trim().isEmpty()) {

            InternationalDescriptionDAS internationalDescriptionDAS = InternationalDescriptionDAS.getInstance();

            //check if the description already exists in international_description
            Collection<InternationalDescriptionDTO> list =
                    internationalDescriptionDAS.roleExists(Constants.TABLE_ROLE,
                            Constants.PSUDO_COLUMN_TITLE,
                            roleName.trim(),
                            languageId,
                            companyId);

            if (list != null && !list.isEmpty()){
                String msg = "The role already exists with name " + roleName;
                String validateRoleMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                        .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                        .action(LogConstants.ACTION_UPDATE.toString())
                        .message(msg).build().toString();
                logger.error(validateRoleMsg);
                throw new SessionInternalError(msg,
                        new String[]{"RoleDTO,title,validation.error.roleName.already.exists," + roleName});

            }
        }

    }

    /**
     * Updates this role's permissions with those of the given role. This method does not
     * update the description or title of a permission. Use {@link #setDescription(Integer, String)} and
     * {@link #setTitle(Integer, String)} to update the roles international descriptions.
     *
     * @param dto role with permissions
     */
    public void update(RoleDTO dto) {
        setPermissions(dto.getPermissions());
    }

    /**
     * Sets the granted permissions of this role to the given set.
     *
     * @param grantedPermissions list of granted permissions
     */
    public void setPermissions(Set<PermissionDTO> grantedPermissions) {
        if (role != null) {
            role.getPermissions().clear();
            role.getPermissions().addAll(grantedPermissions);

            this.role = roleDas.save(role);
            roleDas.flush();

        } else {
            String msg = "Cannot update, RoleDTO not found or not set!";
            String updateRoleErrorMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                    .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                    .action(LogConstants.ACTION_UPDATE.toString())
                    .message(msg).build().toString();
            logger.error(updateRoleErrorMsg);
        }
    }

    /**
     * Deletes this role.
     *
     * Any users that use this role as their primary will be left without a role. It's best to move user
     * out of this role before deleting to ensure that the user doesn't experience an interruption in
     * service (by having no role).
     */
    public void delete() {
        if (role != null) {
            role.getPermissions().clear();
            roleDas.delete(role);
            roleDas.flush();
        } else {
            logger.error("Cannot delete, RoleDTO not found or not set!");
        }
    }
    
    public void updateRoleType(int roleTypeId) {
        if (role != null) {
            role.setRoleTypeId(roleTypeId);
            roleDas.save(role);
            roleDas.flush();
        } else {
            logger.error("Cannot delete, RoleDTO not found or not set!");
        }
    }

    public void setDescription(Integer languageId, String description) {
        this.role.setDescription("description", languageId, description);
    }

    public void setParent(RoleDTO parent) {
        this.role.setParentRole(parent);
    }

    public void setRequiredToModify(PermissionDTO permission) {
        this.role.setRequiredToModify(permission);
    }

    public void setRequiredToCreateUser(PermissionDTO permission) {
        this.role.setRequiredToCreateUser(permission);
    }

    public void setDescription(RoleDTO copyRole, Integer languageId, String description) {
        copyRole.setDescription("description", languageId, description);
    }

    public void setTitle(Integer languageId, String title) {
        this.role.setDescription("title", languageId, title);
    }

    public void setTitle(RoleDTO copyRole, Integer languageId, String title) {
        copyRole.setDescription("title", languageId, title);
    }
    
    public void deleteDescription(Integer languageId) {
        this.role.deleteDescription(languageId);
    }
    
    public void deleteTitle(Integer languageId) {
        this.role.deleteDescription(Constants.PSUDO_COLUMN_TITLE, languageId);
    }

    public void createDefaultRoles(Integer languageId, CompanyDTO entity, CompanyDTO targetEntity) {
        List<RoleDTO> roleDTOs = roleDas.findAllRolesByEntity(entity.getId());
        for (RoleDTO roleDTO : roleDTOs) {
            roleDas.reattach(roleDTO);
            RoleDTO newRole = new RoleDTO();
            newRole.getPermissions().addAll(roleDTO.getPermissions());
            newRole.setCompany(targetEntity);
            newRole.setRoleTypeId(roleDTO.getRoleTypeId());
            newRole.setRequiredToModify(roleDTO.getRequiredToModify());
            newRole.setFinal(roleDTO.isFinal());
            newRole.setRequiredToCreateUser(roleDTO.getRequiredToCreateUser());
            newRole.setExpirePassword(roleDTO.isExpirePassword());
            newRole.setPasswordExpireDays(roleDTO.getPasswordExpireDays());
            create(newRole);
            setDescription(languageId, roleDTO.getDescription(languageId) != null ? roleDTO.getDescription(languageId) : roleDTO.getDescription());
            setTitle(languageId, roleDTO.getTitle(languageId) != null ? roleDTO.getTitle(languageId) : roleDTO.getTitle(1));
        }
    }

    public void setInternationalDescriptions(String title, String description, Integer languageId){
        setDescription(languageId, description);
        setTitle(languageId, title);
        roleDas.flush();
    }

    public boolean isUsedRole() {
        return role != null && role.getBaseUsers() != null && role.getBaseUsers().isEmpty();
    }

    public RoleDTO findByRoleTypeIdAndCompanyId(Integer roleTypeId, Integer companyId) {
        return roleDas.findByRoleTypeIdAndCompanyId(roleTypeId, companyId);
    }

    public RoleDTO findByTypeOrId(Integer id, Integer companyId) {
        RoleDTO roleDTO = roleDas.findNow(id);
        if(roleDTO == null || roleDTO.getCompany() == null || (companyId != null && id <= 5)) {
            roleDTO = new RoleDAS().findByRoleTypeIdAndCompanyId(id, companyId);
        }
        return roleDTO;
    }

    public void setExpirePassword (boolean expirePassword) {
        this.role.setExpirePassword(expirePassword);
    }

    public void setExpirePasswordDays(Integer expirePasswordDays) {
        this.role.setPasswordExpireDays(expirePasswordDays);
    }
}
