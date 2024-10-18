package com.sapienter.jbilling.server.apiUserDetail.db;

import java.util.List;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

public class ApiUserDetailDAS extends AbstractDAS<ApiUserDetailDTO>  {
    public List<ApiUserDetailDTO> findAll(Integer max, Integer offset, Integer companyId) {
        Query query = getSession().createQuery(Queries.FIND_API_USER_DETAILS_OF_COMPANY);
        query.setParameter("company_id", companyId);

        if(max != null)
            query.setMaxResults(max);

        if(offset != null)
            query.setFirstResult(offset);

        return query.list();
    }

    public ApiUserDetailDTO findByAccessCode(String accessCode) {
        Query query = getSession().createQuery(Queries.GET_ACCESS_CODE_DETAILS);
        query.setParameter("access_code", accessCode);

        return (ApiUserDetailDTO) query.uniqueResult();
    }

    public Long countAll() {
        Query query = getSession().createQuery(Queries.countAllSQL);

        return (Long) query.uniqueResult();
    }

    private class Queries {

        public static final String FIND_API_USER_DETAILS_OF_COMPANY =
                " SELECT a " +
                "   FROM ApiUserDetailDTO a " +
                "  WHERE a.companyId = :company_id";

        private static final String countAllSQL =
                " SELECT COUNT(b) " +
                        " FROM ApiUserDetailDTO b ";

        private static final String GET_ACCESS_CODE_DETAILS =
                " SELECT a " +
                "   FROM ApiUserDetailDTO a " +
                "  WHERE a.accessCode = :access_code";
    }
}
