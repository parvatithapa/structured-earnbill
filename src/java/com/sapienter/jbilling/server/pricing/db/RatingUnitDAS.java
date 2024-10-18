package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 *  Rating unit DAS
 *
 * @author Panche Isajeski
 * @since 27-Aug-2013
 */
public class RatingUnitDAS extends AbstractDAS<RatingUnitDTO> {

    /**
     * Returns list of rating units defined for company(entity)
     *
     * @param entityId - the entity id
     */
    public List<RatingUnitDTO> findAll(Integer entityId) {
        Criteria criteria = getSession().createCriteria(RatingUnitDTO.class);
        criteria.add(Restrictions.eq("company.id", entityId));
        return criteria.list();
    }

    public RatingUnitDTO getDefaultRatingUnit(Integer entityId) {
        Criteria criteria = getSession().createCriteria(RatingUnitDTO.class);
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.add(Restrictions.eq("canBeDeleted", false));
        return (RatingUnitDTO) criteria.uniqueResult();
    }

    public boolean isRatingUnitUsed(Integer ratingUnitId) {
        Criteria criteria = getSession().createCriteria(RouteRateCardDTO.class)
                .createAlias("ratingUnit", "ratingUnit")
                .add(Restrictions.eq("ratingUnit.id", ratingUnitId));

        return criteria.list().size() > 0;
    }

    public List<RatingUnitDTO> findByName(String ratingUnitName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(RatingUnitDTO.class);
        criteria.add(Restrictions.eq("name", ratingUnitName));
        criteria.add(Restrictions.eq("company.id", entityId));
        return criteria.list();
    }
    
}
