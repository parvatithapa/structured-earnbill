package com.sapienter.jbilling.server.usageratingscheme.domain.repository;


import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class UsageRatingSchemeDAS extends AbstractDAS<UsageRatingSchemeDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public Long getCountByRatingSchemeCode(Integer entityId, String ratingSchemeCode) {

        Query query = getSession().createQuery(Queries.COUNT_BY_CODE)
                .setParameter("rating_scheme_code", ratingSchemeCode)
                .setParameter("entity_id", entityId);

        return (Long) query.uniqueResult();
    }

    public List<UsageRatingSchemeDTO> findAll(Integer entityId, Integer max, Integer offset) {

        Query query = getSession().createQuery(Queries.FIND_ALL_RATING_SCHEMES)
                .setParameter("entity_id", entityId);

        if(max != null)
            query.setMaxResults(max);

        if(offset != null)
            query.setFirstResult(offset);

        logger.debug("findAll SQL: {}", query.getQueryString());

        return query.list();
    }

    public Long countAll(Integer entityId) {

        Query query = getSession().createQuery(Queries.countAllSQL)
                .setParameter("entity_id", entityId);
        return (Long) query.uniqueResult();
    }

    public UsageRatingSchemeDTO findTopByItemIdAndEventDateLesserThan(Integer itemId, Date eventDate) {

        Query query = getSession().createSQLQuery(Queries.SQL_FIND_RATING_SCHEME_ID_BY_ITEM_AND_DATE)
                .addScalar("id", IntegerType.INSTANCE)
                .setParameter("item_id", itemId)
                .setParameter("event_date", eventDate);

        logger.debug("findTopByItemIdAndEventDateLesserThan SQL: {}", query.getQueryString());

        Object idObj = query.uniqueResult();
        if (idObj == null)
            return null;

        Integer id = (Integer) idObj;
        logger.info("Rating scheme id: {}", id);
        return find(id);
    }


    public List<Integer> findProductsAssociatedWithRatingScheme(Integer ratingSchemeId) {

        Query query = getSession().createSQLQuery(Queries.SQL_FIND_ASSOCIATED_ITEMS)
                .addScalar("id", IntegerType.INSTANCE)
                .setParameter("ratingSchemeId", ratingSchemeId);

        logger.debug("findProductsAssociatedWithRatingScheme SQL: {}", query.getQueryString());

        List<Integer> ids = (List<Integer>) query.list();
        logger.info("Associated active products size: {}", ids.size());

        return ids;
    }

    public UsageRatingSchemeDTO getByRatingSchemeCode(String ratingSchemeCode, Integer entityId){

        Query query = getSession().createQuery(Queries.FIND_BY_CODE)
                .setParameter("rating_scheme_code", ratingSchemeCode)
                .setParameter("entity_id", entityId);

        return (UsageRatingSchemeDTO) query.uniqueResult();
    }

    public Map<Date, UsageRatingSchemeDTO> findAllByItem(Integer itemId) {

        Map<Date, UsageRatingSchemeDTO> result = new HashMap<>();

        Query query = getSession().createSQLQuery(Queries.SQL_FIND_RATING_SCHEME_IDS_BY_ITEM)
                .addScalar("id", IntegerType.INSTANCE)
                .addScalar("start_date", DateType.INSTANCE)
                .setParameter("item_id", itemId);

        logger.debug("findAllByItem SQL: {}", query.getQueryString());
        List<Object[]> rs = query.list();

        for (Object[] e : rs) {
            result.put((Date) e[1], e[0] == null ? null : find((Integer) e[0]));
        }

        return result;
    }

    private class Queries {

        public static final String COUNT_BY_CODE =
                " SELECT COUNT(r) FROM UsageRatingSchemeDTO r " +
                " WHERE r.ratingSchemeCode = :rating_scheme_code " +
                " AND r.entity.id = :entity_id ";

        public static final String FIND_BY_CODE =
                " SELECT r FROM UsageRatingSchemeDTO r " +
                " WHERE r.ratingSchemeCode = :rating_scheme_code " +
                " AND r.entity.id = :entity_id ";

        public static final String FIND_ALL_RATING_SCHEMES =
                "SELECT r FROM UsageRatingSchemeDTO r WHERE r.entity.id = :entity_id";

        public static final String SQL_FIND_RATING_SCHEME_ID_BY_ITEM_AND_DATE =
                " SELECT     u.id id " +
                " FROM 	     usage_rating_scheme u " +
                " RIGHT JOIN rating_configuration rc ON u.id = rc.usage_rating_scheme " +
                " JOIN       item_rating_configuration_map im ON rc.id = im.rating_configuration_id " +
                " WHERE	     im.item_id = :item_id " +
                " AND        im.start_date <= :event_date " +
                " ORDER BY   im.start_date desc " +
                " LIMIT      1 ";

        public static final String SQL_FIND_RATING_SCHEME_IDS_BY_ITEM =
                " SELECT     u.id id, im.start_date start_date " +
                " FROM 	     usage_rating_scheme u " +
                " RIGHT JOIN rating_configuration rc ON u.id = rc.usage_rating_scheme " +
                " JOIN       item_rating_configuration_map im ON rc.id = im.rating_configuration_id " +
                " WHERE	     im.item_id = :item_id ";

        private static final String countAllSQL =
                " SELECT COUNT(b) " +
                " FROM UsageRatingSchemeDTO b " +
                " WHERE b.entity.id = :entity_id";

        public static final String SQL_FIND_ASSOCIATED_ITEMS =
                " SELECT DISTINCT i.id " +
                " FROM   item i " +
                " JOIN 	 item_rating_configuration_map im ON i.id = im.item_id " +
                " JOIN   rating_configuration rc ON rc.id = im.rating_configuration_id " +
                " WHERE	 rc.usage_rating_scheme = :ratingSchemeId " +
                " AND	 i.deleted = 0 ";
    }
}
