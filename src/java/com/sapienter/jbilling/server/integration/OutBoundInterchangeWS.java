package com.sapienter.jbilling.server.integration;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class OutBoundInterchangeWS implements Serializable {

    private Integer id;
    private Integer companyId;
    private String request;
    private String response;
    private Date createDateTime;
    private String methodName;
    private String httpMethod;
    private Integer retryCount;
    private String status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }


    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OutBoundInterchangeWS [id=");
        builder.append(id);
        builder.append(", companyId=");
        builder.append(companyId);
        builder.append(", request=");
        builder.append(request);
        builder.append(", response=");
        builder.append(response);
        builder.append(", createDateTime=");
        builder.append(createDateTime);
        builder.append(", methodName=");
        builder.append(methodName);
        builder.append(", httpMethod=");
        builder.append(httpMethod);
        builder.append(", retryCount=");
        builder.append(retryCount);
        builder.append(", status=");
        builder.append(status);
        builder.append("]");
        return builder.toString();
    }

}
