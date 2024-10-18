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

/**
 * This event is triggered when a user's status is changed.
 */
public class NewUserStatusEvent implements Event {
    private Integer entityId;
    private Integer userId;
    private Integer oldStatusId;
    private Integer newStatusId;
    private UserDTO user;

    public NewUserStatusEvent(Integer entityId, Integer userId, 
            Integer oldStatusId, Integer newStatusId) {
        this.entityId = entityId;
        this.userId = userId;
        this.oldStatusId = oldStatusId;
        this.newStatusId = newStatusId;
    }
    
    public NewUserStatusEvent(UserDTO user, Integer entityId, 
    		Integer oldStatusId, Integer newStatusId) {
        this.entityId = entityId;
        this.userId = user.getId();
        this.oldStatusId = oldStatusId;
        this.newStatusId = newStatusId;
        this.user = user;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getUserId() {
        return userId;
    }
    
    public UserDTO getUser(){
    	return user;
    }

    /**
     * Returns the status id (status_value) of the users original state before the status
     * was changed and the event fired.
     *
     * @see com.sapienter.jbilling.server.util.db.AbstractGenericStatus#getId()
     *
     * @return users original status id
     */
    public Integer getOldStatusId() {
        return oldStatusId;
    }

    /**
     * Returns the new status id (status_value) of the users newly assigned status.
     *
     * @see com.sapienter.jbilling.server.util.db.AbstractGenericStatus#getId()
     *
     * @return users new status id
     */
    public Integer getNewStatusId() {
        return newStatusId;
    }

    public String getName() {
        return "New User Status Event";
    }
    
    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
