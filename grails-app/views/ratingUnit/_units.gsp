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

<div class="table-box">
    <table id="ratingInits" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th class="medium"><g:message code="ratingUnit.name"/></th>
                <th class="medium"><g:message code="ratingUnit.priceUnitName"/></th>
                <th class="large"><g:message code="ratingUnit.incrementUnitName"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="ratingInit" in="${ratingUnits}">

                <tr id="ratingInit-${ratingInit.id}" class="${selected?.id == ratingInit.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${ratingInit.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(ratingInit?.name)}</strong>
                            <em><g:message code="table.id.format" args="[ratingInit.id]"/></em>
                        </g:remoteLink>
                    </td>

                    <td>
                        <g:remoteLink class="cell double" action="show" id="${ratingInit.id}" before="register(this);" onSuccess="render(data, next);">
                            ${StringEscapeUtils.escapeHtml(ratingInit?.priceUnit?.name)}
                        </g:remoteLink>
                    </td>

                    <td>
                        <g:remoteLink class="cell double" action="show" id="${ratingInit.id}" before="register(this);" onSuccess="render(data, next);">
                            ${StringEscapeUtils.escapeHtml(ratingInit?.incrementUnit?.name)}
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
        <jB:remotePaginate controller="ratingUnit" action="${action ?: 'list'}"
                             params="${sortableParams(params: [partial: true])}"
                             total="${ratingUnits?.totalCount ?: 0}" update="column1"/>
    </div>
</div>
<div class="btn-box">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
