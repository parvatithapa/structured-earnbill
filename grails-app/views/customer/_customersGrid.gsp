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

<%--
  Customer table template. The customer table is used multiple times for rendering the
  main list and for rendering a separate list of sub-accounts. 

  @author Brian Cowdery
  @since  24-Nov-2010
--%>


<g:set var="csvAction" value="${actionName == 'subaccounts' ? 'subaccountsCsv' : 'csv'}"/>
<g:set var="parentId" value="${actionName == 'subaccounts' ? parent.id : null}"/>
<g:set var="updateColumn" value="${actionName == 'subaccounts' ? 'column2' : 'column1'}"/>

<!-- table tag will hold our grid
    The updateColumn variable will allow us to identify whether this table
    is for showing users (column1) or subaccounts (column2)
-->

<table id="data_grid_${updateColumn}" class="scroll jqTable" cellpadding="0" cellspacing="0"></table>
<!-- pager will hold our paginator -->
<div id="data_grid_pager_${updateColumn}" class="scroll" style="text-align:center;"></div>

<div class="btn-box">
    <sec:ifAllGranted roles="CUSTOMER_10">
        <g:if test="${parent?.customer?.isParent > 0}">
            <sec:ifAnyGranted roles="CUSTOMER_17, CUSTOMER_18">
                <g:link action="edit" params="[parentId: parent.id]" class="submit add button-primary"><span><g:message code="customer.add.subaccount.button"/></span></g:link>
            </sec:ifAnyGranted>
        </g:if>
        <g:else>
            <sec:ifAllGranted roles="CUSTOMER_17">
                <g:link action='edit' class="submit add button-primary"><span><g:message code="button.create"/></span></g:link>
            </sec:ifAllGranted>
        </g:else>
    </sec:ifAllGranted>
</div>

<div id="parentAndChild" style="display: none;">
    <g:remoteLink action="subaccounts" id="_id_" before="if(!isRowSelected(_id_)) return false;register(this);" onSuccess="render(data, next);"><img
            src="${resource(dir: 'images', file: 'icon17.gif')}"
            alt="parent and child"/><span>_ch_</span></g:remoteLink>
</div>

<div id="parentOnly" style="display: none;">
    <g:remoteLink action="subaccounts" id="_id_" before="if(!isRowSelected(_id_)) return false;register(this);" onSuccess="render(data, next);"><img
            src="${resource(dir: 'images', file: 'icon18.gif')}"
            alt="parent"/><span>_ch_</span></g:remoteLink>
</div>

<div id="childOnly" style="display: none;">
    <img src="${resource(dir: 'images', file: 'icon19.gif')}" alt="child"/>
</div>

<div id="showLink${updateColumn}" style="display: none;">
    <g:remoteLink class="cell" action="show" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
</div>

<div id="execShowLink${updateColumn}" style="display: none;">
</div>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gLastSel${updateColumn} = -1;
var jqTableGrid${updateColumn} = $('#data_grid_${updateColumn}');
var jqTablePager${updateColumn} = $('#data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid${updateColumn}).jqGrid({
        url:'<g:createLink
            action="${actionName == 'subaccounts' ? 'findSubaccounts' : 'findCustomers'}"
            id="${parentId}"/>',
        datatype: "json",
        colNames:[
            '<g:message code="customer.table.th.user.id"/>',
            '<g:message code="customer.table.th.name"/>',
            <g:isRoot>'<g:message code="customer.table.th.user.company.name"/>',</g:isRoot>
            '<g:message code="customer.table.th.status"/>',
            '<g:message code="customer.table.th.balance"/>',
            '<g:message code="customer.table.th.hierarchy"/>'
        ],
        colModel:[
            { name: 'userId', editable: false, width: 90 },
            { name: 'userName', editable: false},
            <g:isRoot>{ name: 'company', editable: false }, </g:isRoot>
            { name: 'status', editable: false, width: 90, search: false, formatter: statusFormatter },
            { name: 'balance', editable: false, search: false, sortable: false, formatter: balanceFormatter},
            { name: 'hierarchy', editable: false, search: false, sortable: false, formatter: hierarchyFormatter }
        ],
        sortname: 'userId',
        sortorder: 'desc',
        autowidth: true,
        height: 'auto',
        rowNum: 10,
        rowList: [10,20,50],
        pager: $(jqTablePager${updateColumn}),
        viewrecords: true,
        gridview: true,
        onSelectRow: function(id){
            if(id && id!==gLastSel${updateColumn}){
                var content = $('#showLink${updateColumn}').clone().html().replace(/_id_/g, id);
                $("#execShowLink${updateColumn}").html(content);
                $("#execShowLink${updateColumn} > a").click();
                gLastSel${updateColumn}=id;
            }
        }
    }).navGrid('#data_grid_pager_${updateColumn}',
            {   add:false,edit:false,del:false,search:false,refresh:true,csv:true
            }, // which buttons to show?
            // edit options
            {},
            // add options
            {},
            // delete options
            {}
    ).jqGrid('navButtonAdd', '#data_grid_pager_${updateColumn}', {caption: 'csv', onClickButton: downloadFilteredCsv, title:'<g:message code="jqview.download.csv.filtered.tip" />' });

    $(jqTableGrid${updateColumn}).jqGrid('filterToolbar',{autosearch:true});

});

function statusFormatter (cellvalue, options, rowObject) {
    if(cellvalue == 'suspended') {
        return '<img src="${resource(dir:"images", file:"icon16.gif")}" alt="suspended" />';
    } else if(cellvalue == 'overdue') {
       return '<img src="${resource(dir:"images", file:"icon15.gif")}" alt="overdue" />';
    }
    return '';
}

function hierarchyFormatter (cellvalue, options, rowObject) {
    var content = '';
    if(cellvalue.parent && cellvalue.child) {
        content = $('#parentAndChild').clone().html().replace(/_id_/g, rowObject.userId).replace(/_ch_/g, cellvalue.children);
    } else if(cellvalue.parent && !cellvalue.child) {
        content = $('#parentOnly').clone().html().replace(/_id_/g, rowObject.userId).replace(/_ch_/g, cellvalue.children);
    } else if(!cellvalue.parent && cellvalue.child) {
        content = $('#childOnly').clone().html();
    }
    return content;
}

// A simple formatter that concatenates the currency symbol with the balance
function balanceFormatter (cellvalue, options, rowObject) {
    return rowObject.currencySymbol + cellvalue.toFixed(2);
}


function downloadFilteredCsv() {
    $(jqTableGrid${updateColumn}).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="customer" action="${csvAction}" id="${parentId}"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gLastSel${updateColumn}:"+gLastSel${updateColumn});
    return gLastSel${updateColumn} == id;
}
// ]]></script>
