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

  @author Panche Isajeski
  @since  05/24/2013
--%>

<%@ page import="org.apache.commons.lang.StringEscapeUtils" contentType="text/html;charset=UTF-8" %>


<div class="table-box">
    <table id="periods" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
        <th class="medium"><g:message code="account.information.type.title"/></th>

        </thead>

        <tbody>
        <g:each var="ait" in="${aits}">

            <tr id="period-${ait.id}" class="${selected?.id == ait?.id ? 'active' : ''}">
                <!-- ID -->
                <td>
                    <g:remoteLink class="cell double" action="showAIT" id="${ait.id}"
                                  params="[accountTypeId: accountType?.id, template: 'showAIT']" before="register(this);"
                                  onSuccess="render(data, next);">

                        <strong>${StringEscapeUtils.escapeHtml(ait?.name)}</strong>
                        <em><g:message code="table.id.format" args="[ait.id]"/></em>
                    </g:remoteLink>
                </td>

            </tr>

        </g:each>
        </tbody>
    </table>
</div>

<div class="btn-box table-box-below">
    <a href="${createLink(action: 'editAIT', params:[accountTypeId : accountType?.id])}" class="submit add button-primary">
        <span><g:message code="button.create"/></span
    ></a>
</div>
