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
    is for showing users (column1) or subaccounts (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div id="showPluginsLink" style="display: none;">
    <g:remoteLink class="cell" action="plugins" params="[template:'show']" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
</div>

<div id="execShowPluginsLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gridLastCategorySel = -1;
var jqTableGrid${updateColumn} = $('#data_grid_${updateColumn}');
var jqTablePager${updateColumn} = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid${updateColumn}).jqGrid({
        url:'<g:createLink action="findCategories"/>',
        datatype: "json",
        colNames:[
            '<g:message code="plugins.category.list.id"/>',
            '<g:message code="plugins.category.list.title"/>'
        ],
        colModel:[
            { name: 'categoryId', editable: false, width: 15},
            { name: 'description', editable: false, sortable:false, formatter: pluginCategoryFormatter }
        ],
        sortname: 'categoryId',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePager${updateColumn}),
        viewrecords: true,
        gridview: true,
        onSelectRow: function(id){
            if(id && id!==gridLastCategorySel){
                var content = $('#showPluginsLink').clone().html().replace(/_id_/g, id);
                $("#execShowPluginsLink").html(content);
                $("#execShowPluginsLink > a").click();
                gridLastCategorySel=id;
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

function pluginCategoryFormatter (cellvalue, options, rowObject) {
    var categoryDisplay = '<em>' + rowObject.interfaceName + '</em>'
    var content = '<div class="medium">' + categoryDisplay + '</div>' ;
    if (cellvalue){
        content = '<div class="medium"><strong>' + cellvalue + '</strong></div>' + categoryDisplay;
    }
    return content
}

function isRowSelected(id) {
    //console.log("id:"+id+" gridLastCategorySel:"+gridLastCategorySel);
    return gridLastCategorySel == id;
}

// ]]></script>
