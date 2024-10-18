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
    <meta name="layout" content="${isPartner ? 'panels' : 'configuration'}"/>
</head>
<body>
    <content tag="menu.item">users</content>

    <%-- show user code list --%>
    <content tag="column1">
        <g:render template="userCodeList" model="[userCodes: userCodes, user: user]"/>
    </content>

    <%-- show detail of selected asset --%>
    <content tag="column2">
        <g:if test="${selectedUserCode}" >
            <g:render template="userCodeShow" model="[userCode: selectedUserCode, user: user]"/>
        </g:if>
    </content>

</body>
</html>
