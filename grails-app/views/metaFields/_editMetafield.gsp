<%@ page import="org.apache.commons.lang.ArrayUtils; com.sapienter.jbilling.server.pricing.db.RouteDTO; com.sapienter.jbilling.server.metafields.validation.ValidationRuleType; org.apache.commons.lang.WordUtils; com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.metafields.DataType;com.sapienter.jbilling.server.metafields.MetaFieldType" %>
%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2013 Enterprise jBilling Software Ltd. and Emiliano Conde

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
  Metafield edit template

  Parameters to be passed to this template:

  1. metaField - MetaFieldWS managed by this template
  2. entityType - entity Type of the metaField
  3. parentId - Id of the parent submit form
  4. metaFieldIdx - (optional) Used when rendering multiple instances of the template in order to provide an unique
        name for input fields. The metaFieldIdx will be appended to all names
  5. allowTypeEditing - (optional) If true the user will be able edit the meta field even though it has been saved.
  6. displayMetaFieldType - (optional) If false the MetaField Type selection will not be displayed

  @author Panche Isajeski
  @since 05/24/2013
--%>

<g:set var="isNew" value="${!metaField || !metaField?.id || metaField?.id <= 0}"/>
<g:set var="metaFieldIdx" value="${metaFieldIdx != null ? metaFieldIdx : ''}"/>
<g:set var="allowTypeChange" value="${allowTypeEditing}"/>
<g:set var="displayMetaFieldType" value="${displayMetaFieldType != null ? displayMetaFieldType : false}"/>
<g:set var="dependentMetaFields"
       value="${metaField.dependentMetaFields != null ? org.apache.commons.lang.ArrayUtils.nullToEmpty(metaField.dependentMetaFields) : ''}"/>
<g:set var="company" value="${com.sapienter.jbilling.server.user.db.CompanyDTO.get(session['company_id'])}"/>

