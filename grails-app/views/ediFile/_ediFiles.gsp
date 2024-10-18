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
  
<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.db.CompanyDAS; com.sapienter.jbilling.common.Util; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.mediation.MediationVersion" contentType="text/html;charset=UTF-8" %>

<div class="table-box" id="ediFiles">

    <table id="tbl-mediation-config" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="first"><g:remoteSort action="list"  searchParams="[typeId:params.typeId, enrollmentId:params.enrollmentId]" sort="name" update="ediFiles"><g:message code="edi.file.name"/></g:remoteSort></th>
            <th><g:remoteSort action="list" searchParams="[typeId:params.typeId, enrollmentId:params.enrollmentId]"  sort="name" update="ediFiles"><g:message code="edi.file.type"/></g:remoteSort></th>
            <th><g:remoteSort action="list" searchParams="[typeId:params.typeId, enrollmentId:params.enrollmentId]"  sort="name" update="ediFiles"><g:message code="edi.type.edit.status.label"/></g:remoteSort></th>
            <th class="last"><g:remoteSort action="list" searchParams="[typeId:params.typeId, enrollmentId:params.enrollmentId]"  sort="type" update="ediFiles"><g:message code="edi.file.transaction.type"/></g:remoteSort></th>
        </tr>
        </thead>

    <sec:ifAllGranted roles="EDI_922">
        <tbody>
        <g:each var="ediFile" in="${ediFiles}">

            <tr id="config-${ediFile.id}" class="${selected?.id == ediFile?.id ? 'active' : ''}">

                <td class="max-width-column">
                    <g:remoteLink class="cell double" controller="ediFile" action="show" id="${ediFile.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFile?.name}
                        <em>Id: ${ediFile?.id}</em>
                    </g:remoteLink>
                </td>

                <td class="max-width-column">
                    <g:remoteLink class="cell double" controller="ediFile" action="show" id="${ediFile.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFile?.ediType.name}
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" controller="ediFile" action="show" id="${ediFile.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFile?.fileStatus.name}
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" controller="ediFile" action="show" id="${ediFile.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFile?.type}
                    </g:remoteLink>
                </td>

            </tr>
        </g:each>
        </tbody>
        </sec:ifAllGranted>
    </table>
    <div class="pager-box">

        <div class="row">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'ediFiles', action:'list', extraParams:[typeId:params.typeId, enrollmentId:params.enrollmentId]]"  />
            </div>

        </div>

        <div class="row-center">
            <jB:remotePaginate  action="list" total="${ediFiles?.totalCount ?: 0}" update="ediFiles" params="[partial:true,  action:'list', typeId: params.typeId, enrollmentId:params.enrollmentId]"/>
        </div>
    </div>

</div>
