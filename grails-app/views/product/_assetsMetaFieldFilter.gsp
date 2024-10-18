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

<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.metafields.DataType; com.sapienter.jbilling.server.item.db.ItemDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.AssetStatusDTO;  com.sapienter.jbilling.server.item.db.AssetDTO" %>

<%--
  Meta Field filter used for asset filtering.

  @author Gerhard Maree
  @since 24-April-2013
--%>


<g:set var="dateValue" value="${ (params['filterByMetaFieldId'+metaFieldIdx] != null && params['filterByMetaFieldId'+metaFieldIdx].contains(DataType.DATE.toString())) ? params['filterByMetaFieldValue'+metaFieldIdx] : null}" />

<div id="mf-row-${metaFieldIdx}" class="row">
    <span class="select6">
        <g:applyLayout name="form/select_holder">
            <content tag="label.for">filterByMetaFieldId${metaFieldIdx}</content>
            <content tag="include.script">true</content>
            <g:select id="mf-id-${metaFieldIdx}" name="filterByMetaFieldId${metaFieldIdx}" from="${assetMetaFields}" class="mf-input"
                    optionKey="${{it.id+":"+it.dataType}}" optionValue="name"
                    value="${params['filterByMetaFieldId' + metaFieldIdx]}"/>
        </g:applyLayout>
    </span>

    <%-- Default control is a textfield --%>
    <div id="mf-val-div-${metaFieldIdx}" class="inp-bg valuemarker-div-${metaFieldIdx}">
        <g:textField id="mf-val-${metaFieldIdx}" name="filterByMetaFieldValue${metaFieldIdx}" class="field default mf-input valuemarker-${metaFieldIdx}"
            value="${params['filterByMetaFieldValue'+metaFieldIdx]}"/>
    </div>

    <g:each in="${assetMetaFields}" var="mf">
        <%-- Display a select with options if the meta field is a list or enumeration--%>
        <g:if test="${mf.dataType in [DataType.ENUMERATION, DataType.LIST]}">
            <g:set var="mfEnum" value="${EnumerationDTO.findByNameAndEntityId(mf.name, mf.entityId)}" />

            <g:set var="enumValues" value="${mfEnum.values.collect {it.value}}"/>

            <div id="mf-val-${mf.id}-div-${metaFieldIdx}" class="inp-bg-inv valuemarker-div-${metaFieldIdx}">
                <g:applyLayout name="form/select_holder">
                    <content tag="label.for">filterByMetaFieldId${metaFieldIdx}</content>
                    <content tag="include.script">true</content>
                    <g:select id="mf-val-${mf.id}-${metaFieldIdx}"
                        class="field mf-input valuemarker-${metaFieldIdx}"
                        name="filterByMetaFieldValue${metaFieldIdx}"
                        from="${enumValues}"
                        value="${params['filterByMetaFieldValue'+metaFieldIdx]}"
                        optionKey=""
                        noSelection="['':'Please select a value']" />
                </g:applyLayout>
            </div>
        </g:if>

        <%-- Display a checkbox if the meta field is a boolean --%>
        <g:if test="${mf.dataType in [DataType.BOOLEAN]}">
            <g:checkBox id="mf-val-${mf.id}-${metaFieldIdx}" class="cb checkbox mf-input valuemarker-${metaFieldIdx}" name="filterByMetaFieldValue${metaFieldIdx}" checked="${'on' == params['filterByMetaFieldValue'+metaFieldIdx]}"/>
        </g:if>

        <%-- Display a date picker if the meta field is a date --%>
        <g:if test="${mf.dataType in [DataType.DATE]}">
            <div id="mf-val-${mf.id}-div-${metaFieldIdx}" class="inp-bg valuemarker-div-${metaFieldIdx}">
                <g:textField id="mf-val-${mf.id}-${metaFieldIdx}"
                         class="field mf-input valuemarker-${metaFieldIdx} mfdate-${metaFieldIdx}"
                         name="filterByMetaFieldValue${metaFieldIdx}"
                         value="${dateValue}"/>
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

                            $(".mfdate-${metaFieldIdx}").datepicker(options);
                        },
                        $('.mfdate-${metaFieldIdx}').is(":visible") ? 0 : 500
                );
            </script>
            </g:if>
        </g:if>
    </g:each>


    <g:if test="${addButton}">
        <a class="plus-icon" onclick="addMetafieldFilter()">&#xe026;</a>
    </g:if>
    <g:if test="${removeButton}">
        <a class="plus-icon" onclick="$('#mf-row-${metaFieldIdx}').remove();removeMetafieldFilter();">&#xe000;</a>
    </g:if>

</div>

