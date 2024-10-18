//TODO MODULARIZATION: MOVE MEDIATION INTEGRATION TESTS IN THE CORE
/*
package com.sapienter.jbilling.server.mediation.processor.integration;

import com.sapienter.jbilling.server.customer.CustomerService;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.processor.JMRProcessorWriterImpl;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.user.UserWS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import static org.testng.Assert.*;


*/
/**
 * Created by marcolin on 14/10/15.
 *//*

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( locations = {"/mediation-context-for-integration-test.xml"})
public class MediationProcessorIntegrationTest {

    @Autowired
    CustomerService customerService;

    @Autowired
    OrderService orderService;

    @Autowired
    JMRProcessorWriterImpl processorWriter;

    @Autowired
    JMRRepository jmrRepository;


    @Test
    public void mediationProcessorForSimpleJmrTest() throws Exception {
        JbillingMediationRecord record = mediationRecord();
        //Create User in jBilling
//        customerService.createUser(testUser());
        //Create Product Category in jBilling
        //Create Product in jBilling
        processorWriter.write(Arrays.asList(record));
        JbillingMediationRecordDao savedRecord = jmrRepository.findAll().iterator().next();
        assertEquals(savedRecord.getStatus().name(), JbillingMediationRecord.STATUS.PROCESSED.name());
        //Clean the environment (remove user, product category etc)
    }

    private UserWS testUser() {
        return null;
    }

    private JbillingMediationRecord mediationRecord() {
        JbillingMediationRecord record = new JbillingMediationRecord();
        record.setRecordKey("TestKey2");
        record.setItemId(2800);
        record.setEventDate(toDate("01/05/2015"));
        record.setCurrencyId(1);
        record.setDescription("TestDescription");
        record.setjBillingCompanyId(1);
        record.setMediationCfgId(1);
        record.setUserId(10760);
        record.setQuantity(new BigDecimal(15));
        return record;
    }

    private static Date toDate(String date) {
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(date);
        } catch (ParseException e) {throw new RuntimeException(e); }
    }
}
*/
