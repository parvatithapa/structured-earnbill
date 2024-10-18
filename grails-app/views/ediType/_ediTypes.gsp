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

<div class="table-box">

    <table id="tbl-edi-types" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="first"><g:remoteSort action="list" sort="name" update="column1"><g:message
                    code="edi.type.name"/></g:remoteSort></th>
            <th><g:message code="edi.type.path"/></th>
            <th class="last"><g:message code="edi.type.entity"/></th>
        </tr>
        </thead>

<sec:ifAllGranted roles="EDI_920">
        <tbody>
        <g:each var="ediType" in="${ediTypes}">
            <tr id="config-${ediType.id}" class="${selected?.id == ediType?.id ? 'active' : ''}">

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ediType.id}" before="register(this);"
                                  onSuccess="render(data, next);" params="[max:params.max, offset:params.offset]">
                        <strong>${ediType?.name?.encodeAsHTML()}</strong>
                        <em>${ediType?.id}</em>
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ediType.id}" before="register(this);" params="[max:params.max, offset:params.offset]"
                                  onSuccess="render(data, next);">
                        ${ediType?.path.encodeAsHTML()}
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ediType.id}" before="register(this);" params="[max:params.max, offset:params.offset]"
                                  onSuccess="render(data, next);">
                        <g:if test="${ediType?.global == 1}">
                            <g:message code="dataTable.query.global"/>
                        </g:if>
                        <g:else test="${ediType.entities}">
                            ${ediType?.entities?.description.join(",")}
                        </g:else>
                    </g:remoteLink>
                </td>

            </tr>
        </g:each>
        </tbody>
</sec:ifAllGranted>
    </table>
</div>

<div class="pager-box">

    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
    </div>

    <div class="row">
        <jB:remotePaginate action="list" total="${ediTypes?.totalCount ?: 0}" update="column1"
                             params="[partial: true]"/>
    </div>
</div>

<div class="btn-box">
    <sec:ifAllGranted roles="EDI_921">
        <g:remoteLink action='edit' class="submit add" update="column2" params="[max: params.max, offset: 0]"><span><g:message
                code="edi.type.button.create"/></span></g:remoteLink>
    </sec:ifAllGranted>
</div>

