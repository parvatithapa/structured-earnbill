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

<div class="btn-box">
    <sec:ifAllGranted roles="PRODUCT_CATEGORY_50">
        <g:link action="editCategory" class="submit add button-primary" params="${[add: true]}"><span><g:message code="button.create.category"/></span></g:link>
    </sec:ifAllGranted>

    <sec:ifAllGranted roles="PRODUCT_CATEGORY_51">
        <a href="#" onclick="return editCategory();" class="submit edit"><span><g:message code="button.edit"/></span></a>
    </sec:ifAllGranted>
</div>

<div id="showLink" style="display: none;">
    <g:remoteLink class="cell" action="products" params="[categoryId:'_id_']" id="_id_" before="register(this);" onSuccess="render(data, next);">

    </g:remoteLink>
</div>

<div id="execShowLink" style="display: none;">
</div>

<div id="editLink" style="display: none;">
    <g:link class="cell" action="editCategory" params="['template': 'show', 'category': _id_]" id="_id_" onClick="a_onClick()">

    </g:link>
</div>

<div id="execEditLink" style="display: none;">
</div>
<!-- edit category control form -->
<g:form name="categoryEditFormTemplate" controller="product" action="editCategory">
    <g:hiddenField name="id" id="editformSelectedCategoryId" value="_categoryId_"/>
</g:form>

<g:form name="category-edit-form" controller="product" action="editCategory">
</g:form>

<script type="text/javascript">// <![CDATA[
/* when the page has finished loading.. execute the follow */
var gridLastCategorySel = -1;
var jqTableGrid${updateColumn} = $('#' + '${updateColumn}' + ' #data_grid_${updateColumn}');
var jqTablePager${updateColumn} = $('#' + '${updateColumn}' + ' #data_grid_pager_${updateColumn}');
$(document).ready(function () {
    $(jqTableGrid${updateColumn}).jqGrid({
        url:'<g:createLink action="findCategories"/>',
        datatype: "json",
        colNames:[
            '<g:message code="product.category.th.name"/>',
            <g:isRoot>'<g:message code="product.label.company.name"/>',</g:isRoot>
            '<g:message code="product.category.th.type"/>'
        ],
        colModel:[
            { name: 'categoryId', editable: false, width: 90, search: false, sortable:false, formatter: categoryFormatter},
            <g:isRoot>{ name: 'company', editable: false, search: false, sortable:false, formatter: companyFormatter }, </g:isRoot>
            { name: 'lineType', editable: false, search: false, sortable:false, formatter: descriptionFormatter }
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
                var content = $('#showLink').clone().html().replace(/_id_/g, id);
                $("#execShowLink").html(content);
                var content = $('#editLink').clone().html().replace(/_id_/g, id);
                $("#execEditLink").html(content);
                $("#execShowLink > a").click();
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

function companyFormatter (cellvalue, options, rowObject) {
    var content;
    if (rowObject.global){
        content = '<strong><g:message code="product.label.company.global"/></strong>'
    }else if (rowObject.multiple){
        content = '<strong><g:message code="product.label.company.multiple"/></strong>'
    }else {
        content = cellvalue
    }
    return content
}

function categoryFormatter (cellvalue, options, rowObject) {
    var categoryIdDisplay = '<em><g:message code="table.id.format" args="['_categoryId_']"/></em>'
    var content = '<div class="medium"><strong>' + rowObject.name + '</strong></div>' + categoryIdDisplay;
    return content.replace(/_categoryId_/g, cellvalue)
}

function descriptionFormatter (cellvalue, options, rowObject) {
    return cellvalue.description;
}

function downloadFilteredCsv() {
    $(jqTableGrid${updateColumn}).jqGrid('excelExport',{tag:'csv', url:'<g:createLink controller="product" action="csv"/>'});
}

function isRowSelected(id) {
    //console.log("id:"+id+" gridLastCategorySel:"+gridLastCategorySel);
    return gridLastCategorySel == id;
}

function editCategory() {
    $("#execEditLink > a")[0].click();
    return false;
}
// ]]></script>
