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

<html>
<head>
    <meta name="layout" content="main" />
    <!-- r:require module="jqGrid" / -->
    <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
    <g:javascript src="jquery.jqGrid.min.js"  />
    <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
</head>
<body>

<g:set var="nestedFilters" value="${[routeRateCardId: routeRateCard.id]+matchedColumnValues}" />
<form  id='_export' method="post" action='<g:createLink action="filteredCsv" />'>
<input type="hidden" name="routeRateCardId" value="${routeRateCard.id}" />
<input type="hidden" name="_filters" id="_filters" value="" />
</form>

<g:link action="list" params="[id:routeRateCard.id]"><g:message code="button.back" /></g:link>
<!-- table tag will hold our grid -->
<table id="data_grid" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager" class="scroll" style="text-align:center;"></div>
<div class="form-edit">&nbsp;</div>
<div class="form-edit">
    <g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>

    <div class="heading">
        <strong>
            <g:message code="dataTable.nested.search.header" />
        </strong>
    </div>

    <div class="form-hold">
        <div class="form-columns">
    <g:form name="nested-search-form" action="nestedSearch">
        <fieldset>
            <input type="hidden" name="curr.routeRateCardId" value="${routeRateCard.id}" />
            <g:each in="${matchedColumnValues}" var="mcentry">
                <g:each in="${mcentry.value}" var="mcvalue">
                    <input type="hidden" name="curr.${mcentry.key}" value="${mcvalue}" />
                </g:each>
            </g:each>
            <div class="row">
                <label for=""><g:message code="dataTable.nested.search.columns.match.prompt"/></label>
                <span class="normal">
                <g:each in="${columnNames}" var="columnName" status="colIdx">
                    <g:checkBox name="match.col.${columnName}" /> ${columnName}
                </g:each>
                </span>
            </div>

            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="dataTable.nested.search.table.prompt"/></content>
                <content tag="label.for">table</content>

                <g:select        name = "match.table"
                                 from = "${tableNames}"
                            optionKey = "id"
                          optionValue = "name"/>
            </g:applyLayout>
            <br />
            <g:hiddenField name="nestedDepth" value="${nestedTables ? nestedTables.size() : 0}" />
            <g:each in="${nestedTables}" var="nestedTable" status="depth">
                <div class="row">
                    <label for=""><g:message code="dataTable.nested.search.depth.label"/>&nbsp;${depth + 1}</label>
                    <span class="normal">
                        <span class="strong"><g:message code="dataTable.nested.search.table.label"/>:</span>
                        ${nestedTables[depth]}
                        <g:hiddenField name="nestedTables[${depth}]" value="${nestedTables[depth]}"/>

                        <span class="strong">&nbsp;&nbsp;<g:message code="dataTable.nested.search.columns.label"/>:</span>
                        ${nestedColumns[depth]}
                        <g:hiddenField name="nestedColumns[${depth}]" value="${nestedColumns[depth]}"/>
                    </span>
                </div>

                <div class="row">
                    <label for="">&nbsp;</label>
                    <span class="normal">
                        <span class="strong">
                            <g:message code="dataTable.nested.search.values.label"/>:
                        </span>
                        ${nestedValues[depth]}
                    <g:hiddenField name="nestedValues[${depth}]" value="${nestedValues[depth]}"/>
                    </span>
                </div>
            </g:each>
        </fieldset>
    </g:form>
    </div>
        <div class="buttons">
            <ul>
                <li>
                    <a class="submit find button-primary" onclick="nestedSearch();">
                        <span><g:message code="button.search"/></span>
                    </a>
                </li>
                <li>
                    <g:link class="submit cancel" action="search" id="${routeRateCard.id}">
                        <span><g:message code="button.clear.search"/></span>
                    </g:link>
                </li>
            </ul>
        </div>
</div>

</div>



