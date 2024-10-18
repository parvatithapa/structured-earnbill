package com.sapienter.jbilling.server.item.db;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;


@Entity
@TableGenerator(
        name = "rating_configuration_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "rating_configuration",
        allocationSize = 1
)
@Table(name = "rating_configuration")
public class RatingConfigurationDTO  extends AbstractDescription implements Serializable {



    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "rating_configuration_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int id;

    @Column(name = "usage_rating_scheme")
    private Integer usageRatingScheme;


    @Column(name = "rating_unit")
    private Integer ratingUnit;


    @Column(name = "active", nullable = false)
    private Boolean active;

    public Boolean isActive(){
        return this.active;
    }


    public Integer getUsageRatingScheme() {
        return usageRatingScheme;
    }

    public void setUsageRatingScheme(Integer usageRatingScheme) {
        this.usageRatingScheme = usageRatingScheme;
    }

    public Integer getRatingUnit() {
        return ratingUnit;
    }

    public void setRatingUnit(Integer ratingUnit) {
        this.ratingUnit = ratingUnit;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    @Override
    public int getId() {
    return this.id;
    }

    @Override
    protected String getTable() {

        return Constants.TABLE_RATING_CONFIGURATION;

    }

    public void setId(int id) {
        this.id = id;
    }
}
