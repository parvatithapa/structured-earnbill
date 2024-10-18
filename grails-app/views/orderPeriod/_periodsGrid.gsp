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
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next); animateToTheTop();">

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
        url:'<g:createLink action="findPeriods"/>',
        datatype: "json",
        colNames:[
            '<g:message code="orderPeriod.description"/>',
            '<g:message code="orderPeriod.unit"/>',
            '<g:message code="orderPeriod.value"/>'
        ],
        colModel:[
            { name: 'periodId', editable: false, width: 150, formatter: periodFormatter },
            { name: 'unit', editable: false, search:false, sortable:false, width: 50 },
            { name: 'value', editable: false, search:false, width: 50 }
        ],
        sortname: 'periodId',
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

function periodFormatter (cellvalue, options, rowObject) {
    var periodIdDisplay = '<em><g:message code="table.id.format" args="['_periodId_']"/></em>'
    var content = '<div class="medium">' + rowObject.description + '</div>' + periodIdDisplay;
    return content.replace(/_periodId_/g, cellvalue)
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

function animateToTheTop(){
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}
// ]]></script>
