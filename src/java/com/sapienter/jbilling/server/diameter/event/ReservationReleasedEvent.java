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

package com.sapienter.jbilling.server.diameter.event;

import com.sapienter.jbilling.server.diameter.db.ReservedAmountDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class ReservationReleasedEvent implements Event {
    private Integer entityId;
    private ReservedAmountDTO reservation;

    public ReservationReleasedEvent (Integer entityId, ReservedAmountDTO reservation) {
        this.entityId = entityId;
        this.reservation = reservation;
    }
    
    public String getName() {
        return "Reservation Released";
    }

    public ReservedAmountDTO getReservation () {
        return reservation;
    }

    @Override
    public Integer getEntityId () {
        return entityId;
    }

    @Override
    public String toString() {
        return "ReservationReleasedEvent{"
                + "reservation=" + reservation.getId()
                + ", amount=" + reservation.getAmount()
                + "}";
    }
    
}
