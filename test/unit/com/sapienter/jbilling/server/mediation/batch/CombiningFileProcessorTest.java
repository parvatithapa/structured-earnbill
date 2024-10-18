//TODO MODULARIZATION: MOVE THIS WHERE THE COMBININGFILEPROCESSORWILL BE
//package com.sapienter.jbilling.server.mediation.batch;
//
//import com.sapienter.jbilling.common.Util;
//import junit.framework.TestCase;
//import org.apache.commons.io.FileUtils;
//
//import java.io.File;
//
///**
// * @author Gerhard Maree
// * @since 24-02-2014
// */
//public class CombiningFileProcessorTest extends TestCase {
//
//    private static final String FOLDER = "test/unit/com/sapienter/jbilling/server/mediation/batch/";
//    private static final String INPUT_FILENAME = FOLDER + "combining_test.txt";
//    private static final File fileOut1 = new File(FOLDER+"combining1.out");
//    private static final File fileOut2 = new File(FOLDER+"combining2.out");
//
//    public void testCombine() throws Exception {
//        if(fileOut1.exists()) fileOut1.delete();
//        if(fileOut2.exists()) fileOut2.delete();
//        CombiningFileProcessor processor = new CombiningFileProcessor();
//        processor.setMaxSize(1024*2);
//        processor.setPrefix("combining");
//        processor.setDeleteInputFile(false);
//        processor.setSuffix("out");
//        processor.process(new File(INPUT_FILENAME));
//        processor.process(new File(INPUT_FILENAME));
//        processor.process(new File(INPUT_FILENAME));
//        processor.process(new File(INPUT_FILENAME));
//        String input = FileUtils.readFileToString(new File(INPUT_FILENAME));
//        String content = FileUtils.readFileToString(fileOut1);
//        assertEquals((input+ Util.LINE_SEPARATOR+input+ Util.LINE_SEPARATOR+input).trim(), content.trim());
//        content = FileUtils.readFileToString(fileOut2);
//        assertEquals(input.trim(), content.trim());
//        fileOut1.delete();
//        fileOut2.delete();
//    }
//}
