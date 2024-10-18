<%@ page import="com.sapienter.jbilling.server.mediation.db.MediationConfiguration; org.apache.commons.lang.StringEscapeUtils" %>
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

<div class="table-box">
	<div class="table-scroll">
    	<table id="processes" cellspacing="0" cellpadding="0">
			<thead>
				<tr>
					<th class="small2 header-sortable">
                        <g:remoteSort action="list" sort="startDate" update="column1">
                            <g:message code="mediation.th.start.date" />
                        </g:remoteSort>
                    </th>
					<th class="small2 header-sortable">
                        <g:remoteSort action="list" sort="endDate" update="column1">
                            <g:message code="mediation.th.end.date" />
                        </g:remoteSort>
                    </th>
					<th class="small header-sortable">
                        <g:message code="mediation.th.total.records" />
                    </th>
                    <th class="small header-sortable">
                        <g:remoteSort action="list" sort="recordsProcessed" update="column1">
                            <g:message code="mediation.th.orders.affected"/>
                        </g:remoteSort>
                    </th>

                </tr>
			</thead>
	
            <tbody>
				<g:each var="entry" status="idx" in="${processes}">
                    <g:set var="proc" value="${entry}"/>
                    <g:set var="configurationId" value="${proc.configurationId}"/>
					<tr id="mediation-${proc.id}" class="${proc?.id == processId ? 'active' : ''}">
						<td>
							<g:remoteLink breadcrumb="id" class="cell" action="show" id="${proc.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${proc.startDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                            </g:remoteLink>
						</td>
                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${proc.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${proc.endDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                            </g:remoteLink>
                        </td>
						<td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${proc.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                ${proc.recordsProcessed}
                            </g:remoteLink>
                        </td>
                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${proc.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                                ${proc.doneAndBillable}
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
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
    </div>

    <div class="row">
        <jB:remotePaginate controller="mediation" action="index" params="${sortableParams(params: [partial: true])}" total="${size}" update="column1"/>
    </div>
</div>

<div class="btn-box">
    <div class="row"></div>
</div>
