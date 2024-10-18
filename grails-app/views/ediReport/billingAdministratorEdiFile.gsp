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
        <style>
        .column{
            width: 50%;
            float: left;
        }
        </style>
    </head>

    <body>
        <div>
            <h3><g:message code="edi.report.billing.administrator.edi.file.header" default="Billing Administrator : EDI File Report"/>
            <g:if test="${ediFiles}">
                ( Total Result: ${ediFiles.totalCount} )
            </g:if>
            </h3>
        </div>

    <div class="table-area">
        <g:form name="filter" controller="ediReport" action="billingAdministratorEdiFile">
            <fieldset>
                <div id="report-filters" class="box-cards box-cards ${(params.list('entities') || params.ediTypeSuffixes)?' box-cards-open' : ''}">
                    <div class="box-cards-title">
                        <a class="btn-open" href="#"><span>Filters</span></a>
                    </div>

                    <div id="priceBox" class="box-card-hold">
                        <div style="width: 900px; margin-left: auto; margin-right: auto">
                            <div class="column">
                                <div class="form-columns">
                                    <g:isRoot>
                                        <g:applyLayout name="form/select_multiple">
                                            <content tag="label"><g:message code="product.assign.entities"/></content>
                                            <g:select id="company-select" multiple="multiple" name="entities"
                                                      from="${allCompanies}"
                                                      optionKey="id" optionValue="${{ it?.description }}"
                                                      value="${params.list('entities').collect {
                                                          Integer.valueOf(it + '')
                                                      }}"/>
                                        </g:applyLayout>
                                    </g:isRoot>
                                    <g:isNotRoot>
                                        <g:hiddenField name="entities" value="${session['company_id']}"/>
                                    </g:isNotRoot>
                                </div>

                                <div class="form-columns" style="padding-left: 38%">
                                    <div class="row">
                                        <g:radio class="rb" id="report.status.processed" name="searchStatus" value="processed" checked="${FileConstants.REPORT_SEARCH_STATUS_PROCESSED.equals(searchStatus)}"/>
                                        <label class="rb" for="report.status.processed"><g:message code="report.search.status.processed"/></label>
                                    </div>

                                    <div class="row">
                                        <g:radio class="rb" id="report.status.onHold" name="searchStatus" value="onHold" checked="${FileConstants.REPORT_SEARCH_STATUS_ON_HOLD.equals(searchStatus)}"/>
                                        <label class="rb" for="report.status.onHold"><g:message code="report.search.status.onHold"/></label>
                                    </div>

                                    <div class="row">
                                        <g:radio class="rb" id="report.status.unProcessable" name="searchStatus" value="unProcessable" checked="${FileConstants.REPORT_SEARCH_STATUS_UN_PROCESSABLE.equals(searchStatus)}"/>
                                        <label class="rb" for="report.status.unProcessable"><g:message code="report.search.status.unProcessable"/></label>
                                    </div>

                                    <div class="row">
                                        <g:radio class="rb" id="report.status.criticalError" name="searchStatus" value="criticalError" checked="${FileConstants.REPORT_SEARCH_STATUS_CRITICAL_ERROR.equals(searchStatus)}"/>
                                        <label class="rb" for="report.status.criticalError"><g:message code="report.search.status.criticalError"/></label>
                                    </div>
                                </div>

                                <div class="form-columns">
                                    <g:applyLayout name="form/select">
                                        <content tag="label"><g:message code="edi.file.type"/></content>
                                        <g:select id="edi-type-suffixes-select" name="ediTypeSuffixes"
                                                  from="${ediTypeSuffixes}" noSelection="['': 'Please select EDI Type']"
                                                  value="${params.ediTypeSuffixes}"/>
                                    </g:applyLayout>
                                </div>
                            </div>

                            <div class="column">
                                <div class="form-columns">
                                    <g:applyLayout name="form/date">
                                        <content tag="label">From</content>
                                        <content tag="label.for">startDate</content>
                                        <g:textField class="field" name="startDate"
                                                     value="${params.startDate}"
                                                     onblur="validateDate(this)"/>
                                    </g:applyLayout>
                                    <g:applyLayout name="form/date">
                                        <content tag="label">To</content>
                                        <content tag="label.for">endDate</content>
                                        <g:textField class="field" name="endDate"
                                                     value="${params.endDate}"
                                                     onblur="validateDate(this)"/>
                                    </g:applyLayout>
                                    <div class="form-columns" id="statusDiv" style="display: none">
                                        <g:applyLayout name="form/select_multiple">
                                            <content tag="label"><g:message code="edi.report.status"/></content>
                                            <content tag="label.for">status</content>
                                            <g:select id="status-select" multiple="multiple" name="statues"
                                                      from="${statues}"
                                                      value="${params.statues}"/>
                                        </g:applyLayout>
                                    </div>

                                    <div class="form-columns" id="exceptionCodeDiv" style="display: none">
                                        <g:applyLayout name="form/select_multiple">
                                            <content tag="label"><g:message code="edi.report.exceptionCode"/></content>
                                            <content tag="label.for">Exception Code</content>
                                            <g:select id="exception-codes-select" multiple="multiple"
                                                      name="exceptionCodes" from="${exceptionCodes}"
                                                      value="${params.exceptionCodes}"/>
                                        </g:applyLayout>
                                    </div>
                                </div>
                            </div>
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
                        <td class="first"><g:message code="edi.billing.administrator.filename"/> </td>
                        <td><g:message code="edi.billing.administrator.edi.type"/> </td>
                        <td><g:message code="edi.billing.administrator.status"/> </td>
                        <td><g:message code="edi.billing.administrator.exceptionCode"/> </td>
                        <td><g:message code="edi.billing.administrator.error.message"/> </td>
                        <td><g:message code="edi.billing.administrator.date"/>  </td>
                        <td><g:message code="edi.billing.administrator.transaction.type"/>  </td>
                        <g:isRoot><td><g:message code="edi.billing.administrator.company.name"/></td></g:isRoot>
                        <td class="last"><g:message code="edi.billing.administrator.download"/></td>
                    </tr>
                </thead>
                <tbody>

                <g:each in="${ediFiles}" var="ediFile">
                    <tr>
                        <td class="col02"> <g:link controller="ediFile" action="list" params="[ediFileId: ediFile?.id]" target="_blank">${ediFile?.name}</g:link></td>
                        <td>${ediFile?.ediType?.name}</td>
                        <td>${ediFile?.fileStatus?.name}</td>
                        <td>${ediFile?.exceptionCode?.exceptionCode}</td>
                        <td>${ediFile?.comment}</td>
                        <td><g:formatDate date="${ediFile?.createDatetime}" formatName="date.pretty.format"/> </td>
                        <td>${ediFile.type}</td>
                        <g:isRoot><td>${ediFile?.entity?.description}</td></g:isRoot>
                        <td><g:link controller="ediFile" action="download" params="[id:ediFile?.id]">Download</g:link> </td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
        <div >
            <div class="pager-box">
                <g:paginate controller="ediReport" action="billingAdministratorEdiFile" total="${ediFiles.totalCount}" params="${params}" />
            </div>
        </div>
    </body>

