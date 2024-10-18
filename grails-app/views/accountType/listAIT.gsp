
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

<%--

  @author Panche Isajeski
  @since  05/24/2013
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<html>
    <head>
        <meta name="layout" content="configuration" />
    </head>
<body>

    <content tag="menu.item">accountType</content>

    <content tag="column1">
        <g:render template="accountInformationTypes" model="[ accountType: accountType, aits: aits ]"/>
    </content>

    <content tag="column2">
        <g:if test="${selected}">
            <g:render template="showAIT" model="[ accountType: accountType, selected: selected ]"/>
        </g:if>
    </content>
</body>
</html>