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
  Shows a list of contact types.

  @author Brian Cowdery
  @since  27-Jan-2011
--%>
<div id="rate-box">
<div class="table-box">
    <table id="rateCards" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th><g:message code="rate.card.name"/></th>
                <g:isRoot>
                	<th class="medium"><g:message code="product.label.company.name"/></th>
                </g:isRoot>
                <th class="medium"><g:message code="rate.card.table.name"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="card" in="${cards}">
                <tr id="type-${card.id}" class="${selected?.id == card.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${card.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(card?.name)}</strong>
                            <em><g:message code="table.id.format" args="[card.id as String]"/></em>
                        </g:remoteLink>
                    </td>
					<g:isRoot>
                        <td class="medium">
                        	<%
							def totalChilds = card?.childCompanies?.size()
							def multiple = false
                            if(totalChilds > 1) {
								multiple = true
							}
							%>
                            <g:remoteLink class="cell" action="show" id="${card.id}" before="register(this);" onSuccess="render(data, next);">
                                <g:if test="${card?.global}">
                                	<strong><g:message code="product.label.company.global"/></strong>
                                </g:if>
                                <g:elseif test="${multiple}">
                                	<strong><g:message code="product.label.company.multiple"/></strong>
                                </g:elseif>
                                <g:elseif test="${totalChilds==1}">
                                	<strong>${StringEscapeUtils.escapeHtml(card?.childCompanies?.toArray()[0]?.description)}</strong>
                                </g:elseif>
                                <g:else>
                                    <strong>-</strong>
                                </g:else>
                            </g:remoteLink>
                        </td>
                        </g:isRoot>
                    <td class="small">
                        <g:remoteLink class="cell" action="show" id="${card.id}" before="register(this);" onSuccess="render(data, next);">
                            ${StringEscapeUtils.escapeHtml(card?.tableName)}
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>
%{--<g:set var="updateColumn" value="${actionName == 'list' ? 'column1' : 'column2'}"/>--}%
	<div class="pager-box">
		<div class="row">
			<div class="results">
				<g:render template="/layouts/includes/pagerShowResults"
					model="[steps: [10, 20, 50], update: 'column1' ]"  />
			</div>
		</div>
		<div class="row-center">
			<jB:remotePaginate controller="rateCard" action="list"
				params="${sortableParams(params: [partial: true])}"
				total="${cards?.totalCount ?: 0}" update="column1" />
		</div>
	</div>
	<div class="btn-box">
    <g:remoteLink action='edit' class="submit add button-primary" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
</div>
