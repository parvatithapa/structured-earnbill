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

<%@ page import="com.sapienter.jbilling.server.ediTransaction.EDIFileBL" %>
<html>
    <head>
        <meta name="layout" content="main"/>
    </head>

    <body>
        <div>
            <h3>EDI Report - Current EDI Exceptions</h3>
        </div>

    <div class="table-area">
        <g:form name="filter" controller="ediReport" action="currentEdiExceptions">
            <fieldset>
                <div id="report-filters" class="box-cards box-cards ${ediTypeId?' box-cards-open' : ''}">
                    <div class="box-cards-title">
                        <a class="btn-open" href="#"><span>Filters</span></a>
                    </div>

                    <div id="priceBox" class="box-card-hold">
                        <div class="form-columns">
                            <g:applyLayout name="form/select">
                                <content tag="label">Transaction Type</content>
                                <content tag="label.for">transactionType</content>
                                <g:select class="field" name="ediTypeId" optionKey="id" optionValue="name" value="${ediTypeId}" from="${ediTypes}" noSelection="['':'']"/>
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
                        <td class="first"><g:message code="edi.currentExceptions.transactionType"/></td>
                        <td><g:message code="edi.currentExceptions.direction"/></td>
                        <td><g:message code="edi.currentExceptions.transactionId"/></td>
                        <td><g:message code="edi.currentExceptions.date"/></td>
                        <td><g:message code="edi.currentExceptions.exceptionCode"/></td>
                        <td class="last"><g:message code="edi.currentExceptions.status"/></td>
                    </tr>
                </thead>
                <tbody>
                <!-- process summary -->
                <g:each in="${ediFiles}" var="ediFile">
                    <tr>
                        <td class="col02"> ${ediFile?.ediType?.name}</td>
                        <td>${ediFile?.type}</td>
                        <td>${EDIFileBL.getEDIFileTransactionId(ediFile)}</td>
                        <td>${ediFile?.createDatetime}</td>
                        <td>${ediFile?.exceptionCode?.exceptionCode}</td>
                        <td>${ediFile?.fileStatus?.name}</td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
        <div >
            <div class="pager-box">
                <g:paginate controller="ediReport" action="currentEdiExceptions" total="${ediFilesTotalCount}" params="[ediTypeId:ediTypeId]"/>
            </div>
        </div>
    </body>
</html>