package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.db.PreferenceDAS;
import org.apache.log4j.Logger;

/**
 * Created by vivek on 21/11/14.
 */
public class PreferenceCopyTask extends AbstractCopyTask {
    PreferenceDAS preferenceDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PreferenceCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        return false;
    }

    public PreferenceCopyTask() {
        init();
    }

    private void init() {
        preferenceDAS = new PreferenceDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create PreferenceCopyTask");
        copyPreference(entityId, targetEntityId);
        LOG.debug("PreferenceCopyTask has been completed.");
    }

    private void copyPreference(int entityId, int targetEntityId) {

        for (Object[] preferenceTypeAndValue : preferenceDAS.getPreferencesByEntity(entityId)) {
            if (preferenceTypeAndValue != null && preferenceTypeAndValue[0] != null) {
                Integer preferenceTypeId = new Integer(preferenceTypeAndValue[0].toString());
                String preferenceValue = "";
                if (preferenceTypeAndValue[1] != null && !preferenceTypeAndValue[1].toString().isEmpty()) {
                    preferenceValue = preferenceTypeAndValue[1].toString();
                } else if (preferenceTypeAndValue[2] != null && !preferenceTypeAndValue[2].equals("0")) {
                    preferenceValue = preferenceTypeAndValue[2].toString();
                }
                // populate the map
                new PreferenceBL().createUpdateForEntity(targetEntityId, preferenceTypeId, preferenceValue);
            }
        }
    }
}
