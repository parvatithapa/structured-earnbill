/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation;

/**
 * Created by Andres Canevaro on 09/08/15.
 */

import javax.persistence.*;
import java.io.Serializable;

@Entity
@TableGenerator(
        name = "rating_scheme_association_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "rating_scheme_association_GEN",
        allocationSize = 50
)
@Table(name = "rating_scheme_association",
        uniqueConstraints=
        @UniqueConstraint(columnNames = {"rating_scheme", "mediation", "entity"}))
public class RatingSchemeAssociation implements Serializable{

    private Integer id;

    private Integer ratingIncrement;

    private Integer mediation;

    private Integer entity;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "rating_scheme_association_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "rating_scheme")
    public Integer getRatingIncrement() {
        return ratingIncrement;
    }

    public void setRatingIncrement(Integer ratingIncrement) {
        this.ratingIncrement = ratingIncrement;
    }

    @Column(name = "mediation")
    public Integer getMediation() {
        return mediation;
    }

    public void setMediation(Integer mediation) {
        this.mediation = mediation;
    }

    @Column(name = "entity")
    public Integer getEntity() {
        return entity;
    }

    public void setEntity(Integer entity) {
        this.entity = entity;
    }

}