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
    Wait screen while product prices are being imported
 --%>

<html>
<head>
    <meta name="layout" content="panels"/>
</head>

<body>

<content tag="column1">
    <div class="heading"><strong><g:message code="upload.products.description"/></strong></div>

    <div class="box">
        <div class="sub-box" id="product-upload-box">
            <g:render template="processProductPricesWait"/>
        </div>
    </div>
</content>

<content tag="column2">
</content>

</body>
</html>
