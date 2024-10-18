package com.sapienter.jbilling.server.ediTransaction.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class EDIFileExceptionCodeDAS extends AbstractDAS<EDIFileExceptionCodeDTO> {

    public EDIFileExceptionCodeDTO findExceptionCodeByStatus(String code, Integer statusId){
        Criteria criteria = getSession().createCriteria(EDIFileExceptionCodeDTO.class)
                .createAlias("status", "status")
                .add(Restrictions.eq("status.id", statusId))
                .add(Restrictions.eq("exceptionCode", code));
        return (EDIFileExceptionCodeDTO)criteria.uniqueResult();
    }

}
