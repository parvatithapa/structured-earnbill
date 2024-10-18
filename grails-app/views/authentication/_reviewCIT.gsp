<%@ page import="com.sapienter.jbilling.server.metafields.EntityType" %>
<%@ page import="com.sapienter.jbilling.server.metafields.DataType"  %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%--
  Shows a review of the company information type metafields

  @author Aamir Ali
  @since  02/21/2017
--%>

<div id="review-box">

    <div id="messages">
        <g:if test="${errorMessages}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${errorMessages}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </div>

            <g:set var="errorMessages" value=""/>
            <ul></ul>
        </g:if>
    </div>

    <div class="box no-heading">
        <div class="sub-box">

            <div class="header">
                <div class="column">
                    <h1><g:message code="company.information.type.metafields.label" args="[cit.id ?: '']"/></h1>
                </div>

                <div style="clear: both;"></div>
            </div>

            <hr/>

            <ul id="metafield-ait">
                <g:each var="metaField" status="index" in="${cit?.metaFields}">
                    <g:set var="editable" value="${index == params.int('newLineIndex')}"/>
                    <g:set var="metaFieldTypeUsages" value="${compInfoType}"/>
                    <g:if test="${metaField.fieldUsage}">
                        <g:set var="fieldType" value="${metaField.fieldUsage}"/>
                        <g:set var="metaFieldTypeUsages" value="${metaFieldTypeUsages + fieldType}"/>
                    </g:if>
                    <g:formRemote name="mf-${index}-update-form" url="[action: 'editCIT']"
                                  update="column2" method="GET">

                        <fieldset>

                            <g:hiddenField name="_eventId" value="updateMetaField"/>
                            <g:hiddenField name="execution" value="${flowExecutionKey}"/>

                            <li id="mf-${index}" class="mf ${editable ? 'active' : ''}">
                                <span class="description">${metaField.name? metaField.name : "-"}</span>
                                <span class="data-type">${metaField.dataType? metaField.dataType : "-"}</span>
                                <span class="mandatory">${metaField.mandatory?'Mandatory':'Not Mandatory'}</span>
                            </li>

                            <li id="mf-${index}-editor" class="editor ${editable ? 'open' : ''}">

                                <div class="box">
                                    <% params.entityType = com.sapienter.jbilling.server.metafields.EntityType.COMPANY_INFO.name(); %>
                                    <g:render template="/metaFields/editMetafield"
                                              model="[metaField: metaField,
                                                      entityType: params.entityType,
                                                      metaFieldType:metaField.dataType,
                                                      parentId: 'mf-'+index+'-update-form',
                                                      metaFieldIdx:index, displayMetaFieldType: true,
                                                      dependencyCheckBox: params.dependencyCheckBox,
                                                      excludeSelf: metaField
                                              ]" />

                                    <g:hiddenField name="index" value="${index}"/>
                                </div>

                                <div class="btn-box">
                                    <a class="submit save" onclick="$('#mf-${index}-update-form').submit();"><span><g:message
                                            code="button.update"/></span></a>
                                    <g:remoteLink class="submit cancel" action="editCIT" params="[_eventId: 'removeMetaField', index: index]"
                                                  update="column2" method="GET">
                                        <span><g:message code="button.remove"/></span>
                                    </g:remoteLink>
                                </div>

                            </li>

                        </fieldset>

                    </g:formRemote>
                </g:each>

                <g:if test="${!cit?.metaFields}">
                    <li><em><g:message code="company.information.type.no.metafields"/></em></li>
                </g:if>
            </ul>
        </div>
    </div>

    <div class="btn-box ait-btn-box">
        <g:link class="submit save" action="editCIT" params="[_eventId: 'save']">
            <span><g:message code="button.save"/></span>
            <g:hiddenField name="saveInProgress" value="false"/>
        </g:link>

        <g:link class="submit cancel" action="editCIT" params="[_eventId: 'cancel']">
            <span><g:message code="button.cancel"/></span>
        </g:link>
    </div>

    <script type="text/javascript">
        $('#metafield-ait li.mf').click(function() {
            var id = $(this).attr('id');
            $('#' + id).toggleClass('active');
            $('#' + id + '-editor').toggle('blind');
        });

        $('select[id^="metaField"][id$="dataType"]').change(function () {
	        if ($(this).val() == '${DataType.ENUMERATION}' || $(this).val() == '${DataType.LIST}') {
	            $('.field-name').hide().find('input').prop('disabled', 'true');
	            $('.field-enumeration').show().find('select').prop('disabled', '');
	            $('.field-filename').hide().find('input').prop('disabled', 'true')
	        } else if ($(this).val() == '${DataType.SCRIPT}'){
	            $('.field-name').show().find('input').prop('disabled', '');
	            $('.field-enumeration').hide().find('select').prop('disabled', 'true');
	            $('.field-filename').show().find('input').prop('disabled', '')
	        } else {
	            $('.field-name').show().find('input').prop('disabled', '');
	            $('.field-enumeration').hide().find('select').prop('disabled', 'true');
	            $('.field-filename').hide().find('input').prop('disabled', 'true')
	        }
   		 });
    </script>


</div>
