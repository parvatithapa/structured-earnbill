<html>
<head>
     <meta name="layout" content="configuration" />
</head>

<body>

<!-- selected configuration menu item -->
<content tag="menu.item">Authentication</content>

<content tag="column1">
  <g:render template="companyInformationTypes" model="[ company: company, cits: cits ]"/>
</content>

<content tag="column2">
  <g:if test="${selected}">
    <g:render template="showCIT" model="[ company: company, selected: selected ]"/>
  </g:if>
</content>

</body>
</html>
