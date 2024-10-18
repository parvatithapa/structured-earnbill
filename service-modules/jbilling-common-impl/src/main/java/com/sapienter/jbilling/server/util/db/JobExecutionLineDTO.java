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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.io.Serializable;
import java.util.Date;

@Entity
@TableGenerator(
        name="job_execution_line_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="job_execution_line",
        allocationSize = 10
)
@Table(name="job_execution_line")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class JobExecutionLineDTO implements Serializable {

    private int id;
    private JobExecutionHeaderDTO header;
    private String type;
    private String name;
    private String value;

    public JobExecutionLineDTO() {
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator = "job_execution_line_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="header_id")
    public JobExecutionHeaderDTO getHeader() {
        return header;
    }

    public void setHeader(JobExecutionHeaderDTO header) {
        this.header = header;
    }

    @Column(name="line_type", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name="name", length = 100, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="value", length = 100, nullable = false)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append("JobExecutionLineDTO [id=").append(id).append(", ");
        builder.append("type=").append(type).append(", ");
        builder.append("name=").append(name).append(", ");
        builder.append("value=").append(value).append(", ");
        builder.append("]");
        return builder.toString();
    }
}
