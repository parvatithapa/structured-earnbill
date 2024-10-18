package com.sapienter.jbilling.server.usageratingscheme.domain.repository;


import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeTypeDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class UsageRatingSchemeTypeDAS extends AbstractDAS<UsageRatingSchemeTypeDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public List<UsageRatingSchemeTypeDTO> findAllActive() {
        return findByCriteria(Restrictions.eq("active", true));
    }

    public UsageRatingSchemeTypeDTO findOneByName(String name) {
        return findByCriteriaSingle(Restrictions.eq("name", name));
    }
}
