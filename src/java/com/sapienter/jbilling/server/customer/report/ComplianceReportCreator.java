package com.sapienter.jbilling.server.customer.report;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * @author Javier Rivero
 * @since 28/12/15.
 */
public class ComplianceReportCreator {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ComplianceReportCreator.class));
    private static final String TERMINATED = "Dropped";
    private static final String EXPIRED = "Expired";
    private CustomerEnrollmentDAS customerEnrollmentDAS = new CustomerEnrollmentDAS();
    Date date= new java.util.Date();
    Timestamp timestamp = new Timestamp(date.getTime());

    //HDR
    private final String headerRecordType = "HDR";
    private final String reportType = "MTERCOT2TDSPCustomerInformation";
    private final String reportId = String.valueOf(timestamp).trim();
    private String TDSPDUNSNumber = "";

    //SUM
    private final String sumRecordType = "SUM";
    private Integer totalNumberOfDETRecords = 0;
    private final String totalNumberOfIDTRecords = "0";
    private final String totalNumberOfNDTRecords = "0";
    private String fileHeader = "";
    private String CRDUNSNumber = "";

    public File createComplianceReport(Integer entityId) {
        List<UserDTO> userList = customerEnrollmentDAS.findEnrolledCustomers(entityId);
        List<ComplianceReportDET> complianceReportDETs = new ArrayList<>(userList.size());
        Integer recordNumber = 1;
        for (UserDTO userDTO : userList) {
            ComplianceReportDET detEntry = createDETEntry(userDTO, recordNumber);
            if (userDTO.getStatus().getDescription().equals(EXPIRED)) {
                //do nothing if the user is expired
            } else if (detEntry.getTermination() != null && detEntry.getTermination().equals(TERMINATED)){
                //do nothing if the user is terminated
            } else if (userDTO.getDeleted() == 1) {
                //do nothing if the user is deleted
            } else {
                complianceReportDETs.add(detEntry);
                recordNumber++;
            }

        }

        totalNumberOfDETRecords = complianceReportDETs.size();

        return createReport(complianceReportDETs);
    }

    private ComplianceReportDET createDETEntry(UserDTO userDTO, Integer recordNumber){
        UserBL userBL = new UserBL();
        CompanyDTO company = userDTO.getCompany();
        List<MetaFieldValue> companyMetaFields = company.getMetaFields();
        for (MetaFieldValue metaFieldValue : companyMetaFields) {
            if (metaFieldValue.getField().getName().equals("UTILITY_DUNS")) {
                TDSPDUNSNumber = (String) metaFieldValue.getValue();
            }
        }
        ComplianceReportDET complianceReportDET = new ComplianceReportDET();
        // get all meta fileds, standard + ait timeline
        List<MetaFieldValue> values =  new ArrayList<>();
        String address = "";
        String city = "";
        String state = "";
        String zipCode = "";
        complianceReportDET.setRecordNumber(recordNumber);
        if (userDTO != null && userDTO.getCustomer() != null) {
            values.addAll(companyMetaFields);
            values.addAll(userDTO.getCustomer().getMetaFields());
            userBL.getCustomerEffectiveAitMetaFieldValues(values, userDTO.getCustomer().getAitTimelineMetaFieldsMap(), company.getId());

            for (MetaFieldValue metaFieldValue : values) {
                if (metaFieldValue.getField().getName().equals("SUPPLIER_DUNS")) {
                    CRDUNSNumber = (String) metaFieldValue.getValue();
                    complianceReportDET.setCRDUNSNumber(CRDUNSNumber);
                }
                if (metaFieldValue.getField().getName().equals("UTILITY_CUST_ACCT_NR")) {
                    complianceReportDET.setESIIDNumber((String) metaFieldValue.getValue());
                }
                if (metaFieldValue.getField().getName().equals("NAME")) {
                    String string = (String) metaFieldValue.getValue();
                    complianceReportDET.setCustomerFirstName(string.toUpperCase());
                }
                if (metaFieldValue.getField().getName().equals("LAST_NAME")) {
                    String string = (String) metaFieldValue.getValue();
                    complianceReportDET.setCustomerLastName(string.toUpperCase());
                }
                if (metaFieldValue.getField().getName().equals("TELEPHONE")) {
                    complianceReportDET.setPrimaryPhoneNumber((String) metaFieldValue.getValue());
                }
                if (metaFieldValue.getField().getName().equals("ADDRESS1")) {
                    String string = (String) metaFieldValue.getValue();
                    address = string.toUpperCase();
                }
                if (metaFieldValue.getField().getName().equals("CITY")) {
                    String string = (String) metaFieldValue.getValue();
                    city = string.toUpperCase();
                }
                if (metaFieldValue.getField().getName().equals("STATE")) {
                    String string = (String) metaFieldValue.getValue();
                    state = string.toUpperCase();
                }
                if (metaFieldValue.getField().getName().equals("ZIP_CODE")) {
                    zipCode = (String) metaFieldValue.getValue();
                }
                if (metaFieldValue.getField().getName().equals("Termination")) {
                    complianceReportDET.setTermination((String) metaFieldValue.getValue());
                }
            }

            complianceReportDET.setCustomerCompanyName(userDTO.getCompany().getDescription());
            complianceReportDET.setCustomerCompanyContactName(address + "|" + city + "|" + state + "|" + zipCode);
            complianceReportDET.setPrimaryPhoneNumberExtension("");

        }

        return complianceReportDET;
    }

    private File createReport(List<ComplianceReportDET> entryList) {
        fileHeader = headerRecordType + "|" + reportType + "|" + reportId + "|" + TDSPDUNSNumber;
        try {
            File report = File.createTempFile("compliance_report", ".txt");
            Writer writer = new FileWriter(report);
            writer.append(fileHeader).append(System.lineSeparator());
            for (ComplianceReportDET detRecord : entryList) {
                String detLine = detRecord.getDetRecordType() + "|" + detRecord.getRecordNumber() + "|" +
                        detRecord.getCRDUNSNumber() + "|" + detRecord.getESIIDNumber() + "|" +
                        detRecord.getCustomerFirstName() + "|" + detRecord.getCustomerLastName() + "|" +
                        detRecord.getCustomerCompanyName() + "|" + detRecord.getCustomerCompanyContactName() + "|" +
                        detRecord.getPrimaryPhoneNumber() + "|" + detRecord.getPrimaryPhoneNumberExtension() + "|";
                writer.append(detLine).append(System.lineSeparator());
            }
            writer.append(sumRecordType + "|" + totalNumberOfDETRecords + "|" + totalNumberOfIDTRecords + "|" +
                            totalNumberOfNDTRecords);
            writer.close();

            return report;
        } catch (IOException e) {
            LOG.debug(e.getMessage());
        }

        return null;
    }

}
