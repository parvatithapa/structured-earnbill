package com.sapienter.jbilling.test.framework;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.mediation.MediationProcess;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.CompanyWS;


import com.sapienter.jbilling.server.util.api.JbillingAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by marcolin on 06/11/15.
 */
public class TestEnvironment extends AbstractTestEnvironment {


    public Map<JbillingAPI,MetaFieldValueWS[]> companyMetaFields;

    public SortedMap<String, JBillingEntity> entities;
    public SortedMap<String, JBillingEntity> entitiesForMultipleTests;

    private static final Logger logger = LoggerFactory.getLogger(TestEnvironment.class);

    private boolean environmentForMultipleTests = false;
    //this flag will decide, do we need to clear entities map or not after test execution.
    private boolean clearEntitiesId = true;

    TestEnvironment() {
        this.entities = new TreeMap<>();
        this.entitiesForMultipleTests = new TreeMap<>();
    }

    public static boolean isMultiNode() {
        String multiNode = System.getProperty("multi-node");
        boolean val = "true".equals(multiNode) || Boolean.TRUE.equals(multiNode);
        logger.info("Multi-node: {}", val);
        return val;
    }

    public void setClearEntitiesId(boolean clearEntitiesId) {
        this.clearEntitiesId = clearEntitiesId;
    }

    public void add(String testCode, Integer entityId, String jBillingCode, JbillingAPI api, TestEntityType type) {
        if (environmentForMultipleTests)
            entitiesForMultipleTests.put(testCode, new JBillingEntity(entityId, jBillingCode, api, type));
        else
            entities.put(testCode, new JBillingEntity(entityId, jBillingCode, api, type));

    }

    public Integer idForCode(String testCode) {
        return (Integer) retrieveFieldInTheEntityWithCode(testCode, jbEntity -> jbEntity.getId());
    }

    public String jBillingCode(String testCode) {
        return (String) retrieveFieldInTheEntityWithCode(testCode, jbEntity -> jbEntity.getJBillingCode());
    }

    private Object retrieveFieldInTheEntityWithCode(String code, Function<JBillingEntity, Object> consumer) {
        if (entities.containsKey(code))
            return consumer.apply(entities.get(code));
        if (entitiesForMultipleTests.containsKey(code))
            return consumer.apply(entitiesForMultipleTests.get(code));
        return null;
    }

    public void removeEntitiesFromJBilling() {
        try {
            removeEntitiesUsingCleanersFromMap(entities);
        } finally {
            if(clearEntitiesId) entities.clear();
        }
    }

    public void removeEntitiesFromJBillingForMultipleTests() {
        try {
            removeEntitiesUsingCleanersFromMap(entitiesForMultipleTests);
        } finally {
            entitiesForMultipleTests.clear();
        }
    }

    public void removeEntitiesUsingCleanersFromMap(Map<String, JBillingEntity> map) {
        removeMetaFieldFromCompany();
        getEntityCleaners().forEach((type, consumer) ->
                map.values().stream().filter(jbE -> jbE.getType().equals(type))
                        .forEach(jbEntity -> executeConsumerOnEntityWithExceptionLogging(consumer, jbEntity)));
    }




