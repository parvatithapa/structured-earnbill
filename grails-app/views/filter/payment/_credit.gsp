<%@ page import="com.sapienter.jbilling.server.payment.db.PaymentMethodDAS; com.sapienter.jbilling.client.util.Constants" %>
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

<div id="${filter.name}">
    <span class="title"><g:message code="filters.credit.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-filter">
                    <g:set var="filter_name" value="filters.${filter.name}.stringValue"/>
                    <g:set var="filter_value" value="${filter.stringValue}"/>
                    <g:set var="filter_from" value="${[[value:new PaymentMethodDAS().findAllValidMethods().collect {it.id}.join(','),label:message(code:'prompt.no')],
                                                       [value:Constants.PAYMENT_METHOD_CREDIT,label:message(code:'prompt.yes')]]}"/>
                    <g:set var="filter_optionKey" value="value"/>
                    <g:set var="filter_optionValue" value="label"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_optionKey: filter_optionKey,
                            select_optionValue: filter_optionValue
                    ]">
                    </g:applyLayout>
                </div>
                <label for="filters.${filter.name}.integerValue"><g:message code="filters.credit.label"/></label>
            </div>
        </fieldset>
    </div>

    <g:set var="filter_selector" value="select[name='filters.${filter.name}.stringValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>
