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
  _login

  @author Vikas Bodani
  @since  1-08-2011
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="${[name: filter.name] + hiddenFilters}" update="filters"/>
    
    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:set var="filter_name" value="filters.${filter.name}.integerValue"/>
                    <g:set var="filter_value" value="${filter.integerValue}"/>
                    <g:set var="filter_from" value="${[1, 0]}"/>
                    <g:set var="filter_valueMessagePrefix" value="filters.trueOrFalse"/>
                    <g:set var="filter_noSelection" value="['': message(code: 'filters.trueOrFalse.empty')]"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_valueMessagePrefix: filter_valueMessagePrefix,
                            select_noSelection: filter_noSelection
                    ]">
                    </g:applyLayout>

                    %{--<g:select name="filters.${filter.name}.integerValue"
                              value="${filter.integerValue}"
                              from="${[1,0]}"
                              valueMessagePrefix='filters.trueOrFalse'
                              noSelection="['': message(code: 'filters.trueOrFalse.empty')]"/>--}%
                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.status.label"/></label>
            </div>
        </fieldset>
    </div>

    <g:set var="filter_selector" value="select[name='filters.${filter.name}.integerValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>
