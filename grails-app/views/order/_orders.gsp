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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO"%>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL; com.sapienter.jbilling.common.Constants;"%>
<%-- 
    Orders list template. 
    
    @author Vikas Bodani
    @since 20-Jan-2011
 --%>
<div id="success-message" class="msg-box successfully" style="display: none;">
    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="${message(code:'info.icon.alt',default:'Information')}"/>
     <strong><g:message code="flash.request.sent.title"/></strong>
     <p><g:message code="records.csv.file.generate"/></p>
 </div>
<div class="table-box">
    <div class="table-scroll">
        <table id="orders" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="order.label.id"/>
                        </g:remoteSort>
                    </th>
                    <th class="large header-sortable">
                        <g:remoteSort action="list" sort="u.userName" update="column1">
                            <g:message code="order.label.customer"/>
                        </g:remoteSort>
                    </th>
                    <g:isRoot>
                		<th class="small header-sortable">
                			<g:remoteSort action="list" sort="company.description" alias="[company: 'baseUserByUserId.company']" update="column1">
                    	    	<g:message code="order.label.company"/>
                    		</g:remoteSort>
                		</th>
                	</g:isRoot>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="createDate" update="column1">
                            <g:message code="order.label.date"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="activeSince" update="column1">
                            <g:message code="order.label.active.since"/>
                        </g:remoteSort>
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="activeUntil" update="column1">
                            <g:message code="order.label.active.until"/>
                        </g:remoteSort>
                    </th>
                    <th class="small">
                        <g:message code="order.label.amount"/>
                    </th>
                    <th class="tiny3">
                        <g:message code="order.label.parent.child"/>
                    </th>
                </tr>
            </thead>
            <tbody>
                <g:each var="ordr" in="${orders}">
                    <g:set var="contact" value="${ContactDTO.findByUserId(ordr?.baseUserByUserId?.id)}"/>
                    <tr id="order-${ordr.id}" class="${(order?.id == ordr?.id) ? 'active' : ''}">
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                ${ordr.id}
                            </jB:secRemoteLink>
                        </td>
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="double cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <strong>
                                    <g:if test="${contact?.firstName || contact?.lastName}">
                                        ${StringEscapeUtils.escapeHtml(contact?.firstName)} &nbsp;${StringEscapeUtils.escapeHtml(contact?.lastName)}
                                    </g:if> 
                                    <g:else>
                                        ${StringEscapeUtils.escapeHtml(displayer?.getDisplayName(ordr?.baseUserByUserId))}
                                    </g:else>
                                </strong>
                                <em>${StringEscapeUtils.escapeHtml(contact?.organizationName)}</em>
                            </jB:secRemoteLink>
                        </td>
                        <g:isRoot>
                        	<td>
                				<jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                	<strong>${StringEscapeUtils.escapeHtml(ordr?.baseUserByUserId?.company?.description)}</strong>
                            	</jB:secRemoteLink>
                			</td>
                		</g:isRoot>
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${ordr?.createDate}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/>
                            </jB:secRemoteLink>
                        </td>
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${ordr?.activeSince}" formatName="date.pretty.format"/>
                            </jB:secRemoteLink>
                        </td>
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${ordr?.activeUntil}" formatName="date.pretty.format"/>
                            </jB:secRemoteLink>
                        </td>
                        <td>
                            <jB:secRemoteLink breadcrumb="id" class="cell" action="show" id="${ordr.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatNumber number="${ordr?.total}" type="currency" currencySymbol="${ordr?.currency?.symbol}"/>
                            </jB:secRemoteLink>
                        </td>
                        <td class="center">
                            <g:if test="${ordr}">
                                <g:set var="childrenSize" value="${children[ordr.id] != null ? children[ordr.id] : 0}"/>
                                <g:set var="isParent" value="${childrenSize > 0}"/>
                                <g:if test="${isParent && ordr.parentOrder}">
                                    <%-- is a parent, but also a child of another order --%>
                                    <g:remoteLink action="suborders" id="${ordr.id}" before="register(this);" onSuccess="render(data, next);">
                                        <img src="${resource(dir:'images', file:'icon17.gif')}" alt="parent and child" />
                                        <span>${childrenSize}</span>
                                    </g:remoteLink>
                                </g:if>
                                <g:elseif test="${isParent && !ordr.parentOrder}">
                                    <%-- is a top level parent --%>
                                    <g:remoteLink action="suborders" id="${ordr.id}" before="register(this);" onSuccess="render(data, next);">
                                        <img src="${resource(dir:'images', file:'icon18.gif')}" alt="parent" />
                                        <span>${childrenSize}</span>
                                    </g:remoteLink>
                                </g:elseif>
                                <g:elseif test="${!isParent && ordr.parentOrder}">
                                    <%-- is a child order, but not a parent --%>
                                    <img src="${resource(dir:'images', file:'icon19.gif')}" alt="child" />
                                </g:elseif>
                            </g:if>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
     </div>
</div>

<g:if test="${!parent}">
<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1', ids: ids, id: params?.id, action: list]"/>
        </div>
        
        
        <g:set var="CSV_Condition" value="${PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0}"/>
  		<g:if test="${CSV_Condition}">
        <div class="pager-button" id="generateCsv" style="text-align : right;">
            <a onclick = "generateCSV(); showMessage()"><g:message code="generate.csv.link"/></a>
        </div>
       	</g:if>
       	 <g:else>
       	 <div class="download">
            <sec:access url="/order/csv">
                <g:link class="pager-button" action="csv" id="${order?.id}" params="[processId: params?.processId]">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </div>
        </g:else>

    </div>
    <jB:isPaginationAvailable total="${orders?.totalCount ?: 0}">
        <div class="row-center">
                <jB:remotePaginate controller="order" action="list" id="${params?.id}" params="${sortableParams(params: [partial: true])}" total="${orders?.totalCount ?: 0}" update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>
<div class="btn-box">
    <div class="row">
        <sec:ifAllGranted roles="ORDER_200,ROLE_CUSTOMER">
            <g:link controller="orderBuilder" action="edit" params="[userId: session['user_id']]" class="submit order"><span><g:message code="button.create.order"/></span></g:link>
        </sec:ifAllGranted>
    </div>
</div>
</g:if>
<script type="text/javascript">

function showMessage() {
	$("#success-message").css("display","block");
}

function generateCSV() {
    $.ajax({
        type: 'POST',
        url: '${createLink(action: 'csv', params:[processId:params?.processId])}',
        data: $('#generateCsv').parents('form').serialize(),
        error: function(data) {}
    });
}
</script>
