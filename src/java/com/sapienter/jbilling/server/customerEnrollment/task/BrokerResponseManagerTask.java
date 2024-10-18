package com.sapienter.jbilling.server.customerEnrollment.task;

import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentAgentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentCSVColumnJoiner;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentCSVStrategy;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentResponse;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentResponseEntryConverter;
import com.sapienter.jbilling.server.customerEnrollment.event.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrokerResponseManagerTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BrokerResponseManagerTask.class));

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        PendingLDCResponseEnrollmentEvent.class,
        AcceptedByLDCEnrollmentEvent.class,
        RejectedByLDCEnrollmentEvent.class,
        MissingAccountNumberEnrollmentEvent.class,
        BusinessRuleViolationEnrollmentEvent.class,
        IncompleteInvalidDataEnrollmentEvent.class
    };

    private final CustomerEnrollmentCSVStrategy csvStrategy = new CustomerEnrollmentCSVStrategy();
    private final CustomerEnrollmentResponseEntryConverter csvEntryConverter = new CustomerEnrollmentResponseEntryConverter();
    private final CustomerEnrollmentCSVColumnJoiner customerEnrollmentCSVColumnJoiner = new CustomerEnrollmentCSVColumnJoiner();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private final String header = "\"LDC\",\"Account Number\",\"Response Code\",\"Reason\",\"Timestamp\",\"Broker Id\"";

    private final List<String> e1DisposalOptions = Arrays.asList(
            "0001", "0002", "0014", "0026", "0027", "0028", "0031", "0032", "0033", "0034", "0035", "0039", "0043",
            "A03", "A76", "A77", "A79", "A91", "CW2", "CW5", "DIV", "EAS", "ESGMDM", "FRB", "FRC", "FRF", "FRG", "IBO",
            "MAR", "MVE", "CPI", "RNE", "RRC", "RRPM", "ZIP");

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof EnrollmentEvent && ((EnrollmentEvent) event).getCustomerEnrollment().isBulkEnrollment()) {

            EnrollmentEvent enrollmentEvent = ((EnrollmentEvent) event);

            CustomerEnrollmentResponse customerEnrollmentResponse = null;
            // Response code PG
            if (event instanceof PendingLDCResponseEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, CustomerEnrollmentResponse.Code.PG);
            }
            // Response code SE
            else if (event instanceof AcceptedByLDCEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, CustomerEnrollmentResponse.Code.SE);
            }
            // Response code L1/L2
            else if (event instanceof RejectedByLDCEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, e1DisposalOptions.contains(enrollmentEvent.getReason()) ? CustomerEnrollmentResponse.Code.L1 : CustomerEnrollmentResponse.Code.L2);
            }
            // Response code V0
            else if (event instanceof MissingAccountNumberEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, CustomerEnrollmentResponse.Code.V0);
            }
            // Response code V1
            else if (event instanceof IncompleteInvalidDataEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, CustomerEnrollmentResponse.Code.V1);
            }
            // Response code V2
            else if (event instanceof BusinessRuleViolationEnrollmentEvent) {
                customerEnrollmentResponse = this.generateCustomerEnrollmentResponse(enrollmentEvent, CustomerEnrollmentResponse.Code.V2);
            }

            this.generateResponseFile(enrollmentEvent.getEntityId(), enrollmentEvent.getCustomerEnrollment(), customerEnrollmentResponse);
        }
    }

    private CustomerEnrollmentResponse generateCustomerEnrollmentResponse(EnrollmentEvent enrollmentEvent, CustomerEnrollmentResponse.Code responseCode) {
        CustomerEnrollmentResponse customerEnrollmentResponse = new CustomerEnrollmentResponse();

        List<String> brokerIdList = new ArrayList<>();
        if(enrollmentEvent.getCustomerEnrollment().getCustomerEnrollmentAgents() != null) {
            for(CustomerEnrollmentAgentWS agentWS : enrollmentEvent.getCustomerEnrollment().getCustomerEnrollmentAgents()) {
                brokerIdList.add(agentWS.getBrokerId());
            }
        }

        customerEnrollmentResponse.setBrokerIds(brokerIdList);
        customerEnrollmentResponse.setLdc(enrollmentEvent.getCustomerEnrollment().getCompanyName());
        customerEnrollmentResponse.setAccountNumber(enrollmentEvent.getCustomerEnrollment().getAccountNumber());
        customerEnrollmentResponse.setCode(responseCode);
        customerEnrollmentResponse.setReason(enrollmentEvent.getReason());
        customerEnrollmentResponse.setTimestamp(TimezoneHelper.serverCurrentDate());

        return customerEnrollmentResponse;
    }

    private boolean generateResponseFile(Integer entityId, CustomerEnrollmentWS customerEnrollmentWS, CustomerEnrollmentResponse customerEnrollmentResponse) {
        try {
            //create files for each broker
            if(customerEnrollmentWS.getCustomerEnrollmentAgents() != null) {
                for(CustomerEnrollmentAgentWS agentWS : customerEnrollmentWS.getCustomerEnrollmentAgents()) {
                    File file = Paths.get(this.getOutboundFolderPath(entityId), agentWS.getBrokerId() + "-" + dateFormat.format(customerEnrollmentResponse.getTimestamp()) + ".csv").toFile();

                    boolean fileExists = file.exists();
                    Writer writer = new FileWriter(file, true);

                    if (!fileExists) {
                        writer.append(header).append(System.lineSeparator());
                    }

                    CSVWriter<CustomerEnrollmentResponse> csvWriter = new CSVWriterBuilder<CustomerEnrollmentResponse>(writer).strategy(csvStrategy).entryConverter(csvEntryConverter).columnJoiner(customerEnrollmentCSVColumnJoiner).build();
                    csvWriter.write(customerEnrollmentResponse);
                    csvWriter.close();
                }
            }

            //create a file for all
            File file = Paths.get(this.getOutboundFolderPath(entityId), "all-" + dateFormat.format(customerEnrollmentResponse.getTimestamp()) + ".csv").toFile();

            boolean fileExists = file.exists();
            Writer writer = new FileWriter(file, true);

            if (!fileExists) {
                writer.append(header).append(System.lineSeparator());
            }

            CSVWriter<CustomerEnrollmentResponse> csvWriter = new CSVWriterBuilder<CustomerEnrollmentResponse>(writer).strategy(csvStrategy).entryConverter(csvEntryConverter).columnJoiner(customerEnrollmentCSVColumnJoiner).build();
            csvWriter.write(customerEnrollmentResponse);
            csvWriter.close();

            return true;
        }
        catch (IOException e) {
            LOG.debug(e.getMessage());
            return false;
        }
    }

    private String getOutboundFolderPath(Integer entityId) throws IOException {
        Path outboundFolderPath = Paths.get(Util.getSysProp("base_dir"), FileConstants.CUSTOMER_ENROLLMENT_FOLDER, entityId.toString(), FileConstants.OUTBOUND_PATH);
        if (!Files.exists(outboundFolderPath)) {
            Files.createDirectories(outboundFolderPath);
        }
        return outboundFolderPath.toString();
    }
}