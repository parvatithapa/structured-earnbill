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
  Shows a list of usage pools.

  @author Amol Gadre
  @since  14-Nov-2013
--%>

<div class="table-box">
    <table id="usagePools" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th><g:message code="usagePool.th.name"/></th>
                <th><g:message code="usagePool.th.quantity"/></th>
                <th><g:message code="usagePool.th.billingCycle"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="usagePool" in="${usagePools}">

                <tr id="usagePool-${usagePool.id}" class="${selected?.id == usagePool.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${usagePool.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(usagePool?.getDescription(session['language_id'], 'name'))}</strong>
                            <em><g:message code="table.id.format" args="[usagePool.id as String]"/></em>
                        </g:remoteLink>
                    </td>
                    
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${usagePool.id}" before="register(this);" onSuccess="render(data, next);">
                            ${usagePool?.quantity}
                        </g:remoteLink>
                    </td>
                    
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${usagePool.id}" before="register(this);" onSuccess="render(data, next);">
                            ${usagePool.cyclePeriodValue}
                            ${StringEscapeUtils.escapeHtml(usagePool?.cyclePeriodUnit)}
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
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate controller="usagePool" action="list" params="${sortableParams(params: [partial: true])}" total="${usagePools?.totalCount ?: 0}" update="column1"/>
    </div>
</div>

<div class="btn-box">
    <g:link action="edit" class="submit add button-primary">
        <span><g:message code="button.create"/></span>
    </g:link>
</div>
