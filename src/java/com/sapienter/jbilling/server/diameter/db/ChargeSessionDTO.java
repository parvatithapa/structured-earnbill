package com.sapienter.jbilling.server.diameter.db;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Entity
@TableGenerator(
        name = "charge_session_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "charge_session",
        allocationSize = 100
)
// No cache, mutable and critical
@Table(name = "charge_sessions")
public class ChargeSessionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private UserDTO baseUser;
    private String sessionId;
    private Date started = TimezoneHelper.serverCurrentDate();
    private Date lastAccessed = TimezoneHelper.serverCurrentDate();
    private BigDecimal carriedUnits = BigDecimal.ZERO;

    private Set<ReservedAmountDTO> reservations = new HashSet<ReservedAmountDTO>();

    public ChargeSessionDTO () {
    }

    public ChargeSessionDTO (UserDTO user, String sessionId) {
        this.baseUser = user;
        this.sessionId = sessionId;
    }

    public void addNewReservation (CurrencyDTO currency, ItemDTO item, BigDecimal amount,
                                   BigDecimal quantity, PricingField[] params) {
        ReservedAmountDTO reservation = new ReservedAmountDTO(this, currency, amount,
                quantity, item, params);
        reservations.add(reservation);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "charge_session_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId () {
        return this.id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getBaseUser () {
        return this.baseUser;
    }

    public void setBaseUser (UserDTO baseUser) {
        this.baseUser = baseUser;
    }

    @Column(name = "session_token", nullable = false, length = 150)
    public String getSessionId () {
        return sessionId;
    }

    public void setSessionId (String sessionId) {
        this.sessionId = sessionId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ts_started")
    public Date getStarted () {
        return started;
    }

    public void setStarted (Date started) {
        this.started = started;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ts_last_access")
    public Date getLastAccessed () {
        return lastAccessed;
    }

    public void setLastAccessed (Date lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "session")
    public Set<ReservedAmountDTO> getReservations () {
        return reservations;
    }

    public void setReservations (Set<ReservedAmountDTO> reservations) {
        this.reservations = reservations;

    }

    @Column(name="carried_units", nullable=false, precision=17, scale=17)
    public BigDecimal getCarriedUnits () {
        return carriedUnits;
    }

    public void setCarriedUnits (BigDecimal carriedUnits) {
        this.carriedUnits = carriedUnits;
    }



    public ReservedAmountDTO findReservationByItem (ItemDTO item) {
        Iterator<ReservedAmountDTO> it = reservations.iterator();
        while (it.hasNext()) {
            ReservedAmountDTO reservation = (ReservedAmountDTO) it.next();
            if (reservation.getItem().getId() == item.getId()) {
                return reservation;
            }
        }
        return null;
    }
}
