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

<%@ page import="com.sapienter.jbilling.server.fileProcessing.FileConstants" %>
<html>
    <head>
        <meta name="layout" content="main"/>
    </head>

    <body>
        <div>
            <h3><g:message code="report.auto.renewed.customer" message="Auto Renewed Customers"/> </h3>
        </div>


    <div class="table-area">
            <table>
                <thead>
                    <tr>
                        <td class="first"><g:message code="report.user.id"/></td>
                        <td><g:message code="report.user.name"/></td>
                        <td><g:message code="report.renewed.date"/></td>

                    </tr>
                </thead>
                <tbody>
                <!-- process summary -->
                <g:each in="${customers}" var="customer">
                    <tr>
                        <td class="col02">

                            <sec:access url="/customerInspector/inspect">
                                <g:link controller="customerInspector" action="inspect" id="${customer?.baseUser?.id}" title="${message(code: 'customer.inspect.link')}">
                                    ${customer?.baseUser?.id}
                                </g:link>
                            </sec:access>
                            <sec:noAccess url="/customerInspector/inspect">
                                ${customer?.baseUser?.id}
                            </sec:noAccess>
                        </td>
                        <td>${customer?.baseUser?.userName}</td>
                        <td><g:formatDate date="${customer.getMetaField(FileConstants.RENEWED_DATE)?.getValue()}" formatName="date.format"/> </td>

                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
        <div >
            <div class="pager-box">
                <g:paginate controller="ediReport" action="autoRenewedCustomer" total="${customers.totalCount}" />
            </div>
        </div>
    </body>
</html>