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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.UserBL; com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL; com.sapienter.jbilling.common.Constants;"%>
<%--
  Customer table template. The customer table is used multiple times for rendering the
  main list and for rendering a separate list of sub-accounts. 

  @author Brian Cowdery
  @since  24-Nov-2010
--%>
<div id="success-message" class="msg-box successfully" style="display: none;">
    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="${message(code:'info.icon.alt',default:'Information')}"/>
     <strong><g:message code="flash.request.sent.title"/></strong>
     <p><g:message code="records.csv.file.generate"/></p>
 </div>
<div class="table-box">
    <table id="users" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th class="header-sortable first">
                    <g:remoteSort action="list" sort="userName" update="column1">
                        <g:message code="customer.table.th.name"/>
                    </g:remoteSort>
                </th>
                <g:isRoot>
                	<th class="header-sortable">
                		<g:remoteSort action="list" sort="company.description" update="column1" alias="[company: 'company']">
                    	    <g:message code="customer.table.th.user.company.name"/>
                    	</g:remoteSort>
                	</th>
                </g:isRoot>
                <th class="small header-sortable">
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="customer.table.th.user.id"/>
                    </g:remoteSort>
                </th>
                <th class="tiny2 header-sortable">
                    <g:remoteSort action="list" sort="userStatus.id" update="column1">
                        <g:message code="customer.table.th.status"/>
                    </g:remoteSort>
                </th>
                <th class="small">
                    <g:message code="customer.table.th.balance"/>
                </th>
                <th class="tiny3 last">
                    <g:message code="customer.table.th.hierarchy"/>
                </th>
            </tr>
        </thead>

        <tbody>
        <g:each in="${users}" var="user">
            <g:set var="customerVar" value="${user.customer}"/>
            <g:set var="contactVar" value="${ContactDTO.findByUserId(user.id)}"/>

            <tr id="user-${user.id}" class="${selected?.id == user.id ? 'active' : ''}">
                <td>
                    <jB:secRemoteLink class="cell double" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                        <strong>
                            <g:if test="${contactVar?.firstName || contactVar?.lastName}">
                                ${StringEscapeUtils.escapeHtml(contactVar?.firstName)} ${StringEscapeUtils.escapeHtml(contactVar?.lastName)}
                            </g:if>
                            <g:else>
                                ${StringEscapeUtils.escapeHtml(displayer?.getDisplayName(user))}
                            </g:else>
                        </strong>
                        <em>${StringEscapeUtils.escapeHtml(contactVar?.organizationName)}</em>
                    </jB:secRemoteLink>
                </td>
                <g:isRoot>
                	<td>
                    	<jB:secRemoteLink class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                        	<strong>${StringEscapeUtils.escapeHtml(user?.company.description)}</strong>
                   		</jB:secRemoteLink>
                	</td>
                </g:isRoot>
                <td>
                    <jB:secRemoteLink class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                        <span>${user.id}</span>
                    </jB:secRemoteLink>
                </td>
                <td class="center">
                    <jB:secRemoteLink class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                        <span>                        	
                             <g:if test="${user.deleted}">
                             	<img src="${resource(dir:'images', file:'cross.png')}" alt="deleted" />
                             </g:if>        
                            <g:elseif test="${user.userStatus.id == 1}">
                                <g:message code="customer.status.active"/>
                            </g:elseif>                       
                            <g:elseif test="${user.userStatus.id > 1 && !user.userStatus.isSuspended()}">
                                <img src="${resource(dir:'images', file:'icon15.gif')}" alt="overdue" />
                            </g:elseif>
                            <g:elseif test="${user.userStatus.id > 1 && user.userStatus.isSuspended()}">
                                <img src="${resource(dir:'images', file:'icon16.gif')}" alt="suspended" />
                            </g:elseif>
                        </span>
                    </jB:secRemoteLink>
                </td>
                <td>
                    <jB:secRemoteLink class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                        <span><g:formatNumber number="${UserBL.getBalance(user.id)}" type="currency"  currencySymbol="${user.currency.symbol}"/></span>
                    </jB:secRemoteLink>
                </td>
                <td class="center">
                    <g:if test="${customerVar}">
                        <g:if test="${customerVar.isParent == 1 && customerVar.parent}">
                            <%-- is a parent, but also a child of another account --%>
                            <jB:secRemoteLink permissions="CUSTOMER_18" action="subaccounts" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                                <img src="${resource(dir:'images', file:'icon17.gif')}" alt="parent and child" />
                                <g:set var="children" value="${customerVar.children.findAll{ it.baseUser.deleted == 0 }}"/>
                                <span>${children.size()}</span>
                            </jB:secRemoteLink>
                        </g:if>
                        <g:elseif test="${customerVar.isParent == 1 && !customerVar.parent}">
                            <%-- is a top level parent --%>
                            <jB:secRemoteLink permissions="CUSTOMER_18" action="subaccounts" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                                <img src="${resource(dir:'images', file:'icon18.gif')}" alt="parent" />
                                <g:set var="children" value="${customerVar.children.findAll{ it.baseUser.deleted == 0 }}"/>
                                <span>${children.size()}</span>
                            </jB:secRemoteLink>
                        </g:elseif>
                        <g:elseif test="${customerVar.isParent == 0 && customerVar.parent}">
                            <%-- is a child account, but not a parent --%>
                            <img src="${resource(dir:'images', file:'icon19.gif')}" alt="child" />
                        </g:elseif>
                    </g:if>
                </td>
            </tr>

        </g:each>
        </tbody>
    </table>
