%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
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
  Parameters for the User Activity report.

  @author Aadil Nazir
  @since  29-Jan-2015
--%>

<div class="form-columns">
    <g:applyLayout name="form/checkbox">
        <content tag="label"><g:message code="include_customers"/></content>
        <content tag="label.for">include_customers</content>
        <g:checkBox class="cb checkbox" name="include_customers" checked="false"/>
    </g:applyLayout>

    <g:applyLayout name="form/text">
        <content tag="label"><g:message code="activity_days"/></content>
        <content tag="label.for">activity_days</content>
        <g:textField class="{validate:{required:true, digits: true}}" name="activity_days" style="width: 21.5%;"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="user_status"/></content>
        <content tag="label.for">active_status</content>
        <content tag="include.script">true</content>
        <g:select name="active_status" from="['Active', 'Inactive', 'Both']" valueMessagePrefix="user_status"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="user_orderby"/></content>
        <content tag="label.for">order_by</content>
        <content tag="include.script">true</content>
        <g:select name="order_by" from="['name', 'login']" valueMessagePrefix="user_orderby"/>
    </g:applyLayout>
</div>
