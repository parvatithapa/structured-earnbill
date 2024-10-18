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
  Time-of-day percentage pricing form.

  @author Brian Cowdery
  @since  08-Feb-2011
--%>

<g:hiddenField name="model.${modelIndex}.id" value="${model?.id}"/>
<g:set var="planIndex" value="${planIndex ?: ''}"/>

<div class="row">
    <label class="toolTipElement" title="<g:message code="price.strategy.COMMON.pricing.tooltip.message"/>" for="model.${modelIndex}.type"><g:message code="plan.model.type"/></label>
    <g:applyLayout name="form/select_holder">
        <content tag="include.script">true</content>
        <content tag="label.for">model.${modelIndex}.type</content>
        <g:select name="model.${modelIndex}.type" class="model-type toolTipElement"
              title="${message(code: 'price.strategy.COMMON.pricing.tooltip.message')}"
              from="${types}"
              keys="${types*.name()}"
              valueMessagePrefix="price.strategy"
              value="${model?.type ?: type.name()}"/>

        <g:hiddenField name="model.${modelIndex}.oldType" value="${model?.type ?: type.name()}"/>

    </g:applyLayout>
    <a class="price-model-help toolTipElement" onclick="openHelpDialog('${planIndex + type?.name() + modelIndex}');"title="${message(code: 'price.strategy.COMMON.pricing.help.tooltip.message')}">
    </a>
</div>

