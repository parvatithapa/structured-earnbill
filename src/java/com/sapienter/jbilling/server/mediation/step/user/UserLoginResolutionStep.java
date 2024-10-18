//TODO MODULARIZATION: MEDIATION 2.0 USED IN UPDATE CURRENT ORDER
/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.step.user;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetTransitionDAS;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractUserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the user from the CDR based on 'Served MSISDN' field
 * <p/>
 * Resolves the currency and user expired date after the user has been resolved
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public class UserLoginResolutionStep extends AbstractUserResolutionStep<MediationStepResult> implements InitializingBean {

    public static String MSISDN_FIELD = "Served MSISDN";
    public static Integer MSISDN_PRODUCT_ID = 102;

    private String usernameField;
    private UserBL userLoader;
    private AssetBL assetBL;
    private AssetTransitionDAS assetTransitionDAS;

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {

        PricingField servedMSISDNField = PricingField.find(fields, MSISDN_FIELD);
        if (servedMSISDNField == null) {
            return false;
        }

        String servedMSISDN = servedMSISDNField.getStrValue();

        AssetDTO assetDTO = getAssetBL().getForItemAndIdentifier(servedMSISDN, MSISDN_PRODUCT_ID);
        if (assetDTO == null) {
            LOG.error("Asset with identifier does not exits %s", servedMSISDN);
            return false;
        }

        OrderLineDTO orderLineDTO = assetDTO.getOrderLine();
        if (orderLineDTO == null) {
            LOG.error("Asset is not assigned to an order %s", assetDTO);
            return false;
        }
        
        Integer userId = orderLineDTO.getPurchaseOrder().getUserId();
        if (userId == null) {
            return false;
        }
        Map<String, Object> userDTOMap = resolveUserById(entityId, userId);
        return setUserOnResult(result, userDTOMap);
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(usernameField, "The username field may not be null");
    }

    @Override
    protected Map<String, Object> resolveUserByUsername(Integer entityId, String username) {
        return buildUserMap(getUserDTO(null, entityId, username));
    }

    @Override
    protected Map<String, Object> resolveUserById(Integer entityId, Integer userId) {
        return buildUserMap(getUserDTO(userId, null, null));
    }

    private UserDTO getUserDTO(Integer userId, Integer entityId, String username) {
        if (userLoader == null) userLoader = new UserBL();

        if (userId != null) userLoader.set(userId);
        else userLoader.set(username, entityId);

        return userLoader.getEntity();
    }

    private Map<String, Object> buildUserMap(UserDTO userDTO) {
        Map<String, Object> userMap = new HashMap<String, Object>();
        if (userDTO != null) {
            userMap.put(MediationStepResult.USER_ID, userDTO.getId());
            userMap.put(MediationStepResult.CURRENCY_ID, userDTO.getCurrencyId());
            if (userDTO.getDeleted() > 0) {
                userMap.put(USER_EXPIRED, userDTO.getDeleted());
            }
        }
        return userMap;
    }

    public void setUsernameField(String usernameField) {
        this.usernameField = usernameField;
    }

    public void setUserLoader(UserBL userLoader) {
        this.userLoader = userLoader;
    }

    private AssetBL getAssetBL() {
        if (assetBL == null) assetBL = new AssetBL();
        return assetBL;
    }

    public void setAssetBL(AssetBL assetBL) {
        this.assetBL = assetBL;
    }

    public AssetTransitionDAS getAssetTransitionDAS() {
        if (assetTransitionDAS == null) assetTransitionDAS = new AssetTransitionDAS();
        return assetTransitionDAS;
    }

    public void setAssetTransitionDAS(AssetTransitionDAS assetTransitionDAS) {
        this.assetTransitionDAS = assetTransitionDAS;
    }
}
