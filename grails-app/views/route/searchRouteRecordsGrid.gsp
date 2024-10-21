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
    <meta name="layout" content="main"/>
    <!-- r:require module="jqGrid" / -->
    <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection"/>
    <g:javascript src="jquery.jqGrid.min.js"/>
    <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"/>
</head>

<body>

<g:set var="nestedFilters" value="${[_routeId: route.id] + matchedColumnValues}"/>
<form id='_export' method="post" action='<g:createLink action="filteredCsv"/>'>
    <input type="hidden" name="_routeId" value="${route.id}"/>
    <input type="hidden" name="_filters" id="_filters" value=""/>
</form>

<g:link action="list" params="[id: route.id]"><g:message code="button.back"/></g:link>
<!-- table tag will hold our grid -->
<table id="data_grid" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager" class="scroll" style="text-align:center;"></div>

<div class="form-edit">&nbsp;</div>

<div class="form-edit">
    <g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>

    <div class="heading">
        <strong>
            <g:message code="dataTable.nested.search.header"/>
        </strong>
    </div>

    <div class="form-hold">
        <div class="form-columns">
            <div>

            %{-- list of executeble queries --}%
                <g:form name="exec-query" action="execQuery">
                    <g:hiddenField name="nestedDepth" value="${nestedTables ? nestedTables.size() : 0}"/>
                    <input type="hidden" name="curr.routeId" value="${route.id}"/>
                    <g:each in="${matchedColumnValues}" var="mcentry">
                        <g:each in="${mcentry.value}" var="mcvalue">
                            <input type="hidden" name="curr.${mcentry.key}" value="${mcvalue}"/>
                        </g:each>
                    </g:each>
                    <g:each in="${nestedTables}" var="nestedTable" status="depth">
                        <g:hiddenField name="nestedTables[${depth}]" value="${nestedTables[depth]}"/>
                        <g:hiddenField name="nestedRouteIds[${depth}]" value="${nestedRouteIds[depth]}"/>
                        <g:hiddenField name="nestedColumns[${depth}]" value="${nestedColumns[depth]}"/>
                        <g:hiddenField name="nestedValues[${depth}]" value="${nestedValues[depth]}"/>
                    </g:each>

                    <g:if test="${queries}">
                        <div id="queryList" >
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="dataTable.nested.mysearches"/></content>
                                <content tag="label.for">queryId</content>
                                <content tag="icon">
                                    <a class="submit-sm left-right-margin no-text play" onclick="execQuery();"><span> </span></a>
                                    <a id="delQueryBtn" class="submit-sm no-text delete" onclick="deleteQueryEntry();"><span> </span></a>
                                </content>
                                <g:select name="queryId" from="${queries}" optionKey="id" optionValue="name" onchange="checkIfCanDeleteQuery();" onkeypress="if (event.keyCode == 13) execQuery();"/>
                            </g:applyLayout>
                        </div>
                    </g:if>
                </g:form>

                %{-- Nested search form --}%
                <g:form name="nested-search-form" action="nestedSearch">
                    <fieldset>
                        <input type="hidden" name="curr.routeId" value="${route.id}"/>
                        <g:each in="${matchedColumnValues}" var="mcentry">
                            <g:each in="${mcentry.value}" var="mcvalue">
                                <input type="hidden" name="curr.${mcentry.key}" value="${mcvalue}"/>
                            </g:each>
                        </g:each>
                        <div class="row">
                            <label for=""><g:message code="dataTable.nested.search.columns.match.prompt"/></label>
                            <span class="normal" style="display:table;">
                                <g:each in="${columnNames}" var="columnName" status="colIdx">
                                    <g:checkBox name="match.col.${columnName}"/>${columnName}
                                </g:each>
                            </span>
                        </div>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="dataTable.nested.search.table.prompt"/></content>
                            <content tag="label.for">table</content>

                            <g:select name="match.table" from="${tableNames}" optionKey="id" optionValue="name"/>
                        </g:applyLayout>

                        <g:hiddenField name="nestedDepth" value="${nestedTables ? nestedTables.size() : 0}"/>
                        <g:hiddenField name="rootTableId" value="${rootTableId}"/>
                        <g:each in="${nestedTables}" var="nestedTable" status="depth">
                            <div class="row">
                                <label for="">
                                    <g:message code="dataTable.nested.search.depth.label"/>&nbsp;${depth + 1}
                                </label>
                                <span class="normal">
                                    <span class="strong"><g:message code="dataTable.nested.search.table.label"/>:</span>
                                    ${nestedTables[depth]}
                                    <g:hiddenField name="nestedTables[${depth}]" value="${nestedTables[depth]}"/>
                                    <g:hiddenField name="nestedRouteIds[${depth}]" value="${nestedRouteIds[depth]}"/>

                                    <span class="strong">
                                        &nbsp;&nbsp;<g:message code="dataTable.nested.search.columns.label"/>:
                                    </span>
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
                                    ${nestedValues[depth]?.decodeHTML()}
                                <g:hiddenField name="nestedValues[${depth}]" value="${nestedValues[depth]}"/>
                                </span>
                            </div>
                        </g:each>

                    </fieldset>
                </g:form>
            </div>
        </div>
        <div><br/></div>
        <div class="buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="nestedSearch();">
                    <span><g:message code="button.search"/></span>
                </a>
                </li>
                <g:if test="${nestedTables}">
                    <li><a class="submit save button-primary" onclick="$('#save-query-dialog').dialog('open');">
                        <span><g:message code="button.save"/></span>
                    </a>
                    </li>
                </g:if>
                <li>
                    <g:link class="submit cancel" action="search" id="${route.id}">
                        <span><g:message code="button.clear.search"/></span>
                    </g:link>
                </li>
            </ul>
        </div>
    </div>


    %{-- Delete query dialog --}%
    <g:each in="${queries}" var="query">
        <g:render template="/confirm"
                  model="[   'message': 'dataTable.query.delete',
                          'controller': 'route',
                              'action': 'deleteQuery',
                                  'id': query.id,
                                'ajax': true,
                               'onYes': 'removeQueryEntry()'
                  ]"/>
    </g:each>

    %{-- Save query dialog --}%
    <div id="save-query-dialog" class="bg-lightbox" title="<g:message code="dataTables.query.save.title"/>" style="display:none;">
        <div id="save-query-dialog-errors" style="display: none; font-size: 12px;"></div>
        <g:formRemote         name = "save-query-confirm"
                               url = "[controller: 'route', action: 'saveQuery']"
                            update = "save-query-dialog-response"
                            before = "\$('#save-query-dialog-errors').empty();"
                             after = "checkSaveResponse();"
                      asynchronous = "false">
            <g:hiddenField name="nestedDepth" value="${nestedTables ? nestedTables.size() : 0}"/>
            <g:hiddenField name="rootTableId" value="${rootTableId}"/>
            <g:each in="${nestedRouteIds}" var="nestedRouteId" status="depth">
                <g:hiddenField name="nestedRouteIds[${depth}]" value="${nestedRouteId}"/>
                <g:hiddenField name="nestedColumns[${depth}]" value="${nestedColumns[depth]}"/>
            </g:each>

            <table>
                <tbody><tr>
                    <td class="col2">
                        <g:message code="dataTable.query.name"/>
                    </td>
                    <td class="col2">
                        <g:textField id="save-query-name" name="name"/>
                    </td>
                </tr>
                <sec:ifAllGranted roles="DATA_TABLES_170">
                    <tr>
                        <td class="col2">
                            <g:message code="dataTable.query.global"/>
                        </td>
                        <td class="col2">
                            <g:checkBox name="global"/>
                        </td>
                    </tr>
                </sec:ifAllGranted>
                </tbody>
            </table>
        </g:formRemote>
        <div id="save-query-dialog-response" style="display: none;"></div>
    </div>
