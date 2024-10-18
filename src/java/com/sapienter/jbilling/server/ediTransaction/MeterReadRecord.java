package com.sapienter.jbilling.server.ediTransaction;

import java.util.Date;

/**
 * Created by neeraj on 19/9/15.
 */
public class MeterReadRecord {
    Integer totalConsumption;
    Date startDate;
    Date endDate;

    public Integer getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(Integer totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "MeterReadRecord{" +
                "totalConsumption=" + totalConsumption +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