</div>


<div class="pager-box">
<g:set var="CSV_CONDITION" value="${PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0}"/>

    %{-- remote pager does not support "onSuccess" for panel rendering, take a guess at the update column --}%
    <g:set var="action" value="${actionName == 'subaccounts' ? 'subaccounts' : 'list'}"/>
    <g:set var="csvAction" value="${actionName == 'subaccounts' ? 'subaccountsCsv' : 'csv'}"/>
    <g:set var="id" value="${actionName == 'subaccounts' ? parent.id : null}"/>
    <g:set var="updateColumn" value="${actionName == 'subaccounts' ? 'column2' : 'column1'}"/>

    <div class="row">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults"
                          model="[steps: [10, 20, 50], update: updateColumn, contactFieldTypes: contactFieldTypes]"/>
            </div>
            <g:if test="${CSV_CONDITION}">
                <div class="generate" id="generateCsv" style="text-align : right;">
                    <a class="pager-button" onclick="generateCSV();
                    showMessage()"><g:message code="generate.csv.link"/></a>
                </div>
            </g:if>
            <g:else>
                <div class="download">
                    <sec:access url="/customer/csv">
                        <g:link action="${csvAction}" id="${id}" class="pager-button"
                                params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}">
                            <g:message code="download.csv.link"/>
                        </g:link>
                    </sec:access>
                </div>
            </g:else>
    </div>
    <jB:isPaginationAvailable total="${users?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="customer" action="${action ?: 'list'}" id="${id}"
                 params="${sortableParams(params: [partial: true, contactFieldTypes: contactFieldTypes])}"
                 total="${users?.totalCount ?: 0}" update="${updateColumn}"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<div class="btn-box">
    <sec:ifAllGranted roles="CUSTOMER_10">
        <g:if test="${parent?.customer?.isParent > 0}">
            <sec:ifAnyGranted roles="CUSTOMER_17, CUSTOMER_18">
                <g:link action="edit" params="[parentId: parent.id]" class="submit add"><span><g:message code="customer.add.subaccount.button"/></span></g:link>
            </sec:ifAnyGranted>
        </g:if>
        <g:else>
            <g:link action='edit' class="submit add button-primary"><span><g:message code="button.create"/></span></g:link>
        </g:else>
    </sec:ifAllGranted>
</div>

<script type="text/javascript">

function showMessage() {
	$("#success-message").css("display","block");
}

function generateCSV() {
    $.ajax({
        type: 'POST',
        url: '${createLink(action: 'csv')}',
        data: $('#generateCsv').parents('form').serialize(),
        error: function(data) {}
    });
}
</script>
