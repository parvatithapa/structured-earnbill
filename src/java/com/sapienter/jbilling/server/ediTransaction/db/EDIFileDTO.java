package com.sapienter.jbilling.server.ediTransaction.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Entity
@TableGenerator(
        name = "edi_file_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "edi_file",
        allocationSize = 100
)
@Table(name = "edi_file")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDIFileDTO implements java.io.Serializable {
    private int id;
    private String name;
    private Date createDatetime;
    private CompanyDTO entity;
    private EDITypeDTO ediType;
    private TransactionType type;
    private List<EDIFileRecordDTO> ediFileRecords = new ArrayList<EDIFileRecordDTO>();
    private int versionNum;
    private EDIFileStatusDTO fileStatus;
    private EDIFileExceptionCodeDTO exceptionCode;
    private String comment;


    private UserDTO user;
    private Date startDate;
    private Date endDate;
    private String utilityAccountNumber;

    public EDIFileDTO() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_type_id")
    public EDITypeDTO getEdiType() {
        return ediType;
    }

    public void setEdiType(EDITypeDTO ediType) {
        this.ediType = ediType;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_file_GEN")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "ediFile")
    @OrderBy("id")
    public List<EDIFileRecordDTO> getEdiFileRecords() {
        return ediFileRecords;
    }

    public void setEdiFileRecords(List<EDIFileRecordDTO> ediFileRecords) {
        this.ediFileRecords = ediFileRecords;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_status_id")
    public EDIFileStatusDTO getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(EDIFileStatusDTO fileStatus) {
        this.fileStatus = fileStatus;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_exception_code_id")
    public EDIFileExceptionCodeDTO getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(EDIFileExceptionCodeDTO exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Column(name = "start_date")
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "utility_account_number")
    public String getUtilityAccountNumber() {
        return utilityAccountNumber;
    }

    public void setUtilityAccountNumber(String utilityAccountNumber) {
        this.utilityAccountNumber = utilityAccountNumber;
    }

    @Column(name = "end_date")
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "EDIFileDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createDatetime=" + createDatetime +
                ", versionNum=" + versionNum +
                '}';
    }

    public void setEDIFileRecord(EDIFileRecordDTO ediFileRecord) {
        this.getEdiFileRecords().add(ediFileRecord);
        ediFileRecord.setEdiFile(this);
    }
}
