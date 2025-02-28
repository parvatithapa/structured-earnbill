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

<%@ page import="com.sapienter.jbilling.server.order.db.OrderDAS" %>

<%--
  _orderDate

  @author Faizan Ahmad
  @since  05-04-2017
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.orderDate.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:select name="filters.${filter.name}.stringValue"
                              from="${['Current', 'Future']}"
                              valueMessagePrefix='filters.orderDate'
                              value="${filter.stringValue}"
                              noSelection="['': message(code: 'filters.orderDate.empty')]"/>
                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.orderDate.label"/></label>
            </div>
        </fieldset>
    </div>
</div>

