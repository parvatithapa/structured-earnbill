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

<%@ page import="com.sapienter.jbilling.client.util.SortableCriteria;" %>
<%@ page import="com.sapienter.jbilling.server.user.UserBL;" %>
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO;" %>
<%@ page import="java.util.regex.Pattern;" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils; java.lang.System;"%>

<%--
   Display a list of assets and allows for sorting.

  @author Gerhard Maree
  @since  18-Apr-2013
--%>

<%-- indexes of metafields we have filter data for --%>
<g:set var="metaFieldIdxs" value="[]" />
<g:set var="metaFieldIMaxIdx" value="${new Integer(0)}" />
<g:set var="filtersUsed" value="${params.containsKey('_showDeleted') || params.containsKey('_showReIssuable') || params.containsKey('_showSuspended')}" />
<%-- parameters the page functionality must include in URLs --%>
<g:set var="searchParams" value="${SortableCriteria.extractParameters(params, ['filterBy','statusId','showDeleted','showReIssuable','showSuspended', Pattern.compile(/filterByMetaFieldId(\d+)/), Pattern.compile(/filterByMetaFieldValue(\d+)/)])}" />

<div class="heading"><strong><g:message code="assets.for.product"/></strong></div>
<div class="box narrow">
    <div class="sub-box-no-pad">
        <!-- product info -->
        <table class="dataTable narrow" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="product.detail.id"/></td>
                <td class="value"><g:link action="show" id="${product.id}">${product.id}</g:link></td>
                <td><g:message code="product.detail.internal.number"/></td>
                <td class="value"><g:link action="show" id="${product.id}">${product.internalNumber}</g:link></td>
            </tr>
            </tbody>
        </table>
    </div>
</div>


<div class="box-cards box-cards-no-margin ${filtersUsed ? "box-cards-open" : ""}">
    <div class="box-cards-title ${filtersUsed ? "active" : ""}">
        <a class="btn-open" href="#" data-cy="filterOpen"><span><g:message
                code="asset.heading.filter"/></span></a>
    </div>

    <div class="box-card-hold narrow">
        <div class="form-columns asset-list-filters">
            <g:form name="asset-filter-form" id="${product.id}" action="assets">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="filters.title"/></content>
                    <content tag="label.for">filterBy</content>
                    <g:textField        name = "filterBy"
                                       class = "field default"
                                 placeholder = "${message(code: 'assets.filter.by.default')}"
                                       value = "${params.filterBy}"/>
                </g:applyLayout>

                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="assets.label.status"/></content>
                    <content tag="label.for">statusId</content>
                    <content tag="include.script">true</content>
                    <g:select        name = "statusId"
                                     from = "${assetStatuses}"
                              noSelection = "['': message(code: 'filters.asset.status.empty')]"
                                optionKey = "id"
                              optionValue = "description"
                                    value = "${params.statusId ? params.int('statusId') : null}"/>
                </g:applyLayout>

                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="filters.deleted.title"/></content>
                    <content tag="label.for">showDeleted</content>
                    <g:checkBox name="showDeleted" class="field default" checked="${'on' == params.showDeleted}"/>
                </g:applyLayout>

                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="filters.reissuable.title"/></content>
                    <content tag="label.for">showReIssuable</content>
                    <g:checkBox name="showReIssuable" class="field default" checked="${'on' == params.showReIssuable}"/>
                </g:applyLayout>

                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="filters.suspended.title"/></content>
                    <content tag="label.for">showSuspended</content>
                    <g:checkBox name="showSuspended" class="field default" checked="${'on' == params.showSuspended}"/>
                </g:applyLayout>

                <g:if test="${metaFields.size() > 0}">
                    <%
                        Pattern pattern = Pattern.compile(/filterByMetaFieldId(\d+)/)
                        //get all the ids in an array
                        params.each {
                            def m = pattern.matcher(it.key)
                            if (m.matches()) {
                                metaFieldIdxs << m.group(1)
                            }
                        }
                        metaFieldIMaxIdx = metaFieldIdxs.max() as Integer ?: 0
                    %>
                    <div id="mf-row-holder"></div>
                    <g:if test="${metaFieldIdxs}">
                        <g:set var="lastIdx" value="${metaFieldIdxs.last()}"/>
                        <g:each in="${metaFieldIdxs}" var="metaFieldIndex">

                            <g:render template = "assetsMetaFieldFilter"
                                         model = "[assetMetaFields: metaFields,
                                                      metaFieldIdx: metaFieldIndex,
                                                         addButton: (lastIdx == metaFieldIndex),
                                                      removeButton: (lastIdx != metaFieldIndex),
                                                    initDatePicker: true]"/>
                        </g:each>
                    </g:if>
                    <g:else>
                        <g:render template = "assetsMetaFieldFilter"
                                     model = "[assetMetaFields: metaFields,
                                                  metaFieldIdx: (metaFieldIMaxIdx + 1),
                                                     addButton: true,
                                                initDatePicker: true]"/>
                    </g:else>
                </g:if>
            </g:form>
        </div>
    </div>
