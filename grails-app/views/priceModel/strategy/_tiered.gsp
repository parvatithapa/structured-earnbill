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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%--
  Tiered pricing form.

  @author Vikas Bodani
  @since  15-Mar-2011
--%>
<g:set var="defaultCurrency" value="${CompanyDTO.get(session['company_id']).getCurrency()}"/>
<g:set var="planIndex" value="${planIndex ?: ''}"/>
<g:hiddenField name="model.${modelIndex}.id" value="${model?.id}"/>

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

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.user.currency"/></content>
    <content tag="label.for">model.${modelIndex}.currencyId</content>
    <content tag="label.title"><g:message code="price.strategy.COMMON.currency.tooltip.message"/></content>
    <content tag="label.class">toolTipElement</content>
    <content tag="include.script">true</content>
    <g:select name="model.${modelIndex}.currencyId"
              class="toolTipElement"
              title="${message(code: 'price.strategy.COMMON.currency.tooltip.message')}"
              from="${currencies}"
              optionKey="id" optionValue="${{it.getDescription(session['language_id'])}}"
              value="${model?.currencyId ?: defaultCurrency?.id}" />
</g:applyLayout>
