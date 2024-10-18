<div class="heading"><strong><em><g:message code="product.category.no.products.title"/></em></strong></div>
<div class="box">
    <div class="sub-box">
        <g:if test="${selectedCategoryId}">
            <em><g:message code="product.category.no.products.warning"/></em>
        </g:if>
        <g:else>
            <em><g:message code="product.category.not.selected.message"/></em>
        </g:else>
    </div>
</div>