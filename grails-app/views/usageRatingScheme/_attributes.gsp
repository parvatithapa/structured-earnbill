
<%@ page import="com.sapienter.jbilling.server.usageRatingScheme.util.IAttributeDefinition" %>

<g:if test="${ratingScheme}">

    <g:set var="fixedAttributeMap" value="${ratingScheme?.fixedAttributes}"/>

    <g:set var="dynamicAttributeSet" value="${ratingScheme?.dynamicAttributes}"/>

    <g:set var="dynamicAttributeName" value="${ratingScheme?.dynamicAttributeName}"/>

</g:if>

<g:set var="lineIndex" value="${0}"/>
<g:hiddenField name="usesDynamicAttributes" value="${ratingSchemeType?.usesDynamicAttributes()}"/>

<%-- Populates fixed attributes of rating Schemes --%>

<g:each var="attribute" in="${ratingSchemeType.fixedAttributes}">
    <g:if test="${IAttributeDefinition.InputType.SELECT == attribute?.inputType}">
        <g:applyLayout name="form/select">
            <g:set var="selectData" value="${attribute?.iterable}"/>
            <content tag="label">${attribute?.name}
                <g:if test="${attribute?.required}">
                    <span id="mandatory-meta-field">*</span>
                </g:if></content>
            </content>
            <content tag="label.for">fixedAttributes.${attribute?.name}</content>
            <content tag="include.script">true</content>
            <g:select name="fixedAttributes.${attribute?.name}"
                      from="${selectData.iterable}"
                      optionKey="${selectData.key}" optionValue="${selectData.value}"
                      value="${fixedAttributeMap?.get(attribute?.name)}"
                      noSelection="['':'-Select']" />
        </g:applyLayout>
    </g:if>
    <g:else>
        <g:applyLayout name="form/input">
            <content tag="label">${attribute?.name}
                <g:if test="${attribute?.required}">
                    <span id="mandatory-meta-field">*</span>
                </g:if></content>
            <content tag="label.for">fixedAttributes.${attribute?.name}</content>
            <g:textField class="field" name="fixedAttributes.${attribute?.name}"
                         value="${fixedAttributeMap?.get(attribute?.name)}"/>
        </g:applyLayout>
    </g:else>
</g:each>

<g:if test="${ratingSchemeType?.usesDynamicAttributes()}">
    <g:hiddenField name="dynamicAttributeName" value="${ratingSchemeType.dynamicAttributeName}"/>
    <table>
        <thead>
            <tr>
                <%-- Populates headers of dynamic attributes of rating Schemes --%>
                <g:each var="attribute" in="${ratingSchemeType.dynamicAttributes}">
                 <td style="width:130px">
                    <g:applyLayout name="form/text">
                     <content tag="label">${attribute?.name}<g:if test="${attribute?.required}">
                         <span id="mandatory-meta-field">*</span>
                     </g:if></content>
                     <content tag="label.for">dynamicAttributes.${attribute?.name}</content>
                    </g:applyLayout>
                 </td>
                </g:each>
            </tr>
        </thead>

    <%-- Populates dynamic attributes of selected rating Schemes --%>
        <g:if test="${dynamicAttributeSet}">
            <g:each var="lineWS" in="${dynamicAttributeSet}">
                <g:hiddenField name="dynamicAttributes.${lineIndex}.id" value="${lineWS?.id}"/>
                <g:hiddenField name="dynamicAttributes.${lineIndex}.sequence" value="${lineWS?.sequence}"/>
                <tr>
                    <g:each var="attribute" in="${ratingSchemeType.dynamicAttributes}">
                        <td style="width:130px">
                            <g:applyLayout name="form/input">
                                <g:textField style="width:80px" class="field"
                                             name="dynamicAttributes.${lineIndex}.attributes.${attribute?.name}"
                                             value="${lineWS?.attributes.get(attribute?.name)}"/>
                            </g:applyLayout>
                        </td>
                    </g:each>
                <g:set var="lineIndex" value="${lineIndex+1}"/>
                </tr>
            </g:each>
        </g:if>

        <tr>
        <%-- Populates dynamic attributes textfields of new rating Schemes --%>
            <g:hiddenField name="dynamicAttributes.${lineIndex}.id" value="" />
            <g:hiddenField name="dynamicAttributes.${lineIndex}.sequence" value="${lineIndex}" />
                    <g:each var="attribute" in="${ratingSchemeType.dynamicAttributes}">
                        <td style="width:130px">
                            <g:applyLayout name="form/input">
                             <g:textField class="field" name="dynamicAttributes.${lineIndex}.attributes.${attribute?.name}" value=""/>
                            </g:applyLayout>
                        </td>
                    </g:each>
                    <td>
                        <a onclick="addDynamicAttributeRow()"
                           class="plus-icon toolTipElement" title="${message(code: 'price.strategy.COMMON.attributes.add.tooltip.message')}">
                            &#xe026;
                        </a>
                    </td>
            <g:set var="lineIndex" value="${lineIndex+1}"/>
        </tr>
    </table>
</g:if>

<script type="text/javascript">
    function addDynamicAttributeRow() {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'addDynamicAttributeRow')}',
            data:  $('#save-unit-form').serialize(),
            success: function(data) { $('#schemeModel').replaceWith(data); }
        });
    }
</script>
