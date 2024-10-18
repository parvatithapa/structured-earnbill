package com.sapienter.jbilling.server.boa.batch.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author Javier Rivero
 * @since 07/01/16.
 */
public class BoaBaiProcessingErrorDAS extends AbstractDAS<BoaBaiProcessingErrorDTO> {
    /**
     * This method checks if a file was processed
     *
     * @param fileName
     * @return true or false
     */
    public boolean isProcessedWithError(String fileName){
        Criteria criteria = getSession().createCriteria(BoaBaiProcessingErrorDTO.class);
        criteria.add(Restrictions.eq("fileName", fileName));
        return  criteria.list().size() > 0;
    }
}
