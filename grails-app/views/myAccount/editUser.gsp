<%@ page import="com.sapienter.jbilling.common.Constants" %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<html>
<head>
    <meta name="layout" content="account" />
</head>
<body>
<!-- selected configuration menu item -->
<content tag="menu.item">mydetails</content>
<content tag="column.size">double</content>
<content tag="column1">
    <g:set var="altFailureView" value="/myAccount/editUser" />
    <g:set var="altRedirect" value="/user/show/${session['user_id']}?altView=/myAccount/showUser" />
    <g:render template="/user/editForm"/>
</content>
</body>
</html>
