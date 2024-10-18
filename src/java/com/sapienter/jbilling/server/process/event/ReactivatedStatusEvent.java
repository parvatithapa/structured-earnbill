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

package com.sapienter.jbilling.server.process.event;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;

import java.util.Date;

/**
 * This event is triggered when the user is reactivated
 *
 * @author Leandro Zoi
 * @since 01/51/18
 *
 */
public class ReactivatedStatusEvent implements Event {
    private UserDTO user;
    private Date date;
    private Integer entityId;
    private String userLoggedName;
    private String statusDescription;
    private UserStatusDTO status;

    public ReactivatedStatusEvent(UserDTO user, Date date, Integer entityId, String userLoggedName,
                                  String statusDescription, UserStatusDTO status) {
        this.user = user;
        this.date = date;
        this.entityId = entityId;
        this.userLoggedName = userLoggedName;
        this.statusDescription = statusDescription;
        this.status = status;
    }

    public UserDTO getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }

    public String getUserLoggedName() {
        return userLoggedName;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public UserStatusDTO getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return " Reactivated Status Event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
