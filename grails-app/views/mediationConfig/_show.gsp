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

<%--
  Shows Mediation Configuration.

  @author Vikas Bodani
  @since  05-Oct-2011
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.name}
        <%-- <em>${selected.id}</em> --%>
        </strong>
    </div>

    <g:set var="isRoot" value="${new CompanyDAS().isRoot(session['company_id'])}" />

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="mediation.config.id" />
                    </td>
                    <td class="value">
                        ${selected.id}
                    </td>
                </tr>
                <tr>
                    <td><g:message code="mediation.config.order" />
                    </td>
                    <td class="value">
                        ${selected?.orderValue}
                    </td>
                </tr>

                <g:if test="${MediationVersion.MEDIATION_VERSION_3_0.isEqualTo(
                        Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)) ||
                        MediationVersion.MEDIATION_VERSION_4_0.isEqualTo(
                                Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))}" >

                    <tr>
                        <td><g:message code="mediation.config.job.launcher" />
                        </td>
                        <td class="value">
                            ${selected?.mediationJobLauncher}
                        </td>
                    </tr>
                    <tr>
                    	<td><g:message code="mediation.config.local.dir" />
                        </td>
                        <td class="value">
                            ${selected?.localInputDirectory}
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.config.rootRoute" />
                        </td>
                        <td class="value">
                            ${selected?.rootRoute?.name}
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.config.global" />
                        </td>
                        <td class="value">
                            <g:formatBoolean boolean="${selected?.global}"/>
                        </td>
                    </tr>

                    <tr>
                        <td><g:message code="mediation.config.cdr.recycle.count" />
                        </td>
                        <td class="value">
                            ${selected?.cdrsForRecycle}
                        </td>
                    </tr>
                </g:if>
                <g:else>
                    <tr>
                        <td><g:message code="mediation.config.plugin" />
                        </td>
                        <td class="value">
                            ${'(' + selected?.pluggableTask?.id + ') ' + selected?.pluggableTask?.type?.getDescription(session.language_id)}
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.config.processor"/></td>
                        <td class="value">
                            ${'(' + selected?.processor?.id + ') ' + selected?.processor?.type?.getDescription(session.language_id)}
                        </td>
                    </tr>
                </g:else>
            </tbody>
        </table>
        <g:if test="${mediationRatingScheme}">
            <div class="box-cards box-cards-open">
                <div class="box-cards-title">
                    <span><g:message code="bean.ratingScheme.mediation"/></span>
                </div>
                <div class="sub-box">
                    <table class="dataTable" cellspacing="0" cellpadding="0">
                        <tbody>
                        <tr>
                            <td><g:message code="ratingScheme.id"/></td>
                            <td class="value">${mediationRatingScheme?.id}</td>
                        </tr>
                        <tr>
                            <td><g:message code="ratingScheme.name"/></td>
                            <td class="value">${mediationRatingScheme?.name}</td>
                        </tr>
                        <tr>
                            <td><g:message code="ratingScheme.initial.increment"/></td>
                            <td class="value">${mediationRatingScheme?.initialIncrement}</td>
                        </tr>
                        <tr>
                            <td><g:message code="ratingScheme.initial.rounding.mode"/></td>
                            <g:if test="${mediationRatingScheme?.initialRoundingMode==1}">
                                <g:set var="initialRoundingMode" value="ROUND DOWN"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.initialRoundingMode==0}">
                                <g:set var="initialRoundingMode" value="ROUND UP"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.initialRoundingMode==4}">
                                <g:set var="initialRoundingMode" value="HALF ROUND UP"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.initialRoundingMode==5}">
                                <g:set var="initialRoundingMode" value="HALF ROUND DOWN"></g:set>
                            </g:if>
                            <td class="value">${initialRoundingMode}</td>
                        </tr>
                        <tr>
                            <td><g:message code="ratingScheme.main.increment"/></td>
                            <td class="value">${mediationRatingScheme?.mainIncrement}</td>
                        </tr>
                        <tr>
                            <td><g:message code="ratingScheme.main.rounding.mode"/></td>
                            <g:if test="${mediationRatingScheme?.mainRoundingMode==1}">
                                <g:set var="mainRoundingMode" value="ROUND DOWN"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.mainRoundingMode==0}">
                                <g:set var="mainRoundingMode" value="ROUND UP"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.mainRoundingMode==4}">
                                <g:set var="mainRoundingMode" value="HALF ROUND UP"></g:set>
                            </g:if>
                            <g:if test="${mediationRatingScheme?.mainRoundingMode==5}">
                                <g:set var="mainRoundingMode" value="HALF ROUND DOWN"></g:set>
                            </g:if>
                            <td class="value">${mainRoundingMode}</td>
                        </tr>
                        <g:if test="${mediationRatingScheme?.global}">
                            <tr>
                                <td><g:message code="ratingScheme.mediation.assign.global"/></td>
                                <td class="value"><g:checkBox name="global" class="cb checkbox" checked="true" disabled="true" /></td>
                            </tr>
                        </g:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </g:if>
    </div>
</div>

    <g:if test="${isRoot || !selected?.global}" >
        <div class="btn-box">

            <div class="row">
                <g:if test="${fileInjectionEnabled}">
                    <g:remoteLink class="submit order" id="${selected.id}" action="showInject" before="register(this);" onSuccess="render(data, second);"
                    params="${params + ['fileInjectionEnabled': true]}"
                    update="column2">
                        <span><g:message code="button.inject.file"/></span>
                    </g:remoteLink>
                </g:if>
                <g:remoteLink class="submit add" id="${selected.id}" action="showInject" before="register(this);" onSuccess="render(data, second);"
                    params="${params + ['fileInjectionEnabled': false]}" update="column2">
                    <span><g:message code="button.inject.record"/></span>
                </g:remoteLink>
                <%--
                Process Recycle CDR button is being commented for AC due to problems faced. It was hanging the system.
                Instead use the process/file level option to recycle the errors.
                 <g:if test="${MediationVersion.MEDIATION_VERSION_3_0.isEqualTo(
                            Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)) ||
                        MediationVersion.MEDIATION_VERSION_4_0.isEqualTo(
                                Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))}" >
                    <g:if test="${selected?.cdrsForRecycle}"> <!-- TODO 4419 - change condition to => selected?.cdrsForRecycle > 0  -->
                        <g:link class="submit" id="${selected.id}" action="recycleCDRs">
                            <span><g:message code="button.recycle.records"/></span>
                        </g:link>
                    </g:if>
                </g:if>
            	--%>
				<g:remoteLink class="submit edit" id="${selected.id}" action="edit" before="register(this);" onSuccess="render(data, second);" update="column2">
                    <span><g:message code="button.edit"/></span>
                </g:remoteLink>
                <g:link class="submit edit" params="[id:selected.id]" action="showAllMediationErrorOfMediationConfig" controller="mediationConfig" >
                    <span><g:message code="button.show.errors"/></span>
                </g:link>
            </div>

            <div class="row">
                <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </div>
        </div>
    </g:if>

    <g:render template="/confirm"
              model="['message': 'mediation.config.delete.confirm',
                      'controller': 'mediationConfig',
                      'action': 'delete',
                      'id': selected.id,
                     ]"/>
</div>