</div>
<p/>
<g:if test="${metaFields.size() > 0}">
<%-- Template used for adding metafields to search --%>
    <div id="metafield-filter-template" style="display: none">
        <g:render template = "assetsMetaFieldFilter"
                     model = "[assetMetaFields: metaFields,
                                  metaFieldIdx: '_mfIdx_',
                                  removeButton: true]"/>
    </div>
</g:if>

<div class="table-box">
    <table id="users" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th>
                    <g:remoteSort action="assets" sort="identifier" update="column1" searchParams="${searchParams}">
                        <g:message code="asset.table.th.identifier"/>
                    </g:remoteSort>
                </th>
                <th class="medium2">
                    <g:remoteSort action="assets" sort="createDatetime" update="column1" searchParams="${searchParams}">
                        <g:message code="asset.table.th.creationDate"/>
                    </g:remoteSort>
                </th>
                <th class="small">
                    <g:remoteSort action="assets" sort="assetStatus.id" update="column1" searchParams="${searchParams}">
                        <g:message code="asset.table.th.status"/>
                    </g:remoteSort>
                </th>
                <g:isRoot>
                    <th class="small">
                        <g:remoteSort action="assets" sort="item.entity.description" update="column1" searchParams="${searchParams}">
                            <g:message code="asset.table.th.entity.description"/>
                        </g:remoteSort>
                    </th>
                </g:isRoot>
            </tr>
        </thead>

        <tbody data-cy="assetListTable">
        <g:each in="${assets}" var="asset">
            <tr id="user-${asset.id}" class="${selected?.id == asset.id ? 'active' : ''} ${asset.isReserved()?'reserved':''}">
                <td class="narrow ${asset.isReserved()?'reserved':''}">
                    <g:remoteLink class="cell double" action="showAsset" params="['template': 'showAsset']" id="${asset.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong>${StringEscapeUtils.escapeHtml(asset?.identifier)}</strong>
                        <em><g:message code="table.id.format" args="[asset.id as String]"/></em>
                    </g:remoteLink>
                </td>
                <td class="narrow ${asset.isReserved()?'reserved':''}">
                    <g:remoteLink class="cell" action="showAsset" params="['template': 'showAsset']" id="${asset.id}" before="register(this);" onSuccess="render(data, next);">
                        <span><g:formatDate formatName="date.timeSecs.format" date="${asset.createDatetime}" timeZone="${session['company_timezone']}"/></span>
                    </g:remoteLink>
                </td>
                <td class="narrow ${asset.isReserved()?'reserved':''}">
                    <g:remoteLink class="cell" action="showAsset" params="['template': 'showAsset']" id="${asset.id}" before="register(this);" onSuccess="render(data, next);">
                        <span>${asset.isReserved() ? g.message(code:"asset.reserved.status") : StringEscapeUtils.escapeHtml(asset?.assetStatus?.description)}</span>
                    </g:remoteLink>
                </td>
                <g:isRoot>
                    <td class="narrow ${asset.isReserved()?'reserved':''}">
                            <g:set var="totalChildren" value="${asset?.entities?.size()}" />
                            <g:remoteLink class="cell" action="showAsset" params="['template': 'showAsset']" id="${asset.id}" before="register(this);" onSuccess="render(data, next);">
                                <g:if test="${asset?.global}">
                                	<strong><g:message code="product.label.company.global"/></strong>
                                </g:if>
                                <g:elseif test="${totalChildren > 1}">
                                	<strong><g:message code="product.label.company.multiple"/></strong>
                                </g:elseif>
                                <g:elseif test="${totalChildren == 1}">
                                    <strong>${StringEscapeUtils.escapeHtml(asset?.entities?.toArray()[0]?.description)}</strong>
                                </g:elseif>
                                <g:else>
                                    <strong><g:message code="default.no.selection"/></strong>
                                </g:else>
                            </g:remoteLink>
                    </td>
                </g:isRoot>
            </tr>

        </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template = "/layouts/includes/pagerShowResults"
                         model = "[      steps: [10, 20, 50],
                                        action: 'assets',
                                            id: product.id,
                                        update: 'column1',
                                   extraParams: searchParams,
                                       success:'initAssetSearchForm()' ]" />
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate controller = "product"
                               action = "assets"
                                   id = "${product.id}"
                               params = "${sortableParams(params: ([partial: true])) + searchParams}"
                                total = "${assets?.totalCount ?: 0}"
                               update = "column1"/>
    </div>
