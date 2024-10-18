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

  @author Aamir Ali
  @since  02/21/2017
--%>

<%@ page import="org.apache.commons.lang.StringEscapeUtils" contentType="text/html;charset=UTF-8" %>


<div class="table-box">
    <table id="periods" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="medium"><g:message code="company.information.type.title"/></th>

        </thead>

        <tbody>
        <g:each var="cit" in="${cits}">

            <tr id="period-${cit.id}" class="${selected?.id == cit?.id ? 'active' : ''}">
                <!-- ID -->
                <td>
                    <g:remoteLink class="cell double" action="showCIT" id="${cit.id}"
                                  params="[companyId: company?.id, template: 'showCIT']" before="register(this);"
                                  onSuccess="render(data, next);">

                        <strong>${StringEscapeUtils.escapeHtml(cit?.name)}</strong>
                        <em><g:message code="table.id.format" args="[cit.id]"/></em>
                    </g:remoteLink>
                </td>

            </tr>

        </g:each>
        </tbody>
    </table>
</div>

<div class="btn-box">
    <a href="${createLink(action: 'editCIT', params:[companyId : company?.id])}" class="submit add">
        <span><g:message code="button.create"/></span
        ></a>
</div>
