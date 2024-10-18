package com.sapienter.jbilling.server.ediTransaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileFieldDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileRecordDTO;
import com.sapienter.jbilling.server.ediTransaction.task.CalculateTaxLine;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

/**
 * Created by neeraj on 18/9/15.
 */
public class EDITransactionHelper {

    private static final Logger LOG = Logger.getLogger(CalculateTaxLine.class);

    public  EDIFileRecordDTO getHeaderRecord(EDIFileDTO ediFileDTO){
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(CommonConstants.METER_READ_HEADER_RECORD_VALUE)){
                return ediFileRecordDTO;
            }
        }
        return null;
    }

    public  EDIFileRecordWS getHeaderRecord(EDIFileWS ediFileWS){
        for(EDIFileRecordWS ediFileRecord:ediFileWS.getEDIFileRecordWSes()){
            if(ediFileRecord.getHeader().equals(CommonConstants.METER_READ_HEADER_RECORD_VALUE)){
                return ediFileRecord;
            }
        }
        return null;
    }

    public  EDIFileRecordDTO getKeyRecord(EDIFileDTO ediFileDTO){
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(CommonConstants.METER_READ_KEY_RECORD_VALUE)){
                return ediFileRecordDTO;
            }
        }
        return null;
    }


    public  String getRecordByFileFieldKeyAndFieldKey(EDIFileDTO ediFileDTO, String fieldKey){
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            for (EDIFileFieldDTO fileFieldDTO:ediFileRecordDTO.getFileFields()){
                if(fileFieldDTO.getEdiFileFieldKey().equals(fieldKey)){
                    return fileFieldDTO.getEdiFileFieldValue();
                }
            }
        }
        return null;
    }

    public  MeterReadRecord getSummaryRecord(EDIFileDTO ediFileDTO){
       MeterReadRecord meterReadRecord=new MeterReadRecord();
        int i=0;
        DateTimeFormatter df = DateTimeFormat.forPattern("yyyyMMdd");
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(CommonConstants.METER_READ_USAGE_RECORD)){
                for(EDIFileFieldDTO fileFieldDTO:ediFileRecordDTO.getFileFields()){
                    if(fileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_USAGE_TYPE) && fileFieldDTO.getEdiFileFieldValue().equals(CommonConstants.METER_READ_USAGE_SUMMARY)){
                        EDIFileRecordDTO usageReadRecordREA=ediFileDTO.getEdiFileRecords().get(i+2);
                        EDIFileRecordDTO usageReadRecordUMR=ediFileDTO.getEdiFileRecords().get(i);
                        for(EDIFileFieldDTO ediFileFieldDTO:usageReadRecordREA.getFileFields()){
                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_CONSUMPTION_RECORD_KEY)){
                                meterReadRecord.setTotalConsumption(Integer.parseInt(ediFileFieldDTO.getEdiFileFieldValue()));
                                break;
                            }
                        }
                        for(EDIFileFieldDTO ediFileFieldDTO:usageReadRecordUMR.getFileFields()){
                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_START_DATE)){
                                if(ediFileFieldDTO.getEdiFileFieldValue()!=null && !ediFileFieldDTO.getEdiFileFieldValue().equals("")){
                                    meterReadRecord.setStartDate(df.parseDateTime(ediFileFieldDTO.getEdiFileFieldValue()).toDate());
                                }
                            }
                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_END_DATE)){
                                if(ediFileFieldDTO.getEdiFileFieldValue()!=null && !ediFileFieldDTO.getEdiFileFieldValue().equals("")){
                                    meterReadRecord.setEndDate(df.parseDateTime(ediFileFieldDTO.getEdiFileFieldValue()).toDate());
                                }
                            }
                        }
                        return meterReadRecord;
                    }
                }
            }
            i++;
        }
        return null;
    }

    public  List<MeterReadRecord> getDetailRecords(EDIFileDTO ediFileDTO){
        List<MeterReadRecord> meterReadRecordList=new ArrayList<MeterReadRecord>();
        int i=0;
        DateTimeFormatter df = DateTimeFormat.forPattern("yyyyMMdd");
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(CommonConstants.METER_READ_USAGE_RECORD)){
                for(EDIFileFieldDTO fileFieldDTO:ediFileRecordDTO.getFileFields()){
                    if(fileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_USAGE_TYPE) && fileFieldDTO.getEdiFileFieldValue().equals(CommonConstants.METER_READ_USAGE_DETAIL)){
                        EDIFileRecordDTO usageReadRecord=ediFileDTO.getEdiFileRecords().get(i+2);
                        MeterReadRecord meterReadRecord=new MeterReadRecord();
                        for(EDIFileFieldDTO ediFileFieldDTO:usageReadRecord.getFileFields()){
                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_CONSUMPTION_RECORD_KEY)){
                                meterReadRecord.setTotalConsumption(Integer.parseInt(ediFileFieldDTO.getEdiFileFieldValue()));
                            }

                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_START_DATE)){
                                meterReadRecord.setStartDate(df.parseDateTime(ediFileFieldDTO.getEdiFileFieldValue()).toDate());
                            }
                            if(ediFileFieldDTO.getEdiFileFieldKey().equals(CommonConstants.METER_READ_END_DATE)){
                                meterReadRecord.setEndDate(df.parseDateTime(ediFileFieldDTO.getEdiFileFieldValue()).toDate());
                            }
                        }
                        meterReadRecordList.add(meterReadRecord);
                    }
                }
            }
            i++;
        }
        return meterReadRecordList;
    }

    public static  Map<Integer, Map<String, String>> getPaymentAccountRecordsMap(EDIFileDTO ediFileDTO){
        Map<Integer, Map<String, String>> paymentAccountRecordMap=new HashMap<Integer, Map<String, String>>();
        int i=0;
        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(FileConstants.ACCOUNT_RECORD_KEY)){
                Map<String, String> values=new HashMap<String, String>();
                for(EDIFileFieldDTO ediFileFieldDTO:ediFileRecordDTO.getFileFields()){
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(FileConstants.UTILITY_CUST_ACCT_NR)){
                        values.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                    }
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(FileConstants.PYMT_AMOUNT)){
                        values.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                    }
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(FileConstants.DISCOUNT_AMT)){
                        values.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                    }
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(FileConstants.ORG_INVOICE_AMT)){
                        values.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                    }
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(FileConstants.INVOICE_NR)){
                        values.put(ediFileFieldDTO.getEdiFileFieldKey(), ediFileFieldDTO.getEdiFileFieldValue());
                    }
                    values.put("recordId", ""+ediFileRecordDTO.getId());

                }
                paymentAccountRecordMap.put(i, values);
                i++;
            }
        }
        return paymentAccountRecordMap;
    }

    public static String getExceptionMessage(Throwable throwable) {
        return ExceptionUtils.getRootCause(throwable) != null && throwable instanceof SessionInternalError ? ExceptionUtils.getRootCause(throwable).getMessage() : throwable.getMessage();
    }

    public static String getCommodityFromCode(String commodity){

        if ("E".equals(commodity)) {
            commodity = FileConstants.COMMODITY_ELECTRICITY;
        } else if ("G".equals(commodity)) {
            commodity = FileConstants.COMMODITY_GAS;
        } else {
            throw new SessionInternalError("Commodity field is not valid is KEY record");
        }

        return commodity;

    }

    /*
    * This method return the metafields which name is in metaFieldNames list and belonging to  'Service Information' or 'Account Information' ait group.
    *
    * @params metaFieldNames list of metafields which want to retrieve form enrollment    *
    * */
    public static Map<String, MetaFieldValue> getMetaFieldValues(CustomerEnrollmentDTO enrollmentDTO, List<String> metaFieldNames) {

        Map<String, MetaFieldValue> metaFieldValues = new HashMap<String, MetaFieldValue>();
        List<String> aits=Arrays.asList(FileConstants.CUSTOMER_INFORMATION_AIT, FileConstants.BUSINESS_INFORMATION_AIT, FileConstants.ACCOUNT_INFORMATION_AIT);
        List<Integer> groupIds=new ArrayList<>();
        enrollmentDTO.getAccountType().getInformationTypes().stream().filter((AccountInformationTypeDTO accountInformationTypeDTO)-> aits.contains(accountInformationTypeDTO.getName())).forEach((AccountInformationTypeDTO accountInformationTypeDTO)->{
            groupIds.add(accountInformationTypeDTO.getId());
        });

        for(MetaFieldValue metaFieldValue:enrollmentDTO.getMetaFields()){
            if(metaFieldValue.getField().getMetaFieldGroups()!=null && metaFieldValue.getField().getMetaFieldGroups().size()>0){
                Integer groupId=metaFieldValue.getField().getMetaFieldGroups().iterator().next().getId();
                if (metaFieldNames.contains(metaFieldValue.getField().getName()) && groupIds.contains(groupId)) {
                    if (metaFieldNames.remove(metaFieldValue.getField().getName())){
                        metaFieldValues.put(metaFieldValue.getField().getName(), metaFieldValue);
                    }
                }
            }
        }

        return metaFieldValues;
    }


}
