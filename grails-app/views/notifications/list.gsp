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
    <r:script disposition="head">
        var selected;

        // todo: should be attached to the ajax "success" event.
        // row should only be highlighted when it really is selected.
        $(document).ready(function() {
            $('.table-box li').bind('click', function() {
                if (selected) selected.attr("class", "");
                selected = $(this);
                selected.attr("class", "active");
            })
        });
    </r:script>
</head>

<body>

    <!-- selected configuration menu item -->
    <content tag="menu.item">notification</content>

    <g:if test="${!selectedNotification}">
        <content tag="column1">
            <g:render template="categoriesTemplate" model="['lst': lst]"/>
        </content>

        <content tag="column2">
            <g:if test="${selected}">
                <g:render template="notificationsTemplate" model="['lstByCategory': lstByCategory, categoryId: categoryId]"/>
            </g:if>
        </content>
    </g:if>
    <g:else>
        <!-- show product list and selected product -->
        <content tag="column1">
            <g:render template="notificationsTemplate" model="['lstByCategory': lstByCategory, categoryId: categoryId]"/>
        </content>

        <content tag="column2">
            <g:render template="show" model="['typeDto': typeDto, dto: dto, 'messageTypeId': messageTypeId, 'languageDto': languageDto, 'entityId': entityId]"/>
        </content>
    </g:else>

</body>
</html>
