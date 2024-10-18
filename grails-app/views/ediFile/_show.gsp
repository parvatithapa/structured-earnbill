<%@ page import="com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.server.customerEnrollment.task.CustomerEnrollmentFileGenerationTask; com.sapienter.jbilling.client.util.Constants" %>
%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%




<div class="column-hold">
    <div class="heading">
        <strong>
            ${ediFile?.name}
        </strong>

    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="edi.file.id" /></td>
                    <td class="value">${ediFile?.id}</td>
                </tr>
            <tr>
                <td><g:message code="edi.file.name"/></td>
                <td class="value">${ediFile?.name}</td>
            </tr>
            <tr>
                <td><g:message code="filters.userStatus.title"/></td>
                <td class="value">${ediFile?.fileStatus?.name}</td>
            </tr>
            <tr>
                <td><g:message code="edi.file.entity"/></td>
                <td class="value">${ediFile?.entity?.description}</td>
            </tr>
            <g:if test="${ediFile?.user}">
                <tr>
                    <td><g:message code="edi.file.customer"/></td>
                    <td class="value">
                        <g:remoteLink controller="customer" action="show" id="${ediFile?.user?.id}" before="register(this);" onSuccess="render(data, next);">
                            ${ediFile?.user?.id}
                        </g:remoteLink>
                    </td>
                </tr>
            </g:if>
            <g:if test="${ediFile?.utilityAccountNumber}">
                <tr>
                    <td><g:message code="edi.file.account.number"/></td>
                    <td class="value">${ediFile?.utilityAccountNumber}</td>
                </tr>
            </g:if>
            <g:if test="${ediFile?.startDate}">
                <tr>
                    <td><g:message code="edi.file.start.date"/></td>
                    <td class="value"><g:formatDate date="${ediFile?.startDate}" formatName="date.pretty.format"/></td>
                </tr>
            </g:if>
            <g:if test="${ediFile?.endDate}">
                <tr>
                    <td><g:message code="edi.file.end.date"/></td>
                    <td class="value"><g:formatDate date="${ediFile?.endDate}" formatName="date.pretty.format"/></td>
                </tr>
            </g:if>

            <tr>
                <td><g:message code="edi.file.record.date"/></td>
                <td class="value"><g:formatDate date="${ediFile?.createDatetime}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/></td>
            </tr>

            <g:if test="${ediFile?.exceptionCode}">
                <tr>
                    <td><g:message code="edi.file.record.exception.code"/></td>
                    <td class="value">${ediFile?.exceptionCode?.exceptionCode}</td>
                </tr>
                <g:if test="${!ediFile?.comment}">
                    <tr>
                        <td><g:message code="edi.file.record.exception.description"/></td>
                        <td class="value">${ediFile?.exceptionCode?.description}</td>
                    </tr>
                </g:if>
            </g:if>

            <g:if test="${ediFile?.comment}">
                <tr>
                    <td><g:message code="customer.enrollment.comment"/></td>
                    <td class="value">${ediFile?.comment}</td>
                </tr>
            </g:if>

            <g:if test="${ediFile?.ediFileRecords && ediFile.ediFileRecords.first().fileFields.find{it.ediFileFieldKey==com.sapienter.jbilling.server.customerEnrollment.task.CustomerEnrollmentFileGenerationTask.TRANS_REF_NR}?.ediFileFieldValue}">
                <tr>
                    <td> <g:message code="edi.file.enrollment.id" default="Enrollment"/> </td>
                    <td class="value">
                        <g:set var="enrollmentId" value="${ediFile?.ediFileRecords ? ediFile?.ediFileRecords?.first()?.fileFields?.find{it.ediFileFieldKey==com.sapienter.jbilling.server.customerEnrollment.task.CustomerEnrollmentFileGenerationTask.TRANS_REF_NR}?.ediFileFieldValue : ''}"/>
                        <g:link controller="customerEnrollment" action="list" params="[enrollmentId:enrollmentId]">Enrollment-${enrollmentId}</g:link>
                    </td>
                </tr>
            </g:if>

            </tbody>
        </table>

            <g:form action="updateFileStatus">
                <fieldset>
                    <div class="form-columns" style="display: none" id="editStatusBlock">
                        <g:applyLayout name="form/select" >
                            <content tag="label"><g:message code="edi.type.edit.status.label"/></content>
                            <content tag="label.for">status</content>
                            <g:hiddenField name="id" value="${ediFile?.id}"/>
                            <g:select name="ediFileStatusId"
                                      from="${ediFile?.fileStatus?.associatedEDIStatuses}"
                                      optionKey="id"
                                      optionValue="name"
                                      value="${ediFile.fileStatus.id}"
                            />
                        </g:applyLayout>
                        <div class="btn-box">
                        <div class="row">
                            <g:submitButton name="${g.message(code: 'update.edi.status')}" class="submit save"/>
                        </div>
                            </div>
                    </div>
                </fieldset>
            </g:form>

      </div>

    </div>

    <g:if test="${ediFile?.fileStatus?.associatedEDIStatuses}">
        <div class="btn-box">
            <div class="row">
                <sec:ifAllGranted roles="EDI_921">

                    <a href="javascript:void(0)" class="submit edit" onclick="$('#editStatusBlock').show(); $(this).hide()">
                        <span><g:message code="button.edit"/></span>
                    </a>
                </sec:ifAllGranted>
            </div>
        </div>

    </g:if>


    <div class="heading">
        <strong>
            <g:message code="edi.file.record.label" default="Edi File Records"/>


        </strong>
    </div>

    <div class="box">
        <div class="sub-box" id="ediFileRecords">
            <g:render template="ediRecordList" model="[ediFileRecords : ediFileRecords, ediFile:ediFile]" />

        </div>
    </div>


    <div class="btn-box">
        <div class="row">
                <g:link controller="ediFile" action="download" params="[id:ediFile?.id]" class="submit save"><span><g:message code="edi.type.button.download"/></span></g:link>
        </div>
    </div>

</div>