<!-- metafield template -->
<div id="metaFieldModel${metaFieldIdx}" class="form-columns">
    <div class="column">
        <g:hiddenField name="entityType${metaFieldIdx}"
                       value="${isNew ? params.entityType : metaField?.entityType?.name()}"/>
        <g:hiddenField name="metaField${metaFieldIdx}.primary" value="${metaField?.primary}"/>

        <g:applyLayout name="form/text">
            <content tag="label"><g:message code="metaField.label.id"/></content>

            <g:if test="${!isNew}">
                <span>${metaField.id}</span>
            </g:if>
            <g:else>
                <em><g:message code="prompt.id.new"/></em>
            </g:else>

            <g:hiddenField name="metaField${metaFieldIdx}.id" value="${metaField?.id}"/>
        </g:applyLayout>

        <div id="field-name${metaFieldIdx}" class="field-name">
            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="metaField.label.name"/><span id="mandatory-meta-field">*</span>
                </content>
                <content tag="label.for">metaField${metaFieldIdx}.name</content>
                <g:textField class="field" name="metaField${metaFieldIdx}.name" value="${metaField?.name}"/>
            </g:applyLayout>
        </div>

        <div id="field-enumeration${metaFieldIdx}" style="display: none;" class="field-enumeration">
            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="metaField.label.name"/></content>
                <content tag="label.for">name</content>
                <content tag="include.script">true</content>
                <g:select name="metaField${metaFieldIdx}.name" class="field"
                          from="${EnumerationDTO.findAllByEntityId(session['company_id'])}"
                          value="${metaField?.name}"
                          optionKey="name"
                          disabled="true"
                          optionValue="name"/>
            </g:applyLayout>
        </div>

        <g:applyLayout name="form/select">
            <content tag="label"><g:message code="metaField.label.dataType"/></content>
            <content tag="label.for">metaField${metaFieldIdx}.dataType</content>
            <content tag="include.script">true</content>
            <g:set var="dataTypes" value="${DataType.values()}"/>
            <g:select
                    disabled="${!isNew && !allowTypeChange}"
                    class="field"
                    name="metaField${metaFieldIdx}.dataType"
                    from="${dataTypes}"
                    value="${metaField?.dataType}"/>
            <g:if test="${!isNew && !allowTypeChange}">
                <g:hiddenField name="metaField${metaFieldIdx}.dataType" value="${metaField?.dataType}"/>
            </g:if>
        </g:applyLayout>

        <div id="field-filename${metaFieldIdx}" class="field-filename"
             style="${metaField?.dataType.equals(DataType.SCRIPT) ?: 'display: none;'}">
            <g:applyLayout name="form/input">
                <content tag="label">
                    <g:message code="metaField.label.filename"/>
                    <span id="mandatory-meta-field">*</span>
                </content>
                <content tag="label.for">metaField${metaFieldIdx}.filename</content>
                <g:textField class="field" name="metaField${metaFieldIdx}.filename" value="${metaField?.filename}"/>
            </g:applyLayout>
        </div>

        <g:applyLayout name="form/checkbox">
            <content tag="label"><g:message code="metaField.label.mandatory"/></content>
            <content tag="label.for">mandatoryCheck</content>
            <g:checkBox class="cb checkbox" id="mandatoryCheck${metaFieldIdx}" name="metaField${metaFieldIdx}.mandatory"
                        checked="${metaField?.mandatory}" disabled="${metaField?.disabled}"/>
        </g:applyLayout>

        <g:applyLayout name="form/checkbox">
            <content tag="label"><g:message code="metaField.label.disabled"/></content>
            <content tag="label.for">disableCheck</content>
            <g:checkBox class="cb checkbox disableCheck" id="disableCheck${metaFieldIdx}"
                        name="metaField${metaFieldIdx}.disabled" checked="${metaField?.disabled}"/>
        </g:applyLayout>

        <g:applyLayout name="form/input">
            <content tag="label"><g:message code="metaField.label.displayOrder"/></content>
            <content tag="label.for">metaField${metaFieldIdx}.displayOrder</content>
            <g:textField class="field" name="metaField${metaFieldIdx}.displayOrder" value="${metaField?.displayOrder}"/>
        </g:applyLayout>

        <g:applyLayout name="form/input">
            <content tag="label"><g:message code="metaField.label.defaultValue"/></content>
            <content tag="label.for">defaultValue${metaFieldIdx}</content>
            <g:textField class="field" name="defaultValue${metaFieldIdx}" value="${metaField?.defaultValue?.value}"/>
        </g:applyLayout>
    </div>

    <div class="column">
        <div id="field-enumeration" style="">
            <g:if test="${displayMetaFieldType}">
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="metaField.label.fieldType"/></content>
                    <content tag="label.for">fieldType${metaFieldIdx}</content>
                    <content tag="include.script">true</content>
                    <g:set var="fieldTypes" value="${java.util.Arrays.asList(MetaFieldType.values())}"/>
                    <g:set var="fieldTypes" value="${[''] + fieldTypes}"/>
                    <g:select style="height: 20px"
                              class="field"
                              name="fieldType${metaFieldIdx}"
                              from="${fieldTypes}"
                              value="${metaField?.fieldUsage}"/>

                </g:applyLayout>
            </g:if>
        <!-- TODO (pai) Add option to reset field usage selection -->
        </div>

    </div>

    <div class="column">
        <div id="dependency">
            <g:set var="totalDependency"
                   value="${metaField.dependentMetaFields ? metaField.dependentMetaFields.size() : 0}"/>
            <g:if test="${dependencyCheckBox}">
                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="button.show.dependencies"/></content>
                    <content tag="label.for">dependencyCheck</content>
                    <g:checkBox class="cb checkbox" id="dependency-checkbox${metaFieldIdx}" name="dependency-checkbox"
                                checked="${totalDependency > 0}"/>
                </g:applyLayout>
            </g:if>
            <div id="metafield-dependency${metaFieldIdx}">
                <g:set var="unSavedMetafields" value="${unSavedMetafields.findAll { it.id != excludeSelf.id }}"/>
                <g:applyLayout name="form/select_multiple">
                    <content tag="label"><g:message code="bean.MetaFieldWS"/></content>
                    <content tag="label.for">name</content>
                    <content tag="include.script">true</content>
                    <g:select name="dependentMetaFields${metaFieldIdx}" class="field"
                              from="${unSavedMetafields}"
                              value="${metaField.dependentMetaFields ? Arrays.asList(metaField.dependentMetaFields) : ''}"
                              optionKey="id"
                              optionValue="name" multiple="true"/>
                </g:applyLayout>

                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="configuration.menu.route"/></content>
                    <content tag="label.for">name</content>
                    <content tag="include.script">true</content>
                    <g:select name="metaField${metaFieldIdx}.dataTableId" class="field"
                              from="${com.sapienter.jbilling.server.pricing.db.RouteDTO.findAllByCompany(company)}"
                              value="${metaField?.dataTableId}"
                              optionKey="id"
                              noSelection="${['null': 'Select One...']}"
                              optionValue="name"/>
                </g:applyLayout>
            </div>

        </div>

    </div>

    <div class="column">
        <div class="help">
            <g:applyLayout name="form/checkbox">
                <content tag="label"><g:message code="price.strategy.COMMON.pricing.help.tooltip.message"/></content>
                <content tag="label.for">helpCheck</content>
                <g:checkBox class="cb checkbox" id="helpCheck${metaFieldIdx}" name="help-checkbox"
                            checked="${metaField?.helpContentURL ? true : false}"/>
            </g:applyLayout>
            <div id="help-contents${metaFieldIdx}" style="display: none">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="metafield.help.url.label"/></content>
                    <content tag="label.for">metaField${metaFieldIdx}.helpContentURL</content>
                    <g:textField class="field" name="metaField${metaFieldIdx}.helpContentURL"
                                 value="${metaField?.helpContentURL}"/>
                </g:applyLayout>
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="metafield.help.description.label"/></content>
                    <content tag="label.for">metaField${metaFieldIdx}.helpDescription</content>
                    <g:textField class="field" name="metaField${metaFieldIdx}.helpDescription"
                                 value="${metaField?.helpDescription}"/>
                </g:applyLayout>
            </div>
        </div>
    </div>
