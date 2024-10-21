package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.util.stream.Stream;

/**
 * @author Neelabh
 * @since Jan 29, 2019
 */
public class CdrRecordType {

    public enum OptusFixedLineRecord {
        NORMAL("50"), LDLP("51");
        private final String typeCode;

        OptusFixedLineRecord(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public static String[] getTypeCodes() {
            return Stream.of(OptusFixedLineRecord.values()).map(OptusFixedLineRecord::getTypeCode).toArray(String[]::new);
        }
    }

    public enum OptusMobileRecord {
        HOME("10"), ROAM("20"), SMS("30"), CONTENT("40"), DATA("50");
        private final String typeCode;

        OptusMobileRecord(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public static OptusMobileRecord fromTypeCode(String recordType) {
            for(OptusMobileRecord optusMobileRecord : OptusMobileRecord.values()) {
                if(optusMobileRecord.getTypeCode().equals(recordType)) {
                    return optusMobileRecord;
                }
            }
            throw new IllegalArgumentException("Invalid optus mobile record type "+recordType);
        }

        public static String[] getTypeCodes() {
            return Stream.of(OptusMobileRecord.values()).map(OptusMobileRecord::getTypeCode).toArray(String[]::new);
        }
    }

    public enum EnginSConnectRecord {
        LOCAL("1"), NATIONAL("2"), INTERNATIONAL("3"), MOBILE("5"), FREE("6"), ENGIN("7"), SERVICE("11");
        private final String typeCode;

        EnginSConnectRecord(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public static String[] getTypeCodes() {
            return Stream.of(EnginSConnectRecord.values()).map(EnginSConnectRecord::getTypeCode).toArray(String[]::new);
        }
    }

    public enum AAPTVoipCtopRecord {
        PWTDET("PWTDET");
        private final String typeCode;

        AAPTVoipCtopRecord(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public static String[] getTypeCodes() {
            return Stream.of(AAPTVoipCtopRecord.values()).map(AAPTVoipCtopRecord::getTypeCode).toArray(String[]::new);
        }
    }

    public enum TelstraRecord {

        UIR("UIR"),SMS("GSMS"), MMS("GMMS"),GPRS("GPRS"),VIDEO("3GVID"),GTEL("GTEL"),PREMIUM_SMS("GPSMS"),
        BYTE("Byte"),SECOND("Sec"),MSG("Msg");
        private final String typeCode;

        TelstraRecord(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public static String[] getTypeCodes() {
            return Stream.of(TelstraRecord.values()).map(TelstraRecord::getTypeCode).toArray(String[]::new);
        }
    }

    public enum InternetDataUsage {

        UPLOAD("UPL", "Upload"), DOWNLOAD("DWL", "Download"), TOTAL("TTL", "Total");
        private final String usageTypeCode;
        private final String usageTypeName;

        InternetDataUsage(String usageTypeCode, String usageTypeName) {
            this.usageTypeCode = usageTypeCode;
            this.usageTypeName = usageTypeName;
        }

        public String getUsageTypeCode() {
            return usageTypeCode;
        }

        public String getUsageTypeName() {
            return usageTypeName;
        }

        public static String[] getTypeCodes() {
            return Stream.of(InternetDataUsage.values()).map(InternetDataUsage::getUsageTypeCode).toArray(String[]::new);
        }

        public static InternetDataUsage fromUsageCode(String usageCode) {
            for(InternetDataUsage dataUsage : InternetDataUsage.values()) {
                if(dataUsage.getUsageTypeCode().equals(usageCode)) {
                    return dataUsage;
                }
            }
            throw new IllegalArgumentException("Invalid data usage code "+usageCode);
        }

        public static InternetDataUsage fromUsageName(String usageName) {
            for(InternetDataUsage dataUsage : InternetDataUsage.values()) {
                if(dataUsage.getUsageTypeName().equals(usageName)) {
                    return dataUsage;
                }
            }
            throw new IllegalArgumentException("Invalid data usage name "+usageName);
        }
    }

    public enum TelstraMonthlyRecord {

        SED("SED"), OCD("OCD");
        private final String typeCode;

        TelstraMonthlyRecord(String typeCode) {
            this.typeCode = typeCode;
        }


        public String getTypeCode() {
            return typeCode;
        }

        public static TelstraMonthlyRecord fromTypeCode(String recordType) {
            for(TelstraMonthlyRecord telstraMonthlyRecord : TelstraMonthlyRecord.values()) {
                if(telstraMonthlyRecord.getTypeCode().equals(recordType)) {
                    return telstraMonthlyRecord;
                }
            }
            throw new IllegalArgumentException("Invalid telstra monthly record type "+recordType);
        }

        public static String[] getTypeCodes() {
            return Stream.of(TelstraMonthlyRecord.values()).map(TelstraMonthlyRecord::getTypeCode).toArray(String[]::new);
        }
    }


}
