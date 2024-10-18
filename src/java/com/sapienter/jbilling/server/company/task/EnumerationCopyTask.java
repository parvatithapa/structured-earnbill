package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.EnumerationBL;
import com.sapienter.jbilling.server.util.db.EnumerationDAS;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;
import com.sapienter.jbilling.server.util.db.EnumerationValueDAS;
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vivek on 17/11/14.
 */
public class EnumerationCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EnumerationCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<EnumerationDTO> enumerationDTOs = new EnumerationDAS().findAllEnumerationByEntity(targetEntityId);
        return enumerationDTOs != null && !enumerationDTOs.isEmpty();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create EnumerationCopyTask");
        EnumerationDAS enumerationDAS = new EnumerationDAS();
        EnumerationValueDAS enumerationValueDAS = new EnumerationValueDAS();
        List<EnumerationDTO> enumerationDTOs = new EnumerationDAS().findAllEnumerationByEntity(entityId);

        for (EnumerationDTO enumerationDTO : enumerationDTOs) {
            enumerationDAS.reattach(enumerationDTO);
            List<EnumerationValueDTO> enumerationValueDTOList = enumerationDTO.getValues();
            EnumerationDTO copyEnumerationDTO = new EnumerationDTO();
            copyEnumerationDTO.setEntityId(targetEntityId);
            copyEnumerationDTO.setName(enumerationDTO.getName());
            copyEnumerationDTO = enumerationDAS.save(copyEnumerationDTO);

            List<EnumerationValueDTO> copyEnumerationValueDTOList = new ArrayList<EnumerationValueDTO>();
            enumerationDAS.reattach(enumerationDTO);
            for (EnumerationValueDTO enumerationValueDTO : enumerationValueDTOList) {
                EnumerationValueDTO copyEnumerationValueDTO = new EnumerationValueDTO(0, enumerationValueDTO.getValue(), copyEnumerationDTO);
                copyEnumerationValueDTO = enumerationValueDAS.save(copyEnumerationValueDTO);
                copyEnumerationValueDTOList.add(copyEnumerationValueDTO);
            }
            copyEnumerationDTO.setValues(copyEnumerationValueDTOList);
            enumerationDAS.save(copyEnumerationDTO);
        }
        LOG.debug("EnumerationCopyTask has been completed.");
    }
}