</div>



<script type="text/javascript">// <![CDATA[
%{-- map query id to boolean indicating if user may delete it --}%
var queryExecMap = {};

/* when the page has finished loading.. execute the follow */
var gLastSel = -1;
$(document).ready(function () {
    jQuery("#data_grid").jqGrid({
        url: '<g:createLink action="findRoutes" params="${[_routeId: route.id]+matchedColumnValues}"/>',
        editurl: '<g:createLink action="recordEdit" params="[_routeId: route.id]"/>',
        datatype: "json",
        colNames: [
            <g:each in="${columnNames}" var="columnName" status="colIdx"><g:if test="${colIdx>0}">, </g:if>'${columnName}'</g:each>
        ],
        colModel: [
            <g:each in="${columnNames}" var="columnName" status="colIdx">
            <g:if test="${colIdx>0}">,
            </g:if>
            {name: '${columnName}',
                <g:if test="${columnName=='id'}">editable: false, width: 70, editrules: {integer: true}, editoptions: {maxlength: "21"}</g:if>
                <g:elseif test="${columnName=='name'}">editable: true, editrules: {required: true}, editoptions: {maxlength: "21"}</g:elseif>
                <g:else>editable: true, editoptions: {maxlength: "255"}</g:else>
            }
            </g:each>

        ],
        autowidth: true,
        height: 'auto',
        rowNum: 20,
        rowList: [20, 50, 100, 500],
        pager: jQuery('#data_grid_pager'),
        viewrecords: true,
        gridview: true,
        caption: '<g:message code="dataTable.nested.browsing.header" args="${[route.name]}"/>',
        onSelectRow: function (id) {
            if (id && id !== gLastSel) {
                jQuery('#data_grid').jqGrid('restoreRow', gLastSel);
                gLastSel = id;
            }
            var editparameters = {
                "keys": true,
                "oneditfunc": null,
                "successfunc": afterUpdateEvent,
                "url": null,
                "extraparam": {},
                "aftersavefunc": null,
                "errorfunc": null,
                "afterrestorefunc": null,
                "restoreAfterError": true,
                "mtype": "POST"
            }
            jQuery('#data_grid').jqGrid('editRow', id, editparameters);
        }
    }).navGrid('#data_grid_pager',
            {   add: true, edit: false, del: true, search: false, refresh: true, csv: true
            }, // which buttons to show?
            // edit options
            {},
            // add options
            {   addCaption: '<g:message code="route.record.add.title"/>',
                afterSubmit: afterSubmitEvent,
                bSubmit: '<g:message code="route.record.button.save"/>',
                savekey: [true, 13],
                left: 300,
                closeAfterAdd:true,
                top: 180
            },
            // delete options
            {   afterSubmit: afterSubmitEvent
            }
    ).jqGrid('navButtonAdd', '#data_grid_pager', {caption: 'csv', onClickButton: downloadFilteredCsv, title: '<g:message code="route.download.csv.filtered.tip" />' });

    $("#data_grid").jqGrid('filterToolbar', {autosearch: true});

    //Save dialog
    setTimeout(function () {
        $('#save-query-dialog').dialog({
            autoOpen: false,
            height: 175,
            width: 375,
            modal: true,
            buttons: {
                '<g:message code="prompt.ok"/>': function () {
                    $("#save-query-confirm").submit();
                },
                '<g:message code="prompt.cancel"/>': function () {
                    $(this).dialog('close');
                }
            },
            open: function( event, ui ) {
                $("#save-query-name").val('');
                $('#save-query-dialog-errors').hide();
                $('#save-query-dialog').dialog( 'option', 'width', 375 );
                $('#save-query-dialog').dialog( 'option', 'height', 175 );
            }
        });
    }, 100);


    %{-- map indicating if a query is deletable --}%
    <g:each in="${queries}" var="query">
        <g:if test="${query.userId != session['user_id']}">
            <sec:ifAllGranted roles="DATA_TABLES_170">
                queryExecMap['${query.id}'] = true;
            </sec:ifAllGranted>
            <sec:ifNotGranted roles="DATA_TABLES_170">
                queryExecMap['${query.id}'] = false;
            </sec:ifNotGranted>
        </g:if>
        <g:else>
            queryExecMap['${query.id}'] = true;
        </g:else>
    </g:each>

    checkIfCanDeleteQuery();
});

