package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Juan Vidal on 01/05/16.
 *
 * Template Meta Fields are linked to a Payment Method and a Company. This task copies this meta fields from the source
 * company and links them to the new company.
 */
public class PaymentMethodTemplateCopyTask extends AbstractCopyTask {
    PaymentMethodTemplateDAS paymentMethodTemplateDAS = null;
    MetaFieldDAS metaFieldDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentMethodTemplateCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<PaymentMethodTemplateDTO> copyPaymentMethodTemplateList = new PaymentMethodTemplateDAS().findAllByEntity(targetEntityId);
        LOG.debug("PaymentMethodTemplateCopyTask isTaskCopied: copyPaymentMethodTemplateList %s", copyPaymentMethodTemplateList);
        return copyPaymentMethodTemplateList != null && !copyPaymentMethodTemplateList.isEmpty();
    }

    public PaymentMethodTemplateCopyTask() {
        LOG.debug("Constructor PaymentMethodTemplateCopyTask");
        init();
    }

    private void init() {
        paymentMethodTemplateDAS = new PaymentMethodTemplateDAS();
        metaFieldDAS = new MetaFieldDAS();
    }


    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create PaymentMethodTemplateCopyTask");
        copyPaymentMethodTemplate(entityId, targetEntityId);
        LOG.debug("PaymentMethodTemplateCopyTask has been completed.");
    }

    private void copyPaymentMethodTemplate(Integer entityId, Integer targetEntityId) {
        CopyOnWriteArrayList<PaymentMethodTemplateDTO> copyPaymentMethodTemplateList = new CopyOnWriteArrayList<>(paymentMethodTemplateDAS.findAllByEntity(entityId));

        for (PaymentMethodTemplateDTO paymentMethodTemplate : copyPaymentMethodTemplateList) {
            CopyOnWriteArraySet<MetaField> metaFields = new CopyOnWriteArraySet<>(paymentMethodTemplate.getPaymentTemplateMetaFields());
            HashSet<String> copiedMetaFields = new HashSet<>();

            for (MetaField metaField : metaFields) {
                if(entityId.equals(metaField.getEntityId())) {
                    LOG.debug("Source MetaField -> %s", metaField);
                    if(CopyCompanyUtils.oldNewMetaFieldMap.containsKey(metaField.getId())) {
                        LOG.debug("Adding Created MetaField -> %s", metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(metaField.getId())));
                        paymentMethodTemplate.getPaymentTemplateMetaFields().add(metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(metaField.getId())));
                    } else if (!copiedMetaFields.contains(metaField.getName())) {
                        MetaFieldWS copyMetaFieldWS = MetaFieldBL.getWS(metaField);
                        copyMetaFieldWS.setId(0);
                        copyMetaFieldWS.setEntityId(targetEntityId);

                        if (copyMetaFieldWS.getValidationRule() != null) {
                            copyMetaFieldWS.getValidationRule().setId(0);
                        }

                        if (copyMetaFieldWS.getDefaultValue() != null) {
                            copyMetaFieldWS.getDefaultValue().setId(0);
                        }

                        MetaField copyMetaField = MetaFieldBL.getDTO(copyMetaFieldWS, targetEntityId);
                        copyMetaField = metaFieldDAS.save(copyMetaField);
                        LOG.debug("Adding Copied MetaField -> %s", metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(metaField.getId())));
                        paymentMethodTemplate.getPaymentTemplateMetaFields().add(copyMetaField);

                        copiedMetaFields.add(metaField.getName());
                    }
                }
            }
        }
    }
}