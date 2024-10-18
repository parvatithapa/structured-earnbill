<%--
  Created by IntelliJ IDEA.
  User: taimoor
  Date: 6/19/18
  Time: 12:16 PM
--%>

<%@ page import="com.sapienter.jbilling.common.Constants" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="configuration" />
    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
        <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
        <g:javascript src="jquery.jqGrid.min.js"  />
        <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
    </g:preferenceEquals>
</head>
<body>
<!-- selected configuration menu item -->
<content tag="menu.item">products</content>

<content tag="column1">
    <g:render template="priceDownloadTypes" model="[ selected: selected ]"/>
</content>
</body>
</html>