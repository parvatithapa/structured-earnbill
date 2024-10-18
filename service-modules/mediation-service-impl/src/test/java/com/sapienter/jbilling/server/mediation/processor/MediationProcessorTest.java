//TODO MODULARIZATION: MOVE MEDIATION INTEGRATION TESTS IN THE CORE
//package com.sapienter.jbilling.server.mediation.processor;
//
//import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
//import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
//import com.sapienter.jbilling.server.order.*;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import static org.testng.Assert.*;
//
//import java.math.BigDecimal;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;
//
///**
// * Created by marcolin on 12/10/15.
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration( locations = {"/mediation-test-context.xml"})
//@EnableBatchProcessing
//public class MediationProcessorTest {
//
//
//    private OrderService orderService;
//
//    @Autowired
//    JMRRepository jmrRepository;
//    @Autowired
//    JMRProcessorWriterImpl writer;
//
//    @Test
//    public void testMediationProcessorForSimpleJMR() throws Exception {
//        JbillingMediationRecord record = testMediationRecord();
//        MediationEventResult result = new MediationEventResult();
//        result.setOrderLinedId(100);
//        result.setCurrentOrderId(1);
//        orderService = Mockito.mock(OrderService.class);
//        Mockito.when(orderService.addMediationEvent(Matchers.eq(record)))
//                .thenReturn(result);
//        writer.setJmrRepository(jmrRepository);
//        writer.setOrderService(orderService);
//        writer.write(Arrays.asList(record));
//        assertEquals(record.getStatus(), JbillingMediationRecord.STATUS.PROCESSED);
//        assertEquals(record.getOrderId(), result.getCurrentOrderId());
//        assertEquals(record.getOrderLineId(), result.getOrderLinedId());
//    }
//
//    private JbillingMediationRecord testMediationRecord() {
//        JbillingMediationRecord record = new JbillingMediationRecord();
//        record.setUserId(1);
//        record.setRecordKey("testKey");
//        record.setPricingFields("testPricingField:1");
//        record.setCurrencyId(2);
//        record.setEventDate(toDate("01/02/2015"));
//        record.setItemId(1000);
//        record.setQuantity(new BigDecimal(10));
//        return record;
//    }
//
//    private static Date toDate(String date) {
//        try {
//            return new SimpleDateFormat("dd/MM/yyyy").parse(date);
//        } catch (ParseException e) {throw new RuntimeException(e); }
//    }
//}
