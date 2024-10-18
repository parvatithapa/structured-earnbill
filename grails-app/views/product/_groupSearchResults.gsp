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
<%@ page import="com.sapienter.jbilling.client.util.SortableCriteria; java.util.regex.Pattern" %>

<%--
   Filters for searching asset which will be added to groups

  @author Gerhard Maree
  @since  18-Jul-2013
--%>
<%-- parameters the page functionality must include in URLs --%>
<g:set var="searchParams" value="${SortableCriteria.extractParameters(params, ['filterBy', Pattern.compile(/search.*/),  Pattern.compile(/filterByMetaFieldId(\d+)/), Pattern.compile(/filterByMetaFieldValue(\d+)/)])}" />

<div class="table-box">
    <table id="users" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th>
                <g:message code="asset.table.th.identifier"/>
            </th>
            <th class="medium2">
                <g:message code="asset.table.th.creationDate"/>
            </th>
            <th class="small">
                <g:message code="asset.table.th.status"/>
            </th>
        </tr>
        </thead>

        <tbody>
            <g:each in="${assets}" var="asset">
                <tr id="group-asset-${asset.id}" class="${selected?.id == asset.id ? 'active' : ''}">
                    <td class="narrow" onclick="addAssetToSelected(this);">
                        <em class="narrow">${asset?.identifier}</em>
                    </td>
                    <td class="narrow" onclick="addAssetToSelected(this);">
                        <span class="narrow"><g:formatDate format="dd-MM-yyyy HH:mm" date="${asset.createDatetime}" timeZone="${session['company_timezone']}"/></span>
                    </td>
                    <td class="narrow" onclick="addAssetToSelected(this);">
                        <span class="narrow">${asset.assetStatus?.description}</span>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <div class="select-holder select-holder_small"><span class="select-value"></span>
            <g:set var="steps" value="${[10, 20, 50]}"/>
            <g:select id="page-size-assets" name="page-size-assets" from="${steps}"
                      onchange="pageSizeChange_groupAssetSearch(this);" value="${params.max}"
                      optionValue="${{ it + " " + message(code: "pager.show.max.results") }}">
            </g:select>
        </div>
    </div>
    <jB:isPaginationAvailable total="${assets?.totalCount ?: 0}">
        <div class="row-center">
            %{--the action will be inserted by a js function--}%
            <jB:remotePaginate action="#" total="${assets?.totalCount ?: 0}" update="asset-search-results"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<script type="text/javascript">
    function pageSizeChange_groupAssetSearch(obj) {
        $('input[name="max"]').val(obj.value);
        $('input[name="offset"]').val(0);
        searchAssetsForGroup();
    }

    function paginationChange_groupAssetSearch(offset) {
        $('input[name="max"]').val("${params.max}");
        $('input[name="offset"]').val(offset);
        searchAssetsForGroup();
    }

    $(document).ready(function () {
        $("select[name='page-size-assets']").each(function () {
            updateSelectLabel(this);
        });

        $("select[name='page-size-assets']").change(function () {
            updateSelectLabel(this);
        });

        // The next functions change the pagination buttons behavior: 1,2,3...
        // When calls the submit form, it includes all filters params situated in form, and the pagination attributes max and offset
        var max = ${params.max};
        $('a.step').each(function (i, obj) {
            var offset = (max * (parseInt($(obj).text(), 10) - 1));
            $(obj).removeAttr('onclick')
                    .removeAttr('href')
                    .attr('onclick', 'paginationChange_groupAssetSearch(' + offset + ')');
        });

        var prevLink = $('a.prevLink');
        if (prevLink[0]) {
            var offset = (max * (parseInt($('span.currentStep').text(), 10) - 2));
            prevLink.removeAttr('onclick')
                    .removeAttr('href')
                    .attr('onclick', 'paginationChange_groupAssetSearch(' + offset + ')');
        }

        var nextLink = $('a.nextLink');
        if (nextLink[0]) {
            var offset = (max * (parseInt($('span.currentStep').text(), 10)));
            nextLink.removeAttr('onclick')
                    .removeAttr('href')
                    .attr('onclick', 'paginationChange_groupAssetSearch(' + offset + ')');
        }
    });

</script>
