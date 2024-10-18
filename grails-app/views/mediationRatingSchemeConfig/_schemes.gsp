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
            <th class="medium"><g:message code="ratingScheme.name"/></th>
            <th class="medium"><g:message code="ratingScheme.initial.increment"/></th>
            <th class="large"><g:message code="ratingScheme.main.increment"/></th>
            <th class="large"><g:message code="ratingScheme.mediation.assign.global"/></th>
        </tr>
        </thead>

        <tbody>
        <g:each var="ratingScheme" in="${ratingSchemes}">

            <tr id="ratingInit-${ratingScheme.id}" class="${selected?.id == ratingScheme.id ? 'active' : ''}">
                <td>
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong>${ratingScheme?.name}</strong>
                        <em><g:message code="table.id.format" args="[ratingScheme.id]"/></em>
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong> ${ratingScheme?.initialIncrement}  </strong>
                        <g:if test="${ratingScheme?.initialRoundingMode==1}">
                            <g:set var="initialRoundingMode" value="ROUND DOWN"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.initialRoundingMode==0}">
                            <g:set var="initialRoundingMode" value="ROUND UP"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.initialRoundingMode==4}">
                            <g:set var="initialRoundingMode" value="HALF ROUND UP"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.initialRoundingMode==5}">
                            <g:set var="initialRoundingMode" value="HALF ROUND DOWN"></g:set>
                        </g:if>
                        <em><g:message code="table.rounding.mode.format" args="[initialRoundingMode]"/></em>
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong> ${ratingScheme?.mainIncrement}  </strong>
                        <g:if test="${ratingScheme?.mainRoundingMode==1}">
                            <g:set var="mainRoundingMode" value="ROUND DOWN"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.mainRoundingMode==0}">
                            <g:set var="mainRoundingMode" value="ROUND UP"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.mainRoundingMode==4}">
                            <g:set var="mainRoundingMode" value="HALF ROUND UP"></g:set>
                        </g:if>
                        <g:if test="${ratingScheme?.mainRoundingMode==5}">
                            <g:set var="mainRoundingMode" value="HALF ROUND DOWN"></g:set>
                        </g:if>
                        <em><g:message code="table.rounding.mode.format" args="[mainRoundingMode]"/></em>
                    </g:remoteLink>
                </td>

                <td style="width: 10%;">
                    <g:remoteLink class="cell double" action="show" id="${ratingScheme.id}" before="register(this);" onSuccess="render(data, next);">
                        <g:if test="${ratingScheme?.global}">
                            <g:checkBox name="global" class="cb checkbox" checked="true" disabled="true" />
                        </g:if>
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
        <jB:remotePaginate controller="mediationRatingSchemeConfig" action="${action ?: 'list'}"
                             params="${sortableParams(params: [partial: true])}"
                             total="${size ?: 0}" update="column1"/>
    </div>
</div>
<div class="btn-box">
    <g:remoteLink class="submit add button-primary" action="edit" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
