package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import grails.plugin.springsecurity.SpringSecurityService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ BasicField.class, MetaFieldField.class, MetaFieldTypeField.class, StaticField.class, ListField.class, SpecificField.class})
public abstract class AbstractField {

    @XmlAttribute
    private String style;

    protected Object value;

    @XmlAttribute
    protected String moneyProperty;

    private IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    private SpringSecurityService springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);

    public String getStyle() {
        return style;
    }

    public boolean isMoneyProperty(String property) {
        return (null!=moneyProperty && null!=property) ? moneyProperty.trim().equals(property) : false;
    }

    public abstract Object getValue(Integer userId);

    protected IWebServicesSessionBean getApi() {
        return this.webServicesSessionSpringBean;
    }

    protected Integer getCompanyId() {
        return ((CompanyUserDetails) springSecurityService.getPrincipal()).getCompanyId();
    }
}