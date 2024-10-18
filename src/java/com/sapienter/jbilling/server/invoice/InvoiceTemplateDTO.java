package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author elmot
 */
@Entity
@Table(name = Constants.TABLE_INVOICE_TEMPLATE)
@TableGenerator(
        name=Constants.TABLE_INVOICE_TEMPLATE + "_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue=Constants.TABLE_INVOICE_TEMPLATE,
        allocationSize = 10,
        uniqueConstraints = @UniqueConstraint(name = "invoice_template_uq_name_entity", columnNames = {"name", "entity_id"})
)
public class InvoiceTemplateDTO extends AbstractDescription implements Serializable {

    private int id;
    private Integer invoiceId;
    private String templateJson;
    private String name;
    private CompanyDTO entity;
    private Set<CustomerDTO> customers = new HashSet<CustomerDTO>(0);
    private Set<InvoiceTemplateVersionDTO> invoiceTemplateVersions = new HashSet<InvoiceTemplateVersionDTO>(0);

    private boolean includeCarriedInvoiceLines;

    @Transient
    public boolean getIncludeCarriedInvoiceLines() {
        return includeCarriedInvoiceLines;
    }

    public void setIncludeCarriedInvoiceLines(boolean includeCarriedInvoiceLines) {
        this.includeCarriedInvoiceLines = includeCarriedInvoiceLines;
    }


    public InvoiceTemplateDTO() {
    }

    public InvoiceTemplateDTO(int id, String name, String templateJson, CompanyDTO entity, Set<CustomerDTO> customers) {
        this.id = id;
        this.name = name;
        this.templateJson = templateJson;
        this.entity = entity;
        this.customers = customers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = Constants.TABLE_INVOICE_TEMPLATE + "_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Transient
    public String getTemplateJson() {
        return templateJson;
    }

    public void setTemplateJson(String templateJson) {
        this.templateJson = templateJson;
    }

    @Column(nullable = false, name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "invoice_id")
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    @Override
    @Transient
    protected String getTable() {
        return Constants.TABLE_INVOICE_TEMPLATE;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoiceTemplate")
    public Set<CustomerDTO> getCustomers() {
        return customers;
    }

    public void setCustomers(Set<CustomerDTO> customers) {
        this.customers = customers;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoiceTemplate")
    public Set<InvoiceTemplateVersionDTO> getInvoiceTemplateVersions() {
        return invoiceTemplateVersions;
    }

    public void setInvoiceTemplateVersions(Set<InvoiceTemplateVersionDTO> invoiceTemplateVersions) {
        this.invoiceTemplateVersions = invoiceTemplateVersions;
    }

    public void setTemplateJson(Integer invoiceTemplateVersionId) {
        InvoiceTemplateVersionDTO requiredTemplateVersion = null;
        for(InvoiceTemplateVersionDTO templateVersion : this.getInvoiceTemplateVersions()){
            if(templateVersion.getId().equals(invoiceTemplateVersionId)){
                requiredTemplateVersion = templateVersion;
                break;
            }
        }

        if (requiredTemplateVersion != null) {
            this.templateJson = requiredTemplateVersion.getTemplateJson();
            this.includeCarriedInvoiceLines = requiredTemplateVersion.getIncludeCarriedInvoiceLines();
        } else {
            this.templateJson = null;
            this.includeCarriedInvoiceLines = false;
        }
    }
}
