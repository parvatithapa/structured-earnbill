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
    <meta name="layout" content="configuration" />

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
    <content tag="filters">
    </content>

    <content tag="column1">

    <g:render template="pluginsTemplate"/>
    </content>

    <content tag="column2">
        <g:render template="show"/>
    </content>
</body>
</html>
