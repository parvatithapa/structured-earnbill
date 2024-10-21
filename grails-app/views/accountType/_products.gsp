%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.pricing.db.PriceModelStrategy"%>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.ItemTypeDTO" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Shows the product list and provides some basic filtering capabilities.

  @author Panche Isajeski, Brian Cowdery
  @since 05/16/2013
--%>

<div class="heading">
    <strong><g:message code="builder.products.title"/></strong>
</div>

<div class="box no-buttons">
    <div class="sub-box">
      <!-- filter -->
        <div class="form-columns">
          <g:formRemote name="products-filter-form" url="[action: 'filterProducts']" update="products-column">
              <g:hiddenField name="accountTypeId" value="${accountType?.id ?: params.accountTypeId}"/>
  
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
              $(document).ready(function() {
                  $('#filterBy').blur(function() { $('#products-filter-form').submit(); });
                  $('#typeId').change(function() { $('#products-filter-form').submit(); });
                  placeholder();
              });
          </script>
      </div>
  
      <!-- product list -->
      <div class="table-box tab-table">
          <div class="table-scroll">
              <table id="products" cellspacing="0" cellpadding="0">
                  <tbody>

                  <g:each var="product" in="${products}">
                      <tr>
                          <td>
                              <g:remoteLink class="cell double" action="productPrices" id="${product.id}" params="[accountTypeId: accountType?.id ?: params.accountTypeId]" update="prices-column">
                                  <strong>${StringEscapeUtils.escapeHtml(product?.getDescription(session['language_id']))}</strong>
                              </g:remoteLink>
                          </td>
                          <td class="small">
                              <g:remoteLink class="cell" action="productPrices" id="${product.id}" params="[accountTypeId: accountType?.id ?: params.accountTypeId]" update="prices-column">
                                  <span>${StringEscapeUtils.escapeHtml(product?.internalNumber)}</span>
                              </g:remoteLink>
                          </td>
                          <td class="medium">
                              <g:remoteLink class="cell" action="productPrices" id="${product.id}" params="[accountTypeId: accountType?.id ?: params.accountTypeId]" update="prices-column">
                                  <g:set var = "price" value="${product.getPrice(TimezoneHelper.currentDateForTimezone(session['company_timezone']), session['company_id'] as Integer) }" />
                                  <g:formatPriceForDisplay price="${price}" />	
                              </g:remoteLink>
                          </td>
                      </tr>
                  </g:each>

                  </tbody>
              </table>
          </div>
          <div class="pager-box">
              <div class="row">
                  <div class="results">
                      <g:render template="/layouts/includes/pagerShowResults"
                                model="[steps: [10, 20, 50], update: 'products-column', contactFieldTypes: contactFieldTypes, action: 'filterProducts',
                                        extraParams: [
                                                accountTypeId: accountType?.id ?: params.accountTypeId,
                                                typeId: params.typeId ?: '',
                                                filterBy: params.filterBy ?: ''
                                        ]]"/>
                  </div>
              </div>
              <jB:isPaginationAvailable total="${products.totalCount ?: 0}">
                <div class="row-center">
                    <jB:remotePaginate action="filterProducts"
                                     params="${sortableParams(params: [partial: true, accountTypeId: accountType?.id ?: params.accountTypeId, max: params.max, typeId: params.typeId ?: "", filterBy: params.filterBy ?: ""])}"
                                     total="${products.totalCount ?: 0}"
                                     update="products-column"
                                     method="GET"/>
                </div>
              </jB:isPaginationAvailable>
          </div>
      </div>
    </div>
</div>

