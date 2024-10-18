package com.sapienter.jbilling.server.usageratingscheme.domain.entity;

import java.io.Serializable;
import java.util.Map;

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
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;


@Entity
@Table(name = "usage_rating_scheme_dynamic_attribute_line")
@TableGenerator(
        name = "dynamic_attribute_line_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_rating_scheme_dynamic_attribute_line",
        allocationSize = 1
)

public class DynamicAttributeLineDTO implements Serializable, Comparable<DynamicAttributeLineDTO> {

    private Integer id;
    private Integer sequence;
    private Map<String, String> attributes;
    private UsageRatingSchemeDTO ratingScheme;


    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "dynamic_attribute_line_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "sequence", nullable = false)
    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "usage_rating_scheme_dynamic_attribute_map",
            joinColumns = @JoinColumn(name = "line_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    @Fetch(FetchMode.SELECT)
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_scheme_id")
    public UsageRatingSchemeDTO getRatingScheme() {
        return ratingScheme;
    }

    public void setRatingScheme(UsageRatingSchemeDTO ratingScheme) {
        this.ratingScheme = ratingScheme;
    }

    @Override
    public int compareTo(DynamicAttributeLineDTO o) {
        if (o == null) return -1;

        return this.sequence.compareTo(o.sequence);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("DynamicAttributeLineDTO { ")
                .append("id=" )
                .append(id)
                .append(", attributes=")
                .append(attributes)
                .append(" }")
                .toString();
    }
}
