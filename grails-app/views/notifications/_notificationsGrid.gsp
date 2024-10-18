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

<g:set var="updateColumn" value="column2" />
<g:set var="categoryId" value="${selected ? selected : categoryId}" />

<!-- table tag will hold our grid
    The updateColumn variable will allow us to identify whether this table
    is for showing entities (column1) or their children (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <g:remoteLink action="editNotification" class="submit add button-primary"
                  before="register(this);"
                  onSuccess="render(data, next);" params="['categoryId':categoryId]">
        <span><g:message code="button.create.notification"/></span>
    </g:remoteLink>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="show" params="['template': 'show']" id="_id_" before="register(this);" onSuccess="render(data, next); animateToTheTop();">

    </g:remoteLink>
</div>

<div id="execShowLink" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSelNotif = -1;
var jqTableGridNotif = $('#data_grid_${updateColumn}');
var jqTablePagerNotif = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGridNotif).jqGrid({
        url:'<g:createLink action="findNotifications" id="${categoryId}"/>',
        datatype: "json",
        colNames:[
            '<g:message code="title.notification"/>',
            '<g:message code="title.notification.active"/>'
        ],
        colModel:[
            { name: 'notificationId', editable: false, width: 150, formatter: notificationFormatter },
            { name: 'active', editable: false, search: false, sortable: false, width: 30, formatter: activeFormatter }
        ],
        sortname: 'notificationId',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePagerNotif),
        viewrecords: true,
        gridview: true,
        onSelectRow: function(id){
            if(id && id!==gLastSelNotif){
                var content = $('#showLink').clone().html().replace(/_id_/g, id);
                $("#execShowLink").html(content);
                $("#execShowLink > a").click();
                gLastSelNotif=id;
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

    $(jqTableGridNotif).jqGrid('filterToolbar',{autosearch:true});

});

function notificationFormatter (cellvalue, options, rowObject) {
    var notificationIdDisplay = '<em><g:message code="table.id.format" args="['_notificationId_']"/></em>'
    var content = '<div class="medium">' + rowObject.description + '</div>' + notificationIdDisplay;
    return content.replace(/_notificationId_/g, cellvalue)
}

function activeFormatter (cellvalue, options, rowObject) {
    if(cellvalue == true) {
        return '<g:message code="prompt.yes"/>';
    } else {
        return '<g:message code="prompt.no"/>';
    }
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSelNotif:"+gLastSelNotif);
    return gLastSelNotif == id;
}

function animateToTheTop(){
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}
// ]]></script>