function downloadFilteredCsv() {
    jQuery("#data_grid").jqGrid('excelExport', {tag: 'csv', url: '<g:createLink action="filteredCsv"  params="[_routeId: route.id]" />'});
}

%{-- Called after a row has been updated --}%
function afterUpdateEvent(response, postdata) {
    var success = true;

    var json = eval('(' + response.responseText + ')');
    var messages = json.messages;

    if (json.state == 'fail') {
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


    if (json.state == 'fail') {
        success = false;
    }

    var new_id = json.id;
    return [success, messages[0], new_id];
}

function nestedSearch() {
    var data = $("#data_grid").jqGrid('getGridParam', 'postData');
    var ignored = ["_search", "nd", "page", "sidx", "sord"];
    $.each(data, function (key, value) {
        if (ignored.indexOf(key) < 0) {
            $('#nested-search-form').append('<input type="hidden" name="curr.' + key + '" value="' + value + '" />');
        }
    });
    $('#nested-search-form').submit();
}

function searchFormToJSON() {
    var o = {};
    var a = this.serializeArray();
    $('#nested-search-form').each(a, function () {
        o[this.name] = this.value || '';
    });
    return o;
}

%{-- Delete a saved query on server --}%
function saveQuery() {
    var data = $("#data_grid").jqGrid('getGridParam', 'postData');
    var ignored = ["_search", "nd", "page", "sidx", "sord"];
    $.each(data, function (key, value) {
        if (ignored.indexOf(key) < 0) {
            $('#exec-query').append('<input type="hidden" name="curr.' + key + '" value="' + value + '" />');
        }
    });
    $('#exec-query').submit();
}

%{-- Execute a saved query  --}%
function execQuery() {
    if($('#queryId').hasClass('disabled')) return;

    var data = $("#data_grid").jqGrid('getGridParam', 'postData');
    var ignored = ["_search", "nd", "page", "sidx", "sord"];
    $.each(data, function (key, value) {
        if (ignored.indexOf(key) < 0) {
            $('#exec-query').append('<input type="hidden" name="curr.' + key + '" value="' + value + '" />');
        }
    });
    $('#exec-query').submit();
}

%{-- Delete a saved query on server --}%
function deleteQueryEntry() {
    if($('#delQueryBtn').hasClass('disabled')) return;
    var id = $('#queryId option:selected').val();
    showConfirm('deleteQuery-' + id);
}

%{-- Remove the current selected query query from the list of options --}%
function removeQueryEntry() {
    $('#queryId option:selected').remove();
    var nrQueries = $('#queryId > option').length;
    if(nrQueries <= 0) {
        $('#queryList').remove();
    }
}


%{-- Check if the current user can delete the query --}%
function checkIfCanDeleteQuery() {
    var id = $('#queryId option:selected').val();
    if(queryExecMap[id]) {
        $('#delQueryBtn').removeClass('disabled');
    } else {
        $('#delQueryBtn').addClass('disabled');
    }
}

%{-- Called after a query has been saved. Check if the response is ok else display the error --}%
function checkSaveResponse() {
    if($('#save-query-dialog-response').text() == 'ok') {
        $('#save-query-dialog-errors').hide();
        $('#save-query-dialog').dialog('close');
    } else {
        $('#save-query-dialog-response :first').remove().appendTo('#save-query-dialog-errors');
        $('#save-query-dialog-errors').show();
        $('#save-query-dialog').dialog( 'option', 'width', 575 );
        $('#save-query-dialog').dialog( 'option', 'height', 275 );
    }
}
// ]]></script>

</body>
</html>
