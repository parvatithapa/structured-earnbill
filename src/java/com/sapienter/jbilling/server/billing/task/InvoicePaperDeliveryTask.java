package com.sapienter.jbilling.server.billing.task;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import javax.mail.MessagingException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.grails.datastore.mapping.query.Query.In;
import org.hibernate.SessionFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

/**
 * @author Harhsad Pathan
 * @since 06-09-2019 This schedule task is for generating batch of invoice pdf
 *        for billing process.
 */
public class InvoicePaperDeliveryTask extends AbstractCronTask {

    private static final Logger logger        = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BASE_DIR      = "base_dir";
    private static final String INVOICES      = "invoices";
    private static final String HYPHEN        = "-";
    private static final String INVOICES_DIR  = Util.getSysProp(BASE_DIR) + INVOICES ;
    private static final String SOURCE_FOLDER = INVOICES_DIR + File.separator;
    private static final String COMPRESSED_FORMAT = ".tar.gz";
    private static final Integer BATCH_SIZE   = 1000;
    private SimpleDateFormat format           = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat dateFormat       = new SimpleDateFormat("yyyy-MM-dd");

    private static final ParameterDescription SFTP_HOST        = new ParameterDescription("sftp host", true, ParameterDescription.Type.STR);
    private static final ParameterDescription SFTP_PORT        = new ParameterDescription("sftp port", true, ParameterDescription.Type.STR);
    private static final ParameterDescription SFTP_USER        = new ParameterDescription("sftp username", true, ParameterDescription.Type.STR);
    private static final ParameterDescription SFTP_PASS        = new ParameterDescription("sftp password", true, ParameterDescription.Type.STR, true);
    private static final ParameterDescription SFTP_WORKING_DIR = new ParameterDescription("sftp working directory", true, ParameterDescription.Type.STR);
    private static final ParameterDescription SPC_EMAIL            = new ParameterDescription("spc billing admin email", true, ParameterDescription.Type.STR);
    private static final ParameterDescription AGL_EMAIL            = new ParameterDescription("agl billing admin email", true, ParameterDescription.Type.STR);
    private static final String INDEX_FILE_HEADER              = "File Name,User Id,CRM Account Number,Invoice Id,Invoice Number,Address,Start Impression,End Impression"+ "\n";
    private static final int THREAD_POOL_SIZE                  = 5;
    private static final ParameterDescription USE_COMPRESSION_FOR_PDF_FORMAT = new ParameterDescription("use compression for pdf format", true, ParameterDescription.Type.BOOLEAN);

    private String outPutFile        = StringUtils.EMPTY;
    private static Date billingDate  = null;
    private ExecutorService service;

    public InvoicePaperDeliveryTask() {
        super.setUseTransaction(true);
        descriptions.add(SFTP_HOST);
        descriptions.add(SFTP_PORT);
        descriptions.add(SFTP_USER);
        descriptions.add(SFTP_PASS);
        descriptions.add(SFTP_WORKING_DIR);
        descriptions.add(SPC_EMAIL);
        descriptions.add(AGL_EMAIL);
        descriptions.add(USE_COMPRESSION_FOR_PDF_FORMAT);
        service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        // Populate plugin parameters
        _init(context);

        IBillingProcessSessionBean  billing  = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
        Integer               lastBillingId  = billing.getLast(getEntityId());
        BillingProcessBL   billingProcessBL  = new BillingProcessBL(lastBillingId);
        Integer invoicesGeneratedInLastBill  = billing.getInvoiceCountByBillingProcessId(lastBillingId);
        BigInteger invoicesGeneratedInLastBillSPC  = billingProcessBL.getSPCInvoiceCountForBillRun(lastBillingId, Boolean.FALSE);
        BigInteger invoicesGeneratedInLastBillAGL  = billingProcessBL.getSPCInvoiceCountForBillRun(lastBillingId, Boolean.TRUE);
        billingDate                          = null != billingProcessBL ? billingProcessBL.getEntity().getBillingDate() : null;
        String[] paramsForEmptyExract        = new String[3];
        paramsForEmptyExract[0]              = lastBillingId.toString();
        paramsForEmptyExract[1]              = getEntityId().toString();
        paramsForEmptyExract[2]              = dateFormat.format(billingDate);

        List<Integer>           invoicesPaperAGL  = billing.findAllInvoiceIdsForByProcessIdAndInvoiceDesign(lastBillingId,SPCConstants.AGL_INVOICE);

        if (CollectionUtils.isNotEmpty(invoicesPaperAGL)) {
            logger.info("invoices for AGL Paper extract  :  {} ", invoicesPaperAGL.size());
            extractPaperDelivery(invoicesPaperAGL, invoicesGeneratedInLastBill,invoicesGeneratedInLastBillAGL.intValue(), lastBillingId, Boolean.TRUE,parameters.get(AGL_EMAIL.getName()));
        }else {
            logger.error("No invoices for AGL Paper extract");
            try {
                NotificationBL.sendSapienterEmail(parameters.get(AGL_EMAIL.getName()), getEntityId(), "invoice_batch_export_empty_agl",null, paramsForEmptyExract);
            } catch (MessagingException | IOException e) {
                logger.error("Exception from InvoicePaperDeliveryTask : Not able to send email for AGL empty batch :  {} ", e.getMessage());
            }
        }

        List<Integer> invoicesPaperSPC = billing.findAllInvoiceIdsForByProcessIdAndInvoiceDesign(lastBillingId,StringUtils.EMPTY);
        if (CollectionUtils.isNotEmpty(invoicesPaperSPC)) {
            logger.info("invoices for Paper extract  :  {} ", invoicesPaperSPC.size());
            extractPaperDelivery(invoicesPaperSPC, invoicesGeneratedInLastBill, invoicesGeneratedInLastBillSPC.intValue(), lastBillingId, Boolean.FALSE,parameters.get(SPC_EMAIL.getName()));
        }else {
            logger.error("No invoices for NON AGL Paper extract");
            try {
                NotificationBL.sendSapienterEmail(parameters.get(SPC_EMAIL.getName()), getEntityId(), "invoice_batch_export_empty",null, paramsForEmptyExract);
            } catch (MessagingException | IOException e) {
                logger.error("Exception from InvoicePaperDeliveryTask : Not able to send email for AGL empty batch :  {} ", e.getMessage());
            }
        }
    }

