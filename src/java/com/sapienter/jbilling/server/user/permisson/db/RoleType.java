package com.sapienter.jbilling.server.user.permisson.db;

import com.sapienter.jbilling.common.CommonConstants;

import java.util.Arrays;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public enum RoleType {
    
    INTERNAL("internal", CommonConstants.TYPE_INTERNAL, "ROLE_INTERNAL"),
    ROOT("super.user", CommonConstants.TYPE_ROOT, "ROLE_SUPER_USER") ,
    CLERK("clerk", CommonConstants.TYPE_CLERK, "ROLE_CLERK"),
    PARTNER("partner", CommonConstants.TYPE_PARTNER, "ROLE_PARTNER"),
    SYSTEM_ADMIN("system.admin", CommonConstants.TYPE_SYSTEM_ADMIN, "ROLE_SYSTEM_ADMIN"),
    CUSTOMER("customer", CommonConstants.TYPE_CUSTOMER, "ROLE_CUSTOMER");
    
    private Integer roleTypeId;
    private String title;
    private String authorityTitle;

    RoleType(String title, Integer roleTypeId, String authorityTitle) {
        this.title = title;
        this.roleTypeId = roleTypeId;
        this.authorityTitle = authorityTitle;
    }

    public Integer getRoleTypeId() {
        return roleTypeId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthorityTitle() { return authorityTitle; }

    public static RoleType getRoleTypeById(Integer roleTypeId) {
        return Arrays.stream(values())
                     .filter(roleType -> roleType.getRoleTypeId().equals(roleTypeId))
                     .findFirst()
                     .orElse(null);
    }

    public static RoleType getRoleTypeByTitle(String title) {
        return Arrays.stream(values())
                .filter(roleType -> roleType.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    public static RoleType[] getRoleTypes() {
        return Arrays.stream(values())
                     .filter(roleType -> roleType.equals(ROOT) || roleType.equals(CLERK))
                     .toArray(RoleType[] :: new);
    }
    
}
