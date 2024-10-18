%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.AssetStatusDTO;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.ItemDTO;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.DataType;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO;" %>

<%--
  Meta Field filter used in asset selection.

  @author Gerhard Maree
  @since 24-April-2011
--%>

<g:set var="dateValue" value="${ (params['filterByMetaFieldId'+metaFieldIdx] != null && params['filterByMetaFieldId'+metaFieldIdx].contains(DataType.DATE.toString())) ? params['filterByMetaFieldValue'+metaFieldIdx] : null}" />

<div id="mf-row-${assetFlow}-${metaFieldIdx}" class="row">
    <span class="select6">
        <g:applyLayout name="form/select_holder">
            <content tag="label.for">filterByMetaFieldId${metaFieldIdx}</content>
            <content tag="include.script">true</content>
            <g:select          id = "mf-id-${assetFlow}-${metaFieldIdx}"
                             name = "filterByMetaFieldId${metaFieldIdx}"
                             from = "${assetMetaFields}"
                        optionKey = "${{it.id+":"+it.dataType}}"
                      optionValue = "name"
                            value = "${params.filterByMetaFieldId}"/>
        </g:applyLayout>
    </span>
    <div id="mf-val-div-${assetFlow}-${metaFieldIdx}" class="inp-bg valuemarker-div-${assetFlow}-${metaFieldIdx}">
        <g:textField          id = "mf-val-${assetFlow}-${metaFieldIdx}"
                            name = "filterByMetaFieldValue${metaFieldIdx}"
                           class = "field default valuemarker-${assetFlow}-${metaFieldIdx}"
                     placeholder = "${message(code: 'assets.filter.by.metafield.default')}"
                           value = "${params.filterByMetaFieldValue}"/>
    </div>

    <g:each in="${assetMetaFields}" var="mf">
        <g:if test="${mf.dataType in [DataType.ENUMERATION, DataType.LIST]}">
            <g:set var="mfEnum" value="${EnumerationDTO.findByNameAndEntityId(mf.name, mf.entityId)}" />

            <g:set var="enumValues" value="${mfEnum.values.collect {it.value}}"/>

            <div id="mf-val-${mf.id}-div-${assetFlow}-${metaFieldIdx}" class="inp-bg-inv valuemarker-div-${assetFlow}-${metaFieldIdx}">
                <g:applyLayout name="form/select_holder">
                    <content tag="label.for">filterByMetaFieldValue${metaFieldIdx}</content>
                    <content tag="include.script">true</content>
                    <g:select          id = "mf-val-${mf.id}-${assetFlow}-${metaFieldIdx}"
                                    class = "field valuemarker-${assetFlow}-${metaFieldIdx}"
                                     name = "filterByMetaFieldValue${metaFieldIdx}"
                                     from = "${enumValues}"
                                optionKey = ""
                              noSelection = "['':'Please select a value']" />
                </g:applyLayout>
            </div>
        </g:if>

        <g:if test="${mf.dataType in [DataType.BOOLEAN]}">
            <g:checkBox    id = "mf-val-${mf.id}-${assetFlow}-${metaFieldIdx}"
                        class = "cb checkbox valuemarker-${assetFlow}-${metaFieldIdx}"
                         name = "filterByMetaFieldValue${metaFieldIdx}" />
        </g:if>

        <g:if test="${mf.dataType in [DataType.DATE]}">
            <div id="mf-val-${mf.id}-div-${assetFlow}-${metaFieldIdx}" class="inp-bg icon-date-picker valuemarker-div-${assetFlow}-${metaFieldIdx}">
                <g:textField    id = "mf-val-${mf.id}-${assetFlow}-${metaFieldIdx}"
                             class = "field valuemarker-${assetFlow}-${metaFieldIdx} mfdate-${assetFlow}-${metaFieldIdx}"
                              name = "filterByMetaFieldValue${metaFieldIdx}"
                             value = "${dateValue}"/>
            </div>
            <g:if test="${initDatePicker}">
                <script type="text/javascript">
                    // wait to initialize the date picker if it's not visible
                    setTimeout(
                            function() {
                                var options = $.datepicker.regional['${session.locale.language}'];
                                if (options == null) options = $.datepicker.regional[''];

                                options.dateFormat = "${message(code: 'datepicker.jquery.ui.format')}";
                                options.buttonImage = "";

                                $(".mfdate-${assetFlow}-${metaFieldIdx}").datepicker(options);
                            },
                            $('.mfdate-${assetFlow}-${metaFieldIdx}').is(":visible") ? 0 : 500
                    );
                </script>
            </g:if>
        </g:if>
    </g:each>

    <div id="iconAdd">
        <g:if test="${addButton}">
            <a class="plus-icon" onclick="addMetafieldFilter${assetFlow}(${metaFieldIdx})">&#xe026;</a>
        </g:if>
        <g:if test="${removeButton}">
            <a class="plus-icon" onclick="$('#mf-row-${assetFlow}-${metaFieldIdx}').remove();$('#assets-filter-form-${assetFlow}').submit();">&#xe000;</a>
        </g:if>
    </div>
</div>

