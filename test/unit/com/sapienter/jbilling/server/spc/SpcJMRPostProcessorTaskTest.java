/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

/**
 *
 * @author Ashwinkumar
 * @since 20-05-2021
 */
package com.sapienter.jbilling.server.spc;

import org.junit.Test;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import junit.framework.TestCase;

public class SpcJMRPostProcessorTaskTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testReducePricingFields_for_optus_mobile_pricing_fields() {
        SpcJMRPostProcessorTask task = new SpcJMRPostProcessorTask();
        JbillingMediationRecord record = getJbillingMediationRecord("om");
        String serviceNumber = record.getPricingFieldValueByName("SERVICE_NUMBER");
        task.reducePricingFields(record);
        String pricingFields = record.getPricingFields();        
        assertFalse("pricing fields should not contain CDR_IDENTIFIER",pricingFields.contains("CDR_IDENTIFIER"));
        assertTrue("pricing fields should contain SERVICE_NUMBER",pricingFields.contains("SERVICE_NUMBER"));
        String serviceNumberAfterReduce = record.getPricingFieldValueByName("SERVICE_NUMBER");
        assertEquals("value of SERVICE_NUMBER should match before and after reducing",serviceNumber, serviceNumberAfterReduce);
    }

    @Test
    public void testReducePricingFields_for_telstra_mobile_pricing_fields() {
        SpcJMRPostProcessorTask task = new SpcJMRPostProcessorTask();
        JbillingMediationRecord record = getJbillingMediationRecord("tm");
        String serviceNumber = record.getPricingFieldValueByName("SERVICE_NUMBER");
        task.reducePricingFields(record);
        String pricingFields = record.getPricingFields();        
        assertFalse("pricing fields should not contain CDR_IDENTIFIER",pricingFields.contains("CDR_IDENTIFIER"));
        assertTrue("pricing fields should contain SERVICE_NUMBER",pricingFields.contains("SERVICE_NUMBER"));
        String serviceNumberAfterReduce = record.getPricingFieldValueByName("SERVICE_NUMBER");
        assertEquals("value of SERVICE_NUMBER should match before and after reducing",serviceNumber, serviceNumberAfterReduce);
    }

    private JbillingMediationRecord getJbillingMediationRecord(String serviceType) {
        JbillingMediationRecord jmr = new JbillingMediationRecord();
        if( "om".equalsIgnoreCase(serviceType)) {
            jmr.setPricingFields("CDR_IDENTIFIER:1:string:50,SERVICE_NUMBER:1:string:0428396669,EVENT_DATE:1:string:20210304145605,POINT_TARGET:1:string:0,DIRECTION_FLAG:1:string:2,CALLED_PLACE:1:string:Lismore,PRODUCT_PLAN_CODE:1:string:SPA2DINT,TOTAL_CHARGES:1:string:00000000000,USAGE_IDENTIFIER:1:string:G1999989,DATA_CHARGING_METHOD:1:string:V,NETWORK_TYPE:1:string:00,PEAK_USAGE:1:string:27310015,OFF_PEAK_USAGE:1:string:0,OTHER_USAGE:1:string:0,CHARGE_OVERRIDE:1:string:,SESSION_ID:1:string:042839666,CODE_STRING:1:string:50%3ASPA2DINT%3A00%3A2,SERVICE_TYPE:1:string:om,PURCHASE_ORDER_ID:1:integer:740800,PLAN_ID:1:string:588,asset+number:1:string:0428396669,TARIFF_CODE:1:string:OM%3AGPRS,CALL_CHARGE:1:string:0E-10");
        } else if ("tm".equalsIgnoreCase(serviceType)) {
            jmr.setPricingFields("PART_TYPE:1:string:P1,P1_S_P_NUMBER_EC_ADDRESS:1:string:61408827763,P1_S_P_NUMBER_EC_NETWORK_PKEY:1:string:HHHHH,P1_S_P_NUMBER_EC_NUMBERING_PLAN_PKEY:1:string:E.164,P1_S_P_NUMBER_EC_TYPE_OF_NUMBER:1:string:1,P1_S_P_NUMBER_EC_USER_PROFILE_ID:1:string:0,P1_S_P_PORT_EC_ADDRESS:1:string:505015602749038,P1_S_P_PORT_EC_NUMBERING_PLAN_PKEY:1:string:IMSI,P1_NET_ELEMENT_EC_ADDRESS:1:string:cjprvocc01,P1_NET_ELEMENT_EC_NETWORK_PKEY:1:string:HHHHH,P1_NET_ELEMENT_EC_NUMBERING_PLAN_PKEY:1:string:E.164,P1_O_P_NUMBER_EC_ADDRESS:1:string:telstra.wap,P1_O_P_NUMBER_EC_NUMBERING_PLAN_PKEY:1:string:APN,P1_O_P_NUMBER_EC_TYPE_OF_NUMBER:1:string:3,P1_O_P_NUMBER_EC_USER_PROFILE_ID:1:string:0,P1_RECORD_IDENTIFICATION_EC_EXT_ORIGIN:1:string:CS,P1_CALL_DEST_EC_CALL_DEST:1:string:IP,P1_HOME_NETWORK_EC_COUNTRY_CODE:1:string:61,P1_HOME_NETWORK_EC_NETWORK_CODE:1:string:1001,P1_HOME_NETWORK_EC_NETWORK_PKEY:1:string:HHHHH,P1_O_P_NORMED_NUMBER_EC_ADDRESS:1:string:telstra.wap,P1_O_P_NORMED_NUMBER_EC_NUMBERING_PLAN_PKEY:1:string:APN,P1_BASIC_CALL_TYPE_INFO_EC_CALL_TYPE:1:string:11,P1_BASIC_CALL_TYPE_INFO_EC_CHARGING_CHARACTERISTICS:1:string:N,P1_ROUTING_INFO_EC_IMPORT_EXPORT_MODE:1:string:B,P1_ROUTING_INFO_EC_ROUTING_PRIORITY:1:string:500,P1_INITIAL_START_TIME_EC_TIME_OFFSET:1:string:39600,P1_INITIAL_START_TIME_EC_TIMESTAMP:1:string:20201114133810,P1_SERVICE_CLASS:1:string:4222,P1_UDR_REF_NUM:1:string:54942839,P1_RECORD_IDENTIFICATIONRERATE_ID:1:string:20030102,For+future+Use+P1+32:1:string:,For+future+Use+P1+33:1:string:,For+future+Use+P1+34:1:string:,For+future+Use+P1+35:1:string:,For+future+Use+P1+36:1:string:,For+future+Use+P1+37:1:string:,For+future+Use+P1+38:1:string:,For+future+Use+P1+39:1:string:,End+of+P1:1:string:%23,Start+Of+P2:1:string:P2,P2_CALL_TYPE_INFO_EC_CALL_TYPE:1:string:11,P2_CALL_TYPE_INFO_EC_FOLLOW_UP_CALL_TYPE:1:string:1,P2_CALL_TYPE_INFO_EC_XFILE_IND:1:string:H,P2_CAMEL_DESTINATION_EC_ADDRESS:1:string:,P2_CAMEL_DESTINATION_EC_NUMBERING_PLAN_PKEY:1:string:,P2_CAMEL_DESTINATION_EC_TYPE_OF_NUMBER:1:string:,P2_CAMEL_SERVER_ADDRESS_EC_ADDRESS:1:string:,P2_CAMEL_SERVER_ADDRESS_EC_NUMBERING_PLAN_PKEY:1:string:,P2_CAMEL_SERVER_ADDRESS_EC_TYPE_OF_NUMBER:1:string:,P2_CAUSE_FOR_FORWARD_EC_CAUSE_FOR_FORWARD:1:string:,P2_DATA_VOLUME_EC_VOLUME:1:string:19293,P2_DATA_EC_UOM_PKEY:1:string:Byte,P2_DURATION_EC_VOLUME:1:string:,P2_DURATION_EC_UOM_PKEY:1:string:,P2_MESSAGES_EC_VOLUME:1:string:,P2_MESSAGES_EC_UOM_PKEY:1:string:,P2_REMARK_EC_REMARK:1:string:,P2_ROUTING_NUMBER_EC_ADDRESS:1:string:,P2_ROUTING_NUMBER_EC_NUMBERING_PLAN_PKEY:1:string:,P2_S_P_LOCATION_EC_ADDRESS:1:string:Yetholme,P2_S_P_LOCATION_EC_NUMBERING_PLAN_PKEY:1:string:ALL,P2_SERVICE_EC_ACTION_CODE:1:string:I,P2_SERVICE_EC_VAS_CODE:1:string:,P2_START_TIME_EC_TIME_OFFSET:1:string:39600,P2_START_TIME_EC_TIMESTAMP:1:string:20201114133810,P2_TARIFF_INFO_EC_SN_PKEY:1:string:GPRS,P2_TARIFF_INFO_EC_USAGE_IND_PKEY:1:string:OUT,P2_TARIFF_INFO_EC_ZN_PKEY:1:string:W0061,P2_TARIFF_INFO_EC_ZP_PKEY:1:string:CAALL,P2_TECHNICAL_INFO_EC_PARTIAL_REC_TYPE:1:string:S,P2_TECHNICAL_INFO_EC_PREPAY_IND:1:string:N,P2_TECHNICAL_INFO_EC_SC_PKEY:1:string:GSM,P2_TECHNICAL_INFO_EC_SCCODE:1:string:1,P2_TECHNICAL_INFO_EC_TERMINATION_IND:1:string:0,P2_TECHNICAL_INFO_WS_PREPAY_IND:1:string:,P2_TARIFF_INFO_EC_ZN_WHLSL_PKEY:1:string:,P2_TARIFF_INFO_EC_ZP_WHLSL_PKEY:1:string:,For+future+use+P2+40:1:string:,P2_REMARK_EC_REMARK_REASON_ID:1:string:,P2_REMARK_EC_REMARK_REASON_PKEY:1:string:,End+of+P2:1:string:%23,Start+of+P3:1:string:P3,P3_RATED_VOLUME_EC_UOM_PKEY:1:string:Byte,P3_RATED_VOLUME_EC_VOLUME:1:string:19456,P3_ROUNDED_VOLUME_EC_UOM_PKEY:1:string:Byte,P3_ROUNDED_VOLUME_EC_VOLUME:1:string:19456,P3_RATED_FLAT_AMOUNT_EC_AMOUNT:1:string:0.000000,P3_RATED_FLAT_AMOUNT_EC_CURRENCY_PKEY:1:string:AUD,P3_RATED_FLAT_AMOUNT_EC_DISCOUNTED_AMOUNT:1:string:0.000000,P3_RATED_FLAT_AMOUNT_EC_GROSS_IND:1:string:1,P3_TARIFF_INFO_DETAIL_EC_CHARGEABLE_QUANTITY:1:string:37,P3_TARIFF_INFO_DETAIL_EC_INTERCONNECT_IND:1:string:N,P3_TARIFF_INFO_DETAIL_EC_LOGICAL_QUAN_PKEY:1:string:DATAV,P3_TARIFF_INFO_DETAIL_EC_RATE_TYPE_PKEY:1:string:BASE,P3_TARIFF_INFO_DETAIL_EC_RTX_CHARGE_TYPE:1:string:I,P3_TARIFF_INFO_DETAIL_EC_TT_PKEY:1:string:TWTT000001,P3_START_TIME_CHARGE_EC_TIME_OFFSET:1:string:39600,P3_START_TIME_CHARGE_EC_TIMESTAMP:1:string:20201114133810,P3_RATED_FLAT_AMOUNT_ORIG_EC_AMOUNT:1:string:0.000000,P3_RATED_FLAT_AMOUNT_ORIG_EC_DISCOUNTED_AMOUNT:1:string:0.000000,P3_RATED_FLAT_AMOUNT_ORIG_EC_GROSS_IND:1:string:1,P3_CHARGE_INFO_EC_CASH_FLOW_DIRECTION:1:string:0,P3_CHARGE_INFO_EC_DISABLE_TAX:1:string:0,P3_TARIFF_INFO_EC_ZN_PKEY:1:string:,Spare+2:1:string:,Spare+3:1:string:,Spare+4:1:string:,Spare+5:1:string:,Spare+6:1:string:,Spare+7:1:string:%23,End+of+P3:1:string:P4,Start+Of+P4:1:string:U,P4_FREE_UNITS_INFO_EC_DISCOUNT_TYPE:1:string:S,P4_FREE_UNITS_INFO_EC_PART_CREATOR:1:string:Byte,P4_FREE_ROUNDED_VOLUME_EC_UOM_PKEY:1:string:19456,P4_FREE_ROUNDED_VOLUME_EC_VOLUME:1:string:Byte,P4_FREE_RATED_VOLUME_EC_UOM_PKEY:1:string:19456,P4_FREE_RATED_VOLUME_EC_VOLUME:1:string:0.000000,P4_FREE_CHARGE_EC_AMOUNT:1:string:AUD,P4_FREE_CHARGE_EC_CURRENCY_PKEY:1:string:0,P4_FREE_CHARGE_EC_TAX:1:string:4270240,P4_BALANCE_AUDIT_DATA_EC_ACCOUNT_ID:1:string:D,P4_BALANCE_AUDIT_DATA_EC_ACCOUNT_TYPE:1:string:,P4_BALANCE_AUDIT_DATA_EC_PURCHASE_SEQ_NO:1:string:,NA:1:string:%23,End+of+P4:1:string:,CODE_STRING:1:string:W0061%3A%3AGPRS%3A11,SERVICE_TYPE:1:string:telstraMobile4G,PURCHASE_ORDER_ID:1:integer:737700,PLAN_ID:1:string:962,SERVICE_NUMBER:1:string:61408827763,asset+number:1:string:61408827763,TARIFF_CODE:1:string:TM%3AGPRS,CALL_CHARGE:1:string:0.000000");
        }

        return jmr;
    }

}