<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
$(document).ready(function () {
    jQuery("#data_grid").jqGrid({
        url:'<g:createLink action="findRouteRateCardRecord" params="${[routeRateCardId: routeRateCard.id]+matchedColumnValues}"/>',
        editurl:'<g:createLink action="recordEdit" params="[routeRateCardId: routeRateCard.id]"/>',
        datatype: "json",
        colNames:[
            <g:each in="${columnNames}" var="columnName" status="colIdx"><g:if test="${colIdx>0}">,</g:if>'${columnName}'</g:each>
        ],
        colModel:[
            <g:each in="${columnNames}" var="columnName" status="colIdx">
                <g:if test="${colIdx>0}">,</g:if>
                {name:'${columnName}',
                    <g:if test="${columnName=='id'}">editable:false, width: 70,editrules: {integer:true}, editoptions: {maxlength: "21"}</g:if>
                    <g:elseif test="${columnName=='name'}">editable:true, editrules: {required:true}, editoptions: {maxlength: "21"}</g:elseif>
                    <g:else>editable:true, editoptions: {maxlength: "255"}</g:else>
                }
            </g:each>

        ],
        autowidth: true,
        height: 'auto',
        rowNum: 20,
        rowList: [20,50,100,500],
        pager: jQuery('#data_grid_pager'),
        viewrecords: true,
        gridview: true,
        caption: '<g:message code="dataTable.nested.browsing.header" args="${[routeRateCard.name]}"/>',
        onSelectRow: function(id){
                if(id && id!==gLastSel){
                    jQuery('#data_grid').jqGrid('restoreRow', gLastSel);
                    gLastSel=id;
                }
                var editparameters = {
                    "keys" : true,
                    "oneditfunc" : null,
                    "successfunc" : afterUpdateEvent,
                    "url" : null,
                    "extraparam" : {},
                    "aftersavefunc" : null,
                    "errorfunc": null,
                    "afterrestorefunc" : null,
                    "restoreAfterError" : true,
                    "mtype" : "POST"
                }
                jQuery('#data_grid').jqGrid('editRow', id, editparameters);
            }
        }).navGrid('#data_grid_pager',
            {   add:true,edit:false,del:true,search:false,refresh:true,csv:true
            }, // which buttons to show?
            // edit options
            {},
            // add options
            {   addCaption:'<g:message code="route.record.add.title"/>',
                afterSubmit:afterSubmitEvent,
                bSubmit: '<g:message code="route.record.button.save"/>',
                savekey: [true, 13],
                left: 300,
                width: 'auto',
                closeAfterAdd:true,
                top: 180
            },
            // delete options
            {   afterSubmit:afterSubmitEvent
            }
    ).jqGrid('navButtonAdd', '#data_grid_pager', {caption: 'csv', onClickButton: downloadFilteredCsv, title:'<g:message code="route.download.csv.filtered.tip" />' });

    $("#data_grid").jqGrid('filterToolbar',{autosearch:true});

});

function downloadFilteredCsv() {
    jQuery("#data_grid").jqGrid('excelExport',{tag:'csv', url:'<g:createLink action="filteredCsv"  params="[routeRateCardId: routeRateCard.id]" />'});
}

%{-- Called after a row has been updated --}%
function afterUpdateEvent(response, postdata) {
    var success = true;

    var json = eval('(' + response.responseText + ')');
    var messages = json.messages;

    if(json.state == 'fail') {
        success = false;
    }

    var new_id = json.id;
    return success;
}

%{-- Called for adding/deleting --}%
function afterSubmitEvent(response, postdata) {
    var success = true;

    var json = eval('(' + response.responseText + ')');
    var messages = json.messages;


    if(json.state == 'fail') {
        success = false;
    }

    var new_id = json.id;
    return [success,messages[0],new_id];
}

function nestedSearch() {
    var data = $("#data_grid").jqGrid('getGridParam','postData');
    var ignored = ["_search","nd","page","sidx","sord"];
    $.each(data, function(key, value) {
        if(ignored.indexOf(key) < 0) {
            $('#nested-search-form').append('<input type="hidden" name="curr.'+key+'" value="'+value+'" />');
        }
    });
    $('#nested-search-form').submit();
}

function searchFormToJSON() {
    var o = {};
    var a = this.serializeArray();
    $('#nested-search-form').each(a, function() {
        o[this.name] = this.value || '';
    });
    return o;
}
// ]]></script>

</body>
</html>
