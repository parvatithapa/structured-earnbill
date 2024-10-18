//TODO MODULARIZATION: MOVE THIS WHERE THE LineSubstringFilterFileProcessor WILL BE IMPORTED
//package com.sapienter.jbilling.server.mediation.batch;
//
//import junit.framework.TestCase;
//import org.apache.commons.io.FileUtils;
//
//import java.io.File;
//
///**
// * @author Gerhard Maree
// * @since 24-02-2014
// */
//public class LineSubstringFilterFileProcessorTest  extends TestCase {
//
//    private static final String INPUT_FILENAME = "test/unit/com/sapienter/jbilling/server/mediation/batch/substring_filter_input1.txt";
//    private static final String EXPECTED = String.format(
//            "qqq123456%n" +
//            "1qqq23456%n" +
//            "123qqq456%n" +
//            "123456qqq%n" +
//            "qqqqqq%n");
//
//    public void testFilter() throws Exception {
//        File outputfile = File.createTempFile("substring_filter", "txt");
//        LineSubstringFilterFileProcessor processor = new LineSubstringFilterFileProcessor();
//        processor.setMatchingValue("qqq");
//        processor.transform(new File(INPUT_FILENAME), outputfile);
//        String content = FileUtils.readFileToString(outputfile);
//        assertEquals(EXPECTED, content);
//    }
//}
