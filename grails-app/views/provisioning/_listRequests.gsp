<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
   <div class="table-box">
       <div class="table-scroll">
           <table id="requests" cellspacing="0" cellpadding="0">
               <thead>
               <tr>
                    <g:isRoot>
                        <th class="medium"><g:message code="provisioning.request.label.id"/></th>
                    </g:isRoot>
                    <th class="medium"><g:message code="provisioning.request.label.processor"/></th>
                    <th class="medium"><g:message code="provisioning.request.label.status"/></th>
                    <th class="medium"><g:message code="provisioning.request.label.execution_order"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="req" in="${requests}">
                    <tr id="req-${req.id}" class="${selected?.id == req.id ? 'active' : ''}">
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="showRequests" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.id}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="showRequests" before="register(this);" onSuccess="render(data, next);">
                                <span>${StringEscapeUtils.escapeHtml(req?.processor)}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="showRequests" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.requestStatus}</span>
                            </g:remoteLink>
                        </td>
                        <td class="medium">
                            <g:remoteLink class="cell" id="${req.id}" action="showRequests" before="register(this);" onSuccess="render(data, next);">
                                <span>${req.executionOrder}</span>
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
                        action: 'callRequestsList',
                        extraParams: [selectedId : params.selectedId]
                        ]"/>

        </div>
    </div>
    <jB:isPaginationAvailable total="${totalCount?totalCount:0}">
        <div class="row-center">
            <jB:remotePaginate controller="provisioning" action="callRequestsList" before="register(this);" onSuccess="render(data, current);" params="[max: params.max]"
                                 total="${totalCount?totalCount:0}"
                                 update="column1"/>
        </div>
    </jB:isPaginationAvailable>
</div>

