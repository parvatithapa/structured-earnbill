package com.sapienter.jbilling.saml.integration.remote.vo;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;

@XmlRootElement(name = "user")
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 2543707612093688306L;

    private String uuid;
    private String openId;
    private String email;
    private String firstName;
    private String lastName;
    private String language;
    private Map<String, String> attributes;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
