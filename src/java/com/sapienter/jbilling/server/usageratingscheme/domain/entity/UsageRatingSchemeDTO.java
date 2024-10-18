package com.sapienter.jbilling.server.usageratingscheme.domain.entity;


import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SortNatural;

import com.sapienter.jbilling.server.user.db.CompanyDTO;


@Entity
@Table(name = "usage_rating_scheme")
@TableGenerator(
        name = "usage_rating_scheme_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_rating_scheme",
        allocationSize = 1
)
public class UsageRatingSchemeDTO implements Serializable {

    private Integer id;
    private CompanyDTO entity;
    private String ratingSchemeCode;
    private UsageRatingSchemeTypeDTO ratingSchemeType;
    private Map<String, String> fixedAttributes;

    private boolean usesDynamicAttributes = false;
    private String dynamicAttributeName;
    private SortedSet<DynamicAttributeLineDTO> dynamicAttributes;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_rating_scheme_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    @Column(name = "rating_scheme_code", unique = true, nullable = false, length = 50)
    public String getRatingSchemeCode() {
        return ratingSchemeCode;
    }

    public void setRatingSchemeCode(String ratingSchemeCode) {
        this.ratingSchemeCode = ratingSchemeCode;
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_scheme_type_id")
    public UsageRatingSchemeTypeDTO getRatingSchemeType() {
        return ratingSchemeType;
    }

    public void setRatingSchemeType(UsageRatingSchemeTypeDTO ratingSchemeType) {
        this.ratingSchemeType = ratingSchemeType;
    }

    @Column(name = "uses_dynamic_attributes", nullable = false)
    public boolean getUsesDynamicAttributes() {
        return usesDynamicAttributes;
    }

    public boolean usesDynamicAttributes() {
        return usesDynamicAttributes;
    }

    public void setUsesDynamicAttributes(boolean usesDynamicAttributes) {
        this.usesDynamicAttributes = usesDynamicAttributes;
    }

    @Column(name = "dynamic_attribute_name")
    public String getDynamicAttributeName() {
        return dynamicAttributeName;
    }

    public void setDynamicAttributeName(String dynamicAttributeName) {
        this.dynamicAttributeName = dynamicAttributeName;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ratingScheme")
    @SortNatural
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public SortedSet<DynamicAttributeLineDTO> getDynamicAttributes() {
        return dynamicAttributes;
    }

    public void setDynamicAttributes(SortedSet<DynamicAttributeLineDTO> dynamicAttributes) {
        this.dynamicAttributes = dynamicAttributes;
    }

    public void setDynamicAttributes(Set<DynamicAttributeLineDTO> dynamicAttributes) {
        this.dynamicAttributes = new TreeSet<>(dynamicAttributes);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "usage_rating_scheme_fixed_attribute", joinColumns = @JoinColumn(name = "rating_scheme_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    @Fetch(FetchMode.SELECT)
    public Map<String, String> getFixedAttributes() {
        return fixedAttributes;
    }

    public void setFixedAttributes(Map<String, String> fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
          ToStringStyle.MULTI_LINE_STYLE);
    }
}
