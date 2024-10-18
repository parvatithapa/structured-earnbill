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
    <a href="${createLink(action: 'showForm')}" class="submit add button-primary"><span><g:message code="button.create"/></span></a>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" params="[template:'show']" before="register(this);" onSuccess="render(data, next)">

    </g:remoteLink>
</div>

<div id="execShowLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
var jqTableGridPlugins = $('#data_grid_${updateColumn}');
var jqTablePagerPlugins = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGridPlugins).jqGrid({
        url:'<g:createLink action="findPlugins" id="${selectedCategoryId}"/>',
        datatype: "json",
        colNames:[
            '<g:message code="plugins.plugin.id"/>',
            '<g:message code="plugins.plugin.type"/>',
            '<g:message code="plugins.plugin.order"/>'
        ],
        colModel:[
            { name: 'pluginId', editable: false, width: 15, sortable:true, search:true},
            { name: 'type', editable: false, sortable:true, search:true, formatter: pluginFormatter},
            { name: 'order', editable: false, width: 15, sortable:true, search:false}
        ],
        sortname: 'order',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePagerPlugins),
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

    $(jqTableGridPlugins).jqGrid('filterToolbar',{autosearch:true});

});

function pluginFormatter (cellvalue, options, rowObject) {
    var classNameDisplay = '<em>' + rowObject.typeClassName + '</em>'
    var content = '<div class="medium">' + classNameDisplay + '</div>' ;
    if (rowObject.typeTitle){
        content = '<div class="medium"><strong>' + rowObject.typeTitle + '</strong></div>' + classNameDisplay;
    }
    return content
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

// ]]></script>
