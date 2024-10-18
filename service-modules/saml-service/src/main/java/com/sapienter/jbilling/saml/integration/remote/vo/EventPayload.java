package com.sapienter.jbilling.saml.integration.remote.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
public class EventPayload implements Serializable {
    private static final long serialVersionUID = 3080925569209286979L;

    private UserInfo user;
    private CompanyInfo company;
    private AccountInfo account;
    /*private AddonInstanceInfo addonInstance;
    private AddonBindingInfo addonBinding;*/
    private OrderInfo order;
    /*private NoticeInfo notice;*/
    private HashMap<String, String> configuration = new HashMap<>();
}
