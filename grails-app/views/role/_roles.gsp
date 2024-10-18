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

<%@ page import="org.apache.commons.lang.StringEscapeUtils" contentType="text/html;charset=UTF-8" %>

<%--
  Shows a list of user roles.

  @author Brian Cowdery
  @since  02-Jun-2011
--%>

<div class="table-box">
    <table id="roles" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th><g:message code="role.th.name"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="role" in="${roles}">

                <tr id="role-${role.id}" class="${selected?.id == role.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${role.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(role?.getTitle(session['language_id']))}</strong>
                            <em><g:message code="table.id.format" args="[role.id as String]"/></em>
                        </g:remoteLink>
                    </td>
                </tr>

            </g:each>
        </tbody>
    </table>
</div>
<div class="pager-box">
  <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                      model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
    </div>

    <div class="row">
        <jB:remotePaginate controller="role" action="${action ?: 'list'}"
                             params="${sortableParams(params: [partial: true])}"
                             total="${roles?.totalCount ?: 0}" update="column1"/>
    </div>
</div>
<sec:ifAllGranted roles="CONFIGURATION_1903">
    <div class="btn-box">
        <g:link action="edit" class="submit add button-primary">
            <span><g:message code="button.create"/></span>
        </g:link>
    </div>
</sec:ifAllGranted>
