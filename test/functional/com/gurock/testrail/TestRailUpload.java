package com.gurock.testrail;

import eu.infomas.annotation.AnnotationDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by branko on 6/2/16.
 */
public class TestRailUpload {

    private static final Logger logger = LoggerFactory.getLogger(TestRailUpload.class);
    private static final Integer JBILLING_PROJECT_ID = 12;
    private static final Integer HP_SUITE_ID = 9575;
//    private static final Integer TEST_SUITE_ID = 12301;

    private static final String DEFAULT_TEST_SUITE_NAME = "TestUploadofTestSuit";

    private APIClient client;


    public TestRailUpload(String url, String username, String password) {

        client = new APIClient(url);
        client.setUser(username);
        client.setPassword(password);
    }

    public APIClient getClient() {
        return client;
    }

    public void setClient(APIClient client) {
        this.client = client;
    }


    public static final void main(String[] args) throws APIException, IOException {

        TestRailUpload testRailUpload = new TestRailUpload(
                "https://appdirect.testrail.com/",
                "branko.stevanovic@jbilling.com",
                "SOVVcmNnU0tau.nkyzpn-FpSOS3E.rwCSopMwA2Eg");

        Map<String, Integer> suites = testRailUpload.getSuitesInProject(JBILLING_PROJECT_ID)
                .stream().collect(Collectors.toMap(TestRailSuite::getName, TestRailSuite::getId));

        Integer testSuiteId = null;
        if(null != suites && !suites.isEmpty()) {
            testSuiteId = suites.get(DEFAULT_TEST_SUITE_NAME);

            List<TestRailSection> trServerSections = testRailUpload.getSectionsInSuite(testSuiteId);

            logger.debug("Showing sections and cases found in suite with id: {}.", testSuiteId);
            for (TestRailSection section : trServerSections) {
                logger.debug("Section {}.", section.toString());
                for (TestRailCase trCase : section.getCases()) {
                    logger.debug("Case {}.", trCase.toString());
                }
            }

            List<TestRailSection> localSections = testRailUpload.findClassesWithAnnotation(TestRailClass.class);
            localSections.forEach((el) -> {el.setSuiteId(suites.get(suites.get(el.getSuiteName())));});

            logger.debug("Showing locally annotated sections and cases.");
            for (TestRailSection section : localSections) {
                logger.debug("Section {}.", section.toString());
                for (TestRailCase trCase : section.getCases()) {
                    logger.debug("Case {}.", trCase.toString());
                }
            }

            testRailUpload.uploadToRepository(trServerSections, localSections);

            trServerSections = testRailUpload.getSectionsInSuite(testSuiteId);

            logger.debug("Showing sections and cases found in suite with id: {}.", testSuiteId);
            for (TestRailSection section : trServerSections) {
                logger.debug("Section {}.", section.toString());
                for (TestRailCase trCase : section.getCases()) {
                    logger.debug("Case {}.", trCase.toString());
                }
            }
        }
    }



    public List<TestRailSuite> getSuitesInProject(Integer projectId) {
        if (null == projectId) {
            throw new IllegalArgumentException("projectId can not be null");
        }
        List<TestRailSuite> result = new ArrayList<>();
        ArrayList<JSONObject> suiteObjects = null;
        try {
            suiteObjects = (JSONArray) client.sendGet("get_suites/" + projectId);
        } catch (Exception e) {
            logger.error("Error getting the suites in the project", e);
        }
        if (null != suiteObjects) {
            for (JSONObject suiteObject : suiteObjects) {

                TestRailSuite suite = new TestRailSuite();
                suite.setId(Integer.parseInt(suiteObject.get("id").toString()));
                suite.setName(suiteObject.get("name").toString());
                Object description = suiteObject.get("description");
                if (null != description) {
                    suite.setDescription(description.toString());
                }
                suite.setSections(getSectionsInSuite(suite.getId()));
                result.add(suite);
            }
        }

        return result;
    }

    public List<TestRailSection> getSectionsInSuite(Integer suiteId) {

        List<TestRailSection> result = new ArrayList<>();
        ArrayList<JSONObject> sectionObjects = null;
        try {
            sectionObjects = (JSONArray) client.sendGet("get_sections/" + JBILLING_PROJECT_ID + "&suite_id=" + suiteId);
        } catch (Exception e) {
            logger.error("Error getting the sections in the suite", e);
        }
        if (null != sectionObjects) {
            for (JSONObject sectionObject : sectionObjects) {

                TestRailSection section = new TestRailSection();
                section.setId(Integer.parseInt(sectionObject.get("id").toString()));
                section.setName(sectionObject.get("name").toString());
                section.setSuiteId(Integer.parseInt(sectionObject.get("suite_id").toString()));
                Object description = sectionObject.get("description");
                if (null != description) {
                    section.setDescription(description.toString());
                }
                Object parentId = sectionObject.get("parent_id");
                if (null != parentId) {
                    section.setParentId(Integer.parseInt(parentId.toString()));
                }
                section.setCases(getCasesInSection(section.getSuiteId(), section.getId()));
                result.add(section);
            }
        }

        return result;
    }

