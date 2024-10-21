package com.sapienter.jbilling.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.Resource;

public abstract class AbstarctFilter extends GenericFilterBean {

    @Resource(name = "basicAuthenticationEntryPoint")
    private AuthenticationEntryPoint authenticationEntryPoint;
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public static boolean authenticationIsRequired(String username) {
        Authentication existingAuth = SecurityContextHolder.getContext()
            .getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()) {
            if (existingAuth instanceof UsernamePasswordAuthenticationToken
                && !existingAuth.getName().equals(username)) {
                return true;
            } else {
                return existingAuth instanceof AnonymousAuthenticationToken;
            }
        } else {
            return true;
        }
    }

    private static final String FIND_USER_NAME_SQL = "SELECT user_name FROM base_user "
        + "WHERE id = %d AND entity_id = %d AND deleted = 0";

    public String findUsernameByIdAndEntityId(Integer userId, Integer entityId) {
        return jdbcTemplate.queryForObject(String.format(
            FIND_USER_NAME_SQL, userId, entityId), String.class);
    }
}