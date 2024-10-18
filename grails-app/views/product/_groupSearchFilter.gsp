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
<%@ page import="java.util.regex.Pattern" %>

<%--
   Filters for searching asset which will be added to groups

  @author Gerhard Maree
  @since  18-Jul-2013
--%>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="asset.label.product"/></content>
    <content tag="label.for">searchItemId</content>
    <content tag="include.script">true</content>
    <g:select        name = "searchItemId"
                     from = "${products}"
                    class = "group-search-filter"
              noSelection = "['': message(code: 'filters.asset.status.empty')]"
                optionKey = "id"
              optionValue = "description"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="assets.label.status"/></content>
    <content tag="label.for">searchStatusId</content>
    <content tag="include.script">true</content>
    <g:select        name = "searchStatusId"
                     from = "${assetStatuses}"
                    class = "group-search-filter"
              noSelection = "['': message(code: 'filters.asset.status.empty')]"
                optionKey = "id"
              optionValue = "description" />
</g:applyLayout>

<g:if test="${metaFields?.size() > 0}">
    <div id="mf-row-holder"></div>

    <g:render template = "assetsMetaFieldFilter"
                 model = "[assetMetaFields: metaFields,
                              metaFieldIdx: 1,
                                 addButton: true,
                            initDatePicker: true]"/>

</g:if>

<g:if test="${metaFields?.size() > 0}">
<%-- Template used for adding metafields to search --%>
    <div id="metafield-filter-template" style="display: none">
        <g:render template = "assetsMetaFieldFilter"
                     model = "[assetMetaFields: metaFields,
                                  metaFieldIdx: '_mfIdx_',
                                  removeButton: true]"/>
    </div>
</g:if>

<script src="${resource(dir: 'js', file:'asset-metafield-search-fields.js')}" ></script>

<script type="text/javascript">
    //required by the meta field java script included
    var metaFieldIdx = 1;
    var localLang = '${session.locale.language}';
    var pickerDateFormat = "${message(code: 'datepicker.jquery.ui.format')}";
    var assetSearchFormId = 'group-search-holder';
    <%-- assetSearchFunction is defined in editAsset --%>

    <g:if test="${metaFields?.size() > 0}" >
    <%-- event listeners to reload results for first metafield search line --%>
    $('#asset-filter-form :input[name=filterByMetaFieldValue1]').change(function () {
        searchAssetsForGroup();
    });

    <%-- Show the correct widget to select a meta field value for first meta search line--%>
    $('#mf-id-1').change(function () {
        showCorrectWidget(this);
    }).change();
    </g:if>

    $('#group-search-holder .group-search-filter').change(function () {
        searchAssetsForGroup();
    });
    $('#group-search-holder .mf-input').change(function () {
        searchAssetsForGroup();
    });
</script>