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
    <div class="row"></div>
</div>

<div id="showLink" style="display: none;">
    <g:link class="cell" action="show" id="_id_" onClick="a_onClick()">

    </g:link>
</div>

<div id="execShowLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
var jqTableGrid = $('#' + '${updateColumn}' + ' #data_grid_${updateColumn}');
var jqTablePager = $('#' + '${updateColumn}' + ' #data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid).jqGrid({
        url:'<g:createLink action="findProcesses"/>',
        datatype: "json",
        colNames:[
            '<g:message code="label.billing.cycle.id"/>',
            '<g:message code="label.billing.cycle.date"/>',
            '<g:message code="label.billing.order.count"/>',
            '<g:message code="label.billing.invoice.count"/>',
            '<g:message code="label.billing.total.invoiced"/>',
            '<g:message code="label.billing.total.carried"/>'
        ],
        colModel:[
            { name: 'billingId', editable: false, width: 70 },
            { name: 'date', editable: false, width: 90, search: false, formatter: 'date' , formatOption:{newFormat:'<g:message code="date.pretty.format"/>'}},
            { name: 'orderCount', editable: false, width: 40, search: false, sortable: false},
            { name: 'invoiceCount', editable: false, width: 90, search: false, sortable: false},
            { name: 'totalInvoiced', editable: false, search: false, sortable: false, formatter: multiCurrencyFormatter },
            { name: 'totalCarried', editable: false, search: false, sortable: false, formatter: multiCurrencyFormatter }
        ],
        sortname: 'billingId',
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
                gLastSel=id;
                $("#execShowLink > a")[0].click();
            }
        },
        loadComplete: function(data){
            var dataSize = data.rows.length;
            for (i = 0; i < dataSize; i++) {
                if (data.rows[i].cell.isReview == 1) {
                    var rowId = data.rows[i].cell.billingId
                    $(jqTableGrid).jqGrid('setRowData', rowId, false, 'forReview');
                }
            }
        }
    }).navGrid('#data_grid_pager_${updateColumn}',
            {   add:false,edit:false,del:false,search:false,refresh:true,csv:false
            }, // which buttons to show?
            // edit options
            {},
            // add options
            {},
            // delete options
            {}
    );

    $(jqTableGrid).jqGrid('filterToolbar',{autosearch:true});

});

// A simple formatter that concatenates the currency symbol with the balance
function multiCurrencyFormatter (cellvalue, options, rowObject) {
    if (rowObject.multiCurrency){
        return '<em><g:message code="label.billing.multi.currency"/></em>'
    }
    return rowObject.currencySymbol + cellvalue.toFixed(2);
}

// A simple formatter that concatenates the currency symbol with the balance
function balanceFormatter (cellvalue, options, rowObject) {
    if (cellvalue){
        return rowObject.currencySymbol + cellvalue.toFixed(2);
    }
    return '';
}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="billing" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
