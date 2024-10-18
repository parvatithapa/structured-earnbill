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

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Created by igutierrez on 3/31/17.
 */
public class UserHelperDisplayerDistributel implements UserHelperDisplayer {
    private static UserHelperDisplayerDistributel userHelperDisplayerDistributel;

    private UserHelperDisplayerDistributel(){

    }

    public static UserHelperDisplayerDistributel getInstance() {
        if (userHelperDisplayerDistributel != null) {
            return userHelperDisplayerDistributel;
        } else {
            return new UserHelperDisplayerDistributel();
        }
    }

    @Override
    public String getDisplayName(UserWS user) {
        return user.getUserName().split(Constants.UNDERSCORE_DELIMITER)[0];
    }

    @Override
    public String getDisplayName(UserDTO user) {
        return user.getUserName().split(Constants.UNDERSCORE_DELIMITER)[0];
    }
}
