%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%
<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.Required" %>

<div class="heading">
    <strong><g:message code="route.matching.fields.title"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <div id="users-contain">
            <g:if test="${matchingFields}">
                <div class="table-box">
                    <table id="users" cellspacing="0" cellpadding="0">
                        <thead>
                        <tr class="ui-widget-header" >
                        <th style="width:20%" ><g:message code="bean.MatchingFieldWS.name"/></th>
                        <th style="width:10%"><g:message code="bean.MatchingFieldWS.order"/></th>
                        <th style="width:10%"><g:message code="bean.MatchingFieldWS.type"/></th>
                        <th style="width:15%"><g:message code="bean.MatchingFieldWS.required"/></th>
                        <th style="width:25%"><g:message code="bean.MatchingFieldWS.mediationField"/></th>
                        <th style="width:25%"><g:message code="bean.MatchingFieldWS.matchingField"/></th>
                        </thead>
                        <tbody>
                        <g:hiddenField name="newNotesTotal" id="newNotesTotal" />
                        <g:each in="${matchingFields}">
                            <tr id="${it.id}"  class="delete-matching-${it.id}" onclick="$('#id').val('${it.id}')">
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${StringEscapeUtils.escapeHtml(it?.description)}</g:remoteLink></td>
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${it?.orderSequence}</g:remoteLink></td>
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${it?.type}</g:remoteLink></td>
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${Required.values().find{val-> val.id == it?.required}.name}</g:remoteLink></td>
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${StringEscapeUtils.escapeHtml(it?.mediationField)}</g:remoteLink></td>
                                <td><g:remoteLink class="cell double" action="editDeleteMatchingField"  params="[matchingFieldId:it.id,routeId:selected?.id]" id="${it.id}" update="edit-delete-matchingField">${StringEscapeUtils.escapeHtml(it?.matchingField)}</g:remoteLink></td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </g:if>
            <g:else>
                <p><em><g:message code="matching.field.empty.message"/></em></p>
            </g:else>
        </div>
    </div>

    <div class="btn-box buttons">
        <div class="edit-delete-matchingField" id="edit-delete-matchingField">
            <g:remoteLink action="editMatchingField" update="matching-field-form-holder"
                          class="submit add" params="[routeId : route.id]">
                <g:message code="button.add"/>
            </g:remoteLink>
        </div>
    </div>

</div>
