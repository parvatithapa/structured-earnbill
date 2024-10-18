%{--
-  JBILLING CONFIDENTIAL
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

<%@page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>
<%@page import="com.sapienter.jbilling.server.order.OrderStatusFlag" %>
<%@page import="com.sapienter.jbilling.client.util.Constants" %>



<%@ page contentType="text/html;charset=UTF-8" %>

<%--

  @author Neeraj Bhatt

--%>

<div class="table-box" id="ediStatuses">
    <table id="orderStatuses" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="tiny first"><g:remoteSort action="list" sort="id" searchParams="[typeId:params.typeId]" update="ediStatuses"><g:message code="ediStatus.id"/></g:remoteSort></th>
            <th class="large last"><g:remoteSort action="list" sort="name" searchParams="[typeId:params.typeId]" update="ediStatuses"> <g:message code="ediStatus.name"/></g:remoteSort></th>

        </tr>
        </thead>


        <tbody>
        <g:each var="ediStatus" in="${ediStatusList}">
            <tr id="orderStatus-${ediStatus?.id}" class="${selected?.id == ediStatus?.id ? 'active' : ''}">

                <td>
                    <g:remoteLink class="cell double" action="show" id="${ediStatus?.id}" before="register(this);" params="[typeId:params.typeId]" onSuccess="render(data, next);">
                        ${ediStatus?.id}
                    </g:remoteLink>
                </td>
                <td>
                    <g:remoteLink class="cell double" action="show" id="${ediStatus?.id}" before="register(this);" params="[typeId:params.typeId]" onSuccess="render(data, next);">
                        ${ediStatus?.name}
                    </g:remoteLink>
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>
    <div class="pager-box">

        <div class="row">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'ediStatuses', action:'list', extraParams:[typeId:params.typeId]]"  />
            </div>

        </div>

        <div class="row">
            <jB:remotePaginate  action="list" total="${ediStatusList?.totalCount ?: 0}" update="ediStatuses" params="[partial:true,  action:'list', typeId:params.typeId]"/>
        </div>
    </div>
    <sec:ifAllGranted roles="EDI_923">
        <div class="btn-box">
            <g:remoteLink class="submit add" action="edit" before="register(this);" onSuccess="render(data, next);" params="[typeId:params.typeId]">
                <span><g:message code="button.create"/></span>
            </g:remoteLink>
        </div>
    </sec:ifAllGranted>
</div>
