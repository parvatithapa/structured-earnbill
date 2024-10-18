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

<%@ page contentType="text/html;charset=UTF-8" %>

<div class="table-box">
    <table id="apiUserDetails" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="medium"><g:message code="apiUserDetails.user.name"/></th>
            <th class="medium"><g:message code="apiUserDetails.access.code"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="apiUserDetail" in="${apiUserDetails}">
            <tr id="apiUser-${apiUserDetail.accessCode}">
                <td> <strong>${apiUserDetail?.userName}</strong> </td>
                <td> <strong>${apiUserDetail?.accessCode}</strong> </td>
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
        <jB:remotePaginate controller="apiUserDetail" action="${action ?: 'list'}"
                      params="${sortableParams(params: [partial: true])}"
                      total="${size ?: 0}" update="column1"/>
    </div>
</div>
<div class="btn-box">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