<r:script>
$(function() {
$('#edi-type-suffixes-select > option').length;
    var statusLength=$('#status-select > option').length;
    var exceptionCodeLength=$('#exception-codes-select > option').length;
    if(statusLength){
        $("#statusDiv").show();
    }else{
        $("#statusDiv").hide();
    }
    if(exceptionCodeLength){
        $("#exceptionCodeDiv").show();
    }else{
        $("#exceptionCodeDiv").hide();
    }
});

$("#edi-type-suffixes-select").change(function() {
    var suffixes=$("#edi-type-suffixes-select").val()
    var status = $("input[name='searchStatus']:checked").val();
    findStatues(suffixes,status);
});

$(document).ready(function() {
    $('input[type=radio][name=searchStatus]').change(function() {
        var length = $('#edi-type-suffixes-select > option').length;
        if(length >  0){
        var ediTypeSuffix=$("#edi-type-suffixes-select").val()
            searchStatus(this.value,ediTypeSuffix)
        }
    });
});

$(document.body).on('change','#status-select',function() {
    var statues=$("#status-select").val()
    if(statues){
        findExceptionCode(statues);
    }else{
        $("#exceptionCodeDiv").hide();
    }
});

function findStatues(suffixes,status){
    var ediStatues = $("#status-select");
    $("#exceptionCodeDiv").hide();
    $.ajax({
        type: 'POST',
        url: '${createLink(controller: 'ediReport', action: 'findEdiStatuses')}',
        data: {suffixes:suffixes,status:status},
        success: function (data) {
            ediStatues.replaceWith(data)
            var length = $('#status-select > option').length;
            if(length > 0){
                $("#statusDiv").show();
            }else{
                $("#statusDiv").hide();
            }
        }
    });
}

function findExceptionCode(statues){
    var ediException = $("#exception-codes-select");
    $.ajax({
        type: 'POST',
        url: '${createLink(controller: 'ediReport', action: 'findEdiExceptionCode')}',
        data: {statues: JSON.stringify(statues)},
        success: function (data) {
            ediException.replaceWith(data)
            var length = $('#exception-codes-select > option').length;
            if(length>0){
                $("#exceptionCodeDiv").show();
            }else{
                $("#exceptionCodeDiv").hide();
            }
        }
    });
}

function searchStatus(searchStatus,ediTypeSuffix){
    $("#exceptionCodeDiv").hide();
    var ediStatues = $("#status-select");
    $.ajax({
        type: 'POST',
        url: '${createLink(controller: 'ediReport', action: 'searchStatus')}',
        data: {ediTypeSuffix:ediTypeSuffix,status:searchStatus},
        success: function (data) {
            ediStatues.replaceWith(data)
            var length = $('#status-select > option').length;
            if(length > 0){
                $("#statusDiv").show();
            }else{
                $("#statusDiv").hide();
            }
        }
    });
}
</r:script>
</html>
