package com.sapienter.jbilling.appdirect.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlRootElement(name = "usage")
@NoArgsConstructor
public class UsageBean implements Serializable {
    private static final long serialVersionUID = -227136513522373050L;

    /**
     * Account information
     */
    @Getter
    @Setter
    private AccountInfo account;

    /**
     * Add-on instance information
     */
    @Getter @Setter
    private AddonInstanceInfo addonInstance;

    /**
     * Date information
     */
    @Getter
    @Setter
    private Date date;

    /**
     * List of usage items
     */
    @Setter
    @XmlElement(name = "item")
    private List<UsageItemBean> items = new ArrayList<>();
}
