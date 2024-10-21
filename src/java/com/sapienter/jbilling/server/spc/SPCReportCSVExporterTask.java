package com.sapienter.jbilling.server.spc;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.invoice.db.SpcDetailedBillingReportDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.report.db.ReportExporter;
import com.sapienter.jbilling.server.report.db.ReportSQLQueries;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

public class SPCReportCSVExporterTask extends AbstractCronTask {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DETAILED_BILLING_REPORT = "detailed_billing_report";
    private static final String ACTIVE_SERVICES_REPORT = "active_services_report";
    private static final String PARTITIONED_FILES = "/partitioned-files";
    private static final String DEFAULT_DIRECTORY = "exported-reports";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final int PAGE_SIZE = 100;

    private static final ParameterDescription PARAM_SPC_REPORT_NAMES_LIST =
            new ParameterDescription("spc_report_names_list", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_SPC_CSV_EXPORT_PATH =
            new ParameterDescription("spc_csv_export_path", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_SPC_CSV_FILES_SPLIT_LIMIT =
            new ParameterDescription("spc_csv_files_split_limit", true, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_SPC_BILLING_DATE =
            new ParameterDescription("billing_date", false, ParameterDescription.Type.DATE);
    private SpcReportHelperService spcReportHelperService = Context.getBean(SpcReportHelperService.class);

    public SPCReportCSVExporterTask() {
        descriptions.add(PARAM_SPC_REPORT_NAMES_LIST);
        descriptions.add(PARAM_SPC_CSV_EXPORT_PATH);
        descriptions.add(PARAM_SPC_CSV_FILES_SPLIT_LIMIT);
        descriptions.add(PARAM_SPC_BILLING_DATE);
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            _init(context);
            logger.debug("Executing : {} : ", getName());
            String reportNameList = getParameter(PARAM_SPC_REPORT_NAMES_LIST.getName(), StringUtils.EMPTY);
            logger.debug("reportNameList : {}", reportNameList);
            String baseDirectory = getParameter(PARAM_SPC_CSV_EXPORT_PATH.getName(), StringUtils.EMPTY);
            logger.debug("baseDirectory : {}", baseDirectory);
            Integer filesSplitLimit = getParameter(PARAM_SPC_CSV_FILES_SPLIT_LIMIT.getName(), 0);
            logger.debug("filesSplitLimit : {}", filesSplitLimit);
            String customBillingDate = getParameterValue(PARAM_SPC_BILLING_DATE.getName());
            logger.debug("customBillingDate : {}", customBillingDate);

            if (StringUtils.isNotBlank(reportNameList)) {
                Stream.of(reportNameList.split(",")).forEach(reportName -> {
                    try {
                        if(reportName.trim().equalsIgnoreCase(ACTIVE_SERVICES_REPORT)) {
                            activeServicesReport(baseDirectory, ACTIVE_SERVICES_REPORT, getEntityId(), filesSplitLimit);
                        } else if(reportName.trim().equalsIgnoreCase(DETAILED_BILLING_REPORT)) {
                            detailedBillingReport(baseDirectory, DETAILED_BILLING_REPORT, filesSplitLimit, customBillingDate);
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                });
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void activeServicesReport(String baseDirectory, String reportName, Integer entityId, Integer filesSplitLimit) throws IOException {
        String directory = createDirectoryIfNotExists(baseDirectory, reportName, sdf.format(DateConvertUtils.getNow()));
        logger.debug("exporting active_services_report report to {}", directory);
        String sql = String.format(ReportSQLQueries.ACTIVE_SERVICES_REPORT, entityId);
        new ReportExporter().readAndExportDataToFile(sql, directory, directory.concat(PARTITIONED_FILES), reportName.concat(".csv"), filesSplitLimit);
    }

    private void detailedBillingReport(String baseDirectory, String reportName, Integer filesSplitLimit, String customBillingDate) throws IOException {
        String validBillingRunDate = null;
        if (StringUtils.isNotBlank(customBillingDate)) {
            validBillingRunDate = spcReportHelperService.validateAndGetBillingProcessDate(getEntityId(), customBillingDate);
        } else {
            validBillingRunDate = spcReportHelperService.getLatestBillingProcessDate(getEntityId());
        }

        if (StringUtils.isNotBlank(validBillingRunDate)) {
            //Generate data and populate detailed billing report table before exporting to CSV.
            createDetailedBillingRecords(validBillingRunDate);
            String directory = createDirectoryIfNotExists(baseDirectory, reportName, validBillingRunDate);
            logger.debug("exporting detailed billing report for bill run {} to location {}", validBillingRunDate, directory);
            String sql = ReportSQLQueries.DETAILED_BILLING_REPORT;
            new ReportExporter().readAndExportDataToFile(sql.replace("billing_date", "'" + validBillingRunDate + "'"), directory, directory.concat(PARTITIONED_FILES), reportName.concat(".csv"), filesSplitLimit);
        }
    }

    private String createDirectoryIfNotExists(String directory, String reportName, String exportDate) throws IOException {
        if(StringUtils.isBlank(directory)) {
            directory = DEFAULT_DIRECTORY;
        }
        if(!new File(directory).exists()) {
            directory = System.getProperty("user.home").concat("/").concat(directory);
        }
        directory = directory.concat("/").concat(reportName).concat("/").concat("%s");
        String createdDirectory = createDirectoryIfNotExists(String.format(directory, exportDate));
        createDirectoryIfNotExists(createdDirectory.concat(PARTITIONED_FILES));
        return createdDirectory;
    }

    public String createDirectoryIfNotExists(String directory) throws IOException {
        if (!new File(directory).exists()) {
            Files.createDirectories(Paths.get(directory));
        }
        return directory;
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() +"-"+getTaskId()+ "-" + getEntityId();
    }
    
    public String getName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    private void createDetailedBillingRecords(String billingDate) {
        try {
            long startTime = System.currentTimeMillis();
            boolean shouldPopulate = spcReportHelperService.shouldPopulateDetailedBillingReportTable(getEntityId(), billingDate);
            if (!shouldPopulate) {
                logger.debug("The detailed billing report is already completed for latest/custom billing date");
                return;
            }
            logger.debug("Generating detailed billing report for billing date {}", billingDate);

            List<SpcDetailedBillingReportDTO> reportDTOs = new ArrayList<>();
            List<Map<String, Object>> invoiceRecords =
                    spcReportHelperService.findInvoicesByBillingProcessPaged(billingDate, PAGE_SIZE, null);
            while(invoiceRecords != null && !invoiceRecords.isEmpty()) {
                try {
                    for(Map<String, Object> invoiceMap : invoiceRecords) {
                        Integer invoiceId = (Integer)invoiceMap.get("id");
                        Date invoiceDate = (Date)invoiceMap.get("create_datetime");
                        Integer billingProcessId = (Integer)invoiceMap.get("billing_process_id");
                        Integer userId = (Integer)invoiceMap.get("user_id");
                        String userName = spcReportHelperService.getUserName(userId);

                        List<Map<String, Object>> invoiceLines = spcReportHelperService.findInvoiceLinesByInvoiceId(invoiceId);
                        for(Map<String, Object> invoiceLineMap : invoiceLines) {
                            SpcDetailedBillingReportDTO detailedBillingReportDTO =
                                    new SpcDetailedBillingReportDTO(billingProcessId, invoiceId, invoiceDate);
                            detailedBillingReportDTO.setUserId(userId);
                            detailedBillingReportDTO.setUserName(userName);

                            Integer orderId = null;
                            Integer itemId = null;
                            String callIdentifier = StringUtils.EMPTY;
                            if (CollectionUtils.isNotEmpty(invoiceLineMap.entrySet())) {
                                orderId = (Integer)invoiceLineMap.get("order_id");
                                itemId  = (Integer)invoiceLineMap.get("item_id");
                                callIdentifier = (String) invoiceLineMap.get("call_identifier");
                                BigDecimal grossAmount= (BigDecimal)invoiceLineMap.get("gross_amount");
                                BigDecimal salesExGst = null != grossAmount && grossAmount.compareTo(BigDecimal.ZERO) != 0 ? grossAmount : (BigDecimal)invoiceLineMap.get("amount");
                                detailedBillingReportDTO.setSalesExGst(salesExGst.setScale(10));
                                BigDecimal gst = null != itemId && itemId.equals(45) ? BigDecimal.ZERO : (BigDecimal)invoiceLineMap.get("tax_amount");
                                detailedBillingReportDTO.setGst(gst.setScale(10));
                            }

                            String glCode = null;
                            Map<String, Object> product = spcReportHelperService.getProduct(itemId);
                            if (null != product && CollectionUtils.isNotEmpty(product.entrySet())) {
                                String productCode = (String)product.get("internal_number");
                                detailedBillingReportDTO.setProductCode(productCode);
                                Date producEndDate = (Date)product.get("active_until");
                                detailedBillingReportDTO.setProducEndDate(producEndDate);
                                glCode = spcReportHelperService.getItemMetafieldByName(itemId, SPCConstants.COSTS_GL_CODE);
                            }
                            boolean isPlan = spcReportHelperService.isPlan(itemId);
                            List<String> callIdentifiers = new ArrayList<>();
                            if (null != callIdentifier) {
                                detailedBillingReportDTO.setCallIdentifier(callIdentifier);
                            } else {
                                if (isPlan) {
                                    callIdentifiers = spcReportHelperService.getAssetIdetifierForMonthlyOrder(orderId);
                                }
                                if (CollectionUtils.isEmpty(callIdentifiers)) {
                                    callIdentifiers = spcReportHelperService.getAssetIdetifierForSubscriptionOrder(orderId);
                                }
                            }

                            if (CollectionUtils.isNotEmpty(callIdentifiers)) {
                                detailedBillingReportDTO.setCallIdentifier(String.join(",", callIdentifiers));
                                List<String>  serviceEmailList = spcReportHelperService.getAssetMetaFieldByName(orderId, SPCConstants.EMAIL, callIdentifiers);
                                detailedBillingReportDTO.setServiceEmail(CollectionUtils.isNotEmpty(serviceEmailList) ? String.join(",", serviceEmailList) : null);
                                List<String> serviceNumberList = spcReportHelperService.getAssetMetaFieldByName(orderId, SPCConstants.SERVICE_ID, callIdentifiers);
                                detailedBillingReportDTO.setServiceNumber(CollectionUtils.isNotEmpty(serviceNumberList) ? String.join(",", serviceNumberList) : null);
                            }

                            String planOrProductName =spcReportHelperService.getPlanOrProductName(itemId);
                            detailedBillingReportDTO.setPlanOrProductName(planOrProductName);

                            String serviceType = null;
                            String serviceDescription = null;
                            String revenueGlCode = null;
                            String costsGlCode = null;
                            String taxCode = null;
                            if (isPlan) {
                                revenueGlCode = spcReportHelperService.getPlanMetaFiledByName(itemId, SPCConstants.PLAN_GL);
                                costsGlCode = spcReportHelperService.getPlanMetaFiledByName(itemId, SPCConstants.COSTS_GL_CODE);
                                taxCode = spcReportHelperService.getPlanMetaFiledByName(itemId, SPCConstants.TAX_SCHEME); 
                            } else if (null != itemId) {
                                serviceType =  spcReportHelperService.getServiceType(itemId);
                                serviceDescription =  spcReportHelperService.getServiceDescription(itemId);
                                revenueGlCode = null != product ? (String)product.get("gl_code") : null;
                                costsGlCode = glCode;
                                taxCode = spcReportHelperService.getItemMetafieldByName(itemId, SPCConstants.TAX_SCHEME);
                            }
                            //Revenue GL code and costs GL code
                            detailedBillingReportDTO.setRevenueGlCode(revenueGlCode);
                            detailedBillingReportDTO.setCostsGlCode(costsGlCode);
                            //Service type and description
                            detailedBillingReportDTO.setServiceDescription(serviceDescription);
                            detailedBillingReportDTO.setServiceType(serviceType);
                            //Plan type and tax code
                            detailedBillingReportDTO.setPlanType(spcReportHelperService.getPlanType(itemId));
                            detailedBillingReportDTO.setTaxCode(taxCode);
                            //Tariff code
                            String tariffCode = spcReportHelperService.getTariffCode(itemId,orderId);
                            detailedBillingReportDTO.setTariffCode(tariffCode);

                            detailedBillingReportDTO.setCostOfService(spcReportHelperService.getCostOfService(itemId, orderId));

                            detailedBillingReportDTO.setOrigin(spcReportHelperService.getUserMetaFiledByName(userId, SPCConstants.ORIGIN));

                            detailedBillingReportDTO.setTariffDescription(spcReportHelperService.getTariffDescription(tariffCode));
                            //Rollup codes and its descriptions
                            String rollUpCode =spcReportHelperService.getRollUpCode(itemId);
                            detailedBillingReportDTO.setRollupCode(rollUpCode);
                            detailedBillingReportDTO.setRollupDescription(spcReportHelperService.getRollupDescription(rollUpCode));

                            String superRollUpCode = spcReportHelperService.getSuperRollUpCode(itemId);
                            detailedBillingReportDTO.setSuperRollupCode(superRollUpCode);
                            detailedBillingReportDTO.setSuperRollupDescription(spcReportHelperService.getSuperRollupDescription(superRollUpCode));

                            String superSuperRollUpCode = spcReportHelperService.getSuperSuperRollUpCode(itemId);
                            detailedBillingReportDTO.setSuperSuperRollupCode(superSuperRollUpCode);
                            detailedBillingReportDTO.setSuperSuperRollupDescription(spcReportHelperService.getSuperSuperRollupDescription(superSuperRollUpCode));
                            //Order from date and to date
                            Integer invoiceLineId = (Integer)invoiceLineMap.get("id");
                            Date fromDate = spcReportHelperService.getFromDate(invoiceLineId);
                            if (null == fromDate) {
                                fromDate = spcReportHelperService.getOrderProcessFromDate(orderId, invoiceId);
                                if (null == fromDate) {
                                    fromDate = spcReportHelperService.getActiveSince(orderId);
                                }
                            }
                            detailedBillingReportDTO.setFromDate(fromDate);
                            Date toDate = spcReportHelperService.getToDate(invoiceLineId);
                            if (null == toDate) {
                                toDate = spcReportHelperService.getOrderProcessToDate(orderId, invoiceId);
                            }
                            detailedBillingReportDTO.setToDate(toDate);
                            //add all fields to list
                            reportDTOs.add(detailedBillingReportDTO);
                        }
                    }

                } catch(Exception ex) {
                    logger.error("Exception occurred while fetching detailed billing report data from different sources", ex);
                    invoiceRecords.clear();
                    reportDTOs.clear();
                    spcReportHelperService.deleteSpcDetailedBillingReportRecords(billingDate);
                }

                //batch insert into database using jdbc template
                spcReportHelperService.executeBatchInsert(reportDTOs);
                reportDTOs.clear();

                if (!invoiceRecords.isEmpty()) {
                    Map<String, Object> lastInvoiceMap = invoiceRecords.get(invoiceRecords.size() - 1);
                    Integer lastInvoiceId = (Integer)lastInvoiceMap.get("id");
                    invoiceRecords.clear();
                    invoiceRecords = spcReportHelperService.findInvoicesByBillingProcessPaged(billingDate, PAGE_SIZE, lastInvoiceId);
                }
            }
            long endTime = System.currentTimeMillis();
            logger.debug("SpcDetailedBillingReportTask process time = "+(endTime-startTime)+" ms");
        } catch(Exception ex) {
            logger.error("spc detailed billing report job is failed for entity {}", getEntityId(), ex);
        }

    }

    private String getParameterValue(String key) {
        String paramValue = getParameters().get(key);
        Date date = parseDate(paramValue);
        return date != null ? sdf.format(date) : null;
    }

    private Date parseDate(String strDate) {
        Date date = null;
        try {
            date = StringUtils.isNotBlank(strDate) ? sdf.parse(strDate) : null;
        } catch (ParseException ex) {
            logger.error("Date {} could not be parsed", strDate, ex);
        }
        return date;
    }
}
