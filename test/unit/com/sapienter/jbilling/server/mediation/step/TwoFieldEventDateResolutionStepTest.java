//TODO MODULARIZATION: MOVE IN THE MODULES
//package com.sapienter.jbilling.server.mediation.step;
//
//import com.sapienter.jbilling.server.item.PricingField;
//import com.sapienter.jbilling.server.mediation.step.eventDate.TwoFieldEventDateResolutionStep;
//import junit.framework.TestCase;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.List;
//
///**
// * @author Vladimir Carevski
// */
//public class TwoFieldEventDateResolutionStepTest extends TestCase {
//
//    private TwoFieldEventDateResolutionStep eventDateResolutionStep;
//
//    @Override
//    protected void setUp() throws Exception {
//        eventDateResolutionStep = new TwoFieldEventDateResolutionStep(
//                "CALL_DATE", "yyyyMMdd", "CALL_TIME", "HHmmss");
//    }
//
//    public void testEventDate001(){
//        MediationStepResult result = new MediationStepResult();
//        List<PricingField> fields = new ArrayList<PricingField>();
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(2013, 10, 24, 22, 15, 21);
//        calendar.set(Calendar.MILLISECOND, 0);
//        PricingField.add(fields, createPricingField("CALL_DATE", "20131124"));
//        PricingField.add(fields, createPricingField("CALL_TIME", "221521"));
//        eventDateResolutionStep.executeStep(null, result, fields);
//        assertEquals("Dates are not equal", 0, calendar.getTime().compareTo(result.getEventDate()));
//    }
//
//    public void testEventDate002(){
//        MediationStepResult result = new MediationStepResult();
//        List<PricingField> fields = new ArrayList<PricingField>();
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(2013, 10, 25, 21, 33, 57);
//        calendar.set(Calendar.MILLISECOND, 0);
//        fields.add(createPricingField("CALL_DATE", "20131125"));
//        fields.add(createPricingField("CALL_TIME", "213357"));
//        eventDateResolutionStep.executeStep(null, result, fields);
//        assertEquals("Dates are not equal", 0, calendar.getTime().compareTo(result.getEventDate()));
//    }
//
//    public void testEventDate003(){
//        MediationStepResult result = new MediationStepResult();
//        List<PricingField> fields = new ArrayList<PricingField>();
//
//        PricingField.add(fields, createPricingField("CALL_DATE", "20131125"));
//        PricingField.add(fields, createPricingField("CALL_TIME", "2157"));
//        boolean success = eventDateResolutionStep.executeStep(null, result, fields);
//
//        assertFalse(success);
//        assertEquals("Error parsing the CDR date and time fields", result.getErrors().get(0));
//    }
//
//    public void testEventDate004(){
//        MediationStepResult result = new MediationStepResult();
//        List<PricingField> fields = new ArrayList<PricingField>();
//        fields.add(createPricingField("CALL_DATE", "20131125"));
//        boolean success = eventDateResolutionStep.executeStep(null, result, fields);
//
//        assertFalse(success);
//        assertEquals("The CDR does not contain the time field:CALL_TIME", result.getErrors().get(0));
//    }
//
//    public void testEventDate005(){
//        MediationStepResult result = new MediationStepResult();
//        List<PricingField> fields = new ArrayList<PricingField>();
//
//        fields.add(createPricingField("CALL_TIME", "2157"));
//        boolean success = eventDateResolutionStep.executeStep(null, result, fields);
//
//        assertFalse(success);
//        assertEquals("The CDR does not contain the date field:CALL_DATE", result.getErrors().get(0));
//    }
//
//    private PricingField createPricingField(String name, String value) {
//        PricingField field = new PricingField();
//        field.setName(name);
//        field.setStrValue(value);
//        field.setType(PricingField.Type.STRING);
//        return field;
//    }
//}
