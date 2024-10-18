<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<html>
<head>
    <meta name="layout" content="panels" />
</head>

<body>
<content tag="column1">
    <div class="table-box">
        <div class="table-scroll">
            <table id="invoices" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <th class="large">
                        <g:remoteSort action="showCommands" sort="name" update="column1">
                            <g:message code="provisioning.label.name"/>
                        </g:remoteSort>
                    </th>
                    <th class="large">
                        <g:remoteSort action="showCommands" sort="commandStatus" update="column1">
                            <g:message code="provisioning.label.status"/>
                        </g:remoteSort>
                    </th>
                    <g:isRoot>
                        <th class="medium">
                            <g:remoteSort action="showCommands" sort="commandType" update="column1">
                                <g:message code="provisioning.label.type"/>
                            </g:remoteSort>
                        </th>
                    </g:isRoot>
                    <th class="medium">
                        <g:remoteSort action="showCommands" sort="createDate" update="column1">
                            <g:message code="provisioning.label.date"/>
                        </g:remoteSort>
                    </th>
                </tr>
                </thead>

                <tbody>
                <g:each var="cmd" in="${commands}">
                    <tr id="req-${cmd.id}" class="${selected?.id == cmd.id ? 'active' : ''}">
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="filter" params="[type: type, typeId: typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${StringEscapeUtils.escapeHtml(cmd?.name)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="filter" params="[type: type, typeId: typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${cmd.commandStatus}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="filter" params="[type: type, typeId: typeId]" before="register(this);" onSuccess="render(data, next);">
                                <span>${cmd.commandType}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="filter" params="[type: type, typeId: typeId]" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${cmd.createDate}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/>
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
    <g:render template="showCommand"/>
</content>
</body>
</html>