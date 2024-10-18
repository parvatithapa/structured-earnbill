package com.sapienter.jbilling.server.integration.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.springframework.http.HttpMethod;

import com.sapienter.jbilling.server.user.db.CompanyDTO;

@Entity
@TableGenerator(
        name            = "outbound_interchange_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "outbound_interchange"
)
@Table(name = "outbound_interchange")
public class OutBoundInterchange {

    private Integer id;
    private Integer userId;
    private CompanyDTO company;
    private String request;
    private String response;
    private Date createDateTime;
    private Date lastRetryDateTime;
    private String methodName;
    private HttpMethod httpMethod;
    private Integer retryCount;
    private Integer version;
    private Status status;
    private Integer source;

    public OutBoundInterchange() {
        this.createDateTime = new Date();
        this.status = Status.UNPROCESSED;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "outbound_interchange_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    @Column(name = "request", nullable = false)
    public String getRequest() {
        return request;
    }

    public void setRequest(final String request) {
        this.request = request;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(final CompanyDTO compnay) {
        this.company = compnay;
    }

    @Column(name = "response", nullable = true)
    public String getResponse() {
        return response;
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(final Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    @Column(name = "retry_count", nullable = true)
    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Column(name = "method_name", nullable = false)
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Version
    @Column(name = "optlock")
    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Column(name = "user_id", nullable = false)
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    @Column(name = "last_retry_datetime", nullable = true, length = 29)
    public Date getLastRetryDateTime() {
        return lastRetryDateTime;
    }

    public void setLastRetryDateTime(final Date lastRetryDateTime) {
        this.lastRetryDateTime = lastRetryDateTime;
    }

    @Transient
    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    @Transient
    public void incrementRetry() {
        if(null == retryCount) {
            retryCount = 1;
        } else {
            ++retryCount;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OutBoundInterchange [id=");
        builder.append(id);
        builder.append(", company=");
        builder.append(company);
        builder.append(", userId=");
        builder.append(userId);
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
        builder.append(", lastRetryDateTime=");
        builder.append(lastRetryDateTime);
        builder.append(", version=");
        builder.append(version);
        builder.append(", status=");
        builder.append(status);
        builder.append("]");
        return builder.toString();
    }
}