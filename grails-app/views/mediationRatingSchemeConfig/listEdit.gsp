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

<html>
<head>
    <meta name="layout" content="configuration" />
</head>
<body>
    <content tag="menu.item">Rating Schemes</content>

    <content tag="column1">
        <g:render template="schemes" />
    </content>

    <content tag="column2">
        <g:render template="edit" model="[ ratingScheme: ratingScheme, ratingSchemes: ratingSchemes, allCompanies: allCompanies, ratingSchemes: ratingSchemes]"/>
    </content>
</body>
</html>