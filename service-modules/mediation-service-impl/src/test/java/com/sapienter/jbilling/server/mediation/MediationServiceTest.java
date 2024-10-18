//TODO MODULARIZATION: MOVE MEDIATION INTEGRATION TESTS IN THE CORE
//package com.sapienter.jbilling.server.mediation;
//
//import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation.SampleMediationJob;
//import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
//import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
//import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationErrorRecordDao;
//import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import java.io.File;
//
//import static org.junit.Assert.*;
//import static org.testng.Assert.assertEquals;
//
///**
// * Created by marcolin on 09/10/15.
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration( locations = {"/mediation-test-context.xml"})
//@EnableBatchProcessing
//public class MediationServiceTest {
//
//    public static final String FILE_LOCATION = "custom-mediations/sampleMediationRecords.csv";
//    @Autowired
//    MediationService mediationService;
//
//    @Autowired
//    JMErrorRepository jmErrorRepository;
//
//    @Autowired
//    JMRRepository jmrRepository;
//
////    @Test
////    public void testProcessCdrWithErrorRecord() {
////        ConversionResult conversionResult = mediationService.processCdr(1, 1, SampleMediationJob.getJobInstance().getJob(),
////                "1,38922734081,38925634973,1975-05-25 00:00:00,14126,320100,username-for-failure");
////        assertNotNull(conversionResult.getErrorRecord());
////        cleanDatabase();
////    }
////
////    @Test
////    public void testProcessCdrWithSuccessRecord() {
////        ConversionResult conversionResult = mediationService.processCdr(1, 1, SampleMediationJob.getJobInstance().getJob(),
////                "1,38922734081,38925634973,1975-05-25 00:00:00,14126,320100,username-for-success");
////        assertNotNull(conversionResult.getRecordCreated());
////        cleanDatabase();
////    }
//
//    private void cleanDatabase() {
//        jmErrorRepository.delete(jmErrorRepository.findAll());
//        jmrRepository.delete(jmrRepository.findAll());
//    }
//
//    @Test
//    public void testMediationLaunchWithDifferentRecordsRecords() {
//        mediationService.launchMediation(1, 1, SampleMediationJob.getJobInstance().getJob());
//        asserMediationErrorEqual("1", jmErrorRepository.findAll().iterator().next());
//        asserMediationRecordEqual("2", jmrRepository.findAll().iterator().next());
//        cleanDatabase();
//    }
//
//    @Test
//    public void testMediationLaunchWithFileAndDifferentRecordsRecords() {
//        File file = new File(FILE_LOCATION);
//        mediationService.launchMediation(1, 1, SampleMediationJob.getJobInstance().getJob(),file);
//        asserMediationErrorEqual("1", jmErrorRepository.findAll().iterator().next());
//        asserMediationRecordEqual("2", jmrRepository.findAll().iterator().next());
//        cleanDatabase();
//    }
//
//    private void asserMediationRecordEqual(String recordKey, JbillingMediationRecordDao record) {
//        assertEquals(record.getRecordKey(), recordKey);
//        assertEquals(record.getjBillingCompanyId(), new Integer(1));
//        assertEquals(record.getMediationCfgId(), new Integer(1));
//        assertEquals(record.getOrderId(), new Integer(1));
//        assertEquals(record.getStatus(), JbillingMediationRecordDao.STATUS.PROCESSED);
//    }
//
//    private void asserMediationErrorEqual(String recordKey, JbillingMediationErrorRecordDao errorRecord) {
//        assertEquals(errorRecord.getRecordKey(), recordKey);
//        assertEquals(errorRecord.getjBillingCompanyId(), new Integer(1));
//        assertEquals(errorRecord.getMediationCfgId(), new Integer(1));
//    }
//}
