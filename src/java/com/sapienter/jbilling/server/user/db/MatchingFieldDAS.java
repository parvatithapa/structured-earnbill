package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class MatchingFieldDAS extends AbstractDAS<MatchingFieldDTO> {

    public MatchingFieldDTO getMatchingFieldById(Integer matchingFieldId){
        Criteria criteria = getSession().createCriteria(MatchingFieldDTO.class)
                .add(Restrictions.eq("id", matchingFieldId))
                .setMaxResults(1);
        return (MatchingFieldDTO) criteria.uniqueResult();

    }

    public List<String> getRouteUsedMatchingFields(Integer routeId){
        Query query = getSession().createQuery("select M.matchingField from MatchingFieldDTO  M where M.route.id=:routeId");
        query.setInteger("routeId", routeId);
        return (List<String>) query.list();
    }

    public List<String> getRouteRateCardUsedMatchingFields(Integer routeRateCardId){
        Query query = getSession().createQuery("select M.matchingField from MatchingFieldDTO  M where M.routeRateCard.id=:routeRateCardId");
        query.setInteger("routeRateCardId",routeRateCardId);
        return (List<String>) query.list();
    }

    public List<MatchingFieldDTO> findMatchingFieldByRequiredField(Integer routeId,Boolean required){
        Query query = getSession().createQuery(" from MatchingFieldDTO as M WHERE M.route.id =:routeId AND M.required =:required order by M.orderSequence asc ");
              query.setInteger("routeId",routeId);
              query.setBoolean("required",required);
        return (List<MatchingFieldDTO>) query.list();
    }

    public List<MatchingFieldDTO> getMatchingFieldsByRouteId(Integer routeId){
        Query query = getSession().createQuery(" from MatchingFieldDTO as M where M.route.id=:routeId");
        query.setInteger("routeId",routeId);
        return (List<MatchingFieldDTO>) query.list();
    }

    public List<MatchingFieldDTO> getMatchingFieldsByRouteRateCardId(Integer routeRateCardId){
        Query query = getSession().createQuery(" from MatchingFieldDTO as M where M.routeRateCard.id=:routeRateCardId");
        query.setInteger("routeRateCardId",routeRateCardId);
        return (List<MatchingFieldDTO>) query.list();
    }

}
