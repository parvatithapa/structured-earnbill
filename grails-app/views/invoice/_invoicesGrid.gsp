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

<g:set var="processId" value="${actionName == 'byProcess' ? params.id : null}"/>
<g:set var="csvAction" value="${actionName == 'byProcess' ? 'csvByProcess' : 'csv'}"/>
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
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
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
        url:'<g:createLink action="${actionName == 'byProcess' ? 'findByProcess' : 'findInvoices'}" id="${processId}"/>',
        datatype: "json",
        colNames:[
            '<g:message code="invoice.label.id"/>',
            '<g:message code="invoice.label.customer"/>',
            <g:isRoot>'<g:message code="invoice.label.company.name"/>',</g:isRoot>
            '<g:message code="invoice.label.duedate"/>',
            '<g:message code="invoice.label.status"/>',
            '<g:message code="invoice.label.amount"/>',
            '<g:message code="invoice.label.balance"/>'
        ],
        colModel:[
            { name: 'invoiceId', editable: false, width: 90, formatter: invoiceFormatter },
            { name: 'userName', editable: false },
            <g:isRoot>{ name: 'company', editable: false }, </g:isRoot>
            { name: 'dueDate', editable: false, width: 90, search: false, formatter: 'date' , formatOption:{newFormat:'<g:message code="date.pretty.format"/>'}},
            { name: 'status', editable: false, width: 90, search: false, formatter: descriptionFormatter },
            { name: 'amount', editable: false, width: 90, search: false, formatter: balanceFormatter },
            { name: 'balance', editable: false, search: false, formatter: balanceFormatter }
        ],
        sortname: 'invoiceId',
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
    ).jqGrid('navButtonAdd', '#data_grid_pager_${updateColumn}',
            {caption: 'pdf', onClickButton: downloadFilteredPdf, title:'<g:message code="jqview.download.pdf.filtered.tip" />'});

    $(jqTableGrid).jqGrid('filterToolbar',{autosearch:true});

});

function invoiceFormatter (cellvalue, options, rowObject) {
    var invoiceIdDisplay = '<em><g:message code="table.id.format" args="['_invoiceId_']"/></em>'
    var content = '<div class="medium"><strong>' + rowObject.invoiceNumber + '</strong></div>' + invoiceIdDisplay;
    return content.replace(/_invoiceId_/g, cellvalue)
}

function descriptionFormatter (cellvalue, options, rowObject) {
    return cellvalue.description;
}

// A simple formatter that concatenates the currency symbol with the balance
function balanceFormatter (cellvalue, options, rowObject) {
    return rowObject.currencySymbol + cellvalue.toFixed(2);
}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="invoice" action="${csvAction}" id="${processId}"/>'});
}

function downloadFilteredPdf() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'pdf', url:'<g:createLink controller="invoice" action="batchPdf"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
