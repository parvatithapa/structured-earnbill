package com.sapienter.jbilling.server.spa.cis;

import java.util.Date;

/**
 * Created by pablo on 19/06/17.
 */
public class DistOrderLine {
    
    private String id;
    private String description;
    private String category;
    private String status;
    private Date activeSince;
    private Date activeUntil;
    private String period;
    private String amount;

    public static final String ACTIVE = "Active";
    public static final String SUSPENDED = "Suspended";
    public static final String FINISHED = "Finished";
    public static final String PENDING = "PENDING";
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    public Date getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
    
    public int getStatusToCompare() {
        switch (status) {
            case ACTIVE : return 0;
            case PENDING : return 1;
            case FINISHED : return 2;
            case SUSPENDED : return 4;
        }
        return 5;
    }
}
