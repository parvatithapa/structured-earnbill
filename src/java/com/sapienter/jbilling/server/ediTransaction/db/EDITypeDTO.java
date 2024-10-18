package com.sapienter.jbilling.server.ediTransaction.db;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@TableGenerator(
        name="edi_type_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="edi_type",
        allocationSize = 1
)
@Table(name = "edi_type")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EDITypeDTO implements java.io.Serializable
{
    private int id;
    private String name;
    private String path;
    private Integer global;
    private CompanyDTO entity;
    private int versionNum;
    private Date createDatetime;
    private Set<CompanyDTO> entities = new HashSet<CompanyDTO>(0);
    private Set<EDIFileStatusDTO> statuses = new HashSet<EDIFileStatusDTO>(0);
    private String ediSuffix;

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "edi_type_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "path", nullable = false)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    @Column(name = "global", nullable = false)
    public Integer getGlobal() {
        return global;
    }

    public void setGlobal(Integer global) {
        this.global = global;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "edi_type_entity_map", joinColumns = {
            @JoinColumn(name = "edi_type_id", updatable = true) }, inverseJoinColumns = {
            @JoinColumn(name = "entity_id", updatable = true) })
    public Set<CompanyDTO> getEntities() {
        return this.entities;
    }

    public void setEntities(Set<CompanyDTO> entities) {
        this.entities = entities;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "edi_type_edi_status_map",
            joinColumns = @JoinColumn(name = "edi_type_id"),
            inverseJoinColumns = @JoinColumn(name = "edi_status_id")
    )
    public Set<EDIFileStatusDTO> getStatuses() {
        return statuses;
    }

    public void setStatuses(Set<EDIFileStatusDTO> statuses) {
        this.statuses = statuses;
    }

    @Column(name = "edi_suffix", nullable = false)
    public String getEdiSuffix() {
        return ediSuffix;
    }

    public void setEdiSuffix(String ediSuffix) {
        this.ediSuffix = ediSuffix;
    }

    @Override
    public String toString() {
        return "EDITypeDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", global=" + global +
                ", entity=" + entity +
                ", versionNum=" + versionNum +
                ", createDatetime=" + createDatetime +
                '}';
    }
}
