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

<%@ page import="com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType" %>

<g:set var="updateColumn" value="column1"/>

<!-- table tag will hold our grid
    The updateColumn variable will allow us to identify whether this table
    is for showing entities (column1) or their children (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <sec:ifAllGranted roles="DISCOUNT_151">
        <g:link action="edit" class="submit add button-primary">
            <span><g:message code="button.create"/></span>
        </g:link>
    </sec:ifAllGranted>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
</div>

<div id="execShowLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
//Discount types and their internationalized message
var discountTypes = {};
<g:each in="${DiscountStrategyType.values()}" var="type">
discountTypes['${type}'] = '<g:message code="${type.messageKey}"/>';
</g:each>

var gLastSel = -1;
var jqTableGrid = $('#' + '${updateColumn}' + ' #data_grid_${updateColumn}');
var jqTablePager = $('#' + '${updateColumn}' + ' #data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid).jqGrid({
        url:'<g:createLink action="findDiscounts"/>',
        datatype: "json",
        colNames:[
            '<g:message code="discounts.th.code"/>',
            '<g:message code="discounts.th.description"/>',
            '<g:message code="discounts.th.type"/>'
        ],
        colModel:[
            { name: 'code', editable: false, width: 90, formatter: discountCodeFormatter },
            { name: 'description', editable: false, sortable: false, search: false },
            { name: 'type', editable: false, width: 90, search: false, formatter: discountTypeFormatter}
        ],
        sortname: 'code',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePager),
        viewrecords: true,
        gridview: true,
        onSelectRow: function(id){
            if(id && id!==gLastSel){
                var content = $('#showLink').clone().html().replace(/_id_/g, id);
                $("#execShowLink").html(content);
                $("#execShowLink > a").click();
                gLastSel=id;
            }
        }
    }).navGrid('#data_grid_pager_${updateColumn}',
            {   add:false,edit:false,del:false,search:false,refresh:true,csv:true
            }, // which buttons to show?
            // edit options
            {},
            // add options
            {},
            // delete options
            {}
    ).jqGrid('navButtonAdd', '#data_grid_pager_${updateColumn}',
            {caption: 'csv', onClickButton: downloadFilteredCsv, title:'<g:message code="jqview.download.csv.filtered.tip" />'});

    $(jqTableGrid).jqGrid('filterToolbar',{autosearch:true});

});

function discountCodeFormatter (cellvalue, options, rowObject) {
    var discountIdDisplay = '<em><g:message code="table.id.format" args="['_discountId_']"/></em>';
    options.rowId = rowObject.discountId;
    var content = '<div class="medium"><strong>' + cellvalue + '</strong></div>' + discountIdDisplay;
    return content.replace(/_discountId_/g, rowObject.discountId);
}

function discountTypeFormatter (cellvalue, options, rowObject) {
    return discountTypes[cellvalue.name];
}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="discount" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
