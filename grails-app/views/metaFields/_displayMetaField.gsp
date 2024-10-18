<%@ page import="com.sapienter.jbilling.server.metafields.DataType" %>
<g:if test="${!metaField.field.disabled}">
    <g:set var="fieldValue" value="${metaField.getValue()}"/>

    <g:applyLayout name="form/text">
        <content tag="label">${metaField.field.name}</content>
        <span title="${metaField.field.name}">
            <g:render template="/metaFields/formatMetaFieldValue" model="[fieldType:metaField?.field?.fieldUsage, dataType: metaField?.field?.dataType, fieldValue:fieldValue, companyId:companyId]"/>
        </span>
    </g:applyLayout>

</g:if>