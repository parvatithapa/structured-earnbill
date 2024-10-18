package com.sapienter.jbilling.server.sql.api.db;

import java.io.Serializable;

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
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.sapienter.jbilling.server.sql.api.db.ParameterType;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;

@SuppressWarnings("serial")
@Entity
@Table(name = "pre_evaluated_sql_parameter")
@TableGenerator(
        name = "pre_evaluated_sql_parameter_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "pre_evaluated_sql_parameter",
        allocationSize = 1
)
public class PreEvaluatedSQLParameterDTO implements Serializable {
    
    private Integer id;
    private String parameterName;
    private ParameterType parameterType;
    private PreEvaluatedSQLDTO preEvaluatedSQL;
    
    public PreEvaluatedSQLParameterDTO() {
	
    }
    
    public PreEvaluatedSQLParameterDTO(String parameterName, ParameterType parameterType) {
	this.parameterName = parameterName;
	this.parameterType = parameterType;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "pre_evaluated_sql_parameter_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    @Column(name="parameter_name", unique=false, nullable= false )
    public String getParameterName() {
        return parameterName;
    }
    
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_type", nullable = false, length = 25)
    public ParameterType getParameterType() {
        return parameterType;
    }
    
    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="pre_evaluated_sql_id", nullable=false)
    public PreEvaluatedSQLDTO getPreEvaluatedSQL() {
        return preEvaluatedSQL;
    }

    public void setPreEvaluatedSQL(PreEvaluatedSQLDTO preEvaluatedSQL) {
        this.preEvaluatedSQL = preEvaluatedSQL;
    }

    @Override
    public String toString() {
	return "PreEvaluatedSQLParameterDTO [id=" + id + ", parameterName="
		+ parameterName + ", parameterType=" + parameterType + "]";
    }
    
}
