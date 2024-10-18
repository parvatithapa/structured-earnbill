<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<html>
<head>
    <meta name="layout" content="panels" />
</head>

<body>
<content tag="column1">
    <div class="table-box">
        <div class="table-scroll">
            <table id="requests" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <g:isRoot>
                        <th class="medium">
                            <g:remoteSort action="list" sort="provisioning.id" update="column1">
                                <g:message code="provisioning.request.label.id"/>
                            </g:remoteSort>
                        </th>
                    </g:isRoot>
                    <th class="medium">
                        <g:remoteSort action="list" sort="provisioning.processor" update="column1">
                            <g:message code="provisioning.request.label.processor"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium">
                        <g:remoteSort action="list" sort="provisioning.processor" update="column1">
                            <g:message code="provisioning.request.label.status"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium">
                        <g:remoteSort action="list" sort="provisioning.execution_order" update="column1">
                            <g:message code="provisioning.request.label.execution_order"/>
                        </g:remoteSort>
                    </th>
                </tr>
                </thead>
                <tbody>
                <g:each var="req" in="${selectedCMD?.provisioningRequests}">
                    <tr id="req-${req.id}">
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="show_requests" params="[selectedId: selectedId, type: params.type, typeId: params.typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.id}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="show_requests" params="[selectedId: selectedId, type: params.type, typeId: params.typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${StringEscapeUtils.escapeHtml(req?.processor)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="show_requests" params="[selectedId: selectedId, type: params.type, typeId: params.typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.requestStatus}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="show_requests" params="[selectedId: selectedId, type: params.type, typeId: params.typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.executionOrder}</span>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
    </div>
</content>

<content tag="column2">
    <g:render template="showRequest"/>
</content>
</body>
</html>