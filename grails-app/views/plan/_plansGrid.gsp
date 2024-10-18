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

<g:set var="updateColumn" value="column1"/>

<!-- table tag will hold our grid
    The updateColumn variable will allow us to identify whether this table
    is for showing entities (column1) or their children (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <sec:ifAllGranted roles="PLAN_60">
        <g:link controller="planBuilder" action="edit" class="submit add button-primary"><span><g:message code="button.create"/></span></g:link>
    </sec:ifAllGranted>
    <div class="row"></div>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
</div>

<div id="execShowLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
var jqTableGrid = $('#data_grid_${updateColumn}');
var jqTablePager = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid).jqGrid({
        url:'<g:createLink action="findPlans"/>',
        datatype: "json",
        colNames:[
            '<g:message code="plan.th.name"/>',
            <g:isRoot>'<g:message code="product.label.company.name"/>',</g:isRoot>
            '<g:message code="plan.th.item.number"/>',
            '<g:message code="plan.th.products"/>'
        ],
        colModel:[
            { name: 'planId', editable: false,  formatter: planFormatter },
            <g:isRoot>{ name: 'company', editable: false, width: 90, formatter: companyFormatter }, </g:isRoot>
            { name: 'itemNumber', editable: false, width: 90, formatter: itemNumberFormatter},
            { name: 'products', editable: false, width: 35, search: false, sortable: false }
        ],
        sortname: 'planId',
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
            {caption: 'csv', onClickButton: downloadFilteredCsv, title:'<g:message code="jqview.download.csv.filtered.tip" />'}
    );

    $(jqTableGrid).jqGrid('filterToolbar',{autosearch:true});

});

function planFormatter (cellvalue, options, rowObject) {
    var planIdDisplay = '<em><g:message code="table.id.format" args="['_planId_']"/></em>'
    var content = '<div class="medium"><strong>' + rowObject.item.cell.name + '</strong></div>' + planIdDisplay;
    return content.replace(/_planId_/g, cellvalue)
}

function itemNumberFormatter (cellvalue, options, rowObject) {
    return rowObject.item.cell.number
}

function companyFormatter (cellvalue, options, rowObject) {
    var content;
    if (rowObject.item.cell.global){
        content = '<strong><g:message code="product.label.company.global"/></strong>'
    }else if (rowObject.item.cell.multiple){
        content = '<strong><g:message code="product.label.company.multiple"/></strong>'
    }else {
        content = rowObject.item.cell.company
    }
    return content
}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="plan" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
