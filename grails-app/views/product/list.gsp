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

<%@ page import="com.sapienter.jbilling.common.Constants" %>

<html>
<head>
    <meta name="layout" content="panels" />
    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
        <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
        <g:javascript src="jquery.jqGrid.min.js"  />
        <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
    </g:preferenceEquals>
</head>
<body>
    <g:if test="${!selectedProduct}">
        <!-- show product categories and products -->
        <content tag="column1">
            <g:render template="categoriesTemplate" model="[categories: categories]"/>
        </content>

        <content tag="column2">
            <g:render template="productsTemplate" model="[products: products]"/>
        </content>
    </g:if>
    <g:else>
        <!-- show product list and selected product -->
        <content tag="column1">
            <g:render template="productsTemplate" model="[products: products]"/>
        </content>

        <content tag="column2">
            <g:render template="show" model="[selectedProduct: selectedProduct]"/>
        </content>
    </g:else>
</body>
</html>
