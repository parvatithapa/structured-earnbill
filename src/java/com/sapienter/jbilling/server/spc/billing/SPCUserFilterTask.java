package com.sapienter.jbilling.server.spc.billing;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.IBillableUserFilterTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.EnumerationDAS;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;

public class SPCUserFilterTask extends PluggableTask implements IBillableUserFilterTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ERROR_MESSAGE_FORMAT = "error in isNotBillable for entity %s for billing run date %s";
    public static final ParameterDescription PARAM_DAYS_TO_DELAY_BILLING = new ParameterDescription("Days to Delay Billing", true, Type.INT);
    public static final ParameterDescription PARAM_CUSTOMER_TYPE_MF_NAME = new ParameterDescription("Customer Type MetaField Name", true, Type.STR);
    public static final ParameterDescription PARAM_CUSTOMER_TYPE = new ParameterDescription("Customer Type To Delay", true, Type.STR);

    public SPCUserFilterTask() {
        descriptions.add(PARAM_DAYS_TO_DELAY_BILLING);
        descriptions.add(PARAM_CUSTOMER_TYPE_MF_NAME);
        descriptions.add(PARAM_CUSTOMER_TYPE);
    }

    @Override
    public boolean isNotBillable(Integer userId, Date billingRunDate) {
        Integer entityId = getEntityId();
        try {
            validateParameters(entityId);
            UserBL userBL = new UserBL(userId);
            UserDTO user = userBL.getEntity();
            CustomerDTO customer = user.getCustomer();
            boolean useDefault = Boolean.FALSE;
            String customerTypeMetaFieldName = getMandatoryStringParameter(PARAM_CUSTOMER_TYPE_MF_NAME.getName());
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> customerTypeMetaFieldValue = customer.getMetaField(customerTypeMetaFieldName);
            if(null == customerTypeMetaFieldValue) {
                logger.debug("customerType not found on user {}", userId);
                useDefault = Boolean.TRUE;
            }

            if(null!= customerTypeMetaFieldValue &&
                    !customerTypeMetaFieldValue.isEmpty()) {
                String customerType = getMandatoryStringParameter(PARAM_CUSTOMER_TYPE.getName());
                if(!customerTypeMetaFieldValue.getValue().equals(customerType)) {
                    logger.debug("user {} not belongs to customer type {}", userId, customerType);
                    useDefault = Boolean.TRUE;
                }
            }
            if(useDefault) {
                return userBL.isNotBillable(billingRunDate);
            }
            int daysToDelay = Integer.parseInt(getMandatoryStringParameter(PARAM_DAYS_TO_DELAY_BILLING.getName()));
            Calendar nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.setTime(user.getCustomer().getNextInvoiceDate());
            nextInvoiceDate.add(Calendar.DAY_OF_MONTH, daysToDelay);
            return !nextInvoiceDate.getTime().equals(billingRunDate);
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError(String.format(ERROR_MESSAGE_FORMAT, entityId, billingRunDate), ex);
        }
    }

    private EnumerationDTO checkAndValidateEnumMetaField(String mfName, EntityType entityType, Integer entityId) {
        MetaField originMetaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { entityType }, mfName);
        if(null == originMetaField) {
            throw new SessionInternalError(mfName + " not found on " + entityType.name() + " meta field for entity "+ entityId);
        }
        logger.debug("{} level meta field {} found for entity {}", mfName, entityType, entityId);
        EnumerationDTO enumDTO = new EnumerationDAS().getEnumerationByName(mfName, entityId);
        if(null == enumDTO) {
            throw new SessionInternalError(" no enum " + mfName + " defined for entity "+ entityId);
        }
        logger.debug("enum {} found for entity {}", mfName, entityId);
        return enumDTO;

    }

    /**
     * validates plugin parameters.
     * @param entityId
     * @throws PluggableTaskException
     */
    private void validateParameters(Integer entityId) throws PluggableTaskException {
        String customerTypeMfName = getMandatoryStringParameter(PARAM_CUSTOMER_TYPE_MF_NAME.getName());
        EnumerationDTO customerTypeEnum = checkAndValidateEnumMetaField(customerTypeMfName, EntityType.CUSTOMER, entityId);
        String customerType = getMandatoryStringParameter(PARAM_CUSTOMER_TYPE.getName());
        if(!customerTypeEnum.isValuePresent(customerType)) {
            throw new SessionInternalError("invalid " + customerType + " customer type value passed for entity "+ entityId + " in plugin");
        }
        try {
            int daysToDelay = Integer.parseInt(getMandatoryStringParameter(PARAM_DAYS_TO_DELAY_BILLING.getName()));
            if(daysToDelay < 0) {
                throw new SessionInternalError("negative value for " + PARAM_DAYS_TO_DELAY_BILLING.getName() + " passed for entity "+ entityId + " in plugin");
            }
        } catch(NumberFormatException numberFormatException) {
            throw new SessionInternalError("invalid " + PARAM_DAYS_TO_DELAY_BILLING.getName() + " passed for entity "+ entityId + " in plugin");
        }
    }

}
