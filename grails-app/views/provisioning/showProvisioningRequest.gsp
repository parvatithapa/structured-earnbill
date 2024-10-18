<html>
<head>
    <meta name="layout" content="panels" />
</head>
<body>
${flash.t = 'requests' }
<content tag="column1">
    <g:render template="listRequests"/>
</content>
<content tag="column2">
    <g:render template="showRequest"/>
</content>
</body>
</html>