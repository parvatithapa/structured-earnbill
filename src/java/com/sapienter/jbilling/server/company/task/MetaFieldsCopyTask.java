package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by vivek on 30/10/14.
 */
public class MetaFieldsCopyTask extends AbstractCopyTask {

    MetaFieldDAS metaFieldDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MetaFieldsCopyTask.class));

    private static final Class dependencies[] = new Class[]{
            DataTableCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        Long metaFields = metaFieldDAS.countMetaFieldsByEntity(targetEntityId);
        return !(metaFields == 0);
    }

    public MetaFieldsCopyTask() {
        init();
    }

    private void init() {
        metaFieldDAS = new MetaFieldDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create MetaFieldsCopyTask");
        copyMetaFields(entityId, targetEntityId);
        LOG.debug("MetaFieldsCopyTask has been completed.");
    }

    public void copyMetaFields(Integer entityId, Integer targetEntityId) {
        List<MetaField> metaFields = metaFieldDAS.getMetaFieldsByEntity(entityId);
        Map<Integer, Integer[]> dependencyListMap = new HashMap<Integer, Integer[]>();
        for (MetaField metaField : metaFields) {
            MetaFieldWS copyMetaFieldWS = MetaFieldBL.getWS(metaField);
            copyMetaFieldWS.setId(0);
            copyMetaFieldWS.setEntityId(targetEntityId);

            if (copyMetaFieldWS.getDataTableId() != null) {
                LOG.debug("copyMetaFieldWS.getDataTableId()  " + copyMetaFieldWS.getDataTableId());
                LOG.debug("CopyCompanyUtils.oldNewDataTableMap.get(copyMetaFieldWS.getDataTableId())    " + CopyCompanyUtils.oldNewDataTableMap.get(copyMetaFieldWS.getDataTableId()));
                copyMetaFieldWS.setDataTableId(CopyCompanyUtils.oldNewDataTableMap.get(copyMetaFieldWS.getDataTableId()));
            }
            if(copyMetaFieldWS.getDependentMetaFields().length > 0) {
                dependencyListMap.put(metaField.getId(), copyMetaFieldWS.getDependentMetaFields());
            }

            if (copyMetaFieldWS.getValidationRule() != null) {
                copyMetaFieldWS.getValidationRule().setId(0);
            }
            MetaField copyMetaField = MetaFieldBL.getDTO(copyMetaFieldWS, targetEntityId);
            LOG.debug("copyMetaField.getDataTableId()   " + copyMetaField.getDataTableId());
            Map<Integer, String> errorMessages = null;
            if (copyMetaField.getValidationRule() != null)
                errorMessages = copyMetaField.getValidationRule().getErrors();
            copyMetaField = metaFieldDAS.save(copyMetaField);
            if (copyMetaField.getValidationRule() != null && errorMessages != null) {
                for(Map.Entry<Integer, String> errorMessage : errorMessages.entrySet()){
                    copyMetaField.getValidationRule().setErrorMessage(errorMessage.getKey(), errorMessage.getValue());
                }
            }
            CopyCompanyUtils.oldNewMetaFieldMap.put(metaField.getId(),copyMetaField.getId());
        }

        for(Map.Entry<Integer, Integer[]> entry : dependencyListMap.entrySet()) {
            MetaField dependencyMetaField = metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(entry.getKey()));
            Set<MetaField> dependentFields = new HashSet<MetaField>();
            for(Integer dependentId : entry.getValue()) {
                dependentFields.add(metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(dependentId)));
            }
            dependencyMetaField.setDependentMetaFields(dependentFields);
            dependencyMetaField = metaFieldDAS.save(dependencyMetaField);
            LOG.debug("dependencyMetaField.getDataTableId()   " + dependencyMetaField.getDataTableId());
        }

        metaFieldDAS.flush();

    }
}
