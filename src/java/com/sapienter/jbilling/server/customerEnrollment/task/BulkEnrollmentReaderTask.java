package com.sapienter.jbilling.server.customerEnrollment.task;

import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.csv.*;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentBL;
import com.sapienter.jbilling.server.customerEnrollment.event.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BulkEnrollmentReaderTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BulkEnrollmentReaderTask.class));
    private static final String PROCESSED_SUB_FOLDER_NAME = "processed";
    private static final String FAILED_SUB_FOLDER_NAME = "failed";

    private final CustomerEnrollmentCSVStrategy csvStrategy = new CustomerEnrollmentCSVStrategy();
    private final CustomerEnrollmentEntryParser entryParser = new CustomerEnrollmentEntryParser();
    private final CompanyDAS companyDAS = new CompanyDAS();
    private final CustomerEnrollmentBL customerEnrollmentBL = new CustomerEnrollmentBL();
    private IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

    public BulkEnrollmentReaderTask() {
        setUseTransaction(true);
    }

    @Override
    public String getTaskName() {
        return "Bulk enrollment reader task, entity Id: " + this.getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        _init(context);

        // Iterates over all CSV files under the inbound folder
        for (File csvFile : this.getCsvFiles()) {

            boolean successful = false;

            List<CustomerEnrollmentWS> customerEnrollments = null;
            try {
                customerEnrollments = this.readCustomerEnrollmentsFile(csvFile);
            }
            catch (CustomerEnrollmentEntryParserException e) {
                // Reply with V1 response if failed
                EventManager.process(new IncompleteInvalidDataEnrollmentEvent(this.getEntityId(), e.getCustomerEnrollment(), e.getMessage()));
            }

            if (customerEnrollments != null) {
                // Generates a CustomerEnrollmentWS for each CSV record
                for (CustomerEnrollmentWS customerEnrollment : customerEnrollments) {
                    customerEnrollment.setBulkEnrollment(true);

                    CompanyDTO company = companyDAS.findEntityByName(customerEnrollment.getCompanyName());
                    if (!customerEnrollment.getBrokerCatalogVersion().equals(company.getBrokerCatalogVersion())) {
                        // Reply with V1 response if failed
                        EventManager.process(new IncompleteInvalidDataEnrollmentEvent(this.getEntityId(), customerEnrollment, "Catalog version inconsistency."));
                        continue;
                    }

                    try {
                        EventManager.process(new ValidateEnrollmentEvent(this.getEntityId(), customerEnrollmentBL.getDTO(customerEnrollment)));
                    }
                    catch (SessionInternalError e) {
                        // Reply with V2 response if failed
                        EventManager.process(new BusinessRuleViolationEnrollmentEvent(this.getEntityId(), customerEnrollment, "Customer already enrolled."));
                        continue;
                    }

                    if (StringUtils.isBlank(customerEnrollment.getAccountNumber())) {
                        // Reply with V2 response if failed
                        EventManager.process(new MissingAccountNumberEnrollmentEvent(this.getEntityId(), customerEnrollment));
                        continue;
                    }

                    try {
                        customerEnrollment = this.validateCustomerEnrollment(customerEnrollment);
                    }
                    catch (SessionInternalError e) {
                        // Reply with V1 response if failed
                        EventManager.process(new IncompleteInvalidDataEnrollmentEvent(this.getEntityId(), customerEnrollment, StringUtils.join(e.getErrorMessages(), ',')));
                        continue;
                    }

                    try {
                        Integer customerEnrollmentId = webServicesSessionBean.createUpdateEnrollment(customerEnrollment);

                        customerEnrollment.setId(customerEnrollmentId);

                        successful = true;

                        // Reply with PG response if OK
                        EventManager.process(new PendingLDCResponseEnrollmentEvent(getEntityId(), customerEnrollment, customerEnrollmentId.toString()));
                    }
                    catch (SessionInternalError e) {
                        // Reply with V2 response if failed
                        EventManager.process(new BusinessRuleViolationEnrollmentEvent(this.getEntityId(), customerEnrollment, StringUtils.join(e.getErrorMessages(), ',')));
                    }
                }
            }

            this.moveFileToFolder(csvFile, successful ? PROCESSED_SUB_FOLDER_NAME : FAILED_SUB_FOLDER_NAME);
        }
    }

    private List<File> getCsvFiles() {
        File inboundFolder = Paths.get(Util.getSysProp("base_dir"), FileConstants.CUSTOMER_ENROLLMENT_FOLDER, this.getEntityId().toString(), FileConstants.INBOUND_PATH).toFile();

        if (inboundFolder.exists() && inboundFolder.isDirectory()) {
            List<File> csvFiles = new ArrayList<>();
            for (File file : inboundFolder.listFiles()) {
                if (file.isFile()) {
                    csvFiles.add(file);
                }
            }

            return csvFiles;
        }

        return Collections.emptyList();
    }

    private List<CustomerEnrollmentWS> readCustomerEnrollmentsFile(File file) throws CustomerEnrollmentEntryParserException {
        List<CustomerEnrollmentWS> customerEnrollments = new ArrayList<>();
        CustomerEnrollmentWS customerEnrollment;
        int line = 1;

        try {
            CSVReader<CustomerEnrollmentWS> csvReader = new CSVReaderBuilder<CustomerEnrollmentWS>(new FileReader(file)).strategy(csvStrategy).entryParser(entryParser).build();

            while ((customerEnrollment = csvReader.readNext()) != null) {
                customerEnrollments.add(customerEnrollment);
                line++;
            }

            return customerEnrollments;
        }
        catch (IOException e) {
            LOG.debug(String.format("Error reading CSV file (file: %s)", file.getPath()), e);
            return Collections.emptyList();
        }
        catch (CustomerEnrollmentEntryParserException e) {
            e.setLine(line);
            LOG.debug(e.getMessage(), e);
            throw e;
        }
    }

    private CustomerEnrollmentWS validateCustomerEnrollment(CustomerEnrollmentWS customerEnrollment) {
        String zipCode = (String) customerEnrollment.getMetaFieldValue("ZIP_CODE");

        if (zipCode.isEmpty()) {
            String commodity = (String) customerEnrollment.getMetaFieldValue(FileConstants.COMMODITY);
            throw new SessionInternalError("Validation error.", new String[] {String.format("ZIP Code is required (LDC: %s, Commodity: %s, Account Number: %s).",
                    customerEnrollment.getCompanyName(), !commodity.isEmpty() ? commodity.charAt(0) : "", customerEnrollment.getAccountNumber())});
        }

        customerEnrollment = webServicesSessionBean.validateCustomerEnrollment(customerEnrollment);

        String generatedZipCode = (String) customerEnrollment.getMetaFieldValue("ZIP_CODE");

        if (!zipCode.equals(generatedZipCode)) {
            String commodity = (String) customerEnrollment.getMetaFieldValue(FileConstants.COMMODITY);
            throw new SessionInternalError("Validation error.", new String[] {String.format("ZIP Code is not valid for the provided address, it should be %s (LDC: %s, Commodity: %s, Account Number: %s).",
                    generatedZipCode, customerEnrollment.getCompanyName(), !commodity.isEmpty() ? commodity.charAt(0) : "", customerEnrollment.getAccountNumber())});
        }

        customerEnrollment.setStatus(CustomerEnrollmentStatus.VALIDATED);

        return customerEnrollment;
    }

    private void moveFileToFolder(File csvFile, String subFolderName) {
        try {
            Path targetFolderPath = Paths.get(csvFile.getParent(), subFolderName);
            if (!Files.exists(targetFolderPath)) {
                Files.createDirectories(targetFolderPath);
            }

            Files.move(csvFile.toPath(), targetFolderPath.resolve(csvFile.getName()), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            LOG.debug(e.getMessage());
        }
    }
}