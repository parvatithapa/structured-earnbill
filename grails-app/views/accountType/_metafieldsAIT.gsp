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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.ItemTypeDTO" %>

<%--
  Shows list of metafields available as a templates
  for creation of an account information type fields

  @author Panche Isajeski
  @since 05/24/2013
--%>

<div id="metafield-box">

    <div class="table-box tab-table">
        <div class="table-scroll">
            <table id="metafields" cellspacing="0" cellpadding="0">
                <tbody>

                <g:each var="metaField" in="${metaFields}">
                    <tr>
                        <td>
                            <g:remoteLink class="cell double" action="editAIT" id="${metaField.id}" params="[_eventId: 'addMetaField']" update="column2" method="GET"
                                          onSuccess="changeMetafieldDataType()">
                                <span>${StringEscapeUtils.escapeHtml(metaField?.name)}</span>
                                <em><g:message code="table.id.format" args="[metaField.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <td class="small">
                            <g:remoteLink class="cell double" action="editAIT" id="${metaField.id}" params="[_eventId: 'addMetaField']" update="column2" method="GET"
                                          onSuccess="changeMetafieldDataType()">
                                <span>${StringEscapeUtils.escapeHtml(metaField?.dataType?.toString())}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell double" action="editAIT" id="${metaField.id}"
                                          params="[_eventId: 'addMetaField']" update="column2" method="GET" onSuccess="changeMetafieldDataType()">
                                <span>${metaField.mandatory ? "Mandatory" : "Not Mandatory"}</span>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
    </div>

</div>