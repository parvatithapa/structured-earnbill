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

<%@ page import="groovy.json.JsonOutput;com.sapienter.jbilling.server.timezone.TimezoneHelper; com.sapienter.jbilling.server.integration.db.OutBoundInterchange;" %>
<%--
  Shows details of a selected outboundInterChange.

  @author Satyendra Soni
  @since 16-May-2019
--%>
<g:set var="outboundInterChange" value="${OutBoundInterchange.findById(selected?.id)}"/>
<g:set var="JsonOutput" value="${JsonOutput}"/>
<div class="column-hold">
    <div class="heading">
        <strong>
                <g:message code="outboundInterChange.outbound.title"/>
            <em>${selected.id}</em>
        </strong>
    </div>
    <!-- OutBound  details -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="outboundInterChange.id.title"/></td>
                    <td class="value">${selected.id}</td>
                </tr>
                <tr>
                    <td><g:message code="outboundInterChange.userId.title"/></td>
                    <td class="value">${selected.userId}
                    <sec:access url="/outboundInterChange/list">
                        <g:link controller="outboundInterChange" action="user" id="${selected.userId}">
                            <g:message code="outboundInterchange.show.all.outbounds"/>
                        </g:link>
                    </sec:access>
                </td>
                </tr>
                <tr>
                    <td><g:message code="outboundInterChange.requestedDate.title"/></td>
                    <td class="value"><g:formatDate date="${selected.createDateTime}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/> </td>
                </tr>

                <tr>
                    <td><g:message code="outBoundInterchange.methodName.title"/></td>
                    <td class="value">${selected.methodName}</td>
                </tr>
                <tr>
                    <td><g:message code="outboundInterChange.status.title"/></td>
                    <td class="value">${selected.status}</td>
                </tr>
                <tr>
                    <td><g:message code="outBoundInterchange.retryCount.title"/></td>
                    <td class="value">${selected?.retryCount}</td>
                </tr>
                 <tr>
                    <td><g:message code="outBoundInterchange.lastRetryDateTime.title"/></td>
                     <td class="value"><g:formatDate date="${selected.lastRetryDateTime}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/> </td>
                </tr>
                <tr >
                    <td><g:message code="outboundInterChange.request.title"/></td>
                    <td class="value"><textarea id="json-area" rows="12" cols="45" readonly="readonly" wrap='off' >${JsonOutput.prettyPrint(selected.request)}</textarea></td>
                </tr>
                <tr>
                    <td><g:message code="outboundInterChange.response.title"/></td>
                    <td class="value"><textarea rows="12" cols="45" readonly="readonly" wrap='off'>${(selected.response)}</textarea></td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
