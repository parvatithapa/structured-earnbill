package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.log4j.Logger;

/**
 * Created by vivek on 30/10/14.
 */
public abstract class AbstractCopyTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractCopyTask.class));

    public abstract void create(Integer entityId, Integer targetEntityId);

    public abstract Boolean isTaskCopied(Integer entityId, Integer targetEntityId);

    public abstract Class[] getDependencies();

    public void initialise(Integer entityId, Integer targetEntityId) {
        for (Class copyTask : getDependencies()) {
            try {
                AbstractCopyTask abstractCopyTask = (AbstractCopyTask) copyTask.newInstance();
                if (!abstractCopyTask.isTaskCopied(entityId, targetEntityId)) {
                    abstractCopyTask.create(entityId, targetEntityId);
                }
            } catch (InstantiationException instantiationException) {
                LOG.debug("copyTask cannot be instantiated.  " + instantiationException);
            } catch (IllegalAccessException illegalAccessException) {
                LOG.debug("can not access a member of class " + copyTask + illegalAccessException);
            }
        }
    }

    //used this method for cleanUp Activities
    public void cleanUp(Integer targetEntityId){
        LOG.debug("Call cleanUp of AbstractCopyTask");
    }
}
