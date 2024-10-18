package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Account Information Type entity.
 * 
 * @author Aamir Ali
 * @since  02/21/2017
 */
@Entity
@DiscriminatorValue("COMPANY_INFO")
public class CompanyInformationTypeDTO extends MetaFieldGroup implements Serializable {

    @Column(name = "name", nullable = false, length = 100)
    @NotNull(message = "validation.error.notnull")
    @Size(min = 1, max = 100, message = "validation.error.size,1,100")
    private String name;

    @ManyToOne(targetEntity = CompanyDTO.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyDTO company;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "companyInfoType", cascade = CascadeType.ALL)
    private Set<CompanyInfoTypeMetaField> companyInfoTypeMetaFields = new HashSet<CompanyInfoTypeMetaField>(0);

    public CompanyInformationTypeDTO() {
        super();
    }

    public CompanyInformationTypeDTO(String name,
                                     CompanyDTO company) {
        super();

        this.name = name;
        this.company = company;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }

    public Set<CompanyInfoTypeMetaField> getCompanyInfoTypeMetaFields() {
        return this.companyInfoTypeMetaFields;
    }

    public void setCompanyInfoTypeMetaFields(Set<CompanyInfoTypeMetaField> companyInfoTypeMetaFields) {
        this.companyInfoTypeMetaFields = companyInfoTypeMetaFields;
    }
}
