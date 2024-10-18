<%@ page import="org.apache.commons.lang.StringEscapeUtils; org.apache.commons.lang.StringUtils" %>
<html>
<head>
    <meta name="layout" content="configuration"/>
</head>

<body>

<!-- selected configuration menu item -->
<content tag="menu.item">companies</content>

<content tag="column1">
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

    <div class="table-box">
        <div class="table-scroll">
            <table id="categories" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <th class="first"><g:message code="config.companies.th.name"/></th>
                    <th class="small last"><g:message code="config.companies.th.id"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="company" in="${comapnies}">
                    <tr id="company-${company.id}">
                        <td>
                            <strong>${StringUtils.abbreviate(StringEscapeUtils.escapeHtml(company?.description), 45)}</strong>
                        </td>
                        <td class="small">
                            <span>${company.id}</span>
                        </td>
                    </tr>

                </g:each>
                </tbody>
            </table>
        </div>
    </div>
</content>

</body>
</html>
