package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Created by hitesh on 14/3/16.
 */

@Test(groups = {"web-services", "edi-type"}, sequential = true, testName = "EDITypeTest")
public class EDITypeTest extends ApiTestCase {

    private static final Logger logger = LoggerFactory.getLogger(EDITypeTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public void setup() throws IOException, JbillingAPIException {
        if (null == api) {
            logger.debug("Inside setup api value null");
            api = JbillingAPIFactory.getAPI();
        }
    }

    @AfterClass
    public void cleanUp() {
        if (null != api) {
            api = null;
        }
    }

    private List<EDIFileStatusWS> createEdiTypeStatus(int statusCount) {

        List<EDIFileStatusWS> statusWSList = new ArrayList<>();
        for (int i = 1; i < statusCount + 1; i++) {
            EDIFileStatusWS statusWS = new EDIFileStatusWS();
            statusWS.setName("EDIStatus-" + i);
            statusWSList.add(statusWS);
        }
        return statusWSList;
    }

    private List<EDIFileStatusWS> createEdiTypeStatusWithChildStatus() {
        List<EDIFileStatusWS> statusWSList = new ArrayList<>();
        EDIFileStatusWS statusWS1 = new EDIFileStatusWS();
        statusWS1.setName("EDIStatus-1");

        EDIFileStatusWS statusWS2 = new EDIFileStatusWS();
        statusWS2.setName("EDIStatus-2");
        List<EDIFileStatusWS> subStatusWSList = new ArrayList<>();
        subStatusWSList.add(statusWS1);
        statusWS2.setAssociatedEDIStatuses(subStatusWSList);

        statusWSList.add(statusWS1);
        statusWSList.add(statusWS2);
        return statusWSList;
    }

    private EDITypeWS createEdIType(String name, String ediSuffix, Integer entityId, boolean ediTypeStatus, boolean ediTypeStatusWithSubStatus, int ediStatusCount) {
        EDITypeWS ediTypeWs = new EDITypeWS();
        List<Integer> entities = new ArrayList<Integer>();
        entities.add(entityId);
        ediTypeWs.setName(name);
        ediTypeWs.setEdiSuffix(ediSuffix);
        ediTypeWs.setEntityId(entityId);
        ediTypeWs.setEntities(entities);
        if (ediTypeStatus) ediTypeWs.setEdiStatuses(createEdiTypeStatus(ediStatusCount));
        if (ediTypeStatusWithSubStatus) ediTypeWs.setEdiStatuses(createEdiTypeStatusWithChildStatus());

        return ediTypeWs;
    }

    private File getSampleFile(String fileName) {
        String path = Thread.currentThread()
                .getContextClassLoader().getResource(fileName).getPath();
        File tempFile = null;
        try {
            tempFile = new File("/tmp", fileName);
            Files.copy(new File(path).toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            fail("There was an error during the copy of the file in the tmp folder");
        }
        return tempFile;
    }

    //EDIType creation testcase
    @Test
    public void test001CreateEmptyEDIType() {
        try {
            Integer ediTypeId = api.createEDIType(null, null);
            fail("EDIType should not be created");
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    @Test
    public void test002CreateEDITypeWithOutFormatFile() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, null);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
            assertNotNull("EDIType had been created:", ediTypeId);
        } catch (Exception e) {
            fail("EDIType should be created:" + e);
        }
    }

    @Test
    public void test003CreateEDITypeWithInvalidFormatFileExtension() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);
        File ediFormatFile = new File("test.csv");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            fail("EDIType should not be created");
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    @Test
    public void test004CreateEDIType() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);

        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            logger.debug("ediTypeId: {}", ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
            assertNotNull("EDIType had been created:", ediTypeId);
        } catch (Exception e) {
            fail("EDIType should be created:" + e);
        }
    }

    @Test
    public void test005CreateEDITypeWithEDIStatus() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, true, false, 5);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            EDITypeWS ediTypeWsNew = api.getEDIType(ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
            assertEquals("EDIType ID should be equal", ediTypeId, ediTypeWsNew.getId());
            assertEquals("EDIType status count should be equal", ediTypeWs.getEdiStatuses().size(), ediTypeWsNew.getEdiStatuses().size());
        } catch (Exception e) {
            fail("exception:" + e);
        }
    }

    @Test
    public void test006CreateEDITypeWithEDIStatusAndSubStatus() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, true, 0);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            EDITypeWS ediTypeWsNew = api.getEDIType(ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
            assertEquals("EDIType ID should be equal", ediTypeId, ediTypeWsNew.getId());
            assertEquals("EDIType status count should be equal", ediTypeWs.getEdiStatuses().size(), ediTypeWsNew.getEdiStatuses().size());
            for (EDIFileStatusWS statusWS : ediTypeWs.getEdiStatuses()) {
                if (statusWS.getName().equals("EDIStatus-2")) {
                    for (EDIFileStatusWS statusWSNew : ediTypeWsNew.getEdiStatuses()) {
                        if (statusWSNew.getName().equals("EDIStatus-2")) {
                            assertEquals("EDIType sub status count should be equal", statusWS.getAssociatedEDIStatuses().size(), statusWSNew.getAssociatedEDIStatuses().size());
                            assertEquals("EDIType sub status name should be equal", statusWS.getAssociatedEDIStatuses().get(0).getName(), statusWSNew.getAssociatedEDIStatuses().get(0).getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            fail("exception:" + e);
        }
    }

    //EDIType finding testcase
    @Test
    public void test007FindEDIType() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            ediTypeWs = api.getEDIType(ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);
            assertEquals("EDIType ID should be equal", ediTypeId, ediTypeWs.getId());
        } catch (Exception e) {
            fail("exception:" + e);
        }
    }

    @Test
    public void test008FindNonExistEDIType() {
        try {
            EDITypeWS ediTypeWs = api.getEDIType(1000);
            assertEquals("EDIType should be null:", ediTypeWs == null);
            fail("EDIType should not be created");
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    //EDIType delete testcase
    @Test
    public void test009DeleteEDIType() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");
        Integer ediTypeId = null;
        try {
            ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            api.deleteEDIType(ediTypeId);
        } catch (Exception e) {
            fail("exception:" + e);
        }

        try {
            EDITypeWS newEdiTypeWs = api.getEDIType(ediTypeId);
            fail("EDIType should throw ObjectNotFoundException");
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    @Test
    public void test010DeleteNonExistEDIType() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            api.deleteEDIType(ediTypeId);
            api.deleteEDIType(ediTypeId);
            fail("EDIType has not be deleted NonExistEDIType");
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    @Test
    public void test011DeleteEDITypeWithEDIStatus() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, true, false, 5);

        File ediFormatFile = getSampleFile("edi_type_sample.xml");
        EDIFileStatusWS statusWS = null;

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            EDITypeWS createdEdiTypeWs = api.getEDIType(ediTypeId);
            api.deleteEDIType(ediTypeId);
            statusWS = createdEdiTypeWs.getEdiStatuses().get(0);

        } catch (Exception e) {
            fail("exception:" + e);
        }


        try {
            EDIFileStatusWS ediFileStatusWS = api.findEdiStatusById(statusWS.getId());
            fail("EDIType should throw ObjectNotFoundException");
        } catch (Exception e) {
            assertNotNull("Exception caught:" + e, e);
        }
    }

    //EDIType updation testcase
    @Test
    public void test012UpdateEDIType() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, false, false, 0);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            EDITypeWS newEdiTypeWs = api.getEDIType(ediTypeId);
            newEdiTypeWs.setName("EDIType-10-update");
            newEdiTypeWs.setEdiSuffix("888");
            Integer updateEdiTypeId = api.createEDIType(newEdiTypeWs, ediFormatFile);
            //find updated EDI
            EDITypeWS updateEdiType = api.getEDIType(ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);

            assertEquals("EDIType update id should be equal", newEdiTypeWs.getId(), updateEdiTypeId);
            assertEquals("Update EDIType name should be updated", "EDIType-10-update", updateEdiType.getName());
            assertEquals("Update EDIType suffix should be updated", "888", updateEdiType.getEdiSuffix());

        } catch (Exception e) {
            fail("test012UpdateEDIType:" + e);
        }
    }

    @Test
    public void test013UpdateEDITypeWithEDIStatus() {
        EDITypeWS ediTypeWs = createEdIType("EDIType-10", "999", 1, true, false, 1);
        File ediFormatFile = getSampleFile("edi_type_sample.xml");

        try {
            Integer ediTypeId = api.createEDIType(ediTypeWs, ediFormatFile);
            EDITypeWS newEdiTypeWs = api.getEDIType(ediTypeId);
            newEdiTypeWs.setName("EDIType-10-update");
            newEdiTypeWs.setEdiSuffix("888");
            newEdiTypeWs.getEdiStatuses();
            newEdiTypeWs.getEdiStatuses().get(0).setName("EDIStatusUpdate-1");

            Integer updateEdiTypeId = api.createEDIType(newEdiTypeWs, ediFormatFile);
            //find updated EDI
            EDITypeWS updateEdiType = api.getEDIType(ediTypeId);
            if (ediTypeId != null) api.deleteEDIType(ediTypeId);

            assertEquals("EDIType update id should be equal", newEdiTypeWs.getId(), updateEdiTypeId);
            assertEquals("Update EDIType status name should be updated", "EDIStatusUpdate-1", updateEdiType.getEdiStatuses().get(0).getName());

        } catch (Exception e) {
            fail("test013UpdateEDITypeWithEDIStatus:" + e);
        }
    }

}
