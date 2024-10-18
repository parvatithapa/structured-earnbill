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
    Wait screen while assets are being imported
 --%>

<html>
<head>
    <meta name="layout" content="panels"/>
</head>

<body>

<content tag="column1">
    <div class="heading"><strong><g:message code="asset.heading.upload"/></strong></div>

    <div class="box">
        <div class="sub-box" id="asset-upload-box">
            <g:render template="processAssetsWait"/>
        </div>
    </div>
</content>

<content tag="column2">
</content>

</body>
</html>