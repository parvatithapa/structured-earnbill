/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static com.sapienter.jbilling.server.util.audit.EventLogger.ADDRESS_LINE_1_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.ADDRESS_LINE_2_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.CITY_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.CONTACT_NO_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.COUNTRY_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.CUSTOMER_TYPE_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.EMAIL_ID_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.FIRST_NAME_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.GOVERNORATE_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.LAST_NAME_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.POSTAL_CODE_UPDATED;
import static com.sapienter.jbilling.server.util.audit.EventLogger.STATE_UPDATED;

@Getter
public enum MessageIdMap {
    CUSTOMER_TYPE("Customer Type", CUSTOMER_TYPE_UPDATED),
    GOVERNORATE("Governorate", GOVERNORATE_UPDATED),
    FIRST_NAME("First Name", FIRST_NAME_UPDATED),
    LAST_NAME("Last Name", LAST_NAME_UPDATED),
    ADDRESS_LINE_1("Address Line 1", ADDRESS_LINE_1_UPDATED),
    ADDRESS_LINE_2("Address Line 2", ADDRESS_LINE_2_UPDATED),
    CITY("City", CITY_UPDATED),
    STATE("State", STATE_UPDATED),
    POSTAL_CODE("Postal Code", POSTAL_CODE_UPDATED),
    COUNTRY("Country", COUNTRY_UPDATED),
    CONTACT_NO("Contact Number", CONTACT_NO_UPDATED),
    EMAIL_ID("Email Id", EMAIL_ID_UPDATED);
    private static final Map<String, MessageIdMap> ID_BY_NAME = new HashMap<>();

    static {
        for (MessageIdMap message : values()) {
            ID_BY_NAME.put(message.getName(), message);
        }
    }

    private final int value;
    private final String name;

    MessageIdMap(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static MessageIdMap fromName(String name) {
        return ID_BY_NAME.get(name);
    }
}