    public List<TestRailCase> getCasesInSection(Integer suiteId, Integer sectionId) {

        List<TestRailCase> result = new ArrayList<>();
        ArrayList<JSONObject> caseObjects = null;
        try {
            caseObjects = (JSONArray) client.sendGet("get_cases/" + JBILLING_PROJECT_ID + "&suite_id=" + suiteId + "&section_id=" + sectionId);
        } catch (Exception e) {
            logger.error("Error getting the cases in the section", e);
        }
        for (JSONObject caseObject : caseObjects) {

            TestRailCase trCase = new TestRailCase();
            trCase.setId(Integer.parseInt(caseObject.get("id").toString()));
            trCase.setTitle(caseObject.get("title").toString());
            trCase.setSuiteId(Integer.parseInt(caseObject.get("suite_id").toString()));
            trCase.setSectionId(Integer.parseInt(caseObject.get("section_id").toString()));
            Object customSummary = caseObject.get("custom_summary");
            if (null != customSummary) {
                trCase.setCustomSummary(customSummary.toString());
            }
            Object refs = caseObject.get("refs");
            if (null != refs) {
                trCase.setRefs(refs.toString());
            }
            result.add(trCase);
        }

        return result;
    }

    public TestRailSuite getSuite(Integer suiteId) {

        TestRailSuite result = null;
        JSONObject suite = null;
        try {
            suite = (JSONObject) client.sendGet("get_suite/" + suiteId);
        } catch (Exception e) {
            logger.error("Error getting the suite", e);
        }
        if (null != suite) {
            result = new TestRailSuite(
                    Integer.parseInt(suite.get("id").toString()),
                    suite.get("name").toString(),
                    suite.get("description").toString()
            );
        }

        return result;
    }

    public JSONObject createSuite(String name, String description) {

        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("add_suite/" + JBILLING_PROJECT_ID, map);
        } catch (Exception e) {
            logger.error("Error creating the suite", e);
        }

