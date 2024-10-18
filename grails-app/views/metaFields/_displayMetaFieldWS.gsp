<%@ page import="com.sapienter.jbilling.server.metafields.DataType" %>
<g:if test="${!metaField.disabled}">
    <g:set var="fieldValue" value="${metaField.getValue()}"/>

    <g:applyLayout name="form/text">
        <content tag="label">${metaField.fieldName}</content>
        <span title="${metaField.fieldName}">
            <g:render template="/metaFields/formatMetaFieldValue" model="[ dataType: metaField?.dataType, fieldValue:fieldValue]"/>
        </span>
    </g:applyLayout>

</g:if>