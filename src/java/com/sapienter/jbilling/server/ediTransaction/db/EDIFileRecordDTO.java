package com.sapienter.jbilling.server.ediTransaction.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@TableGenerator(
        name="edi_file_record_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="edi_file_record",
        allocationSize = 100
)
@Table(name = "edi_file_record")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDIFileRecordDTO implements java.io.Serializable
{
    private int id;
    private EDIFileDTO ediFile;
    private String ediFileRecordHeader;
    private Integer recordOrder;
    private Set<EDIFileFieldDTO> fileFields=new HashSet<EDIFileFieldDTO>();
    private Date creationTime;
    private int versionNum;
    private Integer totalFileField;
    private String comment;

    public EDIFileRecordDTO(){}

    public EDIFileRecordDTO(String ediFileRecordHeader, Integer recordOrder, Set<EDIFileFieldDTO> fileFields, EDIFileDTO ediFile) {
        this.ediFileRecordHeader = ediFileRecordHeader;
        this.recordOrder = recordOrder;
        this.fileFields = fileFields;
        this.ediFile = ediFile;
    }

    public EDIFileRecordDTO(String ediFileRecordHeader, Integer recordOrder, EDIFileDTO ediFile) {
        this.ediFileRecordHeader = ediFileRecordHeader;
        this.recordOrder = recordOrder;
        this.ediFile = ediFile;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_file_record_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
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


    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_file_id", nullable = false)
    public EDIFileDTO getEdiFile() {
        return ediFile;
    }

    public void setEdiFile(EDIFileDTO ediFile) {
        this.ediFile = ediFile;
    }

    @Column(name = "record_order", nullable = false)
    public Integer getRecordOrder() {
        return recordOrder;
    }

    public void setRecordOrder(Integer recordOrder) {
        this.recordOrder = recordOrder;
    }

    @Column(name = "edi_file_field_header", nullable = false)
    public String getEdiFileRecordHeader() {
        return ediFileRecordHeader;
    }

    public void setEdiFileRecordHeader(String ediFileRecordHeader) {
        this.ediFileRecordHeader = ediFileRecordHeader;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "ediFileRecord")
    public Set<EDIFileFieldDTO> getFileFields() {
        return fileFields;
    }

    public void setFileFields(Set<EDIFileFieldDTO> fileFields) {
        this.fileFields = fileFields;
    }

    @Column(name = "total_file_field")
    public Integer getTotalFileField() {
        return totalFileField;
    }

    public void setTotalFileField(Integer totalFileField) {
        this.totalFileField = totalFileField;
    }

    @Column(name = "comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "EDIFileRecordDTO{" +
                "id=" + id +
                ", header='" + ediFileRecordHeader + '\'' +
                ", recordOrder=" + recordOrder +
                ", creationTime=" + creationTime +
                '}';
    }

    public void setFileField(EDIFileFieldDTO fileField){
        this.getFileFields().add(fileField);
        fileField.setEdiFileRecord(this);
    }
}
