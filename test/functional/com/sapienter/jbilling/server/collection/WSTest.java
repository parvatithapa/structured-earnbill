package com.sapienter.jbilling.server.collection;

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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by Leandro Zoi on 10/26/17.
 */

@Test(groups = { "web-services" }, testName = "collection.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);

    private JbillingAPI api;

    @BeforeClass
    private void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void test001CheckValidations(){
        AgeingWS[] steps = Arrays.stream(api.getAgeingConfiguration(Constants.LANGUAGE_ENGLISH_ID))
                                 .map(WSTest::setMessages)
                                 .toArray(AgeingWS[]::new);

        logger.debug("Steps count {}", steps.length);

        //duplicate names into the steps
        AgeingWS[] copySteps = addElement(steps, buildDuplicateNameAgeing(steps[0].getStatusStr()));
        boolean exceptionCached = saveSteps(copySteps, "config.ageing.error.non.unique.steps.name");
        assertTrue("An exception should been happen"    , exceptionCached);

        //duplicate days into the steps
        copySteps = addElement(steps, buildDuplicateDaysAgeing(steps[0].getDays()));
        exceptionCached = saveSteps(copySteps, "config.ageing.error.non.unique.steps");
        assertTrue("An exception should been happen", exceptionCached);

        //negative day into the steps
        copySteps = addElement(steps, buildNegativeDaysAgeing());
        exceptionCached = saveSteps(copySteps, "config.ageing.error.non.negative.days");
        assertTrue("An exception should been happen", exceptionCached);
    }

    private static AgeingWS[] addElement(AgeingWS[] steps, AgeingWS step) {
        AgeingWS[] newSteps = Arrays.copyOf(steps, steps.length + 1);
        newSteps[newSteps.length - 1] = step;
        return newSteps;
    }

    private boolean saveSteps(AgeingWS[] steps, String error) {
        try {
            api.saveAgeingConfiguration(steps, Constants.LANGUAGE_ENGLISH_ID);
        } catch (SessionInternalError sie) {
            logger.debug(sie.getErrorMessages()[0]);
            Assert.assertEquals(sie.getErrorMessages()[0], error, "Expected SessionInternal Error");
            return true;
        }

        return false;
    }

    private AgeingWS buildStep() {
        AgeingWS step = new AgeingWS();
        step.setSuspended(Boolean.TRUE);
        step.setDays(900);
        step.setStatusStr("Ageing Day Step");
        step.setCollectionType(CollectionType.REGULAR);
        step.setSendNotification(Boolean.FALSE);
        step.setPaymentRetry(Boolean.FALSE);
        step.setInUse(Boolean.FALSE);
        step.setStopActivationOnPayment(Boolean.FALSE);

        return step;
    }

    private AgeingWS buildDuplicateNameAgeing(String name) {
        AgeingWS step = buildStep();
        step.setStatusStr(name);

        return step;
    }

    private AgeingWS buildDuplicateDaysAgeing(int days) {
        AgeingWS step = buildStep();
        step.setDays(days);

        return step;
    }

    private AgeingWS buildNegativeDaysAgeing() {
        AgeingWS step = buildStep();
        step.setDays(-200);

        return step;
    }

    private static AgeingWS setMessages(AgeingWS step) {
        step.setCollectionType(CollectionType.REGULAR);

        return step;
    }
}
