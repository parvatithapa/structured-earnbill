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
<sec:ifAllGranted roles="CUSTOMER_ENROLLMENT_910">
<g:if test="${accountTypes}" >
        <content tag="column1">
            <g:render template="../customer/accountTypeSelector" model="[accountTypes: accountTypes, companies: companies]"/>
        </content>
</g:if>
<g:else>
    <content tag="column1">
        <g:render template="customerEnrollmentTemplate" model="[orders: customerEnrollments, selected:selected]"/>
    </content>

    <content tag="column2">
        <g:if test="${selected}">
            <g:render template="show" model="[customerEnrollment: selected]"/>
        </g:if>
        <g:else>
            <!-- show empty block -->
            <g:render template="noSelected"/>
        </g:else>
    </content>
</g:else>
</sec:ifAllGranted>

</body>
</html>
