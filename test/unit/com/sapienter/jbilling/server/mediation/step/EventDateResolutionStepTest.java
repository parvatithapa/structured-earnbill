////TODO MODULARIZATION: MOVE IN THE MODULES
///*
// JBILLING CONFIDENTIAL
// _____________________
//
// [2003] - [2012] Enterprise jBilling Software Ltd.
// All Rights Reserved.
//
// NOTICE:  All information contained herein is, and remains
// the property of Enterprise jBilling Software.
// The intellectual and technical concepts contained
// herein are proprietary to Enterprise jBilling Software
// and are protected by trade secret or copyright law.
// Dissemination of this information or reproduction of this material
// is strictly forbidden.
// */
//
//package com.sapienter.jbilling.server.mediation.step;
//
//import com.sapienter.jbilling.server.item.PricingField;
//import com.sapienter.jbilling.server.mediation.step.eventDate.EventDateResolutionStep;
//import com.sapienter.jbilling.server.mediation.step.user.UserLoginResolutionStep;
//import com.sapienter.jbilling.server.mediation.task.MediationResult;
//import junit.framework.TestCase;
//import org.joda.time.DateMidnight;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import static org.easymock.classextension.EasyMock.replay;
//import static org.easymock.classextension.EasyMock.verify;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @author Panche Isajeski
// * @since 12/17/12
// */
//public class EventDateResolutionStepTest extends TestCase {
//
//    private static final Integer ENTITY_ID = 1;
//    private static final Integer USER_ID = 1;
//    private static final String USERNAME = "test_mediation_user";
//    private static final Integer CURRENCY_ID = 1;
//    private static final Date EVENT_DATE = new DateMidnight(2013, 1, 1).toDate();
//
//    public void testResolveEventDate() {
//
//        EventDateResolutionStep eventDateResolutionStep = new EventDateResolutionStep();
//        MediationResult mediationResult = new MediationResult("testConfig", false);
//        eventDateResolutionStep.executeStep(ENTITY_ID, mediationResult, buildPricingFields());
//
//        assertNotNull(mediationResult.getEventDate());
//        assertEquals(mediationResult.getEventDate(), EVENT_DATE);
//    }
//
//    private static List<PricingField> buildPricingFields() {
//
//        List<PricingField> pricingFields = new ArrayList<PricingField>();
//        PricingField.add(pricingFields, new PricingField("userfield", USERNAME));
//        PricingField.add(pricingFields, new PricingField("start", EVENT_DATE));
//
//        return pricingFields;
//    }
//}
