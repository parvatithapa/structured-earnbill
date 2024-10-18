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
            ${ediFileStatus?.name}
        </strong>
    </div>
    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="edi.file.status.id" default="id"/></td>
                    <td class="value">${ediFileStatus?.id}</td>
                </tr>
            <tr>
                <td><g:message code="edi.file.status.name" default="Name"/></td>
                <td class="value">${ediFileStatus?.name}</td>
            </tr>
            <tr>
                <td><g:message code="edi.file.status.date.created" default="Date Created"/></td>
                <td class="value"><g:formatDate date="${ediFileStatus?.createDatetime}" format="MM/dd/yyyy" timeZone="${session['company_timezone']}"/> </td>
            </tr>


            <g:if test="${ediFileStatus?.associatedEDIStatuses}">
                <tr>
                    <td><g:message code="edi.file.status.child.status"/></td>
                    <td class="value">
                        <g:each in="${ediFileStatus?.associatedEDIStatuses}" var="child">
                            ${child.name}<br/>
                        </g:each>
                    </td>
                </tr>
            </g:if>
            <g:if test="${ediFileStatus?.getExceptionCodes()}">
                <tr>
                    <td><g:message code="edi.status.exception.code.label"/></td>
                    <td class="value">
                    <g:each in="${ediFileStatus?.exceptionCodes}" var="exceptionCode">

                        ${exceptionCode.exceptionCode}<br/>
                    </g:each>
                    </td>
                </tr>
            </g:if>

            </tbody>
        </table>


        </div>

    </div>

    <div class="btn-box">
        <div class="row">
            %{--<sec:ifAllGranted roles="EDI_921">--}%

            <g:remoteLink action='edit' class="submit edit" update="column2" params="[id:ediFileStatus?.id, typeId:params.typeId]"><span><g:message code="button.edit"/></span></g:remoteLink>

            %{--</sec:ifAllGranted>--}%
        </div>
    </div>

</div>
