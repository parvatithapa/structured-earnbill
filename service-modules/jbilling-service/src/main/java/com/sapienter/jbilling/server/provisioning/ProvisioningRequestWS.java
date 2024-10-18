package com.sapienter.jbilling.server.provisioning;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Provisioning Request Data", description = "ProvisioningRequestWS model")
public class ProvisioningRequestWS implements WSSecured, Serializable {

    private Integer id;
    private String identifier;
    private Integer provisioningCommandId;
    private Integer entityId;
    private String processor;

    private Integer executionOrder;

    @ConvertToTimezone
    private Date createDate;
    @ConvertToTimezone
    private Date submitDate;

    private String submitRequest;
    private String rollbackRequest;

    private ProvisioningRequestStatus requestStatus;
    @ConvertToTimezone
    private Date resultReceivedDate;
    private Map<String, String> resultMap = new HashMap<String, String>();

    private int versionNum;

    @ApiModelProperty(value = "The id of the provisioning request entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The identifier of the provisioning request entity")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty(value = "The version number of the provisioning request entity")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @ApiModelProperty(value = "The rollback request of the provisioning request entity")
    public String getRollbackRequest() {
        return rollbackRequest;
    }

    public void setRollbackRequest(String rollbackRequest) {
        this.rollbackRequest = rollbackRequest;
    }

    @ApiModelProperty(value = "The processor of the provisioning request entity")
    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    @ApiModelProperty(value = "The id of the provisioning command")
    public Integer getProvisioningCommandId() {
        return provisioningCommandId;
    }

    public void setProvisioningCommandId(Integer provisioningCommandId) {
        this.provisioningCommandId = provisioningCommandId;
    }

    @ApiModelProperty(value = "The id of the company for which this entity is defined")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "The execution order of the provisioning request entity")
    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    @ApiModelProperty(value = "Date of creation")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @ApiModelProperty(value = "Date of submission")
    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    @ApiModelProperty(value = "The submit request of the provisioning request entity")
    public String getSubmitRequest() {
        return submitRequest;
    }

    public void setSubmitRequest(String submitRequest) {
        this.submitRequest = submitRequest;
    }

    @ApiModelProperty(value = "Request status")
    public ProvisioningRequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(ProvisioningRequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    @ApiModelProperty(value = "Date when the result was received")
    public Date getResultReceivedDate() {
        return resultReceivedDate;
    }

    public void setResultReceivedDate(Date resultReceivedDate) {
        this.resultReceivedDate = resultReceivedDate;
    }

    @ApiModelProperty(value = "The result map of the provisioning request entity")
    public Map<String, String> getResultMap() {
        return resultMap;
    }

    public void setResultMap(Map<String, String> resultMap) {
        this.resultMap = resultMap;
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

/*    @Override
    public String toString() {
        return "ProvisioningRequestedWS{"
                + "id=" + id
                + ", entity_id=" + entity
                + ", execution_order=" + executionOrder
                + ", create_date=" + createDate
                + ", last_update_date=" + lastUpdateDate
                + ", command_status=" + commandStatus
                + ", optlock=" + versionNum
                + '}';
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProvisioningRequestWS)) return false;

        ProvisioningRequestWS that = (ProvisioningRequestWS) o;

        return versionNum == that.versionNum &&
                nullSafeEquals(id, that.id) &&
                nullSafeEquals(identifier, that.identifier) &&
                nullSafeEquals(provisioningCommandId, that.provisioningCommandId) &&
                nullSafeEquals(entityId, that.entityId) &&
                nullSafeEquals(processor, that.processor) &&
                nullSafeEquals(executionOrder, that.executionOrder) &&
                nullSafeEquals(createDate, that.createDate) &&
                nullSafeEquals(submitDate, that.submitDate) &&
                nullSafeEquals(submitRequest, that.submitRequest) &&
                nullSafeEquals(rollbackRequest, that.rollbackRequest) &&
                nullSafeEquals(requestStatus, that.requestStatus) &&
                nullSafeEquals(resultReceivedDate, that.resultReceivedDate) &&
                nullSafeEquals(resultMap, that.resultMap);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(identifier);
        result = 31 * result + nullSafeHashCode(provisioningCommandId);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(processor);
        result = 31 * result + nullSafeHashCode(executionOrder);
        result = 31 * result + nullSafeHashCode(createDate);
        result = 31 * result + nullSafeHashCode(submitDate);
        result = 31 * result + nullSafeHashCode(submitRequest);
        result = 31 * result + nullSafeHashCode(rollbackRequest);
        result = 31 * result + nullSafeHashCode(requestStatus);
        result = 31 * result + nullSafeHashCode(resultReceivedDate);
        result = 31 * result + nullSafeHashCode(resultMap);
        result = 31 * result + versionNum;
        return result;
    }
}
