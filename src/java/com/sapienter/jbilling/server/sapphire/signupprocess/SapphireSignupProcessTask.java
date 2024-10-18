package com.sapienter.jbilling.server.sapphire.signupprocess;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.signup.ISignupProcessTask;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.process.signup.SignupRequestWS;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class SapphireSignupProcessTask extends PluggableTask implements ISignupProcessTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_CUSTOMER_BILLING_CYCLE_PERIOD_ID =
            new ParameterDescription("customer_billing_cycle_period_id", true, ParameterDescription.Type.INT);

    private static final ParameterDescription PARAM_ORDER_PERIOD_ID =
            new ParameterDescription("order_period_id", true, ParameterDescription.Type.INT);

    private static final ParameterDescription PARAM_NEXT_INVOICE_DAY =
            new ParameterDescription("next_invoice_day", false, ParameterDescription.Type.INT);

    private static final ParameterDescription PARAM_PRODUCT_IDS_INVENTORY_ALLOCATION =
            new ParameterDescription("product_Ids_for_direct_inventory_allocation", false, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME =
            new ParameterDescription("customer provisioning metafield name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_AIT_GROUP_NAME =
            new ParameterDescription("AIT group name", true, ParameterDescription.Type.STR);


    private static final String FIND_SUBSCRIPTION_ITEM_BY_PLAN_CODE_SQL =
            "SELECT id FROM item WHERE internal_number = ?";

    public SapphireSignupProcessTask() {
        descriptions.add(PARAM_CUSTOMER_BILLING_CYCLE_PERIOD_ID);
        descriptions.add(PARAM_ORDER_PERIOD_ID);
        descriptions.add(PARAM_NEXT_INVOICE_DAY);
        descriptions.add(PARAM_PRODUCT_IDS_INVENTORY_ALLOCATION);
        descriptions.add(PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME);
        descriptions.add(PARAM_AIT_GROUP_NAME);
    }

    @Override
    public void processSignupRequest(SignupPlaceHolder signupPlaceHolder) {
        try {
            logger.debug("Processing Signup reuqest for entity {}", getEntityId());
            signupPlaceHolder.addParameters(getParameters());
            List<ISapphireSignupStep> steps = Context.getBean("sapphireSignupSteps");
            SignupResponseWS response = signupPlaceHolder.getSignUpResponse();
            for(ISapphireSignupStep step : steps) {
                if(response.hasError()) {
                    break;
                }
                step.executeStep(signupPlaceHolder);
            }
        } catch(Exception ex) {
            logger.error("Error in processSignupRequest", ex);
        }
    }

    @Override
    public void validateSignupRequest(SignupPlaceHolder signupPlaceHolder) {
        SignupRequestWS request = signupPlaceHolder.getSignUpRequest();
        SignupResponseWS response = signupPlaceHolder.getSignUpResponse();
        boolean isPlanEmpty = StringUtils.isEmpty(request.getPlanCode());
        boolean isAddOnEmpty = ArrayUtils.isEmpty(request.getAddonProductCodes());
        if(isPlanEmpty && isAddOnEmpty) {
            response.addErrorResponse("planCode and addonProductCodes are not present in request, atleast one parameter is required");
        }
        if(!isPlanEmpty) {
            String planCode = request.getPlanCode();
            // check plan code
            try {
                Integer planItem = getItemIdFromCode(planCode);
                ItemDTO planItemDTO = new ItemDAS().find(planItem);
                if(!planItemDTO.isPlan()) {
                    response.addErrorResponse("PLAN-CODE:["+ planCode + "] IS NOT PLAN");
                    logger.debug("code {} is product not plan", planCode);
                }
                logger.debug("Plan Item {} Found for code {}", planItem, planCode);
            } catch(IncorrectResultSizeDataAccessException ex) {
                response.addErrorResponse("INVALID-PLAN-CODE:["+ planCode + "] Passed");
                logger.error("Plan code {} not found", planCode, ex);
            }
        }

        if(!isAddOnEmpty) {
            // check Add on products code
            for(String code: request.getAddonProductCodes()) {
                checkProductCode(code, response);
            }
        }

        if(!ArrayUtils.isEmpty(request.getOneTimeCharges())) {
            // check One time products code
            for(String code: request.getOneTimeCharges()) {
                checkProductCode(code, response);
            }
        }
    }

    private Integer getItemIdFromCode(String productCode) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        return jdbcTemplate.queryForObject(FIND_SUBSCRIPTION_ITEM_BY_PLAN_CODE_SQL, new Object[] { productCode }, Integer.class);
    }

    private void checkProductCode(String code, SignupResponseWS response) {
        try {
            // check product code
            Integer itemId = getItemIdFromCode(code);
            ItemDTO item = new ItemDAS().find(itemId);
            if(item.isPlan()) {
                response.addErrorResponse("PRODUCT-CODE:[" + code + "] IS PLAN");
            }
            logger.debug("Item {} Found for code {}", itemId, code);
        } catch(IncorrectResultSizeDataAccessException ex) {
            response.addErrorResponse("INVALID-PRODUCT-CODE:[" + code + "] Passed");
            logger.error("Product code {} not found", code, ex);
        }
    }

}
