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

<%@ page import="jbilling.EdiReportController" %>
<html>
<head>
    <meta name="layout" content="main"/>
</head>

<body>
<div>
    <h3>Customers Not Invoiced</h3>
</div>

<div class="table-area">
    <g:form name="filter" controller="ediReport" action="customersNotInvoiced">
        <fieldset>
            <div id="report-filters" class="box-cards box-cards">
                <div class="box-cards-title">
                    <a class="btn-open" href="#"><span>Filters</span></a>
                </div>

                <div id="priceBox" class="box-card-hold">
                    <div class="form-columns">
                        <g:applyLayout name="form/date">
                            <content tag="label">From</content>
                            <content tag="label.for">startDate</content>
                            <g:textField class="field" name="startDate"
                                         value="${formatDate(date: startDate, formatName: 'datepicker.format')}"
                                         onblur="validateDate(this)"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/date">
                            <content tag="label">To</content>
                            <content tag="label.for">endDate</content>
                            <g:textField class="field" name="endDate"
                                         value="${formatDate(date: endDate, formatName: 'datepicker.format')}"
                                         onblur="validateDate(this)"/>
                        </g:applyLayout>
                    </div>

                    <div class="form-columns" style="padding-left: 350px">
                        <g:submitButton name="filter" class="submit">Filter</g:submitButton>
                    </div>
                </div>
            </div>
        </fieldset>
    </g:form>
</div>

<div class="table-area">
    <table>
        <thead>
        <tr>
            <td class="first"><g:message code="edi.customersNotInvoiced.userId"/></td>
            <td class="last"><g:message code="edi.customersNotInvoiced.userName"/></td>
        </tr>
        </thead>
        <tbody>
        <g:each in="${users}" var="user">
            <tr>
                <td class="col02">
                    <sec:access url="/customerInspector/inspect">
                        <g:link controller="customerInspector" action="inspect" id="${user.id}" title="${message(code: 'customer.inspect.link')}">
                            ${user.id}
                        </g:link>
                    </sec:access>
                    <sec:noAccess url="/customerInspector/inspect">
                        ${user.id}
                    </sec:noAccess>
                </td>
                <td>${user.userName}</td>
            </tr>
        </g:each>

        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <g:paginate controller="ediReport" action="customersNotInvoiced" total="${usersTotal}" params="[startDate: EdiReportController.simpleDateFormat.format(startDate),
                                                                                                        endDate:EdiReportController.simpleDateFormat.format(endDate)]"/>
    </div>
</div>

<div>
</div>
</body>
</html>
