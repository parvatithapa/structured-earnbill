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

<%@page import="com.sapienter.jbilling.server.metafields.validation.ValidationRuleType"%>
<%@ page import="org.apache.commons.lang.StringUtils; com.sapienter.jbilling.server.pricing.cache.MatchType; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.ItemDTO; com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;" %>

<%--
  Editor form for validation model attributes

  Parameters to be passed to this template:

  1. validationRule - Validation Rule
  2. metaFieldIdx - (optional) Used when rendering multiple instances of the template in order to provide an unique
        name for input fields.

  @author Panche Isajeski, Shweta Gupta
--%>

<g:set var="attributeIndex" value="${0}"/>
<g:set var="attrs" value="${validationRule?.ruleAttributes ? new TreeMap<String, String>(validationRule.ruleAttributes) : new TreeMap<String, String>()}"/>

<!-- all validation rule attribute definitions -->
<g:each var="definition" in="${validationRule?.ruleType ? ValidationRuleType.valueOf(validationRule?.ruleType)?.validationRuleModel?.attributeDefinitions : null }">
    <g:set var="attributeIndex" value="${attributeIndex + 1}"/>

    <g:set var="attribute" value="${attrs?.remove(definition?.name)}"/>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="${definition.name}"/></content>
        <content tag="label.for">metaField${metaFieldIdx}.validationRule.ruleAttributes.${attributeIndex}.value</content>

        <g:hiddenField name="metaField${metaFieldIdx}.validationRule.ruleAttributes.${attributeIndex}.name" value="${definition?.name}"/>
        <g:textField class="field" name="metaField${metaFieldIdx}.validationRule.ruleAttributes.${attributeIndex}.value"
                     value="${attribute}"/>
    </g:applyLayout>
</g:each>

