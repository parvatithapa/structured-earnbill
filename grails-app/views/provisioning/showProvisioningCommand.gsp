<html>
<head>
    <meta name="layout" content="panels" />
</head>
<body>
<content tag="column1">
    <g:render template="listCommands"/>
</content>
<content tag="column2">
    <g:if test="${selected}">
        <g:render template="showCommand"/>
    </g:if>
</content>
</body>
</html>