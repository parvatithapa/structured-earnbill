package com.sapienter.jbilling.server.security;

import java.util.List;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;

public class RunAsCompanyAdmin extends RunAsUser {

    public RunAsCompanyAdmin (final Integer entityId) {
        super(findFirstAdminUserNameForCompany (entityId));
    }

    private static String findFirstAdminUserNameForCompany (Integer entityId) {
    	IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");
    	return actionTxWrapper.execute(() -> {
    		List<UserDTO> adminsList = new UserDAS().findAdminUsers(entityId);
               if (adminsList.isEmpty()) {
    			throw new SessionInternalError("No admins was found for entity:: " + entityId);
    		}
    		return adminsList.get(0).getUserName() + ";" + entityId;
    	});
    }
}
