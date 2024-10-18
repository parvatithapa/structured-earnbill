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

<%@ page import="java.util.stream.Collectors; com.sapienter.jbilling.server.mediation.MediationVersion; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.common.Util" contentType="text/html;charset=UTF-8" %>

<%--
  Shows edit form for Mediation Configuration.

  @author Vikas Bodani
  @since  05-Oct-2011
--%>

<div class="column-hold">

    <g:set var="isNew" value="${!config || !config?.id || config?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="mediation.config.add.title"/>
            </g:if>
            <g:else>
                <g:message code="mediation.config.edit.title"/>
            </g:else>
        </strong>
    </div>

    <g:form id="save-config-form" name="mediation-config-form" url="[action: 'save']">

        <div class="box">
            <fieldset>
                <div class="form-columns form-columns-spacer">
                    <g:hiddenField name="id" value="${config?.id?:0}"/>
                    <g:hiddenField name="entityId" value="${session['company_id']}"/>
                    <g:hiddenField name="createDatetime" value="${config?.createDatetime ?
                        formatDate(date: config.createDatetime, formatName: 'date.format') : null}"/>
                    <g:hiddenField name="versionNum" value="${config?.versionNum?: 0}"/>

                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="mediation.config.name"/></content>
                        <content tag="label.for">name</content>
                        <g:textField class="field" name="name" value="${config?.name}"/>
                    </g:applyLayout>

                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="mediation.config.order"/></content>
                        <content tag="label.for">orderValue</content>
                        <g:textField class="field" name="orderValue" value="${config?.orderValue}"/>
                    </g:applyLayout>

                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="mediation.config.local.dir"/></content>
                        <content tag="label.for">localInputDirectory</content>
                        <g:textField class="field" name="localInputDirectory" value="${config?.localInputDirectory}"/>
                    </g:applyLayout>

                    <g:if test="${MediationVersion.MEDIATION_VERSION_3_0.isEqualTo(
                            Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)) ||
                            MediationVersion.MEDIATION_VERSION_4_0.isEqualTo(
                                    Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))}" >

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="mediation.config.job.launcher"/></content>
                            <content tag="label.for">mediationJobLauncher</content>
                            <content tag="include.script">true</content>
                            <g:select from="${com.sapienter.jbilling.server.mediation.converter.MediationJobs.getJobs().findAll{
                                it.getRecycleJob() != null}}"
                                      optionKey="job"
                                      optionValue="job"
                                      name="mediationJobLauncher"
                                      value="${config?.mediationJobLauncher}"/>
                        %{--<g:textField class="field" name="mediationJobLauncher" value="${config?.mediationJobLauncher}"/>--}%
                        </g:applyLayout>

                        <g:if test="${global}" >
                            <g:isRoot>
                                <g:applyLayout name="form/checkbox">
                                    <content tag="label"><g:message code="mediation.config.global"/></content>
                                    <content tag="label.for">global</content>
                                    <g:checkBox class="cb checkbox" name="global" checked="${config?.global}"/>
                                </g:applyLayout>
                            </g:isRoot>
                        </g:if>
                    </g:if>
                    <g:else>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="mediation.config.plugin"/></content>
                            <content tag="label.for">pluggableTaskId</content>
                            <content tag="include.script">true</content>
                            <g:select from="${readers}"
                                      optionKey="id"
                                      optionValue="${{'(Id:' + it.id + ') ' + it.type?.getDescription(session['language_id'])}}"
                                      name="pluggableTaskId"
                                      value="${config?.pluggableTaskId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <g:checkBox class="cb checkbox" name="global" style="display:none" />
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="mediation.config.processor"/></content>
                            <content tag="label.for">processorTaskId</content>
                            <content tag="include.script">true</content>
                            <g:select from="${processors}"
                                      optionKey="id"
                                      optionValue="${{it.type.getDescription(session['language_id'], 'title') + ' (' + it.id + ')'}}"
                                      name="processorTaskId"
                                      value="${config?.processorTaskId}"/>
                        </g:applyLayout>
                    </g:else>

                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="mediation.config.rootRoute"/></content>
                        <content tag="label.for">rootRoute</content>
                        <content tag="include.script">true</content>
                        <g:select from="${routes}"
                                  noSelection="${['null':'Select One...']}"
                                  optionKey="id"
                                  optionValue="${{it?.name}}"
                                  name="rootRoute"
                                  value="${config?.rootRoute}"/>
                    </g:applyLayout>
                    <script>
                        var mediationJobsHandleRootRateMap = {};
                        var mediationJobsNeedsInputDirectory = {}
                        <g:each var="mediationJob" in="${com.sapienter.jbilling.server.mediation.converter.MediationJobs.getJobs().findAll{
                    it.getRecycleJob() != null}}">
                        mediationJobsHandleRootRateMap["${mediationJob.job}"] = "${mediationJob.handleRootRateTables()}";
                        mediationJobsNeedsInputDirectory["${mediationJob.job}"] = "${mediationJob.needsInputDirectory()}";
                        </g:each>
                        showHideRootRoutAndNeedInputFolderOnMediationJobChange();
                        $('#mediationJobLauncher').on("change", showHideRootRoutAndNeedInputFolderOnMediationJobChange);
                        function showHideRootRoutAndNeedInputFolderOnMediationJobChange() {
                            var mediationJobChosen = $('#mediationJobLauncher').val();
                            if (mediationJobsHandleRootRateMap[mediationJobChosen] == "true") {
                                $('#rootRoute').closest('row').show();
                            } else {
                                $('#rootRoute').closest('row').hide();
                            }
                            if (mediationJobsNeedsInputDirectory[mediationJobChosen] == "true") {
                                $('#localInputDirectory').parent().parent().show();
                            } else {
                                $('#localInputDirectory').parent().parent().hide();
                            }
                        }
                    </script>

                </div>
            </fieldset>

            <div class="btn-box buttons">
                <ul>
                    <li><a class="submit save button-primary" onclick="$('#save-config-form').submit();"><span><g:message code="button.save"/></span></a></li>
                    <li><a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a></li>
                </ul>
            </div>

        </div>

    </g:form>
</div>
