package com.sapienter.jbilling.server.sql.api.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterDTO;

@SuppressWarnings("serial")
@Entity
@Table(name = "pre_evaluated_sql")
@TableGenerator(
        name = "pre_evaluated_sql_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "pre_evaluated_sql",
        allocationSize = 1
)
public class PreEvaluatedSQLDTO implements Serializable {
    private Integer id;
    private String queryCode;
    private String sqlQuery;
    private String functionalDescription;
    private Integer parentEntityId;
    private List<PreEvaluatedSQLParameterDTO> parameters;
    
    public PreEvaluatedSQLDTO() {
    	parameters = new ArrayList<PreEvaluatedSQLParameterDTO>();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "pre_evaluated_sql_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "query_code", nullable =false, unique= true)
    public String getQueryCode() {
        return queryCode;
    }

    public void setQueryCode(String queryCode) {
        this.queryCode = queryCode;
    }

    @Column(name = "sql_query", nullable =false, unique= true)
    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    @Column(name = "functional_description")
    public String getFunctionalDescription() {
        return functionalDescription;
    }

    public void setFunctionalDescription(String functionalDescription) {
        this.functionalDescription = functionalDescription;
    }

    @Column(name = "parent_entity_id")
    public Integer getParentEntityId() {
        return parentEntityId;
    }

    public void setParentEntityId(Integer parentEntityId) {
        this.parentEntityId = parentEntityId;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "preEvaluatedSQL")
    @Column(nullable=true)
    public List<PreEvaluatedSQLParameterDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<PreEvaluatedSQLParameterDTO> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
	return "PreEvaluatedSQLDTO [id=" + id + ", queryCode=" + queryCode
		+ ", parentEntityId=" + parentEntityId + "]";
    }
    
    @Transient
    public PreEvaluatedSQLParameterDTO getParameterByName(String parameterName) {
    	for(PreEvaluatedSQLParameterDTO parameter: getParameters()) {
    		if(parameter.getParameterName().equals(parameterName)) {
    			return parameter;
    		}
    	}
    	return null;
    }
    
    @Transient
    public boolean isParameterRequired() {
    	return (getParameters()!=null && getParameters().size()!=0);
    }
}