    private Map<TestEntityType, Consumer<JBillingEntity>> getEntityCleaners() {
        Map<TestEntityType, Consumer<JBillingEntity>> entityCleaners = new LinkedHashMap<>();
        entityCleaners.put(TestEntityType.ASSET, jbEntity -> jbEntity.getCreationAPI().deleteAsset(jbEntity.getId()));
        entityCleaners.put(TestEntityType.MEDIATION_CONFIGURATION, jbEntity -> deleteMediationConfigurationWithProcesses(jbEntity));
        entityCleaners.put(TestEntityType.INVOICE, jbEntity -> jbEntity.getCreationAPI().deleteInvoice(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ORDER, jbEntity -> jbEntity.getCreationAPI().deleteOrder(jbEntity.getId()));
        entityCleaners.put(TestEntityType.PLAN, jbEntity -> jbEntity.getCreationAPI().deletePlan(jbEntity.getId()));
        //ToDo there should be a way to delete customer usage pool entity
//        entityCleaners.put(TestEntityType.USAGE_POOL, jbEntity -> jbEntity.getCreationAPI().deleteUsagePool(jbEntity.getId()));
        entityCleaners.put(TestEntityType.PRODUCT_CATEGORY, jbEntity -> deleteItemCategoryAndItemsInside(jbEntity));
        entityCleaners.put(TestEntityType.CUSTOMER, jbEntity -> jbEntity.getCreationAPI().deleteUser(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ACCOUNT_TYPE, jbEntity -> jbEntity.getCreationAPI().deleteAccountType(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ENUMERATION, jbEntity -> jbEntity.getCreationAPI().deleteEnumeration(jbEntity.getId()));
        entityCleaners.put(TestEntityType.META_FIELD, jbEntity -> jbEntity.getCreationAPI().deleteMetaField(jbEntity.getId()));
        entityCleaners.put(TestEntityType.PLUGGABLE_TASK, jbEntity -> jbEntity.getCreationAPI().deletePlugin(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ROUTE, jbEntity -> jbEntity.getCreationAPI().deleteRoute(jbEntity.getId()));
        entityCleaners.put(TestEntityType.PAYMENT_METHOD_TYPE, jbEntity -> jbEntity.getCreationAPI().deletePaymentMethodType(jbEntity.getId()));
        //ToDo there should be a way to delete Discount entity, the discount lines when order is deleted they are not deleted
//        entityCleaners.put(TestEntityType.DISCOUNT, jbEntity-> jbEntity.getCreationAPI().deleteDiscount(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ORDER_CHANGE_STATUS, jbEntity -> jbEntity.getCreationAPI().deleteOrderChangeStatus(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ORDER_PERIOD, jbEntity -> jbEntity.getCreationAPI().deleteOrderPeriod(jbEntity.getId()));
        entityCleaners.put(TestEntityType.EDI_TYPE, jbEntity -> jbEntity.getCreationAPI().deleteEDIType(jbEntity.getId()));
        entityCleaners.put(TestEntityType.ENROLLMENT, jbEntity -> jbEntity.getCreationAPI().deleteEnrollment(jbEntity.getId()));
        entityCleaners.put(TestEntityType.USAGE_RATING_SCHEME, jbEntity -> jbEntity.getCreationAPI().deleteUsageRatingScheme(jbEntity.getId()));
        return entityCleaners;
    }

    private void removeMetaFieldFromCompany() {
        if (companyMetaFields == null) return;
        companyMetaFields.entrySet().stream().forEach(e -> {
            JbillingAPI api = e.getKey();
            CompanyWS company = api.getCompany();
            company.setMetaFields(e.getValue());
            api.updateCompany(company);
        });
    }

    private void executeConsumerOnEntityWithExceptionLogging(Consumer<JBillingEntity> consumer, JBillingEntity jbEntity) {
        try {
            consumer.accept(jbEntity);
        } catch (Exception e) {
            // If customer or enrollment is already exist in account type then it will through exception while deleting account type.
            // It is known so no need to through error message.
            if (jbEntity.getType().equals(EntityType.ACCOUNT_TYPE) && e instanceof SessionInternalError) {
                logger.debug("There is enrollment or Customer exist in account type.");
            } else
            logger.error("There has been an error during clean up of test entities, for entity of type: {} and code {}", jbEntity.getType().name(), jbEntity.getJBillingCode(), e);
        }
    }

    private void deleteItemCategoryAndItemsInside(JBillingEntity jbEntity) {
        for (ItemDTOEx itemInCategory : jbEntity.getCreationAPI().getItemByCategory(jbEntity.getId())) {
            clearItemDependenciesIfNecessary(itemInCategory, jbEntity.creationAPI);
            jbEntity.getCreationAPI().deleteItem(itemInCategory.getId());
        }
        jbEntity.getCreationAPI().deleteItemCategory(jbEntity.getId());
    }

    private void clearItemDependenciesIfNecessary(ItemDTOEx item, JbillingAPI api){
        if (null != item.getDependencies() && item.getDependencies().length != 0){
            item.setDependencies(null);
            api.updateItem(item);
        }
    }

    private void deleteMediationConfigurationWithProcesses(JBillingEntity jbEntity) {
        MediationProcess[] allMediationProcesses = jbEntity.getCreationAPI().getAllMediationProcesses();
        Arrays.asList(allMediationProcesses).stream().filter(mp -> mp.getConfigurationId().equals(jbEntity.getId()))
                .forEach(mp -> jbEntity.getCreationAPI().undoMediation(mp.getId()));
        jbEntity.getCreationAPI().deleteMediationConfiguration(jbEntity.getId());
    }

    public void setEnvironmentForMultipleTests(boolean environmentForMultipleTests) {
        this.environmentForMultipleTests = environmentForMultipleTests;
    }

    private class JBillingEntity {
        private Integer id;
        private JbillingAPI creationAPI;
        private TestEntityType type;
        private String jBillingCode;

        public JBillingEntity(Integer id, String jBillingCode, JbillingAPI creationAPI, TestEntityType type) {
            this.id = id;
            this.jBillingCode = jBillingCode;
            this.creationAPI = creationAPI;
            this.type = type;
        }

        public Integer getId() {
            return id;
        }

        public JbillingAPI getCreationAPI() {
            return creationAPI;
        }

        public TestEntityType getType() {
            return type;
        }

        public String getJBillingCode() {
            return jBillingCode;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestEnvironment that = (TestEnvironment) o;

        if (environmentForMultipleTests != that.environmentForMultipleTests) return false;
        if (entities != null ? !entities.equals(that.entities) : that.entities != null) return false;
        return !(entitiesForMultipleTests != null ? !entitiesForMultipleTests.equals(that.entitiesForMultipleTests) : that.entitiesForMultipleTests != null);

    }

    @Override
    public int hashCode() {
        int result = entities != null ? entities.hashCode() : 0;
        result = 31 * result + (entitiesForMultipleTests != null ? entitiesForMultipleTests.hashCode() : 0);
        result = 31 * result + (environmentForMultipleTests ? 1 : 0);
        return result;
    }
}
