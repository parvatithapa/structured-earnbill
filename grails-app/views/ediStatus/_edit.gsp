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


<div class="column-hold">

    <div class="heading">
        <strong>
                <g:message code="edit.edi.status.title" />
        </strong>
    </div>

    <g:form id="edi-file-upload" name="edi-file-upload" url="[action: 'save']">
        <g:hiddenField name="id" value="${ediStatus?.id? ediStatus?.id:0}"/>
        <g:hiddenField name="typeId" value="${params.typeId}"/>
        <div class="box" >
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="edi.status.edit.name"/></content>
                            <content tag="label.for">name</content>
                            <g:textField name="name" class="field" value="${ediStatus?.name}"/>
                        </g:applyLayout>

                            <g:applyLayout name="form/select_multiple">
                                <content tag="label"><g:message code="child.edi.status"/></content>
                                <content tag="label.for">childStatusIds</content>
                                <g:select name="childStatusIds"
                                          from="${childEdiFileStatus.sort{it.id}}"
                                          optionKey="id"
                                          optionValue="name"
                                          multiple="multiple"
                                    value="${ediStatus?.associatedEDIStatuses?.id}"
                                         />
                            </g:applyLayout>


                        <div class="statusContainer">
                            <g:if test="${ediStatus?.exceptionCodes}">
                                <g:each in="${ediStatus?.exceptionCodes.sort{it.id}}" var="exceptionCode" status="i">
                                    <div class="row">
                                        <label >
                                            <g:if test="${i==0}">
                                                <g:message code='edi.status.exception.code.label'/>
                                            </g:if>
                                            &nbsp;
                                        </label>

                                        <input type="text" readonly="readonly" name="exceptionCode.code" placeholder="Code" value="${exceptionCode?.exceptionCode}"  >
                                        <input type="text" name="exceptionCode.description"  placeholder="Description" value="${exceptionCode?.description}"  >
                                        <g:hiddenField name="exceptionCode.id" value="${exceptionCode?.id}"/>

                                    </div>
                                </g:each>
                            </g:if>

                            <div class="row">
                                <label >
                                    &nbsp;
                                </label>

                                <input type="text" name="exceptionCode.code"  placeholder="Code" value=""  >
                                <input type="text" name="exceptionCode.description"  placeholder="Description" value=""  >
                                <g:hiddenField name="exceptionCode.id" value=""/>
                                <div class="actionDiv" style="display: inline-block">
                                <a class="addStatus plus-icon" >&#xe026;</a>
                            </div>
                            </div>

                            <div class="deleteLink" style="display: none">
                                <a class="deleteStatus plus-icon">&#xe000;</a>
                            </div>

                            <div id="cloneStatus" style="display: none">
                                <div class="row">
                                <label >
                                    &nbsp;
                                </label>

                                <input type="text"  name="exceptionCode.code"  placeholder="Code" value=""  >
                                <input type="text" name="exceptionCode.description"  placeholder="Description" value=""  >
                                <g:hiddenField name="exceptionCode.id" value=""/>
                                <div class="actionDiv" style="display: inline-block">
                                    <a class="addStatus plus-icon" >&#xe026;</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </fieldset>
            </div>
        </div>


    <div class="btn-box buttons">
        <div class="row">
            <g:submitButton name="${g.message(code: 'button.save')}" class="submit save"/>
            <g:link controller="ediType" action="list" id="${ediType?.id}" class="submit cancel">
                <span><g:message code="button.cancel"/></span>
            </g:link>
        </div>
    </div>
    </g:form>

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
