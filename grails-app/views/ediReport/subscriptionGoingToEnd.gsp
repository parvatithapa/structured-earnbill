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
        <g:form name="filter" controller="ediReport" action="subscriptionGoingEnd">
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
                                <g:textField class="field" name="subscriptionEndDate"
                                             value="${formatDate(date: subscriptionEndDate, formatName: 'datepicker.format')}"
                                             onblur="validateDate(this)"/>
                            </g:applyLayout>

                        </div>

                        <div id="report-filters-submit" class="form-columns">
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
                        <td class="first"><g:message code="report.user.id"/></td>
                        <td><g:message code="report.user.name"/></td>
                        <td><g:message code="report.renewed.date"/></td>

                    </tr>
                </thead>
                <tbody>
                <!-- process summary -->
                <g:each in="${customers}" var="customer">
                    <tr>
                        <td class="col02"> ${customer?.baseUser?.id}</td>
                        <td>${customer?.baseUser?.userName}</td>
                        <td><g:formatDate date="${customer.getMetaField(FileConstants.CUSTOMER_COMPLETION_DATE_METAFIELD)?.getValue()}" formatName="date.format"/> </td>

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