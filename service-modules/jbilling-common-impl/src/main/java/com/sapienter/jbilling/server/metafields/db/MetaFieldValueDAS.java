/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.hibernate.NonUniqueResultException;
import org.hibernate.criterion.Restrictions;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class MetaFieldValueDAS extends AbstractDAS<MetaFieldValue> {

	private static final String CHECK_META_FILED_ID_EXISTS = 
			"SELECT id "
			+ " FROM meta_field_value "
			+ " WHERE id = %d "
			+ " AND meta_field_name_id = %d ";
	
    public boolean checkMetaFieldValueExists(Integer metaFieldId, MetaFieldValue<?> value) {
        try {
            MetaFieldValue<?> result = (MetaFieldValue<?>) getSession().createCriteria(value.getClass())
                    .add(Restrictions.eq("field.id", metaFieldId))
                    .add(Restrictions.eq("value", value.getValue()))
                    .uniqueResult();

            return result!=null &&
                    checkMetaFieldValueIdExists(value);
        } catch(NonUniqueResultException nonUniqueResultException) {
            // do noting if exception comes
            return true;
        }
    }

    private boolean checkMetaFieldValueIdExists(MetaFieldValue<?> value) {
        DataSource dataSource = Context.getBean(Name.DATA_SOURCE);
        try(Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(String.format(CHECK_META_FILED_ID_EXISTS, value.getId(), value.getField().getId()))) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new SessionInternalError(e);
        }
    }

}
