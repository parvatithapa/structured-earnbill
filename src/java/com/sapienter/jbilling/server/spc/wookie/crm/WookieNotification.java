
package com.sapienter.jbilling.server.spc.wookie.crm;

import lombok.Data;

@Data
public class WookieNotification {

    private String crmAccountRecordId;
    private String crmOrderRecordId;
    private String crmServiceRecordId;
    private String vendorReference;
    private SendSms sendSms;

}
