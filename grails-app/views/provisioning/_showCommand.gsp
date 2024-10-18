<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<div class="column-hold">
    <div class="heading">
        <strong><g:message code="provisioning.information.title"/></strong>
    </div>

    <!-- command information -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="provisioning.label.id"/></td>
                    <td class="value">${selected?.id}</td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.name"/></td>
                    <td class="value">${selected?.name}</td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.execution_order"/></td>
                    <td class="value">${selected?.executionOrder}</td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.create_date"/></td>
                    <td class="value">
                        <g:formatDate date="${selected?.createDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.last_update_date"/></td>
                    <td class="value">
                        <g:formatDate date="${selected?.lastUpdateDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.command_status"/></td>
                    <td class="value">${selected?.commandStatus}</td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.command_type"/></td>
                    <td class="value">${selected?.commandType}</td>
                </tr>
                <tr>
                    <td><g:message code="provisioning.label.type_identifier"/></td>
                    <td class="value">${typeId}</td>
                </tr>
                </tbody>
            </table>

        </div>
    </div>
    <div class="table-box">
        <div class="table-scroll">
            <table id="requests" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <g:isRoot>
                        <th class="medium"><g:message code="provisioning.request.label.id"/></th>
                    </g:isRoot>
                    <th class="medium"><g:message code="provisioning.request.label.processor"/></th>
                    <th class="medium"><g:message code="provisioning.request.label.execution_order"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="req" in="${selected?.provisioningRequests}">
                    <tr id="req-${req.id}">
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}">
                                <span>${req.id}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}">
                                <span>${StringEscapeUtils.escapeHtml(req?.processor)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}">
                                <span>${req.executionOrder}</span>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
    </div>
    <g:if test="${selected?.id}">
        <div class="btn-box table-box-below">
            <div class="row">
                <g:link class="submit show button-primary" controller="provisioning" action="showRequests" params="[selectedId: selected?.id]">
                    <span><g:message code="button.show_request" /></span>
                </g:link>
            </div>
        </div>
    </g:if>
</div>
