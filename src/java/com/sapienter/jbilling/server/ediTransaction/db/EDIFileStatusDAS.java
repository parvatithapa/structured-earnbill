package com.sapienter.jbilling.server.ediTransaction.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class EDIFileStatusDAS extends AbstractDAS<EDIFileStatusDTO> {

    public EDIFileStatusDTO getFileStatusByName(String statusName) {
        EDIFileStatusDTO ediFileStatusDTO = null;
        statusName = StringUtils.trimToNull(statusName);
        if(statusName != null) {
            Criteria criteria = getSession().createCriteria(EDIFileStatusDTO.class)
                    .add(Restrictions.eq("name", statusName));
            ediFileStatusDTO = (EDIFileStatusDTO) criteria.uniqueResult();
        }
        return ediFileStatusDTO;
    }


}
