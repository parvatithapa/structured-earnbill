package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "company_info")
@TableGenerator(
        name="company_info_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="company_info",
        allocationSize = 100
)
public class CompanyInfoTypeMetaField implements java.io.Serializable{

	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "company_info_GEN")
    @Column(name = "id", unique = true, nullable = false)
	private int id;

	@ManyToOne
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyDTO company;

	@ManyToOne
	@JoinColumn(name = "company_info_type_id", nullable = true)
    private CompanyInformationTypeDTO companyInfoType;

	@ManyToOne(cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@JoinColumn(name = "meta_field_value_id", nullable = false)
    private MetaFieldValue metaFieldValue;

	public CompanyInfoTypeMetaField() {}

	public CompanyInfoTypeMetaField(CompanyDTO company, CompanyInformationTypeDTO companyInfoType, MetaFieldValue value) {
    	this.company = company;
    	this.companyInfoType = companyInfoType;
		this.metaFieldValue = value;
    }
	
	public CompanyInformationTypeDTO getCompanyInfoType() {
		return companyInfoType;
	}
	
	public MetaFieldValue getMetaFieldValue() {
		return metaFieldValue;
	}
	
	public void setMetaFieldValue(MetaFieldValue value) {
		this.metaFieldValue = value;
	}
}
