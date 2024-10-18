package com.sapienter.jbilling.server.dt;

/**
 * Created by Taimoor Choudhary on 4/9/18.
 */
public class PlanUsagePool {

    private Integer usagePoolId;
    private int usagePoolQuantity;
    private String usagePoolName;

    public PlanUsagePool() {
    }

    public Integer getUsagePoolId() {
        return usagePoolId;
    }

    public void setUsagePoolId(Integer usagePoolId) {
        this.usagePoolId = usagePoolId;
    }

    public int getUsagePoolQuantity() {
        return usagePoolQuantity;
    }

    public void setUsagePoolQuantity(int usagePoolQuantity) {
        this.usagePoolQuantity = usagePoolQuantity;
    }

    public String getUsagePoolName() {
        return usagePoolName;
    }

    public void setUsagePoolName(String usagePoolName) {
        this.usagePoolName = usagePoolName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PlanUsagePool{");
        sb.append("usagePoolId=").append(usagePoolId);
        sb.append(", usagePoolQuantity=").append(usagePoolQuantity);
        sb.append(", usagePoolName='").append(usagePoolName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
