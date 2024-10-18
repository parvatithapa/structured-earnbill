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
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO; com.sapienter.jbilling.common.Constants" %>
<html>
<head>
    <meta name="layout" content="panels" />
<%--    <g:javascript src="form.js" />--%>

    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
        <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
        <g:javascript src="jquery.jqGrid.min.js"  />
        <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
    </g:preferenceEquals>

    <r:script disposition="head">
        function validateDate(element) {
            var dateFormat= "<g:message code="date.format"/>";
            if(!isValidDate(element, dateFormat)) {
                $("#error-messages").css("display","block");
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
                //element.focus();
                return false;
            } else {
                return true;
            }
        }
    </r:script>
</head>
<body>

    <!-- show report types and reports -->
<content tag="column1">
        <g:render template="typesTemplate" model="[types: types]"/>
</content>

<content tag="column2">
    <g:if test="${selectedTypeId}">
        <g:render template="reportsTemplate" model="['selected': selected]"/>
	</g:if>
	<g:else>
        <!-- show empty block -->
        <g:render template="noSelected"/>
	</g:else>
</content>

</body>
</html>
