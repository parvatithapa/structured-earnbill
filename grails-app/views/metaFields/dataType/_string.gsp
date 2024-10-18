<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldType;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.CountryList;" %>
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
<g:if test="${field.fieldUsage?.equals(MetaFieldType.COUNTRY_CODE)}">
    <g:applyLayout name="form/select">
        <content tag="label">
            <g:message code="${field.name}"/>
            <g:if test="${field.mandatory}">
                <span id="mandatory-meta-field">*</span>
            </g:if>
        </content>
        <content tag="label.for">metaField_${field.id}.value</content>
        <g:select         name = "metaField_${field.id}.value"
                          from = "${new CountryList().getCountries(session['language_id'])}"
                   noSelection = "['': message(code: 'default.no.selection')]"
                     optionKey = "key"
                   optionValue = "value"
                   value="${fieldValue}"/>

        <g:render template="/metaFields/metaFieldHelp" model="[field:field]" />
    </g:applyLayout>
</g:if>
<g:else>
    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="${field.name}"/><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></content>
        <content tag="label.for">metaField_${field.id}.value</content>

        <g:textField name="metaField_${field.id}.value"
                     class="field text ${field.fieldUsage ? 'field_usage':''}"
                     value="${fieldValue}"/>

       <g:render template="/metaFields/metaFieldHelp" model="[field:field]" />
    </g:applyLayout>
</g:else>