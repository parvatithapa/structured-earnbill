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
    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
        <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
        <g:javascript src="jquery.jqGrid.min.js"  />
        <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
    </g:preferenceEquals>
</head>
<body>

<g:if test="${accountTypes}" >
    <sec:ifAllGranted roles="MENU_90">
        <content tag="column1">
            <g:render template="accountTypeSelector" model="[accountTypes: accountTypes, parentId: parentId, companies: companies]"/>
        </content>
    </sec:ifAllGranted>
</g:if>

<g:else>
    %{--show all user's details--}%
    <sec:ifAllGranted roles="MENU_90">
        <content tag="column1">
            <g:render template="customersTemplate" model="[users: users, displayer: displayer]"/>
        </content>

        <content tag="column2">
            <g:if test="${selected}">
                <!-- show selected user details -->
                <g:render template="show" model="[selected: selected, contact: contact, displayer: displayer]"/>
            </g:if>
            <g:else>
                <!-- show empty block -->
                <g:render template="noSelected"/>
            </g:else>
        </content>
    </sec:ifAllGranted>

    %{--just show details of the current user--}%
    <sec:ifNotGranted roles="MENU_90">
        <content tag="column1">
            <g:if test="${selected}">
                <!-- show selected user details only -->
                <g:render template="show" model="[customerNotes: customerNotes, selected: selected, contact: contact]"/>
            </g:if>
            <g:else>
                <!-- show empty block -->
                <g:render template="noSelected"/>
            </g:else>
        </content>
    </sec:ifNotGranted>

</g:else>
</body>
</html>