</div>

<div class="btn-box">
    <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_131">
        <g:link action="editAsset" params="[add: 'true', prodId: product.id]" class="submit add"><span><g:message code="button.create"/></span></g:link>
    </sec:ifAllGranted>

    <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_133">
        <g:remoteLink id="uploadAsset" class="submit add" action="showUploadAssets" params="[prodId: product.id]" before="register(this);" onSuccess="render(data, next);">
            <span><g:message code="button.assets.upload"/></span>
        </g:remoteLink>
    </sec:ifAllGranted>
</div>

<script type="text/javascript">
    //required by the meta field java script included
    var metaFieldIdx = ${metaFieldIMaxIdx + 3};
    var localLang = '${session.locale.language}';
    var pickerDateFormat = "${message(code: 'datepicker.jquery.ui.format')}";
    var assetSearchFormId = 'asset-filter-form';
    <%-- function which will search for assets --%>
    var assetSearchFunction = function() {
        $('#'+assetSearchFormId).submit();
    };

</script>

<script src="${resource(dir: 'js', file:'asset-metafield-search-fields.js')}" ></script>

<script type="text/javascript">
    $(document).ready(function() {
        <g:if test="${params.partial}" >
        <%-- register for the slide events if it is loaded by pagination --%>
        registerSlideEvents();
        </g:if>
        placeholder();
        initAssetSearchForm();
    });

    function initAssetSearchForm() {
        $('#statusId').val('${params.statusId}');
        <%-- event listeners to reload results --%>
        $('#asset-filter-form :input[name=filterBy]').blur(function () {
            $('#asset-filter-form').submit();
        });
        $('#asset-filter-form :input[name=statusId]').change(function () {
            $('#asset-filter-form').submit();
        });
        $('#asset-filter-form :input[name=showDeleted]').change(function () {
            $('#asset-filter-form').submit();
        });

        $('#asset-filter-form :input[name=showReIssuable]').change(function () {
            $('#asset-filter-form').submit();
        });

        $('#asset-filter-form :input[name=showSuspended]').change(function () {
            $('#asset-filter-form').submit();
        });

        <g:if test="${metaFields.size() > 0 && metaFieldIdxs.size() == 0}" >
        <%-- event listeners to reload results for first metafield search line if there is no previous search--%>
        $('#asset-filter-form :input[name=filterByMetaFieldValue${metaFieldIMaxIdx + 1}]').change(function () {
            $('#asset-filter-form').submit();
        });

        <%-- Show the correct widget to select a meta field value for first meta search line--%>
        $('#mf-id-${metaFieldIMaxIdx + 1}').change(function () {
            showCorrectWidget(this);
        });
        showCorrectWidget($('#mf-id-${metaFieldIMaxIdx + 1}'), true);
        </g:if>

        <g:each in="${metaFieldIdxs}" var="metaFieldIndex">
        <%-- event listeners to reload results for metafield search lines after first search --%>
        $('#asset-filter-form :input[name=filterByMetaFieldValue${metaFieldIndex}]').change(function () {
            $('#asset-filter-form').submit();
        });

        <%-- Show the correct widget to select a meta field value for metafield search lines after first search --%>
        $('#mf-id-${metaFieldIndex}').change(function () {
            showCorrectWidget(this);
        });
        showCorrectWidget($('#mf-id-${metaFieldIndex}'), true);
        </g:each>
    }
</script>
