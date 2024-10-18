package com.sapienter.jbilling.server.ediTransaction.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@TableGenerator(
        name="edi_file_field_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="edi_file_field",
        allocationSize = 100
)
@Table(name = "edi_file_field")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDIFileFieldDTO implements java.io.Serializable
{
    private int id;
    private String ediFileFieldKey;
    private String ediFileFieldValue;
    private Integer ediFileFieldOrder;
    private EDIFileRecordDTO ediFileRecord;
    private String comment;

    public EDIFileFieldDTO(String ediFileFieldKey, String ediFileFieldValue, String comment, Integer ediFileFieldOrder) {
        this.ediFileFieldKey = ediFileFieldKey;
        this.ediFileFieldValue = ediFileFieldValue;
        this.comment = comment;
        this.ediFileFieldOrder = ediFileFieldOrder;

    }

    public EDIFileFieldDTO(){}

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_file_field_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "edi_file_field_key", nullable = false)
    public String getEdiFileFieldKey() {
        return ediFileFieldKey;
    }

    public void setEdiFileFieldKey(String ediFileFieldKey) {
        this.ediFileFieldKey = ediFileFieldKey;
    }

    @Column(name = "edi_file_field_value")
    public String getEdiFileFieldValue() {
        return ediFileFieldValue;
    }

    public void setEdiFileFieldValue(String ediFileFieldValue) {
        this.ediFileFieldValue = ediFileFieldValue;
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_file_record_id")
    public EDIFileRecordDTO getEdiFileRecord() {
        return ediFileRecord;
    }

    public void setEdiFileRecord(EDIFileRecordDTO ediFileRecord) {
        this.ediFileRecord = ediFileRecord;
    }

    @Column(name = "comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Column(name = "field_order")
    public Integer getEdiFileFieldOrder() {
        return ediFileFieldOrder;
    }

    public void setEdiFileFieldOrder(Integer ediFileFieldOrder) {
        this.ediFileFieldOrder = ediFileFieldOrder;
    }

    @Override
    public String toString() {
        return "EDIFileFieldDTO{" +
                "id=" + id +
                ", key='" + ediFileFieldKey + '\'' +
                ", value='" + ediFileFieldValue + '\'' +
                ", order=" + ediFileFieldOrder +
                ", ediFileRecord=" + ediFileRecord +
                ", comment='" + comment + '\'' +
                '}';
    }
}
