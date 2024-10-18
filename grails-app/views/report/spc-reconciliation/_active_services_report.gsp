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
  Active Services Report

  @author Mahesh Kalshetty
  @since  18-Feb-2019
--%>

<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<div class="form-columns">
    <g:applyLayout name="form/input">
        <content tag="label"><g:message  code="active.services.report.offset" /></content>
        <content tag="label.for">offset</content>
        <g:textField class="field" name="offset"/>
    </g:applyLayout>
    <g:applyLayout name="form/input">
        <content tag="label"><g:message  code="active.services.report.limit" /></content>
        <content tag="label.for">limit</content>
        <g:textField class="field" name="limit"/>
    </g:applyLayout>
</div>
