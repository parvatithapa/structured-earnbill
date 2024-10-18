%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%@ page import="com.sapienter.jbilling.common.CommonConstants; com.sapienter.jbilling.server.pricing.PriceModelWS" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<g:each var="d" in="${pricingDates}">
    <g:if test="${TimezoneHelper.currentDateForTimezone(session['company_timezone']).after(d) }">
    %{-- Overwrite the applicable Date such that it is the max date less than or equal to the current date--}%
        <g:set var="applicableDate" value="${d}"/>
    </g:if>
</g:each>
<div id="timeline">
    <div class="form-columns">
        <ul>
            <g:if test="${pricingDates}">
                <g:each var="date" status="i" in="${pricingDates}">
                    <li class="${(startDate ? startDate.equals(date) : applicableDate.equals(date)) ? 'current' : ''}">
                        <g:set var="pricingDate" value="${formatDate(date: date)}"/>
                        <g:remoteLink action="edit" params="[_eventId: 'editDate', startDate: pricingDate]"
                                      update="ui-tabs-review" method="GET" onSuccess="refreshPlan();">
                            ${pricingDate}
                        </g:remoteLink>
                    </li>
                </g:each>
            </g:if>
            <g:else>
                <li class="current">
                    <g:set var="pricingDate" value="${formatDate(date: CommonConstants.EPOCH_DATE)}"/>
                    <g:remoteLink action="edit" params="[_eventId: 'editDate', startDate : pricingDate]"
                                  update="ui-tabs-review" method="GET" onSuccess="refreshPlan();">
                        ${pricingDate}
                    </g:remoteLink>
                </li>
            </g:else>

            <li class="new">
                <g:remoteLink action="edit" params="[_eventId: 'addDate', startDate : formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')]"
                              update="ui-tabs-review" method="GET" onSuccess="refreshPlan();">
                    <g:message code="button.add.price.date"/>
                </g:remoteLink>
            </li>
        </ul>
    </div>

    <div class="hidden">
        <g:formRemote name="showTimeline-form" url="[action: 'edit']" update="timeline" method="GET">
            <g:hiddenField name="_eventId" value="timeline"/>
        </g:formRemote>
    </div>

    <div class="hidden">
        <g:formRemote name="showDetails-form" url="[action: 'edit']" update="details-box" method="GET">
            <g:hiddenField name="_eventId" value="details"/>
        </g:formRemote>
    </div>
    <script type="text/javascript">

    function refreshPlan() {
        refreshTimeline();
        refreshDetails();
    }

    function refreshTimeline() {
        $('#showTimeline-form').submit();
    };

    function refreshDetails() {
        $('#showDetails-form').submit();
    };

    </script>
</div>

