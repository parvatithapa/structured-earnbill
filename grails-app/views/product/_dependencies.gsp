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

<%@ page contentType="text/html;charset=UTF-8" %>

<%--
  Product dependencies

  @author Shweta Gupta
  @since  30-May-2013
--%>

<div class="content">
    <table class="dataTable" cellspacing="0" cellpadding="0" style="width: 66%">
        <tr>
            <td class="short-width1">
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="product.dependencies.category"/></content>
                    <content tag="holder.class">select-holder-nofloat</content>
                    <content tag="include.script">true</content>
                    <div id="product.dependencyItemTypes" style="display: inline;">
                        <g:select name="product.dependencyItemTypes"
                                  from="${dependencyItemTypes}"
                                  optionKey="id"
                                  optionValue="description"
                                  value=""
                                  noSelection="['':'-']"
                                  style="width:50%;"/>
                    </div>
                </g:applyLayout>
            </td>
            <td class="short-width1">
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="product.dependencies.product"/></content>
                    <content tag="holder.class">select-holder-nofloat</content>
                    <content tag="include.script">true</content>
                    <div id="product.dependencyItems" style="display: inline;">
                        <g:select name="product.dependencyItems"
                                  from="${dependencyItems}"
                                  optionKey="id"
                                  optionValue="description"
                                  value=""
                                  noSelection="['':'-']"
                                  style="width:50%;"/>
                    </div>

                </g:applyLayout>
            </td>
            <td class="short-width2">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="product.detail.dependencies.min.title"/></content>
                    <content tag="style">inp-short</content>
                    <content tag="row.class">dep-row</content>
                    <g:textField name="product.dependencyMin" value="0" class="narrow field-no-size"/>
                </g:applyLayout>
            </td>
            <td class="short-width2">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="product.detail.dependencies.max.title"/></content>
                    <content tag="style">inp-short</content>
                    <content tag="row.class">dep-row</content>
                    <g:textField name="product.dependencyMax" value="" class="narrow field-no-size"/>
                </g:applyLayout>
            </td>
            <td class="short-width3">
                <div class="row dep-row">
                    <a href="#" onclick="addDependency(); return false;" class="plus-icon">
                        &#xe026;
                    </a>
                </div>
            </td>
        </tr>
    </table>

    <div class="form-columns">
        <span><g:message code="product.detail.dependencies.products.title"/></span>
        <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <thead>
            <tr class="dependency-th small-width">
                <th><g:message code="product.detail.dependencies.id.title"/></th>
                <th><g:message code="product.detail.dependencies.name.title"/></th>
                <th><g:message code="product.detail.dependencies.min.title"/></th>
                <th><g:message code="product.detail.dependencies.max.title"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody id="dependencyItems">
                <%-- items --%>
                <g:each var="depItem" in="${dependentItems}">
                    <g:render template="dependencyRow" model="[obj:depItem, type:false]"  />
                </g:each>
            </tbody>
        </table>


        <span><g:message code="product.detail.dependencies.categories.title"/></span>
        <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <thead>
            <tr class="dependency-th small-width">
                <th><g:message code="product.detail.dependencies.id.title"/></th>
                <th><g:message code="product.detail.dependencies.name.title"/></th>
                <th><g:message code="product.detail.dependencies.min.title"/></th>
                <th><g:message code="product.detail.dependencies.max.title"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody id="dependencyTypes">
                <%-- item types --%>
                <g:each var="depType" in="${dependentTypes}">
                    <g:render template="dependencyRow" model="[obj:depType, type:true]"  />
                </g:each>
            </tbody>
        </table>
    </div>
    <script type="text/javascript">
        $('div[id="product.dependencyItemTypes"]').delegate("select","change",function(){
            var typeId = $('select[id="product.dependencyItemTypes"]').val();
            var toExcludeItemIds = [];
            var id;
            $('tr[id^="Items"]').each(function() {
                id = $(this).attr('id');
                id = id.split('.')[1];
                toExcludeItemIds.push( id );
            });

            toExcludeItemIds.push('${selectedProduct?.id}');
            toExcludeItemIds.push('${selectedProduct?.id}');
            $.ajax({
                url: '${createLink(controller: 'product', action: 'getItemsByItemType')}',
                data: {typeId: typeId, toExcludeItemIds: toExcludeItemIds},
                cache: false,
                success: function(html) {
                    $('div[id="product.dependencyItems"]').html(html);
                    $('select[id="product.dependencyItems"]').attr('style','width:50%');
                    var minDefaultValue = $('select[id="product.dependencyItems"]').find('option').length>1 ? 1 : 0
                    $('input[id="product.dependencyMin"]').val(minDefaultValue);

                    var selector = 'select[id="product.dependencyItems"]';
                    $(selector).each(function () {
                        updateSelectLabel(this);
                    });

                    $(selector).change(function () {
                        updateSelectLabel(this);
                    });
                }
            });
        });

        function addDependency(){
            var typeId = $('select[id="product.dependencyItemTypes"]').val();
            var itemId = $('select[id="product.dependencyItems"]').val();
            var min = $('input[id="product.dependencyMin"]').val();
            var max = $('input[id="product.dependencyMax"]').val();

            var typeIds = [];
            $('select[id="product.dependencyItemTypes"]').find('option').each(function() {
                typeIds.push( $(this).val() );
            });

            var itemIds = [];
            $('select[id="product.dependencyItems"]').find('option').each(function() {
                itemIds.push( $(this).val() );
            });

            var toExcludeItemIds = [];
            var id;
            $('tr[id^="Items"]').each(function() {
                id = $(this).attr('id');
                id = id.split('.')[1];
                toExcludeItemIds.push( id );
            });

            var toExcludeTypeIds = [];
            $('tr[id^="Types"]').each(function() {
                id = $(this).attr('id');
                id = id.split('.')[1];
                toExcludeTypeIds.push( id );
            });

            if (typeId == '' && itemId == ''){
                $("#error-messages ul").html("${message(code: 'product.dependencies.not.selected')}");
                $("#error-messages ul").show();
                $("#error-messages").show();
                $('html, body').animate({scrollTop: ''}, 'fast');
            } else {
                callGetDependencyList(toExcludeTypeIds, toExcludeItemIds, typeIds, itemIds, typeId, itemId);
                callAddDependencyRow(typeId, itemId, min, max);
            }
        }

        function removeDependency(trId, id, name){
            $('tr[id="'+trId+'"]').remove();

            var type = trId.split('.')[0];
            if(type.indexOf('Types')>=0){
                $('select[id="product.dependencyItemTypes"]').append('<option value="'+id+'">'+name+'</option>');
            }

            $('select[id="product.dependencyItemTypes"]').val('') ;
            $('select[id="product.dependencyItems"]').val('');
            $('select[id="product.dependencyItems"]').html('<option>-</option>');
        }

        function callGetDependencyList(toExcludeTypeIds, toExcludeItemIds, typeIds, itemIds, typeId, itemId){
            $.ajax({
                url: '${createLink(controller: 'product', action: 'getDependencyList')}',
                data: {toExcludeTypeIds: toExcludeTypeIds, toExcludeItemIds: toExcludeItemIds, typeIds: typeIds, itemIds: itemIds, typeId: typeId, itemId: itemId},
                cache: false,
                success: function(html){
                    var selector = 'select[id="product.dependencyItems"]';
                    if(typeId!="" && itemId==""){
                        $('div[id="product.dependencyItemTypes"]').html(html);
                        $('select[id="product.dependencyItemTypes"]').attr('style','width:50%');

                        $('select[id="product.dependencyItems"]').html('<option></option>');
                        $('select[id="product.dependencyItems"]').attr('style','width:50%');

                        selector = 'select[id="product.dependencyItemTypes"]';
                    } else if(typeId!="" && itemId!=""){
                        $('div[id="product.dependencyItems"]').html(html);
                        $('select[id="product.dependencyItems"]').attr('style','width:50%');
                    }
                    $(selector).each(function () {
                        updateSelectLabel(this);
                    });

                    $(selector).change(function () {
                        updateSelectLabel(this);
                    });
                }
            });
        }

        function callAddDependencyRow(typeId, itemId, min, max){
            $.ajax({
                url: '${createLink(controller: 'product', action: 'addDependencyRow')}',
                data: {typeId: typeId, itemId: itemId, min: min, max: max},
                cache: false,
                success: function(html) {
                    if(typeId!="" && itemId==""){
                        $('tbody[id="dependencyTypes"]').append(html);
                    } else if(typeId!="" && itemId!=""){
                        $('tbody[id="dependencyItems"]').append(html);
                    }
                }
            });
        }

    </script>
</div>
