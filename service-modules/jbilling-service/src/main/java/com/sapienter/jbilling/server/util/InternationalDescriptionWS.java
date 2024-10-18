/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.util;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * InternationalDescriptionWS
 *
 * @author Brian Cowdery
 * @since 27/01/11
 */
@ApiModel(value = "International Description Model", description = "International Description Model")
public class InternationalDescriptionWS implements Serializable{

    private static final long serialVersionUID = 20130704L;

    private String psudoColumn;
    private Integer languageId;
    private String content;
    private boolean deleted;

    public InternationalDescriptionWS() {
    }

    public InternationalDescriptionWS(Integer languageId, String content) {
        this.psudoColumn = "description";
        this.languageId = languageId;
        this.content = content;
    }

    public InternationalDescriptionWS(String psudoColumn, Integer languageId, String content) {
        this.psudoColumn = psudoColumn;
        this.languageId = languageId;
        this.content = content;
    }

    /**
     * Alias for {@link #getPsudoColumn()}
     * @return psudo-column label
     */
    @ApiModelProperty(value = "Psudo column name")
    public String getLabel() {
        return getPsudoColumn();
    }

    /**
     * Alias for {@link #setPsudoColumn(String)}
     * @param label psudo-column label string
     */
    public void setLabel(String label) {
        setPsudoColumn(label);
    }

    @ApiModelProperty("Psudo column name")
    public String getPsudoColumn() {
        return psudoColumn;
    }

    public void setPsudoColumn(String psudoColumn) {
        this.psudoColumn = psudoColumn;
    }

    @ApiModelProperty("The id of the description language")
    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    @ApiModelProperty("Description content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @ApiModelProperty(value = "Is this description deleted or not")
    @JsonProperty(value = "deleted")
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "InternationalDescriptionWS{"
               + ", psudoColumn='" + psudoColumn + '\''
               + ", languageId=" + languageId
               + ", content='" + content + '\''
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InternationalDescriptionWS that = (InternationalDescriptionWS) o;

        if (deleted != that.deleted) return false;
        if (psudoColumn != null ? !psudoColumn.equals(that.psudoColumn) : that.psudoColumn != null) return false;
        if (languageId != null ? !languageId.equals(that.languageId) : that.languageId != null) return false;
        return !(content != null ? !content.equals(that.content) : that.content != null);

    }

    @Override
    public int hashCode() {
        int result = psudoColumn != null ? psudoColumn.hashCode() : 0;
        result = 31 * result + (languageId != null ? languageId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (deleted ? 1 : 0);
        return result;
    }
}
