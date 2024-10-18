package com.sapienter.jbilling.server.mediation.movius.db;

import org.hibernate.criterion.Restrictions;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrgCountPositionDAS extends AbstractDAS<OrgCountPositionDTO> {

    public OrgCountPositionDTO findByOrgIdOrderIdAndItemId(String orgId, Integer orderId, Integer itemId, Integer entityId) {
        return (OrgCountPositionDTO) getSession().createCriteria(OrgCountPositionDTO.class)
                .add(Restrictions.eq("orgId", orgId))
                .add(Restrictions.eq("orderId", orderId))
                .add(Restrictions.eq("itemId", itemId))
                .add(Restrictions.eq("entityId", entityId))
                .add(Restrictions.eq("deleted", 0))
                .uniqueResult();
    }
}
