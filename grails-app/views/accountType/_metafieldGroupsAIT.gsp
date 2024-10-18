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
  Shows list of metafield groups available as a templates for creation of an account information type

  @author Panche Isajeski
  @since 05/24/2013
--%>

<div id="metafield-group-box">

    <div class="table-box tab-table">
        <div class="table-scroll">
            <table id="metafieldGroups" cellspacing="0" cellpadding="0">
                <tbody>

                <g:each var="mfGroup" in="${metaFieldGroups}">
                    <tr>
                        <td>
                            <g:remoteLink class="cell double" action="editAIT" id="${mfGroup.id}" params="[_eventId: 'importMetaFieldGroup']" update="column2" method="GET">
                                <strong>${mfGroup.id}</strong>
                                <em><g:message code="table.id.format" args="[mfGroup.id as String]"/></em>
                            </g:remoteLink>
                        </td>
                        <td class="small">
                            <g:remoteLink class="cell double" action="editAIT" id="${mfGroup.id}" params="[_eventId: 'importMetaFieldGroup']" update="column2" method="GET">
                                <span>${StringEscapeUtils.escapeHtml(mfGroup?.getDescription())}</span>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>

                </tbody>
            </table>
        </div>
    </div>

</div>