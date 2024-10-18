%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2012 Enterprise jBilling Software Ltd. and Emiliano Conde

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

<%@ page import="com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.common.Util; com.sapienter.jbilling.server.mediation.MediationVersion" contentType="text/html;charset=UTF-8" %>



<div class="column-hold">

    <div class="heading">
        <strong>
                <g:message code="edit.type.file.title"/>
        </strong>
    </div>

    <g:uploadForm id="edi-file-upload" name="edi-file-upload" url="[action: 'save']">
        <g:hiddenField name="id" value="${ediType?.id? ediType?.id:0}"/>

        <div class="box" >
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">

                        <g:hiddenField name="max" value="${params.max}" />
                        <g:hiddenField name="offset" value="${params.offset}" />
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="edi.type.name"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">name</content>
                            <g:textField name="name" class="field" value="${ediType?.name}"/>
                        </g:applyLayout>

                        <g:isRoot>
                            <g:applyLayout name="form/select_multiple" >
                                <content tag="label"><g:message code="prompt.user.company"/></content>
                                <content tag="label.for">user.entityId</content>
                                <g:select name="entities" disabled="${ediType?.global==1? true:false}"
                                          from="${companies}"
                                          optionKey="id"
                                          optionValue="${{it?.description}}"
                                          value="${session['company_id']}"
                                          multiple="multiple"
                                          onChange="${remoteFunction(controller: 'customer', action: 'getAccountTypes',
                                                  update: 'account-select',
                                                  params: '\'user.entityId=\' + this.value')}" />
                            </g:applyLayout>

                        </g:isRoot>
                        <g:isNotRoot>
                            <g:hiddenField name="entities" value="${session['company_id']}"/>
                        </g:isNotRoot>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="bean.EDITypeWS.ediSuffix"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">EDI Suffix</content>
                            <g:textField name="ediSuffix" class="field" value="${ediType?.ediSuffix}" />
                        </g:applyLayout>

                        <g:isRoot>
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="product.assign.global"/></content>
                                <content tag="label.for">global-checkbox</content>
                                <g:checkBox id="global-checkbox" class="cb checkbox" name="global" checked="${ediType?.global?true:false}"/>
                            </g:applyLayout>

                        </g:isRoot>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="edi.type.edit.type.xml"/></content>
                            <input type="file" name="events">
                        </g:applyLayout>


                        <div class="statusContainer">
                            <g:if test="${ediType?.ediStatuses}">
                                <g:each in="${ediType?.ediStatuses}" var="status" status="i">
                                    <g:applyLayout name="form/description">
                                        <content tag="label">
                                            <g:if test="${i==0}">
                                                <g:message code='edi.type.edit.status.label'/>
                                            </g:if>
                                            &nbsp;
                                        </content>
                                        <content tag="label.for">ediFileStatus.name</content>
                                        <input type="text" name="ediFileStatus.name" value="${status?.name}" class="field" >
                                        <g:hiddenField name="ediFileStatus.id" value="${status?.id}"/>

                                        <div class="actionDiv ">
                                             <a class="deleteStatus plus-icon">&#xe000;</a>
                                         </div>

                                    </g:applyLayout>
                                </g:each>
                            </g:if>
                            %{--<g:applyLayout name="form/description">
                                <content tag="label">
                                    &nbsp;
                                </content>
                                <content tag="label.for">ediFileStatus.name</content>
                                <input type="text" name="ediFileStatus.name"  value="" class="field" >
                                <g:hiddenField name="ediFileStatus.id" value=""/>
                                <div class="actionDiv">
                                    <a class="addStatus" ><img src="${resource(dir:'images', file:'add.png')}" alt="add"/></a>
                                </div>
                            </g:applyLayout>--}%
                        </div>

                        <div class="deleteLink" style="display: none">
                                <a class="deleteStatus plus-icon">&#xe000;</a>
                        </div>

                        %{--<div id="cloneStatus" style="display: none">
                            <g:applyLayout name="form/description">
                                <content tag="label">
                                    &nbsp;
                                </content>
                                <content tag="label.for">ediFileStatus.name</content>
                                <input type="text" name="ediFileStatus.name"  value="" class="field" >
                                <g:hiddenField name="ediFileStatus.id" value=""/>
                                <div class="actionDiv">
                                <a class="addStatus" ><img src="${resource(dir:'images', file:'add.png')}" alt="add"/></a>
                                </div>
                            </g:applyLayout>
                        </div>--}%


                    </div>
                </fieldset>
            </div>
        </div>


    <div class="btn-box buttons">
        <div class="row">
            <g:submitButton name="${g.message(code: 'button.save')}" class="submit save"/>
            <g:link controller="ediType" action="list" id="${ediType?.id}" params="[max:params.max, offset:params.offset]" class="submit cancel">
                <span><g:message code="button.cancel"/></span>
            </g:link>
        </div>
    </div>
    </g:uploadForm>

        </div>
<script>
    $(function(){
        $('#global-checkbox').on('click', function(){
            var mandatory=$("#entities")
            if ($(this).is(':checked')) {
                mandatory.attr('checked', false);
                mandatory.attr('disabled', true);
            }else {
                mandatory.attr('disabled', false);
            }
        });

        $(document).on("click", '.addStatus', function(){
            $(".statusContainer").append($("#cloneStatus").html());
            $(this).closest('.actionDiv').html($(".deleteLink").html())
        });

        $(document).on("click", '.deleteStatus', function(){
            $(this).closest('.row').remove();
        });

    });



</script>
