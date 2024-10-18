<%@ page import="com.sapienter.jbilling.server.provisioning.ProvisioningCommandType" %>


<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:set var="filter_name" value="filters.${filter.name}.integerValue"/>
                    <g:set var="filter_from" value="${ProvisioningCommandType.values() }"/>
                    <g:set var="filter_value" value="${filter.integerValue}"/>
                    <g:set var="filter_optionKey" value="key"/>
                    <g:set var="filter_optionValue" value="value"/>
                    <g:set var="filter_noSelection" value="['': message(code: 'filters.status.empty')]"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_noSelection: filter_noSelection,
                            select_optionKey: filter_optionKey,
                            select_optionValue: filter_optionValue
                    ]">
                    </g:applyLayout>

                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.provisioning.command_type.title"/></label>
            </div>
        </fieldset>
    </div>
    <g:set var="filter_selector" value="select[name='filters.${filter.name}.integerValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>