        return result;
    }

    public JSONObject updateSuite(String name, String description, Integer suiteId) {

        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("update_suite/" + suiteId, map);
        } catch (Exception e) {
            logger.error("Error updating the suite", e);
        }

        return result;
    }


    public TestRailSection getSection(Integer sectionId) throws IOException, APIException{

        TestRailSection result = null;
        JSONObject section = (JSONObject) client.sendGet("get_section/" + sectionId);
        if (null != section) {
            result = new TestRailSection(
                Integer.parseInt(section.get("id").toString()),
                section.get("name").toString(),
                section.get("description").toString(),
                Integer.parseInt(section.get("parent_id").toString()),
                Integer.parseInt(section.get("suite_id").toString())
            );
        }

        return result;
    }

    public JSONObject createSection(TestRailSection section) {

        return createSection(section.getName(), section.getDescription(), section.getSuiteId(), section.getParentId());
    }

    public JSONObject createSection(String name, String description, Integer suiteId, Integer parentId) {
        logger.debug("Create suite with, Name: {}, Description:{}, SuiteId:{}, ParentId:{}",
                    name, description, suiteId, parentId);
        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("suite_id", suiteId.toString());
        if (null != parentId) {
            map.put("parent_id", parentId.toString());
        }
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("add_section/" + JBILLING_PROJECT_ID, map);
        } catch (Exception e) {
            logger.error("Error creating the section", e);
        }

        return result;
    }

    public JSONObject updateSection(TestRailSection section) {

        return updateSection(section.getName(), section.getDescription(), section.getId());
    }

    public JSONObject updateSection(String name, String description, Integer sectionId) {

        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("update_section/" + sectionId, map);
        } catch (Exception e) {
            logger.error("Error updating the suite", e);
        }

        return result;
    }

    public TestRailCase getCase(Integer caseId) throws IOException, APIException{

        TestRailCase result = null;
        JSONObject testCase = (JSONObject) client.sendGet("get_case/" + caseId);
        if (null != testCase) {
            result = new TestRailCase(
                    Integer.parseInt(testCase.get("id").toString()),
                    testCase.get("title").toString(),
                    testCase.get("custom_summary").toString(),
                    testCase.get("refs").toString(),
                    Integer.parseInt(testCase.get("section_id").toString()),
                    Integer.parseInt(testCase.get("suite_id").toString())
            );
        }

        return result;
    }

    public JSONObject createCase(TestRailCase trCase) {

        return createCase(trCase.getTitle(), trCase.getCustomSummary(), trCase.getRefs(), trCase.getSectionId());
    }

    public JSONObject createCase(String title, String customSummary, String refs, Integer sectionId) {

        HashMap<String, String> map = new HashMap<>();
        map.put("title", title);
        map.put("custom_summary", customSummary);
        map.put("refs", refs);
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("add_case/" + sectionId, map);
        } catch (Exception e) {
            logger.error("Error creating the case", e);
        }

        return result;
    }

    public JSONObject updateCase(TestRailCase trCase) {

        return updateCase(trCase.getTitle(), trCase.getCustomSummary(), trCase.getRefs(), trCase.getId());
    }

    public JSONObject updateCase(String title, String customSummary, String refs, Integer caseId) {

        HashMap<String, String> map = new HashMap<>();
        map.put("title", title);
        map.put("custom_summary", customSummary);
        map.put("refs", refs);
        JSONObject result = null;
        try {
            result = (JSONObject) client.sendPost("update_case/" + caseId, map);
        } catch (Exception e) {
            logger.error("Error updating the case", e);
        }

        return result;
    }

    public List<TestRailSection> findClassesWithAnnotation(Class annotation) {

        List<TestRailSection> result = new ArrayList<>();

        final AnnotationDetector.TypeReporter reporter = new AnnotationDetector.TypeReporter() {

            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends Annotation>[] annotations() {
                return new Class[]{annotation};
            }

            @Override
            public void reportTypeAnnotation(Class<? extends Annotation> annotation,
                                             String className) {
                try {
                    TestRailClass trClass = Class.forName(className).getAnnotation(TestRailClass.class);

                    TestRailSection section = new TestRailSection();
                    section.setId(0);
                    section.setName(className);
                    if (!trClass.suite().isEmpty()) {
                        section.setSuiteName(trClass.suite());
                    }
                    if (!trClass.parent().isEmpty()) {
                        section.setParentSuiteName(trClass.parent());
                    }
                    if (!trClass.description().isEmpty()) {
                        section.setDescription(trClass.description());
                    }
                    section.setCases(
                            findMethodsWithAnnotations(
                                    section.getSuiteId(), section.getId(), Class.forName(className).getMethods()));
                    result.add(section);
                } catch (Exception e) {
                    logger.error("Error finding classes with annotation", e);
                }
            }

        };

        final AnnotationDetector cf = new AnnotationDetector(reporter);
        try {
            cf.detect();
        } catch (Exception e) {
            logger.error("Error in the AnnotationDetector", e);
        }

        return result;
    }

    public List<TestRailCase> findMethodsWithAnnotations(Integer suiteId, Integer sectionId, Method[] methods) {

        List<TestRailCase> result = new ArrayList<>();

        for (Method method : methods) {
            TestRailMethod annotation = method.getAnnotation(TestRailMethod.class);
            if (null != annotation) {
                TestRailCase trCase = new TestRailCase();
                trCase.setId(0);
                trCase.setSuiteId(suiteId);
                trCase.setSectionId(sectionId);
                if (!annotation.title().isEmpty()) {
                    trCase.setTitle(annotation.title());
                } else {
                    trCase.setTitle(method.getName());
                }
                if (!annotation.refs().isEmpty()) {
                    trCase.setRefs(annotation.refs());
                } else {
                    trCase.setRefs("No refs.");
                }
                trCase.setCustomSummary(annotation.summary());
                result.add(trCase);
            }
        }

        return result;
    }

    public void uploadToRepository(List<TestRailSection> remote, List<TestRailSection> local) {

        Integer currentSectionId = 0;

        for (TestRailSection localSection : local) {

            List<TestRailSection> filteredRemoteSection = remote.stream()
                                                    .filter(sec -> localSection.getName().equals(sec.getName()))
                                                    .collect(Collectors.toList());

            if (0 == filteredRemoteSection.size()) {
                logger.debug("Creating section: {}", localSection);
                JSONObject response = createSection(localSection);
                currentSectionId = Integer.parseInt(response.get("id").toString());
            } else {
                currentSectionId = filteredRemoteSection.get(0).getId();
                if (!filteredRemoteSection.get(0).equals(localSection)) {
                    localSection.setId(filteredRemoteSection.get(0).getId());
                    logger.debug("Updating section: {}", localSection);
                    updateSection(localSection);
                }
            }

            for (TestRailCase localCase : localSection.getCases()) {

                List<TestRailCase> filteredRemoteCase = new ArrayList<>();
                if (0 != filteredRemoteSection.size()) {
                    filteredRemoteCase = filteredRemoteSection.get(0).getCases().stream()
                            .filter(frCase -> localCase.getTitle().equals(frCase.getTitle()))
                            .collect(Collectors.toList());
                }

                if (0 == filteredRemoteCase.size()) {
                    localCase.setSectionId(currentSectionId);
                    logger.debug("Creating case: {}", localCase);
                    createCase(localCase);
                } else {
                    if (!filteredRemoteCase.get(0).equals(localCase)) {
                        localCase.setId(filteredRemoteCase.get(0).getId());
                        localCase.setSectionId(currentSectionId);
                        logger.debug("Updating case: {}", localCase);
                        updateCase(localCase);
                    }
                }

            }
        }
    }

}

