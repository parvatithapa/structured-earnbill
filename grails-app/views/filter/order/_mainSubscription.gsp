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

<%--
  Order main subscription flag filter

  @author Brian Cowdery
  @since  11-Apr-2011
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:set var="filter_name" value="filters.${filter.name}.integerValue"/>
                    <g:set var="filter_from" value="${[0, 1]}"/>
                    <g:set var="filter_valueMessagePrefix" value="filters.isCurrent"/>
                    <g:set var="filter_noSelection" value="['': message(code: 'filters.isCurrent.empty')]"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_from: filter_from,
                            select_valueMessagePrefix: filter_valueMessagePrefix,
                            select_noSelection: filter_noSelection
                    ]">
                    </g:applyLayout>

                </div>
                <label for="filters.${filter.name}.integerValue"><g:message code="filters.${filter.field}.label"/></label>
            </div>
        </fieldset>
    </div>

    <g:set var="filter_selector" value="select[name='filters.${filter.name}.integerValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>
