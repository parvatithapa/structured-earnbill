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

<g:set var="updateColumn" value="column2"/>

<!-- table tag will hold our grid
    The updateColumn variable will allow us to identify whether this table
    is for showing entities (column1) or their children (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <g:remoteLink action="allReports" update="column1" class="submit show" onSuccess="cleanColumn('column2');"><span><g:message code="button.show.all"/></span></g:remoteLink>
</div>

<div id="showReportLink" style="display: none;">
    <g:remoteLink class="cell" action="show" params="[template: 'show']" id="_id_" before="register(this);" onSuccess="render(data, next); animateToTheTop();">

    </g:remoteLink>
</div>

<div id="execShowReportLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
var jqTableGrid${updateColumn} = $('#data_grid_${updateColumn}');
var jqTablePager${updateColumn} = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid${updateColumn}).jqGrid({
        url:'<g:createLink action="findReports" id="${selectedTypeId}"/>',
        datatype: "json",
        colNames:[
            '<g:message code="report.th.name"/>'
        ],
        colModel:[
            { name: 'name', editable: false, sortable:false ,formatter: reportFormatter }
        ],
        sortname: 'name',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePager${updateColumn}),
        viewrecords: true,
        gridview: true,
        onSelectRow: function(id){
            if(id && id!==gLastSel){
                var content = $('#showReportLink').clone().html().replace(/_id_/g, id);
                $("#execShowReportLink").html(content);
                $("#execShowReportLink > a").click();
                gLastSel=id;
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

    $(jqTableGrid${updateColumn}).jqGrid('filterToolbar',{autosearch:true});

});

function reportFormatter (cellvalue, options, rowObject) {
    var reportNameDisplay = '<em>' + rowObject.fileName + '</em>'
    var content = '<div class="medium" style="font-weight: bold">' + cellvalue + '</div>' + reportNameDisplay;
    return content
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

function cleanColumn(column){
    $('#' + column).html('');
}

function animateToTheTop(){
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}
// ]]></script>
