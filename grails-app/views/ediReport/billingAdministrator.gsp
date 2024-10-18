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
        html[dir="rtl"] .column{
            float: right;
        }
    </style>
</head>

<body>
<div>
    <h3><g:message code="edi.report.billing.administrator.header" default="Billing Administrator Report"/> </h3>
</div>

<div class="table-area">
    <g:form name="filter" controller="ediReport" action="billingAdministrator">
        <fieldset>
            <div id="report-filters" class="box-cards box-cards ${params.companyId ? ' box-cards-open' : ''}">
                <div class="box-cards-title">
                    <a class="btn-open" href="#"><span>Filters</span></a>
                </div>

                <div  class="box-card-hold">
                    <div style="width: 900px; margin-left: auto; margin-right: auto">
                        <div class="column">
                            <div class="form-columns">
                                <g:isRoot>
                                    <g:applyLayout name="form/select">
                                        <content tag="label"><g:message code="product.assign.entities"/></content>
                                        <g:select id="company-select" name="companyId" from="${allCompanies}"
                                                  optionKey="id" optionValue="${{it?.description}}"
                                                  value="${params.int('companyId')}" />
                                    </g:applyLayout>
                                </g:isRoot>
                                <g:isNotRoot>
                                    <g:hiddenField id="company-select" name="companyId" value="${session['company_id']}"/>
                                </g:isNotRoot>
                            </div>
                            <div class="form-columns">
                                <g:applyLayout name="form/date">
                                    <content tag="label"><g:message code="subscription.end.date"/></content>
                                    <content tag="label.for">end_date</content>
                                    <g:textField class="field" name="end_date" value="${params.end_date?:new Date().format("MM/dd/yyyy")}"/>
                                </g:applyLayout>
                            </div>

                            <div class="form-columns">
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="edi.file.type"/></content>
                                %{--TODO need to fix the edi type--}%
                                    <g:select id="type-select" name="suffix" from="${["814", "867", "810", "820"]}" noSelection="['': 'Please select EDI Type']"
                                              value="${params.suffix}" />
                                </g:applyLayout>
                            </div>
                        </div>

                        <div class="column">
                            <div class="form-columns">
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="edi.report.account.type"/></content>
                                    <content tag="label.for">accountType</content>
                                    <g:select name="accountType" from="${[FileConstants.RESIDENTIAL_ACCOUNT_TYPE, FileConstants.COMMERCIAL_ACCOUNT_TYPE ]}" noSelection="['': 'Any']"
                                               value="${params.accountType}" />
                                </g:applyLayout>
                            </div>

                            <div class="form-columns">
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="report.billing.register.label.plan"/></content>
                                    <content tag="label.for">planSelect</content>
                                    <g:select id="planSelect" name="plan" from="[]" />
                                </g:applyLayout>
                            </div>

                            <div class="form-columns">
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="edi.report.status"/></content>
                                    <content tag="label.for">status</content>
                                    <g:select name="status" from="${["Successfully Processed":"processed", "On Hold":"onHold", "Cannot be Processed":"unProcessable" ].entrySet()}" noSelection="['': 'Please select']"
                                              optionKey="value" optionValue="key" value="${params.status}" />
                                </g:applyLayout>
                            </div>
                        </div>
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
            <td class="first"><g:message code="edi.billing.administrator.file.username"/> </td>
            <td><g:message code="edi.billing.administrator.file.fileNmae"/> </td>
            <td><g:message code="edi.billing.administrator.file.status"/> </td>
            <td><g:message code="edi.billing.administrator.file.date"/></td>
            <td><g:message code="edi.billing.administrator.file.error.message"/> </td>
            <td><g:message code="edi.billing.administrator.file.transaction.type"/></td>
            <td class="last"><g:message code="edi.billing.administrator.download"/></td>
        </tr>
        </thead>
        <tbody>

        <g:each in="${users}" var="user">
            <g:set var="userFiles" value="${ediFiles.findAll{it?.user?.id==user.id}}"/>
            <g:if test="${userFiles}" >
                <g:each in="${userFiles}" var="ediFile">
                    <tr>
                        <td class="col02"> <g:link controller="customerInspector" action="inspect" id="${ediFile?.user?.id}" target="_blank" >
                            ${ediFile?.user?.userName}
                        </g:link>
                        </td>
                        <td><g:link controller="ediFile" action="list" params="[ediFileId: ediFile?.id]" target="_blank">${ediFile?.name}</g:link> </td>
                        <td> ${ediFile?.fileStatus?.error ? 'Processed with error' : 'Processed successfully'}</td>
                        <td><g:formatDate date="${ediFile?.createDatetime}" formatName="date.pretty.format"/> </td>
                        <td>${ediFile?.comment}</td>
                        <td>${ediFile.type}</td>
                        <td><g:link controller="ediFile" action="download" params="[id:ediFile?.id]">Download</g:link> </td>
                    </tr>
                </g:each>
            </g:if>
            <g:else>
                <tr>
                    <td class="col02"> <g:link controller="customerInspector" action="inspect" id="${user.id}" target="_blank" >
                        ${user.getUserName()}
                    </g:link>
                    </td>
                    <td><g:message code="edi.report.blank"/></td>
                    <td><g:message code="edi.report.blank"/></td>
                    <td><g:message code="edi.report.blank"/></td>
                    <td><g:message code="edi.report.blank"/></td>
                    <td><g:message code="edi.report.blank"/></td>
                    <td><g:message code="edi.report.blank"/></td>
                </tr>
            </g:else>
        </g:each>

        </tbody>
    </table>
</div>
<div >
    <g:if test="${users}">
        <div class="pager-box">
            <g:paginate controller="ediReport" action="billingAdministrator" total="${users.totalCount}" params="${params}" />
        </div>
    </g:if>

</div>
<script>
    $(function() {

        var companyId=$("#company-select").val()
        searchData("planSelect", "findPlanByCompany", "${params.plan}", companyId);
        searchData("type-select", "findEdiType", "${params.suffix}", '');
        searchData("accountType", "findAccountType", "${params.accountType}", '');

        $("#company-select").on("change", function () {
            searchData("planSelect", "findPlanByCompany", "${params.plan}", companyId);
        });
    });


    function searchData(select, action, value, companyId){
        var element = $("#"+select);
        $.ajax({
            type: 'POST',
            url: action,
            data:{'companyId':companyId},
            success: function (data) {
                var elementParent = element.parent("div");
                element.remove();
                elementParent.append(data)
                if(value!=''){
                    $("#"+select).val(value);

                }

            }
        });
    }

</script>
</body>
</html>
