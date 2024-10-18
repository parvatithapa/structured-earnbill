package com.sapienter.jbilling.server.order.db;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderLineTierDAS extends AbstractDAS<OrderLineTierDTO> {
    public List<OrderLineTierDTO> getOrderLineTiersByOrderLineId(Integer id){
        Criteria criteria = getSession().createCriteria(OrderLineTierDTO.class)
                .add(Restrictions.eq("orderLine.id", id));

        return criteria.list();
    }

}