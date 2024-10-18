/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.plugin;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.nges.export.row.*;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hitesh on 3/8/16.
 */
public class NGESExportTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportTask.class));

    private static final String PARAM_EXPORT_CUSTOMER_FILE = "Export Customer File";
    private static final String PARAM_EXPORT_INVOICE_FILE = "Export Invoice File";
    private static final String PARAM_EXPORT_PAYMENT_FILE = "Export Payment File";
    private static final String PARAM_EXPORT_PRODUCT_FILE = "Export Product File";
    private static final String PARAM_EXPORT_ENROLLMENT_FILE = "Export Enrollment File";

    private static final String JOB_CUSTOMER_EXPORT = "customerExportJob";
    private static final String JOB_INVOICE_EXPORT = "invoiceExportJob";
    private static final String JOB_PAYMENT_EXPORT = "paymentExportJob";
    private static final String JOB_PRODUCT_EXPORT = "productExportJob";
    private static final String JOB_ENROLLMENT_EXPORT = "enrollmentExportJob";

    private static final String JOB_ENTITY_CUSTOMER = "Customer";
    private static final String JOB_ENTITY_INVOICE = "Invoice";
    private static final String JOB_ENTITY_PAYMENT = "Payment";
    private static final String JOB_ENTITY_PRODUCT = "Product";
    private static final String JOB_ENTITY_ENROLLMENT = "Enrollment";

    private static final String DATE_FORMATE = "MM-dd-yyyy hh.mm.ss";
    private static final String JOB_PARAM_FILE_NAME = "fileName";
    private static final String JOB_PARAM_ERROR_FILE_NAME = "errorFileName";
    private static final String JOB_PARAM_DATE = "date";
    private static final String FILE_SUFFIX = ".csv";
    private static final String ERROR_FILE_SUFFIX = "-Error.csv";
    private static final String JOB_PARAM_COMPANY_NAME = "companyName";

    private Map jobParams;
    private static final Map<String, String> map = new HashMap<String, String>() {{
        put(JOB_CUSTOMER_EXPORT, PARAM_EXPORT_CUSTOMER_FILE);
        put(JOB_INVOICE_EXPORT, PARAM_EXPORT_INVOICE_FILE);
        put(JOB_PAYMENT_EXPORT, PARAM_EXPORT_PAYMENT_FILE);
        put(JOB_PRODUCT_EXPORT, PARAM_EXPORT_PRODUCT_FILE);
        put(JOB_ENROLLMENT_EXPORT, PARAM_EXPORT_ENROLLMENT_FILE);
    }};

    protected static final ParameterDescription EXPORT_CUSTOMER_FILE_DESC =
            new ParameterDescription(PARAM_EXPORT_CUSTOMER_FILE, false, ParameterDescription.Type.BOOLEAN);
    protected static final ParameterDescription EXPORT_INVOICE_FILE_DESC =
            new ParameterDescription(PARAM_EXPORT_INVOICE_FILE, false, ParameterDescription.Type.BOOLEAN);
    protected static final ParameterDescription EXPORT_PAYMENT_FILE_DESC =
            new ParameterDescription(PARAM_EXPORT_PAYMENT_FILE, false, ParameterDescription.Type.BOOLEAN);
    protected static final ParameterDescription EXPORT_PRODUCT_FILE_DESC =
            new ParameterDescription(PARAM_EXPORT_PRODUCT_FILE, false, ParameterDescription.Type.BOOLEAN);
    protected static final ParameterDescription EXPORT_ENROLLMENT_FILE_DESC =
            new ParameterDescription(PARAM_EXPORT_ENROLLMENT_FILE, false, ParameterDescription.Type.BOOLEAN);

    {
        descriptions.add(EXPORT_CUSTOMER_FILE_DESC);
        descriptions.add(EXPORT_INVOICE_FILE_DESC);
        descriptions.add(EXPORT_PAYMENT_FILE_DESC);
        descriptions.add(EXPORT_PRODUCT_FILE_DESC);
        descriptions.add(EXPORT_ENROLLMENT_FILE_DESC);
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Try to execute cron job for NGES export task");
        super._init(context);

        LOG.debug("prepare a job launcher");
        JobLauncher jobLauncher = Context.getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);

        try {
            for (Job job : getJobs()) {
                LOG.debug("set the job parameter for " + job.getName());
                setJobParameter(job.getName());
                LOG.debug("start the execution of " + job.getName());
                JobExecution execution = jobLauncher.run(job, new JobParameters(jobParams));
                LOG.debug("Exit Status : " + execution.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e);
        }
    }

    /**
     * This method used for check the file is export or not based on parameter.
     *
     * @param parameterName This is plugin parameter name
     * @return boolean
     */
    private boolean isExportable(String parameterName) {
        return Boolean.parseBoolean(getParameterValueFor(parameterName));
    }

    /**
     * This method used for find the plugin parameter value.
     *
     * @param parameterName This is plugin parameter name
     * @return String
     */
    private String getParameterValueFor(String parameterName) {
        String parameter = getParameter(parameterName, "");
        LOG.debug("checking the we have plugin parameter or not");
        if (parameter.trim().isEmpty()) {
            return Boolean.TRUE.toString();
        }
        return parameter;
    }

    /**
     * This method used for getting the Job's object which is exportable.
     *
     * @return List of job
     */
    private List<Job> getJobs() {
        List<Job> jobs = new ArrayList<>(5);
        for (String jobName : map.keySet()) {
            if (isExportable(map.get(jobName))) {
                jobs.add(Context.getBean(jobName));
            }
        }
        LOG.debug("Job list for execution:" + jobs);
        return jobs;
    }

    /**
     * This method used for set the job parameter..
     *
     * @param jobName This is job name.
     * @return Nothing.
     */
    private void setJobParameter(String jobName) {
        jobParams = new HashMap();
        LOG.debug("set common parameter for job");
        jobParams.put(JOB_PARAM_DATE, new JobParameter(new Date()));
        jobParams.put(Constants.BATCH_JOB_PARAM_ENTITY_ID, new JobParameter((long) getEntityId()));

        switch (jobName) {
            case JOB_CUSTOMER_EXPORT:
                jobParams.put(JOB_PARAM_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_CUSTOMER, FILE_SUFFIX, ExportCustomerRow.getHeader())));
                jobParams.put(JOB_PARAM_ERROR_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_CUSTOMER, ERROR_FILE_SUFFIX, ExportCustomerRow.getErrorFileHeader())));
                break;
            case JOB_INVOICE_EXPORT:
                jobParams.put(JOB_PARAM_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_INVOICE, FILE_SUFFIX, ExportInvoiceRow.getHeader())));
                jobParams.put(JOB_PARAM_ERROR_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_INVOICE, ERROR_FILE_SUFFIX, ExportInvoiceRow.getErrorFileHeader())));
                break;
            case JOB_PAYMENT_EXPORT:
                jobParams.put(JOB_PARAM_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_PAYMENT, FILE_SUFFIX, ExportPaymentRow.getHeader())));
                jobParams.put(JOB_PARAM_ERROR_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_PAYMENT, ERROR_FILE_SUFFIX, ExportPaymentRow.getErrorFileHeader())));
                break;
            case JOB_PRODUCT_EXPORT:
                jobParams.put(JOB_PARAM_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_PRODUCT, FILE_SUFFIX, ExportProductRow.getHeader())));
                jobParams.put(JOB_PARAM_ERROR_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_PRODUCT, ERROR_FILE_SUFFIX, ExportProductRow.getErrorFileHeader())));
                jobParams.put(JOB_PARAM_COMPANY_NAME, new JobParameter(new CompanyDAS().findCompanyNameByEntityId(getEntityId())));
                break;
            case JOB_ENROLLMENT_EXPORT:
                jobParams.put(JOB_PARAM_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_ENROLLMENT, FILE_SUFFIX, ExportEnrollmentRow.getHeader())));
                jobParams.put(JOB_PARAM_ERROR_FILE_NAME, new JobParameter(getFileName(JOB_ENTITY_ENROLLMENT, ERROR_FILE_SUFFIX, ExportEnrollmentRow.getErrorFileHeader())));
                break;
        }
    }

    /**
     * This method used for creating a new file in a system if you provide header.
     * otherwise return the file name which is making by entity and suffix.
     *
     * @param entity job entity name.
     * @param suffix file suffix.
     * @param header file header.
     * @return string file name.
     */
    private String getFileName(String entity, String suffix, String header) {
        IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        String fileName = ediTransactionBean.getEDICommunicationPath(getEntityId(), TransactionType.EXPORT) + File.separator + getEntityId() + "-" + entity + "-" + getCurrentDate().concat(suffix);
        LOG.debug("file name:" + fileName);
        if (header == null) return fileName;
        return createFile(fileName, header);
    }

    /**
     * This method used for creating a new file in a system.
     * If you provide the header then also write the header in the newly created file.
     *
     * @param name   job entity name.
     * @param header file header.
     * @return string file name.
     */
    private String createFile(String name, String header) {
        File file = new File(name);
        try {
            if (file.createNewFile()) {
                LOG.debug("File is created!");
                if (header != null) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
                    bw.write(header);
                    bw.newLine();
                    bw.close();
                }
            }
        } catch (IOException e) {
            LOG.debug("Exception occurs while working on file::" + file.getAbsolutePath());
            throw new SessionInternalError("Exception occurs while working on file::" + file.getAbsolutePath());
        }
        return file.getAbsolutePath();
    }

    /**
     * This method used for getting the current date based on MM-dd-yyyy hh.mm.ss format.
     *
     * @return string current date.
     */
    private String getCurrentDate() {
        return (new SimpleDateFormat(DATE_FORMATE).format(new Date())).toString();
    }

    @Override
    public String getTaskName() {
        return "Export file, entity Id: " + getEntityId() + ", task Id:" + getTaskId();
    }

}
