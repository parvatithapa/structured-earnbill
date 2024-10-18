%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.util.db.EnumerationDTO" %>

<g:set var="enumerations" value="${EnumerationDTO.createCriteria().list(){eq('entity', new CompanyDTO(session['company_id']))}}"/>
<g:set var="enumValues" value="${null}"/>
<%
    for (EnumerationDTO dto : enumerations) {
        if (dto.name == field.getName()) {
            enumValues= []
            enumValues.addAll(dto.values.collect {it.value})
        }
    }
%>
<g:applyLayout name="form/select_multiple">
    <content tag="label"><g:message code="${field.name}"/><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></content>
    <content tag="label.for">metaField_${field.id}.value</content>
    <g:select
            class="field ${validationRules} ${field.fieldUsage ? 'field_usage':''}"
            name="metaField_${field.id}.value"
            from="${enumValues}"
            optionKey=""
            value="${fieldValue}"
            multiple="true"/>
</g:applyLayout>
