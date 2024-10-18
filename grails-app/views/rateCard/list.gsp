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
    <r:script>
    	function hideCompanies(){
            $("#company-select option").removeAttr("selected");
			if ($("#global-checkbox").is(":checked")) {
                $("#company-select").attr('disabled', true);
			} else {
                $("#company-select").removeAttr('disabled');
			}
		}
    </r:script>
</head>
<body>
    <!-- selected configuration menu item -->
    <content tag="menu.item">rateCard</content>

    <content tag="column1">
        <g:render template="rateCardsTemplate" />
    </content>

    <content tag="column2">

        <g:if test="${flash.error || flash.errorMessages}">
            <!-- when errors are present don't hide the edit page. -->
            <g:render template="edit" model="[rateCard: selected]"/>
        </g:if>
        <g:elseif test="${selected}">
            <!-- show selected rate card -->
            <g:render template="show" model="[selected: selected]"/>
        </g:elseif>
        <g:elseif test="${rateCard}">
            <!-- edit selected rate card -->
            <g:render template="edit" model="[selected: rateCard]"/>
        </g:elseif>

    </content>
</body>
</html>
