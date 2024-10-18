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

<html>
<head>
    <meta name="layout" content="configuration" />
</head>
<body>

<!-- selected configuration menu item -->
<content tag="menu.item">route</content>

<content tag="column1">
    <g:render template="routes" model="[routes: routes]" />
</content>

<content tag="column2">
    <g:if test="${selected}" >
        <g:if test="${edit}" >
            <g:render template="edit" model="[route: selected]" />
        </g:if>
        <g:else>
            <g:render template="show" model="[selected: selected, matchingFields: matchingFields]" />
        </g:else>
    </g:if>
    <g:else>
        <!-- show empty block -->
        <div class="heading"><strong><em><g:message code="route.detail.selected.title"/></em></strong></div>
        <div class="box"><div class="sub-box"><em><g:message code="route.detail.not.selected.message"/></em></div></div>
        <div class="btn-box"></div>
    </g:else>
</content>

</body>
</html>
