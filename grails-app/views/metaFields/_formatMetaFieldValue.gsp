<%@ page import="com.fasterxml.jackson.databind.ObjectMapper; com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.server.metafields.DataType; com.sapienter.jbilling.server.metafields.MetaFieldType; com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.common.Util;" %>
<%@ page import ="com.sapienter.jbilling.server.metafields.validation.ValidationRuleType" %>
<g:if test="${fieldValue != null}">
    <g:if test="${fieldType == MetaFieldType.PAYMENT_CARD_NUMBER}">
        <g:set var="card" value="${instrument?.metaFields.grep{it.field?.fieldUsage?.equals(MetaFieldType.PAYMENT_CARD_NUMBER)}}"/>
        <g:if test="${card?.field?.validationRule?.ruleType[0]?.toString().equals(ValidationRuleType.PAYMENT_CARD_OBSCURED.toString())}">
            <g:set var="creditCardNumber" value="${Util.getObscuredCardNumberNew(fieldValue)}"/>
     </g:if>
     <g:else>
          <g:set var="creditCardNumber" value="${Util.getObscuredCardNumber(fieldValue)}"/>
     </g:else>
        ${creditCardNumber}
    </g:if>
    <g:elseif test="${fieldType == MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED}">
        <g:set var="bankAccountNumberEncrypted"
               value="${Util.getObscuredCardNumber(new String(fieldValue)?.toCharArray())}"/>
        ${bankAccountNumberEncrypted}
    </g:elseif>
    <g:else>
        <g:if test="${dataType == DataType.DECIMAL}">
            <g:set var="formatName"
                   value="${fieldName.equals(FileConstants.CUSTOMER_RATE_METAFIELD_NAME) || fieldName.equals(FileConstants.CUSTOMER_SPECIFIC_RATE) || fieldName.equals(FileConstants.ADDER_FEE_METAFIELD_NAME) ? 'price.format' : 'decimal.format'}"/>
            <g:if test="${formatName.equals("price.format")}">
                <g:formatNumber number="${fieldValue}" maxFractionDigits="5"/>
            </g:if>
            <g:else>
                <g:formatNumber number="${fieldValue}" formatName="${formatName}"/>
            </g:else>
        </g:if>
        <g:elseif test="${dataType == DataType.CHAR}">
            ${new String(fieldValue)}
        </g:elseif>
        <g:elseif test="${dataType == DataType.DATE}">
            <g:formatDate date="${fieldValue}" formatName="date.pretty.format"/>
        </g:elseif>
        <g:elseif test="${dataType == DataType.LIST}">
            ${fieldValue?.join(', ')}
        </g:elseif>
        <g:elseif test="${dataType == DataType.JSON_OBJECT}">
            <%
                com.fasterxml.jackson.databind.ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> mapObject = mapper.readValue(fieldValue != null ? fieldValue : '{}', new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                });
            %>

            <g:each in="${mapObject}" var="object">
                <br/>

                ${object.key} : ${object.value}
            </g:each>

        </g:elseif>
        <g:else>
            <g:if test="${fieldType == MetaFieldType.PHONE_NUMBER}">
                <g:formatPhoneNumber number="${fieldValue}" companyId="${companyId}"/>
            </g:if>
            <g:else>
                ${fieldValue != null ? fieldValue : ''}
            </g:else>
        </g:else>
    </g:else>
</g:if>
