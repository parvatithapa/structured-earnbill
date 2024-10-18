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

<%@ page import="com.sapienter.jbilling.server.user.db.UserDTO; com.sapienter.jbilling.server.user.UserBL; com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>
<html>
<div class="table-box">
    <table id="users" cellspacing="0" cellpadding="0">
        	<thead>
            	<tr>
                	<th class="first">
                       	 <g:message code="customer.table.th.name"/>
                	</th>
                	<th class="small">
                        <g:message code="customer.table.th.user.id"/>
                	</th>
                	<th class="tiny2">
                        <g:message code="customer.table.th.status"/>
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
                    	    <strong>
                        	    <g:if test="${contactVar?.firstName || contactVar?.lastName}">
                            	    ${contactVar.firstName} ${contactVar.lastName}
                            	</g:if>
                            	<g:else>
                                	${user.userName}
                            	</g:else>
                        	</strong>
                        	<em>${contactVar?.organizationName}</em>
                	</td>
                	<td>
                        	<span>${user.id}</span>
                	</td>
                	<td class="center">
                        	<span>
                            	<g:if test="${user.userStatus.id > 1 && !user.userStatus.isSuspended()}">
                                	<img src="${resource(dir:'images', file:'icon15.gif')}" alt="overdue" />
                            	</g:if>
                            	<g:elseif test="${user.userStatus.id > 1 && user.userStatus.isSuspended()}">
                                	<img src="${resource(dir:'images', file:'icon16.gif')}" alt="suspended" />
                            	</g:elseif>
                        	</span>
                	</td>
                	<td>
                        	<span><g:formatNumber number="${UserBL.getBalance(user.id)}" type="currency"  currencySymbol="${user.currency.symbol}"/></span>
                	</td>
                	<td class="center">
                    	<g:if test="${customerVar}">
                        	<g:if test="${customerVar.isParent == 1 && customerVar.parent}">
                            	<%-- is a parent, but also a child of another account --%>
                            	<g:remoteLink action="subaccounts" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                                	<img src="${resource(dir:'images', file:'icon17.gif')}" alt="parent and child" />
                                	<g:set var="children" value="${customerVar.children.findAll{ it.baseUser.deleted == 0 }}"/>
                                	<span>${children.size()}</span>
                            	</g:remoteLink>
                        	</g:if>
                        	<g:elseif test="${customerVar.isParent == 1 && !customerVar.parent}">
                            	<%-- is a top level parent --%>
                            	<g:remoteLink action="subaccounts" id="${user.id}" before="register(this);" onSuccess="render(data, next);">
                                	<img src="${resource(dir:'images', file:'icon18.gif')}" alt="parent" />
                                	<g:set var="children" value="${customerVar.children.findAll{ it.baseUser.deleted == 0 }}"/>
                                	<span>${children.size()}</span>
                            	</g:remoteLink>
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
