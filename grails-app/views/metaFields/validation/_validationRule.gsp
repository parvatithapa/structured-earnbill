<%@ page import="com.sapienter.jbilling.server.metafields.validation.ValidationRuleType" %>
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
  Validation rule model

    @author Shweta Gupta
--%>

<div class="column">
    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="validation.rule.type"/></content>
        <content tag="label.for">metaField${metaFieldIdx}.validationRule.ruleType</content>
        <content tag="include.script">true</content>
        <g:select name="metaField${metaFieldIdx}.validationRule.ruleType"
                  id="metaField${metaFieldIdx}.validationRule.ruleType"
                  class="validation-type"
                  from="${types}"
                  valueMessagePrefix="validation.rule.type"
                  value="${validationRule?.ruleType ? ValidationRuleType.valueOf(validationRule?.ruleType) : null}"
                  noSelection="['': message(code: 'default.no.selection')]"/>

    </g:applyLayout>
    <g:render template="/metaFields/validation/validationAttributes" model="[validationRule: validationRule, metaFieldIdx: metaFieldIdx, validationType : validationType]"/>
</div>

<div class="column">
    <g:if test="${validationRule?.enabled}">
        <g:render template="/metaFields/validation/errorDescriptions" model="[validationRule: validationRule, parentId: parentId, metaFieldIdx : metaFieldIdx]"/>
    </g:if>
</div>
