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
    <g:link action="edit" class="submit add button-primary">
        <span><g:message code="button.create"/></span>
    </g:link>
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
        url:'<g:createLink action="findEnums"/>',
        datatype: "json",
        colNames:[
            '<g:message code="enumeration.type.name"/>'
        ],
        colModel:[
            { name: 'enumId', editable: false, width: 150, formatter: enumerationFormatter }
        ],
        sortname: 'enumId',
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

function enumerationFormatter (cellvalue, options, rowObject) {
    var enumIdDisplay = '<em><g:message code="table.id.format" args="['_enumId_']"/></em>'
    var content = '<div class="medium">' + rowObject.name + '</div>' + enumIdDisplay;
    return content.replace(/_enumId_/g, cellvalue)
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

function animateToTheTop(){
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}
// ]]></script>
