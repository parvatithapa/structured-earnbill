package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS

/**
 * @author Vojislav Stanojevikj
 * @since 23-Sep-2016.
 */
class BuilderHelper {

    private BuilderHelper(){}

    private static final String META_FIELD_NAME = 'test.email'
    private static final String META_FIELD_DEFAULT_VALUE = 'test@test.com'
    private static final String VALIDATION_RULE_ERROR_MESSAGE = 'InvalidEmail!'
    private static final String RULE_ATTRIBUTE_KEY = 'validationScript'
    private static final String RULE_ATTRIBUTE_VALUE = '_this==~/[_A-Za-z0-9-]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})/'
    private static final String RULE_TYPE = 'SCRIPT'

    final static def mapper
    final static def random

    static {
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        random = new Random()
    }

    // JSON Strings
    public static final String buildEmailMetaFieldJson(id, EntityType entityType){

        StringBuilder sb = new StringBuilder("{")
        sb.append(""""dataType":"${DataType.STRING.name()}",""")
            .append(""""disabled":false,""")
            .append(""""primary":false,""")
            .append(""""mandatory":false,""")
            .append(""""displayOrder":1,""")
            .append(""""entityId":1,""")
            .append(""""entityType":"${entityType.name()}",""")
            .append(""""fieldUsage":"${MetaFieldType.EMAIL.name()}",""")
            .append(""""id":${id},""")
            .append(""""name":"${META_FIELD_NAME}",""")
            .append(""""defaultValue":{""")
            .append(""""fieldName":"${META_FIELD_NAME}",""")
            .append(""""dataType":"${DataType.STRING.name()}",""")
            .append(""""mandatory":false,""")
            .append(""""disabled":false,""")
            .append(""""stringValue":"${META_FIELD_DEFAULT_VALUE}"},""")
            .append(""""validationRule":{"enabled":true,"id":0,""")
            .append(""""ruleAttributes":{"${RULE_ATTRIBUTE_KEY}":"${RULE_ATTRIBUTE_VALUE}"},""")
            .append(""""ruleType":"${RULE_TYPE}",""")
            .append(""""errorMessages":[${buildInternationalDescriptionJson(VALIDATION_RULE_ERROR_MESSAGE,
                    'errorMessage')}]}}""")

        sb.toString()
    }

    public static final String buildInternationalDescriptionJson(content, label){

        StringBuilder sb = new StringBuilder('{')
        sb.append(""""content":"${content}",""")
            .append(""""deleted":false,""")
            .append(""""label":"${label}",""")
            .append(""""languageId":1,""")
            .append(""""psudoColumn":"${label}"}""")

        sb.toString()
    }

    public static final String buildMainSubscriptionJson(periodId = Integer.valueOf(1),
                                                         nextInvoiceDayOfPeriod = Integer.valueOf(1)){
        StringBuilder sb = new StringBuilder('{')
        sb.append(""""periodId":${periodId},""")
            .append(""""nextInvoiceDayOfPeriod":${nextInvoiceDayOfPeriod}}""")

        sb.toString()
    }

    public static final String buildAssetJsonStringArray(id, int... ids){
        StringBuilder sb = new StringBuilder("[${buildAssetJsonString(id, random.nextInt(Integer.MAX_VALUE), random.nextInt(Integer.MAX_VALUE))}")
        ids.each {
            sb.append(",${buildAssetJsonString(it, random.nextInt(Integer.MAX_VALUE), random.nextInt(Integer.MAX_VALUE))}")
        }
        sb.append(']').toString()
    }

    public static final String buildAssetJsonString(id, itemId, orderLineId){

        StringBuilder sb = new StringBuilder('{')
        sb.append(""""id":${id},""")
                .append(""""identifier": "AssetId-${id}",""")
                .append(""""status": "DEFAULT",""")
                .append(""""assetStatusId": 0,""")
                .append(""""itemId": ${itemId},""")
                .append(""""orderLineId": ${orderLineId},""")
                .append(""""deleted": 0,""")
                .append(""""notes": "Test-Notes",""")
                .append(""""entityId": 1,""")
                .append(""""global": true,""")
                .append(""""metaFieldsMap": {},""")
                .append(""""entities": [1],""")
                .append(""""assignments":[${buildAssetAssignment(random.nextInt(Integer.MAX_VALUE),
                random.nextInt(Integer.MAX_VALUE), random.nextInt(Integer.MAX_VALUE))}]}""").toString()
    }

