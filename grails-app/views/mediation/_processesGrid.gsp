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
    <g:remoteLink class="cell" action="show" params="['template': 'show']" id="_id_" before="register(this);" onSuccess="render(data, next);">

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
        url:'<g:createLink action="findProcesses"/>',
        datatype: "json",
        colNames:[
            '<g:message code="mediation.th.id"/>',
            '<g:message code="mediation.th.start.date"/>',
            '<g:message code="mediation.th.end.date"/>',
            '<g:message code="mediation.th.total.records"/>',
            '<g:message code="mediation.th.orders.affected"/>'
        ],
        colModel:[
            { name: 'processId', editable: false, width: 90 },
            { name: 'startDate', editable: false, width: 90, search: false, formatter: dateFormatter},
            { name: 'endDate', editable: false, width: 90, search: false, formatter: dateFormatter},
            { name: 'totalRecords', editable: false, width: 90, search: false, sortable: false},
            { name: 'orders', editable: false, width: 90, search: false}
        ],
        sortname: 'processId',
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

// A simple date formatter. This is just a quick fix until a better solution is found
function dateFormatter (cellvalue, options, rowObject) {
    var date = new Date(cellvalue);
    var day = date.getDay() -1;
    var month = date.getMonth() +1;
    var year = date.getFullYear();

    if (month < 10) {
        month = "0" + month;
    }

    if (day < 10) {
        day = "0" + day;
    }

    var dateFormat = "<g:message code="date.timeSecs.format"/>";

    var hours = date.getHours();
    var minutes = date.getMinutes();
    var seconds = date.getSeconds();

    return dateFormat.replace(/MM/g, month).replace(/dd/g, day).replace(/yyyy/g, year)
            .replace(/hh/g, hours).replace(/mm/g, minutes).replace(/ss/g, seconds).replace(/ /g, '</br>');
}

function processFormatter (cellvalue, options, rowObject) {
    var processIdDisplay;
    if (rowObject.configurationName){
        processIdDisplay = '<em>' + rowObject.configurationName +'</em>'
    }
    return content = '<div class="medium"><strong>' + cellvalue + '</strong></div>' + processIdDisplay

}

function downloadFilteredCsv() {
    $(jqTableGrid).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="mediation" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}
// ]]></script>
