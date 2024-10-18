<%@ page import="com.sapienter.jbilling.common.Constants;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.DataType;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.db.MetaField;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.db.MetaFieldValue;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDAS;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO;" %>
<%@ page import="org.apache.commons.lang.StringUtils;" %>
<%@ page import="org.apache.commons.lang.WordUtils;" %>
<%@ page import="com.sapienter.jbilling.common.Util;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldHelper;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldType;" %>

%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%--
  View template for rendering meta-field input's to create and update meta-field values..

  <g:render template="/metaFields/editMetaFields" model="[availableFields: availableFields, fieldValues: object.metaFields]"/>

  @author Brian Cowdery
  @since 26-Oct-2011
--%>
%{--Group by company so that company name can be displayed before meta fields and easy to identify them --}%
<% Map<Integer, MetaField> mfByEntity = availableFields?.groupBy({ metaField -> metaField.entityId }); %>

<g:each var="groupedMetaFields" in="${mfByEntity}">
%{--Display company name if meta fields are from multiple companies--}%
    <g:if test="${mfByEntity.size() > 1}">
        <div class="msg-box text-success">
            ${new CompanyDAS().find(groupedMetaFields?.key)?.description}
        </div>
    </g:if>
    <g:each var="field" in="${groupedMetaFields?.value?.sort { it.displayOrder }}">
        <g:set var="metaFieldTemplateName"
               value="${WordUtils.uncapitalize(WordUtils.capitalizeFully(field.getDataType().name(), ['_'] as char[]).replaceAll('_', ''))}"/>
        <g:set var="fieldName" value="${StringUtils.abbreviate(message(code: field.name), 50)}"/>
    %{--
        To compare meta field, need to compare name, group id and entity Id too. It may possible that
        two child companies can have same name meta field. Group id is only significant in case of AIT meta fields.
    --}%

        <g:set var="fieldValue" value="${fieldValues?.find {
            (it.metaField.name == field.name) &&
                    ((it.groupId && groupId && it.groupId == groupId) || (!it.groupId && !groupId)) &&
                    (field?.entityId == null || it.metaField.entityId == null || field?.entityId == it.metaField.entityId)
        }?.getValue()}"/>

        <g:if test="${fieldValue == null && field.getDefaultValue()}">
            <g:set var="fieldValue" value="${field.getDefaultValue().getValue()}"/>
        </g:if>
        <g:elseif test="${g.ifValuePresent(field: field, fieldsArray: fieldsArray)}">
            <g:set var="fieldValue" value="${g.setFieldValue(field: field, fieldsArray: fieldsArray)}"/>
        </g:elseif>
        <g:if test="${fieldName == Constants.METAFIELD_NAME_CC_NUMBER}">
            <g:if test="${(!user?.id || user?.id < 0 || !fieldValues?.getAt(0)?.id || fieldValues?.getAt(0)?.id == null) && !isObscure}">
                <g:set var="fieldValue" value="${fieldValue ? new String(fieldValue) : ""}"/>
            </g:if>
            <g:elseif test="${user?.id > 0}">
                <g:set var="fieldValue"
                       value="${fieldValue ? new String(fieldValue)?.replaceAll('^\\d{12}', '************') : ""}"/>
            </g:elseif>
        </g:if>
        <g:if test="${MetaFieldHelper.isValueOfType(field?.id, MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED)}">
            <g:if test="${!user?.id || user?.id < 0 || !fieldValues?.getAt(0)?.id}">
                <g:set var="fieldValue" value="${fieldValue ? new String(fieldValue) : ""}"/>
            </g:if>
            <g:elseif test="${user?.id > 0}">
                <g:set var="fieldValue"
                       value="${fieldValue ? Util.getObscuredCardNumber(new String(fieldValue)?.toCharArray()) : ""}"/>
            </g:elseif>
        </g:if>
        <g:hiddenField name="metaFieldId" value="${field?.id}"/>
        <g:if test="${!field.disabled}">
            <g:render id="dataTypeTemplate"
                      template="/metaFields/dataType/${metaFieldTemplateName}"
                      model="[field          : field,
                              fieldName      : fieldName,
                              fieldValue     : fieldValue,
                              validationRules: validationRules]"/>
        </g:if>
        <g:else>
            <g:hiddenField name="metaField_${field.id}.value" value="${fieldValue}"/>
        </g:else>
    </g:each>
</g:each>