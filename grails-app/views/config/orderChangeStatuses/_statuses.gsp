<%@page import="com.sapienter.jbilling.server.order.ApplyToOrder"%>
<%@ page import="com.sapienter.jbilling.server.util.InternationalDescriptionWS; com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO" %>
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

    <div id="orderChangeStatuses" class="form-hold">
        <g:hiddenField name="recCnt" value="${statuses.size()}"/>
        <g:set var="nextIndex" value="${statuses.size()}"/>
        <g:hiddenField name="stepIndex" value="${nextIndex}"/>
        <fieldset>
            <div class="form-columns column single">
                <table id="orderChangeStatusesTable" class="innerTable">
                    <thead class="innerHeader">
                    <tr>
                        <th class="left tiny first"><g:message code="config.order.change.status.id"/></th>
                        <th class="left large2"><g:message code="config.order.change.status.language"/></th>
                        <th class="left large2"><g:message code="config.order.change.status.name"/></th>
                        <th class="left tiny2"><g:message code="config.order.change.status.order"/></th>
                        <th class="left tiny2"><g:message code="config.order.change.status.apply"/></th>
                        <th class="left tiny2 last"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each status="iter" var="status" in="${statuses}">
                        <g:hiddenField name="obj[${iter}].id" value="${status.id}" />
                        <g:hiddenField name="obj[${iter}].deleted" value="${status.deleted}" />
                        <g:if test="${status.deleted == 0}">
                            <tr valign="top">
                                <td>
                                    <label id="orderChangeStatuses-table-label1">${status.id}</label>
                                </td>
                                <td class="tiny">
                                    <g:each in="${languages}" var="lang">
                                        <div class="lang_description_${lang.id}" style="${lang.id != com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID ? 'display: none;' : ''}">
                                            <label id="orderChangeStatuses-table-label2" for="obj_${iter}_description_${lang.id}">${lang.description}</label>
                                        </div>
                                    </g:each>
                                </td>
                                <td class="large2">
                                    <g:each in="${languages}" var="lang">
                                        <div class="lang_description_${lang.id}" style="${lang.id != com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID ? 'display: none;' : ''}">
                                            <g:set var="currentLangDescription" value=""/>
                                            <g:each in="${status.descriptions}" var="langDescription">
                                                <g:if test="${langDescription.languageId == lang.id}">
                                                    <g:set var="currentLangDescription" value="${langDescription.content}"/>
                                                </g:if>
                                            </g:each>
                                            <g:textField class="inp-bg inp-desc"
                                                         name="obj[${iter}].description_${lang.id}" id="obj_${iter}_description_${lang.id}"
                                                         value="${currentLangDescription}"/>
                                        </div>
                                    </g:each>
                                </td>
                                <td class="medium">
                                    <g:field type="number" class="inp-bg inp4" name="obj[${iter}].order" value="${status.order}"/>
                                </td>
                                <td class="tiny">
                                    <g:checkBox class="cb checkbox" name="obj[${iter}].applyToOrder" value="true" checked="${ApplyToOrder.YES.equals(status.applyToOrder)}"/>
                                </td>
                                <td class="tiny">
                                    <a class="plus-icon" onclick="removeOrderChangeStatus(${iter});">&#xe000;</a>
                                </td>
                            </tr>
                        </g:if>
                        <g:else>
                            <g:each in="${languages}" var="lang">
                                <g:set var="currentLangDescription" value=""/>
                                <g:each in="${status.descriptions}" var="langDescription">
                                    <g:if test="${langDescription.languageId == lang.id}">
                                        <g:set var="currentLangDescription" value="${langDescription.content}"/>
                                    </g:if>
                                </g:each>
                                <g:hiddenField name="obj[${iter}].description_${lang.id}" value="${currentLangDescription}"/>
                            </g:each>
                        </g:else>
                    </g:each>

                    <tr valign="top">
                        <td>

                        </td>
                        <td >
                            <g:each in="${languages}" var="lang">
                                <div class="lang_description_${lang.id}" style="${lang.id != com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID ? 'display: none;' : ''}">
                                    <label id="orderChangeStatuses-table-label2" for="obj_${nextIndex}_description_${lang.id}">${lang.description}</label>
                                </div>
                            </g:each>
                        </td>
                        <td >
                            <g:each in="${languages}" var="lang">
                                <div class="lang_description_${lang.id}" style="${lang.id != com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID ? 'display: none;' : ''}">
                                    <g:textField class="inp-bg inp-desc"
                                                 name="obj[${nextIndex}].description_${lang.id}" id="obj_${nextIndex}_description_${lang.id}"/>
                                </div>
                            </g:each>
                        </td>
                        <td >
                            <g:field type="number" class="inp-bg inp4" name="obj[${nextIndex}].order"/>
                        </td>
                        <td >
                            <g:checkBox class="cb checkbox" value="true" name="obj[${nextIndex}].applyToOrder" checked="false"/>

                        </td>
                        <td >
                            <a class="plus-icon" onclick="addOrderChangeStatus()">&#xe026;</a>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <div class="row">&nbsp;</div>
            </div>
        </fieldset>
        <div class="btn-box buttons">
            <ul>
                <li><a onclick="$('#save-orderChangeStatuses-form').submit();" class="submit save button-primary"><span><g:message code="button.save"/></span></a></li>
                <li><g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
            </ul>
        </div>

        <script type="text/javascript">
            $(".numericOnly").keydown(function(event){
                // Allow only backspace, delete, left & right
                if ( event.keyCode==37 || event.keyCode== 39 || event.keyCode == 46 || event.keyCode == 8 || event.keyCode == 9 ) {
                    // let it happen, don't do anything
                }
                else {
                    // Ensure that it is a number and stop the keypress
                    if (event.keyCode < 48 || event.keyCode > 57 ) {
                        event.preventDefault();
                    }
                }
            });

            $(".numericOnly").change(function (event){
                if ($(this).val() > 0 ) {
                    $(this).parent().parent().find(':input[type=checkbox]').attr('checked', false);
                }
            });

            %{--
                Post with ajax adding new OrderChangeStatus to conversation, update existed one in conversation.
                Repaint form on success with html data from response
            --}%
            function addOrderChangeStatus() {
                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'addOrderChangeStatus')}',
                    data: $('#orderChangeStatuses').parents('form').serialize(),
                    success: function(data) {
                        $('#orderChangeStatuses').replaceWith(data);
                        onLanguageChange($('#language_selector'));
                    }
                });
            }

            function removeOrderChangeStatus(stepIndex) {
                $('#stepIndex').val(stepIndex);
                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'removeOrderChangeStatus')}',
                    data: $('#orderChangeStatuses').parents('form').serialize(),
                    success: function(data) {
                        $('#orderChangeStatuses').replaceWith(data);
                        onLanguageChange($('#language_selector'));
                    }
                });
            }
        </script>
</div>
