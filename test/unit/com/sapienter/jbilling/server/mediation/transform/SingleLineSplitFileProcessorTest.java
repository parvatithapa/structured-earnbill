//TODO MODULARIZATION: MOVE IN THE MODULES
//package com.sapienter.jbilling.server.mediation.transform;
//
//
//import com.sapienter.jbilling.server.mediation.batch.AbstractFileTransformer;
//import com.sapienter.jbilling.server.mediation.batch.SingleLineSplitFileProcessor;
//import junit.framework.TestCase;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.LineNumberReader;
//import java.lang.String;
//
//public class SingleLineSplitFileProcessorTest extends TestCase {
//
//    private static final String GOOD_INPUT_FILENAME = "test/unit/com/sapienter/jbilling/server/mediation/transform/good_example.txt";
//    private static final String BAD_INPUT_FILENAME = "test/unit/com/sapienter/jbilling/server/mediation/transform/bad_example.txt";
//    private static final String OUTPUT_FILENAME_EXTENSION = "cdr";
//
//    public void testGoodFileProcessing() throws IOException {
//
//        File inputFile = new File(GOOD_INPUT_FILENAME);
//
//        if(inputFile.exists() && inputFile.isFile()) {
//
//            String inputFileName = inputFile.getAbsolutePath();
//            File outputFile= new File(inputFileName.substring(0, inputFileName.lastIndexOf('.')) + "." + OUTPUT_FILENAME_EXTENSION);
//
//            SingleLineSplitFileProcessor splitter = new SingleLineSplitFileProcessor(160, OUTPUT_FILENAME_EXTENSION);
//            splitter.setFileAction(AbstractFileTransformer.FileAction.NONE);
//            splitter.process(inputFile);
//
//            assertEquals("There should be 15 CDRs/Lines in the file", 15, countLines(outputFile));
//            assertTrue("Problem deleting the output file", outputFile.delete());
//        } else {
//            fail("File does not exist: " + GOOD_INPUT_FILENAME);
//        }
//    }
//
//    public void testBadFileProcessing() throws IOException {
//        File inputFile = new File(BAD_INPUT_FILENAME);
//
//        if(inputFile.exists() && inputFile.isFile()) {
//
//            String inputFileName = inputFile.getAbsolutePath();
//            File outputFile= new File(inputFileName.substring(0, inputFileName.lastIndexOf('.')) + "." + OUTPUT_FILENAME_EXTENSION);
//
//            SingleLineSplitFileProcessor splitter = new SingleLineSplitFileProcessor(160, OUTPUT_FILENAME_EXTENSION);
//            splitter.process(inputFile);
//
//            assertTrue("Input file should be there", inputFile.exists());
//            assertFalse("Problem deleting the output file", outputFile.exists());
//        } else {
//            fail("File does not exist: " + BAD_INPUT_FILENAME);
//        }
//    }
//
//    private int countLines(File file) throws IOException {
//        LineNumberReader  lnr = new LineNumberReader(new FileReader(file));
//        lnr.skip(Long.MAX_VALUE);
//        int count = lnr.getLineNumber()+1;
//        lnr.close();
//        return count;
//    }
//}
