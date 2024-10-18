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
            ${ediFileRecord?.ediFileRecordHeader}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="edi.file.record.id" /></td>
                    <td class="value">${ediFileRecord?.id}</td>
                </tr>
            <tr>
                <td><g:message code="edi.file.record.header"/></td>
                <td class="value">${ediFileRecord?.ediFileRecordHeader}</td>
            </tr>

            <tr>
                <td><g:message code="edi.file.record.date"/></td>
                <td class="value"><g:formatDate date="${ediFileRecord?.creationTime}" formatName="date.pretty.format"/></td>
            </tr>

            <g:each in="${ediFileRecord?.fileFields.sort{it.ediFileFieldOrder}}" var="fileField">
                <tr>
                    <td>${fileField?.ediFileFieldKey}</td>
                    <td class="value">${fileField?.ediFileFieldValue}</td>
                </tr>

            </g:each>
            <g:if test="${ediFileRecord?.comment}">
                <tr>
                    <td><g:message code="edi.file.record.comment"/></td>
                    <td class="value" style="color: #ff0000">${ediFileRecord?.comment}</td>
                </tr>
            </g:if>

            </tbody>
        </table>
      </div>
    </div>

</div>
