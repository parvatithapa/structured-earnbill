package com.sapienter.jbilling.server.usageratingscheme.domain.entity;


import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;


@Entity
@Table(name = "usage_rating_scheme_type")
@TableGenerator(
        name = "usage_rating_scheme_type_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_rating_scheme_type",
        allocationSize = 1
)
public class UsageRatingSchemeTypeDTO {

    private Integer id;
    private String  name;
    private String  implClass;
    private List<UsageRatingSchemeDTO> usageRatingSchemes;
    private boolean cacheable = true;
    private boolean active;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_rating_scheme_type_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "name", unique = true, nullable = false, length = 128)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "impl_class", nullable = false)
    public String getImplClass() {
        return implClass;
    }

    public void setImplClass(String implClass) {
        this.implClass = implClass;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ratingSchemeType")
    public List<UsageRatingSchemeDTO> getUsageRatingSchemes() {
        return usageRatingSchemes;
    }

    public void setUsageRatingSchemes(List<UsageRatingSchemeDTO> usageRatingSchemes) {
        this.usageRatingSchemes = usageRatingSchemes;
    }

    @Column(name = "active", nullable = false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Column(name = "cacheable", nullable = false)
    public boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("UsageRatingSchemeTypeDTO { ")
                .append("id=")
                .append(id)
                .append(", name='")
                .append(name)
                .append('\'')
                .append(", implClass='" )
                .append(implClass)
                .append('\'')
                .append(", active=")
                .append(active)
                .append(" }")
                .toString();
    }
}
