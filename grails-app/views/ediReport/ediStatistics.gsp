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
        <meta name="layout" content="main"/>

        <style type="text/css">
        .filter-button-row {
            background: #f6f6f6;
            border-top: 1px solid #bbbbbb;
            border-bottom: 1px solid #bbbbbb;
            padding: 6px 0;
            margin-top: 30px;
        }

        .box-cards {
            margin: 0 25px 15px !important;
        }

        .box-cards .box-card-hold {
            padding: 10px 10px 0px !important;
        }
        </style>
    </head>

    <body>
        <div>
            <h3>EDI Report - Statistics</h3>
        </div>

        <div class="table-area">
            <g:form name="filter" controller="ediReport" action="ediStatistics">
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
                                    <g:textField class="field" name="startDate" value="${formatDate(date: startDate, formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/date">
                                    <content tag="label">To</content>
                                    <content tag="label.for">endDate</content>
                                    <g:textField class="field" name="endDate" value="${formatDate(date: endDate, formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/select">
                                    <content tag="label">Transaction Type</content>
                                    <content tag="label.for">transactionType</content>
                                    <g:select class="field" name="ediTypeId" optionKey="id" optionValue="name" value="${ediTypeId}" from="${ediTypes}" noSelection="['':'']"/>
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
                        <td class="first"><g:message code="edi.ediStatistic.transactionType"/></td>
                        <td><g:message code="edi.ediStatistic.inbound"/></td>
                        <td><g:message code="edi.ediStatistic.outbound"/></td>
                        <td class="last"><g:message code="edi.ediStatistic.total"/></td>
                    </tr>
                </thead>
                <tbody>
                <g:set var="inboundTotal" value="${0}"/>
                <g:set var="outboundTotal" value="${0}"/>
                <g:set var="grandTotal" value="${0}"/>
                <!-- process summary -->
                <g:each in="${statistics}" var="statistic">
                    <g:set var="inboundTotal" value="${inboundTotal + statistic.inbound}"/>
                    <g:set var="outboundTotal" value="${outboundTotal + statistic.outbound}"/>
                    <g:set var="grandTotal" value="${grandTotal + statistic.total}"/>
                    <tr>
                        <td class="col02">${statistic.transactionType}</td>
                        <td>${statistic.inbound}</td>
                        <td>${statistic.outbound}</td>
                        <td>${statistic.total}</td>
                    </tr>
                </g:each>

                    <!-- grand totals -->
                    <tr class="bg">
                        <td class="col02"></td>
                        <td><strong>${inboundTotal}</strong></td>
                        <td><strong>${outboundTotal}</strong></td>
                        <td><strong>${grandTotal}</strong></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </body>
</html>
