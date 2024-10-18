package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pricing.RatingUnitBL;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDAS;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 21/11/2014.
 */
public class RatingUnitCopyTask extends AbstractCopyTask {

    RatingUnitDAS ratingUnitDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RatingUnitCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        return false;
    }

    public  RatingUnitCopyTask() {
        init();
    }

    public void init() {
        ratingUnitDAS = new RatingUnitDAS();
    }
    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create RatingUnitCopyTask");
        copyRatingUnitTask(entityId, targetEntityId);
        LOG.debug("RatingUnitCopyTask has been completed.");
    }

    private void copyRatingUnitTask(int entityId, int targetEntityId) {

        RatingUnitBL unitBL = new RatingUnitBL();
        List<RatingUnitDTO> unitDTOs = ratingUnitDAS.findAll(entityId);
        List<RatingUnitDTO> copyUnitDTOs = ratingUnitDAS.findAll(targetEntityId);
        if (copyUnitDTOs.isEmpty()) {
            for (RatingUnitDTO unitDTO : unitDTOs) {
                RatingUnitWS ratingUnitWS = RatingUnitBL.getWS(unitDTO);
                ratingUnitWS.setId(0);
                RatingUnitDTO copyRatingUnitDTO = RatingUnitBL.getDTO(ratingUnitWS, targetEntityId);
                unitBL.create(copyRatingUnitDTO);
            }
        }
    }

}
