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
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next)"
                  params="[partial: true]">
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
        url: '<g:createLink action="findUsers"/>',
        datatype: "json",
        colNames: [
            '<g:message code="users.th.login"/>',
            '<g:message code="users.th.name"/>',
            '<g:message code="users.th.organization"/>',
            '<g:message code="users.th.role"/>'
        ],
        colModel: [
            {name: 'userId', editable: false, width: 90, formatter: userFormatter},
            {name: 'userName', editable: false, formatter: firstAndLastNameFormatter},
            {name: 'organization', editable: false, formatter: organizationFormatter},
            {name: 'type', editable: false, sortable: false, search: false}
        ],
        sortname: 'userId',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10, 20, 50],
        pager: $(jqTablePager),
        viewrecords: true,
        gridview: true,
        onSelectRow: function (id) {
            if (id && id !== gLastSel) {
                var content = $('#showLink').clone().html().replace(/_id_/g, id);
                $("#execShowLink").html(content);
                $("#execShowLink > a").click();
                gLastSel = id;
            }
        }
    }).navGrid('#data_grid_pager_${updateColumn}',
        {
            add: false, edit: false, del: false, search: false, refresh: true, csv: false
        }, // which buttons to show?
        // edit options
        {},
        // add options
        {},
        // delete options
        {}
    );

    $(jqTableGrid).jqGrid('filterToolbar', {autosearch: true});

});

function firstAndLastNameFormatter(cellvalue, options, rowObject) {
    var displayName = ''
    if (rowObject.contact != null) {
        if (rowObject.contact.firstName || rowObject.contact.lastName) {
            displayName = rowObject.contact.firstName + ' ' + rowObject.contact.lastName
        }
    }

    return displayName
}

function userFormatter(cellvalue, options, rowObject) {
    var userIdDisplay = '<em><g:message code="table.id.format" args="['_userId_']"/></em>'
    var content = '<div class="medium">' + rowObject.userName + '</div>' + userIdDisplay;
    return content.replace(/_userId_/g, cellvalue)
}

function organizationFormatter(cellvalue, options, rowObject) {
    if (rowObject.contact != null && rowObject.contact.organization != null) {
        return rowObject.contact.organization;
    } else {
        return ''
    }
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel:"+gLastSel);
    return gLastSel == id;
}

// ]]></script>
