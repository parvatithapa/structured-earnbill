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

<%--
  Shows a list of Mediation COnfigurations.

  @author Vikas Bodani
  @since  05-Oct-2011
--%>

<g:set var="isRoot" value="${new CompanyDAS().isRoot(session['company_id'])}" />

<g:if test="${lastMediationProcessStatus}">
    <div id="msg-med-info" class="msg-box wide info" >

        <strong> <g:message code="mediation.config.last.process"/> </strong>
        <a class="msg-box-close" onclick="$('#msg-med-info').hide();">&#xe020;</a>
        <g:link controller="mediation" action="show" id="${lastMediationProcessStatus.mediationProcessId}">
            ${lastMediationProcessStatus.mediationProcessId}
        </g:link>

    </div>
</g:if>

<div class="table-box">
    <table id="tbl-mediation-config" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th><g:message code="mediation.config.name"/></th>
                <th><g:message code="mediation.config.order"/></th>
                <th><g:message code="mediation.config.plugin"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="config" in="${configs}">

                <tr id="config-${config.id}" class="${selected?.id == config.id ? 'active' : ''}">
                    <!-- Name ID -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${config.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(config?.name)}</strong>
                            <em><g:message code="table.id.format" args="[config.id as String]"/></em>
                        </g:remoteLink>
                    </td>
                    
                    <!-- Order -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${config.id}" before="register(this);" onSuccess="render(data, next);">
                            ${config?.orderValue}
                        </g:remoteLink>
                    </td>

                    <!-- Plugin -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${config.id}" before="register(this);" onSuccess="render(data, next);">

                            <g:if test="${MediationVersion.MEDIATION_VERSION_3_0.isEqualTo(
                                    Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)) ||
                                    MediationVersion.MEDIATION_VERSION_4_0.isEqualTo(
                                            Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))}" >

                                ${StringEscapeUtils.escapeHtml(config?.mediationJobLauncher)}
                            </g:if>
                            <g:else>

                                <g:set var="configReader" value="${readers.find{ it.id == config.pluggableTaskId}}"/>

                                ${'(' + configReader?.id + ') ' + StringEscapeUtils.escapeHtml(configReader?.type?.getDescription(session.language_id))}
                            </g:else>

                        </g:remoteLink>
                    </td>
                    
                </tr>

            </g:each>
        </tbody>
    </table>
</div>

<div class="btn-box table-box-below">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>

    <g:if test="${!isMediationProcessRunning && configs && (isRoot || hasNonGlobalConfig)}">
        <g:link controller="mediationConfig" action="run" class="submit play"><span><g:message code="button.run.mediation"/></span></g:link>
    </g:if>
</div>