    private void extractPaperDelivery(List<Integer> invoices, Integer totalInvoicesForBillRun, Integer clientInvoiceCount,  Integer lastBillingId, Boolean isAGLExtract,String emailAddress){

        File destFileDir = null;
        try {
            List<IndexFileObject> fileObjects   = new ArrayList<>();
            List<IndexFileObject> failedObjects = new ArrayList<>();
            Collections.sort(invoices);
            logger.debug("invoicesForPaper :  {} ", invoices.size());
            logger.debug("Total number of invoices to be extracted in billing process {} of enitity Id {} is {}", lastBillingId, 
                   getEntityId(), invoices.size());
            StringBuilder billingDateWithCurrentTimeStamp  = new StringBuilder(format.format(billingDate) +HYPHEN + System.currentTimeMillis());
            if (isAGLExtract) {
                destFileDir = new File(SOURCE_FOLDER +File.separator +"paper-invoice-batch-export-AGL-" +lastBillingId+HYPHEN +billingDateWithCurrentTimeStamp);
            }else {
                destFileDir = new File(SOURCE_FOLDER +File.separator +"paper-invoice-batch-export-" +lastBillingId+HYPHEN +billingDateWithCurrentTimeStamp);
            }
            if(!destFileDir.exists()){
                destFileDir.mkdir();
            }

            outPutFile = destFileDir.getPath();

            String fileName = null;
            int fileCounter = 1;
            List<List<Integer>> invoiceBatches = partition(invoices, BATCH_SIZE);
            Future<List<BulkIndexFile>> future = executeBatchExportInNewThread(invoiceBatches);
            List<BulkIndexFile> files          = future.get();
            for (BulkIndexFile bulkIndexFile : files) {
                logger.info("Starting generating batch of invoice pdf for billing process {} of enitity Id {}", lastBillingId, getEntityId());
                logger.info("generated file {} is generated under {}", fileName, INVOICES_DIR);
                // rename fileName = 1001-20190921-1.pdf

                String batchFile               = INVOICES_DIR + File.separator + bulkIndexFile.getOriginalFileName();
                File renamedFile               = new File(destFileDir.getPath() + File.separator + lastBillingId.toString() + HYPHEN + format.format(billingDate) +HYPHEN+ fileCounter + ".pdf");
                ++fileCounter;
                File sourceInvoiceBatchFile    = new File(batchFile);
                sourceInvoiceBatchFile.renameTo(renamedFile);
                logger.info("sourceInvoiceBatchFile {}", sourceInvoiceBatchFile.getAbsolutePath());
                logger.info("renamedFile {}", renamedFile.getAbsolutePath());

                List<IndexFileObject> indexFileObjects = null != bulkIndexFile ? bulkIndexFile.getIndexFileObjects() : Collections.EMPTY_LIST;
                if (CollectionUtils.isNotEmpty(indexFileObjects)) {
                    for (IndexFileObject indexFile : indexFileObjects) {
                        if (null != indexFile) {
                            indexFile.setFileName(renamedFile.getName());
                            fileObjects.add(indexFile);
                        }
                    }
                    writeToCSV(fileObjects, "index_"+lastBillingId);
                }

                List<IndexFileObject> failedFileObjects = null != bulkIndexFile ? bulkIndexFile.getFailedFileObjects() : Collections.EMPTY_LIST;
                if (CollectionUtils.isNotEmpty(failedFileObjects)) {
                    for (IndexFileObject indexFile : failedFileObjects) {
                         if (null != indexFile) {
                           failedObjects.add(indexFile);
                         }
                    }
                    writeToCSV(failedObjects, "failed_"+lastBillingId);
                }
                createTarFile(destFileDir.getPath());
            }

            String finalCompressedFile = destFileDir.getPath() + COMPRESSED_FORMAT;
            logger.debug("finalCompressed File {} is generated ", finalCompressedFile);

            String[] params = new String[9];
            params[0] = parameters.get(SFTP_WORKING_DIR.getName());
            // File name
            params[1] = destFileDir.getName() + COMPRESSED_FORMAT ;
            // Billing Date
            params[2] = dateFormat.format(billingDate);
            // Bill run Id
            params[3] = lastBillingId.toString();
            // Company Id
            params[4] = getEntityId().toString();
            // Total number of Invoices generated for Bill Run
            params[5] = String.valueOf(totalInvoicesForBillRun);
            // Total number of invoices for SPC/AGL
            params[6] = String.valueOf(clientInvoiceCount);
            // Total number of paper invoices for SPC
            params[7] = String.valueOf(invoices.size());
            // Total number of records skipped
            params[8] = String.valueOf(failedObjects.size());

            boolean isFileSent = sendFileToSFTP(finalCompressedFile);
            logger.debug("finalCompressed File {} is sent to sftp ", finalCompressedFile);
            try {
                if(isFileSent && Boolean.FALSE.equals(isAGLExtract)){
                   NotificationBL.sendSapienterEmail(emailAddress, getEntityId(), "invoice_batch_export_success",null, params);
                } else if (!isFileSent && Boolean.FALSE.equals(isAGLExtract)) {
                   NotificationBL.sendSapienterEmail(emailAddress, getEntityId(), "invoice_batch_export_sftp_failed",null, params);
                }
                if(isFileSent && isAGLExtract){
                   NotificationBL.sendSapienterEmail(emailAddress, getEntityId(), "invoice_batch_export_agl_success",null, params);
                } else if (!isFileSent && isAGLExtract) {
                   NotificationBL.sendSapienterEmail(emailAddress, getEntityId(), "invoice_batch_export_failed_agl",null, params);
                }
            } catch ( MessagingException e) {
                   throw new SessionInternalError("Exception from InvoicePaperDeliveryTask : Not able to send email", e);
                }
        } catch (SessionInternalError | IOException e) {
            try {
                String[] params = new String[3];
                params[0] = lastBillingId.toString();
                params[1] = getEntityId().toString();
                params[2] = dateFormat.format(billingDate);
                NotificationBL.sendSapienterEmail(emailAddress, getEntityId(), "invoice_batch_export_failed",null, params);
            } catch (MessagingException | IOException ex) {
                logger.error("Exception from InvoicePaperDeliveryTask : Not able to send failed email ", ex);
            }
        } catch (InterruptedException | ExecutionException e1) {
           logger.error("Exception from InvoicePaperDeliveryTask : Not able to send failed email ", e1);
        }  finally {
           if (null != destFileDir && destFileDir.exists()) {
                logger.debug("deleting destFileDir {}: ", destFileDir);
                deleteOldFiles(new File(INVOICES_DIR));
           }
        }
        logger.debug("Ended generating batch of invoice pdf for billing process {} of enitity Id {}", lastBillingId, getEntityId());
    }

