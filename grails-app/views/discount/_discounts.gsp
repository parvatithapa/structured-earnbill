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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO" contentType="text/html;charset=UTF-8" %>

<%--
  Shows a list of discounts.

  @author Amol Gadre
  @since  27-Nov-2012
--%>

<div class="table-box">
    <table id="discounts" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th class="first header-sortable">
                	<g:remoteSort action="list" sort="code" update="column1" >
                		<g:message code="discounts.th.code"/>
                	</g:remoteSort>
                </th>
                <th><g:message code="discounts.th.description"/></th>
                <th class="last header-sortable">
                	<g:remoteSort action="list" sort="type" update="column1" >
                		<g:message code="discounts.th.type"/>
                	</g:remoteSort>
                </th>
            </tr>
        </thead>
        <tbody>
            <g:each var="discount" in="${discounts}">

                <tr id="discount-${discount.id}" class="${selected?.id == discount.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${discount.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(discount?.code)}</strong>
                            <em><g:message code="table.id.format" args="[discount.id as String]"/></em>
                        </g:remoteLink>
                    </td>

                    <td>
                        <g:remoteLink class="cell" action="show" id="${discount.id}" before="register(this);" onSuccess="render(data, next);">
                            ${StringEscapeUtils.escapeHtml(discount?.getDescription(session['language_id']))}
                        </g:remoteLink>
                    </td>

                    <td>
                        <g:remoteLink class="cell" action="show" id="${discount.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:message code="${StringEscapeUtils.escapeHtml(discount?.type?.messageKey)}"/>
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

    <jB:isPaginationAvailable total="${discounts?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="discount" action="list" params="${sortableParams(params: [partial: true])}" total="${discounts?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<div class="btn-box">
    <sec:ifAllGranted roles="DISCOUNT_151">
	    <g:link action="edit" class="submit add button-primary">
		<span><g:message code="button.create"/></span>
	    </g:link>
    </sec:ifAllGranted>
</div>
