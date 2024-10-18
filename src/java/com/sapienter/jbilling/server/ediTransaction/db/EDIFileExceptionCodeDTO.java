package com.sapienter.jbilling.server.ediTransaction.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * Created by aman on 25/9/15.
 */
@Entity
@TableGenerator(
        name = "edi_file_exception_code_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "edi_file_exception_code",
        allocationSize = 1
)
@Table(name = "edi_file_exception_code")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDIFileExceptionCodeDTO {
    private int id;
    private String exceptionCode;
    private String description;
    private EDIFileStatusDTO status;
    private int versionNum;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_file_exception_code_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "exception_code", nullable = false)
    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Column(name = "description", nullable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    public EDIFileStatusDTO getStatus() {
        return status;
    }

    public void setStatus(EDIFileStatusDTO status) {
        this.status = status;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "EDIFileExceptionCodeDTO{" +
                "id=" + id +
                ",exceptionCode='" + exceptionCode +
                ",description='" + description  +
                ",status=" + status +
                '}';
    }
}