    private boolean sendFileToSFTP(String finalCompressedFile) {

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        File f = new File(finalCompressedFile);
        try (FileInputStream fileInputStream = new FileInputStream(f);){
            JSch jsch = new JSch();
            session = jsch.getSession(parameters.get(SFTP_USER.getName()), parameters.get(SFTP_HOST.getName()),
            Integer.parseInt(parameters.get(SFTP_PORT.getName())));
            session.setPassword(parameters.get(SFTP_PASS.getName()));
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(parameters.get(SFTP_WORKING_DIR.getName()));
            channelSftp.put(fileInputStream, f.getName());
        } catch (NumberFormatException | JSchException | SftpException | IOException ex) {
            logger.debug("Exception from InvoicePaperDeliveryTask : Exception occured while file transfer to sftp  ", ex);
            return false;
        } finally {
            if (null != channelSftp) {
                channelSftp.disconnect();
            }
            if (null != channel) {
                channel.disconnect();
            }
            if (null != session) {
                session.disconnect();    
            }
        }
        return true;
    }

    private void createTarFile(String sourceDir) {

        File source = new File(sourceDir);
        try (FileOutputStream fos = new FileOutputStream(source.getAbsolutePath().concat(COMPRESSED_FORMAT));
                GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));
                TarArchiveOutputStream tarOs = new TarArchiveOutputStream(gos);) {
            addFilesToTarGZ(sourceDir, "", tarOs);
        } catch (IOException e) {
            throw new SessionInternalError("Unable to create a tar file", e);
        }
    }

    private static void addFilesToTarGZ(String filePath, String parent, TarArchiveOutputStream tarArchive) throws IOException {
        File file = new File(filePath);

        // Create entry name relative to parent file path
        String entryName = parent + file.getName();
        // add tar ArchiveEntry
        tarArchive.putArchiveEntry(new TarArchiveEntry(file, entryName));
        if (file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);){
                // Write file content to archive
                IOUtils.copy(bis, tarArchive);
            } catch (IOException e) {
                throw new SessionInternalError("Unable to add files to tar", e);
            }
            tarArchive.closeArchiveEntry();
        } else if (file.isDirectory()) {
            // no need to copy any content since it is a directory, just close the outputstream
            tarArchive.closeArchiveEntry();
            // for files in the directories
            for (File f : file.listFiles()) {
                // recursively call the method for all the subdirectories
                addFilesToTarGZ(f.getAbsolutePath(), entryName + File.separator, tarArchive);
            }
        }
    }

    /**
     * This method is used to partition the list into batch of lists
     * @param list
     * @param batchSize
     * @return
     */
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> parts = new ArrayList<List<T>>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            parts.add(new ArrayList<T>(list.subList(i, Math.min(size, i + batchSize))));
        }
        return parts;
    }


    public String writeToCSV(List<IndexFileObject> message, String fileName) throws IOException {
        try(FileWriter writer = new FileWriter(createFile(fileName));){
            writer.write(INDEX_FILE_HEADER);
            message.forEach(messages -> {
                try {
                    writer.write(messages.toString() + "\n");
                } catch (IOException e) {
                    throw new SessionInternalError("Exception in csv creation", e);
                }
            });
        }

        return fileName;
    }

    private String createFile(String fileName){

        String filePath = outPutFile;
        fileName = new StringBuilder()
        .append(filePath)
        .append(File.separator)
        .append(fileName)
        .append("-")
        .append(format.format(billingDate)).toString();
        fileName = fileName + ".csv";
        return fileName;
    }

    /**
     * This method will clear the directory parameter
     * @param file = directory to be cleaned up
     */
    public static void deleteOldFiles(File file) {
        int days = 1;
        long diff = new Date().getTime() - file.lastModified();
        logger.debug("Now will search folders and delete files");
        if (file.isDirectory()) {
            logger.debug("Date Modified : {} ", file.lastModified());
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e) {
                        throw new SessionInternalError("Exception at directory cleanup", e);
                    }
                } else {
                    deleteOldFiles(f);
                }
            }
        } else {
            if (diff > days * 24 * 60 * 60 * 1000) {
                file.delete();
            }
        }
    }

    private Future<List<BulkIndexFile>> executeBatchExportInNewThread(final List<List<Integer>> invoiceBatches) {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        return service.submit(() -> txAction.executeInReadOnlyTx(() -> {
            List<BulkIndexFile> bulkIndexfiles = new ArrayList<>();
            BulkIndexFile bulkIndexfile = null;
            for(List<Integer> exportable: invoiceBatches) {
                try {
                    PaperInvoiceBatchBL paperInvoiceBatchBL = new PaperInvoiceBatchBL();
                    bulkIndexfile = paperInvoiceBatchBL.generateSPCBatchPdf(exportable, getEntityId(),getParameter(USE_COMPRESSION_FOR_PDF_FORMAT.getName(), Boolean.FALSE));
                    bulkIndexfiles.add(bulkIndexfile);
                    SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
                    sf.getCurrentSession().clear();
                }
                catch(Exception ex) {
                    logger.error("Record got Skipped: ", ex);
                }

            }
            return bulkIndexfiles;
        }));
    }
}