package com.sapienter.jbilling.server.metafield;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.RegExValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Arrays;

/**
 * Created by wajeeha on 8/29/17.
 */
@Test(groups = { "web-services", "meta-fields" }, sequential = true, testName = "metaField.MetaFieldAccountNumberEncryptionTest")
public class MetaFieldAccountNumberEncryptionTest {

    JbillingAPI api;
    private static final Logger logger = LoggerFactory.getLogger(MetaFieldAccountNumberEncryptionTest.class);
    private static Integer entityId = 1;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void test001BackwardCompatibilityTest() {
        try {
            AccountTypeWS accountTypeWS1 = new AccountTypeWS();
            accountTypeWS1.setName("Test-Account-Type - " + Calendar.getInstance().getTimeInMillis(), 1);
            accountTypeWS1.setEntityId(entityId);
            Integer ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
            accountTypeWS1.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));
            accountTypeWS1.setCurrencyId(1);
            accountTypeWS1.setLanguageId(1);
            accountTypeWS1.setInvoiceDeliveryMethodId(1);

            Integer accountTypeId = api.createAccountType(accountTypeWS1);
            AccountTypeWS accountTypeWS = api.getAccountType(accountTypeId);

            logger.debug("Account Type Ws  = {}" , accountTypeWS.toString());

            String metaFieldName = "Account Number - " + Calendar.getInstance().getTimeInMillis();
            String decryptedAccount = "123456789";

            //creating metafieldValue using old methods
            MetaFieldValueWS accountNumber = new MetaFieldValueWS();
            accountNumber.setDataType(DataType.CHAR);
            accountNumber.setEntityId(entityId);
            accountNumber.setFieldName(metaFieldName);
            accountNumber.setCharValue(decryptedAccount.toCharArray());
            accountNumber.setDisplayOrder(1);
            accountNumber.setDisabled(false);
            accountNumber.setMandatory(true);

