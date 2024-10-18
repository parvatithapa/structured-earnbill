<%@ page import="org.apache.commons.lang.StringEscapeUtils; org.apache.commons.lang.StringUtils" %>

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
  Products list

  @author Brian Cowdery
  @since  16-Dec-2010
--%>

<g:set var="paginateAction" value="${actionName == 'products' || actionName == 'list' || actionName == 'show' ? 'products' : 'allProducts'}"/>
<g:set var="selectedCategoryId" value="${selectedCategory?.id}"/>
<g:set var="columnUpdate" value="${paginateAction == 'allProducts' ? 'column1' : 'column2'}"/>
<%-- list of products --%>
<g:if test="${products}">
    <div class="table-box">
        <div class="table-scroll">
            <table id="products" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <th>
                        <g:message code="product.th.name"/>
                    </th>
                    <g:isRoot>
                        <th class="medium header-sortable">
                            <g:remoteSort action="${paginateAction}" id="${selectedCategoryId}" sort="internalNumber" update="${columnUpdate}">
                                <g:message code="product.label.available.company.name"/>
                            </g:remoteSort>
                        </th>
                    </g:isRoot>
                    <th class="medium header-sortable">
                        <g:remoteSort action="${paginateAction}" id="${selectedCategoryId}" sort="internalNumber" update="${columnUpdate}">
                            <g:message code="product.th.internal.number"/>
                        </g:remoteSort>
                    </th>
                </tr>
                </thead>
                <tbody>

                <g:each var="product" in="${products}">

                    <tr id="product-${product.id}" class="${selectedProduct?.id == product.id ? 'active' : ''}">
                        <td>
                            <jB:secRemoteLink permissions="PRODUCT_43" class="cell double" action="show" id="${product.id}" params="['template': 'show', 'category': selectedCategoryId]" before="register(this);" onSuccess="render(data, next);">
                                <strong>${StringUtils.abbreviate(StringEscapeUtils.escapeHtml(product?.getDescription(session['language_id'])), 45)}</strong>
                                <em><g:message code="table.id.format" args="[product.id as String]"/></em>
                            </jB:secRemoteLink>
                        </td>
                        <g:isRoot>
                            <td class="medium">
                                <%
                                def totalChilds = product?.entities?.size()
                                def prodId = product?.entities
                                def multiple = false
                                if(totalChilds > 1 ) {
                                    multiple = true
                                }
                                %>
                                <jB:secRemoteLink permissions="PRODUCT_43" class="cell" action="show" id="${product.id}" params="['template': 'show', 'category': selectedCategoryId]" before="register(this);" onSuccess="render(data, next);">
                                    <g:if test="${product?.global}">
                                        <strong><g:message code="product.label.company.global"/></strong>
                                    </g:if>
                                    <g:elseif test="${multiple}">
                                        <strong><g:message code="product.label.company.multiple"/></strong>
                                    </g:elseif>
                                    <g:elseif test="${totalChilds==1}">
                                        <strong>${StringEscapeUtils.escapeHtml(product?.entities?.toArray()[0]?.description)}</strong>
                                    </g:elseif>
                                    <g:else>
                                        <strong>-</strong>
                                    </g:else>
                                </jB:secRemoteLink>
                            </td>
                        </g:isRoot>
                        <td class="medium">
                            <jB:secRemoteLink permissions="PRODUCT_43" class="cell" action="show" id="${product.id}" params="['template': 'show', 'category': selectedCategoryId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${StringEscapeUtils.escapeHtml(product?.internalNumber)}</span>
                            </jB:secRemoteLink>
                        </td>
                    </tr>

                </g:each>

                </tbody>
            </table>
        </div>
    </div>

    <div class="pager-box">
        <div class="row">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], action: paginateAction, update: columnUpdate, id:selectedCategoryId]"/>
            </div>
            <div class="download">
                <sec:access url="/product/csv">
                    <g:link action="csv" id="${selectedCategoryId}" class="pager-button">
                        <g:message code="download.csv.link"/>
                    </g:link>
                </sec:access>
            </div>
        </div>

        <jB:isPaginationAvailable total="${products?.totalCount ?: 0}">
            <div class="row-center">
                <jB:remotePaginate controller="product" action="${paginateAction}" id="${selectedCategoryId}" params="${sortableParams(params: [partial: true])}" total="${products?.totalCount ?: 0}" update="${columnUpdate}"/>
            </div>
        </jB:isPaginationAvailable>
    </div>
</g:if>

<%-- no products to show --%>
<g:if test="${!products}">
    <g:render template="noSelected"/>
</g:if>

<div class="btn-box">
    <g:if test="${selectedCategoryId}">
        <sec:ifAllGranted roles="PRODUCT_40">
            <g:link action="editProduct" params="['category': selectedCategoryId]" class="submit add button-primary"><span><g:message code="button.create.product"/></span></g:link>
        </sec:ifAllGranted>

        <g:if test="${!products && selectedCategory?.entity?.id == session['company_id']}">
            <sec:ifAllGranted roles="PRODUCT_CATEGORY_52">
                <a onclick="showConfirm('deleteCategory-${selectedCategoryId}');" class="submit delete"><span><g:message code="button.delete.category"/></span></a>
            </sec:ifAllGranted>
        </g:if>
    </g:if>
    <g:elseif test="${!params.action.equals("allProducts")}">
        <em><g:message code="product.category.not.selected.message"/></em>
    </g:elseif>
    <sec:access url="/product/allProducts">
        <g:remoteLink action="allProducts" update="column1" class="submit show" onSuccess="\$('.submit.show').hide(); closePanel(\'#column2\');" ><span><g:message code="button.show.all"/></span></g:remoteLink>
    </sec:access>
</div>

<g:render template="/confirm"
          model="['message':'product.category.delete.confirm',
                  'controller':'product',
                  'action':'deleteCategory',
                  'id':selectedCategoryId,
                 ]"/>

<script type="text/javascript">
$(function(){
    $('div#paginate a').click(function(){
        $('#column2').html('');
    });
});

    function getColumn() {
        if($(this).closest('#column1').size() > 0) {
            return 'column1';
        }
        return 'column2';
    }
</script>
