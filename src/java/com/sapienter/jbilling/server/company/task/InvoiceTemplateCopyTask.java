package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDAS;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateVersionDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.EntityBL;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by Juan Vidal on 08Jan16
 * <p>
 * This task copies all Invoice Templates defined in the source company.
 */
public class InvoiceTemplateCopyTask extends AbstractCopyTask {
    InvoiceTemplateDAS invoiceTemplateDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(InvoiceTemplateCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<InvoiceTemplateDTO> invoiceTemplateDTOs = new InvoiceTemplateDAS().findAllInvoiceTemplateByEntity(targetEntityId);
        return invoiceTemplateDTOs != null && !invoiceTemplateDTOs.isEmpty();
    }

    public InvoiceTemplateCopyTask() {
        init();
    }

    private void init() {
        invoiceTemplateDAS = new InvoiceTemplateDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create InvoiceTemplateCopyTask");

        List<InvoiceTemplateDTO> invoiceTemplateDTOs = invoiceTemplateDAS.findAllInvoiceTemplateByEntity(entityId);

        for (InvoiceTemplateDTO invoiceTemplateDTO : invoiceTemplateDTOs) {
            InvoiceTemplateDTO copyInvoiceTemplateDTO = new InvoiceTemplateDTO();
            copyInvoiceTemplateDTO.setId(0);
            copyInvoiceTemplateDTO.setName(invoiceTemplateDTO.getName());
            copyInvoiceTemplateDTO.setEntity(new EntityBL(targetEntityId).getEntity());
            copyInvoiceTemplateDTO.setTemplateJson(invoiceTemplateDTO.getTemplateJson());
            copyInvoiceTemplateDTO = invoiceTemplateDAS.save(copyInvoiceTemplateDTO);

            if (invoiceTemplateDTO.getInvoiceTemplateVersions() != null) {
                int versionCount = 1;
                for (InvoiceTemplateVersionDTO invoiceTemplateVersionDTO : invoiceTemplateDTO.getInvoiceTemplateVersions()) {
                    InvoiceTemplateVersionDTO copyInvoiceTemplateVersionDTO = new InvoiceTemplateVersionDTO();
                    copyInvoiceTemplateVersionDTO.setId(0);
                    copyInvoiceTemplateVersionDTO.setTemplateJson(invoiceTemplateVersionDTO.getTemplateJson());
                    copyInvoiceTemplateVersionDTO.setCreatedDatetime(TimezoneHelper.serverCurrentDate());
                    copyInvoiceTemplateVersionDTO.setInvoiceTemplate(copyInvoiceTemplateDTO);
                    copyInvoiceTemplateVersionDTO.setSize(invoiceTemplateVersionDTO.getSize());
                    copyInvoiceTemplateVersionDTO.setTagName(invoiceTemplateVersionDTO.getTagName());
                    copyInvoiceTemplateVersionDTO.setUseForInvoice(invoiceTemplateVersionDTO.getUseForInvoice());

                    copyInvoiceTemplateVersionDTO.setVersionNumber(copyInvoiceTemplateDTO.getId() + "." + versionCount);
                    versionCount += 1;

                    copyInvoiceTemplateDTO.getInvoiceTemplateVersions().add(copyInvoiceTemplateVersionDTO);
                }
            }

            invoiceTemplateDAS.save(copyInvoiceTemplateDTO);
        }

        LOG.debug("InvoiceTemplateCopyTask has been completed.");
    }
}