    public static final String buildAssetAssignment(assetId, orderId, orderLineId){
        StringBuilder sb = new StringBuilder('{')
        sb.append(""""assetId": ${assetId},""")
                .append(""""orderId": ${orderId},""")
                .append(""""orderLineId": ${orderLineId},""")
                .append(""""startDatetime": 1475115230760,""")
                .append(""""endDatetime": 1475135221730}""").toString()
    }

    public static final String buildFlatPriceJson(rate){
        StringBuilder sb = new StringBuilder('{')
        sb.append(""""type": "FLAT",""")
                .append(""""rate": ${rate},""")
                .append(""""attributes":{},""")
                .append(""""currencyId": 1}""")
    }

    public static final String buildPaymentJson(int id, int userId){
        """{
            "userId": ${userId},
            "method": "Cheque",
            "invoiceIds": [],
            "paymentId": null,
            "isRefund": 0,
            "paymentDate": 1153864800000,
            "currencyId": 1,
            "id": ${id},
            "isPreauth": 0,
            "attempt": 1,
            "createDatetime": 1153899889443,
            "updateDatetime": 1166695498113,
            "deleted": 1,
            "resultId": 4,
            "paymentNotes": null,
            "paymentPeriod": null,
            "metaFields": [],
            "paymentInstruments":
            [
                {
                    "id": 1008,
                    "userId": null,
                    "processingOrder": 1,
                    "paymentMethodTypeId": 8,
                    "paymentMethodId": null,
                    "metaFields":
                    [
                        {
                            "fieldName": "cheque.number",
                            "groupId": null,
                            "disabled": false,
                            "mandatory": true,
                            "dataType": "STRING",
                            "defaultValue": null,
                            "displayOrder": 2,
                            "id": 210227,
                            "stringValue": "123-123-123",
                            "dateValue": null,
                            "booleanValue": null,
                            "integerValue": null,
                            "listValue": null,
                            "decimalValue": null
                        },
                        {
                            "fieldName": "cheque.bank.name",
                            "groupId": null,
                            "disabled": false,
                            "mandatory": true,
                            "dataType": "STRING",
                            "defaultValue": null,
                            "displayOrder": 1,
                            "id": 210229,
                            "stringValue": "Gloin's Gold",
                            "dateValue": null,
                            "booleanValue": null,
                            "integerValue": null,
                            "listValue": null,
                            "decimalValue": null
                        }
                    ]
                }
            ],
            "userPaymentInstruments": [],
            "authorization": null,
            "balance": 0,
            "amount": 20,
            "paymentMethodId": 1
        }"""
    }

    public static final String buildPaymentJsonStringArray(id, userId, int... ids){
        StringBuilder sb = new StringBuilder("[${buildPaymentJson(id, userId)}")
        ids.each {
            sb.append(",${buildPaymentJson(it, userId)}")
        }
        sb.append(']').toString()
    }

    // WS
    public static MetaFieldWS buildEmailMetaField(id, EntityType entityType){
        MetaFieldWS metaField = new MetaFieldWS()
        metaField.id = id
        metaField.entityId = Integer.valueOf(1)
        metaField.name = META_FIELD_NAME
        metaField.entityType = entityType
        metaField.dataType = DataType.STRING
        metaField.fieldUsage = MetaFieldType.EMAIL
        metaField.defaultValue = new MetaFieldValueWS(META_FIELD_NAME, null, DataType.STRING, false, META_FIELD_DEFAULT_VALUE)
        ValidationRuleWS validationRule = new ValidationRuleWS()
        validationRule.addErrorMessage(Integer.valueOf(1), VALIDATION_RULE_ERROR_MESSAGE)
        validationRule.addRuleAttribute(RULE_ATTRIBUTE_KEY, RULE_ATTRIBUTE_VALUE)
        validationRule.ruleType = RULE_TYPE
        metaField.validationRule = validationRule

        metaField
    }

    public static buildWSMocks(String jsonString, Class aClass){
        mapper.readValue(jsonString, mapper.getTypeFactory().constructCollectionType(List, aClass))
    }

    public static buildWSMock(String jsonString, Class aClass){
        mapper.readValue(jsonString, aClass)
    }

}
