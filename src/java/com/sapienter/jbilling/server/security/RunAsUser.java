package com.sapienter.jbilling.server.security;

import grails.plugin.springsecurity.SpringSecurityUtils;

import org.springframework.security.core.context.SecurityContextHolder;

public class RunAsUser implements AutoCloseable {

    private final String             userName;

    public RunAsUser (final String userName, final String passWord) {
        this.userName = userName;
        SpringSecurityUtils.reauthenticate(userName, passWord);
    }

    public RunAsUser (final String userName) {
        this(userName, null);
    }

    public String getUserName () {
        return userName;
    }

    @Override
    public void close () {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

}
