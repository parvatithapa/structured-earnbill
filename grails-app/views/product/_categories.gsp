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

<%@page import="org.apache.commons.lang.StringEscapeUtils; org.apache.commons.lang.StringUtils; com.sapienter.jbilling.server.order.db.OrderLineTypeDTO"%>

<%--
  Categories list

  @author Brian Cowdery
  @since  16-Dec-2010
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="categories" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th><g:message code="product.category.th.name"/></th>
                    <g:isRoot><th><g:message code="product.label.available.company.name"/></th></g:isRoot>
                    <th class="small"><g:message code="product.category.th.type"/></th>
                </tr>
            </thead>
            <tbody>
            <g:each var="category" in="${categories}">
                <g:set var="lineType" value="${new OrderLineTypeDTO(category.orderLineTypeId, 0)}"/>

                    <tr id="category-${category.id}" class="${selectedCategoryId == category.id ? 'active' : ''}">
                        <td>
                            <g:remoteLink class="cell double" action="products" id="${category.id}" before="register(this);" onSuccess="render(data, next);">
                                <strong>${StringUtils.abbreviate(StringEscapeUtils.escapeHtml(category?.description), 45)}</strong>
                                <em><g:message code="table.id.format" args="[category.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <g:isRoot>
                        <td class="small">
                        	<%
							def totalChilds = category?.entities?.size()
							def multiple = false
							if(totalChilds > 1 ) {
								multiple = true
							}
							 %>
                            <g:remoteLink class="cell" action="products" id="${category.id}" before="register(this);" onSuccess="render(data, next);">
                                <g:if test="${category?.global}">
                                	<strong><g:message code="product.label.company.global"/></strong>
                                </g:if>
                                <g:elseif test="${multiple}">
                                	<strong><g:message code="product.label.company.multiple"/></strong>
                                </g:elseif>
                                <g:else>
                                	<strong>${StringEscapeUtils.escapeHtml(category?.entities?.toArray()[0]?.description)}</strong>
                                </g:else>
                            </g:remoteLink>
                        </td>
                        </g:isRoot>
                        <td class="small">
                            <g:remoteLink class="cell" action="products" id="${category.id}" before="register(this);" onSuccess="render(data, next);">
                                <span>${StringEscapeUtils.escapeHtml(lineType?.description)}</span>
                            </g:remoteLink>
                        </td>
                    </tr>

                </g:each>
                <g:if test="${selectedCategoryId != null}">
                    <g:remoteLink class="hidden" action="products" name="categoryToClick" id="${selectedCategoryId}" before="register(this);"
                                  onSuccess="render(data, next);"/>
                    <script type="text/javascript">$("[name='categoryToClick']").click();</script>
                </g:if>
            </tbody>
        </table>
    </div>
</div>


    <div class="pager-box">
        <div class="row left">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], action: 'categories', update: 'column1']"/>
            </div>
        </div>
        <jB:isPaginationAvailable total="${categories.totalCount}">
            <div class="row-center">
                <jB:remotePaginate controller="product" action="categories" total="${categories.totalCount}" update="column1"/>
            </div>
        </jB:isPaginationAvailable>
    </div>


<div class="btn-box">
    <sec:ifAllGranted roles="PRODUCT_CATEGORY_50">
        <g:link action="editCategory" class="submit add button-primary" params="${[add: true]}"><span><g:message code="button.create.category"/></span></g:link>
    </sec:ifAllGranted>

    <sec:ifAllGranted roles="PRODUCT_CATEGORY_51">
        <a href="#" onclick="return editCategory();" class="submit edit"><span><g:message code="button.edit"/></span></a>
    </sec:ifAllGranted>
</div>


<!-- edit category control form -->
<g:form name="category-edit-form" controller="product" action="editCategory">
    <g:hiddenField name="id" id="editformSelectedCategoryId" value="${selectedCategoryId}"/>
</g:form>
${categoryId}

<script type="text/javascript">
    function editCategory() {
        $('#editformSelectedCategoryId').val(getSelectedId('#categories'));
        $('#category-edit-form').submit();
        return false;
    }
</script>
