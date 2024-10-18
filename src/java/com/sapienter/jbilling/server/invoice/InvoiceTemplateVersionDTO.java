package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.util.Constants;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author prashant
 */
@Entity
@Table(name = Constants.TABLE_INVOICE_TEMPLATE_VERSION)
@TableGenerator(
        name=Constants.TABLE_INVOICE_TEMPLATE_VERSION + "_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue=Constants.TABLE_INVOICE_TEMPLATE_VERSION,
        allocationSize = 1
)
public class InvoiceTemplateVersionDTO implements Serializable{

    private Integer id;
    private InvoiceTemplateDTO invoiceTemplate;
    private String versionNumber;
    private String tagName;
    private Date createdDatetime;
    private Integer size;
    private Integer userId;
    private String templateJson;
    private Boolean useForInvoice = Boolean.FALSE;
    private boolean includeCarriedInvoiceLines = false;

    public InvoiceTemplateVersionDTO(){}

    public InvoiceTemplateVersionDTO(String versionNumber, String tagName, Date createdDatetime, String templateJson) {
        this.versionNumber = versionNumber;
        this.tagName = tagName;
        this.createdDatetime = createdDatetime;
        this.templateJson = templateJson;
        this.size = templateJson.length();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = Constants.TABLE_INVOICE_TEMPLATE_VERSION + "_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "version_number", nullable = false, unique = true)
    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Column(name = "tag_name")
    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Column(name = "created_datetime", length = 29)
    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    @Column(name = "size")
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Column(name = "user_id")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Column(name = "template_json")
    @Basic(fetch = FetchType.LAZY)
    public String getTemplateJson() {
        return templateJson;
    }

    public void setTemplateJson(String templateJson) {
        this.templateJson = templateJson;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    public InvoiceTemplateDTO getInvoiceTemplate() {
        return invoiceTemplate;
    }

    public void setInvoiceTemplate(InvoiceTemplateDTO invoiceTemplate) {
        this.invoiceTemplate = invoiceTemplate;
    }

    @Column(name = "use_for_invoice")
    public Boolean getUseForInvoice() {
        return useForInvoice;
    }
    
    @Column(name = "include_carried_invoice_lines")
    public boolean getIncludeCarriedInvoiceLines() {
        return includeCarriedInvoiceLines;
    }

    public void setUseForInvoice(Boolean useForInvoice) {
        this.useForInvoice = useForInvoice;
    }
    
    public void setIncludeCarriedInvoiceLines(boolean includeCarriedInvoiceLines) {
        this.includeCarriedInvoiceLines = includeCarriedInvoiceLines;
    }
}
