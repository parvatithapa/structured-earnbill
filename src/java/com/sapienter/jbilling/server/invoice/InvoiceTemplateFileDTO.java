package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.util.Constants;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * @author Klim Sviridov
 */
@Entity
@Table(name = Constants.TABLE_INVOICE_TEMPLATE_FILE)
@TableGenerator(
        name=Constants.TABLE_INVOICE_TEMPLATE_FILE + "_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue=Constants.TABLE_INVOICE_TEMPLATE,
        allocationSize = 10
)
public class InvoiceTemplateFileDTO {

    private long id;
    private InvoiceTemplateDTO template;
    private String name;
    private byte[] data;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = Constants.TABLE_INVOICE_TEMPLATE_FILE + "_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(cascade = CascadeType.ALL, targetEntity = InvoiceTemplateDTO.class)
    @JoinColumn(name = "invoice_template_id")
    public InvoiceTemplateDTO getTemplate() {
        return template;
    }

    public void setTemplate(InvoiceTemplateDTO template) {
        this.template = template;
    }

    @Column(nullable = false, name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable = false, name = "data")
    @Basic(fetch = FetchType.LAZY)
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
