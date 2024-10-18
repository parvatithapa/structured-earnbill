package com.sapienter.jbilling.server.mediation.movius;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusHelperService;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Service
@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class MoviusHelperServiceImpl implements MoviusHelperService {

    private static final Logger LOG = LoggerFactory.getLogger(MoviusHelperServiceImpl.class);

    @Override
    public Map<String, Integer> resolveUserIdByOrgId(Integer entityId, String orgId) {
        try {
            Map<Integer, Integer> userCurrencyMap = MoviusUtil.getUserIdByOrgIdMetaField(entityId, orgId);
            if(userCurrencyMap.isEmpty()) {
                throw new SessionInternalError("User not found!");
            }
            UserDAS userDAS = new UserDAS();
            UserDTO user = userDAS.find(userCurrencyMap.get(MediationStepResult.USER_ID));
            CustomerDTO customer = user.getCustomer();
            while (!customer.invoiceAsChild()) {
                CustomerDTO parentCustomer = customer.getParent();
                if( null == parentCustomer ) {
                    break;
                }
                customer = parentCustomer;
            }

            UserDTO resolvedUser = customer.getBaseUser();
            Map<String, Integer> result = new HashMap<>();
            result.put(MediationStepResult.USER_ID, resolvedUser.getId());
            result.put(MediationStepResult.CURRENCY_ID, resolvedUser.getCurrencyId());
            LOG.debug("User Resolved {}", result);
            return result;
        } catch(Exception ex) {
            LOG.error("User Resolution Failed", ex);
            throw new SessionInternalError(ex);
        }

    }

    @Override
    public List<Integer> getAllChildEntityForGivenEntity(Integer entityId) {
        return new CompanyDAS().findAllCurrentAndChildEntities(entityId);
    }

    @Override
    public Map<String, String> getMetaFieldsForEntity(Integer entityId) {
        Map<String, String> result = new HashMap<>();
        CompanyDTO company = new CompanyDAS().find(entityId);
        for(@SuppressWarnings("rawtypes") MetaFieldValue value : company.getMetaFields()) {
            Object metaFieldValue = value.getValue();
            result.put(value.getField().getName(), Objects.nonNull(metaFieldValue) ? metaFieldValue.toString() : "");
        }
        return result;
    }

}
