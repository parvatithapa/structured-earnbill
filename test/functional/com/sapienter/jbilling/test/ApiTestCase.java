/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.test;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

/**
 * 
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 *
 */
@ContextConfiguration(classes = ApiTestConfig.class, loader = AnnotationConfigContextLoader.class)
public class ApiTestCase extends AbstractTestNGSpringContextTests {

    public static final Integer TEST_LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
    public static final Integer TEST_ENTITY_ID   = 1;

    @Autowired
    protected JbillingAPI       api;

    @Override
    @BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
    protected void springTestContextPrepareTestInstance () throws Exception {
        super.springTestContextPrepareTestInstance();
        prepareTestInstance();
    }

    @Override
    @BeforeClass(alwaysRun = true)
    protected void springTestContextBeforeTestClass () throws Exception {
        super.springTestContextBeforeTestClass();
        beforeTestClass();
    }

    @Override
    @AfterClass(alwaysRun = true)
    protected void springTestContextAfterTestClass () throws Exception {
        afterTestClass();
        super.springTestContextAfterTestClass();
    }

    /*
     * methods for subclasses to override
     */
    protected void afterTestClass () throws Exception {
    }

    protected void beforeTestClass () throws Exception {
    }

    protected void prepareTestInstance () throws Exception {
    }

    /*
     * utility methods
     */
    protected static Date AsDate (String dateStr) {
        return TestUtils.AsDate(dateStr);
    }

    protected static Date AsDate (int year, int month, int day) {
        return TestUtils.AsDate(year, month, day);
    }

    protected static String AsString (Date date) {
        return TestUtils.AsString(date);
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation
     */
    protected static String S (final String pattern, final Object argument) {
        return Util.S(pattern, argument);
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation. multi-param
     * version.
     */
    protected static String S (final String pattern, final Object... arguments) {
        return Util.S(pattern, arguments);
    }
}
