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

<%@ page import="com.sapienter.jbilling.server.item.db.ItemTypeDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.Constants" %>
<%@ page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy"%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%--
  Shows the product list and provides some basic filtering capabilities.

  @author Brian Cowdery
  @since 23-Jan-2011
--%>

<div id="product-box">

    <!-- filter -->
    <div class="form-columns">
        <g:formRemote name="products-filter-form" url="[action: 'edit']" update="ui-tabs-products" method="GET">
            <g:hiddenField name="_eventId" value="products"/>
            <g:hiddenField name="execution" value="${flowExecutionKey}"/>

            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="filters.title"/></content>
                <content tag="label.for">filterBy</content>
                <g:textField name="filterBy" class="field default" placeholder="${message(code: 'products.filter.by.default')}" value="${params.filterBy}"/>
            </g:applyLayout>

            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="order.label.products.category"/></content>
                <content tag="label.for">typeId</content>
                <content tag="include.script">true</content>
                <g:select name="typeId" from="${itemTypes}"
                          noSelection="['': message(code: 'filters.item.type.empty')]"
                          optionKey="id" optionValue="description"
                          value="${params.typeId && !params.typeId.isEmpty() ? params.typeId as Integer : ''}"/>
            </g:applyLayout>
        </g:formRemote>

        <script type="text/javascript">
            $('#products-filter-form :input[name=filterBy]').blur(function() { $('#products-filter-form').submit(); });
            $('#products-filter-form :input[name=typeId]').change(function() { $('#products-filter-form').submit(); });
        </script>
    </div>

    <!-- product list -->
    <div class="table-box tab-table">
        <div class="table-scroll">
            <table id="products" cellspacing="0" cellpadding="0">
                <tbody>
				
				<g:set var="hasSubscriptionProduct" value="${hasSubscriptionProduct}" />
				<g:set var="productService" bean="productService"/>
                <g:each var="product" in="${products}">
                    <g:set var="productDetails" value="${productService.getProductDetails(product?.id, order)}"/>
                    <g:set var="isAssetMgmt" value="${productDetails.isAssetMgmt}" />
                    <g:set var="isSubsProd" value="${productDetails.isSubsProd}" />
                    <tr>
                        <td>
                            <g:remoteLink class="cell double" action="edit" id="${product.id}" params="[_eventId: isAssetMgmt ? 'initAssets' : 'addLine']" update="${isAssetMgmt ? 'assets-box-add' : 'ui-tabs-edit-changes'}" method="GET" >
                                <strong>${productDetails?.description}</strong>
                                <em><g:message code="table.id.format" args="[product.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <td>
                            <g:remoteLink class="cell double" action="edit" id="${product.id}" params="[_eventId: (hasSubscriptionProduct && isSubsProd) ? 'subscription' : (isAssetMgmt ? 'initAssets' : 'addLine'), isPlan : false, isAssetMgmt : isAssetMgmt]" update="${ (hasSubscriptionProduct && isSubsProd) ? 'subscription-box-add' : (isAssetMgmt ? 'assets-box-add' : 'ui-tabs-edit-changes')}" method="GET" >
                                <g:if test="${productDetails?.global}">
                                    <strong><g:message code="product.label.company.global"/></strong>
                                </g:if>
                                <g:elseif test="${productDetails?.multiple}">
                                    <strong><g:message code="product.label.company.multiple"/></strong>
                                </g:elseif>
                                <g:else>
                                    <strong>${productDetails?.entityDescription}</strong>
                                </g:else>
                            </g:remoteLink>
                        </td>
                        <td class="small">

                            <g:remoteLink class="cell double" action="edit" id="${product.id}" params="[_eventId: isAssetMgmt ? 'initAssets' : 'addLine']" update="${ isAssetMgmt ? 'assets-box-add' : 'ui-tabs-edit-changes'}" method="GET" >
                                <span>${productDetails?.internalNumber}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell double" action="edit" id="${product.id}" 
                                    params="[_eventId: (hasSubscriptionProduct && isSubsProd) ? 'subscription' : isAssetMgmt ? 'selectProductWithAsset' : 'addLine', isPlan : false]" update="${ (hasSubscriptionProduct && isSubsProd) ? 'subscription-box-add' : (isAssetMgmt ? 'assets-box-add' : 'ui-tabs-edit-changes')}" method="GET">
                                <g:set var="price" value="${productDetails?.price}"/>
								<g:if test="${price != null && price.type.equals(PriceModelStrategy.LINE_PERCENTAGE.name())}" >
                                    <g:formatNumber number="${price?.getRateAsDecimal()}" formatName="money.format"/>
                                </g:if>
                                <g:else>
                                    <g:formatNumber number="${price?.getRateAsDecimal()}" type="currency" formatName="price.format" currencySymbol="${price?.currencySymbol}"/>
                                </g:else>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
    </div>
    <div class="pager-box">
        <div class="results">
            <div style="display: none; visibility: hidden">
                <g:set var="steps" value="${[10,20,50]}" />
                <g:each var="max" in="${steps}">
                    <g:set var="extraParams" value="${extraParams?extraParams:[:]}"/>
                    <g:remoteLink     id = "page-size-prod-${max}"
                                  action = "edit"
                                  update = "ui-tabs-products"
                                  method = "GET"
                                  params = "${sortableParams(params: [ partial: true,
                                                                           max: max,
                                                                      _eventId: 'products',
                                                                        typeId: params.typeId ?: "",
                                                                      filterBy: params.filterBy ?: "" ])}">
                        ${max}
                    </g:remoteLink>
                </g:each>
            </div>

            <div class="select-holder select-holder_small"><span class="select-value"></span>
                <g:select       class = "pager-button"
                                 name = "page-size-prod"
                                 from = "${steps}"
                                value = "${params.max?.toInteger()}"
                             onchange = "pageSizeChange(this);"
                          optionValue = "${{it + " " + message(code:"pager.show.max.results")}}">
                </g:select>
            </div>

        </div>

        <div class="row-right">
            <jB:remotePaginate action = "edit"
                               params = "${sortableParams(params: [ partial: true,
                                                                   _eventId: 'products',
                                                                        max: maxProductsShown,
                                                                     typeId: params.typeId ?: "",
                                                                   filterBy: params.filterBy ?: ""])}"
                                total = "${products.totalCount ?: 0}"
                               update = "ui-tabs-products"
                               method = "GET"/>
        </div>
    </div>

</div>

<script type="text/javascript">
    function pageSizeChange(obj) {
        $('#page-size-prod-'+obj.value).click();
    }

    $(document).ready(function() {
        $("select[name='page-size-prod']").each(function () {
            updateSelectLabel(this);
        });

        $("select[name='page-size-prod']").change(function () {
            updateSelectLabel(this);
        });
    });
</script>

<g:render template="assetDialogs"/>
<g:render template="subscriptionDialog"/>
