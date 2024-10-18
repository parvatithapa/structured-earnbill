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
    <sec:access url="/payment/create">
        <g:link action="create" class="submit payment button-secondary"><span><g:message code="button.create.payment"/></span></g:link>
    </sec:access>
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
        url:'<g:createLink action="findPayments"/>',
        datatype: "json",
        colNames:[
            '<g:message code="payment.th.id"/>',
            '<g:message code="invoice.label.customer"/>',
            <g:isRoot>'<g:message code="invoice.label.company.name"/>',</g:isRoot>
            '<g:message code="payment.th.date"/>',
            '<g:message code="payment.th.payment.or.refund"/>',
            '<g:message code="payment.th.amount"/>',
            '<g:message code="payment.th.method"/>',
            '<g:message code="payment.th.result"/>'
        ],
        colModel:[
            { name: 'paymentId', editable: false, width: 70 },
            { name: 'userName', editable: false },
            <g:isRoot>{ name: 'company', editable: false }, </g:isRoot>
            { name: 'date', editable: false, width: 90, search: false, formatter: 'date' , formatOption:{newFormat:'<g:message code="date.pretty.format"/>'}},
            { name: 'paymentOrRefund', editable: false, width: 40, search: false},
            { name: 'amount', editable: false, width: 90, search: false, formatter: balanceFormatter },
            { name: 'method', editable: false, search: false, formatter: descriptiveFormatter  },
            { name: 'result', editable: false, search: false, formatter: descriptiveFormatter }
        ],
        sortname: 'paymentId',
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

// A simple formatter that concatenates the currency symbol with the balance
function balanceFormatter (cellvalue, options, rowObject) {
    return rowObject.currencySymbol + cellvalue.toFixed(2);
}

// A simple formatter that shows the description attribute of the cellvalue object
function descriptiveFormatter (cellvalue, options, rowObject) {
    return cellvalue.description
}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="payment" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
