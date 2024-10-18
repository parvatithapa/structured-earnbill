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
//public class AbstractFileTransformerTest extends TestCase {
//
//    private static final String CONTENT = String.format(
//            "qqq123456%n" +
//            "1qqq23456%n" +
//            "123qqq456%n" +
//            "123456qqq%n" +
//            "qqqqqq%n");
//
//    public void testFileRename() throws Exception {
//        File inputFile = File.createTempFile("abstract_in", ".txt");
//        String fn = inputFile.getAbsolutePath();
//        System.out.println(fn);
//        File outputfile = new File(fn.substring(0, fn.lastIndexOf('.')) + ".tmp");
//        FileUtils.write(inputFile, CONTENT);
//        LineSubstringFilterFileProcessor processor = new LineSubstringFilterFileProcessor();
//        processor.setSuffix("tmp");
//        processor.setMatchingValue("qqq");
//        processor.process(inputFile);
//
//        assertFalse(inputFile.exists());
//        File doneFile = new File(inputFile.getAbsolutePath()+".done");
//        assertTrue(doneFile.exists());
//        doneFile.delete();
//        assertTrue(outputfile.exists());
//        outputfile.delete();
//    }
//
//    public void testFileDelete() throws Exception {
//        File inputFile = File.createTempFile("abstract_in", ".txt");
//        String fn = inputFile.getAbsolutePath();
//        File outputfile = new File(fn.substring(0, fn.lastIndexOf('.')) + ".tmp");
//        FileUtils.write(inputFile, CONTENT);
//        LineSubstringFilterFileProcessor processor = new LineSubstringFilterFileProcessor();
//        processor.setMatchingValue("qqq");
//        processor.setSuffix("tmp");
//        processor.setFileAction(AbstractFileTransformer.FileAction.DELETE);
//        processor.process(inputFile);
//
//        assertFalse(inputFile.exists());
//        File doneFile = new File(inputFile.getAbsolutePath()+".done");
//        assertFalse(doneFile.exists());
//        doneFile.delete();
//        assertTrue(outputfile.exists());
//        outputfile.delete();
//    }
//
//    public void testFileActionNone() throws Exception {
//        File inputFile = File.createTempFile("abstract_in", ".txt");
//        String fn = inputFile.getAbsolutePath();
//        File outputfile = new File(fn.substring(0, fn.lastIndexOf('.')) + ".tmp");
//        FileUtils.write(inputFile, CONTENT);
//        LineSubstringFilterFileProcessor processor = new LineSubstringFilterFileProcessor();
//        processor.setMatchingValue("qqq");
//        processor.setSuffix("tmp");
//        processor.setFileAction(AbstractFileTransformer.FileAction.NONE);
//        processor.transform(inputFile, outputfile);
//
//        assertTrue(inputFile.exists());
//        File doneFile = new File(inputFile.getAbsolutePath()+".done");
//        assertFalse(doneFile.exists());
//        doneFile.delete();
//        assertTrue(outputfile.exists());
//        outputfile.delete();
//    }
//}
