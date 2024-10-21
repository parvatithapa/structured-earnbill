<%@ page import="com.sapienter.jbilling.server.adennet.AdennetConstants;" %>

<div id="${filter.name}">
    <span class="title"><g:message
            code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>


    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-filter">
                    <g:set var="filter_name" value="filters.${filter.name}.stringValue"/>
                    <g:set var="filter_value" value="${filter.stringValue}"/>
                    <g:set var="filter_from" value="${[[value:'',label:message(code:'filters.status.select.option')],[value:AdennetConstants.TRN_STATUS_WALLET_TOP_UP,label:message(code:'filters.status.wallet.topup')], [value:AdennetConstants.TRN_STATUS_RECHARGE,label:message(code:'filters.status.recharge')],
                    [value:AdennetConstants.TRN_STATUS_RECHARGE_REQUEST,label:message(code:'filters.status.recharge.request')],[value:AdennetConstants.TRN_STATUS_BUY_SUBSCRIPTION,label:message(code:'filters.status.buy.subscription')],[value:AdennetConstants.TRN_STATUS_REFUND,label:message(code:'filters.status.refund')],[value:AdennetConstants.TRN_STATUS_SIM_REISSUED,label:message(code:'filters.status.sim.reissued')]]}"/>
                    <g:set var="filter_optionKey" value="value"/>
                    <g:set var="filter_optionValue" value="label"/>

                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_optionKey: filter_optionKey,
                            select_optionValue: filter_optionValue
                    ]">
                    </g:applyLayout>

                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="Type"/></label>
            </div>
        </fieldset>
    </div>
    <g:set var="filter_selector" value="select[name='filters.${filter.name}.stringValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>
