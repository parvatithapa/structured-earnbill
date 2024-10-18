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

<g:if test="${lastMediationProcessStatus}">
    <div class="msg-box wide info" >
        <strong> <g:message code="mediation.config.last.process"/> </strong>
        <g:link controller="mediation" action="show" id="${lastMediationProcessStatus.mediationProcessId}">
            ${lastMediationProcessStatus.mediationProcessId}
        </g:link>
    </div>
</g:if>

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>

    <g:if test="${!isMediationProcessRunning && types}">
        <g:link controller="mediationConfig" action="run" class="submit apply"><span><g:message code="button.run.mediation"/></span></g:link>
    </g:if>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next)">

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
        url:'<g:createLink action="findMediationConfigs"/>',
        datatype: "json",
        colNames:[
            '<g:message code="mediation.config.name"/>',
            '<g:message code="mediation.config.order"/>',
            '<g:message code="mediation.config.plugin"/>'
        ],
        colModel:[
            { name: 'mediationConfigId', editable: false, width: 90, sortable:false, search:false, formatter: mediationConfigsFormatter },
            { name: 'order', editable: false, width: 15, sortable:false, search:false},
            { name: 'plugin', editable: false, sortable:false, search:false, formatter: pluginConfigFormatter}
        ],
        sortname: 'mediationConfigId',
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

function mediationConfigsFormatter (cellvalue, options, rowObject) {
    var mediationConfigIdDisplay = '<em><g:message code="table.id.format" args="['_mediationId_']"/></em>'
    var content = '<div class="medium">' + rowObject.name + '</div>' + mediationConfigIdDisplay;
    return content.replace(/_mediationId_/g, cellvalue)
}

function pluginConfigFormatter (cellvalue, options, rowObject) {
    return "(" + rowObject.readerId + ") " + rowObject.readerDescription
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

// ]]></script>
