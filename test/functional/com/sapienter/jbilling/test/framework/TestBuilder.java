package com.sapienter.jbilling.test.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by marcolin on 06/11/15.
 */
public class TestBuilder {

    private static final Logger logger = LoggerFactory.getLogger(TestEnvironment.class);

    private boolean removeEntities = true;
    private TestEnvironmentBuilder testEnvironmentBuilder;
    private TestEnvironment testEnvironment;
    private boolean logExecutionTime = false;

    private TestBuilder() {
        testEnvironment = new TestEnvironment();
        testEnvironmentBuilder = new TestEnvironmentBuilder(testEnvironment);
    }

    public static TestBuilder newTest() {
        TestBuilder testBuilder = new TestBuilder();
        testBuilder.removeEntities = true;
        return testBuilder;
    }

    public static TestBuilder newTest(boolean removeEntities) {
        TestBuilder testBuilder = new TestBuilder();
        testBuilder.removeEntities = removeEntities;
        return testBuilder;
    }

    public void setLogExecutionTime(boolean logExecutionTime) {
        this.logExecutionTime = logExecutionTime;
    }

    public TestBuilder given(Consumer<TestEnvironmentBuilder> consumer) {
        try {
            withTimeLog("Environment creation took %s ms", () -> consumer.accept(this.testEnvironmentBuilder));
        } catch (Exception e) {
            if (removeEntities) removeEntitiesCreatedOnJBilling();
            throw e;
        }
        return this;
    }

    public TestBuilder givenForMultiple(Consumer<TestEnvironmentBuilder> consumer) {
        try {
            this.testEnvironment.setEnvironmentForMultipleTests(true);
            withTimeLog("Environment creation took {} ms", () -> consumer.accept(this.testEnvironmentBuilder));
        } catch (Exception e) {
            testEnvironment.removeEntitiesFromJBillingForMultipleTests();
            throw e;
        } finally {
            this.testEnvironment.setEnvironmentForMultipleTests(false);
        }
        return this;
    }

    public void removeEntitiesCreatedOnJBillingForMultipleTests() {
        testEnvironment.removeEntitiesFromJBillingForMultipleTests();
    }

    private void withTimeLog(String logMessage, Runnable runnable) {
        Date date = new Date();
        try {
            runnable.run();
        } finally {
            if (logExecutionTime)
                logger.debug(logMessage, new Date().getTime() - date.getTime());
        }
    }

    public TestEnvironment test(Consumer<TestEnvironment> consumer) {
        return executeTest(() -> consumer.accept(this.testEnvironment));
    }

    public TestEnvironment test(BiConsumer<TestEnvironment, TestEnvironmentBuilder> consumer) {
        return executeTest(() -> consumer.accept(this.testEnvironment, this.testEnvironmentBuilder));
    }
    
    public TestBuilder validate(BiConsumer<TestEnvironment, TestEnvironmentBuilder> consumer) {
        executeTest(() -> consumer.accept(this.testEnvironment, this.testEnvironmentBuilder));
        return this;
    }

    private TestEnvironment executeTest(Runnable test) {
        try {
            withTimeLog("Test Execution took {} ms", test);
        } finally {
            if (removeEntities) removeEntitiesCreatedOnJBilling();
        }
        return this.testEnvironment;
    }

    public void removeEntitiesCreatedOnJBilling() {
        testEnvironment.removeEntitiesFromJBilling();
    }

    public TestEnvironment getTestEnvironment() {
        return testEnvironment;
    }
}
