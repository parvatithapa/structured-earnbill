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

<%--
  View template for rendering 'Show Records' link for different statuses of mediation process records.

  @author Neelabh Dubey
  @since 25-Jul-2016
--%>

<td><g:message code="${messageCode}"/></td>
<td class="value">${value}</td>
<g:if test="${value != 0}">
    <td class="value">
        <sec:access url="/invoice/list">
            <g:link controller="mediation" action="${action}" id="${processId}" params="${params + ['status': status, 'first': first]}">
                <g:message code="mediation.show.all.records"/>
            </g:link>
        </sec:access>
    </td>
</g:if>
