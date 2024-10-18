package com.sapienter.jbilling.server.ediTransaction.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OrderBy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@TableGenerator(
        name = "edi_file_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "edi_file_status",
        allocationSize = 1
)
@Table(name = "edi_file_status")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDIFileStatusDTO implements java.io.Serializable {
    private int id;
    private String name;
    private Date createDatetime;
    private int versionNum;
    private boolean isError = false;
    private List<EDIFileExceptionCodeDTO> exceptionCodes = new ArrayList<EDIFileExceptionCodeDTO>(0);
    private List<EDIFileStatusDTO> associatedEDIStatuses = new ArrayList<EDIFileStatusDTO>(0);

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_file_status_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name = "is_error", nullable = false)
    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "status")
    @OrderBy(clause = "id")
    public List<EDIFileExceptionCodeDTO> getExceptionCodes() {
        return exceptionCodes;
    }

    public void setExceptionCodes(List<EDIFileExceptionCodeDTO> exceptionCodes) {

        this.exceptionCodes=exceptionCodes;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "associated_edi_status_edi_status_map",
            joinColumns = @JoinColumn(name = "edi_status_id"),
            inverseJoinColumns = @JoinColumn(name = "associated_edi_status_id")
    )
    public List<EDIFileStatusDTO> getAssociatedEDIStatuses() {
        return associatedEDIStatuses;
    }

    public void setAssociatedEDIStatuses(List<EDIFileStatusDTO> ediFileStatuses) {
        this.associatedEDIStatuses=ediFileStatuses;
    }

    @Override
    public String toString() {
        return "EDIFileStatusDTO{" +
                "id=" + id +
                ", status='" + name + '\'' +
                ", createDatetime=" + createDatetime +
                ", versionNum=" + versionNum +
                '}';
    }
}
