package com.sapienter.jbilling.server.ediTransaction.task;


import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.FlatFileGenerator;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Record;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.RouteBeanFactory;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchResult;
import jbilling.RouteService;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;

// This class is used to generated the Missing Interval records of Meter Read
public class GenerateIntervalRecordTask extends PluggableTask
        implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(GenerateIntervalRecordTask.class);

    protected static final ParameterDescription METER_READ_STATUS_NAME =
            new ParameterDescription("meter_read_status", true, ParameterDescription.Type.STR);

    {
        descriptions.add(METER_READ_STATUS_NAME);
    }

    private RouteService routeService = Context.getBean(Context.Name.ROUTE_SERVICE);
    private static final Class<Event> events[] = new Class[]{
            NewEDIFileEvent.class,
            UpdateEDIFileEvent.class
    };

    private EDIFileDTO ediFileDTO;
    private String plan;
    private UserDTO userDTO;
    private Integer days;
    private String startDate;
    private String endDate;
    private PlanDTO planDTO;
    private Integer recordOrder;
    private EDIFileWS fileWS;
    protected Map<String, Map<String, String>> recordFields = new HashMap<String, Map<String, String>>();
    // File Constants
    IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);

    private String exceptionCode;
    private String status;

    private String INVALID_RECORD_GENERATION_STATUS;

    public enum IntervalRecordConstant{

        //QTY Record constants

        QTY_QUALIFIER("QTY_QUALIFIER"),
        TOTAL_CONSUMPTION("TOTAL_CONSUMPTION"),
        INTERVAL_DT("INTERVAL_DT"),
        INTERVAL_TIME("INTERVAL_TIME"),

        //REA Record
        READ_CONSUMPTION("READ_CONSUMPTION"),
        READ_START_DT("READ_START_DT"),
        READ_END_DT("READ_END_DT"),

        DATE_TIME_LOAD_CURVE("date_time"),
        E_FACILITY_LOAD_CURVE("e_facility"),
        G_FACILITY_LOAD_CURVE("g_facility"),
        CUST_LOAD_CURVE_METAFIELD("interval_load_curve"),
        SYSTEM_GENERATED("SYSTEM_GENERATED");
        String val;

        IntervalRecordConstant(String value) {
            val = value;
        }

        String getValue() {
            return val;
        }

        }


    public Class<Event>[] getSubscribedEvents() {

        return events;
    }

    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd HHmm");

    public void process(Event event) throws PluggableTaskException {
        MetaFieldValue meterReadMetaField=null;
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        INVALID_RECORD_GENERATION_STATUS = getParameter(METER_READ_STATUS_NAME.getName(),"");

        if (event instanceof NewEDIFileEvent) {
            LOG.debug("this event instance of NewEDIFileEvent");
            ediFileDTO = ((NewEDIFileEvent) event).getEdiFileDTO();
            if (ediFileDTO.getFileStatus().getId() != FileConstants.EDI_STATUS_PROCESSED) {
                return;
            }
        } else if (event instanceof UpdateEDIFileEvent) {
            LOG.debug("this event instance of NewEDIFileEvent");
            ediFileDTO = ((UpdateEDIFileEvent) event).getEdiFileDTO();
        } else {
            return;
        }



        try{

            meterReadMetaField =  new CompanyDAS().find(getEntityId()).getMetaField(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);

            if(meterReadMetaField ==null || meterReadMetaField.getValue()==null){
                throw new SessionInternalError("Configuration issue : company level meta-field for meter read is not defined");
            }

            if(ediFileDTO.getEdiType().getId()!=(Integer)meterReadMetaField.getValue()){
                return;
            }

            LOG.debug("Binding Data");
            bindData();

           //Validating customer : if customer not exist in the system then return
            String customerAccountNumber = findField(MeterReadParserTask.MeterRead.HDR, MeterReadParserTask.MeterReadField.UTILITY_CUST_ACCT_NR.toString(), true);
            try{
                //if user not found then escaping plugin
                String commodity = findField(MeterReadParserTask.MeterRead.KEY, MeterReadParserTask.MeterReadField.COMMODITY.toString());
                LOG.debug("Commodity code :"+ commodity);
                try{
                    commodity = EDITransactionHelper.getCommodityFromCode(commodity);
                }catch (SessionInternalError sie){
                    throwException(sie.getMessage(), null, INVALID_RECORD_GENERATION_STATUS);
                }
                LOG.debug("Commodity :"+ commodity);
                userDTO = ediTransactionBean.findUserByAccountNumber(getEntityId(), FileConstants.CUSTOMER_ACCOUNT_KEY, customerAccountNumber, commodity);
            }catch (SessionInternalError se){

            }

            if(userDTO==null){
                return;
            }

            UserWS userWS = new UserBL(userDTO.getId()).getUserWS();
            plan=(String)Arrays.asList(userWS.getMetaFields()).stream().filter((MetaFieldValueWS metaFieldValue) -> {return metaFieldValue.getFieldName().equals(FileConstants.PLAN); }).findFirst().get().getValue();
            ItemDAS itemDAS=new ItemDAS();
            ItemDTO itemDTO=itemDAS.findItemByInternalNumber(plan, getEntityId());
            planDTO=new PlanDAS().findPlanByItemId(itemDTO.getId());
            if(planDTO==null){
                throw new SessionInternalError("Plan for code "+plan +" not found in the system");
            }

            //IF plan 'Interval Usage Required' meta-field is checked then generate the interval records
            MetaFieldValue intervalUsageMetaField=planDTO.getMetaField(FileConstants.INTERVAL_USAGE_REQUIRED_METAFIELD);
            if(intervalUsageMetaField ==null || intervalUsageMetaField.getValue()==null || !(Boolean)intervalUsageMetaField.getValue()) {
                return;
            }


            if (INVALID_RECORD_GENERATION_STATUS != null && !INVALID_RECORD_GENERATION_STATUS.equals("Invalid Record Generation")) {
                throw new SessionInternalError("Configuration issue: you have to configure meter_read_status parameter value: 'Invalid Record Generation' for GenerateIntervalRecordTask plugin.");
            }

            LOG.debug("Validating Record");
            validateRecord();

            LOG.debug("Generate Internal Record for Meter Read");
            addIntervalRecords();

            userWS.close();

        }catch (Exception e){
            EDIFileStatusWS statusWS = null;
            if (meterReadMetaField!=null && meterReadMetaField.getValue() != null) {
                EDITypeWS ediType = webServicesSessionSpringBean.getEDIType((Integer) meterReadMetaField.getValue());
                for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
                    if (ediStatus.getName().equals(status)) {
                        statusWS = ediStatus;
                        break;
                    }
                }
            }
            if(statusWS == null) statusWS = new EDIFileStatusBL().getWS(new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_ERROR_DETECTED));
            if (statusWS != null) fileWS.setEdiFileStatusWS(statusWS);
            if (exceptionCode != null) fileWS.setExceptionCode(exceptionCode);
            fileWS.setComment(e.getMessage());
        }

        new EDIFileBL().saveEDIFile(fileWS);

    }

    //set data in global level
    private void bindData(){
        fileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());
        EDIFileRecordWS keyRecord=Arrays.asList(fileWS.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals(MeterReadParserTask.MeterRead.KEY.toString())).findFirst().get();
        EDIFileRecordWS headerRecord=Arrays.asList(fileWS.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals(MeterReadParserTask.MeterRead.HDR.toString())).findFirst().get();
        EDIFileRecordWS summaryRecord=Arrays.asList(fileWS.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals(MeterReadParserTask.MeterRead.UMR.toString())).findFirst().get();
        EDIFileRecordWS summeryQtyRecord=Arrays.asList(fileWS.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals(MeterReadParserTask.MeterRead.QTY.toString())).findFirst().get();

        recordFields.put(MeterReadParserTask.MeterRead.KEY.toString(), parseRecord(keyRecord));
        recordFields.put(MeterReadParserTask.MeterRead.HDR.toString(), parseRecord(headerRecord));
        recordFields.put(MeterReadParserTask.MeterRead.UMR.toString(), parseRecord(summaryRecord));
        recordFields.put(MeterReadParserTask.MeterRead.QTY.toString(), parseRecord(summeryQtyRecord));
    }

    private void validateRecord(){
        fileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());
        MetaFieldValue metaFieldValue=planDTO.getMetaField(FileConstants.BILLING_MODEL);
        if(metaFieldValue==null || metaFieldValue.getValue()==null){
            throw new SessionInternalError("Plan should be bill ready for Interval product for Non Interval customer");
        }

        String billingModel=(String)metaFieldValue.getValue();
        if(!billingModel.equals(FileConstants.BILLING_MODEL_BILL_READY)){
            throw new SessionInternalError("Plan should be bill ready for Interval product for Non Interval customer");
        }
    }

    private void validateCustomer(){
        String customerAccountNumber = findField(MeterReadParserTask.MeterRead.HDR, MeterReadParserTask.MeterReadField.UTILITY_CUST_ACCT_NR.toString(), true);
        userDTO = new UserDAS().findByMetaFieldNameAndValue(getEntityId(), FileConstants.CUSTOMER_ACCOUNT_KEY, customerAccountNumber);
        if(userDTO==null){
            throw new SessionInternalError("No user found for account number");
        }
    }

    List<EDIFileRecordWS> createIntervalRecords(){
        List<EDIFileRecordWS> records=new ArrayList<EDIFileRecordWS>();

        MetaFieldValue intervalLoadCurve=userDTO.getCustomer().getMetaField(FileConstants.INTERVAL_LOAD_CURVE_CUSTOMER_METAFIELD);
        if(intervalLoadCurve==null || intervalLoadCurve.getValue()==null){
            throw new SessionInternalError("Customer should have a load curve");
        }

        CompanyDTO companyDTO=new CompanyDAS().find(getEntityId());
        RouteDTO routeDTO=new RouteDAS().getRoute(companyDTO.getParent().getId(), (String)intervalLoadCurve.getValue());

        if(routeDTO==null){
            throwException("No Load curve found for name "+intervalLoadCurve.getValue().toString(),FileConstants.LOAD_CURVE_MISSING_EXP_CODE,INVALID_RECORD_GENERATION_STATUS);
        }

        MetaFieldValue zoneMetaField=userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME);
        if(zoneMetaField==null || zoneMetaField.getValue()==null){
            throw new SessionInternalError("Customer should belongs to a Zone");
        }

        MetaFieldValue rateMetaField=userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_RATE_ID_METAFILE_FIELD_NAME);
        if(rateMetaField==null || rateMetaField.getValue()==null){
            throw new SessionInternalError("Generating meter read interval records Customer do not have rate_id");
        }

        startDate = findField(MeterReadParserTask.MeterRead.UMR, MeterReadParserTask.MeterReadField.START_SERVICE_DT.toString(), true);
        endDate = findField(MeterReadParserTask.MeterRead.UMR, MeterReadParserTask.MeterReadField.END_SERVICE_DT.toString(), true);

        String totalConsumption = findField(MeterReadParserTask.MeterRead.QTY, MeterReadParserTask.MeterReadField.TOTAL_CONSUMPTION.toString(), true);
        Integer intervalSize = Integer.parseInt(findField(MeterReadParserTask.MeterRead.UMR, MeterReadParserTask.MeterReadField.INTERVAL_TYPE.toString(), true));

        SearchResult<String> searchResult=fetchTableData(routeDTO.getId(), startDate, endDate, zoneMetaField.getValue().toString(), rateMetaField.getValue().toString());

        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();

        Integer dateTimeColumnIdx = columnNames.indexOf(IntervalRecordConstant.DATE_TIME_LOAD_CURVE.getValue());
        Integer eFacilityColumnIdx = columnNames.indexOf(IntervalRecordConstant.E_FACILITY_LOAD_CURVE.getValue());
        Integer gFacilityColumnIdx = columnNames.indexOf(IntervalRecordConstant.G_FACILITY_LOAD_CURVE.getValue());

        ItemDAS itemDAS=new ItemDAS();
        ItemDTO itemDTO=itemDAS.findItemByInternalNumber(plan, getEntityId());

        planDTO=new PlanDAS().findPlanByItemId(itemDTO.getId());
        String commodity=(String)planDTO.getPlanItems().stream().map((PlanItemDTO planItemDTO) -> planItemDTO.getItem()).findFirst().get().getMetaField(FileConstants.COMMODITY).getValue();

        Integer facilityColumnIndex=commodity.equals("E")? eFacilityColumnIdx:gFacilityColumnIdx;

        List<String> errorMessages=new ArrayList<String>();

        Integer loadCurveInterval=60;
        DateTime startDT=formatter.parseDateTime(startDate+" 0000");

        BigDecimal total=BigDecimal.ZERO;
        if(searchResult.getRows().size()==0){
            throwException("No load curve found for "+startDate+" to "+endDate+" for zone "+zoneMetaField.getValue(),FileConstants.LOAD_CURVE_MISSING_EXP_CODE,INVALID_RECORD_GENERATION_STATUS);
        }

        for(List row : searchResult.getRows()){
            //if subscribed item is electircity the eFacility else gFaciliy
            String dateTime=(String)row.get(dateTimeColumnIdx);
            if(!formatter.print(startDT).equals(dateTime)){
                errorMessages.add("No load curve found for "+startDT);
            }

            startDT=startDT.plusMinutes(loadCurveInterval);
            String facility=(String)row.get(facilityColumnIndex);
            total=total.add(new BigDecimal(facility));
        }

        if(errorMessages.size()>0){
            throwException("Load curve missing ", new String[]{"Load curve missing for period "+errorMessages},FileConstants.LOAD_CURVE_MISSING_EXP_CODE,INVALID_RECORD_GENERATION_STATUS);
        }

        if(total.compareTo(new BigDecimal(totalConsumption))==1){
            throw new SessionInternalError("Total consumption("+totalConsumption+") should be greater then sum of facility value("+total+") on load curve period ("+startDate+") and ("+endDate+")");
        }

        total=new BigDecimal(totalConsumption).divide(total, MathContext.DECIMAL64);

        DateTime startD=formatter.parseDateTime(startDate+" 0000");
        BigDecimal consumptionSum = BigDecimal.ZERO;
        for(List row:searchResult.getRows()){
            String intervalDateTime=formatter.print(startD);
            String facility=(String)row.get(facilityColumnIndex);
            BigDecimal data=total.multiply(new BigDecimal(facility)).multiply(new BigDecimal(intervalSize/loadCurveInterval));
            consumptionSum = consumptionSum.add(data);
            // Generate File record from data
            records.add(buildRecordWS(MeterReadParserTask.MeterRead.QTY.toString(), buildQTYRecord(data + "", intervalDateTime.split(" ")[0], intervalDateTime.split(" ")[1])));
            records.add(buildRecordWS(MeterReadParserTask.MeterRead.REA.toString(), buildREARecord(data + "")));
            startD = startD.plusMinutes(intervalSize);
        }
        /*
        * Adjust amount is last record as per total consumption. There may have some fractional difference
        * We need to add that in last record, so that total remain same in interval record too.
        * */
        BigDecimal summaryRecordConsumption = new BigDecimal(totalConsumption);
        BigDecimal difference = summaryRecordConsumption.subtract(consumptionSum);
        LOG.debug("totalConsumption: " + consumptionSum + ", summaryRecordConsumption: " + summaryRecordConsumption + ", difference: " + difference);
        if (BigDecimal.ONE.compareTo(difference) != 1) {
            LOG.error("Difference between totalConsumption and summaryRecordConsumption should not be greater than 1. Total Consumption: "+consumptionSum+" Summary Record: "+summaryRecordConsumption+" Difference: "+difference);
            throw new SessionInternalError("Difference between totalConsumption and summaryRecordConsumption should not be greater than 1. Total Consumption: "+consumptionSum+" Summary Record: "+summaryRecordConsumption+" Difference: "+difference);
        }else if(records.size()>=2 && difference.negate().compareTo(BigDecimal.ONE)<1){
            EDIFileRecordWS lastQTY = records.get(records.size()-2);
            EDIFileFieldWS field = Arrays.stream(lastQTY.getEdiFileFieldWSes())
                    .filter(ediFileFieldWS -> ediFileFieldWS.getKey().equals(MeterReadParserTask.MeterReadField.TOTAL_CONSUMPTION.toString()))
                    .findFirst().get();
            field.setValue(difference.add(new BigDecimal(field.getValue())).toString());
        }

        return records;
    }

    private void addIntervalRecords() {
        // Set record order according to existing records in file.
        recordOrder=ediFileDTO.getEdiFileRecords().size()+1;

        // Calculate usage interval and generate records for new intervals
        List<EDIFileRecordWS> systemGeneratedIntervalRecords = createIntervalRecords();

       // EDIFileWS ediFileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());

        // Get existing record from file and add new records in it.
        List<EDIFileRecordWS> existingRecords =  new LinkedList<EDIFileRecordWS>(Arrays.asList(fileWS.getEDIFileRecordWSes()));
        existingRecords.addAll(systemGeneratedIntervalRecords);

        fileWS.setEDIFileRecordWSes(existingRecords.toArray(new EDIFileRecordWS[existingRecords.size()]));
    }

    /**
     * Generate record from key-value
     * @param recordName
     * @param recordValueMap Record's field value in
     * @return EDIFileRecordWS
     */
    private EDIFileRecordWS buildRecordWS(String recordName, Map<String, String> recordValueMap) {
        FileFormat fileFormat = FileFormat.getFileFormat(ediFileDTO.getEdiType().getId());
        FlatFileGenerator generator = new FlatFileGenerator(fileFormat, getEntityId(), null, null);

        Record record = fileFormat.getFileStructure().getRecord(recordName);
        if (record == null) {
            throw new SessionInternalError("Record name not exist in format :" + recordName);
        }

        LOG.debug("Creating single record");
        EDIFileRecordWS ediFileRecordWS= generator.generateEDIRecord(record, recordValueMap);
        ediFileRecordWS.setHeader(recordName);
        ediFileRecordWS.setEntityId(getEntityId());
        ediFileRecordWS.setRecordOrder(recordOrder);

        //Adding a new EDIFile field system generated in the file record
        List<EDIFileFieldWS> ediFileFieldWSList=new ArrayList(Arrays.asList(ediFileRecordWS.getEdiFileFieldWSes()));
        ediFileFieldWSList.add(new EDIFileFieldWS(IntervalRecordConstant.SYSTEM_GENERATED.getValue(), "true", null, ediFileRecordWS.getEdiFileFieldWSes().length+1));
        ediFileRecordWS.setEdiFileFieldWSes(ediFileFieldWSList.toArray(new EDIFileFieldWS[ediFileFieldWSList.size()]));

        recordOrder++;
        return ediFileRecordWS;
    }

    private void parseRecords(List<String> recordNames, EDIFileRecordWS[] allRecords) {
        recordFields = new HashMap<String, Map<String, String>>();
        for (EDIFileRecordWS record : allRecords) {
            String recordName = record.getHeader();
            if (recordNames.contains(recordName)) {
                recordFields.put(record.getHeader(), parseRecord(record));
            }
        }
    }

    private Map<String, String> parseRecord(EDIFileRecordWS record) {
        Map fields = new HashMap<String, String>();
        for (EDIFileFieldWS field : record.getEdiFileFieldWSes()) {
            fields.put(field.getKey(), field.getValue());
        }
        return fields;
    }

    protected String findField(FileStructure recordName, String fieldName) {
        if (recordFields.containsKey(recordName.toString())) {
            Map<String, String> fields = recordFields.get(recordName.toString());
            if (fields.containsKey(fieldName)) {
                return fields.get(fieldName);
            }
        }
        return null;
    }

    protected String findField(FileStructure recordName, String fieldName, boolean mandatory) {
        String value = findField(recordName, fieldName);
        if ((value == null || value.isEmpty()) && mandatory == true) {
            throw new SessionInternalError("Mandatory field not found : \"" + fieldName + "\"");
        }
        return value;
    }


    Map<String, String> findRecords(String name){
        Map<String, String> result=new HashMap<>();
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(name)){
                for(EDIFileFieldDTO ediFileFieldDTO:ediFileRecordDTO.getFileFields()){
                    result.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                }
            }
        }
        return result;
    }
    private Map<String, String> buildQTYRecord(String quantity, String intervalDate, String intervalTime) {
        Map<String, String> qtyMap = new HashMap<String, String>();
        qtyMap.put("rec-id", MeterReadParserTask.MeterRead.QTY.name());
        qtyMap.put(IntervalRecordConstant.QTY_QUALIFIER.name(), "QD");
        qtyMap.put(IntervalRecordConstant.TOTAL_CONSUMPTION.name(), quantity);
        qtyMap.put(IntervalRecordConstant.INTERVAL_DT.name(), intervalDate);
        qtyMap.put(IntervalRecordConstant.INTERVAL_TIME.name(), intervalTime);

        return qtyMap;
    }

    private Map<String, String> buildREARecord(String quantity) {
        Map<String, String> reaMap = new HashMap<String, String>();
        reaMap.put("rec-id", MeterReadParserTask.MeterRead.REA.name());
        reaMap.put(IntervalRecordConstant.READ_CONSUMPTION.name(), quantity);
        reaMap.put(IntervalRecordConstant.READ_START_DT.name(),startDate);
        reaMap.put(IntervalRecordConstant.READ_END_DT.name(),endDate);
        return reaMap;
    }

    private SearchResult<String> fetchTableData(Integer tableId, String startDate, String endDate,  String zone, String rateId) {

        String dateFormat = "yyyymmdd HH24MI";
        String dateConversionMethod = "to_timestamp";

        DateTime startDT=formatter.parseDateTime(startDate+" 0000" );
        DateTime endtDT=formatter.parseDateTime(endDate + " 2300");

        days=Days.daysBetween(startDT.toLocalDate(), endtDT.toLocalDate()).getDays()+1;

        Object startValue = new java.sql.Timestamp(startDT.toDate().getTime());
        Object endValue = new java.sql.Timestamp(endtDT.toDate().getTime());

        Map<String, Map<String, Object>> filters = new HashMap<String, Map<String, Object>>();
        Map<String, Object> startDateFilter = new HashMap<String, Object>();
        startDateFilter.put("key", dateConversionMethod+"(date_time,'"+dateFormat+"')");
        startDateFilter.put("value", endValue);
        startDateFilter.put("constraint", Filter.FilterConstraint.LE.toString());

        Map<String, Object> endDateFilter = new HashMap<String, Object>();
        endDateFilter.put("key", dateConversionMethod+"(date_time,'"+dateFormat+"')");
        endDateFilter.put("value", startValue);
        endDateFilter.put("constraint", Filter.FilterConstraint.GE.toString());

        Map<String, Object> zoneFilter = new HashMap<String, Object>();
        zoneFilter.put("key", "zone_id");
        zoneFilter.put("value", zone);
        zoneFilter.put("constraint", Filter.FilterConstraint.EQ.toString());

        Map<String, Object> rateFilter = new HashMap<String, Object>();
        rateFilter.put("key", "rate_id");
        rateFilter.put("value", rateId);
        rateFilter.put("constraint", Filter.FilterConstraint.EQ.toString());


        filters.put("date_1", startDateFilter);
        filters.put("date_2", endDateFilter);
        filters.put("zone", zoneFilter);
        filters.put("rate", rateFilter);

        return routeService.getFilteredRecords(tableId, filters);
    }

    public void throwException(String message, String exceptionCode, String status){
        if(exceptionCode!=null && !exceptionCode.isEmpty())this.exceptionCode=exceptionCode;
        if(status!=null && !status.isEmpty())this.status=status;
        throw new SessionInternalError(message);
    }

    public void throwException(String message, String[] errors,String exceptionCode, String status){
        if(exceptionCode!=null && !exceptionCode.isEmpty())this.exceptionCode=exceptionCode;
        if(status!=null && !status.isEmpty())this.status=status;
        throw new SessionInternalError(message,errors);
    }
}
