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
  
<%@ page contentType="text/html;charset=UTF-8" %>

<g:set var="headerNames" value="${[]}" />
<g:set var="rowHeaderMap" value="${[:]}" />
<g:set var="searchParams" value="${[partial: true, jobType : params.jobType?: '', startDate : params.startDate?: '', startBeforeDate : params.startBeforeDate?: '', id : params.id?: '']}" />


<div class="table-box">
    <table id="jobExecutionList" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th class="header-sortable first">
                    <g:remoteSort action="list" sort="id" update="job-exec-list" searchParams="${searchParams}"><g:message code="jobExecution.label.id"/></g:remoteSort>
                </th>
                <th class="large">
                    <g:remoteSort action="list" sort="startDate" update="job-exec-list" searchParams="${searchParams}"><g:message code="jobExecution.label.startDate"/></g:remoteSort>
                </th>
                <th class="large">
                    <g:remoteSort action="list" sort="endDate" update="job-exec-list" searchParams="${searchParams}"><g:message code="jobExecution.label.endDate"/></g:remoteSort>
                </th>
                <th class="large">
                    <g:remoteSort action="list" sort="jobType" update="job-exec-list" searchParams="${searchParams}"><g:message code="jobExecution.label.jobType"/></g:remoteSort>
                </th>
                <th class="large">
                    <g:remoteSort action="list" sort="status" update="job-exec-list" searchParams="${searchParams}"><g:message code="jobExecution.label.status"/></g:remoteSort>
                </th>
                <g:if test="${params.jobType && jobExecutionList.size() > 0}">
                    <g:each in="${jobExecutionList[0].lines.findAll{it.type=='HEADER'}.sort{it.name}}" var="header">
                        <th class="large"><g:message code="jobExecution.header.${header.name}" default="${header.name}"/></th>
                        <%
                            headerNames.add(header.name)
                        %>
                    </g:each>
                </g:if>
            </tr>
        </thead>


        <tbody>
            <g:each var="jobExecution" in="${jobExecutionList}">
                <tr id="jobExecution-${jobExecution.id}" >
                    
                    %{-- Id --}%
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                            ${jobExecution?.id}
                        </g:remoteLink>
                    </td>
                    %{--Start Date --}%
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:formatDate date="${jobExecution?.startDate}" formatName="date.time.format"/>
                        </g:remoteLink>
                    </td>
                    %{--End Date--}%
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:formatDate date="${jobExecution?.endDate}" formatName="date.time.format"/>
                        </g:remoteLink>
                    </td>
                    %{-- Type --}%
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                            ${jobExecution?.jobType}
                        </g:remoteLink>
                    </td>
                    %{-- Status --}%
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                            ${jobExecution?.status}
                        </g:remoteLink>
                    </td>
                    <%
                        rowHeaderMap.clear()
                        jobExecution.lines.findAll{it.type=='HEADER'}.each{rowHeaderMap[it.name] = it.value}
                    %>
                    <g:each in="${headerNames}" var="headerName">
                        <td>
                            <g:remoteLink class="cell double" action="show" id="${jobExecution.id}" before="register(this);" onSuccess="render(data, next);">
                                ${rowHeaderMap[headerName] ?: ''}
                            </g:remoteLink>
                        </td>
                    </g:each>
                </tr>
            </g:each>
        </tbody>
    </table>


</div>

<div class="pager-box">

    %{-- remote pager does not support "onSuccess" for panel rendering, take a guess at the update column --}%
    <g:set var="action" value="list"/>
    <g:set var="updateColumn" value="job-exec-list"/>

    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                      model="${[steps: [10, 20, 50], updateOverride: updateColumn, extraParams: searchParams]}"/>
        </div>
    </div>
    <jB:isPaginationAvailable total="${jobExecutionList.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="jobExecution" action="${action}"
                               params="${sortableParams(params: searchParams)}"
                               total="${jobExecutionList?.totalCount ?: 0}" update="${updateColumn}"/>
        </div>
    </jB:isPaginationAvailable>
</div>

