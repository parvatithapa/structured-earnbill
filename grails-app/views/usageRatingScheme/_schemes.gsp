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
    <table id="ratingSchemes" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="medium"><g:message code="usageRatingScheme.scheme.code"/></th>
            <th class="medium"><g:message code="usageRatingScheme.scheme.type"/></th>

        </tr>
        </thead>

        <tbody>
        <g:each var="ratingScheme" in="${ratingSchemes}">

            <tr id="ratingInit-${ratingScheme.id}" class="${selected?.id == ratingScheme.id ? 'active' : ''}">
                <td>
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong>${ratingScheme?.ratingSchemeCode}</strong>
                        <em><g:message code="table.id.format" args="[ratingScheme.id]"/></em>
                    </g:remoteLink>
                </td>


                <td>
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong>${ratingScheme?.ratingSchemeType}</strong>

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
        <jB:remotePaginate controller="usageRatingScheme" action="${action ?: 'list'}"
                             params="${sortableParams(params: [partial: true])}"
                             total="${size ?: 0}" update="column1"/>
    </div>
</div>
<div class="btn-box">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
