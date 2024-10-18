<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDAS; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.common.Util; com.sapienter.jbilling.server.mediation.MediationVersion" %>
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
            ${ediType?.name}

        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="edi.type.id" /></td>
                    <td class="value">${ediType?.id}</td>
                </tr>
            <tr>
                <td><g:message code="edi.type.name"/></td>
                <td class="value">${ediType?.name}</td>
            </tr>
            <tr>
                <td><g:message code="edi.type.path"/></td>
                <td class="value">${ediType?.path}</td>
            </tr>
            <tr>
                <td><g:message code="bean.EDITypeWS.ediSuffix"/></td>
                <td class="value">${ediType?.ediSuffix}</td>
            </tr>

            <g:if test="${ediType.statuses}">
                <tr>
                    <td><g:message code="edi.type.available.status"/></td>
                    <td>
                        <table class="dataTable" cellspacing="0" cellpadding="0">
                            <thead>
                            <tr>
                               <td class="value"><g:message code="bean.LanguageWS.id"/> </td>
                               <td class="value"><g:message code="filters.status.label"/> </td>
                            </tr>
                            </thead>
                            <tbody>
                        <g:each in="${ediType.statuses.sort{ it.id }}" var="status">
                            <tr>
                                <td class="value"> ${status?.id}</td>
                                <td class="value">${status?.name}</td>
                            </tr>
                        </g:each>
                            </tbody>
                        </table>
                    </td>
                </tr>

            </g:if>

            </tbody>
        </table>
            <g:uploadForm id="response-file-upload" name="edi-file-upload" url="[action: 'uploadOutboundFile']" style="display: none">
                <div class="form-columns">
                    <g:hiddenField name="ediTypeId" value="${ediType?.id}"/>
                    <g:hiddenField name="max" value="${params?.max}"/>
                    <g:hiddenField name="offset" value="${params?.offset}"/>

                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="edi.type.inbound.file"/></content>
                        <input type="file" name="file">
                    </g:applyLayout>

                    <g:applyLayout name="form/text">
                        <content tag="label">&nbsp;</content>
                        <g:submitButton name="submit" class="submit save" value="Upload"/>
                    </g:applyLayout>

                </div>

            </g:uploadForm>
        </div>
    </div>


    <div class="btn-box">
        <div class="row">
            <sec:ifAllGranted roles="EDI_921">
                <g:remoteLink action='edit' class="submit edit" update="column2" params="[id:ediType?.id, max:params.max, offset:params.offset]"><span><g:message code="button.edit"/></span></g:remoteLink>
            </sec:ifAllGranted>
                <sec:ifAllGranted roles="EDI_920">
                    <g:link controller="ediType" action="testEdi" params="[id:ediType?.id]" class="submit edit"><span><g:message code="route.test"/></span></g:link>
                    <g:link controller="ediType" action="download" params="[id:ediType?.id, max:params.max, offset:params.offset]" class="submit save"><span><g:message code="edi.type.button.download"/></span></g:link>
                    <g:link controller="ediFile" action="list" params="[typeId:ediType?.id]" class="submit edit"><span><g:message code="customer.enrollment.edi.files"/></span></g:link>
                    <a href="javascript:void(0)" id="upload-edi-response-file" class="submit save"><span><g:message code="edi.type.button.upload"/></span></a>
                </sec:ifAllGranted>
            <g:link controller="ediStatus" action="list" params="[typeId:ediType?.id]" class="submit edit"><span><g:message code="edi.file.statuses"/></span></g:link>
        </div>
    </div>

</div>
<script>
    $(function(){
        <g:if test="${flash.error}">
        $("#response-file-upload").show()
        </g:if>
       $("#upload-edi-response-file").on('click', function(){
           $("#response-file-upload").show()
           $(this).hide()
       })
    });
</script>