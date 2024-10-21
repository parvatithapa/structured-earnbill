package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.mediation.mocks.MockMediationService;
import com.sapienter.jbilling.server.mediation.process.db.MediationProcessRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.annotations.AfterTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by marcolin on 27/10/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( locations = {"/mediation-process-test-context.xml"})
public class MediationProcessServiceImplTest {

    @Autowired
    MediationProcessService mediationProcessService;

    @Autowired
    MediationProcessRepository mediationProcessRepository;

    @Autowired
    MediationService mediationService;

    private static List<UUID> mediationProcessIdCreatedDuringTests = new ArrayList<>();

    @Test
    public void testMediationRepositoryIsWorking() {
        MediationProcess mediationProcess = saveMediationProcessForTest(1, 2);
        assertNotNull(mediationProcess.getId());
        MediationProcess mediationProcessRetrieved = mediationProcessService.getMediationProcess(mediationProcess.getId());
        assertEquals(mediationProcess.getId(), mediationProcessRetrieved.getId());
        testMediationProcessHasEntityAndConfiguration(mediationProcessRetrieved, 1, 2);
        cleanDataBase();
    }

    @Test
    public void testMediationProcessEntityIdFiltering() {
        saveMediationProcessForTest(1, 1);
        saveMediationProcessForTest(2, 2);
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList());
        assertEquals(1, mediationProcessForEntity.size());
        testMediationProcessHasEntityAndConfiguration(mediationProcessForEntity.get(0), 2, 2);
        cleanDataBase();
    }

    private void cleanDataBase() {
        for (UUID id: mediationProcessIdCreatedDuringTests) {
            mediationProcessService.deleteMediationProcess(id);
        }
        mediationProcessIdCreatedDuringTests.clear();
    }

    @Test
    public void testMediationProcessErrorsFiltering() {
        saveMediationProcessWithErrors();
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("errors", FilterConstraint.GREATER_THAN, 0)));
        assertEquals(1, mediationProcessForEntity.size());
        testMediationProcessHasEntityAndConfiguration(mediationProcessForEntity.get(0), 2, 2);
        mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("errors", FilterConstraint.EQ, 0)));
        assertEquals(0, mediationProcessForEntity.size());
        mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("errors", FilterConstraint.EQ, 2)));
        assertEquals(1, mediationProcessForEntity.size());
        cleanDataBase();
    }

    /**
     * We will retrieve jbilling_mediation_records with order id and then filter mediation processes using the process_id on the jbilling_mediation_record
     */
    @Test
    public void testMediationProcessOrderIdFilteringNotReturnsAnything() {
        MediationProcess mediationProcess = saveMediationProcessForTest(2, 2);
        Integer testOrderId = 100;
        Integer orderIdNotOnMediationProcess = 200;
        ((MockMediationService) mediationService).setMediationRecordsForTest(createMediationRecordForTest(mediationProcess.getId(), testOrderId));
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("orderId", FilterConstraint.EQ, orderIdNotOnMediationProcess)));
        assertEquals(0, mediationProcessForEntity.size());
        cleanDataBase();
    }

    @Test
    public void testMediationProcessOrderIdFilteringShouldReturnTheMediationProcess() {
        MediationProcess mediationProcess = saveMediationProcessForTest(2, 2);
        Integer testOrderId = 100;
        ((MockMediationService) mediationService).setMediationRecordsForTest(createMediationRecordForTest(mediationProcess.getId(), testOrderId));
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("orderId", FilterConstraint.EQ, testOrderId)));
        assertEquals(1, mediationProcessForEntity.size());
        cleanDataBase();
    }

    private List<JbillingMediationRecord> createMediationRecordForTest(UUID mediationProcessId, Integer orderId) {
        List<JbillingMediationRecord> mediationRecords = new ArrayList<>();
        JbillingMediationRecord record = new JbillingMediationRecord();
        record.setOrderId(orderId);
        record.setProcessId(mediationProcessId);
        mediationRecords.add(record);
        return mediationRecords;
    }

    @Test
    public void testMediationProcessStartDateFilteringShouldReturn1MediationProcess() throws ParseException {
        setMediationProcessWithStartDate();
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(1, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.betweenDates("startDate",
                        toDate("01/09/2015"), toDate("01/11/2015"))));
        assertEquals(1, mediationProcessForEntity.size());
        testMediationProcessHasEntityAndConfiguration(mediationProcessForEntity.get(0), 1, 1);
        cleanDataBase();
    }

    @Test
    public void testMediationProcessStartDateFilteringShouldReturnNothing() throws ParseException {
        setMediationProcessWithStartDate();
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(1, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.betweenDates("startDate",
                        toDate("01/12/2015"), toDate("01/01/2016"))));
        assertEquals(0, mediationProcessForEntity.size());
        cleanDataBase();
    }

    @Test
    public void testMediationProcessEntityIdAndErrorsFiltering() {
        saveMediationProcessForTest(1, 1);
        saveMediationProcessWithErrors();
        saveMediationProcessForTest(2, 2);
        List<MediationProcess> mediationProcessForEntity = mediationProcessService.findMediationProcessByFilters(2, 0, 10,
                "startDate", "desc", Arrays.asList(Filter.integer("errors", FilterConstraint.GREATER_THAN, 0)));
        assertEquals(1, mediationProcessForEntity.size());
        testMediationProcessHasEntityAndConfiguration(mediationProcessForEntity.get(0), 2, 2);
        cleanDataBase();
    }

    private MediationProcess saveMediationProcessForTest(int entityId, int configurationId) {
        MediationProcess mediationProcess = mediationProcessService.saveMediationProcess(entityId, configurationId, null);
        mediationProcessIdCreatedDuringTests.add(mediationProcess.getId());
        return mediationProcess;
    }

    private void saveMediationProcessWithErrors() {
        MediationProcess mediationProcess = saveMediationProcessForTest(2, 2);
        mediationProcess.setErrors(2);
        mediationProcessService.updateMediationProcess(mediationProcess);
    }

    private void setMediationProcessWithStartDate() throws ParseException {
        MediationProcess mediationProcess = saveMediationProcessForTest(1, 1);
        mediationProcess.setStartDate(toDate("01/10/2015"));
        mediationProcessService.updateMediationProcess(mediationProcess);
    }

    private Date toDate(String dateString) throws ParseException {
        return new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
    }


    private void testMediationProcessHasEntityAndConfiguration(MediationProcess inTesting, Integer expectedEntityId, Integer expectedConfigurationId) {
        assertEquals(expectedEntityId, inTesting.getEntityId());
        assertEquals(expectedConfigurationId, inTesting.getConfigurationId());
    }
}
