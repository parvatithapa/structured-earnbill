<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
    <div class="table-box">
        <div class="table-scroll">
            <table id="invoices" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <th class="medium first header-sortable">
                        <g:remoteSort action="showCommands" sort="name" update="column1">
                            <g:message code="provisioning.label.name"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium header-sortable">
                        <g:remoteSort action="showCommands" sort="commandStatus" update="column1">
                            <g:message code="provisioning.label.status"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium header-sortable">
                        <g:remoteSort action="showCommands" sort="class" update="column1">
                            <g:message code="provisioning.label.type"/>
                        </g:remoteSort>
                    </th>
                    <th class="medium last header-sortable">
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
                            <g:remoteLink class="cell" id="${cmd.id}" action="showCommands" before="register(this);" onSuccess="render(data, next);"  params="[show: true]">
                                <span>${StringEscapeUtils.escapeHtml(cmd?.name)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="showCommands" before="register(this);" onSuccess="render(data, next);" params="[show: true]">
                                <span>${cmd.commandStatus}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="showCommands" before="register(this);" onSuccess="render(data, next);" params="[show: true]">
                                <span>${cmd.commandType}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${cmd.id}" action="showCommands" before="register(this);" onSuccess="render(data, next);" params="[show: true]">
                                <g:formatDate date="${cmd.createDate}" formatName="date.pretty.format" timeZone="${session['company_timezone']}"/>
                            </g:remoteLink>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
    </div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                      model="[steps: [10, 20, 50], update: 'column1', contactFieldTypes: contactFieldTypes,
                              action: 'callCommandsList',
                              extraParams: [selectedId : params.selectedId]
                      ]"/>
        </div>
    </div>
    <jB:isPaginationAvailable total="${commands?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="provisioning" action="callCommandsList" before="register(this);" onSuccess="render(data, current);" params="${sortableParams(params: [partial: true, max: params.max])}"
                                 total="${commands.totalCount?:0}"
                                 update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