            //creating validation
            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.REGEX.name());
            rule.addRuleAttribute(RegExValidationRuleModel.VALIDATION_REG_EX_FIELD, "[^*]+");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Invalid Account Number");

            //creating metaField
            MetaFieldWS metafieldWS = new MetaFieldWS();
            metafieldWS.setName(metaFieldName);
            metafieldWS.setEntityType(EntityType.CUSTOMER);
            metafieldWS.setValidationRule(rule);
            metafieldWS.setDataType(DataType.CHAR);
            metafieldWS.setPrimary(true);
            metafieldWS.setFieldUsage(MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED);
            metafieldWS.setEntityId(entityId);

            Integer metaFieldId = api.createMetaField(metafieldWS);
            logger.debug("Metafield ID: {}" , metaFieldId);

            //MetaField from api result
            MetaFieldWS accountNameMetaField = api.getMetaField(metaFieldId);
            logger.debug("Metafield from api: {}" , accountNameMetaField.toString());

            UserWS newUser = new UserWS();

            newUser.setLanguageId(1);
            newUser.setMainRoleId(5);
            newUser.setAccountTypeId(1);
            newUser.setUserName("Test User " + Calendar.getInstance().getTimeInMillis());
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCurrencyId(1);
            newUser.setInvoiceChild(false);
            newUser.setAccountTypeId(accountTypeId);
            newUser.setEntityId(entityId);

            MetaFieldValueWS[] list = new MetaFieldValueWS[]{accountNumber};
            newUser.setMetaFields(list);

            logger.debug("Creating user");
            Integer id = api.createUser(newUser);

            logger.debug("User created with id {}" , id);
            UserWS user = api.getUserWS(id);

            for (MetaFieldValueWS metaField : user.getMetaFields()) {
                if (metaField.getFieldName().equals(metaFieldName)) {
                    logger.debug("Account Number Value {}" , metaField.toString());
                    break;
                }
            }

            //deleting test data
            api.deleteMetaField(metaFieldId);
            api.deleteUser(id);
            api.deleteAccountType(accountTypeId);
        } catch (Exception e){
            Assert.fail("Exception occurred: " + e.getMessage());
        }

    }

    @Test
    public void test016createEncrytpedMetaField() {

        try {
            AccountTypeWS accountTypeWS1 = new AccountTypeWS();
            accountTypeWS1.setName("Test-Account-Type - " + Calendar.getInstance().getTimeInMillis(), 1);
            accountTypeWS1.setEntityId(entityId);
            Integer ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
            accountTypeWS1.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));
            accountTypeWS1.setCurrencyId(1);
            accountTypeWS1.setLanguageId(1);
            accountTypeWS1.setInvoiceDeliveryMethodId(1);

            Integer accountTypeId = api.createAccountType(accountTypeWS1);
            AccountTypeWS accountTypeWS = api.getAccountType(accountTypeId);

            logger.debug("Account Type Ws {}" , accountTypeWS.toString());

            String metaFieldName = "Account Number - " + Calendar.getInstance().getTimeInMillis();
            MetaFieldValueWS accountNumber = getMetaFieldValue("123456789".toCharArray(), DataType.CHAR, metaFieldName, null, null);

            MetaFieldWS metafieldWS = new MetaFieldWS();
            metafieldWS.setName(metaFieldName);
            metafieldWS.setEntityType(EntityType.CUSTOMER);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.REGEX.name());
            rule.addRuleAttribute(RegExValidationRuleModel.VALIDATION_REG_EX_FIELD, "[^*]+");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Invalid Account Number");

            metafieldWS.setValidationRule(rule);
            metafieldWS.setDataType(DataType.CHAR);
            metafieldWS.setPrimary(true);
            metafieldWS.setFieldUsage(MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED);
            metafieldWS.setEntityId(entityId);

            Integer metaFieldId = api.createMetaField(metafieldWS);

            //creating metaField
            MetaFieldWS isNaedoMetaField = new MetaFieldWS();
            isNaedoMetaField.setName("Is Naedo");
            isNaedoMetaField.setEntityType(EntityType.CUSTOMER);
            isNaedoMetaField.setDataType(DataType.BOOLEAN);
            isNaedoMetaField.setPrimary(false);
            isNaedoMetaField.setEntityId(entityId);

            Integer isNaedoMetaFieldId =  api.createMetaField(isNaedoMetaField);

            //creating brandName metaField
            MetaFieldWS brandNameMetaField = new MetaFieldWS();
            brandNameMetaField.setName("Brand Name");
            brandNameMetaField.setEntityType(EntityType.CUSTOMER);
            brandNameMetaField.setDataType(DataType.STRING);
            brandNameMetaField.setPrimary(false);
            brandNameMetaField.setEntityId(entityId);

            Integer brandNameMetaFieldId =  api.createMetaField(brandNameMetaField);

            logger.debug("Metafield ID: {}" , metaFieldId);

            MetaFieldWS accountNameMetaField = api.getMetaField(metaFieldId);

            logger.debug("Metafield from api: {}" , accountNameMetaField.toString());

            UserWS newUser = new UserWS();

            newUser.setLanguageId(1);
            newUser.setMainRoleId(5);
            newUser.setAccountTypeId(1);
            newUser.setUserName("Test User " + Calendar.getInstance().getTimeInMillis());
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCurrencyId(1);
            newUser.setInvoiceChild(false);
            newUser.setAccountTypeId(accountTypeId);
            newUser.setEntityId(entityId);

            MetaFieldValueWS brandName = getMetaFieldValue("xyz", DataType.STRING, "Brand Name", null, null);
            MetaFieldValueWS isNaedo = getMetaFieldValue(true, DataType.BOOLEAN, "Is Naedo", null, null);
            MetaFieldValueWS[] list = new MetaFieldValueWS[]{brandName, isNaedo, accountNumber};
            newUser.setMetaFields(list);

            logger.debug("Creating user");

            Integer id = api.createUser(newUser);

            logger.debug("User created with id {}" , id);

            UserWS user = api.getUserWS(id);

            for (MetaFieldValueWS metaField : user.getMetaFields()) {
                if (metaField.getFieldName().equals(metaFieldName)) {
                    logger.debug("Account Number Value {}" , metaField.toString());
                    break;
                }
            }

            //deleting test data
            api.deleteMetaField(metaFieldId);
            api.deleteMetaField(isNaedoMetaFieldId);
            api.deleteMetaField(brandNameMetaFieldId);
            api.deleteUser(id);
            api.deleteAccountType(accountTypeId);
        } catch (Exception exception) {
            Assert.fail("Exception occurred: " + exception.getMessage());
        }
    }

    private MetaFieldValueWS getMetaFieldValue(Object value, DataType type, String name, Integer groupId, Integer metaFieldId){
        MetaFieldValueWS mfChildValue = new MetaFieldValueWS();

        mfChildValue.setValue(value);
        mfChildValue.getMetaField().setDataType(type);
        mfChildValue.setFieldName(name);
        mfChildValue.getMetaField().setEntityId(entityId);
        if(groupId != null) {
            mfChildValue.setGroupId(groupId);
        }

        if(metaFieldId != null){
            mfChildValue.setId(metaFieldId);
        }


        return mfChildValue;
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }
}
