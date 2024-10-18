<%--
  Created by IntelliJ IDEA.
  User: taimoor
  Date: 6/12/18
  Time: 11:30 AM
--%>

<html>
<head>
    <meta name="layout" content="panels"/>
</head>

<body>

<content tag="column1">
    <div class="heading"><strong><g:message code="download.products.description"/></strong></div>

    <div class="box">
        <div class="sub-box" id="product-download-box">
            <g:render template="processDownloadPricesWait"/>
        </div>
    </div>
</content>

<content tag="column2">
</content>

</body>
</html>