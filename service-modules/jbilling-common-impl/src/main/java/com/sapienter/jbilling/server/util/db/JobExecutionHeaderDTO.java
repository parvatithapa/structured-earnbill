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
package com.sapienter.jbilling.server.util.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@TableGenerator(
        name="job_execution_header_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="job_execution_header",
        allocationSize = 10
)
@Table(name="job_execution_header")
@NamedQueries({
        @NamedQuery(name = "JobExecutionHeaderDTO.findByExecutionId",
                query = "FROM JobExecutionHeaderDTO AS h " +
                        "WHERE h.jobExecutionId = :executionId ")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class JobExecutionHeaderDTO implements Serializable {

    public enum Status {STARTED, SUCCESS, STOPPED, ERROR, UNKNOWN}
    private int id;
    private long jobExecutionId;
    private String jobType;
    private Date startDate;
    private Date endDate;
    private Status status = Status.UNKNOWN;
    private List<JobExecutionLineDTO> lines = new ArrayList<>(0);
    private Integer entityId;

    public JobExecutionHeaderDTO() {
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator = "job_execution_header_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name="job_execution_id")
    public long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    @Column(name="entity_id")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Column(name="job_type", nullable = false)
    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Column(name="start_date", nullable = false)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name="end_date")
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Column(name="status", nullable = false)
    @Enumerated(EnumType.STRING)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="header")
    @org.hibernate.annotations.OrderBy(clause="id")
    public List<JobExecutionLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<JobExecutionLineDTO> lines) {
        this.lines = lines;
    }

    @Override
    public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append("JobExecutionHeaderDTO [id=").append(id).append(", jobExecutionId=").append(jobExecutionId).append(", ");
        if (jobType != null) {
            builder.append("jobType=").append(jobType).append(", ");
        }
        if (startDate != null) {
            builder.append("startDate=").append(startDate).append(", ");
        }
        if (endDate != null) {
            builder.append("endDate=").append(endDate);
        }
        if (status != null) {
            builder.append("status=").append(status);
        }
        builder.append("]");
        return builder.toString();
    }
}