</div>

<g:render id="validationTemplate" template="/metaFields/validation/validation"
          model="[validationRule: metaField?.validationRule, parentId: parentId, metaFieldIdx: metaFieldIdx, enabled: metaField?.validationRule ? true : false]"/>
<script>
    $(document).ready(function () {
        <g:if test="${totalDependency==0}">
        $("#metafield-dependency${metaFieldIdx}").hide();
        </g:if>

        <g:if test="${metaField?.helpContentURL || metaField?.helpDescription}">
        $("#help-contents${metaFieldIdx}").show();
        $("#help-contents${metaFieldIdx}").closest('.help').find('.checkbox').attr('checked', true);
        </g:if>

        $('#disableCheck${metaFieldIdx}').on('change', function () {
            var mandatory = $("#mandatoryCheck${metaFieldIdx}")
            if ($(this).is(':checked')) {
                mandatory.prop('checked', false);
                mandatory.prop('disabled', true);
            } else {
                mandatory.prop('disabled', false);
            }
        });

        $("#dependency-checkbox${metaFieldIdx}").on("change", function (e) {
            if ($(this).is(":checked")) {
                $("#metafield-dependency${metaFieldIdx}").show();
                if ($("[name='metaField${metaFieldIdx}.dataType']").val() == "ENUMERATION") {
                    $('#field-name${metaFieldIdx}').show().find('input').prop('disabled', '');
                    $('#field-enumeration${metaFieldIdx}').hide().find('select').prop('disabled', 'true');
                    $('#field-filename${metaFieldIdx}').hide().find('input').prop('disabled', 'true')
                }
            } else {
                if ($("[name='metaField${metaFieldIdx}.dataType']").val() == "ENUMERATION") {
                    $('#field-name${metaFieldIdx}').hide().find('input').prop('disabled', 'true');
                    $('#field-enumeration${metaFieldIdx}').show().find('select').prop('disabled', '');
                    $('#field-filename${metaFieldIdx}').show().find('input').prop('disabled', 'true')
                }
                $("#metafield-dependency${metaFieldIdx}").hide();
            }
        });

        $("#helpCheck${metaFieldIdx}").on("change", function (e) {
            if ($(this).is(":checked")) {
                $("#help-contents${metaFieldIdx}").show()
            } else {
                $("input[name='metaField${metaFieldIdx}.helpContentURL']").val('')
                $("input[name='metaField${metaFieldIdx}.helpDescription']").val('')
                $("#help-contents${metaFieldIdx}").hide()
            }
        })

    });
</script>
