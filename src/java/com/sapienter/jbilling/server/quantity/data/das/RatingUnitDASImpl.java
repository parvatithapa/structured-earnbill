package com.sapienter.jbilling.server.quantity.data.das;

import java.math.BigDecimal;
import java.util.Date;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.server.ratingUnit.domain.RatingUnit;

public class RatingUnitDASImpl implements RatingUnitDAS {

    @Getter @Setter
    private JdbcTemplate jdbcTemplate;

    private static final String ITEM_RATING_UNIT_QUERY =
            "SELECT ru.id ,ru.name ,ircm.start_date,ru.increment_unit_quantity " +
                    "FROM rating_unit ru " +
                    "RIGHT JOIN rating_configuration rc ON ru.id = rc.rating_unit AND ru.entity_id = ?  " +
                    "JOIN item_rating_configuration_map ircm ON rc.id = ircm.rating_configuration_id  " +
                    "where ircm.item_id = ? ";


    @Override
    public NavigableMap<Date, RatingUnit> getRatingUnit(int entityId, int itemId) {

        NavigableMap<Date, RatingUnit> models = new TreeMap<>();

        SqlRowSet rs = jdbcTemplate.queryForRowSet(ITEM_RATING_UNIT_QUERY, entityId, itemId);
        while (rs.next()) {
            int id = rs.getInt("id");
            Date startDate = rs.getDate("start_date");

            if (id <= 0) {
                models.put(startDate, RatingUnit.NONE);
                continue;
            }

            String name = rs.getString("name");
            BigDecimal incrementUnitQuantity = rs.getBigDecimal("increment_unit_quantity");
            RatingUnit ratingUnit = new RatingUnit().builder()
                                        .id(id)
                                        .name(name)
                                        .incrementUnitQuantity(incrementUnitQuantity).build();

            models.put(startDate, ratingUnit);
        }
        return models;
    }
}
